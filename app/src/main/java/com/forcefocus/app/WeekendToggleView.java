package com.forcefocus.app;

import android.content.Context;
import android.graphics.*;
import android.view.View;

public class WeekendToggleView extends View {
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int GREEN=Color.parseColor("#8AA832");
    private final int CREAM=Color.parseColor("#FFFBD3");
    private final int BROWN=Color.parseColor("#331915");
    private boolean checked=true;
    public WeekendToggleView(Context c){super(c);setClickable(true);}
    public void setChecked(boolean c){checked=c;invalidate();}
    public boolean isChecked(){return checked;}
    @Override protected void onMeasure(int w,int h){setMeasuredDimension(dp(72),dp(38));}
    @Override protected void onDraw(Canvas c){
        RectF r=new RectF(dp(1),dp(1),getWidth()-dp(1),getHeight()-dp(1));
        p.setStyle(Paint.Style.FILL);p.setColor(checked?GREEN:Color.parseColor("#E6DFC0"));c.drawRoundRect(r,dp(20),dp(20),p);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(1.5f));p.setColor(BROWN);c.drawRoundRect(r,dp(20),dp(20),p);
        p.setStyle(Paint.Style.FILL);p.setColor(CREAM);
        float cx=checked?getWidth()-dp(19):dp(19);c.drawCircle(cx,getHeight()/2f,dp(14),p);
    }
    private int dp(float x){return (int)(x*getResources().getDisplayMetrics().density);}
}
