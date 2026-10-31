//
//  AppRouting.swift
//  iOSHUB
//
//  统一的导航注册。每个 Tab 的 NavigationStack 只需 .appRouting()，
//  避免在四个地方重复写 navigationDestination。
//

import SwiftUI

struct AppRouting: ViewModifier {
    func body(content: Content) -> some View {
        content
            .navigationDestination(for: ResourceItem.self) { item in
                ResourceDetailView(item: item)
            }
            .navigationDestination(for: AppRoute.self) { route in
                switch route {
                case .search:
                    SearchView()
                case .category(let category):
                    CategoryDetailView(category: category)
                case .newsFeed:
                    NewsFeedView()
                case .followedCategories:
                    FollowedCategoriesView()
                }
            }
    }
}

extension View {
    func appRouting() -> some View {
        modifier(AppRouting())
    }
}
