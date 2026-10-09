import XCTest
import UIKit
import Capacitor
import ObjectiveC.runtime
@testable import PushwooshPlugin

/// What `Inbox.presentInboxUI` is allowed to refuse, and what it has to present over.
///
/// `present(_:animated:completion:)` only reports success through its completion handler, which
/// UIKit fires after the modal transition finishes — not when the call is made. A second
/// `presentInboxUI` arriving in that window must not reach `present` a second time: UIKit drops a
/// second presentation attempt on a view controller that already has one in flight and never calls
/// that completion, so a guard that is still looking at "has anything finished presenting yet" lets
/// the race through and leaves the second call's promise unsettled forever.
///
/// The other half is that the guard has to be about the inbox and nothing else. A system alert, a
/// share sheet or the browser the inbox itself opened for a URL message all sit modally above the
/// bridge's root controller, and none of them is a reason to tell the caller an inbox is on screen:
/// the inbox goes above them.
///
/// `RecordingPresenter` stands in for the real presenter: it records every `present` call instead of
/// performing one, and lets the test decide exactly when — or whether — the completion fires. That
/// reproduces the open transition window deterministically, without a host app or a key window, and
/// without touching `Inbox`'s private state.
final class InboxPresentationTests: PluginCallTestCase {

    private final class RecordingPresenter: UIViewController {
        private(set) var presentCalls: [(controller: UIViewController, completion: (() -> Void)?)] = []
        private var currentlyPresented: UIViewController?

        override var presentedViewController: UIViewController? {
            currentlyPresented
        }

        override func present(_ viewControllerToPresent: UIViewController, animated flag: Bool, completion: (() -> Void)?) {
            presentCalls.append((viewControllerToPresent, completion))
            currentlyPresented = viewControllerToPresent
        }

        func completePresentation(at index: Int) {
            presentCalls[index].completion?()
        }

        /// What UIKit does once a dismissal finishes: the presenter stops answering with the
        /// controller it used to have up.
        func endPresentation() {
            currentlyPresented = nil
        }
    }

    /// Runs the main-queue work the `presentInboxUI` calls enqueued, in the order they enqueued it,
    /// before their effect is inspected.
    private func drainMainQueue() {
        let drained = expectation(description: "main queue drained")
        DispatchQueue.main.async { drained.fulfill() }
        wait(for: [drained], timeout: 1)
    }

    func testASecondCallArrivingBeforeTheFirstCompletesIsRejectedAndSettlesItsOwnPromise() {
        let presenter = RecordingPresenter()
        let inbox = Inbox()

        inbox.presentInboxUI(makeCall("first", methodName: "presentInboxUI"), from: { presenter })
        inbox.presentInboxUI(makeRejectingCall("second", methodName: "presentInboxUI"), from: { presenter })
        drainMainQueue()

        XCTAssertEqual(presenter.presentCalls.count, 1, "the second call must never reach present")
        XCTAssertEqual(rejected.map(\.callbackId), ["second"])
        XCTAssertTrue(
            rejected.first?.message.contains("already on screen") ?? false,
            "unexpected rejection reason: \(rejected.first?.message ?? "<none>")"
        )

        // The first call is still in flight — nothing has resolved it yet.
        XCTAssertEqual(resolved, [])

        presenter.completePresentation(at: 0)
        XCTAssertEqual(resolved, ["first"], "the first call must still settle once its own transition finishes")
    }

    func testACallArrivingWhileTheInboxIsAlreadyUpIsRejected() {
        let presenter = RecordingPresenter()
        let inbox = Inbox()

        inbox.presentInboxUI(makeCall("first", methodName: "presentInboxUI"), from: { presenter })
        drainMainQueue()
        presenter.completePresentation(at: 0)
        XCTAssertEqual(resolved, ["first"])

        inbox.presentInboxUI(makeRejectingCall("second", methodName: "presentInboxUI"), from: { presenter })
        drainMainQueue()

        XCTAssertEqual(rejected.map(\.callbackId), ["second"], "an inbox must not be opened over an inbox")
        XCTAssertTrue(
            rejected.first?.message.contains("already on screen") ?? false,
            "unexpected rejection reason: \(rejected.first?.message ?? "<none>")"
        )
        XCTAssertEqual(presenter.presentCalls.count, 1)
    }

    func testAModalThatIsNotTheInboxIsPresentedOverInsteadOfRefused() {
        let presenter = RecordingPresenter()
        let unrelatedModal = RecordingPresenter()
        presenter.present(unrelatedModal, animated: false, completion: nil)
        let inbox = Inbox()

        inbox.presentInboxUI(makeCall("first", methodName: "presentInboxUI"), from: { presenter })
        drainMainQueue()

        XCTAssertEqual(rejected.map(\.callbackId), [], "someone else's modal is not a reason to refuse")
        XCTAssertEqual(presenter.presentCalls.count, 1, "the root still holds only the modal that was there")
        XCTAssertTrue(presenter.presentCalls.first?.controller === unrelatedModal)
        XCTAssertEqual(unrelatedModal.presentCalls.count, 1, "the inbox goes above the modal already up")
        XCTAssertTrue(unrelatedModal.presentCalls.first?.controller is UINavigationController)

        XCTAssertEqual(resolved, [])
        unrelatedModal.completePresentation(at: 0)
        XCTAssertEqual(resolved, ["first"], "the call settles on the transition it actually started")
    }

    func testTheInboxOpensAgainAfterItWasClosed() {
        let presenter = RecordingPresenter()
        let inbox = Inbox()

        inbox.presentInboxUI(makeCall("first", methodName: "presentInboxUI"), from: { presenter })
        drainMainQueue()
        presenter.completePresentation(at: 0)

        inbox.closeInbox()
        presenter.endPresentation()

        inbox.presentInboxUI(makeCall("second", methodName: "presentInboxUI"), from: { presenter })
        drainMainQueue()

        XCTAssertEqual(rejected.map(\.callbackId), [], "a closed inbox must not keep the next call out")
        XCTAssertEqual(presenter.presentCalls.count, 2)
        presenter.completePresentation(at: 1)
        XCTAssertEqual(resolved, ["first", "second"])
    }

    /// The inbox factory is declared without nullability in `PWIInboxUI.h`, so Swift imports its
    /// result as implicitly unwrapped and a nil traps the app instead of rejecting the call. The
    /// only way to see that nil is to make the framework hand one back, which is what the swapped
    /// class-method implementation below does; it is put back before the case ends, and again on
    /// teardown, because every other case in this target needs the real one.
    func testAFactoryThatBuildsNoControllerRejectsInsteadOfTrapping() {
        guard let factory = NSClassFromString("PWIInboxUI"),
              let method = class_getClassMethod(factory, NSSelectorFromString("createInboxControllerWithStyle:")) else {
            return XCTFail("PWIInboxUI.createInboxControllerWithStyle: is not in the test image")
        }
        let original = method_getImplementation(method)
        addTeardownBlock { method_setImplementation(method, original) }
        let buildsNothing: @convention(block) (AnyObject, AnyObject?) -> AnyObject? = { _, _ in nil }
        method_setImplementation(method, imp_implementationWithBlock(buildsNothing))

        let presenter = RecordingPresenter()
        let inbox = Inbox()

        inbox.presentInboxUI(makeRejectingCall("first", methodName: "presentInboxUI"), from: { presenter })
        drainMainQueue()

        XCTAssertEqual(rejected.map(\.callbackId), ["first"])
        XCTAssertEqual(presenter.presentCalls.count, 0, "no controller to present means nothing was presented")

        method_setImplementation(method, original)

        inbox.presentInboxUI(makeCall("second", methodName: "presentInboxUI"), from: { presenter })
        drainMainQueue()

        XCTAssertEqual(presenter.presentCalls.count, 1, "a rejection here must not keep the next call out")
        presenter.completePresentation(at: 0)
        XCTAssertEqual(resolved, ["second"])
    }

    /// UIKit is not under contract to call the completion handler at all, and `RecordingPresenter`
    /// simply never firing it is that case. Without a deadline the call sits unsettled forever and
    /// the in-flight flag keeps every later presentInboxUI out, so the screen can never be opened
    /// again for the rest of the process — the state Android's SCREEN_TIMEOUT_MS exists to avoid.
    func testAPresentationThatNeverCallsBackIsSettledByTheDeadline() {
        let original = Inbox.screenTimeout
        Inbox.screenTimeout = 0.05
        addTeardownBlock { Inbox.screenTimeout = original }

        let presenter = RecordingPresenter()
        let inbox = Inbox()

        inbox.presentInboxUI(makeRejectingCall("first", methodName: "presentInboxUI"), from: { presenter })
        drainMainQueue()
        XCTAssertEqual(presenter.presentCalls.count, 1)
        XCTAssertEqual(rejected.map(\.callbackId), [], "the deadline has not passed yet")

        let deadlinePassed = expectation(description: "the deadline passed")
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) { deadlinePassed.fulfill() }
        wait(for: [deadlinePassed], timeout: 2)

        XCTAssertEqual(rejected.map(\.callbackId), ["first"], "the call must not be left unsettled")
        XCTAssertTrue(
            rejected.first?.message.contains("did not come up") ?? false,
            "unexpected rejection reason: \(rejected.first?.message ?? "<none>")"
        )

        // The call is settled; a completion UIKit gets around to firing late must not settle it again.
        presenter.completePresentation(at: 0)

        inbox.closeInbox()
        presenter.endPresentation()
        inbox.presentInboxUI(makeCall("second", methodName: "presentInboxUI"), from: { presenter })
        drainMainQueue()

        XCTAssertEqual(presenter.presentCalls.count, 2, "a timed-out presentation must not keep the next call out")
        presenter.completePresentation(at: 1)
        XCTAssertEqual(resolved, ["second"])
    }
}
