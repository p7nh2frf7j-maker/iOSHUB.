# 网络线路：为什么有的资源链接打不开

> 一句话：**不是资源没了，是 `github.com` / `api.github.com` 在你这条网络上不通。**
> App 现在会把这些地址自动改写到能通的镜像上，你不需要科学上网，也不需要改系统设置。

---

## 1. 本机实测（2026-10-31，Windows，家宽）

| 地址 | 结果 | 说明 |
| --- | --- | --- |
| `github.com` | `000` / 15s 超时 | 打不开仓库页面 |
| `api.github.com` | `Recv failure: Connection was reset` | 拿不到版本、Star、README |
| `raw.githubusercontent.com` | **200** | 图片和原始文件反而是通的 |
| `gitee.com` | 265 ms | 国内站正常 |
| `gh-proxy.com` | **200**（仓库页 70 KB、raw 200、api 200） | ✅ 唯一稳定可用的镜像 |
| `ghfast.top` | 超时 | ❌ |
| `ghproxy.net` | 超时 | ❌ |
| `hub.gitmirror.com` | 超时 | ❌ |
| `api.kkgithub.com` | 超时 | ❌ |
| `bgithub.xyz` | 超时 | ❌ |
| `github.moeyy.xyz` | 超时 | ❌ |
| `gh.llkk.cc` | 超时 | ❌ |
| `gitclone.com` | 502 | ❌ |
| `ghproxy.cc` | 证书过期（`SEC_E_CERT_EXPIRED`） | ❌ |
| `cdn.jsdelivr.net/gh/owner/repo@branch/path` | 959 ms | 只代理单个文件，不能代理仓库页面 |

结论：镜像不是「随便找一个就行」，**能用的会变**，所以 App 里做了「自动探测 + 可手动锁定 + 可自定义前缀」。

---

## 2. App 里怎么工作的

只改写 **GitHub 系域名**，其它站点（Apple、Reddit、开发者官网）原样直连 —— 不做全局中间人，不改系统代理。

会被改写的域名：

```
github.com  www.github.com  api.github.com
raw.githubusercontent.com  gist.githubusercontent.com
objects.githubusercontent.com  codeload.github.com  avatars.githubusercontent.com
```

改写规则：

| 线路 | 前缀 | 适用 |
| --- | --- | --- |
| 自动（默认） | 启动时探测：直连通就用直连，不通就用 `gh-proxy` | 绝大多数人 |
| 直连 | 空 | 你在能直连 GitHub 的网络里 |
| gh-proxy | `https://gh-proxy.com/` | 页面、文件、API 全代理，最稳 |
| ghfast | `https://ghfast.top/` | 备用 |
| gitmirror | `https://hub.gitmirror.com/` | 备用，偏文件 |
| 自定义 | 你自己填，例如自建 Worker | 想用自己的代理 |

入口：**我的 → 网络线路**。可以手动选，也可以点「测速并选最快线路」，会给每条线路打勾/打叉并显示毫秒数。

### 一个必须知道的细节

镜像对「会 302 跳回 github.com 的地址」一律返回 **403**，典型就是 `/releases/latest`：

```
https://gh-proxy.com/https://github.com/opa334/TrollStore/releases/latest   → 403
https://gh-proxy.com/https://github.com/opa334/TrollStore/releases          → 200
https://gh-proxy.com/https://github.com/opa334/TrollStore/releases/tag/2.0.15 → 403
```

所以 App 在改写前会把这些地址**自动换成 releases 列表页**（`/releases/latest`、`/releases/tag/x.y.z` → `/releases`），你点进去仍然能选到具体版本。这是 App 侧的兜底，不是资源的问题。

---

## 3. 还有哪些方案（按省事程度排序）

1. **用 App 自带的线路切换**（已实现，零成本）。
2. **换 DNS / 换网络**：有些人换个 Wi-Fi 或把 DNS 改成 `223.5.5.5` 就通了，因为干扰是分线路的。
3. **自建反代**（最可控）：Cloudflare Worker / Vercel 写 10 行代码转发 `github.com`，把地址填进「自定义前缀」。适合长期用。
4. **镜像站订阅**：如果以后出现新的公共镜像，直接在「我的 → 网络线路 → 自定义」里填，不用等 App 更新。
5. **本地代理**：手机或电脑上开代理，App 会走系统代理（`URLSession` 默认遵循），不需要改任何设置 —— 但这属于你自己的网络配置，App 不内置、不引导。

> 合规提醒：上面第 5 条要遵守你所在地区与学校/公司的网络规定。App 本身只做「同一份公开资源的地址替换」，不提供、不托管任何受版权保护的内容。

---

## 4. 数据层面的取舍（为什么不内置下载）

- 所有下载按钮都指向**官方发布页**（GitHub Releases、开发者官网），App 不转存 IPA、不镜像安装包。
- 镜像只改写「地址」，不改内容，也不缓存文件。
- 因此镜像失效时最坏情况是「打不开」，而不是「下到被篡改的包」。

---

## 5. 已知限制

- 公共镜像带宽有限，`/archive/*.zip` 这类整包下载可能很慢（实测 15 秒 440 KB）。
- `gh-proxy` 的可用性会变，所以默认线路是「自动」，会实时探测。
- 未认证 GitHub API 是 60 次/小时，走镜像不改变配额；需要更多请在「我的」里填只读 Token。
- iOS 端已按同一套规则实现（`iOSHUB/Core/Networking/LinkRouter.swift`），但没有 Mac 无法编译验证。
