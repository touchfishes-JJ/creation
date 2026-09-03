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
    @Override protected void onMeasure(int w,int h){int s=MeasureSpec.getSize(w);setMeasuredDimension(s,s);}
    @Override protected void onDraw(Canvas c){
        super.onDraw(c); if(day<=0)return;
        float pad=dp(3),r=dp(10); RectF box=new RectF(pad,pad,getWidth()-pad,getHeight()-pad);
        p.setColor(Color.rgb(247,242,196));p.setStyle(Paint.Style.FILL);c.drawRoundRect(box,r,r,p);
        if(fraction>0){float top=box.bottom-box.height()*fraction;RectF fill=new RectF(box.left,top,box.right,box.bottom);c.save();c.clipPath(roundRectPath(box,r));p.setColor(GREEN);c.drawRect(fill,p);c.restore();}
        p.setColor(BROWN);p.setTextAlign(Paint.Align.CENTER);p.setTypeface(Typeface.create(Typeface.SERIF,Typeface.BOLD));p.setTextSize(dp(11));c.drawText(String.valueOf(day),getWidth()/2f,getHeight()-dp(8),p);
        if(marked) drawTree(c,getWidth()-dp(10),dp(10));
    }
    private Path roundRectPath(RectF r,float rad){Path p=new Path();p.addRoundRect(r,rad,rad,Path.Direction.CW);return p;}
    private void drawTree(Canvas c,float x,float y){p.setStyle(Paint.Style.FILL);p.setColor(BROWN);c.drawRect(x-dp(1.5f),y+dp(6),x+dp(1.5f),y+dp(13),p);p.setColor(GREEN);c.drawCircle(x,y+dp(3),dp(6),p);c.drawCircle(x-dp(4),y+dp(6),dp(4.5f),p);c.drawCircle(x+dp(4),y+dp(6),dp(4.5f),p);}
    private int dp(float x){return (int)(x*getResources().getDisplayMetrics().density);}
}
