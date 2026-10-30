package com.project.healthbuddy.notification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;
import com.project.healthbuddy.data.FirestoreHelper;
import com.project.healthbuddy.data.Medicine;

public class AlarmReceiver extends BroadcastReceiver {
    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_MESSAGE = "message";
    public static final String EXTRA_ID = "id";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;

        String action = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action) ||
                Intent.ACTION_MY_PACKAGE_REPLACED.equals(action) ||
                "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED".equals(action)) {

            Log.d("AlarmReceiver", "Rescheduling alarms on action: " + action);
            rescheduleAllAlarms(context.getApplicationContext());
            return;
        }

        String title = intent.getStringExtra(EXTRA_TITLE);
        String message = intent.getStringExtra(EXTRA_MESSAGE);
        int id = intent.getIntExtra(EXTRA_ID, 10001);

        if (title == null) title = "Medicine Reminder";
        if (message == null) message = "It's time to take your medicine!";
        int safeId = (id == 0) ? 10001 : id;

        Intent serviceIntent = new Intent(context, AlarmRingtoneService.class);
        serviceIntent.putExtra(AlarmRingtoneService.EXTRA_TITLE, title);
        serviceIntent.putExtra(AlarmRingtoneService.EXTRA_MESSAGE, message);
        serviceIntent.putExtra(AlarmRingtoneService.EXTRA_ID, safeId);

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
        } catch (Exception e) {
            Log.e("AlarmReceiver", "Failed to start AlarmRingtoneService", e);
        }
    }

    private void rescheduleAllAlarms(Context context) {
        Query query = FirestoreHelper.getAllMedicinesQuery();
        if (query == null) return;

        query.get().addOnSuccessListener(queryDocumentSnapshots -> {
            if (queryDocumentSnapshots == null) return;
            for (DocumentSnapshot doc : queryDocumentSnapshots) {
                Medicine medicine = doc.toObject(Medicine.class);
                if (medicine != null) {
                    medicine.setId(doc.getId());
                    AlarmScheduler.scheduleAlarmsForMedicine(context, medicine);
                }
            }
        }).addOnFailureListener(e -> Log.e("AlarmReceiver", "Error fetching medicines for reschedule", e));
    }
}
