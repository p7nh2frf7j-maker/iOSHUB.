//
//  MarkdownRenderer.swift
//  iOSHUB
//
//  轻量 Markdown 渲染（零依赖）：
//  支持标题 / 段落 / 有序无序列表 / 代码块 / 引用 / 分隔线，
//  行内 **粗体**、*斜体*、`代码`、[链接](url) 用 AttributedString 解析。
//
//  为什么不用第三方：README 只需要「读得懂」，不是为了 100% 还原 GitHub 排版；
//  表格与图片（徽章）在手机窄屏上没有阅读价值，直接跳过。
//

import SwiftUI

// MARK: - 块模型

enum MarkdownBlock {
    case heading(level: Int, text: String)
    case paragraph(String)
    case bullet([String])
    case ordered([String])
    case code(String)
    case quote(String)
    case divider
}

// MARK: - 解析

enum MarkdownParser {
    static func parse(_ raw: String, maxBlocks: Int = 150) -> [MarkdownBlock] {
        let normalized = raw
            .replacingOccurrences(of: "\r\n", with: "\n")
            .replacingOccurrences(of: "\r", with: "\n")

        var blocks: [MarkdownBlock] = []
        var paragraph: [String] = []
        var bullets: [String] = []
        var ordered: [String] = []
        var code: [String] = []
        var inCode = false

        func flushParagraph() {
            guard !paragraph.isEmpty else { return }
            let text = paragraph.joined(separator: " ").trimmingCharacters(in: .whitespaces)
            paragraph.removeAll()
            guard !text.isEmpty else { return }
            blocks.append(.paragraph(text))
        }

        func flushBullets() {
            guard !bullets.isEmpty else { return }
            blocks.append(.bullet(bullets))
            bullets.removeAll()
        }

        func flushOrdered() {
            guard !ordered.isEmpty else { return }
            blocks.append(.ordered(ordered))
            ordered.removeAll()
        }

        func flushAll() {
            flushParagraph()
            flushBullets()
            flushOrdered()
        }

        for line in normalized.components(separatedBy: "\n") {
            if blocks.count >= maxBlocks { break }

            let trimmed = line.trimmingCharacters(in: .whitespaces)

            // 代码围栏
            if trimmed.hasPrefix("```") {
                if inCode {
                    blocks.append(.code(code.joined(separator: "\n")))
                    code.removeAll()
                    inCode = false
                } else {
                    flushAll()
                    inCode = true
                }
                continue
            }
            if inCode {
                code.append(line)
                continue
            }

            // 空行 = 段落结束
            if trimmed.isEmpty {
                flushAll()
                continue
            }

            // 注释、表格分隔行、纯图片行（徽章）直接跳过
            if trimmed.hasPrefix("<!--") { continue }
            if isTableSeparator(trimmed) { continue }
            if isImageOnly(trimmed) { continue }
            if isHTMLTag(trimmed) { continue }

            // 标题
            if let heading = headingLevel(trimmed) {
                flushAll()
                let text = String(trimmed.dropFirst(heading)).trimmingCharacters(in: .whitespaces)
                if !text.isEmpty {
                    blocks.append(.heading(level: min(heading, 6), text: stripInlineHTML(text)))
                }
                continue
            }

            // 分隔线
            if trimmed == "---" || trimmed == "***" || trimmed == "___" {
                flushAll()
                blocks.append(.divider)
                continue
            }

            // 引用
            if trimmed.hasPrefix(">") {
                flushAll()
                let text = trimmed.dropFirst().trimmingCharacters(in: .whitespaces)
                if !text.isEmpty {
                    blocks.append(.quote(stripInlineHTML(text)))
                }
                continue
            }

            // 无序列表
            if let item = listItem(trimmed, markers: ["- ", "* ", "+ "]) {
                flushParagraph()
                flushOrdered()
                bullets.append(stripInlineHTML(item))
                continue
            }

            // 有序列表
            if let item = orderedItem(trimmed) {
                flushParagraph()
                flushBullets()
                ordered.append(stripInlineHTML(item))
                continue
            }

            paragraph.append(stripInlineHTML(trimmed))
        }

        if inCode, !code.isEmpty {
            blocks.append(.code(code.joined(separator: "\n")))
        }
        flushAll()

        return Array(blocks.prefix(maxBlocks))
    }

    // MARK: 行内解析

    static func inline(_ text: String) -> AttributedString {
        let options = AttributedString.MarkdownParsingOptions(
            interpretedSyntax: .inlineOnlyPreservingWhitespace
        )
        if let attributed = try? AttributedString(markdown: text, options: options) {
            return attributed
        }
        return AttributedString(text)
    }

    // MARK: 行判定

    private static func headingLevel(_ line: String) -> Int? {
        var count = 0
        for character in line {
            if character == "#" {
                count += 1
            } else {
                break
            }
        }
        guard count > 0, count <= 6 else { return nil }
        let rest = line.dropFirst(count)
        guard rest.hasPrefix(" ") else { return nil }
        return count
    }

    private static func listItem(_ line: String, markers: [String]) -> String? {
        for marker in markers where line.hasPrefix(marker) {
            return String(line.dropFirst(marker.count)).trimmingCharacters(in: .whitespaces)
        }
        return nil
    }

    private static func orderedItem(_ line: String) -> String? {
        guard let dot = line.firstIndex(of: ".") else { return nil }
        let prefix = line[line.startIndex..<dot]
        guard !prefix.isEmpty,
              prefix.count <= 3,
              prefix.allSatisfy(\.isNumber),
              line.index(after: dot) < line.endIndex,
              line[line.index(after: dot)] == " "
        else { return nil }
        return String(line[line.index(dot, offsetBy: 2)...]).trimmingCharacters(in: .whitespaces)
    }

    private static func isTableSeparator(_ line: String) -> Bool {
        guard line.contains("-"), line.contains("|") else { return false }
        let allowed = CharacterSet(charactersIn: "|-: ")
        return line.unicodeScalars.allSatisfy { allowed.contains($0) }
    }

    private static func isImageOnly(_ line: String) -> Bool {
        guard line.hasPrefix("!["), line.hasSuffix(")") else { return false }
        return line.contains("](")
    }

    private static func isHTMLTag(_ line: String) -> Bool {
        guard line.hasPrefix("<"), line.hasSuffix(">"), !line.contains(" ") else { return false }
        return line.count < 60
    }

    private static func stripInlineHTML(_ text: String) -> String {
        var result = text.replacingOccurrences(of: "<[^>]+>", with: "", options: .regularExpression)
        result = result.replacingOccurrences(of: "&nbsp;", with: " ")
        result = result.replacingOccurrences(of: "&amp;", with: "&")
        result = result.replacingOccurrences(of: "&lt;", with: "<")
        result = result.replacingOccurrences(of: "&gt;", with: ">")
        return result.trimmingCharacters(in: .whitespaces)
    }
}

// MARK: - 渲染

struct MarkdownView: View {
    let blocks: [MarkdownBlock]
    var compact: Bool = true

    var body: some View {
        VStack(alignment: .leading, spacing: 9) {
            ForEach(Array(blocks.enumerated()), id: \.offset) { _, block in
                blockView(block)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    @ViewBuilder
    private func blockView(_ block: MarkdownBlock) -> some View {
        switch block {
        case .heading(let level, let text):
            Text(MarkdownParser.inline(text))
                .font(headingFont(level))
                .foregroundStyle(.primary)
                .fixedSize(horizontal: false, vertical: true)
                .padding(.top, level <= 2 ? 6 : 2)
                .frame(maxWidth: .infinity, alignment: .leading)

        case .paragraph(let text):
            Text(MarkdownParser.inline(text))
                .font(bodyFont)
                .foregroundStyle(.secondary)
                .fixedSize(horizontal: false, vertical: true)
                .frame(maxWidth: .infinity, alignment: .leading)

        case .bullet(let items):
            VStack(alignment: .leading, spacing: 5) {
                ForEach(Array(items.enumerated()), id: \.offset) { _, item in
                    listRow(marker: "•", text: item)
                }
            }

        case .ordered(let items):
            VStack(alignment: .leading, spacing: 5) {
                ForEach(Array(items.enumerated()), id: \.offset) { index, item in
                    listRow(marker: "\(index + 1).", text: item)
                }
            }

        case .code(let text):
            ScrollView(.horizontal) {
                Text(text)
                    .font(.system(size: 11, weight: .regular, design: .monospaced))
                    .foregroundStyle(.primary)
                    .padding(10)
            }
            .scrollIndicators(.hidden)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(
                Color.primary.opacity(0.055),
                in: RoundedRectangle(cornerRadius: 10, style: .continuous)
            )

        case .quote(let text):
            HStack(alignment: .top, spacing: 8) {
                Rectangle()
                    .fill(Palette.brand.opacity(0.45))
                    .frame(width: 2)
                Text(MarkdownParser.inline(text))
                    .font(bodyFont)
                    .foregroundStyle(.secondary)
                    .fixedSize(horizontal: false, vertical: true)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
            .fixedSize(horizontal: false, vertical: true)

        case .divider:
            Divider().opacity(0.4)
        }
    }

    private func listRow(marker: String, text: String) -> some View {
        HStack(alignment: .firstTextBaseline, spacing: 7) {
            Text(marker)
                .font(.system(size: 11, weight: .semibold, design: .rounded))
                .foregroundStyle(Palette.brand.opacity(0.8))
                .frame(minWidth: 12, alignment: .trailing)
            Text(MarkdownParser.inline(text))
                .font(bodyFont)
                .foregroundStyle(.secondary)
                .fixedSize(horizontal: false, vertical: true)
                .frame(maxWidth: .infinity, alignment: .leading)
        }
    }

    private var bodyFont: Font {
        compact ? .system(size: 12) : .system(size: 14)
    }

    private func headingFont(_ level: Int) -> Font {
        switch level {
        case 1: return .system(size: 17, weight: .bold)
        case 2: return .system(size: 15, weight: .bold)
        case 3: return .system(size: 13.5, weight: .semibold)
        default: return .system(size: 12.5, weight: .semibold)
        }
    }
}

#Preview {
    ScrollView {
        MarkdownView(blocks: MarkdownParser.parse("""
        # TrollStore

        永久签名安装 IPA，**无需越狱**。

        ## 安装

        1. 下载 `TrollStore.tar`
        2. 打开 TrollStore Helper
        3. 重启设备

        - 支持 iOS 14.0 – 16.6.1
        - 依赖 CoreTrust 漏洞

        ```bash
        git clone https://github.com/opa334/TrollStore
        ```

        > 请注意：修改系统文件有风险。
        """))
        .padding()
    }
}
