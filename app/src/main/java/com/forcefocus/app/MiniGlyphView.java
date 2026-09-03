package com.forcefocus.app;

import android.content.Context;
import android.graphics.*;
import android.view.View;

public class MiniGlyphView extends View {
    public static final int TREE=1, FOREST=2, CALENDAR=3, SETTINGS=4, LIST=5, LOCK=6, MEMORY=7, PLAY=8, APP_WPS=20, APP_XHS=21, APP_FENBI=22, APP_RECORD=23, ACCESS=24, ALARM=25;
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG); private int type; private int tint=Color.parseColor("#331915");
    public MiniGlyphView(Context c,int type){super(c);this.type=type;}
    public void setTint(int c){tint=c;invalidate();}
    @Override protected void onMeasure(int w,int h){int s=MeasureSpec.getSize(w);if(s<=0)s=dp(28);setMeasuredDimension(s,s);}
    @Override protected void onDraw(Canvas c){super.onDraw(c);switch(type){case TREE:drawTree(c);break;case FOREST:drawForest(c);break;case CALENDAR:drawCalendar(c);break;case SETTINGS:drawSettings(c);break;case LIST:drawList(c);break;case LOCK:drawLock(c);break;case MEMORY:drawMemory(c);break;case PLAY:drawPlay(c);break;case APP_WPS:drawWps(c);break;case APP_XHS:drawBook(c);break;case APP_FENBI:drawPencil(c);break;case APP_RECORD:drawRecord(c);break;case ACCESS:drawAccess(c);break;case ALARM:drawAlarm(c);break;}}
    private void drawTree(Canvas c){float cx=getWidth()/2f,cy=getHeight()/2f;p.setStyle(Paint.Style.FILL);p.setColor(Color.parseColor("#8AA832"));c.drawCircle(cx,cy-dp(6),dp(7),p);c.drawCircle(cx-dp(6),cy,dp(6),p);c.drawCircle(cx+dp(6),cy,dp(6),p);p.setColor(Color.parseColor("#6E411E"));c.drawRoundRect(new RectF(cx-dp(2),cy+dp(2),cx+dp(2),cy+dp(12)),dp(2),dp(2),p);}
    private void drawForest(Canvas c){float cx=getWidth()/2f,cy=getHeight()/2f;pine(c,cx-dp(10),cy+dp(2),dp(9));pine(c,cx,cy-dp(2),dp(11));pine(c,cx+dp(10),cy+dp(3),dp(8));}
    private void pine(Canvas c,float x,float y,int s){p.setStyle(Paint.Style.FILL);p.setColor(Color.parseColor("#8AA832"));Path path=new Path();path.moveTo(x,y-s);path.lineTo(x-s,y);path.lineTo(x-dp(4),y);path.lineTo(x-s,y+dp(7));path.lineTo(x+s,y+dp(7));path.lineTo(x+dp(4),y);path.lineTo(x+s,y);path.close();c.drawPath(path,p);p.setColor(Color.parseColor("#6E411E"));c.drawRect(x-dp(1),y+dp(7),x+dp(1),y+dp(13),p);}
    private void drawCalendar(Canvas c){RectF r=new RectF(dp(4),dp(6),getWidth()-dp(4),getHeight()-dp(4));p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(2.5f));p.setColor(tint);c.drawRoundRect(r,dp(6),dp(6),p);c.drawLine(dp(4),dp(14),getWidth()-dp(4),dp(14),p);c.drawLine(dp(10),dp(3),dp(10),dp(10),p);c.drawLine(getWidth()-dp(10),dp(3),getWidth()-dp(10),dp(10),p);}
    private void drawSettings(Canvas c){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(2.5f));p.setColor(tint);c.drawCircle(getWidth()/2f,getHeight()/2f,dp(6),p);p.setStyle(Paint.Style.FILL);for(int i=0;i<6;i++){double a=Math.PI/3*i;c.drawCircle((float)(getWidth()/2f+Math.cos(a)*dp(11)),(float)(getHeight()/2f+Math.sin(a)*dp(11)),dp(2),p);}}
    private void drawList(Canvas c){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(2.8f));p.setColor(tint);for(int i=0;i<3;i++){float y=dp(8+i*8);c.drawCircle(dp(8),y,dp(1.2f),p);c.drawLine(dp(13),y,getWidth()-dp(6),y,p);}}
    private void drawLock(Canvas c){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(2.5f));p.setColor(tint);c.drawRoundRect(new RectF(dp(7),dp(15),getWidth()-dp(7),getHeight()-dp(6)),dp(5),dp(5),p);c.drawArc(new RectF(dp(11),dp(6),getWidth()-dp(11),dp(20)),180,180,false,p);}
    private void drawMemory(Canvas c){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(2.5f));p.setColor(tint);c.drawArc(new RectF(dp(6),dp(8),getWidth()-dp(6),getHeight()-dp(8)),35,290,false,p);Path q=new Path();q.moveTo(dp(7),dp(12));q.lineTo(dp(14),dp(12));q.lineTo(dp(11),dp(6));c.drawPath(q,p);}
    private void drawPlay(Canvas c){p.setStyle(Paint.Style.FILL);p.setColor(tint);Path q=new Path();q.moveTo(dp(11),dp(7));q.lineTo(getWidth()-dp(8),getHeight()/2f);q.lineTo(dp(11),getHeight()-dp(7));q.close();c.drawPath(q,p);}
    private void drawWps(Canvas c){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(2.5f));p.setColor(tint);c.drawLine(dp(10),getHeight()-dp(9),getWidth()-dp(10),dp(9),p);c.drawLine(dp(14),getHeight()-dp(9),getWidth()-dp(6),dp(13),p);}
    private void drawBook(Canvas c){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(2.4f));p.setColor(tint);c.drawRoundRect(new RectF(dp(6),dp(7),getWidth()/2f,getHeight()-dp(7)),dp(3),dp(3),p);c.drawRoundRect(new RectF(getWidth()/2f,dp(7),getWidth()-dp(6),getHeight()-dp(7)),dp(3),dp(3),p);c.drawLine(getWidth()/2f,dp(7),getWidth()/2f,getHeight()-dp(7),p);}
    private void drawPencil(Canvas c){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(2.4f));p.setColor(tint);c.drawLine(dp(8),getHeight()-dp(8),getWidth()-dp(10),dp(10),p);c.drawLine(getWidth()-dp(10),dp(10),getWidth()-dp(7),dp(13),p);c.drawLine(dp(8),getHeight()-dp(8),dp(11),getHeight()-dp(5),p);}
    private void drawRecord(Canvas c){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(2.4f));p.setColor(tint);c.drawRoundRect(new RectF(dp(12),dp(6),getWidth()-dp(12),dp(18)),dp(4),dp(4),p);c.drawLine(getWidth()/2f,dp(18),getWidth()/2f,dp(25),p);c.drawArc(new RectF(dp(9),dp(18),getWidth()-dp(9),getHeight()-dp(6)),0,180,false,p);}
    private void drawAccess(Canvas c){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(2.4f));p.setColor(Color.parseColor("#7350E6"));c.drawCircle(getWidth()/2f,getHeight()/2f,dp(11),p);c.drawLine(dp(10),dp(10),getWidth()-dp(10),getHeight()-dp(10),p);c.drawLine(getWidth()-dp(10),dp(10),dp(10),getHeight()-dp(10),p);}
    private void drawAlarm(Canvas c){p.setStyle(Paint.Style.STROKE);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeWidth(dp(3f));p.setColor(tint);float cx=getWidth()/2f,cy=getHeight()/2f;c.drawLine(cx-dp(12),cy+dp(4),cx-dp(1),cy+dp(4),p);c.drawLine(cx+dp(1),cy-dp(12),cx+dp(1),cy+dp(4),p);c.drawLine(cx+dp(1),cy+dp(4),cx+dp(13),cy+dp(4),p);}
    private int dp(float x){return (int)(x*getResources().getDisplayMetrics().density);}
}
