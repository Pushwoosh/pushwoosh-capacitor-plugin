import XCTest
import Capacitor
@testable import PushwooshPlugin

/// A call the host got wrong has to come back as a rejection naming the parameter, and it has to
/// come back before the SDK is touched — an empty code reaches the SDK as a real lookup and does
/// nothing at all, which from JavaScript is indistinguishable from a promise that never settles.
final class InboxValidationTests: PluginCallTestCase {

    func testMissingCodeIsRejectedByName() {
        XCTAssertNil(Inbox.requiredCode(makeRejectingCall("cb-1", methodName: "readMessage")))
        XCTAssertNil(Inbox.requiredCode(makeRejectingCall("cb-2", methodName: "readMessage", options: ["code": ""])))

        XCTAssertEqual(rejected.map(\.message), [
            "Missing required parameter: code",
            "Missing required parameter: code"
        ])
    }

    func testPresentCodeIsAccepted() {
        let call = makeRejectingCall("cb-1", methodName: "readMessage", options: ["code": "abc"])

        XCTAssertEqual(Inbox.requiredCode(call), "abc")
        XCTAssertTrue(rejected.isEmpty)
    }

    func testMissingOrEmptyCodesAreRejectedByName() {
        XCTAssertNil(Inbox.requiredCodes(makeRejectingCall("cb-1", methodName: "readMessages")))
        XCTAssertNil(Inbox.requiredCodes(makeRejectingCall("cb-2", methodName: "readMessages", options: ["codes": []])))

        XCTAssertEqual(rejected.map(\.message), [
            "Missing required parameter: codes",
            "Missing required parameter: codes"
        ])
    }

    func testACodesArrayHoldingAnEmptyOrNonStringEntryIsRejected() {
        XCTAssertNil(Inbox.requiredCodes(
            makeRejectingCall("cb-1", methodName: "readMessages", options: ["codes": ["abc", ""]])))
        XCTAssertNil(Inbox.requiredCodes(
            makeRejectingCall("cb-2", methodName: "readMessages", options: ["codes": ["abc", 7]])))

        XCTAssertEqual(rejected.map(\.message), [
            "Invalid parameter: codes",
            "Invalid parameter: codes"
        ])
    }

    func testAPopulatedCodesArrayIsAccepted() {
        let call = makeRejectingCall("cb-1", methodName: "readMessages", options: ["codes": ["abc", "def"]])

        XCTAssertEqual(Inbox.requiredCodes(call), ["abc", "def"])
        XCTAssertTrue(rejected.isEmpty)
    }
}
