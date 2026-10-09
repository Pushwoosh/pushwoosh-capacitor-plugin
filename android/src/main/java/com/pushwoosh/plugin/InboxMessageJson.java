package com.pushwoosh.plugin;

import com.getcapacitor.JSObject;
import com.pushwoosh.inbox.data.InboxMessage;
import com.pushwoosh.inbox.data.InboxMessageType;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Maps one SDK inbox message onto the JSON contract the TypeScript `InboxNotification` declares.
 *
 * iOS produces the same field set with the same types from a different native model, so a change
 * here needs the matching change in InboxMessageDictionary.swift. A field the native message does
 * not carry is left out rather than sent as an empty string or null, because the declared type
 * marks everything but `code` optional.
 */
final class InboxMessageJson {

    private InboxMessageJson() {
    }

    static JSObject toJson(InboxMessage message) {
        JSObject json = new JSObject();
        json.put("code", message.getCode());
        putIfPresent(json, "title", message.getTitle());
        putIfPresent(json, "message", message.getMessage());
        putIfPresent(json, "imageUrl", message.getImageUrl());
        putIfPresent(json, "bannerUrl", message.getBannerUrl());
        putIfPresent(json, "sendDate", message.getISO8601SendDate());
        json.put("isRead", message.isRead());
        json.put("isActionPerformed", message.isActionPerformed());

        InboxMessageType type = message.getType();
        if (type != null) {
            json.put("type", type.getCode());
        }

        JSONObject actionParams = parseObject(message.getActionParams());
        if (actionParams != null) {
            json.put("actionParams", actionParams);
            JSONObject customData = customDataOf(actionParams);
            if (customData != null) {
                json.put("customData", customData);
            }
        }
        return json;
    }

    private static void putIfPresent(JSObject json, String key, String value) {
        if (value != null && !value.isEmpty()) {
            json.put(key, value);
        }
    }

    private static JSONObject customDataOf(JSONObject actionParams) {
        Object raw = actionParams.opt("u");
        if (raw instanceof JSONObject) {
            return (JSONObject) raw;
        }
        if (raw instanceof String) {
            return parseObject((String) raw);
        }
        return null;
    }

    private static JSONObject parseObject(String raw) {
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        try {
            return new JSONObject(raw);
        } catch (JSONException e) {
            return null;
        }
    }
}
