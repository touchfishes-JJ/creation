package com.forcefocus.app;

import android.Manifest;
import android.app.Activity;
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
  final int GREEN=Color.parseColor("#8AA832"), CREAM=Color.parseColor("#FFFBD3"), BROWN=Color.parseColor("#331915"), PALE=Color.parseColor("#F4F0C7");
  FrameLayout root,focus,calendar,drawer,shade,running; LinearLayout regular,weekend,apps,mem; GridLayout grid; TextView month;
  ImageView tree,forest; FocusOrbView orb,runOrb; ValueBoxView hh,mm; Button[] choices=new Button[4]; Button[][] slots=new Button[3][4]; Mode selected=Mode.RESUME; int page=0,sub=0; float sx;
  Handler h=new Handler(Looper.getMainLooper()); Runnable tick=new Runnable(){public void run(){refresh();h.postDelayed(this,1000);}};

  @Override public void onCreate(Bundle b){super.onCreate(b);build();selected=loadMode();loadTime();autoSub();requestBasics();Scheduler.scheduleNext14Days(this);h.post(tick);}
  @Override public void onResume(){super.onResume();autoSub();refresh();renderCal();}
  @Override public void onDestroy(){super.onDestroy();h.removeCallbacks(tick);}
  @Override public boolean dispatchTouchEvent(MotionEvent e){
    if(e.getAction()==MotionEvent.ACTION_DOWN)sx=e.getX();
    if(e.getAction()==MotionEvent.ACTION_UP && running.getVisibility()!=View.VISIBLE){float dx=e.getX()-sx;
      if(sx<dp(18)&&dx>dp(48))showDrawer(); else if(Math.abs(dx)>dp(90)){if(dx<0&&page==0)showPage(1);else if(dx>0&&page==1)showPage(0);}}
    return super.dispatchTouchEvent(e);
  }

  void build(){root=new FrameLayout(this);root.setBackgroundColor(CREAM);setContentView(root);buildFocus();buildCalendar();buildDrawer();buildShade();showPage(0);}

  void buildFocus(){focus=new FrameLayout(this);focus.setBackgroundColor(CREAM);root.addView(focus,full());
    LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER);FrameLayout.LayoutParams tp=new FrameLayout.LayoutParams(-1,dp(92));tp.topMargin=dp(92);focus.addView(top,tp);
    tree=modeImage(R.drawable.tree_mode,v->switchSub(0));forest=modeImage(R.drawable.forest_mode,v->switchSub(1));top.addView(tree,new LinearLayout.LayoutParams(dp(96),dp(82)));Space sp=new Space(this);top.addView(sp,new LinearLayout.LayoutParams(dp(28),1));top.addView(forest,new LinearLayout.LayoutParams(dp(96),dp(82)));

    regular=new LinearLayout(this);regular.setOrientation(LinearLayout.VERTICAL);regular.setGravity(Gravity.CENTER_HORIZONTAL);FrameLayout.LayoutParams rp=new FrameLayout.LayoutParams(-1,-1);rp.topMargin=dp(176);focus.addView(regular,rp);
    FrameLayout zone=new FrameLayout(this);regular.addView(zone,new LinearLayout.LayoutParams(dp(340),dp(332)));
    orb=new FocusOrbView(this);orb.setFill(GREEN);orb.setCenterTextColor(CREAM);orb.setOnClickListener(v->toggleChoices());FrameLayout.LayoutParams op=new FrameLayout.LayoutParams(dp(220),dp(220));op.gravity=Gravity.CENTER;zone.addView(orb,op);
    String[] labs={"简历","考公","岗位调研","磨耳朵"};Mode[] modes={Mode.RESUME,Mode.EXAM,Mode.JOB,Mode.AUDIO};int[] grav={Gravity.TOP|Gravity.CENTER_HORIZONTAL,Gravity.CENTER_VERTICAL|Gravity.START,Gravity.CENTER_VERTICAL|Gravity.END,Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL};
    for(int i=0;i<4;i++){final Mode m=modes[i];choices[i]=pill(labs[i],v->{selected=m;saveMode();hideChoices();refreshApps();});FrameLayout.LayoutParams p=new FrameLayout.LayoutParams(dp(98),dp(42));p.gravity=grav[i];zone.addView(choices[i],p);}hideChoices();
    apps=new LinearLayout(this);apps.setGravity(Gravity.CENTER);regular.addView(apps,new LinearLayout.LayoutParams(-2,dp(40)));
    Space fill=new Space(this);regular.addView(fill,new LinearLayout.LayoutParams(1,0,1));
    LinearLayout bottom=new LinearLayout(this);bottom.setGravity(Gravity.CENTER_VERTICAL);bottom.setPadding(dp(18),0,dp(18),dp(58));regular.addView(bottom,new LinearLayout.LayoutParams(-1,-2));
    LinearLayout time=new LinearLayout(this);time.setPadding(dp(8),dp(8),dp(8),dp(8));GradientDrawableCompat.panel(time,CREAM,CREAM,dp(26),GREEN);bottom.addView(time,new LinearLayout.LayoutParams(0,dp(108),1));
    hh=new ValueBoxView(this);hh.setRange(0,4);hh.setUnit("h");mm=new ValueBoxView(this);mm.setRange(0,59);mm.setUnit("min");ValueBoxView.Listener vl=new ValueBoxView.Listener(){public void onSelected(ValueBoxView v){hh.setSelectedState(v==hh);mm.setSelectedState(v==mm);}public void onValueChanged(ValueBoxView v,int x){saveTimeIfLocked();}};hh.setListener(vl);mm.setListener(vl);hh.setSelectedState(true);time.addView(hh,new LinearLayout.LayoutParams(dp(82),dp(88)));LinearLayout.LayoutParams ml=new LinearLayout.LayoutParams(dp(82),dp(88));ml.leftMargin=dp(6);time.addView(mm,ml);
    View play=iconBtn(MiniGlyphView.PLAY,GREEN,CREAM,v->startFocus());LinearLayout.LayoutParams pl=new LinearLayout.LayoutParams(dp(108),dp(108));pl.leftMargin=dp(12);bottom.addView(play,pl);
    LinearLayout tiny=new LinearLayout(this);tiny.setGravity(Gravity.CENTER);regular.addView(tiny,new LinearLayout.LayoutParams(-2,dp(32)));View lock=tinyIcon(MiniGlyphView.LOCK,v->toggleTimeLock());View memory=tinyIcon(MiniGlyphView.MEMORY,v->toggleMem());tiny.addView(lock,new LinearLayout.LayoutParams(dp(28),dp(28)));LinearLayout.LayoutParams mml=new LinearLayout.LayoutParams(dp(28),dp(28));mml.leftMargin=dp(18);tiny.addView(memory,mml);
    mem=new LinearLayout(this);mem.setGravity(Gravity.CENTER);mem.setVisibility(View.GONE);regular.addView(mem,new LinearLayout.LayoutParams(-2,dp(36)));

    weekend=new LinearLayout(this);weekend.setOrientation(LinearLayout.VERTICAL);weekend.setPadding(dp(18),dp(6),dp(18),dp(24));FrameLayout.LayoutParams wp=new FrameLayout.LayoutParams(-1,-1);wp.topMargin=dp(176);focus.addView(weekend,wp);
    LinearLayout lr=new LinearLayout(this);lr.setGravity(Gravity.END);weekend.addView(lr,new LinearLayout.LayoutParams(-1,dp(34)));lr.addView(tinyIcon(MiniGlyphView.LOCK,v->toggleWeekendLock()),new LinearLayout.LayoutParams(dp(30),dp(30)));
    for(int i=0;i<3;i++){LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(12),dp(12),dp(12),dp(12));GradientDrawableCompat.panel(card,CREAM,PALE,dp(22),GREEN);LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.topMargin=dp(10);weekend.addView(card,cp);TextView tv=txt(LockState.slotStart(i)+"   "+LockState.slotEnd(i),20,true);card.addView(tv);LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);card.addView(row,new LinearLayout.LayoutParams(-1,dp(42)));final int idx=i;String[] ls={"简历","岗位","考公","磨耳"};Mode[] ms={Mode.RESUME,Mode.JOB,Mode.EXAM,Mode.AUDIO};for(int j=0;j<4;j++){final Mode md=ms[j];slots[i][j]=pill(ls[j],v->{LockState.setWeekendMode(this,idx,md);refresh();});LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(0,dp(34),1);if(j>0)bp.leftMargin=dp(5);row.addView(slots[i][j],bp);}LinearLayout ar=new LinearLayout(this);ar.setGravity(Gravity.CENTER);card.addView(ar,new LinearLayout.LayoutParams(-1,dp(38)));card.setTag(ar);}

    running=new FrameLayout(this);running.setBackgroundColor(CREAM);running.setVisibility(View.GONE);focus.addView(running,full());runOrb=new FocusOrbView(this);runOrb.setFill(GREEN);runOrb.setCenterTextColor(CREAM);FrameLayout.LayoutParams ro=new FrameLayout.LayoutParams(dp(224),dp(224));ro.gravity=Gravity.CENTER;running.addView(runOrb,ro);
  }

  void buildCalendar(){calendar=new FrameLayout(this);calendar.setBackgroundColor(CREAM);root.addView(calendar,full());LinearLayout wrap=new LinearLayout(this);wrap.setOrientation(LinearLayout.VERTICAL);wrap.setGravity(Gravity.CENTER);calendar.addView(wrap,full());month=txt("",20,true);month.setGravity(Gravity.CENTER);wrap.addView(month);LinearLayout wk=new LinearLayout(this);wk.setGravity(Gravity.CENTER);wrap.addView(wk,new LinearLayout.LayoutParams(-2,-2));for(String s:new String[]{"一","二","三","四","五","六","日"}){TextView t=txt(s,12,false);t.setGravity(Gravity.CENTER);wk.addView(t,new LinearLayout.LayoutParams(dp(48),dp(26)));}grid=new GridLayout(this);grid.setColumnCount(7);grid.setRowCount(6);wrap.addView(grid,new LinearLayout.LayoutParams(-2,-2));renderCal();}

  void buildDrawer(){drawer=new FrameLayout(this);drawer.setBackgroundColor(Color.parseColor("#55000000"));drawer.setVisibility(View.GONE);root.addView(drawer,full());drawer.setOnClickListener(v->drawer.setVisibility(View.GONE));LinearLayout panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);panel.setPadding(dp(18),dp(72),dp(18),dp(18));panel.setBackgroundColor(CREAM);FrameLayout.LayoutParams p=new FrameLayout.LayoutParams((int)(getResources().getDisplayMetrics().widthPixels*.42f),-1);p.gravity=Gravity.START;drawer.addView(panel,p);panel.setOnClickListener(v->{});panel.addView(menuBtn("设置",v->{drawer.setVisibility(View.GONE);showSettings();}),new LinearLayout.LayoutParams(-1,dp(54)));LinearLayout.LayoutParams q=new LinearLayout.LayoutParams(-1,dp(54));q.topMargin=dp(10);panel.addView(menuBtn("白名单",v->{drawer.setVisibility(View.GONE);showWhite();}),q);}
  void buildShade(){shade=new FrameLayout(this);shade.setBackgroundColor(Color.parseColor("#66000000"));shade.setVisibility(View.GONE);root.addView(shade,full());shade.setOnClickListener(v->shade.setVisibility(View.GONE));}

  void showSettings(){LinearLayout c=sheet();c.addView(txt("设置",18,true));LinearLayout r=new LinearLayout(this);r.setGravity(Gravity.CENTER);c.addView(r,new LinearLayout.LayoutParams(-1,dp(94)));r.addView(menuBtn("无障碍",v->startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))),new LinearLayout.LayoutParams(0,dp(72),1));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(72),1);p.leftMargin=dp(8);r.addView(menuBtn("闹钟",v->alarm()),p);showSheet(c);}
  void showWhite(){LinearLayout c=sheet();c.addView(txt("白名单",18,true));LinearLayout r=new LinearLayout(this);r.setGravity(Gravity.CENTER);c.addView(r,new LinearLayout.LayoutParams(-1,dp(52)));Mode m=sub==0?selected:LockState.weekendMode(this,Math.max(0,LockState.currentWeekendSlot()));addAppIcons(r,m,false);showSheet(c);}
  LinearLayout sheet(){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(16),dp(16),dp(16),dp(16));GradientDrawableCompat.panel(c,CREAM,PALE,dp(26),GREEN);return c;}
  void showSheet(View c){shade.removeAllViews();shade.setBackgroundColor(Color.parseColor("#66000000"));shade.addView(c,new FrameLayout.LayoutParams((int)(getResources().getDisplayMetrics().widthPixels*.78f),-2,Gravity.CENTER));shade.setVisibility(View.VISIBLE);c.setOnClickListener(v->{});}

  void refresh(){tree.setScaleX(sub==0?1.14f:.94f);tree.setScaleY(sub==0?1.14f:.94f);forest.setScaleX(sub==1?1.14f:.94f);forest.setScaleY(sub==1?1.14f:.94f);refreshApps();refreshSlots();refreshMem();LockState.Session s=LockState.current(this);if(page==0&&s!=null){running.setVisibility(View.VISIBLE);runOrb.setCenterText(LockState.timeLeftShort(s.end));}else running.setVisibility(View.GONE);}
  void refreshApps(){addAppIcons(apps,selected,false);}
  void refreshSlots(){for(int i=0;i<3;i++){Mode m=LockState.weekendMode(this,i);for(int j=0;j<4;j++){Mode[] ms={Mode.RESUME,Mode.JOB,Mode.EXAM,Mode.AUDIO};GradientDrawableCompat.bg(slots[i][j],ms[j]==m?BROWN:GREEN,dp(16));slots[i][j].setTextColor(CREAM);}LinearLayout card=(LinearLayout)slots[i][0].getParent().getParent();LinearLayout ar=(LinearLayout)card.getTag();addAppIcons(ar,m,LockState.slotFinished(i));card.setAlpha(LockState.slotFinished(i)?.45f:1f);}}
  void addAppIcons(LinearLayout row,Mode m,boolean dim){row.removeAllViews();Set<String> seen=new LinkedHashSet<>(m.allowed);for(String pkg:seen){int type=pkg.contains("xhs")?MiniGlyphView.APP_XHS:pkg.contains("fenbi")?MiniGlyphView.APP_FENBI:pkg.contains("wps")?MiniGlyphView.APP_WPS:MiniGlyphView.APP_RECORD;View v=iconBtn(type,PALE,BROWN,x->launch(pkg));v.setAlpha(dim?.35f:1);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(dp(32),dp(32));if(row.getChildCount()>0)p.leftMargin=dp(6);row.addView(v,p);}}

  void renderCal(){if(grid==null)return;grid.removeAllViews();Calendar c=Calendar.getInstance();int y=c.get(Calendar.YEAR),m=c.get(Calendar.MONTH);month.setText(String.format(Locale.CHINA,"%d 年 %02d 月",y,m+1));c.set(y,m,1);int blanks=(c.get(Calendar.DAY_OF_WEEK)+5)%7,max=c.getActualMaximum(Calendar.DAY_OF_MONTH);for(int i=0;i<blanks;i++)blank();for(int d=1;d<=max;d++){CalendarDayView v=new CalendarDayView(this);float f=Math.min(1f,StudyStats.millisFor(this,y,m,d)/(8f*3600000f));v.setData(d,f,StudyStats.marked(this,y,m,d));final int day=d;v.setOnClickListener(x->{StudyStats.toggleMark(this,y,m,day);renderCal();});GridLayout.LayoutParams p=new GridLayout.LayoutParams();p.width=dp(48);p.height=dp(66);p.setMargins(dp(2),dp(2),dp(2),dp(2));grid.addView(v,p);}int n=blanks+max;while(n++<42)blank();}
  void blank(){View v=new View(this);GridLayout.LayoutParams p=new GridLayout.LayoutParams();p.width=dp(48);p.height=dp(66);grid.addView(v,p);}

  void startFocus(){int mins=hh.getValue()*60+mm.getValue();if(mins<=0)mins=25;remember();saveTimeIfLocked();LockState.startManual(this,selected,mins);if(!accessOn())startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));refresh();}
  void toggleChoices(){int v=choices[0].getVisibility()==View.VISIBLE?View.GONE:View.VISIBLE;for(Button b:choices)b.setVisibility(v);}void hideChoices(){for(Button b:choices)b.setVisibility(View.GONE);}
  void switchSub(int s){sub=s;regular.setVisibility(s==0?View.VISIBLE:View.GONE);weekend.setVisibility(s==1?View.VISIBLE:View.GONE);refresh();}
  void autoSub(){if(pref().getBoolean("weekend_lock",false)){switchSub(1);return;}int d=Calendar.getInstance().get(Calendar.DAY_OF_WEEK);switchSub(d==Calendar.SATURDAY||d==Calendar.SUNDAY?1:0);}
  void toggleWeekendLock(){pref().edit().putBoolean("weekend_lock",!pref().getBoolean("weekend_lock",false)).apply();}
  void toggleTimeLock(){pref().edit().putBoolean("time_lock",!pref().getBoolean("time_lock",false)).apply();saveTimeIfLocked();}
  void saveTimeIfLocked(){if(pref().getBoolean("time_lock",false))pref().edit().putInt("h",hh.getValue()).putInt("m",mm.getValue()).apply();}
  void loadTime(){if(pref().getBoolean("time_lock",false)){hh.setValue(pref().getInt("h",0));mm.setValue(pref().getInt("m",25));}else{hh.setValue(0);mm.setValue(25);}}
  void remember(){String now=hh.getValue()+":"+mm.getValue();ArrayList<String>a=new ArrayList<>();a.add(now);for(int i=0;i<3;i++){String x=pref().getString("mem"+i,null);if(x!=null&&!x.equals(now)&&a.size()<3)a.add(x);}SharedPreferences.Editor e=pref().edit();for(int i=0;i<3;i++)e.putString("mem"+i,i<a.size()?a.get(i):null);e.apply();}
  void toggleMem(){mem.setVisibility(mem.getVisibility()==View.VISIBLE?View.GONE:View.VISIBLE);refreshMem();}
  void refreshMem(){if(mem==null||mem.getVisibility()!=View.VISIBLE)return;mem.removeAllViews();for(int i=0;i<3;i++){String x=pref().getString("mem"+i,null);if(x==null)continue;Button b=pill(x.replace(":","h ")+"m",v->{String[]z=x.split(":");hh.setValue(Integer.parseInt(z[0]));mm.setValue(Integer.parseInt(z[1]));mem.setVisibility(View.GONE);});LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-2,dp(30));if(mem.getChildCount()>0)p.leftMargin=dp(5);mem.addView(b,p);}}

  void showPage(int p){page=p;focus.setVisibility(p==0?View.VISIBLE:View.GONE);calendar.setVisibility(p==1?View.VISIBLE:View.GONE);if(p==1)renderCal();refresh();}
  void showDrawer(){drawer.setVisibility(View.VISIBLE);}
  void alarm(){if(Build.VERSION.SDK_INT>=31)try{startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,Uri.parse("package:"+getPackageName())));}catch(Exception e){startActivity(new Intent(Settings.ACTION_SETTINGS));}}
  void launch(String pkg){Intent i=getPackageManager().getLaunchIntentForPackage(pkg);if(i!=null)startActivity(i);else Toast.makeText(this,"未安装",Toast.LENGTH_SHORT).show();}
  void saveMode(){pref().edit().putString("mode",selected.name()).apply();}
  Mode loadMode(){try{return Mode.valueOf(pref().getString("mode",Mode.RESUME.name()));}catch(Exception e){return Mode.RESUME;}}
  SharedPreferences pref(){return getSharedPreferences("ui",MODE_PRIVATE);}
  boolean accessOn(){String x=Settings.Secure.getString(getContentResolver(),Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);return x!=null&&x.contains(getPackageName());}
  void requestBasics(){if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},5);}

  ImageView modeImage(int res,View.OnClickListener l){ImageView v=new ImageView(this);v.setImageResource(res);v.setScaleType(ImageView.ScaleType.FIT_CENTER);v.setOnClickListener(l);return v;}
  Button pill(String s,View.OnClickListener l){Button b=new Button(this);b.setAllCaps(false);b.setText(s);b.setTextSize(13);b.setTypeface(Typeface.create(Typeface.SERIF,Typeface.BOLD));b.setTextColor(CREAM);GradientDrawableCompat.bg(b,BROWN,dp(18));b.setOnClickListener(l);return b;}
  Button menuBtn(String s,View.OnClickListener l){Button b=new Button(this);b.setAllCaps(false);b.setText(s);b.setTextSize(17);b.setTextColor(BROWN);GradientDrawableCompat.bg(b,PALE,dp(16));b.setOnClickListener(l);return b;}
  View iconBtn(int type,int bg,int fg,View.OnClickListener l){FrameLayout f=new FrameLayout(this);GradientDrawableCompat.bg(f,bg,dp(22));MiniGlyphView g=new MiniGlyphView(this,type);g.setTint(fg);FrameLayout.LayoutParams p=new FrameLayout.LayoutParams(dp(24),dp(24),Gravity.CENTER);f.addView(g,p);f.setOnClickListener(l);return f;}
  View tinyIcon(int type,View.OnClickListener l){MiniGlyphView g=new MiniGlyphView(this,type);g.setTint(BROWN);g.setOnClickListener(l);return g;}
  TextView txt(String s,int size,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(BROWN);t.setTypeface(Typeface.create(Typeface.SERIF,bold?Typeface.BOLD:Typeface.NORMAL));return t;}
  FrameLayout.LayoutParams full(){return new FrameLayout.LayoutParams(-1,-1);}int dp(float n){return(int)(n*getResources().getDisplayMetrics().density);}
}
