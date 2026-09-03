package com.forcefocus.app;

import android.content.Context;
import android.graphics.*;
import android.view.View;

public class PlayButtonView extends View {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int GREEN = Color.parseColor("#8AA832");
    private final int CREAM = Color.parseColor("#FFFBD3");

    public PlayButtonView(Context c){ super(c); setClickable(true); }

    @Override protected void onDraw(Canvas c){
        super.onDraw(c);
        RectF box = new RectF(dp(2),dp(2),getWidth()-dp(2),getHeight()-dp(2));
        p.setStyle(Paint.Style.FILL); p.setColor(GREEN);
        c.drawRoundRect(box,dp(28),dp(28),p);
        float cx=getWidth()/2f, cy=getHeight()/2f;
        Path tri=new Path();
        tri.moveTo(cx-dp(8),cy-dp(13));
        tri.lineTo(cx+dp(14),cy);
        tri.lineTo(cx-dp(8),cy+dp(13));
        tri.close();
        p.setColor(CREAM); c.drawPath(tri,p);
    }
    private int dp(float x){ return (int)(x*getResources().getDisplayMetrics().density); }
}
