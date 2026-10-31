"""渲染 iOS HUB 首页设计稿（PNG）。

不是真实截图，是按设计规范绘制的静态稿，用于对齐视觉与信息层级。
运行：<bundled python> tools/render_mockup.py
"""
from __future__ import annotations

import pathlib

from PIL import Image, ImageDraw, ImageFilter, ImageFont

S = 2  # 2x 渲染，390x1240 pt -> 780x2480 px

PAGE = (245, 245, 250)
CARD = (255, 255, 255)
TEXT = (17, 17, 20)
SECOND = (120, 120, 128)
TERT = (176, 176, 184)
STROKE = (231, 231, 236)
BRAND1 = (84, 102, 250)
BRAND2 = (120, 63, 240)
VIOLET = (158, 107, 250)
CYAN = (56, 199, 235)
MINT = (41, 194, 153)
AMBER = (247, 173, 56)
ROSE = (242, 92, 117)
SLATE = (107, 120, 148)
SEC_RED = (217, 77, 82)
DEV_BLUE = (77, 140, 242)
THEME_PURPLE = (184, 102, 230)
UTIL_TEAL = (56, 168, 161)

FONT_DIR = pathlib.Path("C:/Windows/Fonts")
_fonts: dict[tuple[str, int], ImageFont.FreeTypeFont] = {}


def font(size: int, bold: bool = False) -> ImageFont.FreeTypeFont:
    key = ("bd" if bold else "rg", size)
    if key not in _fonts:
        name = "msyhbd.ttc" if bold else "msyh.ttc"
        _fonts[key] = ImageFont.truetype(str(FONT_DIR / name), size * S)
    return _fonts[key]


def px(v: float) -> int:
    return int(round(v * S))


def rr(d: ImageDraw.ImageDraw, box, radius: float, fill=None, outline=None, width: float = 1):
    d.rounded_rectangle(
        [px(box[0]), px(box[1]), px(box[2]), px(box[3])],
        radius=px(radius),
        fill=fill,
        outline=outline,
        width=px(width) if outline else 0,
    )


def text(d, xy, s, size, fill=TEXT, bold=False, anchor="la"):
    d.text((px(xy[0]), px(xy[1])), s, font=font(size, bold), fill=fill, anchor=anchor)


def gradient(size_pt, c1, c2) -> Image.Image:
    w, h = px(size_pt[0]), px(size_pt[1])
    img = Image.new("RGB", (1, h))
    px_ = img.load()
    for y in range(h):
        t = y / max(h - 1, 1)
        px_[0, y] = tuple(int(c1[i] + (c2[i] - c1[i]) * t) for i in range(3))
    return img.resize((w, h), Image.BILINEAR)


def gradient_card(base: Image.Image, box, radius, c1, c2):
    x0, y0, x1, y1 = px(box[0]), px(box[1]), px(box[2]), px(box[3])
    w, h = x1 - x0, y1 - y0
    layer = gradient((box[2] - box[0], box[3] - box[1]), c1, c2).convert("RGBA")
    mask = Image.new("L", (w, h), 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, w - 1, h - 1], radius=px(radius), fill=255)
    layer.putalpha(mask)
    base.paste(layer, (x0, y0), layer)


def wrap(d, s: str, size: int, max_w: float, bold=False) -> list[str]:
    f = font(size, bold)
    lines, cur = [], ""
    for ch in s:
        trial = cur + ch
        if d.textlength(trial, font=f) / S > max_w and cur:
            lines.append(cur)
            cur = ch
        else:
            cur = trial
    if cur:
        lines.append(cur)
    # 避免标点单独成行
    if len(lines) > 1 and lines[-1] in "，。、；：！？）」":
        lines[-2] += lines[-1]
        lines.pop()
    return lines


def overlay(base, box, radius, rgba):
    """带 alpha 的圆角块（直接 fill 四元组在 RGB 图上会变成实心）。"""
    x0, y0, x1, y1 = px(box[0]), px(box[1]), px(box[2]), px(box[3])
    w, h = x1 - x0, y1 - y0
    layer = Image.new("RGBA", (w, h), rgba[:3] + (0,))
    ImageDraw.Draw(layer).rounded_rectangle([0, 0, w - 1, h - 1], radius=px(radius), fill=rgba)
    base.paste(layer, (x0, y0), layer)


def glass_pill(base, d, xy, label):
    f = font(10, True)
    w = d.textlength(label, font=f) / S + 18
    overlay(base, (xy[0], xy[1], xy[0] + w, xy[1] + 20), 10, (255, 255, 255, 52))
    text(d, (xy[0] + 9, xy[1] + 4), label, 10, CARD, True)
    return w


def layers_icon(d, cx, cy, size, color, width=1.8):
    """logo：三层叠在一起的方块。"""
    s = size / 2
    for i, dy in enumerate((s * 0.42, 0, -s * 0.42)):
        d.rounded_rectangle([px(cx - s + i * 1), px(cy + dy - s * 0.42),
                             px(cx + s - i * 1), px(cy + dy + s * 0.42)],
                            radius=px(2), outline=color, width=px(width))


def bell_icon(d, cx, cy, color):
    d.arc([px(cx - 7), px(cy - 9), px(cy + 7) if False else px(cx + 7), px(cy + 5)],
          start=180, end=360, fill=color, width=px(1.6))
    d.line([px(cx - 7), px(cy - 2), px(cx - 8), px(cy + 3)], fill=color, width=px(1.6))
    d.line([px(cx + 7), px(cy - 2), px(cx + 8), px(cy + 3)], fill=color, width=px(1.6))
    d.line([px(cx - 8), px(cy + 3), px(cx + 8), px(cy + 3)], fill=color, width=px(1.6))
    d.ellipse([px(cx - 2), px(cy + 5), px(cx + 2), px(cy + 9)], fill=color)


def info_icon(d, cx, cy, color):
    d.ellipse([px(cx - 6), px(cy - 6), px(cx + 6), px(cy + 6)], outline=color, width=px(1.5))
    text(d, (cx, cy - 0.5), "i", 8, color, True, anchor="mm")

def pill(d, xy, label, fg, bg, size=10, pad=(9, 5)):
    f = font(size, True)
    w = d.textlength(label, font=f) / S + pad[0] * 2
    h = size + pad[1] * 2
    rr(d, (xy[0], xy[1], xy[0] + w, xy[1] + h), h / 2, fill=bg)
    text(d, (xy[0] + pad[0], xy[1] + pad[1] - 1), label, size, fg, True)
    return w


def magnifier(d, cx, cy, r, color, width=1.6):
    d.ellipse([px(cx - r), px(cy - r), px(cx + r), px(cy + r)], outline=color, width=px(width))
    d.line([px(cx + r * 0.7), px(cy + r * 0.7), px(cx + r * 1.6), px(cy + r * 1.6)], fill=color, width=px(width))


def icon_tile(d, xy, size, tint, glyph):
    rr(d, (xy[0], xy[1], xy[0] + size, xy[1] + size), size * 0.28,
       fill=tuple(int(c + (255 - c) * 0.86) for c in tint))
    text(d, (xy[0] + size / 2, xy[1] + size / 2), glyph, int(size * 0.42), tint, True, anchor="mm")


def brand_logo(base, x, y, size, radius=None):
    """品牌标记：渐变圆角方块 + 三层白色叠片（与 BrandLogo / AppIcon 一致）。"""
    r = radius if radius is not None else size * 0.31
    gradient_card(base, (x, y, x + size, y + size), r, BRAND1, BRAND2)
    cx, cy = x + size / 2, y + size / 2
    h = size * 0.095
    for ratio, offset in ((0.40, -0.17), (0.54, 0.0), (0.40, 0.17)):
        w = size * ratio
        overlay(
            base,
            (cx - w / 2, cy + offset * size - h / 2, cx + w / 2, cy + offset * size + h / 2),
            h / 2,
            (255, 255, 255, 242),
        )


CATEGORIES = [
    ("J", "越狱", BRAND1), ("T", "巨魔", VIOLET), ("I", "IPA", CYAN),
    ("S", "侧载", MINT), ("P", "插件", ROSE), ("R", "软件源", AMBER),
    ("O", "开源", SLATE), ("V", "漏洞", SEC_RED), ("D", "开发", DEV_BLUE),
    ("M", "美化", THEME_PURPLE), ("U", "工具", UTIL_TEAL), ("N", "资讯", SECOND),
]

def glass_ambient(img: Image.Image) -> Image.Image:
    """液态玻璃底色：四团高斯模糊的品牌色光斑，卡片叠在上面像毛玻璃。"""
    w, h = img.size
    layer = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    ld = ImageDraw.Draw(layer)
    specs = [
        ((84, 102, 250, 60), (0.00, 0.00), 0.32),
        ((158, 107, 250, 48), (1.00, 0.06), 0.30),
        ((41, 194, 153, 34), (0.00, 0.52), 0.26),
        ((242, 92, 117, 30), (1.00, 0.94), 0.26),
    ]
    for (r, g, b, a), (cx, cy), rad in specs:
        radius = int(max(w, h) * rad)
        x, y = int(w * cx), int(h * cy)
        ld.ellipse([x - radius, y - radius, x + radius, y + radius], fill=(r, g, b, a))
    layer = layer.filter(ImageFilter.GaussianBlur(int(w * 0.06)))
    return Image.alpha_composite(img.convert("RGBA"), layer).convert("RGB")


W_PT, H_PT = 390, 1300
base = glass_ambient(Image.new("RGB", (px(W_PT), px(H_PT)), PAGE))
d = ImageDraw.Draw(base)

# ---------------- 状态栏 ----------------
text(d, (26, 16), "9:41", 15, TEXT, True)
for i in range(3):
    d.rounded_rectangle([px(316 + i * 5), px(22 - i * 3), px(319 + i * 5), px(28)],
                        radius=px(1), fill=TEXT)
d.arc([px(336), px(17), px(352), px(31)], start=200, end=340, fill=TEXT, width=px(1.6))
d.rounded_rectangle([px(358, ), px(17), px(374), px(28)], radius=px(3), outline=TEXT, width=px(1))
d.rounded_rectangle([px(360), px(19), px(371), px(26)], radius=px(1.5), fill=TEXT)

# ---------------- 品牌头 ----------------
brand_logo(base, 20, 48, 42, 13)
text(d, (74, 50), "iOS HUB", 27, TEXT, True)
title_w = d.textlength("iOS HUB", font=font(27, True)) / S
text(d, (74 + title_w + 7, 72), "by nevercrynow", 10, BRAND1, True, anchor="ls")
text(d, (75, 84), "早上好，今天有 32 个资源待发现", 12, SECOND)

rr(d, (330, 52, 368, 90), 19, fill=(236, 236, 250))
bell_icon(d, 349, 70, BRAND1)

# ---------------- 搜索入口 ----------------
rr(d, (20, 108, 370, 154), 15, fill=CARD, outline=STROKE)
magnifier(d, 40, 131, 6, SECOND)
text(d, (58, 123), "搜索工具、插件、IPA、开发者或 iOS 版本", 13, TERT)
for i, w in enumerate((14, 10, 14)):
    d.line([px(338), px(126 + i * 6), px(338 + w), px(126 + i * 6)], fill=BRAND1, width=px(1.6))

# ---------------- 今日精选 ----------------
text(d, (24, 176), "今日精选", 19, TEXT, True)
text(d, (24, 202), "人工核对来源的长期项目", 12, SECOND)

gradient_card(base, (20, 224, 370, 420), 22, BRAND1, BRAND2)
overlay(base, (36, 240, 80, 284), 12, (255, 255, 255, 58))
text(d, (58, 262), "T", 20, BRAND2, True, anchor="mm")
text(d, (92, 241), "巨魔商店", 11, (236, 236, 255))
text(d, (92, 254), "TrollStore", 19, CARD, True)
text(d, (344, 246), "↗", 15, CARD, True, anchor="ra")

for i, line in enumerate(wrap(d, "利用 CoreTrust 做永久签名，装上的 IPA 不会掉签，也不需要每周重签。", 12, 300)):
    text(d, (36, 300 + i * 18), line, 12, (238, 238, 255))

w = glass_pill(base, d, (36, 370), "谨慎使用")
glass_pill(base, d, (36 + w + 6, 370), "部分支持")
text(d, (344, 378), "待验证", 11, (236, 236, 255), False, anchor="ra")

# ---------------- 资源分类 ----------------
text(d, (24, 448), "资源分类", 19, TEXT, True)
text(d, (24, 474), "覆盖从越狱到开发工具的 12 个方向", 12, SECOND)

x = 20
for glyph, label, tint in CATEGORIES:
    icon_tile(d, (x, 498), 46, tint, glyph)
    text(d, (x + 23, 552), label, 10, TEXT, False, anchor="ma")
    x += 64 + 8

# ---------------- 最新收录 ----------------
text(d, (24, 596), "最新收录", 19, TEXT, True)
text(d, (24, 622), "内置库策展顺序 · 联网后按真实更新时间重排", 12, SECOND)


def resource_card(x0, y0, glyph, name, dev, summary, tint, tags):
    rr(d, (x0, y0, x0 + 232, y0 + 172), 18, fill=CARD, outline=STROKE)
    icon_tile(d, (x0 + 14, y0 + 14), 38, tint, glyph)
    text(d, (x0 + 60, y0 + 16), name, 15, TEXT, True)
    text(d, (x0 + 60, y0 + 37), dev, 10, SECOND)
    for i, line in enumerate(wrap(d, summary, 11, 204)[:3]):
        text(d, (x0 + 14, y0 + 64 + i * 16), line, 11, SECOND)
    pw = pill(d, (x0 + 14, y0 + 140), tags[0], tint,
              tuple(int(c + (255 - c) * 0.88) for c in tint))
    pill(d, (x0 + 14 + pw + 5, y0 + 140), tags[1], AMBER, (253, 244, 226))


resource_card(20, 646, "D", "Dopamine", "opa334", "面向 iOS 15 至 16.6.1 的免电脑越狱方案。", BRAND1, ("越狱", "★ 9.8千"))
resource_card(264, 646, "A", "AltStore", "Riley Testut", "用个人证书侧载 IPA 的经典方案，可自签续期。", MINT, ("侧载", "★ 1.4万"))

# ---------------- 最近更新 ----------------
text(d, (24, 856), "最近更新", 19, TEXT, True)
text(d, (24, 882), "数据来自各项目在 GitHub 的最后推送时间", 12, SECOND)

rr(d, (20, 906, 370, 1104), 22, fill=CARD, outline=STROKE)
rows = [
    ("T", "TrollStore", "巨魔商店 · 3 天前", VIOLET),
    ("P", "palera1n", "越狱工具 · 1 周前", BRAND1),
    ("I", "ipatool", "IPA 应用 · 2 周前", CYAN),
]
ry = 918
for i, (glyph, name, meta, tint) in enumerate(rows):
    icon_tile(d, (34, ry + 4), 44, tint, glyph)
    text(d, (90, ry + 8), name, 15, TEXT, True)
    text(d, (90, ry + 30), meta, 11, SECOND)
    text(d, (346, ry + 18), "›", 16, TERT, False, anchor="ra")
    ry += 62
    if i < len(rows) - 1:
        d.line([px(90), px(ry - 6), px(356), px(ry - 6)], fill=(238, 238, 242), width=px(1))

# ---------------- 数据说明 ----------------
rr(d, (20, 1126, 370, 1210), 16, fill=(236, 236, 245))
info_icon(d, 42, 1147, MINT)
text(d, (52, 1140), "数据来源说明", 12, TEXT, True)
for i, line in enumerate(wrap(d, "所有资源均指向官方仓库或开发者主页，不托管 IPA，标记「待验证」的字段尚未核对。", 10, 306)[:2]):
    text(d, (36, 1160 + i * 16), line, 10, SECOND)
text(d, (36, 1192), "上次联网刷新：12 分钟前", 10, TERT)

# ---------------- 品牌落款 ----------------
d.line([px(20), px(1236), px(370), px(1236)], fill=(228, 228, 234), width=px(1))

fy = 1252
logo_s = 22
w_title = d.textlength("iOS HUB", font=font(13, True)) / S
w_by = d.textlength("by nevercrynow", font=font(10, True)) / S
total_w = logo_s + 8 + w_title + 7 + w_by
bx = (W_PT - total_w) / 2
brand_logo(base, bx, fy, logo_s, 7)
text(d, (bx + logo_s + 8, fy + 4), "iOS HUB", 13, TEXT, True)
text(d, (bx + logo_s + 8 + w_title + 7, fy + 7), "by nevercrynow", 10, BRAND1, True)
text(d, (W_PT / 2, fy + 26), "用 SwiftUI 写的 iOS 生态资源浏览器 · 不托管安装包", 10, TERT, anchor="ma")

out = pathlib.Path("docs/mockup-home.png")
base.save(out)
print("saved", out, base.size)
