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
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.HorizontalScrollView;
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
    private final int LIGHT_GREEN = Color.parseColor("#D7E7A8");

    private FrameLayout root, mainPage, calendarPage, bodyContainer, orbZone, overlaySheet;
    private LinearLayout regularPanel, weekendPanel, regularAppsRow, memoryPopup, whiteSheetContent;
    private FocusOrbView orb;
    private Button btnResume, btnJob, btnExam, btnAudio;
    private ValueBoxView hourBox, minuteBox, selectedBox;
    private Mode selectedMode = Mode.RESUME;
    private View treeTab, forestTab, weekendLockButton, presetLockButton, memoryButton;
    private GridLayout dayGrid;
    private TextView monthLabel;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable tick = new Runnable(){ public void run(){ refreshUi(); handler.postDelayed(this,1000); }};
    private final LinearLayout[] weekendCards = new LinearLayout[3];
    private final LinearLayout[] weekendApps = new LinearLayout[3];
    private final TextView[] weekendTimes = new TextView[3];
    private final Button[][] weekendModeButtons = new Button[3][4];

    @Override protected void onCreate(Bundle b){
        super.onCreate(b);
        build();
        loadPreset();
        requestBasics();
        Scheduler.scheduleNext14Days(this);
        autoChooseTopMode();
        setSelectedMode(loadRegularMode());
        handler.post(tick);
    }
    @Override protected void onResume(){ super.onResume(); autoChooseTopMode(); refreshUi(); renderCalendar(); }
    @Override protected void onDestroy(){ super.onDestroy(); handler.removeCallbacks(tick); }

    private void build(){
        root=new FrameLayout(this); root.setBackgroundColor(CREAM); setContentView(root);
        buildMainPage(); buildCalendarPage(); buildOverlaySheet(); showMainPage();
    }

    private void buildMainPage(){
        mainPage=new FrameLayout(this); mainPage.setBackgroundColor(CREAM); root.addView(mainPage,full());
        LinearLayout top=new LinearLayout(this); top.setOrientation(LinearLayout.HORIZONTAL); top.setGravity(Gravity.CENTER_VERTICAL|Gravity.CENTER_HORIZONTAL);
        FrameLayout.LayoutParams topLp=new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT); topLp.gravity=Gravity.TOP; topLp.setMargins(dp(22),dp(18),dp(22),0); mainPage.addView(top,topLp);
        treeTab=topIconTab(MiniGlyphView.TREE,v->showRegularPanel(true)); top.addView(treeTab,new LinearLayout.LayoutParams(dp(66),dp(66)));
        top.addView(new Space(this),new LinearLayout.LayoutParams(0,1,1));
        View calendarTab=topIconTab(MiniGlyphView.CALENDAR,v->showCalendarPage()); top.addView(calendarTab,new LinearLayout.LayoutParams(dp(54),dp(54)));
        top.addView(new Space(this),new LinearLayout.LayoutParams(0,1,1));
        forestTab=topIconTab(MiniGlyphView.FOREST,v->showWeekendPanel(true)); top.addView(forestTab,new LinearLayout.LayoutParams(dp(66),dp(66)));

        LinearLayout rail=new LinearLayout(this); rail.setOrientation(LinearLayout.VERTICAL); rail.setGravity(Gravity.CENTER_HORIZONTAL);
        FrameLayout.LayoutParams railLp=new FrameLayout.LayoutParams(dp(58),ViewGroup.LayoutParams.WRAP_CONTENT); railLp.gravity=Gravity.START|Gravity.CENTER_VERTICAL; railLp.leftMargin=dp(10); mainPage.addView(rail,railLp);
        rail.addView(sideIcon(MiniGlyphView.SETTINGS,v->openSettingsSheet()),new LinearLayout.LayoutParams(dp(46),dp(46)));
        LinearLayout.LayoutParams wl=new LinearLayout.LayoutParams(dp(46),dp(46)); wl.topMargin=dp(10); rail.addView(sideIcon(MiniGlyphView.LIST,v->openWhitelistSheet()),wl);

        bodyContainer=new FrameLayout(this); FrameLayout.LayoutParams bodyLp=new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT); bodyLp.setMargins(dp(62),dp(82),dp(12),0); mainPage.addView(bodyContainer,bodyLp);
        buildRegularPanel(); buildWeekendPanel(); bodyContainer.addView(regularPanel,full()); bodyContainer.addView(weekendPanel,full());
    }

    private void buildRegularPanel(){
        regularPanel=new LinearLayout(this); regularPanel.setOrientation(LinearLayout.VERTICAL); regularPanel.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL);
        orbZone=new FrameLayout(this); LinearLayout.LayoutParams zoneLp=new LinearLayout.LayoutParams(dp(320),dp(350)); zoneLp.topMargin=dp(12); regularPanel.addView(orbZone,zoneLp);
        orb=new FocusOrbView(this); orb.setFill(GREEN); orb.setCenterTextColor(CREAM); orb.setLongPressListener(this::toggleModes); orb.setOnClickListener(v->toggleModes());
        FrameLayout.LayoutParams orbLp=new FrameLayout.LayoutParams(dp(224),dp(224)); orbLp.gravity=Gravity.CENTER_HORIZONTAL|Gravity.BOTTOM; orbLp.bottomMargin=dp(28); orbZone.addView(orb,orbLp);
        btnResume=arcModeBtn("简历",Mode.RESUME); btnExam=arcModeBtn("考公",Mode.EXAM); btnAudio=arcModeBtn("磨耳",Mode.AUDIO); btnJob=arcModeBtn("岗位",Mode.JOB);
        placeMode(btnResume,Gravity.TOP|Gravity.START,dp(28),dp(10),0,0); placeMode(btnExam,Gravity.TOP|Gravity.CENTER_HORIZONTAL,0,0,0,0); placeMode(btnAudio,Gravity.TOP|Gravity.END,0,dp(10),dp(28),0); placeMode(btnJob,Gravity.CENTER_VERTICAL|Gravity.END,0,0,dp(4),dp(42)); hideModes();

        regularAppsRow=new LinearLayout(this); regularAppsRow.setOrientation(LinearLayout.HORIZONTAL); regularAppsRow.setGravity(Gravity.CENTER); LinearLayout.LayoutParams appLp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,dp(54)); appLp.topMargin=dp(-8); regularPanel.addView(regularAppsRow,appLp);
        regularPanel.addView(new Space(this),new LinearLayout.LayoutParams(1,0,1f));

        LinearLayout bottom=new LinearLayout(this); bottom.setOrientation(LinearLayout.HORIZONTAL); bottom.setGravity(Gravity.BOTTOM|Gravity.CENTER_VERTICAL); bottom.setPadding(dp(8),0,dp(10),dp(56)); regularPanel.addView(bottom,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));
        hourBox=new ValueBoxView(this); hourBox.setRange(0,4); hourBox.setValue(0); hourBox.setUnit("h");
        minuteBox=new ValueBoxView(this); minuteBox.setRange(0,59); minuteBox.setValue(25); minuteBox.setUnit("min");
        ValueBoxView.Listener vl=new ValueBoxView.Listener(){ public void onSelected(ValueBoxView v){selectBox(v);} public void onValueChanged(ValueBoxView v,int value){refreshUi();saveLockedPresetIfNeeded();}};
        hourBox.setListener(vl); minuteBox.setListener(vl); selectBox(hourBox);
        LinearLayout timeWrap=new LinearLayout(this); timeWrap.setOrientation(LinearLayout.HORIZONTAL); timeWrap.setGravity(Gravity.CENTER_VERTICAL); GradientDrawableCompat.panel(timeWrap,CREAM,PALE,dp(30),GREEN); timeWrap.setPadding(dp(10),dp(10),dp(10),dp(10));
        timeWrap.addView(hourBox,new LinearLayout.LayoutParams(dp(82),dp(88))); LinearLayout.LayoutParams minLp=new LinearLayout.LayoutParams(dp(82),dp(88)); minLp.setMargins(dp(8),0,0,0); timeWrap.addView(minuteBox,minLp); bottom.addView(timeWrap,new LinearLayout.LayoutParams(0,dp(110),1.05f));

        LinearLayout aux=new LinearLayout(this); aux.setOrientation(LinearLayout.VERTICAL); LinearLayout.LayoutParams auxLp=new LinearLayout.LayoutParams(dp(52),dp(110)); auxLp.leftMargin=dp(10); bottom.addView(aux,auxLp);
        presetLockButton=iconFrame(MiniGlyphView.LOCK,true,v->togglePresetLock()); aux.addView(presetLockButton,new LinearLayout.LayoutParams(dp(46),dp(46)));
        memoryButton=iconFrame(MiniGlyphView.MEMORY,false,v->toggleMemoryPopup()); LinearLayout.LayoutParams memLp=new LinearLayout.LayoutParams(dp(46),dp(46)); memLp.topMargin=dp(8); aux.addView(memoryButton,memLp);
        View play=iconFrame(MiniGlyphView.PLAY,false,v->startFocus()); LinearLayout.LayoutParams playLp=new LinearLayout.LayoutParams(0,dp(110),0.55f); playLp.leftMargin=dp(12); bottom.addView(play,playLp);

        memoryPopup=new LinearLayout(this); memoryPopup.setOrientation(LinearLayout.HORIZONTAL); memoryPopup.setGravity(Gravity.CENTER); GradientDrawableCompat.panel(memoryPopup,CREAM,PALE,dp(24),GREEN); memoryPopup.setPadding(dp(8),dp(8),dp(8),dp(8)); memoryPopup.setVisibility(View.GONE); LinearLayout.LayoutParams mpLp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT); mpLp.bottomMargin=dp(12); regularPanel.addView(memoryPopup,mpLp);
    }

    private void buildWeekendPanel(){
        weekendPanel=new LinearLayout(this); weekendPanel.setOrientation(LinearLayout.VERTICAL); weekendPanel.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL); weekendPanel.setPadding(dp(8),dp(8),dp(8),dp(26));
        LinearLayout lockRow=new LinearLayout(this); lockRow.setOrientation(LinearLayout.HORIZONTAL); lockRow.setGravity(Gravity.END|Gravity.CENTER_VERTICAL); weekendPanel.addView(lockRow,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT)); weekendLockButton=iconFrame(MiniGlyphView.LOCK,true,v->toggleWeekendLock()); lockRow.addView(weekendLockButton,new LinearLayout.LayoutParams(dp(46),dp(46)));
        for(int i=0;i<3;i++){
            LinearLayout card=new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL); card.setPadding(dp(14),dp(12),dp(14),dp(12)); GradientDrawableCompat.panel(card,CREAM,PALE,dp(26),GREEN); LinearLayout.LayoutParams clp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT); clp.topMargin=dp(12); weekendPanel.addView(card,clp); weekendCards[i]=card;
            TextView time=t(LockState.slotStart(i)+"  ·  "+LockState.slotEnd(i),20,BROWN,true); weekendTimes[i]=time; card.addView(time);
            LinearLayout appRow=new LinearLayout(this); appRow.setOrientation(LinearLayout.HORIZONTAL); appRow.setGravity(Gravity.START|Gravity.CENTER_VERTICAL); LinearLayout.LayoutParams arlp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,dp(40)); arlp.topMargin=dp(8); card.addView(appRow,arlp); weekendApps[i]=appRow;
            HorizontalScrollView hsv=new HorizontalScrollView(this); hsv.setHorizontalScrollBarEnabled(false); LinearLayout row=new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL); hsv.addView(row); LinearLayout.LayoutParams mrlp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT); mrlp.topMargin=dp(10); card.addView(hsv,mrlp);
            final int idx=i;
            weekendModeButtons[i][0]=miniModeBtn("简历",v->chooseWeekendSlot(idx,Mode.RESUME)); weekendModeButtons[i][1]=miniModeBtn("岗位",v->chooseWeekendSlot(idx,Mode.JOB)); weekendModeButtons[i][2]=miniModeBtn("考公",v->chooseWeekendSlot(idx,Mode.EXAM)); weekendModeButtons[i][3]=miniModeBtn("磨耳",v->chooseWeekendSlot(idx,Mode.AUDIO));
            for(int j=0;j<4;j++){LinearLayout.LayoutParams blp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,dp(34));if(j>0)blp.leftMargin=dp(6);row.addView(weekendModeButtons[i][j],blp);}
        }
    }

    private void buildCalendarPage(){
        calendarPage=new FrameLayout(this); calendarPage.setBackgroundColor(CREAM); root.addView(calendarPage,full());
        LinearLayout wrap=new LinearLayout(this); wrap.setOrientation(LinearLayout.VERTICAL); wrap.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL); calendarPage.addView(wrap,full());
        LinearLayout top=new LinearLayout(this); top.setOrientation(LinearLayout.HORIZONTAL); top.setGravity(Gravity.CENTER_VERTICAL); top.setPadding(dp(18),dp(16),dp(18),0); wrap.addView(top,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));
        View back=sideIcon(MiniGlyphView.TREE,v->showMainPage()); top.addView(back,new LinearLayout.LayoutParams(dp(44),dp(44))); monthLabel=t("",20,BROWN,true); monthLabel.setGravity(Gravity.CENTER); top.addView(monthLabel,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f)); top.addView(new Space(this),new LinearLayout.LayoutParams(dp(44),dp(44)));
        LinearLayout week=new LinearLayout(this); week.setOrientation(LinearLayout.HORIZONTAL); week.setGravity(Gravity.CENTER); week.setPadding(dp(18),dp(8),dp(18),0); wrap.addView(week,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT)); String[] ds={"一","二","三","四","五","六","日"}; for(String d:ds){TextView tv=t(d,12,BROWN,false);tv.setGravity(Gravity.CENTER);week.addView(tv,new LinearLayout.LayoutParams(dp(56),ViewGroup.LayoutParams.WRAP_CONTENT));}
        dayGrid=new GridLayout(this); dayGrid.setColumnCount(7); dayGrid.setRowCount(6); LinearLayout.LayoutParams glp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT);glp.topMargin=dp(10);wrap.addView(dayGrid,glp);renderCalendar();
    }

    private void buildOverlaySheet(){ overlaySheet=new FrameLayout(this);overlaySheet.setBackgroundColor(Color.parseColor("#66000000"));overlaySheet.setVisibility(View.GONE);root.addView(overlaySheet,full());overlaySheet.setOnClickListener(v->closeSheet()); }
    private View topIconTab(int type,View.OnClickListener click){FrameLayout f=new FrameLayout(this);f.setPadding(dp(6),dp(6),dp(6),dp(6));MiniGlyphView icon=new MiniGlyphView(this,type);icon.setTint(BROWN);FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(dp(34),dp(34));lp.gravity=Gravity.CENTER;f.addView(icon,lp);f.setOnClickListener(click);return f;}
    private View sideIcon(int type,View.OnClickListener click){FrameLayout f=new FrameLayout(this);GradientDrawableCompat.panel(f,CREAM,PALE,dp(18),GREEN);f.setPadding(dp(6),dp(6),dp(6),dp(6));MiniGlyphView icon=new MiniGlyphView(this,type);icon.setTint(BROWN);FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(dp(26),dp(26));lp.gravity=Gravity.CENTER;f.addView(icon,lp);f.setOnClickListener(click);return f;}
    private View iconFrame(int type,boolean selected,View.OnClickListener click){FrameLayout f=new FrameLayout(this);GradientDrawableCompat.panel(f,selected?LIGHT_GREEN:CREAM,selected?LIGHT_GREEN:PALE,dp(22),GREEN);f.setPadding(dp(6),dp(6),dp(6),dp(6));MiniGlyphView icon=new MiniGlyphView(this,type);icon.setTint(BROWN);int s=type==MiniGlyphView.PLAY?34:24;FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(dp(s),dp(s));lp.gravity=Gravity.CENTER;f.addView(icon,lp);f.setOnClickListener(click);return f;}
    private Button arcModeBtn(String label,Mode mode){Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextSize(14);b.setTypeface(Typeface.create(Typeface.SERIF,Typeface.BOLD));styleModeButton(b,mode==selectedMode);b.setOnClickListener(v->{setSelectedMode(mode);hideModes();});return b;}
    private Button miniModeBtn(String label,View.OnClickListener click){Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextSize(12);b.setPadding(dp(8),0,dp(8),0);b.setTypeface(Typeface.create(Typeface.SERIF,Typeface.BOLD));b.setOnClickListener(click);return b;}
    private void styleModeButton(Button b,boolean sel){GradientDrawableCompat.bg(b,sel?BROWN:GREEN,dp(22));b.setTextColor(CREAM);} private void styleMiniModeButton(Button b,boolean sel){GradientDrawableCompat.bg(b,sel?BROWN:GREEN,dp(18));b.setTextColor(CREAM);}
    private void placeMode(Button b,int gravity,int l,int t,int r,int bottom){b.setVisibility(View.GONE);FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(dp(78),dp(40));lp.gravity=gravity;lp.setMargins(l,t,r,bottom);orbZone.addView(b,lp);}
    private void toggleModes(){int vis=btnResume.getVisibility()==View.VISIBLE?View.GONE:View.VISIBLE;btnResume.setVisibility(vis);btnJob.setVisibility(vis);btnExam.setVisibility(vis);btnAudio.setVisibility(vis);} private void hideModes(){btnResume.setVisibility(View.GONE);btnJob.setVisibility(View.GONE);btnExam.setVisibility(View.GONE);btnAudio.setVisibility(View.GONE);} private void selectBox(ValueBoxView box){selectedBox=box;hourBox.setSelectedState(box==hourBox);minuteBox.setSelectedState(box==minuteBox);}
    private void setSelectedMode(Mode mode){selectedMode=mode;prefs().edit().putString("regular_mode",mode.name()).apply();styleModeButton(btnResume,mode==Mode.RESUME);styleModeButton(btnJob,mode==Mode.JOB);styleModeButton(btnExam,mode==Mode.EXAM);styleModeButton(btnAudio,mode==Mode.AUDIO);populateAppIcons(regularAppsRow,mode,false);} private Mode loadRegularMode(){try{return Mode.valueOf(prefs().getString("regular_mode",Mode.RESUME.name()));}catch(Exception e){return Mode.RESUME;}}
    private void startFocus(){int mins=hourBox.getValue()*60+minuteBox.getValue();if(mins<=0)mins=25;saveMemoryPreset(hourBox.getValue(),minuteBox.getValue());saveLockedPresetIfNeeded();LockState.startManual(this,selectedMode,mins);hideModes();refreshUi();if(!isAccessibilityEnabled()){Toast.makeText(this,"先开无障碍",Toast.LENGTH_SHORT).show();startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));}}
    private void refreshUi(){LockState.Session s=LockState.current(this);if(orb!=null)orb.setCenterText(s==null?"":LockState.timeLeftShort(s.end));populateAppIcons(regularAppsRow,selectedMode,false);updateTopTabs();updatePresetLockUi();updateWeekendLockUi();updateMemoryPopup();refreshWeekendCards();if(calendarPage.getVisibility()==View.VISIBLE)renderCalendar();}
    private void showMainPage(){mainPage.setVisibility(View.VISIBLE);calendarPage.setVisibility(View.GONE);refreshUi();} private void showCalendarPage(){mainPage.setVisibility(View.GONE);calendarPage.setVisibility(View.VISIBLE);renderCalendar();}
    private void showRegularPanel(boolean manual){if(manual)prefs().edit().putString("panel","regular").apply();regularPanel.setVisibility(View.VISIBLE);weekendPanel.setVisibility(View.GONE);updateTopTabs();} private void showWeekendPanel(boolean manual){if(manual)prefs().edit().putString("panel","weekend").apply();regularPanel.setVisibility(View.GONE);weekendPanel.setVisibility(View.VISIBLE);updateTopTabs();refreshWeekendCards();}
    private void autoChooseTopMode(){boolean locked=prefs().getBoolean("weekend_ui_lock",false);if(locked){showWeekendPanel(false);return;}Calendar c=Calendar.getInstance();int dow=c.get(Calendar.DAY_OF_WEEK);if(dow==Calendar.SATURDAY||dow==Calendar.SUNDAY)showWeekendPanel(false);else showRegularPanel(false);}
    private void updateTopTabs(){boolean regular=regularPanel.getVisibility()==View.VISIBLE;setTabState(treeTab,regular);setTabState(forestTab,!regular);} private void setTabState(View tab,boolean selected){GradientDrawableCompat.panel(tab,selected?LIGHT_GREEN:CREAM,selected?LIGHT_GREEN:PALE,dp(22),GREEN);float scale=selected?1.12f:1f;tab.setScaleX(scale);tab.setScaleY(scale);}
    private void updatePresetLockUi(){boolean locked=prefs().getBoolean("preset_lock",false);GradientDrawableCompat.panel(presetLockButton,locked?LIGHT_GREEN:CREAM,locked?LIGHT_GREEN:PALE,dp(22),GREEN);} private void togglePresetLock(){boolean now=!prefs().getBoolean("preset_lock",false);prefs().edit().putBoolean("preset_lock",now).apply();saveLockedPresetIfNeeded();updatePresetLockUi();} private void saveLockedPresetIfNeeded(){if(prefs().getBoolean("preset_lock",false))prefs().edit().putInt("locked_h",hourBox.getValue()).putInt("locked_m",minuteBox.getValue()).apply();} private void loadPreset(){if(prefs().getBoolean("preset_lock",false)){if(hourBox!=null)hourBox.setValue(prefs().getInt("locked_h",0));if(minuteBox!=null)minuteBox.setValue(prefs().getInt("locked_m",25));}}
    private void saveMemoryPreset(int h,int m){String value=h+":"+m;ArrayList<String> list=new ArrayList<>();list.add(value);for(int i=0;i<3;i++){String old=prefs().getString("mem_"+i,null);if(old!=null&&!old.equals(value)&&list.size()<3)list.add(old);}SharedPreferences.Editor ed=prefs().edit();for(int i=0;i<3;i++){if(i<list.size())ed.putString("mem_"+i,list.get(i));else ed.remove("mem_"+i);}ed.apply();}
    private void toggleMemoryPopup(){memoryPopup.setVisibility(memoryPopup.getVisibility()==View.VISIBLE?View.GONE:View.VISIBLE);updateMemoryPopup();} private void updateMemoryPopup(){memoryPopup.removeAllViews();for(int i=0;i<3;i++){final String raw=prefs().getString("mem_"+i,null);if(raw==null)continue;Button b=miniModeBtn(formatMemory(raw),v->applyMemory(raw));styleMiniModeButton(b,false);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,dp(34));if(memoryPopup.getChildCount()>0)lp.leftMargin=dp(6);memoryPopup.addView(b,lp);}if(memoryPopup.getChildCount()==0){Button b=miniModeBtn("0h 25m",v->applyMemory("0:25"));styleMiniModeButton(b,false);memoryPopup.addView(b,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,dp(34)));}}
    private String formatMemory(String raw){String[] sp=raw.split(":");if(sp.length!=2)return raw;return sp[0]+"h "+String.format(Locale.CHINA,"%02d",Integer.parseInt(sp[1]))+"m";} private void applyMemory(String raw){String[] sp=raw.split(":");if(sp.length!=2)return;hourBox.setValue(Integer.parseInt(sp[0]));minuteBox.setValue(Integer.parseInt(sp[1]));saveLockedPresetIfNeeded();memoryPopup.setVisibility(View.GONE);}
    private void toggleWeekendLock(){boolean now=!prefs().getBoolean("weekend_ui_lock",false);prefs().edit().putBoolean("weekend_ui_lock",now).apply();updateWeekendLockUi();} private void updateWeekendLockUi(){boolean locked=prefs().getBoolean("weekend_ui_lock",false);GradientDrawableCompat.panel(weekendLockButton,locked?LIGHT_GREEN:CREAM,locked?LIGHT_GREEN:PALE,dp(22),GREEN);}
    private void chooseWeekendSlot(int idx,Mode mode){LockState.setWeekendMode(this,idx,mode);refreshWeekendCards();openWhitelistSheetForMode(mode);} private void refreshWeekendCards(){for(int i=0;i<3;i++){Mode mode=LockState.weekendMode(this,i);populateAppIcons(weekendApps[i],mode,LockState.slotFinished(i));styleMiniModeButton(weekendModeButtons[i][0],mode==Mode.RESUME);styleMiniModeButton(weekendModeButtons[i][1],mode==Mode.JOB);styleMiniModeButton(weekendModeButtons[i][2],mode==Mode.EXAM);styleMiniModeButton(weekendModeButtons[i][3],mode==Mode.AUDIO);boolean fin=LockState.slotFinished(i);weekendCards[i].setAlpha(fin?0.45f:1f);weekendTimes[i].setText(LockState.slotStart(i)+"  ·  "+LockState.slotEnd(i)+(fin?"  ✓":""));}}
    private void populateAppIcons(LinearLayout row,Mode mode,boolean dim){if(row==null)return;row.removeAllViews();Set<String> unique=new LinkedHashSet<>(mode.allowed);if(unique.isEmpty()){Button b=miniModeBtn("电脑",v->{});styleMiniModeButton(b,false);b.setAlpha(dim?0.35f:1f);row.addView(b,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,dp(34)));return;}int shown=0;Set<Integer> shownTypes=new LinkedHashSet<>();for(String pkg:unique){int type=appType(pkg);if(shownTypes.contains(type))continue;shownTypes.add(type);View v=appLaunchIcon(pkg,dim);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(34),dp(34));if(shown++>0)lp.leftMargin=dp(6);row.addView(v,lp);}}
    private View appLaunchIcon(String pkg,boolean dim){int type=appType(pkg);FrameLayout f=new FrameLayout(this);GradientDrawableCompat.panel(f,CREAM,PALE,dp(16),GREEN);f.setAlpha(dim?0.35f:1f);MiniGlyphView icon=new MiniGlyphView(this,type);icon.setTint(BROWN);FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(dp(18),dp(18));lp.gravity=Gravity.CENTER;f.addView(icon,lp);f.setOnClickListener(v->launchPackage(pkg));return f;} private int appType(String pkg){if(pkg.contains("xhs"))return MiniGlyphView.APP_XHS;if(pkg.contains("fenbi"))return MiniGlyphView.APP_FENBI;if(pkg.contains("wps"))return MiniGlyphView.APP_WPS;return MiniGlyphView.APP_RECORD;} private void launchPackage(String pkg){Intent i=getPackageManager().getLaunchIntentForPackage(pkg);if(i!=null){i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);startActivity(i);}else Toast.makeText(this,"未安装",Toast.LENGTH_SHORT).show();}
    private void openSettingsSheet(){LinearLayout card=sheetCard();card.addView(t("设置",18,BROWN,true));LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER);LinearLayout.LayoutParams rlp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);rlp.topMargin=dp(12);card.addView(row,rlp);row.addView(settingCell(MiniGlyphView.ACCESS,"无障碍",v->startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))),new LinearLayout.LayoutParams(0,dp(88),1f));View alarm=settingCell(MiniGlyphView.ALARM,"闹钟",v->openAlarmSettings());LinearLayout.LayoutParams alp=new LinearLayout.LayoutParams(0,dp(88),1f);alp.leftMargin=dp(10);row.addView(alarm,alp);showSheet(card);} private void openAlarmSettings(){if(Build.VERSION.SDK_INT>=31){try{startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,Uri.parse("package:"+getPackageName())));}catch(Exception e){startActivity(new Intent(Settings.ACTION_SETTINGS));}}}
    private View settingCell(int iconType,String label,View.OnClickListener click){LinearLayout cell=new LinearLayout(this);cell.setOrientation(LinearLayout.VERTICAL);cell.setGravity(Gravity.CENTER);cell.setPadding(dp(8),dp(8),dp(8),dp(8));GradientDrawableCompat.panel(cell,CREAM,PALE,dp(24),GREEN);MiniGlyphView icon=new MiniGlyphView(this,iconType);icon.setTint(iconType==MiniGlyphView.ACCESS?Color.parseColor("#7350E6"):BROWN);cell.addView(icon,new LinearLayout.LayoutParams(dp(28),dp(28)));TextView tv=t(label,13,BROWN,false);LinearLayout.LayoutParams tlp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT);tlp.topMargin=dp(6);cell.addView(tv,tlp);cell.setOnClickListener(click);return cell;}
    private void openWhitelistSheet(){if(regularPanel.getVisibility()==View.VISIBLE)openWhitelistSheetForMode(selectedMode);else{int idx=LockState.currentWeekendSlot();if(idx<0)idx=0;openWhitelistSheetForMode(LockState.weekendMode(this,idx));}} private void openWhitelistSheetForMode(Mode mode){LinearLayout card=sheetCard();card.addView(t(mode.title,18,BROWN,true));whiteSheetContent=new LinearLayout(this);whiteSheetContent.setOrientation(LinearLayout.HORIZONTAL);whiteSheetContent.setGravity(Gravity.CENTER_HORIZONTAL);LinearLayout.LayoutParams wlp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT);wlp.topMargin=dp(14);card.addView(whiteSheetContent,wlp);populateAppIcons(whiteSheetContent,mode,false);showSheet(card);}
    private LinearLayout sheetCard(){LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(18),dp(18),dp(18),dp(18));GradientDrawableCompat.panel(card,CREAM,PALE,dp(28),GREEN);card.setClickable(true);return card;} private void showSheet(View card){overlaySheet.removeAllViews();FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(Math.min(dp(280),getResources().getDisplayMetrics().widthPixels-dp(36)),ViewGroup.LayoutParams.WRAP_CONTENT);lp.gravity=Gravity.CENTER;overlaySheet.addView(card,lp);overlaySheet.setVisibility(View.VISIBLE);card.setOnClickListener(v->{});} private void closeSheet(){overlaySheet.setVisibility(View.GONE);overlaySheet.removeAllViews();}
    private boolean isAccessibilityEnabled(){String enabled=Settings.Secure.getString(getContentResolver(),Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);return enabled!=null&&enabled.contains(getPackageName()+"/"+LockAccessibilityService.class.getName());}
    private void renderCalendar(){if(dayGrid==null||monthLabel==null)return;dayGrid.removeAllViews();Calendar c=Calendar.getInstance();int year=c.get(Calendar.YEAR),month=c.get(Calendar.MONTH);monthLabel.setText(String.format(Locale.CHINA,"%d · %02d",year,month+1));c.set(year,month,1);int blanks=(c.get(Calendar.DAY_OF_WEEK)+5)%7,max=c.getActualMaximum(Calendar.DAY_OF_MONTH),cell=dp(50);for(int i=0;i<blanks;i++)addBlank(cell);for(int day=1;day<=max;day++){long ms=StudyStats.millisFor(this,year,month,day);float fraction=Math.min(1f,ms/(8f*60f*60f*1000f));boolean marked=StudyStats.marked(this,year,month,day);CalendarDayView view=new CalendarDayView(this);view.setData(day,fraction,marked);final int d=day;view.setOnClickListener(v->{StudyStats.toggleMark(this,year,month,d);renderCalendar();});GridLayout.LayoutParams lp=new GridLayout.LayoutParams();lp.width=cell;lp.height=dp(66);lp.setMargins(dp(3),dp(4),dp(3),dp(4));dayGrid.addView(view,lp);}int cells=blanks+max;while(cells<42){addBlank(cell);cells++;}}
    private void addBlank(int cell){View v=new View(this);GridLayout.LayoutParams lp=new GridLayout.LayoutParams();lp.width=cell;lp.height=dp(66);lp.setMargins(dp(3),dp(4),dp(3),dp(4));dayGrid.addView(v,lp);}
    private TextView t(String s,int sp,int c,boolean bold){TextView v=new TextView(this);v.setText(s);v.setTextSize(sp);v.setTextColor(c);v.setTypeface(Typeface.create(Typeface.SERIF,bold?Typeface.BOLD:Typeface.NORMAL));return v;} private FrameLayout.LayoutParams full(){return new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT);} private int dp(float x){return (int)(x*getResources().getDisplayMetrics().density);} private SharedPreferences prefs(){return getSharedPreferences("ui_state",MODE_PRIVATE);} private void requestBasics(){if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},5);}
}
