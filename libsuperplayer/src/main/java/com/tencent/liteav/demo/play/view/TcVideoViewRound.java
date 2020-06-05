package com.tencent.liteav.demo.play.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.SurfaceView;

import com.tencent.liteav.demo.play.R;
import com.tencent.rtmp.ui.TXCloudVideoView;


public class TcVideoViewRound extends TXCloudVideoView {
    private final Paint roundPaint;
    private final Paint imagePaint;

    private float radius=0;


    public TcVideoViewRound(Context context) {
        this(context,null);
    }

    public TcVideoViewRound(Context context, AttributeSet attributeSet) {
        super(context,attributeSet);
        radius = dp2px(context,8);
        setWillNotDraw(false);
        roundPaint = new Paint();
        roundPaint.setColor(Color.WHITE);
        roundPaint.setAntiAlias(true);
        roundPaint.setStyle(Paint.Style.FILL);
        roundPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));

        imagePaint = new Paint();
        imagePaint.setXfermode(null);
    }
    @Override
    protected void dispatchDraw(Canvas canvas) {
        canvas.saveLayer(new RectF(0, 0, canvas.getWidth(), canvas.getHeight()), imagePaint, Canvas.ALL_SAVE_FLAG);
        super.dispatchDraw(canvas);
        drawTopLeft(canvas);
        drawTopRight(canvas);
        drawBottomLeft(canvas);
        drawBottomRight(canvas);
        canvas.restore();
    }
    public static int dp2px(Context context, float dipValue) {
        final float scale = getScreenDensity(context);
        return (int) (dipValue * scale + 0.5);
    }
    public static float getScreenDensity(Context context) {
        return context.getResources().getDisplayMetrics().density;
    }
    private void drawTopLeft(Canvas canvas) {
        if (radius > 0) {
            Path path = new Path();
            path.moveTo(0, radius);
            path.lineTo(0, 0);
            path.lineTo(radius, 0);
            path.arcTo(new RectF(0, 0, radius * 2, radius * 2),
                    -90, -90);
            path.close();
            canvas.drawPath(path, roundPaint);
        }
    }

    private void drawTopRight(Canvas canvas) {
        if (radius > 0) {
            int width = getWidth();
            Path path = new Path();
            path.moveTo(width - radius, 0);
            path.lineTo(width, 0);
            path.lineTo(width, radius);
            path.arcTo(new RectF(width - 2 * radius, 0, width,
                    radius * 2), 0, -90);
            path.close();
            canvas.drawPath(path, roundPaint);
        }
    }

    private void drawBottomLeft(Canvas canvas) {
        if (radius > 0) {
            int height = getHeight();
            Path path = new Path();
            path.moveTo(0, height - radius);
            path.lineTo(0, height);
            path.lineTo(radius, height);
            path.arcTo(new RectF(0, height - 2 * radius,
                    radius * 2, height), 90, 90);
            path.close();
            canvas.drawPath(path, roundPaint);
        }
    }

    private void drawBottomRight(Canvas canvas) {
        if (radius > 0) {
            int height = getHeight();
            int width = getWidth();
            Path path = new Path();
            path.moveTo(width - radius, height);
            path.lineTo(width, height);
            path.lineTo(width, height - radius);
            path.arcTo(new RectF(width - 2 * radius, height - 2
                    * radius, width, height), 0, 90);
            path.close();
            canvas.drawPath(path, roundPaint);
        }
    }

}
