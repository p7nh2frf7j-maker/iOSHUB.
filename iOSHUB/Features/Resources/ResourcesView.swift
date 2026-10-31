//
//  ResourcesView.swift
//  iOSHUB
//
//  资源 Tab：12 个分类的入口列表。
//  分类数量不写死，按实际收录情况展示。
//

import SwiftUI

struct ResourcesView: View {
    @Environment(CatalogStore.self) private var catalog

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 18) {
                    NavigationLink(value: AppRoute.search) {
                        SearchEntry(placeholder: "搜索资源名称、开发者或 iOS 版本")
                    }
                    .buttonStyle(.plain)

                    HStack(spacing: 6) {
                        Image(systemName: "square.grid.2x2.fill")
                            .font(.system(size: 12, weight: .semibold))
                            .foregroundStyle(Palette.brand)
                        Text("\(ResourceCategory.allCases.count) 个分类 · \(catalog.items.count) 个资源")
                            .font(Typography.caption)
                            .foregroundStyle(.secondary)
                        Spacer(minLength: 0)
                    }

                    if catalog.items.isEmpty {
                        EmptyStateView(
                            symbol: "tray",
                            title: "资源库为空",
                            message: "内置资源尚未载入，请稍后重试或检查 seed-resources.json。"
                        )
                        .cardSurface(padding: 0)
                    } else {
                        categoryList
                    }

                    integrityNote
                }
                .padding(.horizontal, Metric.screenPadding)
                .padding(.top, 8)
                .padding(.bottom, 32)
            }
            .background(Palette.pageBackground)
            .navigationTitle("资源")
            .appRouting()
        }
    }

    private var categoryList: some View {
        VStack(spacing: 0) {
            ForEach(catalog.categoriesInUse) { category in
                NavigationLink(value: AppRoute.category(category)) {
                    CategoryRow(category: category, count: catalog.count(in: category))
                }
                .buttonStyle(.plain)

                if category != catalog.categoriesInUse.last {
                    Divider().opacity(0.35)
                }
            }
        }
        .padding(.horizontal, 14)
        .cardSurface(padding: 0)
    }

    private var integrityNote: some View {
        HStack(alignment: .top, spacing: 9) {
            Image(systemName: "checkmark.shield.fill")
                .font(.system(size: 12, weight: .semibold))
                .foregroundStyle(Palette.mint)
            Text("分类只收录指向官方仓库 / 开发者主页的条目。标记「待验证」的版本与兼容性尚未核对，请以原始来源为准。")
                .font(Typography.micro)
                .foregroundStyle(.secondary)
                .fixedSize(horizontal: false, vertical: true)
        }
        .padding(13)
        .background(Palette.subtleGradient, in: RoundedRectangle(cornerRadius: 15, style: .continuous))
    }
}

#Preview {
    ResourcesView()
        .environment(CatalogStore())
        .modelContainer(PersistenceController.shared)
}
