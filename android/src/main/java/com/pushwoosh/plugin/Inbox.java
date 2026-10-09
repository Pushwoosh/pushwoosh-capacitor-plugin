package com.pushwoosh.plugin;

import android.app.Activity;

import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.PluginCall;
import com.pushwoosh.function.Result;
import com.pushwoosh.inbox.PushwooshInbox;
import com.pushwoosh.inbox.data.InboxMessage;
import com.pushwoosh.inbox.exception.InboxMessagesException;
import com.pushwoosh.internal.utils.PWLog;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Message Inbox half of the plugin.
 *
 * Every entry point settles its PluginCall exactly once: a native failure rejects with the reason
 * in the message, and a call whose arguments are wrong is rejected here, before the SDK is asked
 * anything, with a message naming the parameter at fault.
 */
public class Inbox {

    static final String TAG = Pushwoosh.TAG;

    private final InboxPresenter presenter = new InboxPresenter();

    public void presentInboxUI(Activity activity, JSONObject style, PluginCall call) {
        presenter.present(activity, style, call);
    }

    public void loadMessages(PluginCall call) {
        PushwooshInbox.loadMessages(result -> {
            if (!result.isSuccess() || result.getData() == null) {
                call.reject("Failed to load inbox messages: " + describe(result));
                return;
            }
            JSONArray messages = new JSONArray();
            for (InboxMessage message : result.getData()) {
                messages.put(InboxMessageJson.toJson(message));
            }
            PWLog.info(TAG, "Inbox messages loaded: " + messages.length());
            JSObject response = new JSObject();
            response.put("messages", messages);
            call.resolve(response);
        });
    }

    public void messagesCount(PluginCall call) {
        PushwooshInbox.messagesCount(result -> resolveCount(call, result, "Failed to get inbox messages count"));
    }

    public void unreadMessagesCount(PluginCall call) {
        PushwooshInbox.unreadMessagesCount(result -> resolveCount(call, result, "Failed to get unread inbox messages count"));
    }

    public void messagesWithNoActionPerformedCount(PluginCall call) {
        PushwooshInbox.messagesWithNoActionPerformedCount(
                result -> resolveCount(call, result, "Failed to get inbox messages with no action performed count"));
    }

    public void readMessage(PluginCall call) {
        String code = requiredCode(call);
        if (code == null) {
            return;
        }
        PushwooshInbox.readMessage(code);
        call.resolve();
    }

    public void readMessages(PluginCall call) {
        List<String> codes = requiredCodes(call);
        if (codes == null) {
            return;
        }
        PushwooshInbox.readMessages(codes);
        call.resolve();
    }

    public void deleteMessage(PluginCall call) {
        String code = requiredCode(call);
        if (code == null) {
            return;
        }
        PushwooshInbox.deleteMessage(code);
        call.resolve();
    }

    public void deleteMessages(PluginCall call) {
        List<String> codes = requiredCodes(call);
        if (codes == null) {
            return;
        }
        PushwooshInbox.deleteMessages(codes);
        call.resolve();
    }

    public void performAction(PluginCall call) {
        String code = requiredCode(call);
        if (code == null) {
            return;
        }
        PushwooshInbox.performAction(code);
        call.resolve();
    }

    static String requiredCode(PluginCall call) {
        String code = call.getString("code");
        if (code == null || code.isEmpty()) {
            call.reject("Missing required parameter: code");
            return null;
        }
        return code;
    }

    static List<String> requiredCodes(PluginCall call) {
        JSArray raw = call.getArray("codes");
        if (raw == null || raw.length() == 0) {
            call.reject("Missing required parameter: codes");
            return null;
        }
        List<String> codes = new ArrayList<>(raw.length());
        for (int index = 0; index < raw.length(); index++) {
            Object entry = raw.opt(index);
            if (!(entry instanceof String) || ((String) entry).isEmpty()) {
                call.reject("Invalid parameter: codes");
                return null;
            }
            codes.add((String) entry);
        }
        return codes;
    }

    private static void resolveCount(PluginCall call, Result<Integer, InboxMessagesException> result, String failure) {
        if (!result.isSuccess() || result.getData() == null) {
            call.reject(failure + ": " + describe(result));
            return;
        }
        JSObject response = new JSObject();
        response.put("value", result.getData().intValue());
        call.resolve(response);
    }

    private static String describe(Result<?, ? extends Exception> result) {
        Exception exception = result.getException();
        if (exception == null || exception.getMessage() == null) {
            return "no reason reported by the SDK";
        }
        return exception.getMessage();
    }
}
