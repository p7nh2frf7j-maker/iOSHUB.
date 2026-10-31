//
//  DiscoverView.swift
//  iOSHUB
//
//  发现 Tab：两件事 —— 用 GitHub 实时检索 iOS 生态项目，以及查看官方资讯源。
//  这里的所有内容都来自远端；断网时明确提示，不落回编造数据。
//

import SwiftUI

struct DiscoverView: View {
    enum Mode: String, CaseIterable, Identifiable {
        case github
        case news

        var id: String { rawValue }

        var title: String {
            switch self {
            case .github: return "GitHub 探索"
            case .news:   return "资讯"
            }
        }
    }

    @Environment(CatalogStore.self) private var catalog

    @State private var mode: Mode = .github
    @State private var query = ""
    @State private var repos: [GitHubRepo] = []
    @State private var isSearching = false
    @State private var errorText: String?

    private let keywordSuggestions = ["trollstore", "jailbreak", "ipa sideload", "theos tweak", "swiftui", "ios exploit"]

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    Picker("模式", selection: $mode) {
                        ForEach(Mode.allCases) { item in
                            Text(item.title).tag(item)
                        }
                    }
                    .pickerStyle(.segmented)

                    switch mode {
                    case .github:
                        githubSection
                    case .news:
                        newsSection
                    }
                }
                .padding(.horizontal, Metric.screenPadding)
                .padding(.top, 8)
                .padding(.bottom, 32)
            }
            .background(Palette.pageBackground)
            .navigationTitle("发现")
            .appRouting()
        }
    }

    // MARK: - GitHub

    private var githubSection: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack(spacing: 8) {
                HStack(spacing: 8) {
                    Image(systemName: "chevron.left.forwardslash.chevron.right")
                        .font(.system(size: 13, weight: .semibold))
                        .foregroundStyle(.secondary)
                    TextField("搜索 GitHub 项目，如 trollstore", text: $query)
                        .font(Typography.body)
                        .submitLabel(.search)
                        .autocorrectionDisabled()
                        .textInputAutocapitalization(.never)
                        .onSubmit { runSearch(query) }
                    if !query.isEmpty {
                        Button {
                            query = ""
                        } label: {
                            Image(systemName: "xmark.circle.fill")
                                .foregroundStyle(.tertiary)
                        }
                        .buttonStyle(.plain)
                    }
                }
                .padding(.horizontal, 12)
                .frame(height: 44)
                .background(Palette.cardBackground, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
                .overlay(
                    RoundedRectangle(cornerRadius: 14, style: .continuous)
                        .strokeBorder(Palette.stroke, lineWidth: 1)
                )

                Button {
                    runSearch(query.isEmpty ? "ios" : query)
                } label: {
                    Image(systemName: "magnifyingglass")
                        .font(.system(size: 15, weight: .bold))
                        .foregroundStyle(.white)
                        .frame(width: 44, height: 44)
                        .background(Palette.brand, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
                }
                .buttonStyle(.plain)
                .disabled(isSearching)
            }

            ScrollView(.horizontal) {
                HStack(spacing: 8) {
                    ForEach(keywordSuggestions, id: \.self) { word in
                        Button {
                            query = word
                            runSearch(word)
                        } label: {
                            Text(word)
                                .font(Typography.micro)
                                .foregroundStyle(Palette.brand)
                                .padding(.horizontal, 11)
                                .padding(.vertical, 6)
                                .background(Palette.brand.opacity(0.09), in: Capsule())
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
            .scrollIndicators(.hidden)

            if isSearching {
                LoadingRow(text: "正在向 GitHub 查询…")
            }

            if let errorText {
                NoticeBanner(message: errorText) {
                    runSearch(query.isEmpty ? "ios" : query)
                }
            }

            if repos.isEmpty {
                emptyGithubState
            } else {
                VStack(spacing: 0) {
                    ForEach(repos) { repo in
                        NavigationLink(value: ResourceItem.make(from: repo)) {
                            GitHubRepoRow(repo: repo)
                        }
                        .buttonStyle(.plain)

                        if repo.id != repos.last?.id {
                            Divider().opacity(0.35)
                        }
                    }
                }
                .padding(.horizontal, 14)
                .cardSurface(padding: 0)

                Text("结果来自 GitHub 公开搜索 API，按 Star 排序。点击条目可查看详情并跳转原仓库。")
                    .font(Typography.micro)
                    .foregroundStyle(.tertiary)
            }
        }
    }

    private var emptyGithubState: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("搜点什么")
                .font(Typography.cardTitle)
            Text("输入项目名或关键词，直接从 GitHub 检索 iOS 生态项目：越狱工具、巨魔、侧载、插件、开源组件都可以查。")
                .font(Typography.caption)
                .foregroundStyle(.secondary)
                .fixedSize(horizontal: false, vertical: true)

            if !catalog.trending.isEmpty {
                Text("当前热门")
                    .font(Typography.micro)
                    .foregroundStyle(.secondary)
                ForEach(catalog.trending.prefix(3)) { repo in
                    NavigationLink(value: ResourceItem.make(from: repo)) {
                        GitHubRepoRow(repo: repo)
                    }
                    .buttonStyle(.plain)
                }
            }
        }
        .cardSurface(padding: 15)
    }

    // MARK: - 资讯

    private var newsSection: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack {
                Text("最新资讯")
                    .font(Typography.cardTitle)
                Spacer()
                Button {
                    Task { await catalog.loadNews(feeds: [.appleDeveloper, .appleSecurity, .githubBlog]) }
                } label: {
                    Label("刷新", systemImage: "arrow.clockwise")
                        .font(Typography.micro)
                }
                .buttonStyle(.plain)
                .foregroundStyle(Palette.brand)
            }

            if let notice = catalog.newsNotice {
                NoticeBanner(message: notice, symbol: "wifi.exclamationmark") {
                    Task { await catalog.loadNews(feeds: [.appleDeveloper, .appleSecurity, .githubBlog]) }
                }
            }

            if catalog.news.isEmpty {
                EmptyStateView(
                    symbol: "newspaper",
                    title: "暂无资讯",
                    message: "资讯只在联网时从官方 RSS 拉取。没有网络时不展示任何生成内容。"
                )
                .cardSurface(padding: 0)
            } else {
                VStack(alignment: .leading, spacing: 2) {
                    ForEach(catalog.news) { item in
                        Link(destination: LinkRouter.resolved(item.url)) {
                            NewsRow(item: item)
                        }
                        .buttonStyle(.plain)
                        if item.id != catalog.news.last?.id {
                            Divider().opacity(0.3)
                        }
                    }
                }
                .padding(.horizontal, 14)
                .padding(.vertical, 6)
                .cardSurface(padding: 0)
            }

            sourcesCard
        }
    }

    private var sourcesCard: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("信息源")
                .font(Typography.cardTitle)
            Text("只聚合官方与公开社区源，标题与链接原样保留，不改编、不伪造发布时间。")
                .font(Typography.micro)
                .foregroundStyle(.secondary)
                .fixedSize(horizontal: false, vertical: true)

            ForEach(NewsFeed.allCases) { feed in
                if let url = feed.siteURL {
                    Link(destination: LinkRouter.resolved(url)) {
                        HStack(spacing: 10) {
                            Image(systemName: feed.symbol)
                                .font(.system(size: 12, weight: .semibold))
                                .foregroundStyle(Palette.brand)
                                .frame(width: 26, height: 26)
                                .background(Palette.brand.opacity(0.09), in: RoundedRectangle(cornerRadius: 8, style: .continuous))
                            VStack(alignment: .leading, spacing: 1) {
                                Text(feed.title)
                                    .font(Typography.caption)
                                    .foregroundStyle(.primary)
                                Text(feed.note)
                                    .font(Typography.micro)
                                    .foregroundStyle(.secondary)
                                    .lineLimit(2)
                            }
                            Spacer(minLength: 4)
                            Image(systemName: "arrow.up.right")
                                .font(.system(size: 10, weight: .bold))
                                .foregroundStyle(.tertiary)
                        }
                        .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                }
            }
        }
        .cardSurface(padding: 15)
    }

    // MARK: - 动作

    private func runSearch(_ text: String) {
        let keyword = text.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !keyword.isEmpty, !isSearching else { return }

        isSearching = true
        errorText = nil

        Task {
            var api = GitHubAPI()
            let token = AppSettings.githubToken
            api.token = token.isEmpty ? nil : token

            do {
                let found = try await api.searchRepositories(keyword: keyword, limit: 20)
                repos = found
            } catch let error as APIError {
                errorText = error.userMessage
                repos = []
            } catch {
                errorText = "GitHub 搜索失败，请稍后重试。"
                repos = []
            }

            isSearching = false
        }
    }
}

#Preview {
    DiscoverView()
        .environment(CatalogStore())
        .modelContainer(PersistenceController.shared)
}
