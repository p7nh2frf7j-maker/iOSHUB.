//
//  ResourceItem.swift
//  iOSHUB
//
//  统一的资源数据模型。字段设计允许后期扩展：
//  新增字段请声明为可选（Optional），保证旧数据与远端数据可以继续解码。
//

import Foundation

/// 使用风险等级：帮助用户区分「官方渠道 / 社区来源 / 会修改系统 / 高风险」。
enum SecurityRisk: String, Codable, CaseIterable, Sendable {
    /// 官方渠道，风险最低
    case official
    /// 社区来源，需要自行判断
    case community
    /// 会修改系统或需要关闭系统安全机制
    case caution
    /// 高风险：未签名 IPA、漏洞利用工具等
    case high

    var title: String {
        switch self {
        case .official:  return "官方来源"
        case .community: return "社区来源"
        case .caution:   return "谨慎使用"
        case .high:      return "高风险"
        }
    }

    var detail: String {
        switch self {
        case .official:
            return "该资源由官方团队或开发者本人维护，来源可直接核对。"
        case .community:
            return "来自社区维护者，代码质量与更新节奏需要自行判断。"
        case .caution:
            return "该工具会修改系统文件或依赖系统漏洞，可能影响稳定性、保修与安全策略。"
        case .high:
            return "来源无法完整追溯或需要绕过系统保护，请仅在可丢弃的设备上尝试。"
        }
    }

    /// 排序用，数值越大越需要注意。
    var severity: Int {
        switch self {
        case .official:  return 0
        case .community: return 1
        case .caution:   return 2
        case .high:      return 3
        }
    }
}

/// 兼容性状态。无法确认时永远是 pending —— 不猜测、不编造。
enum CompatibilityState: String, Codable, CaseIterable, Sendable {
    /// 已在当前版本核对过
    case verified
    /// 部分设备 / 部分版本可用
    case partial
    /// 待验证
    case pending
    /// 已知不支持
    case unsupported

    var title: String {
        switch self {
        case .verified:    return "已验证"
        case .partial:     return "部分支持"
        case .pending:     return "待验证"
        case .unsupported: return "不支持"
        }
    }
}

struct ResourceItem: Identifiable, Codable, Hashable, Sendable {
    let id: String
    var name: String
    var category: ResourceCategory
    var summary: String
    var detail: String?

    /// SF Symbols 兜底图标；有 iconURL 时优先展示远程图标。
    var symbol: String
    var iconURL: URL?

    var version: String?
    var developer: String
    var developerURL: URL?

    var githubURL: URL?
    var downloadURL: URL?
    var sourceURL: URL
    var tutorialURL: URL?

    var supportedIOS: [String]
    var supportedDevices: [String]

    var releaseDate: Date?
    var lastUpdated: Date?
    /// 最后一次人工 / 自动核对来源的时间。nil 表示尚未核对。
    var lastVerified: Date?

    var compatibility: CompatibilityState
    /// 兼容性脚注，例如「仅 A12 及以上设备，请以官方说明为准」。
    var verificationNote: String?

    var risk: SecurityRisk
    var tags: [String]
    var changelog: String?
    var screenshots: [URL]

    var isFeatured: Bool

    // MARK: - 运行期增量字段（来自 GitHub API，不写入种子数据）

    var stars: Int?
    var forks: Int?
    var language: String?
    var latestRelease: String?
    var latestReleaseDate: Date?

    // MARK: - 展示辅助

    /// 未确认版本时不要展示编造的版本号。
    var versionText: String {
        if let version, !version.isEmpty { return version }
        return "待验证"
    }

    var isVersionVerified: Bool {
        if let version, !version.isEmpty { return true }
        return false
    }

    var supportedIOSText: String {
        if supportedIOS.isEmpty { return "待验证" }
        return supportedIOS.joined(separator: " / ")
    }

    var supportedDevicesText: String {
        if supportedDevices.isEmpty { return "待验证" }
        return supportedDevices.joined(separator: "、")
    }

    var updatedText: String {
        guard let lastUpdated else { return "待验证" }
        return RelativeTime.string(from: lastUpdated)
    }

    var releaseText: String {
        guard let releaseDate else { return "待验证" }
        return RelativeTime.string(from: releaseDate)
    }

    var verifiedText: String {
        guard let lastVerified else { return "尚未核对" }
        return RelativeTime.string(from: lastVerified)
    }

    /// 综合检索文本：全局搜索、分类内搜索都走这里。
    var searchText: String {
        var parts: [String] = [name, developer, summary, category.title, category.shortTitle]
        if let detail { parts.append(detail) }
        if let version { parts.append(version) }
        parts.append(contentsOf: tags)
        parts.append(contentsOf: supportedIOS)
        parts.append(contentsOf: supportedDevices)
        if let latestRelease { parts.append(latestRelease) }
        return parts.joined(separator: " ").lowercased()
    }

    /// 有可用外链时给出主行动按钮的文案。
    var primaryActionTitle: String {
        if downloadURL != nil { return "官方下载" }
        if githubURL != nil { return "打开 GitHub" }
        return "查看原始来源"
    }

    var primaryActionURL: URL {
        downloadURL ?? githubURL ?? sourceURL
    }
}

extension ResourceItem {
    /// 占位预览数据，仅用于 Xcode Preview 与 SwiftUI 画布，不参与运行时。
    static let preview = ResourceItem(
        id: "preview.trollstore",
        name: "TrollStore",
        category: .trollstore,
        summary: "永久签名安装 IPA 的巨魔商店，无需越狱即可长期保留应用。",
        detail: "TrollStore 利用 CoreTrust 漏洞为 IPA 做永久签名，安装后不会因证书过期而失效。",
        symbol: "shippingbox.fill",
        iconURL: nil,
        version: nil,
        developer: "Lars Fröder (opa334)",
        developerURL: URL(string: "https://github.com/opa334"),
        githubURL: URL(string: "https://github.com/opa334/TrollStore"),
        downloadURL: URL(string: "https://github.com/opa334/TrollStore/releases/latest"),
        sourceURL: URL(string: "https://github.com/opa334/TrollStore")!,
        tutorialURL: nil,
        supportedIOS: ["iOS 14.0 – 16.6.1"],
        supportedDevices: ["A12 及以上"],
        releaseDate: nil,
        lastUpdated: nil,
        lastVerified: nil,
        compatibility: .pending,
        verificationNote: "支持范围请以官方 Release 说明为准。",
        risk: .caution,
        tags: ["trollstore", "巨魔", "永久签名"],
        changelog: nil,
        screenshots: [],
        isFeatured: true,
        stars: nil,
        forks: nil,
        language: nil,
        latestRelease: nil,
        latestReleaseDate: nil
    )
}
