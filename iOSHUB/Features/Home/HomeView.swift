//
//  HomeView.swift
//  iOSHUB
//
//  首页 Dashboard：卡片式布局，纵向分区。
//  信息层级：品牌头 → 精选 → 分类入口 → 最新收录 → 最近更新
//            → GitHub 热门 → 资讯 → 本周热门 → 数据说明
//
//  每个联网区块都允许「空但完整」：拿不到数据时说明原因，不伪造内容。
//

import SwiftUI

struct HomeView: View {
    @Environment(CatalogStore.self) private var catalog

    var body: some View {
        NavigationStack {
            scrollContent
                .background(Palette.pageBackground)
                .refreshable {
                    await catalog.refreshFromNetwork()
                }
                .toolbar(.hidden, for: .navigationBar)
                .navigationDestination(for: ResourceItem.self) { item in
                    ResourceDetailView(item: item)
                }
                .navigationDestination(for: AppRoute.self) { route in
                    routeDestination(route)
                }
        }
    }

    // MARK: - 主体

    private var scrollContent: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: Metric.sectionSpacing) {
                header
                switch catalog.phase {
                case .loading:
                    LoadingRow(text: "正在载入内置资源库…")
                        .screenPadding()
                case .failed(let message):
                    NoticeBanner(
                        message: message,
                        symbol: "exclamationmark.triangle.fill",
                        tint: Palette.rose
                    ) {
                        Task { await catalog.loadIfNeeded() }
                    }
                    .screenPadding()
                case .idle, .ready:
                    EmptyView()
                }

                if let notice = catalog.remoteNotice {
                    NoticeBanner(message: notice) {
                        Task { await catalog.refreshFromNetwork() }
                    }
                    .screenPadding()
                }

                featuredSection
                categorySection
                curatedSection
                portSection
                recentlyUpdatedSection
                trendingSection
                newsSection
                weeklySection
                dataNoteSection
                brandFooterSection
            }
            .padding(.top, 10)
            .padding(.bottom, 40)
        }
        .scrollIndicators(.hidden)
    }

    // MARK: - 头部

    private var header: some View {
        VStack(alignment: .leading, spacing: 14) {            HStack(spacing: 11) {
                BrandLogo(size: 42)
                VStack(alignment: .leading, spacing: 1) {
                    BrandSignature(titleSize: 27)
                    Text(greeting)
                        .font(Typography.caption)
                        .foregroundStyle(.secondary)
                }
                Spacer(minLength: 8)
                NavigationLink(value: AppRoute.newsFeed) {
                    Image(systemName: "bell.badge")
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundStyle(Palette.brand)
                        .frame(width: 38, height: 38)
                        .background(Palette.brand.opacity(0.10), in: Circle())
                }
                .buttonStyle(.plain)
            }

            NavigationLink(value: AppRoute.search) {
                SearchEntry()
            }
            .buttonStyle(.plain)
        }
        .screenPadding()
    }

    private var greeting: String {
        let hour = Calendar.current.component(.hour, from: Date())
        switch hour {
        case 5..<11:  return "早上好，今天有 \(catalog.items.count) 个资源待发现"
        case 11..<14: return "中午好，看看社区的新动态"
        case 14..<19: return "下午好，一次把生态逛完"
        default:      return "晚上好，睡前刷一遍更新"
        }
    }

    // MARK: - 今日精选

    private var featuredSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            SectionHeader(title: "今日精选", subtitle: "人工核对来源的长期项目")
                .screenPadding()

            if catalog.featured.isEmpty {
                EmptyStateView(
                    symbol: "star.slash",
                    title: "暂无精选",
                    message: "内置资源库为空，请检查 seed-resources.json。"
                )
                .screenPadding()
            } else {
                hScroll {
                    ForEach(catalog.featured.prefix(3)) { item in
                        NavigationLink(value: item) {
                            FeaturedCard(item: item)
                                .frame(width: 300)
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
        }
    }

    // MARK: - 分类快捷入口

    private var categorySection: some View {
        VStack(alignment: .leading, spacing: 12) {
            SectionHeader(title: "资源分类", subtitle: "13 个方向，含移植与模拟")
                .screenPadding()

            hScroll {
                ForEach(ResourceCategory.allCases) { category in
                    NavigationLink(value: AppRoute.category(category)) {
                        CategoryChip(category: category, count: catalog.count(in: category))
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }

    // MARK: - 最新收录

    private var curatedSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            SectionHeader(title: "最新收录", subtitle: "内置库策展顺序 · 联网后按真实更新时间重排")
                .screenPadding()

            hScroll {
                ForEach(catalog.curatedHighlights) { item in
                    NavigationLink(value: item) {
                        ResourceCard(item: item)
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }

    // MARK: - 移植与模拟

    private var portSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            SectionHeader(title: "移植与模拟", subtitle: "官方移植 vs 网络传闻：把「能玩」和「能装」分清楚")
                .screenPadding()

            if catalog.items(in: .port).isEmpty {
                EmptyStateView(
                    symbol: "gamecontroller",
                    title: "暂无移植条目",
                    message: "在 seed-resources.json 里把条目的 category 设为 port，就会出现在这里。"
                )
                .screenPadding()
            } else {
                hScroll {
                    ForEach(catalog.items(in: .port)) { item in
                        NavigationLink(value: item) {
                            ResourceCard(item: item)
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
        }
    }

    // MARK: - 最近更新

    private var recentlyUpdatedSection: some View {
        VStack(alignment: .leading, spacing: 6) {
            SectionHeader(title: "最近更新", subtitle: "数据来自各项目在 GitHub 的最后推送时间")
                .screenPadding()

            if catalog.recentlyUpdated.isEmpty {
                VStack(alignment: .leading, spacing: 10) {
                    Text("尚未获取真实更新时间。下拉刷新后会从 GitHub 拉取，不会用假日期填充。")
                        .font(Typography.caption)
                        .foregroundStyle(.secondary)
                    Button {
                        Task { await catalog.refreshFromNetwork() }
                    } label: {
                        Label("获取最新更新时间", systemImage: "arrow.clockwise")
                            .font(Typography.micro)
                    }
                    .buttonStyle(.bordered)
                    .controlSize(.small)
                }
                .screenPadding()
                .padding(.top, 4)
            } else {
                VStack(spacing: 0) {
                    ForEach(catalog.recentlyUpdated.prefix(4)) { item in
                        NavigationLink(value: item) {
                            ResourceRow(item: item)
                        }
                        .buttonStyle(.plain)
                        if item.id != catalog.recentlyUpdated.prefix(4).last?.id {
                            Divider().opacity(0.4)
                        }
                    }
                }
                .padding(.horizontal, 14)
                .padding(.vertical, 4)
                .cardSurface(padding: 0)
                .screenPadding()
            }
        }
    }

    // MARK: - GitHub 热门

    private var trendingSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            SectionHeader(title: "GitHub 热门 iOS 项目", subtitle: "Star 800+ 且近一年有推送，实时检索")
                .screenPadding()

            if catalog.trending.isEmpty {
                VStack(alignment: .leading, spacing: 10) {
                    Text("需要网络才能看到实时热门项目。内置条目本身不依赖此区块。")
                        .font(Typography.caption)
                        .foregroundStyle(.secondary)
                    Button {
                        Task { await catalog.refreshFromNetwork() }
                    } label: {
                        Label(catalog.isRefreshing ? "获取中…" : "从 GitHub 获取", systemImage: "arrow.down.circle")
                            .font(Typography.micro)
                    }
                    .buttonStyle(.borderedProminent)
                    .controlSize(.small)
                    .disabled(catalog.isRefreshing)
                }
                .screenPadding()
                .padding(.top, 4)
            } else {
                VStack(spacing: 0) {
                    ForEach(catalog.trending.prefix(5)) { repo in
                        NavigationLink(value: ResourceItem.make(from: repo)) {
                            GitHubRepoRow(repo: repo)
                        }
                        .buttonStyle(.plain)
                        if repo.id != catalog.trending.prefix(5).last?.id {
                            Divider().opacity(0.4)
                        }
                    }
                }
                .padding(.horizontal, 14)
                .padding(.vertical, 4)
                .cardSurface(padding: 0)
                .screenPadding()
            }
        }
    }

    // MARK: - 资讯

    private var newsSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            SectionHeader(title: "最新资讯", subtitle: "官方 RSS 原样聚合，不改写标题") {
                NavigationLink(value: AppRoute.newsFeed) {
                    Text("全部")
                        .font(Typography.micro)
                        .foregroundStyle(Palette.brand)
                }
            }
            .screenPadding()

            if catalog.latestNews.isEmpty {
                Text("资讯需要联网获取。没有网络时不展示任何编造的新闻。")
                    .font(Typography.caption)
                    .foregroundStyle(.secondary)
                    .screenPadding()
                    .padding(.top, 4)
            } else {
                VStack(alignment: .leading, spacing: 4) {
                    ForEach(catalog.latestNews) { item in
                        Link(destination: LinkRouter.resolved(item.url)) {
                            NewsRow(item: item)
                        }
                        .buttonStyle(.plain)
                    }
                }
                .padding(.horizontal, 14)
                .padding(.vertical, 6)
                .cardSurface(padding: 0)
                .screenPadding()
            }
        }
    }

    // MARK: - 本周热门

    private var weeklySection: some View {
        VStack(alignment: .leading, spacing: 12) {
            SectionHeader(title: "本周热门", subtitle: "按 GitHub Star 排行的内置条目")
                .screenPadding()

            LazyVGrid(
                columns: [GridItem(.flexible(), spacing: 12), GridItem(.flexible(), spacing: 12)],
                spacing: 12
            ) {
                ForEach(catalog.weeklyPopular) { item in
                    NavigationLink(value: item) {
                        ResourceTile(item: item)
                    }
                    .buttonStyle(.plain)
                }
            }
            .screenPadding()
        }
    }

    // MARK: - 数据说明

    private var dataNoteSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(spacing: 6) {
                Image(systemName: "checkmark.shield.fill")
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundStyle(Palette.mint)
                Text("数据来源说明")
                    .font(Typography.caption)
                    .foregroundStyle(.primary)
            }
            Text("所有资源均指向开发者官方页面或 GitHub 原始仓库，App 不托管、不分发未经授权的 IPA。标记为「待验证」的字段表示尚未核对，请以原始来源为准。")
                .font(Typography.micro)
                .foregroundStyle(.secondary)
                .fixedSize(horizontal: false, vertical: true)
            if let refreshed = catalog.lastRemoteRefresh {
                Text("上次联网刷新：\(RelativeTime.string(from: refreshed))")
                    .font(Typography.micro)
                    .foregroundStyle(.tertiary)
            }
        }
        .padding(14)
        .background(Palette.subtleGradient, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
        .screenPadding()
    }

    // MARK: - 品牌落款

    private var brandFooterSection: some View {
        VStack(spacing: 0) {
            Divider()
                .opacity(0.35)
                .padding(.bottom, 6)
            BrandFooter()
        }
        .screenPadding()
    }

    // MARK: - 工具

    private func hScroll<Content: View>(@ViewBuilder content: () -> Content) -> some View {
        ScrollView(.horizontal) {
            HStack(alignment: .top, spacing: 12) {
                content()
            }
            .padding(.horizontal, Metric.screenPadding)
            .padding(.vertical, 4)
        }
        .scrollIndicators(.hidden)
    }

    @ViewBuilder
    private func routeDestination(_ route: AppRoute) -> some View {
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

#Preview {
    HomeView()
        .environment(CatalogStore())
        .modelContainer(PersistenceController.shared)
}
