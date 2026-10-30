package com.project.healthbuddy.ui.home;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.material.textfield.TextInputEditText;
import com.project.healthbuddy.R;
import com.project.healthbuddy.data.FirestoreHelper;
import com.project.healthbuddy.data.Medicine;
import com.project.healthbuddy.notification.AlarmScheduler;

import java.util.Calendar;
import java.util.Locale;

public class EditMedicineDialog extends Dialog {

    public interface OnMedicineUpdatedListener {
        void onUpdated();
    }

    private final Medicine medicine;
    private final OnMedicineUpdatedListener listener;
    private String selectedTime;

    private TextInputEditText editName, editDosage, editStock;
    private Button btnTimePicker;
    private AutoCompleteTextView dropdownFood;

    private final String[] foodOptions = {"Before Food", "After Food", "With Food", "No Food Relation"};

    public EditMedicineDialog(@NonNull Context context, Medicine medicine, OnMedicineUpdatedListener listener) {
        super(context);
        this.medicine = medicine;
        this.listener = listener;
        this.selectedTime = medicine != null && medicine.getMedicineTime() != null ? medicine.getMedicineTime() : "10:00 AM";
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_edit_medicine);

        if (getWindow() != null) {
            getWindow().setLayout(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        editName = findViewById(R.id.edit_dialog_name);
        editDosage = findViewById(R.id.edit_dialog_dosage);
        editStock = findViewById(R.id.edit_dialog_stock);
        btnTimePicker = findViewById(R.id.btn_dialog_time);
        dropdownFood = findViewById(R.id.dropdown_dialog_food);

        Button btnCancel = findViewById(R.id.btn_dialog_cancel);
        Button btnSave = findViewById(R.id.btn_dialog_save);

        if (medicine != null) {
            editName.setText(medicine.getName());
            editDosage.setText(medicine.getDosage());
            editStock.setText(String.valueOf(medicine.getRemainingStock()));
            btnTimePicker.setText(selectedTime);
            dropdownFood.setText(medicine.getFoodRelation(), false);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, foodOptions);
        dropdownFood.setAdapter(adapter);

        btnTimePicker.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            int hour = c.get(Calendar.HOUR_OF_DAY);
            int minute = c.get(Calendar.MINUTE);

            TimePickerDialog timePickerDialog = new TimePickerDialog(getContext(), (view, hourOfDay, minuteOfHour) -> {
                String amPm = (hourOfDay < 12) ? "AM" : "PM";
                int displayHour = (hourOfDay > 12) ? hourOfDay - 12 : (hourOfDay == 0 ? 12 : hourOfDay);
                selectedTime = String.format(Locale.US, "%02d:%02d %s", displayHour, minuteOfHour, amPm);
                btnTimePicker.setText(selectedTime);
            }, hour, minute, false);
            timePickerDialog.show();
        });

        if (btnCancel != null) btnCancel.setOnClickListener(v -> dismiss());
        if (btnSave != null) btnSave.setOnClickListener(v -> saveChanges());
    }

    private void saveChanges() {
        if (medicine == null) return;

        String name = editName.getText() != null ? editName.getText().toString().trim() : "";
        String dosage = editDosage.getText() != null ? editDosage.getText().toString().trim() : "";
        String stockStr = editStock.getText() != null ? editStock.getText().toString().trim() : "10";
        String food = dropdownFood.getText().toString();

        if (name.isEmpty() || dosage.isEmpty()) {
            Toast.makeText(getContext(), "Please enter name and dosage", Toast.LENGTH_SHORT).show();
            return;
        }

        int stock = 10;
        try {
            stock = Integer.parseInt(stockStr);
        } catch (Exception ignored) {}

        medicine.setName(name);
        medicine.setDosage(dosage);
        medicine.setRemainingStock(stock);
        medicine.setMedicineTime(selectedTime);
        medicine.setFoodRelation(food);

        var task = FirestoreHelper.updateMedicine(medicine);
        if (task != null) {
            task.addOnSuccessListener(aVoid -> {
                AlarmScheduler.scheduleAlarmsForMedicine(getContext().getApplicationContext(), medicine);
                Toast.makeText(getContext(), "Medicine updated successfully", Toast.LENGTH_SHORT).show();
                if (listener != null) listener.onUpdated();
                dismiss();
            }).addOnFailureListener(e -> {
                Toast.makeText(getContext(), "Failed to update: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        }
    }
}
