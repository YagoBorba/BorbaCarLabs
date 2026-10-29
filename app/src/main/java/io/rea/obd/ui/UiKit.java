package io.rea.obd.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class UiKit {
    public static final int ACCENT = -11147583;
    public static final int AMBER = -13190;
    public static final int BG = -16051680;
    public static final int CARD = -15392976;
    public static final int LINE = -14273205;
    public static final int MUTED = -6706494;
    public static final int TEXT = -920069;
    public final Context context;
    public final ThemePalette palette;

    public UiKit(Context context) {
        this.context = context;
        this.palette = ThemePalette.of(context);
    }

    public int dp(int i) {
        return (int) ((i * this.context.getResources().getDisplayMetrics().density) + 0.5f);
    }

    public GradientDrawable surface(boolean flag) {
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setColor(this.palette.card);
        gradientDrawable.setCornerRadius(dp(22));
        gradientDrawable.setStroke(dp(1), flag ? this.palette.accent : this.palette.line);
        return gradientDrawable;
    }

    public LinearLayout column() {
        LinearLayout linearLayout = new LinearLayout(this.context);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        return linearLayout;
    }

    public TextView label(LinearLayout linearLayout, String text, int i, int value, boolean flag) {
        TextView textView = new TextView(this.context);
        textView.setText(I18n.t(text));
        textView.setTextSize(i);
        textView.setTextColor(this.palette.color(value));
        textView.setTypeface(Typeface.create(flag ? "sans-serif-medium" : "sans-serif", Typeface.NORMAL));
        textView.setFontFeatureSettings("tnum");
        textView.setLineSpacing(dp(2), 1.0f);
        textView.setPadding(0, 0, 0, dp(8));
        linearLayout.addView(textView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return textView;
    }

    public LinearLayout card(LinearLayout linearLayout, boolean flag) {
        LinearLayout column = column();
        column.setPadding(dp(18), dp(18), dp(18), dp(14));
        column.setBackground(surface(flag));
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.bottomMargin = dp(16);
        linearLayout.addView(column, layoutParams);
        return column;
    }

    public Button button(LinearLayout linearLayout, String text, boolean flag, final Runnable runnable) {
        Button button = new Button(this.context);
        button.setText(I18n.t(text));
        button.setAllCaps(false);
        button.setTextSize(15.0f);
        button.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        this.palette.button(button, flag);
        Motion.press(button);
        button.setMinHeight(dp(52));
        button.setPadding(dp(16), dp(14), dp(16), dp(14));
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.topMargin = dp(8);
        layoutParams.bottomMargin = dp(4);
        linearLayout.addView(button, layoutParams);
        button.setOnClickListener((view) -> runnable.run());
        return button;
    }

    public void eyebrow(LinearLayout linearLayout, String category) {
        label(linearLayout, category, 11, ACCENT, true).setLetterSpacing(0.12f);
    }

    public LinearLayout feature(LinearLayout linearLayout) {
        LinearLayout card = card(linearLayout, false);
        GradientDrawable gradientDrawable = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{this.palette.light ? -2034709 : -15319751, this.palette.card});
        gradientDrawable.setCornerRadius(dp(24));
        gradientDrawable.setStroke(dp(1), this.palette.light ? -4531249 : -13870768);
        card.setBackground(gradientDrawable);
        return card;
    }

    public void artwork(LinearLayout linearLayout) {
        artwork(linearLayout, 120);
    }

    public void artwork(LinearLayout linearLayout, int heightDp) {
        VehicleArtwork artwork = new VehicleArtwork(this.context);
        artwork.setContentDescription(I18n.t("Ilustração genérica de um carro"));
        linearLayout.addView(artwork, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(heightDp)));
    }

    public void section(LinearLayout linearLayout, String text, String text2) {
        label(linearLayout, text, 20, TEXT, true);
        if (text2 == null || text2.isEmpty()) {
            return;
        }
        label(linearLayout, text2, 14, MUTED, false);
    }

    public Button action(LinearLayout linearLayout, String text, String text2, int i, Runnable runnable) {
        LinearLayout card = card(linearLayout, false);
        Button button = button(card, text, false, runnable);
        button.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        Drawable mutate = this.context.getDrawable(i).mutate();
        mutate.setTint(this.palette.accent);
        mutate.setBounds(0, 0, dp(22), dp(22));
        button.setCompoundDrawablesRelative(mutate, null, null, null);
        button.setCompoundDrawablePadding(dp(12));
        label(card, text2, 13, MUTED, false);
        return button;
    }

    public EditText input(LinearLayout linearLayout, String title, String serverUrl, String tag) {
        TextView label = label(linearLayout, title, 14, TEXT, true);
        EditText editText = new EditText(this.context);
        editText.setTextColor(this.palette.text);
        editText.setHintTextColor(this.palette.muted);
        editText.setTextSize(16.0f);
        editText.setText(serverUrl);
        editText.setSingleLine(true);
        editText.setMinHeight(dp(48));
        editText.setPadding(dp(12), dp(10), dp(12), dp(10));
        editText.setTag(tag);
        editText.setId(View.generateViewId());
        label.setLabelFor(editText.getId());
        editText.setBackground(surface(false));
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.bottomMargin = dp(12);
        linearLayout.addView(editText, layoutParams);
        return editText;
    }

    public Button row(LinearLayout linearLayout, String text, Runnable runnable) {
        Button button = button(linearLayout, text, false, runnable);
        button.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        TypedValue typedValue = new TypedValue();
        this.context.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, typedValue, true);
        button.setBackgroundResource(typedValue.resourceId);
        button.setTextColor(this.palette.text);
        trailing(button, io.rea.obd.R.drawable.ic_chevron);
        return button;
    }

    public LinearLayout disclosure(LinearLayout linearLayout, final String text) {
        final LinearLayout column = column();
        final Button row = row(linearLayout, text, () -> {
        });
        row.setTag("disclosure-" + text);
        trailing(row, io.rea.obd.R.drawable.ic_expand);
        column.setVisibility(View.GONE);
        linearLayout.addView(column, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        row.setContentDescription(I18n.t(text + ". Recolhido"));
        row.setOnClickListener((view) -> {
            boolean flag = column.getVisibility() != View.VISIBLE;
            column.setVisibility(flag ? View.VISIBLE : View.GONE);
            trailing(row, flag ? io.rea.obd.R.drawable.ic_collapse : io.rea.obd.R.drawable.ic_expand);
            StringBuilder builder = new StringBuilder();
            builder.append(text);
            builder.append(flag ? ". Expandido" : ". Recolhido");
            row.setContentDescription(I18n.t(builder.toString()));
            if (flag) {
                Motion.enter(column);
            }
        });
        return column;
    }

    private void trailing(Button button, int i) {
        Drawable mutate = this.context.getDrawable(i).mutate();
        mutate.setTint(this.palette.muted);
        mutate.setBounds(0, 0, dp(20), dp(20));
        button.setCompoundDrawablesRelative(null, null, mutate, null);
        button.setCompoundDrawablePadding(dp(8));
    }
}