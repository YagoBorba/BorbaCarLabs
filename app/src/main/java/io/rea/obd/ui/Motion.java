package io.rea.obd.ui;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.PathInterpolator;

public final class Motion {
    private Motion() {
    }

    public static boolean reduced(Context context) {
        return context.getSharedPreferences("appearance", 0).getBoolean("reduce_motion", false);
    }

    public static boolean enabled(Context context) {
        return !reduced(context) && ValueAnimator.areAnimatorsEnabled();
    }

    public static boolean saveReduced(Context context, boolean reduceMotion) {
        return context.getSharedPreferences("appearance", 0).edit().putBoolean("reduce_motion", reduceMotion).commit();
    }

    public static void enter(View view) {
        view.animate().cancel();
        view.setTranslationY(0.0f);
        view.setAlpha(1.0f);
        if (enabled(view.getContext())) {
            view.setTranslationY(view.getResources().getDisplayMetrics().density * 12.0f);
            view.animate().translationY(0.0f).setDuration(220L).setInterpolator(new PathInterpolator(0.2f, 0.0f, 0.0f, 1.0f)).start();
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    public static void press(View view) {
        view.setStateListAnimator(null);
        view.setScaleX(1.0f);
        view.setScaleY(1.0f);
        view.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public final boolean onTouch(View view2, MotionEvent motionEvent) {
                return Motion.animatePress(view2, motionEvent);
            }
        });
        view.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View view3) {
            }

            @Override
            public void onViewDetachedFromWindow(View view4) {
                view4.animate().cancel();
                view4.setScaleX(1.0f);
                view4.setScaleY(1.0f);
            }
        });
    }

    static boolean animatePress(View view, MotionEvent motionEvent) {
        int actionMasked;
        if (view.isEnabled() && enabled(view.getContext()) && ((actionMasked = motionEvent.getActionMasked()) == MotionEvent.ACTION_DOWN || actionMasked == MotionEvent.ACTION_UP || actionMasked == MotionEvent.ACTION_CANCEL)) {
            float value = actionMasked == MotionEvent.ACTION_DOWN ? 0.98f : 1.0f;
            view.animate().cancel();
            view.animate().scaleX(value).scaleY(value).setDuration(actionMasked == MotionEvent.ACTION_DOWN ? 90L : 160L).start();
        }
        return false;
    }
}