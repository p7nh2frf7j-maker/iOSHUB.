import io

root = r"android\app\src\main\java\com\nevercrynow\ioshub"


def load(rel):
    p = root + "\\" + rel
    return p, io.open(p, encoding="utf-8").read().replace("\r\n", "\n")


def save(p, src):
    io.open(p, "w", encoding="utf-8", newline="\n").write(src)


# ---------- Screens.kt ----------
p, s = load(r"ui\Screens.kt")
s = s.replace(
    "import com.nevercrynow.ioshub.data.Category\n",
    "import com.nevercrynow.ioshub.data.Category\nimport com.nevercrynow.ioshub.data.LinkRouter\n",
    1,
)

old_card = """            item {
                HubCard {
                    Text("数据与网络", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "GitHub 未认证接口每小时只有 60 次请求。刷新只覆盖前 12 个条目，命中限流会立刻停止并提示。",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
"""
assert old_card in s, "profile card block missing"
s = s.replace(old_card, "            item { LinkCard() }\n", 1)

s += '''
// MARK: - 网络线路

/**
 * 线路卡片：GitHub 的页面 / API 在部分网络下打不开，
 * 这里让用户选一条镜像，或自己填前缀。
 */
@Composable
private fun LinkCard() {
    val scope = rememberCoroutineScope()
    var custom by remember { mutableStateOf(LinkRouter.customPrefix) }

    HubCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GlyphIcon(Glyph.Globe, size = 14.dp, tint = Brand)
            Spacer(Modifier.width(6.dp))
            Text("网络线路", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            Text(
                if (LinkRouter.current == LinkRouter.Route.AUTO) {
                    val good = LinkRouter.lastGood
                    when {
                        good == null -> "自动 · 待探测"
                        good.isEmpty() -> "自动 · 直连"
                        else -> "自动 · 镜像"
                    }
                } else {
                    LinkRouter.current.label
                },
                fontSize = 10.5f.sp,
                fontWeight = FontWeight.SemiBold,
                color = Brand,
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "链接打不开时换一条线路：App 只会改写 github.com / api.github.com 的地址，其它站点原样打开。",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(10.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(LinkRouter.Route.entries.toList()) { route ->
                val selected = LinkRouter.current == route
                Box(
                    Modifier
                        .background(
                            if (selected) Brand else MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(9.dp),
                        )
                        .clickable { LinkRouter.select(route) }
                        .padding(horizontal = 11.dp, vertical = 7.dp),
                ) {
                    Text(
                        route.label,
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        if (LinkRouter.current == LinkRouter.Route.CUSTOM) {
            Spacer(Modifier.height(10.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
                    .padding(horizontal = 11.dp, vertical = 9.dp),
            ) {
                if (custom.isBlank()) {
                    Text(
                        "https://your-mirror.example/",
                        fontSize = 11.5f.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                BasicTextField(
                    value = custom,
                    onValueChange = {
                        custom = it
                        LinkRouter.setCustom(it)
                    },
                    singleLine = true,
                    textStyle = TextStyle(
                        fontSize = 11.5f.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    cursorBrush = SolidColor(Brand),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .background(Brand.copy(alpha = 0.12f), RoundedCornerShape(9.dp))
                    .clickable {
                        if (!LinkRouter.checking) scope.launch { LinkRouter.healthCheck() }
                    }
                    .padding(horizontal = 12.dp, vertical = 7.dp),
            ) {
                Text(
                    if (LinkRouter.checking) "正在测速…" else "测速并自动选最快",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Brand,
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                "未认证 GitHub 接口 60 次/小时",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (LinkRouter.results.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            LinkRouter.results.forEach { health ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    GlyphIcon(
                        if (health.ok) Glyph.Check else Glyph.Warning,
                        size = 11.dp,
                        tint = if (health.ok) Mint else Rose,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(health.route.label, fontSize = 11.sp)
                    Spacer(Modifier.weight(1f))
                    Text(
                        health.detail,
                        fontSize = 10.5f.sp,
                        color = if (health.ok) MaterialTheme.colorScheme.onSurfaceVariant else Rose,
                    )
                }
            }
        }
    }
}
'''

save(p, s)
print("Screens.kt patched")

# ---------- DetailScreen.kt ----------
p, s = load(r"ui\DetailScreen.kt")
s = s.replace(
    "import com.nevercrynow.ioshub.data.AppState\n",
    "import com.nevercrynow.ioshub.data.AppState\nimport com.nevercrynow.ioshub.data.LinkRouter\n",
    1,
)
s = s.replace(
    """private fun safeOpen(handler: UriHandler, raw: String?) {
    if (raw.isNullOrBlank()) return
    runCatching { handler.openUri(raw) }
}""",
    """private fun safeOpen(handler: UriHandler, raw: String?) {
    if (raw.isNullOrBlank()) return
    // 按当前线路改写（只影响 GitHub 域名），再交给系统浏览器
    runCatching { handler.openUri(LinkRouter.resolve(raw)) }
}""",
    1,
)
save(p, s)
print("DetailScreen.kt patched")

# ---------- DiscoverScreen.kt / NewsScreen.kt ----------
for rel, olds in (
    (r"ui\DiscoverScreen.kt", ["{ uriHandler.openUri(repo.htmlUrl) }"]),
    (r"ui\NewsScreen.kt", ["{ uriHandler.openUri(item.url) }", "{ uriHandler.openUri(it) }"]),
):
    p, s = load(rel)
    s = s.replace(
        "import com.nevercrynow.ioshub.data.AppState\n",
        "import com.nevercrynow.ioshub.data.AppState\nimport com.nevercrynow.ioshub.data.LinkRouter\n",
        1,
    )
    for old in olds:
        new = old.replace("openUri(repo.htmlUrl)", "openUri(LinkRouter.resolve(repo.htmlUrl))") \
                 .replace("openUri(item.url)", "openUri(LinkRouter.resolve(item.url))") \
                 .replace("openUri(it)", "openUri(LinkRouter.resolve(it))")
        assert old in s, (rel, old)
        s = s.replace(old, new)
    save(p, s)
    print(rel, "patched")

# ---------- MainActivity.kt ----------
p, s = load("MainActivity.kt")
if "LinkRouter" not in s:
    s = s.replace(
        "import com.nevercrynow.ioshub.ui.HubApp\n",
        "import com.nevercrynow.ioshub.ui.HubApp\n",
        1,
    )
    s = s.replace(
        "import androidx.activity.enableEdgeToEdge\n",
        "import androidx.activity.enableEdgeToEdge\nimport com.nevercrynow.ioshub.data.LinkRouter\n",
        1,
    )
    s = s.replace(
        "        enableEdgeToEdge()",
        "        enableEdgeToEdge()\n        LinkRouter.attach(applicationContext)",
        1,
    )
save(p, s)
print("MainActivity.kt patched")
