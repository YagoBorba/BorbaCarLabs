package io.rea.obd;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class ReportLayout {
    public static final float WIDTH = 595.0f;
    public static final float HEIGHT = 842.0f;
    public static final float LEFT = 42.0f;
    public static final float RIGHT = 553.0f;
    public static final float TOP = 66.0f;
    public static final float BOTTOM = 798.0f;
    static final float DETAIL_INDENT = 14.0f;

    public enum Style {
        TITLE(16.0f, 24.0f),
        HEADING(12.5f, 19.0f),
        BODY(10.5f, 15.0f),
        DETAIL(9.5f, 13.5f);

        public final float size;
        final float leading;

        Style(float size, float leading) {
            this.size = size;
            this.leading = leading;
        }
    }

    public interface Measure {
        float width(String rest, Style style);
    }

    public static final class Line {
        public final Style style;
        public final String text;
        public final float x;
        public final float y;

        Line(Style style, String segment, float x, float y) {
            this.style = style;
            this.text = segment;
            this.x = x;
            this.y = y;
        }
    }

    public final List<List<Line>> pages;
    public final boolean simulated;

    private ReportLayout(List<List<Line>> pages, boolean simulated) {
        this.pages = Collections.unmodifiableList(pages);
        this.simulated = simulated;
    }

    public static ReportLayout of(List<String> source, Measure measure) {
        List<String> lines = new ArrayList<>();
        boolean simulated = false;
        for (String entry : source) {
            for (String line : entry.split("\\r?\\n", -1)) {
                lines.add(line);
                String upper = line.toUpperCase(Locale.ROOT);
                simulated |= upper.contains("SIMULAÇÃO") || upper.contains("SIMULADO") || upper.contains("DADOS SIMULADOS");
            }
        }

        Planner planner = new Planner(measure);
        boolean titled = false;
        boolean afterBlank = false;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.trim().isEmpty()) {
                afterBlank = titled;
                planner.gap(7.0f);
                continue;
            }
            Style style;
            float x = LEFT;
            String text = line;
            if (!titled) {
                style = Style.TITLE;
                titled = true;
            } else if (afterBlank) {
                style = Style.HEADING;
                planner.gap(8.0f);
                planner.reserve(Style.HEADING.leading + Style.BODY.leading + 6.0f);
            } else if (line.startsWith("  ")) {
                style = Style.DETAIL;
                x = LEFT + DETAIL_INDENT;
                text = line.trim();
            } else {
                style = Style.BODY;
            }
            afterBlank = false;
            planner.add(style, text.trim(), x);
            if (style == Style.TITLE || style == Style.HEADING) {
                planner.gap(4.0f);
            }
        }
        return new ReportLayout(planner.finish(), simulated);
    }

    private static final class Planner {
        private final Measure measure;
        private final List<List<Line>> pages = new ArrayList<>();
        private List<Line> page = new ArrayList<>();
        private float y = TOP;

        Planner(Measure measure) {
            this.measure = measure;
        }

        void gap(float points) {
            if (!this.page.isEmpty()) {
                this.y += points;
            }
        }

        void reserve(float points) {
            if (!this.page.isEmpty() && this.y + points > BOTTOM) {
                newPage();
            }
        }

        void add(Style style, String text, float x) {
            for (String segment : wrap(text, style, RIGHT - x)) {
                if (this.y + style.leading > BOTTOM) {
                    newPage();
                }
                this.page.add(new Line(style, segment, x, this.y + style.size));
                this.y += style.leading;
            }
        }

        private List<String> wrap(String text, Style style, float available) {
            List<String> segments = new ArrayList<>();
            String rest = text;
            while (this.measure.width(rest, style) > available) {
                int fit = 1;
                while (fit < rest.length() && this.measure.width(rest.substring(0, fit + 1), style) <= available) {
                    fit++;
                }
                int space = rest.lastIndexOf(' ', fit);
                int cut = space > 0 ? space : fit;
                segments.add(rest.substring(0, cut).trim());
                rest = rest.substring(cut).trim();
            }
            if (!rest.isEmpty() || segments.isEmpty()) {
                segments.add(rest);
            }
            return segments;
        }

        private void newPage() {
            this.pages.add(this.page);
            this.page = new ArrayList<>();
            this.y = TOP;
        }

        List<List<Line>> finish() {
            if (!this.page.isEmpty() || this.pages.isEmpty()) {
                this.pages.add(this.page);
            }
            return this.pages;
        }
    }
}
