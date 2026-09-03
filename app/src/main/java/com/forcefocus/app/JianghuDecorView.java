package com.forcefocus.app;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;

public class JianghuDecorView extends View {
    private final Paint bgPaint = new Paint();
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stemPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint leafPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint flutePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mistPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public JianghuDecorView(Context c){ super(c); init(); }
    public JianghuDecorView(Context c, AttributeSet a){ super(c,a); init(); }

    private void init(){
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(2f);
        stemPaint.setStyle(Paint.Style.STROKE);
        stemPaint.setStrokeWidth(7f);
        stemPaint.setStrokeCap(Paint.Cap.ROUND);
        leafPaint.setStyle(Paint.Style.FILL);
        flutePaint.setStyle(Paint.Style.STROKE);
        flutePaint.setStrokeCap(Paint.Cap.ROUND);
        flutePaint.setStrokeWidth(8f);
        mistPaint.setStyle(Paint.Style.FILL);
    }

    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);
        int w=getWidth(), h=getHeight();
        LinearGradient bg = new LinearGradient(0,0,0,h,
                new int[]{Color.rgb(6,25,18), Color.rgb(14,45,34), Color.rgb(8,25,18)},
                new float[]{0f,0.5f,1f}, Shader.TileMode.CLAMP);
        bgPaint.setShader(bg);
        canvas.drawRect(0,0,w,h,bgPaint);
        drawMist(canvas,w,h);
        drawPattern(canvas,w,h);
        drawBambooCluster(canvas, 0.12f*w, h*0.05f, h*0.92f, 1.0f);
        drawBambooCluster(canvas, 0.90f*w, h*0.12f, h*0.88f, -1.0f);
        drawFlute(canvas,w,h);
    }

    private void drawMist(Canvas c,int w,int h){
        mistPaint.setColor(Color.argb(16,232,255,240));
        c.drawOval(new RectF(w*0.12f,h*0.08f,w*0.72f,h*0.24f),mistPaint);
        mistPaint.setColor(Color.argb(10,232,255,240));
        c.drawOval(new RectF(w*0.30f,h*0.70f,w*0.92f,h*0.92f),mistPaint);
    }

    private void drawPattern(Canvas c,int w,int h){
        linePaint.setColor(Color.argb(18,190,245,214));
        for(int i=-h;i<w+h;i+=38){ c.drawLine(i,h*0.72f,i+h*0.38f,h,linePaint); }
        linePaint.setColor(Color.argb(10,215,255,230));
        for(int y=(int)(h*0.14f); y<h*0.66f; y+=26){ c.drawLine(w*0.20f,y,w*0.78f,y,linePaint); }
    }

    private void drawBambooCluster(Canvas c,float x,float top,float bottom,float dir){
        stemPaint.setColor(Color.argb(70, 175, 233, 192));
        linePaint.setColor(Color.argb(90, 210, 255, 225));
        for(int i=0;i<3;i++){
            float sx = x + i*26f*dir;
            c.drawLine(sx, top, sx, bottom, stemPaint);
            for(float y=top+55;y<bottom;y+=95){ c.drawLine(sx-12, y, sx+12, y, linePaint); }
        }
        leafPaint.setColor(Color.argb(88, 130, 220, 165));
        drawLeaf(c,x+18*dir, top+90, 62, -28*dir);
        drawLeaf(c,x-12*dir, top+160, 72, 30*dir);
        drawLeaf(c,x+15*dir, top+260, 66, -35*dir);
        drawLeaf(c,x-10*dir, top+340, 72, 28*dir);
        drawLeaf(c,x+18*dir, top+430, 68, -33*dir);
    }

    private void drawLeaf(Canvas c,float x,float y,float len,float angle){
        Path p = new Path();
        p.moveTo(x,y);
        p.quadTo(x+len*0.55f, y-18, x+len, y);
        p.quadTo(x+len*0.55f, y+18, x, y);
        Matrix m = new Matrix();
        m.setRotate(angle, x, y);
        p.transform(m);
        c.drawPath(p,leafPaint);
    }

    private void drawFlute(Canvas c,int w,int h){
        float x1=w*0.58f, y1=h*0.10f, x2=w*0.90f, y2=h*0.18f;
        flutePaint.setColor(Color.argb(92, 217, 255, 230));
        c.drawLine(x1,y1,x2,y2,flutePaint);
        linePaint.setColor(Color.argb(130, 232, 255, 238));
        linePaint.setStrokeWidth(4f);
        for(int i=0;i<5;i++){
            float t=0.16f + i*0.13f;
            c.drawCircle(x1+(x2-x1)*t, y1+(y2-y1)*t, 5f,linePaint);
        }
        linePaint.setStrokeWidth(2f);
    }
}
