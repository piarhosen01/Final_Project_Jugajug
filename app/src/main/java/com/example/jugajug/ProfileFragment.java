package com.example.jugajug;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.jugajug.model.UserModel;
import com.example.jugajug.utils.AndroidUtil;
import com.example.jugajug.utils.FirebaseUtil;
import com.github.dhaval2404.imagepicker.ImagePicker;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.storage.UploadTask;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;

public class ProfileFragment extends Fragment {

    ImageView profilePic;
    EditText usernameInput;
    EditText phoneInput;
    Button updateProfileBtn;
    ProgressBar progressBar;
    TextView logoutBtn;

    static UserModel cachedUserModel; // Cached user data
    static Uri cachedProfilePicUri;   // Cached profile picture URI

    UserModel currentUserModel;
    ActivityResultLauncher<Intent> imagePickLauncher;
    Uri selectedImageUri;

    public ProfileFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        imagePickLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null && data.getData() != null) {
                            selectedImageUri = data.getData();
                            AndroidUtil.setProfilePic(getContext(), selectedImageUri, profilePic);
                        }
                    }
                }
        );
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        profilePic = view.findViewById(R.id.profile_image_view);
        usernameInput = view.findViewById(R.id.profile_username);
        phoneInput = view.findViewById(R.id.profile_phone);
        updateProfileBtn = view.findViewById(R.id.profle_update_btn);
        progressBar = view.findViewById(R.id.profile_progress_bar);
        logoutBtn = view.findViewById(R.id.logout_btn);

        if (cachedUserModel != null && cachedProfilePicUri != null) {
            // Use cached data
            setUserData(cachedUserModel, cachedProfilePicUri);
        } else {
            // Fetch data from server if not cached
            getUserData();
        }

        updateProfileBtn.setOnClickListener((v -> {
            updateBtnClick();
        }));

        logoutBtn.setOnClickListener((v) -> {
            setInProgress(true); // Show progress bar during logout
            FirebaseMessaging.getInstance().deleteToken().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    // Clear Firebase Authentication session
                    FirebaseUtil.logout();

                    // Clear cached user data
                    cachedUserModel = null;
                    cachedProfilePicUri = null;

                    // Navigate to the login screen
                    Intent intent = new Intent(getContext(), SplashActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    setInProgress(false); // Hide progress bar
                    startActivity(intent);
                } else {
                    setInProgress(false); // Hide progress bar
                    AndroidUtil.showToast(getContext(), "Logout failed. Please try again.");
                }
            });
        });

        profilePic.setOnClickListener((v) -> {
            ImagePicker.with(this).cropSquare().compress(512).maxResultSize(512, 512)
                    .createIntent(intent -> {
                        imagePickLauncher.launch(intent);
                        return null;
                    });
        });

        return view;
    }

    void updateBtnClick() {
        String newUsername = usernameInput.getText().toString();
        if (newUsername.isEmpty() || newUsername.length() < 3) {
            usernameInput.setError("Username length should be at least 3 chars");
            return;
        }

        if (currentUserModel != null) {
            currentUserModel.setUsername(newUsername);
            setInProgress(true);

            if (selectedImageUri != null) {
                FirebaseUtil.getCurrentProfilePicStorageRef().putFile(selectedImageUri)
                        .addOnCompleteListener(task -> updateToFirestore());
            } else {
                updateToFirestore();
            }
        } else {
            Log.e("ProfileFragment", "currentUserModel is null");
        }
    }

    void updateToFirestore() {
        FirebaseUtil.currentUserDetails().set(currentUserModel)
                .addOnCompleteListener(task -> {
                    setInProgress(false);
                    if (task.isSuccessful()) {
                        cachedUserModel = currentUserModel; // Update cached data
                        if (selectedImageUri != null) {
                            cachedProfilePicUri = selectedImageUri;
                        }
                        AndroidUtil.showToast(getContext(), "Updated successfully");
                    } else {
                        AndroidUtil.showToast(getContext(), "Update failed");
                    }
                });
    }

    void getUserData() {
        setInProgress(true);

        FirebaseUtil.getCurrentProfilePicStorageRef().getDownloadUrl()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Uri uri = task.getResult();
                        cachedProfilePicUri = uri; // Cache profile picture URI
                        AndroidUtil.setProfilePic(getContext(), uri, profilePic);
                    } else {
                        Log.e("ProfileFragment", "Failed to get profile picture URL", task.getException());
                    }
                });

        FirebaseUtil.currentUserDetails().get().addOnCompleteListener(task -> {
            setInProgress(false);
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document != null && document.exists()) {
                    currentUserModel = document.toObject(UserModel.class);
                    if (currentUserModel != null) {
                        cachedUserModel = currentUserModel; // Cache user data
                        setUserData(currentUserModel, cachedProfilePicUri);
                    } else {
                        Log.e("ProfileFragment", "UserModel is null");
                        AndroidUtil.showToast(getContext(), "Failed to load user data");
                    }
                } else {
                    Log.e("ProfileFragment", "Document does not exist");
                    AndroidUtil.showToast(getContext(), "User data not found");
                }
            } else {
                Log.e("ProfileFragment", "Task failed with exception: ", task.getException());
                AndroidUtil.showToast(getContext(), "Failed to retrieve user data");
            }
        });
    }

    void setUserData(UserModel userModel, Uri profilePicUri) {
        usernameInput.setText(userModel.getUsername());
        phoneInput.setText(userModel.getPhone());
        if (profilePicUri != null) {
            AndroidUtil.setProfilePic(getContext(), profilePicUri, profilePic);
        }
    }

    void setInProgress(boolean inProgress) {
        if (inProgress) {
            progressBar.setVisibility(View.VISIBLE);
            updateProfileBtn.setVisibility(View.GONE);
        } else {
            progressBar.setVisibility(View.GONE);
            updateProfileBtn.setVisibility(View.VISIBLE);
        }
    }
}










