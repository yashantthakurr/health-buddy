package com.project.healthbuddy.ui.prescription;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.project.healthbuddy.R;
import com.project.healthbuddy.ai.MedicineAiKnowledge;

public class MedicineInfoDialog extends Dialog {

    private final String medicineName;

    public MedicineInfoDialog(@NonNull Context context, String medicineName) {
        super(context);
        this.medicineName = medicineName != null ? medicineName : "Medicine";
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_medicine_info);

        TextView textTitle = findViewById(R.id.text_dialog_title);
        TextView textPurpose = findViewById(R.id.text_dialog_purpose);
        TextView textPrecautions = findViewById(R.id.text_dialog_precautions);
        Button btnClose = findViewById(R.id.btn_dialog_close);

        textTitle.setText(medicineName);

        MedicineAiKnowledge.MedicineSummary summary = MedicineAiKnowledge.getSummary(medicineName);
        
        if (summary.isValidMedicine()) {
            String purposeText = summary.getWhyTaken() + "\n• " + summary.getWhoFor();
            textPurpose.setText(purposeText);
            textPrecautions.setText(summary.getPrecaution());
        } else {
            textPurpose.setText("Purpose: Cannot show AI insights for this unverified entry.");
            textPrecautions.setText("Precautions: Please verify the medicine name or consult your doctor/pharmacist.");
        }

        btnClose.setOnClickListener(v -> dismiss());
    }
}
