package com.project.healthbuddy.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.project.healthbuddy.MainActivity;
import com.project.healthbuddy.R;

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText editEmail, editPassword, editConfirmPassword;
    private Button btnRegister, btnGoogle;
    private ProgressBar progressBar;
    private TextView textGoToLogin;

    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;

    private final ActivityResultLauncher<Intent> googleSignInLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                    try {
                        GoogleSignInAccount account = task.getResult(ApiException.class);
                        firebaseAuthWithGoogle(account.getIdToken());
                    } catch (ApiException e) {
                        Toast.makeText(this, "Google sign in failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        initViews();
    }

    private void initViews() {
        editEmail = findViewById(R.id.edit_email);
        editPassword = findViewById(R.id.edit_password);
        editConfirmPassword = findViewById(R.id.edit_confirm_password);
        btnRegister = findViewById(R.id.btn_register);
        btnGoogle = findViewById(R.id.btn_google_sign_in);
        progressBar = findViewById(R.id.progress_register);
        textGoToLogin = findViewById(R.id.text_go_to_login);

        btnRegister.setOnClickListener(v -> createAccount());
        btnGoogle.setOnClickListener(v -> {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            googleSignInLauncher.launch(signInIntent);
        });
        textGoToLogin.setOnClickListener(v -> finish());
    }

    private void createAccount() {
        String email = editEmail.getText().toString().trim();
        String password = editPassword.getText().toString().trim();
        String confirmPassword = editConfirmPassword.getText().toString().trim();

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editEmail.setError("Valid email is required");
            return;
        }
        if (password.length() < 6) {
            editPassword.setError("Password must be at least 6 characters");
            return;
        }
        if (!password.equals(confirmPassword)) {
            editConfirmPassword.setError("Passwords do not match");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnRegister.setVisibility(View.GONE);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    progressBar.setVisibility(View.GONE);
                    btnRegister.setVisibility(View.VISIBLE);
                    if (task.isSuccessful()) {
                        startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                        finishAffinity();
                    } else {
                        Toast.makeText(RegisterActivity.this, "Registration failed: " + 
                                (task.getException() != null ? task.getException().getMessage() : ""), 
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void firebaseAuthWithGoogle(String idToken) {
        progressBar.setVisibility(View.VISIBLE);
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                        finishAffinity();
                    } else {
                        Toast.makeText(RegisterActivity.this, "Firebase Authentication with Google failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
