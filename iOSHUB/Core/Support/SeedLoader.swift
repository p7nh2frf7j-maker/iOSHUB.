//
//  AppSettings.swift + SeedLoader.swift
//  iOSHUB
//
//  本地设置项与内置资源库读取。
//

import Foundation

enum AppSettings {
    static let githubTokenKey = "settings.githubToken"
    static let userIOSVersionKey = "settings.userIOSVersion"
    static let userDeviceKey = "settings.userDevice"
    static let notifyUpdatesKey = "settings.notifyUpdates"
    static let lastRemoteRefreshKey = "settings.lastRemoteRefresh"

    static var githubToken: String {
        UserDefaults.standard.string(forKey: githubTokenKey)?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
    }

    static var userIOSVersion: String {
        UserDefaults.standard.string(forKey: userIOSVersionKey) ?? "未设置"
    }

    static var userDevice: String {
        UserDefaults.standard.string(forKey: userDeviceKey) ?? "未设置"
    }
}

enum SeedError: LocalizedError {
    case missingFile(String)
    case decodeFailed(String, String)

    var errorDescription: String? {
        switch self {
        case .missingFile(let name):
            return "内置数据文件缺失：\(name).json"
        case .decodeFailed(let name, let detail):
            return "内置数据解析失败（\(name).json）：\(detail)"
        }
    }
}

enum SeedLoader {
    static func resources() throws -> [ResourceItem] {
        try decode("seed-resources")
    }

    static func news() throws -> [NewsItem] {
        try decode("seed-news")
    }

    private static func decode<T: Decodable>(_ name: String) throws -> T {
        guard let url = Bundle.main.url(forResource: name, withExtension: "json") else {
            throw SeedError.missingFile(name)
        }
        let data = try Data(contentsOf: url)
        do {
            return try JSONCoding.decoder.decode(T.self, from: data)
        } catch {
            throw SeedError.decodeFailed(name, String(describing: error))
        }
    }
}
