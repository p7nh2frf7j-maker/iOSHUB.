//
//  HTTPClient.swift
//  iOSHUB
//
//  极简 HTTP 层：只做请求、状态码归一化和错误语义化。
//  业务层（GitHubAPI / NewsLoader）拿到的是 Data，自己负责解码。
//

import Foundation

enum APIError: LocalizedError, Equatable {
    case invalidURL
    case offline
    case rateLimited(reset: Date?)
    case notFound
    case http(Int)
    case decoding(String)

    var errorDescription: String? {
        userMessage
    }

    /// 直接展示给用户的文案：说清楚发生了什么、能做什么。
    var userMessage: String {
        switch self {
        case .invalidURL:
            return "请求地址不合法。"
        case .offline:
            return "当前无法连接网络，已切换到内置资源库。"
        case .rateLimited(let reset):
            if let reset {
                return "GitHub 未认证请求已用尽（每小时 60 次），约 \(RelativeTime.string(from: reset)) 恢复。可在「我的 → 数据与网络」填写 Token 提升额度。"
            }
            return "GitHub 请求超出频率限制，请稍后再试，或填写个人 Token。"
        case .notFound:
            return "该资源在原始来源已不存在（404），可能已改名或删除。"
        case .http(let code):
            return "服务器返回异常状态码 \(code)。"
        case .decoding(let detail):
            return "数据解析失败：\(detail)"
        }
    }
}

struct HTTPClient: Sendable {
    var session: URLSession = .shared

    func get(
        _ url: URL,
        query: [URLQueryItem] = [],
        headers: [String: String] = [:]
    ) async throws -> Data {
        guard var components = URLComponents(url: url, resolvingAgainstBaseURL: false) else {
            throw APIError.invalidURL
        }
        if !query.isEmpty {
            components.queryItems = (components.queryItems ?? []) + query
        }
        guard let finalURL = components.url else { throw APIError.invalidURL }

        var request = URLRequest(url: finalURL, timeoutInterval: 25)
        request.httpMethod = "GET"
        request.setValue("iOSHUB/1.0 (iOS)", forHTTPHeaderField: "User-Agent")
        headers.forEach { request.setValue($1, forHTTPHeaderField: $0) }

        let data: Data
        let response: URLResponse
        do {
            (data, response) = try await session.data(for: request)
        } catch let urlError as URLError {
            switch urlError.code {
            case .notConnectedToInternet, .networkConnectionLost, .cannotFindHost,
                 .cannotConnectToHost, .timedOut, .dataNotAllowed:
                throw APIError.offline
            default:
                throw APIError.offline
            }
        } catch {
            throw APIError.offline
        }

        guard let http = response as? HTTPURLResponse else { throw APIError.http(0) }

        switch http.statusCode {
        case 200..<300:
            return data
        case 404:
            throw APIError.notFound
        case 403, 429:
            let remaining = http.value(forHTTPHeaderField: "X-RateLimit-Remaining")
            if remaining == "0" || http.statusCode == 429 {
                let reset = http.value(forHTTPHeaderField: "X-RateLimit-Reset")
                    .flatMap(Int.init)
                    .map { Date(timeIntervalSince1970: TimeInterval($0)) }
                throw APIError.rateLimited(reset: reset)
            }
            throw APIError.http(http.statusCode)
        default:
            throw APIError.http(http.statusCode)
        }
    }

    func decode<T: Decodable>(_ type: T.Type, from data: Data) throws -> T {
        do {
            return try JSONCoding.decoder.decode(type, from: data)
        } catch {
            let text = String(describing: error)
            throw APIError.decoding(String(text.prefix(180)))
        }
    }
}
