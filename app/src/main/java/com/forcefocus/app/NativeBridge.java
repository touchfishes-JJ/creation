package com.forcefocus.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

public class NativeBridge {
    private final Activity activity;
    private final SharedPreferences prefs;

    NativeBridge(Activity activity) {
        this.activity = activity;
        this.prefs = activity.getSharedPreferences(FocusState.PREFS, Context.MODE_PRIVATE);
    }

    @JavascriptInterface
    public void openAccessibilitySettings() {
        activity.runOnUiThread(() -> activity.startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
    }

    @JavascriptInterface
    public void openExactAlarmSettings() {
        activity.runOnUiThread(() -> {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    activity.startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                            Uri.parse("package:" + activity.getPackageName())));
                } else activity.startActivity(new Intent(Settings.ACTION_SETTINGS));
            } catch (Exception e) { activity.startActivity(new Intent(Settings.ACTION_SETTINGS)); }
        });
    }

    @JavascriptInterface
    public void startFocus(String task, int durationSeconds) {
        long end = System.currentTimeMillis() + Math.max(1, durationSeconds) * 1000L;
        FocusState.start(activity, task, end);
        FocusAlarmReceiver.scheduleEnd(activity, end);
    }

    @JavascriptInterface
    public int endFocusEarly() {
        if (!FocusState.consumeEarlyEnd(activity)) {
            toast("本周提前结束次数已用完");
            return -1;
        }
        FocusState.stop(activity);
        FocusAlarmReceiver.cancelEnd(activity);
        return FocusState.getEarlyRemaining(activity);
    }

    @JavascriptInterface public void finishFocus() { FocusState.stop(activity); FocusAlarmReceiver.cancelEnd(activity); }
    @JavascriptInterface public boolean isFocusActive() { return FocusState.isActive(activity); }
    @JavascriptInterface public long getFocusEnd() { return prefs.getLong("focus_end", 0L); }
    @JavascriptInterface public String getFocusTask() { return prefs.getString("focus_task", "考公"); }
    @JavascriptInterface public int getEarlyRemaining() { return FocusState.getEarlyRemaining(activity); }

    @JavascriptInterface
    public void launchApp(String label) {
        activity.runOnUiThread(() -> {
            Intent launch = AppResolver.findLaunchIntentByLabel(activity, label);
            if (launch != null) {
                try { activity.startActivity(launch); }
                catch (Exception e) { toast("无法打开 " + label); }
            } else toast("未找到已安装的 " + label);
        });
    }

    @JavascriptInterface
    public void setWeekendTask(int slot, String task) {
        if (slot < 0 || slot > 2) return;
        prefs.edit().putString("weekend_task_" + slot, task).apply();
        WeekendScheduler.scheduleAll(activity);
    }

    @JavascriptInterface public void rescheduleWeekend() { WeekendScheduler.scheduleAll(activity); }

    private void toast(String text) {
        activity.runOnUiThread(() -> Toast.makeText(activity, text, Toast.LENGTH_SHORT).show());
    }
}
