package io.rea.obd.journal;

import android.Manifest;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import io.rea.obd.MainActivity;
import io.rea.obd.R;
import io.rea.obd.ui.I18n;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ReminderReceiver extends BroadcastReceiver {
    static final String ACTION_CHECK = "io.rea.obd.action.CHECK_REMINDERS";
    private static final String CHANNEL = "manutencao";
    private static final String PREFS = "reminders";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!enabled(context)) {
            return;
        }
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            schedule(context);
            return;
        }
        PendingResult pending = goAsync();
        new Thread(() -> {
            try {
                check(context, LocalDate.now());
            } finally {
                pending.finish();
            }
        }, "borbacar-reminders").start();
    }

    public static boolean enabled(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean("enabled", false);
    }

    public static void setEnabled(Context context, boolean enabled) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean("enabled", enabled).apply();
        if (enabled) {
            schedule(context);
        } else {
            alarms(context).cancel(alarm(context));
        }
    }

    public static void schedule(Context context) {
        LocalDateTime next = LocalDate.now().atTime(LocalTime.of(9, 0));
        if (!next.isAfter(LocalDateTime.now())) {
            next = next.plusDays(1);
        }
        long at = next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        alarms(context).setInexactRepeating(AlarmManager.RTC, at, AlarmManager.INTERVAL_DAY, alarm(context));
    }

    public static int check(Context context, LocalDate today) {
        List<MaintenanceReminders.Due> due;
        JournalStore store = new JournalStore(context);
        try {
            due = MaintenanceReminders.due(store.latest(), today);
        } catch (Exception e) {
            return 0;
        } finally {
            store.close();
        }
        if (due.isEmpty() || !canNotify(context)) {
            return 0;
        }
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        Set<String> notified = new HashSet<>(prefs.getStringSet("notified", new HashSet<>()));
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        I18n.init(context);
        manager.createNotificationChannel(new NotificationChannel(CHANNEL, I18n.t("Lembretes de manutenção"), NotificationManager.IMPORTANCE_DEFAULT));
        int posted = 0;
        for (MaintenanceReminders.Due reminder : due) {
            if (!notified.add(reminder.key)) {
                continue;
            }
            Notification notification = new Notification.Builder(context, CHANNEL)
                    .setSmallIcon(R.drawable.ic_garage)
                    .setContentTitle(I18n.t(reminder.overdue ? "Manutenção atrasada" : "Manutenção próxima") + " · " + reminder.vehicleName)
                    .setContentText(I18n.t(reminder.title) + " · " + I18n.t(reminder.detail))
                    .setStyle(new Notification.BigTextStyle().bigText(I18n.t(reminder.title) + "\n" + I18n.t(reminder.detail) + "\n" + I18n.t("Conforme o lembrete registrado no Diário.")))
                    .setContentIntent(PendingIntent.getActivity(context, 0, new Intent(context, MainActivity.class), PendingIntent.FLAG_IMMUTABLE))
                    .setAutoCancel(true)
                    .build();
            manager.notify(reminder.key.hashCode(), notification);
            posted++;
        }
        prefs.edit().putStringSet("notified", notified).apply();
        return posted;
    }

    public static boolean canNotify(Context context) {
        return Build.VERSION.SDK_INT < 33
                || context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
    }

    private static AlarmManager alarms(Context context) {
        return context.getSystemService(AlarmManager.class);
    }

    private static PendingIntent alarm(Context context) {
        Intent intent = new Intent(context, ReminderReceiver.class).setAction(ACTION_CHECK);
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }
}
