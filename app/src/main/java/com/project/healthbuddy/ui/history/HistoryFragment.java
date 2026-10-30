package com.project.healthbuddy.ui.history;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import com.project.healthbuddy.R;
import com.project.healthbuddy.data.FirestoreHelper;
import com.project.healthbuddy.data.MedicationHistory;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryFragment extends Fragment {

    private RecyclerView recyclerView;
    private ListenerRegistration historyListener;
    private final List<MedicationHistory> currentHistoryList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_history, container, false);

        recyclerView = root.findViewById(R.id.recycler_history);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        Button btnExportPdf = root.findViewById(R.id.btn_export_pdf);
        if (btnExportPdf != null) {
            btnExportPdf.setOnClickListener(v -> exportPdfReport());
        }

        return root;
    }

    @Override
    public void onStart() {
        super.onStart();
        attachRealtimeHistoryListener();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (historyListener != null) {
            historyListener.remove();
            historyListener = null;
        }
    }

    private void attachRealtimeHistoryListener() {
        if (historyListener != null) {
            historyListener.remove();
            historyListener = null;
        }

        Query query = FirestoreHelper.getAllHistoryQuery();
        if (query == null) return;

        historyListener = query.addSnapshotListener((queryDocumentSnapshots, e) -> {
            if (e != null || queryDocumentSnapshots == null) return;

            currentHistoryList.clear();
            for (DocumentSnapshot doc : queryDocumentSnapshots) {
                MedicationHistory h = doc.toObject(MedicationHistory.class);
                if (h != null) {
                    h.setId(doc.getId());
                    currentHistoryList.add(h);
                }
            }
            recyclerView.setAdapter(new HistoryAdapter(currentHistoryList));
        });
    }

    private void exportPdfReport() {
        Context context = getContext();
        if (context == null) return;

        if (currentHistoryList.isEmpty()) {
            Toast.makeText(context, "No medication history available to export", Toast.LENGTH_SHORT).show();
            return;
        }

        PdfDocument document = new PdfDocument();
        int pageWidth = 595; // A4 width in points
        int pageHeight = 842; // A4 height in points
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        Paint titlePaint = new Paint();
        titlePaint.setColor(Color.rgb(0, 121, 107)); // Teal #00796B
        titlePaint.setTextSize(22);
        titlePaint.setFakeBoldText(true);

        Paint headerPaint = new Paint();
        headerPaint.setColor(Color.BLACK);
        headerPaint.setTextSize(12);
        headerPaint.setFakeBoldText(true);

        Paint textPaint = new Paint();
        textPaint.setColor(Color.DKGRAY);
        textPaint.setTextSize(11);

        Paint linePaint = new Paint();
        linePaint.setColor(Color.LTGRAY);
        linePaint.setStrokeWidth(1);

        int y = 40;
        canvas.drawText("Health Buddy — Medication Adherence Report", 30, y, titlePaint);
        
        y += 24;
        SimpleDateFormat fullFormat = new SimpleDateFormat("MMMM dd, yyyy 'at' hh:mm a", Locale.US);
        canvas.drawText("Generated on: " + fullFormat.format(new Date()), 30, y, textPaint);

        y += 20;
        canvas.drawLine(30, y, pageWidth - 30, y, linePaint);

        // Summary Stats
        y += 26;
        int total = currentHistoryList.size();
        int taken = 0;
        for (MedicationHistory h : currentHistoryList) {
            if ("TAKEN".equalsIgnoreCase(h.getStatus())) taken++;
        }
        int missed = total - taken;
        int complianceRate = total > 0 ? (taken * 100 / total) : 100;

        canvas.drawText("ADHERENCE SUMMARY", 30, y, headerPaint);
        y += 18;
        canvas.drawText("Total Recorded Doses: " + total + "  |  Taken: " + taken + "  |  Missed: " + missed + "  |  Adherence Rate: " + complianceRate + "%", 30, y, textPaint);

        y += 20;
        canvas.drawLine(30, y, pageWidth - 30, y, linePaint);

        // Hydration & Water Intake Summary Section
        y += 26;
        canvas.drawText("HYDRATION & WATER INTAKE REPORT", 30, y, headerPaint);
        y += 18;

        SharedPreferences prefs = context.getSharedPreferences("health_buddy_prefs", Context.MODE_PRIVATE);
        int targetGoal = prefs.getInt("key_water_goal", 8);
        SimpleDateFormat dateFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        String todayKey = "water_count_" + dateFmt.format(new Date());
        int todayDrunk = prefs.getInt(todayKey, 0);

        String waterStatusStr = todayDrunk >= targetGoal ? "Goal Achieved (" + (todayDrunk * 100 / Math.max(1, targetGoal)) + "%)" : "In Progress (" + (todayDrunk * 100 / Math.max(1, targetGoal)) + "%)";
        canvas.drawText("Daily Target Goal: " + targetGoal + " Glasses  |  Drunk Today: " + todayDrunk + " Glasses  |  Status: " + waterStatusStr, 30, y, textPaint);

        y += 20;
        canvas.drawLine(30, y, pageWidth - 30, y, linePaint);

        // History Log Table
        y += 26;
        canvas.drawText("MEDICATION LOG", 30, y, headerPaint);
        y += 20;

        // Table Column Headers
        canvas.drawText("Medicine Name", 30, y, headerPaint);
        canvas.drawText("Status", 230, y, headerPaint);
        canvas.drawText("Date & Time", 360, y, headerPaint);
        y += 8;
        canvas.drawLine(30, y, pageWidth - 30, y, linePaint);

        SimpleDateFormat rowDateFormat = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.US);

        for (MedicationHistory h : currentHistoryList) {
            y += 22;
            if (y > pageHeight - 60) break; // Keep within page 1 bounds

            String name = h.getMedicineName() != null ? h.getMedicineName() : "Medicine";
            String status = "TAKEN".equalsIgnoreCase(h.getStatus()) ? "TAKEN" : "MISSED";
            String timeStr = rowDateFormat.format(new Date(h.getTakenTime() > 0 ? h.getTakenTime() : h.getScheduledTime()));

            canvas.drawText(name, 30, y, textPaint);
            canvas.drawText(status, 230, y, textPaint);
            canvas.drawText(timeStr, 360, y, textPaint);
        }

        y += 30;
        canvas.drawLine(30, y, pageWidth - 30, y, linePaint);
        y += 20;
        canvas.drawText("Medical Disclaimer: Health Buddy reports are for informational adherence tracking.", 30, y, textPaint);

        document.finishPage(page);

        // Save PDF File to Cache
        try {
            File pdfFile = new File(context.getCacheDir(), "HealthBuddy_Adherence_Report.pdf");
            FileOutputStream fos = new FileOutputStream(pdfFile);
            document.writeTo(fos);
            document.close();
            fos.close();

            Uri pdfUri = FileProvider.getUriForFile(context, "com.project.healthbuddy.fileprovider", pdfFile);

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("application/pdf");
            shareIntent.putExtra(Intent.EXTRA_STREAM, pdfUri);
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Health Buddy Medication Adherence Report");
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(shareIntent, "Share Adherence Report via"));

        } catch (Exception ex) {
            Log.e("HistoryFragment", "Error exporting PDF report", ex);
            Toast.makeText(context, "Failed to generate PDF: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private static class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {
        private final List<MedicationHistory> historyList;
        private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault());

        HistoryAdapter(List<MedicationHistory> historyList) {
            this.historyList = historyList;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            MedicationHistory history = historyList.get(position);
            holder.textName.setText(history.getMedicineName());
            
            String timeStr = holder.itemView.getContext().getString(R.string.history_taken_at, dateFormat.format(new Date(history.getTakenTime())));
            holder.textTime.setText(timeStr);

            if ("TAKEN".equals(history.getStatus())) {
                holder.imgStatus.setImageResource(android.R.drawable.checkbox_on_background);
                holder.imgStatus.setColorFilter(holder.itemView.getContext().getColor(R.color.green_taken));
                holder.textStatus.setTextColor(holder.itemView.getContext().getColor(R.color.green_taken));
                holder.textStatus.setText(R.string.label_taken);
            } else {
                holder.imgStatus.setImageResource(android.R.drawable.ic_delete);
                holder.imgStatus.setColorFilter(holder.itemView.getContext().getColor(R.color.red_error));
                holder.textStatus.setTextColor(holder.itemView.getContext().getColor(R.color.red_error));
                holder.textStatus.setText(R.string.history_missed);
            }
        }

        @Override
        public int getItemCount() {
            return historyList.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView textName, textTime, textStatus;
            ImageView imgStatus;

            ViewHolder(View itemView) {
                super(itemView);
                textName = itemView.findViewById(R.id.text_history_name);
                textTime = itemView.findViewById(R.id.text_history_time);
                textStatus = itemView.findViewById(R.id.text_history_status);
                imgStatus = itemView.findViewById(R.id.img_status);
            }
        }
    }
}
