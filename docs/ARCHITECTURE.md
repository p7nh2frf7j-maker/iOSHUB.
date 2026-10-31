# iOS HUB · 技术架构

> 版本：Phase 1–2 已交付（可编译的完整源码），Phase 3–5 见文末路线。
> 工程：`iOSHUB.xcodeproj`（Xcode 16+，iOS 17.0+，SwiftUI + SwiftData）

---

## 1. 定位与技术选型

一款把整个 iOS 生态（越狱 / 巨魔 / 侧载 / 插件 / 开源 / 漏洞 / 开发 / 美化 / 资讯）聚合到一处的原生 App。
核心矛盾是 **「信息聚合」与「真实性」** —— 因此架构上把「数据来源」当作一等公民：每条资源都带 `sourceURL`、`lastVerified`、`compatibility`、`risk`，未核对的字段一律显示「待验证」。

| 层 | 选型 | 理由 |
| --- | --- | --- |
| UI | SwiftUI（iOS 17+） | 原生动效、深色模式、Dynamic Type 零成本；无 WebView 套壳 |
| 状态 | `@Observable` + `@MainActor` | Observation 框架比 ObservableObject 少一轮视图刷新 |
| 架构 | MVVM（View / Store / Service） | Store 只做状态与派生数据，网络与持久化在 Service |
| 持久化 | SwiftData | 收藏 / 历史 / 搜索只存用户数据，模型轻 |
| 网络 | URLSession + async/await | 无第三方依赖，包体最小 |
| 外部数据 | GitHub REST v3 + RSS/Atom | 公开、无需服务端即可跑通 |
| 后端 | 无（Phase 5 可选） | 当前是「客户端直连 + 本地兜底」，不引入运维成本 |

依赖为零：整个工程只用系统框架（SwiftUI / SwiftData / Foundation / UIKit 的颜色适配）。

---

## 2. 分层

```
┌───────────────────────────────────────────────┐
│ View 层     HomeView / ResourcesView / ...      │  只读 Store，不写网络代码
├───────────────────────────────────────────────┤
│ Store 层    CatalogStore (@Observable)          │  目录、派生列表、刷新编排
├───────────────────────────────────────────────┤
│ Service 层  GitHubAPI · NewsLoader ·            │  无状态、可单元测试
│             LibraryService(SwiftData)           │
├───────────────────────────────────────────────┤
│ Model 层    ResourceItem · NewsItem · GitHubRepo│  纯 Codable 值类型
├───────────────────────────────────────────────┤
│ Infra       HTTPClient · JSONCoding · SeedLoader│  错误语义化、格式统一
└───────────────────────────────────────────────┘
```

三条硬规则：

1. **View 不解析 JSON、不认识 URL**。要数据就调 Store，要收藏就调 LibraryService。
2. **网络永远是增强**。断网时 App 必须完整可用：种子数据 → 缓存统计 → 远端增量。
3. **不猜数据**。拿不到日期就是 `nil`，UI 显示「待验证」，不用 `Date()` 冒充。

---

## 3. 目录结构

```
iOSHUB.xcodeproj/
├─ project.pbxproj                    # Xcode 16 同步文件夹工程（加文件无需改工程）
└─ xcshareddata/xcschemes/iOSHUB.xcscheme

iOSHUB/
├─ App/
│  ├─ iOSHUBApp.swift                 # 入口：装配 ModelContainer + CatalogStore
│  └─ RootTabView.swift               # 五 Tab + AppRoute 路由枚举
├─ Models/
│  ├─ ResourceCategory.swift          # 13 分类（标题/图标/关键词/是否需风险提示）
│  ├─ ResourceItem.swift              # 统一资源模型 + SecurityRisk + CompatibilityState
│  └─ NewsItem.swift                  # 资讯模型 + 5 个可信信息源定义
├─ Core/
│  ├─ Design/
│  │  ├─ Theme.swift                  # 颜色/字号/圆角/间距 token + 分类配色
│  │  └─ Components.swift             # 卡片、胶囊、区块头、搜索入口、空/错态
│  ├─ Data/
│  │  └─ CatalogStore.swift           # 唯一数据源 + 排序 + 搜索 + 刷新编排 + 统计缓存
│  ├─ Networking/
│  │  ├─ HTTPClient.swift             # 请求 + 状态码归一化 + APIError
│  │  ├─ GitHubAPI.swift              # 搜索/仓库/Release/README + 模型转换
│  │  └─ NewsFeedLoader.swift         # RSS/Atom 解析 + 多源并发聚合
│  ├─ Persistence/
│  │  └─ Persistence.swift            # SwiftData 模型 + 容器 + LibraryService
│  └─ Support/
│     ├─ JSONCoding.swift             # 统一日期策略（ISO8601 / yyyy-MM-dd / RFC822）
│     ├─ Formatting.swift             # 相对时间、数字缩写（1.2万）
│     └─ SeedLoader.swift             # 内置 JSON 读取 + AppSettings 键
├─ Features/
│  ├─ Home/HomeView.swift             # 首页 Dashboard（9 个分区）
│  ├─ Detail/ResourceDetailView.swift # 详情：规格/风险/教程/来源/核对状态
│  ├─ Detail/MarkdownRenderer.swift   # 自研 Markdown 渲染（标题/列表/代码/引用）
│  ├─ Detail/ResourceDetailModel.swift# 详情页远端增量：统计 / Release / README
│  ├─ Resources/ResourcesView.swift   # 分类入口列表
│  ├─ Resources/CategoryDetailView.swift
│  ├─ Search/SearchView.swift         # 全字段搜索 + 范围筛选 + 搜索历史
│  ├─ Discover/DiscoverView.swift     # GitHub 实时检索 + 资讯
│  ├─ News/NewsFeedView.swift         # 资讯列表 + 信息源清单
│  ├─ Favorites/FavoritesView.swift   # 收藏 / 浏览历史（可滑动删除）
│  ├─ Profile/ProfileView.swift       # 设备、Token、通知、免责声明
│  ├─ Profile/FollowedCategoriesView.swift
│  └─ Shared/
│     ├─ ResourceViews.swift          # 大卡/横滑卡/行/砖/分类/仓库/资讯 7 种形态
│     ├─ BrandViews.swift             # 品牌：Logo / 署名 / 页脚 / 启动过渡页
│     └─ AppRouting.swift             # 一次注册所有 navigationDestination
└─ Resources/
   ├─ seed-resources.json             # 41 条内置资源（官方来源，未核对字段留空）
   └─ seed-news.json                  # 空数组：不内置任何新闻
```

工程使用 Xcode 16 的 **文件系统同步分组**（`PBXFileSystemSynchronizedRootGroup`）：
`iOSHUB/` 目录下新增 `.swift` 自动参与编译、`.json` 自动打包，**不需要手改 pbxproj**。

---

## 4. 数据模型

### 4.1 ResourceItem（核心）

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | `String` | 稳定标识，如 `trollstore` / `github.opa334/dopamine` |
| `name` / `summary` / `detail` | `String` | 名称、一句话简介、详细说明 |
| `category` | `ResourceCategory` | 13 分类枚举 |
| `symbol` / `iconURL` | `String` / `URL?` | 无图标时用 SF Symbols 兜底 |
| `version` | `String?` | **nil 即未核对**，UI 显示「待验证」 |
| `developer` / `developerURL` | `String` / `URL?` | 开发者与主页 |
| `githubURL` / `downloadURL` / `sourceURL` / `tutorialURL` | `URL?` / `URL` | 四类外链，`sourceURL` 必填 |
| `supportedIOS` / `supportedDevices` | `[String]` | 支持范围原文，不自动推断 |
| `releaseDate` / `lastUpdated` / `lastVerified` | `Date?` | 发布日期、最近更新、**人工/自动核对时间** |
| `compatibility` | `CompatibilityState` | `verified` / `partial` / `pending` / `unsupported` |
| `verificationNote` | `String?` | 兼容性脚注（「以官方 README 为准」） |
| `risk` | `SecurityRisk` | `official` / `community` / `caution` / `high` |
| `tags` / `screenshots` / `changelog` | `[String]` / `[URL]` / `String?` | 检索词、截图、更新日志 |
| `isFeatured` | `Bool` | 首页「今日精选」 |
| `stars` / `forks` / `language` / `latestRelease` | 运行期增量 | 由 GitHub API 填充，不写种子数据 |

派生属性（避免 View 里散落格式化逻辑）：`versionText`、`updatedText`、`verifiedText`、`searchText`、`primaryActionURL`。

### 4.2 枚举

- `ResourceCategory`：jailbreak / trollstore / ipa / sideload / tweak / repo / openSource / security / devTools / theming / utilities / port / news，每个带 `title`、`shortTitle`、`blurb`、`symbol`、`githubKeywords`、`needsRiskNotice`。
- `SecurityRisk`：`severity` 0–3，用于排序与是否展示风险横幅。
- `CompatibilityState`：颜色 + 图标，`pending` 是默认值。

### 4.3 NewsItem / NewsFeed

`NewsItem` 保存标题、来源、摘要、原文链接、发布时间。`NewsFeed` 定义 5 个信息源（Apple Developer、Apple Newsroom、Apple 安全更新、GitHub Blog、r/jailbreak），每个都带 RSS 地址、站点地址、一句话可信度说明。

### 4.4 SwiftData（只存用户数据）

| 模型 | 唯一键 | 内容 |
| --- | --- | --- |
| `FavoriteRecord` | `resourceID` | 完整 `ResourceItem` 快照（Data），远端下线也不丢 |
| `HistoryRecord` | `resourceID` | 最近浏览时间 + 快照 |
| `SearchQueryRecord` | `text` | 搜索词、最近使用、命中次数 |

---

## 5. 数据流

```
启动
 └─ CatalogStore.loadIfNeeded()
     ├─ SeedLoader.resources()        → 41 条内置资源（必成功）
     ├─ StatsStore.load()             → 24h 内的 GitHub 统计缓存（可失败）
     └─ phase = .ready                → 首页立即可用

下拉刷新 / 按钮
 └─ CatalogStore.refreshFromNetwork()
     ├─ refreshTrending()  GitHub 搜索 topic:ios，Star 800+，近一年推送
     ├─ refreshStats()     最多 15 次单仓库请求，补 stars/forks/pushed_at
     └─ refreshNews()      Apple Developer + GitHub Blog RSS，多源并发
         失败 → remoteNotice / newsNotice，UI 顶部黄色提示条，不阻断阅读
```

合并规则：**远端字段覆盖本地同名字段，本地字段永不凭空生成**。刷新失败不清空已有数据。

搜索：`searchText` 预拼接（名称 + 开发者 + 简介 + 分类 + 标签 + 支持版本）→ 全 `tokens.allSatisfy(contains)` 匹配 → 名称命中优先，其次 Star 数。

---

## 6. 真实性与安全策略（本项目的硬约束）

1. 种子数据只收录**能指明官方仓库 / 开发者主页**的条目；所有条目 `sourceURL` 必填且可点击核对。
2. 不伪造版本号：`version` 为空就显示「待验证」并标黄。
3. 不伪造日期：`lastUpdated` 只来自 GitHub `pushed_at`，没有就显示「待验证」。
4. 不虚构新闻：`seed-news.json` 是空数组，资讯只在联网时从官方 RSS 拉取，断网时明确说「需要网络」。
5. 不托管安装包：所有下载按钮指向 `downloadURL`（官方 Release / 官网），App 内无下载器。
6. 风险分级：越狱、巨魔、未签名 IPA、漏洞利用工具默认 `caution`/`high`，详情页强制展示风险横幅与「非主力机 + 先备份」建议。
7. 未授权 IPA：`IPA 应用` 分类只收录**工具**（ipatool、侧载器），不收录盗版应用商店链接。

---

## 7. API 与配额

| 接口 | 地址 | 配额 |
| --- | --- | --- |
| 搜索仓库 | `GET /search/repositories` | 未认证 10 次/分钟 |
| 单仓库 | `GET /repos/{o}/{r}` | 未认证 60 次/小时 |
| 最新 Release | `GET /repos/{o}/{r}/releases/latest` | 同上 |
| README | `GET /repos/{o}/{r}/readme` | 同上 |

- `remoteNotice` 会把 403/429 翻译成人话，并提示去「我的 → 数据与网络」填 Token。
- `refreshStats()` 单次最多 15 个仓库，命中限流立刻 `break`。
- 统计结果写入 `Caches/github-stats.json`，24h 内复用，不重复烧配额。

---

## 8. 阶段与完成度

| 阶段 | 内容 | 状态 |
| --- | --- | --- |
| P1 | 架构、目录、数据模型、导航、设计系统 | ✅ 已完成 |
| P2 | 首页、分类、详情、搜索、收藏、个人中心 | ✅ 已完成 |
| P3 | GitHub API 真实数据、趋势榜、仓库详情 | ✅ 已完成（详情页含 README 渲染与最新 Release） |
| P4 | 资讯聚合、更新监控、推送接口 | 🟡 RSS 聚合已完成；APNs + 后端比对未做 |
| P5 | 动效打磨、图片缓存（Kingfisher）、性能与错误埋点 | 🟡 基础版已完成；图片缓存与埋点未做 |

### 已知限制（不隐瞒）

- **没有后端**：更新提醒目前只是本地开关，真正的「某工具发新版本 → 推送」需要服务端定时比对 + APNs，属于 P4。
- **图标**：`Assets.xcassets/AppIcon.appiconset/AppIcon-1024.png` 已生成（单尺寸 1024，Xcode 15+ 自动降采样）；资源图标用 `AsyncImage`，无磁盘缓存（P5 换 Kingfisher）。
- **兼容性无法自动判定**：GitHub API 不提供「支持哪些 iOS 版本」的结构化字段，因此兼容性只能靠人工核对，绝不由程序猜。
- **种子数据的版本号与支持范围**：代码中一律标记为 `partial`/`pending` 并附「以官方为准」的脚注，因为离线环境无法逐条核对。
