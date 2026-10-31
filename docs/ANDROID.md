# 安卓版打包教程（保姆级 · 零基础）

> 目标：40 分钟内，从「电脑上啥都没有」到「手机桌面上多出 iOS HUB 图标」。
> 不需要 GitHub 账号、不需要签名证书、不需要 Mac、不需要懂命令行。
>
> 每一步都写了 **「你应该看到什么」**，对不上就停下来看第 9 节的报错表。

---

## 0. 最快路径：直接装我编译好的 APK（1 分钟）

这份源码已经用真实的 **JDK 17 + Gradle 8.9 + Android SDK 34** 在 Windows 上完整编译通过（不是「应该能编」）。产物：

| 文件 | 大小 | 用途 |
| --- | --- | --- |
| `dist/iOSHub-1.0-release.apk` | 6.4 MB | **装这个**。正式签名，设置里不会显示「调试版」 |
| `dist/iOSHub-1.0-debug.apk` | 9.3 MB | 备用，调试信息完整 |

安装：
1. 把 APK 传到手机（微信「文件传输助手」/ QQ / 数据线 / 网盘都行）。
2. 手机上点开文件 → 允许「安装未知应用」→ 安装。
3. 桌面出现 iOS HUB 图标，打开即用。

> 支持 Android 8.0（API 26）及以上，包名 `com.nevercrynow.ioshub`。
> **升级规则**：以后重新打包必须用同一份签名（`android/keystore.properties` 指向的 `.tools/ioshub-release.jks`），否则手机会拒绝覆盖安装，只能先卸载——卸载会清掉收藏和浏览记录。

想自己从源码打包（或者改完代码重新出包），看第 14 节：一条命令的本地编译流程。

---

## 0.1 之后还有一条慢路径：整个过程只有 6 个动作

| # | 动作 | 耗时 | 你在哪操作 |
| --- | --- | --- | --- |
| 1 | 下载安装 Android Studio | 25 分钟（大部分是下载） | Windows |
| 2 | 新建一个空项目 | 3 分钟 + 首次同步 5–15 分钟 | Android Studio |
| 3 | 把源码包覆盖进去 | 2 分钟 | 资源管理器 |
| 4 | 加 1 行依赖 + 1 行权限 | 2 分钟 | Android Studio |
| 5 | 连手机点 Run | 3 分钟 | Android Studio + 手机 |
| 6 | 导出 APK（可选，不用线时用） | 2 分钟 | Android Studio |

**你要准备的东西**：Windows 10/11 电脑（15 GB 空闲磁盘、8 GB 内存）、一根能传数据的 USB 线、安卓手机（Android 8.0 以上）、能上网。

---

## 1. 装 Android Studio（25 分钟）

### 1.1 下载

打开 <https://developer.android.com/studio> → 点 **Download Android Studio** → 同意协议 → 下载 Windows 版 `.exe`（约 1.2 GB）。

> 国内下载慢的话，用搜索引擎搜「Android Studio 国内镜像 下载」，或者挂着下载去泡杯茶。

### 1.2 安装

双击安装包 → 一路 **Next**：

- 组件**全部保持勾选**（Android Studio + Android SDK + 模拟器）
- 安装位置**默认就行**（不要放中文路径）
- 最后点 **Install** → 等进度条 → **Finish**

### 1.3 首次启动（关键，别跳过）

1. 打开 Android Studio，如果问 `Import settings` → 选 **Do not import settings**。
2. 出现 **Welcome** 界面 → 直接 **Next** 走完 Setup Wizard。
3. 安装类型选 **Standard**（不是 Customize）→ Next → **Accept** 两个协议 → Finish。
4. 等它下载 SDK 组件（界面上有进度条，**5–15 分钟**，别点取消）。

✅ **成功标志**：看到 Welcome 界面，左侧有 `New Project`、`Open`、`Get from VCS` 三个按钮。

---

## 2. 新建项目（3 分钟）

1. 点 **New Project**。
2. 在模板列表里选 **Empty Activity**（右上角有 Jetpack Compose 图标的那个）→ **Next**。
3. 按下面**一字不差**填：

| 输入框 | 填什么 | 说明 |
| --- | --- | --- |
| **Name** | `iOSHUB` | 桌面图标下显示的名字 |
| **Package name** | `com.nevercrynow.ioshub` | ⚠️ 必须一字不差，写错后面全是红 |
| **Save location** | 默认即可（不要含中文） | 记住这个路径，第 3 步要用 |
| **Language** | `Kotlin` | |
| **Minimum SDK** | `API 26 ("Oreo")` | 覆盖 Android 8.0 以上 |
| Build configuration language | `Kotlin DSL (build.gradle.kts)` | |

4. 点 **Finish**。

✅ **成功标志**：左下角状态栏出现 `Gradle sync finished` / `BUILD SUCCESSFUL`，左边项目树能展开看到 `app → java → com.nevercrynow.ioshub → MainActivity.kt`。

❌ 如果一直转圈或报 `Could not download ...`：看第 9 节第 1、2 条（换阿里云镜像）。

---

## 3. 把源码覆盖进去（2 分钟）

我已经把安卓版源码打成了一个压缩包：**`dist/iOSHub-android-payload.zip`**（在你的项目文件夹里）。

1. 右键这个 zip → **全部解压**（Windows 自带解压就行，解到桌面）。
2. 解压后你会看到 `app` 文件夹 + `开始读我.txt`。
3. 打开第 2 步记下的项目路径，进入项目根目录（能看到 `app`、`build.gradle.kts`、`settings.gradle.kts` 的那一层）。
4. **把解压出来的 `app` 文件夹复制进去**，Windows 会问「是否合并/覆盖」→ 选 **是 / 替换目标中的文件**。
5. 回到 Android Studio，它会自动刷新（没刷新就按 `Ctrl+Alt+Y`，或菜单 **File → Reload All from Disk**）。

✅ **成功标志**：项目树里 `app → java → com.nevercrynow.ioshub` 下面现在有 **`MainActivity.kt`、`data`、`ui`** 三项；`app → assets` 下能看到 `seed-resources.json`。

⚠️ 如果出现 `com.example.ioshub` 这种文件夹，说明第 2 步包名填错了。删掉那个文件夹，重新按第 2 步来。

---

## 4. 改两个文件里的两行（2 分钟）

### 4.1 加依赖

打开 `app/build.gradle.kts`，找到 `dependencies { ... }` 这一段，在**里面**加一行：

```kotlin
    implementation("androidx.compose.material:material-icons-core")
```

加完点右上角出现的 **Sync Now**（或工具栏的大象图标），等同步结束。

✅ **成功标志**：状态栏 `Gradle sync finished`，`app/build.gradle.kts` 没有红色波浪线。

### 4.2 加网络权限

打开 `app/src/main/AndroidManifest.xml`，在 `<application` 这一行**上面**加一行：

```xml
    <uses-permission android:name="android.permission.INTERNET" />
```

不加也能编译，但首页拉不到 GitHub 数据，会一直提示「需要网络」。

### 4.3（可选，30 秒）改桌面显示的名字

打开 `app/src/main/res/values/strings.xml`，把 `app_name` 改成带空格的版本：

```xml
<string name="app_name">iOS HUB</string>
```

这样桌面图标下面显示的是 `iOS HUB` 而不是 `iOSHUB`。

---

## 5. 装到手机（3 分钟）

### 5.1 手机开 USB 调试

设置 → 关于手机 → 连点 **版本号** 7 次 → 提示「你已处于开发者模式」→ 返回设置 → **系统和更新 → 开发者选项**（不同品牌名字略有差异）→ 打开 **USB 调试**。

### 5.2 连线

1. 数据线连电脑。
2. 手机弹窗「允许 USB 调试吗？」→ 勾 **始终允许** → **允许**。
3. Android Studio 顶部中间的下拉框里，应该出现你的手机型号（如 `Pixel 7` / `Mi 13`）。

### 5.3 运行

点顶部绿色 **▶ Run 'app'**（或按 `Shift+F10`）。

✅ **成功标志**：手机上自动出现 iOS HUB 图标并打开，首页显示 41 条资源、分类横排、品牌头。

> 没有手机/不想用线：在 5.2 里选择 `Device Manager → Create Device`，建一个模拟器（多花 10 分钟下载系统镜像），效果一样。

---

## 6. 导出 APK 文件（要发给别人 / 不用线时）

1. 菜单 **Build → Build Bundle(s) / APK(s) → Build APK(s)**。
2. 右下角弹提示 **APK(s) generated successfully** → 点 **locate**（或 `find`）。
3. 打开的位置里就是 **`app-debug.apk`**（在 `app/build/outputs/apk/debug/`）。
4. 把这个文件通过微信/QQ/数据线传到手机 → 点击安装 → 系统提示「未知来源」→ 允许 → 安装完成。

✅ 这个 APK 可以直接装在任何 Android 8.0+ 手机上，**没有 7 天过期，也不用账号**。

---

## 7. 装好之后建议做三件事

1. 首页点一次 **「从 GitHub 获取」**（或「我的 → 立即刷新远端数据」），让它拉真实 Star 数和更新时间。
2. 看到「GitHub 请求超出频率限制」是正常的：未认证接口每小时 60 次。等一会儿再试即可，不影响浏览内置数据。
3. 如果点资源链接打不开（`github.com` 在国内常被干扰），去 **我的 → 网络线路** 点一次「测速并选最快线路」。原理和实测数据见 [LINKS.md](LINKS.md)。

---

## 8. 记不住的话，只看这 6 句

```
1. 装 Android Studio（Standard 模式）
2. New Project → Empty Activity → 包名 com.nevercrynow.ioshub → API 26
3. 把 iOSHub-android-payload.zip 解压出的 app 文件夹覆盖到项目根目录
4. build.gradle.kts 加 material-icons-core 一行 → Sync
5. Manifest 加 INTERNET 权限一行
6. 连手机 → ▶ Run；要 APK 就 Build → Build APK(s) → app-debug.apk
```

---

## 9. 报错对照表（按出现频率排序）

| 报错 / 现象 | 原因 | 解决 |
| --- | --- | --- |
| `Could not resolve all dependencies` / 一直 `Downloading ...` | 拉不到 Google/Maven 仓库 | 打开 `settings.gradle.kts`，把两个 `repositories { }` 块里的内容换成下面「镜像配置」 |
| `Gradle sync failed: Unsupported Java` / JDK 版本报错 | Gradle JDK 不匹配 | **File → Settings → Build, Execution, Deployment → Build Tools → Gradle → Gradle JDK** 选 `jbr-17`（或 21），Apply 后重新 Sync |
| `Unresolved reference: Brand` / `Category` 一片红 | 源码没放对位置 | 确认文件在 `app/src/main/java/com/nevercrynow/ioshub/` 下，`data/`、`ui/` 是它的子目录 |
| `Class 'MainActivity' is defined multiple times` | 旧模板的 MainActivity 没被覆盖 | 删掉 `com/example/...` 或重复的那个 `MainActivity.kt` |
| `Unresolved reference: material.icons` | 第 4.1 步没做或没 Sync | 加依赖 → Sync Now |
| 编译通过但 App 打开就闪退 | 少了 `INTERNET` 权限 / assets 没覆盖 | 检查第 4.2 步和 `app/src/main/assets/seed-resources.json` |
| 首页有数据、但 GitHub 区块一直空 | 没联网 / 被限流 | 正常现象，点「从 GitHub 获取」重试 |
| `SDK location not found` | 缺 `local.properties` | 让 Android Studio 重新 Sync 一次会自动生成；仍报错就新建 `local.properties` 写 `sdk.dir=C\:\\Users\\你的用户名\\AppData\\Local\\Android\\Sdk` |
| `INSTALL_FAILED_USER_RESTRICTED` / 手机上装不上 | 小米/OPPO 等需要额外开关 | 开发者选项里打开「USB 安装」「安装未知来源应用」 |
| `Device is unauthorized` | 手机没点允许调试 | 拔线重插，重新允许；或 `开发者选项 → 撤销 USB 调试授权` 后再插 |
| AS 卡在 `Gradle: Download gradle-8.x-bin.zip` | 首次下载 Gradle 本体 | 等（可能 10 分钟）；或设置代理/镜像 |
| 中文全是问号/乱码 | 文件编码 | Android Studio 右下角确认 `UTF-8`；用我们的 zip 正常不会有这个问题 |

### 镜像配置（第 1 条用）

打开 `settings.gradle.kts`，把**两个** `repositories { ... }` 都改成：

```kotlin
repositories {
    maven { url = uri("https://maven.aliyun.com/repository/google") }
    maven { url = uri("https://maven.aliyun.com/repository/central") }
    maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
    google()
    mavenCentral()
}
```

改完点 **Sync Now**。

---

## 10. 卡住时的自查 5 问

1. **包名是不是 `com.nevercrynow.ioshub`？**（最容易错，错了全是红）
2. **`app/src/main/java/com/nevercrynow/ioshub/` 下有没有 `MainActivity.kt`、`data/`、`ui/`？**
3. **`app/build.gradle.kts` 里那行 `material-icons-core` 加了并且 Sync 过了吗？**
4. **`app/src/main/assets/seed-resources.json` 在不在？**
5. **手机 USB 调试开了、电脑上弹窗点允许了吗？**

还是不行，把 Android Studio 底部 **Build** 面板里带 `error:` 的那几行（或整段红字）复制给我，我直接改。

---

## 11. 进阶：直接打开 `android/` 目录（不推荐新手）

仓库里的 `android/` 是完整工程（含 Gradle 配置）。理论上 **File → Open** 选这个目录就能跑，但它**没有附带 `gradle-wrapper.jar`**（那是二进制文件）。如果 Android Studio 报 `Could not find gradle-wrapper.jar`：

- 用本教程的方式 A（新建项目再覆盖），或者
- 在任意已配置 Gradle 的环境执行一次 `gradle wrapper --gradle-version 8.9` 生成 wrapper。

---


---

## 12. 本次视觉打磨清单（v1.0 美化版）

如果你之前已经覆盖过源码，**重新覆盖一次**即可拿到这些升级（旧文件会被替换）。

| 改动 | 之前 | 现在 |
| --- | --- | --- |
| 卡片质感 | 纯色块 + 默认阴影 | 18dp 圆角 + 1dp 细边框 + 柔和投影，深色模式下靠边框区分层级 |
| 按下反馈 | 默认涟漪 | 卡片按压缩到 97.5%（弹簧回弹），精选大卡无涟漪、只缩放 |
| 首屏加载 | 白屏 → 内容突然出现 | 呼吸骨架屏（品牌头 / 搜索框 / 大卡 / 分类 / 列表共 6 块） |
| 入场动画 | 无 | 区块依次淡入上浮，错开 45ms，最多 8 组 |
| 头部 | 纯文字 | Logo 背后一层品牌柔光，铃铛按钮加 1dp 描边 |
| 精选大卡 | 单色渐变 | 斜向高光 + 品牌色投影 + 玻璃态徽章 + 1dp 白色描边 |
| 区块标题 | 纯文字 | 左侧 3dp 品牌色竖条 + 副标题缩进对齐 |
| 详情页 | 统一白卡 | 按分类着色：外投影、1dp 描边、左上角渐变底，每个分类看起来不一样 |
| 主行动按钮 | 在内容里滚动 | **固定在底部**的渐变主按钮（不滚动，随时能点） |
| 收藏按钮 | 瞬间变色 | 弹性放大到 1.15 倍再回落 |
| 底部导航 | 默认样式 | 品牌色选中态 + 12% 透明胶囊指示器 + 20dp 图标 |
| 深色模式 | 灰黑 | 更深的页面底（#0B0B10）+ 稍亮的卡片（#16161C），层级更清楚 |

技术上没加任何新依赖：动画全部用 Compose 自带的 `animateFloatAsState` / `rememberInfiniteTransition`，手写组件在 `ui/Polish.kt`（按下反馈、骨架、入场、光晕）。

**v1.2 新增（本轮：启动动画 / 图标重绘 / 品牌标识）**：

| 改动 | 之前 | 现在 |
| --- | --- | --- |
| App 图标 | 三条等宽胶囊 | **阶梯状三层叠片**（上窄→中宽→下宽，向右下错位），5 个密度 mipmap + iOS 1024 图标同一套几何 |
| 启动动画 | 瞬间进首页 | **1.55 秒五阶段**：光晕 → 图标弹入 → 叠片逐条从左绘出 → 字样上浮 → 进度条 + `N E V E R C R Y N O W`，最后放大淡出 |
| 界面图标 | 23 处 emoji（🔍 ⚙ ⚠️ 📰 🎲 🛡 …） | 全部手绘矢量字形（`ui/Glyphs.kt`，24×24 坐标格 + 圆头描边），跨设备渲染一致 |
| 分类图标 | 13 个 emoji | 渐变方块 + 白色字母字标（JB / TS / IP / SD / TW / RP / OS / SC / DX / TH / UT / PT / NW） |
| 品牌标识 | 首页 + 我的 + 详情页脚 | 再加：顶栏右侧 `by nevercrynow` 水印、我的页整块渐变品牌卡、分类页 / 搜索页 / 发现页 / 资讯页底部落款 |

> 预渲染效果见 [docs/mockup-launch-frames.png](mockup-launch-frames.png)（4 帧节奏图）与 [docs/app-icon-1024.png](app-icon-1024.png)。

**v1.5 新增（本轮：液态玻璃 + 献词）**：

| 改动 | 之前 | 现在 |
| --- | --- | --- |
| 页面底色 | 纯色 `#F2F2F7` / `#0B0B10` | 四团高斯模糊品牌光斑（蓝 / 紫 / 薄荷 / 玫瑰）铺底，`Modifier.glassPage()` 一处替换 15 个页面 |
| 卡片 | 白底 + 描边 | `GlassSurface`：18dp 彩色投影 + 白 86%→52% 垂直渐变 + 1dp 渐变高光描边（深色 16%→5%） |
| 顶栏 | 实心 surface | `GlassTopBar` 磨砂条 + 底部 1px 高光，滚动时内容从玻璃下穿过 |
| 底部导航 | 贴底 NavigationBar | **悬浮玻璃胶囊**（26dp 圆角、24dp 投影），图标 + 9.5sp 文字 + 选中圆点，选中项 1.06 倍缩放 |
| 搜索框 | 44dp 实心 | 46dp `glassCapsule(corner = 15.dp)`，只画背景 / 描边 / 投影 |
| 启动动画 | 1.55 秒五阶段 | 追加**献词**：0.88s 后「献给 赵菁然」淡入，15sp 玫瑰色粗体 + 手绘心形弹簧放大，停留到 1.56s 才淡出 |

> 视觉参考：[docs/mockup-home.png](mockup-home.png)（首页 + 光斑底）、[docs/mockup-launch-frames.png](mockup-launch-frames.png)（5 帧，含献词）。

## 13. 这一版有什么 / 还没有什么
**有**：首页（精选 / 13 分类 / 最新收录 / 最近更新 / GitHub 热门 / 本周热门 / 数据说明 / 品牌落款）、分类列表、分类内搜索、全局搜索、详情（规格 / 风险提示 / 来源 / 教程）、收藏（本地保存）、我的（统计 / 刷新 / 免责声明）、GitHub 实时 Star、深色模式、启动图标。

**v1.1 新增（本轮）**：

| 功能 | 说明 |
| --- | --- |
| 资讯 RSS | 首页显示最新 3 条 + 独立「资讯」页，聚合 Apple Developer / Apple Newsroom / GitHub Blog / r/jailbreak，解析失败只提示不造假 |
| 发现（GitHub 搜索） | 输入关键词 400ms 防抖后搜索仓库，按 Star 排序，点一条直接在浏览器打开原仓库 |
| 详情页 README | 自研轻量 Markdown 渲染（标题 / 列表 / 代码 / 引用），最多 2 万字符并提示截断 |
| 最新 Release | 详情页显示 tag、发布时间、Release 说明（同样走 Markdown 渲染） |
| 搜索历史 | 最近 8 条，点一下回填，可一键清空 |
| 最近浏览 | 最近 10 条，出现在收藏页底部 |
| 复制链接 | 详情页顶栏「复制」，直接复制原始来源 URL 并弹 Toast |
| 手气不错 | 首页一颗骰子，随机打开一个资源 |

**还没有**：版本更新提醒（需要服务端定时比对 + APNs/FCM，属独立工程）。

> 这些在 iOS 版已达，安卓版是同一套产品逻辑的第二实现，数据文件共用，补起来很快。

---

## 14. 本机真实编译记录（不装 Android Studio 也能出包）

这一版是**实际编译通过**才交付的，下面是可复现的完整流程。全程只需要命令行的 JDK + Gradle + Android SDK，不需要 Android Studio。

### 14.1 工具链版本（已验证组合）

| 组件 | 版本 | 放在哪 |
| --- | --- | --- |
| JDK | Temurin 17.0.20.1 | `.tools/jdk-17.0.20.1+1` |
| Gradle | 8.9 | `.tools/gradle-8.9` |
| Android SDK | cmdline-tools 11076708 + platforms;android-34 + build-tools;34.0.0 | `.tools/android-sdk` |
| AGP / Kotlin | 8.5.2 / 2.0.20 | 由 `android/build.gradle.kts` 决定 |

> `.tools/` 是本地临时目录，不进版本库（已被 `.gitignore` 忽略）。换机器时重新下载即可，命令见 14.3。

### 14.2 一条命令打包

在仓库根目录（有 `android/` 的那一层）打开 PowerShell：

```powershell
$env:JAVA_HOME   = "$PWD\.tools\jdk-17.0.20.1+1"
$env:ANDROID_HOME = "$PWD\.tools\android-sdk"
$env:ANDROID_SDK_ROOT = $env:ANDROID_HOME
$env:GRADLE_USER_HOME = "$PWD\.tools\gradle-home"   # 关键：缓存别放用户目录
$env:TEMP = "$PWD\.tools\tmp"; $env:TMP = $env:TEMP # 关键：某些环境系统 temp 不可写
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

& .\tools\gradle-8.9\bin\gradle.bat -p android --no-daemon --console=plain assembleRelease
# 产物：android/app/build/outputs/apk/release/app-release.apk
```

装到手机（手机开 USB 调试）：

```powershell
& .\.tools\android-sdk\platform-tools\adb.exe install -r .\dist\iOSHub-1.0-release.apk
```

### 14.3 从零准备工具链（换机器时）

```powershell
# JDK 17（清华镜像，比 aka.ms 快很多）
Invoke-WebRequest "https://mirrors.tuna.tsinghua.edu.cn/Adoptium/17/jdk/x64/windows/OpenJDK17U-jdk_x64_windows_hotspot_17.0.20.1_1.zip" -OutFile jdk.zip
Expand-Archive jdk.zip -DestinationPath .tools

# Gradle 8.9（腾讯镜像；services.gradle.org 国内经常超时）
Invoke-WebRequest "https://mirrors.cloud.tencent.com/gradle/gradle-8.9-bin.zip" -OutFile gradle.zip
Expand-Archive gradle.zip -DestinationPath .tools

# Android cmdline-tools + 组件
Invoke-WebRequest "https://dl.google.com/android/repository/commandlinetools-win-11076708_latest.zip" -OutFile clt.zip
Expand-Archive clt.zip -DestinationPath .tools\clt
New-Item -ItemType Directory -Force .tools\android-sdk\cmdline-tools | Out-Null
Move-Item .tools\clt\cmdline-tools .tools\android-sdk\cmdline-tools\latest
$env:JAVA_HOME = "$PWD\.tools\jdk-17.0.20.1+1"
& .tools\android-sdk\cmdline-tools\latest\bin\sdkmanager.bat --sdk_root="$PWD\.tools\android-sdk" `
    "platform-tools" "platforms;android-34" "build-tools;34.0.0"
```

### 14.4 这次踩到的 6 个坑（都已修好，记录备查）

| 现象 | 原因 | 修法 |
| --- | --- | --- |
| `Failed to load native library 'native-platform.dll'` | Gradle 往 `~/.gradle` 解压原生库，被环境挡住 | 把 `GRADLE_USER_HOME` 指到工程内 `.tools/gradle-home` |
| 依赖下载卡 17 分钟无进度 | Maven Central / google() 国内链路抖动 | `settings.gradle.kts` 已把阿里云镜像放最前；再加 HTTP 超时兜底 |
| `AccessDeniedException: ...\Temp\kotlin-compiler-*.alive` | 系统 temp 目录不可写 | `gradle.properties` 里 `kotlin.compiler.execution.strategy=in-process` + 工程内 `TEMP` |
| `Unresolved reference 'Base64'` | 少了 import | `import android.util.Base64` |
| `Unresolved reference 'onOpenDetail'` | Header 组合函数少传两个回调 | 补 `onOpenDetail` / `onOpenNews` 参数并传参 |
| `Unresolved reference: util`（build.gradle.kts） | Kotlin DSL 里 `java` 是插件扩展，不是包名 | 文件头 `import java.util.Properties`，改用 `Properties()` |

### 14.5 签名说明

`dist/iOSHub-1.0-release.apk` 用的是本机自签名（`CN=iOS HUB, OU=nevercrynow`），SHA-256 指纹 `9f18d240…fad79b870`。

- 自己用、发给朋友装，完全够用。
- 上 Google Play 要用你自己的签名 + AAB 格式，那是另一套流程，和这份自签不冲突。
- 想换成自己的签名：删掉 `android/keystore.properties`，用 `keytool` 生成新的 jks，再把四项写回该文件即可（`app/build.gradle.kts` 检测到文件才启用）。

---

## 15. 网络线路（v1.3）：链接打不开怎么办

`github.com` / `api.github.com` 在国内经常连不上，而 `raw.githubusercontent.com` 反而是通的 —— 表现就是「App 能显示资源，但点进去 404 / 白屏 / 一直转圈」。

App 内置了线路改写，只动 GitHub 系域名，其它站点原样直连：

| 线路 | 前缀 | 实测 |
| --- | --- | --- |
| 自动（默认） | 启动探测一次，直连不通就用 gh-proxy | 推荐 |
| 直连 | — | 你的网络能直连时使用 |
| gh-proxy | `https://gh-proxy.com/` | ✅ 本机唯一稳定可用（页面/文件/API 全代理） |
| ghfast | `https://ghfast.top/` | 备用，本机超时 |
| gitmirror | `https://hub.gitmirror.com/` | 备用，本机超时 |
| 自定义 | 你自己的反代前缀 | 自建 Worker 时用 |

入口：**我的 → 网络线路**，可以手动选，也可以点「测速并选最快线路」自动挑。

两个细节：

- 镜像对 `/releases/latest`、`/releases/tag/x.y.z` 这类会 302 跳回 github.com 的地址返回 403，App 会自动改成 `/releases` 列表页。
- 只改地址、不缓存文件、不转存安装包，所以镜像失效最坏是「打不开」，不会「下到被篡改的包」。

完整原理与实测表见 [LINKS.md](LINKS.md)。
