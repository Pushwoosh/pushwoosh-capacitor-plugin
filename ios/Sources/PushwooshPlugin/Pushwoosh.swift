import Foundation
import UIKit
import PushwooshFramework
import Capacitor
import ObjectiveC.runtime
import UserNotifications

@objc public class Pushwoosh: NSObject, PushNotificationDelegate, UIApplicationDelegate, UNUserNotificationCenterDelegate  {
    var pushManager: PushNotificationManager?
    var pushwoosh: PushwooshFramework.Pushwoosh?
    var pushReceivedPluginCall: CAPPluginCall?
    var pushOpenedPluginCall: CAPPluginCall?
    
    @objc func pwplugin_didRegisterWithDeviceToken(_ application: UIApplication, deviceToken: Data) {
        PushwooshFramework.Pushwoosh.sharedInstance().handlePushRegistration(deviceToken)
    }

    @objc func pwplugin_didFailToRegisterForRemoteNotificationsWithError(_ application: UIApplication, error: Error) {
        PushwooshFramework.Pushwoosh.sharedInstance().handlePushRegistrationFailure(error)
    }
    
    @objc func pwplugin_didReceiveRemoteNotification(_ application: UIApplication, userInfo: [AnyHashable : Any], completionHandler: @escaping (UIBackgroundFetchResult) -> Void) {
        PushwooshFramework.Pushwoosh.sharedInstance().handlePushReceived(userInfo)
        completionHandler(.noData)
    }

    public override init() {
        super.init()
        
        pushManager = PushNotificationManager.push()
        pushwoosh = PushwooshFramework.Pushwoosh.sharedInstance()
        
        PushNotificationManager.push().delegate = self
    }
    
    public func onDeviceReady(appId: String) {
        DispatchQueue.main.async {
            PushwooshSwizzler.swizzleNotificationHandlers()
        }
        PushNotificationManager.init(applicationCode: appId, appName: nil)
    }
    
    public func pushReceivedCallback(_ call: CAPPluginCall) {
        pushReceivedPluginCall = call
        pushReceivedPluginCall?.keepAlive = true
    }
    
    public func pushOpenedCallback(_ call: CAPPluginCall) {
        pushOpenedPluginCall = call
        pushOpenedPluginCall?.keepAlive = true
    }
    
    public func registerDevice(_ call: CAPPluginCall) {
        DispatchQueue.main.async {
            self.pushwoosh?.registerForPushNotifications { token, error in
                if let error = error {
                    call.reject("Failed to register for pushes", error.localizedDescription)
                } else {
                    if let token = token {
                        let result: [String: Any] = ["pushToken": token]
                        call.resolve(result as PluginCallResultData)
                    }
                }
            }
        }
    }
    
    public func unregisterDevice(_ call: CAPPluginCall) {
        DispatchQueue.main.async {
            self.pushwoosh?.unregisterForPushNotifications { error in
                if let error = error {
                    call.reject("FAILED", error.localizedDescription)
                } else {
                    call.resolve()
                }
            }
        }
    }
    
    public func setUserId(_ call: CAPPluginCall) {
        guard let userId = call.getString("userId") else {
            call.reject("Missing userId")
            return
        }
        self.pushwoosh?.setUserId(userId, completion: { error in
            if (error != nil) {
                call.reject("Failed to set user Id", error?.localizedDescription)
            }
            call.resolve()
        })
    }
    
    public func setLanguage(_ call: CAPPluginCall) {
        DispatchQueue.main.async {
            guard let language = call.getString("language") else {
                call.reject("No language code provided")
                return
            }
            self.pushwoosh?.language = language
            call.resolve()
        }
    }
    
    public func postEvent(_ call: CAPPluginCall) {
        guard let event = call.getString("event") else {
            call.reject("failed to post event: No event specified")
            return
        }
        let attributes = call.getObject("attributes")
        PWInAppManager.shared().postEvent(event, withAttributes: attributes) { error in
            if (error != nil) {
                call.reject("Failed to post event", error?.localizedDescription)
            }
            call.resolve()
        }
    }
    
    public func setTags(_ call: CAPPluginCall) {
        guard let tags = call.options else {
            call.reject("failed to set tags: No tags specified")
            return
        }
        self.pushwoosh?.setTags(tags) { error in
            if (error != nil) {
                call.reject("Failed to set tags", error?.localizedDescription)
            }
            call.resolve()
        }
    }
    
    public func getTags(_ call: CAPPluginCall) {
        self.pushwoosh?.getTags({ tags in
            let tagsAsDictionary: [String: Any] = tags as! [String: Any]
            call.resolve(tagsAsDictionary)
        }, onFailure: { error in
            call.reject("Failed to get tags from server", error?.localizedDescription)
        })
    }
    
    public func getPushToken(_ call: CAPPluginCall) {
        let pushToken = self.pushManager?.getPushToken()
        if (pushToken == nil) {
            call.reject("Push token is nil")
        } else {
            call.resolve(["value" : pushToken!])
        }
    }
    
    public func getPushwooshHWID(_ call: CAPPluginCall) {
        let pushwooshHWID = self.pushManager?.getHWID()
        call.resolve(["value" : pushwooshHWID!])
    }
    
    public func getRemoteNotificationStatus(_ call: CAPPluginCall) {
        DispatchQueue.main.async {
            let remoteNotificationStatus = PushNotificationManager.getRemoteNotificationStatus()
            call.resolve(["status" : remoteNotificationStatus])
        }
    }
    
    public func setApplicationIconBadgeNumber(_ call: CAPPluginCall) {
        DispatchQueue.main.async {
            let badgeNumber: Int = call.getInt("badge") ?? 0
            UIApplication.shared.applicationIconBadgeNumber = badgeNumber
            call.resolve()
        }
    }
    
    public func getApplicationIconBadgeNumber(_ call: CAPPluginCall) {
        DispatchQueue.main.async {
            call.resolve(["value" : UIApplication.shared.applicationIconBadgeNumber])
        }
    }
    
    public func addToApplicationIconBadgeNumber(_ call: CAPPluginCall) {
        DispatchQueue.main.async {
            let badgeNumber: Int = call.getInt("badge") ?? 0
            UIApplication.shared.applicationIconBadgeNumber += badgeNumber
            call.resolve()
        }
    }
    
    public func getLaunchNotification(_ call: CAPPluginCall) {
        let launchNotification = pushwoosh?.launchNotification
        call.resolve(["notification": launchNotification])
    }
    
    public func clearLaunchNotification(_ call: CAPPluginCall) {
        //stub
    }
    
    public func showGDPRConsentUI(_ call: CAPPluginCall) {
        PWGDPRManager.shared().showGDPRConsentUI()
        call.resolve()
    }
    
    public func showGDPRDeletionUI(_ call: CAPPluginCall) {
        PWGDPRManager.shared().showGDPRDeletionUI()
        call.resolve()
    }
    
    public func setCommunicationEnabled(_ call: CAPPluginCall) {
        let enabled: Bool = call.getBool("enabled") ?? true
        PWGDPRManager.shared().setCommunicationEnabled(enabled) { error in
            if (error != nil) {
                call.reject("Failed to enabled communications with Pushwoosh", error?.localizedDescription)
            }
            call.resolve()
        }
    }
}

extension Pushwoosh {
    public func onPushReceived(_ pushManager: PushNotificationManager!, withNotification pushNotification: [AnyHashable : Any]!, onStart: Bool) {
        if (pushReceivedPluginCall != nil) {
            let result = ["notification" : pushNotification]
            pushReceivedPluginCall?.resolve(result as PluginCallResultData)
        }    }
    
    public func onPushAccepted(_ pushManager: PushNotificationManager!, withNotification pushNotification: [AnyHashable : Any]!, onStart: Bool) {
        if (pushOpenedPluginCall != nil) {
            let result = ["notification" : pushNotification]
            pushOpenedPluginCall?.resolve(result as PluginCallResultData)
        }
    }
}

final class PushwooshSwizzler {
    
    static func swizzleNotificationHandlers() {
        guard let delegate = UIApplication.shared.delegate else {
            print("Error: No application delegate found")
            return
        }
        
        let clazz: AnyClass = object_getClass(delegate)!
        
        swizzleMethod(in: clazz, originalSelector: #selector(UIApplicationDelegate.application(_:didRegisterForRemoteNotificationsWithDeviceToken:)), swizzledSelector: #selector(Pushwoosh.pwplugin_didRegisterWithDeviceToken(_:deviceToken:)))
        
        swizzleMethod(in: clazz, originalSelector: #selector(UIApplicationDelegate.application(_:didFailToRegisterForRemoteNotificationsWithError:)), swizzledSelector: #selector(Pushwoosh.pwplugin_didFailToRegisterForRemoteNotificationsWithError(_:error:)))
        
        swizzleMethod(in: clazz, originalSelector: #selector(UIApplicationDelegate.application(_:didReceiveRemoteNotification:fetchCompletionHandler:)), swizzledSelector: #selector(Pushwoosh.pwplugin_didReceiveRemoteNotification(_:userInfo:completionHandler:)))
    }

    private static func swizzleMethod(in clazz: AnyClass, originalSelector: Selector, swizzledSelector: Selector) {
        if let originalMethod = class_getInstanceMethod(clazz, originalSelector) {
            print("Method found: \(originalSelector)")
            
            if let swizzledMethod = class_getInstanceMethod(Pushwoosh.self, swizzledSelector) {
                method_exchangeImplementations(originalMethod, swizzledMethod)
                print("Swizzling successful for \(originalSelector)")
            }
        } else {
            print("Method \(originalSelector) not found in delegate")
            addMethodIfNeeded(in: clazz, originalSelector: originalSelector, swizzledSelector: swizzledSelector)
        }
    }
    
    private static func addMethodIfNeeded(in clazz: AnyClass, originalSelector: Selector, swizzledSelector: Selector) {
        if let swizzledMethod = class_getInstanceMethod(Pushwoosh.self, swizzledSelector) {
            let didAddMethod = class_addMethod(clazz, originalSelector, method_getImplementation(swizzledMethod), method_getTypeEncoding(swizzledMethod))
            
            if didAddMethod {
                print("Method \(originalSelector) added successfully")
            } else {
                print("Failed to add method \(originalSelector)")
            }
        }
    }
}
