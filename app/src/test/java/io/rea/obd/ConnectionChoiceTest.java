package io.rea.obd;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.app.AlertDialog;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, qualifiers = "pt-rBR-w393dp-h852dp-xhdpi")
public class ConnectionChoiceTest {
    private static void idle() {
        shadowOf(Looper.getMainLooper()).idle();
    }

    private static void edits(View view, List<EditText> out) {
        if (view instanceof EditText) {
            out.add((EditText) view);
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                edits(group.getChildAt(i), out);
            }
        }
    }

    @Test
    public void obdAdaptersComeBeforeHeadphones() {
        assertTrue(MainActivity.looksLikeObd("OBDII"));
        assertTrue(MainActivity.looksLikeObd("Vgate iCar Pro"));
        assertTrue(MainActivity.looksLikeObd("IOS-Vlink"));
        assertFalse(MainActivity.looksLikeObd("JBL Tune 510BT"));
        assertFalse(MainActivity.looksLikeObd(null));
    }

    @Test
    public void theLastAdapterIsOfferedFirstAndWifiStartsWithTheCommonAddress() {
        ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup();
        MainActivity activity = controller.get();
        assertNull(activity.lastAdapterName());
        activity.wifiDialog();
        idle();
        List<EditText> fields = new ArrayList<>();
        edits(ShadowAlertDialog.getLatestAlertDialog().getWindow().getDecorView(), fields);
        assertEquals("192.168.0.10", fields.get(0).getText().toString());
        assertEquals("35000", fields.get(1).getText().toString());
        ShadowAlertDialog.getLatestAlertDialog().dismiss();

        activity.getPreferences(0).edit().putString("last_adapter_kind", "classic").putString("last_adapter_address", "00:11:22:33:44:55").putString("last_adapter_name", "OBDII").apply();
        activity.chooseTransport();
        idle();
        AlertDialog screen = ShadowAlertDialog.getLatestAlertDialog();
        View last = screen.getWindow().getDecorView().findViewWithTag("connect-last");
        assertNotNull(last);
        assertEquals("Reconectar · OBDII", ((Button) last).getText().toString());
        controller.pause().stop().destroy();
    }
}
