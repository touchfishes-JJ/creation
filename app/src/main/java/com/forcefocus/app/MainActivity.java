package com.forcefocus.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.content.ComponentName;
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
import android.widget.TextView;
import android.widget.Toast;

import java.util.Calendar;
import java.util.Locale;

public class MainActivity extends Activity {
    private final int GREEN=Color.parseColor("#8AA832");
    private final int CREAM=Color.parseColor("#FFFBD3");
    private final int BROWN=Color.parseColor("#331915");
    private final int LIGHT=Color.parseColor("#F4F0C7");

    private FrameLayout root,focusPage,calendarPage,weekendPage;
    private FocusOrbView orb;
    private FrameLayout orbZone;
    private Button btnResume,btnJob,btnExam,btnAudio;
    private ValueBoxView hourBox,minuteBox;
    private Mode selectedMode=Mode.RESUME;
    private GridLayout dayGrid;
    private TextView monthHeader;
    private WeekendToggleView weekendToggle;
    private GestureDetector gestureDetector;
    private int page=0;

    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Runnable tick=new Runnable(){public void run(){StudyStats.tick(MainActivity.this);refreshOrb();handler.postDelayed(this,1000);}};

    @Override protected void onCreate(Bundle b){
        super.onCreate(b);
        gestureDetector=new GestureDetector(this,new SwipeListener());
        build();requestBasics();Scheduler.scheduleNext14Days(this);handler.post(tick);
    }
    @Override protected void onResume(){super.onResume();refreshOrb();renderCalendar();if(weekendToggle!=null)weekendToggle.setChecked(WeekendPrefs.enabled(this));}
    @Override protected void onDestroy(){super.onDestroy();handler.removeCallbacks(tick);}
    @Override public boolean dispatchTouchEvent(MotionEvent ev){gestureDetector.onTouchEvent(ev);return super.dispatchTouchEvent(ev);}

    private void build(){
        root=new FrameLayout(this);root.setBackgroundColor(CREAM);setContentView(root);
        buildFocusPage();buildCalendarPage();buildWeekendPage();showPage(0);
    }

    private void buildFocusPage(){
        focusPage=new FrameLayout(this);focusPage.setBackgroundColor(CREAM);root.addView(focusPage,full());

        orbZone=new FrameLayout(this);
        FrameLayout.LayoutParams zoneLp=new FrameLayout.LayoutParams(dp(340),dp(340));zoneLp.gravity=Gravity.CENTER;zoneLp.setMargins(0,0,0,dp(42));focusPage.addView(orbZone,zoneLp);

        orb=new FocusOrbView(this);orb.setFill(GREEN);orb.setCenterTextColor(CREAM);
        FrameLayout.LayoutParams orbLp=new FrameLayout.LayoutParams(dp(228),dp(228));orbLp.gravity=Gravity.CENTER;orbZone.addView(orb,orbLp);
        orb.setOnClickListener(v->toggleModes());
        orb.setLongPressListener(this::toggleModes);

        btnResume=modeBtn("简历");btnJob=modeBtn("岗位调研");btnExam=modeBtn("考公");btnAudio=modeBtn("磨耳朵");
        placeMode(btnResume,Gravity.TOP|Gravity.CENTER_HORIZONTAL,0,dp(6),0,0,Mode.RESUME);
        placeMode(btnAudio,Gravity.CENTER_VERTICAL|Gravity.START,dp(2),0,0,0,Mode.AUDIO);
        placeMode(btnJob,Gravity.CENTER_VERTICAL|Gravity.END,0,0,dp(2),0,Mode.JOB);
        placeMode(btnExam,Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL,0,0,0,dp(6),Mode.EXAM);
        hideModes();

        LinearLayout bottom=new LinearLayout(this);bottom.setOrientation(LinearLayout.HORIZONTAL);bottom.setGravity(Gravity.CENTER_VERTICAL);bottom.setPadding(dp(18),0,dp(18),dp(92));
        FrameLayout.LayoutParams bottomLp=new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);bottomLp.gravity=Gravity.BOTTOM;focusPage.addView(bottom,bottomLp);

        hourBox=new ValueBoxView(this);hourBox.setRange(0,4);hourBox.setValue(0);hourBox.setUnit("h");
        minuteBox=new ValueBoxView(this);minuteBox.setRange(0,59);minuteBox.setValue(25);minuteBox.setUnit("min");
        ValueBoxView.Listener valueListener=new ValueBoxView.Listener(){
            @Override public void onSelected(ValueBoxView view){selectBox(view);}
            @Override public void onValueChanged(ValueBoxView view,int value){}
        };
        hourBox.setListener(valueListener);minuteBox.setListener(valueListener);selectBox(hourBox);

        LinearLayout timeWrap=new LinearLayout(this);timeWrap.setOrientation(LinearLayout.HORIZONTAL);timeWrap.setGravity(Gravity.CENTER);timeWrap.setPadding(dp(8),dp(8),dp(8),dp(8));
        GradientDrawableCompat.panel(timeWrap,CREAM,CREAM,dp(28),BROWN);
        timeWrap.addView(hourBox,new LinearLayout.LayoutParams(dp(88),dp(84)));
        LinearLayout.LayoutParams minLp=new LinearLayout.LayoutParams(dp(88),dp(84));minLp.setMargins(dp(6),0,0,0);timeWrap.addView(minuteBox,minLp);
        bottom.addView(timeWrap,new LinearLayout.LayoutParams(0,dp(104),1f));

        PlayButtonView play=new PlayButtonView(this);play.setOnClickListener(v->startFocus());
        LinearLayout.LayoutParams playLp=new LinearLayout.LayoutParams(dp(104),dp(104));playLp.setMargins(dp(14),0,0,0);bottom.addView(play,playLp);
    }

    private void buildCalendarPage(){
        calendarPage=new FrameLayout(this);calendarPage.setBackgroundColor(CREAM);root.addView(calendarPage,full());
        LinearLayout center=new LinearLayout(this);center.setOrientation(LinearLayout.VERTICAL);center.setGravity(Gravity.CENTER_HORIZONTAL);FrameLayout.LayoutParams cp=new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT);cp.gravity=Gravity.CENTER;calendarPage.addView(center,cp);

        monthHeader=text("",22,BROWN,true);monthHeader.setGravity(Gravity.CENTER);center.addView(monthHeader,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(44)));
        LinearLayout weekdays=new LinearLayout(this);weekdays.setOrientation(LinearLayout.HORIZONTAL);weekdays.setGravity(Gravity.CENTER);
        String[] ws={"一","二","三","四","五","六","日"};
        int cell=calendarCell();
        for(String w:ws){TextView tv=text(w,13,BROWN,false);tv.setGravity(Gravity.CENTER);weekdays.addView(tv,new LinearLayout.LayoutParams(cell+dp(6),dp(30)));}
        center.addView(weekdays);

        dayGrid=new GridLayout(this);dayGrid.setColumnCount(7);dayGrid.setRowCount(6);center.addView(dayGrid,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT));
        renderCalendar();
    }

    private void buildWeekendPage(){
        weekendPage=new FrameLayout(this);weekendPage.setBackgroundColor(CREAM);root.addView(weekendPage,full());
        LinearLayout center=new LinearLayout(this);center.setOrientation(LinearLayout.VERTICAL);center.setGravity(Gravity.CENTER_HORIZONTAL);center.setPadding(dp(28),0,dp(28),0);
        FrameLayout.LayoutParams cp=new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);cp.gravity=Gravity.CENTER;weekendPage.addView(center,cp);

        TextView title=text("周末",26,BROWN,true);title.setGravity(Gravity.CENTER);center.addView(title,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(54)));
        weekendToggle=new WeekendToggleView(this);weekendToggle.setChecked(WeekendPrefs.enabled(this));
        LinearLayout.LayoutParams toggleLp=new LinearLayout.LayoutParams(dp(72),dp(38));toggleLp.gravity=Gravity.CENTER_HORIZONTAL;toggleLp.setMargins(0,dp(4),0,dp(28));center.addView(weekendToggle,toggleLp);
        weekendToggle.setOnClickListener(v->{boolean on=!weekendToggle.isChecked();weekendToggle.setChecked(on);WeekendPrefs.setEnabled(this,on);if(on)requestExactAlarmIfNeeded();});

        center.addView(scheduleCard("09:00", "11:30"),cardLp());
        center.addView(scheduleCard("13:30", "17:00"),cardLp());
        center.addView(scheduleCard("19:00", "21:30"),cardLp());
        TextView mode=text("考公",15,GREEN,true);mode.setGravity(Gravity.CENTER);LinearLayout.LayoutParams mlp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(34));mlp.setMargins(0,dp(10),0,0);center.addView(mode,mlp);
    }

    private LinearLayout scheduleCard(String start,String end){
        LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.HORIZONTAL);card.setGravity(Gravity.CENTER);card.setPadding(dp(18),0,dp(18),0);GradientDrawableCompat.bg(card,GREEN,dp(26));
        TextView a=text(start,23,CREAM,true);a.setGravity(Gravity.CENTER);TextView dash=text("—",18,CREAM,false);dash.setGravity(Gravity.CENTER);TextView b=text(end,23,CREAM,true);b.setGravity(Gravity.CENTER);
        card.addView(a,new LinearLayout.LayoutParams(0,dp(74),1));card.addView(dash,new LinearLayout.LayoutParams(dp(34),dp(74)));card.addView(b,new LinearLayout.LayoutParams(0,dp(74),1));return card;
    }
    private LinearLayout.LayoutParams cardLp(){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(74));lp.setMargins(0,0,0,dp(14));return lp;}

    private Button modeBtn(String label){Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextSize(14);b.setTextColor(CREAM);b.setTypeface(Typeface.create(Typeface.SERIF,Typeface.BOLD));GradientDrawableCompat.bg(b,BROWN,dp(24));return b;}
    private void placeMode(Button b,int gravity,int l,int t,int r,int bot,Mode mode){b.setVisibility(View.GONE);FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(dp(98),dp(48));lp.gravity=gravity;lp.setMargins(l,t,r,bot);orbZone.addView(b,lp);b.setOnClickListener(v->{selectedMode=mode;hideModes();orb.setCenterText(shortMode(mode));});}
    private String shortMode(Mode m){if(m==Mode.RESUME)return "简历";if(m==Mode.JOB)return "岗位";if(m==Mode.EXAM)return "考公";return "磨耳朵";}
    private void toggleModes(){boolean show=btnResume.getVisibility()!=View.VISIBLE;int v=show?View.VISIBLE:View.GONE;btnResume.setVisibility(v);btnJob.setVisibility(v);btnExam.setVisibility(v);btnAudio.setVisibility(v);if(show)orb.setCenterText("");}
    private void hideModes(){btnResume.setVisibility(View.GONE);btnJob.setVisibility(View.GONE);btnExam.setVisibility(View.GONE);btnAudio.setVisibility(View.GONE);}
    private void selectBox(ValueBoxView v){hourBox.setSelectedState(v==hourBox);minuteBox.setSelectedState(v==minuteBox);}

    private void startFocus(){
        if(!accessibilityEnabled()){Toast.makeText(this,"开启无障碍后再开始",Toast.LENGTH_SHORT).show();startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));return;}
        int mins=hourBox.getValue()*60+minuteBox.getValue();if(mins<=0)mins=25;LockState.startManual(this,selectedMode,mins);hideModes();refreshOrb();
    }
    private void refreshOrb(){if(orb==null)return;LockState.Session s=LockState.current(this);if(s==null){if(btnResume==null||btnResume.getVisibility()!=View.VISIBLE)orb.setCenterText("");}else orb.setCenterText(LockState.timeLeftShort(s.end));}

    private void renderCalendar(){
        if(dayGrid==null)return;dayGrid.removeAllViews();Calendar c=Calendar.getInstance();int year=c.get(Calendar.YEAR),month=c.get(Calendar.MONTH);monthHeader.setText(String.format(Locale.CHINA,"%04d  ·  %02d",year,month+1));c.set(year,month,1);int blanks=(c.get(Calendar.DAY_OF_WEEK)+5)%7,max=c.getActualMaximum(Calendar.DAY_OF_MONTH),cell=calendarCell();
        for(int i=0;i<blanks;i++)addBlank(cell);
        for(int day=1;day<=max;day++){long ms=StudyStats.millisFor(this,year,month,day);float f=Math.min(1f,ms/(8f*60f*60f*1000f));boolean mark=StudyStats.marked(this,year,month,day);CalendarDayView v=new CalendarDayView(this);v.setData(day,f,mark);final int d=day;v.setOnClickListener(x->{StudyStats.toggleMark(this,year,month,d);renderCalendar();});addCell(v,cell);}
        int cells=blanks+max;while(cells<42){addBlank(cell);cells++;}
    }
    private int calendarCell(){int width=getResources().getDisplayMetrics().widthPixels;int available=width-dp(34);return Math.min(dp(45),(available/7)-dp(6));}
    private void addBlank(int cell){View v=new View(this);addCell(v,cell);}
    private void addCell(View v,int cell){GridLayout.LayoutParams lp=new GridLayout.LayoutParams();lp.width=cell;lp.height=cell;lp.setMargins(dp(3),dp(3),dp(3),dp(3));dayGrid.addView(v,lp);}

    private void showPage(int p){page=Math.max(0,Math.min(2,p));focusPage.setVisibility(page==0?View.VISIBLE:View.GONE);calendarPage.setVisibility(page==1?View.VISIBLE:View.GONE);weekendPage.setVisibility(page==2?View.VISIBLE:View.GONE);if(page==1)renderCalendar();}

    private TextView text(String s,int sp,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(sp);t.setTextColor(color);t.setTypeface(Typeface.create(Typeface.SERIF,bold?Typeface.BOLD:Typeface.NORMAL));return t;}
    private FrameLayout.LayoutParams full(){return new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT);}
    private int dp(float x){return (int)(x*getResources().getDisplayMetrics().density);}
    private void requestBasics(){if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},5);}
    private boolean accessibilityEnabled(){String enabled=Settings.Secure.getString(getContentResolver(),Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);if(enabled==null)return false;ComponentName cn=new ComponentName(this,LockAccessibilityService.class);return enabled.toLowerCase(Locale.ROOT).contains(cn.flattenToString().toLowerCase(Locale.ROOT));}
    private void requestExactAlarmIfNeeded(){if(Build.VERSION.SDK_INT>=31){AlarmManager am=(AlarmManager)getSystemService(ALARM_SERVICE);if(am!=null&&!am.canScheduleExactAlarms()){try{startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:"+getPackageName())));}catch(Exception ignored){}}}}

    private final class SwipeListener extends GestureDetector.SimpleOnGestureListener{
        @Override public boolean onDown(MotionEvent e){return false;}
        @Override public boolean onFling(MotionEvent e1,MotionEvent e2,float vx,float vy){if(e1==null||e2==null)return false;float dx=e2.getX()-e1.getX(),dy=e2.getY()-e1.getY();if(Math.abs(dx)<dp(70)||Math.abs(dx)<Math.abs(dy))return false;showPage(page+(dx<0?1:-1));return true;}
    }
}
