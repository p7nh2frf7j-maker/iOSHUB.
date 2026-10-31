//
//  NewsFeedView.swift
//  iOSHUB
//
//  资讯列表页：展示聚合结果与信息源清单。
//  条目永远链回原文，App 不提供「站内阅读」以避免二次加工。
//

import SwiftUI

struct NewsFeedView: View {
    @Environment(CatalogStore.self) private var catalog

    private var feeds: [NewsFeed] {
        [.appleDeveloper, .appleSecurity, .githubBlog, .appleNewsroom, .redditJailbreak]
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                if let notice = catalog.newsNotice {
                    NoticeBanner(message: notice) {
                        Task { await catalog.loadNews(feeds: feeds) }
                    }
                }

                HStack(spacing: 8) {
                    Text("共 \(catalog.news.count) 条")
                        .font(Typography.caption)
                        .foregroundStyle(.secondary)
                    Spacer()
                    Button {
                        Task { await catalog.loadNews(feeds: feeds) }
                    } label: {
                        Label("刷新", systemImage: "arrow.clockwise")
                            .font(Typography.micro)
                    }
                    .buttonStyle(.plain)
                    .foregroundStyle(Palette.brand)
                }

                if catalog.news.isEmpty {
                    EmptyStateView(
                        symbol: "newspaper",
                        title: "暂无资讯",
                        message: "需要网络才能拉取官方 RSS。断网时不展示任何生成内容。"
                    )
                    .cardSurface(padding: 0)
                } else {
                    newsList
                }

                sourcesCard
            }
            .padding(.horizontal, Metric.screenPadding)
            .padding(.top, 8)
            .padding(.bottom, 32)
        }
        .background(Palette.pageBackground)
        .navigationTitle("资讯")
        .navigationBarTitleDisplayMode(.inline)
        .appRouting()
    }

    private var newsList: some View {
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

    private var sourcesCard: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("信息源清单")
                .font(Typography.cardTitle)

            ForEach(feeds) { feed in
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
}

#Preview {
    NavigationStack {
        NewsFeedView()
    }
    .environment(CatalogStore())
    .modelContainer(PersistenceController.shared)
}
