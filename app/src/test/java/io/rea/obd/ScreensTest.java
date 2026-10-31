package io.rea.obd;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Looper;
import android.view.View;
import io.rea.obd.ui.NavigationState;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = 35, qualifiers = "pt-rBR-w393dp-h852dp-xhdpi")
public class ScreensTest {
    private static final File OUT = new File("build/screenshots");

    private static void idle() {
        shadowOf(Looper.getMainLooper()).idle();
    }

    private static void capture(MainActivity activity, String name) throws IOException {
        idle();
        View root = activity.getWindow().getDecorView();
        assertTrue(name + " sem tamanho", root.getWidth() > 0 && root.getHeight() > 0);
        Bitmap bitmap = Bitmap.createBitmap(root.getWidth(), root.getHeight(), Bitmap.Config.ARGB_8888);
        root.draw(new Canvas(bitmap));
        OUT.mkdirs();
        try (FileOutputStream out = new FileOutputStream(new File(OUT, name + ".png"))) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        }
    }

    private static String texts(View view) {
        StringBuilder builder = new StringBuilder();
        if (view instanceof android.widget.TextView) {
            builder.append(((android.widget.TextView) view).getText()).append('\n');
        }
        if (view instanceof android.view.ViewGroup) {
            android.view.ViewGroup group = (android.view.ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                builder.append(texts(group.getChildAt(i)));
            }
        }
        return builder.toString();
    }

    @Test
    public void screensExplainTermsAndDescribeRemindersAsTheyWork() {
        ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup();
        MainActivity activity = controller.get();
        activity.startDemo();
        activity.navigate(NavigationState.DIAGNOSIS);
        idle();
        String diagnosis = texts(activity.getWindow().getDecorView());
        assertTrue(diagnosis.contains(io.rea.obd.ui.DiagnosisScreen.READINESS_HINT));
        assertTrue(diagnosis.contains(io.rea.obd.ui.DiagnosisScreen.FREEZE_FRAME_HINT));
        activity.navigate(NavigationState.JOURNAL);
        idle();
        String journal = texts(activity.getWindow().getDecorView());
        assertTrue(journal.contains("viram lembretes"));
        assertFalse(journal.contains("Notificações automáticas"));
        controller.pause().stop().destroy();
    }

    @Test
    public void diagnosisShowsTheResultBeforeTheConnectionControls() throws Exception {
        ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup();
        MainActivity activity = controller.get();
        activity.startDemo();
        activity.navigate(NavigationState.DIAGNOSIS);
        idle();
        String before = texts(activity.getWindow().getDecorView());
        assertTrue(before.indexOf("Iniciar leitura guiada") >= 0);
        assertFalse(before.contains("Conexão e nova leitura"));

        activity.scan();
        waitForWorkers();
        View root = activity.getWindow().getDecorView();
        String after = texts(root);
        int verdict = after.indexOf("códigos de falha encontrados");
        int connection = after.indexOf("Conexão e nova leitura");
        assertTrue(verdict >= 0);
        assertTrue(connection > verdict);
        assertTrue(after.indexOf("Iniciar leitura guiada") > connection);
        View controls = root.findViewWithTag("disclosure-Conexão e nova leitura");
        assertNotNull(controls);
        android.view.ViewGroup parent = (android.view.ViewGroup) controls.getParent();
        View content = parent.getChildAt(parent.indexOfChild(controls) + 1);
        assertTrue(content.getVisibility() == View.GONE);
        controls.performClick();
        assertTrue(content.getVisibility() == View.VISIBLE);
        capture(activity, "14-diagnostico-resultado-primeiro");
        controller.pause().stop().destroy();
    }

    @Test
    public void readingWithCodesOffersToUnderstandItAndPrepareAWorkshopSummary() throws Exception {
        ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup();
        MainActivity activity = controller.get();
        activity.startDemo();
        activity.navigate(NavigationState.DIAGNOSIS);
        activity.scan();
        waitForWorkers();
        View root = activity.getWindow().getDecorView();
        View understand = root.findViewWithTag("action-understand");
        View workshop = root.findViewWithTag("action-workshop");
        for (View action : new View[]{understand, workshop}) {
            assertNotNull(action);
            android.graphics.Rect visible = new android.graphics.Rect();
            assertTrue(action.getGlobalVisibleRect(visible));
            assertTrue(visible.height() == action.getHeight());
        }
        capture(activity, "16-diagnostico-proximos-passos");

        understand.performClick();
        idle();
        android.app.AlertDialog explanation = org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();
        assertTrue(explanation.isShowing());
        View dialogRoot = explanation.getWindow().getDecorView();
        assertNotNull(dialogRoot.findViewWithTag("explanation-P0133"));
        assertNotNull(dialogRoot.findViewWithTag("explanation-P0301"));
        String explained = texts(dialogRoot);
        assertTrue(explained.contains("Falha de combustão indicada no cilindro 1."));
        assertTrue(explained.contains("Pendente (serviço 07)"));
        assertTrue(explained.contains("PRÓXIMOS PASSOS"));
        explanation.getButton(android.app.AlertDialog.BUTTON_NEUTRAL).performClick();
        idle();

        android.app.AlertDialog summary = org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();
        assertTrue(summary != explanation && summary.isShowing());
        String message = shadowOf(summary).getMessage().toString();
        assertTrue(message.startsWith("Resumo para oficina · BorbaCar Labs"));
        assertTrue(message.contains("P0133 · Armazenado"));
        assertTrue(message.contains("P0301 · Pendente"));
        assertTrue(message.contains("sem internet"));
        assertTrue("Salvar PDF".contentEquals(summary.getButton(android.app.AlertDialog.BUTTON_NEUTRAL).getText()));
        summary.getButton(android.app.AlertDialog.BUTTON_POSITIVE).performClick();
        idle();
        android.content.Intent chooser = shadowOf(activity).getNextStartedActivity();
        assertNotNull(chooser);
        android.content.Intent send = chooser.getParcelableExtra(android.content.Intent.EXTRA_INTENT);
        assertTrue(android.content.Intent.ACTION_SEND.equals(send.getAction()));
        assertTrue(send.getStringExtra(android.content.Intent.EXTRA_TEXT).equals(message));
        controller.pause().stop().destroy();
    }

    @Test
    public void deniedBluetoothPermissionExplainsInsteadOfListingDevices() {
        ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup();
        MainActivity activity = controller.get();
        String[] permissions = {"android.permission.BLUETOOTH_CONNECT", "android.permission.BLUETOOTH_SCAN"};
        int[] denied = {android.content.pm.PackageManager.PERMISSION_DENIED, android.content.pm.PackageManager.PERMISSION_DENIED};
        activity.onRequestPermissionsResult(100, permissions, denied);
        idle();
        android.app.AlertDialog dialog = org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();
        assertNotNull(dialog);
        assertTrue(dialog.isShowing());
        assertTrue(shadowOf(dialog).getMessage().toString().contains("Permissão necessária para acessar o adaptador."));
        int[] dismissed = {};
        activity.onRequestPermissionsResult(101, permissions, dismissed);
        idle();
        assertTrue(org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog() == dialog);
        controller.pause().stop().destroy();
    }

    @Test
    public void everyMainScreenRenders() throws IOException {
        ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup();
        MainActivity activity = controller.get();
        assertNotNull(activity);
        capture(activity, "01-hoje");
        activity.navigate(NavigationState.DIAGNOSIS);
        capture(activity, "02-diagnostico");
        activity.navigate(NavigationState.HISTORY);
        capture(activity, "03-historico");
        activity.navigate(NavigationState.GARAGE);
        capture(activity, "04-garagem");
        activity.navigate(NavigationState.SETTINGS);
        capture(activity, "05-ajustes");
        activity.navigate(NavigationState.JOURNAL);
        capture(activity, "06-diario");
        activity.startDemo();
        activity.navigate(NavigationState.LIVE);
        capture(activity, "07-ao-vivo-demo");
        controller.pause().stop().destroy();
    }

    @Test
    @Config(qualifiers = "pt-rBR-w360dp-h640dp-xhdpi")
    public void todayShowsTheThreeSummariesOnASmallScreen() throws Exception {
        seedVehicleAndJournal();
        ActivityController<MainActivity> reader = Robolectric.buildActivity(MainActivity.class).setup();
        reader.get().startDemo();
        reader.get().navigate(NavigationState.DIAGNOSIS);
        reader.get().scan();
        waitForWorkers();
        reader.pause().stop().destroy();

        ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup();
        MainActivity activity = controller.get();
        waitForWorkers();
        capture(activity, "14-hoje-360x640");
        View root = activity.getWindow().getDecorView();
        String text = texts(root);
        assertTrue(text.contains("Conectar adaptador"));
        assertTrue(text.contains("código"));
        for (String tag : new String[]{io.rea.obd.ui.HomeScreen.SUMMARY_READING, io.rea.obd.ui.HomeScreen.SUMMARY_SERVICE, io.rea.obd.ui.HomeScreen.SUMMARY_FUEL}) {
            View summary = root.findViewWithTag(tag);
            assertNotNull(tag, summary);
            android.graphics.Rect visible = new android.graphics.Rect();
            assertTrue(tag + " fora da tela", summary.getGlobalVisibleRect(visible));
            assertTrue(tag + " cortado", visible.height() == summary.getHeight() && visible.width() == summary.getWidth());
        }
        controller.pause().stop().destroy();
    }

    @Test
    @Config(qualifiers = "pt-rBR-w360dp-h640dp-xhdpi")
    public void todaySummariesWrapWithLargeFonts() throws Exception {
        seedVehicleAndJournal();
        org.robolectric.RuntimeEnvironment.setFontScale(1.5f);
        ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup();
        MainActivity activity = controller.get();
        waitForWorkers();
        capture(activity, "15-hoje-fonte-grande");
        View root = activity.getWindow().getDecorView();
        for (String tag : new String[]{io.rea.obd.ui.HomeScreen.SUMMARY_READING, io.rea.obd.ui.HomeScreen.SUMMARY_SERVICE, io.rea.obd.ui.HomeScreen.SUMMARY_FUEL}) {
            View summary = root.findViewWithTag(tag);
            assertNotNull(tag, summary);
            assertFits(tag, summary);
        }
        controller.pause().stop().destroy();
        org.robolectric.RuntimeEnvironment.setFontScale(1.0f);
    }

    private static void assertFits(String tag, View view) {
        if (view instanceof android.widget.TextView) {
            android.widget.TextView text = (android.widget.TextView) view;
            assertNotNull(tag, text.getLayout());
            for (int line = 0; line < text.getLineCount(); line++) {
                assertTrue(tag + " corta \"" + text.getText() + "\"", text.getLayout().getLineWidth(line) <= text.getWidth() - text.getTotalPaddingLeft() - text.getTotalPaddingRight() + 1);
            }
            assertTrue(tag + " sem altura para \"" + text.getText() + "\"", text.getHeight() >= text.getLayout().getHeight());
        }
        if (view instanceof android.view.ViewGroup) {
            android.view.ViewGroup group = (android.view.ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                assertFits(tag, group.getChildAt(i));
            }
        }
    }

    private static void seedVehicleAndJournal() throws Exception {
        android.content.Context context = org.robolectric.RuntimeEnvironment.getApplication();
        long now = System.currentTimeMillis();
        long day = 24L * 60 * 60 * 1000;
        io.rea.obd.vehicle.VehicleProfile cobalt = new io.rea.obd.vehicle.VehicleProfile("car-1", "Meu Cobalt",
                "Chevrolet", "Cobalt", 2019, "BR", "LTZ", "1.8 L · 8V", "Flex", "Automático · 6 marchas", "");
        ActivityController<MainActivity> seed = Robolectric.buildActivity(MainActivity.class).setup();
        io.rea.obd.vehicle.VehicleRepository vehicles = new io.rea.obd.vehicle.VehicleRepository(seed.get().getPreferences(0));
        vehicles.save(cobalt);
        vehicles.select("car-1");
        io.rea.obd.journal.JournalStore journal = new io.rea.obd.journal.JournalStore(context);
        java.math.BigDecimal km = new java.math.BigDecimal("48000");
        journal.save(new io.rea.obd.journal.JournalEntry("f1", "car-1", "Meu Cobalt", io.rea.obd.journal.JournalEntry.Kind.FUEL, 1,
                now - 20 * day, now - 20 * day, now - 20 * day, "", km, new java.math.BigDecimal("40"),
                new java.math.BigDecimal("232.00"), "Gasolina", true, false, null, null));
        journal.save(new io.rea.obd.journal.JournalEntry("f2", "car-1", "Meu Cobalt", io.rea.obd.journal.JournalEntry.Kind.FUEL, 1,
                now - 2 * day, now - 2 * day, now - 2 * day, "", km.add(new java.math.BigDecimal("480")), new java.math.BigDecimal("38.5"),
                new java.math.BigDecimal("224.50"), "Gasolina", true, true, null, null));
        journal.save(new io.rea.obd.journal.JournalEntry("s1", "car-1", "Meu Cobalt", io.rea.obd.journal.JournalEntry.Kind.SERVICE, 1,
                now - 30 * day, now - 30 * day, now - 30 * day, "Troca de óleo e filtro", km.subtract(new java.math.BigDecimal("1200")), null,
                new java.math.BigDecimal("289.90"), null, false, false,
                java.time.LocalDate.now().plusDays(45).toString(), new java.math.BigDecimal("56800")));
        journal.close();
        seed.pause().stop().destroy();
    }

    @Test
    public void populatedScreensRender() throws Exception {
        seedVehicleAndJournal();
        ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup();
        MainActivity activity = controller.get();
        waitForWorkers();
        capture(activity, "10-hoje-com-carro");
        activity.startDemo();
        activity.navigate(NavigationState.DIAGNOSIS);
        activity.scan();
        waitForWorkers();
        capture(activity, "11-diagnostico-veredito");
        activity.navigate(NavigationState.JOURNAL);
        waitForWorkers();
        capture(activity, "12-diario");
        activity.navigate(NavigationState.HOME);
        waitForWorkers();
        capture(activity, "13-hoje-apos-leitura");
        controller.pause().stop().destroy();
    }

    private static void waitForWorkers() throws InterruptedException {
        for (int i = 0; i < 20; i++) {
            Thread.sleep(50);
            idle();
        }
    }
}
