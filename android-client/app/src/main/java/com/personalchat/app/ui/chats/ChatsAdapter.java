package com.personalchat.app.ui.chats;

import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.personalchat.app.R;
import com.personalchat.app.data.model.Conversation;
import com.personalchat.app.data.model.PeerConnectionState;

import java.util.Calendar;

public class ChatsAdapter extends ListAdapter<Conversation, ChatsAdapter.ChatViewHolder> {

    private final OnChatClickListener listener;

    public interface OnChatClickListener {
        void onChatClick(Conversation conversation);
    }

    public ChatsAdapter(OnChatClickListener listener) {
        super(new DiffUtil.ItemCallback<Conversation>() {
            @Override
            public boolean areItemsTheSame(@NonNull Conversation oldItem, @NonNull Conversation newItem) {
                return oldItem.getId().equals(newItem.getId());
            }

            @Override
            public boolean areContentsTheSame(@NonNull Conversation oldItem, @NonNull Conversation newItem) {
                return oldItem.getLastMessage().equals(newItem.getLastMessage()) &&
                        oldItem.getLastMessageTimestamp() == newItem.getLastMessageTimestamp() &&
                        oldItem.getConnectionState().equals(newItem.getConnectionState());
            }
        });
        this.listener = listener;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvName;
        private final TextView tvLastMsg;
        private final TextView tvTime;
        private final View connectionDot;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_chat_name);
            tvLastMsg = itemView.findViewById(R.id.tv_chat_last_message);
            tvTime = itemView.findViewById(R.id.tv_chat_time);
            connectionDot = itemView.findViewById(R.id.peer_connection_dot);
        }

        public void bind(Conversation conversation, OnChatClickListener listener) {
            tvName.setText(conversation.getPeerName());
            tvLastMsg.setText(conversation.getLastMessage().isEmpty() ? "No messages yet" : conversation.getLastMessage());
            
            // Format time
            if (conversation.getLastMessageTimestamp() > 0) {
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(conversation.getLastMessageTimestamp());
                tvTime.setText(DateFormat.format("h:mm a", cal).toString());
            } else {
                tvTime.setText("");
            }

            // Connection state styling
            String stateStr = conversation.getConnectionState();
            if (PeerConnectionState.CONNECTED.name().equals(stateStr)) {
                connectionDot.setBackgroundResource(R.drawable.status_dot_green);
            } else if (PeerConnectionState.CONNECTING.name().equals(stateStr)) {
                connectionDot.setBackgroundResource(R.drawable.status_dot_yellow);
            } else {
                connectionDot.setBackgroundResource(R.drawable.status_dot_red);
            }

            itemView.setOnClickListener(v -> listener.onChatClick(conversation));
        }
    }
}
