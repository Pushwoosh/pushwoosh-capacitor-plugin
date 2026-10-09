package com.pushwoosh.plugin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.os.Looper;

import com.getcapacitor.JSObject;
import com.getcapacitor.PluginCall;
import com.pushwoosh.inbox.ui.PushwooshInboxStyle;
import com.pushwoosh.inbox.ui.presentation.view.activity.InboxActivity;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Date;
import java.util.Locale;

/**
 * The SDK style is a process-wide singleton with no reset, so a style applied by one call is
 * still in place for the next one unless the plugin puts the defaults back. These cases pin what
 * a caller can observe from the one entry point that exists: the keys they passed land on the SDK
 * object, a value the platform cannot parse is skipped without taking its neighbours down, nothing
 * a previous call set survives into a call that did not set it, and the inbox screen is launched.
 *
 * The screen comes up asynchronously - startActivity only queues it, and InboxActivity reads the
 * style singleton in its own onCreate - so the cases below drive that second half themselves: a
 * call is answered when the screen reports itself, a call arriving while a screen is still coming
 * up must not get to rewrite the style under it, and a screen that never arrives must not leave
 * the caller waiting forever.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34, manifest = Config.NONE)
public class InboxStyleApplierTest {

    private static class RecordingCall extends PluginCall {

        int settlements;
        boolean resolved;
        String rejectMessage;

        RecordingCall() {
            super(null, "Pushwoosh", "cb-1", "presentInboxUI", new JSObject());
        }

        @Override
        public void resolve() {
            settlements++;
            resolved = true;
        }

        @Override
        public void resolve(JSObject data) {
            settlements++;
            resolved = true;
        }

        @Override
        public void reject(String msg, String code, Exception ex, JSObject data) {
            settlements++;
            rejectMessage = msg;
        }
    }

    private Activity activity;
    private Inbox inbox;

    @Before
    public void setUp() {
        activity = Robolectric.buildActivity(Activity.class).create().get();
        inbox = new Inbox();
    }

    private static JSONObject style(String... pairs) throws JSONException {
        JSONObject style = new JSONObject();
        for (int index = 0; index < pairs.length; index += 2) {
            style.put(pairs[index], pairs[index + 1]);
        }
        return style;
    }

    private RecordingCall present(JSONObject style) {
        RecordingCall call = new RecordingCall();
        inbox.presentInboxUI(activity, style, call);
        return call;
    }

    /**
     * Brings the screen the plugin launched up for real: creating the activity is what makes the
     * SDK read the style singleton, and it is what the plugin is waiting to hear about. Stops at
     * onCreate - the inbox fragment talks to an initialized native SDK on start, which a unit test
     * has none of.
     */
    private void inboxScreenComesUp() {
        Robolectric.buildActivity(InboxActivity.class).create();
    }

    private Intent nextStartedActivity() {
        return Shadows.shadowOf(activity).getNextStartedActivity();
    }

    @Test
    public void passedKeysLandOnTheSdkStyleAndTheScreenIsLaunched() throws JSONException {
        Date sendDate = new Date(1791371525000L);

        RecordingCall call = present(style(
                "barTitle", "Demo Inbox",
                "accentColor", "#FF6600",
                "listEmptyMessage", "Nothing here yet",
                "dateFormat", "dd.MM.yyyy"));

        assertEquals("Demo Inbox", PushwooshInboxStyle.INSTANCE.getBarTitle());
        assertEquals(Integer.valueOf(Color.parseColor("#FF6600")), PushwooshInboxStyle.INSTANCE.getAccentColor());
        assertEquals("Nothing here yet", String.valueOf(PushwooshInboxStyle.INSTANCE.getListEmptyText()));
        assertEquals(new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(sendDate),
                PushwooshInboxStyle.INSTANCE.getDateFormatter().transform(sendDate));

        Intent started = nextStartedActivity();
        assertNotNull("the inbox screen must be started", started);
        assertEquals("com.pushwoosh.inbox.ui.presentation.view.activity.InboxActivity",
                started.getComponent().getClassName());
        assertNull(call.rejectMessage);
    }

    @Test
    public void theCallIsAnsweredOnlyOnceTheScreenIsUp() throws JSONException {
        RecordingCall call = present(style("barTitle", "Demo Inbox"));

        assertFalse("startActivity only queues the screen - nothing is on display yet", call.resolved);
        assertEquals(0, call.settlements);

        inboxScreenComesUp();

        assertTrue("the screen reporting itself must answer the caller", call.resolved);
        assertEquals(1, call.settlements);
        assertNull(call.rejectMessage);
    }

    @Test
    public void aCallArrivingBeforeTheScreenIsUpIsRefusedAndLeavesTheFirstStyleInPlace() throws JSONException {
        RecordingCall first = present(style("barTitle", "First", "accentColor", "#FF6600"));

        RecordingCall second = present(style("barTitle", "Second", "accentColor", "#0000FF"));

        assertEquals("the style the screen is about to read must be the first call's",
                "First", PushwooshInboxStyle.INSTANCE.getBarTitle());
        assertEquals(Integer.valueOf(Color.parseColor("#FF6600")), PushwooshInboxStyle.INSTANCE.getAccentColor());
        assertNotNull("the refused call must be rejected", second.rejectMessage);
        assertFalse(second.resolved);
        assertEquals("the refused call must settle exactly once", 1, second.settlements);
        assertFalse("the first call is still waiting for its screen", first.resolved);

        assertNotNull("the first call's screen must be started", nextStartedActivity());
        assertNull("a refused call must not stack a second screen", nextStartedActivity());

        inboxScreenComesUp();

        assertTrue(first.resolved);
        assertEquals(1, first.settlements);
    }

    @Test
    public void lowercaseAndAlphaHexAreAccepted() throws JSONException {
        present(style(
                "accentColor", "#ff6600",
                "titleColor", "0x80ff6600",
                "dateColor", "#f60"));

        assertEquals(Integer.valueOf(Color.parseColor("#FF6600")), PushwooshInboxStyle.INSTANCE.getAccentColor());
        assertEquals(Integer.valueOf(Color.parseColor("#80FF6600")), PushwooshInboxStyle.INSTANCE.getTitleColor());
        assertEquals(Integer.valueOf(Color.parseColor("#FF6600")), PushwooshInboxStyle.INSTANCE.getDateColor());
    }

    @Test
    public void anUnparseableValueIsSkippedAndItsNeighboursStillApply() throws JSONException {
        JSONObject style = new JSONObject();
        style.put("accentColor", "not a color");
        style.put("dateFormat", "dd.MM.yyyy'");
        style.put("barTitle", 42);
        style.put("listEmptyMessage", new JSONObject().put("text", "nope"));
        style.put("titleColor", "#112233");

        RecordingCall call = present(style);

        assertNull("an unparseable color must not be written", PushwooshInboxStyle.INSTANCE.getAccentColor());
        assertNull("a non-string title must not be coerced", PushwooshInboxStyle.INSTANCE.getBarTitle());
        assertNotNull("the default formatter must survive a broken pattern",
                PushwooshInboxStyle.INSTANCE.getDateFormatter());
        assertEquals(Integer.valueOf(Color.parseColor("#112233")), PushwooshInboxStyle.INSTANCE.getTitleColor());

        inboxScreenComesUp();
        assertTrue("a skipped key must not fail the call", call.resolved);
    }

    @Test
    public void keysOfAPreviousCallDoNotSurviveIntoTheNextOne() throws JSONException {
        present(style("barTitle", "First", "accentColor", "#FF6600"));
        inboxScreenComesUp();

        present(style("barTitle", "Second"));

        assertEquals("Second", PushwooshInboxStyle.INSTANCE.getBarTitle());
        assertNull("the accent of the previous call must be gone", PushwooshInboxStyle.INSTANCE.getAccentColor());
    }

    @Test
    public void aCallWithoutAStyleRestoresTheDefaults() throws JSONException {
        present(style("barTitle", "First", "accentColor", "#FF6600"));
        inboxScreenComesUp();

        present(null);

        assertNull(PushwooshInboxStyle.INSTANCE.getBarTitle());
        assertNull(PushwooshInboxStyle.INSTANCE.getAccentColor());
    }

    /**
     * Stands in for an integrator who excluded or shrank away pushwoosh-inbox-ui's manifest entry.
     * Capacitor rethrows anything escaping a plugin method on its task-handler thread, so an
     * exception out of startActivity would kill the process with the call never settled.
     */
    public static class ActivityWithoutInboxUi extends Activity {
        @Override
        public void startActivity(Intent intent) {
            throw new ActivityNotFoundException("no InboxActivity in this build");
        }
    }

    @Test
    public void anInboxScreenThatCannotBeStartedRejectsInsteadOfEscaping() {
        Activity withoutInboxUi = Robolectric.buildActivity(ActivityWithoutInboxUi.class).create().get();
        RecordingCall call = new RecordingCall();

        inbox.presentInboxUI(withoutInboxUi, null, call);

        assertNotNull("an unlaunchable inbox screen must reject the call", call.rejectMessage);
        assertTrue(call.rejectMessage, call.rejectMessage.startsWith("Failed to present the inbox: "));
        assertTrue(call.rejectMessage, call.rejectMessage.contains("no InboxActivity in this build"));
        assertFalse("a rejected call must not also resolve", call.resolved);
        assertEquals(1, call.settlements);

        RecordingCall afterwards = present(null);
        assertNull("a failed launch must not block the next one", afterwards.rejectMessage);
        assertNotNull("a failed launch must not block the next one", nextStartedActivity());
    }

    /**
     * A style object that blows up halfway through the apply. Nothing in the parser throws today -
     * every value it cannot read is caught and warned about - which is the point: the guard has to
     * hold for the exception nobody predicted, because Capacitor rethrows whatever escapes a plugin
     * method on its task-handler thread and the promise behind the call is then never settled.
     */
    @Test
    public void aStyleThatThrowsHalfwayThroughRejectsInsteadOfEscaping() {
        JSONObject hostile = new JSONObject() {
            @Override
            public Object opt(String name) {
                throw new IllegalStateException("broken style");
            }
        };

        RecordingCall call = present(hostile);

        assertNotNull("the call must come back rejected", call.rejectMessage);
        assertTrue(call.rejectMessage, call.rejectMessage.contains("broken style"));
        assertEquals(1, call.settlements);
        assertNull("a style that never applied must not start a screen", nextStartedActivity());
    }

    @Test
    public void aScreenThatNeverComesUpRejectsInsteadOfLeavingTheCallWaiting() throws JSONException {
        RecordingCall call = present(style("barTitle", "Demo Inbox"));
        assertEquals(0, call.settlements);

        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(11));

        assertNotNull("a screen that never came up must reject the call", call.rejectMessage);
        assertTrue(call.rejectMessage, call.rejectMessage.contains("did not come up"));
        assertFalse(call.resolved);
        assertEquals(1, call.settlements);

        RecordingCall next = present(style("barTitle", "Second"));
        inboxScreenComesUp();
        assertTrue("a timed-out launch must not block the next one", next.resolved);
    }

    @Test
    public void presentInboxUIWithoutAnActivityRejects() {
        RecordingCall call = new RecordingCall();

        inbox.presentInboxUI(null, null, call);

        assertEquals("Failed to present the inbox: the plugin has no current activity", call.rejectMessage);
        assertFalse("a rejected call must not also resolve", call.resolved);
        assertEquals(1, call.settlements);
    }
}
