package com.pushwoosh.plugin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.getcapacitor.JSObject;
import com.getcapacitor.PluginCall;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * Regression guard for the Android side of the plugin call handoff.
 *
 * <p>{@code pushReceivedPluginCall} / {@code pushOpenedPluginCall} are static fields written from
 * the bridge's "CapacitorPlugins" HandlerThread (that is where @PluginMethod bodies run) and read
 * from whichever thread the notification service extension delivers on. Two defects lived there:
 *
 * <ol>
 *   <li>the readers ({@code doOnPushReceived} / {@code doOnPushOpened}) run inside
 *       {@code synchronized (sStartPushLock)}, but the writers took no lock at all — a lock only one
 *       side holds is not a lock: neither mutual exclusion nor the happens-before that would publish
 *       the write to the reading thread exists;
 *   <li>{@code pushReceivedPluginCall = call; pushReceivedPluginCall.setKeepAlive(true);} loads the
 *       field back instead of using the argument, so a registration landing between the two
 *       statements leaves the earlier call without keepAlive — the bridge then releases it after its
 *       first resolve and that JS callback goes silent for the rest of the process.
 * </ol>
 *
 * <p>(1) is what {@code registrationBlocksWhileDeliveryHoldsTheLock} pins down: mutual exclusion is
 * observable without racing for it — hold the delivery lock, and a registration on another thread
 * must not be able to complete. (2) is pinned by asserting keepAlive on the argument itself.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34, manifest = Config.NONE)
public class PluginCallHandoffTest {

    private static final String PAYLOAD = "{\"title\":\"title\",\"message\":\"body\"}";

    /** How long a registration is given to prove it is NOT blocked. */
    private static final long BLOCK_PROBE_MS = 500;

    /** How long the same registration is given to finish once the lock is released. */
    private static final long RELEASE_TIMEOUT_MS = 5_000;

    /**
     * Records the plugin's answer instead of posting it through a MessageHandler, which cannot be
     * built without a live Bridge. Mirrors the RecordingCall the sibling crash guards in this package
     * use, plus the resolved payload, which is what a handoff test has to look at.
     */
    private static class RecordingCall extends PluginCall {

        private final List<String> resolvedNotifications = new ArrayList<>();

        RecordingCall(String callbackId) {
            super(null, "Pushwoosh", callbackId, "pushCallback", new JSObject());
        }

        @Override
        public void resolve(JSObject data) {
            resolvedNotifications.add(data.getString("notification"));
        }

        @Override
        public void resolve() {
            resolvedNotifications.add(null);
        }

        @Override
        public void reject(String msg, String code, Exception ex, JSObject data) {
            throw new AssertionError("the plugin call must not be rejected: " + msg);
        }
    }

    private static Object staticField(String name) throws Exception {
        Field field = Pushwoosh.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(null);
    }

    private static void setStaticField(String name, Object value) throws Exception {
        Field field = Pushwoosh.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(null, value);
    }

    /** The statics outlive an instance, so every case starts from a clean slate. */
    @Before
    public void resetStaticState() throws Exception {
        setStaticField("pushReceivedPluginCall", null);
        setStaticField("pushOpenedPluginCall", null);
        setStaticField("sStartPushData", null);
        setStaticField("sReceivedPushData", null);
        setStaticField("sAppReady", new AtomicBoolean(false));
    }

    /**
     * An instance that deliveries actually reach. The constructor resets sAppReady to false, so the
     * flag is raised after it — directly, and not through onDeviceReady, which reaches the native
     * SDK: what these cases are about is the handoff, not initialisation.
     */
    private Pushwoosh readyPushwoosh() throws Exception {
        Pushwoosh pushwoosh = new Pushwoosh();
        setStaticField("sAppReady", new AtomicBoolean(true));
        return pushwoosh;
    }

    // --- (1) the writers must take the lock the readers already hold -------------------------

    /**
     * Holds the delivery lock and starts a registration on another thread. On unguarded code the
     * registration sails through — which is exactly the report: the field is written with no lock
     * while deliveries read it under one. No sleep-and-hope: the assert is on the writer still being
     * alive while the lock is held, and on it finishing once the lock is dropped.
     */
    private void assertRegistrationBlocks(String kind) throws Exception {
        Pushwoosh pushwoosh = new Pushwoosh();
        RecordingCall call = new RecordingCall(kind + "-blocked");
        Object deliveryLock = staticField("sStartPushLock");
        Thread writer;

        synchronized (deliveryLock) {
            writer = new Thread(() -> {
                if ("received".equals(kind)) {
                    pushwoosh.pushReceivedCallback(call);
                } else {
                    pushwoosh.pushOpenedCallback(call);
                }
            }, "test-bridge-" + kind);
            writer.start();
            writer.join(BLOCK_PROBE_MS);

            assertTrue(
                kind + " registration completed while the delivery lock was held: it takes no lock",
                writer.isAlive());
        }

        writer.join(RELEASE_TIMEOUT_MS);
        assertFalse(kind + " registration must complete once the lock is released", writer.isAlive());
    }

    @Test
    public void receivedRegistrationBlocksWhileDeliveryHoldsTheLock() throws Exception {
        assertRegistrationBlocks("received");
    }

    @Test
    public void openedRegistrationBlocksWhileDeliveryHoldsTheLock() throws Exception {
        assertRegistrationBlocks("opened");
    }

    // --- (2) keepAlive belongs to the call that was passed in ---------------------------------

    @Test
    public void receivedKeepAliveIsSetOnTheArgument() {
        Pushwoosh pushwoosh = new Pushwoosh();
        RecordingCall call = new RecordingCall("received-alive");

        pushwoosh.pushReceivedCallback(call);

        assertTrue("the callback is persistent: the bridge must not release it after one resolve",
            call.isKeptAlive());
    }

    @Test
    public void openedKeepAliveIsSetOnTheArgument() {
        Pushwoosh pushwoosh = new Pushwoosh();
        RecordingCall call = new RecordingCall("opened-alive");

        pushwoosh.pushOpenedCallback(call);

        assertTrue(call.isKeptAlive());
    }

    /**
     * The same guarantee through the plugin class, which is what the bridge actually calls: whoever
     * owns the keepAlive statement, a call that came in through @PluginMethod has to come back
     * persistent and be the one a delivery would resolve.
     */
    @Test
    public void receivedRegistrationThroughThePluginKeepsTheCallAlive() throws Exception {
        PushwooshPlugin plugin = new PushwooshPlugin();
        RecordingCall call = new RecordingCall("plugin-received");

        plugin.pushReceivedCallback(call);

        assertTrue(call.isKeptAlive());
        assertSame(call, staticField("pushReceivedPluginCall"));
    }

    @Test
    public void openedRegistrationThroughThePluginKeepsTheCallAlive() throws Exception {
        PushwooshPlugin plugin = new PushwooshPlugin();
        RecordingCall call = new RecordingCall("plugin-opened");

        plugin.pushOpenedCallback(call);

        assertTrue(call.isKeptAlive());
        assertSame(call, staticField("pushOpenedPluginCall"));
    }

    // --- delivery behaviour the fix must not change -------------------------------------------

    @Test
    public void receivedDeliveryResolvesTheRegisteredCall() throws Exception {
        Pushwoosh pushwoosh = readyPushwoosh();
        RecordingCall call = new RecordingCall("received-1");
        pushwoosh.pushReceivedCallback(call);

        Pushwoosh.messageReceived(PAYLOAD);

        assertEquals(1, call.resolvedNotifications.size());
        assertEquals(PAYLOAD, call.resolvedNotifications.get(0));
    }

    @Test
    public void openedDeliveryResolvesTheRegisteredCall() throws Exception {
        Pushwoosh pushwoosh = readyPushwoosh();
        RecordingCall call = new RecordingCall("opened-1");
        pushwoosh.pushOpenedCallback(call);

        Pushwoosh.openPush(PAYLOAD);

        assertEquals(1, call.resolvedNotifications.size());
        assertEquals(PAYLOAD, call.resolvedNotifications.get(0));
    }

    @Test
    public void deliveryWithoutRegistrationIsANoOp() throws Exception {
        readyPushwoosh();

        Pushwoosh.messageReceived(PAYLOAD);
        Pushwoosh.openPush(PAYLOAD);

        assertNull(staticField("pushReceivedPluginCall"));
        assertNull(staticField("pushOpenedPluginCall"));
    }

    /**
     * The last registration wins, and the previous call is not resolved by it. Cheap ground truth
     * that a re-registration replaces rather than accumulates.
     */
    @Test
    public void reRegistrationReplacesThePreviousCall() throws Exception {
        Pushwoosh pushwoosh = readyPushwoosh();
        RecordingCall first = new RecordingCall("received-old");
        RecordingCall second = new RecordingCall("received-new");
        pushwoosh.pushReceivedCallback(first);
        pushwoosh.pushReceivedCallback(second);

        Pushwoosh.messageReceived(PAYLOAD);

        assertTrue("the replaced call must not be resolved", first.resolvedNotifications.isEmpty());
        assertEquals(1, second.resolvedNotifications.size());
        assertSame(second, staticField("pushReceivedPluginCall"));
    }
}
