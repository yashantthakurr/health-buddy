package com.project.healthbuddy.notification;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.project.healthbuddy.MainActivity;

import java.util.Locale;

public class AlarmRingtoneService extends Service {

    public static final String ACTION_STOP_ALARM = "com.project.healthbuddy.STOP_ALARM";
    public static final String ACTION_ALARM_STARTED = "com.project.healthbuddy.ALARM_STARTED";
    public static final String ACTION_ALARM_STOPPED = "com.project.healthbuddy.ALARM_STOPPED";

    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_MESSAGE = "message";
    public static final String EXTRA_ID = "id";

    private static volatile boolean isRinging = false;

    public static boolean isRinging() {
        return isRinging;
    }

    private MediaPlayer mediaPlayer;
    private TextToSpeech tts;
    private Handler handler;
    private Runnable stopRunnable;

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());
        try {
            String lang = com.project.healthbuddy.ui.settings.LocaleHelper.getLanguage(getApplicationContext());
            Locale locale = "hi".equalsIgnoreCase(lang) ? new Locale("hi", "IN") : Locale.US;
            tts = new TextToSpeech(getApplicationContext(), status -> {
                if (status == TextToSpeech.SUCCESS && tts != null) {
                    tts.setLanguage(locale);
                }
            });
        } catch (Exception e) {
            Log.e("AlarmRingtoneService", "TTS init error", e);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP_ALARM.equals(intent.getAction())) {
            stopRingtoneAndSelf();
            return START_NOT_STICKY;
        }

        String title = intent != null && intent.hasExtra(EXTRA_TITLE) ? intent.getStringExtra(EXTRA_TITLE) : "Medicine Reminder";
        String message = intent != null && intent.hasExtra(EXTRA_MESSAGE) ? intent.getStringExtra(EXTRA_MESSAGE) : "It's time to take your medicine!";
        int rawId = intent != null ? intent.getIntExtra(EXTRA_ID, 10001) : 10001;
        int safeId = (rawId == 0) ? 10001 : rawId;

        startForegroundNotification(title, message, safeId);
        playAlarmRingtoneForOneMinute();
        speakAlarmAnnouncement(title, message);
        scheduleFiveMinuteSnooze(title, message, safeId);

        return START_STICKY;
    }

    private void speakAlarmAnnouncement(String title, String message) {
        if (tts != null) {
            try {
                String announcement = title + ". " + message;
                tts.speak(announcement, TextToSpeech.QUEUE_FLUSH, null, "ALARM_TTS_ID");
            } catch (Exception e) {
                Log.e("AlarmRingtoneService", "Error speaking announcement", e);
            }
        }
    }

    private void startForegroundNotification(String title, String message, int id) {
        String channelId = "alarm_service_channel";
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Active Alarm Channel",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Rings loud alarm sound when it's time to take medicine or food");
            channel.setSound(null, null); // Sound handled by MediaPlayer for continuous loop
            if (manager != null) manager.createNotificationChannel(channel);
        }

        Intent openIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingOpen = PendingIntent.getActivity(this, id, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent stopIntent = new Intent(this, AlarmRingtoneService.class);
        stopIntent.setAction(ACTION_STOP_ALARM);
        PendingIntent pendingStop = PendingIntent.getService(this, id + 1, stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setContentIntent(pendingOpen)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Silence Alarm", pendingStop)
                .setOngoing(true)
                .build();

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    startForeground(id, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
                } else {
                    startForeground(id, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_NONE);
                }
            } else {
                startForeground(id, notification);
            }
        } catch (Exception e) {
            Log.e("AlarmRingtoneService", "Error starting foreground service", e);
        }
    }

    private void playAlarmRingtoneForOneMinute() {
        stopAudio();

        Uri[] urisToTry = new Uri[]{
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        };

        boolean started = false;
        for (Uri uri : urisToTry) {
            if (uri == null) continue;
            try {
                mediaPlayer = new MediaPlayer();
                mediaPlayer.setDataSource(this, uri);
                mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build());
                mediaPlayer.setLooping(true);
                mediaPlayer.prepare();
                mediaPlayer.start();
                started = true;
                break;
            } catch (Exception e) {
                Log.e("AlarmRingtoneService", "Failed to play uri: " + uri, e);
                stopAudio();
            }
        }

        if (started) {
            isRinging = true;
            try {
                sendBroadcast(new Intent(ACTION_ALARM_STARTED).setPackage(getPackageName()));
            } catch (Exception e) {
                Log.e("AlarmRingtoneService", "Error sending alarm started broadcast", e);
            }
            stopRunnable = this::stopAudio;
            handler.postDelayed(stopRunnable, 60000);
        }
    }

    private void scheduleFiveMinuteSnooze(String title, String message, int id) {
        long snoozeTime = System.currentTimeMillis() + (5 * 60 * 1000L);
        AlarmScheduler.scheduleRawAlarm(this, title, message, id, snoozeTime);
    }

    private void stopAudio() {
        if (isRinging) {
            isRinging = false;
            try {
                sendBroadcast(new Intent(ACTION_ALARM_STOPPED).setPackage(getPackageName()));
            } catch (Exception e) {
                Log.e("AlarmRingtoneService", "Error sending alarm stopped broadcast", e);
            }
        }
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.release();
            } catch (Exception ignored) {}
            mediaPlayer = null;
        }
        if (tts != null) {
            try {
                tts.stop();
                tts.shutdown();
            } catch (Exception ignored) {}
            tts = null;
        }
        if (handler != null && stopRunnable != null) {
            handler.removeCallbacks(stopRunnable);
        }
    }

    private void stopRingtoneAndSelf() {
        stopAudio();
        stopForeground(true);
        stopSelf();
    }

    @Override
    public void onDestroy() {
        stopAudio();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
