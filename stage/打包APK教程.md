# Android Studio 本地部署 + APK 打包教程

> 本机实际安装记录（2026-10-31 完成）。所有路径、版本号都是这台机器上的真实值，可以直接照抄命令。

---

## 一、装好了什么

| 组件 | 版本 | 位置 |
|---|---|---|
| Android Studio | 2025.2.1.7（build AI-252.25557.131.2521.14344949） | `D:\Android\android-studio` |
| 内置 JBR（IDE 自用） | 21.0.8 | `D:\Android\android-studio\jbr` |
| JDK（编译用） | Temurin 21.0.12.1 LTS | `D:\Android\jdk` |
| Gradle | 8.14.3 | `D:\Android\gradle-8.14.3` |
| Android SDK | platform-tools / android-36 / build-tools 36.0.0 | `D:\Android\Sdk` |
| Gradle 缓存 | — | `D:\Android\gradle-home` |
| IDE 配置/索引/插件/日志 | — | `D:\Android\studio`（config / cache / plugins / log） |
| 签名密钥库 | PKCS12，RSA 2048，有效期 10000 天 | `D:\Android\Keystore\helloapk.jks` |
| 示例工程 | AGP 8.13.0 / compileSdk 36 / minSdk 24 | `D:\Android\Projects\HelloApk` |

桌面上已生成两个快捷方式：**Android Studio**、**HelloApk 工程**。

**为什么全放 D 盘**：你这台机器 C 盘只剩 9.7 GB。SDK + Gradle 缓存 + 工程构建产物轻松吃掉 3～5 GB，所以 IDE、JDK、SDK、Gradle 缓存、工程、keystore 全都落在了 D 盘（可用 58.8 GB）。

IDE 的索引缓存默认写在 `C:\Users\33041\AppData\Local\Google\AndroidStudio*`，工程一大就会涨到几个 G。
已通过修改 `D:\Android\android-studio\bin\idea.properties` 末尾 4 行把它整体挪到 `D:\Android\studio`：

```properties
idea.config.path=D:/Android/studio/config
idea.system.path=D:/Android/studio/cache
idea.plugins.path=D:/Android/studio/plugins
idea.log.path=D:/Android/studio/log
```

想改回默认行为，把这 4 行注释掉即可。

---

## 二、环境变量（已写入用户变量，永久生效）

| 变量 | 值 |
|---|---|
| `JAVA_HOME` | `D:\Android\jdk` |
| `ANDROID_HOME` | `D:\Android\Sdk` |
| `ANDROID_SDK_ROOT` | `D:\Android\Sdk` |
| `GRADLE_USER_HOME` | `D:\Android\gradle-home` |

`PATH` 里追加了：`jdk\bin`、`platform-tools`、`cmdline-tools\latest\bin`、`build-tools\36.0.0`、`gradle-8.14.3\bin`、`android-studio\bin`。

> ⚠️ **已经打开的终端 / CMD / PowerShell 读不到新变量**，必须关掉重开。验证：
>
> ```cmd
> java -version
> adb version
> gradle -v
> ```
>
> 三条都能出版本号就说明环境通了。

---

## 三、第一次打开 Android Studio

1. 双击桌面 **Android Studio**（首次启动要 1～2 分钟，正常）。
2. 弹出 **Import Android Studio Settings** → 选 **Do not import settings**（全新安装没东西可导）。
3. 出现 Setup Wizard（下载 SDK 的向导）：
   - 选 **Custom**，不要选 Standard（Standard 会往 C 盘塞几个 G）。
   - 到 **SDK Components** 这一步，把 SDK 路径改成 `D:\Android\Sdk`。
   - 组件已经装好了，向导里什么都不用勾，直接 Next → Finish。
4. 欢迎页点 **Open**，选 `D:\Android\Projects\HelloApk`。
5. 弹出 *Trust Project* → 点 **Trust Project**。
6. 右下角开始 **Gradle Sync**，第一次约 1～3 分钟（依赖已缓存，比全新机器快很多）。看到 `BUILD SUCCESSFUL` / 底部 `Sync finished` 就好了。

---

## 四、两种 APK，先搞清区别

| | debug APK | release APK |
|---|---|---|
| 签名 | 自动用 IDE 的 debug key | 用你自己的 keystore |
| 能上架应用商店吗 | ❌ 不能 | ✅ 能 |
| 能给别人装吗 | ✅ 能（但不能覆盖安装正式版） | ✅ 能 |
| 体积/性能 | 未优化 | 可开混淆压缩 |
| 用途 | 自己调试、发给同事内测 | 正式发布 |

**关键点：Android 要求每个 APK 必须签名才能安装。** debug 包用的是系统自动生成的调试证书，正式包必须用你自己的、且**永久保存**的证书。

---

## 五、方式 A：命令行打包（最快，推荐日常用）

打开 CMD 或 PowerShell：

```cmd
cd /d D:\Android\Projects\HelloApk

:: 打 debug 包
gradle assembleDebug

:: 打正式签名包
gradle assembleRelease
```

产物位置：

```
app\build\outputs\apk\debug\app-debug.apk        ← debug 包
app\build\outputs\apk\release\app-release.apk    ← release 包（已签名）
```

想一步到位装到手机上：

```cmd
gradle installDebug
```

> 第一次运行 `gradle` 会启动守护进程，之后每次编译只要几秒。
> 想看详细过程加 `--info`，想看耗时加 `--profile`。

工程里也带了 `gradlew.bat`（Gradle Wrapper）。用法完全一样：

```cmd
gradlew assembleDebug
```

区别是它会自动下载「工程指定的那个 Gradle 版本」（已配到腾讯镜像），
所以换一台机器、或者别人 clone 你的代码，不需要先装 Gradle 也能直接编。
**实际项目建议统一用 `gradlew`**，保证所有人 Gradle 版本一致。

---

## 六、方式 B：Android Studio 图形界面打包

### 打 debug 包

菜单 **Build → Build Bundle(s) / APK(s) → Build APK(s)**，
完成后右下角弹提示，点 **locate** 直接打开输出文件夹。

### 打正式签名包（上架用这个）

1. 菜单 **Build → Generate Signed Bundle / APK…**
2. 选 **APK**（不是 Android App Bundle），Next
3. **Key store path** 点右侧文件夹图标，选：
   `D:\Android\Keystore\helloapk.jks`
4. 填：
   - Key store password：`hello123456`
   - Key alias：`helloapk`
   - Key password：`hello123456`
5. Next → 选 **release** → 勾上 **V1**、**V2** 签名 → **Create**
6. 产物：`app\release\app-release.apk`（注意这个路径和命令行不同，是 `app\release\` 不是 `app\build\outputs\`）

> 如果你勾了 "Remember passwords"，密码会存到 IDE 的密码库里，下次不用再输。

---

## 七、签名（keystore）到底是怎么回事

### 已经给你生成好了

| 项 | 值 |
|---|---|
| 文件 | `D:\Android\Keystore\helloapk.jks` |
| 类型 | PKCS12 |
| 别名 alias | `helloapk` |
| 密码（store / key 同一个） | `hello123456` |
| 算法 | RSA 2048，有效期 10000 天 |
| 主体 | `CN=HelloApk, OU=Dev, O=Demo, L=Hangzhou, ST=Zhejiang, C=CN` |

查找证书信息：

```cmd
keytool -list -v -keystore D:\Android\Keystore\helloapk.jks -storepass hello123456
```

### 自己重新生成一份

```cmd
keytool -genkeypair -v ^
  -keystore D:\Android\Keystore\myapp.jks -storetype PKCS12 ^
  -alias myapp -keyalg RSA -keysize 2048 -validity 10000 ^
  -storepass 你的密码 -keypass 你的密码 ^
  -dname "CN=MyApp, OU=Dev, O=MyCompany, L=Hangzhou, ST=Zhejiang, C=CN"
```

### 密码没有写死在构建脚本里

`app/build.gradle` 从根目录的 `keystore.properties` 读取：

```properties
storeFile=D:/Android/Keystore/helloapk.jks
storePassword=hello123456
keyAlias=helloapk
keyPassword=hello123456
```

`keystore.properties` 已经在 `.gitignore` 里，不会被提交。

> 🔴 **三件绝对不能做的事**
> 1. 不要把 `*.jks` 提交到 Git / 网盘公开分享。
> 2. 不要弄丢 keystore —— 丢了就**永远无法**给你的 App 发升级包，只能换个包名重新上架，老用户全部丢失。
> 3. 不要改 alias 和密码后忘记记录 —— 编译时对不上会直接报签名失败。
>
> 建议：把 `.jks` + 密码单独备份两份（U 盘 / 密码管理器）。

---

## 八、安装到手机

### 用数据线（推荐）

1. 手机打开 **开发者选项**（设置 → 关于手机 → 连点"版本号"7 次）
2. 打开 **USB 调试**，插线，手机上点"允许调试"
3. 验证设备已连上：

```cmd
adb devices
```

4. 安装：

```cmd
adb install -r D:\Android\Projects\HelloApk\app\build\outputs\apk\debug\app-debug.apk
```

`-r` 表示覆盖安装并保留数据。

### 直接传文件

把 APK 拷到手机，用文件管理器点开安装；系统会提示"未知来源应用"，允许一次即可。

---

## 九、验证签名是否成功

```cmd
apksigner verify --print-certs D:\Android\Projects\HelloApk\app\build\outputs\apk\release\app-release.apk
```

正常会输出 `Verifies` 以及 V2/V3 签名方案、证书指纹、DN。

本机实测输出（可直接对照）：

| 包 | 大小 | 签名证书 DN |
|---|---|---|
| `app-debug.apk` | 9,891 字节 | `C=US, O=Android, CN=Android Debug`（系统调试证书） |
| `app-release.apk` | 11,649 字节 | `CN=HelloApk, OU=Dev, O=Demo, L=Hangzhou, ST=Zhejiang, C=CN`（你的证书） |

release 包的 `aapt2 dump badging` 关键行：

```
package: name='com.example.helloapk' versionCode='1' versionName='1.0' compileSdkVersion='36'
minSdkVersion:'24'
targetSdkVersion:'36'
launchable-activity: name='com.example.helloapk.MainActivity'
```

> 包只有 10 KB 出头是正常的 —— 这个示例没有图标、没有第三方库、没开混淆。
> 一旦引入 AndroidX / 图片资源，体积会到几百 KB 甚至几 MB。

也可以看 APK 基本信息：

```cmd
aapt2 dump badging D:\Android\Projects\HelloApk\app\build\outputs\apk\release\app-release.apk
```

---

## 十、这个示例工程的结构

```
D:\Android\Projects\HelloApk\
├── settings.gradle          ← 声明从哪个仓库下载依赖（已配阿里云镜像）
├── build.gradle             ← 声明 AGP 版本 8.13.0
├── gradle.properties        ← JVM 内存、并行、缓存等开关
├── keystore.properties      ← 签名密码（不进 Git）
├── local.properties         ← SDK 路径（不进 Git，Studio 自动生成）
└── app\
    ├── build.gradle         ← 模块配置：包名/版本号/签名/混淆
    └── src\main\
        ├── AndroidManifest.xml
        ├── java\com\example\helloapk\MainActivity.java
        └── res\layout\activity_main.xml
```

改版本号就在这里：

```groovy
defaultConfig {
    applicationId 'com.example.helloapk'
    versionCode 1        // 整数，每次发版 +1，否则商店拒收
    versionName '1.0'    // 用户看到的版本号
}
```

---

## 十一、常见报错对照表

| 报错 | 原因 | 解决 |
|---|---|---|
| `SDK location not found` | 缺 `local.properties` | 新建，内容 `sdk.dir=D:\\Android\\Sdk`，或设 `ANDROID_HOME` |
| `Could not resolve com.android.tools.build:gradle` | 连不上 Google Maven | 检查 `settings.gradle` 里阿里云镜像是否在最前面 |
| `Unsupported class file major version` | JDK 版本不匹配 | 确认 `JAVA_HOME` 指向 `D:\Android\jdk`（21），别用其它 JDK |
| `Failed to install ... build-tools` | AGP 想装它默认的 build-tools | 本工程已用 `buildToolsVersion '36.0.0'` 固定 |
| `INSTALL_FAILED_UPDATE_INCOMPATIBLE` | 手机上的旧包签名不同 | 先 `adb uninstall 包名` 再装 |
| `INSTALL_FAILED_VERSION_DOWNGRADE` | versionCode 比已装的小 | 调大 versionCode |
| `Keystore was tampered with, or password was incorrect` | 密码/别名写错 | 核对 `keystore.properties` |
| 中文注释乱码 | 文件编码不是 UTF-8 | `gradle.properties` 里已有 `-Dfile.encoding=UTF-8` |
| 编译突然变得很慢 / C 盘满 | Gradle 缓存默认在 C 盘 | 已设 `GRADLE_USER_HOME=D:\Android\gradle-home` |
| `SDK emulator directory is missing` | 没装模拟器 | `sdkmanager "emulator" "system-images;android-36;google_apis;x86_64"` |

---

## 十二、下一步可以做什么

### 加第三方库（AndroidX / OkHttp 等）

在 `app/build.gradle` 里加：

```groovy
dependencies {
    implementation 'androidx.appcompat:appcompat:1.7.0'
    implementation 'com.google.android.material:material:1.12.0'
}
```

仓库已在 `settings.gradle` 配好镜像，加完直接 `gradle assembleDebug` 即可。

> 注意：示例工程目前**没有用 AndroidX**（为了零依赖、编译最快）。一旦引入 AndroidX，主题要改成 `Theme.AppCompat.*`，`MainActivity` 要继承 `AppCompatActivity`。

### 改用 Kotlin

1. `build.gradle` 加 Kotlin 插件：
   ```groovy
   plugins {
       id 'org.jetbrains.kotlin.android' version '2.2.10' apply false
   }
   ```
2. `app/build.gradle` 顶部 `id 'org.jetbrains.kotlin.android'`
3. Java 文件改成 `.kt`。

### 开启混淆压缩（发布前）

```groovy
release {
    minifyEnabled true
    shrinkResources true
    proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
}
```

混淆后**务必**真机跑一遍，反射/序列化的类要写 keep 规则。

### 上架 Google Play

商店要的是 **AAB** 不是 APK：

```cmd
gradle bundleRelease
```

产物在 `app\build\outputs\bundle\release\app-release.aab`。

---

## 十三、快速命令备忘

```cmd
:: 环境自检
java -version && adb version && gradle -v

:: 打各种包
gradle assembleDebug        :: debug APK
gradle assembleRelease      :: 签名 release APK
gradle bundleRelease        :: 上架用 AAB
gradle clean                :: 清理构建产物
gradle tasks                :: 看所有可用任务

:: 装机
adb devices
adb install -r app\build\outputs\apk\debug\app-debug.apk
adb uninstall com.example.helloapk.debug

:: 看日志
adb logcat -s AndroidRuntime
```
