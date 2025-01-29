package com.example.jugajug.adapter;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.jugajug.ChatActivity;
import com.example.jugajug.R;
import com.example.jugajug.model.UserModel;
import com.example.jugajug.utils.AndroidUtil;
import com.example.jugajug.utils.FirebaseUtil;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;

public class SearchUserRecyclerAdapter extends FirestoreRecyclerAdapter<UserModel, SearchUserRecyclerAdapter.UserModelViewHolder> {

    private Context context;
    private TextView emptyStateText;
    private RecyclerView recyclerView;
    private final Handler mainHandler;

    public SearchUserRecyclerAdapter(@NonNull FirestoreRecyclerOptions<UserModel> options, Context context, TextView emptyStateText, RecyclerView recyclerView) {
        super(options);
        this.context = context;
        this.emptyStateText = emptyStateText;
        this.recyclerView = recyclerView;
        this.mainHandler = new Handler(Looper.getMainLooper());
        setHasStableIds(true);
    }

    @Override
    public long getItemId(int position) {
        UserModel model = getItem(position);
        return model != null && model.getUserId() != null ?
                model.getUserId().hashCode() : RecyclerView.NO_ID;
    }

    @Override
    public void onDataChanged() {
        super.onDataChanged();
        mainHandler.post(this::updateEmptyState);
    }

    private void updateEmptyState() {
        if (emptyStateText != null && recyclerView != null) {
            int itemCount = getItemCount();
            emptyStateText.setVisibility(itemCount == 0 ? View.VISIBLE : View.GONE);
            recyclerView.setVisibility(itemCount == 0 ? View.GONE : View.VISIBLE);
            if (itemCount == 0) {
                emptyStateText.setText("No users found");
            }
        }
    }

    @Override
    protected void onBindViewHolder(@NonNull UserModelViewHolder holder, int position, @NonNull UserModel model) {
        if (model == null) return;

        String username = model.getUsername();
        String userId = model.getUserId();
        String currentUserId = FirebaseUtil.currentUserId();

        if (username != null) {
            holder.usernameText.setText(userId != null && userId.equals(currentUserId) ?
                    username + " (Me)" : username);
        } else {
            holder.usernameText.setText("");
        }

        holder.phoneText.setText(model.getPhone() != null ? model.getPhone() : "");
        holder.profilePic.setImageResource(R.drawable.baseline_account_circle_24);

        if (userId != null) {
            FirebaseUtil.getOtherProfilePicStorageRef(userId).getDownloadUrl()
                    .addOnSuccessListener(uri -> {
                        if (uri != null && holder.getAdapterPosition() == position) {
                            AndroidUtil.setProfilePic(context, uri, holder.profilePic);
                        }
                    })
                    .addOnFailureListener(e -> {
                        // Keep default profile picture on failure
                    });
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ChatActivity.class);
            AndroidUtil.passUserModelAsIntent(intent, model);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        });
    }

    @NonNull
    @Override
    public UserModelViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.search_user_recycler_row, parent, false);
        return new UserModelViewHolder(view);
    }

    static class UserModelViewHolder extends RecyclerView.ViewHolder {
        TextView usernameText;
        TextView phoneText;
        ImageView profilePic;

        public UserModelViewHolder(@NonNull View itemView) {
            super(itemView);
            usernameText = itemView.findViewById(R.id.user_name_text);
            phoneText = itemView.findViewById(R.id.phone_text);
            profilePic = itemView.findViewById(R.id.profile_pic_image_view);
        }
    }

    @Override
    public void onViewRecycled(@NonNull UserModelViewHolder holder) {
        super.onViewRecycled(holder);
        holder.profilePic.setImageResource(R.drawable.baseline_account_circle_24);
    }
}