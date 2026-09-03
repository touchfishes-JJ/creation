package com.forcefocus.app;

import android.content.Context;
import android.graphics.*;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class FocusOrbView extends View {
    public interface LongPressListener{ void onLongPress(); }
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Handler h=new Handler(Looper.getMainLooper());
    private Runnable longPress;
    private LongPressListener listener;
    private String centerText="";
    private int fill=Color.parseColor("#8AA832");
    private int textColor=Color.parseColor("#FFFBD3");

    public FocusOrbView(Context c){super(c);init();}
    public FocusOrbView(Context c, AttributeSet a){super(c,a);init();}
    private void init(){ setClickable(true); }

    public void setLongPressListener(LongPressListener l){listener=l;}
    public void setCenterText(String s){centerText=s==null?"":s;invalidate();}
    public void setFill(int c){fill=c;invalidate();}
    public void setCenterTextColor(int c){textColor=c;invalidate();}

    @Override protected void onMeasure(int w,int hSpec){
        int size=Math.min(MeasureSpec.getSize(w), dp(250));
        if(size<=0) size=dp(250);
        setMeasuredDimension(size,size);
    }

    @Override protected void onDraw(Canvas c){
        super.onDraw(c);
        float cx=getWidth()/2f, cy=getHeight()/2f, r=Math.min(cx,cy)-dp(2);
        p.setStyle(Paint.Style.FILL); p.setColor(fill); c.drawCircle(cx,cy,r,p);
        if(!centerText.isEmpty()){
            p.setColor(textColor); p.setTextAlign(Paint.Align.CENTER); p.setTypeface(Typeface.create(Typeface.SERIF,Typeface.BOLD)); p.setTextSize(dp(28));
            Paint.FontMetrics fm=p.getFontMetrics();
            c.drawText(centerText,cx,cy-(fm.ascent+fm.descent)/2f,p);
        }
    }

    @Override public boolean onTouchEvent(MotionEvent e){
        if(e.getAction()==MotionEvent.ACTION_DOWN){
            longPress=()->{ if(listener!=null) listener.onLongPress(); };
            h.postDelayed(longPress,3000);
            return true;
        }
        if(e.getAction()==MotionEvent.ACTION_UP || e.getAction()==MotionEvent.ACTION_CANCEL){
            if(longPress!=null) h.removeCallbacks(longPress);
            return true;
        }
        return super.onTouchEvent(e);
    }

    private int dp(int x){return (int)(x*getResources().getDisplayMetrics().density);}
}
