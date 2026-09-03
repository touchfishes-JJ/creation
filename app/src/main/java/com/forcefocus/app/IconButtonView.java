package com.forcefocus.app;

import android.content.Context;
import android.graphics.*;
import android.view.View;

public class IconButtonView extends View {
    public static final int ACCESS=1, ALARM=2;
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    private int icon;
    private int fg=Color.parseColor("#331915");
    public IconButtonView(Context c,int icon){super(c);this.icon=icon;setClickable(true);}
    public void setColors(int fg,int unusedBg){this.fg=fg;invalidate();}
    @Override protected void onMeasure(int w,int h){setMeasuredDimension(dp(34),dp(34));}
    @Override protected void onDraw(Canvas c){
        super.onDraw(c);
        float cx=getWidth()/2f, cy=getHeight()/2f;
        p.setColor(fg);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStyle(Paint.Style.STROKE);
        if(icon==ALARM){
            p.setStrokeWidth(dp(4f));
            c.drawLine(cx-dp(12), cy+dp(6), cx-dp(1), cy+dp(6), p);
            c.drawLine(cx+dp(1), cy-dp(14), cx+dp(1), cy+dp(6), p);
            c.drawLine(cx+dp(1), cy+dp(6), cx+dp(15), cy+dp(6), p);
            p.setStrokeWidth(dp(3.5f));
            c.drawLine(cx-dp(9), cy-dp(8), cx-dp(5), cy-dp(4), p);
            c.drawLine(cx+dp(8), cy-dp(8), cx+dp(12), cy-dp(4), p);
            c.drawLine(cx+dp(1), cy+dp(11), cx+dp(1), cy+dp(17), p);
            c.drawLine(cx+dp(10), cy+dp(10), cx+dp(14), cy+dp(14), p);
            c.drawLine(cx-dp(9), cy+dp(10), cx-dp(13), cy+dp(14), p);
        } else if(icon==ACCESS){
            p.setColor(Color.parseColor("#7350E6"));
            p.setStrokeWidth(dp(3f));
            Path path=new Path();
            path.moveTo(cx-dp(7), cy-dp(11));
            path.lineTo(cx-dp(1), cy-dp(14));
            path.lineTo(cx+dp(7), cy-dp(11));
            path.lineTo(cx+dp(11), cy-dp(6));
            path.lineTo(cx+dp(11), cy+dp(6));
            path.lineTo(cx+dp(7), cy+dp(11));
            path.lineTo(cx-dp(1), cy+dp(14));
            path.lineTo(cx-dp(7), cy+dp(11));
            path.lineTo(cx-dp(11), cy+dp(6));
            path.lineTo(cx-dp(11), cy-dp(6));
            path.close();
            c.drawPath(path,p);
            p.setStyle(Paint.Style.FILL);
            c.drawCircle(cx-dp(2), cy+dp(1), dp(3.2f), p);
            c.drawCircle(cx+dp(4), cy-dp(3), dp(2.8f), p);
            p.setStyle(Paint.Style.STROKE);
            c.drawLine(cx-dp(2), cy+dp(1), cx+dp(2), cy+dp(8), p);
            c.drawLine(cx+dp(4), cy-dp(3), cx+dp(5), cy-dp(9), p);
        }
    }
    private int dp(float x){return (int)(x*getResources().getDisplayMetrics().density);}    
}
