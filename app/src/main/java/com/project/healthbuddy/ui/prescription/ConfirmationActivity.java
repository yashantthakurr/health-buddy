package com.project.healthbuddy.ui.prescription;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.project.healthbuddy.R;
import com.project.healthbuddy.ai.MedicineExtractionResult;
import com.project.healthbuddy.data.FirestoreHelper;
import com.project.healthbuddy.data.Medicine;
import com.project.healthbuddy.notification.AlarmScheduler;

import java.util.ArrayList;
import java.util.List;

public class ConfirmationActivity extends AppCompatActivity {

    private List<MedicineExtractionResult> medicines;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirmation);

        medicines = getIntent().getParcelableArrayListExtra("results");
        if (medicines == null) medicines = new ArrayList<>();

        setupToolbar();
        initViews();
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar_confirmation);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        }
    }

    private void initViews() {
        RecyclerView recyclerView = findViewById(R.id.recycler_extracted_medicines);
        Button btnSave = findViewById(R.id.btn_confirm_all);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new ExtractionAdapter(medicines));

        btnSave.setOnClickListener(v -> saveAllMedicines());
    }

    private void saveAllMedicines() {
        if (medicines.isEmpty()) {
            finish();
            return;
        }

        for (MedicineExtractionResult res : medicines) {
            Medicine m = new Medicine();
            m.setName(res.getName());
            m.setDosage(res.getDosage());
            m.setQuantity(res.getQuantity() != null ? res.getQuantity() : "1 tablet");
            m.setFrequency(res.getFrequency());
            m.setMedicineTime(res.getMedicineTime());
            m.setFoodRelation(res.getFoodRelation());
            m.setStartDate(System.currentTimeMillis());

            FirestoreHelper.addMedicine(m).addOnSuccessListener(aVoid -> {
                AlarmScheduler.scheduleAlarmsForMedicine(ConfirmationActivity.this, m);
            });
        }
        Toast.makeText(this, "Medicines saved successfully", Toast.LENGTH_SHORT).show();
        finish();
    }

    private static class ExtractionAdapter extends RecyclerView.Adapter<ExtractionAdapter.ViewHolder> {
        private final List<MedicineExtractionResult> list;

        ExtractionAdapter(List<MedicineExtractionResult> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_extracted_medicine, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            MedicineExtractionResult item = list.get(position);
            holder.editName.setText(item.getName());
            holder.editDosage.setText(item.getDosage());
            holder.editFrequency.setText(item.getFrequency());
            holder.editTime.setText(item.getMedicineTime());
            holder.editFood.setText(item.getFoodRelation());

            holder.editName.addTextChangedListener(new SimpleTextWatcher(item::setName));
            holder.editDosage.addTextChangedListener(new SimpleTextWatcher(item::setDosage));
            holder.editFrequency.addTextChangedListener(new SimpleTextWatcher(item::setFrequency));
            holder.editTime.addTextChangedListener(new SimpleTextWatcher(item::setMedicineTime));
            holder.editFood.addTextChangedListener(new SimpleTextWatcher(item::setFoodRelation));

            holder.btnInfo.setOnClickListener(v -> {
                MedicineInfoDialog dialog = new MedicineInfoDialog(holder.itemView.getContext(), item.getName());
                dialog.show();
            });
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextInputEditText editName, editDosage, editFrequency, editTime, editFood;
            Button btnInfo;

            ViewHolder(View itemView) {
                super(itemView);
                editName = itemView.findViewById(R.id.edit_extracted_name);
                editDosage = itemView.findViewById(R.id.edit_extracted_dosage);
                editFrequency = itemView.findViewById(R.id.edit_extracted_frequency);
                editTime = itemView.findViewById(R.id.edit_extracted_time);
                editFood = itemView.findViewById(R.id.edit_extracted_food);
                btnInfo = itemView.findViewById(R.id.btn_learn_info);
            }
        }
    }

    private interface OnTextChanged {
        void onChanged(String s);
    }

    private static class SimpleTextWatcher implements TextWatcher {
        private final OnTextChanged callback;
        SimpleTextWatcher(OnTextChanged callback) { this.callback = callback; }
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}
        @Override
        public void afterTextChanged(Editable s) { callback.onChanged(s.toString()); }
    }
}
