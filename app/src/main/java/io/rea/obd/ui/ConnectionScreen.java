package io.rea.obd.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Build;
import android.widget.LinearLayout;
import android.widget.ScrollView;

public final class ConnectionScreen {
    public interface Actions {
        void nearby();

        void paired();

        void wifi();

        default void last() {
        }
    }

    public static void show(Activity activity, String lastName, final Actions actions) {
        UiKit uiKit = new UiKit(activity);
        LinearLayout column = uiKit.column();
        column.setPadding(uiKit.dp(20), uiKit.dp(16), uiKit.dp(20), uiKit.dp(12));
        uiKit.eyebrow(column, "IDENTIFICADO PELO ANDROID");
        uiKit.label(column, Build.MANUFACTURER + " " + Build.MODEL, 18, UiKit.TEXT, true);
        uiKit.label(column, "Android " + Build.VERSION.RELEASE + " · API " + Build.VERSION.SDK_INT, 13, UiKit.MUTED, false);
        uiKit.label(column, "Conecte o scanner ao carro e escolha onde procurar. O app solicita as permissões necessárias e consulta as respostas do dispositivo escolhido.", 15, UiKit.TEXT, false);
        final AlertDialog alertDialog = new LocalizedDialog(activity).setTitle("Encontrar meu scanner").setNegativeButton("Cancelar", null).create();
        if (lastName != null) {
            uiKit.button(column, "Reconectar · " + lastName, true, () -> {
                alertDialog.dismiss();
                actions.last();
            }).setTag("connect-last");
            uiKit.label(column, "Último adaptador que respondeu neste aparelho.", 12, UiKit.MUTED, false);
        }
        uiKit.button(column, "Ver dispositivos pareados", lastName == null, () -> {
            alertDialog.dismiss();
            actions.paired();
        });
        uiKit.label(column, "Lista fornecida pelo Android · conexão Bluetooth clássico.", 12, UiKit.MUTED, false);
        uiKit.button(column, "Procurar dispositivos próximos", false, () -> {
            alertDialog.dismiss();
            actions.nearby();
        });
        uiKit.label(column, "Busca Bluetooth LE. Um nome encontrado não comprova compatibilidade OBD.", 12, UiKit.MUTED, false);
        uiKit.button(column, "Meu scanner usa Wi-Fi", false, () -> {
            alertDialog.dismiss();
            actions.wifi();
        });
        uiKit.label(column, "Conecte à rede do scanner. Se o endereço não puder ser descoberto, use o endereço documentado pelo fabricante.", 12, UiKit.MUTED, false);
        ScrollView scrollView = new ScrollView(activity);
        scrollView.addView(column);
        alertDialog.setView(scrollView);
        alertDialog.show();
    }
}