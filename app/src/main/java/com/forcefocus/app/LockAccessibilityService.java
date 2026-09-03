package com.forcefocus.app;

import android.accessibilityservice.AccessibilityService;
import android.content.*;
import android.graphics.*;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.view.accessibility.AccessibilityEvent;
import android.widget.*;
import java.util.*;

public class LockAccessibilityService extends AccessibilityService {
    WindowManager wm; View overlay; TextView title,time,escapeInfo; LinearLayout apps; Handler handler=new Handler(Looper.getMainLooper());
    String lastPkg=""; BroadcastReceiver refresh;
    Runnable tick=new Runnable(){public void run(){enforce(lastPkg);handler.postDelayed(this,1000);}};

    @Override public void onServiceConnected(){
        wm=(WindowManager)getSystemService(WINDOW_SERVICE);
        refresh=new BroadcastReceiver(){@Override public void onReceive(Context c,Intent i){enforce(lastPkg);}};
        IntentFilter f=new IntentFilter(LockState.ACTION_REFRESH);
        if(Build.VERSION.SDK_INT>=33)registerReceiver(refresh,f,Context.RECEIVER_NOT_EXPORTED); else registerReceiver(refresh,f);
        handler.post(tick); Scheduler.scheduleNext14Days(this);
    }
    @Override public void onAccessibilityEvent(AccessibilityEvent e){
        CharSequence p=e.getPackageName(); if(p!=null){lastPkg=p.toString();enforce(lastPkg);}
    }
    @Override public void onInterrupt(){}
    @Override public void onDestroy(){super.onDestroy();handler.removeCallbacks(tick);if(refresh!=null)try{unregisterReceiver(refresh);}catch(Exception ignored){}hide();}

    boolean allowed(Mode m,String pkg){
        if(pkg==null||pkg.isEmpty())return false;
        if(pkg.equals(getPackageName())||pkg.equals("com.android.systemui"))return true;
        return m.allowed.contains(pkg);
    }
    void enforce(String pkg){
        LockState.Session s=LockState.current(this);
        if(s==null){hide();return;}
        if(allowed(s.mode,pkg)){hide();return;}
        show(s);
    }
    void show(LockState.Session s){
        if(overlay==null)createOverlay();
        title.setText(s.mode.title+"\n\n"+s.mode.desc);
        time.setText("剩余 "+LockState.timeLeft(s.end));
        escapeInfo.setText("本周解除机会："+LockState.escapesLeft(this)+" / 2");
        populateApps(s.mode);
        if(overlay.getParent()==null){
            WindowManager.LayoutParams lp=new WindowManager.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT,WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN|WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    PixelFormat.TRANSLUCENT);
            lp.gravity=Gravity.TOP|Gravity.START; wm.addView(overlay,lp);
        }
    }
    void hide(){if(overlay!=null&&overlay.getParent()!=null)try{wm.removeView(overlay);}catch(Exception ignored){}}
    void createOverlay(){
        ScrollView sc=new ScrollView(this); LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setGravity(Gravity.CENTER_HORIZONTAL);root.setPadding(48,100,48,60);root.setBackgroundColor(Color.rgb(8,9,12));sc.addView(root);
        TextView small=t("现在不能做别的",15,Color.rgb(161,161,170));root.addView(small);
        title=t("",29,Color.WHITE);title.setGravity(Gravity.CENTER);root.addView(title);
        time=t("",34,Color.WHITE);time.setGravity(Gravity.CENTER);root.addView(time);
        escapeInfo=t("",15,Color.rgb(161,161,170));root.addView(escapeInfo);
        apps=new LinearLayout(this);apps.setOrientation(LinearLayout.VERTICAL);root.addView(apps,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));
        Button dial=new Button(this);dial.setText("紧急通话");dial.setOnClickListener(v->{try{startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));}catch(Exception ignored){}});root.addView(dial);
        Button escape=new Button(this);escape.setText("长按 8 秒 · 使用一次提前解除");root.addView(escape);
        final Handler h=new Handler(Looper.getMainLooper()); final Runnable[] r=new Runnable[1];
        r[0]=()->{LockState.Session s=LockState.current(this); if(s!=null){boolean ok=LockState.useEscape(this,s.end);Toast.makeText(this,ok?"已解除到本时段结束":"本周 2 次机会已用完",Toast.LENGTH_LONG).show();enforce(lastPkg);}};
        escape.setOnTouchListener((v,e)->{if(e.getAction()==MotionEvent.ACTION_DOWN){h.postDelayed(r[0],8000);escape.setText("继续按住 8 秒…");return true;} if(e.getAction()==MotionEvent.ACTION_UP||e.getAction()==MotionEvent.ACTION_CANCEL){h.removeCallbacks(r[0]);escape.setText("长按 8 秒 · 使用一次提前解除");return true;}return false;});
        overlay=sc;
    }
    TextView t(String s,int sp,int c){TextView v=new TextView(this);v.setText(s);v.setTextSize(sp);v.setTextColor(c);v.setPadding(0,18,0,18);return v;}
    void populateApps(Mode m){
        apps.removeAllViews();
        if(m.allowed.isEmpty()){TextView x=t("允许应用：无（请去电脑完成简历）",15,Color.rgb(161,161,170));x.setGravity(Gravity.CENTER);apps.addView(x);return;}
        TextView x=t("只能打开：",14,Color.rgb(161,161,170));apps.addView(x);
        HashSet<String> seen=new HashSet<>();
        for(String pkg:m.allowed){
            Intent launch=getPackageManager().getLaunchIntentForPackage(pkg); if(launch==null)continue;
            String label;try{label=getPackageManager().getApplicationLabel(getPackageManager().getApplicationInfo(pkg,0)).toString();}catch(Exception e){label=pkg;}
            if(!seen.add(label))continue;
            Button b=new Button(this);b.setText("打开 "+label); final String p=pkg;b.setOnClickListener(v->{Intent i=getPackageManager().getLaunchIntentForPackage(p);if(i!=null){i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);startActivity(i);}});apps.addView(b);
        }
        if(seen.isEmpty())apps.addView(t("没有检测到对应 App，请确认已安装。",14,Color.rgb(239,68,68)));
    }
}
