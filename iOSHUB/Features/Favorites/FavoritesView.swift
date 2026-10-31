//
//  FavoritesView.swift
//  iOSHUB
//
//  收藏 / 浏览历史。数据全部存 SwiftData，删除即删，不上传、不同步。
//

import SwiftUI
import SwiftData

struct FavoritesView: View {
    enum Tab: String, CaseIterable, Identifiable {
        case favorites
        case history

        var id: String { rawValue }

        var title: String {
            switch self {
            case .favorites: return "收藏"
            case .history:   return "历史"
            }
        }
    }

    @Environment(\.modelContext) private var modelContext

    @Query(sort: \FavoriteRecord.createdAt, order: .reverse)
    private var favorites: [FavoriteRecord]

    @Query(sort: \HistoryRecord.viewedAt, order: .reverse)
    private var history: [HistoryRecord]

    @State private var tab: Tab = .favorites

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                Picker("视图", selection: $tab) {
                    ForEach(Tab.allCases) { item in
                        Text(item.title).tag(item)
                    }
                }
                .pickerStyle(.segmented)
                .padding(.horizontal, Metric.screenPadding)
                .padding(.top, 8)
                .padding(.bottom, 10)

                switch tab {
                case .favorites:
                    favoritesContent
                case .history:
                    historyContent
                }
            }
            .background(Palette.pageBackground)
            .navigationTitle("收藏")
            .appRouting()
        }
    }

    // MARK: - 收藏

    @ViewBuilder
    private var favoritesContent: some View {
        if favorites.isEmpty {
            ScrollView {
                EmptyStateView(
                    symbol: "heart",
                    title: "还没有收藏",
                    message: "在任意资源详情页点右上角的心形按钮，就能把它存到这里。"
                )
                .cardSurface(padding: 0)
                .padding(.horizontal, Metric.screenPadding)
                .padding(.top, 12)
            }
        } else {
            List {
                ForEach(favorites) { record in
                    favoriteRow(record)
                        .listRowBackground(Color.clear)
                        .listRowSeparator(.hidden)
                        .listRowInsets(EdgeInsets(top: 2, leading: Metric.screenPadding, bottom: 2, trailing: Metric.screenPadding))
                }
                .onDelete(perform: deleteFavorites)
            }
            .listStyle(.plain)
            .scrollContentBackground(.hidden)
            .background(Palette.pageBackground)
        }
    }

    @ViewBuilder
    private func favoriteRow(_ record: FavoriteRecord) -> some View {
        if let item = record.item {
            NavigationLink(value: item) {
                ResourceRow(item: item)
                    .cardSurface(padding: 12, radius: 16)
            }
            .buttonStyle(.plain)
        } else {
            HStack(spacing: 10) {
                Image(systemName: "questionmark.square.dashed")
                    .foregroundStyle(.secondary)
                Text(record.name)
                    .font(Typography.body)
                Spacer()
            }
            .padding(12)
        }
    }

    private func deleteFavorites(_ offsets: IndexSet) {
        for index in offsets {
            modelContext.delete(favorites[index])
        }
        try? modelContext.save()
    }

    // MARK: - 历史

    @ViewBuilder
    private var historyContent: some View {
        if history.isEmpty {
            ScrollView {
                EmptyStateView(
                    symbol: "clock.arrow.circlepath",
                    title: "暂无浏览记录",
                    message: "打开过的资源会出现在这里，方便回看。"
                )
                .cardSurface(padding: 0)
                .padding(.horizontal, Metric.screenPadding)
                .padding(.top, 12)
            }
        } else {
            List {
                Section {
                    ForEach(history) { record in
                        historyRow(record)
                            .listRowBackground(Color.clear)
                            .listRowSeparator(.hidden)
                            .listRowInsets(EdgeInsets(top: 2, leading: Metric.screenPadding, bottom: 2, trailing: Metric.screenPadding))
                    }
                    .onDelete(perform: deleteHistory)
                } header: {
                    HStack {
                        Text("\(history.count) 条记录")
                        Spacer()
                        Button("清空") {
                            LibraryService(context: modelContext).clearHistory()
                        }
                        .font(Typography.micro)
                        .textCase(nil)
                    }
                }
            }
            .listStyle(.plain)
            .scrollContentBackground(.hidden)
            .background(Palette.pageBackground)
        }
    }

    @ViewBuilder
    private func historyRow(_ record: HistoryRecord) -> some View {
        if let item = record.item {
            NavigationLink(value: item) {
                VStack(alignment: .leading, spacing: 4) {
                    ResourceRow(item: item)
                    Text(RelativeTime.string(from: record.viewedAt))
                        .font(Typography.micro)
                        .foregroundStyle(.tertiary)
                        .padding(.leading, 58)
                        .padding(.bottom, 6)
                }
                .cardSurface(padding: 12, radius: 16)
            }
            .buttonStyle(.plain)
        } else {
            Text(record.name)
        }
    }

    private func deleteHistory(_ offsets: IndexSet) {
        for index in offsets {
            modelContext.delete(history[index])
        }
        try? modelContext.save()
    }
}

#Preview {
    FavoritesView()
        .environment(CatalogStore())
        .modelContainer(PersistenceController.shared)
}
