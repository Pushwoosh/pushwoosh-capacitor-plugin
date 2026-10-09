import Foundation
import PushwooshFramework

/// Maps one SDK inbox message onto the JSON contract the TypeScript `InboxNotification` declares.
///
/// Android produces the same field set with the same types from a different native model, so a
/// change here needs the matching change in InboxMessageJson.java. A field the native message does
/// not carry is left out rather than sent as an empty string, because the declared type marks
/// everything but `code` optional.
///
/// `code` is the one field the contract guarantees and the one every mutator takes, and the native
/// protocol carries no nullability annotations, so a message without one is reported as no message
/// at all rather than shipped with an identifier no mutator would accept.
enum InboxMessageDictionary {

    private static let sendDateFormatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.dateFormat = "yyyy-MM-dd'T'HH:mm:ssZ"
        return formatter
    }()

    static func make(from message: PWInboxMessageProtocol) -> [String: Any]? {
        guard let code = message.code, !code.isEmpty else {
            NSLog("Inbox: skipping a message the SDK reported without a code")
            return nil
        }
        var json: [String: Any] = [
            "code": code,
            "type": message.type.rawValue,
            "isRead": message.isRead,
            "isActionPerformed": message.isActionPerformed
        ]
        put(&json, "title", message.title)
        put(&json, "message", message.message)
        put(&json, "imageUrl", message.imageUrl)
        if let sendDate = message.sendDate {
            json["sendDate"] = sendDateFormatter.string(from: sendDate)
        }

        let actionParams = (message.actionParams as? [String: Any]) ?? [:]
        if !actionParams.isEmpty {
            json["actionParams"] = actionParams
        }
        // Android reads bannerUrl from the same `b` parameter; attachmentUrl is a different key.
        put(&json, "bannerUrl", actionParams["b"] as? String)
        if let customData = object(from: actionParams["u"]) {
            json["customData"] = customData
        }
        return json
    }

    private static func put(_ json: inout [String: Any], _ key: String, _ value: String?) {
        guard let value = value, !value.isEmpty else {
            return
        }
        json[key] = value
    }

    private static func object(from raw: Any?) -> [String: Any]? {
        if let dictionary = raw as? [String: Any] {
            return dictionary
        }
        guard let text = raw as? String, let data = text.data(using: .utf8) else {
            return nil
        }
        return (try? JSONSerialization.jsonObject(with: data)) as? [String: Any]
    }
}
