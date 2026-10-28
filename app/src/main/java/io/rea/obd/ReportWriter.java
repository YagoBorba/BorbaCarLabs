package io.rea.obd;

import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class ReportWriter {
    private static final int TEXT = 0xFF1F2933;
    private static final int MUTED = 0xFF52606D;
    private static final int ACCENT = 0xFF006B55;
    private static final int RULE = 0xFFD9DEE5;
    private static final int BANNER = 0xFFFFF4D6;
    private static final int BANNER_TEXT = 0xFF8A5A00;

    private ReportWriter() {
    }

    public static void pdf(File file, List<String> list) throws IOException {
        Map<ReportLayout.Style, Paint> paints = new EnumMap<>(ReportLayout.Style.class);
        paints.put(ReportLayout.Style.TITLE, paint(ReportLayout.Style.TITLE, TEXT, true));
        paints.put(ReportLayout.Style.HEADING, paint(ReportLayout.Style.HEADING, ACCENT, true));
        paints.put(ReportLayout.Style.BODY, paint(ReportLayout.Style.BODY, TEXT, false));
        paints.put(ReportLayout.Style.DETAIL, paint(ReportLayout.Style.DETAIL, MUTED, false));
        ReportLayout layout = ReportLayout.of(list, (text, style) -> paints.get(style).measureText(text));

        Paint small = new Paint(Paint.ANTI_ALIAS_FLAG);
        small.setTextSize(8.5f);
        small.setColor(MUTED);
        Paint rule = new Paint();
        rule.setColor(RULE);
        rule.setStrokeWidth(0.8f);
        String generated = "gerado em " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));

        PdfDocument pdfDocument = new PdfDocument();
        PdfDocument.Page page = null;
        try {
            int total = layout.pages.size();
            for (int i = 0; i < total; i++) {
                page = pdfDocument.startPage(new PdfDocument.PageInfo.Builder((int) ReportLayout.WIDTH, (int) ReportLayout.HEIGHT, i + 1).create());
                android.graphics.Canvas canvas = page.getCanvas();
                header(canvas, layout.simulated, small, rule);
                for (ReportLayout.Line line : layout.pages.get(i)) {
                    canvas.drawText(line.text, line.x, line.y, paints.get(line.style));
                    if (line.style == ReportLayout.Style.HEADING) {
                        canvas.drawLine(ReportLayout.LEFT, line.y + 4.0f, ReportLayout.RIGHT, line.y + 4.0f, rule);
                    }
                }
                canvas.drawLine(ReportLayout.LEFT, 812.0f, ReportLayout.RIGHT, 812.0f, rule);
                canvas.drawText("BorbaCar Labs · relatório local · " + generated, ReportLayout.LEFT, 826.0f, small);
                String number = "página " + (i + 1) + " de " + total;
                canvas.drawText(number, ReportLayout.RIGHT - small.measureText(number), 826.0f, small);
                pdfDocument.finishPage(page);
                page = null;
            }
            try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
                pdfDocument.writeTo(fileOutputStream);
            }
        } finally {
            if (page != null) {
                pdfDocument.finishPage(page);
            }
            pdfDocument.close();
        }
    }

    private static void header(android.graphics.Canvas canvas, boolean simulated, Paint small, Paint rule) {
        if (simulated) {
            Paint band = new Paint();
            band.setColor(BANNER);
            canvas.drawRect(ReportLayout.LEFT, 20.0f, ReportLayout.RIGHT, 42.0f, band);
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setTextSize(9.5f);
            paint.setColor(BANNER_TEXT);
            paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            canvas.drawText("DADOS SIMULADOS · nenhum veículo real foi consultado", ReportLayout.LEFT + 10.0f, 35.0f, paint);
        } else {
            canvas.drawText("BorbaCar Labs · relatório local", ReportLayout.LEFT, 34.0f, small);
            canvas.drawLine(ReportLayout.LEFT, 42.0f, ReportLayout.RIGHT, 42.0f, rule);
        }
    }

    private static Paint paint(ReportLayout.Style style, int color, boolean bold) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTextSize(style.size);
        paint.setColor(color);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, bold ? Typeface.BOLD : Typeface.NORMAL));
        return paint;
    }
}
