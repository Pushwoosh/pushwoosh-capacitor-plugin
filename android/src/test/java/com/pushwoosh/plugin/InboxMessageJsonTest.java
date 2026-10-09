package com.pushwoosh.plugin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.annotation.NonNull;

import com.getcapacitor.JSObject;
import com.pushwoosh.inbox.data.InboxMessage;
import com.pushwoosh.inbox.data.InboxMessageType;

import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * The JSON a loaded message turns into is what every host reads. Three things have to hold:
 * the declared fields carry the declared types, a field the native message does not have is
 * absent rather than empty, and a message whose action parameters are missing or malformed
 * still comes through — an inbox is not allowed to disappear because one row has a broken `u`.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34, manifest = Config.NONE)
public class InboxMessageJsonTest {

    private static class FakeMessage implements InboxMessage {
        String code = "code-1";
        String title = "Title";
        String message = "Body";
        String imageUrl = "https://example.com/icon.png";
        String bannerUrl = "https://example.com/banner.png";
        String iso8601 = "2026-10-07T14:32:05+0300";
        InboxMessageType type = InboxMessageType.URL;
        String actionParams = "{\"l\":\"https://example.com\",\"u\":\"{\\\"orderId\\\":42}\"}";
        boolean read = false;
        boolean actionPerformed = false;

        @NonNull @Override public String getCode() { return code; }
        @Override public String getTitle() { return title; }
        @Override public String getImageUrl() { return imageUrl; }
        @NonNull @Override public String getMessage() { return message; }
        @NonNull @Override public java.util.Date getSendDate() { return new java.util.Date(0); }
        @NonNull @Override public String getISO8601SendDate() { return iso8601; }
        @NonNull @Override public InboxMessageType getType() { return type; }
        @Override public String getBannerUrl() { return bannerUrl; }
        @Override public String getActionParams() { return actionParams; }
        @Override public boolean isRead() { return read; }
        @Override public boolean isActionPerformed() { return actionPerformed; }
        @Override public int compareTo(InboxMessage other) { return 0; }
    }

    @Test
    public void fullMessageCarriesEveryDeclaredField() throws JSONException {
        FakeMessage message = new FakeMessage();
        message.read = true;

        JSObject json = InboxMessageJson.toJson(message);

        assertEquals("code-1", json.getString("code"));
        assertEquals("Title", json.getString("title"));
        assertEquals("Body", json.getString("message"));
        assertEquals("https://example.com/icon.png", json.getString("imageUrl"));
        assertEquals("https://example.com/banner.png", json.getString("bannerUrl"));
        assertEquals("2026-10-07T14:32:05+0300", json.getString("sendDate"));
        assertEquals(2, json.getInt("type"));
        assertTrue(json.getBoolean("isRead"));
        assertFalse(json.getBoolean("isActionPerformed"));
        assertEquals("https://example.com", json.getJSONObject("actionParams").getString("l"));
        assertEquals(42, json.getJSONObject("customData").getInt("orderId"));
    }

    @Test
    public void missingFieldsAreOmittedRatherThanEmptied() {
        FakeMessage message = new FakeMessage();
        message.title = null;
        message.imageUrl = null;
        message.bannerUrl = null;

        JSObject json = InboxMessageJson.toJson(message);

        assertFalse("an absent title must not become an empty string", json.has("title"));
        assertFalse(json.has("imageUrl"));
        assertFalse(json.has("bannerUrl"));
        assertEquals("code-1", json.optString("code"));
    }

    @Test
    public void blankActionParamsStillProduceAMessage() {
        FakeMessage message = new FakeMessage();
        message.actionParams = "";

        JSObject json = InboxMessageJson.toJson(message);

        assertEquals("code-1", json.optString("code"));
        assertFalse(json.has("actionParams"));
        assertFalse(json.has("customData"));
    }

    @Test
    public void malformedActionParamsStillProduceAMessage() {
        FakeMessage message = new FakeMessage();
        message.actionParams = "{not json";

        JSObject json = InboxMessageJson.toJson(message);

        assertEquals("code-1", json.optString("code"));
        assertFalse(json.has("actionParams"));
        assertFalse(json.has("customData"));
    }

    @Test
    public void customDataSurvivesBothTransportsAndIsSkippedWhenItIsNeither() throws JSONException {
        FakeMessage asObject = new FakeMessage();
        asObject.actionParams = "{\"u\":{\"orderId\":42}}";
        assertEquals(42, InboxMessageJson.toJson(asObject).getJSONObject("customData").getInt("orderId"));

        FakeMessage asScalar = new FakeMessage();
        asScalar.actionParams = "{\"u\":\"plain text\"}";
        assertFalse(InboxMessageJson.toJson(asScalar).has("customData"));

        FakeMessage absent = new FakeMessage();
        absent.actionParams = "{\"l\":\"https://example.com\"}";
        assertFalse(InboxMessageJson.toJson(absent).has("customData"));
    }
}
