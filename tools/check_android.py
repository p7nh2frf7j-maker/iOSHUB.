"""Android 工程的静态检查：括号配平、重复声明、资源引用、种子数据。

没有 Kotlin 编译器时的兜底检查（和 tools/check_project.py 同一思路）。
用法：python tools/check_android.py
"""
import json
import pathlib
import re
import sys

ROOT = pathlib.Path("android")
KT_ROOT = ROOT / "app/src/main/java"


def strip_code(src: str) -> str:
    out = []
    i, n = 0, len(src)
    while i < n:
        c = src[i]
        if c == "/" and i + 1 < n and src[i + 1] == "/":
            j = src.find("\n", i)
            i = n if j == -1 else j
        elif c == "/" and i + 1 < n and src[i + 1] == "*":
            j = src.find("*/", i + 2)
            i = n if j == -1 else j + 2
        elif src[i : i + 3] == '"""':
            j = src.find('"""', i + 3)
            i = n if j == -1 else j + 3
        elif c == "'":
            i += 1
            while i < n:
                if src[i] == "\\":
                    i += 2
                    continue
                if src[i] == "'":
                    i += 1
                    break
                i += 1
        elif c == '"':
            i += 1
            while i < n:
                if src[i] == "\\":
                    i += 2
                    continue
                if src[i] == '"':
                    i += 1
                    break
                i += 1
        else:
            out.append(c)
            i += 1
    return "".join(out)


errors = []
decls = {}
files = sorted(KT_ROOT.rglob("*.kt"))
for path in files:
    raw = path.read_text(encoding="utf-8")
    code = strip_code(raw)
    for opener, closer, label in (("{", "}", "大括号"), ("(", ")", "小括号"), ("[", "]", "方括号")):
        if code.count(opener) != code.count(closer):
            errors.append(f"{path.name}: {label}不配平 {code.count(opener)} vs {code.count(closer)}")
    for m in re.finditer(r"^(?:private |internal |public )?(?:data |sealed |enum |abstract )?(?:class|object|interface) (\w+)", code, re.M):
        decls.setdefault(m.group(1), []).append(path.name)

dups = {k: v for k, v in decls.items() if len(v) > 1}

# 分类 id 与种子数据一致性
models = (KT_ROOT / "com/nevercrynow/ioshub/data/Models.kt").read_text(encoding="utf-8")
enum_ids = set(re.findall(r'^\s+[A-Z][A-Z_]+?\("([\w]+)"', models, re.M))
seed = json.loads((ROOT / "app/src/main/assets/seed-resources.json").read_text(encoding="utf-8"))
seed_cats = {item["category"] for item in seed}
missing = seed_cats - enum_ids
if missing:
    errors.append(f"种子数据里出现 Kotlin 侧没有的分类: {sorted(missing)}")

# 与 iOS 端种子数据同步校验（两端共用一份数据，容易漏同步）
ios_seed_path = ROOT.parent / "iOSHUB/Resources/seed-resources.json"
if ios_seed_path.exists():
    ios_seed = json.loads(ios_seed_path.read_text(encoding="utf-8"))
    ios_ids = {item.get("id") for item in ios_seed}
    and_ids = {item.get("id") for item in seed}
    if ios_ids != and_ids:
        only_ios = sorted(ios_ids - and_ids)
        only_and = sorted(and_ids - ios_ids)
        errors.append(
            f"安卓 assets 的种子数据与 iOS 不一致（iOS {len(ios_seed)} 条 / 安卓 {len(seed)} 条）"
            f"；仅 iOS 有: {only_ios[:6]}；仅安卓有: {only_and[:6]}"
        )

# 资源文件完整性
required_resources = [
    ROOT / "app/src/main/AndroidManifest.xml",
    ROOT / "app/src/main/res/values/strings.xml",
    ROOT / "app/src/main/res/values/themes.xml",
    ROOT / "app/src/main/res/mipmap-xxxhdpi/ic_launcher.png",
    ROOT / "app/src/main/assets/seed-resources.json",
    ROOT / "app/build.gradle.kts",
    ROOT / "build.gradle.kts",
    ROOT / "settings.gradle.kts",
]
for item in required_resources:
    if not item.exists():
        errors.append(f"缺少文件: {item}")

manifest = (ROOT / "app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
for ref in ("@string/app_name", "@style/Theme.iOSHUB", "@mipmap/ic_launcher"):
    if ref not in manifest:
        errors.append(f"Manifest 未引用 {ref}")
if "@string/app_name" in manifest and "app_name" not in (ROOT / "app/src/main/res/values/strings.xml").read_text(encoding="utf-8"):
    errors.append("strings.xml 缺少 app_name")
if "Theme.iOSHUB" in manifest and "Theme.iOSHUB" not in (ROOT / "app/src/main/res/values/themes.xml").read_text(encoding="utf-8"):
    errors.append("themes.xml 缺少 Theme.iOSHUB")

print(f"Kotlin 文件: {len(files)}")
print(f"声明的类型: {len(decls)}")
print(f"重名类型: {dups if dups else '无'}")
print(f"括号问题: {errors if errors else '无'}")
print(f"种子分类: {len(seed_cats)} 个，Kotlin 分类枚举: {len(enum_ids)} 个")
print(f"Kotlin 代码行数: {sum(len(p.read_text(encoding='utf-8').splitlines()) for p in files)}")

sys.exit(1 if (dups or errors) else 0)
