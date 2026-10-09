import XCTest
import Capacitor

/// Shared base for the cases that watch a plugin call get resolved: both suites need the same
/// recording `CAPPluginCall` and the same notification-center stub, and a change to either (the
/// initializer's shape, what a resolve records) has to reach them together.
class PluginCallTestCase: XCTestCase {

    /// Callback ids of the calls resolved so far, in resolve order.
    private(set) var resolved: [String] = []

    /// Callback ids of the calls rejected so far, paired with the message, in reject order.
    private(set) var rejected: [(callbackId: String, message: String)] = []

    override func setUp() {
        super.setUp()
        XCTAssertTrue(notificationCenterStubbed)
        resolved = []
        rejected = []
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

    /// A call that records its own rejection. The counterpart of `makeCall`, for the paths whose
    /// whole point is that they reject: there a resolve is the failure.
    func makeRejectingCall(_ callbackId: String, methodName: String, options: [String: Any] = [:]) -> CAPPluginCall {
        CAPPluginCall(
            callbackId: callbackId,
            methodName: methodName,
            options: options,
            success: { _, _ in
                XCTFail("the plugin call must not be resolved")
            },
            error: { [weak self] error in
                self?.rejected.append((callbackId, error?.message ?? "<nil>"))
            }
        )
    }
}
