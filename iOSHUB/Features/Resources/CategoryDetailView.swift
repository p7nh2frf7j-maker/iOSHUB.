//
//  CategoryDetailView.swift
//  iOSHUB
//
//  单个分类页：搜索 / 排序 / 列表。
//  排序里「推荐」优先展示精选条目，避免默认顺序看起来随机。
//

import SwiftUI

struct CategoryDetailView: View {
    let category: ResourceCategory

    @Environment(CatalogStore.self) private var catalog
    @State private var query = ""
    @State private var sort: ResourceSort = .recommended

    private var list: [ResourceItem] {
        let base = query.trimmingCharacters(in: .whitespaces).isEmpty
            ? catalog.items(in: category)
            : catalog.search(query, category: category)
        return catalog.sorted(base, by: sort)
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                header
                toolbar
                if list.isEmpty {
                    emptyState
                } else {
                    resultList
                }
            }
            .padding(.horizontal, Metric.screenPadding)
            .padding(.top, 6)
            .padding(.bottom, 32)
        }
        .background(Palette.pageBackground)
        .navigationTitle(category.title)
        .navigationBarTitleDisplayMode(.inline)
        .searchable(text: $query, prompt: "在「\(category.title)」中搜索")
        .appRouting()
    }

    private var header: some View {
        HStack(spacing: 12) {
            ZStack {
                RoundedRectangle(cornerRadius: 15, style: .continuous)
                    .fill(category.tint.opacity(0.14))
                    .frame(width: 50, height: 50)
                Image(systemName: category.symbol)
                    .font(.system(size: 20, weight: .semibold))
                    .foregroundStyle(category.tint)
            }

            VStack(alignment: .leading, spacing: 2) {
                Text(category.title)
                    .font(Typography.cardTitle)
                Text(category.blurb)
                    .font(Typography.caption)
                    .foregroundStyle(.secondary)
                    .lineLimit(2)
            }
            Spacer(minLength: 0)
        }
        .padding(.bottom, 2)
    }

    private var toolbar: some View {
        HStack(spacing: 10) {
            Menu {
                ForEach(ResourceSort.allCases) { option in
                    Button {
                        sort = option
                    } label: {
                        if option == sort {
                            Label(option.title, systemImage: "checkmark")
                        } else {
                            Text(option.title)
                        }
                    }
                }
            } label: {
                HStack(spacing: 5) {
                    Image(systemName: "arrow.up.arrow.down")
                        .font(.system(size: 10, weight: .bold))
                    Text(sort.title)
                        .font(Typography.micro)
                }
                .foregroundStyle(Palette.brand)
                .padding(.horizontal, 11)
                .padding(.vertical, 7)
                .background(Palette.brand.opacity(0.10), in: Capsule())
            }

            Text("\(list.count) 个资源")
                .font(Typography.caption)
                .foregroundStyle(.secondary)

            Spacer(minLength: 0)

            if !query.isEmpty {
                Button("清除") {
                    query = ""
                }
                .font(Typography.micro)
                .foregroundStyle(.secondary)
            }
        }
    }

    private var resultList: some View {
        VStack(spacing: 0) {
            ForEach(list) { item in
                NavigationLink(value: item) {
                    ResourceRow(item: item)
                }
                .buttonStyle(.plain)

                if item.id != list.last?.id {
                    Divider().opacity(0.35)
                }
            }
        }
        .padding(.horizontal, 14)
        .cardSurface(padding: 0)
    }

    private var emptyState: some View {
        EmptyStateView(
            symbol: "magnifyingglass",
            title: query.isEmpty ? "该分类暂无条目" : "没有匹配的资源",
            message: query.isEmpty
                ? "这个分类还在整理中。可以先在「发现」里用 GitHub 搜索相关项目。"
                : "换个关键词试试，或到「发现」直接搜索 GitHub。"
        )
        .cardSurface(padding: 0)
    }
}

#Preview {
    NavigationStack {
        CategoryDetailView(category: .trollstore)
    }
    .environment(CatalogStore())
    .modelContainer(PersistenceController.shared)
}
