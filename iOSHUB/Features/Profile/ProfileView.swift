//
//  ProfileView.swift
//  iOSHUB
//
//  我的：设备配置、关注分类、数据与网络、通知、关于。
//  所有设置存在本地 UserDefaults / SwiftData，不上传。
//

import SwiftUI
import SwiftData

struct ProfileView: View {
    @Environment(CatalogStore.self) private var catalog
    @Environment(\.modelContext) private var modelContext

    @AppStorage(AppSettings.userIOSVersionKey) private var iosVersion = ""
    @AppStorage(AppSettings.userDeviceKey) private var device = ""
    @AppStorage(AppSettings.notifyUpdatesKey) private var notifyUpdates = false
    @AppStorage(AppSettings.githubTokenKey) private var githubToken = ""

    @Query private var favorites: [FavoriteRecord]

    @ObservedObject private var router = LinkRouter.shared
    @State private var showDisclaimer = false

    var body: some View {
        NavigationStack {
            List {
                header

                Section {
                    HStack {
                        Text("iOS 版本")
                        Spacer()
                        TextField("如 17.5.1", text: $iosVersion)
                            .multilineTextAlignment(.trailing)
                            .keyboardType(.numbersAndPunctuation)
                            .foregroundStyle(.secondary)
                    }
                    HStack {
                        Text("设备型号")
                        Spacer()
                        TextField("如 iPhone 15 Pro", text: $device)
                            .multilineTextAlignment(.trailing)
                            .foregroundStyle(.secondary)
                    }
                    NavigationLink(value: AppRoute.followedCategories) {
                        Label("关注分类", systemImage: "star")
                    }
                } header: {
                    Text("我的设备")
                } footer: {
                    Text("填写后可让 App 在资源详情里对照兼容性；当前仅保存在本机。")
                }

                Section {
                    HStack {
                        Label("GitHub Token", systemImage: "key.fill")
                        Spacer()
                        SecureField("可选", text: $githubToken)
                            .multilineTextAlignment(.trailing)
                            .autocorrectionDisabled()
                            .textInputAutocapitalization(.never)
                    }
                    Button {
                        Task { await catalog.refreshFromNetwork() }
                    } label: {
                        Label(catalog.isRefreshing ? "正在刷新…" : "立即刷新远端数据", systemImage: "arrow.clockwise")
                    }
                    .disabled(catalog.isRefreshing)
                    if let refreshed = catalog.lastRemoteRefresh {
                        Text("上次刷新：\(RelativeTime.string(from: refreshed))")
                            .font(Typography.micro)
                            .foregroundStyle(.secondary)
                    }
                } header: {
                    Text("数据与网络")
                } footer: {
                    Text("未认证的 GitHub API 每小时只能请求 60 次。填写只读 Token 可提升到 5000 次/小时。Token 仅保存在本机 UserDefaults，不会上传到任何服务器。")
                }

                Section {
                    Picker(selection: $router.route) {
                        ForEach(LinkRoute.allCases) { route in
                            Text(route.label).tag(route)
                        }
                    } label: {
                        Label("镜像线路", systemImage: "arrow.triangle.swap")
                    }
                    if router.route == .custom {
                        TextField("前缀，如 https://gh-proxy.com/", text: $router.customPrefix)
                            .autocorrectionDisabled()
                            .textInputAutocapitalization(.never)
                            .keyboardType(.URL)
                    }
                    Button {
                        Task { await router.healthCheck() }
                    } label: {
                        Label(router.checking ? "正在测速…" : "测速并选最快线路", systemImage: "speedometer")
                    }
                    .disabled(router.checking)
                    ForEach(router.results) { health in
                        HStack {
                            Image(systemName: health.ok ? "checkmark.circle.fill" : "xmark.circle.fill")
                                .foregroundStyle(health.ok ? Color.green : Color.red)
                            Text(health.route.label)
                            Spacer()
                            Text(health.detail)
                                .font(Typography.micro)
                                .foregroundStyle(.secondary)
                        }
                    }
                } header: {
                    Text("网络线路")
                } footer: {
                    Text("GitHub 在国内经常连不上。这里只改写 github.com / api.github.com 的地址，其它网站照常直连；镜像不支持 /releases/latest 的跳转，这类链接会自动换成 releases 列表。")
                }

                Section("我的数据") {
                    HStack {
                        Text("已收藏")
                        Spacer()
                        Text("\(favorites.count)")
                            .foregroundStyle(.secondary)
                    }
                    HStack {
                        Text("资源库条目")
                        Spacer()
                        Text("\(catalog.items.count)")
                            .foregroundStyle(.secondary)
                    }
                    Button(role: .destructive) {
                        LibraryService(context: modelContext).clearSearchHistory()
                    } label: {
                        Text("清除搜索历史")
                    }
                    Button(role: .destructive) {
                        LibraryService(context: modelContext).clearHistory()
                    } label: {
                        Text("清除浏览历史")
                    }
                }

                Section {
                    Toggle(isOn: $notifyUpdates) {
                        Label("资源更新提醒", systemImage: "bell.badge")
                    }
                } header: {
                    Text("通知")
                } footer: {
                    Text("本地开关已就绪，推送通道为 Phase 4 预留：后续接入 APNs + 后端定时比对版本后才能真正推送。")
                }

                Section("关于") {
                    HStack {
                        Text("版本")
                        Spacer()
                        Text(appVersion)
                            .foregroundStyle(.secondary)
                    }
                    Button {
                        showDisclaimer = true
                    } label: {
                        Label("数据来源与免责声明", systemImage: "text.badge.checkmark")
                    }
                    if let url = URL(string: "https://github.com/opa334/TrollStore") {
                        Link(destination: LinkRouter.resolved(url)) {
                            Label("参考项目来源示例", systemImage: "arrow.up.right.square")
                        }
                    }
                }

                Section {
                    VStack(spacing: 10) {
                        BrandSignature(titleSize: 17)
                        BrandFooter(note: "一个把 iOS 生态资源集中起来的浏览器 · 不托管安装包，不生成新闻，不猜测兼容性")
                        Text("v\(appVersion)")
                            .font(Typography.micro)
                            .foregroundStyle(.tertiary)
                    }
                    .padding(.vertical, 6)
                }
                .listRowBackground(Color.clear)
            }
            .listStyle(.insetGrouped)
            .navigationTitle("我的")
            .appRouting()
            .sheet(isPresented: $showDisclaimer) {
                DisclaimerSheet()
            }
        }
    }

    private var header: some View {
        Section {
            HStack(spacing: 12) {
                ZStack {
                    Circle()
                        .fill(Palette.brandGradient)
                        .frame(width: 52, height: 52)
                    Image(systemName: "person.fill")
                        .font(.system(size: 20, weight: .semibold))
                        .foregroundStyle(.white)
                }

                VStack(alignment: .leading, spacing: 2) {
                    Text("本地用户")
                        .font(Typography.cardTitle)
                    Text(device.isEmpty ? "未设置设备" : device)
                        .font(Typography.caption)
                        .foregroundStyle(.secondary)
                }
                Spacer()
                Text(iosVersion.isEmpty ? "iOS 未设置" : "iOS \(iosVersion)")
                    .font(Typography.micro)
                    .foregroundStyle(Palette.brand)
                    .padding(.horizontal, 9)
                    .padding(.vertical, 5)
                    .background(Palette.brand.opacity(0.10), in: Capsule())
            }
            .padding(.vertical, 4)
        }
    }

    private var appVersion: String {
        let version = Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "1.0"
        let build = Bundle.main.infoDictionary?["CFBundleVersion"] as? String ?? "1"
        return "\(version) (\(build))"
    }
}

// MARK: - 免责声明

struct DisclaimerSheet: View {
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 14) {
                    item(
                        title: "资源来源",
                        text: "所有条目都指向开发者官网、GitHub 仓库或官方发布页。App 不托管、不分发任何 IPA、插件包或漏洞利用工具本身。"
                    )
                    item(
                        title: "版本与兼容性",
                        text: "标记「待验证」的版本号、支持系统与设备信息尚未核对。任何兼容性结论都以项目 Release 说明和 README 为准。"
                    )
                    item(
                        title: "资讯",
                        text: "资讯来自 Apple Developer、Apple 安全更新、GitHub Blog 等公开 RSS。标题与链接原样保留，不生成、不改写、不把旧闻当新闻。"
                    )
                    item(
                        title: "风险提示",
                        text: "越狱、巨魔、未签名 IPA 与漏洞研究工具可能违反设备保修条款，或带来数据风险。请在可恢复、非主力设备上测试，并提前备份。"
                    )
                    item(
                        title: "第三方接口",
                        text: "GitHub 数据来自 GitHub REST API，受其速率限制与可用性影响。接口异常时 App 会明确提示，并用内置数据兜底。"
                    )
                }
                .padding(.horizontal, Metric.screenPadding)
                .padding(.vertical, 16)
            }
            .navigationTitle("数据来源与免责声明")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("完成") { dismiss() }
                }
            }
        }
    }

    private func item(title: String, text: String) -> some View {
        VStack(alignment: .leading, spacing: 5) {
            Text(title)
                .font(Typography.cardTitle)
            Text(text)
                .font(Typography.caption)
                .foregroundStyle(.secondary)
                .fixedSize(horizontal: false, vertical: true)
        }
        .padding(13)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.primary.opacity(0.05), in: RoundedRectangle(cornerRadius: 14, style: .continuous))
    }
}

#Preview {
    ProfileView()
        .environment(CatalogStore())
        .modelContainer(PersistenceController.shared)
}
