package com.forcefocus.app;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;

public class MainActivity extends Activity {
    private Typeface kai;
    private final int GREEN = Color.parseColor("#8AA832");
    private final int CREAM = Color.parseColor("#FFFBD3");
    private final int BROWN = Color.parseColor("#331915");
    private final int WHITE = Color.parseColor("#F8F7E8");

    private Mode selectedMode = Mode.RESUME;
    private int selectedMinutes = 25;

    private TextView currentTask;
    private TextView currentTime;
    private TextView nextWeekend;
    private TextView selectedTaskView;
    private TextView allowedView;
    private Button tabSettings;
    private Button tabFocus;
    private LinearLayout pageSettings;
    private LinearLayout pageFocus;

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        kai = Typeface.create(Typeface.SERIF, Typeface.NORMAL);
        build();
        requestBasics();
        Scheduler.scheduleNext14Days(this);
        update();
    }

    @Override public void onResume(){
        super.onResume();
        update();
    }

    private void build(){
        ScrollView sc = new ScrollView(this);
        sc.setFillViewport(true);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(18), dp(18), dp(22));
        root.setBackgroundColor(CREAM);
        sc.addView(root, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        setContentView(sc);

        LinearLayout tabs = new LinearLayout(this);
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        tabs.setPadding(dp(6), dp(6), dp(6), dp(6));
        GradientDrawableCompat.bg(tabs, GREEN, dp(22));
        root.addView(tabs, matchWrap(0,0,0,dp(14)));

        tabSettings = tabBtn("任务设置");
        tabFocus = tabBtn("专注");
        tabs.addView(tabSettings, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        LinearLayout.LayoutParams t2 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        t2.setMargins(dp(6),0,0,0);
        tabs.addView(tabFocus, t2);

        pageSettings = new LinearLayout(this);
        pageSettings.setOrientation(LinearLayout.VERTICAL);
        root.addView(pageSettings);

        pageFocus = new LinearLayout(this);
        pageFocus.setOrientation(LinearLayout.VERTICAL);
        root.addView(pageFocus);

        buildSettingsPage();
        buildFocusPage();

        tabSettings.setOnClickListener(v -> showPage(true));
        tabFocus.setOnClickListener(v -> showPage(false));
        showPage(true);
    }

    private void buildSettingsPage(){
        pageSettings.removeAllViews();

        LinearLayout permCard = card(GREEN, WHITE);
        permCard.addView(label("权限", WHITE, 16));
        Button access = miniBtn("无障碍", WHITE, BROWN);
        access.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        Button alarm = miniBtn("闹钟", WHITE, BROWN);
        alarm.setOnClickListener(v -> {
            if(Build.VERSION.SDK_INT >= 31){
                try{ startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:"+getPackageName()))); }
                catch (Exception e){ startActivity(new Intent(Settings.ACTION_SETTINGS)); }
            }
        });
        LinearLayout permRow = new LinearLayout(this);
        permRow.setOrientation(LinearLayout.HORIZONTAL);
        permRow.addView(access, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        LinearLayout.LayoutParams alarmLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        alarmLp.setMargins(dp(8),0,0,0);
        permRow.addView(alarm, alarmLp);
        permCard.addView(permRow, matchWrap(0,dp(8),0,0));
        pageSettings.addView(permCard, matchWrap(0,0,0,dp(12)));

        LinearLayout taskCard = card(WHITE, BROWN);
        taskCard.addView(label("任务", BROWN, 16));
        taskCard.addView(modeOption(Mode.RESUME));
        taskCard.addView(modeOption(Mode.JOB));
        taskCard.addView(modeOption(Mode.EXAM));
        taskCard.addView(modeOption(Mode.AUDIO));
        pageSettings.addView(taskCard, matchWrap(0,0,0,dp(12)));

        LinearLayout durationCard = card(BROWN, WHITE);
        durationCard.addView(label("时长", WHITE, 16));
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        Button b25 = durationBtn(25, BROWN, CREAM);
        Button b60 = durationBtn(60, BROWN, CREAM);
        Button b90 = durationBtn(90, BROWN, CREAM);
        row.addView(b25, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        LinearLayout.LayoutParams mid = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        mid.setMargins(dp(8),0,dp(8),0);
        row.addView(b60, mid);
        row.addView(b90, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        durationCard.addView(row, matchWrap(0,dp(8),0,0));
        pageSettings.addView(durationCard, matchWrap(0,0,0,dp(12)));

        LinearLayout fixedCard = card(GREEN, WHITE);
        fixedCard.addView(label("周末自动", WHITE, 16));
        fixedCard.addView(text("09:00–11:30\n13:30–17:00\n19:00–21:30", 22, WHITE, true));
        fixedCard.addView(text("模式：考公专业课", 14, WHITE, false));
        pageSettings.addView(fixedCard, matchWrap(0,0,0,dp(12)));
    }

    private void buildFocusPage(){
        pageFocus.removeAllViews();

        LinearLayout currentCard = card(GREEN, WHITE);
        currentCard.addView(label("当前", WHITE, 16));
        currentTask = text("未开始", 28, WHITE, true);
        currentTime = text("00:00:00", 34, WHITE, true);
        nextWeekend = text("", 14, WHITE, false);
        currentCard.addView(currentTask);
        currentCard.addView(currentTime);
        currentCard.addView(nextWeekend);
        pageFocus.addView(currentCard, matchWrap(0,0,0,dp(12)));

        LinearLayout selectedCard = card(WHITE, BROWN);
        selectedCard.addView(label("本次专注", BROWN, 16));
        selectedTaskView = text("简历相关 · 25 分钟", 24, BROWN, true);
        allowedView = text("手机锁住", 14, BROWN, false);
        selectedCard.addView(selectedTaskView);
        selectedCard.addView(allowedView);
        pageFocus.addView(selectedCard, matchWrap(0,0,0,dp(12)));

        Button startBtn = bigBtn("开始专注", BROWN, WHITE);
        startBtn.setOnClickListener(v -> {
            LockState.startManual(this, selectedMode, selectedMinutes);
            Toast.makeText(this, "已开始", Toast.LENGTH_SHORT).show();
            showPage(false);
            update();
        });
        pageFocus.addView(startBtn, matchWrap(0,0,0,dp(10)));
    }

    private LinearLayout modeOption(Mode mode){
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(16), dp(14), dp(16), dp(14));
        GradientDrawableCompat.bg(box, CREAM, dp(18));
        box.addView(text(mode.title, 20, BROWN, true));
        box.addView(text(mode.desc, 13, BROWN, false));
        box.setOnClickListener(v -> { selectedMode = mode; updateSelection(); });
        LinearLayout.LayoutParams lp = matchWrap(0,0,0,dp(8));
        box.setLayoutParams(lp);
        return box;
    }

    private Button durationBtn(int minutes, int bg, int fg){
        Button b = miniBtn(String.valueOf(minutes), fg, bg);
        b.setOnClickListener(v -> { selectedMinutes = minutes; updateSelection(); });
        return b;
    }

    private void showPage(boolean settings){
        pageSettings.setVisibility(settings ? View.VISIBLE : View.GONE);
        pageFocus.setVisibility(settings ? View.GONE : View.VISIBLE);
        styleTab(tabSettings, settings);
        styleTab(tabFocus, !settings);
        updateSelection();
    }

    private void updateSelection(){
        if(selectedTaskView != null){
            selectedTaskView.setText(selectedMode.title + " · " + selectedMinutes + " 分钟");
        }
        if(allowedView != null){
            String extra = selectedMode.allowed.isEmpty() ? "手机锁住" : selectedMode.desc;
            allowedView.setText(extra);
        }

        if(pageSettings != null){
            // recolor task cards
            for(int i=0;i<pageSettings.getChildCount();i++){
                View child = pageSettings.getChildAt(i);
                if(!(child instanceof LinearLayout)) continue;
                LinearLayout ll=(LinearLayout)child;
                if(ll.getChildCount()>1 && ll.getChildAt(0) instanceof TextView){
                    CharSequence head=((TextView)ll.getChildAt(0)).getText();
                    if("任务".contentEquals(head)){
                        for(int j=1;j<ll.getChildCount();j++){
                            View v=ll.getChildAt(j);
                            if(v instanceof LinearLayout){
                                LinearLayout item=(LinearLayout)v;
                                TextView title=(TextView)item.getChildAt(0);
                                boolean on=title.getText().toString().equals(selectedMode.title);
                                GradientDrawableCompat.bg(item, on ? GREEN : CREAM, dp(18));
                                ((TextView)item.getChildAt(0)).setTextColor(on ? WHITE : BROWN);
                                if(item.getChildCount()>1) ((TextView)item.getChildAt(1)).setTextColor(on ? WHITE : BROWN);
                            }
                        }
                    }
                    if("时长".contentEquals(head)){
                        LinearLayout row=(LinearLayout)ll.getChildAt(1);
                        for(int j=0;j<row.getChildCount();j++){
                            Button b=(Button)row.getChildAt(j);
                            boolean on=Integer.parseInt(b.getText().toString())==selectedMinutes;
                            GradientDrawableCompat.button(b, on ? GREEN : CREAM, on ? GREEN : CREAM, BROWN, on ? WHITE : BROWN);
                        }
                    }
                }
            }
        }
    }

    private void update(){
        LockState.Session s = LockState.current(this);
        if(currentTask == null) return;
        if(s == null){
            currentTask.setText("未开始");
            currentTime.setText(String.format("%02d:00", selectedMinutes));
        } else {
            currentTask.setText(s.mode.title);
            currentTime.setText(LockState.timeLeft(s.end));
        }
        nextWeekend.setText("下次自动：" + LockState.nextWeekendText() + "   解除：" + LockState.escapesLeft(this) + "/2");
        updateSelection();
    }

    private Button tabBtn(String s){
        Button b = new Button(this);
        b.setAllCaps(false);
        b.setText(s);
        b.setTypeface(kai);
        b.setTextSize(18);
        b.setPadding(dp(8), dp(12), dp(8), dp(12));
        return b;
    }

    private void styleTab(Button b, boolean active){
        GradientDrawableCompat.bg(b, active ? CREAM : GREEN, dp(18));
        b.setTextColor(active ? BROWN : WHITE);
    }

    private LinearLayout card(int bg, int text){
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(18), dp(18), dp(18), dp(18));
        GradientDrawableCompat.bg(box, bg, dp(28));
        return box;
    }

    private TextView label(String s, int color, int sp){
        TextView v = new TextView(this);
        v.setText(s);
        v.setTextColor(color);
        v.setTextSize(sp);
        v.setTypeface(kai, Typeface.BOLD);
        return v;
    }

    private TextView text(String s, int sp, int color, boolean bold){
        TextView v = new TextView(this);
        v.setText(s);
        v.setTextColor(color);
        v.setTextSize(sp);
        v.setTypeface(kai, bold ? Typeface.BOLD : Typeface.NORMAL);
        v.setPadding(0, dp(6), 0, 0);
        return v;
    }

    private Button miniBtn(String s, int bgText, int fgBg){
        Button b = new Button(this);
        b.setAllCaps(false);
        b.setText(s);
        b.setTextColor(bgText);
        b.setTypeface(kai);
        b.setTextSize(16);
        b.setPadding(dp(10), dp(10), dp(10), dp(10));
        GradientDrawableCompat.bg(b, fgBg, dp(18));
        return b;
    }

    private Button bigBtn(String s, int bg, int fg){
        Button b = new Button(this);
        b.setAllCaps(false);
        b.setText(s);
        b.setTextColor(fg);
        b.setTypeface(kai, Typeface.BOLD);
        b.setTextSize(24);
        b.setPadding(dp(16), dp(16), dp(16), dp(16));
        GradientDrawableCompat.bg(b, bg, dp(28));
        return b;
    }

    private LinearLayout.LayoutParams matchWrap(int l, int t, int r, int b){
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(l, t, r, b);
        return lp;
    }

    private int dp(int x){ return (int)(x * getResources().getDisplayMetrics().density); }

    private void requestBasics(){
        if(Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 5);
        }
    }
}
