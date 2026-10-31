"""轻量静态检查：括号配平 + 类型重名 + 关键引用 + 种子 JSON。

没有 Swift 编译器时的兜底检查，能抓出大部分手写代码的机械错误。
用法：python tools/check_project.py
"""
import json
import pathlib
import re
import sys

ROOT = pathlib.Path("iOSHUB")


def strip_code(src: str) -> str:
    """去掉注释与字符串字面量，避免括号计数被内容干扰。"""
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
        elif c == '"':
            if src[i : i + 3] == '"""':
                j = src.find('"""', i + 3)
                i = n if j == -1 else j + 3
            else:
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


DECL = re.compile(r"\b(?:struct|final class|class|enum|actor|protocol)\s+([A-Za-z_][A-Za-z0-9_]*)")

errors = []
decls = {}
files = sorted(ROOT.rglob("*.swift"))
for path in files:
    raw = path.read_text(encoding="utf-8")
    code = strip_code(raw)
    for opener, closer, label in (("{", "}", "大括号"), ("(", ")", "小括号"), ("[", "]", "方括号")):
        if code.count(opener) != code.count(closer):
            errors.append(f"{path.name}: {label}不配平 {code.count(opener)} vs {code.count(closer)}")
    for m in DECL.finditer(code):
        decls.setdefault(m.group(1), []).append(path.name)

dups = {k: v for k, v in decls.items() if len(v) > 1 and k != "CodingKeys"}

# .xcodeproj 结构检查：括号配平 + GUID 引用完整性
pbx_path = pathlib.Path("iOSHUB.xcodeproj/project.pbxproj")
pbx_errors = []
pbx_text = pbx_path.read_text(encoding="utf-8")
if pbx_text.count("{") != pbx_text.count("}"):
    pbx_errors.append(f"pbxproj 大括号不配平 {pbx_text.count('{')} vs {pbx_text.count('}')}")
if pbx_text.count("(") != pbx_text.count(")"):
    pbx_errors.append("pbxproj 小括号不配平")
guid = re.compile(r"C0FFEE[0-9A-F]{18}")
defined = set(re.findall(r"(C0FFEE[0-9A-F]{18})(?:\s*/\*[^*]*\*/)?\s*=", pbx_text))
referenced = set(guid.findall(pbx_text))
undefined = referenced - defined
if undefined:
    pbx_errors.append(f"pbxproj 引用了未定义的对象: {sorted(undefined)}")
if not re.search(r"rootObject = C0FFEE[0-9A-F]{18}", pbx_text):
    pbx_errors.append("pbxproj 缺少 rootObject")

required = [
    "CatalogStore", "ResourceItem", "ResourceCategory", "NewsItem", "NewsFeed",
    "Pill", "SectionHeader", "SearchEntry", "RemoteIcon", "EmptyStateView",
    "LoadingRow", "NoticeBanner", "FeaturedCard", "ResourceCard", "ResourceRow",
    "ResourceTile", "CategoryChip", "CategoryRow", "GitHubRepoRow", "NewsRow",
    "HomeView", "ResourcesView", "CategoryDetailView", "SearchView", "DiscoverView",
    "FavoritesView", "ProfileView", "NewsFeedView", "FollowedCategoriesView",
    "ResourceDetailView", "AppRouting", "LibraryService", "PersistenceController",
    "GitHubAPI", "NewsLoader", "HTTPClient", "APIError", "JSONCoding",
    "SeedLoader", "AppSettings", "ResourceSort", "StatsStore",
]
missing = [name for name in required if name not in decls]

# 种子数据校验
seed_errors = []
seed = json.loads((ROOT / "Resources" / "seed-resources.json").read_text(encoding="utf-8"))
required_keys = [
    "id", "name", "category", "summary", "symbol", "developer", "sourceURL",
    "supportedIOS", "supportedDevices", "compatibility", "risk", "tags",
    "screenshots", "isFeatured",
]
cats = {"jailbreak", "trollstore", "ipa", "sideload", "tweak", "repo", "openSource",
        "security", "devTools", "theming", "utilities", "port", "news"}
risks = {"official", "community", "caution", "high"}
comps = {"verified", "partial", "pending", "unsupported"}
seen = set()
for item in seed:
    for key in required_keys:
        if key not in item:
            seed_errors.append(f"{item.get('id')}: 缺字段 {key}")
    if item["id"] in seen:
        seed_errors.append(f"重复 id: {item['id']}")
    seen.add(item["id"])
    if item["category"] not in cats:
        seed_errors.append(f"{item['id']}: 非法分类 {item['category']}")
    if item["risk"] not in risks:
        seed_errors.append(f"{item['id']}: 非法风险等级 {item['risk']}")
    if item["compatibility"] not in comps:
        seed_errors.append(f"{item['id']}: 非法兼容性 {item['compatibility']}")
    if not str(item["sourceURL"]).startswith("https://"):
        seed_errors.append(f"{item['id']}: sourceURL 不是 https")

print(f"Swift 文件: {len(files)}")
print(f"声明的类型: {len(decls)}")
print(f"重名类型: {dups if dups else '无'}")
print(f"括号问题: {errors if errors else '无'}")
print(f"缺失类型: {missing if missing else '无'}")
print(f"pbxproj 问题: {pbx_errors if pbx_errors else '无'}")
print(f"种子资源: {len(seed)} 条，问题: {seed_errors if seed_errors else '无'}")
print(f"Swift 代码行数: {sum(len(p.read_text(encoding='utf-8').splitlines()) for p in files)}")

sys.exit(1 if (missing or dups or errors or seed_errors or pbx_errors) else 0)
