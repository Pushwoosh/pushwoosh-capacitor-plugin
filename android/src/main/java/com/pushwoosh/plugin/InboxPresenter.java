package com.pushwoosh.plugin;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.getcapacitor.PluginCall;
import com.pushwoosh.inbox.ui.presentation.view.activity.InboxActivity;
import com.pushwoosh.internal.utils.PWLog;

import org.json.JSONObject;

/**
 * Opens the native inbox screen for one plugin call at a time.
 *
 * startActivity only queues the launch: InboxActivity reads the process-wide SDK style in its own
 * onCreate, long after the plugin method returned. Answering the caller at startActivity would
 * let the next call put its own style on that singleton before the first screen ever read it, and
 * would stack a second screen on a double tap, so a launch stays in flight until InboxActivity
 * reports itself through the application's lifecycle callbacks, and a launch arriving while
 * another is in flight is refused rather than queued.
 *
 * The call is settled exactly once on every path: the screen coming up, a launch the platform
 * refused, and the screen that never comes up at all.
 */
final class InboxPresenter implements Application.ActivityLifecycleCallbacks {

    /** How long the launched screen has to report itself before the caller is told it failed. */
    private static final long SCREEN_TIMEOUT_MS = 10_000L;

    private final Handler mainThread = new Handler(Looper.getMainLooper());
    private final Runnable screenNeverCameUp = () ->
            reject("Failed to present the inbox: the inbox screen did not come up within "
                    + (SCREEN_TIMEOUT_MS / 1000L) + " seconds");

    private Application application;
    private PluginCall pending;

    void present(Activity activity, JSONObject style, PluginCall call) {
        if (activity == null) {
            call.reject("Failed to present the inbox: the plugin has no current activity");
            return;
        }
        synchronized (this) {
            if (pending != null) {
                call.reject("Failed to present the inbox: an inbox screen is already opening");
                return;
            }
            pending = call;
            application = activity.getApplication();
            application.registerActivityLifecycleCallbacks(this);
            mainThread.postDelayed(screenNeverCameUp, SCREEN_TIMEOUT_MS);
        }
        try {
            InboxStyleApplier.apply(activity, style);
            activity.startActivity(new Intent(activity, InboxActivity.class));
        } catch (RuntimeException e) {
            reject("Failed to present the inbox: " + e);
        }
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
        if (!(activity instanceof InboxActivity)) {
            return;
        }
        PluginCall call = settle();
        if (call == null) {
            return;
        }
        PWLog.info(Inbox.TAG, "Inbox UI presented");
        call.resolve();
    }

    private void reject(String message) {
        PluginCall call = settle();
        if (call != null) {
            call.reject(message);
        }
    }

    /** Takes the call still waiting, if any, and drops the in-flight state so the next one can start. */
    private synchronized PluginCall settle() {
        PluginCall call = pending;
        if (call == null) {
            return null;
        }
        pending = null;
        mainThread.removeCallbacks(screenNeverCameUp);
        application.unregisterActivityLifecycleCallbacks(this);
        application = null;
        return call;
    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {
    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {
    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
    }
}
