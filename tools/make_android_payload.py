"""把安卓源码打包成一个「解压即合并」的 zip，供零基础用户直接覆盖到 Android Studio 工程。

产物：dist/iOSHub-android-payload.zip
结构（解压后直接覆盖到项目根目录）：
  app/src/main/java/com/nevercrynow/ioshub/...
  app/src/main/assets/seed-resources.json
  开始读我.txt
  参考-依赖片段.txt

用法：<bundled python> tools/make_android_payload.py
"""
from __future__ import annotations

import pathlib
import zipfile

ROOT = pathlib.Path(".")
SRC = ROOT / "android/app/src/main"
OUT = ROOT / "dist/iOSHub-android-payload.zip"

README = """iOS HUB · 安卓版源码包
=========================

用途：把这个包里的 app 文件夹，覆盖到你用 Android Studio 新建的项目根目录。

步骤：
1. 在 Android Studio 里新建一个 Empty Activity 项目：
   Name = iOSHUB
   Package name = com.nevercrynow.ioshub
   Language = Kotlin
   Minimum SDK = API 26
2. 把本压缩包解压到任意位置。
3. 把解压出来的 app 文件夹，复制到项目根目录（和原来的 app 合并，提示覆盖时选"是"）。
   正确结果：app/src/main/java/com/nevercrynow/ioshub/ 下会出现 MainActivity.kt、data、ui。
4. 打开 app/build.gradle.kts，在 dependencies { } 里加一行（见"参考-依赖片段.txt"），点 Sync Now。
5. 打开 app/src/main/AndroidManifest.xml，在 <application> 上面加一行：
   <uses-permission android:name="android.permission.INTERNET" />
6. 点绿色 ▶ Run。

更详细的图文步骤见 docs/ANDROID.md。
"""

SNIPPET = """把下面这一行加进 app/build.gradle.kts 的 dependencies { ... } 里：

    implementation("androidx.compose.material:material-icons-core")

加完后点右上角的 Sync Now（或大象图标）等待同步完成。
"""


def collect() -> list[pathlib.Path]:
    files: list[pathlib.Path] = []
    files.extend(sorted(SRC.glob("java/**/*.kt")))
    files.append(SRC / "assets/seed-resources.json")
    return files


def main() -> None:
    OUT.parent.mkdir(parents=True, exist_ok=True)
    files = collect()
    with zipfile.ZipFile(OUT, "w", zipfile.ZIP_DEFLATED) as zf:
        for path in files:
            # 相对 android/app/src/main 的路径，映射到 app/src/main/...
            relative = path.relative_to(SRC)
            target = pathlib.PurePosixPath("app/src/main") / pathlib.PurePosixPath(relative.as_posix())
            zf.write(path, str(target))
        zf.writestr("开始读我.txt", README)
        zf.writestr("参考-依赖片段.txt", SNIPPET)

    size = OUT.stat().st_size
    print(f"saved {OUT} ({size/1024:.1f} KB, {len(files)} 个源码文件)")


if __name__ == "__main__":
    main()
