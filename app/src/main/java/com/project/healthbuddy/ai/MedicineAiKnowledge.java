package com.project.healthbuddy.ai;

import java.util.Locale;

/**
 * AI knowledge provider providing 2-3 line non-diagnostic summaries explaining
 * why a medicine is taken, who it is for, and key safety precautions for recognized medical drugs.
 */
public class MedicineAiKnowledge {

    public static class MedicineSummary {
        private final boolean validMedicine;
        private final String whyTaken;
        private final String whoFor;
        private final String precaution;

        public MedicineSummary(boolean validMedicine, String whyTaken, String whoFor, String precaution) {
            this.validMedicine = validMedicine;
            this.whyTaken = whyTaken;
            this.whoFor = whoFor;
            this.precaution = precaution;
        }

        public boolean isValidMedicine() { return validMedicine; }
        public String getWhyTaken() { return whyTaken; }
        public String getWhoFor() { return whoFor; }
        public String getPrecaution() { return precaution; }

        public String getFullSummary() {
            if (!validMedicine) {
                return "⚠️ Cannot show AI insights for this entry. Please verify the medicine name.";
            }
            return "• " + whyTaken + "\n• " + whoFor + "\n• " + precaution;
        }
    }

    public static MedicineSummary getSummary(String medicineName) {
        if (medicineName == null || medicineName.trim().isEmpty()) {
            return new MedicineSummary(
                    false,
                    "Cannot show AI insights for this entry.",
                    "Unverified medicine name.",
                    "Please verify the medicine name or consult your doctor/pharmacist."
            );
        }

        String lower = medicineName.toLowerCase(Locale.US);

        if (lower.contains("paracetamol") || lower.contains("crocin") || lower.contains("dolo") || lower.contains("acetaminophen")) {
            return new MedicineSummary(
                    true,
                    "Why Taken: Relieves mild to moderate pain and reduces fever.",
                    "Who For: Prescribed for patients with fever, headaches, or body aches.",
                    "Precaution: Do not exceed 4000mg daily and avoid taking multiple paracetamol products."
            );
        } else if (lower.contains("metformin") || lower.contains("glycomet")) {
            return new MedicineSummary(
                    true,
                    "Why Taken: Regulates and lowers blood glucose (sugar) levels.",
                    "Who For: Prescribed for individuals with Type 2 Diabetes.",
                    "Precaution: Take with or immediately after meals to reduce stomach upset."
            );
        } else if (lower.contains("amlodipine") || lower.contains("atelol") || lower.contains("losartan") || lower.contains("telmisartan") || lower.contains("enalapril") || lower.contains("ramipril") || lower.contains("metoprolol") || lower.contains("furosemide") || lower.contains("lasix")) {
            return new MedicineSummary(
                    true,
                    "Why Taken: Relaxes blood vessels to lower high blood pressure.",
                    "Who For: Prescribed for adults managing hypertension or cardiac health.",
                    "Precaution: Stand up slowly from sitting or lying down if feeling dizzy."
            );
        } else if (lower.contains("amoxicillin") || lower.contains("azithromycin") || lower.contains("ciprofloxacin") || lower.contains("augmentin") || lower.contains("cefixime") || lower.contains("ofloxacin") || lower.contains("doxycycline") || lower.contains("cephalexin")) {
            return new MedicineSummary(
                    true,
                    "Why Taken: Antibiotic that destroys harmful bacterial infections.",
                    "Who For: Prescribed for patients fighting respiratory, throat, or urinary bacterial infections.",
                    "Precaution: Complete the full prescribed course even if feeling better early."
            );
        } else if (lower.contains("omeprazole") || lower.contains("pantoprazole") || lower.contains("rabeprazole") || lower.contains("ranitidine") || lower.contains("famotidine") || lower.contains("gelusil") || lower.contains("digene")) {
            return new MedicineSummary(
                    true,
                    "Why Taken: Reduces stomach acid to treat heartburn and acidity.",
                    "Who For: Prescribed for patients with acid reflux (GERD) or indigestion.",
                    "Precaution: Best taken 30 to 60 minutes before breakfast with water."
            );
        } else if (lower.contains("atorvastatin") || lower.contains("rosuvastatin") || lower.contains("simvastatin")) {
            return new MedicineSummary(
                    true,
                    "Why Taken: Lowers 'bad' LDL cholesterol and protects cardiovascular health.",
                    "Who For: Prescribed for individuals with high cholesterol or heart health needs.",
                    "Precaution: Usually taken once daily in the evening as directed."
            );
        } else if (lower.contains("aspirin") || lower.contains("clopidogrel") || lower.contains("ecosprin")) {
            return new MedicineSummary(
                    true,
                    "Why Taken: Blood thinner that prevents harmful blood clots from forming.",
                    "Who For: Prescribed for heart condition management or stroke prevention.",
                    "Precaution: Take after meals with plenty of water to protect stomach."
            );
        } else if (lower.contains("ibuprofen") || lower.contains("naproxen") || lower.contains("diclofenac") || lower.contains("combiflam") || lower.contains("aceclofenac") || lower.contains("mefenamic")) {
            return new MedicineSummary(
                    true,
                    "Why Taken: Anti-inflammatory medication reducing pain and joint swelling.",
                    "Who For: Prescribed for patients with arthritis, joint pain, or muscle soreness.",
                    "Precaution: Take with food or milk to avoid stomach irritation."
            );
        } else if (lower.contains("cetirizine") || lower.contains("levocetirizine") || lower.contains("allegra") || lower.contains("fexofenadine") || lower.contains("montelukast")) {
            return new MedicineSummary(
                    true,
                    "Why Taken: Relieves allergy symptoms such as sneezing, runny nose, and itching.",
                    "Who For: Prescribed for individuals with seasonal allergies or hives.",
                    "Precaution: May cause mild drowsiness; take at bedtime if feeling sleepy."
            );
        } else if (lower.contains("vitamin") || lower.contains("calcium") || lower.contains("b12") || lower.contains("folic") || lower.contains("zinc") || lower.contains("iron") || lower.contains("ferrous") || lower.contains("becosules") || lower.contains("evion")) {
            return new MedicineSummary(
                    true,
                    "Why Taken: Nutritional supplement restoring essential vitamins and minerals.",
                    "Who For: Prescribed for individuals with vitamin deficiencies or bone health needs.",
                    "Precaution: Take consistently as recommended by your doctor."
            );
        } else if (lower.contains("levothyroxine") || lower.contains("thyronorm") || lower.contains("eltroxin") || lower.contains("gabapentin") || lower.contains("pregabalin") || lower.contains("prednisolone") || lower.contains("dexamethasone")) {
            return new MedicineSummary(
                    true,
                    "Why Taken: Specialized prescription medication for hormone or nerve health management.",
                    "Who For: Prescribed for patients with diagnosed endocrine, inflammatory, or nerve conditions.",
                    "Precaution: Take strictly at scheduled times as directed by your doctor."
            );
        } else {
            // Unrecognized / random string entry
            return new MedicineSummary(
                    false,
                    "Cannot show AI insights for this entry.",
                    "Unverified medicine name.",
                    "Please verify the medicine name or consult your doctor/pharmacist."
            );
        }
    }
}
