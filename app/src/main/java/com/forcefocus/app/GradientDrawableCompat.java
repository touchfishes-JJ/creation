package com.forcefocus.app;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
public final class GradientDrawableCompat {
 public static void bg(View v,int color,int radius){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(radius);v.setBackground(g);}
}
