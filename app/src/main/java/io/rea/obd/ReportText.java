package io.rea.obd;

import io.rea.obd.diagnostics.DiagnosticCase;
import io.rea.obd.diagnostics.DtcObservation;
import io.rea.obd.knowledge.DtcCatalog;
import io.rea.obd.knowledge.DtcDefinition;
import io.rea.obd.knowledge.DtcReference;
import io.rea.obd.knowledge.SourceReference;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ReportText {
    private static final Pattern INSTANT = Pattern.compile("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}(?::\\d{2}(?:\\.\\d{1,9})?)?Z");
    private static final Pattern PLURAL = Pattern.compile("(\\d+)((?:\\s+\\p{L}+\\((?:s|ões)\\))+)");
    private static final Pattern WORD = Pattern.compile("(\\p{L}+)\\((s|ões)\\)");
    private static final DateTimeFormatter LOCAL = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private ReportText() {
    }

    public static List<String> readable(List<String> lines, ZoneId zone) {
        List<String> out = new ArrayList<>();
        boolean converted = false;
        for (String line : lines) {
            String local = times(line, zone);
            converted |= !local.equals(line);
            out.add(plurals(local));
        }
        if (converted && !out.isEmpty()) {
            String offset = ZonedDateTime.now(zone).getOffset().getId().replace("Z", "+00:00");
            out.add(1, "Horários no fuso do aparelho (UTC" + offset + ").");
        }
        return out;
    }

    static String times(String line, ZoneId zone) {
        Matcher matcher = INSTANT.matcher(line);
        StringBuffer out = new StringBuffer();
        while (matcher.find()) {
            String local = Instant.parse(matcher.group()).atZone(zone).format(LOCAL);
            matcher.appendReplacement(out, Matcher.quoteReplacement(local));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    public static String plurals(String summary) {
        Matcher matcher = PLURAL.matcher(summary);
        StringBuffer out = new StringBuffer();
        while (matcher.find()) {
            boolean one = matcher.group(1).equals("1");
            Matcher word = WORD.matcher(matcher.group(2));
            StringBuffer words = new StringBuffer();
            while (word.find()) {
                String base = word.group(1);
                String form = one ? base
                        : word.group(2).equals("ões") && base.endsWith("ão") ? base.substring(0, base.length() - 2) + "ões"
                        : base + word.group(2);
                word.appendReplacement(words, Matcher.quoteReplacement(form));
            }
            word.appendTail(words);
            matcher.appendReplacement(out, Matcher.quoteReplacement(matcher.group(1) + words));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    public static List<String> referenceAppendix(DiagnosticCase diagnosticCase) {
        Set<String> codes = new LinkedHashSet<>();
        for (DtcObservation observation : diagnosticCase.dtc.observations()) {
            if (DtcCatalog.find(observation.code) == null && DtcReference.find(observation.code) != null) {
                codes.add(observation.code);
            }
        }
        List<String> lines = new ArrayList<>();
        if (codes.isEmpty()) {
            return lines;
        }
        lines.add("");
        lines.add("Catálogo ampliado · " + DtcReference.VERSION);
        lines.add("Consulta feita ao exportar; não integra a análise salva acima.");
        for (String code : codes) {
            DtcDefinition definition = DtcReference.find(code);
            lines.add(definition.display());
            for (SourceReference source : DtcReference.sources(definition)) {
                lines.add("  Fonte: " + source.title + " · " + source.publisher + " · " + definition.sourceSection);
            }
        }
        return lines;
    }
}
