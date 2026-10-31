package io.rea.obd;

import static org.junit.Assert.fail;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.view.View;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

final class ScreenshotComparison {
    static final File REFERENCES = new File("src/test/screenshots");
    static final File FAILURES = new File("build/screenshots/diferencas");
    static final int CHANNEL_TOLERANCE = 6;
    static final int PIXEL_TOLERANCE = 24;

    private ScreenshotComparison() {
    }

    static final class Result {
        final int differing;
        final Bitmap diff;
        final boolean sameSize;

        Result(int differing, Bitmap diff, boolean sameSize) {
            this.differing = differing;
            this.diff = diff;
            this.sameSize = sameSize;
        }

        boolean matches() {
            return this.sameSize && this.differing <= PIXEL_TOLERANCE;
        }
    }

    static boolean updating() {
        return Boolean.getBoolean("screenshots.update");
    }

    static Bitmap capture(View root) {
        Bitmap bitmap = Bitmap.createBitmap(root.getWidth(), root.getHeight(), Bitmap.Config.ARGB_8888);
        root.draw(new Canvas(bitmap));
        return bitmap;
    }

    static Result compare(Bitmap expected, Bitmap actual) {
        if (expected.getWidth() != actual.getWidth() || expected.getHeight() != actual.getHeight()) {
            return new Result(actual.getWidth() * actual.getHeight(), actual, false);
        }
        int width = actual.getWidth();
        int height = actual.getHeight();
        int[] before = new int[width * height];
        int[] after = new int[width * height];
        expected.getPixels(before, 0, width, 0, 0, width, height);
        actual.getPixels(after, 0, width, 0, 0, width, height);
        int[] marked = new int[width * height];
        int differing = 0;
        for (int i = 0; i < after.length; i++) {
            if (distance(before[i], after[i]) > CHANNEL_TOLERANCE) {
                differing++;
                marked[i] = 0xFFFF0000;
            } else {
                int gray = (((after[i] >> 16) & 0xFF) + ((after[i] >> 8) & 0xFF) + (after[i] & 0xFF)) / 3;
                gray = 160 + gray * 95 / 255;
                marked[i] = 0xFF000000 | (gray << 16) | (gray << 8) | gray;
            }
        }
        Bitmap diff = Bitmap.createBitmap(marked, width, height, Bitmap.Config.ARGB_8888);
        return new Result(differing, diff, true);
    }

    private static int distance(int first, int second) {
        int max = 0;
        for (int shift = 0; shift <= 24; shift += 8) {
            max = Math.max(max, Math.abs(((first >> shift) & 0xFF) - ((second >> shift) & 0xFF)));
        }
        return max;
    }

    static void verify(View root, String name) throws IOException {
        Bitmap actual = capture(root);
        File reference = new File(REFERENCES, name + ".png");
        if (updating()) {
            REFERENCES.mkdirs();
            write(actual, reference);
            return;
        }
        if (!reference.isFile()) {
            FAILURES.mkdirs();
            write(actual, new File(FAILURES, name + "-atual.png"));
            fail("Sem referência para " + name + ". Revise " + new File(FAILURES, name + "-atual.png") + " e rode ./gradlew :app:testDebugUnitTest -PupdateScreenshots");
        }
        Bitmap expected = BitmapFactory.decodeFile(reference.getPath());
        Result result = compare(expected, actual);
        if (!result.matches()) {
            FAILURES.mkdirs();
            write(actual, new File(FAILURES, name + "-atual.png"));
            write(result.diff, new File(FAILURES, name + "-diferenca.png"));
            fail(name + (result.sameSize ? " mudou em " + result.differing + " pixels" : " mudou de tamanho")
                    + ". Diferença em vermelho: " + new File(FAILURES, name + "-diferenca.png")
                    + ". Se a mudança é intencional: ./gradlew :app:testDebugUnitTest -PupdateScreenshots");
        }
    }

    private static void write(Bitmap bitmap, File file) throws IOException {
        try (FileOutputStream out = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        }
    }
}
