package com.forcefocus.app;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.Button;

public final class GradientDrawableCompat {
    public static void bg(View v,int color,int radius){
        GradientDrawable g=new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(radius);
        v.setBackground(g);
    }

    public static void panel(View v,int startColor,int endColor,int radius,int strokeColor){
        GradientDrawable g=new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{startColor,endColor});
        g.setCornerRadius(radius);
        g.setStroke(2, strokeColor);
        v.setBackground(g);
    }

    public static void button(Button b,int startColor,int endColor,int strokeColor,int textColor){
        GradientDrawable g=new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{startColor,endColor});
        g.setCornerRadius(999);
        g.setStroke(2, strokeColor);
        b.setBackground(g);
        b.setTextColor(textColor);
    }

    public static int alpha(int color,int alpha){
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }
}
