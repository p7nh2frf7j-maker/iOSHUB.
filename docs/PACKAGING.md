# 从零把 iOS HUB 装到 iPhone 上（纯 Windows · 零基础）

> 你不需要 Mac，也不需要懂命令行。全程约 **30–45 分钟**，其中大部分时间是等下载。
> 目标：拿到一个能在手机上点开的 `iOS HUB` App。

---

## 0. 先确认这 4 件事（不然会白忙一场）

| 检查项 | 怎么查 | 不合格怎么办 |
| --- | --- | --- |
| iPhone 系统版本 **≥ iOS 17.0** | 设置 → 通用 → 关于本机 → 软件版本 | 告诉我你的版本号，我把工程改成 iOS 16 / 15（改动很小） |
| 有可用的 **Apple ID 和密码** | 就是你平时登 App Store 的账号 | 免费注册一个即可，不需要开发者账号 |
| Windows 10 / 11 + 能上网 | —— | —— |
| 磁盘留 500MB | —— | —— |

### 关于「打包成 IPA」，你只需要记住三句话

1. **IPA 就是一个压缩包**（里面是 `Payload/iOS HUB.app`）。GitHub 上的 macOS 机器负责编译，产出这个包。
2. **未签名的 IPA 装不进 iPhone**。iOS 只认 Apple 签名的 App，所以最后一步必须用**你自己的 Apple ID** 签一次名（Sideloadly 帮你做，点几下而已）。
3. **免费 Apple ID 签的 App 7 天后失效**。到期用同一个工具点一下「刷新」就行，**App 里的数据不会丢**。

---

## 没有 GitHub 账号怎么办？三条路对比

| 路线 | 要准备什么 | 花钱 | 耗时 | 适合谁 |
| --- | --- | --- | --- | --- |
| **A. GitHub 网页版**（本文主线） | 一个免费 GitHub 账号，不装任何软件 | 0 元 | 10 分钟上传 + 5 分钟编译 | 绝大多数人，推荐 |
| **B. 借一台 Mac** | 朋友/公司/学校任意 Mac + Xcode | 0 元 | 约 15 分钟 | 身边有 Mac 的人 |
| **C. 云 Mac** | MacinCloud / MacStadium，远程桌面 | 约 ¥10–30/小时 | 约 1 小时（含装 Xcode） | 完全不碰账号，愿意付费 |

三条路都绕不开的一件事：**最后必须用你自己的 Apple ID 签名**，因为签名是「这台设备信任某个开发者」的凭证，任何人（包括我）都无法替你完成 —— 这也正是苹果的安全设计。

> 我这边为什么不能直接给你 IPA：我没有 macOS、没有 Xcode、这台机器也连不上外网。SwiftUI 只能在苹果的编译链上编译，这是硬限制，不是配置问题。

---

## 第 1 步：把代码上传到 GitHub（约 10 分钟）

GitHub 是免费的代码托管网站，同时提供免费的 macOS 机器帮我们编译 iOS App。
**一个 GitHub 账号就够，不用装 git，也不用懂命令行。**

### 1.1 注册账号（2 分钟）

1. 打开 <https://github.com/signup>
2. 填邮箱 → 密码 → 用户名（英文，如 `nevercrynow-hub`）→ 完成人机验证
3. 邮箱收 6 位验证码，填进去

### 1.2 方案 A：纯网页上传（不装任何软件，推荐）

1. 打开 <https://github.com/new>：
   - **Repository name**：`iOSHUB`
   - 选 **Private**（私库同样能用免费 Actions）
   - **不要**勾选 "Add a README file"
   - 点 **Create repository**
2. 新页面会有一行 *"uploading an existing file"* —— 点它（或直接打开 `https://github.com/你的用户名/iOSHUB/upload/main`）。
3. 打开本机的项目文件夹（包含 `iOSHUB.xcodeproj` 的那一层），**把下面 4 项一起选中拖进浏览器**：
   - `iOSHUB`（文件夹）
   - `iOSHUB.xcodeproj`（文件夹）
   - `project.yml`
   - `.github`（文件夹，如果拖不进去见 1.3）
4. 等文件列表加载完（约 40 个文件）→ 底部 **Commit changes**。

> 拖文件夹是浏览器原生支持的，会保留目录结构。不要只拖文件，否则工程会散架。

### 1.3 手动补上工作流文件（关键，1 分钟）

Windows 上 `.github` 这种以点开头的文件夹有时拖不进去，**最稳的做法是直接在网页里建**：

1. 打开 `https://github.com/你的用户名/iOSHUB/new/main?filename=.github/workflows/ios.yml`
   （这个链接会把路径自动填好，你只需要粘内容）
2. 打开本机的 `.github/workflows/ios.yml`，**全选复制**，粘贴到网页编辑框
3. 底部 **Commit changes → Commit changes**

### 1.4 方案 B：GitHub Desktop（文件多、想稳妥时用）

1. 到 <https://desktop.github.com> 下载安装，用刚注册的账号登录。
2. **File → Add local repository…** → 选项目文件夹（含 `iOSHUB.xcodeproj` 那一层）。
3. 提示 "not a Git repository" → 点 **create a repository** → Name 填 `iOSHUB` → **Create repository**。
4. 右上角 **Publish repository** → 勾 **Keep this code private** → **Publish repository**。

### 1.5 确认传对了（30 秒）

打开 `https://github.com/你的用户名/iOSHUB`，页面上应该能看到：

- `iOSHUB/`（里面是 App、Core、Features、Models、Resources）
- `iOSHUB.xcodeproj/`
- `project.yml`
- `.github/workflows/ios.yml` ← **没有它就不会编译**

> 少一个都不行。特别是 `.github`，它是「让 GitHub 帮你编译」的开关。

---

## 第 2 步：让 GitHub 编译出 IPA（约 5 分钟）

1. 仓库页面顶部点 **Actions** 标签。
2. 第一次会看到提示，点 **I understand my workflows, go ahead and enable them**。
3. 列表里找 **iOS Build**：
   - 如果刚才的提交已经触发了，会显示一条运行记录，直接点进去；
   - 否则点右侧 **Run workflow** → 分支选 `main` → 绿色 **Run workflow**。
4. 点进这次运行，会看到两个任务：
   - `Build (iOS Simulator, unsigned)` —— 检查代码能不能编译
   - `Package unsigned IPA` —— 产出安装包
   两个都变绿 = 成功（通常 4–8 分钟）。
5. 页面**最底部**找到 **Artifacts**，点 **iOSHUB-unsigned-ipa** 下载。
6. 解压下载的 zip，得到 **`iOSHUB-unsigned.ipa`** ← 这就是要装的东西。

### 如果这里红了

点开红色任务，展开失败的 step，把带 `error:` 的那几行复制给我。常见情况：

| 报错关键词 | 含义 | 处理 |
| --- | --- | --- |
| `cannot be opened because it is in a future Xcode project file format` | 工程文件版本太新 | 工作流已内置 XcodeGen 兜底，重新跑一次即可 |
| `error: package ... requires a minimum platform version` | 依赖版本不匹配（本项目无第三方依赖，一般不会出现） | 发给我 |
| `xcodebuild: error: Unable to find a destination` | 模拟器名称变了 | 发给我，我改工作流 |

> 工作流里有一道保险：如果 `iOSHUB.xcodeproj` 读不动，会自动用 `project.yml`（XcodeGen）现场重新生成一个等价工程再编译。所以**理论上不会因为工程文件问题彻底卡死**。

---

## 第 3 步：用 Sideloadly 装到 iPhone（约 15 分钟）

Sideloadly 是 Windows 上最省事的自签工具，免费。

### 3.1 装驱动（必做，否则电脑认不到手机）

1. 去 <https://www.apple.com/itunes/download/win64> 下载 **iTunes**（微软商店版不行，要用官网 exe）。
2. 安装 → 打开一次 → 数据线连 iPhone → 手机上点 **信任此电脑** → 输入锁屏密码。
3. 也可以在控制面板里确认有 "Apple Mobile Device Support"。

> 如果 iTunes 装不上，可以只装 **Apple Devices**（微软商店有），效果一样。

### 3.2 装 Sideloadly 并签名

1. 打开 <https://sideloadly.io> → 下载 Windows 版 → 安装。
2. iPhone 用数据线连电脑，**解锁并停在桌面**。
3. 打开 Sideloadly：
   - **Device**：下拉选你的 iPhone（如果为空 → 驱动没装好，回 3.1）
   - **IPA**：把 `iOSHUB-unsigned.ipa` 拖进窗口
   - **Apple ID**：填你的 Apple 账号邮箱
4. 点 **Start**：
   - 弹出密码框 → 输入 Apple ID 密码
   - 如果开了双重认证 → 手机会弹 6 位验证码 → 填进去
5. 等进度条走完（1–3 分钟），Sideloadly 日志出现 `Done.` 就成功了。

### 3.3 信任证书（第一次必做，否则打不开闪退）

iPhone 上：**设置 → 通用 → VPN 与设备管理 → 开发者 App → 点你的 Apple ID → 信任**

然后回到桌面，点开 **iOS HUB**。

---

## 第 4 步：7 天后续签（1 分钟）

免费证书 7 天到期后 App 会闪退/打不开，重签即可，**收藏和历史都还在**：

- 重新连电脑，打开 Sideloadly，拖同一个 IPA，点 Start（覆盖安装）。
- 或者用 **AltStore / SideStore**：它们能在手机上自动续签，不用每次连电脑（配置麻烦一些，进阶再考虑）。

### 如果你有 TrollStore（巨魔商店）

把 IPA 传到手机上 → 用 TrollStore 打开 → Install。**永久签名，不会过期**。前提是你的系统在 TrollStore 支持范围内。

---

## 常见问题速查

| 现象 | 原因 | 处理 |
| --- | --- | --- |
| `Unable to install "iOS HUB"` | 签名失败 / 设备未解锁 | 解锁手机、换 USB 口、关掉 VPN 重试 |
| 安装后点开立刻闪退，提示「未受信任的开发者」 | 没做 3.3 的信任 | 设置里信任证书 |
| `The maximum number of apps for free development profiles has been reached` | 免费账号最多同时 3 个自签 App | 用 Sideloadly 删掉一个旧的自签 App |
| 装上了但一打开就白屏 | 系统版本低于 17（App 要求 iOS 17+） | 把系统版本号发我，我下调最低版本 |
| Sideloadly 卡在 `Generating...` 不动 | 驱动/网络问题 | 换线、重启 iTunes 服务、关 VPN |
| GitHub Actions 页面找不到 Artifacts | 任务没跑完或失败 | 等绿了再下载 |

---

## 想要「长期正规」的方案（以后再说）

1. 付费 Apple 开发者账号（$99/年）→ 证书 1 年有效，可 **TestFlight** 分发给朋友。
2. 借一台 Mac 或租云 Mac → Xcode 里 `⌘R` 直接装，不用折腾签名工具。
3. 后端 + 推送（更新提醒）需要服务器，属于另一个话题。

---

## 装好之后建议先做两件事

1. 首页**下拉刷新**，让它去 GitHub 拉真实 Star 数和更新时间。
2. 「我的 → 数据与网络」填一个 GitHub Token（只读权限即可），把每小时 60 次的限制提到 5000 次。

> Token 获取：GitHub → 右上角头像 → Settings → Developer settings → Personal access tokens → Tokens (classic) → Generate new token → **不勾任何权限** → 复制那串 `ghp_...`。
