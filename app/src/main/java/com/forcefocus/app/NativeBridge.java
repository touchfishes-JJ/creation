package com.forcefocus.app;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/** The narrow bridge between the accepted v16 DOM and Android system APIs. */
public final class NativeBridge {
    private final Activity activity;

    NativeBridge(Activity activity) {
        this.activity = activity;
    }

    @JavascriptInterface
    public String getStateJson() {
        try {
            JSONObject state = FocusState.stateJson(activity);
            state.put("exactAlarmAllowed", WeekendScheduler.canScheduleExact(activity));
            state.put("accessibilityEnabled", isAccessibilityEnabled());
            return state.toString();
        } catch (JSONException error) {
            return "{}";
        }
    }

    @JavascriptInterface
    public void setSelectedTask(String task) {
        FocusState.setSelectedTask(activity, task);
    }

    @JavascriptInterface
    public long startFocus(String task, int durationSeconds) {
        int safeSeconds = Math.max(1, Math.min(4 * 3600 + 59 * 60, durationSeconds));
        long end = FocusState.start(
                activity, task, System.currentTimeMillis() + safeSeconds * 1000L);
        FocusState.rememberDuration(activity, safeSeconds);
        FocusAlarmReceiver.scheduleEnd(activity, end);
        return end;
    }

    @JavascriptInterface
    public int endFocusEarly() {
        int remaining = FocusState.endEarly(activity);
        if (remaining >= 0) FocusAlarmReceiver.cancelEnd(activity);
        return remaining;
    }

    @JavascriptInterface
    public void finishFocus() {
        FocusState.finishNaturally(activity);
        FocusAlarmReceiver.cancelEnd(activity);
    }

    @JavascriptInterface
    public int getEarlyRemaining() {
        return FocusState.getEarlyRemaining(activity);
    }

    @JavascriptInterface
    public String getRecentDurationsJson() {
        JSONArray array = new JSONArray();
        List<Integer> recent = FocusState.recentDurations(activity);
        for (Integer seconds : recent) array.put(seconds);
        return array.toString();
    }

    @JavascriptInterface
    public void setDurationLock(boolean locked, int hours, int minutes) {
        FocusState.setDurationLock(activity, locked, hours, minutes);
    }

    @JavascriptInterface
    public void setWeekendTask(int slot, String task) {
        FocusState.setWeekendTask(activity, slot, task);
        WeekendScheduler.scheduleAll(activity);
    }

    @JavascriptInterface
    public void setWeekendLocked(boolean locked) {
        FocusState.setWeekendLocked(activity, locked);
        WeekendScheduler.scheduleAll(activity);
    }

    @JavascriptInterface
    public String getCalendarMonthJson(int year, int monthOneBased) {
        try {
            return FocusState.calendarMonth(activity, year, monthOneBased).toString();
        } catch (JSONException | IllegalArgumentException error) {
            return "{\"minutes\":{},\"marks\":[]}";
        }
    }

    @JavascriptInterface
    public void setCalendarMark(
            int year, int monthOneBased, int day, boolean marked) {
        FocusState.setCalendarMark(activity, year, monthOneBased, day, marked);
    }

    @JavascriptInterface
    public void launchApp(String label) {
        activity.runOnUiThread(() -> {
            Intent launch = AppResolver.findLaunchIntent(activity, label);
            if (launch == null) {
                toast("未找到已安装的 " + label);
                return;
            }
            try {
                activity.startActivity(launch);
            } catch (RuntimeException error) {
                toast("无法打开 " + label);
            }
        });
    }

    @JavascriptInterface
    public void openAccessibilitySettings() {
        activity.runOnUiThread(() -> {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            activity.startActivity(intent);
        });
    }

    @JavascriptInterface
    public void openExactAlarmSettings() {
        activity.runOnUiThread(() -> {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Intent intent = new Intent(
                            Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                            Uri.parse("package:" + activity.getPackageName()));
                    activity.startActivity(intent);
                } else {
                    Intent intent = new Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse("package:" + activity.getPackageName()));
                    activity.startActivity(intent);
                }
            } catch (RuntimeException error) {
                activity.startActivity(new Intent(Settings.ACTION_SETTINGS));
            }
        });
    }

    private boolean isAccessibilityEnabled() {
        String enabled = Settings.Secure.getString(
                activity.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (TextUtils.isEmpty(enabled)) return false;
        ComponentName component = new ComponentName(activity, FocusAccessibilityService.class);
        String full = component.flattenToString();
        String shortName = component.flattenToShortString();
        for (String item : enabled.split(":")) {
            if (full.equalsIgnoreCase(item) || shortName.equalsIgnoreCase(item)) return true;
        }
        return false;
    }

    private void toast(String text) {
        activity.runOnUiThread(
                () -> Toast.makeText(activity, text, Toast.LENGTH_SHORT).show());
    }
}
