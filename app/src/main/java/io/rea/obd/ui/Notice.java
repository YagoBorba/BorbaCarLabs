package io.rea.obd.ui;

import android.app.Activity;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import android.widget.TextView;

public final class Notice {
    private static final String TAG = "borbacar-notice";
    private static final long VISIBLE_MS = 3500L;

    private Notice() {
    }

    public static void show(Activity activity, String text) {
        if (activity.isFinishing()) {
            return;
        }
        ViewGroup decor = (ViewGroup) activity.getWindow().getDecorView();
        View previous = decor.findViewWithTag(TAG);
        if (previous != null) {
            decor.removeView(previous);
        }
        UiKit ui = new UiKit(activity);
        TextView view = new TextView(activity);
        view.setTag(TAG);
        view.setText(text);
        view.setTextSize(15.0f);
        view.setTextColor(ui.palette.bg);
        view.setPadding(ui.dp(18), ui.dp(14), ui.dp(18), ui.dp(14));
        view.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        GradientDrawable background = new GradientDrawable();
        background.setColor(ui.palette.text);
        background.setCornerRadius(ui.dp(14));
        view.setBackground(background);
        view.setElevation(ui.dp(6));
        view.setOnClickListener(v -> dismiss(decor, view));

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM);
        params.setMargins(ui.dp(16), 0, ui.dp(16), ui.dp(104) + bottomInset(decor));
        decor.addView(view, params);

        if (Motion.enabled(activity)) {
            view.setAlpha(0.0f);
            view.setTranslationY(ui.dp(12));
            view.animate().alpha(1.0f).translationY(0.0f).setDuration(180L).start();
        }
        view.postDelayed(() -> dismiss(decor, view), VISIBLE_MS);
    }

    private static void dismiss(ViewGroup decor, View view) {
        if (view.getParent() != decor) {
            return;
        }
        if (Motion.enabled(view.getContext())) {
            view.animate().alpha(0.0f).setDuration(160L).withEndAction(() -> decor.removeView(view)).start();
        } else {
            decor.removeView(view);
        }
    }

    private static int bottomInset(View decor) {
        WindowInsets insets = decor.getRootWindowInsets();
        if (insets == null) {
            return 0;
        }
        if (Build.VERSION.SDK_INT >= 30) {
            return insets.getInsets(WindowInsets.Type.systemBars()).bottom;
        }
        return insets.getSystemWindowInsetBottom();
    }
}
