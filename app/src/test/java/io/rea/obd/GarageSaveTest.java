package io.rea.obd;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import io.rea.obd.model.FactProvenance;
import io.rea.obd.ui.NavigationState;
import io.rea.obd.vehicle.VehicleAttribute;
import io.rea.obd.vehicle.VehicleRecord;
import io.rea.obd.vehicle.VehicleRepository;
import java.util.Arrays;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowDialog;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, qualifiers = "pt-rBR-w393dp-h852dp-xhdpi")
public class GarageSaveTest {
    private static void idle() {
        shadowOf(Looper.getMainLooper()).idle();
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

    private static Button button(View view, String text) {
        if (view instanceof Button && ((Button) view).getText().toString().equals(text)) {
            return (Button) view;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                Button found = button(group.getChildAt(i), text);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static void press(Dialog editor, String text) {
        View root = editor.getWindow().getDecorView();
        Button button = button(root, text);
        assertNotNull("Botão \"" + text + "\" ausente:\n" + texts(root), button);
        assertTrue("Botão \"" + text + "\" desativado", button.isEnabled());
        button.performClick();
        idle();
    }

    private static AlertDialog list() {
        AlertDialog list = ShadowAlertDialog.getLatestAlertDialog();
        assertNotNull(list);
        assertTrue(list.isShowing());
        return list;
    }

    private static void choose(String option) {
        AlertDialog list = list();
        CharSequence[] items = shadowOf(list).getItems();
        int index = Arrays.asList(items).stream().map(CharSequence::toString).toList().indexOf(option);
        assertTrue("Opção " + option + " ausente em " + Arrays.toString(items), index >= 0);
        shadowOf(list).clickOnItem(index);
        idle();
    }

    private static void chooseFirstVersion() {
        AlertDialog list = list();
        assertTrue(shadowOf(list).getItems().length > 0);
        shadowOf(list).clickOnItem(0);
        idle();
    }

    private static void pick(Dialog editor, VehicleAttribute.Key key, String option) {
        View field = editor.getWindow().getDecorView().findViewWithTag("vehicle-field-" + key.name());
        assertNotNull("Campo " + key + " ausente", field);
        field.performClick();
        idle();
        choose(option);
    }

    private static Dialog openEditor(MainActivity activity, VehicleRecord record) {
        activity.editVehicle(record == null ? null : record.projection());
        idle();
        Dialog editor = ShadowDialog.getLatestDialog();
        assertNotNull(editor);
        assertTrue(editor.isShowing());
        return editor;
    }

    @Test
    public void savingACarChosenFromTheCatalogClosesTheEditorAndSelectsTheCar() throws Exception {
        ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup();
        MainActivity activity = controller.get();
        activity.navigate(NavigationState.GARAGE);
        idle();
        Dialog editor = openEditor(activity, null);
        pick(editor, VehicleAttribute.Key.MAKE, "Chevrolet");
        pick(editor, VehicleAttribute.Key.MODEL, "Cobalt");
        pick(editor, VehicleAttribute.Key.MODEL_YEAR, "2019");
        press(editor, "Continuar");
        press(editor, "Escolher versão do catálogo");
        chooseFirstVersion();
        press(editor, "Continuar");
        press(editor, "Salvar perfil");
        assertFalse("O editor continuou aberto:\n" + texts(editor.getWindow().getDecorView()), editor.isShowing());

        VehicleRecord saved = new VehicleRepository(activity.getPreferences(0)).activeRecord();
        assertNotNull(saved);
        assertEquals("Cobalt 2019", saved.name);
        assertEquals("Chevrolet", saved.attribute(VehicleAttribute.Key.MAKE).value);
        assertNotNull(saved.attribute(VehicleAttribute.Key.ENGINE).value);
        assertEquals(FactProvenance.Origin.CATALOG, saved.attribute(VehicleAttribute.Key.ENGINE).provenance.origin);
        assertTrue(texts(activity.getWindow().getDecorView()).contains("Cobalt 2019"));
        controller.pause().stop().destroy();
    }

    @Test
    public void renamingASavedCatalogCarSavesANewRevision() throws Exception {
        ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup();
        MainActivity activity = controller.get();
        activity.navigate(NavigationState.GARAGE);
        idle();
        Dialog editor = openEditor(activity, null);
        pick(editor, VehicleAttribute.Key.MAKE, "Chevrolet");
        pick(editor, VehicleAttribute.Key.MODEL, "Cobalt");
        pick(editor, VehicleAttribute.Key.MODEL_YEAR, "2019");
        press(editor, "Continuar");
        press(editor, "Continuar");
        press(editor, "Salvar perfil");
        assertFalse(editor.isShowing());

        VehicleRecord first = new VehicleRepository(activity.getPreferences(0)).activeRecord();
        Dialog again = openEditor(activity, first);
        EditText name = again.getWindow().getDecorView().findViewWithTag("vehicle-name");
        assertNotNull(name);
        name.setText("Cobalt da família");
        press(again, "Continuar");
        press(again, "Continuar");
        press(again, "Salvar perfil");
        assertFalse("O editor continuou aberto:\n" + texts(again.getWindow().getDecorView()), again.isShowing());

        VehicleRecord renamed = new VehicleRepository(activity.getPreferences(0)).activeRecord();
        assertEquals(first.id, renamed.id);
        assertEquals(first.revision + 1, renamed.revision);
        assertEquals("Cobalt da família", renamed.name);
        assertEquals(first.attribute(VehicleAttribute.Key.MODEL), renamed.attribute(VehicleAttribute.Key.MODEL));
        controller.pause().stop().destroy();
    }
}
