"""渲染详情页「项目 README」区块的设计稿。

运行：<bundled python> tools/render_readme_card.py
输出：docs/mockup-readme.png
"""
from __future__ import annotations

import pathlib

from PIL import Image, ImageDraw, ImageFont

S = 2
PAGE = (242, 242, 247)
CARD = (255, 255, 255)
TEXT = (17, 17, 20)
SECOND = (120, 120, 128)
TERT = (176, 176, 184)
STROKE = (231, 231, 236)
BRAND1 = (84, 102, 250)
BRAND2 = (120, 63, 240)
MINT = (41, 194, 153)

W_PT, H_PT = 390, 380


def px(v: float) -> int:
    return int(round(v * S))


_fonts: dict[tuple[bool, int], ImageFont.FreeTypeFont] = {}


def font(size: int, bold: bool = False) -> ImageFont.FreeTypeFont:
    key = (bold, size)
    if key not in _fonts:
        name = "msyhbd.ttc" if bold else "msyh.ttc"
        _fonts[key] = ImageFont.truetype(str(pathlib.Path("C:/Windows/Fonts") / name), size * S)
    return _fonts[key]


def text(d, xy, s, size, fill=TEXT, bold=False, anchor="la"):
    d.text((px(xy[0]), px(xy[1])), s, font=font(size, bold), fill=fill, anchor=anchor)


def rr(d, box, radius, fill=None, outline=None, width=1):
    d.rounded_rectangle(
        [px(box[0]), px(box[1]), px(box[2]), px(box[3])],
        radius=px(radius), fill=fill, outline=outline,
        width=px(width) if outline else 0,
    )


def wrap(d, s, size, max_w, bold=False):
    f = font(size, bold)
    lines, cur = [], ""
    for ch in s:
        if d.textlength(cur + ch, font=f) / S > max_w and cur:
            lines.append(cur)
            cur = ch
        else:
            cur += ch
    if cur:
        lines.append(cur)
    if len(lines) > 1 and lines[-1] in "，。、；：！？":
        lines[-2] += lines[-1]
        lines.pop()
    return lines


def pill(d, xy, label, fg, bg, size=9):
    f = font(size, True)
    w = d.textlength(label, font=f) / S + 16
    h = size + 10
    rr(d, (xy[0], xy[1], xy[0] + w, xy[1] + h), h / 2, fill=bg)
    text(d, (xy[0] + 8, xy[1] + 4), label, size, fg, True)
    return w


base = Image.new("RGB", (px(W_PT), px(H_PT)), PAGE)
d = ImageDraw.Draw(base)

rr(d, (20, 20, 370, 360), 20, fill=CARD, outline=STROKE)

# 区块标题
text(d, (36, 34), "项目 README", 16, TEXT, True)
x = 36 + d.textlength("项目 README", font=font(16, True)) / S + 10
pill(d, (x, 36), "GitHub 实时", MINT, (231, 248, 243))

y = 66

# H1
text(d, (36, y), "TrollStore", 17, TEXT, True)
y += 28

for line in wrap(d, "利用 CoreTrust 签名校验缺陷，让已签名的 IPA 在设备上长期有效，安装后不会像企业签那样掉签。", 12, 296):
    text(d, (36, y), line, 12, SECOND)
    y += 19

y += 6
text(d, (36, y), "安装步骤", 15, TEXT, True)
y += 25

for index, item in enumerate(("下载 TrollStore 安装包", "用 TrollStore Helper 安装", "重启设备后打开 TrollStore"), start=1):
    text(d, (36, y + 1), f"{index}.", 11, BRAND1, True)
    text(d, (54, y), item, 12, SECOND)
    y += 20

y += 6
text(d, (36, y), "支持范围", 15, TEXT, True)
y += 24

for item in ("iOS 14.0 – 16.6.1", "A12 及以上设备为主"):
    d.ellipse([px(38), px(y + 5), px(42), px(y + 9)], fill=BRAND1)
    text(d, (52, y), item, 12, SECOND)
    y += 20

y += 6
rr(d, (36, y, 354, y + 34), 10, fill=(246, 246, 249))
text(d, (46, y + 10), "git clone https://github.com/opa334/TrollStore", 10, TEXT)

y += 46
text(d, (36, y), "↗  在 GitHub 查看完整 README", 11, BRAND1, True)

out = pathlib.Path("docs/mockup-readme.png")
base.save(out)
print("saved", out, base.size)
