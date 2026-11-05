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
import org.json.JSONArray;
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

    /** A reply shaped like the providers' structured output; refs come from the demo case. */
    private static JSONObject reply(String summary, String safety, JSONArray hypotheses, JSONArray missing) throws Exception {
        return new JSONObject().put("model", "gemini-3.5-flash-lite").put("analysis", new JSONObject()
                .put("summary", summary).put("hypotheses", hypotheses).put("missing_information", missing).put("safety_note", safety));
    }

    private static String ref(AnalysisRequest source, String code) {
        for (java.util.Map.Entry<String, String> label : source.labels.entrySet()) if (label.getValue().startsWith(code)) return label.getKey();
        throw new AssertionError(code);
    }

    /** Simulated conversation (no provider is called) for the README screenshots of the chat. */
    private void chat(String language) throws Exception {
        I18n.save(org.robolectric.RuntimeEnvironment.getApplication(), language);
        boolean en = language.equals("en");
        ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup();
        MainActivity activity = controller.get();
        DiagnosticCase kase = kase();
        AnalysisRequest source = AnalysisRequest.from(kase);
        String misfire = ref(source, "P0301"), oxygen = ref(source, "P0133");
        AiConversation conversation = new AiConversation(activity, kase.id, kase.revision, source.refs());
        conversation.append(en ? "What do these codes mean?" : "O que estes códigos significam?", reply(en
                ? "Simulated data: P0301 means cylinder 1 is misfiring, so the fuel in it is not burning properly. P0133 says the oxygen sensor is reacting slowly to changes in the air-fuel mix. Together they match the rough idle you described."
                : "Dados simulados: o P0301 indica falha de combustão no cilindro 1, ou seja, o combustível dele não está queimando direito. O P0133 diz que a sonda lambda está demorando para reagir às mudanças na mistura. Juntos, combinam com a falha em marcha lenta que você descreveu.",
                "", new JSONArray(), new JSONArray()));
        conversation.append(en ? "Is this serious? Can I keep driving?" : "Isso é grave? Posso continuar rodando?", reply(en
                ? "It is not an emergency, but it should be checked soon. Unburned fuel from the misfire can overheat and damage the catalytic converter, which is an expensive part. Short, calm trips are fine; avoid long trips and heavy loads until a workshop looks at it."
                : "Não é uma emergência, mas vale verificar logo. O combustível que não queima pode superaquecer e danificar o catalisador, que é uma peça cara. Trajetos curtos e tranquilos tudo bem; evite viagens longas e carga pesada até a oficina olhar.",
                en ? "If the check engine light starts flashing, stop as soon as it is safe." : "Se a luz da injeção começar a piscar, pare assim que for seguro.",
                new JSONArray(), new JSONArray()));
        conversation.append(en ? "What is the most likely cause?" : "Qual a causa mais provável?", reply(en
                ? "The most likely cause is in the ignition of cylinder 1: a worn spark plug, a damaged plug wire or a weak coil. That is the cheapest and most common fix for P0301. The slow oxygen sensor may be a consequence of the misfire, so it is worth checking the ignition first."
                : "A causa mais provável está na ignição do cilindro 1: vela gasta, cabo de vela danificado ou bobina fraca. É a correção mais comum e barata para o P0301. A sonda lenta pode ser consequência da falha, então vale começar pela ignição.",
                "",
                new JSONArray()
                        .put(new JSONObject().put("title", en ? "Spark plug, wire or coil of cylinder 1" : "Vela, cabo ou bobina do cilindro 1")
                                .put("reasoning", en ? "A misfire limited to one cylinder usually points to its ignition parts." : "Falha em um único cilindro costuma apontar para a ignição dele.")
                                .put("evidence", new JSONArray().put(misfire)).put("confidence", "alta")
                                .put("next_check", en ? "Swap the cylinder 1 coil with another and see if the misfire moves." : "Trocar a bobina do cilindro 1 com outra e ver se a falha muda de cilindro."))
                        .put(new JSONObject().put("title", en ? "Slow oxygen sensor" : "Sonda lambda lenta")
                                .put("reasoning", en ? "The sensor may be aging, or reacting to the misfire." : "A sonda pode estar envelhecida ou reagindo à falha de combustão.")
                                .put("evidence", new JSONArray().put(oxygen)).put("confidence", "media")
                                .put("next_check", en ? "Watch the sensor signal on a scanner after the misfire is fixed." : "Acompanhar o sinal da sonda no scanner depois de resolver a falha.")),
                new JSONArray().put(en ? "Mileage since the last spark plug change" : "Quilometragem desde a última troca de velas")));
        AiAssistant assistant = new AiAssistant(activity, kase); assistant.show();
        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(500));
        View root = dialog.getWindow().getDecorView();
        ViewGroup body = (ViewGroup) root.findViewWithTag("ai-question").getParent();
        if (!en) {
            View details = null;
            for (int i = 0; i < body.getChildCount(); i++) {
                View found = body.getChildAt(i).findViewWithTag("disclosure-Possíveis causas e o que verificar");
                if (found != null) details = found;
            }
            assertNotNull(details);
            details.performClick();
            shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(500));
        }
        body.measure(View.MeasureSpec.makeMeasureSpec(body.getWidth(), View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        body.layout(0, 0, body.getMeasuredWidth(), body.getMeasuredHeight());
        Bitmap bitmap = Bitmap.createBitmap(body.getMeasuredWidth(), body.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(new io.rea.obd.ui.UiKit(activity).palette.bg);
        body.draw(canvas);
        File dir = new File("build/screenshots/assistant"); dir.mkdirs();
        try (FileOutputStream out = new FileOutputStream(new File(dir, "chat-" + language + ".png"))) { bitmap.compress(Bitmap.CompressFormat.PNG, 100, out); }
        assertTrue(text(body, en ? "What is the most likely cause?" : "Qual a causa mais provável?") != null);
        assistant.close(); controller.pause().stop().destroy();
    }

    @Test public void chatRendersInPortuguese() throws Exception { chat("pt"); }

    @Test @Config(qualifiers = "en-rUS-w393dp-h852dp-xhdpi")
    public void chatRendersInEnglish() throws Exception { chat("en"); }

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
        assertNotNull(text(root, language.equals("en") ? "Send" : "Enviar"));
        File dir = new File("build/screenshots/assistant"); dir.mkdirs();
        Bitmap bitmap = Bitmap.createBitmap(root.getWidth(), root.getHeight(), Bitmap.Config.ARGB_8888);
        root.draw(new Canvas(bitmap));
        try (FileOutputStream out = new FileOutputStream(new File(dir, language + ".png"))) { bitmap.compress(Bitmap.CompressFormat.PNG, 100, out); }
        assistant.close(); controller.pause().stop().destroy();
    }
}
