//
//  LinkRouter.swift
//  iOSHUB
//
//  线路：把 github.com / api.github.com 这类「国内时通时不通」的地址，
//  按用户选的规则改写成可访问的镜像地址。
//
//  设计原则与安卓端 LinkRouter.kt 完全一致：
//  - 只改写 GitHub 域名，其它站点原样打开，不做中间人。
//  - 「自动」= 启动时探测一次（直连 vs 镜像），把结果记在 lastGood。
//  - 任何时候都能在「我的 → 网络线路」里锁定某条线路或填自己的前缀。
//

import Foundation
import Combine

enum LinkRoute: String, CaseIterable, Identifiable {
    case auto
    case direct
    case ghProxy
    case ghFast
    case gitMirror
    case custom

    var id: String { rawValue }

    var label: String {
        switch self {
        case .auto:      return "自动"
        case .direct:    return "直连"
        case .ghProxy:   return "gh-proxy"
        case .ghFast:    return "ghfast"
        case .gitMirror: return "gitmirror"
        case .custom:    return "自定义"
        }
    }

    var prefix: String? {
        switch self {
        case .auto:      return nil
        case .direct:    return ""
        case .ghProxy:   return "https://gh-proxy.com/"
        case .ghFast:    return "https://ghfast.top/"
        case .gitMirror: return "https://hub.gitmirror.com/"
        case .custom:    return nil
        }
    }

    var note: String {
        switch self {
        case .auto:      return "探测一次，谁通走谁"
        case .direct:    return "不经过任何镜像"
        case .ghProxy:   return "最稳：页面 / 文件 / API 都代理"
        case .ghFast:    return "备用线路，部分网络不通"
        case .gitMirror: return "备用线路，偏文件下载"
        case .custom:    return "前缀 + 原始地址"
        }
    }
}

final class LinkRouter: ObservableObject {
    static let shared = LinkRouter()

    struct Health: Identifiable, Sendable {
        let route: LinkRoute
        let ok: Bool
        let millis: Int
        var id: String { route.rawValue }
        var detail: String {
            if ok { return "\(millis) ms" }
            return millis >= 5000 ? "超时" : "连接失败"
        }
    }

    private enum Key {
        static let route = "link.route"
        static let custom = "link.custom"
        static let lastGood = "link.lastGood"
    }

    /// 命中这些域名才会被改写。
    private static let proxyable: Set<String> = [
        "github.com",
        "www.github.com",
        "api.github.com",
        "raw.githubusercontent.com",
        "gist.githubusercontent.com",
        "objects.githubusercontent.com",
        "codeload.github.com",
        "avatars.githubusercontent.com",
    ]

    private let defaults: UserDefaults

    @Published var route: LinkRoute {
        didSet { defaults.set(route.rawValue, forKey: Key.route) }
    }

    @Published var customPrefix: String {
        didSet { defaults.set(customPrefix, forKey: Key.custom) }
    }

    @Published private(set) var lastGood: String?

    @Published private(set) var checking = false
    @Published private(set) var results: [Health] = []

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
        self.route = LinkRoute(rawValue: defaults.string(forKey: Key.route) ?? "") ?? .auto
        self.customPrefix = defaults.string(forKey: Key.custom) ?? ""
        self.lastGood = defaults.string(forKey: Key.lastGood)
    }

    // MARK: - 地址改写

    private static func host(of url: URL) -> String {
        url.host?.lowercased() ?? ""
    }

    static func isProxyable(_ url: URL) -> Bool {
        proxyable.contains(host(of: url))
    }

    /// 当前线路对应的前缀（"" 表示直连，nil 表示还没探测出来）。
    var currentPrefix: String? {
        switch route {
        case .auto:      return lastGood
        case .direct:    return ""
        case .custom:    return customPrefix.isEmpty ? nil : normalized(customPrefix)
        case .ghProxy, .ghFast, .gitMirror:
            return route.prefix
        }
    }

    private func normalized(_ prefix: String) -> String {
        prefix.hasSuffix("/") ? prefix : prefix + "/"
    }

    /// 镜像对会 302 跳回 github.com 的地址返回 403（例如 /releases/latest），
    /// 统一改成 releases 列表页，用户仍能点到具体版本。
    private static func normalizedForMirror(_ url: URL) -> URL {
        let text = url.absoluteString
        guard let range = text.range(of: "/releases/latest", options: .backwards) else {
            return url
        }
        return URL(string: text.replacingCharacters(in: range, with: "/releases")) ?? url
    }

    /// 把地址改写成「当前应该打开的那个」。同步、零网络，供点击时立即调用。
    static func resolved(_ url: URL) -> URL {
        let router = shared
        guard isProxyable(url) else { return url }
        guard let prefix = router.currentPrefix, !prefix.isEmpty else { return url }
        let target = normalizedForMirror(url)
        return URL(string: prefix + target.absoluteString) ?? url
    }

    /// GitHub API 的候选前缀，按优先级排列（先当前线路，再直连，再镜像）。
    func apiPrefixes() -> [String] {
        var ordered: [String] = []
        if let prefix = currentPrefix { ordered.append(prefix) }
        ordered.append("")
        ordered.append(contentsOf: ["https://gh-proxy.com/", "https://ghfast.top/", "https://hub.gitmirror.com/"])
        if !customPrefix.isEmpty { ordered.append(normalized(customPrefix)) }
        return Array(NSOrderedSet(array: ordered)) as? [String] ?? ordered
    }

    /// 给 GitHubAPI 用的前缀（不触发 @Published 更新，可安全跨线程读）。
    static var apiPrefix: String {
        shared.currentPrefix ?? ""
    }

    private func rememberGood(_ prefix: String) {
        lastGood = prefix
        defaults.set(prefix, forKey: Key.lastGood)
    }

    // MARK: - 探测

    private static func probe(_ url: URL, timeout: TimeInterval = 5) async -> (Bool, Int) {
        var request = URLRequest(url: url, timeoutInterval: timeout)
        request.httpMethod = "GET"
        request.setValue("iOSHUB/1.0", forHTTPHeaderField: "User-Agent")
        request.setValue("application/vnd.github+json", forHTTPHeaderField: "Accept")
        let started = Date()
        do {
            let (_, response) = try await URLSession.shared.data(for: request)
            let code = (response as? HTTPURLResponse)?.statusCode ?? 0
            let millis = Int(Date().timeIntervalSince(started) * 1000)
            return ((200...399).contains(code), millis)
        } catch {
            return (false, Int(Date().timeIntervalSince(started) * 1000))
        }
    }

    /// 逐个线路测速（并行），并把最快可用的一条记为 lastGood。
    @discardableResult
    func healthCheck() async -> [Health] {
        await MainActor.run { checking = true }
        let routes = LinkRoute.allCases.filter { route in
            if route == .custom { return !customPrefix.isEmpty }
            return route != .auto
        }
        let custom = normalized(customPrefix)
        let targets: [(LinkRoute, String)] = routes.map { route in
            (route, route.prefix ?? (route == .custom ? custom : ""))
        }
        var collected: [Health] = []
        await withTaskGroup(of: Health.self) { group in
            for (route, prefix) in targets {
                group.addTask {
                    let target = URL(string: prefix + "https://api.github.com/rate_limit")
                        ?? URL(string: "https://api.github.com/rate_limit")!
                    let (ok, millis) = await Self.probe(target)
                    return Health(route: route, ok: ok, millis: millis)
                }
            }
            for await health in group {
                collected.append(health)
            }
        }
        let sorted = collected.sorted { $0.millis < $1.millis }
        await MainActor.run {
            results = sorted
            checking = false
            guard route == .auto, let best = sorted.first(where: { $0.ok }) else { return }
            rememberGood(best.route.prefix ?? (best.route == .custom ? normalized(customPrefix) : ""))
        }
        return sorted
    }

    /// 启动时的静默探测：直连不通就切到 gh-proxy。
    func autoDetect() async {
        guard route == .auto else { return }
        let (ok, _) = await Self.probe(URL(string: "https://api.github.com/rate_limit")!, timeout: 4)
        if ok {
            await MainActor.run { rememberGood("") }
            return
        }
        let (mirrorOK, _) = await Self.probe(
            URL(string: "https://gh-proxy.com/https://api.github.com/rate_limit")!,
            timeout: 5
        )
        if mirrorOK {
            await MainActor.run { rememberGood("https://gh-proxy.com/") }
        }
    }
}
