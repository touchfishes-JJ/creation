package com.forcefocus.app;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Persistent source of truth for focus, duration, weekend and calendar data. */
public final class FocusState {
    public static final String PREFS = "forcefocus_native_v16";
    public static final int NO_WEEKEND_SLOT = -1;

    private static final String[] VALID_TASKS = {"简历", "岗位调研", "考公", "磨耳朵"};
    private static final String[] DEFAULT_WEEKEND_TASKS = {"简历", "岗位调研", "考公"};

    private FocusState() {}

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static String normalizeTask(String task) {
        if (task != null) {
            for (String valid : VALID_TASKS) {
                if (valid.equals(task)) return valid;
            }
        }
        return "简历";
    }

    public static synchronized long start(Context context, String task, long endTime) {
        return start(context, task, endTime, NO_WEEKEND_SLOT);
    }

    public static synchronized long start(
            Context context, String task, long endTime, int weekendSlot) {
        long now = System.currentTimeMillis();
        long safeEnd = Math.max(now + 1_000L, endTime);
        SharedPreferences p = prefs(context);
        if (p.getBoolean("focus_active", false)) {
            finishAndRecordLocked(context, Math.min(now, p.getLong("focus_end", now)));
        }
        p.edit()
                .putBoolean("focus_active", true)
                .putString("focus_task", normalizeTask(task))
                .putString("selected_task", normalizeTask(task))
                .putLong("focus_start", now)
                .putLong("focus_end", safeEnd)
                .putInt("focus_weekend_slot", weekendSlot)
                .commit();
        return safeEnd;
    }

    public static synchronized boolean isActive(Context context) {
        SharedPreferences p = prefs(context);
        if (!p.getBoolean("focus_active", false)) return false;
        long end = p.getLong("focus_end", 0L);
        if (end <= 0L || System.currentTimeMillis() >= end) {
            finishAndRecordLocked(context, end > 0L ? end : System.currentTimeMillis());
            return false;
        }
        return true;
    }

    public static synchronized boolean finishNaturally(Context context) {
        SharedPreferences p = prefs(context);
        long end = p.getLong("focus_end", System.currentTimeMillis());
        return finishAndRecordLocked(context, Math.min(System.currentTimeMillis(), end));
    }

    public static synchronized int endEarly(Context context) {
        if (!isActive(context)) return getEarlyRemaining(context);
        if (!consumeEarlyEndLocked(context)) return -1;

        SharedPreferences p = prefs(context);
        int slot = p.getInt("focus_weekend_slot", NO_WEEKEND_SLOT);
        if (slot >= 0 && slot < 3) {
            p.edit().putString("weekend_skip_instance", WeekendScheduler.currentInstanceId(slot)).commit();
        }
        finishAndRecordLocked(context, System.currentTimeMillis());
        return getEarlyRemaining(context);
    }

    private static boolean finishAndRecordLocked(Context context, long stopTime) {
        SharedPreferences p = prefs(context);
        if (!p.getBoolean("focus_active", false)) return false;
        long start = p.getLong("focus_start", stopTime);
        long end = p.getLong("focus_end", stopTime);
        long safeStop = Math.max(start, Math.min(stopTime, end > 0L ? end : stopTime));

        SharedPreferences.Editor editor = p.edit();
        addStudyTimeByDay(p, editor, start, safeStop);
        editor.putBoolean("focus_active", false)
                .remove("focus_start")
                .remove("focus_end")
                .remove("focus_weekend_slot")
                .commit();
        return true;
    }

    private static void addStudyTimeByDay(
            SharedPreferences p, SharedPreferences.Editor editor, long start, long stop) {
        if (stop <= start) return;
        long cursor = start;
        while (cursor < stop) {
            Calendar nextDay = Calendar.getInstance();
            nextDay.setTimeInMillis(cursor);
            nextDay.add(Calendar.DAY_OF_YEAR, 1);
            nextDay.set(Calendar.HOUR_OF_DAY, 0);
            nextDay.set(Calendar.MINUTE, 0);
            nextDay.set(Calendar.SECOND, 0);
            nextDay.set(Calendar.MILLISECOND, 0);
            long segmentEnd = Math.min(stop, nextDay.getTimeInMillis());
            String key = "study_ms_" + dateKey(cursor);
            editor.putLong(key, p.getLong(key, 0L) + (segmentEnd - cursor));
            cursor = segmentEnd;
        }
    }

    public static long focusEnd(Context context) {
        return prefs(context).getLong("focus_end", 0L);
    }

    public static long focusStart(Context context) {
        return prefs(context).getLong("focus_start", 0L);
    }

    public static String focusTask(Context context) {
        return normalizeTask(prefs(context).getString("focus_task", "简历"));
    }

    public static int focusWeekendSlot(Context context) {
        return prefs(context).getInt("focus_weekend_slot", NO_WEEKEND_SLOT);
    }

    public static void setSelectedTask(Context context, String task) {
        prefs(context).edit().putString("selected_task", normalizeTask(task)).apply();
    }

    public static String selectedTask(Context context) {
        return normalizeTask(prefs(context).getString("selected_task", "简历"));
    }

    private static String weekKey() {
        Calendar calendar = Calendar.getInstance();
        calendar.setFirstDayOfWeek(Calendar.MONDAY);
        calendar.setMinimalDaysInFirstWeek(4);
        return calendar.getWeekYear() + "-W" + calendar.get(Calendar.WEEK_OF_YEAR);
    }

    public static synchronized int getEarlyRemaining(Context context) {
        SharedPreferences p = prefs(context);
        String currentWeek = weekKey();
        if (!currentWeek.equals(p.getString("early_week", ""))) {
            p.edit().putString("early_week", currentWeek).putInt("early_used", 0).commit();
            return 2;
        }
        return Math.max(0, 2 - p.getInt("early_used", 0));
    }

    private static boolean consumeEarlyEndLocked(Context context) {
        SharedPreferences p = prefs(context);
        int remaining = getEarlyRemaining(context);
        if (remaining <= 0) return false;
        p.edit().putInt("early_used", 3 - remaining).commit();
        return true;
    }

    public static void setDurationLock(Context context, boolean locked, int hours, int minutes) {
        SharedPreferences.Editor editor = prefs(context).edit().putBoolean("duration_locked", locked);
        if (locked) {
            editor.putInt("locked_hours", clamp(hours, 0, 4));
            editor.putInt("locked_minutes", clamp(minutes, 0, 59));
        }
        editor.apply();
    }

    public static boolean durationLocked(Context context) {
        return prefs(context).getBoolean("duration_locked", false);
    }

    public static int lockedHours(Context context) {
        return clamp(prefs(context).getInt("locked_hours", 1), 0, 4);
    }

    public static int lockedMinutes(Context context) {
        return clamp(prefs(context).getInt("locked_minutes", 0), 0, 59);
    }

    public static synchronized void rememberDuration(Context context, int seconds) {
        int safe = clamp(seconds, 1, 4 * 3600 + 59 * 60);
        List<Integer> values = recentDurations(context);
        values.remove(Integer.valueOf(safe));
        values.add(0, safe);
        while (values.size() > 3) values.remove(values.size() - 1);
        prefs(context).edit().putString("recent_durations", joinInts(values)).apply();
    }

    public static List<Integer> recentDurations(Context context) {
        String stored = prefs(context).getString("recent_durations", "");
        if (stored == null || stored.trim().isEmpty()) return new ArrayList<>();
        Set<Integer> unique = new LinkedHashSet<>();
        for (String part : stored.split(",")) {
            try {
                int value = Integer.parseInt(part.trim());
                if (value > 0) unique.add(value);
            } catch (NumberFormatException ignored) {
                // Ignore a damaged entry and keep the remaining history.
            }
            if (unique.size() == 3) break;
        }
        return new ArrayList<>(unique);
    }

    private static String joinInts(List<Integer> values) {
        StringBuilder result = new StringBuilder();
        for (Integer value : values) {
            if (result.length() > 0) result.append(',');
            result.append(value);
        }
        return result.toString();
    }

    public static void setWeekendTask(Context context, int slot, String task) {
        if (slot < 0 || slot >= 3) return;
        prefs(context).edit().putString("weekend_task_" + slot, normalizeTask(task)).apply();
    }

    public static String weekendTask(Context context, int slot) {
        if (slot < 0 || slot >= 3) return "简历";
        return normalizeTask(prefs(context).getString(
                "weekend_task_" + slot, DEFAULT_WEEKEND_TASKS[slot]));
    }

    public static void setWeekendLocked(Context context, boolean locked) {
        prefs(context).edit().putBoolean("weekend_locked", locked).apply();
    }

    public static boolean weekendLocked(Context context) {
        return prefs(context).getBoolean("weekend_locked", false);
    }

    public static boolean isWeekendNow() {
        int day = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
        return day == Calendar.SATURDAY || day == Calendar.SUNDAY;
    }

    public static boolean wasWeekendInstanceSkipped(Context context, int slot) {
        return WeekendScheduler.currentInstanceId(slot).equals(
                prefs(context).getString("weekend_skip_instance", ""));
    }

    public static void setCalendarMark(
            Context context, int year, int monthOneBased, int day, boolean marked) {
        if (!validDate(year, monthOneBased, day)) return;
        prefs(context).edit().putBoolean(markKey(year, monthOneBased, day), marked).apply();
    }

    public static JSONObject calendarMonth(Context context, int year, int monthOneBased)
            throws JSONException {
        JSONObject root = new JSONObject();
        JSONObject minutes = new JSONObject();
        JSONArray marks = new JSONArray();
        Calendar calendar = Calendar.getInstance();
        calendar.setLenient(false);
        calendar.clear();
        calendar.set(year, monthOneBased - 1, 1);
        int days = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        SharedPreferences p = prefs(context);
        for (int day = 1; day <= days; day++) {
            String date = String.format(Locale.US, "%04d-%02d-%02d", year, monthOneBased, day);
            long millis = p.getLong("study_ms_" + date, 0L);
            minutes.put(String.valueOf(day), millis / 60_000L);
            if (p.getBoolean(markKey(year, monthOneBased, day), false)) marks.put(day);
        }
        root.put("minutes", minutes);
        root.put("marks", marks);
        return root;
    }

    public static JSONObject stateJson(Context context) throws JSONException {
        JSONObject root = new JSONObject();
        boolean active = isActive(context);
        root.put("active", active);
        root.put("focusEnd", active ? focusEnd(context) : 0L);
        root.put("focusStart", active ? focusStart(context) : 0L);
        root.put("focusTask", focusTask(context));
        root.put("selectedTask", selectedTask(context));
        root.put("earlyRemaining", getEarlyRemaining(context));
        root.put("durationLocked", durationLocked(context));
        root.put("lockedHours", lockedHours(context));
        root.put("lockedMinutes", lockedMinutes(context));
        root.put("weekendLocked", weekendLocked(context));
        root.put("isWeekend", isWeekendNow());
        JSONArray tasks = new JSONArray();
        for (int i = 0; i < 3; i++) tasks.put(weekendTask(context, i));
        root.put("weekendTasks", tasks);
        return root;
    }

    private static boolean validDate(int year, int monthOneBased, int day) {
        if (year < 1970 || year > 2200 || monthOneBased < 1 || monthOneBased > 12 || day < 1) {
            return false;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setLenient(false);
        calendar.clear();
        calendar.set(year, monthOneBased - 1, day);
        try {
            calendar.getTime();
            return true;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    private static String markKey(int year, int monthOneBased, int day) {
        return "mark_" + String.format(Locale.US, "%04d-%02d-%02d", year, monthOneBased, day);
    }

    private static String dateKey(long timeMillis) {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date(timeMillis));
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
