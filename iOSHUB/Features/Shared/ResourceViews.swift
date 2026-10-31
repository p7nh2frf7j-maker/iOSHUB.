//
//  ResourceViews.swift
//  iOSHUB
//
//  资源的多种展示形态。首页、分类页、搜索结果、收藏页共用，
//  保证同一个资源在任何位置都有同样的视觉语言。
//

import SwiftUI

// MARK: - 大卡（首页精选）

struct FeaturedCard: View {
    let item: ResourceItem

    var body: some View {
        VStack(alignment: .leading, spacing: 13) {
            HStack(alignment: .center, spacing: 12) {
                RemoteIcon(url: item.iconURL, symbol: item.symbol, size: 44, tint: .white)
                VStack(alignment: .leading, spacing: 3) {
                    Text(item.category.title)
                        .font(Typography.micro)
                        .foregroundStyle(.white.opacity(0.72))
                    Text(item.name)
                        .font(.system(size: 19, weight: .bold, design: .rounded))
                        .foregroundStyle(.white)
                        .lineLimit(1)
                }
                Spacer(minLength: 4)
                Image(systemName: "arrow.up.right")
                    .font(.system(size: 13, weight: .bold))
                    .foregroundStyle(.white.opacity(0.8))
            }

            Text(item.summary)
                .font(Typography.caption)
                .foregroundStyle(.white.opacity(0.86))
                .lineLimit(3)
                .multilineTextAlignment(.leading)
                .frame(maxWidth: .infinity, alignment: .leading)

            HStack(spacing: 6) {
                Pill(text: item.risk.title, symbol: item.risk.symbol, tint: .white)
                Pill(text: item.compatibility.title, symbol: item.compatibility.symbol, tint: .white)
                Spacer(minLength: 0)
                Text(item.versionText)
                    .font(Typography.micro)
                    .foregroundStyle(.white.opacity(0.72))
                    .lineLimit(1)
            }
        }
        .padding(17)
        .background(
            Palette.brandGradient,
            in: RoundedRectangle(cornerRadius: Metric.cardRadius, style: .continuous)
        )
        .overlay(
            RoundedRectangle(cornerRadius: Metric.cardRadius, style: .continuous)
                .strokeBorder(Color.white.opacity(0.14), lineWidth: 1)
        )
        .shadow(color: Palette.brand.opacity(0.28), radius: 18, x: 0, y: 10)
    }
}

// MARK: - 横滑卡

struct ResourceCard: View {
    let item: ResourceItem

    var body: some View {
        VStack(alignment: .leading, spacing: 9) {
            HStack(spacing: 10) {
                RemoteIcon(url: item.iconURL, symbol: item.symbol, size: 38, tint: item.category.tint)
                VStack(alignment: .leading, spacing: 1) {
                    Text(item.name)
                        .font(Typography.cardTitle)
                        .lineLimit(1)
                    Text(item.developer)
                        .font(Typography.micro)
                        .foregroundStyle(.secondary)
                        .lineLimit(1)
                }
                Spacer(minLength: 0)
            }

            Text(item.summary)
                .font(Typography.caption)
                .foregroundStyle(.secondary)
                .lineLimit(3)
                .multilineTextAlignment(.leading)
                .frame(maxWidth: .infinity, alignment: .leading)

            Spacer(minLength: 0)

            HStack(spacing: 5) {
                Pill(text: item.category.shortTitle, symbol: item.category.symbol, tint: item.category.tint)
                if let stars = item.stars {
                    Pill(text: NumberText.compact(stars), symbol: "star.fill", tint: Palette.amber)
                } else {
                    Pill(text: item.compatibility.title, tint: item.compatibility.tint)
                }
                Spacer(minLength: 0)
            }
        }
        .frame(width: 232, height: 172, alignment: .topLeading)
        .cardSurface(padding: 14, radius: 18)
    }
}

// MARK: - 列表行

struct ResourceRow: View {
    let item: ResourceItem
    var showsChevron: Bool = true

    private var metaText: String {
        if let stars = item.stars {
            return "★ \(NumberText.compact(stars))"
        }
        return item.compatibility.title
    }

    var body: some View {
        HStack(spacing: 12) {
            RemoteIcon(url: item.iconURL, symbol: item.symbol, size: 46, tint: item.category.tint)

            VStack(alignment: .leading, spacing: 3) {
                Text(item.name)
                    .font(Typography.cardTitle)
                    .lineLimit(1)
                Text(item.summary)
                    .font(Typography.caption)
                    .foregroundStyle(.secondary)
                    .lineLimit(1)
                HStack(spacing: 5) {
                    Text(item.category.title)
                        .font(Typography.micro)
                        .foregroundStyle(item.category.tint)
                    Text("·")
                        .font(Typography.micro)
                        .foregroundStyle(.tertiary)
                    Text(metaText)
                        .font(Typography.micro)
                        .foregroundStyle(.secondary)
                }
            }

            Spacer(minLength: 6)

            if showsChevron {
                Image(systemName: "chevron.right")
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundStyle(.tertiary)
            }
        }
        .padding(.vertical, 9)
        .contentShape(Rectangle())
    }
}

// MARK: - 网格砖（本周热门）

struct ResourceTile: View {
    let item: ResourceItem

    var body: some View {
        VStack(alignment: .leading, spacing: 9) {
            HStack(spacing: 8) {
                RemoteIcon(url: item.iconURL, symbol: item.symbol, size: 32, tint: item.category.tint)
                Spacer(minLength: 0)
                Image(systemName: "arrow.up.right")
                    .font(.system(size: 11, weight: .bold))
                    .foregroundStyle(.tertiary)
            }
            Text(item.name)
                .font(Typography.cardTitle)
                .lineLimit(1)
            Text(item.summary)
                .font(Typography.micro)
                .foregroundStyle(.secondary)
                .lineLimit(2)
                .multilineTextAlignment(.leading)
                .frame(maxWidth: .infinity, alignment: .leading)
            Spacer(minLength: 0)
            Text(item.stars.map { "★ " + NumberText.compact($0) } ?? item.category.shortTitle)
                .font(Typography.micro)
                .foregroundStyle(item.stars == nil ? item.category.tint : Palette.amber)
        }
        .frame(height: 142, alignment: .topLeading)
        .cardSurface(padding: 13, radius: 18)
    }
}

// MARK: - 分类

struct CategoryChip: View {
    let category: ResourceCategory
    var count: Int? = nil

    var body: some View {
        VStack(spacing: 7) {
            ZStack {
                RoundedRectangle(cornerRadius: 14, style: .continuous)
                    .fill(category.tint.opacity(0.13))
                    .frame(width: 46, height: 46)
                Image(systemName: category.symbol)
                    .font(.system(size: 17, weight: .semibold))
                    .foregroundStyle(category.tint)
            }
            Text(category.shortTitle)
                .font(Typography.micro)
                .foregroundStyle(.primary)
                .lineLimit(1)
        }
        .frame(width: 64)
        .contentShape(Rectangle())
    }
}

struct CategoryRow: View {
    let category: ResourceCategory
    let count: Int

    var body: some View {
        HStack(spacing: 13) {
            ZStack {
                RoundedRectangle(cornerRadius: 15, style: .continuous)
                    .fill(category.tint.opacity(0.13))
                    .frame(width: 48, height: 48)
                Image(systemName: category.symbol)
                    .font(.system(size: 19, weight: .semibold))
                    .foregroundStyle(category.tint)
            }

            VStack(alignment: .leading, spacing: 2) {
                Text(category.title)
                    .font(Typography.cardTitle)
                Text(category.blurb)
                    .font(Typography.caption)
                    .foregroundStyle(.secondary)
                    .lineLimit(1)
            }

            Spacer(minLength: 6)

            Text("\(count)")
                .font(Typography.number)
                .foregroundStyle(.secondary)

            Image(systemName: "chevron.right")
                .font(.system(size: 12, weight: .semibold))
                .foregroundStyle(.tertiary)
        }
        .padding(.vertical, 9)
        .contentShape(Rectangle())
    }
}

// MARK: - GitHub 仓库行

struct GitHubRepoRow: View {
    let repo: GitHubRepo

    var body: some View {
        HStack(spacing: 12) {
            RemoteIcon(
                url: repo.avatarURL,
                symbol: "chevron.left.forwardslash.chevron.right",
                size: 42,
                tint: Palette.slate
            )

            VStack(alignment: .leading, spacing: 3) {
                Text(repo.fullName)
                    .font(Typography.cardTitle)
                    .lineLimit(1)
                if let description = repo.description, !description.isEmpty {
                    Text(description)
                        .font(Typography.caption)
                        .foregroundStyle(.secondary)
                        .lineLimit(2)
                        .multilineTextAlignment(.leading)
                }
                HStack(spacing: 8) {
                    Label(NumberText.compact(repo.stargazersCount), systemImage: "star.fill")
                        .font(Typography.micro)
                        .foregroundStyle(Palette.amber)
                    Label(NumberText.compact(repo.forksCount), systemImage: "tuningfork")
                        .font(Typography.micro)
                        .foregroundStyle(.secondary)
                    if let language = repo.language {
                        Text(language)
                            .font(Typography.micro)
                            .foregroundStyle(.secondary)
                    }
                }
            }

            Spacer(minLength: 4)
        }
        .padding(.vertical, 8)
        .contentShape(Rectangle())
    }
}

// MARK: - 资讯行

struct NewsRow: View {
    let item: NewsItem

    var body: some View {
        HStack(alignment: .top, spacing: 11) {
            Image(systemName: item.feed.symbol)
                .font(.system(size: 13, weight: .semibold))
                .foregroundStyle(Palette.brand)
                .frame(width: 26, height: 26)
                .background(Palette.brand.opacity(0.10), in: RoundedRectangle(cornerRadius: 8, style: .continuous))

            VStack(alignment: .leading, spacing: 4) {
                Text(item.title)
                    .font(Typography.body)
                    .lineLimit(2)
                    .multilineTextAlignment(.leading)
                HStack(spacing: 6) {
                    Text(item.source)
                        .font(Typography.micro)
                        .foregroundStyle(Palette.brand)
                    Text("·")
                        .font(Typography.micro)
                        .foregroundStyle(.tertiary)
                    Text(item.publishedText)
                        .font(Typography.micro)
                        .foregroundStyle(.secondary)
                }
            }

            Spacer(minLength: 0)
        }
        .padding(.vertical, 8)
        .contentShape(Rectangle())
    }
}
