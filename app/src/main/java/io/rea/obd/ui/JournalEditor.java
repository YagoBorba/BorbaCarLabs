package io.rea.obd.ui;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.graphics.Insets;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.DigitsKeyListener;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.TimePicker;
import io.rea.obd.journal.JournalEntry;
import io.rea.obd.journal.JournalJson;
import io.rea.obd.vehicle.VehicleProfile;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.json.JSONObject;

public final class JournalEditor {
    private Button cancelButton;
    private CheckBox complete;
    private final long createdAt;
    private Button dateButton;
    private final Dialog dialog;
    private Button dueButton;
    private String dueDate;
    private TextView error;
    private final Map<String, EditText> fields = new LinkedHashMap();
    private LinearLayout form;
    private Spinner fuel;
    private CheckBox full;
    private final Activity host;
    private final String id;
    private final JournalEntry.Kind kind;
    private long occurrence;
    private final JournalEntry original;
    private final Save save;
    private Button saveButton;
    private boolean saving;
    private Button timeButton;
    private final UiKit ui;
    private final String vehicleId;
    private final String vehicleName;

    public interface Completion {
        void complete(String text);
    }

    public interface Save {
        void save(JournalEntry journalEntry, Completion completion);
    }

    public JournalEditor(Activity activity, VehicleProfile vehicleProfile, JournalEntry.Kind kind, JournalEntry journalEntry, Bundle bundle, Save save) throws Exception {
        JournalEntry.Kind kind2;
        String string;
        String vehicleId;
        String text;
        long currentTimeMillis;
        String dueDate;
        LinearLayout column;
        LinearLayout column2;
        String text2;
        String text3;
        String serverUrl;
        String text4;
        String decimal;
        LinearLayout linearLayout;
        String decimal2;
        String decimal3;
        int indexOf;
        boolean checked2;
        boolean checked3;
        String decimal4;
        this.dueDate = "";
        this.host = activity;
        this.save = save;
        UiKit uiKit = new UiKit(activity);
        this.ui = uiKit;
        JournalEntry original = (bundle == null || bundle.getString("base", "").isEmpty()) ? journalEntry : JournalJson.decode(new JSONObject(bundle.getString("base")));
        this.original = original;
        if (bundle != null) {
            kind2 = JournalEntry.Kind.valueOf(bundle.getString("kind"));
        } else if (journalEntry != null) {
            kind2 = journalEntry.kind;
        } else {
            kind2 = kind;
        }
        this.kind = kind2;
        if (bundle != null) {
            string = bundle.getString("id");
        } else if (journalEntry != null) {
            string = journalEntry.id;
        } else {
            string = UUID.randomUUID().toString();
        }
        this.id = string;
        if (bundle != null) {
            vehicleId = bundle.getString("vehicle_id");
        } else if (journalEntry != null) {
            vehicleId = journalEntry.vehicleId;
        } else {
            vehicleId = vehicleProfile.id;
        }
        this.vehicleId = vehicleId;
        if (bundle != null) {
            text = bundle.getString("vehicle_name");
        } else if (journalEntry != null) {
            text = journalEntry.vehicleName;
        } else {
            text = vehicleProfile.name;
        }
        String vehicleName = text;
        this.vehicleName = vehicleName;
        if (bundle != null) {
            currentTimeMillis = bundle.getLong("created");
        } else if (journalEntry != null) {
            currentTimeMillis = journalEntry.createdAt;
        } else {
            currentTimeMillis = System.currentTimeMillis();
        }
        this.createdAt = currentTimeMillis;
        if (bundle != null) {
            currentTimeMillis = bundle.getLong("occurrence");
        } else if (journalEntry != null) {
            currentTimeMillis = journalEntry.occurredAt;
        }
        this.occurrence = currentTimeMillis;
        if (bundle != null) {
            dueDate = bundle.getString("due_date", "");
        } else if (journalEntry != null) {
            dueDate = journalEntry.dueDate;
        } else {
            dueDate = "";
        }
        this.dueDate = dueDate;
        Dialog dialog = new Dialog(activity, uiKit.palette.style()) {
            @Override
            public void onBackPressed() {
                if (JournalEditor.this.saving) {
                    return;
                }
                dismiss();
            }
        };
        this.dialog = dialog;
        dialog.setCanceledOnTouchOutside(false);
        column = uiKit.column();
        column.setBackgroundColor(uiKit.palette.bg);
        column.setPadding(uiKit.dp(20), uiKit.dp(16), uiKit.dp(20), uiKit.dp(16));
        ScrollView scrollView = new ScrollView(activity);
        column.addView(scrollView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));
        column2 = uiKit.column();
        this.form = column2;
        scrollView.addView(column2);
        uiKit.eyebrow(column2, "DIÁRIO · " + title(kind2).toUpperCase(Locale.ROOT));
        if (original == null) {
            text2 = "Novo registro";
        } else {
            text2 = "Corrigir registro";
        }
        uiKit.label(column2, text2, 24, UiKit.TEXT, true);
        uiKit.label(column2, vehicleName + " · informado por você", 14, UiKit.MUTED, false);
        Button button = uiKit.button(column2, "", false, () -> this.pickDate());
        this.dateButton = button;
        button.setTag("journal-date");
        this.timeButton = uiKit.button(column2, "", false, () -> this.pickTime());
        updateTimeLabels();
        if (kind2 == JournalEntry.Kind.FUEL) {
            text3 = "Observação (opcional)";
        } else {
            text3 = "O que aconteceu ou foi feito?";
        }
        String title = text3;
        if (original == null) {
            serverUrl = "";
        } else {
            serverUrl = original.note;
        }
        field(column2, "note", title, serverUrl, bundle, false, 2000).setSingleLine(false);
        if (kind2 == JournalEntry.Kind.FUEL) {
            text4 = "";
        } else {
            text4 = " · opcional";
        }
        String concat = "Hodômetro (km)".concat(text4);
        if (original == null) {
            decimal = "";
        } else {
            decimal = decimal(original.odometer);
        }
        field(column2, "odometer", concat, decimal, bundle, true, 32);
        if (kind2 != JournalEntry.Kind.SYMPTOM) {
            if (original == null) {
                decimal4 = "";
            } else {
                decimal4 = decimal(original.cost);
            }
            field(column2, "cost", "Valor total pago (R$) · opcional", decimal4, bundle, true, 32);
        }
        if (kind2 == JournalEntry.Kind.FUEL) {
            if (original == null) {
                decimal3 = "";
            } else {
                decimal3 = decimal(original.liters);
            }
            field(column2, "liters", "Litros abastecidos", decimal3, bundle, true, 32);
            TextView label = uiKit.label(column2, "Combustível", 14, UiKit.TEXT, true);
            Spinner spinner = new Spinner(activity);
            this.fuel = spinner;
            spinner.setId(View.generateViewId());
            label.setLabelFor(this.fuel.getId());
            this.fuel.setTag("journal-fuel");
            ArrayAdapter arrayAdapter = new ArrayAdapter(activity, android.R.layout.simple_spinner_item, JournalEntry.FUELS);
            arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            this.fuel.setAdapter((SpinnerAdapter) arrayAdapter);
            this.fuel.setMinimumHeight(uiKit.dp(48));
            linearLayout = column2;
            linearLayout.addView(this.fuel, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            Spinner fuel = this.fuel;
            if (bundle != null) {
                indexOf = Math.max(0, Math.min(JournalEntry.FUELS.size() - 1, bundle.getInt("fuel")));
            } else if (original == null) {
                indexOf = 0;
            } else {
                indexOf = JournalEntry.FUELS.indexOf(original.fuel);
            }
            fuel.setSelection(indexOf);
            if (bundle != null) {
                checked2 = bundle.getBoolean("full");
            } else {
                checked2 = original != null && original.fullTank;
            }
            this.full = check(linearLayout, "Completei o tanque", "journal-full", checked2);
            if (bundle != null) {
                checked3 = bundle.getBoolean("complete");
            } else {
                checked3 = original != null && original.completeInterval;
            }
            CheckBox complete = check(linearLayout, "Registrei todos os abastecimentos desde o tanque cheio anterior", "journal-complete", checked3);
            this.complete = complete;
            complete.setEnabled(this.full.isChecked());
            this.full.setOnCheckedChangeListener((button2, checked) -> {
                this.complete.setEnabled(checked);
                if (checked) {
                    return;
                }
                this.complete.setChecked(false);
            });
            uiKit.label(linearLayout, "Um registro parcial entra na soma do intervalo. Sem confirmação de completude, o app não calcula a média.", 13, UiKit.MUTED, false);
        } else {
            linearLayout = column2;
        }
        if (kind2 == JournalEntry.Kind.SERVICE) {
            uiKit.section(linearLayout, "Próximo cuidado", "Opcional. Use um prazo que você definiu ou verificou; o app não presume o plano do fabricante.");
            this.dueButton = uiKit.button(linearLayout, "", false, () -> this.pickDue());
            updateDueLabel();
            uiKit.button(linearLayout, "Não definir data", false, () -> {
                this.dueDate = "";
                updateDueLabel();
            });
            if (original == null) {
                decimal2 = "";
            } else {
                decimal2 = decimal(original.dueOdometer);
            }
            field(linearLayout, "due_odometer", "Próximo hodômetro (km) · opcional", decimal2, bundle, true, 32);
        }
        if (original != null) {
            uiKit.label(linearLayout, "Salvar cria a revisão " + (original.revision + 1) + ". O registro anterior continuará disponível.", 13, UiKit.MUTED, false);
        }
        TextView label2 = uiKit.label(linearLayout, "", 14, UiKit.AMBER, false);
        this.error = label2;
        label2.setAccessibilityLiveRegion(1);
        this.error.setTag("journal-error");
        this.saveButton = uiKit.button(linearLayout, "Salvar registro", true, () -> this.submit());
        this.cancelButton = uiKit.button(linearLayout, "Cancelar", false, () -> this.dialog.dismiss());
        dialog.setContentView(column);
        if (Build.VERSION.SDK_INT >= 30) {
            column.setOnApplyWindowInsetsListener((view, windowInsets) -> {
                Insets insets = windowInsets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                view.setPadding(this.ui.dp(20) + insets.left, this.ui.dp(16) + insets.top, this.ui.dp(20) + insets.right, this.ui.dp(16) + insets.bottom);
                return windowInsets;
            });
        } else {
            column.setOnApplyWindowInsetsListener((view, windowInsets) -> {
                view.setPadding(this.ui.dp(20) + windowInsets.getSystemWindowInsetLeft(), this.ui.dp(16) + windowInsets.getSystemWindowInsetTop(), this.ui.dp(20) + windowInsets.getSystemWindowInsetRight(), this.ui.dp(16) + windowInsets.getSystemWindowInsetBottom());
                return windowInsets;
            });
        }
        column.requestApplyInsets();
    }

    private static String title(JournalEntry.Kind kind) {
        if (kind == JournalEntry.Kind.SYMPTOM) {
            return "sintoma";
        }
        return kind == JournalEntry.Kind.SERVICE ? "serviço" : "abastecimento";
    }

    private static String decimal(BigDecimal bigDecimal) {
        return bigDecimal == null ? "" : bigDecimal.toPlainString().replace('.', ',');
    }

    private EditText field(LinearLayout linearLayout, String text, String title, String serverUrl, Bundle bundle, boolean flag, int i) {
        UiKit uiKit = this.ui;
        if (bundle != null) {
            serverUrl = bundle.getString(text, serverUrl);
        }
        EditText input = uiKit.input(linearLayout, title, serverUrl, "journal-" + text);
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(i)});
        if (flag) {
            input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            input.setKeyListener(DigitsKeyListener.getInstance("0123456789.,"));
        }
        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable editable) {
            }

            @Override
            public void beforeTextChanged(CharSequence charSequence, int value, int value2, int value3) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int value4, int value5, int value6) {
                if (JournalEditor.this.error != null) {
                    JournalEditor.this.error.setText("");
                }
            }
        });
        this.fields.put(text, input);
        return input;
    }

    private CheckBox check(LinearLayout linearLayout, String text, String tag, boolean checked) {
        CheckBox checkBox = new CheckBox(this.host);
        checkBox.setText(text);
        checkBox.setTextColor(this.ui.palette.text);
        checkBox.setTextSize(14.0f);
        checkBox.setMinHeight(this.ui.dp(48));
        checkBox.setTag(tag);
        checkBox.setChecked(checked);
        linearLayout.addView(checkBox, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return checkBox;
    }

    private LocalDateTime time() {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(this.occurrence), ZoneId.systemDefault());
    }

    public void pickDate() {
        final LocalDateTime time = time();
        new DatePickerDialog(this.host, (datePicker, i, i2, i3) -> {
            this.occurrence = LocalDateTime.of(LocalDate.of(i, i2 + 1, i3), time.toLocalTime()).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            updateTimeLabels();
        }, time.getYear(), time.getMonthValue() - 1, time.getDayOfMonth()).show();
    }

    public void pickTime() {
        final LocalDateTime time = time();
        new TimePickerDialog(this.host, (timePicker, i, i2) -> {
            this.occurrence = LocalDateTime.of(time.toLocalDate(), LocalTime.of(i, i2)).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            updateTimeLabels();
        }, time.getHour(), time.getMinute(), true).show();
    }

    private void updateTimeLabels() {
        this.dateButton.setText("Data: " + time().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        this.timeButton.setText("Horário: " + time().format(DateTimeFormatter.ofPattern("HH:mm")));
    }

    public void pickDue() {
        LocalDate now = this.dueDate.isEmpty() ? LocalDate.now() : LocalDate.parse(this.dueDate);
        new DatePickerDialog(this.host, (datePicker, i, i2, i3) -> {
            this.dueDate = LocalDate.of(i, i2 + 1, i3).toString();
            updateDueLabel();
        }, now.getYear(), now.getMonthValue() - 1, now.getDayOfMonth()).show();
    }

    private void updateDueLabel() {
        String text;
        Button button = this.dueButton;
        if (this.dueDate.isEmpty()) {
            text = "Definir próxima data";
        } else {
            text = "Próxima data: " + LocalDate.parse(this.dueDate).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        }
        button.setText(text);
    }

    private String value(String text) {
        return this.fields.containsKey(text) ? this.fields.get(text).getText().toString() : "";
    }

    public void submit() {
        long value;
        long value2;
        if (this.saving) {
            return;
        }
        try {
            long currentTimeMillis = System.currentTimeMillis();
            if (this.occurrence > 60000 + currentTimeMillis) {
                throw new IllegalArgumentException("Use uma data e horário já ocorridos para registrar o acontecimento.");
            }
            String id = this.id;
            String vehicleId = this.vehicleId;
            String vehicleName = this.vehicleName;
            JournalEntry.Kind kind = this.kind;
            JournalEntry journalEntry = this.original;
            int i = journalEntry == null ? 1 : journalEntry.revision + 1;
            long occurrence = this.occurrence;
            long createdAt = this.createdAt;
            JournalEntry original = this.original;
            if (original == null) {
                value2 = currentTimeMillis;
                value = createdAt;
            } else {
                value = original.revisedAt;
                value2 = currentTimeMillis;
            }
            long max = Math.max(value2, value);
            String note = value("note");
            BigDecimal decimal = JournalEntry.decimal(value("odometer"), 3);
            BigDecimal decimal2 = JournalEntry.decimal(value("liters"), 3);
            BigDecimal decimal3 = JournalEntry.decimal(value("cost"), 2);
            String fuel = this.fuel == null ? "" : JournalEntry.FUELS.get(this.fuel.getSelectedItemPosition());
            CheckBox checkBox = this.full;
            boolean full = checkBox != null && checkBox.isChecked();
            CheckBox complete = this.complete;
            JournalEntry journalEntry2 = new JournalEntry(id, vehicleId, vehicleName, kind, i, occurrence, createdAt, max, note, decimal, decimal2, decimal3, fuel, full, complete != null && complete.isChecked(), this.dueDate, JournalEntry.decimal(value("due_odometer"), 3));
            setSaving(true);
            this.save.save(journalEntry2, new Completion() {
                @Override
                public final void complete(String text3) {
                    JournalEditor.this.finishSubmit(text3);
                }
            });
        } catch (Exception e) {
            setSaving(false);
            this.error.setText(e.getMessage());
        }
    }

    void finishSubmit(String text) {
        if (this.dialog.isShowing()) {
            setSaving(false);
            if (text == null) {
                this.dialog.dismiss();
                return;
            }
            this.error.setText(text);
            this.saveButton.setEnabled(true);
            this.cancelButton.setEnabled(true);
        }
    }

    private void setSaving(boolean saving) {
        this.saving = saving;
        this.saveButton.setText(saving ? "Salvando…" : "Salvar registro");
        enable(this.form, !saving);
        CheckBox checkBox = this.complete;
        if (checkBox != null) {
            checkBox.setEnabled(!saving && this.full.isChecked());
        }
    }

    private static void enable(View view, boolean enabled) {
        view.setEnabled(enabled);
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                enable(viewGroup.getChildAt(i), enabled);
            }
        }
    }

    public Bundle state() throws Exception {
        Bundle bundle = new Bundle();
        bundle.putString("id", this.id);
        bundle.putString("vehicle_id", this.vehicleId);
        bundle.putString("vehicle_name", this.vehicleName);
        bundle.putString("kind", this.kind.name());
        JournalEntry journalEntry = this.original;
        bundle.putString("base", journalEntry == null ? "" : JournalJson.encode(journalEntry).toString());
        bundle.putLong("created", this.createdAt);
        bundle.putLong("occurrence", this.occurrence);
        bundle.putString("due_date", this.dueDate);
        for (Map.Entry<String, EditText> entry : this.fields.entrySet()) {
            bundle.putString(entry.getKey(), entry.getValue().getText().toString());
        }
        Spinner spinner = this.fuel;
        if (spinner != null) {
            bundle.putInt("fuel", spinner.getSelectedItemPosition());
            bundle.putBoolean("full", this.full.isChecked());
            bundle.putBoolean("complete", this.complete.isChecked());
        }
        return bundle;
    }

    public void show() {
        this.dialog.show();
        this.dialog.getWindow().setLayout(-1, -1);
        this.ui.palette.window(this.dialog.getWindow());
        this.dialog.getWindow().setSoftInputMode(16);
    }

    public boolean isShowing() {
        return this.dialog.isShowing();
    }

    public void dismiss() {
        this.dialog.dismiss();
    }
}