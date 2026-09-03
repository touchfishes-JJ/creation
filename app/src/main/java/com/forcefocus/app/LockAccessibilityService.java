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
    String lastPkg=""; BroadcastReceiver refresh; Typeface kai;
    int ink=Color.rgb(232,246,236), muted=Color.rgb(171,211,185), stroke=Color.rgb(91,153,113), gold=Color.rgb(211,236,184);
    Runnable tick=new Runnable(){ public void run(){ enforce(lastPkg); handler.postDelayed(this,1000); } };

    @Override public void onServiceConnected(){
        kai=Typeface.create(Typeface.SERIF, Typeface.NORMAL);
        wm=(WindowManager)getSystemService(WINDOW_SERVICE);
        refresh=new BroadcastReceiver(){ @Override public void onReceive(Context c,Intent i){ enforce(lastPkg); } };
        IntentFilter f=new IntentFilter(LockState.ACTION_REFRESH);
        if(Build.VERSION.SDK_INT>=33) registerReceiver(refresh,f,Context.RECEIVER_NOT_EXPORTED); else registerReceiver(refresh,f);
        handler.post(tick); Scheduler.scheduleNext14Days(this);
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent e){
        CharSequence p=e.getPackageName(); if(p!=null){ lastPkg=p.toString(); enforce(lastPkg); }
    }
    @Override public void onInterrupt(){}
    @Override public void onDestroy(){ super.onDestroy(); handler.removeCallbacks(tick); if(refresh!=null)try{ unregisterReceiver(refresh); }catch(Exception ignored){} hide(); }

    boolean allowed(Mode m,String pkg){
        if(pkg==null||pkg.isEmpty()) return false;
        if(pkg.equals(getPackageName())||pkg.equals("com.android.systemui")) return true;
        return m.allowed.contains(pkg);
    }

    void enforce(String pkg){
        LockState.Session s=LockState.current(this);
        if(s==null){ hide(); return; }
        if(allowed(s.mode,pkg)){ hide(); return; }
        show(s);
    }

    void show(LockState.Session s){
        if(overlay==null) createOverlay();
        title.setText(s.mode.title+"\n\n"+s.mode.desc);
        time.setText("剩余 "+LockState.timeLeft(s.end));
        escapeInfo.setText("本周解除机会："+LockState.escapesLeft(this)+" / 2");
        populateApps(s.mode);
        if(overlay.getParent()==null){
            WindowManager.LayoutParams lp=new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN|WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    PixelFormat.TRANSLUCENT);
            lp.gravity=Gravity.TOP|Gravity.START; wm.addView(overlay,lp);
        }
    }

    void hide(){ if(overlay!=null&&overlay.getParent()!=null)try{ wm.removeView(overlay); }catch(Exception ignored){} }

    LinearLayout.LayoutParams matchMargin(){
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0,0,0,12); return lp;
    }

    TextView t(String s,int sp,int c){
        TextView v=new TextView(this); v.setText(s); v.setTextSize(sp); v.setTextColor(c); v.setPadding(0,14,0,14); v.setTypeface(kai); return v;
    }

    Button btn(String s){
        Button b=new Button(this); b.setText(s); b.setAllCaps(false); b.setTextSize(17); b.setTypeface(kai); b.setPadding(22,18,22,18);
        GradientDrawableCompat.button(b, Color.rgb(34,89,59), Color.rgb(24,69,47), stroke, ink); return b;
    }

    void createOverlay(){
        FrameLayout frame=new FrameLayout(this);
        frame.addView(new JianghuDecorView(this),new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT));
        ScrollView sc=new ScrollView(this);
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setGravity(Gravity.CENTER_HORIZONTAL); root.setPadding(42,86,42,56); root.setBackgroundColor(Color.TRANSPARENT); sc.addView(root);
        frame.addView(sc,new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout hero=new LinearLayout(this); hero.setOrientation(LinearLayout.VERTICAL); hero.setPadding(28,24,28,24);
        GradientDrawableCompat.panel(hero,Color.argb(220,24,66,46),Color.argb(190,10,34,24),30,stroke);
        TextView small=t("竹林有风，杂念止步",15,gold); small.setGravity(Gravity.CENTER); hero.addView(small);
        title=t("",29,ink); title.setGravity(Gravity.CENTER); hero.addView(title);
        time=t("",34,ink); time.setGravity(Gravity.CENTER); hero.addView(time);
        escapeInfo=t("",15,muted); escapeInfo.setGravity(Gravity.CENTER); hero.addView(escapeInfo);
        root.addView(hero,matchMargin());

        apps=new LinearLayout(this); apps.setOrientation(LinearLayout.VERTICAL);
        LinearLayout appCard=new LinearLayout(this); appCard.setOrientation(LinearLayout.VERTICAL); appCard.setPadding(24,20,24,20);
        GradientDrawableCompat.panel(appCard,Color.argb(208,22,62,44),Color.argb(188,10,34,24),24,stroke);
        appCard.addView(apps,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(appCard,matchMargin());

        Button dial=btn("紧急通话");
        dial.setOnClickListener(v->{ try{ startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); }catch(Exception ignored){} });
        root.addView(dial,matchMargin());

        Button escape=btn("长按 8 秒 · 使用一次提前解除"); root.addView(escape,matchMargin());
        final Handler h=new Handler(Looper.getMainLooper()); final Runnable[] r=new Runnable[1];
        r[0]=()->{ LockState.Session s=LockState.current(this); if(s!=null){ boolean ok=LockState.useEscape(this,s.end); Toast.makeText(this,ok?"已解除到本时段结束":"本周 2 次机会已用完",Toast.LENGTH_LONG).show(); enforce(lastPkg); } };
        escape.setOnTouchListener((v,e)->{
            if(e.getAction()==MotionEvent.ACTION_DOWN){ h.postDelayed(r[0],8000); escape.setText("继续按住 8 秒…"); return true; }
            if(e.getAction()==MotionEvent.ACTION_UP||e.getAction()==MotionEvent.ACTION_CANCEL){ h.removeCallbacks(r[0]); escape.setText("长按 8 秒 · 使用一次提前解除"); return true; }
            return false;
        });
        overlay=frame;
    }

    void populateApps(Mode m){
        apps.removeAllViews();
        if(m.allowed.isEmpty()){
            TextView x=t("允许应用：无（请去电脑完成简历）",16,muted); x.setGravity(Gravity.CENTER); apps.addView(x); return;
        }
        apps.addView(t("只能打开：",16,gold));
        HashSet<String> seen=new HashSet<>();
        for(String pkg:m.allowed){
            Intent launch=getPackageManager().getLaunchIntentForPackage(pkg); if(launch==null) continue;
            String label; try{ label=getPackageManager().getApplicationLabel(getPackageManager().getApplicationInfo(pkg,0)).toString(); }catch(Exception e){ label=pkg; }
            if(!seen.add(label)) continue;
            Button b=btn("打开 "+label); final String p=pkg;
            b.setOnClickListener(v->{ Intent i=getPackageManager().getLaunchIntentForPackage(p); if(i!=null){ i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); startActivity(i); } });
            apps.addView(b,matchMargin());
        }
        if(seen.isEmpty()) apps.addView(t("没有检测到对应 App，请确认已安装。",14,Color.rgb(248,186,186)));
    }
}
