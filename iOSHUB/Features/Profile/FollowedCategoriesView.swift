//
//  FollowedCategoriesView.swift
//  iOSHUB
//
//  关注分类：写进 UserDefaults，后续用于首页排序与更新提醒过滤。
//

import SwiftUI

struct FollowedCategoriesView: View {
    @AppStorage("settings.followedCategories") private var followedRaw = ""

    private var followed: Set<String> {
        Set(followedRaw.split(separator: ",").map(String.init))
    }

    var body: some View {
        List {
            Section {
                ForEach(ResourceCategory.allCases) { category in
                    Toggle(isOn: binding(for: category)) {
                        HStack(spacing: 10) {
                            Image(systemName: category.symbol)
                                .font(.system(size: 13, weight: .semibold))
                                .foregroundStyle(category.tint)
                                .frame(width: 26, height: 26)
                                .background(category.tint.opacity(0.12), in: RoundedRectangle(cornerRadius: 8, style: .continuous))
                            VStack(alignment: .leading, spacing: 1) {
                                Text(category.title)
                                Text(category.blurb)
                                    .font(Typography.micro)
                                    .foregroundStyle(.secondary)
                                    .lineLimit(1)
                            }
                        }
                    }
                }
            } footer: {
                Text("已关注 \(followed.count) 个分类。首页与更新提醒会优先考虑关注的分类（排序接入在 Phase 3）。")
            }
        }
        .listStyle(.insetGrouped)
        .navigationTitle("关注分类")
        .navigationBarTitleDisplayMode(.inline)
    }

    private func binding(for category: ResourceCategory) -> Binding<Bool> {
        Binding(
            get: { followed.contains(category.rawValue) },
            set: { isOn in
                var set = followed
                if isOn {
                    set.insert(category.rawValue)
                } else {
                    set.remove(category.rawValue)
                }
                followedRaw = set.sorted().joined(separator: ",")
            }
        )
    }
}

#Preview {
    NavigationStack {
        FollowedCategoriesView()
    }
}
