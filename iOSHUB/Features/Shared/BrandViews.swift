//
//  BrandViews.swift
//  iOSHUB
//
//  品牌资产：Logo 标记、署名行、页脚、启动画面。
//  同一个标记在图标、启动页、首页头部、我的页脚四处复用，
//  保证「iOS HUB by nevercrynow」在任何截图中都完整出现。
//

import SwiftUI

// MARK: - Logo 标记

/// 三层阶梯叠片的几何（单位方块，0…1），与 Android 侧 `BrandMark.kt` 完全一致。
private enum HubMarkGeometry {
    struct Slab {
        let x0: CGFloat
        let x1: CGFloat
        let y: CGFloat
        let opacity: Double
    }

    static let slabs: [Slab] = [
        Slab(x0: 0.235, x1: 0.585, y: 0.265, opacity: 1.00),
        Slab(x0: 0.330, x1: 0.720, y: 0.437, opacity: 1.00),
        Slab(x0: 0.425, x1: 0.765, y: 0.609, opacity: 0.94),
    ]

    static let slabHeight: CGFloat = 0.112
    static let slabCorner: CGFloat = 0.056
    static let tileCorner: CGFloat = 0.300
}

/// 品牌标记：品牌渐变圆角方块 + 三层阶梯白色叠片。
/// 图形与 AppIcon（Assets.xcassets/AppIcon）逐像素对应。
struct BrandLogo: View {
    var size: CGFloat = 42
    var showsShadow: Bool = true
    /// 0…1，用于启动动画里逐条「画出来」。
    var progress: CGFloat = 1

    var body: some View {
        ZStack {
            RoundedRectangle(cornerRadius: size * HubMarkGeometry.tileCorner, style: .continuous)
                .fill(Palette.brandGradient)
                .overlay(
                    // 左上柔光，避免整块色板发死
                    RoundedRectangle(cornerRadius: size * HubMarkGeometry.tileCorner, style: .continuous)
                        .fill(
                            RadialGradient(
                                colors: [.white.opacity(0.22), .clear],
                                center: UnitPoint(x: 0.26, y: 0.16),
                                startRadius: 0,
                                endRadius: size * 0.85
                            )
                        )
                )

            Canvas { context, canvasSize in
                let unit = min(canvasSize.width, canvasSize.height)
                for (index, slab) in HubMarkGeometry.slabs.enumerated() {
                    let grow = max(0, min(1, (progress - CGFloat(index) * 0.18) / 0.62))
                    guard grow > 0.001 else { continue }
                    let height = HubMarkGeometry.slabHeight * unit
                    let width = (slab.x1 - slab.x0) * grow * unit
                    let rect = CGRect(
                        x: slab.x0 * unit,
                        y: slab.y * unit,
                        width: width,
                        height: height
                    )
                    let radius = min(HubMarkGeometry.slabCorner * unit, min(height, width) / 2)
                    let path = Path(roundedRect: rect, cornerRadius: radius, style: .continuous)
                    context.fill(path, with: .color(.white.opacity(slab.opacity)))
                }
            }
        }
        .frame(width: size, height: size)
        .drawingGroup()
        .shadow(
            color: showsShadow ? Palette.brand.opacity(0.30) : .clear,
            radius: size * 0.22,
            x: 0,
            y: size * 0.12
        )
        .accessibilityHidden(true)
    }
}

// MARK: - 署名行

/// 「iOS HUB by nevercrynow」横排署名。
struct BrandSignature: View {
    var titleSize: CGFloat = 27
    var showsTitle: Bool = true

    private var bylineSize: CGFloat {
        max(9, min(12, titleSize * 0.37))
    }

    var body: some View {
        HStack(alignment: .firstTextBaseline, spacing: 7) {
            if showsTitle {
                Text("iOS HUB")
                    .font(.system(size: titleSize, weight: .bold, design: .rounded))
                    .foregroundStyle(.primary)
            }
            Text("by nevercrynow")
                .font(.system(size: bylineSize, weight: .semibold, design: .rounded))
                .foregroundStyle(Palette.brand.opacity(0.9))
                .kerning(0.2)
        }
        .accessibilityElement(children: .combine)
        .accessibilityLabel("iOS HUB by nevercrynow")
    }
}

// MARK: - 页脚

/// 各页底部的品牌落款。
struct BrandFooter: View {
    var note: String = "用 SwiftUI 写的 iOS 生态资源浏览器 · 不托管安装包"

    var body: some View {
        VStack(spacing: 7) {
            HStack(spacing: 8) {
                BrandLogo(size: 22, showsShadow: false)
                Text("iOS HUB")
                    .font(.system(size: 13, weight: .bold, design: .rounded))
                Text("by nevercrynow")
                    .font(.system(size: 10, weight: .semibold, design: .rounded))
                    .foregroundStyle(Palette.brand.opacity(0.85))
            }
            Text(note)
                .font(Typography.micro)
                .foregroundStyle(.tertiary)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 8)
    }
}

// MARK: - 启动画面

/// 冷启动时的品牌过渡页，约 1.5 秒：
/// 光晕 → 图标弹入 → 叠片逐条绘出 → 字样上浮 → 进度条 → 整体淡出。
/// 系统 LaunchScreen 仍是纯色底（INFOPLIST_KEY_UILaunchScreen_Generation 生成），
/// 这一层负责把「iOS HUB by nevercrynow」补上。
struct LaunchBrandView: View {
    var onFinished: (() -> Void)? = nil

    @State private var markIn = false
    @State private var markProgress: CGFloat = 0
    @State private var wordIn = false
    @State private var byIn = false
    @State private var tagIn = false
    @State private var dedicationIn = false
    @State private var barProgress: CGFloat = 0
    @State private var leaving = false

    var body: some View {
        ZStack {
            Palette.pageBackground.ignoresSafeArea()

            Circle()
                .fill(Palette.brand.opacity(0.16))
                .frame(width: 320, height: 320)
                .blur(radius: 88)
                .scaleEffect(markIn ? 1 : 0.86)
                .offset(y: -60)

            VStack(spacing: 0) {
                BrandLogo(size: 96, progress: markProgress)
                    .scaleEffect(markIn ? 1 : 0.72)
                    .opacity(markIn ? 1 : 0)

                VStack(spacing: 6) {
                    Text("iOS HUB")
                        .font(.system(size: 34, weight: .bold, design: .rounded))
                        .foregroundStyle(.primary)

                    Text("by nevercrynow")
                        .font(.system(size: 13, weight: .semibold, design: .rounded))
                        .foregroundStyle(Palette.brand)
                        .kerning(0.5)
                        .opacity(byIn ? 1 : 0)
                        .offset(y: byIn ? 0 : 8)
                }
                .padding(.top, 20)
                .opacity(wordIn ? 1 : 0)
                .offset(y: wordIn ? 0 : 14)

                Text("全生态 iOS 资源中心")
                    .font(Typography.caption)
                    .foregroundStyle(.secondary)
                    .padding(.top, 8)
                    .opacity(tagIn ? 1 : 0)
                    .offset(y: tagIn ? 0 : 10)

                HStack(spacing: 7) {
                    Text("献给")
                        .font(.system(size: 10, weight: .medium))
                        .kerning(2)
                        .foregroundStyle(.secondary.opacity(0.75))
                    Text("赵菁然")
                        .font(.system(size: 15, weight: .bold))
                        .kerning(1.2)
                        .foregroundStyle(Palette.rose)
                    Image(systemName: "heart.fill")
                        .font(.system(size: 10))
                        .foregroundStyle(Palette.rose.opacity(0.85))
                }
                .padding(.top, 26)
                .opacity(dedicationIn ? 1 : 0)
                .offset(y: dedicationIn ? 0 : 10)
            }

            VStack {
                Spacer()

                VStack(spacing: 14) {
                    GeometryReader { proxy in
                        ZStack(alignment: .leading) {
                            Capsule()
                                .fill(Color.primary.opacity(0.10))
                            Capsule()
                                .fill(Palette.brandGradient)
                                .frame(width: proxy.size.width * barProgress)
                        }
                    }
                    .frame(width: 132, height: 3)

                    Text("NEVERCRYNOW")
                        .font(.system(size: 9, weight: .semibold, design: .rounded))
                        .kerning(3.4)
                        .foregroundStyle(.tertiary)
                }
                .padding(.bottom, 46)
                .opacity(barProgress > 0.02 ? 1 : 0)
            }
        }
        .scaleEffect(leaving ? 1.06 : 1)
        .opacity(leaving ? 0 : 1)
        .ignoresSafeArea()
        .onAppear { play() }
    }

    private func play() {
        withAnimation(.spring(response: 0.62, dampingFraction: 0.78)) {
            markIn = true
        }
        withAnimation(.easeOut(duration: 0.7).delay(0.17)) {
            markProgress = 1
        }
        withAnimation(.easeOut(duration: 0.34).delay(0.39)) {
            wordIn = true
        }
        withAnimation(.easeOut(duration: 0.30).delay(0.54)) {
            byIn = true
        }
        withAnimation(.easeOut(duration: 0.30).delay(0.67)) {
            tagIn = true
        }
        withAnimation(.linear(duration: 1.15).delay(0.10)) {
            barProgress = 1
        }
        // 1.42 秒后整体放大淡出，交回内容
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.42) {
            withAnimation(.easeIn(duration: 0.34)) {
                leaving = true
            }
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.34) {
                onFinished?()
            }
        }
    }
}

#Preview {
    VStack(spacing: 24) {
        BrandSignature()
        BrandFooter()
        BrandLogo(size: 96)
    }
    .padding()
}
