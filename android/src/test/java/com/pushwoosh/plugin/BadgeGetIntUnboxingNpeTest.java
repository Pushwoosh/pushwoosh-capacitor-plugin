package com.pushwoosh.plugin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.os.Looper;

import com.getcapacitor.JSObject;
import com.getcapacitor.PluginCall;

import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * Regression guard: crash-capacitor-android-badge-getint-unboxing-npe
 *
 * <p>{@code int badgeNumber = call.getInt("badge")} used to unbox a {@code @Nullable Integer}.
 * PluginCall.getInt only returns non-null when org.json boxed the value exactly as Integer;
 * anything else (fractional number, value > 2^31-1, string, JS null, missing key) yielded null ->
 * NPE thrown synchronously out of the @PluginMethod, which the Capacitor bridge rethrows on its
 * HandlerThread and kills the process.
 *
 * <p>Both crash sites are guarded, byte-for-byte identical before the fix:
 * PushwooshPlugin.setApplicationIconBadgeNumber and PushwooshPlugin.addToApplicationIconBadgeNumber.
 * Each bad value must now come back as a reject, and the badge must never reach the SDK.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34, manifest = Config.NONE)
public class BadgeGetIntUnboxingNpeTest {

    private static final String SET_METHOD = "setApplicationIconBadgeNumber";
    private static final String ADD_METHOD = "addToApplicationIconBadgeNumber";
    private static final String EXPECTED_REJECT = "Missing badge";

    /**
     * Records the plugin's answer instead of posting it through a MessageHandler, which cannot be
     * built without a live Bridge. Overriding the widest reject() overload catches every other one:
     * they all delegate to it. The two resolve() overloads do not delegate — each dereferences
     * msgHandler on its own — so both are overridden and no path here can touch the null handler.
     */
    private static class RecordingCall extends PluginCall {

        private String rejectMessage;
        private boolean resolved;

        RecordingCall(String methodName, JSObject data) {
            super(null, "Pushwoosh", "cb-1", methodName, data);
        }

        @Override
        public void reject(String msg, String code, Exception ex, JSObject data) {
            rejectMessage = msg;
        }

        @Override
        public void resolve() {
            resolved = true;
        }

        @Override
        public void resolve(JSObject data) {
            resolved = true;
        }
    }

    /**
     * Builds the call the way MessageHandler.postMessage does — {@code new JSObject(jsonStr)} — so the
     * options carry the boxing a real bridge would give them, which is the whole subject of this test.
     * Always a RecordingCall: a raw PluginCall has a null MessageHandler, so its resolve() NPEs and
     * that NPE masks whatever the case was measuring.
     */
    private RecordingCall callWith(String json, String methodName) throws JSONException {
        return new RecordingCall(methodName, new JSObject(json));
    }

    /**
     * A bad badge must come back as a reject. No try/catch on purpose: on unguarded code the
     * unboxing NPE escapes here and reddens the case, which is what proves this guard is not empty.
     * The applied path always ends in a resolve, so an unset resolved flag is the proof that the
     * method returned early and the badge never reached the SDK.
     */
    private void assertRejectsBadge(String json, String methodName) throws JSONException {
        PushwooshPlugin plugin = new PushwooshPlugin();
        RecordingCall call = callWith(json, methodName);

        if (SET_METHOD.equals(methodName)) {
            plugin.setApplicationIconBadgeNumber(call);
        } else {
            plugin.addToApplicationIconBadgeNumber(call);
        }

        assertEquals("reject message for " + methodName + " with options " + json, EXPECTED_REJECT, call.rejectMessage);
        assertFalse("must not resolve for " + methodName + " with options " + json, call.resolved);
    }

    // --- path (A): TS-legal `number` that org.json does not box as Integer -------------------

    @Test
    public void fractionalBadge_rejects() throws JSONException {
        // setApplicationIconBadgeNumber(2.5) — legal per definitions.ts (badge: number)
        assertRejectsBadge("{\"badge\":2.5}", SET_METHOD);
    }

    @Test
    public void badgeOverIntMax_rejects() throws JSONException {
        // e.g. Date.now() — boxed as Long
        assertRejectsBadge("{\"badge\":1770768000000}", SET_METHOD);
    }

    // --- path (B): value the app does not control (badge lifted from a push payload) ---------

    @Test
    public void stringBadge_rejects() throws JSONException {
        // pw_badges rides as a string in the push JSON handed to pushReceivedCallback
        assertRejectsBadge("{\"badge\":\"5\"}", SET_METHOD);
    }

    @Test
    public void jsNullBadge_rejects() throws JSONException {
        assertRejectsBadge("{\"badge\":null}", SET_METHOD);
    }

    @Test
    public void missingBadgeKey_rejects() throws JSONException {
        // `undefined` is stripped by JSON.stringify in native-bridge.js
        assertRejectsBadge("{}", SET_METHOD);
    }

    // --- sibling crash site -------------------------------------------------------------------

    @Test
    public void addToBadge_fractional_rejects() throws JSONException {
        assertRejectsBadge("{\"badge\":2.5}", ADD_METHOD);
    }

    @Test
    public void addToBadge_missingKey_rejects() throws JSONException {
        assertRejectsBadge("{}", ADD_METHOD);
    }

    // --- ground truth: the guard must not swallow a badge the SDK can take --------------------

    /**
     * An int32 badge must get all the way through to a resolve. The plugin method itself is
     * synchronous — getInt, hand the badge to PushwooshBadge, resolve — while PushwooshBadge only
     * posts a lambda to the main looper, which Robolectric's default PAUSED mode leaves sitting in the
     * queue. Draining the looper runs that lambda too, so this case covers the applied path whole and
     * not just its synchronous half: nothing on it throws on an uninitialised SDK. The guard regress
     * itself is caught by the reject cases above, not here — an int32 badge unboxes cleanly with or
     * without the null check.
     */
    @Test
    public void int32Badge_isNotRejected() throws JSONException {
        PushwooshPlugin plugin = new PushwooshPlugin();
        RecordingCall call = callWith("{\"badge\":5}", SET_METHOD);

        plugin.setApplicationIconBadgeNumber(call);
        // the badge runnable is only queued under PAUSED mode; run it so the whole path is exercised
        shadowOf(Looper.getMainLooper()).idle();

        assertNull("int32 badge must not be rejected, got: " + call.rejectMessage, call.rejectMessage);
        assertTrue("int32 badge must be applied and resolved", call.resolved);
    }

    // --- negative controls --------------------------------------------------------------------

    /** The accessor itself: exactly which boxings survive getInt. Keeps the asserts honest. */
    @Test
    public void negativeControl_accessorBoxing() throws JSONException {
        assertNotNull(callWith("{\"badge\":5}", SET_METHOD).getInt("badge"));
        assertNull(callWith("{\"badge\":2.5}", SET_METHOD).getInt("badge"));
        assertNull(callWith("{\"badge\":1770768000000}", SET_METHOD).getInt("badge"));
        assertNull(callWith("{\"badge\":\"5\"}", SET_METHOD).getInt("badge"));
        assertNull(callWith("{\"badge\":null}", SET_METHOD).getInt("badge"));
        assertNull(callWith("{}", SET_METHOD).getInt("badge"));
    }

    /** Sanity: JS null really lands as JSONObject.NULL, so `opt` returns non-null garbage. */
    @Test
    public void negativeControl_jsNullIsJsonNull() throws JSONException {
        JSObject data = new JSObject("{\"badge\":null}");
        assertTrue(data.isNull("badge"));
    }
}
