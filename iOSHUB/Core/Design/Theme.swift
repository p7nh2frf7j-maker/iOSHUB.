//
//  Theme.swift
//  iOSHUB
//
//  设计系统单一来源：颜色、圆角、间距、字体、分类色。
//  所有视图只引用这里的 token，禁止在视图里写魔法数字和散落颜色。
//

import SwiftUI
import UIKit

enum Palette {
    // 品牌色：深蓝紫 → 紫罗兰，用于 Hero、选中态、主按钮
    static let brand = Color(red: 0.33, green: 0.40, blue: 0.98)
    static let brandDeep = Color(red: 0.47, green: 0.25, blue: 0.93)
    static let violet = Color(red: 0.63, green: 0.42, blue: 0.98)
    static let cyan = Color(red: 0.22, green: 0.78, blue: 0.92)
    static let mint = Color(red: 0.16, green: 0.76, blue: 0.60)
    static let amber = Color(red: 0.97, green: 0.68, blue: 0.22)
    static let rose = Color(red: 0.95, green: 0.36, blue: 0.46)
    static let slate = Color(red: 0.42, green: 0.47, blue: 0.58)

    static let pageBackground = Color(uiColor: .systemGroupedBackground)
    static let cardBackground = Color(uiColor: .secondarySystemGroupedBackground)
    static let stroke = Color.primary.opacity(0.07)
    static let secondaryText = Color.secondary

    static let brandGradient = LinearGradient(
        colors: [brand, brandDeep, violet],
        startPoint: .topLeading,
        endPoint: .bottomTrailing
    )

    static let subtleGradient = LinearGradient(
        colors: [Color.primary.opacity(0.05), Color.primary.opacity(0.01)],
        startPoint: .topLeading,
        endPoint: .bottomTrailing
    )
}

enum Metric {
    static let screenPadding: CGFloat = 20
    static let cardRadius: CGFloat = 22
    static let compactRadius: CGFloat = 14
    static let sectionSpacing: CGFloat = 26
    static let iconSize: CGFloat = 54
    static let rowSpacing: CGFloat = 12
}

enum Typography {
    static let hero = Font.system(size: 27, weight: .bold, design: .rounded)
    static let sectionTitle = Font.system(size: 19, weight: .semibold)
    static let cardTitle = Font.system(size: 16, weight: .semibold)
    static let body = Font.system(size: 15, weight: .regular)
    static let caption = Font.system(size: 12, weight: .medium)
    static let micro = Font.system(size: 11, weight: .semibold)
    static let number = Font.system(size: 15, weight: .semibold, design: .rounded)
}

extension ResourceCategory {
    var tint: Color {
        switch self {
        case .jailbreak:   return Palette.brand
        case .trollstore:  return Palette.violet
        case .ipa:         return Palette.cyan
        case .sideload:    return Palette.mint
        case .tweak:       return Palette.rose
        case .repo:        return Palette.amber
        case .openSource:  return Palette.slate
        case .security:    return Color(red: 0.85, green: 0.30, blue: 0.32)
        case .devTools:    return Color(red: 0.30, green: 0.55, blue: 0.95)
        case .theming:     return Color(red: 0.72, green: 0.40, blue: 0.90)
        case .utilities:   return Color(red: 0.22, green: 0.66, blue: 0.63)
        case .port:        return Color(red: 1.00, green: 0.42, blue: 0.29)
        case .news:        return Color(red: 0.45, green: 0.45, blue: 0.50)
        }
    }
}

extension SecurityRisk {
    var tint: Color {
        switch self {
        case .official:  return Palette.mint
        case .community: return Palette.brand
        case .caution:   return Palette.amber
        case .high:      return Palette.rose
        }
    }

    var symbol: String {
        switch self {
        case .official:  return "checkmark.seal.fill"
        case .community: return "person.2.fill"
        case .caution:   return "exclamationmark.triangle.fill"
        case .high:      return "exclamationmark.octagon.fill"
        }
    }
}

extension CompatibilityState {
    var tint: Color {
        switch self {
        case .verified:    return Palette.mint
        case .partial:     return Palette.amber
        case .pending:     return Palette.slate
        case .unsupported: return Palette.rose
        }
    }

    var symbol: String {
        switch self {
        case .verified:    return "checkmark.circle.fill"
        case .partial:     return "circle.lefthalf.filled"
        case .pending:     return "questionmark.circle.fill"
        case .unsupported: return "xmark.circle.fill"
        }
    }
}
