package io.rea.obd;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Insets;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.InputFilter;
import android.text.InputType;
import android.util.SparseIntArray;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.WindowInsets;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import io.rea.obd.ai.AiSettings;
import io.rea.obd.ai.AnalysisClient;
import io.rea.obd.ai.AnalysisRequest;
import io.rea.obd.ai.AnalysisResult;
import io.rea.obd.application.GuidedReadController;
import io.rea.obd.diagnostics.CaptureComparison;
import io.rea.obd.diagnostics.DiagnosticCase;
import io.rea.obd.diagnostics.DiagnosticCaseBuilder;
import io.rea.obd.diagnostics.DtcDemoScenario;
import io.rea.obd.diagnostics.DtcFormatter;
import io.rea.obd.diagnostics.DtcObservation;
import io.rea.obd.diagnostics.DtcReadResult;
import io.rea.obd.diagnostics.ReadingGuide;
import io.rea.obd.diagnostics.SupplementalDemoScenario;
import io.rea.obd.diagnostics.SupplementalFormatter;
import io.rea.obd.diagnostics.SupplementalReadResult;
import io.rea.obd.history.DiagnosticCaseJson;
import io.rea.obd.history.DiagnosticCaseStore;
import io.rea.obd.history.DiagnosticHistoryStore;
import io.rea.obd.history.DtcReadJson;
import io.rea.obd.journal.ReminderReceiver;
import io.rea.obd.knowledge.DtcCatalog;
import io.rea.obd.knowledge.DtcDefinition;
import io.rea.obd.knowledge.DtcReference;
import io.rea.obd.knowledge.DtcStructure;
import io.rea.obd.knowledge.KnowledgeRepository;
import io.rea.obd.knowledge.SourceReference;
import io.rea.obd.model.DataOrigin;
import io.rea.obd.telemetry.DashboardState;
import io.rea.obd.telemetry.TelemetrySample;
import io.rea.obd.ui.ConnectionScreen;
import io.rea.obd.ui.DashboardScreen;
import io.rea.obd.ui.DiagnosisScreen;
import io.rea.obd.ui.Format;
import io.rea.obd.ui.GarageScreen;
import io.rea.obd.ui.HomeScreen;
import io.rea.obd.ui.I18n;
import io.rea.obd.ui.JournalController;
import io.rea.obd.ui.LocalizedDialog;
import io.rea.obd.ui.Motion;
import io.rea.obd.ui.NavigationState;
import io.rea.obd.ui.Notice;
import io.rea.obd.ui.ThemePalette;
import io.rea.obd.ui.UiKit;
import io.rea.obd.ui.VehicleEditor;
import io.rea.obd.ui.WallClock;
import io.rea.obd.vehicle.VehicleProfile;
import io.rea.obd.vehicle.VehicleProfileReportV1;
import io.rea.obd.vehicle.VehicleRecord;
import io.rea.obd.vehicle.VehicleRepository;
import io.rea.obd.vehicle.catalog.VehicleCatalog;
import io.rea.obd.vehicle.catalog.VehicleCatalogStore;
import io.rea.obd.vehicle.catalog.VehicleEfficiency;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public final class MainActivity extends Activity {
    private static final ExecutorService CASE_IO = Executors.newSingleThreadExecutor((runnable) -> {
        Thread thread = new Thread(runnable, "investigation-storage");
        thread.setDaemon(true);
        return thread;
    });
    private String acquisitionSessionId;
    private BluetoothAdapter adapter;
    private ScanCallback bleCallback;
    private BluetoothLeScanner bleScanner;
    private LinearLayout body;
    private boolean caseBusy;
    private boolean caseExportPreparing;
    private DiagnosticCaseStore caseStore;
    private ChartView chart;
    private volatile ElmSession connection;
    private DtcReadResult currentDtcRead;
    private VehicleRecord currentDtcRecord;
    private VehicleProfile currentDtcVehicle;
    private SupplementalReadResult currentSupplement;
    private DashboardScreen.Views dashboardViews;
    private VehicleEditor garageEditor;
    private int historySection;
    private DiagnosticHistoryStore historyStore;
    private JournalController journal;
    private File lastCsv;
    private LinearLayout nav;
    private File pendingCaseExport;
    private File pendingPdf;
    private TelemetryRecorder recorder;
    private String[] pendingAdapter;
    private int pollFailures;
    private TextView recordingBanner;
    private LinearLayout root;
    private DataOrigin historyOrigin;
    private TextView scanView;
    private TextView statusView;
    private VehicleCatalogStore.Loaded vehicleCatalog;
    private java.util.concurrent.Future<VehicleCatalogStore.Loaded> catalogLoad;
    private VehicleEfficiency efficiency;

    private VehicleCatalogStore.Loaded catalog(boolean wait) {
        java.util.concurrent.Future<VehicleCatalogStore.Loaded> load = this.catalogLoad;
        if (this.vehicleCatalog == null && load != null && (wait || load.isDone())) {
            try {
                this.vehicleCatalog = load.get();
            } catch (Exception e) {
                this.vehicleCatalog = null;
            }
            this.catalogLoad = null;
        }
        return this.vehicleCatalog;
    }

    private VehicleEfficiency.Official officialConsumption(VehicleProfile vehicle) {
        if (vehicle == null || catalog(false) == null) {
            return null;
        }
        if (this.efficiency == null) {
            try (java.io.InputStream input = getResources().openRawResource(R.raw.vehicle_efficiency)) {
                this.efficiency = VehicleEfficiency.read(input);
            } catch (Exception e) {
                return null;
            }
        }
        return this.efficiency.find(this.vehicleCatalog.catalog, vehicle);
    }
    private VehicleRepository vehicles;
    private DataOrigin voltageOrigin;
    private long voltageTime;
    private TextView voltageView;
    private int BG = UiKit.BG;
    private int CARD = UiKit.CARD;
    private int LINE = UiKit.LINE;
    private int TEXT = UiKit.TEXT;
    private int MUTED = UiKit.MUTED;
    private int ACCENT = UiKit.ACCENT;
    private int AMBER = UiKit.AMBER;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final ScheduledExecutorService watchdog = Executors.newSingleThreadScheduledExecutor();
    private final Map<Integer, TelemetrySample> samples = new HashMap();
    private final Map<Integer, TextView> qualityViews = new HashMap();
    private final Map<Integer, TextView> valueViews = new HashMap();
    private final Map<Integer, TextView> extraViews = new HashMap();
    private final Map<Integer, List<ChartView.Point>> histories = new HashMap();
    private final Map<Integer, Boolean> historyGaps = new HashMap();
    private int expandedPid = 12;
    private String currentDtcAdapter = "";
    private final Map<Integer, DiagnosticCase.Sample> contextualSamples = new HashMap();
    private List<DiagnosticCase.Sample> currentCaseSamples = Collections.emptyList();
    private final ExecutorService caseWorker = CASE_IO;
    private List<JSONObject> caseDocuments = Collections.emptyList();
    private String caseWarning = "";
    private final SparseIntArray scrollPositions = new SparseIntArray();
    private String historySessionId = "";
    private String recordingAcquisitionId = "";
    private final Set<Integer> selectedPids = new LinkedHashSet(Arrays.asList(12, 13, 5, 4, 17, 15));
    private boolean demo = false;
    private boolean busy = false;
    private boolean resumed = false;
    private boolean destroyed = false;
    private boolean polling = false;
    private int historyLimit = 8;
    private String guidedStatus = "";
    private int guidedCompleted = 0;
    private int page = 4;
    private int demoTick = 0;
    private int session = 0;
    private String status = "Nenhum adaptador conectado";
    private String scanSummary = "Inicie uma leitura para consultar os códigos OBD2.";
    private String deviceName = "";
    private String activeTransportDescription = "";
    private String voltage = "—";
    private JSONArray history = new JSONArray();
    private JSONArray detachedHistory = new JSONArray();
    private String storageWarning = "";
    private final ObdReader.Clock clock = new ObdReader.Clock() {
        @Override
        public long elapsedMillis() {
            return SystemClock.elapsedRealtime();
        }

        @Override
        public long epochMillis() {
            return WallClock.millis();
        }
    };
    private final Runnable pollTask = () -> this.poll();
    private final Runnable freshnessTask = new Runnable() {
        @Override
        public void run() {
            if (!MainActivity.this.resumed || MainActivity.this.destroyed) {
                return;
            }
            MainActivity.this.refreshValues();
            MainActivity.this.ui.postDelayed(this, 1000L);
        }
    };

    interface ReadAction {
        String run(ObdReader obdReader) throws Exception;
    }

    interface SessionAction {
        String run(ElmSession elmSession) throws Exception;
    }

    @Override
    public void onCreate(Bundle bundle) {
        I18n.init(this);
        ThemePalette themePalette = ThemePalette.of(this);
        setTheme(themePalette.style());
        this.BG = themePalette.bg;
        this.CARD = themePalette.card;
        this.LINE = themePalette.line;
        this.TEXT = themePalette.text;
        this.MUTED = themePalette.muted;
        this.ACCENT = themePalette.accent;
        this.AMBER = themePalette.attention;
        super.onCreate(bundle);
        io.rea.obd.backup.BackupRepository recovery = new io.rea.obd.backup.BackupRepository(this, getPreferences(0));
        if (recovery.hasPendingRestore()) {
            recoveryBlocked = true;
            android.widget.TextView warning = new android.widget.TextView(this);
            warning.setPadding(32, 48, 32, 32);
            warning.setText(I18n.english() ? "Recovering interrupted restore…" : "Recuperando restauração interrompida…");
            setContentView(warning);
            new Thread(() -> {
                try {
                    recovery.recoverInterruptedRestore();
                    runOnUiThread(() -> { if (!isDestroyed()) recreate(); });
                } catch (Exception failure) {
                    runOnUiThread(() -> {
                        if (!isDestroyed()) warning.setText(I18n.english()
                            ? "Storage recovery could not finish. Free some space and restart. The recovery copy was preserved."
                            : "A recuperação não pôde terminar. Libere espaço e reinicie. A cópia de recuperação foi preservada.");
                    });
                }
            }, "backup-recovery").start();
            return;
        }
        themePalette.window(getWindow());
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(BluetoothManager.class);
        this.adapter = bluetoothManager == null ? null : bluetoothManager.getAdapter();
        try {
            this.vehicles = new VehicleRepository(getPreferences(0));
            if (ReminderReceiver.enabled(this)) {
                ReminderReceiver.schedule(this);
            }
        } catch (Exception e) {
            this.storageWarning = "Garagem indisponível: " + error(e) + ". Os dados originais foram preservados.";
        }
        java.util.concurrent.FutureTask<VehicleCatalogStore.Loaded> catalogLoad = new java.util.concurrent.FutureTask<>(() -> {
            VehicleCatalogStore.Loaded loaded = new VehicleCatalogStore(this).load();
            this.ui.post(() -> {
                if (!this.destroyed && this.page == 4) {
                    render();
                }
            });
            return loaded;
        });
        this.catalogLoad = catalogLoad;
        new Thread(catalogLoad, "vehicle-catalog").start();
        try {
            this.historyStore = new DiagnosticHistoryStore(getPreferences(0));
            refreshHistory();
        } catch (Exception exc) {
            this.storageWarning += " Histórico indisponível: " + error(exc) + ". Os dados originais foram preservados.";
        }
        this.caseStore = new DiagnosticCaseStore(this);
        refreshCases();
        String string = getPreferences(0).getString("selected_pids", "");
        if (!string.isEmpty()) {
            LinkedHashSet seen = new LinkedHashSet();
            for (String text : string.split(",")) {
                try {
                    int i = Integer.parseInt(text);
                    if (SensorCatalog.get(i) != null) {
                        seen.add(Integer.valueOf(i));
                    }
                } catch (NumberFormatException unused2) {
                }
            }
            if (!seen.isEmpty() && seen.size() <= 6) {
                this.selectedPids.clear();
                this.selectedPids.addAll(seen);
            }
        }
        try {
            File externalFilesDir = getExternalFilesDir(null);
            String lastCsvPath = getPreferences(0).getString("last_csv_path", "");
            if (externalFilesDir != null && !lastCsvPath.isEmpty()) {
                File file = new File(lastCsvPath);
                if (file.getCanonicalPath().startsWith(new File(externalFilesDir, "registros").getCanonicalPath() + File.separator) && file.isFile()) {
                    this.lastCsv = file;
                }
            }
        } catch (IOException unused3) {
        }
        if (bundle != null) {
            this.historyLimit = Math.max(8, Math.min(10000, bundle.getInt("history_limit", 8)));
            this.page = NavigationState.restore(bundle.getInt("page", 4));
            String caseExport = bundle.getString("case_export", "");
            if (!caseExport.isEmpty()) {
                try {
                    File file2 = new File(caseExport);
                    if (file2.getCanonicalFile().getParentFile().equals(getCacheDir().getCanonicalFile()) && file2.getName().startsWith("investigacao-") && file2.isFile()) {
                        this.pendingCaseExport = file2;
                    }
                } catch (IOException unused4) {
                }
            }
        }
        this.historySection = bundle != null ? Math.max(0, Math.min(1, bundle.getInt("history_section"))) : 0;
        this.journal = new JournalController(this, new JournalController.Host() {
            @Override
            public void changed() {
                if (!MainActivity.this.destroyed && (MainActivity.this.page == 6 || MainActivity.this.page == 4)) {
                    MainActivity.this.render();
                }
            }

            @Override
            public boolean demo() {
                return MainActivity.this.demo;
            }

            @Override
            public VehicleProfile vehicle() {
                return MainActivity.this.activeVehicle();
            }

            @Override
            public void offerReminders() {
                if (MainActivity.this.destroyed || ReminderReceiver.enabled(MainActivity.this)) {
                    return;
                }
                new LocalizedDialog(MainActivity.this).setTitle("Ligar lembretes?").setMessage("O app avisa quando faltarem 7 dias ou 500 km para o próximo serviço. Você pode mudar isso em Ajustes.").setPositiveButton("Ligar lembretes", (dialog, which) -> MainActivity.this.toggleReminders()).setNegativeButton("Agora não", null).show();
            }

            @Override
            public Set<String> vehicleIds() {
                HashSet seen = new HashSet();
                if (MainActivity.this.vehicles != null) {
                    Iterator<VehicleProfile> it = MainActivity.this.vehicles.all().iterator();
                    while (it.hasNext()) {
                        seen.add(it.next().id);
                    }
                }
                return seen;
            }
        }, bundle == null ? null : bundle.getBundle("journal"));
        render();
        if (bundle != null && bundle.containsKey("garage_editor")) {
            openGarageEditor(null, bundle.getBundle("garage_editor"));
        }
        if (bundle != null) {
            this.journal.restoreEditor(bundle.getBundle("journal"));
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putInt("page", this.page);
        bundle.putInt("history_limit", this.historyLimit);
        bundle.putInt("history_section", this.historySection);
        JournalController journalController = this.journal;
        if (journalController != null) {
            try {
                bundle.putBundle("journal", journalController.state());
            } catch (Exception e) {
                message("Não foi possível preservar o rascunho do diário: " + error(e));
            }
        }
        VehicleEditor vehicleEditor = this.garageEditor;
        if (vehicleEditor != null && vehicleEditor.isShowing()) {
            try {
                bundle.putBundle("garage_editor", this.garageEditor.state());
            } catch (Exception exc) {
                message("Não foi possível preservar o rascunho: " + error(exc));
            }
        }
        File file = this.pendingCaseExport;
        if (file != null) {
            bundle.putString("case_export", file.getAbsolutePath());
        }
    }

    private boolean recoveryBlocked;

    @Override
    protected void onResume() {
        super.onResume();
        if (recoveryBlocked) return;
        this.resumed = true;
        refreshValues();
        schedulePoll(200L);
        this.ui.post(this.freshnessTask);
    }

    @Override
    protected void onPause() {
        if (recoveryBlocked) { super.onPause(); return; }
        this.resumed = false;
        refreshValues();
        stopBleScan();
        this.ui.removeCallbacks(this.pollTask);
        this.ui.removeCallbacks(this.freshnessTask);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        this.destroyed = true;
        if (aiAssistant != null) aiAssistant.close();
        JournalController journalController = this.journal;
        if (journalController != null) {
            journalController.close();
        }
        VehicleEditor vehicleEditor = this.garageEditor;
        if (vehicleEditor != null) {
            vehicleEditor.dismiss();
        }
        this.session++;
        this.ui.removeCallbacksAndMessages(null);
        stopBleScan();
        if (this.connection != null) {
            this.connection.close();
        }
        closeRecorder();
        this.worker.shutdownNow();
        this.watchdog.shutdownNow();
        final DiagnosticCaseStore diagnosticCaseStore = this.caseStore;
        if (diagnosticCaseStore != null) {
            ExecutorService executorService = this.caseWorker;
            Objects.requireNonNull(diagnosticCaseStore);
            executorService.execute(() -> diagnosticCaseStore.close());
        }
        super.onDestroy();
    }

    public boolean connected() {
        ElmSession elmSession = this.connection;
        return elmSession != null && elmSession.isConnected();
    }

    private String appVersion() {
        try {
            String versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            return versionName == null ? "desenvolvimento" : versionName;
        } catch (PackageManager.NameNotFoundException ignored) {
            return "desconhecida";
        }
    }

    public VehicleProfile activeVehicle() {
        VehicleRepository vehicleRepository = this.vehicles;
        if (vehicleRepository == null) {
            return null;
        }
        return vehicleRepository.active();
    }

    public boolean profileLocked() {
        return connected() || this.demo || this.busy || this.recorder != null;
    }

    public void refreshHistory() throws JSONException {
        VehicleProfile activeVehicle = activeVehicle();
        this.history = this.historyStore.forVehicle(activeVehicle == null ? null : activeVehicle.id);
        HashSet seen = new HashSet();
        VehicleRepository vehicleRepository = this.vehicles;
        if (vehicleRepository != null) {
            Iterator<VehicleProfile> it = vehicleRepository.all().iterator();
            while (it.hasNext()) {
                seen.add(it.next().id);
            }
        }
        this.detachedHistory = this.historyStore.detached(seen);
    }

    private int dp(float value) {
        return (int) ((value * getResources().getDisplayMetrics().density) + 0.5f);
    }

    private GradientDrawable background(int i, int value) {
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setColor(i);
        gradientDrawable.setCornerRadius(dp(value));
        return gradientDrawable;
    }

    private TextView text(String text, int i, int textColor, boolean flag) {
        TextView textView = new TextView(this);
        textView.setText(I18n.t(text));
        textView.setTextSize(i);
        textView.setTextColor(textColor);
        textView.setFontFeatureSettings("tnum");
        textView.setLineSpacing(dp(2.0f), 1.0f);
        textView.setTypeface(Typeface.create(flag ? "sans-serif-medium" : "sans-serif", Typeface.NORMAL));
        return textView;
    }

    private void gap(LinearLayout linearLayout, int i) {
        linearLayout.addView(new View(this), new LinearLayout.LayoutParams(1, dp(i)));
    }

    private TextView label(LinearLayout linearLayout, String text, int i, int value, boolean flag) {
        TextView label = text(text, i, value, flag);
        linearLayout.addView(label);
        return label;
    }

    private Button button(LinearLayout linearLayout, String text, boolean flag, Runnable runnable) {
        return new UiKit(this).button(linearLayout, text, flag, runnable);
    }

    private LinearLayout card(LinearLayout linearLayout) {
        return new UiKit(this).card(linearLayout, false);
    }

    private void heading(String category, String text, String name) {
        UiKit uiKit = new UiKit(this);
        uiKit.eyebrow(this.body, category);
        uiKit.title(this.body, text, 28);
        uiKit.label(this.body, name, 14, this.MUTED, false);
        gap(this.body, 12);
    }

    public void render() {
        String text;
        int i;
        int value;
        int value2;
        int value3;
        this.valueViews.clear();
        this.extraViews.clear();
        this.qualityViews.clear();
        this.voltageView = null;
        this.scanView = null;
        this.chart = null;
        this.dashboardViews = null;
        this.recordingBanner = null;
        LinearLayout linearLayout = new LinearLayout(this);
        this.root = linearLayout;
        boolean focusable = true;
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        this.root.setBackgroundColor(this.BG);
        setContentView(this.root);
        if (Build.VERSION.SDK_INT >= 30) {
            this.root.setOnApplyWindowInsetsListener((view, windowInsets) -> {
                Insets insets = windowInsets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                view.setPadding(insets.left, insets.top, insets.right, insets.bottom);
                return windowInsets;
            });
        } else {
            this.root.setOnApplyWindowInsetsListener((view, windowInsets) -> {
                view.setPadding(windowInsets.getSystemWindowInsetLeft(), windowInsets.getSystemWindowInsetTop(), windowInsets.getSystemWindowInsetRight(), windowInsets.getSystemWindowInsetBottom());
                return windowInsets;
            });
        }
        this.root.requestApplyInsets();
        LinearLayout row = new LinearLayout(this);
        boolean compactHeader = getResources().getConfiguration().screenWidthDp < 360 || getResources().getConfiguration().fontScale > 1.3f;
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(22.0f), dp(compactHeader ? 8.0f : 16.0f), dp(22.0f), dp(compactHeader ? 4.0f : 12.0f));
        this.root.addView(row);
        TextView label = text(compactHeader ? "" : "BorbaCar Labs", 18, this.TEXT, true);
        Drawable drawable = getDrawable(R.drawable.ic_brand);
        drawable.setBounds(0, 0, dp(30.0f), dp(30.0f));
        label.setCompoundDrawablesRelative(drawable, null, null, null);
        label.setCompoundDrawablePadding(dp(compactHeader ? 0.0f : 10.0f));
        if (compactHeader) {
            label.setContentDescription("BorbaCar Labs");
        }
        row.addView(label, compactHeader ? new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT) : new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        if (this.demo) {
            text = "DEMONSTRAÇÃO";
        } else {
            text = connected() ? "● CONECTADO" : "○ DESCONECTADO";
        }
        if (this.demo) {
            i = this.AMBER;
        } else {
            i = connected() ? this.ACCENT : this.MUTED;
        }
        TextView connection = text(text, 11, i, true);
        connection.setContentDescription(I18n.t(this.demo ? "Modo demonstração" : connected() ? "Conectado ao adaptador" : "Sem conexão com o adaptador"));
        if (compactHeader) {
            connection.setPadding(dp(12.0f), 0, 0, 0);
            row.addView(connection, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        } else {
            row.addView(connection);
        }
        TextView ajustes = text("Ajustes", 13, this.ACCENT, true);
        ajustes.setMinHeight(dp(48.0f));
        ajustes.setGravity(Gravity.CENTER);
        ajustes.setPadding(dp(12.0f), 0, 0, 0);
        ajustes.setContentDescription(I18n.t("Ajustes e informações"));
        row.addView(ajustes);
        ajustes.setOnClickListener((view) -> navigate(3));
        if (this.demo) {
            String banner = this.page == 6 ? "MODO DEMONSTRAÇÃO · diário contém registros declarados" : "DADOS SIMULADOS · nenhum veículo está sendo consultado";
            TextView label2 = text(compactHeader ? banner.substring(0, banner.indexOf(" · ")) : banner, 13, this.AMBER, true);
            if (compactHeader) {
                label2.setContentDescription(I18n.t(banner));
            }
            label2.setPadding(dp(22.0f), dp(compactHeader ? 0.0f : 4.0f), dp(22.0f), dp(compactHeader ? 6.0f : 10.0f));
            this.root.addView(label2);
        }
        if (this.recorder != null) {
            TextView recordingBanner = text(dashboardState().recording, 12, this.AMBER, true);
            this.recordingBanner = recordingBanner;
            recordingBanner.setPadding(dp(22.0f), dp(4.0f), dp(22.0f), dp(10.0f));
            this.root.addView(this.recordingBanner);
        }
        final ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        this.root.addView(scrollView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));
        LinearLayout row2 = new LinearLayout(this);
        this.body = row2;
        row2.setOrientation(LinearLayout.VERTICAL);
        this.body.setPadding(dp(22.0f), dp(12.0f), dp(22.0f), dp(24.0f));
        scrollView.addView(this.body);
        scrollView.setVerticalScrollBarEnabled(false);
        final int page = this.page;
        final int scrollPositions = this.scrollPositions.get(page, 0);
        scrollView.setOnScrollChangeListener((view, i7, i8, i9, i10) -> this.scrollPositions.put(page, i8));
        scrollView.post(() -> scrollView.scrollTo(0, scrollPositions));
        if (this.storageWarning.isEmpty()) {
            value = -2;
            value2 = 13;
            value3 = 6;
        } else {
            value3 = 6;
            value2 = 13;
            value = -2;
            label(card(this.body), this.storageWarning, 13, this.AMBER, false);
        }
        int page2 = this.page;
        if (page2 == 0) {
            dashboard();
        } else if (page2 == 1) {
            diagnostics();
        } else if (page2 == 2) {
            garage();
        } else if (page2 == 3) {
            more();
        } else if (page2 == 5) {
            historyScreen();
        } else if (page2 == value3) {
            historyTabs(2);
            this.journal.render(this.body);
        } else {
            home();
        }
        this.nav = new LinearLayout(this);
        boolean stacked = getResources().getConfiguration().fontScale > 1.3f;
        this.nav.setOrientation(stacked ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
        this.nav.setPadding(dp(12.0f), dp(stacked ? 4.0f : 10.0f), dp(12.0f), dp(stacked ? 4.0f : 10.0f));
        this.nav.setBackgroundColor(this.CARD);
        this.root.addView(this.nav);
        String[] parts = {"Hoje", "Diagnóstico", "Histórico", "Garagem"};
        int[] values = {R.drawable.ic_home, R.drawable.ic_diagnosis, R.drawable.ic_history, R.drawable.ic_garage};
        LinearLayout nav = this.nav;
        int value4 = 0;
        while (value4 < 4) {
            final int tabIndex = value4;
            if (stacked && value4 % 2 == 0) {
                nav = new LinearLayout(this);
                this.nav.addView(nav, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, value));
            }
            TextView label2 = text(parts[value4], value2, NavigationState.tab(this.page) == value4 ? this.ACCENT : this.MUTED, NavigationState.tab(this.page) == value4 ? focusable : false);
            label2.setGravity(Gravity.CENTER);
            label2.setPadding(dp(stacked ? 10.0f : 2.0f), dp(stacked ? 4.0f : 10.0f), dp(stacked ? 10.0f : 2.0f), dp(stacked ? 4.0f : 10.0f));
            label2.setBackground(ThemePalette.of(this).navigation(this, NavigationState.tab(this.page) == value4 ? focusable : false));
            Motion.press(label2);
            label2.setMinHeight(dp(48.0f));
            label2.setContentDescription(I18n.t(parts[value4]));
            label2.setSelected(NavigationState.tab(this.page) == value4 ? focusable : false);
            label2.setFocusable(focusable);
            Drawable mutate = getDrawable(values[value4]).mutate();
            mutate.setTint(NavigationState.tab(this.page) == value4 ? this.ACCENT : this.MUTED);
            mutate.setBounds(0, 0, dp(22.0f), dp(22.0f));
            if (!stacked) {
                label2.setCompoundDrawables(null, mutate, null, null);
            }
            label2.setCompoundDrawablePadding(dp(4.0f));
            nav.addView(label2, new LinearLayout.LayoutParams(0, value, 1.0f));
            label2.setOnClickListener((view) -> navigate(NavigationState.route(tabIndex)));
            value4++;
            focusable = true;
            value2 = 13;
        }
        I18n.apply(this.root);
        if (this.page == 0 && (connected() || this.demo || this.recorder != null)) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    public void navigate(int i) {
        int restore = NavigationState.restore(i);
        if (restore == this.page) {
            return;
        }
        this.page = restore;
        render();
        Motion.enter(this.body);
        schedulePoll(100L);
    }

    @Override
    public void onBackPressed() {
        int i = this.page;
        if (i != 4) {
            navigate(NavigationState.back(i));
        } else {
            super.onBackPressed();
        }
    }

    private void home() {
        String string;
        VehicleProfile activeVehicle = activeVehicle();
        Iterator<JSONObject> it = this.caseDocuments.iterator();
        int i = 0;
        while (it.hasNext()) {
            JSONObject input = it.next().optJSONObject("input");
            JSONObject vehicle = input == null ? null : input.optJSONObject("vehicle");
            if (activeVehicle != null && vehicle != null && activeVehicle.id.equals(vehicle.optString("id"))) {
                i++;
            }
        }
        JSONObject json = this.history.optJSONObject(0);
        if (json == null) {
            string = "";
        } else {
            StringBuilder builder = new StringBuilder();
            builder.append(json.optString("date", "Horário não informado"));
            builder.append(json.optBoolean("demo") ? " · SIMULADO" : "");
            string = builder.toString();
        }
        DtcReadResult lastRead = null;
        if (json != null && json.optJSONObject("dtc_read") != null) {
            try {
                lastRead = DtcReadJson.decode(json.getJSONObject("dtc_read"));
            } catch (JSONException e) {
                lastRead = null;
            }
        }
        HomeScreen.render(this.body, activeVehicle, this.status, string, this.history.length(), i, this.demo, this.busy || connected(), connected(), lastRead, this.journal == null ? null : this.journal.glance(activeVehicle), officialConsumption(activeVehicle), new HomeScreen.Actions() {
            @Override
            public void add() {
                MainActivity.this.editVehicle(null);
            }

            @Override
            public void connect() {
                MainActivity.this.navigate(1);
                MainActivity.this.chooseTransport();
            }

            @Override
            public void history() {
                MainActivity.this.navigate(NavigationState.HISTORY);
            }

            @Override
            public void journal() {
                MainActivity.this.navigate(NavigationState.JOURNAL);
            }

            @Override
            public void fuel() {
                MainActivity.this.navigate(NavigationState.JOURNAL);
                if (MainActivity.this.journal != null) {
                    MainActivity.this.journal.add(io.rea.obd.journal.JournalEntry.Kind.FUEL);
                }
            }
        });
        if (this.caseWarning.isEmpty()) {
            return;
        }
        label(this.body, this.caseWarning, 13, this.AMBER, false);
    }

    private DashboardState dashboardState() {
        return new DashboardState(this.demo, connected(), this.busy, this.resumed, this.page, this.selectedPids, this.samples, this.clock.elapsedMillis(), this.recorder != null);
    }

    private void dashboard() {
        DashboardState dashboardState = dashboardState();
        LinearLayout linearLayout = this.body;
        VehicleProfile activeVehicle = activeVehicle();
        String status = this.status;
        boolean busy = this.busy;
        File file = this.lastCsv;
        boolean enabled = file != null && file.exists();
        File lastCsv = this.lastCsv;
        DashboardScreen.Views dashboardViews = DashboardScreen.render(linearLayout, activeVehicle, dashboardState, expandedPid(), status, busy, enabled, lastCsv == null ? "" : lastCsv.getName(), new DashboardScreen.Actions() {
            @Override
            public void connection() {
                if (MainActivity.this.busy) {
                    return;
                }
                if (MainActivity.this.connected() || MainActivity.this.demo) {
                    MainActivity.this.disconnect();
                } else {
                    MainActivity.this.chooseTransport();
                }
            }

            @Override
            public void demo() {
                MainActivity.this.startDemo();
            }

            @Override
            public void garage() {
                MainActivity.this.page = 2;
                MainActivity.this.render();
                MainActivity.this.schedulePoll(100L);
            }

            @Override
            public void expand(int pid) {
                MainActivity.this.expandedPid = pid;
                MainActivity.this.render();
            }

            @Override
            public void export() {
                MainActivity.this.exportCsv();
            }

            @Override
            public void recording() {
                MainActivity.this.toggleRecording();
            }

            @Override
            public void sensors() {
                MainActivity.this.chooseSensors();
            }
        });
        this.dashboardViews = dashboardViews;
        this.chart = dashboardViews.chart;
        this.voltageView = this.dashboardViews.voltage;
        this.valueViews.putAll(this.dashboardViews.values);
        this.qualityViews.putAll(this.dashboardViews.qualities);
        refreshValues();
    }

    private void diagnostics() {
        heading("SCANNER OBD", "Diagnóstico", activeVehicle() == null ? "Selecione um veículo na Garagem." : activeVehicle().name);
        UiKit uiKit = new UiKit(this);
        DtcReadResult dtcReadResult = this.currentDtcRead;
        if (dtcReadResult != null && !this.busy) {
            readingResult(dtcReadResult);
            LinearLayout connection = uiKit.disclosure(this.body, "Conexão e nova leitura");
            label(connection, (connected() || this.demo) ? "Scanner pronto" : "Conecte seu scanner", 17, this.TEXT, true);
            connectionControls(uiKit, connection);
        } else {
            LinearLayout feature = uiKit.feature(this.body);
            uiKit.eyebrow(feature, this.demo ? "EXPERIÊNCIA DE DEMONSTRAÇÃO" : connected() ? "CONEXÃO ATIVA" : "COMECE POR UMA CONEXÃO");
            uiKit.label(feature, (connected() || this.demo) ? "Scanner pronto" : "Conecte seu scanner", 23, this.TEXT, true);
            connectionControls(uiKit, feature);
            if (dtcReadResult != null) {
                readingResult(dtcReadResult);
            }
        }
        diagnosticsDetails(uiKit);
    }

    private void connectionControls(UiKit uiKit, LinearLayout feature) {
        uiKit.label(feature, this.status, 14, this.MUTED, false);
        if (!this.demo && !connected()) {
            uiKit.label(feature, "O app consulta o adaptador e a ECU. Não é necessário conhecer o protocolo do carro.", 14, this.MUTED, false);
        }
        button(feature, this.busy ? "Operação em andamento…" : (this.demo || connected()) ? "Desconectar" : "Conectar adaptador", (this.demo || connected()) ? false : true, () -> {
            if (this.demo || connected()) {
                disconnect();
            } else {
                chooseTransport();
            }
        }).setEnabled(!this.busy);
        if (!this.demo && !connected()) {
            button(feature, "Explorar demonstração", false, () -> this.startDemo()).setEnabled(!this.busy);
        }
        if (this.demo || connected()) {
            button(feature, "Iniciar leitura guiada", true, () -> this.confirmGuidedRead()).setEnabled((this.busy || this.caseBusy) ? false : true);
            button(feature, this.busy ? "Leitura em andamento…" : "Ler só os códigos de falha", false, () -> this.scan()).setEnabled(!this.busy);
            label(feature, "A leitura guiada também coleta a prontidão dos monitores e o quadro congelado.", 12, this.MUTED, false);
        }
        if (!this.guidedStatus.isEmpty()) {
            DiagnosisScreen.progress(feature, this.guidedCompleted, this.guidedStatus);
        }
    }

    private void readingResult(DtcReadResult dtcReadResult) {
        LinearLayout verdict = DiagnosisScreen.verdict(this.body, dtcReadResult, false);
        boolean hasCodes = !dtcReadResult.observations().isEmpty();
        if (this.currentDtcVehicle != null && dtcReadResult == this.currentDtcRead) {
            button(verdict, "Conversar com a IA", true, this::talkAboutCurrentReading)
                    .setEnabled(!this.busy && !this.caseBusy);
        }
        if (hasCodes) {
            button(verdict, "Entender esta leitura", false, () -> understandReading(dtcReadResult)).setTag("action-understand");
            button(verdict, "Preparar resumo para oficina", false, () -> workshopSummary(dtcReadResult)).setTag("action-workshop");
        }
        if (this.currentDtcVehicle != null) {
            LinearLayout investigation = new UiKit(this).disclosure(verdict, I18n.english() ? "Add symptoms and investigate" : "Adicionar sintomas e investigar");
            button(investigation, "Investigar esta leitura", false, () -> {
                try {
                    editCase(capturedCase(), true);
                } catch (Exception e) {
                    message(error(e));
                }
            }).setEnabled((this.caseBusy || this.busy) ? false : true);
        }
        for (final DtcObservation dtcObservation : dtcReadResult.observations()) {
            LinearLayout card = card(this.body);
            label(card, dtcObservation.code, 26, this.AMBER, true);
            StringBuilder builder = new StringBuilder();
            builder.append(DtcFormatter.category(dtcObservation.category));
            builder.append(dtcReadResult.origin == DataOrigin.SIMULATED ? " · exemplo simulado" : "");
            label(card, builder.toString(), 13, this.MUTED, false);
            String structure = DtcStructure.describe(dtcObservation.code);
            if (structure != null) {
                label(card, structure, 13, this.ACCENT, true);
            }
            DtcDefinition dtcDefinition = DtcReference.find(dtcObservation.code);
            label(card, dtcDefinition == null ? "Sem descrição detalhada no catálogo local." : dtcDefinition.meaning, 14, dtcDefinition == null ? this.MUTED : this.TEXT, false);
            io.rea.obd.knowledge.DtcGuidance guidance = io.rea.obd.knowledge.DtcGuidance.of(dtcObservation.code);
            label(card, guidance.headline(), 14, guidance.urgent() ? this.AMBER : this.ACCENT, true).setTag("guidance-" + dtcObservation.code);
            button(card, "Consultar " + dtcObservation.code, false, () -> showDtcDefinition(dtcObservation.code));
        }
    }

    void understandReading(DtcReadResult dtcReadResult) {
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(18.0f), dp(10.0f), dp(18.0f), dp(16.0f));
        DiagnosisScreen.explanation(content, dtcReadResult, this::showDtcDefinition);
        ScrollView scrollView = new ScrollView(this);
        scrollView.setBackgroundColor(this.CARD);
        scrollView.addView(content);
        new LocalizedDialog(this).setTitle("Entender esta leitura").setView(scrollView).setNeutralButton("Resumo para oficina", (dialog, which) -> workshopSummary(dtcReadResult)).setPositiveButton("Fechar", null).show();
    }

    void workshopSummary(DtcReadResult dtcReadResult) {
        String vehicle = this.currentDtcVehicle == null ? "" : this.currentDtcVehicle.name + " · " + this.currentDtcVehicle.description();
        String adapter = this.currentDtcAdapter;
        final List<String> lines = ReportText.readable(ReadingGuide.workshopSummary(dtcReadResult, vehicle, adapter, dtcReadResult == this.currentDtcRead ? this.currentSupplement : null), WallClock.zone());
        final String text = String.join("\n", lines);
        new LocalizedDialog(this).setTitle("Resumo para oficina").setMessage(text).setPositiveButton("Compartilhar", (dialog, which) -> shareWorkshopSummary(text)).setNeutralButton("Salvar PDF", (dialog, which) -> saveWorkshopPdf(lines)).setNegativeButton("Fechar", null).show();
    }

    private void shareWorkshopSummary(String text) {
        Intent send = new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_SUBJECT, I18n.t("Resumo para oficina · BorbaCar Labs")).putExtra(Intent.EXTRA_TEXT, text);
        startActivity(Intent.createChooser(send, I18n.t("Compartilhar resumo")));
    }

    private void saveWorkshopPdf(List<String> lines) {
        try {
            File file = new File(getCacheDir(), "resumo-oficina.pdf");
            ReportWriter.pdf(file, lines);
            this.pendingPdf = file;
            Intent intent = new Intent("android.intent.action.CREATE_DOCUMENT");
            intent.addCategory("android.intent.category.OPENABLE");
            intent.setType("application/pdf");
            intent.putExtra("android.intent.extra.TITLE", "resumo-oficina.pdf");
            startActivityForResult(intent, 500);
        } catch (Exception e) {
            message(error(e));
        }
    }

    private void diagnosticsDetails(UiKit uiKit) {
        if (this.currentSupplement != null) {
            LinearLayout card = card(this.body);
            label(card, this.currentSupplement.incomplete() ? "COMPLEMENTO INCOMPLETO" : "COMPLEMENTO COLETADO", 11, this.AMBER, true);
            label(card, "Prontidão e quadro 00 têm estados e horários próprios. Salve uma investigação para preservar este complemento.", 13, this.MUTED, false);
            label(card, DiagnosisScreen.READINESS_HINT + "\n" + DiagnosisScreen.FREEZE_FRAME_HINT, 12, this.MUTED, false);
            button(card, "Ver prontidão e quadro coletados", false, () -> message(String.join("\n", SupplementalFormatter.lines(this.currentSupplement))));
        }
        uiKit.row(this.body, "Ao vivo", () -> navigate(0));
        LinearLayout disclosure = uiKit.disclosure(this.body, "Detalhes técnicos da leitura");
        this.scanView = label(disclosure, this.scanSummary, 15, this.TEXT, false);
        if (this.currentDtcRead != null) {
            DiagnosisScreen.results(disclosure, this.currentDtcRead);
        }
        if (this.currentDtcRead != null && this.currentDtcVehicle != null) {
            label(disclosure, "Opcional: colete prontidão e quadro antes de criar a investigação.", 12, this.MUTED, false);
            button(disclosure, "Ler dados complementares", false, () -> this.readSupplement()).setEnabled((this.busy || this.caseBusy || !sameAcquisition()) ? false : true);
        }
        LinearLayout disclosure2 = uiKit.disclosure(this.body, "Ferramentas do scanner");
        label(disclosure2, "ABS, airbags e módulos específicos não têm cobertura comprovada nesta versão.", 13, this.MUTED, false);
        button(disclosure2, "Prontidão dos monitores", false, () -> this.readReadiness()).setEnabled(connected() || this.demo);
        label(disclosure2, DiagnosisScreen.READINESS_HINT, 12, this.MUTED, false);
        button(disclosure2, "VIN e calibração", false, () -> this.readVehicleInfo()).setEnabled(connected() || this.demo);
        button(disclosure2, "Quadro congelado", false, () -> this.readFreezeFrame()).setEnabled(connected() || this.demo);
        label(disclosure2, DiagnosisScreen.FREEZE_FRAME_HINT, 12, this.MUTED, false);
        button(disclosure2, "Apagar falhas e monitores", false, () -> this.confirmClear()).setEnabled(connected() && !this.busy);
    }

    private void historyScreen() {
        heading("ACOMPANHAMENTO", "Histórico", activeVehicle() == null ? "Escolha um veículo na Garagem." : activeVehicle().name);
        historyTabs(this.historySection);
        if (this.historySection == 1) {
            investigationList();
            return;
        }
        UiKit uiKit = new UiKit(this);
        if (comparableHistory().size() >= 2) {
            uiKit.row(this.body, "Comparar duas leituras", () -> this.chooseComparison());
        }
        LinearLayout card = card(this.body);
        label(card, "HISTÓRICO DO VEÍCULO SELECIONADO", 11, this.MUTED, true);
        gap(card, 10);
        if (this.history.length() == 0) {
            label(card, "Seus relatórios aparecerão aqui após uma leitura.", 14, this.MUTED, false);
            if (activeVehicle() != null) {
                uiKit.row(card, "Fazer uma leitura no Diagnóstico", () -> this.navigate(NavigationState.DIAGNOSIS));
            }
        }
        for (int i = 0; i < Math.min(this.historyLimit, this.history.length()); i++) {
            final JSONObject json = this.history.optJSONObject(i);
            if (json != null) {
                StringBuilder builder = new StringBuilder();
                builder.append(json.optString("date"));
                builder.append(json.optBoolean("demo") ? " · SIMULADO" : "");
                label(card, builder.toString(), 12, this.ACCENT, true);
                DiagnosisScreen.Headline headline = null;
                if (json.optJSONObject("dtc_read") != null) {
                    try {
                        headline = DiagnosisScreen.headline(DtcReadJson.decode(json.getJSONObject("dtc_read")));
                    } catch (JSONException e) {
                        headline = null;
                    }
                }
                if (headline != null) {
                    label(card, headline.title, 16, headline.clean ? this.TEXT : this.AMBER, true);
                    if (headline.hasCodes || headline.milOn) {
                        label(card, headline.hasCodes ? headline.detail : "Luz de injeção (MIL) acesa", 13, this.TEXT, false);
                    }
                } else {
                    label(card, ReportText.plurals(json.optString("summary")), 13, this.TEXT, false);
                }
                if (json.has("investigation_seed")) {
                    button(card, "Investigar leitura de " + json.optString("date"), false, () -> this.openInvestigationFromReport(json)).setEnabled(!this.caseBusy);
                }
                JSONObject dtcRead = json.optJSONObject("dtc_read");
                final ArrayList items = new ArrayList();
                if (dtcRead != null) {
                    for (JSONObject caseDocument : this.caseDocuments) {
                        if (dtcRead.optString("capture_id").equals(caseDocument.optJSONObject("input").optJSONObject("dtc_read").optString("capture_id"))) {
                            items.add(caseDocument);
                        }
                    }
                }
                if (!items.isEmpty()) {
                    button(card, "Investigações desta leitura (" + items.size() + ")", false, () -> chooseCaseDocuments("Investigações vinculadas à captura", items));
                }
                gap(card, 14);
            }
        }
        if (this.history.length() > this.historyLimit) {
            button(card, "Mostrar mais leituras", false, () -> {
                this.historyLimit = Math.min(this.history.length(), this.historyLimit + 8);
                render();
            });
        }
        LinearLayout disclosure = uiKit.disclosure(this.body, "Exportar e gerenciar leituras");
        button(disclosure, "Compartilhar relatórios JSON", false, () -> this.shareHistory()).setEnabled(this.history.length() > 0);
        button(disclosure, "Ver registros antigos / perfis removidos", false, () -> this.showDetachedHistory()).setEnabled(this.detachedHistory.length() > 0);
        button(disclosure, "Apagar histórico deste veículo", false, () -> this.deleteHistory()).setEnabled(this.history.length() > 0 && this.historyStore != null);
        button(disclosure, "Exportar relatório PDF", false, () -> this.sharePdf()).setEnabled(this.history.length() > 0);
    }

    void openInvestigationFromReport(JSONObject json) {
        try {
            DiagnosticCase diagnosticCase = DiagnosticCaseJson.decode(json.getJSONObject("investigation_seed"));
            editCase(diagnosticCase.vehicleRecord == null ? DiagnosticCaseBuilder.create(diagnosticCase.vehicle, diagnosticCase.adapter, diagnosticCase.dtc, diagnosticCase.telemetry, diagnosticCase.symptoms, this.clock.epochMillis(), diagnosticCase.supplement) : DiagnosticCaseBuilder.createWithProfile(diagnosticCase.vehicleRecord, diagnosticCase.adapter, diagnosticCase.dtc, diagnosticCase.telemetry, diagnosticCase.symptoms, this.clock.epochMillis(), diagnosticCase.supplement), true);
        } catch (Exception e) {
            message(error(e));
        }
    }

    private void historyTabs(int i) {
        UiKit uiKit = new UiKit(this);
        LinearLayout column = uiKit.column();
        column.setOrientation(getResources().getConfiguration().fontScale > 1.3f ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
        this.body.addView(column);
        String[] parts = {"Leituras", "Investigações", "Diário do carro"};
        int value = 0;
        while (value < 3) {
            final int tabIndex = value;
            Button row = uiKit.row(column, parts[value], () -> {
                this.historySection = tabIndex != 1 ? 0 : 1;
                int page = tabIndex == 2 ? 6 : 5;
                this.page = page;
                this.scrollPositions.put(page, 0);
                render();
            });
            row.setCompoundDrawablesRelative(null, null, null, null);
            row.setCompoundDrawablePadding(0);
            row.setTextSize(13.0f);
            row.setPadding(dp(6.0f), dp(column.getOrientation() == LinearLayout.VERTICAL ? 6.0f : 14.0f), dp(6.0f), dp(column.getOrientation() == LinearLayout.VERTICAL ? 6.0f : 14.0f));
            row.setSelected(value == i);
            row.setTextColor(value == i ? this.ACCENT : this.MUTED);
            if (value == i) {
                row.setBackground(ThemePalette.of(this).navigation(this, true));
            }
            if (column.getOrientation() == LinearLayout.HORIZONTAL) {
                row.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
            }
            value++;
        }
    }

    private List<JSONObject> comparableHistory() {
        ArrayList items = new ArrayList();
        for (int i = 0; i < this.history.length(); i++) {
            JSONObject json = this.history.optJSONObject(i);
            if (json != null && json.optJSONObject("dtc_read") != null) {
                items.add(json);
            }
        }
        return items;
    }

    public void chooseComparison() {
        final List<JSONObject> comparableHistory = comparableHistory();
        if (comparableHistory.size() < 2) {
            return;
        }
        final String[] parts = new String[comparableHistory.size()];
        int i = 0;
        while (i < comparableHistory.size()) {
            StringBuilder builder = new StringBuilder();
            int value = i + 1;
            builder.append(value);
            builder.append(" · ");
            builder.append(comparableHistory.get(i).optString("date"));
            builder.append(comparableHistory.get(i).optBoolean("demo") ? " · SIMULADO" : " · conforme registro");
            parts[i] = builder.toString();
            i = value;
        }
        new LocalizedDialog(this).setTitle("Comparar · primeira leitura").setItems(parts, (dialog, which) -> this.chooseSecondReading(parts, comparableHistory, dialog, which)).setNegativeButton("Cancelar", null).show();
    }

    void chooseSecondReading(String[] parts, final List list, DialogInterface dialogInterface, final int i) {
        new LocalizedDialog(this).setTitle("Comparar · segunda leitura").setItems(parts, (dialog, which) -> {
            try {
                message(String.join("\n\n", CaptureComparison.lines(DtcReadJson.decode(((JSONObject) list.get(i)).getJSONObject("dtc_read")), DtcReadJson.decode(((JSONObject) list.get(which)).getJSONObject("dtc_read")))));
            } catch (Exception e) {
                message(error(e));
            }
        }).setNegativeButton("Cancelar", null).show();
    }

    private void garage() {
        VehicleRepository vehicleRepository = this.vehicles;
        if (vehicleRepository == null) {
            heading("SUA GARAGEM", "Garagem", "Os dados originais foram preservados.");
            label(this.body, "Não foi possível carregar a garagem. Reinicie após verificar o armazenamento.", 14, this.AMBER, false);
        } else {
            VehicleRecord activeRecord = vehicleRepository.activeRecord();
            GarageScreen.render(this.body, this.vehicles.records(), activeRecord == null ? "" : activeRecord.id, profileLocked(), catalog(true), new GarageActions());
        }
    }

    class GarageActions implements GarageScreen.Actions {
        @Override
        public void add() {
            MainActivity.this.editVehicle(null);
        }

        @Override
        public void edit(VehicleRecord vehicleRecord) {
            MainActivity.this.editVehicle(vehicleRecord.projection());
        }

        @Override
        public void select(VehicleRecord vehicleRecord) {
            if (MainActivity.this.profileLocked()) {
                return;
            }
            try {
                MainActivity.this.vehicles.select(vehicleRecord.id);
                MainActivity.this.samples.clear();
                MainActivity.this.contextualSamples.clear();
                MainActivity.this.currentDtcRead = null;
                MainActivity.this.currentDtcVehicle = null;
                MainActivity.this.currentDtcRecord = null;
                MainActivity.this.currentSupplement = null;
                MainActivity.this.currentCaseSamples = Collections.emptyList();
                MainActivity.this.scanSummary = "Inicie uma leitura para consultar os códigos OBD2.";
                MainActivity.this.clearHistories();
                MainActivity.this.refreshHistory();
                MainActivity.this.render();
            } catch (Exception e) {
                MainActivity mainActivity = MainActivity.this;
                mainActivity.message(mainActivity.error(e));
            }
        }

        @Override
        public void remove(final VehicleRecord vehicleRecord) {
            if (MainActivity.this.profileLocked()) {
                return;
            }
            new LocalizedDialog(MainActivity.this).setTitle("Remover perfil?").setMessage("Os relatórios e investigações serão preservados na seção de perfis removidos.").setNegativeButton("Cancelar", null).setPositiveButton("Remover", (dialog, which) -> {
                if (MainActivity.this.profileLocked()) {
                    return;
                }
                try {
                    MainActivity.this.vehicles.remove(vehicleRecord.id);
                    if (MainActivity.this.currentDtcVehicle != null && MainActivity.this.currentDtcVehicle.id.equals(vehicleRecord.id)) {
                        MainActivity.this.currentDtcRead = null;
                        MainActivity.this.currentDtcVehicle = null;
                        MainActivity.this.currentDtcRecord = null;
                        MainActivity.this.currentSupplement = null;
                        MainActivity.this.currentCaseSamples = Collections.emptyList();
                        MainActivity.this.scanSummary = "Inicie uma leitura para consultar os códigos OBD2.";
                    }
                    MainActivity.this.refreshHistory();
                    MainActivity.this.render();
                } catch (Exception e) {
                    MainActivity mainActivity = MainActivity.this;
                    mainActivity.message(mainActivity.error(e));
                }
            }).show();
        }

        @Override
        public void guide() {
            MainActivity.this.showCobaltGuide();
        }
    }

    private static String languageName(String choice) {
        if (I18n.PORTUGUESE.equals(choice)) {
            return "Português";
        }
        return I18n.ENGLISH.equals(choice) ? "English" : "Automático";
    }

    private void chooseLanguage() {
        final String[] choices = {I18n.AUTO, I18n.PORTUGUESE, I18n.ENGLISH};
        String[] names = {"Automático (idioma do sistema)", "Português", "English"};
        int current = java.util.Arrays.asList(choices).indexOf(I18n.preference(this));
        new LocalizedDialog(this).setTitle("Idioma").setSingleChoiceItems(names, current, (dialog, which) -> {
            dialog.dismiss();
            if (!I18n.save(this, choices[which])) {
                message("Não foi possível salvar a preferência.");
                return;
            }
            I18n.init(this);
            render();
        }).setNegativeButton("Cancelar", null).show();
    }

    private void more() {
        String text;
        heading("BORBACAR LABS", "Ajustes", "");
        UiKit kit = new UiKit(this);
        kit.section(this.body, "Demonstração", "Conheça o app com dados fictícios, sem adaptador.");
        button(this.body, this.demo ? "Sair da demonstração" : "Ativar demonstração", false, () -> {
            if (this.demo) {
                disconnect();
            } else {
                startDemo();
            }
            this.page = 0;
            render();
        });
        kit.section(this.body, "Uso do dia a dia", "");
        button(this.body, ReminderReceiver.enabled(this) ? "Lembretes de manutenção: ligados" : "Lembretes de manutenção: desligados", false, () -> toggleReminders());
        if (ThemePalette.LIGHT.equals(ThemePalette.preference(this))) {
            text = "Claro";
        } else {
            text = ThemePalette.DARK.equals(ThemePalette.preference(this)) ? "Escuro" : "Sistema";
        }
        button(this.body, "Tema: " + text, false, () -> this.chooseAppearance());
        button(this.body, "Idioma: " + languageName(I18n.preference(this)), false, () -> this.chooseLanguage());
        kit.section(this.body, "Avançado", "");
        button(this.body, I18n.english() ? "Backup and restore" : "Backup e restauração", false, () -> {
            if (connected() || this.demo || this.busy || this.caseBusy || this.caseExportPreparing || (this.journal != null && !this.journal.canBackup())) {
                message(I18n.english() ? "End the connection or demo and wait for pending saves before backing up." : "Encerre a conexão ou demonstração e aguarde os salvamentos antes do backup.");
                return;
            }
            startActivity(new Intent(this, io.rea.obd.backup.BackupActivity.class));
        });
        button(this.body, AiSettings.load(this).configured() ? "Análise por IA: configurada" : "Análise por IA: configurar", false, () -> this.configureAi());
        button(this.body, "Identificação automática", false, () -> this.showAutomaticIdentity());
        button(this.body, "Privacidade", false, () -> message("Privacidade",
                "Tudo o que o app guarda (veículos, leituras, investigações, Diário e preferências) fica só neste celular, sem conta e sem nuvem.\n\n"
                        + "Só sai do aparelho quando você pede uma Análise por IA e confirma a prévia: tipo do veículo, códigos, leituras e sintomas vão direto ao provedor que você escolheu (Gemini ou Claude), com a sua chave de API. "
                        + "Nome do perfil, VIN, anotações, adaptador, identificadores e horários não são enviados. "
                        + "Telefones, e-mails, CPF, CNPJ e placas escritos nos sintomas são removidos; nomes e endereços digitados ali não são detectados.\n\n"
                        + "Relatórios só saem quando você escolhe onde salvar ou compartilhar. Para apagar tudo: Configurações do Android › Apps › BorbaCar Labs › Limpar dados."));
        button(this.body, Motion.reduced(this) ? "Animações: reduzidas" : "Animações: suaves", false, () -> {
            if (Motion.saveReduced(this, !Motion.reduced(this))) {
                render();
            } else {
                message("Não foi possível salvar a preferência.");
            }
        });
        LinearLayout disclosure = new UiKit(this).disclosure(this.body, "Sobre e referências");
        label(disclosure, "SOBRE ESTA VERSÃO", 12, this.ACCENT, true);
        gap(disclosure, 8);
        label(disclosure, "BorbaCar Labs · " + appVersion() + "\nAndroid 8 ou superior\nArmazenamento local · sem conta e sem envio de dados", 14, this.TEXT, false);
        gap(disclosure, 10);
        label(disclosure, "A conexão e as leituras reais precisam ser validadas em um adaptador e veículo. Use o modo de demonstração para conhecer a interface.", 13, this.MUTED, false);
        button(disclosure, "Fontes de protocolo", false, () -> {
            ArrayList items = new ArrayList();
            Iterator<SourceReference> it = KnowledgeRepository.all().iterator();
            while (it.hasNext()) {
                items.add(it.next().display());
            }
            message(String.join("\n\n", items));
        });
        button(disclosure, "Catálogo OBD offline", false, () -> this.browseDtcCatalog());
    }

    public void showAutomaticIdentity() {
        ArrayList items = new ArrayList();
        items.add("CELULAR · informações fornecidas pelo Android");
        items.add("Fabricante: " + Build.MANUFACTURER);
        items.add("Modelo: " + Build.MODEL);
        items.add("Android: " + Build.VERSION.RELEASE + " · API " + Build.VERSION.SDK_INT);
        ElmSession elmSession = this.connection;
        if (this.demo) {
            items.add("ADAPTADOR · demonstração, nenhum adaptador real consultado");
        } else if (elmSession != null && elmSession.isConnected()) {
            StringBuilder builder = new StringBuilder("CONEXÃO ATIVA · ");
            builder.append(this.activeTransportDescription.isEmpty() ? "tipo não registrado" : this.activeTransportDescription);
            items.add(builder.toString());
            StringBuilder builder2 = new StringBuilder("Nome apresentado: ");
            builder2.append(this.deviceName.isEmpty() ? "não disponível" : this.deviceName);
            items.add(builder2.toString());
            items.add("Identificação respondida pelo adaptador: " + elmSession.identity());
            items.add("Protocolo informado na sessão: " + elmSession.protocol());
            items.add("Nome e resposta ELM não comprovam fabricante, modelo comercial ou autenticidade do adaptador.");
        } else {
            items.add("ADAPTADOR · aguardando conexão para identificar a resposta e o protocolo");
        }
        items.add("VEÍCULO · perfil cadastrado não é identificação automática confirmada. VIN e calibração podem ser consultados em Diagnóstico quando houver resposta; motor e câmbio não são deduzidos do nome.");
        items.add("Esta tela usa informações do Android e da sessão existente. Abrir não envia comandos ao carro nem consulta serviços externos.");
        message(String.join("\n\n", items));
    }

    public void chooseAppearance() {
        if (connected() || this.busy || this.recorder != null) {
            message("Encerre a conexão e a gravação antes de mudar o tema. Isso preserva sua sessão atual.");
            return;
        }
        final String[] parts = {ThemePalette.SYSTEM, ThemePalette.LIGHT, ThemePalette.DARK};
        final String preference = ThemePalette.preference(this);
        final int[] values = {Arrays.asList(parts).indexOf(preference)};
        new LocalizedDialog(this).setTitle("Aparência").setSingleChoiceItems(new String[]{"Usar tema do sistema", "Claro", "Escuro"}, values[0], (dialog, which) -> values[0] = which).setNegativeButton("Cancelar", null).setPositiveButton("Aplicar", (dialog, which) -> {
            if (connected() || this.busy || this.recorder != null || preference.equals(parts[values[0]])) {
                return;
            }
            if (this.demo) {
                disconnect();
            }
            if (ThemePalette.save(this, parts[values[0]])) {
                recreate();
            } else {
                message("Não foi possível salvar a aparência.");
            }
        }).show();
    }

    private void capability(String text, String text2) {
        LinearLayout card = card(this.body);
        label(card, "EM DESENVOLVIMENTO", 10, this.AMBER, true);
        gap(card, 6);
        label(card, text, 20, this.TEXT, true);
        gap(card, 8);
        label(card, text2, 14, this.MUTED, false);
    }

    private void showDtcDefinition(String code) {
        DtcDefinition dtcDefinition = DtcReference.find(code);
        String structure = DtcStructure.describe(code);
        if (dtcDefinition == null) {
            message(code, (structure == null ? "" : structure + "\n\n") + "Este código ainda não tem descrição no catálogo local. Ele continua registrado na leitura; nenhuma causa foi presumida.");
            return;
        }
        ArrayList items = new ArrayList();
        items.add(dtcDefinition.display());
        if (structure != null) {
            items.add(structure);
        }
        items.add("Localização da referência: " + dtcDefinition.sourceSection);
        items.add("Texto de significado escrito para este app; não é reprodução de um procedimento de reparo.");
        Iterator<SourceReference> it = DtcReference.sources(dtcDefinition).iterator();
        while (it.hasNext()) {
            items.add(it.next().display());
        }
        message(code, String.join("\n\n", items));
    }

    public void browseDtcCatalog() {
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setPadding(dp(18.0f), dp(10.0f), dp(18.0f), dp(16.0f));
        label(linearLayout, "Catálogo " + DtcReference.VERSION + " · significados genéricos de " + DtcReference.all().size() + " códigos P0 e U0, conferidos em documentos públicos de fabricantes. Não determina causas, peças ou compatibilidade específica.", 14, this.TEXT, false);
        final EditText editText = new EditText(this);
        editText.setTextColor(this.TEXT);
        editText.setHintTextColor(this.MUTED);
        editText.setHint("Código, por exemplo P0301");
        editText.setSingleLine(true);
        editText.setTag("dtc-catalog-query");
        editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        editText.setFilters(new InputFilter[]{new InputFilter.AllCaps(), new InputFilter.LengthFilter(5)});
        linearLayout.addView(editText);
        button(linearLayout, "Consultar código", true, () -> {
            String upperCase = editText.getText().toString().trim().toUpperCase(Locale.ROOT);
            if (upperCase.matches("[PCBU][0-3][0-9A-F]{3}")) {
                showDtcDefinition(upperCase);
            } else {
                message("Informe um código com cinco caracteres, como P0301.");
            }
        });
        UiKit catalogKit = new UiKit(this);
        String group = null;
        for (final DtcDefinition dtcDefinition : DtcReference.all()) {
            String structure = DtcStructure.describe(dtcDefinition.code);
            String area = structure == null ? "Outros" : structure.substring(structure.lastIndexOf(" · ") + 3);
            if (!area.equals(group)) {
                group = area;
                label(linearLayout, area.substring(0, 1).toUpperCase(Locale.ROOT) + area.substring(1), 15, this.ACCENT, true).setPadding(0, dp(14.0f), 0, dp(2.0f));
            }
            catalogKit.row(linearLayout, dtcDefinition.code + " · " + dtcDefinition.meaning, () -> showDtcDefinition(dtcDefinition.code)).setTextSize(14.0f);
        }
        ScrollView scrollView = new ScrollView(this);
        scrollView.setBackgroundColor(this.CARD);
        scrollView.addView(linearLayout);
        new LocalizedDialog(this).setTitle("Catálogo OBD offline").setView(scrollView).setPositiveButton("Fechar", null).show();
    }

    public void editVehicle(VehicleProfile vehicleProfile) {
        openGarageEditor(vehicleProfile == null ? null : this.vehicles.record(vehicleProfile.id), null);
    }

    private void openGarageEditor(VehicleRecord vehicleRecord, Bundle bundle) {
        if (this.vehicles == null || profileLocked()) {
            return;
        }
        try {
            VehicleCatalogStore.Loaded loaded = catalog(true);
            VehicleEditor vehicleEditor = new VehicleEditor(this, vehicleRecord, loaded == null ? null : loaded.catalog, new VehicleEditor.Save() {
                @Override
                public final void save(VehicleRecord vehicleRecord2, int i, VehicleCatalog vehicleCatalog) throws Exception {
                    MainActivity.this.saveVehicleSelection(vehicleRecord2, i, vehicleCatalog);
                }
            }, bundle);
            this.garageEditor = vehicleEditor;
            vehicleEditor.show();
        } catch (Exception e) {
            message("Não foi possível abrir o perfil: " + error(e));
        }
    }

    void saveVehicleSelection(VehicleRecord vehicleRecord, int i, VehicleCatalog vehicleCatalog) throws Exception {
        if (profileLocked()) {
            throw new IllegalStateException("Encerre a conexão ou demonstração antes de salvar o perfil.");
        }
        this.vehicles.saveSelection(vehicleRecord, i, vehicleCatalog);
        refreshHistory();
        render();
    }

    private DiagnosticCase capturedCase() {
        VehicleRecord vehicleRecord = this.currentDtcRecord;
        return vehicleRecord == null ? DiagnosticCaseBuilder.create(this.currentDtcVehicle, this.currentDtcAdapter, this.currentDtcRead, this.currentCaseSamples, new DiagnosticCase.Symptoms("", "", "", ""), this.clock.epochMillis(), this.currentSupplement) : DiagnosticCaseBuilder.createWithProfile(vehicleRecord, this.currentDtcAdapter, this.currentDtcRead, this.currentCaseSamples, new DiagnosticCase.Symptoms("", "", "", ""), this.clock.epochMillis(), this.currentSupplement);
    }

    public void chooseTransport() {
        {
            ConnectionScreen.show(this, lastAdapterName(), new ConnectionScreen.Actions() {
                @Override
                public void last() {
                    MainActivity.this.reconnectLast();
                }

                @Override
                public void nearby() {
                    MainActivity.this.scanBle();
                }

                @Override
                public void paired() {
                    MainActivity.this.chooseDevice();
                }

                @Override
                public void wifi() {
                    MainActivity.this.wifiDialog();
                }
            });
        }
    }

    String lastAdapterName() {
        SharedPreferences preferences = getPreferences(0);
        return preferences.getString("last_adapter_kind", "").isEmpty() ? null : preferences.getString("last_adapter_name", "adaptador");
    }

    public void reconnectLast() {
        SharedPreferences preferences = getPreferences(0);
        String kind = preferences.getString("last_adapter_kind", "");
        String address = preferences.getString("last_adapter_address", "");
        String name = preferences.getString("last_adapter_name", address);
        if (kind.equals("wifi")) {
            try {
                int colon = address.lastIndexOf(':');
                Transports.Wifi wifi = new Transports.Wifi(address.substring(0, colon), Integer.parseInt(address.substring(colon + 1)));
                this.pendingAdapter = new String[]{kind, address, name};
                connect(wifi, name);
            } catch (Exception e) {
                message("Informe IP e porta válidos.");
            }
            return;
        }
        if (this.adapter == null) {
            message("Este aparelho não possui Bluetooth compatível.");
            return;
        }
        if (Build.VERSION.SDK_INT >= 31 && (checkSelfPermission("android.permission.BLUETOOTH_CONNECT") != PackageManager.PERMISSION_GRANTED || checkSelfPermission("android.permission.BLUETOOTH_SCAN") != PackageManager.PERMISSION_GRANTED)) {
            requestPermissions(new String[]{"android.permission.BLUETOOTH_CONNECT", "android.permission.BLUETOOTH_SCAN"}, 102);
            return;
        }
        try {
            if (!this.adapter.isEnabled()) {
                message("Ative o Bluetooth nas configurações.");
                return;
            }
            BluetoothDevice device = this.adapter.getRemoteDevice(address);
            this.pendingAdapter = new String[]{kind, address, name};
            connect(kind.equals("ble") ? new Transports.Ble(this, device) : new Transports.Classic(device, this.adapter), name);
        } catch (IllegalArgumentException | SecurityException e) {
            message("Não foi possível reconectar. Escolha o adaptador de novo.");
        }
    }

    static boolean looksLikeObd(String name) {
        return name != null && name.toLowerCase(Locale.ROOT).matches(".*(obd|elm|vgate|v-link|vlink|veepeak|konnwei|icar|scan|carista|kiwi|obdlink|ancel|autel|launch).*");
    }

    public void chooseDevice() {
        if (this.adapter == null) {
            message("Este aparelho não possui Bluetooth compatível.");
            return;
        }
        if (Build.VERSION.SDK_INT >= 31 && (checkSelfPermission("android.permission.BLUETOOTH_CONNECT") != PackageManager.PERMISSION_GRANTED || checkSelfPermission("android.permission.BLUETOOTH_SCAN") != PackageManager.PERMISSION_GRANTED)) {
            requestPermissions(new String[]{"android.permission.BLUETOOTH_CONNECT", "android.permission.BLUETOOTH_SCAN"}, 100);
            return;
        }
        try {
            if (!this.adapter.isEnabled()) {
                new LocalizedDialog(this).setTitle("Ative o Bluetooth").setMessage("Ative o Bluetooth e pareie seu adaptador nas configurações do Android.").setPositiveButton("Abrir configurações", (dialog, which) -> startActivity(new Intent("android.settings.BLUETOOTH_SETTINGS"))).setNegativeButton("Cancelar", null).show();
                return;
            }
            final ArrayList<BluetoothDevice> items = new ArrayList<>(this.adapter.getBondedDevices());
            final Map<BluetoothDevice, String> names = new HashMap<>();
            for (BluetoothDevice device : items) {
                names.put(device, device.getName() == null ? "Dispositivo Bluetooth" : device.getName());
            }
            items.sort(Comparator.comparing((BluetoothDevice device) -> !looksLikeObd(names.get(device))).thenComparing(device -> names.get(device).toLowerCase(Locale.ROOT)));
            int size = items.size();
            final String[] parts = new String[size + 1];
            for (int i = 0; i < items.size(); i++) {
                BluetoothDevice bluetoothDevice = items.get(i);
                StringBuilder builder = new StringBuilder();
                builder.append(names.get(bluetoothDevice));
                builder.append("\n");
                builder.append(bluetoothDevice.getAddress());
                parts[i] = builder.toString();
            }
            parts[size] = "Parear outro adaptador…";
            new LocalizedDialog(this).setTitle("Escolha seu adaptador ELM327").setItems(parts, (dialog, which) -> {
                if (which == items.size()) {
                    startActivity(new Intent("android.settings.BLUETOOTH_SETTINGS"));
                } else {
                    this.pendingAdapter = new String[]{"classic", items.get(which).getAddress(), parts[which].split("\\n")[0]};
                    connect(new Transports.Classic(items.get(which), this.adapter), parts[which].split("\\n")[0]);
                }
            }).setNegativeButton("Cancelar", null).show();
        } catch (SecurityException ignored) {
            message("O Android não autorizou o acesso ao Bluetooth.");
        }
    }

    @Override
    public void onRequestPermissionsResult(int i, String[] parts, int[] values) {
        super.onRequestPermissionsResult(i, parts, values);
        if (i == REMINDER_PERMISSION) {
            boolean granted = values.length > 0 && values[0] == 0;
            Notice.show(this, granted ? "Lembretes ligados. O app confere o Diário uma vez por dia." : "Sem permissão de notificação: os lembretes aparecem só na tela Hoje.");
            if (granted) {
                new Thread(() -> ReminderReceiver.check(getApplicationContext(), java.time.LocalDate.now()), "borbacar-reminders").start();
            }
            return;
        }
        for (int value : values) {
            if (value != 0) {
                message("Permissão necessária para acessar o adaptador.");
                return;
            }
        }
        if (values.length == 0) {
            return;
        }
        if (i == 100) {
            chooseDevice();
        }
        if (i == 101) {
            scanBle();
        }
        if (i == 102) {
            reconnectLast();
        }
    }

    public void wifiDialog() {
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setPadding(dp(18.0f), 0, dp(18.0f), 0);
        final EditText editText = new EditText(this);
        editText.setSingleLine(true);
        editText.setHint("IP do adaptador (ex.: 192.168.0.10)");
        editText.setText(getPreferences(0).getString("wifi_host", "192.168.0.10"));
        linearLayout.addView(editText);
        final EditText editText2 = new EditText(this);
        editText2.setInputType(InputType.TYPE_CLASS_NUMBER);
        editText2.setHint("Porta (a mais comum é 35000)");
        editText2.setText(getPreferences(0).getString("wifi_port", "35000"));
        linearLayout.addView(editText2);
        new LocalizedDialog(this).setTitle("Adaptador Wi-Fi TCP").setView(linearLayout).setNegativeButton("Cancelar", null).setPositiveButton("Conectar", (dialog, which) -> {
            try {
                String wifiHost = editText.getText().toString().trim();
                int value = Integer.parseInt(editText2.getText().toString());
                if (wifiHost.isEmpty() || value < 1 || value > 65535) {
                    throw new IllegalArgumentException();
                }
                getPreferences(0).edit().putString("wifi_host", wifiHost).putString("wifi_port", Integer.toString(value)).apply();
                this.pendingAdapter = new String[]{"wifi", wifiHost + ":" + value, wifiHost + ":" + value};
                connect(new Transports.Wifi(wifiHost, value), wifiHost + ":" + value);
            } catch (Exception ignored) {
                message("Informe IP e porta válidos.");
            }
        }).show();
    }

    public void scanBle() {
        if (this.adapter == null) {
            message("Bluetooth indisponível.");
            return;
        }
        if (Build.VERSION.SDK_INT >= 31 && (checkSelfPermission("android.permission.BLUETOOTH_CONNECT") != PackageManager.PERMISSION_GRANTED || checkSelfPermission("android.permission.BLUETOOTH_SCAN") != PackageManager.PERMISSION_GRANTED)) {
            requestPermissions(new String[]{"android.permission.BLUETOOTH_CONNECT", "android.permission.BLUETOOTH_SCAN"}, 101);
            return;
        }
        if (Build.VERSION.SDK_INT < 31 && checkSelfPermission("android.permission.ACCESS_FINE_LOCATION") != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{"android.permission.ACCESS_FINE_LOCATION"}, 101);
            return;
        }
        if (!this.adapter.isEnabled()) {
            message("Ative o Bluetooth nas configurações.");
            return;
        }
        stopBleScan();
        BluetoothLeScanner bluetoothLeScanner = this.adapter.getBluetoothLeScanner();
        this.bleScanner = bluetoothLeScanner;
        if (bluetoothLeScanner == null) {
            message("Bluetooth LE indisponível.");
            return;
        }
        final LinkedHashMap map = new LinkedHashMap();
        final List<BluetoothDevice> found = new ArrayList<>();
        final int[] obdCount = {0};
        final ArrayAdapter arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1);
        final AlertDialog alertDialog = new LocalizedDialog(this).setTitle("Procurando adaptadores BLE…").setAdapter(arrayAdapter, (dialog, which) -> {
            BluetoothDevice bluetoothDevice = found.get(which);
            stopBleScan();
            String label = ((String) arrayAdapter.getItem(which)).split(" · ")[0];
            this.pendingAdapter = new String[]{"ble", bluetoothDevice.getAddress(), label};
            connect(new Transports.Ble(this, bluetoothDevice), label);
        }).setNegativeButton("Cancelar", (dialog, which) -> stopBleScan()).create();
        final ScanCallback scanCallback = new ScanCallback() {
            @Override
            public void onScanFailed(int i) {
                MainActivity.this.stopBleScan();
                MainActivity.this.message("Busca BLE falhou (" + i + ").");
            }

            @Override
            @SuppressLint("MissingPermission")
            public void onScanResult(int i, ScanResult scanResult) {
                BluetoothDevice device;
                if (MainActivity.this.bleCallback != this || (device = scanResult.getDevice()) == null || map.containsKey(device.getAddress())) {
                    return;
                }
                map.put(device.getAddress(), device);
                String name = device.getName();
                if (name == null || name.trim().isEmpty()) {
                    return;
                }
                int position = looksLikeObd(name) ? obdCount[0]++ : found.size();
                found.add(position, device);
                arrayAdapter.insert(name + " · " + device.getAddress(), position);
            }
        };
        this.bleCallback = scanCallback;
        try {
            alertDialog.setOnDismissListener((dialog) -> {
                if (this.bleCallback == scanCallback) {
                    stopBleScan();
                }
            });
            this.bleScanner.startScan(this.bleCallback);
            alertDialog.show();
            this.ui.postDelayed(() -> {
                if (this.bleCallback != scanCallback) {
                    return;
                }
                stopBleScan();
                if (alertDialog.isShowing()) {
                    alertDialog.setTitle("Busca concluída · selecione um adaptador");
                }
            }, 10000L);
        } catch (SecurityException ignored) {
            message("Permissão BLE negada.");
        }
    }

    public void stopBleScan() {
        ScanCallback scanCallback;
        BluetoothLeScanner bluetoothLeScanner = this.bleScanner;
        if (bluetoothLeScanner != null && (scanCallback = this.bleCallback) != null) {
            try {
                bluetoothLeScanner.stopScan(scanCallback);
            } catch (SecurityException ignored) {
            }
        }
        this.bleCallback = null;
        this.bleScanner = null;
    }

    private void connect(ElmTransport elmTransport, String deviceName) {
        disconnect(false);
        this.acquisitionSessionId = UUID.randomUUID().toString();
        final int i = this.session;
        final ElmSession elmSession = new ElmSession(elmTransport);
        this.connection = elmSession;
        this.busy = true;
        this.status = "Conectando ao adaptador…";
        this.deviceName = deviceName;
        this.activeTransportDescription = elmTransport.description();
        render();
        this.worker.execute(() -> {
            ScheduledExecutorService scheduledExecutorService = this.watchdog;
            Objects.requireNonNull(elmSession);
            ScheduledFuture<?> schedule = scheduledExecutorService.schedule(() -> elmSession.close(), 35L, TimeUnit.SECONDS);
            String[] candidate = this.pendingAdapter;
            try {
                elmSession.connect();
                schedule.cancel(false);
                post(() -> {
                    if (this.session != i) {
                        return;
                    }
                    if (!elmSession.isConnected()) {
                        connectionLost(elmSession);
                        return;
                    }
                    if (candidate != null) {
                        getPreferences(0).edit().putString("last_adapter_kind", candidate[0]).putString("last_adapter_address", candidate[1]).putString("last_adapter_name", candidate[2]).apply();
                    }
                    this.pollFailures = 0;
                    this.busy = false;
                    this.status = this.deviceName + " · ECU respondeu · " + elmSession.protocol();
                    render();
                    schedulePoll(100L);
                });
            } catch (Exception e) {
                schedule.cancel(false);
                elmSession.close();
                post(() -> {
                    if (this.session != i) {
                        return;
                    }
                    this.connection = null;
                    this.busy = false;
                    this.status = "Falha na conexão";
                    render();
                    message(error(e));
                });
            }
        });
    }

    public void disconnect() {
        disconnect(true);
    }

    private void disconnect(boolean keepReading) {
        boolean keep = keepReading && !this.demo && this.currentDtcRead != null;
        this.session++;
        this.guidedStatus = "";
        this.guidedCompleted = 0;
        if (this.connection != null) {
            this.connection.close();
        }
        closeRecorder();
        this.connection = null;
        this.activeTransportDescription = "";
        this.acquisitionSessionId = null;
        this.demo = false;
        this.busy = false;
        this.ui.removeCallbacks(this.pollTask);
        this.samples.clear();
        this.contextualSamples.clear();
        clearHistories();
        this.voltage = "—";
        this.voltageOrigin = null;
        if (!keep) {
            this.currentDtcRead = null;
            this.currentDtcVehicle = null;
            this.currentDtcRecord = null;
            this.currentSupplement = null;
            this.currentCaseSamples = Collections.emptyList();
            this.scanSummary = "Inicie uma leitura para consultar os códigos OBD2.";
        }
        this.status = "Nenhum adaptador conectado";
        render();
    }

    public void startDemo() {
        disconnect(false);
        this.acquisitionSessionId = UUID.randomUUID().toString();
        this.demo = true;
        this.status = "Simulador local · dados fictícios";
        this.demoTick = 0;
        render();
        schedulePoll(10L);
    }

    public void schedulePoll(long value) {
        this.ui.removeCallbacks(this.pollTask);
        if (this.resumed && !this.destroyed && (this.page == 0 || this.recorder != null)) {
            if (this.demo || connected()) {
                this.ui.postDelayed(this.pollTask, value);
            }
        }
    }

    public void poll() {
        if (!this.resumed || (this.page != 0 && this.recorder == null) || this.busy || this.polling) {
            return;
        }
        if (this.demo || connected()) {
            if (this.demo) {
                int i = this.demoTick + 1;
                this.demoTick = i;
                double sin = Math.sin(((double) i) * 0.25d);
                HashMap map = new HashMap();
                map.put(12, Double.valueOf((500.0d * sin) + 1450.0d));
                map.put(13, Double.valueOf((8.0d * sin) + 42.0d));
                map.put(5, Double.valueOf((Math.sin(((double) this.demoTick) * 0.05d) * 3.0d) + 88.0d));
                map.put(4, Double.valueOf((10.0d * sin) + 32.0d));
                map.put(17, Double.valueOf((sin * 6.0d) + 18.0d));
                map.put(15, Double.valueOf((Math.sin(((double) this.demoTick) * 0.05d) * 2.0d) + 34.0d));
                map.put(11, Double.valueOf((12.0d * sin) + 45.0d));
                map.put(14, Double.valueOf((4.0d * sin) + 14.0d));
                map.put(16, Double.valueOf((1.6d * sin) + 4.2d));
                map.put(47, Double.valueOf(62.0d - (this.demoTick * 0.01d)));
                map.put(66, Double.valueOf((0.1d * sin) + 14.1d));
                for (Integer num : this.selectedPids) {
                    int intValue = num.intValue();
                    Double value = (Double) map.get(num);
                    acceptSample(new TelemetrySample(intValue, value, value == null ? TelemetrySample.Status.NO_DATA : TelemetrySample.Status.VALID, DataOrigin.SIMULATED, this.clock.epochMillis(), this.clock.elapsedMillis(), "Cenário fictício"));
                }
                this.voltage = "13,9 V";
                this.voltageOrigin = DataOrigin.SIMULATED;
                this.voltageTime = this.clock.elapsedMillis();
                refreshValues();
                schedulePoll(900L);
                return;
            }
            final int session = this.session;
            final ElmSession elmSession = this.connection;
            if (elmSession == null) {
                return;
            }
            final ArrayList items = new ArrayList(this.selectedPids);
            this.polling = true;
            this.worker.execute(() -> {
                final ObdReader.Batch poll = new ObdReader(elmSession, this.clock).poll(items);
                post(() -> {
                    this.polling = false;
                    if (this.session != session) {
                        schedulePoll(100L);
                        return;
                    }
                    Iterator<TelemetrySample> it = poll.samples.iterator();
                    while (it.hasNext()) {
                        acceptSample(it.next());
                    }
                    if (poll.failure != null) {
                        this.pollFailures++;
                        if (elmSession.isConnected() && this.pollFailures < 3) {
                            schedulePoll(1500L);
                            return;
                        }
                        connectionLost(elmSession);
                        lost(error(poll.failure));
                        return;
                    }
                    this.pollFailures = 0;
                    this.voltageOrigin = DataOrigin.VEHICLE;
                    this.voltage = poll.adapterVoltage == null ? "—" : String.format(Locale.getDefault(), "%.1f V", poll.adapterVoltage);
                    this.voltageTime = poll.voltageElapsedMillis;
                    refreshValues();
                    schedulePoll(750L);
                });
            });
        }
    }

    public void clearHistories() {
        this.histories.clear();
        this.historyGaps.clear();
        this.historyOrigin = null;
        this.historySessionId = "";
    }

    int expandedPid() {
        if (this.selectedPids.contains(Integer.valueOf(this.expandedPid)) || this.selectedPids.isEmpty()) {
            return this.expandedPid;
        }
        return this.selectedPids.iterator().next().intValue();
    }

    private void acceptSample(TelemetrySample telemetrySample) {
        this.samples.put(Integer.valueOf(telemetrySample.pid), telemetrySample);
        recordSample(telemetrySample);
        VehicleProfile activeVehicle = activeVehicle();
        if (activeVehicle != null && this.acquisitionSessionId != null) {
            this.contextualSamples.put(Integer.valueOf(telemetrySample.pid), new DiagnosticCase.Sample(activeVehicle.id, this.acquisitionSessionId, telemetrySample));
        }
        Integer pid = Integer.valueOf(telemetrySample.pid);
        if (telemetrySample.statusAt(this.clock.elapsedMillis(), DashboardState.MAX_AGE_MS) != TelemetrySample.Status.VALID || telemetrySample.value == null) {
            this.historyGaps.put(pid, Boolean.TRUE);
            return;
        }
        List<ChartView.Point> history = this.histories.computeIfAbsent(pid, (key) -> new ArrayList());
        history.add(new ChartView.Point(telemetrySample.epochMillis, telemetrySample.value.doubleValue(), this.historyGaps.getOrDefault(pid, Boolean.TRUE).booleanValue()));
        this.historyGaps.put(pid, Boolean.FALSE);
        this.historyOrigin = telemetrySample.origin;
        this.historySessionId = this.acquisitionSessionId == null ? "" : this.acquisitionSessionId;
        if (history.size() > 90) {
            history.remove(0);
        }
    }

    public void refreshValues() {
        String text;
        String text2;
        String text3;
        DashboardState dashboardState = dashboardState();
        TextView textView = this.recordingBanner;
        if (textView != null) {
            textView.setText(I18n.t(dashboardState.recording));
        }
        if (this.dashboardViews == null) {
            return;
        }
        long elapsedMillis = this.clock.elapsedMillis() - this.voltageTime;
        boolean flag = !this.voltage.equals("—") && elapsedMillis >= 0 && elapsedMillis <= DashboardState.MAX_AGE_MS;
        StringBuilder builder = new StringBuilder();
        DataOrigin dataOrigin = this.voltageOrigin;
        if (dataOrigin == null) {
            text = "Sem aquisição";
        } else {
            text = dataOrigin == DataOrigin.SIMULATED ? "Dado simulado de alimentação" : "Resposta do adaptador via ATRV";
        }
        builder.append(text);
        if (flag) {
            text2 = " · recebido há " + (elapsedMillis / 1000) + " s · horário UTC não registrado";
        } else {
            text2 = " · sem leitura atual";
        }
        builder.append(text2);
        String string = builder.toString();
        StringBuilder builder2 = new StringBuilder("Sessão de aquisição: ");
        String acquisitionSessionId = this.acquisitionSessionId;
        if (acquisitionSessionId == null) {
            acquisitionSessionId = "não iniciada";
        }
        builder2.append(acquisitionSessionId);
        String text4 = builder2.toString();
        if (this.recorder != null) {
            text4 = text4 + "\nID da gravação (session_id do CSV): " + this.recorder.recordingId() + "\nAquisição vinculada: " + this.recordingAcquisitionId + "\n" + this.recorder.samples() + " amostras gravadas";
        }
        String text5 = text4;
        DashboardScreen.Views views = this.dashboardViews;
        List<ChartView.Point> list = this.histories.getOrDefault(Integer.valueOf(expandedPid()), Collections.emptyList());
        DataOrigin historyOrigin = this.historyOrigin;
        if (historyOrigin == null) {
            text3 = "origem não registrada";
        } else {
            text3 = historyOrigin == DataOrigin.SIMULATED ? "DADOS SIMULADOS" : "leituras OBD";
        }
        views.bind(dashboardState, list, text3, this.historySessionId, flag ? this.voltage : "—", string, text5);
    }

    private void lost(String text) {
        if (this.destroyed) {
            return;
        }
        AlertDialog.Builder builder = new LocalizedDialog(this).setTitle("Conexão perdida").setMessage(text).setNegativeButton("Fechar", null);
        if (lastAdapterName() != null) {
            builder.setPositiveButton("Reconectar", (dialog, which) -> reconnectLast());
        }
        builder.show();
    }

    private void connectionLost(ElmSession elmSession) {
        if (this.connection != elmSession) {
            return;
        }
        elmSession.close();
        this.connection = null;
        this.busy = false;
        this.status = "Conexão encerrada";
        closeRecorder();
        this.samples.clear();
        this.contextualSamples.clear();
        this.voltage = "—";
        this.ui.removeCallbacks(this.pollTask);
        render();
    }

    public void confirmGuidedRead() {
        if (this.busy || this.caseBusy) {
            return;
        }
        if (this.demo || connected()) {
            if (activeVehicle() != null && getPreferences(0).getBoolean("guided_read_explained", false)) {
                startGuidedRead();
            } else if (activeVehicle() != null) {
                getPreferences(0).edit().putBoolean("guided_read_explained", true).apply();
                new LocalizedDialog(this).setTitle("Leitura guiada").setMessage(("Serão consultados códigos de emissões, indicador MIL, prontidão e quadro congelado 00. As respostas disponíveis serão salvas no histórico. Não apaga falhas nem altera configurações.\n\n" + DiagnosisScreen.READINESS_HINT + "\n" + DiagnosisScreen.FREEZE_FRAME_HINT + "\n\n").concat(this.demo ? "Demonstração: todos os resultados serão simulados." : "Use com o veículo parado. Consultas indisponíveis continuarão identificadas como tal.")).setNegativeButton("Cancelar", null).setPositiveButton("Começar", (dialog, which) -> startGuidedRead()).show();
            } else {
                navigate(2);
                message("Crie um perfil, mesmo parcial, para guardar o contexto desta leitura.");
            }
        }
    }

    private void startGuidedRead() {
        if (this.busy || this.caseBusy) {
            return;
        }
        if ((!this.demo && !connected()) || activeVehicle() == null || this.acquisitionSessionId == null) {
            return;
        }
        final VehicleProfile activeVehicle = activeVehicle();
        final String acquisitionSessionId = this.acquisitionSessionId;
        final int i = this.session;
        final ElmSession elmSession = this.connection;
        final boolean demo = this.demo;
        this.currentDtcVehicle = activeVehicle;
        this.currentDtcRecord = this.vehicles.activeRecord();
        this.currentDtcAdapter = demo ? "simulador" : this.deviceName;
        this.currentDtcRead = null;
        this.currentSupplement = null;
        this.currentCaseSamples = new ArrayList(this.contextualSamples.values());
        this.busy = true;
        this.guidedCompleted = 0;
        this.guidedStatus = "Preparando consultas · nenhum resultado ainda";
        this.scanSummary = "Leitura guiada em andamento…";
        render();
        this.worker.execute(() -> {
            ObdReader obdReader;
            if (demo) {
                obdReader = null;
            } else {
                try {
                    obdReader = new ObdReader(elmSession, this.clock);
                } catch (Exception e) {
                    post(() -> {
                        if (this.session != i) {
                            return;
                        }
                        this.busy = false;
                        this.guidedStatus = "Leitura interrompida · " + error(e);
                        render();
                        schedulePoll(100L);
                    });
                    return;
                }
            }
            final ObdReader obdReader2 = obdReader;
            final GuidedReadController.Result result = GuidedReadController.read(new GuidedReadController.Source() {
                @Override
                public DtcReadResult codes() {
                    return demo ? DtcDemoScenario.create(activeVehicle.id, acquisitionSessionId, MainActivity.this.clock.epochMillis()) : obdReader2.readCodes(activeVehicle.id, acquisitionSessionId);
                }

                @Override
                public SupplementalReadResult supplement() {
                    return demo ? SupplementalDemoScenario.create(activeVehicle.id, acquisitionSessionId, MainActivity.this.clock.epochMillis()) : obdReader2.readSupplement(activeVehicle.id, acquisitionSessionId);
                }
            }, new GuidedReadController.Progress() {
                @Override
                public final void stage(int guidedCompleted, String guidedStatus) {
                    MainActivity.this.postGuidedProgress(i, guidedCompleted, guidedStatus);
                }
            });
            post(() -> {
                if (this.session != i) {
                    return;
                }
                this.busy = false;
                this.currentDtcRead = result.codes;
                this.currentSupplement = result.supplement;
                this.scanSummary = DtcFormatter.summary(result.codes);
                this.guidedStatus = result.incomplete() ? "Leitura parcial · confira os serviços sem resposta completa" : "Consultas encerradas · isso não confirma ausência de defeitos";
                if (result.transportFailed() && elmSession != null) {
                    connectionLost(elmSession);
                }
                record(result.codes, activeVehicle);
                render();
                schedulePoll(100L);
            });
        });
    }

    void postGuidedProgress(final int i, final int guidedCompleted, final String guidedStatus) {
        post(() -> {
            if (this.session != i) {
                return;
            }
            this.guidedCompleted = guidedCompleted;
            this.guidedStatus = guidedStatus;
            render();
        });
    }

    public void scan() {
        if (this.busy) {
            return;
        }
        if (this.demo || connected()) {
            this.guidedStatus = "";
            this.guidedCompleted = 0;
            final VehicleProfile activeVehicle = activeVehicle();
            final String id = activeVehicle == null ? null : activeVehicle.id;
            final String acquisitionSessionId = this.acquisitionSessionId;
            if (acquisitionSessionId == null) {
                message("Inicie uma nova conexão ou demonstração antes da leitura.");
                return;
            }
            this.currentSupplement = null;
            this.currentDtcVehicle = activeVehicle;
            VehicleRepository vehicleRepository = this.vehicles;
            this.currentDtcRecord = vehicleRepository == null ? null : vehicleRepository.activeRecord();
            this.currentDtcAdapter = this.demo ? "simulador" : this.deviceName;
            this.currentCaseSamples = new ArrayList(this.contextualSamples.values());
            if (this.demo) {
                DtcReadResult currentDtcRead = DtcDemoScenario.create(id, acquisitionSessionId, this.clock.epochMillis());
                this.currentDtcRead = currentDtcRead;
                this.scanSummary = DtcFormatter.summary(currentDtcRead);
                record(this.currentDtcRead, activeVehicle);
                render();
                return;
            }
            final ElmSession elmSession = this.connection;
            final int i = this.session;
            if (elmSession == null) {
                return;
            }
            this.busy = true;
            this.currentDtcRead = null;
            this.scanSummary = "Consultando falhas armazenadas, pendentes e permanentes…";
            render();
            this.worker.execute(() -> {
                final DtcReadResult codes = new ObdReader(elmSession, this.clock).readCodes(id, acquisitionSessionId);
                post(() -> {
                    if (this.session != i) {
                        return;
                    }
                    this.busy = false;
                    this.currentDtcRead = codes;
                    this.scanSummary = DtcFormatter.summary(codes);
                    if (codes.transportFailed()) {
                        connectionLost(elmSession);
                    }
                    record(codes, activeVehicle);
                    render();
                    schedulePoll(100L);
                });
            });
        }
    }

    private boolean sameAcquisition() {
        VehicleProfile activeVehicle = activeVehicle();
        return this.currentDtcRead != null && activeVehicle != null && activeVehicle.id.equals(this.currentDtcRead.vehicleId) && Objects.equals(this.acquisitionSessionId, this.currentDtcRead.sessionId) && (!this.demo ? !(connected() && this.currentDtcRead.origin == DataOrigin.VEHICLE) : this.currentDtcRead.origin != DataOrigin.SIMULATED);
    }

    public void readSupplement() {
        if (this.busy || this.caseBusy || !sameAcquisition()) {
            return;
        }
        final DtcReadResult dtcReadResult = this.currentDtcRead;
        if (this.demo) {
            this.currentSupplement = SupplementalDemoScenario.create(dtcReadResult.vehicleId, dtcReadResult.sessionId, this.clock.epochMillis());
            render();
            return;
        }
        final ElmSession elmSession = this.connection;
        final int i = this.session;
        if (elmSession == null) {
            return;
        }
        this.busy = true;
        render();
        this.worker.execute(() -> {
            final SupplementalReadResult supplement = new ObdReader(elmSession, this.clock).readSupplement(dtcReadResult.vehicleId, dtcReadResult.sessionId);
            post(() -> {
                if (this.session == i && this.currentDtcRead == dtcReadResult) {
                    this.busy = false;
                    this.currentSupplement = supplement;
                    if (supplement.transportFailed()) {
                        connectionLost(elmSession);
                    }
                    render();
                    schedulePoll(100L);
                }
            });
        });
    }

    public void chooseSensors() {
        final ArrayList items = new ArrayList();
        Set<Integer> supported = (!connected() || this.connection == null) ? null : this.connection.supported();
        for (SensorCatalog.Sensor sensor : SensorCatalog.ALL) {
            if (supported == null || supported.contains(Integer.valueOf(sensor.pid))) {
                items.add(sensor);
            }
        }
        if (items.isEmpty()) {
            message("A ECU não informou sensores compatíveis deste catálogo.");
            return;
        }
        String[] parts = new String[items.size()];
        final boolean[] booleans = new boolean[items.size()];
        for (int i = 0; i < items.size(); i++) {
            SensorCatalog.Sensor sensor2 = (SensorCatalog.Sensor) items.get(i);
            parts[i] = sensor2.name + " (" + sensor2.unit + ")";
            booleans[i] = this.selectedPids.contains(Integer.valueOf(sensor2.pid));
        }
        new LocalizedDialog(this).setTitle("Sensores · até 6").setMultiChoiceItems(parts, booleans, new DialogInterface.OnMultiChoiceClickListener() {
            @Override
            public final void onClick(DialogInterface dialogInterface, int value, boolean flag) {
                MainActivity.toggleSensor(booleans, dialogInterface, value, flag);
            }
        }).setNegativeButton("Cancelar", null).setPositiveButton("Aplicar", (dialog, which) -> this.applySensorSelection(booleans, items, dialog, which)).show();
    }

    static void toggleSensor(boolean[] booleans, DialogInterface dialogInterface, int i, boolean flag) {
        booleans[i] = flag;
    }

    void applySensorSelection(boolean[] booleans, ArrayList items, DialogInterface dialogInterface, int i) {
        int value = 0;
        for (boolean boolean2 : booleans) {
            if (boolean2) {
                value++;
            }
        }
        if (value < 1 || value > 6) {
            message("Selecione de 1 a 6 sensores.");
            return;
        }
        this.samples.clear();
        this.contextualSamples.clear();
        clearHistories();
        this.selectedPids.clear();
        StringBuilder builder = new StringBuilder();
        for (int i2 = 0; i2 < booleans.length; i2++) {
            if (booleans[i2]) {
                int pid = ((SensorCatalog.Sensor) items.get(i2)).pid;
                this.selectedPids.add(Integer.valueOf(pid));
                if (builder.length() > 0) {
                    builder.append(',');
                }
                builder.append(pid);
            }
        }
        getPreferences(0).edit().putString("selected_pids", builder.toString()).apply();
        render();
    }

    public void toggleRecording() {
        TelemetryRecorder telemetryRecorder = this.recorder;
        if (telemetryRecorder != null) {
            long samples = telemetryRecorder.samples();
            closeRecorder();
            render();
            Notice.show(this, "Gravação concluída · " + Format.count((int) samples, "amostra", "amostras") + ". Use Exportar último CSV para salvar.");
            return;
        }
        VehicleProfile activeVehicle = activeVehicle();
        if (activeVehicle == null) {
            message("Selecione um veículo na Garagem.");
            return;
        }
        try {
            File externalFilesDir = getExternalFilesDir(null);
            if (externalFilesDir == null) {
                throw new IOException("Armazenamento externo indisponível");
            }
            File file = new File(externalFilesDir, "registros");
            if (!file.exists() && !file.mkdirs()) {
                throw new IOException("Não foi possível criar a pasta de registros");
            }
            this.recorder = new TelemetryRecorder(new File(file, "telemetria-" + System.currentTimeMillis() + ".csv"), this.demo, activeVehicle.id, activeVehicle.name + " · " + activeVehicle.description(), this.demo ? "simulador" : this.deviceName);
            String acquisitionSessionId = this.acquisitionSessionId;
            if (acquisitionSessionId == null) {
                acquisitionSessionId = "não registrada";
            }
            this.recordingAcquisitionId = acquisitionSessionId;
            render();
        } catch (IOException e) {
            message(error(e));
        }
    }

    public void exportCsv() {
        File file = this.lastCsv;
        if (file == null || !file.exists()) {
            return;
        }
        Intent intent = new Intent("android.intent.action.CREATE_DOCUMENT");
        intent.addCategory("android.intent.category.OPENABLE");
        intent.setType("text/csv");
        intent.putExtra("android.intent.extra.TITLE", this.lastCsv.getName());
        startActivityForResult(intent, 501);
    }

    private void recordSample(TelemetrySample telemetrySample) {
        SensorCatalog.Sensor sensor;
        TelemetryRecorder telemetryRecorder = this.recorder;
        if (telemetryRecorder == null || !this.resumed || this.page != 0 || (sensor = SensorCatalog.get(telemetrySample.pid)) == null) {
            return;
        }
        try {
            telemetryRecorder.add(telemetrySample, sensor.name, sensor.unit);
        } catch (IOException e) {
            closeRecorder();
            render();
            message(error(e));
        }
    }

    private void closeRecorder() {
        TelemetryRecorder telemetryRecorder = this.recorder;
        this.recorder = null;
        if (telemetryRecorder != null) {
            this.lastCsv = telemetryRecorder.file();
            getPreferences(0).edit().putString("last_csv_path", this.lastCsv.getAbsolutePath()).apply();
            try {
                telemetryRecorder.close();
            } catch (IOException ignored) {
            }
        }
    }

    public void showCobaltGuide() {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, String> entry : CobaltGuide.MESSAGES.entrySet()) {
            builder.append(entry.getKey());
            builder.append(" · ");
            builder.append(entry.getValue());
            builder.append("\n\n");
        }
        new LocalizedDialog(this).setTitle("Mensagens do painel · Cobalt MY19").setMessage(builder.toString().trim()).setPositiveButton("Fechar", null).show();
    }

    public void readReadiness() {
        if (!this.demo) {
            runRead("Prontidão dos monitores", (obdReader) -> obdReader.readiness());
            return;
        }
        message("SIMULAÇÃO · consulta avulsa, sem anexar ao caso\nMIL acesa; 1 falha indicada.\n" + String.join("\n", SupplementalFormatter.readinessLines(Collections.singletonList(new SupplementalReadResult.ReadinessBits(129, 7, 5, 4)))));
    }

    public void readVehicleInfo() {
        if (this.demo) {
            message("SIMULAÇÃO: VIN fictício, não representa o veículo.");
        } else {
            VehicleProfile profile = activeVehicle();
            String make = profile == null ? null : profile.make;
            Integer year = profile == null ? null : profile.year;
            runRead("VIN e calibração", (obdReader) -> withVin(obdReader.vehicleInfo(), make, year));
        }
    }

    public void readFreezeFrame() {
        if (this.demo) {
            message("SIMULAÇÃO: quadro congelado fictício. RPM 1450; temperatura 88 °C.");
        } else {
            runRead("Quadro congelado · quadro 00", (obdReader) -> obdReader.freezeFrame());
        }
    }

    static String withVin(String info, String make, Integer year) {
        if (!info.startsWith("VIN: ")) {
            return info;
        }
        int end = info.indexOf('\n');
        String extra = io.rea.obd.vehicle.VinDecoder.describe(info.substring(5, end < 0 ? info.length() : end), java.time.LocalDate.now().getYear(), make, year);
        return extra.isEmpty() ? info : info + "\n" + extra;
    }

    private void runRead(String title, final ReadAction readAction) {
        runSessionAction(title, (elmSession) -> this.runOnReader(readAction, elmSession));
    }

    String runOnReader(ReadAction readAction, ElmSession elmSession) throws Exception {
        return readAction.run(new ObdReader(elmSession, this.clock));
    }

    private void runSessionAction(final String title, final SessionAction sessionAction) {
        if (!connected() || this.busy || this.connection == null) {
            return;
        }
        final ElmSession elmSession = this.connection;
        final int i = this.session;
        this.busy = true;
        render();
        this.worker.execute(() -> {
            Exception failure;
            String result;
            try {
                result = sessionAction.run(elmSession);
                failure = null;
            } catch (Exception e) {
                failure = e;
                result = null;
            }
            final Exception exc = failure;
            final String text = result;
            post(() -> {
                if (this.session != i) {
                    return;
                }
                this.busy = false;
                if (elmSession.isConnected()) {
                    render();
                } else {
                    connectionLost(elmSession);
                }
                message(title, exc == null ? text : error(exc));
                schedulePoll(100L);
            });
        });
    }

    public void confirmClear() {
        if (!connected() || this.busy || this.demo) {
            return;
        }
        new LocalizedDialog(this).setTitle("Apagar falhas e monitores?").setMessage("O comando OBD2 04 apaga códigos e dados de diagnóstico de emissões e reinicia a prontidão. Faça isso somente com o veículo parado e motor desligado, após guardar o relatório.").setNegativeButton("Cancelar", null).setPositiveButton("Apagar", (dialog, which) -> runSessionAction("Apagar falhas", (elmSession) -> EmissionCodeReset.executeAfterUserConfirmation(elmSession))).show();
    }

    public void sharePdf() {
        try {
            ArrayList items = new ArrayList();
            items.add("BorbaCar Labs · Relatórios locais");
            for (int i = 0; i < this.history.length(); i++) {
                JSONObject json = this.history.optJSONObject(i);
                if (json != null) {
                    items.add(" ");
                    StringBuilder builder = new StringBuilder();
                    builder.append(json.optString("date"));
                    builder.append(json.optBoolean("demo") ? " · SIMULADO" : "");
                    items.add(builder.toString());
                    items.add("Veículo: " + json.optString("vehicle_name_snapshot", "não associado"));
                    items.add("Adaptador: " + json.optString("adapter"));
                    JSONObject investigationSeed = json.optJSONObject("investigation_seed");
                    if (investigationSeed != null && investigationSeed.optInt("schema_version") == 4) {
                        items.addAll(VehicleProfileReportV1.report(DiagnosticCaseJson.decode(investigationSeed).vehicleRecord));
                    }
                    items.addAll(Arrays.asList(json.optString("summary").split("\\n")));
                    JSONArray codes = json.optJSONArray("codes");
                    if (codes != null) {
                        for (int i2 = 0; i2 < codes.length(); i2++) {
                            items.add("DTC: " + codes.optString(i2));
                        }
                    }
                }
            }
            File file = new File(getCacheDir(), "relatorio-rea-obd.pdf");
            ReportWriter.pdf(file, ReportText.readable(items, WallClock.zone()));
            this.pendingPdf = file;
            Intent intent = new Intent("android.intent.action.CREATE_DOCUMENT");
            intent.addCategory("android.intent.category.OPENABLE");
            intent.setType("application/pdf");
            intent.putExtra("android.intent.extra.TITLE", "relatorio-rea-obd.pdf");
            startActivityForResult(intent, 500);
        } catch (Exception e) {
            message(error(e));
        }
    }

    @Override
    protected void onActivityResult(int i, int value, Intent intent) {
        super.onActivityResult(i, value, intent);
        if (i == 503) {
            JournalController journalController = this.journal;
            if (journalController != null) {
                journalController.exportResult(value, intent);
                return;
            }
            return;
        }
        if (i == 502) {
            final File file = this.pendingCaseExport;
            this.pendingCaseExport = null;
            if (file == null) {
                return;
            }
            if (value != -1 || intent == null || intent.getData() == null) {
                file.delete();
                return;
            } else {
                final Uri data = intent.getData();
                this.caseWorker.execute(() -> this.writePendingExport(file, data));
                return;
            }
        }
        if ((i != 500 && i != 501) || value != -1 || intent == null || intent.getData() == null) {
            return;
        }
        File pendingPdf = i == 500 ? this.pendingPdf : this.lastCsv;
        try {
            if (pendingPdf == null) {
                return;
            }
            try {
                FileInputStream fileInputStream = new FileInputStream(pendingPdf);
                try {
                    OutputStream outputStream = getContentResolver().openOutputStream(intent.getData());
                    try {
                        if (outputStream == null) {
                            throw new IOException("Destino indisponível");
                        }
                        byte[] bytes = new byte[8192];
                        while (true) {
                            int value2 = fileInputStream.read(bytes);
                            if (value2 == -1) {
                                break;
                            } else {
                                outputStream.write(bytes, 0, value2);
                            }
                        }
                        Notice.show(this, i == 500 ? "PDF salvo." : "CSV salvo.");
                        if (outputStream != null) {
                            outputStream.close();
                        }
                        fileInputStream.close();
                        this.pendingPdf = null;
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
            } catch (Exception e) {
                message(error(e));
            }
        } catch (Throwable throwable) {
            this.pendingPdf = null;
            throw throwable;
        }
    }

    void writePendingExport(File file, Uri uri) {
        try {
            try {
                FileInputStream fileInputStream = new FileInputStream(file);
                try {
                    OutputStream outputStream = getContentResolver().openOutputStream(uri);
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
                        post(() -> Notice.show(this, "Investigação exportada."));
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
            } catch (Exception e) {
                post(() -> message("Falha ao exportar: " + error(e)));
            }
        } catch (Throwable throwable) {
            file.delete();
            throw throwable;
        }
    }

    private void record(DtcReadResult dtcReadResult, VehicleProfile vehicleProfile) {
        String vehicleNameSnapshot;
        if (this.historyStore == null) {
            message("A leitura foi concluída, mas o histórico está indisponível.");
            return;
        }
        try {
            boolean demo = dtcReadResult.origin == DataOrigin.SIMULATED;
            JSONObject json = new JSONObject().put("date", Instant.ofEpochMilli(dtcReadResult.completedAtEpochMillis).atZone(WallClock.zone()).format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))).put("created_at", Instant.ofEpochMilli(dtcReadResult.completedAtEpochMillis).toString()).put("demo", demo).put("vehicle_id", dtcReadResult.vehicleId == null ? JSONObject.NULL : dtcReadResult.vehicleId);
            if (vehicleProfile == null) {
                vehicleNameSnapshot = "não associado";
            } else {
                vehicleNameSnapshot = vehicleProfile.name + " · " + vehicleProfile.description();
            }
            JSONObject json2 = json.put("vehicle_name_snapshot", vehicleNameSnapshot).put("adapter", demo ? "simulador" : this.deviceName).put("summary", DtcFormatter.summary(dtcReadResult)).put("codes", new JSONArray((Collection) DtcFormatter.legacyCodeLines(dtcReadResult))).put("dtc_read", DtcReadJson.encode(dtcReadResult));
            if (vehicleProfile != null) {
                VehicleRecord vehicleRecord = this.currentDtcRecord;
                json2.put("investigation_seed", DiagnosticCaseJson.encode(vehicleRecord == null ? DiagnosticCaseBuilder.create(vehicleProfile, this.currentDtcAdapter, dtcReadResult, this.currentCaseSamples, new DiagnosticCase.Symptoms("", "", "", ""), this.clock.epochMillis(), this.currentSupplement) : DiagnosticCaseBuilder.createWithProfile(vehicleRecord, this.currentDtcAdapter, dtcReadResult, this.currentCaseSamples, new DiagnosticCase.Symptoms("", "", "", ""), this.clock.epochMillis(), this.currentSupplement)));
            }
            this.historyStore.add(json2);
            refreshHistory();
        } catch (Exception e) {
            message("A leitura foi concluída, mas o relatório não pôde ser salvo: " + error(e));
        }
    }

    public void shareHistory() {
        shareReports(this.history);
    }

    private void refreshCases() {
        this.caseWorker.execute(() -> {
            try {
                final List<JSONObject> latestAll = this.caseStore.latestAll();
                post(() -> {
                    this.caseDocuments = latestAll;
                    this.caseWarning = "";
                    render();
                });
            } catch (Exception e) {
                post(() -> {
                    this.caseWarning = "Investigações indisponíveis: " + error(e) + ". Os registros originais foram preservados.";
                    render();
                });
            }
        });
    }

    private void investigationList() {
        LinearLayout card = card(this.body);
        label(card, "INVESTIGAÇÕES OFFLINE", 11, this.ACCENT, true);
        gap(card, 8);
        label(card, "Guarde sintomas junto da leitura, revise e reabra sem internet. A revisão organiza evidências; ainda não propõe causas mecânicas.", 13, this.MUTED, false);
        if (!this.caseWarning.isEmpty()) {
            label(card, this.caseWarning, 13, this.AMBER, false);
        }
        if (this.caseBusy) {
            label(card, "Salvando investigação…", 13, this.AMBER, false);
        }
        VehicleProfile activeVehicle = activeVehicle();
        final HashSet seen = new HashSet();
        VehicleRepository vehicleRepository = this.vehicles;
        if (vehicleRepository != null) {
            Iterator<VehicleProfile> it = vehicleRepository.all().iterator();
            while (it.hasNext()) {
                seen.add(it.next().id);
            }
        }
        int i = 0;
        int value = 0;
        for (final JSONObject caseDocument : this.caseDocuments) {
            JSONObject input = caseDocument.optJSONObject("input");
            if (input != null) {
                String id = input.optJSONObject("vehicle").optString("id");
                if (!seen.contains(id)) {
                    value++;
                }
                if (activeVehicle != null && activeVehicle.id.equals(id)) {
                    int value2 = i + 1;
                    label(card, "Revisado " + Format.when(input.optLong("revised_at_ms")) + " · " + input.optJSONObject("symptoms").optString("description", ""), 12, this.MUTED, false);
                    StringBuilder builder = new StringBuilder("Caso ");
                    builder.append(input.optString("case_id").substring(0, Math.min(8, input.optString("case_id").length())));
                    builder.append(" · revisão ");
                    builder.append(input.optInt("revision"));
                    builder.append(input.optJSONObject("dtc_read").optString("origin").equals("SIMULATED") ? " · SIMULADO" : "");
                    button(card, builder.toString(), false, () -> this.showCase(caseDocument));
                    i = value2;
                }
            }
        }
        if (i == 0) {
            label(card, "Nenhuma investigação salva para o veículo selecionado. Use Investigar esta leitura após ler os códigos.", 13, this.MUTED, false);
        }
        if (value > 0) {
            button(card, "Investigações de perfis removidos", false, () -> this.showRemovedProfileCases(seen));
        }
    }

    void showRemovedProfileCases(Set set) {
        ArrayList items = new ArrayList();
        for (JSONObject caseDocument : this.caseDocuments) {
            if (!set.contains(caseDocument.optJSONObject("input").optJSONObject("vehicle").optString("id"))) {
                items.add(caseDocument);
            }
        }
        chooseCaseDocuments("Perfis removidos · histórico preservado", items);
    }

    private void editCase(final DiagnosticCase diagnosticCase, final boolean flag) {
        if (this.caseBusy) {
            return;
        }
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setPadding(dp(18.0f), dp(8.0f), dp(18.0f), dp(12.0f));
        StringBuilder builder = new StringBuilder();
        builder.append(diagnosticCase.vehicle.name);
        builder.append(" · ");
        builder.append(diagnosticCase.dtc.origin == DataOrigin.SIMULATED ? "SIMULADO" : "LEITURA DO VEÍCULO");
        label(linearLayout, builder.toString(), 14, this.ACCENT, true);
        label(linearLayout, "Informações opcionais, declaradas por você. Salvar não faz nova leitura nem executa comandos.", 13, this.MUTED, false);
        String[] parts = {"O que você percebeu?", "Quando ocorre?", "Com que frequência?", "Condições e observações"};
        String[] parts2 = {diagnosticCase.symptoms.description, diagnosticCase.symptoms.when, diagnosticCase.symptoms.frequency, diagnosticCase.symptoms.conditions};
        final EditText[] editTexts = new EditText[4];
        for (int i = 0; i < 4; i++) {
            label(linearLayout, parts[i], 13, this.MUTED, false);
            EditText editText = new EditText(this);
            editTexts[i] = editText;
            editText.setTextColor(this.TEXT);
            editTexts[i].setTextSize(15.0f);
            editTexts[i].setText(parts2[i]);
            editTexts[i].setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
            editTexts[i].setFilters(new InputFilter[]{new InputFilter.LengthFilter(2000)});
            editTexts[i].setMinLines(2);
            editTexts[i].setTag("case-symptom-" + i);
            linearLayout.addView(editTexts[i]);
        }
        ScrollView scrollView = new ScrollView(this);
        scrollView.setBackgroundColor(this.CARD);
        scrollView.addView(linearLayout);
        final AlertDialog alertDialog = new LocalizedDialog(this).setTitle(flag ? "Nova investigação" : "Revisar sintomas").setView(scrollView).setNegativeButton("Cancelar", null).setPositiveButton("Salvar caso", null).create();
        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public final void onShow(DialogInterface dialogInterface) {
                MainActivity.this.bindCaseSaveButton(alertDialog, editTexts, flag, diagnosticCase, dialogInterface);
            }
        });
        alertDialog.show();
    }

    void bindCaseSaveButton(final AlertDialog alertDialog, final EditText[] editTexts, final boolean flag, final DiagnosticCase diagnosticCase, DialogInterface dialogInterface) {
        alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener((view) -> {
            if (this.caseBusy) {
                return;
            }
            try {
                DiagnosticCase.Symptoms symptoms = new DiagnosticCase.Symptoms(editTexts[0].getText().toString(), editTexts[1].getText().toString(), editTexts[2].getText().toString(), editTexts[3].getText().toString());
                long epochMillis = this.clock.epochMillis();
                final DiagnosticCase diagnosticCase2 = flag ? new DiagnosticCase(diagnosticCase.schemaVersion, diagnosticCase.id, 1, epochMillis, epochMillis, diagnosticCase.vehicle, diagnosticCase.adapter, diagnosticCase.dtc, diagnosticCase.telemetry, symptoms, diagnosticCase.supplement, diagnosticCase.vehicleRecord) : diagnosticCase.revise(symptoms, epochMillis);
                this.caseBusy = true;
                alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
                alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE).setEnabled(false);
                alertDialog.setCancelable(false);
                this.caseWorker.execute(() -> this.saveCaseInBackground(diagnosticCase2, alertDialog));
            } catch (Exception e) {
                message(error(e));
            }
        });
    }

    void saveCaseInBackground(DiagnosticCase diagnosticCase, final AlertDialog alertDialog) {
        try {
            this.caseStore.save(diagnosticCase);
            final List<JSONObject> latestAll = this.caseStore.latestAll();
            final JSONObject json = this.caseStore.load(diagnosticCase.id, diagnosticCase.revision);
            post(() -> {
                this.caseBusy = false;
                this.caseDocuments = latestAll;
                this.caseWarning = "";
                alertDialog.dismiss();
                render();
                showCase(json);
            });
        } catch (Exception e) {
            post(() -> {
                this.caseBusy = false;
                alertDialog.setCancelable(true);
                alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(true);
                alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE).setEnabled(true);
                message("Não foi possível concluir o salvamento: " + error(e) + ". Se houve gravação antes de uma falha de consulta, reabra a lista antes de tentar novamente.");
            });
        }
    }

    public void showCase(final JSONObject document) {
        try {
            final DiagnosticCase diagnosticCase = DiagnosticCaseJson.validateDocument(document);
            final AlertDialog[] alertDialogs = new AlertDialog[1];
            LinearLayout linearLayout = new LinearLayout(this);
            linearLayout.setOrientation(LinearLayout.VERTICAL);
            linearLayout.setPadding(dp(18.0f), dp(8.0f), dp(18.0f), dp(16.0f));
            label(linearLayout, String.join("\n", readableReport(document)), 14, this.TEXT, false);
            button(linearLayout, "Editar sintomas / criar revisão", false, () -> {
                alertDialogs[0].dismiss();
                editCase(diagnosticCase, false);
            }).setEnabled(!this.caseBusy);
            button(linearLayout, "Consultar revisões anteriores", false, () -> this.caseWorker.execute(() -> this.loadRevisions(diagnosticCase)));
            button(linearLayout, "Exportar investigação JSON", false, () -> exportCase(document, false));
            button(linearLayout, "Exportar investigação TXT", false, () -> exportCase(document, "txt"));
            button(linearLayout, "Exportar investigação PDF", false, () -> exportCase(document, true));
            button(linearLayout, "Conversar com a IA", true, () -> this.analyzeWithAi(diagnosticCase));
            button(linearLayout, "Excluir esta investigação", false, () -> this.confirmCaseDeletion(diagnosticCase, alertDialogs));
            ScrollView scrollView = new ScrollView(this);
            scrollView.setBackgroundColor(this.CARD);
            scrollView.addView(linearLayout);
            AlertDialog dialog = new LocalizedDialog(this).setTitle("Investigação · revisão " + diagnosticCase.revision).setView(scrollView).setPositiveButton("Fechar", null).create();
            alertDialogs[0] = dialog;
            dialog.show();
        } catch (Exception e) {
            message("Não foi possível abrir o caso: " + error(e));
        }
    }

    void loadRevisions(final DiagnosticCase diagnosticCase) {
        try {
            final List<JSONObject> revisions = this.caseStore.revisions(diagnosticCase.id);
            post(() -> chooseCaseDocuments("Revisões · " + diagnosticCase.id, revisions));
        } catch (Exception e) {
            post(() -> message(error(e)));
        }
    }

    private io.rea.obd.ai.AiAssistant aiAssistant;

    private void talkAboutCurrentReading() {
        if (busy || caseBusy || currentDtcRead == null || currentDtcVehicle == null) return;
        try {
            final DiagnosticCase candidate = capturedCase();
            caseBusy = true;
            caseWorker.execute(() -> {
                try {
                    DiagnosticCase saved = io.rea.obd.ai.AiCaseRepository.findOrSave(caseStore, candidate);
                    List<JSONObject> documents = caseStore.latestAll();
                    post(() -> {
                        caseBusy = false;
                        caseDocuments = documents;
                        render();
                        analyzeWithAi(saved);
                    });
                } catch (Exception e) {
                    post(() -> { caseBusy = false; message(error(e)); });
                }
            });
        } catch (Exception e) { caseBusy = false; message(error(e)); }
    }

    private void analyzeWithAi(DiagnosticCase diagnosticCase) {
        try {
            if (aiAssistant != null) aiAssistant.close();
            aiAssistant = new io.rea.obd.ai.AiAssistant(this, diagnosticCase);
            aiAssistant.show();
        } catch (Exception e) {
            message(I18n.t("Não foi possível abrir o assistente: ") + error(e));
        }
    }

    private void configureAi() {
        io.rea.obd.ai.AiSetupDialog.show(this);
    }

    void confirmCaseDeletion(final DiagnosticCase diagnosticCase, final AlertDialog[] alertDialogs) {
        new LocalizedDialog(this).setTitle("Excluir caso e todas as revisões?").setMessage("Somente esta investigação será removida. O relatório de leitura e os outros casos serão preservados.").setNegativeButton("Cancelar", null).setPositiveButton("Excluir", (dialog, which) -> this.caseWorker.execute(() -> {
            try {
                this.caseStore.delete(diagnosticCase.id, diagnosticCase.vehicle.id);
                io.rea.obd.ai.AiConversation.deleteCase(this, diagnosticCase.id);
                final List<JSONObject> latestAll = this.caseStore.latestAll();
                post(() -> {
                    alertDialogs[0].dismiss();
                    this.caseDocuments = latestAll;
                    render();
                });
            } catch (Exception e) {
                post(() -> message(error(e)));
            }
        })).show();
    }

    private void chooseCaseDocuments(String title, final List<JSONObject> list) {
        String[] parts = new String[list.size()];
        for (int i = 0; i < list.size(); i++) {
            JSONObject input = list.get(i).optJSONObject("input");
            parts[i] = input.optJSONObject("vehicle").optString("name") + " · revisão " + input.optInt("revision") + " · " + Format.when(input.optLong("revised_at_ms"));
        }
        new LocalizedDialog(this).setTitle(title).setItems(parts, (dialog, which) -> showCase((JSONObject) list.get(which))).setNegativeButton("Fechar", null).show();
    }

    private void exportCase(JSONObject json, boolean flag) {
        exportCase(json, flag ? "pdf" : "json");
    }

    private void exportCase(final JSONObject json, final String text) {
        if (this.caseExportPreparing || this.pendingCaseExport != null) {
            message("Conclua ou cancele a exportação anterior antes de iniciar outra.");
        } else {
            this.caseExportPreparing = true;
            this.caseWorker.execute(() -> {
                try {
                    final DiagnosticCase diagnosticCase = DiagnosticCaseJson.validateDocument(json);
                    final File file = new File(getCacheDir(), "investigacao-" + UUID.randomUUID() + "." + text);
                    if (text.equals("pdf")) {
                        ReportWriter.pdf(file, readableReport(json));
                    } else {
                        FileOutputStream fileOutputStream = new FileOutputStream(file);
                        try {
                            fileOutputStream.write((text.equals("txt") ? String.join("\n", readableReport(json)) : json.toString(2)).getBytes(StandardCharsets.UTF_8));
                            fileOutputStream.close();
                        } catch (Throwable throwable) {
                            try {
                                fileOutputStream.close();
                            } catch (Throwable throwable2) {
                                throwable.addSuppressed(throwable2);
                            }
                            throw throwable;
                        }
                    }
                    post(() -> this.requestCaseExportDestination(file, text, diagnosticCase));
                } catch (Exception e) {
                    post(() -> {
                        this.caseExportPreparing = false;
                        message(error(e));
                    });
                }
            });
        }
    }

    void requestCaseExportDestination(File file, String text, DiagnosticCase diagnosticCase) {
        String type2;
        this.caseExportPreparing = false;
        this.pendingCaseExport = file;
        Intent intent = new Intent("android.intent.action.CREATE_DOCUMENT").addCategory("android.intent.category.OPENABLE");
        if (text.equals("pdf")) {
            type2 = "application/pdf";
        } else {
            type2 = text.equals("txt") ? "text/plain" : "application/json";
        }
        Intent type = intent.setType(type2);
        type.putExtra("android.intent.extra.TITLE", "investigacao-" + diagnosticCase.id + "-r" + diagnosticCase.revision + "." + text);
        try {
            startActivityForResult(type, 502);
        } catch (Exception e) {
            this.pendingCaseExport = null;
            file.delete();
            message(error(e));
        }
    }

    private void shareReports(JSONArray detachedHistory) {
        startActivity(Intent.createChooser(new Intent("android.intent.action.SEND").setType("application/json").putExtra("android.intent.extra.SUBJECT", "Relatórios BorbaCar Labs").putExtra("android.intent.extra.TEXT", detachedHistory.toString()), "Compartilhar relatórios"));
    }

    public void showDetachedHistory() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < this.detachedHistory.length(); i++) {
            JSONObject json = this.detachedHistory.optJSONObject(i);
            if (json != null) {
                builder.append(json.isNull("vehicle_id") ? "VEÍCULO NÃO ASSOCIADO" : json.optString("vehicle_name_snapshot", "Perfil removido"));
                builder.append(" · ");
                builder.append(json.optString("date"));
                builder.append(json.optBoolean("demo") ? " · SIMULADO" : "");
                builder.append("\n");
                builder.append(json.optString("summary"));
                builder.append("\n\n");
            }
        }
        new LocalizedDialog(this).setTitle("Histórico preservado").setMessage(builder.toString()).setPositiveButton("Fechar", null).setNeutralButton("Exportar JSON", (dialog, which) -> shareReports(this.detachedHistory)).show();
    }

    public void deleteHistory() {
        final VehicleProfile activeVehicle = activeVehicle();
        if (activeVehicle == null || this.historyStore == null) {
            return;
        }
        new LocalizedDialog(this).setTitle("Apagar histórico de " + activeVehicle.name + "?").setMessage("Os relatórios deste perfil serão removidos. Outros veículos não serão afetados.").setNegativeButton("Cancelar", null).setPositiveButton("Apagar", (dialog, which) -> {
            try {
                this.historyStore.deleteForVehicle(activeVehicle.id);
                refreshHistory();
                render();
            } catch (Exception e) {
                message(error(e));
            }
        }).show();
    }

    private void post(final Runnable runnable) {
        if (this.destroyed) {
            return;
        }
        this.ui.post(() -> {
            if (this.destroyed) {
                return;
            }
            runnable.run();
        });
    }

    private List<String> readableReport(JSONObject document) throws JSONException {
        List<String> lines = new ArrayList<>(DiagnosticCaseJson.report(document));
        lines.addAll(ReportText.referenceAppendix(DiagnosticCaseJson.decode(document.getJSONObject("input"))));
        return ReportText.readable(lines, WallClock.zone());
    }

    private static final int REMINDER_PERMISSION = 702;

    private void toggleReminders() {
        boolean enable = !ReminderReceiver.enabled(this);
        ReminderReceiver.setEnabled(this, enable);
        if (enable && !ReminderReceiver.canNotify(this)) {
            requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, REMINDER_PERMISSION);
        } else if (enable) {
            Notice.show(this, "Lembretes ligados. O app confere o Diário uma vez por dia.");
            new Thread(() -> ReminderReceiver.check(getApplicationContext(), java.time.LocalDate.now()), "borbacar-reminders").start();
        } else {
            Notice.show(this, "Lembretes desligados.");
        }
        render();
    }

    public String error(Exception exc) {
        if (exc instanceof java.net.SocketTimeoutException) {
            return "O adaptador não respondeu a tempo. Confira se ele está ligado e próximo do celular.";
        }
        if (exc instanceof java.net.ConnectException || exc instanceof java.net.NoRouteToHostException) {
            return "Não foi possível alcançar o adaptador Wi-Fi. Confira se o celular está na rede dele e o endereço/porta.";
        }
        if (exc instanceof java.net.UnknownHostException) {
            return "Endereço do adaptador não encontrado. Confira o IP informado.";
        }
        if (exc instanceof SecurityException) {
            return "O Android bloqueou o acesso. Verifique as permissões do app nas configurações.";
        }
        String message = exc.getMessage();
        if (message == null) {
            return "Falha na comunicação com o adaptador.";
        }
        if (exc instanceof IOException && message.matches("(?i).*(read failed|socket|broken pipe|connection reset|bt socket|closed).*")) {
            return "A conexão com o adaptador caiu. Reconecte e tente novamente.";
        }
        return message;
    }

    public void message(String message) {
        if (this.destroyed) {
            return;
        }
        message("BorbaCar Labs", message);
    }

    public void message(String title, String message) {
        if (this.destroyed) {
            return;
        }
        new LocalizedDialog(this).setTitle(title).setMessage(message).setPositiveButton("Entendi", null).show();
    }
}
