package com.project.healthbuddy.notification;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.project.healthbuddy.MainActivity;
import com.project.healthbuddy.data.Medicine;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class AlarmScheduler {

    public static int getNotificationId(Medicine medicine) {
        if (medicine == null) return 10001;
        String idStr = medicine.getId();
        if (idStr == null || idStr.trim().isEmpty()) {
            idStr = medicine.getName() != null ? medicine.getName() : "default_medicine";
        }
        int id = Math.abs(idStr.hashCode());
        return id == 0 ? 10001 : id;
    }

    public static void scheduleAlarmsForMedicine(Context context, Medicine medicine) {
        if (medicine == null) return;
        int notificationId = getNotificationId(medicine);
        
        long medicineTimeMillis = getTimeInMillis(medicine.getMedicineTime());
        scheduleRawAlarm(context, 
                "💊 Medicine Reminder", 
                "It's time to take " + medicine.getName() + " (" + medicine.getDosage() + ")", 
                notificationId, 
                medicineTimeMillis);

        if ("After Food".equalsIgnoreCase(medicine.getFoodRelation())) {
            long foodTimeMillis = medicineTimeMillis - (30L * 60 * 1000); // 30 mins before
            scheduleRawAlarm(context, 
                    "🍽 Time to Have Food", 
                    "Please have your meal. Your medicine " + medicine.getName() + " is scheduled in 30 minutes.", 
                    notificationId + 10000, 
                    foodTimeMillis);
        }
    }

    public static void cancelAlarm(Context context, int notificationId) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        int safeId = (notificationId == 0) ? 10001 : notificationId;

        Intent intent = new Intent(context, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                safeId,
                intent,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
        );
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
        }

        PendingIntent foodPendingIntent = PendingIntent.getBroadcast(
                context,
                safeId + 10000,
                intent,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
        );
        if (foodPendingIntent != null) {
            alarmManager.cancel(foodPendingIntent);
            foodPendingIntent.cancel();
        }
    }

    public static void scheduleRawAlarm(Context context, String title, String message, int id, long timeMillis) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;
        
        int safeId = (id == 0) ? 10001 : id;
        long scheduledTime = timeMillis;
        if (scheduledTime < System.currentTimeMillis()) {
            scheduledTime += 24 * 60 * 60 * 1000L;
        }

        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.putExtra(AlarmReceiver.EXTRA_TITLE, title);
        intent.putExtra(AlarmReceiver.EXTRA_MESSAGE, message);
        intent.putExtra(AlarmReceiver.EXTRA_ID, safeId);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, safeId, intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent showIntent = new Intent(context, MainActivity.class);
        PendingIntent showPendingIntent = PendingIntent.getActivity(
                context, safeId, showIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    AlarmManager.AlarmClockInfo clockInfo = new AlarmManager.AlarmClockInfo(scheduledTime, showPendingIntent);
                    alarmManager.setAlarmClock(clockInfo, pendingIntent);
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, scheduledTime, pendingIntent);
                }
            } else {
                AlarmManager.AlarmClockInfo clockInfo = new AlarmManager.AlarmClockInfo(scheduledTime, showPendingIntent);
                alarmManager.setAlarmClock(clockInfo, pendingIntent);
            }
        } catch (SecurityException e) {
            Log.e("AlarmScheduler", "Exact alarm security exception fallback", e);
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, scheduledTime, pendingIntent);
        }
    }

    public static long getTimeInMillis(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty()) {
            return System.currentTimeMillis();
        }

        String cleanedTimeStr = timeStr.trim().replaceAll("\\s+", " ");
        String[] patterns = new String[]{
                "hh:mm a",
                "h:mm a",
                "hh:mma",
                "h:mma",
                "HH:mm",
                "H:mm"
        };

        Date date = null;
        for (String pattern : patterns) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(pattern, Locale.US);
                sdf.setLenient(true);
                date = sdf.parse(cleanedTimeStr);
                if (date != null) break;
            } catch (Exception ignored) {}
        }

        if (date == null) {
            Log.e("AlarmScheduler", "Unable to parse time: " + timeStr);
            return System.currentTimeMillis();
        }

        Calendar calendar = Calendar.getInstance();
        Calendar timeCalendar = Calendar.getInstance();
        timeCalendar.setTime(date);

        calendar.set(Calendar.HOUR_OF_DAY, timeCalendar.get(Calendar.HOUR_OF_DAY));
        calendar.set(Calendar.MINUTE, timeCalendar.get(Calendar.MINUTE));
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        return calendar.getTimeInMillis();
    }
}
