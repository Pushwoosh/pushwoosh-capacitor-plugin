package com.pushwoosh.plugin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.PluginCall;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.List;

/**
 * A call the host got wrong has to come back as a rejection naming the parameter, and it has to
 * come back before the SDK is touched — an empty code reaches the SDK as a real lookup and does
 * nothing at all, which from JavaScript is indistinguishable from a promise that never settles.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34, manifest = Config.NONE)
public class InboxCallValidationTest {

    private static class RecordingCall extends PluginCall {

        boolean resolved;
        String rejectMessage;

        RecordingCall(JSObject options) {
            super(null, "Pushwoosh", "cb-1", "inbox", options);
        }

        @Override
        public void resolve(JSObject data) {
            resolved = true;
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

    private static RecordingCall call(JSObject options) {
        return new RecordingCall(options);
    }

    @Test
    public void missingCodeIsRejectedByName() {
        RecordingCall missing = call(new JSObject());
        assertNull(Inbox.requiredCode(missing));
        assertEquals("Missing required parameter: code", missing.rejectMessage);
        assertFalse(missing.resolved);

        RecordingCall blank = call(new JSObject().put("code", ""));
        assertNull(Inbox.requiredCode(blank));
        assertEquals("Missing required parameter: code", blank.rejectMessage);
    }

    @Test
    public void presentCodeIsAccepted() {
        RecordingCall accepted = call(new JSObject().put("code", "abc"));
        assertEquals("abc", Inbox.requiredCode(accepted));
        assertNull(accepted.rejectMessage);
    }

    @Test
    public void missingOrEmptyCodesAreRejectedByName() {
        RecordingCall missing = call(new JSObject());
        assertNull(Inbox.requiredCodes(missing));
        assertEquals("Missing required parameter: codes", missing.rejectMessage);

        RecordingCall empty = call(new JSObject().put("codes", new JSArray()));
        assertNull(Inbox.requiredCodes(empty));
        assertEquals("Missing required parameter: codes", empty.rejectMessage);
        assertFalse(empty.resolved);
    }

    @Test
    public void aCodesArrayHoldingAnEmptyOrNonStringEntryIsRejected() {
        JSArray withBlank = new JSArray();
        withBlank.put("abc");
        withBlank.put("");
        RecordingCall blank = call(new JSObject().put("codes", withBlank));
        assertNull(Inbox.requiredCodes(blank));
        assertEquals("Invalid parameter: codes", blank.rejectMessage);

        JSArray withObject = new JSArray();
        withObject.put("abc");
        withObject.put(new JSObject().put("code", "def"));
        RecordingCall object = call(new JSObject().put("codes", withObject));
        assertNull(Inbox.requiredCodes(object));
        assertEquals("Invalid parameter: codes", object.rejectMessage);
    }

    @Test
    public void aPopulatedCodesArrayIsAccepted() {
        JSArray codes = new JSArray();
        codes.put("abc");
        codes.put("def");
        RecordingCall accepted = call(new JSObject().put("codes", codes));

        List<String> parsed = Inbox.requiredCodes(accepted);

        assertEquals(2, parsed.size());
        assertEquals("abc", parsed.get(0));
        assertEquals("def", parsed.get(1));
        assertNull(accepted.rejectMessage);
    }
}
