package com.forcefocus.app;

import android.content.Context;
import android.graphics.*;
import android.view.View;

public class IconButtonView extends View {
    public static final int ACCESS=1, ALARM=2, FOCUS=3, CALENDAR=4, PLAY=5;
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    private int icon;
    private int fg=Color.parseColor("#331915");
    private int bg=Color.TRANSPARENT;
    public IconButtonView(Context c,int icon){super(c);this.icon=icon;setClickable(true);}
    public void setColors(int fg,int bg){this.fg=fg;this.bg=bg;invalidate();}
    @Override protected void onMeasure(int w,int h){setMeasuredDimension(dp(48),dp(48));}
    @Override protected void onDraw(Canvas c){super.onDraw(c);float cx=getWidth()/2f,cy=getHeight()/2f;
        if(bg!=Color.TRANSPARENT){p.setColor(bg);p.setStyle(Paint.Style.FILL);c.drawCircle(cx,cy,dp(21),p);} p.setColor(fg);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(2.3f));p.setStrokeCap(Paint.Cap.ROUND);
        if(icon==ACCESS){c.drawCircle(cx,cy-dp(10),dp(4),p);c.drawLine(cx,cy-dp(4),cx,cy+dp(9),p);c.drawLine(cx-dp(8),cy,cx+dp(8),cy,p);c.drawLine(cx,cy+dp(9),cx-dp(7),cy+dp(16),p);c.drawLine(cx,cy+dp(9),cx+dp(7),cy+dp(16),p);}
        else if(icon==ALARM){c.drawCircle(cx,cy,dp(12),p);c.drawLine(cx,cy,cx,cy-dp(7),p);c.drawLine(cx,cy,cx+dp(6),cy+dp(4),p);c.drawLine(cx-dp(10),cy-dp(14),cx-dp(15),cy-dp(10),p);c.drawLine(cx+dp(10),cy-dp(14),cx+dp(15),cy-dp(10),p);}
        else if(icon==FOCUS){c.drawCircle(cx,cy,dp(13),p);c.drawCircle(cx,cy,dp(4),p);}
        else if(icon==CALENDAR){RectF r=new RectF(cx-dp(13),cy-dp(11),cx+dp(13),cy+dp(13));c.drawRoundRect(r,dp(4),dp(4),p);c.drawLine(cx-dp(13),cy-dp(4),cx+dp(13),cy-dp(4),p);c.drawLine(cx-dp(7),cy-dp(15),cx-dp(7),cy-dp(8),p);c.drawLine(cx+dp(7),cy-dp(15),cx+dp(7),cy-dp(8),p);}
        else if(icon==PLAY){p.setStyle(Paint.Style.FILL);Path path=new Path();path.moveTo(cx-dp(5),cy-dp(9));path.lineTo(cx+dp(10),cy);path.lineTo(cx-dp(5),cy+dp(9));path.close();c.drawPath(path,p);}
    }
    private int dp(float x){return (int)(x*getResources().getDisplayMetrics().density);}
}
