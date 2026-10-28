package io.rea.obd.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.StateListDrawable;
import android.view.Window;
import android.widget.Button;
import io.rea.obd.R;

public final class ThemePalette {
    public static final String DARK = "dark";
    public static final String LIGHT = "light";
    public static final String SYSTEM = "system";
    public final int accent;
    public final int attention;
    public final int bg;
    public final int card;
    public final boolean light;
    public final int line;
    public final int muted;
    public final int onAccent;
    public final int text;

    public ThemePalette(boolean light) {
        this.light = light;
        int i = UiKit.BG;
        this.bg = light ? -722950 : -16051680;
        this.card = light ? -1 : UiKit.CARD;
        this.line = light ? -1971985 : UiKit.LINE;
        this.text = light ? -15260104 : UiKit.TEXT;
        this.muted = light ? -11838352 : UiKit.MUTED;
        this.accent = light ? -16749739 : UiKit.ACCENT;
        this.attention = light ? -8040448 : UiKit.AMBER;
        this.onAccent = light ? -1 : i;
    }

    public static String preference(Context context) {
        String string = context.getSharedPreferences("appearance", 0).getString("theme", SYSTEM);
        return (LIGHT.equals(string) || DARK.equals(string)) ? string : SYSTEM;
    }

    public static ThemePalette of(Context context) {
        String preference = preference(context);
        boolean light = true;
        boolean flag = (context.getResources().getConfiguration().uiMode & 48) == 32;
        if (!LIGHT.equals(preference) && (!SYSTEM.equals(preference) || flag)) {
            light = false;
        }
        return new ThemePalette(light);
    }

    public static boolean save(Context context, String theme) {
        if (SYSTEM.equals(theme) || LIGHT.equals(theme) || DARK.equals(theme)) {
            return context.getSharedPreferences("appearance", 0).edit().putString("theme", theme).commit();
        }
        throw new IllegalArgumentException("Tema desconhecido");
    }

    public int style() {
        return this.light ? R.style.LightAppTheme : R.style.AppTheme;
    }

    public int color(int i) {
        if (i == -16051680) {
            return this.bg;
        }
        if (i == -15392976) {
            return this.card;
        }
        if (i == -14273205) {
            return this.line;
        }
        if (i == -920069) {
            return this.text;
        }
        if (i == -6706494) {
            return this.muted;
        }
        if (i == -11147583) {
            return this.accent;
        }
        return i == -13190 ? this.attention : i;
    }

    private GradientDrawable shape(Context context, int i, boolean flag) {
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setColor(i);
        gradientDrawable.setCornerRadius(context.getResources().getDisplayMetrics().density * 14.0f);
        if (flag) {
            gradientDrawable.setStroke((int) (context.getResources().getDisplayMetrics().density * 2.0f), this.accent);
        }
        return gradientDrawable;
    }

    public void button(Button button, boolean flag) {
        int[][] intss = {new int[]{-16842910}, new int[]{android.R.attr.state_focused}, new int[0]};
        button.setTextColor(new ColorStateList(intss, new int[]{this.muted, flag ? this.onAccent : this.text, flag ? this.onAccent : this.text}));
        StateListDrawable stateListDrawable = new StateListDrawable();
        stateListDrawable.addState(intss[0], shape(button.getContext(), this.line, false));
        stateListDrawable.addState(intss[1], shape(button.getContext(), flag ? this.accent : this.line, true));
        stateListDrawable.addState(intss[2], shape(button.getContext(), flag ? this.accent : this.line, false));
        button.setBackground(new RippleDrawable(ColorStateList.valueOf((this.accent & 16777215) | 855638016), stateListDrawable, null));
    }

    public Drawable navigation(Context context, boolean flag) {
        int i;
        if (flag) {
            i = this.light ? -2101271 : -14664133;
        } else {
            i = this.card;
        }
        return new RippleDrawable(ColorStateList.valueOf((this.accent & 16777215) | 855638016), shape(context, i, false), null);
    }

    public void window(Window window) {
        window.setStatusBarColor(this.bg);
        window.setNavigationBarColor(this.bg);
        int systemUiVisibility = window.getDecorView().getSystemUiVisibility();
        window.getDecorView().setSystemUiVisibility(this.light ? systemUiVisibility | 8208 : systemUiVisibility & (-8209));
    }
}