package com.project.healthbuddy.ai;

import android.os.Parcel;
import android.os.Parcelable;

public class MedicineExtractionResult implements Parcelable {
    private String name;
    private String dosage;
    private String quantity;
    private String frequency;
    private String medicineTime;
    private String foodRelation;

    public MedicineExtractionResult() {}

    protected MedicineExtractionResult(Parcel in) {
        name = in.readString();
        dosage = in.readString();
        quantity = in.readString();
        frequency = in.readString();
        medicineTime = in.readString();
        foodRelation = in.readString();
    }

    public static final Creator<MedicineExtractionResult> CREATOR = new Creator<MedicineExtractionResult>() {
        @Override
        public MedicineExtractionResult createFromParcel(Parcel in) {
            return new MedicineExtractionResult(in);
        }

        @Override
        public MedicineExtractionResult[] newArray(int size) {
            return new MedicineExtractionResult[size];
        }
    };

    @Override
    public int describeContents() { return 0; }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(dosage);
        dest.writeString(quantity);
        dest.writeString(frequency);
        dest.writeString(medicineTime);
        dest.writeString(foodRelation);
    }

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDosage() { return dosage; }
    public void setDosage(String dosage) { this.dosage = dosage; }

    public String getQuantity() { return quantity; }
    public void setQuantity(String quantity) { this.quantity = quantity; }

    public String getFrequency() { return frequency; }
    public void setFrequency(String frequency) { this.frequency = frequency; }

    public String getMedicineTime() { return medicineTime; }
    public void setMedicineTime(String medicineTime) { this.medicineTime = medicineTime; }

    public String getFoodRelation() { return foodRelation; }
    public void setFoodRelation(String foodRelation) { this.foodRelation = foodRelation; }
}
