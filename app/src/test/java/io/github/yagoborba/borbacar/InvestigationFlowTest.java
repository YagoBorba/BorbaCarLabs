package io.github.yagoborba.borbacar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.app.AlertDialog;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import io.github.yagoborba.borbacar.ui.NavigationState;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, qualifiers = "pt-rBR-w393dp-h852dp-xhdpi")
public class InvestigationFlowTest {

    @Test
    public void aReadingBecomesACaseThatCanBeRevised() throws Exception {
        ScreensTest.seedVehicleAndJournal();
        ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup();
        MainActivity activity = controller.get();
        activity.startDemo();
        activity.navigate(NavigationState.DIAGNOSIS);
        activity.scan();
        waitForWorkers();

        View investigate = withText(activity.getWindow().getDecorView(), "Investigar esta leitura");
        assertNotNull(investigate);
        investigate.performClick();
        idle();

        AlertDialog form = ShadowAlertDialog.getLatestAlertDialog();
        assertTrue(texts(form.getWindow().getDecorView()).contains("Nova investigação"));
        symptom(form, 0).setText("Falha ao acelerar em subida");
        form.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        waitForWorkers();

        AlertDialog saved = ShadowAlertDialog.getLatestAlertDialog();
        assertNotSame(form, saved);
        String first = texts(saved.getWindow().getDecorView());
        assertTrue(first.contains("Investigação · revisão 1"));
        assertTrue(first.contains("Falha ao acelerar em subida"));

        withText(saved.getWindow().getDecorView(), "Editar sintomas / criar revisão").performClick();
        idle();
        AlertDialog revision = ShadowAlertDialog.getLatestAlertDialog();
        assertTrue(texts(revision.getWindow().getDecorView()).contains("Revisar sintomas"));
        assertEquals("Falha ao acelerar em subida", symptom(revision, 0).getText().toString());
        symptom(revision, 1).setText("Só com o motor frio");
        revision.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        waitForWorkers();

        String second = texts(ShadowAlertDialog.getLatestAlertDialog().getWindow().getDecorView());
        assertTrue(second.contains("Investigação · revisão 2"));
        assertTrue(second.contains("Só com o motor frio"));
        controller.pause().stop().destroy();
    }

    private static EditText symptom(AlertDialog dialog, int index) {
        EditText field = dialog.getWindow().getDecorView().findViewWithTag("case-symptom-" + index);
        assertNotNull(field);
        return field;
    }

    private static View withText(View view, String text) {
        if (view instanceof TextView && text.contentEquals(((TextView) view).getText())) {
            return view;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                View found = withText(group.getChildAt(i), text);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static String texts(View view) {
        StringBuilder builder = new StringBuilder();
        if (view instanceof TextView) {
            builder.append(((TextView) view).getText()).append('\n');
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                builder.append(texts(group.getChildAt(i)));
            }
        }
        return builder.toString();
    }

    private static void idle() {
        shadowOf(Looper.getMainLooper()).idle();
    }

    private static void waitForWorkers() throws InterruptedException {
        for (int i = 0; i < 20; i++) {
            Thread.sleep(50);
            idle();
        }
    }
}
