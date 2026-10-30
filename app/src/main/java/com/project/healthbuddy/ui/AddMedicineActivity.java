package com.project.healthbuddy.ui;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.project.healthbuddy.R;
import com.project.healthbuddy.ai.MedicineAiKnowledge;
import com.project.healthbuddy.data.FirestoreHelper;
import com.project.healthbuddy.data.Medicine;
import com.project.healthbuddy.notification.AlarmScheduler;

import java.util.Calendar;
import java.util.Locale;

public class AddMedicineActivity extends AppCompatActivity {

    private TextInputEditText editName, editDosage, editQuantity;
    private MaterialButtonToggleGroup toggleFrequency;
    private Button btnTimePicker, btnSave;
    private AutoCompleteTextView dropdownFood;
    private ProgressBar progressBar;
    private View cardAiInfo;
    private TextView textAiSummary;
    
    private String selectedTime = "10:00 AM";
    private final String[] foodOptions = {"Before Food", "After Food", "With Food", "No Food Relation"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_medicine);

        setupToolbar();
        initViews();
        setupPickers();
        setupAiPreview();
        
        btnSave.setOnClickListener(v -> saveMedicine());
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(v -> finish());
        }
    }

    private void initViews() {
        editName = findViewById(R.id.edit_medicine_name);
        editDosage = findViewById(R.id.edit_dosage);
        editQuantity = findViewById(R.id.edit_quantity);
        toggleFrequency = findViewById(R.id.toggle_frequency);
        btnTimePicker = findViewById(R.id.btn_time_picker);
        btnSave = findViewById(R.id.btn_save_medicine);
        dropdownFood = findViewById(R.id.dropdown_food_relation);
        progressBar = findViewById(R.id.progress_save);
        cardAiInfo = findViewById(R.id.card_ai_info);
        textAiSummary = findViewById(R.id.text_ai_summary);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, foodOptions);
        dropdownFood.setAdapter(adapter);
    }

    private void setupAiPreview() {
        if (editName != null) {
            editName.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    String name = s != null ? s.toString().trim() : "";
                    if (name.length() >= 3 && cardAiInfo != null && textAiSummary != null) {
                        MedicineAiKnowledge.MedicineSummary summary = MedicineAiKnowledge.getSummary(name);
                        textAiSummary.setText(summary.getFullSummary());
                        cardAiInfo.setVisibility(View.VISIBLE);
                    } else if (cardAiInfo != null) {
                        cardAiInfo.setVisibility(View.GONE);
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }
    }

    private void setupPickers() {
        btnTimePicker.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            int hour = c.get(Calendar.HOUR_OF_DAY);
            int minute = c.get(Calendar.MINUTE);

            TimePickerDialog timePickerDialog = new TimePickerDialog(this, (view, hourOfDay, minuteOfHour) -> {
                String amPm = (hourOfDay < 12) ? "AM" : "PM";
                int displayHour = (hourOfDay > 12) ? hourOfDay - 12 : (hourOfDay == 0 ? 12 : hourOfDay);
                selectedTime = String.format(Locale.US, "%02d:%02d %s", displayHour, minuteOfHour, amPm);
                btnTimePicker.setText(selectedTime);
            }, hour, minute, false);
            timePickerDialog.show();
        });
    }

    private void saveMedicine() {
        String name = editName.getText() != null ? editName.getText().toString().trim() : "";
        String dosage = editDosage.getText() != null ? editDosage.getText().toString().trim() : "";
        String quantity = editQuantity.getText() != null ? editQuantity.getText().toString().trim() : "";
        String food = dropdownFood.getText().toString();
        
        int checkedId = toggleFrequency.getCheckedButtonId();
        String frequency = "Once a day";
        if (checkedId == R.id.btn_twice) frequency = "Twice a day";
        else if (checkedId == R.id.btn_thrice) frequency = "Three times a day";

        if (name.isEmpty() || dosage.isEmpty()) {
            Toast.makeText(this, "Please enter name and dosage", Toast.LENGTH_SHORT).show();
            return;
        }

        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        btnSave.setEnabled(false);

        Medicine medicine = new Medicine();
        medicine.setName(name);
        medicine.setDosage(dosage);
        medicine.setQuantity(quantity);
        medicine.setFrequency(frequency);
        medicine.setMedicineTime(selectedTime);
        medicine.setFoodRelation(food);
        medicine.setStartDate(System.currentTimeMillis());
        
        var task = FirestoreHelper.addMedicine(medicine);
        if (task == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            if (progressBar != null) progressBar.setVisibility(View.GONE);
            btnSave.setEnabled(true);
            return;
        }

        // Schedule local notification immediately using the assigned client ID
        try {
            AlarmScheduler.scheduleAlarmsForMedicine(AddMedicineActivity.this, medicine);
        } catch (Exception e) {
            Log.e("AddMedicineActivity", "Error scheduling alarm", e);
        }

        // Complete save locally & navigate back immediately
        Toast.makeText(AddMedicineActivity.this, "Medicine saved successfully", Toast.LENGTH_SHORT).show();
        finish();
    }
}
