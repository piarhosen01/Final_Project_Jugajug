package com.example.jugajug;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.jugajug.adapter.ChatRecyclerAdapter;
import com.example.jugajug.model.ChatMessageModel;
import com.example.jugajug.model.UserModel;
import com.example.jugajug.utils.AndroidUtil;
import com.example.jugajug.utils.FirebaseUtil;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {
    private static final int MESSAGE_LIMIT = 50;
    private static final String TIMESTAMP_FIELD = "timestamp";
    private static final String LAST_MESSAGE_TIMESTAMP = "lastMessageTimestamp";
    private static final String LAST_MESSAGE_SENDER_ID = "lastMessageSenderId";
    private static final String USER_IDS = "userIds";

    private UserModel otherUser;
    private UserModel currentUser;
    private String chatroomId;
    private ChatRecyclerAdapter adapter;
    private boolean isActivityActive = true;

    private EditText messageInput;
    private ImageButton sendMessageBtn;
    private ImageButton backBtn;
    private TextView otherUsername;
    private RecyclerView recyclerView;
    private ImageView profileImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        initializeViews();
        setupChatRoom();
        getCurrentUser();
    }

    private void initializeViews() {
        otherUser = AndroidUtil.getUserModelFromIntent(getIntent());
        if (otherUser == null || otherUser.getUserId() == null) {
            Toast.makeText(this, "Error loading user data", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        chatroomId = FirebaseUtil.getChatroomId(FirebaseUtil.currentUserId(), otherUser.getUserId());
        bindViews();
        setupUI();
    }

    private void bindViews() {
        messageInput = findViewById(R.id.chat_message_input);
        sendMessageBtn = findViewById(R.id.message_send_btn);
        backBtn = findViewById(R.id.back_btn);
        otherUsername = findViewById(R.id.other_username);
        recyclerView = findViewById(R.id.chat_recycler_view);
        profileImage = findViewById(R.id.profile_pic_image_view);
    }

    private void setupUI() {
        otherUsername.setText(otherUser.getUsername());
        backBtn.setOnClickListener(v -> onBackPressed());
        setupProfileImage();
        setupSendButton();
        setupChatRecyclerView();
    }

    private void setupSendButton() {
        sendMessageBtn.setOnClickListener(v -> {
            String message = messageInput.getText().toString().trim();
            if (!message.isEmpty()) {
                sendMessage(message);
            }
        });
    }

    private void getCurrentUser() {
        FirebaseUtil.currentUserDetails()
                .get()
                .addOnCompleteListener(task -> {
                    if (!isActivityActive) return;

                    if (task.isSuccessful() && task.getResult() != null) {
                        currentUser = task.getResult().toObject(UserModel.class);
                    } else {
                        currentUser = createFallbackUser();
                    }
                });
    }

    private UserModel createFallbackUser() {
        String currentUserId = FirebaseUtil.currentUserId();
        //    public UserModel(String phone, String firstName, String lastName,
        //                     Timestamp createdTimestamp, String userId) {
        //        this.phone = phone;
        //        this.firstName = firstName;
        //        this.lastName = lastName;
        //        this.createdTimestamp = createdTimestamp;
        //        this.userId = userId;
        //    }
        return new UserModel(
                "1234567890",
                "Unknown",
                "User",
                Timestamp.now(),
                currentUserId
        );
    }

    private void setupChatRoom() {
        if (chatroomId == null || !isActivityActive) return;

        FirebaseUtil.getChatroomReference(chatroomId)
                .get()
                .addOnCompleteListener(task -> {
                    if (!isActivityActive || !task.isSuccessful()) return;
                    updateChatroomData();
                });
    }

    private void updateChatroomData() {
        Map<String, Object> chatroomData = new HashMap<>();
        chatroomData.put(LAST_MESSAGE_TIMESTAMP, Timestamp.now());
        chatroomData.put(LAST_MESSAGE_SENDER_ID, FirebaseUtil.currentUserId());
        chatroomData.put(USER_IDS, Arrays.asList(
                FirebaseUtil.currentUserId(),
                otherUser.getUserId()
        ));

        FirebaseUtil.getChatroomReference(chatroomId)
                .set(chatroomData, SetOptions.merge())
                .addOnFailureListener(e -> {
                    if (!isActivityActive) return;
                    showError("Failed to update chat room");
                });
    }

    private void sendMessage(String message) {
        if (message == null || message.trim().isEmpty() || chatroomId == null) return;

        sendMessageBtn.setEnabled(false);
        ChatMessageModel chatMessage = createChatMessage(message);

        FirebaseUtil.getChatroomMessageReference(chatroomId)
                .add(chatMessage)
                .addOnCompleteListener(task -> handleMessageSent(task.isSuccessful()));
    }

    private ChatMessageModel createChatMessage(String message) {
        return new ChatMessageModel(
                message.trim(),
                FirebaseUtil.currentUserId(),
                Timestamp.now()
        );
    }

    private void handleMessageSent(boolean isSuccessful) {
        if (!isActivityActive) return;

        sendMessageBtn.setEnabled(true);
        if (isSuccessful) {
            messageInput.setText("");
        } else {
            showError("Failed to send message");
        }
    }

    private void setupProfileImage() {
        if (otherUser == null || otherUser.getUserId() == null) {
            setDefaultProfileImage();
            return;
        }

        loadProfileImage();
    }

    private void loadProfileImage() {
        FirebaseUtil.getOtherProfilePicStorageRef(otherUser.getUserId())
                .getMetadata()
                .addOnSuccessListener(metadata -> {
                    if (!isActivityActive) return;
                    downloadProfileImage();
                })
                .addOnFailureListener(e -> {
                    if (!isActivityActive) return;
                    setDefaultProfileImage();
                });
    }

    private void downloadProfileImage() {
        FirebaseUtil.getOtherProfilePicStorageRef(otherUser.getUserId())
                .getDownloadUrl()
                .addOnSuccessListener(uri -> {
                    if (!isActivityActive) return;
                    AndroidUtil.setProfilePic(getApplicationContext(), uri, profileImage);
                })
                .addOnFailureListener(e -> {
                    if (!isActivityActive) return;
                    setDefaultProfileImage();
                });
    }

    private void setDefaultProfileImage() {
        profileImage.setImageResource(R.drawable.baseline_account_circle_24);
    }

    private void setupChatRecyclerView() {
        if (chatroomId == null) return;

        Query query = FirebaseUtil.getChatroomMessageReference(chatroomId)
                .orderBy(TIMESTAMP_FIELD, Query.Direction.DESCENDING)
                .limit(MESSAGE_LIMIT);

        FirestoreRecyclerOptions<ChatMessageModel> options = new FirestoreRecyclerOptions.Builder<ChatMessageModel>()
                .setQuery(query, ChatMessageModel.class)
                .build();

        setupRecyclerAdapter(options);
    }

    private void setupRecyclerAdapter(FirestoreRecyclerOptions<ChatMessageModel> options) {
        adapter = new ChatRecyclerAdapter(options, getApplicationContext());
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setReverseLayout(true);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);
        adapter.startListening();
    }

    private void showError(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        isActivityActive = true;
        if (adapter != null) {
            adapter.startListening();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        isActivityActive = false;
        if (adapter != null) {
            adapter.stopListening();
        }
    }

    @Override
    protected void onDestroy() {
        isActivityActive = false;
        if (adapter != null) {
            adapter.stopListening();
        }
        super.onDestroy();
    }
}