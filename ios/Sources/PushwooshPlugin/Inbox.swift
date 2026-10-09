import Foundation
import Capacitor
import PushwooshFramework
import UIKit
import PushwooshInboxUI

/// Message Inbox half of the plugin.
///
/// Every entry point settles its CAPPluginCall exactly once: a native failure rejects with the
/// reason in the message — including the counters, where the native API reports an error that
/// the other Pushwoosh plugins drop on the floor — and a call whose arguments are wrong is
/// rejected here, before the SDK is asked anything, naming the parameter at fault.
@objc public class Inbox: NSObject {

    /// How long the presenting transition has to call back before the caller is told it failed.
    /// Settable so a test does not have to spend it.
    static var screenTimeout: TimeInterval = 10

    /// The inbox screen this object put up, for as long as it is up. Weak: whoever presented it
    /// owns it, and a screen dismissed any other way than the Close button has to let go on its own.
    private weak var presentedInbox: UINavigationController?

    /// The call waiting on the presenting transition, which is the window `presentedInbox` cannot
    /// cover. Main queue only, like everything that touches it.
    private var pending: CAPPluginCall?

    private var screenNeverCameUp: DispatchWorkItem?

    public func presentInboxUI(_ call: CAPPluginCall, from presenter: @escaping () -> UIViewController?) {
        DispatchQueue.main.async {
            guard let presenter = presenter() else {
                call.reject("Failed to present the inbox: the plugin has no view controller")
                return
            }
            // A second inbox has to be refused before UIKit silently drops it and never calls back.
            guard self.pending == nil, self.presentedInbox == nil else {
                call.reject("Failed to present the inbox: an inbox screen is already on screen")
                return
            }
            let style = InboxStyle.make(from: call.options as? [String: Any])
            // The factory is nullable in the imported header, and a trap is not a rejection.
            guard let controller: PWIInboxViewController = PWIInboxUI.createInboxController(with: style) else {
                call.reject("Failed to present the inbox: the SDK built no inbox screen")
                return
            }
            controller.navigationItem.leftBarButtonItem = UIBarButtonItem(
                title: NSLocalizedString("Close", comment: "Close"),
                style: .plain,
                target: self,
                action: #selector(self.closeInbox)
            )
            let navigation = UINavigationController(rootViewController: controller)
            self.presentedInbox = navigation
            self.pending = call
            self.startTimeout()
            Inbox.topMost(from: presenter).present(navigation, animated: true) {
                guard let presented = self.settle() else {
                    return
                }
                NSLog("Inbox UI presented")
                presented.resolve()
            }
        }
    }

    /// Answers the caller if `present(_:animated:completion:)` never does.
    ///
    /// UIKit is not under contract to call that completion — a presentation it drops, a presenter
    /// torn down mid-transition, a window that never becomes key — and the call would then sit
    /// unsettled forever while every later presentInboxUI is refused as "already on screen".
    /// Nothing is dismissed here: `presentedInbox` is weak, so a presentation that never happened
    /// has already let go and the next call is free, while a screen that did come up and simply
    /// never reported itself stays up and stays the reason to refuse.
    private func startTimeout() {
        let work = DispatchWorkItem { [weak self] in
            self?.settle()?.reject(
                "Failed to present the inbox: the inbox screen did not come up within "
                    + "\(Int(Inbox.screenTimeout)) seconds"
            )
        }
        screenNeverCameUp = work
        DispatchQueue.main.asyncAfter(deadline: .now() + Inbox.screenTimeout, execute: work)
    }

    /// Takes the call still waiting, if any, and drops the in-flight state so the next one can start.
    @discardableResult
    private func settle() -> CAPPluginCall? {
        guard let call = pending else {
            return nil
        }
        pending = nil
        screenNeverCameUp?.cancel()
        screenNeverCameUp = nil
        return call
    }

    /// The controller the inbox goes above. Anything already modal over the presenter — a system
    /// alert, a share sheet, the browser the inbox itself opened for a URL message — is something
    /// to present over, not a reason to refuse: only another inbox is that.
    private static func topMost(from presenter: UIViewController) -> UIViewController {
        var top = presenter
        while let presented = top.presentedViewController {
            top = presented
        }
        return top
    }

    @objc func closeInbox() {
        presentedInbox?.dismiss(animated: true)
        presentedInbox = nil
    }

    public func loadMessages(_ call: CAPPluginCall) {
        PWInbox.loadMessages { messages, error in
            if let error = error {
                call.reject("Failed to load inbox messages: \(error.localizedDescription)")
                return
            }
            let payload = (messages ?? []).compactMap { InboxMessageDictionary.make(from: $0) }
            NSLog("Inbox messages loaded: %ld", payload.count)
            call.resolve(["messages": payload])
        }
    }

    public func messagesCount(_ call: CAPPluginCall) {
        PWInbox.messagesCount { count, error in
            Inbox.resolveCount(call, count, error, "Failed to get inbox messages count")
        }
    }

    public func unreadMessagesCount(_ call: CAPPluginCall) {
        PWInbox.unreadMessagesCount { count, error in
            Inbox.resolveCount(call, count, error, "Failed to get unread inbox messages count")
        }
    }

    public func messagesWithNoActionPerformedCount(_ call: CAPPluginCall) {
        PWInbox.messagesWithNoActionPerformedCount { count, error in
            Inbox.resolveCount(call, count, error, "Failed to get inbox messages with no action performed count")
        }
    }

    public func readMessage(_ call: CAPPluginCall) {
        guard let code = Inbox.requiredCode(call) else {
            return
        }
        PWInbox.readMessages(withCodes: [code])
        call.resolve()
    }

    public func readMessages(_ call: CAPPluginCall) {
        guard let codes = Inbox.requiredCodes(call) else {
            return
        }
        PWInbox.readMessages(withCodes: codes)
        call.resolve()
    }

    public func deleteMessage(_ call: CAPPluginCall) {
        guard let code = Inbox.requiredCode(call) else {
            return
        }
        PWInbox.deleteMessages(withCodes: [code])
        call.resolve()
    }

    public func deleteMessages(_ call: CAPPluginCall) {
        guard let codes = Inbox.requiredCodes(call) else {
            return
        }
        PWInbox.deleteMessages(withCodes: codes)
        call.resolve()
    }

    public func performAction(_ call: CAPPluginCall) {
        guard let code = Inbox.requiredCode(call) else {
            return
        }
        PWInbox.performActionForMessage(withCode: code)
        call.resolve()
    }

    static func requiredCode(_ call: CAPPluginCall) -> String? {
        guard let code = call.getString("code"), !code.isEmpty else {
            call.reject("Missing required parameter: code")
            return nil
        }
        return code
    }

    static func requiredCodes(_ call: CAPPluginCall) -> [String]? {
        guard let raw = call.getArray("codes"), !raw.isEmpty else {
            call.reject("Missing required parameter: codes")
            return nil
        }
        let codes = raw.compactMap { $0 as? String }.filter { !$0.isEmpty }
        guard codes.count == raw.count else {
            call.reject("Invalid parameter: codes")
            return nil
        }
        return codes
    }

    private static func resolveCount(_ call: CAPPluginCall, _ count: Int, _ error: Error?, _ failure: String) {
        if let error = error {
            call.reject("\(failure): \(error.localizedDescription)")
            return
        }
        call.resolve(["value": count])
    }
}
