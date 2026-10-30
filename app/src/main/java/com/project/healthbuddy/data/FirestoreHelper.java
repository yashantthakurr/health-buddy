package com.project.healthbuddy.data;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

public class FirestoreHelper {

    private static FirebaseFirestore getDb() {
        return FirebaseFirestore.getInstance();
    }

    private static FirebaseAuth getAuth() {
        return FirebaseAuth.getInstance();
    }

    private static String getUid() {
        if (getAuth().getCurrentUser() != null) {
            return getAuth().getCurrentUser().getUid();
        }
        return null;
    }

    public static CollectionReference getMedicinesRef() {
        String uid = getUid();
        if (uid == null) return null;
        return getDb().collection("users").document(uid).collection("medicines");
    }

    public static CollectionReference getHistoryRef() {
        String uid = getUid();
        if (uid == null) return null;
        return getDb().collection("users").document(uid).collection("medicationHistory");
    }

    public static Task<Void> addMedicine(Medicine medicine) {
        CollectionReference ref = getMedicinesRef();
        if (ref == null) return null;
        String id = ref.document().getId();
        medicine.setId(id);
        medicine.setCreatedAt(System.currentTimeMillis());
        return ref.document(id).set(medicine);
    }

    public static Task<Void> updateMedicine(Medicine medicine) {
        CollectionReference ref = getMedicinesRef();
        if (ref == null) return null;
        return ref.document(medicine.getId()).set(medicine);
    }

    public static Task<Void> deleteMedicine(String id) {
        CollectionReference ref = getMedicinesRef();
        if (ref == null) return null;
        return ref.document(id).delete();
    }

    public static Query getAllMedicinesQuery() {
        CollectionReference ref = getMedicinesRef();
        if (ref == null) return null;
        return ref.orderBy("medicineTime", Query.Direction.ASCENDING);
    }

    public static Task<Void> addHistory(MedicationHistory history) {
        CollectionReference ref = getHistoryRef();
        if (ref == null) return null;
        String id = ref.document().getId();
        history.setId(id);
        return ref.document(id).set(history);
    }

    public static Query getAllHistoryQuery() {
        CollectionReference ref = getHistoryRef();
        if (ref == null) return null;
        return ref.orderBy("takenTime", Query.Direction.DESCENDING);
    }
}
