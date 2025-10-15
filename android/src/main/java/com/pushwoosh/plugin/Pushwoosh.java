package com.pushwoosh.plugin;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.getcapacitor.JSObject;
import com.getcapacitor.PluginCall;
import com.pushwoosh.badge.PushwooshBadge;
import com.pushwoosh.exception.GetTagsException;
import com.pushwoosh.function.Callback;
import com.pushwoosh.function.Result;
import com.pushwoosh.inapp.InAppManager;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.notification.PushMessage;
import com.pushwoosh.notification.PushwooshNotificationSettings;
import com.pushwoosh.tags.Tags;
import com.pushwoosh.tags.TagsBundle;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.json.JSONException;
import org.json.JSONObject;

public class Pushwoosh {
    public static final String TAG = "CapacitorPlugin";
    private static final Object sStartPushLock = new Object();

    private static String sStartPushData;
    private static String sReceivedPushData;

    private static PluginCall pushReceivedPluginCall;
    private static PluginCall pushOpenedPluginCall;

    private static AtomicBoolean sAppReady = new AtomicBoolean();
    private static Pushwoosh sInstance;

    private final Handler handler = new Handler(Looper.getMainLooper());

    public Pushwoosh() {
        sInstance = this;
        sAppReady.set(false);
    }

    static void openPush(String pushData) {
        try {
            synchronized (sStartPushLock) {
                sStartPushData = pushData;
                if (sAppReady.get() && sInstance != null) {
                    sInstance.doOnPushOpened(pushData);
                }
            }
        } catch (Exception e) {
            // React Native is highly unstable
            PWLog.exception(e);
        }
    }

    private void doOnPushOpened(String pushData) {
        if (pushOpenedPluginCall != null) {
            JSObject result = new JSObject();
            result.put("notification", pushData);
            pushOpenedPluginCall.resolve(result);
        }
    }

    static void messageReceived(String pushData) {
        try {
            synchronized (sStartPushLock) {
                sReceivedPushData = pushData;
                if (sAppReady.get() && sInstance != null) {
                    sInstance.doOnPushReceived(pushData);
                }
            }
        } catch (Exception e) {
            // React Native is highly unstable
            PWLog.exception(e);
        }
    }

    private void doOnPushReceived(String pushData) {
        if (pushReceivedPluginCall != null) {
            JSObject result = new JSObject();
            result.put("notification", pushData);
            pushReceivedPluginCall.resolve(result);
        }
    }

    private JSONObject getPushFromIntent(Intent intent) {
        if (null == intent) {
            return null;
        }

        if (intent.hasExtra(com.pushwoosh.Pushwoosh.PUSH_RECEIVE_EVENT)) {
            String pushString = intent.getExtras().getString(com.pushwoosh.Pushwoosh.PUSH_RECEIVE_EVENT);
            JSONObject pushObject = null;
            try {
                pushObject = new JSONObject(pushString);
            } catch (JSONException e) {
                PWLog.error(TAG, "Failed to parse push notification", e);
            }

            return pushObject;
        }

        return null;
    }

    public void onDeviceReady(Map<String, String> config) {
        String appId = config.get("appid");
        String projectId = config.get("projectid");

        if (appId != null && projectId != null) {
            com.pushwoosh.Pushwoosh.getInstance().setAppId(appId);
            com.pushwoosh.Pushwoosh.getInstance().setSenderId(projectId);
            Log.i(TAG, "Pushwoosh is initialized with appId: " + appId + " and projectId: " + projectId);
        } else {
            Log.e(TAG, "Missing appId or projectId in onDeviceReady");
        }

        synchronized (sStartPushLock) {
        if (sReceivedPushData != null) {
            sInstance.doOnPushReceived(sReceivedPushData);
            sReceivedPushData = null;
        }

        if (sStartPushData != null) {
            sInstance.doOnPushOpened(sStartPushData);
            sStartPushData = null;
        }
    }

        sAppReady.set(true);
    }

    public void registerDevice(PluginCall call) {
        com.pushwoosh.Pushwoosh.getInstance().registerForPushNotifications(result -> {
            if (result.isSuccess()) {
                JSObject response = new JSObject();
                response.put("pushToken", result.getData().getToken());
                call.resolve(response);
            } else {
                call.reject("Failed to register device", result.getException().getMessage());
            }
        });
    }

    public void unregisterDevice(PluginCall call) {
        com.pushwoosh.Pushwoosh.getInstance().unregisterForPushNotifications(result -> {
            if (result.isSuccess()) {
                call.resolve();
            } else {
                call.reject("Failed to unregister device", result.getException().getMessage());
            }
        });
    }

    public void setTags(JSONObject tags, PluginCall call) {
        com.pushwoosh.Pushwoosh.getInstance().sendTags(Tags.fromJson(tags), result -> {
            if (result.isSuccess()) {
                call.resolve();
            } else {
                call.reject("Failed to set tags: " + result.getException().getMessage());
            }
        });
    }

    public void getTags(PluginCall call) {
        com.pushwoosh.Pushwoosh.getInstance().getTags(new Callback<TagsBundle, GetTagsException>() {
            @Override
            public void process(@NonNull final Result<TagsBundle, GetTagsException> result) {
                if(result.isSuccess()) {
                    try {
                        JSObject response = JSObject.fromJSONObject(result.getData().toJson());
                        call.resolve(response);
                    } catch (Exception e) {

                    }
                } else {
                    call.reject("Failed to get Tags", result.getException().getMessage());
                }
            }
        });
    }

    public void setUserId(String userId, PluginCall call) {
        com.pushwoosh.Pushwoosh.getInstance().setUserId(userId);
        call.resolve();
    }

    public void setLanguage(String language, PluginCall call) {
        com.pushwoosh.Pushwoosh.getInstance().setLanguage(language);
        call.resolve();
    }

    public void setMultiNotificationMode(PluginCall call) {
        PushwooshNotificationSettings.setMultiNotificationMode(true);
        call.resolve();
    }

    public void setSingleNotificationMode(PluginCall call) {
        PushwooshNotificationSettings.setMultiNotificationMode(false);
        call.resolve();
    }

    public void postEvent(String event, JSONObject attributes, PluginCall call) {
        TagsBundle attributesBundle = Tags.fromJson(attributes);
        InAppManager.getInstance().postEvent(event, attributesBundle);
        call.resolve();
    }

    public void getPushToken(PluginCall call) {
        String token = com.pushwoosh.Pushwoosh.getInstance().getPushToken(); // Assuming this is the correct way to get the push token
        JSObject response = new JSObject();
        response.put("value", token); // Add token or null to the response

        // If token is null, it will return null in the response
        call.resolve(response);
    }

    public void getPushwooshHWID(PluginCall call) {
        String hwid = com.pushwoosh.Pushwoosh.getInstance().getHwid(); // Assuming this is how to get HWID
        JSObject response = new JSObject();
        response.put("value", hwid); // Add HWID or null to the response

        // If hwid is null, it will return null in the response
        call.resolve(response);
    }

    public void getRemoteNotificationStatus(PluginCall call) {
        try {
            String enabled = PushwooshNotificationSettings.areNotificationsEnabled() ? "1" : "0";
            JSObject result = new JSObject();
            result.put("status", enabled);
            call.resolve(result);
        } catch (Exception e) {
            call.reject("Failed to get remote notification status", e.getMessage());
        }
    }

    public void setApplicationIconBadgeNumber(int badge) {
        PushwooshBadge.setBadgeNumber(badge);
    }

    public void getApplicationIconBadgeNumber(PluginCall call) {
        int badge = PushwooshBadge.getBadgeNumber();
        JSObject result = new JSObject();
        result.put("value", badge);
        call.resolve(result);
    }

    public void addToApplicationIconBadgeNumber(int badge) {
        PushwooshBadge.addBadgeNumber(badge);
    }

    public void getLaunchNotification(PluginCall call) {
        PushMessage launchNotification = com.pushwoosh.Pushwoosh.getInstance().getLaunchNotification();
        if (launchNotification == null) {
            call.resolve(null);
        } else {
            JSObject result = new JSObject();
            result.put("value", launchNotification.toJson().toString());
            call.resolve(result);
        }
    }

    public void clearLaunchNotification(PluginCall call) {
        com.pushwoosh.Pushwoosh.getInstance().clearLaunchNotification();
        call.resolve();
    }

    public void setCommunicationEnabled(boolean enabled, PluginCall call) {
        if (enabled) {
            com.pushwoosh.Pushwoosh.getInstance().startServerCommunication();
        } else {
            com.pushwoosh.Pushwoosh.getInstance().stopServerCommunication();
        }
        call.resolve();
    }

    public void pushReceivedCallback(PluginCall call) {
        pushReceivedPluginCall = call;
        pushReceivedPluginCall.setKeepAlive(true);
    }

    public void pushOpenedCallback(PluginCall call) {
        pushOpenedPluginCall = call;
        pushOpenedPluginCall.setKeepAlive(true);
    }
}
