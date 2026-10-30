package com.project.healthbuddy.ui.settings;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.project.healthbuddy.R;
import com.project.healthbuddy.ui.auth.LoginActivity;

public class SettingsFragment extends Fragment {

    private TextView textName, textEmail;
    private Button btnSignOut;
    private RadioGroup radioGroupTheme;
    private FirebaseAuth mAuth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_settings, container, false);

        mAuth = FirebaseAuth.getInstance();
        initViews(root);
        displayUserProfile();
        setupLanguageSelection(root);
        setupThemeSelection();
        setupSpeedDialContacts(root);

        return root;
    }

    private void initViews(View root) {
        textName = root.findViewById(R.id.text_user_name);
        textEmail = root.findViewById(R.id.text_user_email);
        btnSignOut = root.findViewById(R.id.btn_sign_out);
        radioGroupTheme = root.findViewById(R.id.radio_group_theme);

        TextView btnDisclaimer = root.findViewById(R.id.btn_medical_disclaimer);

        btnSignOut.setOnClickListener(v -> signOut());

        btnDisclaimer.setOnClickListener(v -> new AlertDialog.Builder(requireContext())
                .setTitle("Medical Safety Disclaimer")
                .setMessage(R.string.medical_disclaimer_text)
                .setPositiveButton("I Understand", (dialog, which) -> dialog.dismiss())
                .show());
    }

    private void displayUserProfile() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String name = user.getDisplayName();
            String email = user.getEmail();
            textName.setText(name != null && !name.isEmpty() ? name : "Health Buddy User");
            textEmail.setText(email);
        }
    }

    private void setupLanguageSelection(View root) {
        RadioGroup radioGroupLang = root.findViewById(R.id.radio_group_language);
        if (radioGroupLang == null) return;

        String currentLang = LocaleHelper.getLanguage(requireContext());
        if ("hi".equalsIgnoreCase(currentLang)) {
            radioGroupLang.check(R.id.radio_lang_hindi);
        } else {
            radioGroupLang.check(R.id.radio_lang_english);
        }

        radioGroupLang.setOnCheckedChangeListener((group, checkedId) -> {
            String targetLang = (checkedId == R.id.radio_lang_hindi) ? "hi" : "en";
            if (!targetLang.equalsIgnoreCase(LocaleHelper.getLanguage(requireContext()))) {
                LocaleHelper.setLocale(requireContext(), targetLang);
                if (getActivity() != null) {
                    getActivity().recreate();
                }
            }
        });
    }

    private void setupThemeSelection() {
        int savedTheme = ThemeManager.getSavedTheme(requireContext());
        if (savedTheme == ThemeManager.THEME_LIGHT) {
            radioGroupTheme.check(R.id.radio_theme_light);
        } else if (savedTheme == ThemeManager.THEME_DARK) {
            radioGroupTheme.check(R.id.radio_theme_dark);
        } else {
            radioGroupTheme.check(R.id.radio_theme_light);
        }

        radioGroupTheme.setOnCheckedChangeListener((group, checkedId) -> {
            int mode;
            if (checkedId == R.id.radio_theme_light) mode = ThemeManager.THEME_LIGHT;
            else if (checkedId == R.id.radio_theme_dark) mode = ThemeManager.THEME_DARK;
            else mode = ThemeManager.THEME_LIGHT;
            
            ThemeManager.saveTheme(requireContext(), mode);
        });
    }

    private void setupSpeedDialContacts(View root) {
        SharedPreferences prefs = requireContext().getSharedPreferences("health_buddy_prefs", Context.MODE_PRIVATE);
        TextInputEditText editCaregiver = root.findViewById(R.id.edit_caregiver_phone);
        TextInputEditText editDoctor = root.findViewById(R.id.edit_doctor_phone);
        Button btnSave = root.findViewById(R.id.btn_save_contacts);
        Button btnCancel = root.findViewById(R.id.btn_cancel_contacts);
        ImageButton btnEdit = root.findViewById(R.id.btn_edit_contacts);

        if (editCaregiver != null) {
            editCaregiver.setText(prefs.getString("key_caregiver_phone", ""));
            editCaregiver.setEnabled(false);
        }
        if (editDoctor != null) {
            editDoctor.setText(prefs.getString("key_doctor_phone", ""));
            editDoctor.setEnabled(false);
        }
        if (btnSave != null) btnSave.setVisibility(View.GONE);
        if (btnCancel != null) btnCancel.setVisibility(View.GONE);

        if (btnEdit != null) {
            btnEdit.setOnClickListener(v -> {
                if (editCaregiver != null) editCaregiver.setEnabled(true);
                if (editDoctor != null) editDoctor.setEnabled(true);
                if (btnSave != null) btnSave.setVisibility(View.VISIBLE);
                if (btnCancel != null) btnCancel.setVisibility(View.VISIBLE);
                if (editCaregiver != null) editCaregiver.requestFocus();
            });
        }

        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> {
                if (editCaregiver != null) {
                    editCaregiver.setText(prefs.getString("key_caregiver_phone", ""));
                    editCaregiver.setEnabled(false);
                }
                if (editDoctor != null) {
                    editDoctor.setText(prefs.getString("key_doctor_phone", ""));
                    editDoctor.setEnabled(false);
                }
                if (btnSave != null) btnSave.setVisibility(View.GONE);
                btnCancel.setVisibility(View.GONE);
            });
        }

        if (btnSave != null) {
            btnSave.setOnClickListener(v -> {
                String cPhone = editCaregiver != null && editCaregiver.getText() != null ? editCaregiver.getText().toString().trim() : "";
                String dPhone = editDoctor != null && editDoctor.getText() != null ? editDoctor.getText().toString().trim() : "";
                prefs.edit()
                        .putString("key_caregiver_phone", cPhone)
                        .putString("key_doctor_phone", dPhone)
                        .apply();

                if (editCaregiver != null) editCaregiver.setEnabled(false);
                if (editDoctor != null) editDoctor.setEnabled(false);
                btnSave.setVisibility(View.GONE);
                if (btnCancel != null) btnCancel.setVisibility(View.GONE);

                Toast.makeText(requireContext(), "Speed Dial Contacts updated", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void signOut() {
        mAuth.signOut();
        
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();
        GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso);
        googleSignInClient.signOut().addOnCompleteListener(task -> {
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            if (getActivity() != null) getActivity().finish();
        });
    }
}
