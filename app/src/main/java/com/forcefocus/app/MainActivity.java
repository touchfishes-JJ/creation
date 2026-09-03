package com.forcefocus.app;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends Activity {
    private final int GREEN = Color.parseColor("#8AA832");
    private final int CREAM = Color.parseColor("#FFFBD3");
    private final int BROWN = Color.parseColor("#331915");
    private final int PALE = Color.parseColor("#F4F0C7");
    private final int LIGHT_GREEN = Color.parseColor("#DDE9B5");

    private static final int PAGE_FOCUS = 0;
    private static final int PAGE_CALENDAR = 1;
    private static final int SUB_REGULAR = 0;
    private static final int SUB_WEEKEND = 1;

    private FrameLayout root;
    private FrameLayout focusPage;
    private FrameLayout calendarPage;
    private LinearLayout regularPanel;
    private LinearLayout weekendPanel;
    private FocusOrbView orb;
    private FrameLayout orbZone;
    private Button btnResume, btnJob, btnExam, btnAudio;
    private LinearLayout regularAppsRow;
    private ValueBoxView hourBox, minuteBox;
    private View presetLockButton, memoryButton;
    private LinearLayout memoryPopup;
    private View regularTreeTab, weekendForestTab;
    private View weekendLockButton;
    private GridLayout dayGrid;
    private TextView monthLabel;
    private View runningOverlay;
    private FocusOrbView runningOrb;
    private View drawerOverlay;
    private LinearLayout drawerPanel;
    private View sheetOverlay;
    private LinearLayout sheetCard;

    private int currentPage = PAGE_FOCUS;
    private int currentSub = SUB_REGULAR;
    private Mode selectedRegularMode = Mode.RESUME;
    private final LinearLayout[] weekendAppRows = new LinearLayout[3];
    private final TextView[] weekendTimeViews = new TextView[3];
    private final View[] weekendWorkButtons = new View[3];
    private final View[] weekendWhitelistButtons = new View[3];

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            refreshUi();
            handler.postDelayed(this, 1000);
        }
    };
    private GestureDetector gestureDetector;
    private float downX;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        gestureDetector = new GestureDetector(this, new SwipeListener());
        build();
        selectedRegularMode = loadRegularMode();
        loadLockedPreset();
        autoChooseSubMode();
        refreshUi();
        requestBasics();
        Scheduler.scheduleNext14Days(this);
        handler.post(tick);
    }

    @Override protected void onResume() {
        super.onResume();
        autoChooseSubMode();
        refreshUi();
        renderCalendar();
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(tick);
    }

    @Override public boolean dispatchTouchEvent(MotionEvent ev) {
        gestureDetector.onTouchEvent(ev);
        if (ev.getAction() == MotionEvent.ACTION_DOWN) downX = ev.getX();
        if (ev.getAction() == MotionEvent.ACTION_UP) {
            if (downX < dp(18) && ev.getX() - downX > dp(42) && runningOverlay.getVisibility() != View.VISIBLE) {
                openDrawer();
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    private void build() {
        root = new FrameLayout(this);
        root.setBackgroundColor(CREAM);
        setContentView(root);
        buildFocusPage();
        buildCalendarPage();
        buildEdgeTrigger();
        buildDrawer();
        buildSheet();
        showPage(PAGE_FOCUS);
    }

    private void buildFocusPage() {
        focusPage = new FrameLayout(this);
        focusPage.setBackgroundColor(CREAM);
        root.addView(focusPage, full());

        LinearLayout topTabs = new LinearLayout(this);
        topTabs.setOrientation(LinearLayout.HORIZONTAL);
        topTabs.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams topLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        topLp.gravity = Gravity.TOP;
        topLp.setMargins(0, dp(94), 0, 0);
        focusPage.addView(topTabs, topLp);

        regularTreeTab = modeTabImage(R.drawable.tree_mode, v -> switchSubMode(SUB_REGULAR));
        weekendForestTab = modeTabImage(R.drawable.forest_mode, v -> switchSubMode(SUB_WEEKEND));
        topTabs.addView(regularTreeTab, new LinearLayout.LayoutParams(dp(84), dp(84)));
        Space mid = new Space(this);
        topTabs.addView(mid, new LinearLayout.LayoutParams(dp(28), 1));
        topTabs.addView(weekendForestTab, new LinearLayout.LayoutParams(dp(84), dp(84)));

        regularPanel = new LinearLayout(this);
        regularPanel.setOrientation(LinearLayout.VERTICAL);
        regularPanel.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        FrameLayout.LayoutParams regLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        regLp.topMargin = dp(170);
        focusPage.addView(regularPanel, regLp);

        orbZone = new FrameLayout(this);
        LinearLayout.LayoutParams zoneLp = new LinearLayout.LayoutParams(dp(312), dp(312));
        regularPanel.addView(orbZone, zoneLp);

        orb = new FocusOrbView(this);
        orb.setFill(GREEN);
        orb.setCenterTextColor(CREAM);
        orb.setOnClickListener(v -> toggleRegularChoices());
        orb.setLongPressListener(this::toggleRegularChoices);
        FrameLayout.LayoutParams orbLp = new FrameLayout.LayoutParams(dp(224), dp(224));
        orbLp.gravity = Gravity.CENTER;
        orbZone.addView(orb, orbLp);

        btnResume = radialButton("简历", Mode.RESUME);
        btnExam = radialButton("考公", Mode.EXAM);
        btnJob = radialButton("岗位调研", Mode.JOB);
        btnAudio = radialButton("磨耳朵", Mode.AUDIO);
        addAroundOrb(btnResume, Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, dp(0), 0, 0);
        addAroundOrb(btnExam, Gravity.CENTER_VERTICAL | Gravity.START, dp(0), 0, 0, 0);
        addAroundOrb(btnJob, Gravity.CENTER_VERTICAL | Gravity.END, 0, 0, dp(0), 0);
        addAroundOrb(btnAudio, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0, 0, dp(0));
        hideRegularChoices();

        regularAppsRow = new LinearLayout(this);
        regularAppsRow.setOrientation(LinearLayout.HORIZONTAL);
        regularAppsRow.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams appLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(38));
        appLp.topMargin = dp(8);
        regularPanel.addView(regularAppsRow, appLp);

        LinearLayout filler = new LinearLayout(this);
        regularPanel.addView(filler, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout bottomWrap = new LinearLayout(this);
        bottomWrap.setOrientation(LinearLayout.VERTICAL);
        bottomWrap.setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout.LayoutParams bwLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bwLp.bottomMargin = dp(56);
        regularPanel.addView(bottomWrap, bwLp);

        LinearLayout timeRow = new LinearLayout(this);
        timeRow.setOrientation(LinearLayout.HORIZONTAL);
        timeRow.setGravity(Gravity.CENTER_VERTICAL);
        bottomWrap.addView(timeRow, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        hourBox = new ValueBoxView(this);
        hourBox.setRange(0, 4); hourBox.setUnit("h"); hourBox.setValue(0);
        minuteBox = new ValueBoxView(this);
        minuteBox.setRange(0, 59); minuteBox.setUnit("min"); minuteBox.setValue(25);
        ValueBoxView.Listener l = new ValueBoxView.Listener() {
            @Override public void onSelected(ValueBoxView view) {
                hourBox.setSelectedState(view == hourBox);
                minuteBox.setSelectedState(view == minuteBox);
            }
            @Override public void onValueChanged(ValueBoxView view, int value) {
                saveLockedPresetIfNeeded();
            }
        };
        hourBox.setListener(l);
        minuteBox.setListener(l);
        hourBox.setSelectedState(true);

        LinearLayout timeWrap = new LinearLayout(this);
        timeWrap.setOrientation(LinearLayout.HORIZONTAL);
        timeWrap.setPadding(dp(10), dp(10), dp(10), dp(10));
        GradientDrawableCompat.panel(timeWrap, CREAM, CREAM, dp(28), GREEN);
        timeWrap.addView(hourBox, new LinearLayout.LayoutParams(dp(86), dp(88)));
        LinearLayout.LayoutParams minLp = new LinearLayout.LayoutParams(dp(86), dp(88));
        minLp.leftMargin = dp(8);
        timeWrap.addView(minuteBox, minLp);
        LinearLayout.LayoutParams twLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        timeRow.addView(timeWrap, twLp);

        View startBtn = iconCircleButton(MiniGlyphView.PLAY, GREEN, CREAM, v -> startRegularFocus());
        LinearLayout.LayoutParams sbLp = new LinearLayout.LayoutParams(dp(110), dp(110));
        sbLp.leftMargin = dp(12);
        timeRow.addView(startBtn, sbLp);

        LinearLayout iconRow = new LinearLayout(this);
        iconRow.setOrientation(LinearLayout.HORIZONTAL);
        iconRow.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams irLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        irLp.topMargin = dp(8);
        bottomWrap.addView(iconRow, irLp);

        presetLockButton = plainTinyIcon(MiniGlyphView.LOCK, v -> togglePresetLock());
        memoryButton = plainTinyIcon(MiniGlyphView.MEMORY, v -> toggleMemoryPopup());
        iconRow.addView(presetLockButton, new LinearLayout.LayoutParams(dp(28), dp(28)));
        LinearLayout.LayoutParams mbLp = new LinearLayout.LayoutParams(dp(28), dp(28)); mbLp.leftMargin = dp(18);
        iconRow.addView(memoryButton, mbLp);

        memoryPopup = new LinearLayout(this);
        memoryPopup.setOrientation(LinearLayout.HORIZONTAL);
        memoryPopup.setGravity(Gravity.CENTER);
        memoryPopup.setVisibility(View.GONE);
        LinearLayout.LayoutParams mpLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        mpLp.topMargin = dp(8);
        bottomWrap.addView(memoryPopup, mpLp);

        weekendPanel = new LinearLayout(this);
        weekendPanel.setOrientation(LinearLayout.VERTICAL);
        weekendPanel.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        FrameLayout.LayoutParams weLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        weLp.topMargin = dp(178);
        focusPage.addView(weekendPanel, weLp);

        LinearLayout wTop = new LinearLayout(this);
        wTop.setOrientation(LinearLayout.HORIZONTAL);
        wTop.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams wTopLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        wTopLp.leftMargin = dp(20); wTopLp.rightMargin = dp(20);
        weekendPanel.addView(wTop, wTopLp);
        weekendLockButton = plainTinyIcon(MiniGlyphView.LOCK, v -> toggleWeekendLock());
        wTop.addView(weekendLockButton, new LinearLayout.LayoutParams(dp(30), dp(30)));

        for (int i = 0; i < 3; i++) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.HORIZONTAL);
            card.setGravity(Gravity.CENTER_VERTICAL);
            card.setPadding(dp(18), dp(18), dp(14), dp(18));
            GradientDrawableCompat.panel(card, CREAM, PALE, dp(26), GREEN);
            LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(104));
            cardLp.leftMargin = dp(18); cardLp.rightMargin = dp(18); cardLp.topMargin = dp(16);
            weekendPanel.addView(card, cardLp);

            TextView time = t(LockState.slotStart(i)+"  "+LockState.slotEnd(i), 22, BROWN, true);
            time.setGravity(Gravity.CENTER_VERTICAL);
            weekendTimeViews[i] = time;
            card.addView(time, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));

            final int idx = i;
            View work = weekendActionIcon(modeGlyph(LockState.weekendMode(this, i)), v -> openWeekendModePicker(idx));
            weekendWorkButtons[i] = work;
            card.addView(work, new LinearLayout.LayoutParams(dp(48), dp(48)));

            View white = weekendActionIcon(MiniGlyphView.WHITELIST, v -> openWhitelistForWeekend(idx));
            weekendWhitelistButtons[i] = white;
            LinearLayout.LayoutParams wlp = new LinearLayout.LayoutParams(dp(48), dp(48));
            wlp.leftMargin = dp(10);
            card.addView(white, wlp);

            LinearLayout apps = new LinearLayout(this);
            apps.setOrientation(LinearLayout.HORIZONTAL);
            apps.setVisibility(View.GONE);
            weekendAppRows[i] = apps;
        }

        runningOverlay = new FrameLayout(this);
        runningOverlay.setBackgroundColor(CREAM);
        runningOverlay.setVisibility(View.GONE);
        focusPage.addView(runningOverlay, full());
        runningOrb = new FocusOrbView(this);
        runningOrb.setFill(GREEN);
        runningOrb.setCenterTextColor(CREAM);
        FrameLayout.LayoutParams roLp = new FrameLayout.LayoutParams(dp(224), dp(224));
        roLp.gravity = Gravity.CENTER;
        ((FrameLayout) runningOverlay).addView(runningOrb, roLp);
    }

    private void buildEdgeTrigger() {
        View edge = new View(this);
        edge.setBackgroundColor(Color.TRANSPARENT);
        final float[] start = new float[1];
        edge.setOnTouchListener((v, e) -> {
            if (e.getAction() == MotionEvent.ACTION_DOWN) {
                start[0] = e.getX();
                return true;
            }
            if (e.getAction() == MotionEvent.ACTION_UP) {
                if (e.getX() - start[0] > dp(28) && runningOverlay.getVisibility() != View.VISIBLE) {
                    openDrawer();
                }
                return true;
            }
            return true;
        });
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(dp(32), ViewGroup.LayoutParams.MATCH_PARENT);
        lp.gravity = Gravity.START;
        root.addView(edge, lp);
    }

    private void buildCalendarPage() {
        calendarPage = new FrameLayout(this);
        calendarPage.setBackgroundColor(CREAM);
        root.addView(calendarPage, full());

        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.VERTICAL);
        wrap.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);
        calendarPage.addView(wrap, full());

        monthLabel = t("", 20, BROWN, true);
        monthLabel.setGravity(Gravity.CENTER);
        wrap.addView(monthLabel);

        LinearLayout week = new LinearLayout(this);
        week.setOrientation(LinearLayout.HORIZONTAL);
        week.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams wlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        wlp.topMargin = dp(8);
        wrap.addView(week, wlp);
        String[] ds = {"一","二","三","四","五","六","日"};
        for (String d : ds) {
            TextView tv = t(d, 12, BROWN, false);
            tv.setGravity(Gravity.CENTER);
            week.addView(tv, new LinearLayout.LayoutParams(dp(48), ViewGroup.LayoutParams.WRAP_CONTENT));
        }

        dayGrid = new GridLayout(this);
        dayGrid.setColumnCount(7);
        dayGrid.setRowCount(6);
        LinearLayout.LayoutParams glp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        glp.topMargin = dp(8);
        wrap.addView(dayGrid, glp);
        renderCalendar();
    }

    private void buildDrawer() {
        drawerOverlay = new FrameLayout(this);
        drawerOverlay.setBackgroundColor(Color.parseColor("#66000000"));
        drawerOverlay.setVisibility(View.GONE);
        root.addView(drawerOverlay, full());
        drawerOverlay.setOnClickListener(v -> closeDrawer());

        drawerPanel = new LinearLayout(this);
        drawerPanel.setOrientation(LinearLayout.VERTICAL);
        drawerPanel.setPadding(dp(18), dp(72), dp(18), dp(18));
        drawerPanel.setBackgroundColor(CREAM);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams((int)(getResources().getDisplayMetrics().widthPixels*0.42f), ViewGroup.LayoutParams.MATCH_PARENT);
        lp.gravity = Gravity.START;
        ((FrameLayout) drawerOverlay).addView(drawerPanel, lp);
        drawerPanel.setOnClickListener(v -> {});

        Button setting = drawerTextButton("设置", v -> { closeDrawer(); openSettingsSheet(); });
        Button whitelist = drawerTextButton("白名单", v -> { closeDrawer(); openWhitelistSheet(); });
        drawerPanel.addView(setting, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(54)));
        LinearLayout.LayoutParams wlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(54)); wlp.topMargin = dp(10);
        drawerPanel.addView(whitelist, wlp);
    }

    private void buildSheet() {
        sheetOverlay = new FrameLayout(this);
        sheetOverlay.setBackgroundColor(Color.parseColor("#66000000"));
        sheetOverlay.setVisibility(View.GONE);
        root.addView(sheetOverlay, full());
        sheetOverlay.setOnClickListener(v -> closeSheet());

        sheetCard = new LinearLayout(this);
        sheetCard.setOrientation(LinearLayout.VERTICAL);
        sheetCard.setPadding(dp(18), dp(18), dp(18), dp(18));
        GradientDrawableCompat.panel(sheetCard, CREAM, PALE, dp(28), GREEN);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams((int)(getResources().getDisplayMetrics().widthPixels*0.78f), ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.CENTER;
        ((FrameLayout) sheetOverlay).addView(sheetCard, lp);
        sheetCard.setOnClickListener(v -> {});
    }

    private View modeTabImage(int drawable, View.OnClickListener click) {
        FrameLayout box = new FrameLayout(this);
        box.setOnClickListener(click);
        box.setPadding(dp(2), dp(2), dp(2), dp(2));
        ImageView iv = new ImageView(this);
        iv.setImageResource(drawable);
        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        lp.gravity = Gravity.CENTER;
        box.addView(iv, lp);
        return box;
    }

    private Button radialButton(String text, Mode mode) {
        Button b = new Button(this);
        b.setAllCaps(false); b.setText(text); b.setTextSize(14); b.setTypeface(Typeface.create(Typeface.SERIF, Typeface.BOLD));
        GradientDrawableCompat.bg(b, BROWN, dp(20));
        b.setTextColor(CREAM);
        b.setOnClickListener(v -> { selectedRegularMode = mode; prefs().edit().putString("regular_mode", mode.name()).apply(); hideRegularChoices(); refreshUi(); });
        return b;
    }

    private void addAroundOrb(Button b, int gravity, int l, int t, int r, int bo) {
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(dp(96), dp(40));
        lp.gravity = gravity;
        lp.setMargins(l, t, r, bo);
        orbZone.addView(b, lp);
    }

    private View weekendActionIcon(int glyph, View.OnClickListener click) {
        FrameLayout box = new FrameLayout(this);
        GradientDrawableCompat.bg(box, LIGHT_GREEN, dp(18));
        MiniGlyphView icon = new MiniGlyphView(this, glyph);
        icon.setTint(BROWN);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(dp(24), dp(24));
        lp.gravity = Gravity.CENTER;
        box.addView(icon, lp);
        box.setOnClickListener(click);
        return box;
    }

    private int modeGlyph(Mode mode) {
        if (mode == Mode.RESUME) return MiniGlyphView.MODE_RESUME;
        if (mode == Mode.JOB) return MiniGlyphView.MODE_JOB;
        if (mode == Mode.EXAM) return MiniGlyphView.MODE_EXAM;
        return MiniGlyphView.MODE_AUDIO;
    }

    private void replaceGlyphInFrame(View view, int glyph) {
        if (!(view instanceof FrameLayout)) return;
        FrameLayout frame = (FrameLayout) view;
        frame.removeAllViews();
        MiniGlyphView icon = new MiniGlyphView(this, glyph);
        icon.setTint(BROWN);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(dp(24), dp(24));
        lp.gravity = Gravity.CENTER;
        frame.addView(icon, lp);
    }

    private void openWeekendModePicker(int idx) {
        sheetCard.removeAllViews();
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        sheetCard.addView(row);
        addModePick(row, idx, Mode.RESUME, "简历");
        addModePick(row, idx, Mode.JOB, "岗位");
        addModePick(row, idx, Mode.EXAM, "考公");
        addModePick(row, idx, Mode.AUDIO, "磨耳");
        sheetOverlay.setVisibility(View.VISIBLE);
    }

    private void addModePick(LinearLayout row, int idx, Mode mode, String label) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        MiniGlyphView glyph = new MiniGlyphView(this, modeGlyph(mode));
        glyph.setTint(BROWN);
        box.addView(glyph, new LinearLayout.LayoutParams(dp(30), dp(30)));
        TextView text = t(label, 12, BROWN, false);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tlp.topMargin = dp(4);
        box.addView(text, tlp);
        box.setOnClickListener(v -> {
            LockState.setWeekendMode(this, idx, mode);
            closeSheet();
            refreshWeekendCards();
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(64), dp(64));
        if (row.getChildCount() > 0) lp.leftMargin = dp(6);
        row.addView(box, lp);
    }

    private void openWhitelistForWeekend(int idx) {
        sheetCard.removeAllViews();
        TextView title = t("白名单", 18, BROWN, true);
        sheetCard.addView(title);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rlp.topMargin = dp(12);
        sheetCard.addView(row, rlp);
        addAppIconsToRow(row, LockState.weekendMode(this, idx), false);
        sheetOverlay.setVisibility(View.VISIBLE);
    }

    private Button smallChoice(String text, View.OnClickListener click) {
        Button b = new Button(this);
        b.setAllCaps(false); b.setText(text); b.setTextSize(12); b.setTypeface(Typeface.create(Typeface.SERIF, Typeface.BOLD));
        b.setPadding(dp(10), 0, dp(10), 0);
        b.setOnClickListener(click);
        return b;
    }

    private View iconCircleButton(int type, int bg, int fg, View.OnClickListener click) {
        FrameLayout f = new FrameLayout(this);
        GradientDrawableCompat.bg(f, bg, dp(28));
        MiniGlyphView icon = new MiniGlyphView(this, type);
        icon.setTint(fg);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(dp(34), dp(34));
        lp.gravity = Gravity.CENTER;
        f.addView(icon, lp);
        f.setOnClickListener(click);
        return f;
    }

    private View plainTinyIcon(int type, View.OnClickListener click) {
        MiniGlyphView icon = new MiniGlyphView(this, type);
        icon.setTint(BROWN);
        icon.setOnClickListener(click);
        return icon;
    }

    private Button drawerTextButton(String text, View.OnClickListener click) {
        Button b = new Button(this);
        b.setAllCaps(false); b.setText(text); b.setTextSize(18); b.setTypeface(Typeface.create(Typeface.SERIF, Typeface.BOLD));
        GradientDrawableCompat.bg(b, PALE, dp(16));
        b.setTextColor(BROWN); b.setOnClickListener(click);
        return b;
    }

    private void switchSubMode(int sub) {
        currentSub = sub;
        regularPanel.setVisibility(sub == SUB_REGULAR ? View.VISIBLE : View.GONE);
        weekendPanel.setVisibility(sub == SUB_WEEKEND ? View.VISIBLE : View.GONE);
        refreshUi();
    }

    private void autoChooseSubMode() {
        boolean locked = prefs().getBoolean("weekend_ui_lock", false);
        if (locked) { switchSubMode(SUB_WEEKEND); return; }
        Calendar c = Calendar.getInstance();
        int dow = c.get(Calendar.DAY_OF_WEEK);
        switchSubMode((dow == Calendar.SATURDAY || dow == Calendar.SUNDAY) ? SUB_WEEKEND : SUB_REGULAR);
    }

    private void toggleRegularChoices() {
        int v = btnResume.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE;
        btnResume.setVisibility(v); btnExam.setVisibility(v); btnJob.setVisibility(v); btnAudio.setVisibility(v);
    }
    private void hideRegularChoices() { btnResume.setVisibility(View.GONE); btnExam.setVisibility(View.GONE); btnJob.setVisibility(View.GONE); btnAudio.setVisibility(View.GONE); }

    private void startRegularFocus() {
        int mins = hourBox.getValue()*60 + minuteBox.getValue();
        if (mins <= 0) mins = 25;
        saveLockedPresetIfNeeded();
        saveMemoryPreset(hourBox.getValue(), minuteBox.getValue());
        if (!isAccessibilityEnabled()) startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
        LockState.startManual(this, selectedRegularMode, mins);
        refreshUi();
    }

    private void chooseWeekendMode(int idx, Mode mode) {
        LockState.setWeekendMode(this, idx, mode);
        refreshUi();
    }

    private void renderCalendar() {
        if (dayGrid == null) return;
        dayGrid.removeAllViews();
        Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR), month = c.get(Calendar.MONTH);
        monthLabel.setText(String.format(Locale.CHINA, "%d 年 %02d 月", year, month + 1));
        c.set(year, month, 1);
        int firstDow = c.get(Calendar.DAY_OF_WEEK);
        int blanks = (firstDow + 5) % 7;
        int max = c.getActualMaximum(Calendar.DAY_OF_MONTH);
        int cellW = dp(48), cellH = dp(66);
        for (int i = 0; i < blanks; i++) addBlank(cellW, cellH);
        for (int d = 1; d <= max; d++) {
            long ms = StudyStats.millisFor(this, year, month, d);
            float frac = Math.min(1f, ms / (8f*60f*60f*1000f));
            boolean marked = StudyStats.marked(this, year, month, d);
            CalendarDayView v = new CalendarDayView(this);
            v.setData(d, frac, marked);
            final int day = d;
            v.setOnClickListener(x -> { StudyStats.toggleMark(this, year, month, day); renderCalendar(); });
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = cellW; lp.height = cellH; lp.setMargins(dp(2), dp(2), dp(2), dp(2));
            dayGrid.addView(v, lp);
        }
        int cells = blanks + max;
        while (cells < 42) { addBlank(cellW, cellH); cells++; }
    }

    private void addBlank(int w, int h) {
        View v = new View(this);
        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
        lp.width = w; lp.height = h; lp.setMargins(dp(2), dp(2), dp(2), dp(2));
        dayGrid.addView(v, lp);
    }

    private void refreshUi() {
        updateModeTabScale();
        populateRegularApps();
        refreshWeekendCards();
        updateLockIcons();
        updateMemoryPopup();
        updateRunningOverlay();
    }

    private void updateModeTabScale() {
        boolean reg = currentSub == SUB_REGULAR;
        regularTreeTab.setScaleX(reg ? 1.12f : 0.95f); regularTreeTab.setScaleY(reg ? 1.12f : 0.95f);
        weekendForestTab.setScaleX(reg ? 0.95f : 1.12f); weekendForestTab.setScaleY(reg ? 0.95f : 1.12f);
        regularTreeTab.setAlpha(reg ? 1f : 0.72f); weekendForestTab.setAlpha(reg ? 0.72f : 1f);
    }

    private void populateRegularApps() {
        regularAppsRow.removeAllViews();
        addAppIconsToRow(regularAppsRow, selectedRegularMode, false);
    }

    private void refreshWeekendCards() {
        for (int i = 0; i < 3; i++) {
            Mode m = LockState.weekendMode(this, i);
            weekendTimeViews[i].setText(LockState.slotStart(i) + "  " + LockState.slotEnd(i));
            replaceGlyphInFrame(weekendWorkButtons[i], modeGlyph(m));
            boolean finished = LockState.slotFinished(i);
            weekendWorkButtons[i].setAlpha(finished ? 0.35f : 1f);
            weekendWhitelistButtons[i].setAlpha(finished ? 0.35f : 1f);
            weekendTimeViews[i].setAlpha(finished ? 0.45f : 1f);
        }
    }

    private void addAppIconsToRow(LinearLayout row, Mode mode, boolean dim) {
        row.removeAllViews();
        Set<String> unique = new LinkedHashSet<>(mode.allowed);
        for (String pkg : unique) {
            View icon = launchIcon(pkg, dim);
            if (icon == null) continue;
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(32), dp(32));
            if (row.getChildCount() > 0) lp.leftMargin = dp(6);
            row.addView(icon, lp);
        }
    }

    private View launchIcon(String pkg, boolean dim) {
        int type;
        if (pkg.contains("xhs")) type = MiniGlyphView.APP_XHS;
        else if (pkg.contains("fenbi")) type = MiniGlyphView.APP_FENBI;
        else if (pkg.contains("wps")) type = MiniGlyphView.APP_WPS;
        else type = MiniGlyphView.APP_RECORD;
        FrameLayout box = new FrameLayout(this);
        GradientDrawableCompat.bg(box, PALE, dp(14));
        box.setAlpha(dim ? 0.35f : 1f);
        MiniGlyphView g = new MiniGlyphView(this, type); g.setTint(BROWN);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(dp(18), dp(18)); lp.gravity = Gravity.CENTER;
        box.addView(g, lp);
        box.setOnClickListener(v -> launchPackage(pkg));
        return box;
    }

    private void launchPackage(String pkg) {
        Intent i = getPackageManager().getLaunchIntentForPackage(pkg);
        if (i != null) { i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); startActivity(i); }
        else Toast.makeText(this, "未安装", Toast.LENGTH_SHORT).show();
    }

    private void updateSmallChoice(Button b, boolean selected) {
        GradientDrawableCompat.bg(b, selected ? BROWN : GREEN, dp(18));
        b.setTextColor(CREAM);
    }

    private void updateLockIcons() {
        ((MiniGlyphView)presetLockButton).setTint(prefs().getBoolean("preset_lock", false) ? GREEN : BROWN);
        ((MiniGlyphView)weekendLockButton).setTint(prefs().getBoolean("weekend_ui_lock", false) ? GREEN : BROWN);
    }

    private void togglePresetLock() {
        boolean now = !prefs().getBoolean("preset_lock", false);
        prefs().edit().putBoolean("preset_lock", now).apply();
        saveLockedPresetIfNeeded();
        updateLockIcons();
    }

    private void saveLockedPresetIfNeeded() {
        if (prefs().getBoolean("preset_lock", false)) {
            prefs().edit().putInt("locked_h", hourBox.getValue()).putInt("locked_m", minuteBox.getValue()).apply();
        }
    }

    private void loadLockedPreset() {
        if (prefs().getBoolean("preset_lock", false)) {
            hourBox.setValue(prefs().getInt("locked_h", 0));
            minuteBox.setValue(prefs().getInt("locked_m", 25));
        }
    }

    private void saveMemoryPreset(int h, int m) {
        String raw = h + ":" + m;
        ArrayList<String> list = new ArrayList<>(); list.add(raw);
        for (int i = 0; i < 3; i++) {
            String old = prefs().getString("mem_" + i, null);
            if (old != null && !old.equals(raw) && list.size() < 3) list.add(old);
        }
        SharedPreferences.Editor ed = prefs().edit();
        for (int i = 0; i < 3; i++) ed.putString("mem_" + i, i < list.size() ? list.get(i) : null);
        ed.apply();
    }

    private void toggleMemoryPopup() {
        memoryPopup.setVisibility(memoryPopup.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
        updateMemoryPopup();
    }

    private void updateMemoryPopup() {
        memoryPopup.removeAllViews();
        if (memoryPopup.getVisibility() != View.VISIBLE) return;
        for (int i = 0; i < 3; i++) {
            String raw = prefs().getString("mem_" + i, null);
            if (raw == null) continue;
            Button b = smallChoice(formatMemory(raw), v -> applyMemory(raw));
            updateSmallChoice(b, false);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(30));
            if (memoryPopup.getChildCount() > 0) lp.leftMargin = dp(6);
            memoryPopup.addView(b, lp);
        }
    }

    private String formatMemory(String raw) {
        String[] sp = raw.split(":");
        if (sp.length != 2) return raw;
        return sp[0] + "h" + sp[1] + "m";
    }

    private void applyMemory(String raw) {
        String[] sp = raw.split(":"); if (sp.length != 2) return;
        hourBox.setValue(Integer.parseInt(sp[0]));
        minuteBox.setValue(Integer.parseInt(sp[1]));
        saveLockedPresetIfNeeded();
        memoryPopup.setVisibility(View.GONE);
    }

    private void toggleWeekendLock() {
        boolean now = !prefs().getBoolean("weekend_ui_lock", false);
        prefs().edit().putBoolean("weekend_ui_lock", now).apply();
        updateLockIcons();
    }

    private void updateRunningOverlay() {
        if (currentPage != PAGE_FOCUS) { runningOverlay.setVisibility(View.GONE); return; }
        LockState.Session s = LockState.current(this);
        if (s != null) {
            runningOverlay.setVisibility(View.VISIBLE);
            runningOrb.setCenterText(LockState.timeLeftShort(s.end));
        } else {
            runningOverlay.setVisibility(View.GONE);
        }
    }

    private void openDrawer() { drawerOverlay.setVisibility(View.VISIBLE); }
    private void closeDrawer() { drawerOverlay.setVisibility(View.GONE); }

    private void openSettingsSheet() {
        sheetCard.removeAllViews();
        TextView title = t("设置", 18, BROWN, true); sheetCard.addView(title);
        LinearLayout row = new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL); row.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT); lp.topMargin = dp(14);
        sheetCard.addView(row, lp);
        row.addView(settingCard(MiniGlyphView.ACCESS, "无障碍", v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))), new LinearLayout.LayoutParams(0, dp(92), 1f));
        LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(0, dp(92), 1f); alp.leftMargin = dp(10);
        row.addView(settingCard(MiniGlyphView.ALARM, "闹钟", v -> openAlarmSetting()), alp);
        sheetOverlay.setVisibility(View.VISIBLE);
    }

    private View settingCard(int type, String text, View.OnClickListener click) {
        LinearLayout box = new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setGravity(Gravity.CENTER); box.setPadding(dp(8), dp(8), dp(8), dp(8));
        GradientDrawableCompat.panel(box, CREAM, PALE, dp(22), GREEN);
        MiniGlyphView g = new MiniGlyphView(this, type); g.setTint(type==MiniGlyphView.ACCESS ? Color.parseColor("#7350E6") : BROWN);
        box.addView(g, new LinearLayout.LayoutParams(dp(26), dp(26)));
        TextView tv = t(text, 13, BROWN, false); LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT); tlp.topMargin = dp(6); box.addView(tv, tlp);
        box.setOnClickListener(click); return box;
    }

    private void openAlarmSetting() {
        if (Build.VERSION.SDK_INT >= 31) {
            try { startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + getPackageName()))); }
            catch (Exception e) { startActivity(new Intent(Settings.ACTION_SETTINGS)); }
        }
    }

    private void openWhitelistSheet() {
        sheetCard.removeAllViews();
        Mode mode = currentSub == SUB_REGULAR ? selectedRegularMode : LockState.weekendMode(this, Math.max(0, LockState.currentWeekendSlot() >= 0 ? LockState.currentWeekendSlot() : 0));
        TextView title = t("白名单", 18, BROWN, true); sheetCard.addView(title);
        LinearLayout row = new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL); row.setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT); lp.topMargin = dp(14);
        sheetCard.addView(row, lp);
        addAppIconsToRow(row, mode, false);
        sheetOverlay.setVisibility(View.VISIBLE);
    }

    private void closeSheet() { sheetOverlay.setVisibility(View.GONE); }

    private void showPage(int page) {
        currentPage = page;
        focusPage.setVisibility(page == PAGE_FOCUS ? View.VISIBLE : View.GONE);
        calendarPage.setVisibility(page == PAGE_CALENDAR ? View.VISIBLE : View.GONE);
        if (page == PAGE_CALENDAR) renderCalendar();
        refreshUi();
    }

    private class SwipeListener extends GestureDetector.SimpleOnGestureListener {
        @Override public boolean onDown(MotionEvent e) { return true; }
        @Override public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (drawerOverlay.getVisibility() == View.VISIBLE || sheetOverlay.getVisibility() == View.VISIBLE || runningOverlay.getVisibility() == View.VISIBLE) return false;
            float dx = e2.getX() - e1.getX();
            float dy = e2.getY() - e1.getY();
            if (Math.abs(dx) < dp(70) || Math.abs(dx) < Math.abs(dy)) return false;
            if (dx < 0 && currentPage == PAGE_FOCUS) showPage(PAGE_CALENDAR);
            else if (dx > 0 && currentPage == PAGE_CALENDAR) showPage(PAGE_FOCUS);
            return true;
        }
    }

    private Mode loadRegularMode() {
        try { return Mode.valueOf(prefs().getString("regular_mode", Mode.RESUME.name())); }
        catch (Exception e) { return Mode.RESUME; }
    }
    private SharedPreferences prefs() { return getSharedPreferences("ui_state", MODE_PRIVATE); }
    private boolean isAccessibilityEnabled() {
        String enabled = Settings.Secure.getString(getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        return enabled != null && enabled.contains(getPackageName()+"/"+LockAccessibilityService.class.getName());
    }
    private void requestBasics() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 5);
        }
    }
    private TextView t(String s,int sp,int color,boolean bold){ TextView v=new TextView(this); v.setText(s); v.setTextSize(sp); v.setTextColor(color); v.setTypeface(Typeface.create(Typeface.SERIF, bold?Typeface.BOLD:Typeface.NORMAL)); return v; }
    private FrameLayout.LayoutParams full(){ return new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT); }
    private int dp(float v){ return (int)(v*getResources().getDisplayMetrics().density); }
}
