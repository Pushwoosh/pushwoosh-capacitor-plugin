import Foundation
import Capacitor
import PushwooshFramework

/**
 * Please read the Capacitor iOS Plugin Development Guide
 * here: https://capacitorjs.com/docs/plugins/ios
 */
@objc(PushwooshPlugin)
public class PushwooshPlugin: CAPPlugin, CAPBridgedPlugin, UIApplicationDelegate, PWMessagingDelegate {
    public let identifier = "PushwooshPlugin"
    public let jsName = "Pushwoosh"
    
    var callbackIds: NSMutableDictionary?
    var pushManager: PushNotificationManager?
    var pushwoosh: PushwooshFramework.Pushwoosh?
    var startPushData: [String: Any]?
    var startPushCleared: Bool = false
    var deviceReady: Bool = false
    
    private var notificationDelegateHandler = Pushwoosh()

    public let pluginMethods: [CAPPluginMethod] = [
            CAPPluginMethod(name: "pushReceivedCallback", returnType: CAPPluginReturnCallback),
            CAPPluginMethod(name: "pushOpenedCallback", returnType: CAPPluginReturnCallback),
            CAPPluginMethod(name: "onDeviceReady", returnType: CAPPluginReturnPromise),
            CAPPluginMethod(name: "registerDevice", returnType: CAPPluginReturnPromise),
            CAPPluginMethod(name: "unregisterDevice", returnType: CAPPluginReturnPromise),
            CAPPluginMethod(name: "setTags", returnType: CAPPluginReturnPromise),
            CAPPluginMethod(name: "getTags", returnType: CAPPluginReturnPromise),
            CAPPluginMethod(name: "setUserId", returnType: CAPPluginReturnPromise),
            CAPPluginMethod(name: "setLanguage", returnType: CAPPluginReturnPromise),
            CAPPluginMethod(name: "setMultiNotificationMode", returnType: CAPPluginReturnPromise),
            CAPPluginMethod(name: "setSingleNotificationMode", returnType: CAPPluginReturnPromise),
            CAPPluginMethod(name: "postEvent", returnType: CAPPluginReturnPromise),
            CAPPluginMethod(name: "getPushToken", returnType: CAPPluginReturnPromise),
            CAPPluginMethod(name: "getPushwooshHWID", returnType: CAPPluginReturnPromise),
            CAPPluginMethod(name: "getRemoteNotificationStatus", returnType: CAPPluginReturnPromise),
            CAPPluginMethod(name: "setApplicationIconBadgeNumber", returnType: CAPPluginReturnPromise),
            CAPPluginMethod(name: "getApplicationIconBadgeNumber", returnType: CAPPluginReturnPromise),
            CAPPluginMethod(name: "addToApplicationIconBadgeNumber", returnType: CAPPluginReturnPromise),
            CAPPluginMethod(name: "getLaunchNotification", returnType: CAPPluginReturnPromise),
            CAPPluginMethod(name: "clearLaunchNotification", returnType: CAPPluginReturnPromise),
            CAPPluginMethod(name: "setCommunicationEnabled", returnType: CAPPluginReturnPromise),
        ]
    
    private let implementation = Pushwoosh()
    
    @objc public override func load() {
        super.load()
    }
    
    @objc public func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey : Any]? = nil) -> Bool {
        PushwooshFramework.Pushwoosh.sharedInstance().delegate = self;
        return true
    }
    
    @objc public func application(_ application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        PushwooshFramework.Pushwoosh.sharedInstance().handlePushRegistration(deviceToken)
    }
    
    @MainActor
    @objc func onDeviceReady(_ call: CAPPluginCall) {
        guard let appId = call.getString("appid") else {
            call.reject("Missing appId")
            return
        }
        implementation.onDeviceReady(appId: appId)
        call.resolve()
    }
    
    @MainActor
    @objc func registerDevice(_ call: CAPPluginCall) {
        implementation.registerDevice(call)
    }
    
    @MainActor
    @objc func unregisterDevice(_ call: CAPPluginCall) {
        implementation.unregisterDevice(call)
    }
    
    @objc func pushReceivedCallback(_ call: CAPPluginCall) {
        let  pushReceivedCallbackId = call.callbackId
        call.keepAlive = true
        implementation.pushReceivedCallback(call)
    }
    
    @objc func pushOpenedCallback(_ call: CAPPluginCall) {
        let  pushOpenedCallbackId = call.callbackId
        call.keepAlive = true
        implementation.pushOpenedCallback(call)
    }
    
    @objc func setUserId(_ call: CAPPluginCall) {
        implementation.setUserId(call)
    }
    
    @objc func setLanguage(_ call: CAPPluginCall) {
        implementation.setLanguage(call)
    }
    
    @objc func setMultiNotificationMode(_ call: CAPPluginCall) {
        //stub, only available for ANdroid
        call.resolve()
    }
    
    @objc func postEvent(_ call: CAPPluginCall) {
        implementation.postEvent(call)
    }
    
    @objc func setTags(_ call: CAPPluginCall) {
        implementation.setTags(call)
    }
    
    @objc func getTags(_ call: CAPPluginCall) {
        implementation.getTags(call)
    }
    
    @objc func getPushToken(_ call: CAPPluginCall) {
        implementation.getPushToken(call)
    }
    
    @objc func getPushwooshHWID(_ call: CAPPluginCall) {
        implementation.getPushwooshHWID(call)
    }
    
    @objc func getRemoteNotificationStatus(_ call: CAPPluginCall) {
        implementation.getRemoteNotificationStatus(call)
    }
    
    @objc func setApplicationIconBadgeNumber(_ call: CAPPluginCall) {
        implementation.setApplicationIconBadgeNumber(call)
    }
    
    @objc func getApplicationIconBadgeNumber(_ call: CAPPluginCall) {
        implementation.getApplicationIconBadgeNumber(call)
    }
    
    @objc func addToApplicationIconBadgeNumber(_ call: CAPPluginCall) {
        implementation.addToApplicationIconBadgeNumber(call)
    }
    
    @objc func getLaunchNotification(_ call: CAPPluginCall) {
        implementation.getLaunchNotification(call)
    }
    
    @objc func clearLaunchNotification(_ call: CAPPluginCall) {
        implementation.clearLaunchNotification(call)
    }
    
    @objc func setCommunicationEnabled(_ call: CAPPluginCall) {
        implementation.setCommunicationEnabled(call)
    }
}
