//
//  ResourceCategory.swift
//  iOSHUB
//
//  13 个资源分类。分类本身是纯数据，不依赖 SwiftUI，
//  颜色 / 图标等视觉属性放在 Design 层的扩展里。
//

import Foundation

enum ResourceCategory: String, Codable, CaseIterable, Identifiable, Sendable {
    case jailbreak
    case trollstore
    case ipa
    case sideload
    case tweak
    case repo
    case openSource
    case security
    case devTools
    case theming
    case utilities
    case port
    case news

    var id: String { rawValue }

    var title: String {
        switch self {
        case .jailbreak:   return "越狱工具"
        case .trollstore:  return "巨魔商店"
        case .ipa:         return "IPA 应用"
        case .sideload:    return "侧载工具"
        case .tweak:       return "越狱插件"
        case .repo:        return "软件源"
        case .openSource:  return "开源项目"
        case .security:    return "漏洞研究"
        case .devTools:    return "开发工具"
        case .theming:     return "系统美化"
        case .utilities:   return "实用工具"
        case .port:        return "移植与模拟"
        case .news:        return "资讯来源"
        }
    }

    /// 分类页副标题，一行为限。
    var blurb: String {
        switch self {
        case .jailbreak:   return "无需电脑或半自动的越狱方案"
        case .trollstore:  return "永久签名安装，不越狱也能装"
        case .ipa:         return "侧载、备份与安装工具"
        case .sideload:    return "电脑端 / 移动端签名安装"
        case .tweak:       return "注入类插件与运行时框架"
        case .repo:        return "Cydia / Sileo / Zebra 软件源"
        case .openSource:  return "可学习、可复用的 iOS 开源代码"
        case .security:    return "系统公告、漏洞分析与研究资料"
        case .devTools:    return "构建、调试与设备通信工具"
        case .theming:     return "图标、状态栏与系统观感定制"
        case .utilities:   return "日常效率与设备管理"
        case .port:        return "手机跑主机 / PC 游戏的现实与边界"
        case .news:        return "官方与社区的信息源"
        }
    }

    /// SF Symbols 图标名（全部为 iOS 17 起可用的稳定符号）。
    var symbol: String {
        switch self {
        case .jailbreak:   return "lock.open.fill"
        case .trollstore:  return "shippingbox.fill"
        case .ipa:         return "tray.and.arrow.down.fill"
        case .sideload:    return "iphone.and.arrow.forward"
        case .tweak:       return "ant.fill"
        case .repo:        return "cylinder.split.1x2.fill"
        case .openSource:  return "chevron.left.forwardslash.chevron.right"
        case .security:    return "shield.lefthalf.filled"
        case .devTools:    return "hammer.fill"
        case .theming:     return "paintpalette.fill"
        case .utilities:   return "wrench.and.screwdriver.fill"
        case .port:        return "gamecontroller.fill"
        case .news:        return "newspaper.fill"
        }
    }

    /// 首页快捷入口里使用的短标题。
    var shortTitle: String {
        switch self {
        case .jailbreak:   return "越狱"
        case .trollstore:  return "巨魔"
        case .ipa:         return "IPA"
        case .sideload:    return "侧载"
        case .tweak:       return "插件"
        case .repo:        return "软件源"
        case .openSource:  return "开源"
        case .security:    return "漏洞"
        case .devTools:    return "开发"
        case .theming:     return "美化"
        case .utilities:   return "工具"
        case .port:        return "移植"
        case .news:        return "资讯"
        }
    }

    /// 在 GitHub 上检索该分类时使用的查询词。
    var githubKeywords: [String] {
        switch self {
        case .jailbreak:   return ["jailbreak ios", "ios jailbreak tool"]
        case .trollstore:  return ["trollstore", "trollstore ipa installer"]
        case .ipa:         return ["ipa installer ios", "ios ipa sideload"]
        case .sideload:    return ["sideload ios", "altstore sidestore"]
        case .tweak:       return ["ios tweak theos", "ios tweak injection"]
        case .repo:        return ["ios package manager", "cydia repo"]
        case .openSource:  return ["swiftui", "ios open source"]
        case .security:    return ["ios security research", "ios exploit"]
        case .devTools:    return ["ios build tool", "ios device library"]
        case .theming:     return ["ios theme tweak", "ios icon theme"]
        case .utilities:   return ["ios utility app", "ios tools"]
        case .port:        return ["ios game port", "ios emulator"]
        case .news:        return ["ios news feed"]
        }
    }

    /// 需要额外安全提示的分类。
    var needsRiskNotice: Bool {
        switch self {
        case .jailbreak, .trollstore, .ipa, .tweak, .security:
            return true
        default:
            return false
        }
    }
}
