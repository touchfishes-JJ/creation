package com.forcefocus.app;

import android.content.Context;
import android.graphics.*;
import android.view.View;

public class CalendarDayView extends View {
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int GREEN=Color.parseColor("#8AA832");
    private final int BROWN=Color.parseColor("#331915");
    private int day=0; private float fraction=0f; private boolean marked=false;
    public CalendarDayView(Context c){super(c);setClickable(true);}
    public void setData(int d,float f,boolean m){day=d;fraction=Math.max(0f,Math.min(1f,f));marked=m;invalidate();}
    @Override protected void onMeasure(int w,int h){int width=MeasureSpec.getSize(w); if(width<=0) width=dp(48); setMeasuredDimension(width, dp(66));}
    @Override protected void onDraw(Canvas c){
        super.onDraw(c); if(day<=0) return;
        float pad=dp(2), topSquare=dp(2), size=getWidth()-pad*2, r=dp(10);
        RectF box=new RectF(pad,topSquare,getWidth()-pad,topSquare+size);
        p.setStyle(Paint.Style.FILL); p.setColor(Color.parseColor("#F4F0C7")); c.drawRoundRect(box,r,r,p);
        if(fraction>0){ float top=box.bottom-box.height()*fraction; c.save(); Path clip=new Path(); clip.addRoundRect(box,r,r,Path.Direction.CW); c.clipPath(clip); p.setColor(GREEN); c.drawRect(box.left,top,box.right,box.bottom,p); c.restore(); }
        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(dp(1.5f)); p.setColor(Color.parseColor("#C8BF8A")); c.drawRoundRect(box,r,r,p);
        if(marked) drawTree(c,box.right-dp(4),box.top+dp(4));
        p.setStyle(Paint.Style.FILL); p.setColor(BROWN); p.setTextAlign(Paint.Align.CENTER); p.setTypeface(Typeface.create(Typeface.SERIF,Typeface.NORMAL)); p.setTextSize(dp(11)); c.drawText(String.valueOf(day),getWidth()/2f,getHeight()-dp(6),p);
    }
    private void drawTree(Canvas c,float x,float y){
        p.setStyle(Paint.Style.FILL); p.setColor(Color.parseColor("#5A8D21")); c.drawCircle(x-dp(5),y+dp(6),dp(4.2f),p); c.drawCircle(x,y+dp(3),dp(4.8f),p); c.drawCircle(x+dp(5),y+dp(6),dp(4.2f),p); p.setColor(Color.parseColor("#7B4A1F")); c.drawRect(x-dp(1),y+dp(7),x+dp(1),y+dp(13),p);
    }
    private int dp(float x){return (int)(x*getResources().getDisplayMetrics().density);}
}
