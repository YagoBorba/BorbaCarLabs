package io.rea.obd.journal;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;

public final class JournalJson {
    private JournalJson() {
    }

    public static JSONObject encode(JournalEntry journalEntry) throws JSONException {
        return new JSONObject().put("schema_version", 1).put("origin", "USER_INPUT").put("id", journalEntry.id).put("vehicle_id", journalEntry.vehicleId).put("vehicle_name", journalEntry.vehicleName).put("kind", journalEntry.kind.name()).put("revision", journalEntry.revision).put("occurred_at", journalEntry.occurredAt).put("created_at", journalEntry.createdAt).put("revised_at", journalEntry.revisedAt).put("note", journalEntry.note).put("odometer_km", value(journalEntry.odometer)).put("liters", value(journalEntry.liters)).put("cost_brl", value(journalEntry.cost)).put("fuel", journalEntry.fuel).put("full_tank", journalEntry.fullTank).put("complete_interval", journalEntry.completeInterval).put("voided", journalEntry.voided).put("due_date", journalEntry.dueDate).put("due_odometer_km", value(journalEntry.dueOdometer));
    }

    private static Object value(BigDecimal bigDecimal) {
        return bigDecimal == null ? JSONObject.NULL : bigDecimal.toPlainString();
    }

    private static BigDecimal number(JSONObject json, String text) throws JSONException {
        if (json.has(text)) {
            if (json.isNull(text)) {
                return null;
            }
            return new BigDecimal(json.getString(text));
        }
        throw new JSONException("Campo ausente: " + text);
    }

    public static JournalEntry decode(JSONObject json) throws JSONException {
        if (json.getInt("schema_version") == 1 && "USER_INPUT".equals(json.getString("origin"))) {
            return new JournalEntry(json.getString("id"), json.getString("vehicle_id"), json.getString("vehicle_name"), JournalEntry.Kind.valueOf(json.getString("kind")), json.getInt("revision"), json.getLong("occurred_at"), json.getLong("created_at"), json.getLong("revised_at"), json.getString("note"), number(json, "odometer_km"), number(json, "liters"), number(json, "cost_brl"), json.getString("fuel"), json.getBoolean("full_tank"), json.getBoolean("complete_interval"), json.getString("due_date"), number(json, "due_odometer_km"), json.getBoolean("voided"));
        }
        throw new JSONException("Formato de diário não reconhecido; dados preservados.");
    }

    public static List<String> lines(JournalEntry journalEntry) {
        String text;
        ArrayList items = new ArrayList();
        items.add(journalEntry.title() + " · " + journalEntry.vehicleName);
        StringBuilder builder = new StringBuilder("Informado por você · revisão ");
        builder.append(journalEntry.revision);
        items.add(builder.toString());
        if (journalEntry.voided) {
            items.add("ANULADO · excluído dos cálculos; revisões preservadas.");
        }
        items.add("Acontecimento: " + Instant.ofEpochMilli(journalEntry.occurredAt));
        items.add("Registrado: " + Instant.ofEpochMilli(journalEntry.createdAt));
        items.add("Revisado: " + Instant.ofEpochMilli(journalEntry.revisedAt));
        items.add(journalEntry.note);
        StringBuilder builder2 = new StringBuilder("Hodômetro: ");
        String text2 = "não informado";
        if (journalEntry.odometer == null) {
            text = "não informado";
        } else {
            text = journalEntry.odometer.toPlainString() + " km";
        }
        builder2.append(text);
        items.add(builder2.toString());
        StringBuilder builder3 = new StringBuilder("Valor pago: ");
        if (journalEntry.cost != null) {
            text2 = "R$ " + journalEntry.cost.toPlainString();
        }
        builder3.append(text2);
        items.add(builder3.toString());
        if (journalEntry.kind == JournalEntry.Kind.FUEL) {
            items.add("Volume: " + journalEntry.liters.toPlainString() + " L · " + journalEntry.fuel);
            items.add(journalEntry.fullTank ? "Tanque cheio informado" : "Abastecimento parcial");
            items.add(journalEntry.completeInterval ? "Intervalo declarado completo" : "Completude do intervalo não confirmada");
        }
        if (!journalEntry.dueDate.isEmpty()) {
            items.add("Próxima data informada: " + journalEntry.dueDate);
        }
        if (journalEntry.dueOdometer != null) {
            items.add("Próximo hodômetro informado: " + journalEntry.dueOdometer + " km");
        }
        if (journalEntry.kind == JournalEntry.Kind.SERVICE) {
            items.add("Serviço informado não confirma causa nem eficácia do reparo. Prazos são declarados, não recomendação do fabricante.");
        }
        return items;
    }
}