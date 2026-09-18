import XCTest
import Capacitor
// Only the SDK class the delegate lives on: a plain `import PushwooshFramework` would also pull in
// its own `Pushwoosh`, which collides with the plugin's class of the same name.
import class PushwooshFramework.PushNotificationManager
@testable import PushwooshPlugin

/// The other half of the handoff: the object a JS callback is registered on has to be the object
/// the native SDK delivers to.
///
/// `PushwooshPlugin` used to build two `Pushwoosh` instances — `notificationDelegateHandler` and
/// `implementation` — while `Pushwoosh.init()` assigns `PushNotificationManager.push().delegate = self`.
/// Both were stored properties with initializers, so which one ends up the delegate was decided by
/// their declaration order alone, while every `@objc` method routes to `implementation`. Nothing in
/// the file said the two had to agree: moving one declaration above the other would have sent every
/// push to an instance no plugin call is ever registered on, and the callbacks would have gone
/// silent with nothing in the diff pointing at the cause. These cases assert the wiring rather than
/// trusting the order.
final class PluginDelegateWiringTests: PluginCallTestCase {

    /// The delegate the SDK holds, as the plugin's own type. Fails the case rather than returning
    /// nil, so a broken wiring reads as "the delegate is not the plugin's instance" and not as a
    /// downstream nil-optional no-op.
    private func sdkDelegate(_ file: StaticString = #filePath, _ line: UInt = #line) -> Pushwoosh? {
        let delegate = PushNotificationManager.push().delegate
        guard let pushwoosh = delegate as? Pushwoosh else {
            XCTFail("the SDK delegate must be the plugin's Pushwoosh instance, got \(String(describing: delegate))",
                    file: file, line: line)
            return nil
        }
        return pushwoosh
    }

    func testPushReceivedRegisteredThroughThePluginLandsOnTheSDKDelegate() {
        let plugin = PushwooshPlugin()
        let call = makeCall("plugin-received", methodName: "pushReceivedCallback")

        plugin.pushReceivedCallback(call)

        sdkDelegate()?.pushReceivedPluginCall?.resolve([:])
        XCTAssertEqual(resolved, ["plugin-received"],
                       "the instance the SDK delivers to does not own the registered call")
        XCTAssertTrue(call.keepAlive, "a call routed through the plugin must come back persistent")
    }

    func testPushOpenedRegisteredThroughThePluginLandsOnTheSDKDelegate() {
        let plugin = PushwooshPlugin()
        let call = makeCall("plugin-opened", methodName: "pushOpenedCallback")

        plugin.pushOpenedCallback(call)

        sdkDelegate()?.pushOpenedPluginCall?.resolve([:])
        XCTAssertEqual(resolved, ["plugin-opened"],
                       "the instance the SDK delivers to does not own the registered call")
        XCTAssertTrue(call.keepAlive, "a call routed through the plugin must come back persistent")
    }

    /// A second plugin takes the delegate over — the SDK holds one delegate, and it is the newest
    /// instance's. Pins the consequence of the property being an instance and not a shared object:
    /// registrations made through an older plugin stop being reachable from the delegate.
    func testTheMostRecentlyBuiltPluginOwnsTheDelegate() {
        let first = PushwooshPlugin()
        first.pushReceivedCallback(makeCall("first-plugin", methodName: "pushReceivedCallback"))

        let second = PushwooshPlugin()
        second.pushReceivedCallback(makeCall("second-plugin", methodName: "pushReceivedCallback"))

        sdkDelegate()?.pushReceivedPluginCall?.resolve([:])

        XCTAssertEqual(resolved, ["second-plugin"])
    }
}
