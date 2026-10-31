//
//  Persistence.swift
//  iOSHUB
//
//  SwiftData 模型与容器。
//  只存「用户产生的数据」（收藏、浏览历史、搜索历史），
//  资源目录本身来自种子 JSON + 远端，避免数据双写不一致。
//

import Foundation
import SwiftData

@Model
final class FavoriteRecord {
    @Attribute(.unique) var resourceID: String
    var name: String
    var categoryRaw: String
    var createdAt: Date
    /// 完整资源快照：即使远端条目下线，收藏夹仍能展示。
    var payload: Data

    init(item: ResourceItem) {
        self.resourceID = item.id
        self.name = item.name
        self.categoryRaw = item.category.rawValue
        self.createdAt = Date()
        self.payload = (try? JSONCoding.encoder.encode(item)) ?? Data()
    }

    var item: ResourceItem? {
        try? JSONCoding.decoder.decode(ResourceItem.self, from: payload)
    }

    var category: ResourceCategory {
        ResourceCategory(rawValue: categoryRaw) ?? .utilities
    }
}

@Model
final class HistoryRecord {
    @Attribute(.unique) var resourceID: String
    var name: String
    var viewedAt: Date
    var payload: Data

    init(item: ResourceItem) {
        self.resourceID = item.id
        self.name = item.name
        self.viewedAt = Date()
        self.payload = (try? JSONCoding.encoder.encode(item)) ?? Data()
    }

    var item: ResourceItem? {
        try? JSONCoding.decoder.decode(ResourceItem.self, from: payload)
    }
}

@Model
final class SearchQueryRecord {
    @Attribute(.unique) var text: String
    var lastUsed: Date
    var hitCount: Int

    init(text: String, lastUsed: Date = Date()) {
        self.text = text
        self.lastUsed = lastUsed
        self.hitCount = 1
    }
}

enum PersistenceController {
    static let shared: ModelContainer = {
        let schema = Schema([
            FavoriteRecord.self,
            HistoryRecord.self,
            SearchQueryRecord.self
        ])

        if let container = try? ModelContainer(
            for: schema,
            configurations: [ModelConfiguration(schema: schema, isStoredInMemoryOnly: false)]
        ) {
            return container
        }

        // 迁移失败 / 沙盒异常时退回内存库，保证 App 仍可启动。
        if let container = try? ModelContainer(
            for: schema,
            configurations: [ModelConfiguration(schema: schema, isStoredInMemoryOnly: true)]
        ) {
            return container
        }

        fatalError("SwiftData 容器初始化失败")
    }()
}

// MARK: - 用户数据操作

@MainActor
struct LibraryService {
    let context: ModelContext

    // MARK: 收藏

    func isFavorite(_ resourceID: String) -> Bool {
        let targetID = resourceID
        let descriptor = FetchDescriptor<FavoriteRecord>(
            predicate: #Predicate<FavoriteRecord> { record in
                record.resourceID == targetID
            }
        )
        return ((try? context.fetchCount(descriptor)) ?? 0) > 0
    }

    /// 返回操作后的收藏状态。
    @discardableResult
    func toggleFavorite(_ item: ResourceItem) -> Bool {
        let targetID = item.id
        let descriptor = FetchDescriptor<FavoriteRecord>(
            predicate: #Predicate<FavoriteRecord> { record in
                record.resourceID == targetID
            }
        )

        if let existing = try? context.fetch(descriptor).first {
            context.delete(existing)
            try? context.save()
            return false
        }

        context.insert(FavoriteRecord(item: item))
        try? context.save()
        return true
    }

    func removeFavorite(_ record: FavoriteRecord) {
        context.delete(record)
        try? context.save()
    }

    // MARK: 浏览历史

    func recordVisit(_ item: ResourceItem) {
        let targetID = item.id
        let descriptor = FetchDescriptor<HistoryRecord>(
            predicate: #Predicate<HistoryRecord> { record in
                record.resourceID == targetID
            }
        )

        if let existing = try? context.fetch(descriptor).first {
            existing.viewedAt = Date()
            existing.payload = (try? JSONCoding.encoder.encode(item)) ?? existing.payload
        } else {
            context.insert(HistoryRecord(item: item))
        }
        try? context.save()
    }

    func clearHistory() {
        try? context.delete(model: HistoryRecord.self)
        try? context.save()
    }

    // MARK: 搜索历史

    func recordSearch(_ text: String) {
        let normalized = text.trimmingCharacters(in: .whitespacesAndNewlines)
        guard normalized.count >= 2 else { return }

        let descriptor = FetchDescriptor<SearchQueryRecord>(
            predicate: #Predicate<SearchQueryRecord> { record in
                record.text == normalized
            }
        )

        if let existing = try? context.fetch(descriptor).first {
            existing.lastUsed = Date()
            existing.hitCount += 1
        } else {
            context.insert(SearchQueryRecord(text: normalized))
        }
        try? context.save()
    }

    func clearSearchHistory() {
        try? context.delete(model: SearchQueryRecord.self)
        try? context.save()
    }
}
