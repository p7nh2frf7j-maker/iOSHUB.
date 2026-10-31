//
//  RootTabView.swift
//  iOSHUB
//
//  底部五 Tab：首页 / 资源 / 发现 / 收藏 / 我的。
//  每个 Tab 自带 NavigationStack，路由用 AppRoute 统一声明。
//

import SwiftUI

enum AppRoute: Hashable {
    case search
    case category(ResourceCategory)
    case newsFeed
    case followedCategories
}

enum AppTab: Hashable {
    case home
    case resources
    case discover
    case favorites
    case profile
}

struct RootTabView: View {
    @State private var selection: AppTab = .home

    var body: some View {
        TabView(selection: $selection) {
            HomeView()
                .tabItem {
                    Label("首页", systemImage: selection == .home ? "house.fill" : "house")
                }
                .tag(AppTab.home)

            ResourcesView()
                .tabItem {
                    Label("资源", systemImage: selection == .resources ? "square.grid.2x2.fill" : "square.grid.2x2")
                }
                .tag(AppTab.resources)

            DiscoverView()
                .tabItem {
                    Label("发现", systemImage: selection == .discover ? "sparkles" : "sparkles")
                }
                .tag(AppTab.discover)

            FavoritesView()
                .tabItem {
                    Label("收藏", systemImage: selection == .favorites ? "heart.fill" : "heart")
                }
                .tag(AppTab.favorites)

            ProfileView()
                .tabItem {
                    Label("我的", systemImage: selection == .profile ? "person.crop.circle.fill" : "person.crop.circle")
                }
                .tag(AppTab.profile)
        }
    }
}

#Preview {
    RootTabView()
        .environment(CatalogStore())
        .modelContainer(PersistenceController.shared)
}
