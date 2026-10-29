package io.rea.obd.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import io.rea.obd.vehicle.VehicleAttribute;
import io.rea.obd.vehicle.VehiclePresentation;
import io.rea.obd.vehicle.VehicleProfile;
import io.rea.obd.vehicle.VehicleRecord;
import io.rea.obd.vehicle.catalog.VehicleCatalog;
import io.rea.obd.vehicle.catalog.VehicleCatalogStore;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class GarageScreen {
    public interface Actions {
        void add();

        void edit(VehicleRecord vehicleRecord);

        void guide();

        void remove(VehicleRecord vehicleRecord);

        void select(VehicleRecord vehicleRecord);
    }

    public static void render(final LinearLayout linearLayout, final List<VehicleRecord> list, final String id, final boolean profileLocked, final VehicleCatalogStore.Loaded loaded, final Actions actions) {
        UiKit uiKit = new UiKit(linearLayout.getContext());
        uiKit.eyebrow(linearLayout, "SEUS VEÍCULOS");
        uiKit.label(linearLayout, "Garagem", 30, UiKit.TEXT, true);
        uiKit.label(linearLayout, "Selecione o carro que vai acompanhar.", 15, UiKit.MUTED, false);
        Objects.requireNonNull(actions);
        uiKit.button(linearLayout, "Adicionar veículo", true, () -> actions.add()).setEnabled(!profileLocked);
        if (profileLocked) {
            uiKit.label(linearLayout, "Encerre a conexão ou demonstração antes de trocar ou editar o perfil.", 13, UiKit.AMBER, false);
        }
        if (list.isEmpty()) {
            LinearLayout card = uiKit.card(linearLayout, false);
            uiKit.label(card, "Sua garagem começa aqui", 22, UiKit.TEXT, true);
            uiKit.label(card, "Nenhum veículo cadastrado. Escolha no catálogo ou cadastre um apelido.", 15, UiKit.MUTED, false);
        }
        final LinearLayout column = uiKit.column();
        if (list.size() > 1) {
            EditText input = uiKit.input(linearLayout, "Buscar nos seus perfis", "", "garage-search");
            input.setHint("Apelido, fabricante ou modelo");
            input.addTextChangedListener(new TextWatcher() {
                @Override
                public void afterTextChanged(Editable editable) {
                }

                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int value, int value2) {
                }

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int value3, int value4) {
                    GarageScreen.populate(column, list, id, profileLocked, actions, charSequence.toString());
                }
            });
        }
        linearLayout.addView(column);
        populate(column, list, id, profileLocked, actions, "");
        if (loaded != null) {
            uiKit.row(linearLayout, "Fontes e cobertura", () -> GarageScreen.sources(linearLayout, loaded));
            if (loaded.warning.isEmpty()) {
                return;
            }
            uiKit.label(linearLayout, loaded.warning, 13, UiKit.AMBER, false);
        }
    }

    public static void populate(LinearLayout linearLayout, List<VehicleRecord> list, String id, final boolean flag, final Actions actions, String text) {
        linearLayout.removeAllViews();
        UiKit uiKit = new UiKit(linearLayout.getContext());
        String lowerCase = text.trim().toLowerCase(Locale.ROOT);
        int i = 0;
        for (final VehicleRecord vehicleRecord : list) {
            VehicleProfile projection = vehicleRecord.projection();
            if ((vehicleRecord.name + " " + projection.description()).toLowerCase(Locale.ROOT).contains(lowerCase)) {
                int value = i + 1;
                boolean flag2 = vehicleRecord.id.equals(id);
                final LinearLayout card = uiKit.card(linearLayout, flag2);
                uiKit.label(card, flag2 ? "PERFIL SELECIONADO" : "PERFIL SALVO", 12, flag2 ? UiKit.ACCENT : UiKit.MUTED, true);
                uiKit.label(card, vehicleRecord.name, 24, UiKit.TEXT, true);
                uiKit.label(card, projection.description().isEmpty() ? "Identificação parcial" : projection.description(), 15, UiKit.MUTED, false);
                if (flag2) {
                    uiKit.artwork(card);
                }
                if (!flag2) {
                    uiKit.button(card, "Selecionar veículo", true, () -> actions.select(vehicleRecord)).setEnabled(!flag);
                }
                uiKit.row(card, "Detalhes do veículo", () -> GarageScreen.details(card, vehicleRecord, flag, actions));
                i = value;
            }
        }
        if (i != 0 || list.isEmpty()) {
            return;
        }
        uiKit.label(linearLayout, "Nenhum perfil encontrado. Tente outro nome.", 15, UiKit.MUTED, false);
    }

    public static void details(LinearLayout linearLayout, final VehicleRecord vehicleRecord, boolean flag, final Actions actions) {
        UiKit uiKit = new UiKit(linearLayout.getContext());
        final LinearLayout column = uiKit.column();
        column.setPadding(uiKit.dp(20), uiKit.dp(12), uiKit.dp(20), uiKit.dp(12));
        column.setBackgroundColor(uiKit.palette.bg);
        int i = 0;
        for (VehicleAttribute vehicleAttribute : vehicleRecord.attributes().values()) {
            if (vehicleAttribute.value == null) {
                i++;
            } else {
                uiKit.label(column, VehiclePresentation.title(vehicleAttribute.key) + " · " + vehicleAttribute.value, 16, UiKit.TEXT, true);
                uiKit.label(column, VehiclePresentation.compactOrigin(vehicleAttribute), 12, UiKit.MUTED, false);
            }
        }
        if (i > 0) {
            uiKit.label(column, "Você pode completar os detalhes restantes quando precisar.", 13, UiKit.MUTED, false);
        }
        final AlertDialog alertDialog = new LocalizedDialog(linearLayout.getContext()).setTitle(vehicleRecord.name).setPositiveButton("Fechar", null).create();
        uiKit.button(column, "Editar perfil", true, () -> {
            alertDialog.dismiss();
            actions.edit(vehicleRecord);
        }).setEnabled(!flag);
        uiKit.row(column, "Ver identificação", () -> GarageScreen.identification(column, vehicleRecord));
        VehicleProfile projection = vehicleRecord.projection();
        if (projection.make.equalsIgnoreCase("Chevrolet") && projection.model.equalsIgnoreCase("Cobalt")) {
            Integer num = 2019;
            if (num.equals(projection.year)) {
                Objects.requireNonNull(actions);
                uiKit.row(column, "Guia de mensagens do painel Cobalt MY19", () -> actions.guide());
            }
        }
        uiKit.row(column, "Remover perfil", () -> {
            alertDialog.dismiss();
            actions.remove(vehicleRecord);
        }).setEnabled(!flag);
        ScrollView scrollView = new ScrollView(linearLayout.getContext());
        scrollView.addView(column);
        alertDialog.setView(scrollView);
        alertDialog.show();
    }

    public static void identification(LinearLayout linearLayout, VehicleRecord vehicleRecord) {
        UiKit uiKit = new UiKit(linearLayout.getContext());
        LinearLayout column = uiKit.column();
        column.setPadding(uiKit.dp(20), uiKit.dp(16), uiKit.dp(20), uiKit.dp(16));
        Iterator<String> it = VehiclePresentation.report(vehicleRecord).iterator();
        while (it.hasNext()) {
            uiKit.label(column, it.next(), 14, UiKit.TEXT, false);
        }
        ScrollView scrollView = new ScrollView(linearLayout.getContext());
        scrollView.setBackgroundColor(uiKit.palette.bg);
        scrollView.addView(column);
        new LocalizedDialog(linearLayout.getContext()).setTitle("Identificação · " + vehicleRecord.name).setView(scrollView).setPositiveButton("Fechar", null).show();
    }

    public static void sources(LinearLayout linearLayout, VehicleCatalogStore.Loaded loaded) {
        UiKit uiKit = new UiKit(linearLayout.getContext());
        LinearLayout column = uiKit.column();
        column.setPadding(uiKit.dp(20), uiKit.dp(16), uiKit.dp(20), uiKit.dp(16));
        uiKit.label(column, "Base " + loaded.catalog.version + " · " + loaded.catalog.coverageNote, 15, UiKit.TEXT, false);
        if (loaded.catalog.sources.isEmpty()) {
            uiKit.label(column, "Nenhuma fonte de configuração publicada. Perfis informados por você são declarações, não confirmação documental.", 15, UiKit.MUTED, false);
        }
        for (VehicleCatalog.Source source : loaded.catalog.sources.values()) {
            uiKit.label(column, source.title + "\n" + source.publisher + " · " + source.documentVersion + "\n" + source.url + "\nSeção: " + source.section + "\nConsulta: " + source.accessedOn + "\nMercados: " + source.markets + "\nAtributos: " + source.fields + "\nPermissão declarada: " + source.permission + "\nReferência: " + source.permissionReference + "\nRevisão: " + source.reviewedBy + " · " + source.reviewedOn, 14, UiKit.TEXT, false);
        }
        ScrollView scrollView = new ScrollView(linearLayout.getContext());
        scrollView.setBackgroundColor(uiKit.palette.bg);
        scrollView.addView(column);
        new LocalizedDialog(linearLayout.getContext()).setTitle("Fontes e cobertura").setView(scrollView).setPositiveButton("Fechar", null).show();
    }
}