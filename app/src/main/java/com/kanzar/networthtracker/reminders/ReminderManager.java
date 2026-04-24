package com.kanzar.networthtracker.reminders;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import com.kanzar.networthtracker.helpers.Prefs;

import java.util.Calendar;

public class ReminderManager {

    public static void updateReminder(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.setAction("com.kanzar.networthtracker.ACTION_REMINDER");
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, 
                0, 
                intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        boolean enabled = Prefs.getBoolean(Prefs.PREFS_REMINDERS_ENABLED, Prefs.DEFAULT_REMINDERS_ENABLED);
        
        if (!enabled) {
            alarmManager.cancel(pendingIntent);
            return;
        }

        int day = Prefs.getInt(Prefs.PREFS_REMINDER_DAY, Prefs.DEFAULT_REMINDER_DAY);
        int hour = Prefs.getInt(Prefs.PREFS_REMINDER_HOUR, Prefs.DEFAULT_REMINDER_HOUR);
        int minute = Prefs.getInt(Prefs.PREFS_REMINDER_MINUTE, Prefs.DEFAULT_REMINDER_MINUTE);

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);

        if (day == 1) {
            calendar.set(Calendar.DAY_OF_MONTH, 1);
        } else {
            calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
        }

        // If time has passed, move to next month
        if (calendar.before(Calendar.getInstance())) {
            calendar.add(Calendar.MONTH, 1);
            if (day == 0) { // If it was end of month, recalculate the maximum day for the next month
                calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
            }
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
            }
        } catch (Exception e) {
            Log.e("ReminderManager", "Error scheduling reminder", e);
        }
    }
}
