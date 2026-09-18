import UserNotifications
import ObjectiveC.runtime

/// `Pushwoosh.init()` reaches the native SDK, which registers a `UNUserNotificationCenter`
/// delegate. In a test bundle there is no app bundle, and `+currentNotificationCenter` itself
/// raises `NSInternalInconsistencyException: bundleProxyForCurrentProcess is nil` — measured on
/// every SDK entry point. Reading is what gets stubbed, exactly once per process; no global app
/// state is replaced. It is never restored, so every test in this target sees a nil notification
/// center — a case that needs the real one has to run in a host application instead.
let notificationCenterStubbed: Bool = {
    guard let method = class_getClassMethod(UNUserNotificationCenter.self, NSSelectorFromString("currentNotificationCenter")) else {
        return false
    }
    let stub: @convention(block) (AnyObject) -> AnyObject? = { _ in nil }
    method_setImplementation(method, imp_implementationWithBlock(stub))
    return true
}()
