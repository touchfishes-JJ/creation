package com.forcefocus.app;

import android.content.Context;
import android.graphics.*;
import android.view.MotionEvent;
import android.view.View;

public class ValueBoxView extends View {
    public interface Listener {
        void onSelected(ValueBoxView view);
        void onValueChanged(ValueBoxView view, int value);
    }

    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int GREEN = Color.parseColor("#8AA832");
    private final int CREAM = Color.parseColor("#FFFBD3");
    private final int BROWN = Color.parseColor("#331915");

    private Listener listener;
    private String unit = "h";
    private int min = 0, max = 4, value = 0;
    private boolean selected = false;
    private float startY;

    public ValueBoxView(Context c){ super(c); setClickable(true); }
    public void setListener(Listener l){ listener = l; }
    public void setUnit(String u){ unit = u; invalidate(); }
    public void setRange(int minValue,int maxValue){ min=minValue; max=maxValue; }
    public void setValue(int v){ value = wrap(v); invalidate(); }
    public int getValue(){ return value; }
    public void setSelectedState(boolean s){ selected = s; invalidate(); }

    @Override protected void onMeasure(int w,int h){
        int width = MeasureSpec.getSize(w);
        if(width<=0) width = dp(86);
        setMeasuredDimension(width, dp(88));
    }

    @Override protected void onDraw(Canvas c){
        super.onDraw(c);
        RectF rect = new RectF(dp(2), dp(2), getWidth()-dp(2), getHeight()-dp(2));
        p.setStyle(Paint.Style.FILL); p.setColor(CREAM); c.drawRoundRect(rect, dp(24), dp(24), p);
        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(dp(selected?3f:2f)); p.setColor(selected?GREEN:BROWN); c.drawRoundRect(rect, dp(24), dp(24), p);
        p.setStyle(Paint.Style.FILL); p.setColor(BROWN); p.setTextAlign(Paint.Align.CENTER); p.setTypeface(Typeface.create(Typeface.SERIF, Typeface.BOLD)); p.setTextSize(dp(28));
        Paint.FontMetrics fm = p.getFontMetrics();
        c.drawText(String.valueOf(value), getWidth()/2f, getHeight()/2f - (fm.ascent+fm.descent)/2f - dp(6), p);
        p.setTypeface(Typeface.create(Typeface.SERIF, Typeface.NORMAL)); p.setTextSize(dp(10)); c.drawText(unit, getWidth()/2f, getHeight()-dp(14), p);
    }

    @Override public boolean onTouchEvent(MotionEvent e){
        if(e.getAction()==MotionEvent.ACTION_DOWN){ startY = e.getY(); if(listener!=null) listener.onSelected(this); return true; }
        if(e.getAction()==MotionEvent.ACTION_MOVE){
            if(!selected) return true;
            float dy = e.getY()-startY;
            if(Math.abs(dy) > dp(18)){
                setValue(value + (dy<0 ? 1 : -1));
                startY = e.getY();
                if(listener!=null) listener.onValueChanged(this, value);
            }
            return true;
        }
        return super.onTouchEvent(e);
    }

    private int wrap(int v){ int range = max-min+1; while(v<min)v+=range; while(v>max)v-=range; return v; }
    private int dp(float x){ return (int)(x*getResources().getDisplayMetrics().density); }
}
