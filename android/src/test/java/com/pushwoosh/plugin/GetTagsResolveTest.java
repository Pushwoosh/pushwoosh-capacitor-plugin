package com.pushwoosh.plugin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.getcapacitor.JSObject;
import com.getcapacitor.PluginCall;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Collections;
import java.util.Iterator;

/**
 * getTags must settle its PluginCall either way. A successful read resolves the tags as a flat
 * object — every tag at the top level, no `tags` envelope — which is what the TypeScript type,
 * the README and the other Pushwoosh plugins promise. A JSON conversion that throws must reject
 * the call: the previous empty catch block left the JS promise pending forever.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34, manifest = Config.NONE)
public class GetTagsResolveTest {

    private static class RecordingCall extends PluginCall {

        private JSObject resolvedData;
        private boolean resolved;
        private String rejectMessage;

        RecordingCall() {
            super(null, "Pushwoosh", "cb-1", "getTags", new JSObject());
        }

        @Override
        public void resolve(JSObject data) {
            resolved = true;
            resolvedData = data;
        }

        @Override
        public void resolve() {
            resolved = true;
        }

        @Override
        public void reject(String msg, String code, Exception ex, JSObject data) {
            rejectMessage = msg;
        }
    }

    /**
     * Looks like a tags object with one key, but reading that key throws — a broken conversion.
     * Capacitor's JSObject.fromJSONObject copies through the JSONObject(JSONObject, String[])
     * constructor, which reads every key with opt(), so that is where the failure must surface.
     */
    private static class UnreadableJson extends JSONObject {

        @Override
        public Iterator<String> keys() {
            return Collections.singletonList("broken").iterator();
        }

        @Override
        public Object opt(String name) {
            throw new IllegalStateException("cannot read " + name);
        }
    }

    @Test
    public void successResolvesTagsAtTopLevelWithoutEnvelope() throws JSONException {
        RecordingCall call = new RecordingCall();
        JSONObject tags = new JSONObject()
                .put("name", "John")
                .put("age", 25)
                .put("favorite_categories", new JSONArray().put("sports").put("news"));

        Pushwoosh.resolveTags(call, tags);

        assertTrue("call must resolve", call.resolved);
        assertNotNull("resolve must carry data", call.resolvedData);
        assertEquals("John", call.resolvedData.getString("name"));
        assertEquals(25, call.resolvedData.getInt("age"));
        assertEquals(2, call.resolvedData.getJSONArray("favorite_categories").length());
        assertFalse("no `tags` envelope", call.resolvedData.has("tags"));
        assertNull("success must not reject", call.rejectMessage);
    }

    @Test
    public void conversionFailureRejectsAndNeverResolves() {
        RecordingCall call = new RecordingCall();

        Pushwoosh.resolveTags(call, new UnreadableJson());

        assertEquals("Failed to get Tags", call.rejectMessage);
        assertFalse("a rejected call must not also resolve", call.resolved);
        assertNull(call.resolvedData);
    }
}
