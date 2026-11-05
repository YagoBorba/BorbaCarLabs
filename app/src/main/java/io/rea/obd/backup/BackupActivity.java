package io.rea.obd.backup;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import io.rea.obd.MainActivity;
import io.rea.obd.ui.I18n;
import io.rea.obd.ui.LocalizedDialog;
import io.rea.obd.ui.ThemePalette;
import io.rea.obd.ui.UiKit;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;

public final class BackupActivity extends Activity {
    private final java.util.concurrent.ExecutorService worker = java.util.concurrent.Executors.newSingleThreadExecutor();
    private BackupRepository repository;
    private boolean working;
    private TextView status;
    private LinearLayout actions;
    private static String t(String pt, String en) { return I18n.english() ? en : pt; }

    @Override public void onCreate(Bundle state) {
        I18n.init(this);
        setTheme(ThemePalette.of(this).style());
        super.onCreate(state);
        ThemePalette.of(this).window(getWindow());
        repository = new BackupRepository(this, getSharedPreferences("MainActivity", MODE_PRIVATE));
        UiKit ui = new UiKit(this);
        LinearLayout body = ui.column();
        body.setPadding(ui.dp(20), ui.dp(20), ui.dp(20), ui.dp(20));
        ui.title(body, t("Backup e restauração", "Backup and restore"), 25);
        ui.label(body, t("Inclui garagem, leituras, investigações com revisões e diário. Não inclui CSV, senhas, configuração ou conversas com IA. O arquivo contém dados pessoais e não é criptografado; escolha um destino privado.",
                "Includes garage, readings, case revisions and journal. Excludes CSV, passwords, settings and AI conversations. The file contains personal data and is not encrypted; choose a private destination."), 15, UiKit.MUTED, false);
        status = ui.label(body, "", 14, UiKit.ACCENT, false);
        actions = ui.column(); body.addView(actions);
        ui.button(actions, t("Criar backup", "Create backup"), true, () -> {
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("application/json")
                    .putExtra(Intent.EXTRA_TITLE, "BorbaCar-backup.json");
            startActivityForResult(intent, 700);
        });
        ui.button(actions, t("Escolher backup para restaurar", "Choose backup to restore"), false, () ->
                startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("application/json"), 701));
        ui.row(actions, t("Desfazer última restauração", "Undo last restore"), () -> task(() -> {
            JSONObject archive = repository.previous();
            String summary = repository.validate(archive);
            runOnUiThread(() -> confirm(archive, summary));
        }));
        ui.row(actions, t("Voltar ao aplicativo", "Back to app"), this::finish);
        ScrollView scroll = new ScrollView(this); scroll.addView(body); setContentView(scroll);
    }

    private interface Work { void run() throws Exception; }
    private void task(Work work) {
        if (working) return;
        working = true; enable(actions, false);
        status.setText(t("Processando…", "Working…"));
        worker.execute(() -> {
            String failure = "";
            try { work.run(); } catch (Exception e) { failure = t("Operação não concluída: ", "Operation not completed: ") + I18n.t(e.getMessage()); }
            final String message = failure;
            runOnUiThread(() -> {
                working = false;
                if (isDestroyed()) return;
                enable(actions, true);
                status.setText(message.isEmpty() ? t("Operação concluída.", "Operation completed.") : message);
            });
        });
    }

    private static void enable(android.view.View view, boolean value) {
        view.setEnabled(value);
        if (view instanceof android.view.ViewGroup) {
            android.view.ViewGroup group = (android.view.ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) enable(group.getChildAt(i), value);
        }
    }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (result != RESULT_OK || data == null || data.getData() == null) return;
        if (request == 700) task(() -> {
            JSONObject archive = repository.snapshot();
            repository.validate(archive);
            try (OutputStream output = getContentResolver().openOutputStream(data.getData(), "wt")) {
                if (output == null) throw new java.io.IOException(t("Destino indisponível.", "Destination unavailable."));
                output.write(archive.toString().getBytes(StandardCharsets.UTF_8));
            }
        });
        if (request == 701) task(() -> {
            JSONObject archive = BackupRepository.read(getContentResolver().openInputStream(data.getData()));
            String summary = repository.validate(archive);
            runOnUiThread(() -> confirm(archive, summary));
        });
    }

    private void confirm(JSONObject archive, String summary) {
        if (isDestroyed() || isFinishing()) return;
        new LocalizedDialog(this).setTitle(t("Substituir os dados locais?", "Replace local data?"))
                .setMessage(summary + "\n\n" + t("Os registros atuais serão substituídos. Uma cópia anterior será mantida para desfazer. Confira o arquivo e só prossiga se confiar na origem.",
                        "Current records will be replaced. A previous copy is kept for undo. Check the file and proceed only if you trust its origin."))
                .setNegativeButton(t("Cancelar", "Cancel"), null).setPositiveButton(t("Restaurar", "Restore"), (d, w) -> task(() -> {
                    repository.restore(archive);
                    runOnUiThread(() -> {
                        if (!isDestroyed()) {
                            startActivity(new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                            finish();
                        }
                    });
                })).show();
    }

    @Override public void onBackPressed() { if (!working) super.onBackPressed(); }
    @Override protected void onDestroy() { worker.shutdown(); super.onDestroy(); }
}
