package com.project.healthbuddy.ui.home;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import com.project.healthbuddy.MainActivity;
import com.project.healthbuddy.R;
import com.project.healthbuddy.data.FirestoreHelper;
import com.project.healthbuddy.data.MedicationHistory;
import com.project.healthbuddy.data.Medicine;
import com.project.healthbuddy.notification.AlarmRingtoneService;
import com.project.healthbuddy.notification.AlarmScheduler;
import com.project.healthbuddy.ui.AddMedicineActivity;
import com.project.healthbuddy.ui.prescription.MedicineInfoDialog;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private RecyclerView recyclerView;
    private LinearLayout emptyState;
    private TextView textWelcome, textSubtitle, textWaterProgress;
    private ProgressBar progressWater;
    private Button btnSilenceAlarm;

    private ListenerRegistration medicinesListener;
    private ListenerRegistration historyListener;
    private BroadcastReceiver alarmStateReceiver;
    private TextToSpeech tts;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getContext() != null) {
            String lang = com.project.healthbuddy.ui.settings.LocaleHelper.getLanguage(getContext());
            Locale locale = "hi".equalsIgnoreCase(lang) ? new Locale("hi", "IN") : Locale.US;
            tts = new TextToSpeech(getContext().getApplicationContext(), status -> {
                if (status == TextToSpeech.SUCCESS && tts != null) {
                    tts.setLanguage(locale);
                }
            });
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);

        recyclerView = root.findViewById(R.id.recycler_medicines);
        emptyState = root.findViewById(R.id.empty_state);
        textWelcome = root.findViewById(R.id.text_welcome);
        textSubtitle = root.findViewById(R.id.text_subtitle);
        
        Button btnAddFirst = root.findViewById(R.id.btn_add_first);
        Button btnScan = root.findViewById(R.id.btn_scan_prescription);
        btnSilenceAlarm = root.findViewById(R.id.btn_silence_alarm);

        Button btnCallCaregiver = root.findViewById(R.id.btn_call_caregiver);
        Button btnCallDoctor = root.findViewById(R.id.btn_call_doctor);

        textWaterProgress = root.findViewById(R.id.text_water_progress);
        progressWater = root.findViewById(R.id.progress_water);
        Button btnAddWater = root.findViewById(R.id.btn_add_water);
        Button btnResetWater = root.findViewById(R.id.btn_reset_water);
        ImageButton btnEditGoal = root.findViewById(R.id.btn_edit_water_goal);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        btnAddFirst.setOnClickListener(v -> startActivity(new Intent(getContext(), AddMedicineActivity.class)));
        
        btnScan.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateToScanTab();
            }
        });

        if (btnCallCaregiver != null) {
            btnCallCaregiver.setOnClickListener(v -> makeSpeedDialCall("key_caregiver_phone", R.string.prompt_set_caregiver));
        }

        if (btnCallDoctor != null) {
            btnCallDoctor.setOnClickListener(v -> makeSpeedDialCall("key_doctor_phone", R.string.prompt_set_doctor));
        }

        if (btnAddWater != null) {
            btnAddWater.setOnClickListener(v -> {
                Context context = getContext();
                if (context == null) return;
                SharedPreferences prefs = context.getSharedPreferences("health_buddy_prefs", Context.MODE_PRIVATE);
                int goal = prefs.getInt("key_water_goal", 8);
                int count = prefs.getInt(getTodayWaterKey(), 0);
                int newCount = count + 1;
                prefs.edit().putInt(getTodayWaterKey(), newCount).apply();
                updateWaterTrackerUI();
                if (newCount == goal) {
                    Toast.makeText(context, "🎉 Excellent! You reached your " + goal + "-glass water goal!", Toast.LENGTH_SHORT).show();
                } else if (newCount > goal) {
                    Toast.makeText(context, "🎉 Outstanding! You exceeded your " + goal + "-glass goal (" + newCount + " glasses drunk)! 💧", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, "Great job staying hydrated! 💧", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (btnResetWater != null) {
            btnResetWater.setOnClickListener(v -> {
                Context context = getContext();
                if (context == null) return;
                SharedPreferences prefs = context.getSharedPreferences("health_buddy_prefs", Context.MODE_PRIVATE);
                prefs.edit().putInt(getTodayWaterKey(), 0).apply();
                updateWaterTrackerUI();
            });
        }

        if (btnEditGoal != null) {
            btnEditGoal.setOnClickListener(v -> {
                Context context = getContext();
                if (context == null) return;
                SharedPreferences prefs = context.getSharedPreferences("health_buddy_prefs", Context.MODE_PRIVATE);
                int currentGoal = prefs.getInt("key_water_goal", 8);

                TextInputEditText input = new TextInputEditText(context);
                input.setInputType(InputType.TYPE_CLASS_NUMBER);
                input.setText(String.valueOf(currentGoal));

                new AlertDialog.Builder(context)
                        .setTitle("Set Daily Water Goal (Glasses)")
                        .setView(input)
                        .setPositiveButton("Save", (dialog, which) -> {
                            String str = input.getText() != null ? input.getText().toString().trim() : "";
                            try {
                                int newGoal = Integer.parseInt(str);
                                if (newGoal > 0 && newGoal <= 30) {
                                    prefs.edit().putInt("key_water_goal", newGoal).apply();
                                    updateWaterTrackerUI();
                                    Toast.makeText(context, "Water goal updated to " + newGoal + " glasses", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(context, "Please enter a valid goal (1-30)", Toast.LENGTH_SHORT).show();
                                }
                            } catch (Exception e) {
                                Toast.makeText(context, "Invalid number", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }

        if (btnSilenceAlarm != null) {
            btnSilenceAlarm.setOnClickListener(v -> {
                Context context = getContext();
                if (context != null) {
                    Intent stopServiceIntent = new Intent(context.getApplicationContext(), AlarmRingtoneService.class);
                    stopServiceIntent.setAction(AlarmRingtoneService.ACTION_STOP_ALARM);
                    context.startService(stopServiceIntent);
                    Toast.makeText(context, "Alarm silenced", Toast.LENGTH_SHORT).show();
                }
            });
        }

        alarmStateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateSilenceButtonVisibility();
            }
        };

        updateGreeting();
        updateSilenceButtonVisibility();
        updateWaterTrackerUI();

        return root;
    }

    private String getTodayWaterKey() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        return "water_count_" + sdf.format(new Date());
    }

    private void updateWaterTrackerUI() {
        Context context = getContext();
        if (context == null || textWaterProgress == null || progressWater == null) return;
        SharedPreferences prefs = context.getSharedPreferences("health_buddy_prefs", Context.MODE_PRIVATE);

        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        String lastSavedDate = prefs.getString("key_water_last_date", "");

        // Automatic 12 AM Reset if date changed
        if (!today.equals(lastSavedDate)) {
            prefs.edit()
                    .putInt(getTodayWaterKey(), 0)
                    .putString("key_water_last_date", today)
                    .apply();
        }

        int goal = prefs.getInt("key_water_goal", 8);
        int count = prefs.getInt(getTodayWaterKey(), 0);

        progressWater.setMax(goal);
        progressWater.setProgress(count);
        textWaterProgress.setText(getString(R.string.water_tracker_progress, count, goal));
    }

    private void makeSpeedDialCall(String prefKey, int promptResId) {
        Context context = getContext();
        if (context == null) return;
        SharedPreferences prefs = context.getSharedPreferences("health_buddy_prefs", Context.MODE_PRIVATE);
        String phone = prefs.getString(prefKey, "").trim();

        if (!phone.isEmpty()) {
            Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone));
            startActivity(intent);
        } else {
            Toast.makeText(context, promptResId, Toast.LENGTH_LONG).show();
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateToSettingsTab();
            }
        }
    }

    private void updateSilenceButtonVisibility() {
        if (btnSilenceAlarm != null) {
            btnSilenceAlarm.setVisibility(AlarmRingtoneService.isRinging() ? View.VISIBLE : View.GONE);
        }
    }

    private void updateGreeting() {
        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);

        if (hour >= 5 && hour < 12) {
            textWelcome.setText("Good Morning 👋");
        } else if (hour >= 12 && hour < 17) {
            textWelcome.setText("Good Afternoon 👋");
        } else if (hour >= 17 && hour < 21) {
            textWelcome.setText("Good Evening 👋");
        } else {
            textWelcome.setText("Good Night 👋");
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        updateGreeting();
        updateSilenceButtonVisibility();
        updateWaterTrackerUI();
        attachRealtimeListeners();
        registerAlarmReceiver();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateSilenceButtonVisibility();
        updateWaterTrackerUI();
    }

    @Override
    public void onStop() {
        super.onStop();
        detachRealtimeListeners();
        unregisterAlarmReceiver();
    }

    @Override
    public void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
        }
        super.onDestroy();
    }

    private void registerAlarmReceiver() {
        if (getContext() != null && alarmStateReceiver != null) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(AlarmRingtoneService.ACTION_ALARM_STARTED);
            filter.addAction(AlarmRingtoneService.ACTION_ALARM_STOPPED);
            try {
                ContextCompat.registerReceiver(
                        getContext(),
                        alarmStateReceiver,
                        filter,
                        ContextCompat.RECEIVER_NOT_EXPORTED
                );
            } catch (Exception e) {
                // Ignore if already registered
            }
        }
    }

    private void unregisterAlarmReceiver() {
        if (getContext() != null && alarmStateReceiver != null) {
            try {
                getContext().unregisterReceiver(alarmStateReceiver);
            } catch (Exception ignored) {}
        }
    }

    private void detachRealtimeListeners() {
        if (medicinesListener != null) {
            medicinesListener.remove();
            medicinesListener = null;
        }
        if (historyListener != null) {
            historyListener.remove();
            historyListener = null;
        }
    }

    private void attachRealtimeListeners() {
        detachRealtimeListeners();

        Query query = FirestoreHelper.getAllMedicinesQuery();
        if (query == null) return;

        medicinesListener = query.addSnapshotListener((queryDocumentSnapshots, e) -> {
            if (e != null || queryDocumentSnapshots == null) {
                return;
            }

            List<Medicine> medicines = new ArrayList<>();
            for (DocumentSnapshot doc : queryDocumentSnapshots) {
                Medicine m = doc.toObject(Medicine.class);
                if (m != null) {
                    m.setId(doc.getId());
                    medicines.add(m);
                }
            }

            fetchHistoryAndUpdateList(medicines);
        });
    }

    private void fetchHistoryAndUpdateList(List<Medicine> allMedicines) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        long startOfDay = cal.getTimeInMillis();
        
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        long endOfDay = cal.getTimeInMillis();

        if (FirestoreHelper.getHistoryRef() == null) return;

        if (historyListener != null) historyListener.remove();

        historyListener = FirestoreHelper.getHistoryRef()
                .whereGreaterThanOrEqualTo("takenTime", startOfDay)
                .whereLessThanOrEqualTo("takenTime", endOfDay)
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    if (e != null || queryDocumentSnapshots == null) return;

                    List<String> takenMedicineIds = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        MedicationHistory history = doc.toObject(MedicationHistory.class);
                        if (history != null) {
                            takenMedicineIds.add(history.getMedicineId());
                        }
                    }

                    List<Medicine> remainingMedicines = new ArrayList<>();
                    Context context = getContext();

                    for (Medicine m : allMedicines) {
                        if (!takenMedicineIds.contains(m.getId())) {
                            remainingMedicines.add(m);
                            if (context != null) {
                                AlarmScheduler.scheduleAlarmsForMedicine(context.getApplicationContext(), m);
                            }
                        } else {
                            if (context != null) {
                                AlarmScheduler.cancelAlarm(context.getApplicationContext(), AlarmScheduler.getNotificationId(m));
                            }
                        }
                    }

                    int takenCount = takenMedicineIds.size();
                    int totalCount = allMedicines.size();

                    if (isAdded()) {
                        if (totalCount == 0) {
                            textSubtitle.setText(R.string.welcome_subtitle);
                        } else if (remainingMedicines.isEmpty()) {
                            textSubtitle.setText("🎉 All medicines taken for today!");
                        } else {
                            String summary = getString(R.string.summary_medicines, takenCount, remainingMedicines.size());
                            textSubtitle.setText(summary);
                        }
                    }

                    if (remainingMedicines.isEmpty()) {
                        emptyState.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                    } else {
                        emptyState.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);
                        recyclerView.setAdapter(new MedicineAdapter(remainingMedicines, tts));
                    }
                });
    }

    private static class MedicineAdapter extends RecyclerView.Adapter<MedicineAdapter.ViewHolder> {
        private final List<Medicine> medicines;
        private final TextToSpeech tts;

        MedicineAdapter(List<Medicine> medicines, TextToSpeech tts) {
            this.medicines = medicines;
            this.tts = tts;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_medicine_card, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Medicine medicine = medicines.get(position);
            holder.textName.setText(medicine.getName());
            String details = medicine.getDosage() + (medicine.getQuantity() != null && !medicine.getQuantity().isEmpty() ? " • " + medicine.getQuantity() : "");
            holder.textDetails.setText(details);
            holder.textTime.setText(medicine.getMedicineTime());
            holder.textFood.setText(medicine.getFoodRelation());

            // Bind Low Stock Badge
            int stock = medicine.getRemainingStock();
            if (stock <= 0) {
                holder.textLowStock.setVisibility(View.VISIBLE);
                holder.textLowStock.setText(R.string.badge_out_of_stock);
            } else if (stock <= 3) {
                holder.textLowStock.setVisibility(View.VISIBLE);
                holder.textLowStock.setText(holder.itemView.getContext().getString(R.string.badge_low_stock, stock));
            } else {
                holder.textLowStock.setVisibility(View.GONE);
            }

            // Speak Button TextToSpeech
            if (holder.btnSpeak != null) {
                holder.btnSpeak.setOnClickListener(v -> {
                    if (tts != null) {
                        String text = "Medicine " + medicine.getName() + ", dosage " + medicine.getDosage() + ", " + medicine.getFoodRelation() + ", time " + medicine.getMedicineTime();
                        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "MED_TTS_ID");
                    } else {
                        Toast.makeText(holder.itemView.getContext(), medicine.getName() + ": " + medicine.getDosage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }

            // Info Dialog
            if (holder.btnCardInfo != null) {
                holder.btnCardInfo.setOnClickListener(v -> {
                    MedicineInfoDialog dialog = new MedicineInfoDialog(holder.itemView.getContext(), medicine.getName());
                    dialog.show();
                });
            }

            // Bind Overflow Menu Options
            holder.btnMore.setOnClickListener(v -> {
                Context context = holder.itemView.getContext();
                PopupMenu popup = new PopupMenu(context, holder.btnMore);
                popup.getMenu().add(0, 1, 0, R.string.action_edit);
                popup.getMenu().add(0, 2, 1, R.string.action_delete);
                popup.setOnMenuItemClickListener(item -> {
                    if (item.getItemId() == 1) { // Edit
                        EditMedicineDialog dialog = new EditMedicineDialog(context, medicine, () -> {});
                        dialog.show();
                        return true;
                    } else if (item.getItemId() == 2) { // Delete
                        new AlertDialog.Builder(context)
                                .setTitle(R.string.action_delete)
                                .setMessage(context.getString(R.string.confirm_delete_msg, medicine.getName()))
                                .setPositiveButton(R.string.action_delete, (d, w) -> {
                                    FirestoreHelper.deleteMedicine(medicine.getId());
                                    AlarmScheduler.cancelAlarm(context.getApplicationContext(), AlarmScheduler.getNotificationId(medicine));
                                    Toast.makeText(context, "Medicine deleted", Toast.LENGTH_SHORT).show();
                                })
                                .setNegativeButton(R.string.btn_cancel, null)
                                .show();
                        return true;
                    }
                    return false;
                });
                popup.show();
            });

            holder.btnTake.setText(R.string.label_take);
            holder.btnTake.setEnabled(true);
            holder.btnTake.setAlpha(1.0f);
            holder.btnTake.setOnClickListener(v -> {
                // Decrement stock
                medicine.setRemainingStock(Math.max(0, medicine.getRemainingStock() - 1));
                FirestoreHelper.updateMedicine(medicine);

                MedicationHistory entry = new MedicationHistory();
                entry.setMedicineId(medicine.getId());
                entry.setMedicineName(medicine.getName());
                entry.setScheduledTime(System.currentTimeMillis());
                entry.setTakenTime(System.currentTimeMillis());
                entry.setStatus("TAKEN");
                
                Context context = holder.itemView.getContext();
                var task = FirestoreHelper.addHistory(entry);
                if (task != null) {
                    task.addOnSuccessListener(aVoid -> {
                        // Cancel pending alarms for this medicine
                        AlarmScheduler.cancelAlarm(context.getApplicationContext(), AlarmScheduler.getNotificationId(medicine));

                        // Stop active alarm ringtone service if currently ringing
                        Intent stopServiceIntent = new Intent(context.getApplicationContext(), AlarmRingtoneService.class);
                        stopServiceIntent.setAction(AlarmRingtoneService.ACTION_STOP_ALARM);
                        context.startService(stopServiceIntent);

                        String msg = context.getString(R.string.msg_marked_taken, medicine.getName());
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
                    });
                }
            });
        }

        @Override
        public int getItemCount() {
            return medicines.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView textName, textDetails, textTime, textFood, textLowStock;
            ImageButton btnMore, btnSpeak, btnCardInfo;
            Button btnTake;

            ViewHolder(View itemView) {
                super(itemView);
                textName = itemView.findViewById(R.id.text_medicine_name);
                textDetails = itemView.findViewById(R.id.text_medicine_details);
                textTime = itemView.findViewById(R.id.text_medicine_time);
                textFood = itemView.findViewById(R.id.text_food_relation);
                textLowStock = itemView.findViewById(R.id.text_low_stock);
                btnMore = itemView.findViewById(R.id.btn_more);
                btnSpeak = itemView.findViewById(R.id.btn_speak);
                btnCardInfo = itemView.findViewById(R.id.btn_card_info);
                btnTake = itemView.findViewById(R.id.btn_take);
            }
        }
    }
}
