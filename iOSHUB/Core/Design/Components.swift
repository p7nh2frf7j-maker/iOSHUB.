//
//  Components.swift
//  iOSHUB
//
//  可复用的原子组件：卡片表面、标签胶囊、区块标题、搜索入口、
//  远程图标、空态 / 加载 / 错误态。
//

import SwiftUI

// MARK: - 卡片表面

struct CardSurface: ViewModifier {
    var padding: CGFloat = 16
    var radius: CGFloat = Metric.cardRadius

    func body(content: Content) -> some View {
        content
            .padding(padding)
            .background(Palette.cardBackground, in: RoundedRectangle(cornerRadius: radius, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: radius, style: .continuous)
                    .strokeBorder(Palette.stroke, lineWidth: 1)
            )
            .shadow(color: Color.black.opacity(0.05), radius: 16, x: 0, y: 8)
    }
}

extension View {
    func cardSurface(padding: CGFloat = 16, radius: CGFloat = Metric.cardRadius) -> some View {
        modifier(CardSurface(padding: padding, radius: radius))
    }

    /// 页面统一左右边距
    func screenPadding() -> some View {
        padding(.horizontal, Metric.screenPadding)
    }
}

// MARK: - 胶囊标签

struct Pill: View {
    let text: String
    var symbol: String? = nil
    var tint: Color = Palette.brand
    var filled: Bool = false

    var body: some View {
        HStack(spacing: 4) {
            if let symbol {
                Image(systemName: symbol)
                    .font(.system(size: 10, weight: .bold))
            }
            Text(text)
                .font(Typography.micro)
        }
        .foregroundStyle(filled ? Color.white : tint)
        .padding(.horizontal, 9)
        .padding(.vertical, 5)
        .background(
            Capsule(style: .continuous)
                .fill(filled ? tint : tint.opacity(0.12))
        )
    }
}

// MARK: - 区块标题

struct SectionHeader<Accessory: View>: View {
    let title: String
    var subtitle: String? = nil
    @ViewBuilder var accessory: () -> Accessory

    var body: some View {
        HStack(alignment: .firstTextBaseline) {
            VStack(alignment: .leading, spacing: 3) {
                Text(title)
                    .font(Typography.sectionTitle)
                    .foregroundStyle(.primary)
                if let subtitle {
                    Text(subtitle)
                        .font(Typography.caption)
                        .foregroundStyle(.secondary)
                }
            }
            Spacer(minLength: 8)
            accessory()
        }
    }
}

extension SectionHeader where Accessory == EmptyView {
    init(title: String, subtitle: String? = nil) {
        self.title = title
        self.subtitle = subtitle
        self.accessory = { EmptyView() }
    }
}

// MARK: - 搜索入口（外观即入口，点击后进入真正的搜索页）

struct SearchEntry: View {
    var placeholder: String = "搜索工具、插件、IPA、开发者或 iOS 版本"

    var body: some View {
        HStack(spacing: 10) {
            Image(systemName: "magnifyingglass")
                .font(.system(size: 15, weight: .semibold))
                .foregroundStyle(.secondary)
            Text(placeholder)
                .font(Typography.body)
                .foregroundStyle(.secondary)
                .lineLimit(1)
            Spacer(minLength: 0)
            Image(systemName: "slider.horizontal.3")
                .font(.system(size: 14, weight: .semibold))
                .foregroundStyle(Palette.brand)
        }
        .padding(.horizontal, 14)
        .frame(height: 46)
        .background(
            RoundedRectangle(cornerRadius: 15, style: .continuous)
                .fill(Palette.cardBackground)
        )
        .overlay(
            RoundedRectangle(cornerRadius: 15, style: .continuous)
                .strokeBorder(Palette.stroke, lineWidth: 1)
        )
        .contentShape(RoundedRectangle(cornerRadius: 15, style: .continuous))
    }
}

// MARK: - 远程图标（失败时回落到 SF Symbols）

struct RemoteIcon: View {
    let url: URL?
    let symbol: String
    var size: CGFloat = Metric.iconSize
    var tint: Color = Palette.brand

    private var shape: RoundedRectangle {
        RoundedRectangle(cornerRadius: size * 0.28, style: .continuous)
    }

    var body: some View {
        Group {
            if let url {
                AsyncImage(url: url) { phase in
                    switch phase {
                    case .success(let image):
                        image.resizable().scaledToFill()
                    default:
                        placeholder
                    }
                }
            } else {
                placeholder
            }
        }
        .frame(width: size, height: size)
        .clipShape(shape)
        .overlay(shape.strokeBorder(Palette.stroke, lineWidth: 1))
    }

    private var placeholder: some View {
        ZStack {
            LinearGradient(
                colors: [tint.opacity(0.22), tint.opacity(0.07)],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
            Image(systemName: symbol)
                .font(.system(size: size * 0.40, weight: .semibold))
                .foregroundStyle(tint)
        }
    }
}

// MARK: - 状态视图

struct EmptyStateView: View {
    let symbol: String
    let title: String
    let message: String
    var actionTitle: String? = nil
    var action: (() -> Void)? = nil

    var body: some View {
        VStack(spacing: 10) {
            Image(systemName: symbol)
                .font(.system(size: 30, weight: .regular))
                .foregroundStyle(.tertiary)
            Text(title)
                .font(Typography.cardTitle)
            Text(message)
                .font(Typography.caption)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
                .fixedSize(horizontal: false, vertical: true)
            if let actionTitle, let action {
                Button(actionTitle, action: action)
                    .font(Typography.caption)
                    .buttonStyle(.borderedProminent)
                    .controlSize(.small)
                    .padding(.top, 2)
            }
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 26)
        .padding(.horizontal, 18)
    }
}

struct LoadingRow: View {
    var text: String = "正在加载…"

    var body: some View {
        HStack(spacing: 10) {
            ProgressView()
            Text(text)
                .font(Typography.caption)
                .foregroundStyle(.secondary)
            Spacer(minLength: 0)
        }
        .padding(.vertical, 14)
    }
}

/// 网络失败 / 降级提示条。不阻断阅读，只说明「这部分数据现在拿不到」。
struct NoticeBanner: View {
    let message: String
    var symbol: String = "wifi.exclamationmark"
    var tint: Color = Palette.amber
    var retry: (() -> Void)? = nil

    var body: some View {
        HStack(alignment: .top, spacing: 10) {
            Image(systemName: symbol)
                .font(.system(size: 13, weight: .semibold))
                .foregroundStyle(tint)
            Text(message)
                .font(Typography.caption)
                .foregroundStyle(.secondary)
                .fixedSize(horizontal: false, vertical: true)
            Spacer(minLength: 4)
            if let retry {
                Button("重试", action: retry)
                    .font(Typography.micro)
                    .foregroundStyle(tint)
            }
        }
        .padding(12)
        .background(tint.opacity(0.10), in: RoundedRectangle(cornerRadius: 14, style: .continuous))
    }
}
