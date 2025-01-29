package com.example.jugajug;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.jugajug.model.UserModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class RegisterActivity extends AppCompatActivity {
    private TextInputEditText v_registerEmail, v_registerPassword,
            v_firstName, v_lastName, v_phoneNumber;
    private MaterialButton v_register, v_gotoLogin;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        initializeViews();
        setupFirebase();
        setupClickListeners();
    }

    private void initializeViews() {
        v_firstName = findViewById(R.id.fName);
        v_lastName = findViewById(R.id.lName);
        v_registerEmail = findViewById(R.id.signinemail);
        v_phoneNumber = findViewById(R.id.phoneNumber);
        v_registerPassword = findViewById(R.id.signinpassword);
        v_register = findViewById(R.id.sign_up_btn);
        v_gotoLogin = findViewById(R.id.gotologin);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Registering...");
        progressDialog.setCancelable(false);
    }

    private void setupFirebase() {
        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
    }

    private void setupClickListeners() {
        v_register.setOnClickListener(view -> registerUser());
        v_gotoLogin.setOnClickListener(view -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void registerUser() {
        String firstName = v_firstName.getText().toString().trim();
        String lastName = v_lastName.getText().toString().trim();
        String email = v_registerEmail.getText().toString().trim();
        String phoneNumber = v_phoneNumber.getText().toString().trim();
        String password = v_registerPassword.getText().toString().trim();

        if (validateInputs(firstName, lastName, email, phoneNumber, password)) {
            progressDialog.show();
            firebaseAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        progressDialog.dismiss();
                        if (task.isSuccessful()) {
                            FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                            if (firebaseUser != null) {
                                firebaseUser.sendEmailVerification()
                                        .addOnCompleteListener(verificationTask -> {
                                            if (verificationTask.isSuccessful()) {
                                                saveUserToFirestore(firebaseUser.getUid(),
                                                        firstName, lastName,
                                                        email, phoneNumber);
                                            } else {
                                                Toast.makeText(RegisterActivity.this,
                                                        "Failed to send verification email",
                                                        Toast.LENGTH_SHORT).show();
                                            }
                                        });
                            }
                        } else {
                            Toast.makeText(RegisterActivity.this,
                                    "Registration Failed: " + task.getException().getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private boolean validateInputs(String firstName, String lastName, String email,
                                   String phoneNumber, String password) {
        boolean isValid = true;

        if (!isValidName(firstName)) {
            v_firstName.setError("Invalid first name");
            isValid = false;
        }

        if (!isValidName(lastName)) {
            v_lastName.setError("Invalid last name");
            isValid = false;
        }

        if (!isValidEmail(email)) {
            v_registerEmail.setError("Invalid email format");
            isValid = false;
        }

        if (!isValidPhoneNumber(phoneNumber)) {
            v_phoneNumber.setError("Invalid phone number");
            isValid = false;
        }

        if (!isStrongPassword(password)) {
            v_registerPassword.setError("Weak password. Use 8+ chars with uppercase, lowercase, number");
            isValid = false;
        }

        return isValid;
    }

    private void saveUserToFirestore(String userId, String firstName, String lastName,
                                     String email, String phoneNumber) {
        List<String> searchKeywords = UserModel.generateSearchKeywords(
                firstName, lastName, email, phoneNumber
        );

        Map<String, Object> userMap = new HashMap<>();
        userMap.put("userId", userId);
        userMap.put("username", firstName + " " + lastName);
        userMap.put("firstName", firstName);
        userMap.put("lastName", lastName);
        userMap.put("email", email);
        userMap.put("phone", phoneNumber);
        userMap.put("fcmToken", "");
        userMap.put("searchKeywords", searchKeywords);
        userMap.put("createdTimestamp", com.google.firebase.Timestamp.now());

        firestore.collection("users").document(userId)
                .set(userMap)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(RegisterActivity.this,
                                "Registration Successful. Please verify your email.",
                                Toast.LENGTH_LONG).show();
                        startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                        finish();
                    } else {
                        Toast.makeText(RegisterActivity.this,
                                "Failed to save user data",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private boolean isValidEmail(String email) {
        return email != null && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private boolean isValidPhoneNumber(String phone) {
        return phone != null && phone.matches("^[+]?[0-9]{10,13}$");
    }

    private boolean isStrongPassword(String password) {
        return password != null &&
                password.length() >= 8 &&
                Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$").matcher(password).matches();
    }

    private boolean isValidName(String name) {
        return name != null && name.length() >= 2 && name.matches("^[A-Za-z]+$");
    }
}