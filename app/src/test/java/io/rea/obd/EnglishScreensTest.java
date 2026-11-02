package io.rea.obd;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import io.rea.obd.journal.JournalEntry;
import io.rea.obd.journal.JournalStore;
import io.rea.obd.ui.NavigationState;
import io.rea.obd.vehicle.VehicleAttribute;
import io.rea.obd.vehicle.VehicleProfile;
import io.rea.obd.vehicle.VehicleRepository;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowDialog;

@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = 35, qualifiers = "en-rUS-w393dp-h852dp-xhdpi")
public class EnglishScreensTest {
    private static final File OUT = new File("build/screenshots/en");
    private static final Pattern PORTUGUESE = Pattern.compile(
            "[ãõçáéíóúâêôÃÕÇÁÉÍÓÚÂÊÔ]|\\b(de|da|dos|das|não|com|para|em|uma|um|sem|seu|sua|você|leitura|leituras|veículo|registro|registros|consulta|hoje|ontem|consumo|gasolina|etanol|cidade|estrada|dias|sensores|ajustes|garagem|sintomas|tanque|cheio|abastecimentos|simulado|acompanhamento|ligados|desligados|salvar|voltar|continuar|cancelar|fechar|remover|apagar|enviar|falha|falhas|indicada|indicadas|simulador|ano|modelo|modelos|marca|marcas|fabricante|mercado|perfil|apelido|opcional|detalhes|escolher|informar|outros|podem|como|simples|meu|minha|carro)\\b",
            Pattern.CASE_INSENSITIVE);

    private final Set<String> untranslated = new LinkedHashSet<>();

    private static void idle() {
        shadowOf(Looper.getMainLooper()).idle();
    }

    private static void waitForWorkers() throws InterruptedException {
        for (int i = 0; i < 20; i++) {
            Thread.sleep(50);
            idle();
        }
    }

    private static void texts(View view, List<String> out) {
        if (view instanceof TextView) {
            out.add(((TextView) view).getText().toString());
            CharSequence hint = ((TextView) view).getHint();
            if (hint != null) {
                out.add(hint.toString());
            }
        }
        if (view.getContentDescription() != null) {
            out.add(view.getContentDescription().toString());
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                texts(group.getChildAt(i), out);
            }
        }
    }

    private void check(MainActivity activity, String name) throws IOException {
        idle();
        check(activity.getWindow().getDecorView(), name);
    }

    private void check(View root, String name) throws IOException {
        List<String> found = new ArrayList<>();
        texts(root, found);
        for (String text : found) {
            for (String line : text.split("\n")) {
                if (PORTUGUESE.matcher(line.replaceAll("https?://\\S+", "")).find() && !line.contains("Cobalt") && !line.contains("Português")) {
                    untranslated.add(name + " | " + line);
                }
            }
        }
        Bitmap bitmap = Bitmap.createBitmap(root.getWidth(), root.getHeight(), Bitmap.Config.ARGB_8888);
        root.draw(new Canvas(bitmap));
        OUT.mkdirs();
        try (FileOutputStream out = new FileOutputStream(new File(OUT, name + ".png"))) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        }
    }

    @Test
    public void everyScreenIsInEnglish() throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        long now = System.currentTimeMillis();
        long day = 24L * 60 * 60 * 1000;
        VehicleProfile cobalt = new VehicleProfile("car-1", "My Cobalt", "Chevrolet", "Cobalt", 2019, "BR",
                "LTZ", "1.8 L · 8V", "Flex", "Automático · 6 marchas", "");
        ActivityController<MainActivity> seed = Robolectric.buildActivity(MainActivity.class).setup();
        VehicleRepository vehicles = new VehicleRepository(seed.get().getPreferences(0));
        vehicles.save(cobalt);
        vehicles.select("car-1");
        JournalStore journal = new JournalStore(context);
        BigDecimal km = new BigDecimal("48000");
        journal.save(new JournalEntry("f1", "car-1", "My Cobalt", JournalEntry.Kind.FUEL, 1, now - 20 * day, now - 20 * day, now - 20 * day, "",
                km, new BigDecimal("40"), new BigDecimal("232.00"), "Gasolina", true, false, null, null));
        journal.save(new JournalEntry("f2", "car-1", "My Cobalt", JournalEntry.Kind.FUEL, 1, now - 2 * day, now - 2 * day, now - 2 * day, "",
                km.add(new BigDecimal("480")), new BigDecimal("38.5"), new BigDecimal("224.50"), "Gasolina", true, true, null, null));
        journal.save(new JournalEntry("s1", "car-1", "My Cobalt", JournalEntry.Kind.SERVICE, 1, now - 30 * day, now - 30 * day, now - 30 * day,
                "Oil and filter change", km.subtract(new BigDecimal("1200")), null, new BigDecimal("289.90"), null, false, false,
                LocalDate.now().plusDays(45).toString(), new BigDecimal("56800")));
        journal.close();
        seed.pause().stop().destroy();

        ActivityController<MainActivity> empty = Robolectric.buildActivity(MainActivity.class).setup();
        MainActivity activity = empty.get();
        waitForWorkers();
        check(activity, "01-today");
        activity.navigate(NavigationState.DIAGNOSIS);
        check(activity, "02-diagnosis-idle");
        activity.navigate(NavigationState.HISTORY);
        check(activity, "03-history");
        activity.navigate(NavigationState.GARAGE);
        check(activity, "04-garage");
        activity.navigate(NavigationState.SETTINGS);
        check(activity, "05-settings");
        activity.navigate(NavigationState.JOURNAL);
        waitForWorkers();
        check(activity, "06-journal");
        activity.startDemo();
        activity.navigate(NavigationState.DIAGNOSIS);
        activity.scan();
        waitForWorkers();
        check(activity, "07-diagnosis");
        activity.getWindow().getDecorView().findViewWithTag("action-understand").performClick();
        idle();
        android.app.AlertDialog explanation = org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();
        check(explanation.getWindow().getDecorView(), "07a-understand-reading");
        explanation.getButton(android.app.AlertDialog.BUTTON_NEUTRAL).performClick();
        idle();
        android.app.AlertDialog summary = org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();
        check(summary.getWindow().getDecorView(), "07b-workshop-summary");
        summary.dismiss();
        activity.navigate(NavigationState.LIVE);
        waitForWorkers();
        check(activity, "08-live");
        activity.navigate(NavigationState.HISTORY);
        waitForWorkers();
        check(activity, "09-history-after-reading");
        activity.navigate(NavigationState.HOME);
        waitForWorkers();
        check(activity, "10-today-after-reading");
        empty.pause().stop().destroy();

        assertAllTranslated("build/untranslated.txt");
    }

    private void assertAllTranslated(String path) throws IOException {
        Files.write(new File(path).toPath(), String.join("\n", untranslated).getBytes(StandardCharsets.UTF_8));
        assertTrue("Textos sem tradução:\n" + String.join("\n", untranslated), untranslated.isEmpty());
        assertEquals(0, untranslated.size());
    }

    private static View withText(View view, String text) {
        if (view instanceof TextView && ((TextView) view).getText().toString().equals(text)) {
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

    private static void tap(Dialog editor, String text) {
        View view = withText(editor.getWindow().getDecorView(), text);
        assertNotNull("\"" + text + "\" ausente no editor", view);
        view.performClick();
        idle();
    }

    private void pick(Dialog editor, VehicleAttribute.Key key, String option, String name) throws IOException {
        editor.getWindow().getDecorView().findViewWithTag("vehicle-field-" + key.name()).performClick();
        idle();
        AlertDialog list = ShadowAlertDialog.getLatestAlertDialog();
        check(list.getWindow().getDecorView(), name);
        int index = Arrays.asList(shadowOf(list).getItems()).stream().map(CharSequence::toString).toList().indexOf(option);
        assertTrue(option + " ausente", index >= 0);
        shadowOf(list).clickOnItem(index);
        idle();
    }

    @Test
    public void garageEditorIsInEnglish() throws Exception {
        ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup();
        MainActivity activity = controller.get();
        activity.navigate(NavigationState.GARAGE);
        idle();
        activity.editVehicle(null);
        idle();
        Dialog editor = ShadowDialog.getLatestDialog();
        check(editor.getWindow().getDecorView(), "11-editor-start");
        pick(editor, VehicleAttribute.Key.MAKE, "Chevrolet", "11a-make-list");
        pick(editor, VehicleAttribute.Key.MODEL, "Cobalt", "11b-model-list");
        pick(editor, VehicleAttribute.Key.MODEL_YEAR, "2019", "11c-year-list");
        check(editor.getWindow().getDecorView(), "12-editor-car");
        tap(editor, "Continue");
        check(editor.getWindow().getDecorView(), "13-editor-version");
        tap(editor, "Choose a version from the catalog");
        AlertDialog versions = ShadowAlertDialog.getLatestAlertDialog();
        check(versions.getWindow().getDecorView(), "13a-version-list");
        shadowOf(versions).clickOnItem(0);
        idle();
        tap(editor, "Other details (optional)");
        check(editor.getWindow().getDecorView(), "14-editor-details");
        editor.getWindow().getDecorView().findViewWithTag("vehicle-field-NOTES").performClick();
        idle();
        AlertDialog notes = ShadowAlertDialog.getLatestAlertDialog();
        check(notes.getWindow().getDecorView(), "14a-notes-list");
        shadowOf(notes).clickOnItem(shadowOf(notes).getItems().length - 1);
        idle();
        AlertDialog manual = ShadowAlertDialog.getLatestAlertDialog();
        check(manual.getWindow().getDecorView(), "14b-notes-manual");
        manual.dismiss();
        idle();
        tap(editor, "Continue");
        editor.getWindow().getDecorView().findViewWithTag("disclosure-Ver fontes e identificação completa").performClick();
        idle();
        check(editor.getWindow().getDecorView(), "15-editor-review");
        tap(editor, "Save profile");
        assertFalse(editor.isShowing());
        check(activity, "16-garage-with-catalog-car");
        controller.pause().stop().destroy();
        assertAllTranslated("build/untranslated-editor.txt");
    }
}
