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
    private final int GREEN = Color.parseColor("#8AA832");
    private final int CREAM = Color.parseColor("#FFFBD3");
    private final int BROWN = Color.parseColor("#331915");

    WindowManager wm; View overlay; TextView title,time,escapeInfo,allowedInfo; LinearLayout apps; Handler handler=new Handler(Looper.getMainLooper());
    String lastPkg=""; BroadcastReceiver refresh; Typeface kai;
    Runnable tick=new Runnable(){public void run(){enforce(lastPkg); updateTimeOnly(); handler.postDelayed(this,1000);}};

    @Override public void onServiceConnected(){
        kai=Typeface.create(Typeface.SERIF, Typeface.NORMAL);
        wm=(WindowManager)getSystemService(WINDOW_SERVICE);
        refresh=new BroadcastReceiver(){@Override public void onReceive(Context c,Intent i){enforce(lastPkg);}};
        IntentFilter f=new IntentFilter(LockState.ACTION_REFRESH);
        if(Build.VERSION.SDK_INT>=33)registerReceiver(refresh,f,Context.RECEIVER_NOT_EXPORTED); else registerReceiver(refresh,f);
        handler.post(tick); Scheduler.scheduleNext14Days(this);
    }
    @Override public void onAccessibilityEvent(AccessibilityEvent e){ CharSequence p=e.getPackageName(); if(p!=null){lastPkg=p.toString();enforce(lastPkg);} }
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
    void updateTimeOnly(){
        if(overlay==null || overlay.getParent()==null) return;
        LockState.Session s=LockState.current(this);
        if(s==null) return;
        time.setText(LockState.timeLeft(s.end));
        escapeInfo.setText("解除 " + LockState.escapesLeft(this) + "/2");
    }
    void show(LockState.Session s){
        if(overlay==null)createOverlay();
        title.setText(s.mode.title);
        time.setText(LockState.timeLeft(s.end));
        escapeInfo.setText("解除 " + LockState.escapesLeft(this) + "/2");
        allowedInfo.setText(s.mode.allowed.isEmpty() ? "请去电脑完成" : s.mode.desc);
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
        FrameLayout frame=new FrameLayout(this);
        frame.setBackgroundColor(CREAM);
        ScrollView sc=new ScrollView(this);
        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18),dp(18),dp(18),dp(24));
        sc.addView(root);
        frame.addView(sc, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout hero=new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setPadding(dp(18),dp(18),dp(18),dp(18));
        GradientDrawableCompat.bg(hero, GREEN, dp(28));
        title=t("",34,WHITE(),true);
        time=t("",40,WHITE(),true);
        escapeInfo=t("",16,WHITE(),false);
        hero.addView(title);
        hero.addView(time);
        hero.addView(tescapeInfo);
        root.addView(hero, matchMargin());

        LinearLayout descCard=new LinearLayout(this);
        descCard.setOrientation(LinearLayout.VERTICAL);
        descCard.setPadding(dp(18),dp(18),dp(18),dp(18));
        GradientDrawableCompat.bg(descCard, WHITE(), dp(28));
        allowedInfo=t("",18,BROWN,false);
        descCard.addView(allowedInfo);
        root.addView(descCard, matchMargin());

        apps=new LinearLayout(this);
        apps.setOrientation(LinearLayout.VERTICAL);
        root.addView(apps, matchMargin());

        LinearLayout row=new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        Button dial=actionBtn("通衽", BROWN, WHITE());
        dial.setOnClickListener(v->{try{startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));}catch(Exception ignored){}});
        Button escape=actionBtn("提前解除", BROWN, WHITE());
        final Handler h=new Handler(Looper.getMainLooper()); final Runnable[] r=new Runnable[1];
        r[0]=()->{LockState.Session s=LockState.current(this); if(s!=null){boolean ok=LockState.useEscape(this,s.end);Toast.makeText(this,ok?"已解除":"本周机会用完",Toast.LENGTH_LONG).show();enforce(lastPkg);}};
        escape.setOnTouchListener((2,e)->{if(e.getAction()==MotionEvent.ACTION_DOWN){h.postDelayed(r[0],8000);escape.setText("按住 8 秒");return true;} if(e.getAction()==MotionEvent.ACTION_UP||e.getAction()==MotionEvent.ACTION_CANCEL){h.removeCallbacks(r[0]);escape.setText("提前解除");return true;}return false;});
        row.addView(dial, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        LinearLayout.LayoutParams eL_p=new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        e_lp.setMargins(dp(8),0,0,0);
        row.addView(escape, e_lp);
        root.addView(row, matchMargin());

        overlay=frame;
    }

    void populateApps(Mode m){
        apps.removeAllViews();
        if(m.allowed.isEmpty())return;
        LinearLayout card=new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18),dp(18),dp(18),dp(18));
        GradientDrawableCompat.bg(card, BROWN, dp(28));
        card.addView(t("可用",16,WHITE(),false));
        HashSet<String> seen=new HashSet<>();
        for(String pkg:m.allowed){
            Intent launch=getPackageManager().getLaunchIntentForPackage(pkg); if(launch==null)continue;
            String label;try{label=getPackageManager().getApplicationLabel(getPackageManager().getApplicationInfo(pkg,0)).toString();}catch(Exception e){label=pkg;}
            if(!seen.add(label))continue;
            Button b=actionBtn(label, WHITE(), BROWN);
            final String p=pkg;
            b.setOnClickListener(v->{Intent i=getPackageManager().getLaunchIntentForPackage(p);if(i!=null){i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);startActivity(i);}});
            card.addView(b, matchInner());
        }
        apps.addView(card, matchMargin());
    }

    TextView t(String s,int sp,int c,boolean bold){TextView v=new TextView(this);v.setText(s);v.setTextSize(sp);v.setTextColor(c);v.setPadding(0,dp(4),0,0);v.setTypeface(kai,bold?Typeface.BOLD:Typeface.NORMAL);return v;}
    Button actionBtn(String s,int textColor,int bgColor){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextSize(18);b.setTypeface(kai);b.setTextColor(textColor);b.setPadding(dp(10),dp(12),dp(10),dp(12));GradientDrawableCompat.bg(b,bgColor,dp(22));return b;}
    LinearLayout.LayoutParams matchMargin(){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);lp.setMargins(0,0,0,dp(12));return lp;}
    LinearLayout.LayoutParams matchInner(){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);lp.setMargins(0,dp(8),0,0);return lp;}
    int dp(int x){ return (int)(x * getResources().getDisplayMetrics().density); }
    int WHITE(){ return Color.parseColor("#F8F7E8"); }
}
