//
//  SearchView.swift
//  iOSHUB
//
//  全局搜索：名称 / 开发者 / 关键词 / iOS 版本 / 标签 全字段匹配。
//  支持分类范围筛选与搜索历史（SwiftData 持久化）。
//

import SwiftUI
import SwiftData

struct SearchView: View {
    @Environment(CatalogStore.self) private var catalog
    @Environment(\.modelContext) private var modelContext

    @Query(sort: \SearchQueryRecord.lastUsed, order: .reverse)
    private var history: [SearchQueryRecord]

    @State private var query = ""
    @State private var scope: ResourceCategory?

    private var trimmed: String {
        query.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    private var results: [ResourceItem] {
        guard !trimmed.isEmpty else { return [] }
        return catalog.search(trimmed, category: scope)
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                scopeBar

                if trimmed.isEmpty {
                    idleContent
                } else if results.isEmpty {
                    EmptyStateView(
                        symbol: "magnifyingglass",
                        title: "没有找到「\(trimmed)」",
                        message: "试试项目名、开发者名，或 iOS 版本号。也可以到「发现」里直接搜索 GitHub。"
                    )
                    .cardSurface(padding: 0)
                } else {
                    resultList
                }
            }
            .padding(.horizontal, Metric.screenPadding)
            .padding(.top, 6)
            .padding(.bottom, 32)
        }
        .background(Palette.pageBackground)
        .navigationTitle("搜索")
        .navigationBarTitleDisplayMode(.inline)
        .searchable(text: $query, placement: .navigationBarDrawer(displayMode: .always), prompt: "搜索资源、开发者、iOS 版本")
        .onSubmit(of: .search) {
            saveQuery(trimmed)
        }
    }

    // MARK: - 范围

    private var scopeBar: some View {
        ScrollView(.horizontal) {
            HStack(spacing: 8) {
                scopeChip(title: "全部", isActive: scope == nil) {
                    scope = nil
                }
                ForEach(ResourceCategory.allCases) { category in
                    scopeChip(title: category.shortTitle, isActive: scope == category) {
                        scope = scope == category ? nil : category
                    }
                }
            }
            .padding(.vertical, 2)
        }
        .scrollIndicators(.hidden)
    }

    private func scopeChip(title: String, isActive: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(title)
                .font(Typography.micro)
                .foregroundStyle(isActive ? Color.white : Color.secondary)
                .padding(.horizontal, 12)
                .padding(.vertical, 7)
                .background(
                    Capsule().fill(isActive ? Palette.brand : Color.primary.opacity(0.06))
                )
        }
        .buttonStyle(.plain)
    }

    // MARK: - 未输入时

    private var idleContent: some View {
        VStack(alignment: .leading, spacing: 18) {
            if !history.isEmpty {
                VStack(alignment: .leading, spacing: 10) {
                    HStack {
                        Text("搜索历史")
                            .font(Typography.cardTitle)
                        Spacer()
                        Button("清空") {
                            LibraryService(context: modelContext).clearSearchHistory()
                        }
                        .font(Typography.micro)
                        .foregroundStyle(.secondary)
                    }

                    LazyVGrid(
                        columns: [GridItem(.adaptive(minimum: 96), spacing: 8)],
                        alignment: .leading,
                        spacing: 8
                    ) {
                        ForEach(history) { record in
                            Button {
                                query = record.text
                                saveQuery(record.text)
                            } label: {
                                Text(record.text)
                                    .font(Typography.caption)
                                    .foregroundStyle(.primary)
                                    .lineLimit(1)
                                    .padding(.horizontal, 11)
                                    .padding(.vertical, 7)
                                    .frame(maxWidth: .infinity)
                                    .background(Color.primary.opacity(0.06), in: Capsule())
                            }
                            .buttonStyle(.plain)
                        }
                    }
                }
                .cardSurface(padding: 15)
            }

            VStack(alignment: .leading, spacing: 10) {
                Text("试试这些")
                    .font(Typography.cardTitle)
                LazyVGrid(
                    columns: [GridItem(.adaptive(minimum: 96), spacing: 8)],
                    alignment: .leading,
                    spacing: 8
                ) {
                    ForEach(suggestions, id: \.self) { word in
                        Button {
                            query = word
                            saveQuery(word)
                        } label: {
                            Text(word)
                                .font(Typography.caption)
                                .foregroundStyle(Palette.brand)
                                .lineLimit(1)
                                .padding(.horizontal, 11)
                                .padding(.vertical, 7)
                                .frame(maxWidth: .infinity)
                                .background(Palette.brand.opacity(0.09), in: Capsule())
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
            .cardSurface(padding: 15)

            Text("搜索只覆盖内置资源库。想找更多项目，请到「发现」用 GitHub 搜索。")
                .font(Typography.micro)
                .foregroundStyle(.tertiary)
        }
    }

    private var suggestions: [String] {
        ["TrollStore", "越狱", "IPA", "侧载", "Theos", "美化", "iOS 17"]
    }

    // MARK: - 结果

    private var resultList: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("\(results.count) 个结果")
                .font(Typography.caption)
                .foregroundStyle(.secondary)

            VStack(spacing: 0) {
                ForEach(results) { item in
                    NavigationLink(value: item) {
                        ResourceRow(item: item)
                    }
                    .buttonStyle(.plain)
                    .simultaneousGesture(TapGesture().onEnded {
                        saveQuery(trimmed)
                    })

                    if item.id != results.last?.id {
                        Divider().opacity(0.35)
                    }
                }
            }
            .padding(.horizontal, 14)
            .cardSurface(padding: 0)
        }
    }

    private func saveQuery(_ text: String) {
        LibraryService(context: modelContext).recordSearch(text)
    }
}

#Preview {
    NavigationStack {
        SearchView()
    }
    .environment(CatalogStore())
    .modelContainer(PersistenceController.shared)
}
