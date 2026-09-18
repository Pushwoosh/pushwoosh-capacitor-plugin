import XCTest
import Capacitor
import UserNotifications
@testable import PushwooshPlugin

/// Regression guard for the `pushReceivedPluginCall` / `pushOpenedPluginCall` handoff.
///
/// The fields are written from the Capacitor bridge's serial queue (`CapacitorBridge.handleJSCall`
/// -> `dispatchQueue.async { plugin.perform(...) }`) and read from main, where the native SDK
/// invokes the plugin's `PushNotificationDelegate` methods. Two defects lived there:
///
///  1. no synchronization at all on a strong `CAPPluginCall?` — reproduced 3/3 as a
///     ThreadSanitizer data race on a live app (writer `pushReceivedCallback`, reader
///     `onPushReceived`), and fatal under `halt_on_error=1`;
///  2. check-then-act inside the reader: `if field != nil { ... field?.resolve(...) }` loads the
///     field twice, so a registration that lands between the two loads makes the delivery resolve
///     the NEW call and drop the one that passed the nil check on the floor.
///
/// (2) is what these tests pin down: the interleaving is injected deterministically instead of
/// being raced for, by having the subclass below run the concurrent registration from inside the
/// window — `createNotificationData` is exactly the code the reader executes between its two
/// loads. (1) needs ThreadSanitizer to be observable at all; `testConcurrentRegistrationAndDelivery`
/// below is the pair it watches, and the measurement lives in the runbook.
final class PluginCallHandoffTests: PluginCallTestCase {

    /// Runs a closure inside the reader's check-then-act window. `createNotificationData` is
    /// stubbed rather than delegated to `super` so the guard needs no UIApplication and no
    /// initialized native SDK.
    private final class WindowedPushwoosh: Pushwoosh {
        var insideWindow: (() -> Void)?
        var notificationDataCalls = 0

        override func createNotificationData(forPush pushNotification: [AnyHashable: Any], onStart: Bool) -> [String: Any] {
            notificationDataCalls += 1
            insideWindow?()
            return ["message": "stub"]
        }
    }

    private let payload: [AnyHashable: Any] = [
        "aps": ["alert": ["title": "title", "body": "body"]],
        "p": "hash"
    ]

    // MARK: - pushReceived

    func testPushReceivedResolvesTheRegisteredCall() {
        let plugin = WindowedPushwoosh()
        plugin.pushReceivedCallback(makeCall("received-1", methodName: "pushReceivedCallback"))

        plugin.onPushReceived(nil, withNotification: payload, onStart: false)

        XCTAssertEqual(resolved, ["received-1"])
    }

    func testPushReceivedResolvesTheCallThatPassedTheNilCheck() {
        let plugin = WindowedPushwoosh()
        plugin.pushReceivedCallback(makeCall("received-old", methodName: "pushReceivedCallback"))
        plugin.insideWindow = {
            plugin.pushReceivedCallback(self.makeCall("received-new", methodName: "pushReceivedCallback"))
        }

        plugin.onPushReceived(nil, withNotification: payload, onStart: false)

        XCTAssertEqual(resolved, ["received-old"], "a registration landing mid-delivery must not steal the delivery")
    }

    func testPushReceivedResolvesExactlyOnceWhenTheCallIsReregisteredInTheWindow() {
        let plugin = WindowedPushwoosh()
        let call = makeCall("received-same", methodName: "pushReceivedCallback")
        plugin.pushReceivedCallback(call)
        plugin.insideWindow = { plugin.pushReceivedCallback(call) }

        plugin.onPushReceived(nil, withNotification: payload, onStart: false)

        XCTAssertEqual(resolved, ["received-same"])
    }

    func testPushReceivedWithoutRegistrationIsANoOp() {
        let plugin = WindowedPushwoosh()

        plugin.onPushReceived(nil, withNotification: payload, onStart: false)

        XCTAssertEqual(resolved, [])
        XCTAssertEqual(plugin.notificationDataCalls, 0, "no notification data is built when nobody is listening")
    }

    func testPushReceivedCallbackKeepsTheCallAlive() {
        let plugin = WindowedPushwoosh()
        let call = makeCall("received-alive", methodName: "pushReceivedCallback")

        plugin.pushReceivedCallback(call)

        XCTAssertTrue(call.keepAlive, "the callback is persistent: the bridge must not release it after the first resolve")
        XCTAssertTrue(plugin.pushReceivedPluginCall === call)
    }

    // MARK: - pushOpened (the sibling field, same defect)

    func testPushAcceptedResolvesTheRegisteredCall() {
        let plugin = WindowedPushwoosh()
        plugin.pushOpenedCallback(makeCall("opened-1", methodName: "pushOpenedCallback"))

        plugin.onPushAccepted(nil, withNotification: payload, onStart: false)

        XCTAssertEqual(resolved, ["opened-1"])
    }

    func testPushAcceptedResolvesTheCallThatPassedTheNilCheck() {
        let plugin = WindowedPushwoosh()
        plugin.pushOpenedCallback(makeCall("opened-old", methodName: "pushOpenedCallback"))
        plugin.insideWindow = {
            plugin.pushOpenedCallback(self.makeCall("opened-new", methodName: "pushOpenedCallback"))
        }

        plugin.onPushAccepted(nil, withNotification: payload, onStart: false)

        XCTAssertEqual(resolved, ["opened-old"], "a registration landing mid-delivery must not steal the delivery")
    }

    func testPushAcceptedWithoutRegistrationIsANoOp() {
        let plugin = WindowedPushwoosh()

        plugin.onPushAccepted(nil, withNotification: payload, onStart: false)

        XCTAssertEqual(resolved, [])
        XCTAssertEqual(plugin.notificationDataCalls, 0)
    }

    func testPushOpenedCallbackKeepsTheCallAlive() {
        let plugin = WindowedPushwoosh()
        let call = makeCall("opened-alive", methodName: "pushOpenedCallback")

        plugin.pushOpenedCallback(call)

        XCTAssertTrue(call.keepAlive)
        XCTAssertTrue(plugin.pushOpenedPluginCall === call)
    }

    // MARK: - the unsynchronized pair itself

    /// The shape ThreadSanitizer reported 3/3 on a live app: registrations submitted to a serial
    /// background queue (the bridge queue) while deliveries read the same field on this thread.
    /// Without `-enableThreadSanitizer YES` this only asserts that no delivery ever resolves a call
    /// that was never registered; run under the sanitizer, an unsynchronized field makes it report.
    func testConcurrentRegistrationAndDelivery() {
        let plugin = WindowedPushwoosh()
        let bridgeQueue = DispatchQueue(label: "test-bridge")
        let registrations = 2_000
        var known = Set<String>(["received-seed"])
        plugin.pushReceivedCallback(makeCall("received-seed", methodName: "pushReceivedCallback"))

        let writesDone = expectation(description: "registrations submitted")
        for i in 0..<registrations {
            let callbackId = "received-\(i)"
            known.insert(callbackId)
            bridgeQueue.async {
                plugin.pushReceivedCallback(self.makeCall(callbackId, methodName: "pushReceivedCallback"))
                if i == registrations - 1 { writesDone.fulfill() }
            }
        }
        for _ in 0..<registrations {
            plugin.onPushReceived(nil, withNotification: payload, onStart: false)
        }
        wait(for: [writesDone], timeout: 30)

        XCTAssertEqual(resolved.count, registrations)
        XCTAssertTrue(resolved.allSatisfy { known.contains($0) }, "a delivery resolved a call that was never registered")
    }
}
