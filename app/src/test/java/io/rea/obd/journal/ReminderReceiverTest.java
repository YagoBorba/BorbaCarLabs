package io.rea.obd.journal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.app.Application;
import android.app.Notification;
import android.app.NotificationManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class ReminderReceiverTest {
    @Test
    public void dueReminderIsNotifiedOnce() throws Exception {
        Application app = RuntimeEnvironment.getApplication();
        shadowOf(app).grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS);
        LocalDate today = LocalDate.of(2026, 11, 10);
        long at = System.currentTimeMillis();
        JournalStore store = new JournalStore(app);
        store.save(new JournalEntry("oil", "car", "Meu Cobalt", JournalEntry.Kind.SERVICE, 1, at, at, at,
                "Troca de óleo e filtro", new BigDecimal("48000"), null, null, null, false, false, "2026-11-13", null));
        store.close();

        assertEquals(1, ReminderReceiver.check(app, today));
        assertEquals(0, ReminderReceiver.check(app, today));

        NotificationManager manager = app.getSystemService(NotificationManager.class);
        assertEquals(1, shadowOf(manager).getAllNotifications().size());
        Notification notification = shadowOf(manager).getAllNotifications().get(0);
        assertEquals("Manutenção próxima · Meu Cobalt", notification.extras.getString(Notification.EXTRA_TITLE));
        assertTrue(notification.extras.getString(Notification.EXTRA_TEXT).contains("vence em 3 dias"));
    }

    @Test
    public void withoutPermissionNothingIsPosted() throws Exception {
        Application app = RuntimeEnvironment.getApplication();
        shadowOf(app).denyPermissions(android.Manifest.permission.POST_NOTIFICATIONS);
        long at = System.currentTimeMillis();
        JournalStore store = new JournalStore(app);
        store.save(new JournalEntry("oil", "car", "Meu Cobalt", JournalEntry.Kind.SERVICE, 1, at, at, at,
                "Troca de óleo", new BigDecimal("48000"), null, null, null, false, false, "2026-11-11", null));
        store.close();
        assertEquals(0, ReminderReceiver.check(app, LocalDate.of(2026, 11, 10)));
    }
}
