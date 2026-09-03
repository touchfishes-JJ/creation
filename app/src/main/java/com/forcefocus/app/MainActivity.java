package com.forcefocus.app;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.Toast;

import java.util.Calendar;

public class MainActivity extends Activity {
    private final int GREEN = Color.parseColor("#8AA832");
    private final int CREAM = Color.parseColor("#FFFBD3");
    private final int BROWN = Color.parseColor("#331915");

    private FrameLayout root;
    private FrameLayout focusPage;
    private FrameLayout calendarPage;
    private FocusOrbView orb;
    private FrameLayout orbZone;
    private Button btnResume, btnJob, btnExam, btnAudio;
    private ValueBoxView hourBox, minuteBox;
    private ValueBoxView selectedBox;
    private Mode selectedMode = Mode.RESUME;
    private GridLayout dayGrid;
    private GestureDetector gestureDetector;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable tick = new Runnable() { public void run() { refreshOrb(); handler.postDelayed(this, 1000); } };

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        gestureDetector = new GestureDetector(this, new SwipeListener());
        build();
        requestBasics();
        Scheduler.scheduleNext14Days(this);
        handler.post(tick);
    }

    @Override protected void onResume() {
        super.onResume();
        refreshOrb();
        renderCalendar();
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(tick);
    }

    @Override public boolean dispatchTouchEvent(MotionEvent ev) {
        gestureDetector.onTouchEvent(ev);
        return super.dispatchTouchEvent(ev);
    }

    private void build() {
        root = new FrameLayout(this);
        root.setBackgroundColor(CREAM);
        setContentView(root);
        buildFocusPage();
        buildCalendarPage();
        showFocus();
    }

    private void buildFocusPage() {
        focusPage = new FrameLayout(this);
        focusPage.setBackgroundColor(CREAM);
        root.addView(focusPage, full());

        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);
        FrameLayout.LayoutParams topLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        topLp.gravity = Gravity.TOP;
        topLp.setMargins(dp(18), dp(18), dp(18), 0);
        focusPage.addView(topRow, topLp);

        View accessFrame = framedIcon(IconButtonView.ACCESS, true);
        accessFrame.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        topRow.addView(accessFrame, new LinearLayout.LayoutParams(dp(64), dp(64)));

        Space spacer = new Space(this);
        topRow.addView(spacer, new LinearLayout.LayoutParams(0, 1, 1));

        View alarmFrame = framedIcon(IconButtonView.ALARM, false);
        alarmFrame.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= 31) {
                try {
                    startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + getPackageName())));
                } catch (Exception e) {
                    startActivity(new Intent(Settings.ACTION_SETTINGS));
                }
            }
        });
        topRow.addView(alarmFrame, new LinearLayout.LayoutParams(dp(64), dp(64)));

        orbZone = new FrameLayout(this);
        FrameLayout.LayoutParams zoneLp = new FrameLayout.LayoutParams(dp(330), dp(330));
        zoneLp.gravity = Gravity.CENTER;
        zoneLp.setMargins(0, 0, 0, dp(24));
        focusPage.addView(orbZone, zoneLp);

        orb = new FocusOrbView(this);
        orb.setFill(GREEN);
        orb.setCenterTextColor(CREAM);
        orb.setLongPressListener(this::toggleModes);
        FrameLayout.LayoutParams orbLp = new FrameLayout.LayoutParams(dp(230), dp(230));
        orbLp.gravity = Gravity.CENTER;
        orbZone.addView(orb, orbLp);

        btnResume = modeBtn("简历");
        btnJob = modeBtn("岗位调研");
        btnExam = modeBtn("考公");
        btnAudio = modeBtn("磨耳朵");
        placeMode(btnResume, Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, dp(12), 0, 0, Mode.RESUME);
        placeMode(btnAudio, Gravity.CENTER_VERTICAL | Gravity.START, dp(10), 0, 0, 0, Mode.AUDIO);
        placeMode(btnJob, Gravity.CENTER_VERTICAL | Gravity.END, 0, 0, dp(10), 0, Mode.JOB);
        placeMode(btnExam, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0, 0, dp(12), Mode.EXAM);
        hideModes();

        LinearLayout bottom = new LinearLayout(this);
        bottom.setOrientation(LinearLayout.HORIZONTAL);
        bottom.setGravity(Gravity.BOTTOM | Gravity.CENTER_VERTICAL);
        bottom.setPadding(dp(18), 0, dp(18), dp(28));
        FrameLayout.LayoutParams bottomLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bottomLp.gravity = Gravity.BOTTOM;
        focusPage.addView(bottom, bottomLp);

        hourBox = new ValueBoxView(this);
        hourBox.setRange(0, 4);
        hourBox.setValue(0);
        hourBox.setUnit("h");
        minuteBox = new ValueBoxView(this);
        minuteBox.setRange(0, 59);
        minuteBox.setValue(25);
        minuteBox.setUnit("min");
        ValueBoxView.Listener valueListener = new ValueBoxView.Listener() {
            @Override public void onSelected(ValueBoxView view) { selectBox(view); }
            @Override public void onValueChanged(ValueBoxView view, int value) { refreshOrb(); }
        };
        hourBox.setListener(valueListener);
        minuteBox.setListener(valueListener);
        selectBox(hourBox);

        LinearLayout timeWrap = new LinearLayout(this);
        timeWrap.setOrientation(LinearLayout.HORIZONTAL);
        timeWrap.setGravity(Gravity.CENTER_VERTICAL);
        GradientDrawableCompat.panel(timeWrap, CREAM, CREAM, dp(28), BROWN);
        timeWrap.setPadding(dp(10), dp(10), dp(10), dp(10));
        timeWrap.addView(hourBox, new LinearLayout.LayoutParams(dp(86), dp(84)));
        LinearLayout.LayoutParams minLp = new LinearLayout.LayoutParams(dp(86), dp(84));
        minLp.setMargins(dp(8), 0, 0, 0);
        timeWrap.addView(minuteBox, minLp);
        bottom.addView(timeWrap, new LinearLayout.LayoutParams(0, dp(104), 1f));

        Button start = new Button(this);
        start.setAllCaps(false);
        start.setText("开始");
        start.setTextColor(CREAM);
        start.setTextSize(24);
        start.setTypeface(Typeface.create(Typeface.SERIF, Typeface.BOLD));
        GradientDrawableCompat.bg(start, BROWN, dp(28));
        start.setOnClickListener(v -> startFocus());
        LinearLayout.LayoutParams startLp = new LinearLayout.LayoutParams(0, dp(104), 0.55f);
        startLp.setMargins(dp(12), 0, 0, 0);
        bottom.addView(start, startLp);
    }

    private void buildCalendarPage() {
        calendarPage = new FrameLayout(this);
        calendarPage.setBackgroundColor(CREAM);
        root.addView(calendarPage, full());

        LinearLayout center = new LinearLayout(this);
        center.setOrientation(LinearLayout.VERTICAL);
        center.setGravity(Gravity.CENTER);
        calendarPage.addView(center, full());

        dayGrid = new GridLayout(this);
        dayGrid.setColumnCount(7);
        dayGrid.setRowCount(6);
        center.addView(dayGrid, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        renderCalendar();
    }

    private View framedIcon(int iconType, boolean purpleIcon) {
        FrameLayout frame = new FrameLayout(this);
        GradientDrawableCompat.panel(frame, CREAM, CREAM, dp(18), BROWN);
        frame.setPadding(dp(10), dp(10), dp(10), dp(10));
        IconButtonView icon = new IconButtonView(this, iconType);
        icon.setColors(purpleIcon ? Color.parseColor("#7350E6") : BROWN, 0);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(dp(34), dp(34));
        lp.gravity = Gravity.CENTER;
        frame.addView(icon, lp);
        return frame;
    }

    private Button modeBtn(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(14);
        b.setTextColor(CREAM);
        b.setTypeface(Typeface.create(Typeface.SERIF, Typeface.BOLD));
        GradientDrawableCompat.bg(b, BROWN, dp(24));
        return b;
    }

    private void placeMode(Button b, int gravity, int left, int top, int right, int bottom, Mode mode) {
        b.setVisibility(View.GONE);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(dp(96), dp(48));
        lp.gravity = gravity;
        lp.setMargins(left, top, right, bottom);
        orbZone.addView(b, lp);
        b.setOnClickListener(v -> {
            selectedMode = mode;
            hideModes();
            Toast.makeText(this, mode.title, Toast.LENGTH_SHORT).show();
        });
    }

    private void toggleModes() {
        int visible = btnResume.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE;
        btnResume.setVisibility(visible);
        btnJob.setVisibility(visible);
        btnExam.setVisibility(visible);
        btnAudio.setVisibility(visible);
    }

    private void hideModes() {
        btnResume.setVisibility(View.GONE);
        btnJob.setVisibility(View.GONE);
        btnExam.setVisibility(View.GONE);
        btnAudio.setVisibility(View.GONE);
    }

    private void selectBox(ValueBoxView box) {
        selectedBox = box;
        hourBox.setSelectedState(box == hourBox);
        minuteBox.setSelectedState(box == minuteBox);
    }

    private void startFocus() {
        int mins = hourBox.getValue() * 60 + minuteBox.getValue();
        if (mins <= 0) mins = 25;
        LockState.startManual(this, selectedMode, mins);
        hideModes();
        refreshOrb();
    }

    private void refreshOrb() {
        if (orb == null) return;
        LockState.Session s = LockState.current(this);
        if (s == null) orb.setCenterText("");
        else orb.setCenterText(LockState.timeLeftShort(s.end));
        if (dayGrid != null && calendarPage.getVisibility() == View.VISIBLE) renderCalendar();
    }

    private void renderCalendar() {
        if (dayGrid == null) return;
        dayGrid.removeAllViews();
        Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR), month = c.get(Calendar.MONTH);
        c.set(year, month, 1);
        int firstDow = c.get(Calendar.DAY_OF_WEEK);
        int blanks = (firstDow + 5) % 7;
        int max = c.getActualMaximum(Calendar.DAY_OF_MONTH);
        int cell = dp(38);

        for (int i = 0; i < blanks; i++) addBlank(cell);
        for (int day = 1; day <= max; day++) {
            long ms = StudyStats.millisFor(this, year, month, day);
            float fraction = Math.min(1f, ms / (8f * 60f * 60f * 1000f));
            boolean marked = StudyStats.marked(this, year, month, day);
            CalendarDayView view = new CalendarDayView(this);
            view.setData(day, fraction, marked);
            final int d = day;
            view.setOnClickListener(v -> { StudyStats.toggleMark(this, year, month, d); renderCalendar(); });
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = cell; lp.height = cell;
            lp.setMargins(dp(4), dp(4), dp(4), dp(4));
            dayGrid.addView(view, lp);
        }
        int cells = blanks + max;
        while (cells < 42) { addBlank(cell); cells++; }
    }

    private void addBlank(int cell) {
        View v = new View(this);
        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
        lp.width = cell; lp.height = cell;
        lp.setMargins(dp(4), dp(4), dp(4), dp(4));
        dayGrid.addView(v, lp);
    }

    private void showFocus() {
        focusPage.setVisibility(View.VISIBLE);
        calendarPage.setVisibility(View.GONE);
    }

    private void showCalendar() {
        focusPage.setVisibility(View.GONE);
        calendarPage.setVisibility(View.VISIBLE);
        renderCalendar();
    }

    private FrameLayout.LayoutParams full() {
        return new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    }

    private int dp(float x) { return (int) (x * getResources().getDisplayMetrics().density); }

    private void requestBasics() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 5);
        }
    }

    private final class SwipeListener extends GestureDetector.SimpleOnGestureListener {
        @Override public boolean onDown(MotionEvent e) { return false; }
        @Override public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (e1 == null || e2 == null) return false;
            float dx = e2.getX() - e1.getX();
            float dy = e2.getY() - e1.getY();
            if (Math.abs(dx) < dp(70) || Math.abs(dx) < Math.abs(dy)) return false;
            if (dx < 0) showCalendar(); else showFocus();
            return true;
        }
    }
}
