package io.rea.obd;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.app.Dialog;
import android.os.Looper;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import io.rea.obd.journal.JournalEntry;
import io.rea.obd.journal.JournalStore;
import java.math.BigDecimal;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowDialog;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, qualifiers = "pt-rBR-w393dp-h852dp-xhdpi")
public class FuelFormTest {
    private static void idle() {
        shadowOf(Looper.getMainLooper()).idle();
    }

    private static void waitForWorkers() throws InterruptedException {
        for (int i = 0; i < 20; i++) {
            Thread.sleep(50);
            idle();
        }
    }

    private static View tagged(Dialog dialog, String tag) {
        View view = dialog.getWindow().getDecorView().findViewWithTag(tag);
        assertNotNull(tag, view);
        return view;
    }

    @Test
    public void todayOpensAFuelFormThatFillsInWhatItCan() throws Exception {
        ScreensTest.seedVehicleAndJournal();
        ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup();
        MainActivity activity = controller.get();
        waitForWorkers();
        View shortcut = activity.getWindow().getDecorView().findViewWithTag("home-fuel");
        assertNotNull(shortcut);
        shortcut.performClick();
        waitForWorkers();
        Dialog form = ShadowDialog.getLatestDialog();
        assertTrue(form.isShowing());
        EditText odometer = (EditText) tagged(form, "journal-odometer");
        assertEquals("Último registro: 48.480 km", odometer.getHint().toString());
        assertEquals("Gasolina", ((Spinner) tagged(form, "journal-fuel")).getSelectedItem().toString());

        odometer.setText("48000");
        ((EditText) tagged(form, "journal-liters")).setText("40");
        ((EditText) tagged(form, "journal-price")).setText("5,99");
        assertEquals("Total calculado: R$ 239,60", ((TextView) tagged(form, "journal-computed")).getText().toString());
        CheckBox full = (CheckBox) tagged(form, "journal-full");
        full.setChecked(true);
        assertTrue(((CheckBox) tagged(form, "journal-complete")).isChecked());
        form.getWindow().getDecorView().findViewWithTag("journal-error");
        clickSave(form);
        assertTrue(((TextView) tagged(form, "journal-error")).getText().toString().startsWith("O hodômetro está menor que o último registro"));

        odometer.setText("48900");
        clickSave(form);
        waitForWorkers();
        assertFalse(form.isShowing());
        JournalStore store = new JournalStore(RuntimeEnvironment.getApplication());
        JournalEntry saved = null;
        for (JournalEntry entry : store.latest()) {
            if (entry.kind == JournalEntry.Kind.FUEL && entry.odometer.compareTo(new BigDecimal("48900")) == 0) {
                saved = entry;
            }
        }
        store.close();
        assertNotNull(saved);
        assertEquals(0, new BigDecimal("239.60").compareTo(saved.cost));
        assertTrue(saved.fullTank && saved.completeInterval);
        controller.pause().stop().destroy();
    }

    private static void clickSave(Dialog form) {
        View root = form.getWindow().getDecorView();
        View save = find(root, "Salvar registro");
        assertNotNull(save);
        save.performClick();
        idle();
    }

    private static View find(View view, String text) {
        if (view instanceof TextView && ((TextView) view).getText().toString().equals(text)) {
            return view;
        }
        if (view instanceof android.view.ViewGroup) {
            android.view.ViewGroup group = (android.view.ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                View found = find(group.getChildAt(i), text);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}
