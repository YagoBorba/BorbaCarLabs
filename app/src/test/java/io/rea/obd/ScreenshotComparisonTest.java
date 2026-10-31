package io.rea.obd;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.graphics.Bitmap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = 35)
public class ScreenshotComparisonTest {
    private static Bitmap filled(int width, int height, int color) {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.eraseColor(color);
        return bitmap;
    }

    @Test
    public void identicalAndBarelyDifferentScreensMatch() {
        Bitmap reference = filled(60, 40, 0xFFF4F7FA);
        assertTrue(ScreenshotComparison.compare(reference, filled(60, 40, 0xFFF4F7FA)).matches());
        assertTrue(ScreenshotComparison.compare(reference, filled(60, 40, 0xFFF1F4F8)).matches());
    }

    @Test
    public void aSmallColorShiftOverTheWholeScreenFails() {
        ScreenshotComparison.Result result = ScreenshotComparison.compare(filled(60, 40, 0xFF006B55), filled(60, 40, 0xFF0B7A5E));
        assertFalse(result.matches());
        assertEquals(60 * 40, result.differing);
    }

    @Test
    public void aFewChangedPixelsAreToleratedButNotALetter() {
        Bitmap reference = filled(60, 40, 0xFFFFFFFF);
        Bitmap noise = reference.copy(Bitmap.Config.ARGB_8888, true);
        for (int i = 0; i < ScreenshotComparison.PIXEL_TOLERANCE; i++) {
            noise.setPixel(i, i % 40, 0xFF000000);
        }
        assertTrue(ScreenshotComparison.compare(reference, noise).matches());

        Bitmap letter = reference.copy(Bitmap.Config.ARGB_8888, true);
        for (int x = 10; x < 16; x++) {
            for (int y = 10; y < 16; y++) {
                letter.setPixel(x, y, 0xFF172638);
            }
        }
        ScreenshotComparison.Result result = ScreenshotComparison.compare(reference, letter);
        assertFalse(result.matches());
        assertEquals(36, result.differing);
        assertEquals(0xFFFF0000, result.diff.getPixel(12, 12));
    }

    @Test
    public void aDifferentSizeFails() {
        assertFalse(ScreenshotComparison.compare(filled(60, 40, 0xFFFFFFFF), filled(60, 41, 0xFFFFFFFF)).matches());
    }
}
