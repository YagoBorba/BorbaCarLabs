package io.rea.obd.vehicle.catalog;

import io.rea.obd.model.FactProvenance;
import io.rea.obd.vehicle.VehicleAttribute;
import io.rea.obd.vehicle.VehicleRecordJson;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public final class VehicleCatalogJson {
    public static final int MAX_BYTES = 2097152;

    public static final class Parsed {
        private final byte[] bytes;
        public final VehicleCatalog catalog;

        Parsed(VehicleCatalog vehicleCatalog, byte[] bytes) {
            this.catalog = vehicleCatalog;
            this.bytes = Arrays.copyOf(bytes, bytes.length);
        }

        public byte[] bytes() {
            byte[] bytes = this.bytes;
            return Arrays.copyOf(bytes, bytes.length);
        }
    }

    private VehicleCatalogJson() {
    }

    public static Parsed read(InputStream inputStream) throws Exception {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] bytes = new byte[4096];
        while (true) {
            int i = inputStream.read(bytes);
            if (i == -1) {
                byte[] byteArray = byteArrayOutputStream.toByteArray();
                JSONObject json = new JSONObject(new String(byteArray, StandardCharsets.UTF_8));
                keys(json, "schema_version", "catalog_version", "sequence", "coverage_note", "markets", "sources", "configurations");
                if (VehicleRecordJson.integer(json, "schema_version") != 1) {
                    throw new JSONException("Versão de catálogo não reconhecida");
                }
                String required = required(json, "catalog_version");
                if (!required.matches("[A-Za-z0-9._-]{1,80}")) {
                    throw new JSONException("Identificador de catálogo inválido");
                }
                long value = VehicleRecordJson.integer(json, "sequence");
                if (value < 1 || value > 2147483647L) {
                    throw new JSONException("Sequência inválida");
                }
                int value2 = (int) value;
                String text = "markets";
                Set<String> strings = strings(json.getJSONArray("markets"));
                if (strings.isEmpty()) {
                    throw new JSONException("Escopo de mercado ausente");
                }
                LinkedHashMap map = new LinkedHashMap();
                JSONArray sources = json.getJSONArray("sources");
                int value3 = 0;
                while (true) {
                    String text2 = "section";
                    String text3 = "APPROVED_EMBEDDED_FACTS";
                    String text4 = "id";
                    if (value3 < sources.length()) {
                        JSONObject jsonObject = sources.getJSONObject(value3);
                        keys(jsonObject, "id", "title", "publisher", "document_version", "url", "section", "accessed_on", "permission", "permission_reference", "reviewed_by", "reviewed_on", "markets", "fields");
                        String required2 = required(jsonObject, "id");
                        String required3 = required(jsonObject, "url");
                        String required4 = required(jsonObject, "permission");
                        if (!required3.startsWith("https://")) {
                            throw new JSONException("Fonte precisa de localização HTTPS");
                        }
                        LocalDate.parse(required(jsonObject, "accessed_on"));
                        Set<String> strings2 = strings(jsonObject.getJSONArray(text));
                        if (strings2.isEmpty()) {
                            throw new JSONException("Fonte sem mercado");
                        }
                        String text5 = text;
                        EnumSet noneOf = EnumSet.noneOf(VehicleAttribute.Key.class);
                        JSONArray array = sources;
                        for (Iterator<String> it = strings(jsonObject.getJSONArray("fields")).iterator(); it.hasNext(); it = it) {
                            noneOf.add(VehicleAttribute.Key.valueOf(it.next()));
                        }
                        if (noneOf.isEmpty()) {
                            throw new JSONException("Fonte sem escopo de atributos");
                        }
                        byte[] byteArray2 = byteArray;
                        if (!Arrays.asList("PENDING", "METADATA_ONLY", "APPROVED_EMBEDDED_FACTS").contains(required4)) {
                            throw new JSONException("Permissão desconhecida");
                        }
                        String permissionReference = VehicleRecordJson.text(jsonObject, "permission_reference");
                        String reviewedBy = VehicleRecordJson.text(jsonObject, "reviewed_by");
                        String reviewedOn = VehicleRecordJson.text(jsonObject, "reviewed_on");
                        if (required4.equals("APPROVED_EMBEDDED_FACTS")) {
                            if (permissionReference.trim().isEmpty() || reviewedBy.trim().isEmpty()) {
                                throw new JSONException("Direito de uso/revisão pendente");
                            }
                            LocalDate.parse(reviewedOn);
                        }
                        if (map.put(required2, new VehicleCatalog.Source(required2, required(jsonObject, "title"), required(jsonObject, "publisher"), required(jsonObject, "document_version"), required3, required(jsonObject, "section"), jsonObject.getString("accessed_on"), required4, permissionReference, reviewedBy, reviewedOn, strings2, noneOf)) != null) {
                            throw new JSONException("Fonte duplicada");
                        }
                        value3++;
                        text = text5;
                        sources = array;
                        byteArray = byteArray2;
                    } else {
                        byte[] bytes2 = byteArray;
                        LinkedHashMap map2 = new LinkedHashMap();
                        JSONArray configurations = json.getJSONArray("configurations");
                        int value4 = 0;
                        while (value4 < configurations.length()) {
                            JSONObject jsonObject = configurations.getJSONObject(value4);
                            keys(jsonObject, text4, "market", "attributes");
                            String required2 = required(jsonObject, text4);
                            String required3 = required(jsonObject, "market");
                            if (!strings.contains(required3)) {
                                throw new JSONException("Configuração fora do mercado publicado");
                            }
                            JSONArray array = configurations;
                            EnumMap enumMap = new EnumMap(VehicleAttribute.Key.class);
                            JSONObject attributes = jsonObject.getJSONObject("attributes");
                            Iterator<String> itKeys = attributes.keys();
                            while (itKeys.hasNext()) {
                                String next = itKeys.next();
                                Iterator<String> itKeys2 = itKeys;
                                VehicleAttribute.Key key = VehicleAttribute.Key.valueOf(next);
                                String text6 = text4;
                                if (key == VehicleAttribute.Key.NOTES) {
                                    throw new JSONException("Anotação do usuário não pertence ao catálogo");
                                }
                                JSONObject jsonObject2 = attributes.getJSONObject(next);
                                JSONObject json2 = attributes;
                                Set<String> set = strings;
                                keys(jsonObject2, "value", "source_id", text2);
                                String required4 = required(jsonObject2, "value");
                                String required5 = required(jsonObject2, "source_id");
                                String required6 = required(jsonObject2, text2);
                                VehicleCatalog.Source source = (VehicleCatalog.Source) map.get(required5);
                                if (source != null) {
                                    String text7 = text2;
                                    if (source.permission.equals(text3) && source.markets.contains(required3) && source.fields.contains(key)) {
                                        List items = Arrays.asList(required + "/" + required5 + "/" + source.documentVersion + "/" + required6, "Documento: " + source.title, "Editor: " + source.publisher, "Localização: " + source.url, "Seção: " + required6, "Consulta: " + source.accessedOn, "Permissão declarada: " + source.permission + " · " + source.permissionReference, "Revisão de origem declarada: " + source.reviewedBy + " · " + source.reviewedOn);
                                        enumMap.put(key, new VehicleAttribute(key, required4, new FactProvenance(FactProvenance.Origin.CATALOG, FactProvenance.Verification.DOCUMENTED, items, required3 + "/" + required2, "", null)));
                                        itKeys = itKeys2;
                                        text4 = text6;
                                        attributes = json2;
                                        strings = set;
                                        text2 = text7;
                                        continue;
                                    }
                                }
                                throw new JSONException("Atributo sem fonte aprovada e aplicável ao mercado/campo");
                            }
                            String text8 = text2;
                            String text9 = text3;
                            String text10 = text4;
                            Set<String> strings3 = strings;
                            VehicleAttribute.Key[] keys = {VehicleAttribute.Key.MAKE, VehicleAttribute.Key.MODEL, VehicleAttribute.Key.MARKET};
                            for (int i2 = 0; i2 < 3; i2++) {
                                if (!enumMap.containsKey(keys[i2])) {
                                    throw new JSONException("Configuração sem identidade/mercado documentado");
                                }
                            }
                            if (!((VehicleAttribute) enumMap.get(VehicleAttribute.Key.MARKET)).value.equals(required3)) {
                                throw new JSONException("Mercado do atributo divergente");
                            }
                            if (map2.put(required2, new VehicleCatalog.Configuration(required2, required3, enumMap)) != null) {
                                throw new JSONException("Configuração duplicada");
                            }
                            value4++;
                            configurations = array;
                            text4 = text10;
                            strings = strings3;
                            text2 = text8;
                            text3 = text9;
                        }
                        return new Parsed(new VehicleCatalog(required, value2, required(json, "coverage_note"), strings, map, map2), bytes2);
                    }
                }
            } else {
                if (byteArrayOutputStream.size() + i > MAX_BYTES) {
                    throw new IOException("Catálogo excede 2 MiB");
                }
                byteArrayOutputStream.write(bytes, 0, i);
            }
        }
    }

    public static void keys(JSONObject json, String... text) throws JSONException {
        HashSet seen = new HashSet();
        Iterator<String> itKeys = json.keys();
        while (itKeys.hasNext()) {
            seen.add(itKeys.next());
        }
        if (!seen.equals(new HashSet(Arrays.asList(text)))) {
            throw new JSONException("Campos ausentes ou desconhecidos");
        }
    }

    private static String required(JSONObject json, String text) throws JSONException {
        Object value = json.get(text);
        if (value instanceof String) {
            String text2 = (String) value;
            if (!text2.trim().isEmpty()) {
                return text2.trim();
            }
        }
        throw new JSONException("Campo textual obrigatório: " + text);
    }

    private static Set<String> strings(JSONArray markets) throws JSONException {
        LinkedHashSet seen = new LinkedHashSet();
        for (int i = 0; i < markets.length(); i++) {
            Object market = markets.get(i);
            if (market instanceof String) {
                String id = (String) market;
                if (!id.trim().isEmpty() && seen.add(id)) {
                    continue;
                }
            }
            throw new JSONException("Lista inválida ou duplicada");
        }
        return seen;
    }
}