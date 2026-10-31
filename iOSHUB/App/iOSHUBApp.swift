//
//  iOSHUBApp.swift
//  iOSHUB
//
//  应用入口。装配：SwiftData 容器、全局 CatalogStore、主题色、品牌启动页。
//

import SwiftUI
import SwiftData

@main
struct iOSHUBApp: App {
    @State private var catalog = CatalogStore()
    @State private var showsLaunchBrand = true

    var body: some Scene {
        WindowGroup {
            ZStack {
                RootTabView()
                    .environment(catalog)
                    .tint(Palette.brand)

                if showsLaunchBrand {
                    LaunchBrandView {
                        withAnimation(.easeOut(duration: 0.2)) {
                            showsLaunchBrand = false
                        }
                    }
                    .transition(.opacity)
                    .zIndex(2)
                }
            }
            .task {
                await catalog.loadIfNeeded()
            }
        }
        .modelContainer(PersistenceController.shared)
    }
}
