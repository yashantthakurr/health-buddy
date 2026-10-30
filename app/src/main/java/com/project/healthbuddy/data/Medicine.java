package com.project.healthbuddy.data;

import com.google.firebase.firestore.Exclude;

public class Medicine {
    private String id;
    private String name;
    private String dosage;
    private String quantity;
    private String frequency;
    private String medicineTime;
    private String foodRelation;
    private int remainingStock = 10;
    private long startDate;
    private long endDate;
    private long createdAt;

    // Required for Firestore
    public Medicine() {}

    @Exclude
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDosage() { return dosage; }
    public void setDosage(String dosage) { this.dosage = dosage; }

    public String getQuantity() { return quantity; }
    public void setQuantity(String quantity) { 
        this.quantity = quantity;
        if (remainingStock <= 0 || remainingStock == 10) {
            this.remainingStock = parseNumericStock(quantity);
        }
    }

    public String getFrequency() { return frequency; }
    public void setFrequency(String frequency) { this.frequency = frequency; }

    public String getMedicineTime() { return medicineTime; }
    public void setMedicineTime(String medicineTime) { this.medicineTime = medicineTime; }

    public String getFoodRelation() { return foodRelation; }
    public void setFoodRelation(String foodRelation) { this.foodRelation = foodRelation; }

    public int getRemainingStock() { return remainingStock; }
    public void setRemainingStock(int remainingStock) { this.remainingStock = Math.max(0, remainingStock); }

    public long getStartDate() { return startDate; }
    public void setStartDate(long startDate) { this.startDate = startDate; }

    public long getEndDate() { return endDate; }
    public void setEndDate(long endDate) { this.endDate = endDate; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    private static int parseNumericStock(String q) {
        if (q == null || q.trim().isEmpty()) return 10;
        try {
            String digits = q.replaceAll("[^0-9]", "");
            if (!digits.isEmpty()) {
                int parsed = Integer.parseInt(digits);
                if (parsed > 0) return parsed;
            }
        } catch (Exception ignored) {}
        return 10;
    }
}
