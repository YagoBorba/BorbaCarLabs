package io.rea.obd.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import io.rea.obd.R;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public final class I18n {
    public static final String AUTO = "auto";
    public static final String PORTUGUESE = "pt";
    public static final String ENGLISH = "en";
    private static final String PREFS = "borbacar_language";
    private static final String KEY = "language";
    private static final String SEPARATOR = " · ";

    private static final class Rule {
        final Pattern pattern;
        final String replacement;

        Rule(Pattern pattern, String replacement) {
            this.pattern = pattern;
            this.replacement = replacement;
        }
    }

    private static volatile boolean english;
    private static Map<String, String> exact = new HashMap<>();
    private static List<Rule> rules = new ArrayList<>();
    private static final Map<String, String> memo = new ConcurrentHashMap<>();

    private I18n() {
    }

    public static void init(Context context) {
        String choice = preference(context);
        boolean useEnglish = ENGLISH.equals(choice)
                || (AUTO.equals(choice) && !"pt".equals(context.getResources().getConfiguration().getLocales().get(0).getLanguage()));
        if (useEnglish && exact.isEmpty()) {
            try (InputStream input = context.getResources().openRawResource(R.raw.translations_en)) {
                load(read(input));
            } catch (IOException | JSONException e) {
                useEnglish = false;
            }
        }
        english = useEnglish;
        memo.clear();
    }

    public static String preference(Context context) {
        return prefs(context).getString(KEY, AUTO);
    }

    public static boolean save(Context context, String choice) {
        return prefs(context).edit().putString(KEY, choice).commit();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static boolean english() {
        return english;
    }

    public static Locale locale() {
        return english ? Locale.US : new Locale("pt", "BR");
    }

    static void load(String json) throws JSONException {
        JSONObject root = new JSONObject(json);
        Map<String, String> table = new HashMap<>();
        JSONObject entries = root.getJSONObject("exact");
        Iterator<String> keys = entries.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            table.put(key, entries.getString(key));
        }
        List<Rule> list = new ArrayList<>();
        JSONArray patterns = root.getJSONArray("patterns");
        for (int i = 0; i < patterns.length(); i++) {
            JSONArray rule = patterns.getJSONArray(i);
            list.add(new Rule(Pattern.compile(rule.getString(0), Pattern.DOTALL), rule.getString(1)));
        }
        exact = table;
        rules = list;
    }

    static void use(String json) throws JSONException {
        load(json);
        english = true;
        memo.clear();
    }

    static void reset() {
        english = false;
        memo.clear();
    }

    public static String t(CharSequence text) {
        if (text == null) {
            return null;
        }
        String source = text.toString();
        if (!english || source.trim().isEmpty()) {
            return source;
        }
        String cached = memo.get(source);
        if (cached != null) {
            return cached;
        }
        String result = translate(source);
        if (memo.size() < 4000) {
            memo.put(source, result);
        }
        return result;
    }

    public static String[] t(CharSequence[] items) {
        String[] result = new String[items.length];
        for (int i = 0; i < items.length; i++) {
            result[i] = t(items[i]);
        }
        return result;
    }

    private static String translate(String source) {
        String direct = single(source);
        if (direct != null) {
            return direct;
        }
        if (source.contains("\n")) {
            String[] lines = source.split("\n", -1);
            for (int i = 0; i < lines.length; i++) {
                lines[i] = translate(lines[i]);
            }
            return String.join("\n", lines);
        }
        if (source.contains(SEPARATOR)) {
            String[] parts = source.split(Pattern.quote(SEPARATOR), -1);
            for (int i = 0; i < parts.length; i++) {
                parts[i] = translate(parts[i]);
            }
            return String.join(SEPARATOR, parts);
        }
        String trimmed = source.trim();
        if (!trimmed.equals(source) && !trimmed.isEmpty()) {
            int start = source.indexOf(trimmed);
            return source.substring(0, start) + translate(trimmed) + source.substring(start + trimmed.length());
        }
        return source;
    }

    private static String single(String source) {
        String found = exact.get(source);
        if (found != null) {
            return found;
        }
        for (Rule rule : rules) {
            Matcher matcher = rule.pattern.matcher(source);
            if (matcher.matches()) {
                return expand(rule.replacement, matcher);
            }
        }
        return null;
    }

    private static String expand(String replacement, Matcher matcher) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < replacement.length(); i++) {
            char c = replacement.charAt(i);
            if ((c == '$' || c == '@') && i + 1 < replacement.length() && Character.isDigit(replacement.charAt(i + 1))) {
                String group = matcher.group(replacement.charAt(i + 1) - '0');
                group = group == null ? "" : group;
                builder.append(c == '@' ? translate(group) : group);
                i++;
            } else {
                builder.append(c);
            }
        }
        return builder.toString();
    }

    public static void apply(View view) {
        if (!english || view == null) {
            return;
        }
        if (view instanceof TextView) {
            TextView text = (TextView) view;
            if (!(view instanceof EditText)) {
                CharSequence current = text.getText();
                String translated = t(current);
                if (current != null && !translated.contentEquals(current)) {
                    text.setText(translated);
                }
            }
            CharSequence hint = text.getHint();
            if (hint != null) {
                text.setHint(t(hint));
            }
        }
        CharSequence description = view.getContentDescription();
        if (description != null) {
            view.setContentDescription(t(description));
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                apply(group.getChildAt(i));
            }
        }
    }

    private static String read(InputStream input) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int read;
        while ((read = input.read(chunk)) != -1) {
            buffer.write(chunk, 0, read);
        }
        return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
    }
}
