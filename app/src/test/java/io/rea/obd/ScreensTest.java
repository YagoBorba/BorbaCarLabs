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
    public void populatedScreensRender() throws Exception {
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
