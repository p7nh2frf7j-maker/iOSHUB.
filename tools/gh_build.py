"""把工程推上 GitHub，用 Actions 的 macOS 机器编出未签名 IPA，再下载回来。

用法（token 从文件读，不写进命令行历史）：
    <bundled python> tools/gh_build.py --token-file .tools/gh_token.txt --repo iOSHUB

token 需要：classic PAT 勾选 `repo` + `workflow` 两个 scope；
           或 fine-grained 勾 Contents/Workflows/Actions 读写。

脚本做的事：
  1. GET /user               验证令牌
  2. POST /user/repos        建仓库（已存在则复用）
  3. 上传文件                blob → tree → commit → ref
  4. workflow_dispatch       触发 .github/workflows/ios.yml
  5. 轮询 run                直到 success / failure
  6. 下载 artifact           解出 iOSHUB-unsigned.ipa 放到 dist/
"""
from __future__ import annotations

import argparse
import base64
import io
import json
import pathlib
import sys
import time
import urllib.error
import urllib.request
import zipfile

ROOT = pathlib.Path(__file__).resolve().parent.parent
API = "https://api.github.com"
WORKFLOW = "ios.yml"

EXCLUDE_DIRS = {".tools", "dist", ".git", "build", ".gradle", "node_modules", "Pods", ".dsh", "__pycache__"}
EXCLUDE_FILES = {"local.properties", "keystore.properties", ".DS_Store", "gh_token.txt"}


def log(msg: str) -> None:
    print(msg, flush=True)


def api(path: str, token: str, method: str = "GET", payload: dict | None = None,
        raw: bool = False, timeout: int = 60):
    url = path if path.startswith("http") else API + path
    data = json.dumps(payload).encode() if payload is not None else None
    req = urllib.request.Request(url, data=data, method=method)
    req.add_header("Accept", "application/vnd.github+json")
    req.add_header("X-GitHub-Api-Version", "2022-11-28")
    req.add_header("User-Agent", "ioshub-build-script")
    req.add_header("Authorization", f"Bearer {token}")
    if data is not None:
        req.add_header("Content-Type", "application/json")
    last_err: Exception | None = None
    for attempt in range(6):
        try:
            with urllib.request.urlopen(req, timeout=timeout) as resp:
                body = resp.read()
                return body if raw else (json.loads(body) if body else {})
        except urllib.error.HTTPError as exc:
            detail = exc.read().decode("utf-8", "replace")[:600]
            if exc.code >= 500 and attempt < 5:
                last_err = RuntimeError(f"{method} {url} -> HTTP {exc.code}")
                time.sleep(2 + attempt * 2)
                continue
            raise RuntimeError(f"{method} {url} -> HTTP {exc.code}\n{detail}") from None
        except (urllib.error.URLError, TimeoutError, OSError, Exception) as exc:  # 网络抖动 / SSL EOF
            name = type(exc).__name__
            if "HTTPError" in name:
                raise
            last_err = exc
            wait = min(2 + attempt * 3, 20)
            log(f"  · 网络中断（{name}），{wait}s 后重试（{attempt + 1}/6）")
            time.sleep(wait)
    raise RuntimeError(f"{method} {url} -> 多次重试仍失败：{last_err}")


def collect_files() -> list[tuple[str, bytes]]:
    """按仓库需要的范围收集文件（不含构建产物与本地工具链）。"""
    files: list[tuple[str, bytes]] = []
    for p in sorted(ROOT.rglob("*")):
        if not p.is_file():
            continue
        rel = p.relative_to(ROOT)
        parts = rel.parts[:-1]
        if any(part in EXCLUDE_DIRS for part in parts):
            continue
        if rel.parts[0] == "android":
            if "build" in parts or p.suffix in {".apk", ".aab", ".jks", ".keystore", ".iml"}:
                continue
        if p.name in EXCLUDE_FILES or p.suffix == ".pyc":
            continue
        if p.stat().st_size > 8 * 1024 * 1024:
            log(f"  跳过过大的文件: {rel}")
            continue
        files.append((rel.as_posix(), p.read_bytes()))
    return files


def ensure_repo(token: str, owner: str, repo: str, private: bool) -> tuple[str, str]:
    """返回 (owner, repo)；已存在就复用。"""
    try:
        info = api(f"/repos/{owner}/{repo}", token)
        log(f"仓库已存在：{owner}/{repo}（默认分支 {info.get('default_branch')}）")
        return owner, repo
    except RuntimeError as exc:
        if "HTTP 404" not in str(exc):
            raise
    log(f"创建仓库 {owner}/{repo} …")
    created = api("/user/repos", token, "POST", {
        "name": repo,
        "private": private,
        "auto_init": True,
        "description": "iOS HUB by nevercrynow · 全生态 iOS 资源中心（SwiftUI + Compose 双端）",
        "has_issues": True,
        "has_wiki": False,
    })
    return created["owner"]["login"], created["name"]


def bootstrap_if_empty(token: str, owner: str, repo: str) -> None:
    """git data API 在空仓库上会 409，先确认仓库里已经有提交。"""
    try:
        listing = api(f"/repos/{owner}/{repo}/contents/", token)
        if isinstance(listing, list) and listing:
            return
        if isinstance(listing, dict) and listing:
            return
    except RuntimeError as exc:
        if "HTTP 404" not in str(exc):
            raise
    log("仓库是空的，先建一个初始提交 …")
    try:
        api(f"/repos/{owner}/{repo}/contents/README.bootstrap", token, "PUT", {
            "message": "chore: bootstrap repository",
            "content": base64.b64encode(b"iOS HUB build bootstrap\n").decode(),
        })
    except RuntimeError as exc:
        if "HTTP 422" not in str(exc):
            raise


def push_tree(token: str, owner: str, repo: str, files: list[tuple[str, bytes]]) -> str:
    bootstrap_if_empty(token, owner, repo)
    log(f"上传 {len(files)} 个文件（blob → tree → commit）…")
    entries = []
    for i, (path, data) in enumerate(files, 1):
        blob = api(f"/repos/{owner}/{repo}/git/blobs", token, "POST", {
            "content": base64.b64encode(data).decode(),
            "encoding": "base64",
        })
        entries.append({"path": path, "mode": "100644", "type": "blob", "sha": blob["sha"]})
        if i % 25 == 0:
            log(f"  已上传 {i}/{len(files)}")
        time.sleep(0.05)
    tree = api(f"/repos/{owner}/{repo}/git/trees", token, "POST", {"tree": entries})
    commit = api(f"/repos/{owner}/{repo}/git/commits", token, "POST", {
        "message": "iOS HUB: SwiftUI 双端工程、种子库与 GitHub Actions 打包流水线",
        "tree": tree["sha"],
    })
    sha = commit["sha"]
    try:
        api(f"/repos/{owner}/{repo}/git/refs", token, "POST", {"ref": "refs/heads/main", "sha": sha})
    except RuntimeError as exc:
        if "HTTP 422" not in str(exc):
            raise
        log("main 分支已存在，强制更新…")
        try:
            api(f"/repos/{owner}/{repo}/git/refs/heads/main", token, "PATCH", {"sha": sha, "force": True})
        except RuntimeError:
            api(f"/repos/{owner}/{repo}/git/refs/heads/master", token, "PATCH", {"sha": sha, "force": True})
    log(f"提交完成：{sha[:10]}")
    return sha


def trigger(token: str, owner: str, repo: str, ref: str) -> None:
    log(f"触发 workflow {WORKFLOW} @ {ref} …")
    last: Exception | None = None
    for attempt in range(6):
        try:
            api(f"/repos/{owner}/{repo}/actions/workflows/{WORKFLOW}/dispatches", token, "POST", {"ref": ref})
            return
        except RuntimeError as exc:
            last = exc
            log(f"  触发失败（{attempt + 1}/6），10 秒后重试：{str(exc).splitlines()[0]}")
            time.sleep(10)
    raise RuntimeError(f"无法触发 workflow：{last}")


def wait_run(token: str, owner: str, repo: str, sha: str, limit_s: int = 1800) -> dict:
    log("等待 Actions 排队与运行 …")
    started = time.time()
    run = None
    while time.time() - started < limit_s:
        runs = api(f"/repos/{owner}/{repo}/actions/runs?per_page=20", token).get("workflow_runs", [])
        for r in runs:
            if r.get("head_sha") == sha or r.get("head_branch") == "main":
                run = r
                break
        if run:
            status, conclusion = run.get("status"), run.get("conclusion")
            log(f"  [{int(time.time() - started)}s] {status} / {conclusion or '-'}")
            if status == "completed":
                return run
        time.sleep(15)
    raise RuntimeError("等待超时（30 分钟），请到仓库 Actions 页面查看。")


def dump_logs(token: str, owner: str, repo: str, run_id: int) -> None:
    log("—— 构建失败，抓取日志 ——")
    try:
        raw = api(f"/repos/{owner}/{repo}/actions/runs/{run_id}/logs", token, raw=True, timeout=180)
    except RuntimeError as exc:
        log(f"日志下载失败：{exc}")
        return
    target = ROOT / ".tools" / "actions-logs"
    target.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(io.BytesIO(raw)) as z:
        z.extractall(target)
    for f in sorted(target.rglob("*.txt")):
        text = f.read_text("utf-8", "replace")
        hits = [ln for ln in text.splitlines() if "error:" in ln or "** BUILD FAILED **" in ln or "The following build commands failed" in ln]
        if hits:
            log(f"\n== {f.name} ==")
            for ln in hits[:25]:
                log("  " + ln.strip()[:300])
    log(f"完整日志在 {target}")


def download_artifact(token: str, owner: str, repo: str, run_id: int) -> pathlib.Path | None:
    arts = api(f"/repos/{owner}/{repo}/actions/runs/{run_id}/artifacts", token).get("artifacts", [])
    ipa_art = [a for a in arts if "ipa" in a["name"].lower()]
    if not ipa_art:
        log("没有找到 IPA 产物（artifact 列表为空）。")
        return None
    art = ipa_art[0]
    log(f"下载产物：{art['name']}（{art['size_in_bytes'] / 1048576:.1f} MB）")
    raw = api(art["archive_download_url"], token, raw=True, timeout=600)
    dist = ROOT / "dist"
    dist.mkdir(exist_ok=True)
    with zipfile.ZipFile(io.BytesIO(raw)) as z:
        z.extractall(dist)
    found = sorted(dist.rglob("*.ipa"), key=lambda p: p.stat().st_mtime, reverse=True)
    if not found:
        log("压缩包里没有 .ipa。")
        return None
    return found[0]


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--token-file", default=".tools/gh_token.txt")
    ap.add_argument("--repo", default="iOSHUB")
    ap.add_argument("--private", action="store_true")
    ap.add_argument("--ref", default="main")
    args = ap.parse_args()

    token_path = ROOT / args.token_file
    lines = [ln.strip() for ln in token_path.read_text("utf-8").splitlines() if ln.strip()]
    if len(lines) < 2:
        log(f"{args.token_file} 需要两行：第一行 GitHub 用户名，第二行令牌。")
        return 2
    user, token = lines[0], lines[-1]

    me = api("/user", token)
    log(f"令牌有效：{me['login']}")
    owner = user if user.lower() == me["login"].lower() else me["login"]

    owner, repo = ensure_repo(token, owner, args.repo, args.private)
    files = collect_files()
    sha = push_tree(token, owner, repo, files)
    trigger(token, owner, repo, args.ref)
    run = wait_run(token, owner, repo, sha)
    run_id = run["id"]
    log(f"结论：{run['conclusion']}  {run['html_url']}")

    ipa = download_artifact(token, owner, repo, run_id)
    if run["conclusion"] == "success" and ipa:
        log(f"\n✅ 成功：{ipa.relative_to(ROOT)}  {ipa.stat().st_size / 1048576:.2f} MB")
        return 0

    if run["conclusion"] != "success":
        dump_logs(token, owner, repo, run_id)
    return 1


if __name__ == "__main__":
    sys.exit(main())
