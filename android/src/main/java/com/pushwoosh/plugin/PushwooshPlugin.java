package com.pushwoosh.plugin;

import android.util.Log;

import com.getcapacitor.Bridge;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.pushwoosh.internal.utils.PWLog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

@CapacitorPlugin(name = "Pushwoosh")
public class PushwooshPlugin extends Plugin {

    private Pushwoosh implementation = new Pushwoosh();
    private static String pushReceivedCallbackId = "";
    private static String pushOpenedCallbackId = "";

    @PluginMethod(returnType = PluginMethod.RETURN_CALLBACK)
    public void pushReceivedCallback(PluginCall call) {
        pushReceivedCallbackId = call.getCallbackId();
        call.setKeepAlive(true);
        implementation.pushReceivedCallback(call);
    }

    @PluginMethod(returnType = PluginMethod.RETURN_CALLBACK)
    public void pushOpenedCallback(PluginCall call) {
        pushOpenedCallbackId = call.getCallbackId();
        call.setKeepAlive(true);
        implementation.pushOpenedCallback(call);
    }

    @PluginMethod
    public void onDeviceReady(PluginCall call) {
        HashMap<String, String> config = convertToHashMapString(call.getData());
        implementation.onDeviceReady(config);
        call.resolve();
    }

    @PluginMethod
    public void registerDevice(PluginCall call) {
        implementation.registerDevice(call);
    }

    @PluginMethod
    public void unregisterDevice(PluginCall call) {
        implementation.unregisterDevice(call);
    }

    @PluginMethod
    public void setTags(PluginCall call) {
        JSObject tags = call.getData();
        JSONObject jsonTags = new JSONObject();

        try {
            for (Iterator<String> it = tags.keys(); it.hasNext(); ) {
                String key = it.next();
                Object value = tags.get(key);

                // Handle different types of values (string, number, array)
                if (value instanceof String) {
                    jsonTags.put(key, (String) value);
                } else if (value instanceof Number) {
                    jsonTags.put(key, value);
                } else if (value instanceof JSONArray) {
                    jsonTags.put(key, (JSONArray) value);
                }
            }

            implementation.setTags(jsonTags, call);
        } catch (Exception e) {
            // If something fails, reject the call with an error message
            call.reject("Failed to set tags", e.getMessage());
        }
    }

    @PluginMethod
    public void getTags(PluginCall call) {
        implementation.getTags(call);
    }

    @PluginMethod
    public void setUserId(PluginCall call) {
        implementation.setUserId(call);
    }

    @PluginMethod
    public void setLanguage(PluginCall call) {
        implementation.setLanguage(call);
    }

    @PluginMethod
    public void setMultiNotificationMode(PluginCall call) {
        implementation.setMultiNotificationMode(call);
    }

    @PluginMethod
    public void setSingleNotificationMode(PluginCall call) {
        implementation.setSingleNotificationMode(call);
    }
    @PluginMethod
    public void postEvent(PluginCall call) {
        JSObject attributes = call.getObject("attributes");
        JSONObject jsonAttributes = new JSONObject();

        try {
            for (Iterator<String> it = attributes.keys(); it.hasNext(); ) {
                String key = it.next();
                Object value = attributes.get(key);

                // Handle different types of values (string, number, array)
                if (value instanceof String) {
                    jsonAttributes.put(key, (String) value);
                } else if (value instanceof Number) {
                    jsonAttributes.put(key, value);
                } else if (value instanceof JSONArray) {
                    jsonAttributes.put(key, (JSONArray) value);
                }
            }

            implementation.postEvent(jsonAttributes, call);
        } catch (Exception e) {
            // If something fails, reject the call with an error message
            call.reject("Failed to set tags", e.getMessage());
        }
    }

    @PluginMethod
    public void getPushToken(PluginCall call) {
        implementation.getPushToken(call);
    }

    @PluginMethod
    public void getPushwooshHWID(PluginCall call) {
        implementation.getPushwooshHWID(call);
    }

    @PluginMethod
    public void getRemoteNotificationStatus(PluginCall call) {
        implementation.getRemoteNotificationStatus(call);
    }

    @PluginMethod
    public void setApplicationIconBadgeNumber(PluginCall call) {
        int badgeNumber = call.getInt("badge");
        implementation.setApplicationIconBadgeNumber(badgeNumber);
        call.resolve();
    }

    @PluginMethod
    public void getApplicationIconBadgeNumber(PluginCall call) {
        implementation.getApplicationIconBadgeNumber(call);
    }

    @PluginMethod
    public void addToApplicationIconBadgeNumber(PluginCall call) {
        int badgeNumber = call.getInt("badge");
        implementation.addToApplicationIconBadgeNumber(badgeNumber);
        call.resolve();
    }

    @PluginMethod
    public void getLaunchNotification(PluginCall call) {
        implementation.getLaunchNotification(call);
    }

    @PluginMethod
    public void clearLaunchNotification(PluginCall call) {
        implementation.clearLaunchNotification(call);
    }

    @PluginMethod
    public void showGDPRConsentUI(PluginCall call) {
        implementation.showGDPRConsentUI(call);
    }

    @PluginMethod
    public void showGDPRDeletionUI(PluginCall call) {
        implementation.showGDPRDeletionUI(call);
    }

    @PluginMethod
    public void setCommunicationEnabled(PluginCall call) {
        boolean enabled = call.getBoolean("enabled");
        implementation.setCommunicationEnabled(enabled, call);
    }

    @PluginMethod
    public void removeAllDeviceData(PluginCall call) {
        implementation.removeAllDeviceData(call);
    }

    private HashMap<String, String> convertToHashMapString(JSObject jsObject) {
        HashMap<String, String> map = new HashMap<>();
        Iterator<String> keys = jsObject.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            try {
                Object value = jsObject.get(key); // Handle JSONException
                if (value instanceof String) {
                    map.put(key, (String) value);
                }
            } catch (JSONException e) {
                e.printStackTrace();
                // Log the error or handle it as per your requirements
            }
        }
        return map;
    }
}
