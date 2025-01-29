package com.example.jugajug;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Objects;

public class LoginActivity extends AppCompatActivity {
    private TextInputEditText v_loginemail, v_loginpassword;
    private MaterialButton v_login, v_createAcc;
    private FirebaseAuth firebaseAuth;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initializeViews();
        setupFirebase();
        setupClickListeners();
    }

    private void initializeViews() {
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(getResources().getColor(R.color.black));

        v_loginemail = findViewById(R.id.loginemail);
        v_loginpassword = findViewById(R.id.loginpassword);
        v_login = findViewById(R.id.login);
        v_createAcc = findViewById(R.id.createAcc);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Logging in...");
        progressDialog.setCancelable(false);
    }

    private void setupFirebase() {
        firebaseAuth = FirebaseAuth.getInstance();
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();

        if (firebaseUser != null && firebaseUser.isEmailVerified()) {
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
        }
    }

    private void setupClickListeners() {
        v_createAcc.setOnClickListener(view ->
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class))
        );

        v_login.setOnClickListener(view -> {
            String email = Objects.requireNonNull(v_loginemail.getText()).toString().trim();
            String password = Objects.requireNonNull(v_loginpassword.getText()).toString().trim();
            loginUser(email, password);
        });
    }

    private void loginUser(String email, String password) {
        if (!isValidEmail(email)) {
            v_loginemail.setError("Invalid email format");
            return;
        }

        if (password.length() < 8) {
            v_loginpassword.setError("Password must be at least 8 characters");
            return;
        }

        progressDialog.show();
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    progressDialog.dismiss();
                    if (task.isSuccessful()) {
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        if (user != null && user.isEmailVerified()) {
                            Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(LoginActivity.this, SearchUserActivity.class));
                            finish();
                        } else {
                            Toast.makeText(this, "Please verify your email", Toast.LENGTH_LONG).show();
                            firebaseAuth.signOut();
                        }
                    } else {
                        String errorMessage = task.getException() != null
                                ? task.getException().getLocalizedMessage()
                                : "Authentication failed";
                        Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private boolean isValidEmail(String email) {
        return email != null && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }
}