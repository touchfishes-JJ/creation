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
import java.util.*;

public class MainActivity extends Activity {
    private final int GREEN=Color.parseColor("#8AA832");
    private final int CREAM=Color.parseColor("#FFFBD3");
    private final int BROWN=Color.parseColor("#331915");

    private FrameLayout root, focusPage;
    private LinearLayout calendarPage, radial;
    private FocusOrbView orb;
    private NumberPicker hourPicker, minutePicker;
    private Mode selectedMode=Mode.RESUME;
    private int shownYear, shownMonth;
    private GridLayout dayGrid;
    private TextView monthText;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Runnable tick=new Runnable(){ public void run(){ refreshOrb(); handler.postDelayed(this,1000); } };

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        Calendar now=Calendar.getInstance(); shownYear=now.get(Calendar.YEAR); shownMonth=now.get(Calendar.MONTH);
        build(); requestBasics(); Scheduler.scheduleNext14Days(this); handler.post(tick);
    }
    @Override protected void onDestroy(){super.onDestroy();handler.removeCallbacks(tick);}
    @Override protected void onResume(){super.onResume();refreshOrb();renderCalendar();}

    private void build(){
        root=new FrameLayout(this); root.setBackgroundColor(CREAM); setContentView(root);
        buildFocusPage(); buildCalendarPage(); showFocus();
    }

    private void buildFocusPage(){
        focusPage=new FrameLayout(this); focusPage.setBackgroundColor(CREAM); root.addView(focusPage,full());

        IconButtonView access=new IconButtonView(this,IconButtonView.ACCESS); access.setColors(BROWN,Color.TRANSPARENT);
        FrameLayout.LayoutParams aLp=new FrameLayout.LayoutParams(dp(48),dp(48));aLp.gravity=Gravity.TOP|Gravity.START;aLp.setMargins(dp(18),dp(18),0,0);focusPage.addView(access,aLp);
        access.setOnClickListener(v->startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));

        IconButtonView alarm=new IconButtonView(this,IconButtonView.ALARM); alarm.setColors(BROWN,Color.TRANSPARENT);
        FrameLayout.LayoutParams alLp=new FrameLayout.LayoutParams(dp(48),dp(48));alLp.gravity=Gravity.TOP|Gravity.END;alLp.setMargins(0,dp(18),dp(18),0);focusPage.addView(alarm,alLp);
        alarm.setOnClickListener(v->{if(Build.VERSION.SDK_INT>=31){try{startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:"+getPackageName())));}catch(Exception e){startActivity(new Intent(Settings.ACTION_SETTINGS));}}});

        orb=new FocusOrbView(this); orb.setFill(GREEN); orb.setCenterTextColor(CREAM);
        FrameLayout.LayoutParams orbLp=new FrameLayout.LayoutParams(dp(250),dp(250));orbLp.gravity=Gravity.CENTER;orbLp.setMargins(0,0,0,dp(36));focusPage.addView(orb,orbLp);
        orb.setLongPressListener(this::showRadial);

        radial=new LinearLayout(this); radial.setOrientation(LinearLayout.VERTICAL); radial.setGravity(Gravity.CENTER); radial.setVisibility(View.GONE);
        FrameLayout.LayoutParams rlp=new FrameLayout.LayoutParams(dp(360),dp(360));rlp.gravity=Gravity.CENTER;rlp.setMargins(0,0,0,dp(36));focusPage.addView(radial,rlp);
        buildRadialMenu();

        LinearLayout bottom=new LinearLayout(this);bottom.setOrientation(LinearLayout.VERTICAL);bottom.setPadding(dp(18),0,dp(18),dp(12));
        FrameLayout.LayoutParams bLp=new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);bLp.gravity=Gravity.BOTTOM;focusPage.addView(bottom,bLp);

        LinearLayout controls=new LinearLayout(this);controls.setOrientation(LinearLayout.HORIZONTAL);controls.setGravity(Gravity.CENTER_VERTICAL);
        hourPicker=picker(0,12,0);minutePicker=picker(0,59,25);
        controls.addView(hourPicker,new LinearLayout.LayoutParams(dp(56),dp(66)));
        controls.addView(unit("h"),new LinearLayout.LayoutParams(dp(22),dp(48)));
        controls.addView(minutePicker,new LinearLayout.LayoutParams(dp(56),dp(66)));
        controls.addView(unit("min"),new LinearLayout.LayoutParams(dp(42),dp(48)));
        Space space=new Space(this);controls.addView(space,new LinearLayout.LayoutParams(0,1,1));
        IconButtonView start=new IconButtonView(this,IconButtonView.PLAY);start.setColors(CREAM,GREEN);controls.addView(start,new LinearLayout.LayoutParams(dp(58),dp(58)));
        start.setOnClickListener(v->startFocus());
        bottom.addView(controls,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));

        bottom.addView(navBar(true),new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(52)));
    }

    private void buildRadialMenu(){
        radial.removeAllViews();
        Space topSpace=new Space(this);radial.addView(topSpace,new LinearLayout.LayoutParams(1,dp(6)));
        Button top=modeButton("简历",Mode.RESUME);radial.addView(top,centerWrap());
        LinearLayout mid=new LinearLayout(this);mid.setOrientation(LinearLayout.HORIZONTAL);mid.setGravity(Gravity.CENTER_VERTICAL);
        Button left=modeButton("磨耳朵",Mode.AUDIO);Button right=modeButton("岗位调研",Mode.JOB);
        mid.addView(left,new LinearLayout.LayoutParams(dp(105),dp(52)));
        Space mspace=new Space(this);mid.addView(mspace,new LinearLayout.LayoutParams(dp(142),1));
        mid.addView(right,new LinearLayout.LayoutParams(dp(105),dp(52)));
        radial.addView(mid,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(160)));
        Button bottom=modeButton("考公",Mode.EXAM);radial.addView(bottom,centerWrap());
        radial.bringToFront();
    }

    private Button modeButton(String text,Mode mode){
        Button b=new Button(this);b.setText(text);b.setTextSize(15);b.setAllCaps(false);b.setTypeface(Typeface.create(Typeface.SERIF,Typeface.BOLD));b.setTextColor(CREAM);b.setPadding(dp(8),0,dp(8),0);GradientDrawableCompat.bg(b,GREEN,dp(26));
        b.setOnClickListener(v->{selectedMode=mode;radial.setVisibility(View.GONE);orb.setVisibility(View.VISIBLE);});return b;
    }
    private LinearLayout.LayoutParams centerWrap(){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(105),dp(52));lp.gravity=Gravity.CENTER_HORIZONTAL;return lp;}
    private void showRadial(){orb.setVisibility(View.VISIBLE);radial.setVisibility(View.VISIBLE);radial.bringToFront();}

    private void startFocus(){
        int mins=hourPicker.getValue()*60+minutePicker.getValue();if(mins<=0)mins=25;
        LockState.startManual(this,selectedMode,mins);radial.setVisibility(View.GONE);refreshOrb();
    }
    private void refreshOrb(){
        if(orb==null)return;LockState.Session s=LockState.current(this);
        if(s==null){orb.setCenterText("");}
        else{orb.setCenterText(LockState.timeLeftShort(s.end));}
    }

    private void buildCalendarPage(){
        calendarPage=new LinearLayout(this);calendarPage.setOrientation(LinearLayout.VERTICAL);calendarPage.setPadding(dp(16),dp(18),dp(16),dp(12));calendarPage.setBackgroundColor(CREAM);root.addView(calendarPage,full());
        LinearLayout head=new LinearLayout(this);head.setOrientation(LinearLayout.HORIZONTAL);head.setGravity(Gravity.CENTER_VERTICAL);
        Button prev=arrow("‹");Button next=arrow("›");monthText=new TextView(this);monthText.setTextColor(BROWN);monthText.setTextSize(18);monthText.setGravity(Gravity.CENTER);monthText.setTypeface(Typeface.create(Typeface.SERIF,Typeface.BOLD));
        head.addView(prev,new LinearLayout.LayoutParams(dp(48),dp(48)));head.addView(monthText,new LinearLayout.LayoutParams(0,dp(48),1));head.addView(next,new LinearLayout.LayoutParams(dp(48),dp(48)));calendarPage.addView(head);
        prev.setOnClickListener(v->moveMonth(-1));next.setOnClickListener(v->moveMonth(1));

        dayGrid=new GridLayout(this);dayGrid.setColumnCount(7);dayGrid.setUseDefaultMargins(false);calendarPage.addView(dayGrid,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,1));
        calendarPage.addView(navBar(false),new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(52)));
        renderCalendar();
    }

    private void renderCalendar(){
        if(dayGrid==null)return;dayGrid.removeAllViews();monthText.setText(String.format(Locale.CHINA,"%04d·%02d",shownYear,shownMonth+1));
        Calendar c=Calendar.getInstance();c.set(shownYear,shownMonth,1);int firstDow=c.get(Calendar.DAY_OF_WEEK);int blanks=(firstDow+5)%7;int max=c.getActualMaximum(Calendar.DAY_OF_MONTH);
        for(int i=0;i<blanks;i++)addBlank();
        for(int day=1;day<=max;day++){
            long ms=StudyStats.millisFor(this,shownYear,shownMonth,day);float fraction=Math.min(1f,ms/(8f*60f*60f*1000f));boolean marked=StudyStats.marked(this,shownYear,shownMonth,day);
            CalendarDayView v=new CalendarDayView(this);v.setData(day,fraction,marked);final int d=day;v.setOnClickListener(x->{StudyStats.toggleMark(this,shownYear,shownMonth,d);renderCalendar();});addDay(v);
        }
        int cells=blanks+max;while(cells%7!=0){addBlank();cells++;}
    }
    private void addBlank(){View v=new View(this);addDay(v);}
    private void addDay(View v){GridLayout.LayoutParams lp=new GridLayout.LayoutParams();lp.width=0;lp.height=0;lp.columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1,1f);lp.rowSpec=GridLayout.spec(GridLayout.UNDEFINED,1,1f);lp.setMargins(dp(3),dp(3),dp(3),dp(3));dayGrid.addView(v,lp);}
    private void moveMonth(int d){shownMonth+=d;if(shownMonth<0){shownMonth=11;shownYear--;}if(shownMonth>11){shownMonth=0;shownYear++;}renderCalendar();}

    private LinearLayout navBar(boolean focusActive){
        LinearLayout nav=new LinearLayout(this);nav.setOrientation(LinearLayout.HORIZONTAL);nav.setGravity(Gravity.CENTER);
        IconButtonView f=new IconButtonView(this,IconButtonView.FOCUS);f.setColors(focusActive?CREAM:BROWN,focusActive?GREEN:Color.TRANSPARENT);
        IconButtonView cal=new IconButtonView(this,IconButtonView.CALENDAR);cal.setColors(!focusActive?CREAM:BROWN,!focusActive?GREEN:Color.TRANSPARENT);
        f.setOnClickListener(v->showFocus());cal.setOnClickListener(v->showCalendar());nav.addView(f);nav.addView(cal);return nav;
    }
    private void showFocus(){focusPage.setVisibility(View.VISIBLE);calendarPage.setVisibility(View.GONE);}
    private void showCalendar(){focusPage.setVisibility(View.GONE);calendarPage.setVisibility(View.VISIBLE);renderCalendar();}

    private NumberPicker picker(int min,int max,int value){NumberPicker p=new NumberPicker(this);p.setMinValue(min);p.setMaxValue(max);p.setValue(value);p.setWrapSelectorWheel(true);p.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);return p;}
    private TextView unit(String s){TextView t=new TextView(this);t.setText(s);t.setTextColor(BROWN);t.setTextSize(18);t.setGravity(Gravity.CENTER);t.setTypeface(Typeface.create(Typeface.SERIF,Typeface.NORMAL));return t;}
    private Button arrow(String s){Button b=new Button(this);b.setText(s);b.setTextSize(28);b.setTextColor(BROWN);b.setBackgroundColor(Color.TRANSPARENT);b.setAllCaps(false);return b;}
    private FrameLayout.LayoutParams full(){return new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT);}
    private int dp(float x){return (int)(x*getResources().getDisplayMetrics().density);}
    private void requestBasics(){if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},5);}
}
