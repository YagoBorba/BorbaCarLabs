package io.rea.obd.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

public final class VehicleArtwork extends View {
    private final Paint ink;
    private final ThemePalette palette;

    public VehicleArtwork(Context context) {
        super(context);
        this.ink = new Paint(1);
        this.palette = ThemePalette.of(context);
        setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    private void line(Canvas canvas, float... value) {
        Path path = new Path();
        path.moveTo(value[0], value[1]);
        for (int i = 2; i < value.length; i += 2) {
            path.lineTo(value[i], value[i + 1]);
        }
        canvas.drawPath(path, this.ink);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.save();
        float min = Math.min(getWidth() / 360.0f, getHeight() / 150.0f);
        canvas.translate((getWidth() - (360.0f * min)) / 2.0f, (getHeight() - (150.0f * min)) / 2.0f);
        canvas.scale(min, min);
        this.ink.setStyle(Paint.Style.STROKE);
        this.ink.setStrokeCap(Paint.Cap.ROUND);
        this.ink.setStrokeJoin(Paint.Join.ROUND);
        this.ink.setColor(this.palette.accent);
        this.ink.setAlpha(22);
        this.ink.setStrokeWidth(1.0f);
        canvas.drawOval(30.0f, 98.0f, 337.0f, 143.0f, this.ink);
        canvas.drawOval(3.0f, 85.0f, 357.0f, 151.0f, this.ink);
        this.ink.setAlpha(48);
        line(canvas, 24.0f, 132.0f, 337.0f, 132.0f);
        line(canvas, 96.0f, 14.0f, 264.0f, 14.0f);
        line(canvas, 180.0f, 7.0f, 180.0f, 21.0f);
        this.ink.setAlpha(255);
        this.ink.setStrokeWidth(2.2f);
        Path path = new Path();
        path.moveTo(35.0f, 99.0f);
        path.cubicTo(39.0f, 84.0f, 53.0f, 79.0f, 87.0f, 74.0f);
        path.lineTo(128.0f, 40.0f);
        path.cubicTo(151.0f, 31.0f, 203.0f, 31.0f, 226.0f, 42.0f);
        path.lineTo(265.0f, 73.0f);
        path.cubicTo(292.0f, 78.0f, 318.0f, 81.0f, 324.0f, 97.0f);
        path.lineTo(325.0f, 117.0f);
        path.lineTo(299.0f, 119.0f);
        path.cubicTo(295.0f, 84.0f, 255.0f, 84.0f, 251.0f, 119.0f);
        path.lineTo(112.0f, 119.0f);
        path.cubicTo(109.0f, 84.0f, 68.0f, 84.0f, 65.0f, 119.0f);
        path.lineTo(36.0f, 113.0f);
        path.close();
        canvas.drawPath(path, this.ink);
        this.ink.setStrokeWidth(1.6f);
        line(canvas, 98.0f, 74.0f, 135.0f, 46.0f, 174.0f, 43.0f, 174.0f, 74.0f, 98.0f, 74.0f);
        line(canvas, 184.0f, 43.0f, 218.0f, 48.0f, 252.0f, 74.0f, 184.0f, 74.0f, 184.0f, 43.0f);
        this.ink.setAlpha(110);
        line(canvas, 181.0f, 80.0f, 181.0f, 111.0f);
        line(canvas, 122.0f, 80.0f, 119.0f, 110.0f);
        line(canvas, 239.0f, 81.0f, 245.0f, 106.0f);
        line(canvas, 188.0f, 84.0f, 201.0f, 84.0f);
        line(canvas, 128.0f, 84.0f, 141.0f, 84.0f);
        this.ink.setAlpha(255);
        canvas.drawCircle(89.0f, 117.0f, 19.0f, this.ink);
        canvas.drawCircle(275.0f, 117.0f, 19.0f, this.ink);
        this.ink.setAlpha(100);
        canvas.drawCircle(89.0f, 117.0f, 10.0f, this.ink);
        canvas.drawCircle(275.0f, 117.0f, 10.0f, this.ink);
        this.ink.setAlpha(255);
        this.ink.setStrokeWidth(3.0f);
        line(canvas, 41.0f, 94.0f, 61.0f, 91.0f);
        line(canvas, 303.0f, 91.0f, 319.0f, 95.0f);
        this.ink.setAlpha(80);
        this.ink.setStrokeWidth(1.0f);
        line(canvas, 149.0f, 130.0f, 216.0f, 130.0f);
        canvas.restore();
    }
}