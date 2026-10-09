import Foundation
import UIKit
import PushwooshInboxUI

/// Writes one call's style onto the SDK's inbox style.
///
/// `PWIInboxStyle.defaultStyle` hands out one process-wide instance that the SDK never resets, so
/// the defaults are captured the first time this type runs and put back before every apply:
/// without that, a key set by one presentInboxUI call would still be in effect for the next one,
/// which is not what the published contract promises.
///
/// Android applies the same key set to a different native object, so a change here needs the
/// matching change in InboxStyleApplier.java. A key that belongs to the other platform is skipped
/// silently; a value this platform cannot parse is skipped with a warning and the rest still apply.
enum InboxStyle {

    private static let colorPattern = "^(#[0-9A-Fa-f]{3}|(0x|#)([0-9A-Fa-f]{2})?[0-9A-Fa-f]{6})$"

    /// The date field symbols UTS #35 defines for a format pattern — and, checked letter by letter
    /// against this platform, exactly the ASCII letters `DateFormatter` renders anything at all for.
    private static let dateFieldSymbols: Set<Character> = Set("GyYuUrQqMLwWdDFgEecabBhHKkmsSAzZOvVXx")

    private static var defaults: Defaults?

    static func make(from options: [String: Any]?) -> PWIInboxStyle {
        let style: PWIInboxStyle = PWIInboxStyle.default()
        if defaults == nil {
            defaults = Defaults(style)
        }
        defaults?.restore(onto: style)
        guard let options = options else {
            return style
        }

        if let barTitle = string(options, "barTitle") {
            style.barTitle = barTitle
        }
        if let listEmptyMessage = string(options, "listEmptyMessage") {
            style.listEmptyMessage = listEmptyMessage
        }
        if let listErrorMessage = string(options, "listErrorMessage") {
            style.listErrorMessage = listErrorMessage
        }
        if let dateFormatterBlock = dateFormatterBlock(options) {
            style.dateFormatterBlock = dateFormatterBlock
        }

        if let defaultImageIcon = image(options, "defaultImageIcon") {
            style.defaultImageIcon = defaultImageIcon
        }
        if let listEmptyImage = image(options, "listEmptyImage") {
            style.listEmptyImage = listEmptyImage
        }
        if let listErrorImage = image(options, "listErrorImage") {
            style.listErrorImage = listErrorImage
        }
        if let unreadImage = image(options, "unreadImage") {
            style.unreadImage = unreadImage
        }

        if let value = color(options, "accentColor") {
            style.accentColor = value
        }
        if let value = color(options, "defaultTextColor") {
            style.defaultTextColor = value
        }
        if let value = color(options, "backgroundColor") {
            style.backgroundColor = value
        }
        if let value = color(options, "highlightColor") {
            style.selectionColor = value
        }
        if let value = color(options, "titleColor") {
            style.titleColor = value
        }
        if let value = color(options, "descriptionColor") {
            style.descriptionColor = value
        }
        if let value = color(options, "dateColor") {
            style.dateColor = value
        }
        if let value = color(options, "dividerColor") {
            style.separatorColor = value
        }
        if let value = color(options, "barBackgroundColor") {
            style.barBackgroundColor = value
        }
        if let value = color(options, "barAccentColor") {
            style.barAccentColor = value
        }
        if let value = color(options, "barTextColor") {
            style.barTextColor = value
        }

        return style
    }

    private static func string(_ options: [String: Any], _ key: String) -> String? {
        guard let raw = options[key] else {
            return nil
        }
        guard let text = raw as? String, !text.isEmpty else {
            warn(key, raw)
            return nil
        }
        return text
    }

    private static func color(_ options: [String: Any], _ key: String) -> UIColor? {
        guard let raw = string(options, key) else {
            return nil
        }
        guard raw.range(of: colorPattern, options: .regularExpression) != nil else {
            warn(key, raw)
            return nil
        }

        var digits = raw.hasPrefix("#") ? String(raw.dropFirst()) : String(raw.dropFirst(2))
        if digits.count == 3 {
            digits = digits.map { "\($0)\($0)" }.joined()
        }
        if digits.count == 6 {
            digits = "FF" + digits
        }
        guard let packed = UInt32(digits, radix: 16) else {
            warn(key, raw)
            return nil
        }
        let channel = { (shift: UInt32) in CGFloat((packed >> shift) & 0xFF) / 255.0 }
        return UIColor(red: channel(16), green: channel(8), blue: channel(0), alpha: channel(24))
    }

    private static func dateFormatterBlock(_ options: [String: Any]) -> PWIDateFormatterBlock? {
        guard let pattern = string(options, "dateFormat") else {
            return nil
        }
        guard isDatePattern(pattern) else {
            warn("dateFormat", pattern)
            return nil
        }
        let formatter = DateFormatter()
        formatter.dateFormat = pattern
        // A nil date is not provable from the header; fall back rather than crash a cell-rendering callback.
        let block: PWIDateFormatterBlock = { date, _ in formatter.string(from: date ?? Date()) }
        return block
    }

    /// Whether ICU can read the pattern.
    ///
    /// `DateFormatter.dateFormat` takes any string and reports it back unchanged, so a pattern it
    /// cannot read is only visible once a cell renders an empty or half-finished date. Two things
    /// make a pattern unreadable: an unpaired `'`, which leaves a literal running past the end of
    /// the pattern, and an unquoted ASCII letter outside the field symbols UTS #35 defines — every
    /// other letter is reserved and renders as nothing, which is what a typo looks like. Android
    /// gets the same answer out of `new SimpleDateFormat(pattern)` throwing.
    private static func isDatePattern(_ pattern: String) -> Bool {
        var quoted = false
        var index = pattern.startIndex
        while index < pattern.endIndex {
            let character = pattern[index]
            let next = pattern.index(after: index)
            if character == "'" {
                // Doubled quotes are one literal quote, inside a literal and outside one alike.
                if next < pattern.endIndex, pattern[next] == "'" {
                    index = pattern.index(after: next)
                    continue
                }
                quoted.toggle()
            } else if !quoted, character.isASCII, character.isLetter, !dateFieldSymbols.contains(character) {
                return false
            }
            index = next
        }
        return !quoted
    }

    private static func image(_ options: [String: Any], _ key: String) -> UIImage? {
        guard let raw = string(options, key) else {
            return nil
        }
        // Android's AssetManager rejects the doubled separator a leading slash leaves behind.
        let path = String(raw.drop(while: { $0 == "/" }))
        guard !path.isEmpty, let resources = Bundle.main.resourceURL else {
            warn(key, raw)
            return nil
        }
        let url = resources.appendingPathComponent("public").appendingPathComponent(path)
        guard let image = UIImage(contentsOfFile: url.path) else {
            warn(key, raw)
            return nil
        }
        return image
    }

    private static func warn(_ key: String, _ raw: Any) {
        NSLog("Inbox style: ignoring invalid value for '%@': %@", key, String(describing: raw))
    }

    /// The style the SDK had before the plugin first touched it.
    private struct Defaults {

        private let dateFormatterBlock: PWIDateFormatterBlock?
        private let barTitle: String?
        private let listEmptyMessage: String?
        private let listErrorMessage: String?
        private let defaultImageIcon: UIImage?
        private let listEmptyImage: UIImage?
        private let listErrorImage: UIImage?
        private let unreadImage: UIImage?
        private let accentColor: UIColor?
        private let defaultTextColor: UIColor?
        private let backgroundColor: UIColor?
        private let selectionColor: UIColor?
        private let titleColor: UIColor?
        private let descriptionColor: UIColor?
        private let dateColor: UIColor?
        private let separatorColor: UIColor?
        private let barBackgroundColor: UIColor?
        private let barAccentColor: UIColor?
        private let barTextColor: UIColor?

        init(_ style: PWIInboxStyle) {
            dateFormatterBlock = style.dateFormatterBlock
            barTitle = style.barTitle
            listEmptyMessage = style.listEmptyMessage
            listErrorMessage = style.listErrorMessage
            defaultImageIcon = style.defaultImageIcon
            listEmptyImage = style.listEmptyImage
            listErrorImage = style.listErrorImage
            unreadImage = style.unreadImage
            accentColor = style.accentColor
            defaultTextColor = style.defaultTextColor
            backgroundColor = style.backgroundColor
            selectionColor = style.selectionColor
            titleColor = style.titleColor
            descriptionColor = style.descriptionColor
            dateColor = style.dateColor
            separatorColor = style.separatorColor
            barBackgroundColor = style.barBackgroundColor
            barAccentColor = style.barAccentColor
            barTextColor = style.barTextColor
        }

        func restore(onto style: PWIInboxStyle) {
            style.dateFormatterBlock = dateFormatterBlock
            style.barTitle = barTitle
            style.listEmptyMessage = listEmptyMessage
            style.listErrorMessage = listErrorMessage
            style.defaultImageIcon = defaultImageIcon
            style.listEmptyImage = listEmptyImage
            style.listErrorImage = listErrorImage
            style.unreadImage = unreadImage
            style.accentColor = accentColor
            style.defaultTextColor = defaultTextColor
            style.backgroundColor = backgroundColor
            style.selectionColor = selectionColor
            style.titleColor = titleColor
            style.descriptionColor = descriptionColor
            style.dateColor = dateColor
            style.separatorColor = separatorColor
            style.barBackgroundColor = barBackgroundColor
            style.barAccentColor = barAccentColor
            style.barTextColor = barTextColor
        }
    }
}
