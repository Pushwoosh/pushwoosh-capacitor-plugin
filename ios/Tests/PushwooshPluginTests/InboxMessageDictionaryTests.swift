import XCTest
import PushwooshFramework
@testable import PushwooshPlugin

/// The dictionary a loaded message turns into is what every host reads, and it has to be the same
/// shape Android produces from a different native model. What is pinned here: the declared
/// fields carry the declared types, a field the native message does not have is absent rather than
/// an empty string, and a message with missing or malformed action parameters still comes through.
/// A message the SDK reports without a code is not a message at all: every mutator takes that code,
/// so it is dropped instead of being handed to the host with an identifier nothing accepts.
final class InboxMessageDictionaryTests: XCTestCase {

    private final class StubMessage: NSObject, PWInboxMessageProtocol {
        var code: String! = "code-1"
        var title: String! = "Title"
        var message: String! = "Body"
        var imageUrl: String! = "https://example.com/icon.png"
        var sendDate: Date! = Date(timeIntervalSince1970: 1_791_371_525)
        var type: PWInboxMessageType = .URL
        var isRead: Bool = false
        var isActionPerformed: Bool = false
        var actionParams: [AnyHashable: Any]! = [
            "l": "https://example.com",
            "b": "https://example.com/banner.png",
            "u": "{\"orderId\":42}"
        ]
        var attachmentUrl: String! = "https://example.com/attachment.png"
    }

    func testFullMessageCarriesEveryDeclaredField() throws {
        let message = StubMessage()
        message.isRead = true

        let json = try XCTUnwrap(InboxMessageDictionary.make(from: message))

        XCTAssertEqual(json["code"] as? String, "code-1")
        XCTAssertEqual(json["title"] as? String, "Title")
        XCTAssertEqual(json["message"] as? String, "Body")
        XCTAssertEqual(json["imageUrl"] as? String, "https://example.com/icon.png")
        XCTAssertEqual(json["bannerUrl"] as? String, "https://example.com/banner.png")
        XCTAssertEqual(json["type"] as? Int, 2)
        XCTAssertEqual(json["isRead"] as? Bool, true)
        XCTAssertEqual(json["isActionPerformed"] as? Bool, false)
        XCTAssertEqual((json["actionParams"] as? [String: Any])?["l"] as? String, "https://example.com")
        XCTAssertEqual((json["customData"] as? [String: Any])?["orderId"] as? Int, 42)
    }

    func testSendDateIsIso8601WithAnOffset() throws {
        let json = try XCTUnwrap(InboxMessageDictionary.make(from: StubMessage()))

        let sendDate = try XCTUnwrap(json["sendDate"] as? String)
        // The same pattern Android's getISO8601SendDate produces: yyyy-MM-dd'T'HH:mm:ssZ
        XCTAssertNotNil(
            sendDate.range(of: "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}[+-]\\d{4}$", options: .regularExpression),
            "unexpected sendDate format: \(sendDate)"
        )
    }

    func testMissingFieldsAreOmittedRatherThanEmptied() throws {
        let message = StubMessage()
        message.title = nil
        message.imageUrl = ""
        message.actionParams = ["l": "https://example.com"]

        let json = try XCTUnwrap(InboxMessageDictionary.make(from: message))

        XCTAssertNil(json["title"], "an absent title must not become an empty string")
        XCTAssertNil(json["imageUrl"])
        XCTAssertNil(json["bannerUrl"])
        XCTAssertNil(json["customData"])
        XCTAssertEqual(json["code"] as? String, "code-1")
    }

    func testBlankOrMalformedActionParamsStillProduceAMessage() throws {
        let empty = StubMessage()
        empty.actionParams = [:]
        let fromEmpty = try XCTUnwrap(InboxMessageDictionary.make(from: empty))
        XCTAssertEqual(fromEmpty["code"] as? String, "code-1")
        XCTAssertNil(fromEmpty["actionParams"])
        XCTAssertNil(fromEmpty["customData"])

        let malformed = StubMessage()
        malformed.actionParams = ["u": "{not json"]
        let fromMalformed = try XCTUnwrap(InboxMessageDictionary.make(from: malformed))
        XCTAssertEqual(fromMalformed["code"] as? String, "code-1")
        XCTAssertNotNil(fromMalformed["actionParams"])
        XCTAssertNil(fromMalformed["customData"])
    }

    func testAMessageWithoutACodeIsNotReportedAtAll() {
        let missing = StubMessage()
        missing.code = nil
        XCTAssertNil(InboxMessageDictionary.make(from: missing),
                     "a message with no code must not reach the host")

        let blank = StubMessage()
        blank.code = ""
        XCTAssertNil(InboxMessageDictionary.make(from: blank),
                     "an empty code is an identifier every mutator rejects")
    }

    func testCustomDataSurvivesBothTransportsAndIsSkippedWhenItIsNeither() throws {
        let asObject = StubMessage()
        asObject.actionParams = ["u": ["orderId": 42]]
        let fromObject = try XCTUnwrap(InboxMessageDictionary.make(from: asObject))
        XCTAssertEqual((fromObject["customData"] as? [String: Any])?["orderId"] as? Int, 42)

        let asScalar = StubMessage()
        asScalar.actionParams = ["u": "plain text"]
        let fromScalar = try XCTUnwrap(InboxMessageDictionary.make(from: asScalar))
        XCTAssertNil(fromScalar["customData"])
    }
}
