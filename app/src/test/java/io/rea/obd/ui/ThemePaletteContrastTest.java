package io.rea.obd.ui;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ThemePaletteContrastTest {
    private static final double MINIMUM = 4.5d;

    static double luminance(int color) {
        double[] channels = {(color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF};
        for (int i = 0; i < channels.length; i++) {
            double value = channels[i] / 255.0d;
            channels[i] = value <= 0.03928d ? value / 12.92d : Math.pow((value + 0.055d) / 1.055d, 2.4d);
        }
        return 0.2126d * channels[0] + 0.7152d * channels[1] + 0.0722d * channels[2];
    }

    static double contrast(int first, int second) {
        double a = luminance(first);
        double b = luminance(second);
        return (Math.max(a, b) + 0.05d) / (Math.min(a, b) + 0.05d);
    }

    private static void check(String theme, String pair, int foreground, int background) {
        double ratio = contrast(foreground, background);
        assertTrue(theme + " " + pair + " " + String.format(java.util.Locale.ROOT, "%.2f", ratio), ratio >= MINIMUM);
    }

    @Test
    public void textColorsReachFourPointFiveToOneInBothThemes() {
        for (boolean light : new boolean[]{true, false}) {
            ThemePalette palette = new ThemePalette(light);
            String theme = light ? "claro" : "escuro";
            int[] surfaces = {palette.bg, palette.card, palette.line};
            String[] surfaceNames = {"fundo", "cartão", "botão"};
            int[] texts = {palette.text, palette.muted, palette.accent, palette.attention};
            String[] textNames = {"texto", "secundário", "destaque", "atenção"};
            for (int t = 0; t < texts.length; t++) {
                for (int s = 0; s < surfaces.length; s++) {
                    check(theme, textNames[t] + " sobre " + surfaceNames[s], texts[t], surfaces[s]);
                }
            }
            check(theme, "botão principal", palette.onAccent, palette.accent);
        }
    }

    @Test
    public void badgeTintKeepsTheSymbolReadable() {
        for (boolean light : new boolean[]{true, false}) {
            ThemePalette palette = new ThemePalette(light);
            for (int tone : new int[]{palette.accent, palette.attention}) {
                int tint = blend(tone, 0x26, palette.card);
                check(light ? "claro" : "escuro", "selo", tone, tint);
            }
        }
    }

    private static int blend(int color, int alpha, int background) {
        int result = 0;
        for (int shift = 16; shift >= 0; shift -= 8) {
            int top = (color >> shift) & 0xFF;
            int bottom = (background >> shift) & 0xFF;
            result |= Math.round((top * alpha + bottom * (255 - alpha)) / 255.0f) << shift;
        }
        return result;
    }
}
