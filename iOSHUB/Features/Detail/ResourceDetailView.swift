//
//  ResourceDetailView.swift
//  iOSHUB
//
//  资源详情页。所有字段都区分「已核对」与「待验证」，
//  下载一律跳转官方来源，App 自身不托管安装包。
//

import SwiftUI
import SwiftData

struct ResourceDetailView: View {
    let item: ResourceItem

    @Environment(\.modelContext) private var modelContext
    @Query private var favorites: [FavoriteRecord]

    /// 远端增量数据（仓库统计 / Release / README）
    @State private var remote = ResourceDetailModel()

    private var isFavorite: Bool {
        favorites.contains { $0.resourceID == item.id }
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 18) {
                heroCard
                actionButtons
                if item.risk.severity >= SecurityRisk.caution.severity {
                    riskNotice
                }
                specSection
                summarySection
                if !item.screenshots.isEmpty {
                    screenshotSection
                }
                tutorialSection
                if let changelog = item.changelog, !changelog.isEmpty {
                    changelogSection(changelog)
                }
                releaseSection
                readmeSection
                sourceSection
                verificationSection
            }
            .padding(.horizontal, Metric.screenPadding)
            .padding(.top, 8)
            .padding(.bottom, 40)
        }
        .background(Palette.pageBackground)
        .navigationTitle(item.name)
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button {
                    toggleFavorite()
                } label: {
                    Image(systemName: isFavorite ? "heart.fill" : "heart")
                        .foregroundStyle(isFavorite ? Palette.rose : Color.primary)
                }
                .accessibilityLabel(isFavorite ? "取消收藏" : "收藏资源")
            }
        }
        .task {
            LibraryService(context: modelContext).recordVisit(item)
        }
        .task {
            await remote.load(for: item)
        }
    }

    // MARK: - Hero

    private var heroCard: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack(spacing: 14) {
                RemoteIcon(url: item.iconURL, symbol: item.symbol, size: 66, tint: item.category.tint)

                VStack(alignment: .leading, spacing: 4) {
                    Text(item.name)
                        .font(.system(size: 21, weight: .bold, design: .rounded))
                        .lineLimit(2)
                    Text(item.developer)
                        .font(Typography.caption)
                        .foregroundStyle(.secondary)
                        .lineLimit(1)
                    HStack(spacing: 5) {
                        Pill(text: item.category.title, symbol: item.category.symbol, tint: item.category.tint)
                        Pill(text: item.compatibility.title, symbol: item.compatibility.symbol, tint: item.compatibility.tint)
                    }
                }
                Spacer(minLength: 0)
            }

            HStack(spacing: 6) {
                Label(item.versionText, systemImage: "tag.fill")
                    .font(Typography.micro)
                    .foregroundStyle(item.isVersionVerified ? Color.secondary : Palette.amber)
                Spacer(minLength: 0)
                Label(item.risk.title, systemImage: item.risk.symbol)
                    .font(Typography.micro)
                    .foregroundStyle(item.risk.tint)
            }
        }
        .cardSurface(padding: 16)
    }

    // MARK: - 行动按钮

    private var actionButtons: some View {
        VStack(spacing: 8) {
            Link(destination: LinkRouter.resolved(item.primaryActionURL)) {
                HStack(spacing: 8) {
                    Image(systemName: "arrow.down.circle.fill")
                    Text(item.primaryActionTitle)
                    Spacer(minLength: 0)
                    Image(systemName: "arrow.up.right")
                        .font(.system(size: 12, weight: .bold))
                }
                .font(Typography.cardTitle)
                .foregroundStyle(.white)
                .padding(.horizontal, 16)
                .frame(height: 50)
                .background(Palette.brandGradient, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
            }
            .buttonStyle(.plain)

            HStack(spacing: 8) {
                if let githubURL = item.githubURL {
                    Link(destination: LinkRouter.resolved(githubURL)) {
                        actionChip(title: "GitHub", symbol: "chevron.left.forwardslash.chevron.right")
                    }
                    .buttonStyle(.plain)
                }

                if let tutorialURL = item.tutorialURL {
                    Link(destination: LinkRouter.resolved(tutorialURL)) {
                        actionChip(title: "使用教程", symbol: "book.fill")
                    }
                    .buttonStyle(.plain)
                }

                ShareLink(item: item.sourceURL, subject: Text(item.name), message: Text("来自 iOS HUB 的资源链接")) {
                    actionChip(title: "分享", symbol: "square.and.arrow.up")
                }
                .buttonStyle(.plain)

                Button {
                    toggleFavorite()
                } label: {
                    actionChip(
                        title: isFavorite ? "已收藏" : "收藏",
                        symbol: isFavorite ? "heart.fill" : "heart",
                        highlighted: isFavorite
                    )
                }
                .buttonStyle(.plain)
            }
        }
    }

    private func actionChip(title: String, symbol: String, highlighted: Bool = false) -> some View {
        VStack(spacing: 4) {
            Image(systemName: symbol)
                .font(.system(size: 14, weight: .semibold))
            Text(title)
                .font(Typography.micro)
                .lineLimit(1)
        }
        .foregroundStyle(highlighted ? Palette.rose : Color.primary)
        .frame(maxWidth: .infinity)
        .frame(height: 54)
        .background(Palette.cardBackground, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 14, style: .continuous)
                .strokeBorder(highlighted ? Palette.rose.opacity(0.35) : Palette.stroke, lineWidth: 1)
        )
    }

    // MARK: - 风险提示

    private var riskNotice: some View {
        VStack(alignment: .leading, spacing: 7) {
            HStack(spacing: 6) {
                Image(systemName: item.risk.symbol)
                    .font(.system(size: 12, weight: .bold))
                Text("安全提示 · \(item.risk.title)")
                    .font(Typography.caption)
            }
            .foregroundStyle(item.risk.tint)

            Text(item.risk.detail)
                .font(Typography.micro)
                .foregroundStyle(.secondary)
                .fixedSize(horizontal: false, vertical: true)

            Text("建议：仅在可恢复设备上操作，先备份；不要在主力机上测试来源不明的 IPA 或漏洞利用工具。")
                .font(Typography.micro)
                .foregroundStyle(.secondary)
                .fixedSize(horizontal: false, vertical: true)
        }
        .padding(13)
        .background(item.risk.tint.opacity(0.10), in: RoundedRectangle(cornerRadius: 16, style: .continuous))
    }

    // MARK: - 规格

    private var specSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            sectionTitle("基本信息", symbol: "info.circle")

            LazyVGrid(
                columns: [GridItem(.flexible(), spacing: 8), GridItem(.flexible(), spacing: 8)],
                spacing: 8
            ) {
                InfoCell(title: "当前版本", value: releaseVersionText)
                InfoCell(title: "最近更新", value: updatedText)
                InfoCell(title: "支持 iOS", value: item.supportedIOSText)
                InfoCell(title: "支持设备", value: item.supportedDevicesText)
                InfoCell(title: "开发者", value: item.developer)
                InfoCell(title: "项目发布", value: item.releaseText)
            }

            statRow
        }
        .cardSurface(padding: 15)
    }

    /// 版本优先取 GitHub 最新 Release，读不到才退回种子数据（或「待验证」）。
    private var releaseVersionText: String {
        if let tag = remote.release?.tagName, !tag.isEmpty { return tag }
        return item.versionText
    }

    private var updatedText: String {
        if let pushed = remote.pushedAt { return RelativeTime.string(from: pushed) }
        return item.updatedText
    }

    @ViewBuilder
    private var statRow: some View {
        let stars = remote.stars ?? item.stars
        let forks = remote.forks ?? item.forks
        let language = remote.language ?? item.language

        if stars != nil || forks != nil || language != nil {
            HStack(spacing: 14) {
                if let stars {
                    Label("\(NumberText.compact(stars)) Star", systemImage: "star.fill")
                }
                if let forks {
                    Label("\(NumberText.compact(forks)) Fork", systemImage: "tuningfork")
                }
                if let language {
                    Label(language, systemImage: "chevron.left.forwardslash.chevron.right")
                }
                if remote.stars != nil {
                    Text("GitHub 实时")
                        .font(Typography.micro)
                        .foregroundStyle(Palette.mint)
                }
                Spacer(minLength: 0)
            }
            .font(Typography.micro)
            .foregroundStyle(.secondary)
        }
    }

    // MARK: - 简介

    private var summarySection: some View {
        VStack(alignment: .leading, spacing: 10) {
            sectionTitle("项目介绍", symbol: "text.alignleft")
            Text(item.summary)
                .font(Typography.body)
                .fixedSize(horizontal: false, vertical: true)
            if let detail = item.detail, !detail.isEmpty {
                Text(detail)
                    .font(Typography.body)
                    .foregroundStyle(.secondary)
                    .fixedSize(horizontal: false, vertical: true)
            }
            if !item.tags.isEmpty {
                tagFlow
            }
        }
        .cardSurface(padding: 15)
    }

    private var tagFlow: some View {
        VStack(alignment: .leading, spacing: 6) {
            ForEach(Array(tagRows.enumerated()), id: \.offset) { _, row in
                HStack(spacing: 6) {
                    ForEach(row, id: \.self) { tag in
                        Pill(text: tag, tint: item.category.tint)
                    }
                    Spacer(minLength: 0)
                }
            }
        }
    }

    /// 每行最多 3 个标签，避免宽度溢出。
    private var tagRows: [[String]] {
        stride(from: 0, to: item.tags.count, by: 3).map { start in
            Array(item.tags[start..<min(start + 3, item.tags.count)])
        }
    }

    // MARK: - 截图

    private var screenshotSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            sectionTitle("项目截图", symbol: "photo.on.rectangle")
            ScrollView(.horizontal) {
                HStack(spacing: 10) {
                    ForEach(item.screenshots, id: \.self) { url in
                        AsyncImage(url: url) { phase in
                            switch phase {
                            case .success(let image):
                                image.resizable().scaledToFill()
                            default:
                                RoundedRectangle(cornerRadius: 12, style: .continuous)
                                    .fill(Color.primary.opacity(0.06))
                                    .overlay(ProgressView())
                            }
                        }
                        .frame(width: 150, height: 300)
                        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                    }
                }
            }
            .scrollIndicators(.hidden)
        }
        .cardSurface(padding: 15)
    }

    // MARK: - 教程

    private var tutorialSection: some View {
        VStack(alignment: .leading, spacing: 9) {
            sectionTitle("使用教程", symbol: "book.fill")
            if let tutorialURL = item.tutorialURL {
                Link(destination: LinkRouter.resolved(tutorialURL)) {
                    HStack {
                        Text("打开官方安装 / 使用说明")
                            .font(Typography.body)
                        Spacer()
                        Image(systemName: "arrow.up.right")
                            .font(.system(size: 12, weight: .bold))
                    }
                    .foregroundStyle(Palette.brand)
                }
                .buttonStyle(.plain)
            } else {
                Text("该资源暂未提供独立教程。请打开 GitHub 首页的 README，按作者说明操作；不同 iOS 版本的步骤可能不同。")
                    .font(Typography.caption)
                    .foregroundStyle(.secondary)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
        .cardSurface(padding: 15)
    }

    // MARK: - 更新日志

    private func changelogSection(_ text: String) -> some View {
        VStack(alignment: .leading, spacing: 9) {
            sectionTitle("更新日志", symbol: "clock.arrow.circlepath")
            Text(text)
                .font(Typography.caption)
                .foregroundStyle(.secondary)
                .fixedSize(horizontal: false, vertical: true)
        }
        .cardSurface(padding: 15)
    }

    // MARK: - 最新发布

    @ViewBuilder
    private var releaseSection: some View {
        if let release = remote.release {
            VStack(alignment: .leading, spacing: 10) {
                HStack(spacing: 6) {
                    sectionTitle("最新发布", symbol: "shippingbox")
                    Spacer(minLength: 6)
                    if let published = release.publishedAt {
                        Text(RelativeTime.string(from: published))
                            .font(Typography.micro)
                            .foregroundStyle(.tertiary)
                    }
                }

                HStack(spacing: 8) {
                    Text(release.tagName)
                        .font(.system(size: 15, weight: .semibold, design: .rounded))
                        .foregroundStyle(Palette.brand)
                    if let name = release.name, !name.isEmpty, name != release.tagName {
                        Text(name)
                            .font(Typography.caption)
                            .foregroundStyle(.secondary)
                            .lineLimit(1)
                    }
                    Spacer(minLength: 0)
                }

                if !remote.releaseNotes.isEmpty {
                    MarkdownView(blocks: remote.releaseNotes)
                        .padding(.top, 2)
                }

                Link(destination: LinkRouter.resolved(release.htmlURL)) {
                    HStack(spacing: 6) {
                        Image(systemName: "arrow.up.right.square")
                            .font(.system(size: 11, weight: .bold))
                        Text("在 GitHub 查看 Release")
                            .font(Typography.micro)
                    }
                    .foregroundStyle(Palette.brand)
                }
                .buttonStyle(.plain)
            }
            .cardSurface(padding: 15)
        }
    }

    // MARK: - README

    private var readmeSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(spacing: 6) {
                sectionTitle("项目 README", symbol: "doc.richtext")
                Spacer(minLength: 6)
                if remote.state == .loading {
                    ProgressView()
                        .controlSize(.mini)
                }
            }

            switch remote.state {
            case .idle, .loading:
                LoadingRow(text: "正在从 GitHub 读取 README…")

            case .failed(let message):
                NoticeBanner(message: message, symbol: "wifi.exclamationmark") {
                    Task { await remote.reload(for: item) }
                }

            case .loaded:
                if remote.readmeBlocks.isEmpty {
                    Text("这个仓库没有可展示的 README，或内容无法解析。可以直接去 GitHub 查看。")
                        .font(Typography.caption)
                        .foregroundStyle(.secondary)
                        .fixedSize(horizontal: false, vertical: true)
                } else {
                    MarkdownView(blocks: remote.readmeBlocks)

                    if remote.readmeTruncated {
                        Text("README 较长，这里只展示前 2 万字符。")
                            .font(Typography.micro)
                            .foregroundStyle(.tertiary)
                    }
                }

                if let readmeURL = remote.readmeURL {
                    Link(destination: LinkRouter.resolved(readmeURL)) {
                        HStack(spacing: 6) {
                            Image(systemName: "arrow.up.right.square")
                                .font(.system(size: 11, weight: .bold))
                            Text("在 GitHub 查看完整 README")
                                .font(Typography.micro)
                        }
                        .foregroundStyle(Palette.brand)
                    }
                    .buttonStyle(.plain)
                }
            }
        }
        .cardSurface(padding: 15)
    }

    // MARK: - 来源

    private var sourceSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            sectionTitle("原始来源", symbol: "link")

            sourceRow(label: "项目主页", url: item.sourceURL)
            if let githubURL = item.githubURL {
                sourceRow(label: "GitHub 仓库", url: githubURL)
            }
            if let downloadURL = item.downloadURL {
                sourceRow(label: "官方下载", url: downloadURL)
            }
            if let developerURL = item.developerURL {
                sourceRow(label: "开发者主页", url: developerURL)
            }

            Text("iOS HUB 不托管任何安装包，以上链接均指向开发者或官方平台。")
                .font(Typography.micro)
                .foregroundStyle(.tertiary)
                .padding(.top, 2)
        }
        .cardSurface(padding: 15)
    }

    private func sourceRow(label: String, url: URL) -> some View {
        Link(destination: LinkRouter.resolved(url)) {
            HStack(spacing: 8) {
                Text(label)
                    .font(Typography.caption)
                    .foregroundStyle(.secondary)
                Spacer(minLength: 8)
                Text(url.host ?? url.absoluteString)
                    .font(Typography.micro)
                    .foregroundStyle(Palette.brand)
                    .lineLimit(1)
                Image(systemName: "arrow.up.right")
                    .font(.system(size: 10, weight: .bold))
                    .foregroundStyle(Palette.brand)
            }
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }

    // MARK: - 核对状态

    private var verificationSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(spacing: 6) {
                Image(systemName: item.compatibility.symbol)
                    .font(.system(size: 12, weight: .bold))
                    .foregroundStyle(item.compatibility.tint)
                Text("兼容性 · \(item.compatibility.title)")
                    .font(Typography.caption)
                Spacer()
                Text("核对：\(item.verifiedText)")
                    .font(Typography.micro)
                    .foregroundStyle(.tertiary)
            }

            if let note = item.verificationNote {
                Text(note)
                    .font(Typography.micro)
                    .foregroundStyle(.secondary)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
        .padding(13)
        .background(item.compatibility.tint.opacity(0.09), in: RoundedRectangle(cornerRadius: 16, style: .continuous))
    }

    // MARK: - 通用

    private func sectionTitle(_ text: String, symbol: String) -> some View {
        HStack(spacing: 6) {
            Image(systemName: symbol)
                .font(.system(size: 12, weight: .semibold))
                .foregroundStyle(item.category.tint)
            Text(text)
                .font(Typography.cardTitle)
        }
    }

    private func toggleFavorite() {
        withAnimation(.snappy) {
            LibraryService(context: modelContext).toggleFavorite(item)
        }
    }
}

// MARK: - 规格单元格

private struct InfoCell: View {
    let title: String
    let value: String

    var body: some View {
        VStack(alignment: .leading, spacing: 3) {
            Text(title)
                .font(Typography.micro)
                .foregroundStyle(.secondary)
            Text(value)
                .font(Typography.body)
                .lineLimit(2)
                .fixedSize(horizontal: false, vertical: true)
                .frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(11)
        .background(Color.primary.opacity(0.045), in: RoundedRectangle(cornerRadius: 12, style: .continuous))
    }
}

#Preview {
    NavigationStack {
        ResourceDetailView(item: .preview)
    }
    .modelContainer(PersistenceController.shared)
}
