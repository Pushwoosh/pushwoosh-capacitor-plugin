package com.pushwoosh.plugin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.getcapacitor.JSObject;
import com.getcapacitor.PluginCall;

import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * Regression guard: crash-capacitor-android-communicationenabled-unboxing-npe
 *
 * <p>{@code boolean enabled = call.getBoolean("enabled")} used to unbox a {@code @Nullable Boolean}.
 * PluginCall.getBoolean only returns non-null when org.json boxed the value exactly as Boolean;
 * anything else (a string from localStorage, a number, JS null, a missing key) yielded null -> NPE
 * thrown synchronously out of the @PluginMethod, which the Capacitor bridge rethrows on its
 * HandlerThread "CapacitorPlugins" and kills the process (measured: FATAL EXCEPTION, SIG: 9).
 *
 * <p>The bad value does not require breaking the TypeScript contract: a value out of JSON.parse is
 * {@code any} and compiles against setCommunicationEnabled(enabled: boolean) even under full
 * strict, and {@code boolean | undefined} passes in any host without strictNullChecks.
 *
 * <p>Contract chosen to match the file's other guarded methods (setUserId, setLanguage,
 * setApiToken): a bad value comes back as a reject, and communication is never toggled on a guess.
 * The badge siblings unbox the same way and are guarded on their own branch, not here.
 *
 * <p>sdk is pinned to this module's compileSdk, not to the API 36 the crash was measured on: the
 * mechanism is unboxing plus reflective dispatch, so it does not depend on the platform version.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34, manifest = Config.NONE)
public class CommunicationEnabledUnboxingNpeTest {

    private static final String CRASH_CLASS = "com.pushwoosh.plugin.PushwooshPlugin";
    private static final String METHOD = "setCommunicationEnabled";
    private static final String EXPECTED_REJECT = "Missing enabled";

    /**
     * Records the plugin's answer instead of posting it through a MessageHandler, which cannot be
     * built without a live Bridge. Overriding the widest reject() overload catches every other one:
     * they all delegate to it.
     */
    private static class RecordingCall extends PluginCall {

        private String rejectMessage;
        private boolean resolved;

        RecordingCall(JSObject data) {
            super(null, "Pushwoosh", "cb-1", METHOD, data);
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
    private RecordingCall callWith(String json) throws JSONException {
        return new RecordingCall(new JSObject(json));
    }

    /**
     * A bad "enabled" must come back as a reject. No try/catch on purpose: on unguarded code the
     * unboxing NPE escapes here and reddens the case, which is what proves this guard is not empty.
     * The applied path always ends in a resolve, so an unset resolved flag is the proof that the
     * method returned early and the flag never reached the SDK.
     */
    private void assertRejectsEnabled(String json) throws JSONException {
        PushwooshPlugin plugin = new PushwooshPlugin();
        RecordingCall call = callWith(json);

        plugin.setCommunicationEnabled(call);

        assertEquals("reject message for options " + json, EXPECTED_REJECT, call.rejectMessage);
        assertFalse("must not resolve for options " + json, call.resolved);
    }

    // --- path (A): value not controlled by the app, arrives as a string ----------------------

    @Test
    public void stringTrueFromDom_rejects() throws JSONException {
        // the measured live-app repro: a GDPR toggle lifted from localStorage.getItem
        assertRejectsEnabled("{\"enabled\":\"true\"}");
    }

    @Test
    public void stringFalseFromJsonParse_rejects() throws JSONException {
        // remote config / push payload parsed with JSON.parse -> `any`, compiles under strict
        assertRejectsEnabled("{\"enabled\":\"false\"}");
    }

    // --- path (B): a number where a boolean was declared -------------------------------------

    @Test
    public void numberOne_rejects() throws JSONException {
        assertRejectsEnabled("{\"enabled\":1}");
    }

    @Test
    public void numberZero_rejects() throws JSONException {
        assertRejectsEnabled("{\"enabled\":0}");
    }

    // --- path (C): nothing usable in the options ---------------------------------------------

    @Test
    public void jsNull_rejects() throws JSONException {
        assertRejectsEnabled("{\"enabled\":null}");
    }

    @Test
    public void missingKey_rejects() throws JSONException {
        // `undefined` is stripped by JSON.stringify in native-bridge.js
        assertRejectsEnabled("{}");
    }

    // --- ground truth: a boolean the SDK can take must pass the guard -------------------------

    /**
     * A real boolean must get all the way through to a resolve. Measured: start/stopServerCommunication
     * are no-ops on an uninitialised SDK under Robolectric, so the whole method completes and nothing
     * is thrown — the assert can be the strong one. Both polarities are run, but what is checked is
     * that each passes the guard and resolves; which SDK branch ran is not observable without a
     * static mock of com.pushwoosh.Pushwoosh, and this suite does not carry one.
     */
    private void assertApplied(String json) throws JSONException {
        PushwooshPlugin plugin = new PushwooshPlugin();
        RecordingCall call = callWith(json);

        plugin.setCommunicationEnabled(call);

        assertNull("boolean " + json + " must not be rejected, got: " + call.rejectMessage, call.rejectMessage);
        assertTrue("boolean " + json + " must be applied and resolved", call.resolved);
    }

    @Test
    public void booleanTrue_isApplied() throws JSONException {
        assertApplied("{\"enabled\":true}");
    }

    @Test
    public void booleanFalse_isApplied() throws JSONException {
        assertApplied("{\"enabled\":false}");
    }

    // --- negative controls --------------------------------------------------------------------

    /**
     * A real boolean must NOT produce the NPE at the crash frame. Proves the crash is caused by the
     * bad value and not by the harness environment (Robolectric, uninitialised SDK, etc.). The frame
     * check stays even though the call is measured to complete cleanly: it is what would tell a
     * future reader that a new NPE came from the unboxing line and not from somewhere else.
     */
    @Test
    public void negativeControl_booleanTrue_doesNotCrashAtUnboxing() throws JSONException {
        PushwooshPlugin plugin = new PushwooshPlugin();
        RecordingCall call = callWith("{\"enabled\":true}");
        try {
            plugin.setCommunicationEnabled(call);
        } catch (Throwable t) {
            boolean sameFrame =
                t instanceof NullPointerException
                    && t.getStackTrace().length > 0
                    && CRASH_CLASS.equals(t.getStackTrace()[0].getClassName())
                    && METHOD.equals(t.getStackTrace()[0].getMethodName());
            assertFalse("a real boolean must not NPE at the unboxing line, got: " + t, sameFrame);
        }
        assertTrue("a real boolean must reach the resolve", call.resolved);
    }

    /** The accessor itself: exactly which boxings survive getBoolean. Keeps the asserts honest. */
    @Test
    public void negativeControl_accessorBoxing() throws JSONException {
        assertNotNull(callWith("{\"enabled\":true}").getBoolean("enabled"));
        assertNotNull(callWith("{\"enabled\":false}").getBoolean("enabled"));
        assertNull(callWith("{\"enabled\":\"true\"}").getBoolean("enabled"));
        assertNull(callWith("{\"enabled\":\"false\"}").getBoolean("enabled"));
        assertNull(callWith("{\"enabled\":1}").getBoolean("enabled"));
        assertNull(callWith("{\"enabled\":0}").getBoolean("enabled"));
        assertNull(callWith("{\"enabled\":null}").getBoolean("enabled"));
        assertNull(callWith("{}").getBoolean("enabled"));
    }

    /** Sanity: JS null really lands as JSONObject.NULL, so `opt` returns non-null garbage. */
    @Test
    public void negativeControl_jsNullIsJsonNull() throws JSONException {
        assertTrue(new JSObject("{\"enabled\":null}").isNull("enabled"));
    }
}
