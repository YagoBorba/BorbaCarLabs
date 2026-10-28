package io.rea.obd.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Insets;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import io.rea.obd.vehicle.VehicleAttribute;
import io.rea.obd.vehicle.VehicleDraft;
import io.rea.obd.vehicle.VehiclePresentation;
import io.rea.obd.vehicle.VehicleRecord;
import io.rea.obd.vehicle.VehicleRecordJson;
import io.rea.obd.vehicle.catalog.VehicleCatalog;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import org.json.JSONObject;

public final class VehicleEditor {
    private final Activity activity;
    private final VehicleCatalog catalog;
    private LinearLayout content;
    private boolean detailsExpanded;
    private final Dialog dialog;
    private final VehicleDraft draft;
    private AlertDialog manualEntry;
    private EditText manualInput;
    private VehicleAttribute.Key manualKey;
    private String restoredManualValue;
    private final Save save;
    private int step;
    private final UiKit ui;
    private String warning = "";

    public interface Save {
        void save(VehicleRecord vehicleRecord, int i, VehicleCatalog vehicleCatalog) throws Exception;
    }

    public VehicleEditor(Activity activity, VehicleRecord vehicleRecord, VehicleCatalog vehicleCatalog, Save save, Bundle bundle) throws Exception {
        this.activity = activity;
        this.catalog = vehicleCatalog;
        this.detailsExpanded = bundle != null && bundle.getBoolean("details_expanded");
        this.save = save;
        UiKit uiKit = new UiKit(activity);
        this.ui = uiKit;
        if (bundle == null) {
            this.draft = new VehicleDraft(vehicleRecord);
        } else {
            VehicleRecord vehicleRecord2 = bundle.containsKey("original") ? VehicleRecordJson.decode(new JSONObject(bundle.getString("original"))) : null;
            VehicleRecord vehicleRecord3 = VehicleRecordJson.decode(new JSONObject(bundle.getString("pending")));
            this.draft = new VehicleDraft(vehicleRecord2, vehicleRecord3.id, bundle.getString("name", ""), vehicleRecord3.attributes());
            this.step = Math.max(0, Math.min(2, bundle.getInt("step")));
        }
        Dialog dialog = new Dialog(activity, uiKit.palette.style()) {
            @Override
            public void onBackPressed() {
                VehicleEditor.this.back();
            }
        };
        this.dialog = dialog;
        if (bundle != null && bundle.containsKey("manual_key")) {
            this.manualKey = VehicleAttribute.Key.valueOf(bundle.getString("manual_key"));
            this.restoredManualValue = bundle.getString("manual_value", "");
        }
        dialog.setOnCancelListener((dialog2) -> {
        });
        render();
    }

    public void show() {
        this.dialog.show();
        Window window = this.dialog.getWindow();
        window.setLayout(-1, -1);
        this.ui.palette.window(window);
        window.setSoftInputMode(16);
        String restoredManualValue = this.restoredManualValue;
        if (restoredManualValue != null) {
            manual(this.manualKey, restoredManualValue);
            this.restoredManualValue = null;
        }
    }

    public void dismiss() {
        AlertDialog alertDialog = this.manualEntry;
        if (alertDialog != null) {
            alertDialog.dismiss();
        }
        this.dialog.dismiss();
    }

    public boolean isShowing() {
        return this.dialog.isShowing();
    }

    public Bundle state() throws Exception {
        Bundle bundle = new Bundle();
        if (this.draft.original != null) {
            bundle.putString("original", VehicleRecordJson.encode(this.draft.original).toString());
        }
        bundle.putString("pending", VehicleRecordJson.encode(new VehicleRecord(this.draft.id, this.draft.name.trim().isEmpty() ? "Rascunho" : this.draft.name, this.draft.expectedRevision() + 1, this.draft.attributes())).toString());
        bundle.putString("name", this.draft.name);
        bundle.putInt("step", this.step);
        bundle.putBoolean("details_expanded", this.detailsExpanded);
        AlertDialog alertDialog = this.manualEntry;
        if (alertDialog != null && alertDialog.isShowing()) {
            bundle.putString("manual_key", this.manualKey.name());
            bundle.putString("manual_value", this.manualInput.getText().toString());
        }
        return bundle;
    }

    public void back() {
        int i = this.step;
        if (i <= 0) {
            this.dialog.dismiss();
            return;
        }
        this.step = i - 1;
        this.warning = "";
        render();
    }

    private void render() {
        LinearLayout column = this.ui.column();
        column.setBackgroundColor(this.ui.palette.bg);
        column.setPadding(this.ui.dp(20), this.ui.dp(16), this.ui.dp(20), this.ui.dp(12));
        this.dialog.setContentView(column);
        if (Build.VERSION.SDK_INT >= 30) {
            column.setOnApplyWindowInsetsListener((view, windowInsets) -> {
                Insets insets = windowInsets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                view.setPadding(this.ui.dp(20) + insets.left, this.ui.dp(16) + insets.top, this.ui.dp(20) + insets.right, this.ui.dp(12) + insets.bottom);
                return windowInsets;
            });
        } else {
            column.setOnApplyWindowInsetsListener((view, windowInsets) -> {
                view.setPadding(this.ui.dp(20) + windowInsets.getSystemWindowInsetLeft(), this.ui.dp(16) + windowInsets.getSystemWindowInsetTop(), this.ui.dp(20) + windowInsets.getSystemWindowInsetRight(), this.ui.dp(12) + windowInsets.getSystemWindowInsetBottom());
                return windowInsets;
            });
        }
        column.requestApplyInsets();
        ScrollView scrollView = new ScrollView(this.activity);
        scrollView.setFillViewport(true);
        column.addView(scrollView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));
        LinearLayout column2 = this.ui.column();
        this.content = column2;
        scrollView.addView(column2);
        this.ui.label(this.content, "GARAGEM · PASSO " + (this.step + 1) + " DE 3", 12, UiKit.ACCENT, true);
        this.ui.label(this.content, new String[]{"Qual é o seu carro?", "Versão e detalhes", "Revise seu perfil"}[this.step], 28, UiKit.TEXT, true);
        if (!this.warning.isEmpty()) {
            this.ui.label(this.content, this.warning, 14, UiKit.AMBER, true).setAccessibilityLiveRegion(1);
        }
        int i = this.step;
        if (i == 0) {
            this.ui.label(this.content, "Escolha marca, modelo e ano. Se não encontrar, você pode criar um perfil simples.", 15, UiKit.MUTED, false);
            field(VehicleAttribute.Key.MAKE);
            field(VehicleAttribute.Key.MODEL);
            field(VehicleAttribute.Key.MODEL_YEAR);
            EditText input = this.ui.input(this.content, "Apelido (opcional com modelo escolhido)", this.draft.name, "vehicle-name");
            input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(100)});
            input.setHint("Ex.: Meu carro");
            input.addTextChangedListener(new TextWatcher() {
                @Override
                public void afterTextChanged(Editable editable) {
                }

                @Override
                public void beforeTextChanged(CharSequence charSequence, int value, int value2, int value3) {
                }

                @Override
                public void onTextChanged(CharSequence charSequence, int value4, int value5, int value6) {
                    VehicleEditor.this.draft.name = charSequence.toString();
                }
            });
            UiKit uiKit = this.ui;
            LinearLayout linearLayout = this.content;
            VehicleCatalog vehicleCatalog = this.catalog;
            uiKit.label(linearLayout, vehicleCatalog == null ? "Catálogo indisponível. Cadastro simples disponível." : vehicleCatalog.coverageNote, 12, UiKit.MUTED, false);
        } else if (i == 1) {
            List<VehicleCatalog.Configuration> configurations = configurations();
            this.ui.label(this.content, "Escolha uma versão para preencher motor, câmbio e combustível juntos.", 15, UiKit.MUTED, false);
            this.ui.button(this.content, "Escolher versão do catálogo", true, () -> this.configurationPicker()).setEnabled(!configurations.isEmpty());
            if (configurations.isEmpty()) {
                this.ui.label(this.content, "Sem versão documentada para esta seleção. Você pode salvar um perfil parcial.", 13, UiKit.MUTED, false);
            }
            VehicleAttribute.Key[] keys = {VehicleAttribute.Key.TRIM, VehicleAttribute.Key.ENGINE, VehicleAttribute.Key.TRANSMISSION, VehicleAttribute.Key.FUEL_TYPE};
            for (int i2 = 0; i2 < 4; i2++) {
                VehicleAttribute.Key key = keys[i2];
                VehicleAttribute attribute = this.draft.attribute(key);
                if (attribute.value != null) {
                    this.ui.label(this.content, VehiclePresentation.title(key) + " · " + attribute.value, 16, UiKit.TEXT, true);
                    this.ui.label(this.content, VehiclePresentation.compactOrigin(attribute), 12, UiKit.MUTED, false);
                }
            }
            this.ui.row(this.content, this.detailsExpanded ? "Ocultar detalhes opcionais" : "Outros detalhes (opcional)", () -> {
                this.detailsExpanded = !this.detailsExpanded;
                render();
            });
            if (this.detailsExpanded) {
                VehicleAttribute.Key[] keys2 = {VehicleAttribute.Key.MANUFACTURE_YEAR, VehicleAttribute.Key.MARKET, VehicleAttribute.Key.TRIM, VehicleAttribute.Key.ENGINE, VehicleAttribute.Key.TRANSMISSION, VehicleAttribute.Key.FUEL_TYPE, VehicleAttribute.Key.NOTES};
                for (int i3 = 0; i3 < 7; i3++) {
                    field(keys2[i3]);
                }
            }
        } else {
            VehicleRecord vehicleRecord = this.draft.result();
            this.ui.label(this.content, vehicleRecord.name, 24, UiKit.TEXT, true);
            int value7 = 0;
            for (VehicleAttribute vehicleAttribute : vehicleRecord.attributes().values()) {
                if (vehicleAttribute.value == null) {
                    value7++;
                } else {
                    this.ui.label(this.content, VehiclePresentation.title(vehicleAttribute.key) + " · " + vehicleAttribute.value, 16, UiKit.TEXT, true);
                    this.ui.label(this.content, VehiclePresentation.compactOrigin(vehicleAttribute), 12, UiKit.MUTED, false);
                }
            }
            if (value7 > 0) {
                this.ui.label(this.content, "Detalhes não preenchidos continuam em branco. Você pode completar depois.", 13, UiKit.MUTED, false);
            }
            this.ui.label(this.content, "A versão é escolhida por você. Dados de catálogo não identificam o carro nem comprovam suporte OBD.", 13, UiKit.MUTED, false);
            LinearLayout disclosure = this.ui.disclosure(this.content, "Ver fontes e identificação completa");
            Iterator<String> it = VehiclePresentation.report(vehicleRecord).iterator();
            while (it.hasNext()) {
                this.ui.label(disclosure, it.next(), 13, UiKit.MUTED, false);
            }
        }
        this.ui.button(column, this.step == 2 ? "Salvar perfil" : "Continuar", true, () -> this.updatePreview());
        if (this.step > 0) {
            this.ui.button(column, "Voltar", false, () -> this.back());
        }
        UiKit ui = this.ui;
        final Dialog dialog = this.dialog;
        Objects.requireNonNull(dialog);
        ui.button(column, "Cancelar", false, () -> dialog.dismiss());
    }

    void updatePreview() {
        String text;
        try {
            if (this.draft.name.trim().isEmpty()) {
                String text2 = this.draft.attribute(VehicleAttribute.Key.MODEL).value;
                if (text2 == null) {
                    throw new IllegalArgumentException("Informe um apelido ou escolha um modelo.");
                }
                VehicleDraft vehicleDraft = this.draft;
                StringBuilder builder = new StringBuilder();
                builder.append(text2);
                if (this.draft.attribute(VehicleAttribute.Key.MODEL_YEAR).value == null) {
                    text = "";
                } else {
                    text = " " + this.draft.attribute(VehicleAttribute.Key.MODEL_YEAR).value;
                }
                builder.append(text);
                vehicleDraft.name = builder.toString();
            }
            int i = this.step;
            if (i >= 2) {
                this.save.save(this.draft.result(), this.draft.expectedRevision(), this.catalog);
                this.dialog.dismiss();
            } else {
                this.step = i + 1;
                this.warning = "";
                render();
            }
        } catch (Exception e) {
            this.warning = e.getMessage() == null ? "Não foi possível salvar. Revise os dados e tente novamente." : e.getMessage();
            render();
        }
    }

    private List<VehicleCatalog.Configuration> configurations() {
        if (this.catalog == null || this.draft.attribute(VehicleAttribute.Key.MAKE).value == null || this.draft.attribute(VehicleAttribute.Key.MODEL).value == null) {
            return Collections.emptyList();
        }
        EnumMap enumMap = new EnumMap(VehicleAttribute.Key.class);
        VehicleAttribute.Key[] keys = {VehicleAttribute.Key.MAKE, VehicleAttribute.Key.MODEL, VehicleAttribute.Key.MODEL_YEAR};
        for (int i = 0; i < 3; i++) {
            VehicleAttribute.Key key = keys[i];
            if (this.draft.attribute(key).value != null) {
                enumMap.put(key, this.draft.attribute(key).value);
            }
        }
        return this.catalog.matching(enumMap);
    }

    public void configurationPicker() {
        final List<VehicleCatalog.Configuration> configurations = configurations();
        ArrayList items = new ArrayList();
        Iterator<VehicleCatalog.Configuration> it = configurations.iterator();
        while (true) {
            if (!it.hasNext()) {
                new AlertDialog.Builder(this.activity).setTitle("Versão documentada · escolha a sua").setItems((CharSequence[]) items.toArray(new String[0]), (dialog, which) -> {
                    try {
                        this.draft.chooseConfiguration(this.catalog, ((VehicleCatalog.Configuration) configurations.get(which)).id);
                        this.warning = "";
                        render();
                    } catch (Exception e) {
                        this.warning = e.getMessage();
                        render();
                    }
                }).setNegativeButton("Não sei / escolher depois", null).show();
                return;
            }
            VehicleCatalog.Configuration next = it.next();
            ArrayList items2 = new ArrayList();
            VehicleAttribute.Key[] keys = {VehicleAttribute.Key.MODEL_YEAR, VehicleAttribute.Key.TRIM, VehicleAttribute.Key.ENGINE, VehicleAttribute.Key.TRANSMISSION, VehicleAttribute.Key.FUEL_TYPE};
            for (int i = 0; i < keys.length; i++) {
                VehicleAttribute.Key key = keys[i];
                if (next.attributes.containsKey(key)) {
                    items2.add(next.attributes.get(key).value);
                }
            }
            items.add(String.join(" · ", items2));
        }
    }

    private void field(final VehicleAttribute.Key key) {
        VehicleAttribute attribute = this.draft.attribute(key);
        this.ui.label(this.content, VehiclePresentation.title(key), 13, UiKit.MUTED, true);
        Button button = this.ui.button(this.content, attribute.value == null ? "Selecionar" : attribute.value, false, () -> this.options(key));
        StringBuilder builder = new StringBuilder("Escolher ");
        builder.append(VehiclePresentation.title(key));
        builder.append(". ");
        builder.append(attribute.value == null ? "Opcional" : attribute.value);
        button.setContentDescription(builder.toString());
        button.setTag("vehicle-field-" + key.name());
        if (attribute.value != null) {
            this.ui.label(this.content, VehiclePresentation.compactOrigin(attribute), 12, UiKit.MUTED, false);
        }
    }

    public void options(final VehicleAttribute.Key key) {
        VehicleCatalog vehicleCatalog = this.catalog;
        final List<String> emptyList = vehicleCatalog == null ? Collections.emptyList() : this.draft.options(vehicleCatalog, key);
        final ArrayList items = new ArrayList();
        items.add("Não sei");
        items.addAll(emptyList);
        items.add("Não encontrei · informar por conta própria");
        AlertDialog.Builder builder = new AlertDialog.Builder(this.activity);
        StringBuilder builder2 = new StringBuilder();
        builder2.append(VehiclePresentation.title(key));
        builder2.append(emptyList.isEmpty() ? " · sem opções no catálogo" : " · catálogo");
        builder.setTitle(builder2.toString()).setItems((CharSequence[]) items.toArray(new String[0]), (dialog, which) -> {
            if (which == items.size() - 1) {
                manual(key);
                return;
            }
            try {
                if (which == 0) {
                    this.draft.unknown(key);
                } else {
                    this.draft.choose(this.catalog, key, (String) emptyList.get(which - 1));
                }
                this.warning = "";
                render();
            } catch (Exception e) {
                this.warning = e.getMessage();
                render();
            }
        }).setNegativeButton("Cancelar", null).show();
    }

    private void manual(VehicleAttribute.Key key) {
        manual(key, this.draft.attribute(key).value == null ? "" : this.draft.attribute(key).value);
    }

    private void manual(final VehicleAttribute.Key key, String serverUrl) {
        LinearLayout column = this.ui.column();
        column.setPadding(this.ui.dp(20), this.ui.dp(12), this.ui.dp(20), this.ui.dp(12));
        column.setBackgroundColor(this.ui.palette.bg);
        this.ui.label(column, "Informação declarada por você. Não confirma a configuração ou a compatibilidade OBD.", 14, UiKit.MUTED, false);
        if (VehicleDraft.GUIDED.contains(key)) {
            this.ui.label(column, "Alterar este campo limpa as escolhas seguintes de modelo, ano ou configuração. Confira tudo na revisão.", 13, UiKit.AMBER, false);
        }
        final EditText input = this.ui.input(column, VehiclePresentation.title(key), serverUrl, "vehicle-manual-value");
        this.manualKey = key;
        this.manualInput = input;
        InputFilter[] filters = new InputFilter[1];
        filters[0] = new InputFilter.LengthFilter(key == VehicleAttribute.Key.NOTES ? 300 : 100);
        input.setFilters(filters);
        if (key == VehicleAttribute.Key.MODEL_YEAR || key == VehicleAttribute.Key.MANUFACTURE_YEAR) {
            input.setInputType(InputType.TYPE_CLASS_NUMBER);
        }
        if (key == VehicleAttribute.Key.NOTES) {
            input.setSingleLine(false);
            input.setMinLines(3);
        }
        ScrollView scrollView = new ScrollView(this.activity);
        scrollView.addView(column);
        final AlertDialog manualEntry = new AlertDialog.Builder(this.activity).setTitle("Informar · " + VehiclePresentation.title(key)).setView(scrollView).setNegativeButton("Cancelar", null).setPositiveButton("Declarar informação", null).create();
        this.manualEntry = manualEntry;
        manualEntry.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public final void onShow(DialogInterface dialogInterface) {
                VehicleEditor.this.bindDeclarationConfirm(manualEntry, key, input, dialogInterface);
            }
        });
        manualEntry.show();
    }

    void bindDeclarationConfirm(final AlertDialog alertDialog, final VehicleAttribute.Key key, final EditText editText, DialogInterface dialogInterface) {
        alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener((view) -> {
            try {
                this.draft.confirmDeclaration(key, editText.getText().toString(), System.currentTimeMillis());
                alertDialog.dismiss();
                this.warning = "";
                render();
            } catch (Exception e) {
                editText.setError(e.getMessage());
            }
        });
    }
}