//
//  Formatting.swift
//  iOSHUB
//
//  时间、数字等展示格式统一放在这里，避免各视图各写一套。
//

import Foundation

enum RelativeTime {
    /// 「3 天前」「刚刚」这类相对时间。
    static func string(from date: Date, relativeTo now: Date = Date()) -> String {
        let interval = now.timeIntervalSince(date)
        if interval < 60 { return "刚刚" }
        if interval < 3600 { return "\(Int(interval / 60)) 分钟前" }
        if interval < 86_400 { return "\(Int(interval / 3600)) 小时前" }
        if interval < 86_400 * 30 { return "\(Int(interval / 86_400)) 天前" }

        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "zh_Hans_CN")
        formatter.dateFormat = "yyyy 年 M 月 d 日"
        return formatter.string(from: date)
    }

    /// 「2024-08-01」这类绝对日期，用于来源核对记录。
    static func absolute(_ date: Date) -> String {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "zh_Hans_CN")
        formatter.dateFormat = "yyyy-MM-dd"
        return formatter.string(from: date)
    }
}

enum NumberText {
    /// 12_345 -> "1.2万"，1_234 -> "1,234"
    static func compact(_ value: Int) -> String {
        if value >= 10_000 {
            let wan = Double(value) / 10_000.0
            return String(format: "%.1f万", wan)
        }
        let formatter = NumberFormatter()
        formatter.numberStyle = .decimal
        formatter.locale = Locale(identifier: "zh_Hans_CN")
        return formatter.string(from: NSNumber(value: value)) ?? "\(value)"
    }
}
