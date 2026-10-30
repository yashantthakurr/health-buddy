package com.project.healthbuddy.data;

import com.google.firebase.firestore.Exclude;

public class MedicationHistory {
    private String id;
    private String medicineId;
    private String medicineName;
    private long scheduledTime;
    private long takenTime;
    private String status; // TAKEN, MISSED

    public MedicationHistory() {}

    @Exclude
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getMedicineId() { return medicineId; }
    public void setMedicineId(String medicineId) { this.medicineId = medicineId; }

    public String getMedicineName() { return medicineName; }
    public void setMedicineName(String medicineName) { this.medicineName = medicineName; }

    public long getScheduledTime() { return scheduledTime; }
    public void setScheduledTime(long scheduledTime) { this.scheduledTime = scheduledTime; }

    public long getTakenTime() { return takenTime; }
    public void setTakenTime(long takenTime) { this.takenTime = takenTime; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
