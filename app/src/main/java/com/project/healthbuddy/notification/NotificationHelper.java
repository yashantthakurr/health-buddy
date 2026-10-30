package com.project.healthbuddy.notification;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.project.healthbuddy.MainActivity;

public class NotificationHelper {
    public static final String CHANNEL_ID = "medicine_alarm_reminders";
    public static final String CHANNEL_NAME = "Medicine Alarm Reminders";

    public static void showNotification(Context context, String title, String message, int notificationId) {
        NotificationManagerCompat manager = NotificationManagerCompat.from(context);

        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if (alarmSound == null) {
            alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .build();

            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, 
                    CHANNEL_NAME, 
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Rings loud alarm sound when it's time to take medicine or food");
            channel.setSound(alarmSound, audioAttributes);
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 1000, 500, 1000, 500, 1000});

            NotificationManager systemManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (systemManager != null) {
                systemManager.createNotificationChannel(channel);
            }
        }

        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, notificationId, intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setSound(alarmSound)
                .setVibrate(new long[]{0, 1000, 500, 1000, 500, 1000})
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            manager.notify(notificationId, builder.build());
        }
    }
}
