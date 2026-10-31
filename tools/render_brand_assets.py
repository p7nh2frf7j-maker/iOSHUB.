"""生成品牌资产：App 图标（1024）与启动画面设计稿。

产出：
  iOSHUB/Assets.xcassets/AppIcon.appiconset/AppIcon-1024.png   ← 真图标
  docs/app-icon-1024.png                                      ← 预览副本
  docs/mockup-splash.png                                      ← 启动画面设计稿

运行：<bundled python> tools/render_brand_assets.py
"""
from __future__ import annotations

import pathlib

import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont

BRAND1 = (84, 102, 250)
BRAND2 = (120, 63, 240)
VIOLET = (158, 107, 250)
PAGE = (242, 242, 247)
TEXT = (17, 17, 20)
SECOND = (120, 120, 128)
FONT_DIR = pathlib.Path("C:/Windows/Fonts")


def font(size_px: int, bold: bool = True) -> ImageFont.FreeTypeFont:
    return ImageFont.truetype(str(FONT_DIR / ("msyhbd.ttc" if bold else "msyh.ttc")), size_px)


def diagonal_gradient(w: int, h: int, c1, c2, mix=0.85) -> Image.Image:
    xs = np.linspace(0, 1, w)[None, :]
    ys = np.linspace(0, 1, h)[:, None]
    t = np.clip(xs * 0.55 + ys * 0.45, 0, 1) * mix
    arr = np.zeros((h, w, 3), dtype=np.uint8)
    for i in range(3):
        arr[..., i] = (c1[i] + (c2[i] - c1[i]) * t).astype(np.uint8)
    return Image.fromarray(arr, "RGB")


def capsule_layer(size: int, progress: float = 1.0) -> Image.Image:
    """三层阶梯叠片：与 Compose 里 BrandMark.drawHubMark 的几何参数一一对应。

    progress < 1 时每片按顺序从左往右「长出来」，用于启动动画关键帧。
    """
    layer = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    h = size * 0.112
    corner = size * 0.056
    for index, (x0, x1, y, alpha) in enumerate((
        (0.235, 0.585, 0.265, 255),
        (0.330, 0.720, 0.437, 255),
        (0.425, 0.765, 0.609, 240),
    )):
        grow = max(0.0, min(1.0, (progress - index * 0.18) / 0.62))
        if grow <= 0.001:
            continue
        d.rounded_rectangle(
            [x0 * size, y * size, (x0 + (x1 - x0) * grow) * size, y * size + h],
            radius=corner,
            fill=(255, 255, 255, alpha),
        )
    return layer


def make_icon(size: int = 1024, slab_progress: float = 1.0) -> Image.Image:
    icon = diagonal_gradient(size, size, BRAND1, BRAND2).convert("RGBA")

    # 左上角柔光，避免整块死板
    yy, xx = np.mgrid[0:size, 0:size]
    r = np.sqrt((xx - size * 0.28) ** 2 + (yy - size * 0.18) ** 2) / size
    glow = np.clip(1 - r * 2.1, 0, 1) ** 2 * 0.22
    white = np.full((size, size, 3), 255, dtype=np.float32)
    arr = np.array(icon, dtype=np.float32)[..., :3]
    arr = arr * (1 - glow[..., None]) + white * glow[..., None]
    icon = Image.fromarray(arr.astype(np.uint8), "RGB").convert("RGBA")

    icon.alpha_composite(capsule_layer(size, slab_progress))
    return icon.convert("RGB")


def text(d, xy, s, f, fill, anchor="la"):
    d.text(xy, s, font=f, fill=fill, anchor=anchor)


def make_splash(scale: int = 2) -> Image.Image:
    W, H = 390, 844
    img = Image.new("RGB", (W * scale, H * scale), PAGE)

    glow = Image.new("RGBA", (W * scale, H * scale), (0, 0, 0, 0))
    gd = ImageDraw.Draw(glow)
    gd.ellipse(
        [int((195 - 150) * scale), int((380 - 150) * scale),
         int((195 + 150) * scale), int((380 + 150) * scale)],
        fill=BRAND1 + (60,),
    )
    glow = glow.filter(ImageFilter.GaussianBlur(28 * scale))
    img = Image.alpha_composite(img.convert("RGBA"), glow).convert("RGB")

    icon = make_icon(78 * scale).resize((78 * scale, 78 * scale), Image.LANCZOS)
    mask = Image.new("L", icon.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, icon.size[0] - 1, icon.size[1] - 1],
                                           radius=int(78 * scale * 0.31), fill=255)
    img.paste(icon, (int((195 - 39) * scale), int(344 * scale)), mask)

    d = ImageDraw.Draw(img)
    text(d, (195 * scale, 444 * scale), "iOS HUB", font(30 * scale), TEXT, anchor="ma")
    text(d, (195 * scale, 482 * scale), "by nevercrynow", font(11 * scale), BRAND1, anchor="ma")
    text(d, (195 * scale, 508 * scale), "全生态 iOS 资源中心", font(12 * scale, False), SECOND, anchor="ma")
    return img


def main() -> None:
    icon = make_icon(1024)
    icon_path = pathlib.Path("iOSHUB/Assets.xcassets/AppIcon.appiconset/AppIcon-1024.png")
    icon_path.parent.mkdir(parents=True, exist_ok=True)
    icon.save(icon_path)
    icon.save("docs/app-icon-1024.png")

    splash = make_splash(2)
    splash.save("docs/mockup-splash.png")
    print("saved:", icon_path, "docs/app-icon-1024.png", "docs/mockup-splash.png", icon.size, splash.size)


if __name__ == "__main__":
    main()
