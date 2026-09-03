package com.forcefocus.app;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import java.util.*;

public class FinalActivity extends Activity {
    static final int GREEN=Color.parseColor("#788F45"), CREAM=Color.parseColor("#FFFBD3"), BROWN=Color.parseColor("#331915"), PALE=Color.parseColor("#F6F0D5"), LINE=Color.parseColor("#D7CBA1");
    static final int FOCUS=0,CALENDAR=1,REGULAR=0,WEEKEND=1;
    FrameLayout root,focusPage,calendarPage,running,drawerShade,sheetShade,body;
    LinearLayout regular,weekend,regularApps,drawer,sheet,runTools;
    ImageView tree,forest;
    AncientLockView weekendLock;
    CircleView circle,runCircle;
    Button[] fan=new Button[4]; Mode chosen=Mode.RESUME; int page=FOCUS,sub=REGULAR;
    TimeWheel hour,minute; MiniIcon timeLock,memory;
    LinearLayout memoryRow; GridLayout grid; TextView month;
    TextView[] slotTime=new TextView[3]; View[] slotWork=new View[3],slotWhite=new View[3];
    GestureDetector gestures; Handler h=new Handler(Looper.getMainLooper());
    Runnable tick=new Runnable(){public void run(){refresh();h.postDelayed(this,1000);}};

    @Override protected void onCreate(Bundle b){super.onCreate(b);gestures=new GestureDetector(this,new Swipe());build();chosen=loadMode();loadTime();autoSub();requestNotify();Scheduler.scheduleNext14Days(this);h.post(tick);}
    @Override protected void onResume(){super.onResume();autoSub();refresh();}
    @Override protected void onDestroy(){super.onDestroy();h.removeCallbacks(tick);}
    @Override public boolean dispatchTouchEvent(MotionEvent e){gestures.onTouchEvent(e);return super.dispatchTouchEvent(e);}

    void build(){root=new FrameLayout(this);root.setBackgroundColor(CREAM);setContentView(root);buildFocus();buildCalendar();buildDrawer();buildSheet();showPage(FOCUS);}

    void buildFocus(){
        focusPage=new FrameLayout(this);focusPage.setBackgroundColor(CREAM);root.addView(focusPage,full());
        LinearLayout tabs=new LinearLayout(this);tabs.setOrientation(LinearLayout.HORIZONTAL);tabs.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams tlp=new FrameLayout.LayoutParams(-1,dp(90));tlp.gravity=Gravity.TOP;tlp.topMargin=dp(78);focusPage.addView(tabs,tlp);
        tree=modeImage(R.drawable.tree_mode,REGULAR);forest=modeImage(R.drawable.forest_mode,WEEKEND);
        tabs.addView(tree,new LinearLayout.LayoutParams(dp(96),dp(84)));tabs.addView(new Space(this),new LinearLayout.LayoutParams(dp(26),1));tabs.addView(forest,new LinearLayout.LayoutParams(dp(96),dp(84)));
        body=new FrameLayout(this);FrameLayout.LayoutParams blp=full();blp.topMargin=dp(158);focusPage.addView(body,blp);
        buildRegular();buildWeekend();body.addView(regular,full());body.addView(weekend,full());
        View edge=new View(this);final float[] x={0};edge.setOnTouchListener((v,e)->{if(e.getAction()==0){x[0]=e.getX();return true;}if(e.getAction()==1&&e.getX()-x[0]>dp(18)){drawerShade.setVisibility(View.VISIBLE);return true;}return true;});FrameLayout.LayoutParams elp=new FrameLayout.LayoutParams(dp(26),-1);elp.gravity=Gravity.START;focusPage.addView(edge,elp);
        buildRunning();
    }

    void buildRegular(){
        regular=new LinearLayout(this);regular.setOrientation(LinearLayout.VERTICAL);regular.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL);regular.setPadding(dp(18),0,dp(18),dp(42));
        FrameLayout zone=new FrameLayout(this);regular.addView(zone,new LinearLayout.LayoutParams(dp(300),dp(294)));
        circle=new CircleView(this);FrameLayout.LayoutParams clp=new FrameLayout.LayoutParams(dp(220),dp(220));clp.gravity=Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL;zone.addView(circle,clp);circle.setOnClickListener(v->toggleFan());
        fan[0]=fanBtn("简历",Mode.RESUME);fan[1]=fanBtn("岗位",Mode.JOB);fan[2]=fanBtn("考公",Mode.EXAM);fan[3]=fanBtn("磨耳",Mode.AUDIO);
        addFan(zone,fan[0],18,28,-17);addFan(zone,fan[1],76,9,-7);addFan(zone,fan[2],162,9,7);addFan(zone,fan[3],220,28,17);hideFan();
        regularApps=new LinearLayout(this);regularApps.setOrientation(LinearLayout.HORIZONTAL);regularApps.setGravity(Gravity.CENTER);regular.addView(regularApps,new LinearLayout.LayoutParams(-2,dp(38)));
        regular.addView(new Space(this),new LinearLayout.LayoutParams(1,0,1));
        LinearLayout bottom=new LinearLayout(this);bottom.setOrientation(LinearLayout.VERTICAL);bottom.setGravity(Gravity.CENTER);regular.addView(bottom,new LinearLayout.LayoutParams(-1,-2));
        LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER);bottom.addView(row,new LinearLayout.LayoutParams(-1,dp(104)));
        LinearLayout clock=new LinearLayout(this);clock.setOrientation(LinearLayout.HORIZONTAL);clock.setGravity(Gravity.CENTER);clock.setPadding(dp(8),dp(8),dp(8),dp(8));GradientDrawableCompat.panel(clock,CREAM,CREAM,dp(22),LINE);
        hour=new TimeWheel(this,4,"h",1);minute=new TimeWheel(this,59,"min",0);clock.addView(hour,new LinearLayout.LayoutParams(0,dp(70),1));clock.addView(minute,new LinearLayout.LayoutParams(0,dp(70),1));
        row.addView(clock,new LinearLayout.LayoutParams(0,dp(104),1.7f));View play=playButton();LinearLayout.LayoutParams plp=new LinearLayout.LayoutParams(0,dp(104),.9f);plp.leftMargin=dp(12);row.addView(play,plp);
        LinearLayout tiny=new LinearLayout(this);tiny.setGravity(Gravity.CENTER);bottom.addView(tiny,new LinearLayout.LayoutParams((int)(getResources().getDisplayMetrics().widthPixels*.49f),dp(28)));
        memory=new MiniIcon(this,MiniIcon.MEMORY,BROWN);timeLock=new MiniIcon(this,MiniIcon.LOCK,BROWN);memory.setOnClickListener(v->toggleMemory());timeLock.setOnClickListener(v->toggleTimeLock());tiny.addView(memory,new LinearLayout.LayoutParams(dp(20),dp(20)));tiny.addView(new Space(this),new LinearLayout.LayoutParams(0,1,1));tiny.addView(timeLock,new LinearLayout.LayoutParams(dp(20),dp(20)));
        memoryRow=new LinearLayout(this);memoryRow.setGravity(Gravity.CENTER);memoryRow.setVisibility(View.GONE);bottom.addView(memoryRow,new LinearLayout.LayoutParams(-2,dp(32)));
    }

    void buildWeekend(){
        weekend=new LinearLayout(this);weekend.setOrientation(LinearLayout.VERTICAL);weekend.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL);weekend.setPadding(dp(26),0,dp(26),dp(32));
        weekendLock=new AncientLockView(this);weekendLock.setOnClickListener(v->{prefs().edit().putBoolean("weekend_locked",!prefs().getBoolean("weekend_locked",false)).apply();refresh();});LinearLayout.LayoutParams llp=new LinearLayout.LayoutParams(dp(62),dp(62));llp.bottomMargin=dp(8);weekend.addView(weekendLock,llp);
        for(int i=0;i<3;i++){final int idx=i;LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.HORIZONTAL);card.setGravity(Gravity.CENTER_VERTICAL);card.setPadding(dp(18),dp(10),dp(12),dp(10));GradientDrawableCompat.bg(card,GREEN,dp(20));LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,dp(92));cp.bottomMargin=dp(16);weekend.addView(card,cp);
            slotTime[i]=text(LockState.slotStart(i)+" – "+LockState.slotEnd(i),22,CREAM,true);slotTime[i].setGravity(Gravity.CENTER_VERTICAL);card.addView(slotTime[i],new LinearLayout.LayoutParams(0,-1,1));LinearLayout right=new LinearLayout(this);right.setOrientation(LinearLayout.VERTICAL);right.setGravity(Gravity.CENTER);card.addView(right,new LinearLayout.LayoutParams(dp(34),-1));
            slotWork[i]=tiny(MiniIcon.MODE,CREAM,v->modePicker(idx));right.addView(slotWork[i],new LinearLayout.LayoutParams(dp(24),dp(24)));LinearLayout.LayoutParams wp=new LinearLayout.LayoutParams(dp(24),dp(24));wp.topMargin=dp(7);slotWhite[i]=tiny(MiniIcon.GRID,CREAM,v->whitelist(LockState.weekendMode(this,idx)));right.addView(slotWhite[i],wp);
        }
    }

    void buildRunning(){
        running=new FrameLayout(this);running.setBackgroundColor(CREAM);running.setVisibility(View.GONE);focusPage.addView(running,full());runCircle=new CircleView(this);FrameLayout.LayoutParams rp=new FrameLayout.LayoutParams(dp(220),dp(220));rp.gravity=Gravity.CENTER;running.addView(runCircle,rp);
        runTools=new LinearLayout(this);runTools.setGravity(Gravity.CENTER);runTools.setPadding(dp(10),dp(7),dp(10),dp(7));GradientDrawableCompat.panel(runTools,CREAM,CREAM,dp(16),LINE);FrameLayout.LayoutParams rtp=new FrameLayout.LayoutParams(-2,dp(50));rtp.gravity=Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL;rtp.bottomMargin=dp(52);running.addView(runTools,rtp);
    }

    void buildCalendar(){
        calendarPage=new FrameLayout(this);calendarPage.setBackgroundColor(CREAM);root.addView(calendarPage,full());LinearLayout wrap=new LinearLayout(this);wrap.setOrientation(LinearLayout.VERTICAL);wrap.setGravity(Gravity.CENTER);calendarPage.addView(wrap,full());month=text("",19,BROWN,true);month.setGravity(Gravity.CENTER);wrap.addView(month,new LinearLayout.LayoutParams(-1,dp(34)));
        LinearLayout week=new LinearLayout(this);week.setGravity(Gravity.CENTER);wrap.addView(week,new LinearLayout.LayoutParams(-2,dp(28)));String[] n={"一","二","三","四","五","六","日"};for(String s:n){TextView t=text(s,11,BROWN,false);t.setGravity(Gravity.CENTER);week.addView(t,new LinearLayout.LayoutParams(dp(42),dp(28)));}
        grid=new GridLayout(this);grid.setColumnCount(7);grid.setRowCount(6);LinearLayout.LayoutParams gp=new LinearLayout.LayoutParams(-2,-2);gp.topMargin=dp(6);wrap.addView(grid,gp);renderCalendar();
    }

    void buildDrawer(){
        drawerShade=new FrameLayout(this);drawerShade.setBackgroundColor(Color.parseColor("#66000000"));drawerShade.setVisibility(View.GONE);root.addView(drawerShade,full());drawerShade.setOnClickListener(v->drawerShade.setVisibility(View.GONE));drawer=new LinearLayout(this);drawer.setOrientation(LinearLayout.VERTICAL);drawer.setPadding(dp(28),dp(78),dp(18),dp(18));drawer.setBackgroundColor(CREAM);drawer.setOnClickListener(v->{});FrameLayout.LayoutParams dpv=new FrameLayout.LayoutParams((int)(getResources().getDisplayMetrics().widthPixels*.44f),-1);dpv.gravity=Gravity.START;drawerShade.addView(drawer,dpv);
        drawer.addView(drawerBtn("设置",v->{drawerShade.setVisibility(View.GONE);settings();}),new LinearLayout.LayoutParams(-1,dp(54)));LinearLayout.LayoutParams wp=new LinearLayout.LayoutParams(-1,dp(54));wp.topMargin=dp(12);drawer.addView(drawerBtn("白名单",v->{drawerShade.setVisibility(View.GONE);whitelist(sub==REGULAR?chosen:LockState.weekendMode(this,Math.max(0,LockState.currentWeekendSlot())));}),wp);
    }

    void buildSheet(){sheetShade=new FrameLayout(this);sheetShade.setBackgroundColor(Color.parseColor("#66000000"));sheetShade.setVisibility(View.GONE);root.addView(sheetShade,full());sheetShade.setOnClickListener(v->sheetShade.setVisibility(View.GONE));sheet=new LinearLayout(this);sheet.setGravity(Gravity.CENTER);sheet.setPadding(dp(16),dp(16),dp(16),dp(16));GradientDrawableCompat.panel(sheet,CREAM,PALE,dp(24),LINE);sheet.setOnClickListener(v->{});FrameLayout.LayoutParams sp=new FrameLayout.LayoutParams((int)(getResources().getDisplayMetrics().widthPixels*.72f),-2);sp.gravity=Gravity.CENTER;sheetShade.addView(sheet,sp);}

    void refresh(){
        tree.setScaleX(sub==REGULAR?1f:.52f);tree.setScaleY(sub==REGULAR?1f:.52f);tree.setAlpha(sub==REGULAR?1f:.7f);forest.setScaleX(sub==WEEKEND?1f:.52f);forest.setScaleY(sub==WEEKEND?1f:.52f);forest.setAlpha(sub==WEEKEND?1f:.7f);regular.setVisibility(sub==REGULAR?View.VISIBLE:View.GONE);weekend.setVisibility(sub==WEEKEND?View.VISIBLE:View.GONE);
        fillApps(regularApps,chosen);timeLock.setTint(prefs().getBoolean("time_locked",false)?GREEN:BROWN);weekendLock.setLocked(prefs().getBoolean("weekend_locked",false));
        for(int i=0;i<3;i++){boolean done=LockState.slotFinished(i);slotTime[i].setAlpha(done?.4f:1);slotWork[i].setAlpha(done?.4f:1);slotWhite[i].setAlpha(done?.4f:1);}
        LockState.Session s=LockState.current(this);if(page==FOCUS&&s!=null){running.setVisibility(View.VISIBLE);runCircle.setText(LockState.timeLeftShort(s.end));runTools.removeAllViews();fillApps(runTools,s.mode);View stop=tiny(MiniIcon.STOP,GREEN,v->{if(LockState.useEscape(this,s.end)){Toast.makeText(this,"剩余 "+LockState.escapesLeft(this)+" 次",Toast.LENGTH_SHORT).show();refresh();}else Toast.makeText(this,"本周已用完",Toast.LENGTH_SHORT).show();});LinearLayout.LayoutParams st=new LinearLayout.LayoutParams(dp(30),dp(30));if(runTools.getChildCount()>0)st.leftMargin=dp(9);runTools.addView(stop,st);}else running.setVisibility(View.GONE);if(page==CALENDAR)renderCalendar();
    }

    void renderCalendar(){if(grid==null)return;grid.removeAllViews();Calendar c=Calendar.getInstance();int y=c.get(Calendar.YEAR),m=c.get(Calendar.MONTH);month.setText(y+"年"+(m+1)+"月");c.set(y,m,1);int blanks=(c.get(Calendar.DAY_OF_WEEK)+5)%7,max=c.getActualMaximum(Calendar.DAY_OF_MONTH);for(int i=0;i<blanks;i++)blank();for(int d=1;d<=max;d++){long ms=StudyStats.millisFor(this,y,m,d);float f=Math.min(1f,ms/(8f*3600000f));boolean mark=StudyStats.marked(this,y,m,d);DayCell v=new DayCell(this,d,f,mark);final int day=d;v.setOnClickListener(x->{StudyStats.toggleMark(this,y,m,day);renderCalendar();});GridLayout.LayoutParams lp=new GridLayout.LayoutParams();lp.width=dp(42);lp.height=dp(58);lp.setMargins(dp(2),dp(2),dp(2),dp(2));grid.addView(v,lp);}int total=blanks+max;while(total++<42)blank();}
    void blank(){View v=new View(this);GridLayout.LayoutParams lp=new GridLayout.LayoutParams();lp.width=dp(42);lp.height=dp(58);lp.setMargins(dp(2),dp(2),dp(2),dp(2));grid.addView(v,lp);}

    void autoSub(){if(prefs().getBoolean("weekend_locked",false)){switchSub(WEEKEND);return;}int d=Calendar.getInstance().get(Calendar.DAY_OF_WEEK);switchSub(d==Calendar.SATURDAY||d==Calendar.SUNDAY?WEEKEND:REGULAR);}
    void switchSub(int s){sub=s;refresh();} void showPage(int p){page=p;focusPage.setVisibility(p==FOCUS?View.VISIBLE:View.GONE);calendarPage.setVisibility(p==CALENDAR?View.VISIBLE:View.GONE);refresh();}
    void toggleFan(){int x=fan[0].getVisibility()==View.VISIBLE?View.GONE:View.VISIBLE;for(Button b:fan)b.setVisibility(x);} void hideFan(){for(Button b:fan)b.setVisibility(View.GONE);}

    ImageView modeImage(int res,int target){ImageView i=new ImageView(this);i.setImageResource(res);i.setScaleType(ImageView.ScaleType.FIT_CENTER);i.setOnClickListener(v->switchSub(target));return i;}
    Button fanBtn(String s,Mode m){Button b=new Button(this);b.setAllCaps(false);b.setText(s);b.setTextSize(11);b.setTextColor(BROWN);b.setTypeface(Typeface.create(Typeface.SERIF,Typeface.BOLD));GradientDrawableCompat.panel(b,CREAM,PALE,dp(13),LINE);b.setOnClickListener(v->{chosen=m;prefs().edit().putString("regular_mode",m.name()).apply();hideFan();refresh();});return b;}
    void addFan(FrameLayout z,View v,int l,int t,float r){FrameLayout.LayoutParams p=new FrameLayout.LayoutParams(dp(62),dp(34));p.leftMargin=dp(l);p.topMargin=dp(t);z.addView(v,p);v.setRotation(r);}
    View playButton(){FrameLayout f=new FrameLayout(this);GradientDrawableCompat.bg(f,GREEN,dp(22));MiniIcon i=new MiniIcon(this,MiniIcon.PLAY,CREAM);FrameLayout.LayoutParams p=new FrameLayout.LayoutParams(dp(46),dp(46));p.gravity=Gravity.CENTER;f.addView(i,p);f.setOnClickListener(v->startFocus());return f;}
    View tiny(int type,int color,View.OnClickListener l){FrameLayout f=new FrameLayout(this);MiniIcon i=new MiniIcon(this,type,color);FrameLayout.LayoutParams p=new FrameLayout.LayoutParams(dp(22),dp(22));p.gravity=Gravity.CENTER;f.addView(i,p);f.setOnClickListener(l);return f;}

    void startFocus(){int mins=hour.value*60+minute.value;if(mins<=0)mins=25;saveMemory(hour.value,minute.value);saveTime();LockState.startManual(this,chosen,mins);if(!accessibility())startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));refresh();}
    void toggleTimeLock(){prefs().edit().putBoolean("time_locked",!prefs().getBoolean("time_locked",false)).apply();saveTime();refresh();}
    void saveTime(){if(prefs().getBoolean("time_locked",false))prefs().edit().putInt("lh",hour.value).putInt("lm",minute.value).apply();}
    void loadTime(){if(prefs().getBoolean("time_locked",false)){hour.value=prefs().getInt("lh",1);minute.value=prefs().getInt("lm",0);}}
    void saveMemory(int a,int b){String now=a+":"+b;ArrayList<String> list=new ArrayList<>();list.add(now);for(int i=0;i<3;i++){String q=prefs().getString("m"+i,null);if(q!=null&&!q.equals(now)&&list.size()<3)list.add(q);}SharedPreferences.Editor e=prefs().edit();for(int i=0;i<3;i++)e.putString("m"+i,i<list.size()?list.get(i):null);e.apply();}
    void toggleMemory(){memoryRow.setVisibility(memoryRow.getVisibility()==View.VISIBLE?View.GONE:View.VISIBLE);memoryRow.removeAllViews();if(memoryRow.getVisibility()!=View.VISIBLE)return;for(int i=0;i<3;i++){String q=prefs().getString("m"+i,null);if(q==null)continue;Button b=new Button(this);b.setAllCaps(false);b.setText(q);b.setTextSize(10);b.setTextColor(BROWN);GradientDrawableCompat.bg(b,PALE,dp(10));b.setOnClickListener(v->{String[] p=q.split(":");hour.value=Integer.parseInt(p[0]);minute.value=Integer.parseInt(p[1]);hour.invalidate();minute.invalidate();memoryRow.setVisibility(View.GONE);});LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(55),dp(28));if(memoryRow.getChildCount()>0)lp.leftMargin=dp(5);memoryRow.addView(b,lp);}}

    void modePicker(int idx){sheet.removeAllViews();sheet.setOrientation(LinearLayout.HORIZONTAL);for(Mode m:new Mode[]{Mode.RESUME,Mode.JOB,Mode.EXAM,Mode.AUDIO}){Button b=new Button(this);b.setAllCaps(false);b.setText(shortName(m));b.setTextSize(11);b.setTextColor(BROWN);GradientDrawableCompat.bg(b,PALE,dp(12));b.setOnClickListener(v->{LockState.setWeekendMode(this,idx,m);sheetShade.setVisibility(View.GONE);refresh();});LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(56),dp(40));if(sheet.getChildCount()>0)lp.leftMargin=dp(5);sheet.addView(b,lp);}sheetShade.setVisibility(View.VISIBLE);}
    String shortName(Mode m){return m==Mode.RESUME?"简历":m==Mode.JOB?"岗位":m==Mode.EXAM?"考公":"磨耳";}
    void whitelist(Mode m){sheet.removeAllViews();sheet.setOrientation(LinearLayout.HORIZONTAL);fillApps(sheet,m);sheetShade.setVisibility(View.VISIBLE);}
    void fillApps(LinearLayout row,Mode m){row.removeAllViews();for(String pkg:new LinkedHashSet<>(m.allowed)){View v=realApp(pkg);if(v==null)continue;LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(34),dp(34));if(row.getChildCount()>0)lp.leftMargin=dp(7);row.addView(v,lp);}}
    View realApp(String pkg){try{PackageManager pm=getPackageManager();ApplicationInfo a=pm.getApplicationInfo(pkg,0);ImageView i=new ImageView(this);i.setImageDrawable(a.loadIcon(pm));i.setScaleType(ImageView.ScaleType.FIT_CENTER);i.setOnClickListener(v->{Intent in=pm.getLaunchIntentForPackage(pkg);if(in!=null)startActivity(in);});return i;}catch(Exception e){return null;}}
    void settings(){sheet.removeAllViews();sheet.setOrientation(LinearLayout.VERTICAL);sheet.addView(drawerBtn("无障碍",v->startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))),new LinearLayout.LayoutParams(-1,dp(48)));sheet.addView(drawerBtn("闹钟",v->alarm()),new LinearLayout.LayoutParams(-1,dp(48)));sheetShade.setVisibility(View.VISIBLE);}
    void alarm(){if(Build.VERSION.SDK_INT>=31)try{startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:"+getPackageName())));}catch(Exception e){startActivity(new Intent(Settings.ACTION_SETTINGS));}}
    Button drawerBtn(String s,View.OnClickListener l){Button b=new Button(this);b.setAllCaps(false);b.setText(s);b.setTextSize(18);b.setTextColor(BROWN);b.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);b.setBackgroundColor(Color.TRANSPARENT);b.setOnClickListener(l);return b;}

    Mode loadMode(){try{return Mode.valueOf(prefs().getString("regular_mode",Mode.RESUME.name()));}catch(Exception e){return Mode.RESUME;}}
    SharedPreferences prefs(){return getSharedPreferences("final_ui",MODE_PRIVATE);} boolean accessibility(){String s=Settings.Secure.getString(getContentResolver(),Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);return s!=null&&s.contains(getPackageName()+"/"+LockAccessibilityService.class.getName());}
    void requestNotify(){if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},5);}
    TextView text(String s,int sp,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(sp);t.setTextColor(color);t.setTypeface(Typeface.create(Typeface.SERIF,bold?Typeface.BOLD:Typeface.NORMAL));return t;}
    FrameLayout.LayoutParams full(){return new FrameLayout.LayoutParams(-1,-1);} int dp(float x){return (int)(x*getResources().getDisplayMetrics().density);}

    class Swipe extends GestureDetector.SimpleOnGestureListener{public boolean onDown(MotionEvent e){return true;}public boolean onFling(MotionEvent a,MotionEvent b,float vx,float vy){if(running.getVisibility()==View.VISIBLE||drawerShade.getVisibility()==View.VISIBLE||sheetShade.getVisibility()==View.VISIBLE)return false;float dx=b.getX()-a.getX(),dy=b.getY()-a.getY();if(Math.abs(dx)<dp(70)||Math.abs(dx)<Math.abs(dy))return false;if(dx<0&&page==FOCUS)showPage(CALENDAR);else if(dx>0&&page==CALENDAR)showPage(FOCUS);return true;}}

    static class CircleView extends View{Paint p=new Paint(1);String text="";CircleView(FinalActivity c){super(c);}void setText(String s){text=s;invalidate();}protected void onDraw(Canvas c){float r=Math.min(getWidth(),getHeight())/2f-4;p.setColor(GREEN);p.setStyle(Paint.Style.FILL);c.drawCircle(getWidth()/2f,getHeight()/2f,r,p);p.setColor(CREAM);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);c.drawCircle(getWidth()/2f,getHeight()/2f,r-8,p);if(!text.isEmpty()){p.setStyle(Paint.Style.FILL);p.setTextAlign(Paint.Align.CENTER);p.setTypeface(Typeface.create(Typeface.SERIF,Typeface.BOLD));p.setTextSize(getResources().getDisplayMetrics().scaledDensity*28);c.drawText(text,getWidth()/2f,getHeight()/2f-(p.ascent()+p.descent())/2,p);}}}
    static class TimeWheel extends View{Paint p=new Paint(1);int max,value;String unit;float y;TimeWheel(FinalActivity c,int mx,String u,int v){super(c);max=mx;unit=u;value=v;setOnTouchListener((x,e)->{if(e.getAction()==0){y=e.getY();return true;}if(e.getAction()==2&&Math.abs(e.getY()-y)>18){value=(value+(e.getY()<y?1:-1)+max+1)%(max+1);y=e.getY();invalidate();return true;}return true;});}protected void onDraw(Canvas c){p.setColor(BROWN);p.setTypeface(Typeface.create(Typeface.SERIF,Typeface.BOLD));p.setTextSize(getResources().getDisplayMetrics().scaledDensity*27);p.setTextAlign(Paint.Align.CENTER);String n=String.format(Locale.CHINA,"%02d",value);float base=getHeight()/2f-(p.ascent()+p.descent())/2;c.drawText(n,getWidth()*.42f,base,p);p.setTypeface(Typeface.DEFAULT);p.setTextSize(getResources().getDisplayMetrics().scaledDensity*10);p.setTextAlign(Paint.Align.LEFT);c.drawText(unit,getWidth()*.70f,base,p);}}
    static class AncientLockView extends View{Paint p=new Paint(1);boolean locked;AncientLockView(FinalActivity c){super(c);}void setLocked(boolean b){locked=b;invalidate();}protected void onDraw(Canvas c){float w=getWidth(),h=getHeight(),cx=w/2f;p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(w*.055f);p.setColor(Color.parseColor("#96A978"));c.drawCircle(cx,h*.33f,w*.22f,p);p.setStrokeWidth(w*.035f);RectF r=new RectF(w*.18f,h*.06f,w*.82f,h*.61f);for(int i=0;i<8;i++){float a=200+i*20;c.drawArc(r,a,11,false,p);}p.setStyle(Paint.Style.FILL);Path base=new Path();base.moveTo(w*.22f,h*.58f);base.lineTo(cx,h*.91f);base.lineTo(w*.78f,h*.58f);base.lineTo(w*.68f,h*.51f);base.lineTo(cx,h*.75f);base.lineTo(w*.32f,h*.51f);base.close();p.setColor(locked?Color.parseColor("#507A52"):Color.parseColor("#A9B889"));c.drawPath(base,p);if(locked){p.setColor(Color.parseColor("#6D8F31"));for(int i=0;i<6;i++){float yy=h*(.30f+.08f*i);float xx=i<3?w*(.18f-.025f*i):w*(.82f+.025f*(i-3));c.drawOval(new RectF(xx-8,yy-4,xx+12,yy+5),p);}}}}
    static class MiniIcon extends View{static final int MEMORY=1,LOCK=2,PLAY=3,MODE=4,GRID=5,STOP=6;Paint p=new Paint(1);int type,color;MiniIcon(FinalActivity c,int t,int co){super(c);type=t;color=co;}void setTint(int c){color=c;invalidate();}protected void onDraw(Canvas c){p.setColor(color);p.setStrokeWidth(2.5f);p.setStyle(Paint.Style.STROKE);float w=getWidth(),h=getHeight(),cx=w/2,cy=h/2;if(type==PLAY){p.setStyle(Paint.Style.FILL);Path q=new Path();q.moveTo(w*.28f,h*.18f);q.lineTo(w*.78f,cy);q.lineTo(w*.28f,h*.82f);q.close();c.drawPath(q,p);}else if(type==STOP){p.setStyle(Paint.Style.FILL);c.drawRoundRect(new RectF(w*.32f,h*.32f,w*.68f,h*.68f),3,3,p);}else if(type==GRID){for(int x=0;x<2;x++)for(int y=0;y<2;y++)c.drawRect(w*(.20f+.36f*x),h*(.20f+.36f*y),w*(.44f+.36f*x),h*(.44f+.36f*y),p);}else if(type==MODE){c.drawRoundRect(new RectF(w*.22f,h*.17f,w*.78f,h*.83f),4,4,p);c.drawLine(w*.34f,h*.38f,w*.66f,h*.38f,p);c.drawLine(w*.34f,h*.55f,w*.66f,h*.55f,p);}else if(type==LOCK){c.drawRoundRect(new RectF(w*.25f,h*.45f,w*.75f,h*.82f),4,4,p);c.drawArc(new RectF(w*.34f,h*.18f,w*.66f,h*.60f),180,180,false,p);}else{RectF r=new RectF(w*.19f,h*.18f,w*.81f,h*.82f);c.drawArc(r,35,290,false,p);}}}
    static class DayCell extends View{Paint p=new Paint(1);int day;float frac;boolean marked;DayCell(FinalActivity c,int d,float f,boolean m){super(c);day=d;frac=f;marked=m;}protected void onDraw(Canvas c){float s=Math.min(getWidth()-8,getHeight()-22),left=(getWidth()-s)/2,top=2;RectF box=new RectF(left,top,left+s,top+s);p.setStyle(Paint.Style.FILL);p.setColor(PALE);c.drawRoundRect(box,7,7,p);if(frac>0){c.save();Path clip=new Path();clip.addRoundRect(box,7,7,Path.Direction.CW);c.clipPath(clip);p.setColor(GREEN);c.drawRect(box.left,box.bottom-s*frac,box.right,box.bottom,p);c.restore();}p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1);p.setColor(LINE);c.drawRoundRect(box,7,7,p);if(marked){p.setStyle(Paint.Style.FILL);p.setColor(Color.parseColor("#5A8D21"));c.drawCircle(box.right-4,box.top+5,4,p);}p.setStyle(Paint.Style.FILL);p.setTextAlign(Paint.Align.CENTER);p.setColor(BROWN);p.setTextSize(getResources().getDisplayMetrics().scaledDensity*10);c.drawText(String.valueOf(day),getWidth()/2f,getHeight()-4,p);}}
}
