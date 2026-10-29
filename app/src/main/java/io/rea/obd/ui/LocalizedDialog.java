package io.rea.obd.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;

public final class LocalizedDialog extends AlertDialog.Builder {
    public LocalizedDialog(Context context) {
        super(context);
    }

    @Override
    public AlertDialog.Builder setTitle(CharSequence title) {
        return super.setTitle(I18n.t(title));
    }

    @Override
    public AlertDialog.Builder setMessage(CharSequence message) {
        return super.setMessage(I18n.t(message));
    }

    @Override
    public AlertDialog.Builder setItems(CharSequence[] items, DialogInterface.OnClickListener listener) {
        return super.setItems(I18n.t(items), listener);
    }

    @Override
    public AlertDialog.Builder setSingleChoiceItems(CharSequence[] items, int checkedItem, DialogInterface.OnClickListener listener) {
        return super.setSingleChoiceItems(I18n.t(items), checkedItem, listener);
    }

    @Override
    public AlertDialog.Builder setMultiChoiceItems(CharSequence[] items, boolean[] checkedItems, DialogInterface.OnMultiChoiceClickListener listener) {
        return super.setMultiChoiceItems(I18n.t(items), checkedItems, listener);
    }

    @Override
    public AlertDialog.Builder setPositiveButton(CharSequence text, DialogInterface.OnClickListener listener) {
        return super.setPositiveButton(I18n.t(text), listener);
    }

    @Override
    public AlertDialog.Builder setNegativeButton(CharSequence text, DialogInterface.OnClickListener listener) {
        return super.setNegativeButton(I18n.t(text), listener);
    }

    @Override
    public AlertDialog.Builder setNeutralButton(CharSequence text, DialogInterface.OnClickListener listener) {
        return super.setNeutralButton(I18n.t(text), listener);
    }

    @Override
    public AlertDialog.Builder setView(View view) {
        I18n.apply(view);
        return super.setView(view);
    }

    @Override
    public AlertDialog show() {
        AlertDialog dialog = super.show();
        if (dialog.getWindow() != null) {
            I18n.apply(dialog.getWindow().getDecorView());
        }
        return dialog;
    }
}
