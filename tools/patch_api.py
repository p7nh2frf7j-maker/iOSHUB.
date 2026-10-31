import io

p = r"android\app\src\main\java\com\nevercrynow\ioshub\data\GitHubApi.kt"
src = io.open(p, encoding="utf-8").read().replace("\r\n", "\n")

pairs = [
    (
        '    private const val API = "https://api.github.com"',
        '    private const val API_HOST = "https://api.github.com"',
    ),
    (
        """        val query = "topic:ios stars:>800 pushed:>$since"
        val url = "$API/search/repositories?q=" + URLEncoder.encode(query, "UTF-8") +
            "&sort=stars&order=desc&per_page=$limit"
        val body = httpGet(url)""",
        """        val query = "topic:ios stars:>800 pushed:>$since"
        val path = "/search/repositories?q=" + URLEncoder.encode(query, "UTF-8") +
            "&sort=stars&order=desc&per_page=$limit"
        val body = request(path)""",
    ),
    (
        '            JSONObject(httpGet("$API/repos/$owner/$name")).toRepo()',
        '            JSONObject(request("/repos/$owner/$name")).toRepo()',
    ),
    (
        """        val url = "$API/search/repositories?q=" + URLEncoder.encode(query, "UTF-8") +
            "&sort=stars&order=desc&per_page=$limit"
        val body = httpGet(url)""",
        """        val path = "/search/repositories?q=" + URLEncoder.encode(query, "UTF-8") +
            "&sort=stars&order=desc&per_page=$limit"
        val body = request(path)""",
    ),
    (
        '            val json = JSONObject(httpGet("$API/repos/$owner/$name/releases/latest"))',
        '            val json = JSONObject(request("/repos/$owner/$name/releases/latest"))',
    ),
    (
        '            val json = JSONObject(httpGet("$API/repos/$owner/$name/readme"))',
        '            val json = JSONObject(request("/repos/$owner/$name/readme"))',
    ),
    (
        """    private fun httpGet(url: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.setRequestProperty("User-Agent", UA)""",
        """    /**
     * 带线路回退的请求：按「当前线路 → 直连 → 镜像」顺序试，
     * 第一个 200 就返回，并把这条线路记下来给「自动」用。
     */
    private fun request(path: String): String {
        var lastError: Exception? = null
        for (prefix in LinkRouter.apiPrefixes()) {
            val url = if (path.startsWith("http")) path else prefix + API_HOST + path
            try {
                val body = httpGet(url)
                if (LinkRouter.current == LinkRouter.Route.AUTO) LinkRouter.rememberGood(prefix)
                return body
            } catch (e: Exception) {
                lastError = e
            }
        }
        throw lastError ?: IllegalStateException("网络不可用，请到「我的 → 网络线路」换一条线路试试。")
    }

    private fun httpGet(url: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.setRequestProperty("User-Agent", UA)""",
    ),
]

for old, new in pairs:
    assert old in src, "missing block:\n" + old[:120]
    src = src.replace(old, new, 1)

io.open(p, "w", encoding="utf-8", newline="\n").write(src)
print("GitHubApi patched")
