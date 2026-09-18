import XCTest
import Capacitor

/// Shared base for the cases that watch a plugin call get resolved: both suites need the same
/// recording `CAPPluginCall` and the same notification-center stub, and a change to either (the
/// initializer's shape, what a resolve records) has to reach them together.
class PluginCallTestCase: XCTestCase {

    /// Callback ids of the calls resolved so far, in resolve order.
    private(set) var resolved: [String] = []

    override func setUp() {
        super.setUp()
        XCTAssertTrue(notificationCenterStubbed)
        resolved = []
    }

    /// A call that records its own resolve instead of posting through the bridge, which cannot be
    /// built in a test bundle. A reject fails the case: none of these paths is supposed to reject.
    func makeCall(_ callbackId: String, methodName: String) -> CAPPluginCall {
        CAPPluginCall(
            callbackId: callbackId,
            methodName: methodName,
            options: [:],
            success: { [weak self] _, call in
                self?.resolved.append(call?.callbackId ?? "<nil>")
            },
            error: { _ in
                XCTFail("the plugin call must not be rejected")
            }
        )
    }
}
