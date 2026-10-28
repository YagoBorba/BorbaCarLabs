package io.rea.obd;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;
import android.view.accessibility.AccessibilityNodeInfo;
import io.rea.obd.telemetry.DashboardState;
import io.rea.obd.ui.ThemePalette;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public final class ChartView extends View {
    private SelectionListener listener;
    private final Paint paint;
    private final ThemePalette palette;
    private List<Point> points;
    private int selected;
    private String unit;

    public interface SelectionListener {
        void selected(Point point);
    }

    public static final class Point {
        public final boolean breakBefore;
        public final long time;
        public final double value;

        public Point(long time, double value2) {
            this(time, value2, false);
        }

        public Point(long time, double value2, boolean breakBefore) {
            this.time = time;
            this.value = value2;
            this.breakBefore = breakBefore;
        }
    }

    public ChartView(Context context) {
        super(context);
        this.paint = new Paint(1);
        this.points = Collections.emptyList();
        this.unit = "";
        this.selected = -1;
        this.palette = ThemePalette.of(context);
        setClickable(true);
        setFocusable(true);
        setMinimumHeight(dp(150.0f));
        setContentDescription("Gráfico de capturas. Sem amostras.");
    }

    private int dp(float value) {
        return (int) ((value * getResources().getDisplayMetrics().density) + 0.5f);
    }

    private float sp(float value) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value, getResources().getDisplayMetrics());
    }

    public void setSelectionListener(SelectionListener selectionListener) {
        this.listener = selectionListener;
    }

    public void data(List<Point> list, String unit) {
        SelectionListener selectionListener;
        Point selectedPoint = selectedPoint();
        ArrayList items = new ArrayList();
        for (Point point : list) {
            if (point != null && point.time >= 0 && Double.isFinite(point.value)) {
                items.add(point);
            }
        }
        this.points = Collections.unmodifiableList(items);
        this.unit = unit;
        this.selected = -1;
        if (selectedPoint != null) {
            for (int i = 0; i < this.points.size(); i++) {
                if (this.points.get(i).time == selectedPoint.time && this.points.get(i).value == selectedPoint.value) {
                    this.selected = i;
                    break;
                }
            }
        }
        if (selectedPoint != null && this.selected < 0 && (selectionListener = this.listener) != null) {
            selectionListener.selected(null);
        }
        setContentDescription("Gráfico de capturas. " + summary() + " Use Amostra anterior e Próxima amostra para explorar.");
        int selected = this.selected;
        if (selected >= 0) {
            select(selected);
        }
        invalidate();
    }

    public Point selectedPoint() {
        int i = this.selected;
        if (i < 0 || i >= this.points.size()) {
            return null;
        }
        return this.points.get(this.selected);
    }

    public int pointCount() {
        return this.points.size();
    }

    public static boolean gap(Point point, Point point2) {
        return point2.breakBefore || point2.time <= point.time || point2.time - point.time > DashboardState.MAX_AGE_MS;
    }

    public String summary() {
        if (this.points.isEmpty()) {
            return "Nenhuma captura de rotação nesta janela.";
        }
        double max = Double.NEGATIVE_INFINITY;
        double min = Double.POSITIVE_INFINITY;
        int i = 0;
        for (int i2 = 0; i2 < this.points.size(); i2++) {
            Point point = this.points.get(i2);
            min = Math.min(min, point.value);
            max = Math.max(max, point.value);
            if (i2 > 0 && gap(this.points.get(i2 - 1), point)) {
                i++;
            }
        }
        return String.format(Locale.getDefault(), "%d capturas · mín %.0f · máx %.0f %s · %d lacunas", Integer.valueOf(this.points.size()), Double.valueOf(min), Double.valueOf(max), this.unit, Integer.valueOf(i)) + "\nEixo X: horário UTC de aquisição. Lacunas de resposta ou acima de 15 s não são conectadas.";
    }

    private long start() {
        Iterator<Point> it = this.points.iterator();
        long min = Long.MAX_VALUE;
        while (it.hasNext()) {
            min = Math.min(min, it.next().time);
        }
        return min;
    }

    private long end() {
        Iterator<Point> it = this.points.iterator();
        long max = Long.MIN_VALUE;
        while (it.hasNext()) {
            max = Math.max(max, it.next().time);
        }
        return max;
    }

    private float left() {
        this.paint.setTextSize(sp(11.0f));
        Iterator<Point> it = this.points.iterator();
        double max = 0.0d;
        while (it.hasNext()) {
            max = Math.max(max, Math.abs(it.next().value));
        }
        return Math.min(getWidth() * 0.4f, this.paint.measureText(String.format(Locale.getDefault(), "%.0f", Double.valueOf(max))) + dp(10.0f));
    }

    private float x(Point point) {
        float left = left();
        return left + (((point.time - start()) * Math.max(1.0f, (getWidth() - left) - dp(5.0f))) / Math.max(1L, end() - start()));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        this.paint.setTextSize(sp(11.0f));
        this.paint.setColor(this.palette.muted);
        this.paint.setStyle(Paint.Style.FILL);
        if (this.points.isEmpty()) {
            canvas.drawText("Sem amostras", dp(4.0f), getHeight() / 2.0f, this.paint);
            return;
        }
        float left = left();
        float sp = sp(12.0f);
        float height = (getHeight() - sp(14.0f)) - dp(8.0f);
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (Point point : this.points) {
            min = Math.min(min, point.value);
            max = Math.max(max, point.value);
        }
        double value = max - min;
        float max2 = Math.max(1.0f, height - sp);
        this.paint.setStrokeWidth(dp(1.0f));
        this.paint.setColor(this.palette.line);
        int i = 0;
        while (i < 3) {
            float value2 = sp + ((i * max2) / 2.0f);
            canvas.drawLine(left, value2, getWidth(), value2, this.paint);
            i++;
            max = max;
        }
        this.paint.setColor(this.palette.muted);
        canvas.drawText(String.format(Locale.getDefault(), "%.0f", Double.valueOf(max)), 0.0f, sp(4.0f) + sp, this.paint);
        canvas.drawText(String.format(Locale.getDefault(), "%.0f", Double.valueOf(min)), 0.0f, height, this.paint);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss", Locale.ROOT);
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        String text = simpleDateFormat.format(new Date(start()));
        String text2 = simpleDateFormat.format(new Date(end()));
        canvas.drawText(text, 0.0f, getHeight() - dp(2.0f), this.paint);
        canvas.drawText(text2, Math.max(0.0f, getWidth() - this.paint.measureText(text2)), getHeight() - dp(2.0f), this.paint);
        Path path = new Path();
        for (int i2 = 0; i2 < this.points.size(); i2++) {
            Point points = this.points.get(i2);
            float x = x(points);
            float value3 = value == 0.0d ? (max2 / 2.0f) + sp : height - (((float) ((points.value - min) / value)) * max2);
            if (i2 == 0 || gap(this.points.get(i2 - 1), points)) {
                path.moveTo(x, value3);
            } else {
                path.lineTo(x, value3);
            }
            this.paint.setColor(this.palette.accent);
            this.paint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(x, value3, dp(2.0f), this.paint);
        }
        this.paint.setColor(this.palette.accent);
        this.paint.setStyle(Paint.Style.STROKE);
        this.paint.setStrokeWidth(dp(2.0f));
        canvas.drawPath(path, this.paint);
        this.paint.setStyle(Paint.Style.FILL);
        Point selectedPoint = selectedPoint();
        if (selectedPoint != null) {
            this.paint.setColor(this.palette.attention);
            this.paint.setStrokeWidth(dp(1.0f));
            canvas.drawLine(x(selectedPoint), sp, x(selectedPoint), height, this.paint);
        }
    }

    public void moveSelection(int i) {
        if (this.points.isEmpty()) {
            return;
        }
        int max = 0;
        if (this.selected >= 0) {
            max = Math.max(0, Math.min(this.points.size() - 1, this.selected + i));
        } else if (i < 0) {
            max = this.points.size() - 1;
        }
        select(max);
    }

    private void select(int i) {
        this.selected = i;
        Point selectedPoint = selectedPoint();
        SelectionListener selectionListener = this.listener;
        if (selectionListener != null) {
            selectionListener.selected(selectedPoint);
        }
        if (selectedPoint != null) {
            setContentDescription(io.rea.obd.ui.Format.when(selectedPoint.time) + " · " + String.format(Locale.getDefault(), "%.0f %s", Double.valueOf(selectedPoint.value), this.unit) + " · captura histórica " + (i + 1) + " de " + this.points.size());
        }
        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        if (this.points.isEmpty()) {
            return true;
        }
        boolean flag = false;
        double value = Double.POSITIVE_INFINITY;
        int i = 0;
        for (int i2 = 0; i2 < this.points.size(); i2++) {
            double value2 = Math.abs(x(this.points.get(i2)) - motionEvent.getX());
            if (value2 < value) {
                i = i2;
                value = value2;
            }
        }
        select(i);
        if (getParent() != null) {
            ViewParent parent = getParent();
            if (motionEvent.getAction() != 1 && motionEvent.getAction() != 3) {
                flag = true;
            }
            parent.requestDisallowInterceptTouchEvent(flag);
        }
        if (motionEvent.getAction() == 1) {
            performClick();
        }
        return true;
    }

    @Override
    public boolean onKeyDown(int i, KeyEvent keyEvent) {
        if (i != 21 && i != 22) {
            return super.onKeyDown(i, keyEvent);
        }
        moveSelection(i == 21 ? -1 : 1);
        return true;
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo accessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(accessibilityNodeInfo);
        accessibilityNodeInfo.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD);
        accessibilityNodeInfo.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_BACKWARD);
    }

    @Override
    public boolean performAccessibilityAction(int i, Bundle bundle) {
        if (i != 4096 && i != 8192) {
            return super.performAccessibilityAction(i, bundle);
        }
        moveSelection(i == 4096 ? 1 : -1);
        return true;
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }
}