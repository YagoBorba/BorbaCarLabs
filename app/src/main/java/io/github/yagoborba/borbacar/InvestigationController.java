package io.github.yagoborba.borbacar;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.text.InputFilter;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import io.github.yagoborba.borbacar.diagnostics.DiagnosticCase;
import io.github.yagoborba.borbacar.ui.Format;
import io.github.yagoborba.borbacar.history.DiagnosticCaseJson;
import io.github.yagoborba.borbacar.history.DiagnosticCaseStore;
import io.github.yagoborba.borbacar.model.DataOrigin;
import io.github.yagoborba.borbacar.ui.LocalizedDialog;
import io.github.yagoborba.borbacar.ui.Notice;
import io.github.yagoborba.borbacar.ui.ThemePalette;
import io.github.yagoborba.borbacar.ui.WallClock;
import io.github.yagoborba.borbacar.vehicle.VehicleProfile;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Investigations: saving symptoms with a reading, revisions, opening, deleting and
 * exporting cases. Storage runs on the case worker; the screen only reads the state
 * kept here and asks the host to render again when it changes.
 */
final class InvestigationController {
    static final int EXPORT_REQUEST = 502;
    static final String EXPORT_PREFIX = "investigacao-";

    interface Host {
        void render();

        void message(String text);

        String error(Exception e);

        void post(Runnable action);

        VehicleProfile activeVehicle();

        Set<String> profileIds();

        TextView label(LinearLayout parent, String text, int size, int color, boolean medium);

        Button button(LinearLayout parent, String text, boolean primary, Runnable action);

        void gap(LinearLayout parent, int height);

        LinearLayout card(LinearLayout parent);

        int dp(float value);
    }

    private final Activity activity;
    private final Host host;
    private final DiagnosticCaseStore store;
    private final ExecutorService worker;
    private final ObdReader.Clock clock;
    private final ThemePalette palette;
    private List<JSONObject> documents = Collections.emptyList();
    private String warning = "";
    private boolean busy;
    private boolean exportPreparing;
    private File pendingExport;

    InvestigationController(Activity activity, Host host, DiagnosticCaseStore store, ExecutorService worker, ObdReader.Clock clock) {
        this.activity = activity;
        this.host = host;
        this.store = store;
        this.worker = worker;
        this.clock = clock;
        this.palette = ThemePalette.of(activity);
    }

    /** True while a case is being saved; readings and other case actions wait for it. */
    boolean busy() {
        return this.busy;
    }

    /** True while a case export is being prepared or waits for its destination. */
    boolean exporting() {
        return this.exportPreparing || this.pendingExport != null;
    }

    List<JSONObject> documents() {
        return this.documents;
    }

    String warning() {
        return this.warning;
    }

    File pendingExport() {
        return this.pendingExport;
    }

    /** Restores an export that was waiting for its destination when the activity was recreated. */
    void restorePendingExport(String path) {
        if (path == null || path.isEmpty()) {
            return;
        }
        try {
            File file = new File(path);
            if (file.getCanonicalFile().getParentFile().equals(this.activity.getCacheDir().getCanonicalFile())
                    && file.getName().startsWith(EXPORT_PREFIX) && file.isFile()) {
                this.pendingExport = file;
            }
        } catch (IOException ignored) {
            // A path that cannot be resolved is not an export of this app.
        }
    }

    void refresh() {
        this.worker.execute(() -> {
            try {
                final List<JSONObject> latest = this.store.latestAll();
                this.host.post(() -> {
                    this.documents = latest;
                    this.warning = "";
                    this.host.render();
                });
            } catch (Exception e) {
                this.host.post(() -> {
                    this.warning = "Investigações indisponíveis: " + this.host.error(e) + ". Os registros originais foram preservados.";
                    this.host.render();
                });
            }
        });
    }

    void list(LinearLayout body) {
        LinearLayout card = this.host.card(body);
        this.host.label(card, "INVESTIGAÇÕES OFFLINE", 11, this.palette.accent, true);
        this.host.gap(card, 8);
        this.host.label(card, "Guarde sintomas junto da leitura, revise e reabra sem internet. A revisão organiza evidências; ainda não propõe causas mecânicas.", 13, this.palette.muted, false);
        if (!this.warning.isEmpty()) {
            this.host.label(card, this.warning, 13, this.palette.attention, false);
        }
        if (this.busy) {
            this.host.label(card, "Salvando investigação…", 13, this.palette.attention, false);
        }
        VehicleProfile activeVehicle = this.host.activeVehicle();
        final Set<String> profiles = this.host.profileIds();
        int shown = 0;
        int orphaned = 0;
        for (final JSONObject document : this.documents) {
            JSONObject input = document.optJSONObject("input");
            if (input == null) {
                continue;
            }
            String vehicleId = input.optJSONObject("vehicle").optString("id");
            if (!profiles.contains(vehicleId)) {
                orphaned++;
            }
            if (activeVehicle != null && activeVehicle.id.equals(vehicleId)) {
                shown++;
                this.host.label(card, "Revisado " + Format.when(input.optLong("revised_at_ms")) + " · " + input.optJSONObject("symptoms").optString("description", ""), 12, this.palette.muted, false);
                String caseId = input.optString("case_id");
                String title = "Caso " + caseId.substring(0, Math.min(8, caseId.length()))
                        + " · revisão " + input.optInt("revision")
                        + (input.optJSONObject("dtc_read").optString("origin").equals("SIMULATED") ? " · SIMULADO" : "");
                this.host.button(card, title, false, () -> show(document));
            }
        }
        if (shown == 0) {
            this.host.label(card, "Nenhuma investigação salva para o veículo selecionado. Use Investigar esta leitura após ler os códigos.", 13, this.palette.muted, false);
        }
        if (orphaned > 0) {
            this.host.button(card, "Investigações de perfis removidos", false, () -> showRemovedProfileCases(profiles));
        }
    }

    private void showRemovedProfileCases(Set<String> profiles) {
        List<JSONObject> items = new ArrayList<>();
        for (JSONObject document : this.documents) {
            if (!profiles.contains(document.optJSONObject("input").optJSONObject("vehicle").optString("id"))) {
                items.add(document);
            }
        }
        choose("Perfis removidos · histórico preservado", items);
    }

    /** Opens the symptoms form for a new case ({@code isNew}) or a new revision of an existing one. */
    void edit(final DiagnosticCase diagnosticCase, final boolean isNew) {
        if (this.busy) {
            return;
        }
        LinearLayout form = new LinearLayout(this.activity);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(this.host.dp(18.0f), this.host.dp(8.0f), this.host.dp(18.0f), this.host.dp(12.0f));
        String origin = diagnosticCase.dtc.origin == DataOrigin.SIMULATED ? "SIMULADO" : "LEITURA DO VEÍCULO";
        this.host.label(form, diagnosticCase.vehicle.name + " · " + origin, 14, this.palette.accent, true);
        this.host.label(form, "Informações opcionais, declaradas por você. Salvar não faz nova leitura nem executa comandos.", 13, this.palette.muted, false);
        String[] questions = {"O que você percebeu?", "Quando ocorre?", "Com que frequência?", "Condições e observações"};
        String[] answers = {diagnosticCase.symptoms.description, diagnosticCase.symptoms.when, diagnosticCase.symptoms.frequency, diagnosticCase.symptoms.conditions};
        final EditText[] fields = new EditText[4];
        for (int i = 0; i < fields.length; i++) {
            this.host.label(form, questions[i], 13, this.palette.muted, false);
            EditText field = new EditText(this.activity);
            field.setTextColor(this.palette.text);
            field.setTextSize(15.0f);
            field.setText(answers[i]);
            field.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
            field.setFilters(new InputFilter[]{new InputFilter.LengthFilter(2000)});
            field.setMinLines(2);
            field.setTag("case-symptom-" + i);
            form.addView(field);
            fields[i] = field;
        }
        ScrollView scrollView = new ScrollView(this.activity);
        scrollView.setBackgroundColor(this.palette.card);
        scrollView.addView(form);
        final AlertDialog dialog = new LocalizedDialog(this.activity)
                .setTitle(isNew ? "Nova investigação" : "Revisar sintomas")
                .setView(scrollView)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Salvar caso", null)
                .create();
        dialog.setOnShowListener((shown) -> bindSaveButton(dialog, fields, isNew, diagnosticCase));
        dialog.show();
    }

    private void bindSaveButton(final AlertDialog dialog, final EditText[] fields, final boolean isNew, final DiagnosticCase diagnosticCase) {
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener((view) -> {
            if (this.busy) {
                return;
            }
            try {
                DiagnosticCase.Symptoms symptoms = new DiagnosticCase.Symptoms(
                        fields[0].getText().toString(), fields[1].getText().toString(),
                        fields[2].getText().toString(), fields[3].getText().toString());
                long now = this.clock.epochMillis();
                final DiagnosticCase savedCase = isNew
                        ? new DiagnosticCase(diagnosticCase.schemaVersion, diagnosticCase.id, 1, now, now, diagnosticCase.vehicle, diagnosticCase.adapter, diagnosticCase.dtc, diagnosticCase.telemetry, symptoms, diagnosticCase.supplement, diagnosticCase.vehicleRecord)
                        : diagnosticCase.revise(symptoms, now);
                this.busy = true;
                dialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
                dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setEnabled(false);
                dialog.setCancelable(false);
                this.worker.execute(() -> save(savedCase, dialog));
            } catch (Exception e) {
                this.host.message(this.host.error(e));
            }
        });
    }

    private void save(DiagnosticCase diagnosticCase, final AlertDialog dialog) {
        try {
            this.store.save(diagnosticCase);
            final List<JSONObject> latest = this.store.latestAll();
            final JSONObject saved = this.store.load(diagnosticCase.id, diagnosticCase.revision);
            this.host.post(() -> {
                this.busy = false;
                this.documents = latest;
                this.warning = "";
                dialog.dismiss();
                this.host.render();
                show(saved);
            });
        } catch (Exception e) {
            this.host.post(() -> {
                this.busy = false;
                dialog.setCancelable(true);
                dialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(true);
                dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setEnabled(true);
                this.host.message("Não foi possível concluir o salvamento: " + this.host.error(e) + ". Se houve gravação antes de uma falha de consulta, reabra a lista antes de tentar novamente.");
            });
        }
    }

    void show(final JSONObject document) {
        try {
            final DiagnosticCase diagnosticCase = DiagnosticCaseJson.validateDocument(document);
            final AlertDialog[] opened = new AlertDialog[1];
            LinearLayout content = new LinearLayout(this.activity);
            content.setOrientation(LinearLayout.VERTICAL);
            content.setPadding(this.host.dp(18.0f), this.host.dp(8.0f), this.host.dp(18.0f), this.host.dp(16.0f));
            this.host.label(content, String.join("\n", readableReport(document)), 14, this.palette.text, false);
            this.host.button(content, "Editar sintomas / criar revisão", false, () -> {
                opened[0].dismiss();
                edit(diagnosticCase, false);
            }).setEnabled(!this.busy);
            this.host.button(content, "Consultar revisões anteriores", false, () -> this.worker.execute(() -> loadRevisions(diagnosticCase)));
            this.host.button(content, "Exportar investigação JSON", false, () -> export(document, "json"));
            this.host.button(content, "Exportar investigação TXT", false, () -> export(document, "txt"));
            this.host.button(content, "Exportar investigação PDF", false, () -> export(document, "pdf"));
            this.host.button(content, "Excluir esta investigação", false, () -> confirmDeletion(diagnosticCase, opened));
            ScrollView scrollView = new ScrollView(this.activity);
            scrollView.setBackgroundColor(this.palette.card);
            scrollView.addView(content);
            AlertDialog dialog = new LocalizedDialog(this.activity)
                    .setTitle("Investigação · revisão " + diagnosticCase.revision)
                    .setView(scrollView)
                    .setPositiveButton("Fechar", null)
                    .create();
            opened[0] = dialog;
            dialog.show();
        } catch (Exception e) {
            this.host.message("Não foi possível abrir o caso: " + this.host.error(e));
        }
    }

    private void loadRevisions(final DiagnosticCase diagnosticCase) {
        try {
            final List<JSONObject> revisions = this.store.revisions(diagnosticCase.id);
            this.host.post(() -> choose("Revisões · " + diagnosticCase.id, revisions));
        } catch (Exception e) {
            this.host.post(() -> this.host.message(this.host.error(e)));
        }
    }

    private void confirmDeletion(final DiagnosticCase diagnosticCase, final AlertDialog[] opened) {
        new LocalizedDialog(this.activity)
                .setTitle("Excluir caso e todas as revisões?")
                .setMessage("Somente esta investigação será removida. O relatório de leitura e os outros casos serão preservados.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Excluir", (dialog, which) -> this.worker.execute(() -> {
                    try {
                        this.store.delete(diagnosticCase.id, diagnosticCase.vehicle.id);
                        final List<JSONObject> latest = this.store.latestAll();
                        this.host.post(() -> {
                            opened[0].dismiss();
                            this.documents = latest;
                            this.host.render();
                        });
                    } catch (Exception e) {
                        this.host.post(() -> this.host.message(this.host.error(e)));
                    }
                }))
                .show();
    }

    /** Lets the user pick one of the given case documents and opens it. */
    void choose(String title, final List<JSONObject> list) {
        String[] names = new String[list.size()];
        for (int i = 0; i < list.size(); i++) {
            JSONObject input = list.get(i).optJSONObject("input");
            names[i] = input.optJSONObject("vehicle").optString("name") + " · revisão " + input.optInt("revision") + " · " + Format.when(input.optLong("revised_at_ms"));
        }
        new LocalizedDialog(this.activity)
                .setTitle(title)
                .setItems(names, (dialog, which) -> show(list.get(which)))
                .setNegativeButton("Fechar", null)
                .show();
    }

    private void export(final JSONObject document, final String extension) {
        if (exporting()) {
            this.host.message("Conclua ou cancele a exportação anterior antes de iniciar outra.");
            return;
        }
        this.exportPreparing = true;
        this.worker.execute(() -> {
            try {
                final DiagnosticCase diagnosticCase = DiagnosticCaseJson.validateDocument(document);
                final File file = new File(this.activity.getCacheDir(), EXPORT_PREFIX + UUID.randomUUID() + "." + extension);
                if (extension.equals("pdf")) {
                    ReportWriter.pdf(file, readableReport(document));
                } else {
                    String content = extension.equals("txt") ? String.join("\n", readableReport(document)) : document.toString(2);
                    try (FileOutputStream output = new FileOutputStream(file)) {
                        output.write(content.getBytes(StandardCharsets.UTF_8));
                    }
                }
                this.host.post(() -> requestExportDestination(file, extension, diagnosticCase));
            } catch (Exception e) {
                this.host.post(() -> {
                    this.exportPreparing = false;
                    this.host.message(this.host.error(e));
                });
            }
        });
    }

    private void requestExportDestination(File file, String extension, DiagnosticCase diagnosticCase) {
        this.exportPreparing = false;
        this.pendingExport = file;
        String type = extension.equals("pdf") ? "application/pdf" : extension.equals("txt") ? "text/plain" : "application/json";
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType(type)
                .putExtra(Intent.EXTRA_TITLE, EXPORT_PREFIX + diagnosticCase.id + "-r" + diagnosticCase.revision + "." + extension);
        try {
            this.activity.startActivityForResult(intent, EXPORT_REQUEST);
        } catch (Exception e) {
            this.pendingExport = null;
            file.delete();
            this.host.message(this.host.error(e));
        }
    }

    /** Receives the destination chosen for a pending export and copies the file there. */
    void exportResult(int resultCode, Intent data) {
        final File file = this.pendingExport;
        this.pendingExport = null;
        if (file == null) {
            return;
        }
        if (resultCode != Activity.RESULT_OK || data == null || data.getData() == null) {
            file.delete();
            return;
        }
        final Uri destination = data.getData();
        this.worker.execute(() -> writeExport(file, destination));
    }

    private void writeExport(File file, Uri destination) {
        try (InputStream input = new FileInputStream(file);
             OutputStream output = this.activity.getContentResolver().openOutputStream(destination)) {
            if (output == null) {
                throw new IOException("Destino indisponível");
            }
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
            this.host.post(() -> Notice.show(this.activity, "Investigação exportada."));
        } catch (Exception e) {
            this.host.post(() -> this.host.message("Falha ao exportar: " + this.host.error(e)));
        } finally {
            file.delete();
        }
    }

    static List<String> readableReport(JSONObject document) throws JSONException {
        List<String> lines = new ArrayList<>(DiagnosticCaseJson.report(document));
        lines.addAll(ReportText.referenceAppendix(DiagnosticCaseJson.decode(document.getJSONObject("input"))));
        return ReportText.readable(lines, WallClock.zone());
    }
}
