package io.rea.obd;

import static org.robolectric.Shadows.shadowOf;

import android.os.Looper;
import io.rea.obd.ui.NavigationState;
import io.rea.obd.ui.WallClock;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.TimeZone;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = 35, qualifiers = "pt-rBR-w393dp-h852dp-xhdpi")
public class ScreenshotReferenceTest {
    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    private TimeZone previous;

    @Before
    public void fixTheClock() {
        this.previous = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone(ZONE));
        WallClock.use(Clock.fixed(Instant.parse("2026-10-28T16:30:00Z"), ZONE));
    }

    @After
    public void releaseTheClock() {
        WallClock.use(null);
        TimeZone.setDefault(this.previous);
    }

    private static void settle() throws InterruptedException {
        for (int i = 0; i < 20; i++) {
            Thread.sleep(50);
            shadowOf(Looper.getMainLooper()).idle();
        }
    }

    private static void verify(MainActivity activity, String name) throws Exception {
        settle();
        ScreenshotComparison.verify(activity.getWindow().getDecorView(), name);
    }

    @Test
    public void screensWithoutDataMatchTheirReferences() throws Exception {
        ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup();
        MainActivity activity = controller.get();
        verify(activity, "pt-hoje-vazio");
        activity.navigate(NavigationState.DIAGNOSIS);
        verify(activity, "pt-diagnostico-inicio");
        activity.navigate(NavigationState.GARAGE);
        verify(activity, "pt-garagem-vazia");
        controller.pause().stop().destroy();
    }

    @Test
    public void screensWithACarAndAReadingMatchTheirReferences() throws Exception {
        ScreensTest.seedVehicleAndJournal();
        ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup();
        MainActivity activity = controller.get();
        verify(activity, "pt-hoje-com-carro");
        activity.navigate(NavigationState.JOURNAL);
        verify(activity, "pt-diario");
        activity.startDemo();
        activity.navigate(NavigationState.DIAGNOSIS);
        activity.scan();
        verify(activity, "pt-diagnostico-leitura");
        activity.navigate(NavigationState.LIVE);
        verify(activity, "pt-ao-vivo");
        controller.pause().stop().destroy();
    }

    @Test
    @Config(qualifiers = "en-rUS-w393dp-h852dp-xhdpi")
    public void englishScreensMatchTheirReferences() throws Exception {
        ScreensTest.seedVehicleAndJournal();
        ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup();
        MainActivity activity = controller.get();
        verify(activity, "en-today");
        activity.startDemo();
        activity.navigate(NavigationState.DIAGNOSIS);
        activity.scan();
        verify(activity, "en-diagnosis-reading");
        controller.pause().stop().destroy();
    }
}
