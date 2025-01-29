package com.example.jugajug;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.jugajug.adapter.SearchUserRecyclerAdapter;
import com.example.jugajug.model.UserModel;
import com.example.jugajug.utils.FirebaseUtil;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.Locale;

public class SearchUserActivity extends AppCompatActivity {

    private EditText searchInput;
    private RecyclerView recyclerView;
    private TextView emptyStateText;
    private SearchUserRecyclerAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_user);

        // Initialize views first
        initializeViews();

        // Initial setup with all users
        setupSearchRecyclerView("");

        // Setup search input listener
        setupSearchListener();

        // Setup profile button
        setupProfileButton();
    }

    private void initializeViews() {
        searchInput = findViewById(R.id.seach_username_input);
        recyclerView = findViewById(R.id.search_user_recycler_view);
        emptyStateText = findViewById(R.id.empty_state_text);

        // Verify views are properly initialized
        if (searchInput == null || recyclerView == null || emptyStateText == null) {
            throw new IllegalStateException("Required views not found in layout. Please check view IDs.");
        }
    }

    private void setupSearchListener() {
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String searchText = s.toString().trim().toLowerCase(Locale.ROOT);
                setupSearchRecyclerView(searchText);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupProfileButton() {
        View searchUserBtn = findViewById(R.id.search_user_btn);
        if (searchUserBtn != null) {
            searchUserBtn.setOnClickListener(v -> {
                Intent intent = new Intent(SearchUserActivity.this, ProfileActivity.class);
                startActivity(intent);
            });
        }
    }

    private void setupSearchRecyclerView(String searchText) {
        // Stop listening to previous adapter if it exists
        if (adapter != null) {
            adapter.stopListening();
        }

        Query query;
        if (searchText.isEmpty()) {
            // Query all users if search text is empty
            query = FirebaseFirestore.getInstance()
                    .collection("users")
                    .orderBy("username")
                    .limit(50);  // Add limit for performance
        } else {
            // Case-insensitive search on username field
            String searchLower = searchText.toLowerCase();
            String searchUpper = searchText.toLowerCase() + '\uf8ff';

            query = FirebaseFirestore.getInstance()
                    .collection("users")
                    .orderBy("username")
                    .whereGreaterThanOrEqualTo("username", searchLower)
                    .whereLessThanOrEqualTo("username", searchUpper)
                    .limit(50);
        }

        FirestoreRecyclerOptions<UserModel> options = new FirestoreRecyclerOptions.Builder<UserModel>()
                .setQuery(query, UserModel.class)
                .build();

        adapter = new SearchUserRecyclerAdapter(options, this, emptyStateText, recyclerView);

        // Set layout manager before setting adapter
        if (recyclerView.getLayoutManager() == null) {
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
        }

        recyclerView.setItemAnimator(null); // Disable animations
        recyclerView.setAdapter(adapter);
        adapter.startListening();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (adapter != null) {
            adapter.startListening();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (adapter != null) {
            adapter.stopListening();
        }
    }
}
