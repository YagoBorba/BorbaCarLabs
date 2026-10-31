package io.rea.obd;

import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import io.rea.obd.ui.NavigationState;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = 35)
public class AccessibilityScreensTest {
    private static final File OUT = new File("build/screenshots/acessibilidade");
    private final Set<String> problems = new LinkedHashSet<>();

    @After
    public void restoreFont() {
        RuntimeEnvironment.setFontScale(1.0f);
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

    @Test
    @Config(qualifiers = "pt-rBR-w360dp-h640dp-xhdpi")
    public void everyScreenWorksWithTheLargestFont() throws Exception {
        RuntimeEnvironment.setFontScale(2.0f);
        walk("fonte-200");
    }

    @Test
    @Config(qualifiers = "pt-rBR-w320dp-h568dp-hdpi")
    public void everyScreenWorksOnASmallPhone() throws Exception {
        walk("tela-320");
    }

    @Test
    @Config(qualifiers = "pt-rBR-w393dp-h852dp-xhdpi")
    public void talkBackFindsScreenTitlesAndReadsTheConnectionState() throws Exception {
        ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup();
        MainActivity activity = controller.get();
        waitForWorkers();
        View root = activity.getWindow().getDecorView();
        assertTrue(headings(root).contains("Hoje"));
        assertTrue(texts(root, "Sem conexão com o adaptador"));
        activity.navigate(NavigationState.DIAGNOSIS);
        idle();
        assertTrue(headings(activity.getWindow().getDecorView()).contains("Diagnóstico"));
        activity.navigate(NavigationState.GARAGE);
        idle();
        assertTrue(headings(activity.getWindow().getDecorView()).contains("Garagem"));
        activity.navigate(NavigationState.JOURNAL);
        waitForWorkers();
        assertTrue(headings(activity.getWindow().getDecorView()).contains("Diário do carro"));
        activity.startDemo();
        activity.navigate(NavigationState.LIVE);
        waitForWorkers();
        root = activity.getWindow().getDecorView();
        assertTrue(headings(root).contains("Ao vivo"));
        assertTrue(texts(root, "Modo demonstração"));
        controller.pause().stop().destroy();
    }

    private static List<String> headings(View view) {
        List<String> found = new ArrayList<>();
        if (view instanceof TextView && view.isAccessibilityHeading()) {
            found.add(((TextView) view).getText().toString());
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                found.addAll(headings(group.getChildAt(i)));
            }
        }
        return found;
    }

    private static boolean texts(View view, String description) {
        if (view.getContentDescription() != null && description.contentEquals(view.getContentDescription())) {
            return true;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                if (texts(group.getChildAt(i), description)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void walk(String prefix) throws Exception {
        ScreensTest.seedVehicleAndJournal();
        ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup();
        MainActivity activity = controller.get();
        waitForWorkers();
        audit(activity, prefix + "-01-hoje");
        activity.navigate(NavigationState.DIAGNOSIS);
        audit(activity, prefix + "-02-diagnostico");
        activity.navigate(NavigationState.HISTORY);
        audit(activity, prefix + "-03-historico");
        activity.navigate(NavigationState.GARAGE);
        audit(activity, prefix + "-04-garagem");
        activity.navigate(NavigationState.SETTINGS);
        audit(activity, prefix + "-05-ajustes");
        activity.navigate(NavigationState.JOURNAL);
        waitForWorkers();
        audit(activity, prefix + "-06-diario");
        activity.startDemo();
        activity.navigate(NavigationState.DIAGNOSIS);
        activity.scan();
        waitForWorkers();
        audit(activity, prefix + "-07-diagnostico-leitura");
        activity.navigate(NavigationState.LIVE);
        waitForWorkers();
        audit(activity, prefix + "-08-ao-vivo");
        activity.navigate(NavigationState.HOME);
        waitForWorkers();
        audit(activity, prefix + "-09-hoje-leitura");
        controller.pause().stop().destroy();

        File report = new File("build/acessibilidade-" + prefix + ".txt");
        Files.write(report.toPath(), String.join("\n", this.problems).getBytes(StandardCharsets.UTF_8));
        assertTrue("Problemas de acessibilidade:\n" + String.join("\n", this.problems), this.problems.isEmpty());
    }

    private void audit(MainActivity activity, String name) throws IOException {
        idle();
        View root = activity.getWindow().getDecorView();
        List<View> disclosures = new ArrayList<>();
        collect(root, disclosures);
        for (View disclosure : disclosures) {
            disclosure.performClick();
        }
        idle();
        int width = root.getWidth();
        float density = activity.getResources().getDisplayMetrics().density;
        check(root, name, width, density);
        android.widget.ScrollView content = scroll(root);
        if (content == null || content.getHeight() * 2 < root.getHeight()) {
            this.problems.add(name + " | conteúdo com menos da metade da tela");
        }
        Bitmap bitmap = Bitmap.createBitmap(root.getWidth(), root.getHeight(), Bitmap.Config.ARGB_8888);
        root.draw(new Canvas(bitmap));
        OUT.mkdirs();
        try (FileOutputStream out = new FileOutputStream(new File(OUT, name + ".png"))) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        }
    }

    private static android.widget.ScrollView scroll(View view) {
        if (view instanceof android.widget.ScrollView) {
            return (android.widget.ScrollView) view;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                android.widget.ScrollView found = scroll(group.getChildAt(i));
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static void collect(View view, List<View> disclosures) {
        if (view.getTag() instanceof String && ((String) view.getTag()).startsWith("disclosure-")) {
            disclosures.add(view);
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                collect(group.getChildAt(i), disclosures);
            }
        }
    }

    private void check(View view, String name, int width, float density) {
        if (!view.isShown() || view.getWidth() == 0) {
            return;
        }
        String label = describe(view);
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        if (location[0] < -1 || location[0] + view.getWidth() > width + 1) {
            this.problems.add(name + " | fora da largura da tela | " + label);
        }
        if (view instanceof TextView && !(view instanceof EditText)) {
            TextView text = (TextView) view;
            if (text.getText().length() > 0 && text.getLayout() != null) {
                int available = text.getWidth() - text.getTotalPaddingLeft() - text.getTotalPaddingRight();
                for (int line = 0; line < text.getLineCount(); line++) {
                    if (text.getLayout().getLineMax(line) > available + 1) {
                        this.problems.add(name + " | texto cortado na largura | " + label);
                        break;
                    }
                }
                CharSequence content = text.getLayout().getText();
                for (int line = 0; line + 1 < text.getLineCount(); line++) {
                    int end = text.getLayout().getLineEnd(line);
                    if (end > 0 && end < content.length() && Character.isLetter(content.charAt(end - 1)) && Character.isLetter(content.charAt(end))) {
                        this.problems.add(name + " | palavra quebrada no meio | " + label);
                        break;
                    }
                }
                if (text.getHeight() + 1 < text.getLayout().getHeight() + text.getTotalPaddingTop() + text.getTotalPaddingBottom() && text.getMaxLines() == Integer.MAX_VALUE) {
                    this.problems.add(name + " | texto cortado na altura | " + label);
                }
            }
        }
        if (view.isClickable() && view.isEnabled() && !(view instanceof EditText)) {
            if (label.isEmpty()) {
                this.problems.add(name + " | controle sem texto para o TalkBack | " + view.getClass().getSimpleName() + " " + view.getTag());
            }
            if (view.getHeight() + 1 < 48 * density) {
                this.problems.add(name + " | área de toque menor que 48 dp | " + label);
            }
        }
        boolean drawing = !(view instanceof TextView) && !(view instanceof ViewGroup) && view.getClass() != View.class;
        if (drawing && view.getImportantForAccessibility() != View.IMPORTANT_FOR_ACCESSIBILITY_NO && label.isEmpty()) {
            this.problems.add(name + " | imagem sem descrição para o TalkBack | " + view.getClass().getSimpleName());
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                check(group.getChildAt(i), name, width, density);
            }
        }
    }

    private static String describe(View view) {
        if (view.getContentDescription() != null && view.getContentDescription().length() > 0) {
            return view.getContentDescription().toString().replace('\n', ' ');
        }
        if (view instanceof TextView && ((TextView) view).getText().length() > 0) {
            return ((TextView) view).getText().toString().replace('\n', ' ');
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                String inner = describe(group.getChildAt(i));
                if (!inner.isEmpty()) {
                    return inner;
                }
            }
        }
        return "";
    }
}
