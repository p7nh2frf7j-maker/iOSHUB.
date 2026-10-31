"""渲染启动动画的关键帧（3 帧），用于 inspect 动画节奏。

产出 docs/mockup-launch-frames.png

运行：<bundled python> tools/render_launch_frames.py
"""
from __future__ import annotations

import pathlib
import sys

from PIL import Image, ImageDraw, ImageFilter

sys.path.insert(0, str(pathlib.Path(__file__).parent))
from render_brand_assets import BRAND1, PAGE, SECOND, TEXT, font, make_icon  # noqa: E402

ROSE = (242, 92, 117)

FRAME_W, FRAME_H = 250, 470
GAP = 26
SCALE = 2


def rounded(img: Image.Image, radius: int) -> Image.Image:
    mask = Image.new("L", img.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, img.size[0] - 1, img.size[1] - 1], radius=radius, fill=255)
    out = Image.new("RGBA", img.size, (0, 0, 0, 0))
    out.paste(img, (0, 0), mask)
    return out


def frame(
    slab_progress: float,
    mark_alpha: float,
    word_alpha: float,
    by_alpha: float,
    tag_alpha: float,
    bar: float,
    dedic_alpha: float = 0.0,
) -> Image.Image:
    w, h = FRAME_W * SCALE, FRAME_H * SCALE
    img = Image.new("RGB", (w, h), PAGE)

    glow = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    gd = ImageDraw.Draw(glow)
    r = int(w * 0.42)
    gd.ellipse([w // 2 - r, int(h * 0.42) - r, w // 2 + r, int(h * 0.42) + r],
               fill=BRAND1 + (int(52 * mark_alpha),))
    glow = glow.filter(ImageFilter.GaussianBlur(int(16 * SCALE)))
    img = Image.alpha_composite(img.convert("RGBA"), glow)

    # 图标
    icon_px = int(66 * SCALE)
    icon = make_icon(icon_px, slab_progress=slab_progress).convert("RGBA")
    icon = rounded(icon, int(icon_px * 0.30))
    if mark_alpha < 1:
        alpha = icon.getchannel("A").point(lambda v: int(v * mark_alpha))
        icon.putalpha(alpha)
    img.alpha_composite(icon, (w // 2 - icon_px // 2, int(h * 0.34)))

    d = ImageDraw.Draw(img)
    cy = int(h * 0.34) + icon_px + int(22 * SCALE)

    if word_alpha > 0.01:
        layer = Image.new("RGBA", (w, h), (0, 0, 0, 0))
        ld = ImageDraw.Draw(layer)
        ld.text((w // 2, cy), "iOS HUB", font=font(30 * SCALE), fill=TEXT + (int(255 * word_alpha),), anchor="ma")
        img = Image.alpha_composite(img, layer)
        d = ImageDraw.Draw(img)

    if by_alpha > 0.01:
        layer = Image.new("RGBA", (w, h), (0, 0, 0, 0))
        ld = ImageDraw.Draw(layer)
        ld.text((w // 2, cy + int(38 * SCALE)), "by nevercrynow",
                font=font(11 * SCALE), fill=BRAND1 + (int(255 * by_alpha),), anchor="ma")
        img = Image.alpha_composite(img, layer)
        d = ImageDraw.Draw(img)

    if tag_alpha > 0.01:
        layer = Image.new("RGBA", (w, h), (0, 0, 0, 0))
        ld = ImageDraw.Draw(layer)
        ld.text((w // 2, cy + int(64 * SCALE)), "全生态 iOS 资源中心",
                font=font(11 * SCALE, False), fill=SECOND + (int(255 * tag_alpha),), anchor="ma")
        img = Image.alpha_composite(img, layer)
        d = ImageDraw.Draw(img)

    if dedic_alpha > 0.01:
        layer = Image.new("RGBA", (w, h), (0, 0, 0, 0))
        ld = ImageDraw.Draw(layer)
        a = int(255 * dedic_alpha)
        dy = cy + int(98 * SCALE)
        ld.text((w // 2 - int(30 * SCALE), dy), "献给",
                font=font(9 * SCALE, False), fill=(120, 120, 135, a), anchor="ma")
        ld.text((w // 2 + int(10 * SCALE), dy), "赵菁然",
                font=font(13 * SCALE), fill=ROSE + (a,), anchor="ma")
        img = Image.alpha_composite(img, layer)
        d = ImageDraw.Draw(img)

    # 底部进度
    track_w, track_h = int(94 * SCALE), int(3 * SCALE)
    tx = w // 2 - track_w // 2
    ty = h - int(72 * SCALE)
    d.rounded_rectangle([tx, ty, tx + track_w, ty + track_h], radius=track_h, fill=(223, 223, 231))
    if bar > 0:
        d.rounded_rectangle([tx, ty, tx + int(track_w * bar), ty + track_h], radius=track_h, fill=BRAND1)
    d.text((w // 2, ty + int(16 * SCALE)), "N E V E R C R Y N O W",
           font=font(8 * SCALE), fill=SECOND + (170,), anchor="ma")
    return img.convert("RGB")


def main() -> None:
    frames = [
        (0.24, 0.65, 0.0, 0.0, 0.0, 0.10, 0.0, "0.15s 光晕 + 图标出现"),
        (0.55, 1.0, 0.0, 0.0, 0.0, 0.38, 0.0, "0.45s 叠片逐条绘出"),
        (1.0, 1.0, 1.0, 0.85, 0.6, 0.72, 0.0, "0.78s 字样上浮"),
        (1.0, 1.0, 1.0, 1.0, 1.0, 0.92, 0.35, "1.05s 献词浮出"),
        (1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, "1.40s 停留后淡出"),
    ]
    tiles = [frame(p, m, wd, b, t, pb, d) for p, m, wd, b, t, pb, d, _ in frames]

    labels = [f[7] for f in frames]
    total_w = FRAME_W * SCALE * len(tiles) + GAP * SCALE * (len(tiles) - 1)
    out = Image.new("RGB", (total_w, FRAME_H * SCALE + 44 * SCALE), (238, 238, 244))
    x = 0
    d = ImageDraw.Draw(out)
    for tile, label in zip(tiles, labels):
        out.paste(tile, (x, 0))
        d = ImageDraw.Draw(out)
        d.text((x + FRAME_W * SCALE // 2, FRAME_H * SCALE + 16 * SCALE), label,
               font=font(10 * SCALE, False), fill=(90, 90, 100), anchor="ma")
        x += FRAME_W * SCALE + GAP * SCALE

    out.save("docs/mockup-launch-frames.png", optimize=True)
    print("saved docs/mockup-launch-frames.png", out.size)


if __name__ == "__main__":
    main()
