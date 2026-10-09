import XCTest
import UIKit
import PushwooshInboxUI
@testable import PushwooshPlugin

/// What a caller can observe about the style: the keys they passed land on the native style
/// object, a value this platform cannot parse is skipped without taking its neighbours down, and
/// nothing a previous call set survives into a call that did not set it.
///
/// `PWIInboxStyle.defaultStyle` hands out one process-wide instance, so `make` returns the same
/// object every time and a value read off it after a call cannot be compared against one read off
/// it before — they are the same storage. Every default these cases compare against is therefore
/// read out and held as a value before the call that is supposed to leave it alone.
final class InboxStyleTests: XCTestCase {

    private let orange = UIColor(red: 1.0, green: 0.4, blue: 0.0, alpha: 1.0)

    /// The value of one key as the SDK ships it, read before anything in this case mutates it.
    private func defaultValue<T>(_ read: (PWIInboxStyle) -> T) -> T {
        read(InboxStyle.make(from: nil))
    }

    func testPassedKeysLandOnTheNativeStyle() throws {
        let style = InboxStyle.make(from: [
            "barTitle": "Demo Inbox",
            "accentColor": "#FF6600",
            "listEmptyMessage": "Nothing here yet",
            "dateFormat": "dd.MM.yyyy"
        ])

        XCTAssertEqual(style.barTitle, "Demo Inbox")
        XCTAssertEqual(style.listEmptyMessage, "Nothing here yet")
        XCTAssertEqual(style.accentColor, orange)
        let formatted = try XCTUnwrap(
            try XCTUnwrap(style.dateFormatterBlock)(Date(timeIntervalSince1970: 1_791_371_525), NSObject())
        )
        XCTAssertNotNil(
            formatted.range(of: "^\\d{2}\\.\\d{2}\\.\\d{4}$", options: .regularExpression),
            "unexpected formatted date: \(formatted)"
        )
    }

    func testLowercaseAndAlphaHexAreAccepted() {
        let style = InboxStyle.make(from: [
            "accentColor": "#ff6600",
            "titleColor": "0x80ff6600",
            "dateColor": "#f60"
        ])

        XCTAssertEqual(style.accentColor, orange)
        XCTAssertEqual(style.dateColor, orange)
        var alpha: CGFloat = 0
        style.titleColor.getRed(nil, green: nil, blue: nil, alpha: &alpha)
        XCTAssertEqual(alpha, 128.0 / 255.0, accuracy: 0.01)
    }

    func testAnUnparseableValueIsSkippedAndItsNeighboursStillApply() {
        let defaultAccent = defaultValue { $0.accentColor }
        let defaultBarTitle = defaultValue { $0.barTitle }
        let defaultListEmptyMessage = defaultValue { $0.listEmptyMessage }

        let style = InboxStyle.make(from: [
            "accentColor": "not a color",
            "barTitle": 42,
            "listEmptyMessage": ["text": "nope"],
            "titleColor": "#112233"
        ])

        XCTAssertEqual(style.accentColor, defaultAccent, "an unparseable color must not be written")
        XCTAssertEqual(style.barTitle, defaultBarTitle, "a non-string title must not be coerced")
        XCTAssertNotEqual(style.barTitle, "42", "a non-string title must not be coerced")
        XCTAssertEqual(style.listEmptyMessage, defaultListEmptyMessage)
        XCTAssertEqual(style.titleColor, UIColor(red: 0x11 / 255.0, green: 0x22 / 255.0, blue: 0x33 / 255.0, alpha: 1))
    }

    /// A pattern `DateFormatter` cannot read is not a pattern it refuses: it stores the string as
    /// given and renders an empty or half-finished date at the cell. Every case here is a pattern
    /// that reaches a cell as something other than a date, so none of them may be written.
    func testABrokenDatePatternIsSkippedAndItsNeighboursStillApply() {
        let date = Date(timeIntervalSince1970: 1_791_371_525)
        let owner = NSObject()
        let defaultFormatted = defaultValue { $0.dateFormatterBlock?(date, owner) }

        for broken in ["dd.MM.yyyy'", "'unclosed", "iii", "jjj", "dd.MM.yyyy 'y"] {
            let style = InboxStyle.make(from: ["dateFormat": broken, "barTitle": "Demo Inbox"])

            XCTAssertEqual(style.barTitle, "Demo Inbox", "'\(broken)' must not stop the other keys")
            XCTAssertEqual(style.dateFormatterBlock?(date, owner), defaultFormatted,
                           "'\(broken)' must leave the default formatting in place")
        }
    }

    /// The other half of the same guard: a pattern ICU does read has to survive it. Quoted literals
    /// and the doubled quote that stands for one literal quote are the part a balance check is most
    /// likely to get backwards.
    func testAQuotedLiteralInThePatternIsAccepted() throws {
        let date = Date(timeIntervalSince1970: 1_791_371_525)

        // The digits are the running device's timezone; only the literals are this case's business.
        for (pattern, expected) in [("dd.MM.yyyy''", "^\\d{2}\\.\\d{2}\\.\\d{4}'$"),
                                    ("dd/MM 'o''clock'", "^\\d{2}/\\d{2} o'clock$")] {
            let style = InboxStyle.make(from: ["dateFormat": pattern])
            let formatted = try XCTUnwrap(try XCTUnwrap(style.dateFormatterBlock)(date, NSObject()))

            XCTAssertNotNil(formatted.range(of: expected, options: .regularExpression),
                            "'\(pattern)' is a pattern ICU reads and must not be skipped, got '\(formatted)'")
        }
    }

    func testANonStringDateFormatIsIgnored() {
        let date = Date(timeIntervalSince1970: 1_791_371_525)
        let owner = NSObject()
        let defaultFormatted = defaultValue { $0.dateFormatterBlock?(date, owner) }

        let style = InboxStyle.make(from: ["dateFormat": 20261007, "barTitle": "Demo Inbox"])

        XCTAssertEqual(style.barTitle, "Demo Inbox")
        XCTAssertEqual(style.dateFormatterBlock?(date, owner), defaultFormatted,
                       "a non-string pattern must leave the default formatting in place")
    }

    func testKeysOfAPreviousCallDoNotSurviveIntoTheNextOne() {
        let defaultAccent = defaultValue { $0.accentColor }

        _ = InboxStyle.make(from: ["barTitle": "First", "accentColor": "#FF6600"])
        let second = InboxStyle.make(from: ["barTitle": "Second"])

        XCTAssertEqual(second.barTitle, "Second")
        XCTAssertNotEqual(second.accentColor, orange, "the accent of the previous call must be gone")
        XCTAssertEqual(second.accentColor, defaultAccent, "the accent must be back to the SDK default")
    }

    func testACallWithoutAStyleRestoresTheDefaults() {
        let defaultAccent = defaultValue { $0.accentColor }
        let defaultBarTitle = defaultValue { $0.barTitle }

        _ = InboxStyle.make(from: ["barTitle": "First", "accentColor": "#FF6600"])
        let bare = InboxStyle.make(from: nil)

        XCTAssertNotEqual(bare.barTitle, "First")
        XCTAssertNotEqual(bare.accentColor, orange)
        XCTAssertEqual(bare.barTitle, defaultBarTitle)
        XCTAssertEqual(bare.accentColor, defaultAccent)
    }

    func testAKeyOfTheOtherPlatformIsIgnoredWithoutAffectingTheRest() {
        let style = InboxStyle.make(from: ["readTitleColor": "#FF6600", "barTitle": "Demo Inbox"])

        XCTAssertEqual(style.barTitle, "Demo Inbox")
    }
}
