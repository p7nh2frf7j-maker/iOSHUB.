"""生成 Android 启动图标（mipmap 各密度）。

运行：<bundled python> tools/render_android_icons.py
"""
from __future__ import annotations

import pathlib
import sys

sys.path.insert(0, str(pathlib.Path(__file__).parent))
from render_brand_assets import make_icon  # noqa: E402

RES = pathlib.Path("android/app/src/main/res")
DENSITIES = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}


def main() -> None:
    for folder, size in DENSITIES.items():
        target = RES / folder
        target.mkdir(parents=True, exist_ok=True)
        icon = make_icon(size)
        icon.save(target / "ic_launcher.png", optimize=True)
        print("saved", target / "ic_launcher.png", icon.size)


if __name__ == "__main__":
    main()
