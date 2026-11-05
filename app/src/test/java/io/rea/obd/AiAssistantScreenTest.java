package io.rea.obd;

import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import io.rea.obd.ai.AiAssistant;
import io.rea.obd.ai.AiConversation;
import io.rea.obd.ai.AnalysisRequest;
import io.rea.obd.diagnostics.DiagnosticCase;
import io.rea.obd.diagnostics.DiagnosticCaseBuilder;
import io.rea.obd.diagnostics.DtcDemoScenario;
import io.rea.obd.ui.I18n;
import io.rea.obd.vehicle.VehicleProfile;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Collections;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import org.robolectric.shadows.ShadowAlertDialog;

@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = 35, qualifiers = "pt-rBR-w393dp-h852dp-xhdpi")
public class AiAssistantScreenTest {
    private static DiagnosticCase kase() {
        long now = 1793000000000L;
        VehicleProfile car = new VehicleProfile("case-car", "Meu Cobalt", "Chevrolet", "Cobalt", 2019, "BR", "", "", "Flex", "", "");
        return DiagnosticCaseBuilder.create(car, "Simulador", DtcDemoScenario.create("case-car", "session", now),
                Collections.emptyList(), new DiagnosticCase.Symptoms("Falha em marcha lenta", "", "", ""), now);
    }

    private static View text(View view, String expected) {
        if (view instanceof TextView && expected.contentEquals(((TextView) view).getText())) return view;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) { View found = text(group.getChildAt(i), expected); if (found != null) return found; }
        }
        return null;
    }

    @Test public void sharesOnlyReviewedFilteredDataWithoutRequiringAServer() throws Exception {
        ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup();
        MainActivity activity = controller.get();
        AiAssistant assistant = new AiAssistant(activity, kase()); assistant.show();
        AlertDialog screen = ShadowAlertDialog.getLatestAlertDialog();
        EditText question = screen.getWindow().getDecorView().findViewWithTag("ai-question");
        question.setText("Explique a falha. Contato pessoa@example.org placa ABC1D23");
        text(screen.getWindow().getDecorView(), "Usar minha IA · compartilhar pergunta").performClick();
        AlertDialog preview = ShadowAlertDialog.getLatestAlertDialog();
        assertNotSame(screen, preview);
        preview.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        shadowOf(Looper.getMainLooper()).idle();
        Intent chooser = shadowOf(activity).getNextStartedActivity();
        assertEquals(Intent.ACTION_CHOOSER, chooser.getAction());
        Intent share = chooser.getParcelableExtra(Intent.EXTRA_INTENT);
        String prompt = share.getStringExtra(Intent.EXTRA_TEXT);
        assertFalse(prompt.contains("pessoa@example.org")); assertFalse(prompt.contains("ABC1D23"));
        assertFalse(prompt.contains("Meu Cobalt")); assertTrue(prompt.contains("P0133"));
        assertTrue(prompt.contains("demonstration_data"));
        assertTrue(prompt.contains("não invente leituras"));
        assistant.close(); controller.pause().stop().destroy();
    }

    @Test public void followUpsSendOnlyTheCurrentCasesBoundedConversation() throws Exception {
        MainActivity activity = Robolectric.buildActivity(MainActivity.class).setup().get();
        DiagnosticCase kase = kase();
        AnalysisRequest source = AnalysisRequest.from(kase);
        AiConversation conversation = new AiConversation(activity, kase.id, kase.revision, source.refs());
        JSONObject request = conversation.request(source, "O que falta? contato pessoa@example.org");
        assertEquals(0, request.getJSONArray("conversation").length());
        assertFalse(request.toString().contains("pessoa@example.org"));
        assertEquals(source.body.getJSONArray("observations").toString(), request.getJSONArray("observations").toString());
        assertEquals(0, new AiConversation(activity, "another-case", 1, source.refs()).turns().length());
        activity.finish();
    }

    @Test public void opensAiFromTheReadingAndReusesTheSavedCapture() throws Exception {
        ScreensTest.seedVehicleAndJournal();
        ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup();
        MainActivity activity = controller.get();
        activity.startDemo();
        activity.navigate(io.rea.obd.ui.NavigationState.DIAGNOSIS);
        activity.scan();
        settle();
        for (int attempt = 0; attempt < 2; attempt++) {
            View action = text(activity.getWindow().getDecorView(), "Conversar com a IA");
            assertNotNull(action); action.performClick(); settle();
            AlertDialog assistant = ShadowAlertDialog.getLatestAlertDialog();
            assertNotNull(assistant.getWindow().getDecorView().findViewWithTag("ai-question"));
            assistant.dismiss(); settle();
        }
        try (io.rea.obd.history.DiagnosticCaseStore store = new io.rea.obd.history.DiagnosticCaseStore(activity)) {
            assertEquals(1, store.latestAll().size());
        }
        controller.pause().stop().destroy();
    }

    private static void settle() throws InterruptedException {
        for (int i = 0; i < 30; i++) { Thread.sleep(50); shadowOf(Looper.getMainLooper()).idle(); }
    }

    @Test public void assistantRendersInPortuguese() throws Exception { capture("pt"); }

    @Test @Config(qualifiers = "en-rUS-w393dp-h852dp-xhdpi")
    public void assistantRendersInEnglish() throws Exception { capture("en"); }

    private void capture(String language) throws Exception {
        I18n.save(org.robolectric.RuntimeEnvironment.getApplication(), language);
        ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup();
        MainActivity activity = controller.get();
        AiAssistant assistant = new AiAssistant(activity, kase()); assistant.show();
        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(500));
        View root = dialog.getWindow().getDecorView();
        assertNotNull(root.findViewWithTag("ai-question"));
        assertNotNull(text(root, language.equals("en") ? "Send to AI" : "Enviar para a IA"));
        File dir = new File("build/screenshots/assistant"); dir.mkdirs();
        Bitmap bitmap = Bitmap.createBitmap(root.getWidth(), root.getHeight(), Bitmap.Config.ARGB_8888);
        root.draw(new Canvas(bitmap));
        try (FileOutputStream out = new FileOutputStream(new File(dir, language + ".png"))) { bitmap.compress(Bitmap.CompressFormat.PNG, 100, out); }
        assistant.close(); controller.pause().stop().destroy();
    }
}
