from pathlib import Path

p = Path('app/src/main/java/com/forcefocus/app/MainActivity.java')
s = p.read_text(encoding='utf-8')

s = s.replace('iv.setScaleType(ImageView.ScaleType.CENTER_CROP);', 'iv.setScaleType(ImageView.ScaleType.FIT_CENTER);')

old = '''        addAroundOrb(btnResume, Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, dp(0), 0, 0);\n        addAroundOrb(btnExam, Gravity.CENTER_VERTICAL | Gravity.START, dp(0), 0, 0, 0);\n        addAroundOrb(btnJob, Gravity.CENTER_VERTICAL | Gravity.END, 0, 0, dp(0), 0);\n        addAroundOrb(btnAudio, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0, 0, dp(0));'''
new = '''        addFanChoice(btnResume, dp(18), dp(26), -18f);\n        addFanChoice(btnJob, dp(78), dp(8), -7f);\n        addFanChoice(btnExam, dp(164), dp(8), 7f);\n        addFanChoice(btnAudio, dp(224), dp(26), 18f);'''
s = s.replace(old, new)

old = '''    private Button radialButton(String text, Mode mode) {\n        Button b = new Button(this);\n        b.setAllCaps(false); b.setText(text); b.setTextSize(14); b.setTypeface(Typeface.create(Typeface.SERIF, Typeface.BOLD));\n        GradientDrawableCompat.bg(b, BROWN, dp(20));\n        b.setTextColor(CREAM);\n        b.setOnClickListener(v -> { selectedRegularMode = mode; prefs().edit().putString("regular_mode", mode.name()).apply(); hideRegularChoices(); refreshUi(); });\n        return b;\n    }\n\n    private void addAroundOrb(Button b, int gravity, int l, int t, int r, int bo) {\n        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(dp(96), dp(40));\n        lp.gravity = gravity;\n        lp.setMargins(l, t, r, bo);\n        orbZone.addView(b, lp);\n    }'''
new = '''    private Button radialButton(String text, Mode mode) {\n        Button b = new Button(this);\n        b.setAllCaps(false); b.setText(text); b.setTextSize(11); b.setTypeface(Typeface.create(Typeface.SERIF, Typeface.BOLD));\n        GradientDrawableCompat.panel(b, CREAM, PALE, dp(13), GREEN);\n        b.setTextColor(BROWN);\n        b.setOnClickListener(v -> { selectedRegularMode = mode; prefs().edit().putString("regular_mode", mode.name()).apply(); hideRegularChoices(); refreshUi(); });\n        return b;\n    }\n\n    private void addFanChoice(Button b, int left, int top, float rotation) {\n        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(dp(62), dp(34));\n        lp.leftMargin = left; lp.topMargin = top;\n        orbZone.addView(b, lp);\n        b.setRotation(rotation);\n    }'''
s = s.replace(old, new)

old = '''        LinearLayout wTop = new LinearLayout(this);\n        wTop.setOrientation(LinearLayout.HORIZONTAL);\n        wTop.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);\n        LinearLayout.LayoutParams wTopLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);\n        wTopLp.leftMargin = dp(20); wTopLp.rightMargin = dp(20);\n        weekendPanel.addView(wTop, wTopLp);\n        weekendLockButton = plainTinyIcon(MiniGlyphView.LOCK, v -> toggleWeekendLock());\n        wTop.addView(weekendLockButton, new LinearLayout.LayoutParams(dp(30), dp(30)));'''
new = '''        WeekendLockView lockView = new WeekendLockView(this);\n        lockView.setOnClickListener(v -> toggleWeekendLock());\n        weekendLockButton = lockView;\n        LinearLayout.LayoutParams lockLp = new LinearLayout.LayoutParams(dp(58), dp(58));\n        lockLp.bottomMargin = dp(8);\n        weekendPanel.addView(lockView, lockLp);'''
s = s.replace(old, new)

start = s.find('        for (int i = 0; i < 3; i++) {', s.find('weekendLockButton = lockView;'))
end_marker = '        runningOverlay = new FrameLayout(this);'
end = s.find(end_marker, start)
if start != -1 and end != -1:
    block = '''        for (int i = 0; i < 3; i++) {\n            final int idx = i;\n            LinearLayout card = new LinearLayout(this);\n            card.setOrientation(LinearLayout.HORIZONTAL);\n            card.setGravity(Gravity.CENTER_VERTICAL);\n            card.setPadding(dp(18), dp(10), dp(12), dp(10));\n            GradientDrawableCompat.bg(card, GREEN, dp(20));\n            LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(92));\n            cardLp.leftMargin = dp(18); cardLp.rightMargin = dp(18); cardLp.topMargin = dp(14);\n            weekendPanel.addView(card, cardLp);\n\n            TextView time = t(LockState.slotStart(i) + " – " + LockState.slotEnd(i), 23, CREAM, true);\n            time.setGravity(Gravity.CENTER_VERTICAL);\n            weekendTimeViews[i] = time;\n            card.addView(time, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));\n\n            LinearLayout right = new LinearLayout(this);\n            right.setOrientation(LinearLayout.VERTICAL);\n            right.setGravity(Gravity.CENTER);\n            card.addView(right, new LinearLayout.LayoutParams(dp(34), ViewGroup.LayoutParams.MATCH_PARENT));\n\n            View work = tinyWeekendAction(modeGlyph(LockState.weekendMode(this, i)), v -> openWeekendModePicker(idx));\n            weekendWorkButtons[i] = work;\n            right.addView(work, new LinearLayout.LayoutParams(dp(24), dp(24)));\n\n            View white = tinyWeekendAction(MiniGlyphView.WHITELIST, v -> openWhitelistForWeekend(idx));\n            weekendWhitelistButtons[i] = white;\n            LinearLayout.LayoutParams wlp = new LinearLayout.LayoutParams(dp(24), dp(24)); wlp.topMargin = dp(5);\n            right.addView(white, wlp);\n\n            LinearLayout apps = new LinearLayout(this); apps.setVisibility(View.GONE); weekendAppRows[i] = apps;\n        }\n\n'''
    s = s[:start] + block + s[end:]

anchor = '    private int modeGlyph(Mode mode) {'
if 'private View tinyWeekendAction' not in s:
    helper = '''    private View tinyWeekendAction(int glyph, View.OnClickListener click) {\n        FrameLayout box = new FrameLayout(this);\n        MiniGlyphView icon = new MiniGlyphView(this, glyph);\n        icon.setTint(CREAM);\n        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(dp(20), dp(20));\n        lp.gravity = Gravity.CENTER;\n        box.addView(icon, lp);\n        box.setOnClickListener(click);\n        return box;\n    }\n\n'''
    s = s.replace(anchor, helper + anchor)

s = s.replace('icon.setTint(BROWN);\n        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(dp(24), dp(24));', 'icon.setTint(CREAM);\n        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(dp(20), dp(20));')

s = s.replace('regularTreeTab.setScaleX(reg ? 1.12f : 0.95f); regularTreeTab.setScaleY(reg ? 1.12f : 0.95f);\n        weekendForestTab.setScaleX(reg ? 0.95f : 1.12f); weekendForestTab.setScaleY(reg ? 0.95f : 1.12f);', 'regularTreeTab.setScaleX(reg ? 1f : 0.5f); regularTreeTab.setScaleY(reg ? 1f : 0.5f);\n        weekendForestTab.setScaleX(reg ? 0.5f : 1f); weekendForestTab.setScaleY(reg ? 0.5f : 1f);')

s = s.replace('((MiniGlyphView)weekendLockButton).setTint(prefs().getBoolean("weekend_ui_lock", false) ? GREEN : BROWN);', 'if (weekendLockButton instanceof WeekendLockView) ((WeekendLockView)weekendLockButton).setLocked(prefs().getBoolean("weekend_ui_lock", false));')

s = s.replace('week.addView(tv, new LinearLayout.LayoutParams(dp(48), ViewGroup.LayoutParams.WRAP_CONTENT));', 'week.addView(tv, new LinearLayout.LayoutParams(dp(42), ViewGroup.LayoutParams.WRAP_CONTENT));')
s = s.replace('int cellW = dp(48), cellH = dp(66);', 'int cellW = dp(42), cellH = dp(58);')

old = '''    private void openSettingsSheet() {\n        sheetCard.removeAllViews();\n        TextView title = t("设置", 18, BROWN, true); sheetCard.addView(title);\n        LinearLayout row = new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL); row.setGravity(Gravity.CENTER);\n        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT); lp.topMargin = dp(14);\n        sheetCard.addView(row, lp);\n        row.addView(settingCard(MiniGlyphView.ACCESS, "无障碍", v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))), new LinearLayout.LayoutParams(0, dp(92), 1f));\n        LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(0, dp(92), 1f); alp.leftMargin = dp(10);\n        row.addView(settingCard(MiniGlyphView.ALARM, "闹钟", v -> openAlarmSetting()), alp);\n        sheetOverlay.setVisibility(View.VISIBLE);\n    }'''
new = '''    private void openSettingsSheet() {\n        sheetCard.removeAllViews();\n        Button access = drawerTextButton("无障碍", v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));\n        Button alarm = drawerTextButton("闹钟", v -> openAlarmSetting());\n        sheetCard.addView(access, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));\n        sheetCard.addView(alarm, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));\n        sheetOverlay.setVisibility(View.VISIBLE);\n    }'''
s = s.replace(old, new)

old = '''        addAppIconsToRow(row, mode, false);\n        sheetOverlay.setVisibility(View.VISIBLE);'''
new = '''        addAppIconsToRow(row, mode, false);\n        if (row.getChildCount() == 0) {\n            TextView none = t("当前任务无手机白名单", 14, BROWN, false);\n            row.addView(none);\n        }\n        sheetOverlay.setVisibility(View.VISIBLE);'''
s = s.replace(old, new, 1)

old = '''        ((FrameLayout) runningOverlay).addView(runningOrb, roLp);\n    }'''
new = '''        ((FrameLayout) runningOverlay).addView(runningOrb, roLp);\n        LinearLayout runTools = new LinearLayout(this);\n        runTools.setTag("run_tools");\n        runTools.setOrientation(LinearLayout.HORIZONTAL);\n        runTools.setGravity(Gravity.CENTER);\n        FrameLayout.LayoutParams rtLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(42));\n        rtLp.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;\n        rtLp.bottomMargin = dp(58);\n        ((FrameLayout) runningOverlay).addView(runTools, rtLp);\n    }'''
s = s.replace(old, new, 1)

s = s.replace('runningOrb.setCenterText(LockState.timeLeftShort(s.end));\n        } else {', 'runningOrb.setCenterText(LockState.timeLeftShort(s.end));\n            updateRunTools(s);\n        } else {')

anchor = '    private void openDrawer() {'
if 'private void updateRunTools' not in s:
    method = '''    private void updateRunTools(LockState.Session s) {\n        View found = ((FrameLayout)runningOverlay).findViewWithTag("run_tools");\n        if (!(found instanceof LinearLayout)) return;\n        LinearLayout row = (LinearLayout)found;\n        row.removeAllViews();\n        addAppIconsToRow(row, s.mode, false);\n        View stop = tinyWeekendAction(MiniGlyphView.STOP, v -> {\n            if (LockState.useEscape(this, s.end)) {\n                Toast.makeText(this, "剩余 " + LockState.escapesLeft(this) + " 次", Toast.LENGTH_SHORT).show();\n                refreshUi();\n            } else {\n                Toast.makeText(this, "本周已用完", Toast.LENGTH_SHORT).show();\n            }\n        });\n        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(28), dp(28));\n        if (row.getChildCount() > 0) lp.leftMargin = dp(10);\n        row.addView(stop, lp);\n    }\n\n'''
    s = s.replace(anchor, method + anchor)

p.write_text(s, encoding='utf-8')

lock = Path('app/src/main/java/com/forcefocus/app/WeekendLockView.java')
lock.write_text(r'''package com.forcefocus.app;
import android.content.Context;
import android.graphics.*;
import android.view.View;
public class WeekendLockView extends View {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private boolean locked = false;
    private final int GREEN = Color.parseColor("#8AA832"), DEEP = Color.parseColor("#4F7A46"), CREAM = Color.parseColor("#FFFBD3");
    public WeekendLockView(Context c){ super(c); setClickable(true); }
    public void setLocked(boolean value){ locked=value; invalidate(); }
    @Override protected void onDraw(Canvas c){
        super.onDraw(c); float w=getWidth(), h=getHeight(), cx=w/2f, cy=h*.36f, r=Math.min(w,h)*.20f;
        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(dp(5)); p.setColor(Color.parseColor("#AFC28A")); c.drawCircle(cx,cy,r,p);
        p.setStrokeWidth(dp(2)); p.setColor(Color.parseColor("#7E9071")); RectF arc=new RectF(cx-r-dp(7),cy-r-dp(7),cx+r+dp(7),cy+r+dp(7)); c.drawArc(arc,205,230,false,p);
        p.setStyle(Paint.Style.FILL); p.setColor(locked?DEEP:Color.parseColor("#AFC28A")); Path d=new Path(); d.moveTo(cx,cy+r*.85f); d.lineTo(cx-r*.9f,cy+r*1.9f); d.lineTo(cx,cy+r*2.85f); d.lineTo(cx+r*.9f,cy+r*1.9f); d.close(); c.drawPath(d,p);
        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(dp(2)); p.setColor(CREAM); Path v=new Path(); v.moveTo(cx-r*.5f,cy+r*1.8f); v.lineTo(cx,cy+r*2.35f); v.lineTo(cx+r*.5f,cy+r*1.8f); c.drawPath(v,p);
        if(locked){ drawLeaf(c,cx+r*.55f,cy+r*.45f,1); drawLeaf(c,cx+r*.95f,cy+r*.85f,1); drawLeaf(c,cx-r*.55f,cy+r*.55f,-1); drawLeaf(c,cx-r*.95f,cy+r*.95f,-1); }
    }
    private void drawLeaf(Canvas c,float x,float y,int dir){ p.setStyle(Paint.Style.FILL); p.setColor(GREEN); Path q=new Path(); q.moveTo(x,y); q.quadTo(x+dir*dp(9),y-dp(5),x+dir*dp(15),y-dp(1)); q.quadTo(x+dir*dp(8),y+dp(6),x,y); c.drawPath(q,p); }
    private float dp(float v){ return v*getResources().getDisplayMetrics().density; }
}
''', encoding='utf-8')