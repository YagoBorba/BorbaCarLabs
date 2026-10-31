package io.rea.obd.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import io.rea.obd.journal.FuelSummary;
import io.rea.obd.journal.JournalEntry;
import io.rea.obd.journal.JournalJson;
import io.rea.obd.journal.JournalStore;
import io.rea.obd.journal.MaintenanceReminders;
import io.rea.obd.vehicle.VehicleProfile;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.function.Predicate;
import org.json.JSONArray;
import org.json.JSONObject;

public final class JournalController {
    public static final String SUMMARY = "journal-summary";
    public static final String FIRST_ENTRY = "journal-first-entry";
    private static final ExecutorService IO = Executors.newSingleThreadExecutor((runnable) -> {
        Thread thread = new Thread(runnable, "journal-storage");
        thread.setDaemon(true);
        return thread;
    });
    private final Activity activity;
    private volatile boolean closed;
    private JournalEditor editor;
    private int filter;
    private final Host host;
    private int limit;
    private File pendingExport;
    private boolean ready;
    private boolean saving;
    private final JournalStore store;
    private final Handler main = new Handler(Looper.getMainLooper());
    private List<JournalEntry> entries = Collections.emptyList();
    private String warning = "";

    public static final class Glance {
        public final int count;
        public final FuelSummary fuel;
        public final JournalEntry reminder;
        public final String lastFuel;

        Glance(int count, FuelSummary fuel, JournalEntry reminder, String lastFuel) {
            this.count = count;
            this.fuel = fuel;
            this.reminder = reminder;
            this.lastFuel = lastFuel;
        }

        public boolean mixedFuel() {
            return this.fuel.explanation.contains("mista ou não confirmada");
        }
    }

    public Glance glance(VehicleProfile vehicle) {
        if (!this.ready || vehicle == null) {
            return null;
        }
        int count = 0;
        JournalEntry lastFill = null;
        for (JournalEntry entry : this.entries) {
            if (!entry.vehicleId.equals(vehicle.id) || entry.voided) {
                continue;
            }
            count++;
            if (entry.kind == JournalEntry.Kind.FUEL && (lastFill == null || entry.occurredAt > lastFill.occurredAt)) {
                lastFill = entry;
            }
        }
        return new Glance(count, FuelSummary.latest(this.entries, vehicle.id), MaintenanceReminders.next(this.entries, vehicle.id), lastFill == null ? "" : lastFill.fuel);
    }

    public interface Host {
        void changed();

        boolean demo();

        VehicleProfile vehicle();

        Set<String> vehicleIds();
    }

    public JournalController(Activity activity, final Host host, Bundle bundle) {
        this.limit = 20;
        this.activity = activity;
        this.host = host;
        this.store = new JournalStore(activity);
        if (bundle != null) {
            this.filter = Math.max(0, Math.min(3, bundle.getInt("filter")));
            this.limit = Math.max(20, Math.min(100000, bundle.getInt("limit", 20)));
            String string = bundle.getString("export", "");
            if (!string.isEmpty()) {
                try {
                    File file = new File(string);
                    if (file.getCanonicalFile().getParentFile().equals(activity.getCacheDir().getCanonicalFile()) && file.getName().startsWith("diario-") && file.isFile()) {
                        this.pendingExport = file;
                    }
                } catch (IOException ignored) {
                }
            }
        }
        IO.execute(() -> {
            try {
                final List<JournalEntry> latest = this.store.latest();
                post(() -> {
                    this.entries = latest;
                    this.ready = true;
                    host.changed();
                });
            } catch (Exception e) {
                post(() -> {
                    this.warning = "Diário indisponível: " + e.getMessage() + ". Os registros existentes foram preservados.";
                    host.changed();
                });
            }
        });
    }

    private void post(final Runnable runnable) {
        this.main.post(() -> {
            if (this.closed || this.activity.isFinishing()) {
                return;
            }
            runnable.run();
        });
    }

    public void render(LinearLayout linearLayout) {
        UiKit uiKit = new UiKit(this.activity);
        final VehicleProfile vehicle = this.host.vehicle();
        uiKit.label(linearLayout, "Diário do carro", 22, UiKit.TEXT, true).setPadding(0, 0, 0, uiKit.dp(2));
        uiKit.label(linearLayout, vehicle == null ? "Escolha um perfil na Garagem para registrar acontecimentos." : vehicle.name + " · informações declaradas por você", 13, UiKit.MUTED, false);
        if (!this.warning.isEmpty()) {
            uiKit.label(linearLayout, this.warning, 14, UiKit.AMBER, false);
        }
        if (!this.ready) {
            if (this.warning.isEmpty()) {
                uiKit.label(linearLayout, "Carregando seus registros…", 14, UiKit.MUTED, false);
            }
            return;
        }
        if (vehicle != null) {
            summary(uiKit, linearLayout, vehicle);
        }
        final String[] parts = {"Todos", "Sintomas", "Serviços", "Abastecimentos"};
        boolean sideBySide = this.activity.getResources().getConfiguration().fontScale <= 1.3f && this.activity.getResources().getConfiguration().screenWidthDp >= 340;
        LinearLayout actions = uiKit.column();
        actions.setOrientation(sideBySide ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
        linearLayout.addView(actions, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        Button add = uiKit.button(actions, "Novo registro", true, () -> new LocalizedDialog(this.activity).setTitle("O que você quer registrar?").setItems(new String[]{"Sintoma", "Serviço", "Abastecimento"}, (dialog, which) -> open(JournalEntry.Kind.values()[which], null, null)).setNegativeButton("Cancelar", null).show());
        add.setEnabled((vehicle == null || this.host.demo() || this.saving) ? false : true);
        Button filter = uiKit.button(actions, "Filtrar: " + parts[this.filter], false, () -> {
            new LocalizedDialog(this.activity).setTitle("Mostrar registros").setItems(parts, (dialog, which) -> {
                this.filter = which;
                this.limit = 20;
                this.host.changed();
            }).setNegativeButton("Cancelar", null).show();
        });
        if (sideBySide) {
            LinearLayout.LayoutParams first = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            first.setMargins(0, 0, uiKit.dp(8), uiKit.dp(12));
            add.setLayoutParams(first);
            LinearLayout.LayoutParams second = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            second.bottomMargin = uiKit.dp(12);
            filter.setLayoutParams(second);
        }
        if (this.host.demo()) {
            uiKit.label(linearLayout, "Encerre a demonstração para registrar acontecimentos reais informados por você.", 13, UiKit.AMBER, false);
        }
        int shown = 0;
        int matching = 0;
        for (final JournalEntry entry : this.entries) {
            if (vehicle != null && entry.vehicleId.equals(vehicle.id) && (this.filter == 0 || entry.kind.ordinal() == this.filter - 1)) {
                matching++;
                if (shown < this.limit) {
                    entry(uiKit, linearLayout, entry, shown == 0);
                    shown++;
                }
            }
        }
        if (matching == 0) {
            uiKit.label(linearLayout, "Nenhum registro neste filtro. Comece pelo que você percebeu ou fez no carro.", 15, UiKit.MUTED, false);
        }
        if (matching > this.limit) {
            uiKit.button(linearLayout, "Mostrar mais registros", false, () -> {
                this.limit += 20;
                this.host.changed();
            });
        }
        if (vehicle != null) {
            calculations(uiKit, uiKit.disclosure(linearLayout, "Consumo, custos e lembretes"), vehicle);
        }
        LinearLayout disclosure = uiKit.disclosure(linearLayout, "Opções do diário");
        uiKit.button(disclosure, "Exportar diário deste veículo", false, () -> this.exportVehicle(vehicle)).setEnabled((vehicle == null || !this.entries.stream().anyMatch((obj) -> ((JournalEntry) obj).vehicleId.equals(vehicle.id)) || this.saving) ? false : true);
        uiKit.button(disclosure, "Diários de perfis removidos", false, () -> this.detached()).setEnabled(this.entries.stream().anyMatch((obj) -> this.isDetached((JournalEntry) obj)));
        uiKit.label(disclosure, "Serviços com data ou quilometragem viram lembretes: aparecem em Hoje e geram notificação quando faltam 7 dias ou 500 km, se os lembretes estiverem ligados em Ajustes. Registrar o mesmo serviço sem novo prazo conclui o lembrete. Restaurar backup ainda não faz parte deste diário.", 12, UiKit.MUTED, false);
    }

    private void summary(UiKit uiKit, LinearLayout linearLayout, VehicleProfile vehicle) {
        FuelSummary fuel = FuelSummary.latest(this.entries, vehicle.id);
        JournalEntry reminder = MaintenanceReminders.next(this.entries, vehicle.id);
        JournalEntry last = null;
        for (JournalEntry entry : this.entries) {
            if (entry.vehicleId.equals(vehicle.id) && !entry.voided && (last == null || entry.occurredAt > last.occurredAt)) {
                last = entry;
            }
        }
        LinearLayout card = uiKit.card(linearLayout, false);
        card.setTag(SUMMARY);
        card.setPadding(uiKit.dp(14), uiKit.dp(12), uiKit.dp(14), uiKit.dp(8));
        ((LinearLayout.LayoutParams) card.getLayoutParams()).bottomMargin = uiKit.dp(12);
        boolean columns = this.activity.getResources().getConfiguration().fontScale <= 1.3f && this.activity.getResources().getConfiguration().screenWidthDp >= 340;
        card.setOrientation(columns ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
        fact(uiKit, card, columns, "CONSUMO", fuel.kmPerLiter == null ? "—" : Format.decimal(fuel.kmPerLiter) + " km/L", fuel.kmPerLiter == null ? "aguardando um intervalo completo" : "último tanque cheio a cheio", false);
        if (reminder == null) {
            fact(uiKit, card, columns, "PRÓXIMO SERVIÇO", "—", "sem lembrete registrado", false);
        } else {
            HomeScreen.Due due = HomeScreen.due(reminder);
            fact(uiKit, card, columns, "PRÓXIMO SERVIÇO", due.value, reminder.note.isEmpty() ? due.detail : reminder.note, due.overdue);
        }
        fact(uiKit, card, columns, "ÚLTIMO REGISTRO", last == null ? "—" : local(last.occurredAt).substring(0, 10), last == null ? "nenhum registro ainda" : last.title(), false);
    }

    private static void fact(UiKit uiKit, LinearLayout card, boolean columns, String title, String value, String detail, boolean attention) {
        LinearLayout item = uiKit.column();
        if (columns) {
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            if (card.getChildCount() > 0) {
                params.setMarginStart(uiKit.dp(10));
            }
            card.addView(item, params);
        } else {
            card.addView(item, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }
        uiKit.label(item, title, 10, UiKit.ACCENT, true).setPadding(0, 0, 0, uiKit.dp(2));
        uiKit.label(item, value, 15, attention ? UiKit.AMBER : UiKit.TEXT, true).setPadding(0, 0, 0, uiKit.dp(2));
        TextView small = uiKit.label(item, detail, 11, attention ? UiKit.AMBER : UiKit.MUTED, false);
        small.setPadding(0, 0, 0, uiKit.dp(columns ? 4 : 8));
        small.setMaxLines(columns ? 2 : 3);
        small.setEllipsize(TextUtils.TruncateAt.END);
    }

    private void entry(UiKit uiKit, LinearLayout linearLayout, final JournalEntry entry, boolean first) {
        LinearLayout card = uiKit.card(linearLayout, false);
        card.setPadding(uiKit.dp(16), uiKit.dp(12), uiKit.dp(16), uiKit.dp(8));
        ((LinearLayout.LayoutParams) card.getLayoutParams()).bottomMargin = uiKit.dp(10);
        if (first) {
            card.setTag(FIRST_ENTRY);
        }
        StringBuilder title = new StringBuilder(entry.title());
        title.append(entry.voided ? " · ANULADO" : "");
        uiKit.eyebrow(card, title.toString());
        uiKit.label(card, entry.note.isEmpty() ? "Abastecimento registrado" : entry.note, 16, UiKit.TEXT, true).setPadding(0, 0, 0, uiKit.dp(4));
        StringBuilder meta = new StringBuilder(local(entry.occurredAt));
        meta.append(entry.odometer != null ? " · " + Format.km(entry.odometer) : "");
        meta.append(entry.cost != null ? " · " + Format.money(entry.cost) : "");
        uiKit.label(card, meta.toString(), 12, UiKit.MUTED, false).setPadding(0, 0, 0, uiKit.dp(4));
        if (!entry.dueDate.isEmpty()) {
            uiKit.label(card, (entry.voided ? "Data do registro anulado: " : "Próxima data informada: ") + day(entry.dueDate), 12, UiKit.MUTED, false).setPadding(0, 0, 0, uiKit.dp(4));
        }
        if (entry.dueOdometer != null) {
            uiKit.label(card, "Próximo hodômetro informado: " + Format.km(entry.dueOdometer), 12, UiKit.MUTED, false).setPadding(0, 0, 0, uiKit.dp(4));
        }
        card.setClickable(true);
        card.setFocusable(true);
        card.setMinimumHeight(uiKit.dp(48));
        card.setOnClickListener((view) -> this.detail(entry));
        card.setAccessibilityDelegate(new View.AccessibilityDelegate() {
            @Override
            public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfo info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                info.addAction(new AccessibilityNodeInfo.AccessibilityAction(AccessibilityNodeInfo.ACTION_CLICK, I18n.t("Ver registro")));
            }
        });
        Motion.press(card);
    }

    private void calculations(UiKit uiKit, LinearLayout disclosure, VehicleProfile vehicle) {
        final FuelSummary latest = FuelSummary.latest(this.entries, vehicle.id);
        uiKit.eyebrow(disclosure, "CONSUMO POR ABASTECIMENTOS");
        uiKit.label(disclosure, latest.explanation, 13, UiKit.MUTED, false);
        if (latest.kmPerLiter != null) {
            uiKit.button(disclosure, "Como foi calculado", false, () -> message("Distância: " + latest.distance + " km\nVolume: " + latest.liters + " L\n" + latest.explanation + "\nEntradas e revisões: " + String.join(", ", latest.entryRevisions)));
        }
        BigDecimal total = BigDecimal.ZERO;
        int count = 0;
        for (JournalEntry journalEntry : this.entries) {
            if (journalEntry.vehicleId.equals(vehicle.id) && !journalEntry.voided && journalEntry.cost != null) {
                total = total.add(journalEntry.cost);
                count++;
            }
        }
        uiKit.label(disclosure, count == 0 ? "Custos ainda não informados." : "Total informado: " + Format.money(total) + " · " + Format.count(count, "registro com valor", "registros com valor"), 14, UiKit.TEXT, false);
    }

    boolean isDetached(JournalEntry journalEntry) {
        return !this.host.vehicleIds().contains(journalEntry.vehicleId);
    }

    private static String day(String isoDate) {
        try {
            return java.time.LocalDate.parse(isoDate).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } catch (java.time.format.DateTimeParseException e) {
            return isoDate;
        }
    }

    private static String local(long occurredAt) {
        return Instant.ofEpochMilli(occurredAt).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("dd/MM/yyyy · HH:mm"));
    }

    private void open(JournalEntry.Kind kind, JournalEntry journalEntry, Bundle bundle) {
        String string;
        if (this.host.demo() || this.saving) {
            return;
        }
        VehicleProfile vehicle = this.host.vehicle();
        if (bundle != null) {
            string = bundle.getString("vehicle_id");
        } else if (journalEntry != null) {
            string = journalEntry.vehicleId;
        } else {
            string = vehicle == null ? null : vehicle.id;
        }
        if (vehicle == null || !vehicle.id.equals(string)) {
            message("O perfil deste rascunho não está selecionado. Nenhum registro foi salvo ou reatribuído.");
            return;
        }
        try {
            JournalEditor journalEditor = new JournalEditor(this.activity, vehicle, kind, journalEntry, bundle, new JournalEditor.Save() {
                @Override
                public final void save(JournalEntry journalEntry2, JournalEditor.Completion completion) {
                    JournalController.this.saveJournalEntry(journalEntry2, completion);
                }
            });
            this.editor = journalEditor;
            journalEditor.show();
        } catch (Exception e) {
            message("Não foi possível abrir o registro: " + e.getMessage());
        }
    }

    void saveJournalEntry(final JournalEntry journalEntry, final JournalEditor.Completion completion) {
        VehicleProfile vehicle = this.host.vehicle();
        if (!this.ready || this.saving || this.host.demo() || vehicle == null || !vehicle.id.equals(journalEntry.vehicleId)) {
            completion.complete("O perfil mudou ou o diário está ocupado. Reabra o registro.");
        } else {
            this.saving = true;
            IO.execute(() -> {
                try {
                    this.store.save(journalEntry);
                    final List<JournalEntry> latest = this.store.latest();
                    post(() -> {
                        this.saving = false;
                        this.entries = latest;
                        completion.complete(null);
                        this.host.changed();
                    });
                } catch (Exception e) {
                    post(() -> {
                        this.saving = false;
                        completion.complete("Não foi possível concluir: " + e.getMessage() + " Se houve gravação, reabra o diário antes de tentar de novo.");
                    });
                }
            });
        }
    }

    public void detail(final JournalEntry journalEntry) {
        UiKit uiKit = new UiKit(this.activity);
        LinearLayout column = uiKit.column();
        column.setPadding(uiKit.dp(18), uiKit.dp(12), uiKit.dp(18), uiKit.dp(12));
        for (String line : JournalJson.lines(journalEntry)) {
            if (!line.isEmpty()) {
                uiKit.label(column, line, 14, UiKit.TEXT, false);
            }
        }
        final AlertDialog alertDialog = new LocalizedDialog(this.activity).setTitle(journalEntry.title()).setPositiveButton("Fechar", null).create();
        uiKit.button(column, "Corrigir registro", false, () -> {
            alertDialog.dismiss();
            open(journalEntry.kind, journalEntry, null);
        }).setEnabled((journalEntry.voided || this.saving || this.host.demo() || this.host.vehicle() == null || !this.host.vehicle().id.equals(journalEntry.vehicleId)) ? false : true);
        uiKit.button(column, journalEntry.voided ? "Reativar registro" : "Anular registro", false, () -> {
            new LocalizedDialog(this.activity).setTitle(journalEntry.voided ? "Reativar registro?" : "Anular registro?").setMessage("Isso cria uma revisão e recalcula custos e consumo. As revisões anteriores continuam preservadas. Anule somente registros que não devem entrar nos cálculos.").setNegativeButton("Cancelar", null).setPositiveButton("Confirmar", (dialog, which) -> {
                if (this.saving || this.host.demo()) {
                    return;
                }
                this.saving = true;
                IO.execute(() -> {
                    try {
                        this.store.save(journalEntry.withVoided(!journalEntry.voided, System.currentTimeMillis()));
                        final List<JournalEntry> latest = this.store.latest();
                        post(() -> {
                            this.saving = false;
                            this.entries = latest;
                            alertDialog.dismiss();
                            this.host.changed();
                        });
                    } catch (Exception e) {
                        post(() -> {
                            this.saving = false;
                            message(e.getMessage());
                        });
                    }
                });
            }).show();
        }).setEnabled((this.saving || this.host.demo() || this.host.vehicle() == null || !this.host.vehicle().id.equals(journalEntry.vehicleId)) ? false : true);
        uiKit.button(column, "Ver revisões", false, () -> IO.execute(() -> {
            try {
                final List<JournalEntry> revisions = this.store.revisions(journalEntry.id);
                post(() -> {
                    int size = revisions.size();
                    String[] parts = new String[size];
                    for (int i = 0; i < size; i++) {
                        parts[i] = "Revisão " + ((JournalEntry) revisions.get(i)).revision + " · " + local(((JournalEntry) revisions.get(i)).revisedAt);
                    }
                    new LocalizedDialog(this.activity).setTitle("Revisões preservadas").setItems(parts, (dialog, which) -> message(String.join("\n\n", JournalJson.lines((JournalEntry) revisions.get(which))))).setNegativeButton("Fechar", null).show();
                });
            } catch (Exception e) {
                post(() -> message(e.getMessage()));
            }
        }));
        uiKit.button(column, "Exportar este registro", false, () -> exportEntries(Collections.singletonList(journalEntry)));
        ScrollView scrollView = new ScrollView(this.activity);
        scrollView.addView(column);
        alertDialog.setView(scrollView);
        alertDialog.show();
    }

    public void detached() {
        final ArrayList items = new ArrayList();
        Set<String> vehicleIds = this.host.vehicleIds();
        for (JournalEntry journalEntry : this.entries) {
            if (!vehicleIds.contains(journalEntry.vehicleId)) {
                items.add(journalEntry);
            }
        }
        int size = items.size();
        String[] parts = new String[size];
        for (int i = 0; i < size; i++) {
            parts[i] = ((JournalEntry) items.get(i)).vehicleName + " · " + ((JournalEntry) items.get(i)).title() + " · " + local(((JournalEntry) items.get(i)).occurredAt);
        }
        new LocalizedDialog(this.activity).setTitle("Perfis removidos · registros preservados").setItems(parts, (dialog, which) -> detail((JournalEntry) items.get(which))).setNegativeButton("Fechar", null).show();
    }

    public void exportVehicle(VehicleProfile vehicleProfile) {
        if (vehicleProfile == null) {
            return;
        }
        ArrayList items = new ArrayList();
        for (JournalEntry journalEntry : this.entries) {
            if (journalEntry.vehicleId.equals(vehicleProfile.id)) {
                items.add(journalEntry);
            }
        }
        exportEntries(items);
    }

    private void exportEntries(final List<JournalEntry> list) {
        if (this.saving) {
            message("Aguarde a operação em andamento.");
            return;
        }
        if (this.pendingExport != null) {
            message("Conclua ou cancele a exportação anterior.");
            return;
        }
        new LocalizedDialog(this.activity).setTitle("Revisar exportação").setMessage("O arquivo incluirá " + Format.count(list.size(), "registro", "registros") + " e suas revisões anteriores, com nomes, notas, datas, hodômetro, combustível e valores informados. Confira se deseja incluir esse conteúdo antes de escolher onde salvar. Não há envio automático pela internet.").setNegativeButton("Cancelar", null).setPositiveButton("Preparar arquivo", (dialog, which) -> prepareExport(list)).show();
    }

    private void prepareExport(final List<JournalEntry> list) {
        if (this.saving || this.pendingExport != null) {
            return;
        }
        this.saving = true;
        IO.execute(() -> {
            File file = null;
            try {
                JSONArray entries = new JSONArray();
                Iterator it = list.iterator();
                while (it.hasNext()) {
                    Iterator<JournalEntry> iterator = this.store.revisions(((JournalEntry) it.next()).id).iterator();
                    while (iterator.hasNext()) {
                        entries.put(JournalJson.encode(iterator.next()));
                    }
                }
                JSONObject json = new JSONObject().put("format", "borbacar-journal-export").put("schema_version", 1).put("exported_at", Instant.now().toString()).put("contains", "User declarations and all revisions of selected entries; not a complete app backup").put("entries", entries);
                file = File.createTempFile("diario-", ".json", this.activity.getCacheDir());
                final File exportFile = file;
                FileOutputStream fileOutputStream = new FileOutputStream(file);
                try {
                    fileOutputStream.write(json.toString(2).getBytes(StandardCharsets.UTF_8));
                    fileOutputStream.close();
                    this.main.post(() -> this.offerJournalExport(exportFile));
                } catch (Throwable throwable) {
                    try {
                        fileOutputStream.close();
                    } catch (Throwable throwable2) {
                        throwable.addSuppressed(throwable2);
                    }
                    throw throwable;
                }
            } catch (Exception e) {
                if (file != null) {
                    file.delete();
                }
                post(() -> {
                    this.saving = false;
                    message("Falha ao preparar exportação: " + e.getMessage());
                });
            }
        });
    }

    void offerJournalExport(File file) {
        if (this.closed || this.activity.isFinishing()) {
            file.delete();
            return;
        }
        this.saving = false;
        this.pendingExport = file;
        try {
            this.activity.startActivityForResult(new Intent("android.intent.action.CREATE_DOCUMENT").addCategory("android.intent.category.OPENABLE").setType("application/json").putExtra("android.intent.extra.TITLE", "borbacar-diario.json"), 503);
        } catch (Exception e) {
            this.pendingExport = null;
            file.delete();
            message("Não foi possível abrir o destino: " + e.getMessage());
        }
    }

    public void exportResult(int i, Intent intent) {
        final File file = this.pendingExport;
        this.pendingExport = null;
        if (file == null) {
            return;
        }
        if (i != -1 || intent == null || intent.getData() == null) {
            file.delete();
        } else {
            final Uri data = intent.getData();
            IO.execute(() -> this.writeJournalExport(file, data));
        }
    }

    void writeJournalExport(File file, Uri uri) {
        try {
            try {
                FileInputStream fileInputStream = new FileInputStream(file);
                try {
                    OutputStream outputStream = this.activity.getContentResolver().openOutputStream(uri);
                    try {
                        if (outputStream == null) {
                            throw new IOException("Destino indisponível");
                        }
                        byte[] bytes = new byte[8192];
                        while (true) {
                            int i = fileInputStream.read(bytes);
                            if (i == -1) {
                                break;
                            } else {
                                outputStream.write(bytes, 0, i);
                            }
                        }
                        post(() -> message("Diário exportado. O arquivo contém declarações e revisões; não é um backup completo do app."));
                        if (outputStream != null) {
                            outputStream.close();
                        }
                        fileInputStream.close();
                        file.delete();
                    } catch (Throwable throwable) {
                        if (outputStream != null) {
                            try {
                                outputStream.close();
                            } catch (Throwable throwable2) {
                                throwable.addSuppressed(throwable2);
                            }
                        }
                        throw throwable;
                    }
                } catch (Throwable throwable) {
                    try {
                        fileInputStream.close();
                    } catch (Throwable throwable3) {
                        throwable.addSuppressed(throwable3);
                    }
                    throw throwable;
                }
            } catch (Throwable throwable) {
                file.delete();
                throw throwable;
            }
        } catch (Exception e) {
            post(() -> message("Falha ao exportar: " + e.getMessage()));
        }
    }

    private void message(String message) {
        new LocalizedDialog(this.activity).setMessage(message).setPositiveButton("Fechar", null).show();
    }

    public Bundle state() throws Exception {
        Bundle bundle = new Bundle();
        bundle.putInt("filter", this.filter);
        bundle.putInt("limit", this.limit);
        JournalEditor journalEditor = this.editor;
        if (journalEditor != null && journalEditor.isShowing()) {
            bundle.putBundle("editor", this.editor.state());
        }
        File file = this.pendingExport;
        if (file != null) {
            bundle.putString("export", file.getAbsolutePath());
        }
        return bundle;
    }

    public void restoreEditor(Bundle bundle) {
        if (bundle == null || !bundle.containsKey("editor")) {
            return;
        }
        open(null, null, bundle.getBundle("editor"));
    }

    public void close() {
        this.closed = true;
        JournalEditor journalEditor = this.editor;
        if (journalEditor != null) {
            journalEditor.dismiss();
        }
        ExecutorService executorService = IO;
        final JournalStore journalStore = this.store;
        Objects.requireNonNull(journalStore);
        executorService.execute(() -> journalStore.close());
    }
}