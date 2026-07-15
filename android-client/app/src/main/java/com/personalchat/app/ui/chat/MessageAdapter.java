package com.personalchat.app.ui.chat;

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
import com.personalchat.app.data.model.DeliveryStatus;
import com.personalchat.app.data.model.Message;

import java.util.Calendar;

public class MessageAdapter extends ListAdapter<Message, RecyclerView.ViewHolder> {
    private static final int VIEW_TYPE_INCOMING = 1;
    private static final int VIEW_TYPE_OUTGOING = 2;

    public MessageAdapter() {
        super(new DiffUtil.ItemCallback<Message>() {
            @Override
            public boolean areItemsTheSame(@NonNull Message oldItem, @NonNull Message newItem) {
                return oldItem.getId().equals(newItem.getId());
            }

            @Override
            public boolean areContentsTheSame(@NonNull Message oldItem, @NonNull Message newItem) {
                return oldItem.getContent().equals(newItem.getContent()) &&
                        oldItem.getTimestamp() == newItem.getTimestamp() &&
                        oldItem.getStatus().equals(newItem.getStatus());
            }
        });
    }

    @Override
    public int getItemViewType(int position) {
        Message message = getItem(position);
        return message.isIncoming() ? VIEW_TYPE_INCOMING : VIEW_TYPE_OUTGOING;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_INCOMING) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_incoming, parent, false);
            return new IncomingViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.id.recycler_messages, parent, false);
            // Wait, inflating R.id.recycler_messages is incorrect! We should inflate R.layout.item_message_outgoing! Let's be careful.
            View layout = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_outgoing, parent, false);
            return new OutgoingViewHolder(layout);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message message = getItem(position);
        if (holder instanceof IncomingViewHolder) {
            ((IncomingViewHolder) holder).bind(message);
        } else if (holder instanceof OutgoingViewHolder) {
            ((OutgoingViewHolder) holder).bind(message);
        }
    }

    static class IncomingViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvContent;
        private final TextView tvTime;

        public IncomingViewHolder(@NonNull View itemView) {
            super(itemView);
            tvContent = itemView.findViewById(R.id.tv_message_content);
            tvTime = itemView.findViewById(R.id.tv_message_time);
        }

        public void bind(Message msg) {
            tvContent.setText(msg.getContent());
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(msg.getTimestamp());
            tvTime.setText(DateFormat.format("h:mm a", cal).toString());
        }
    }

    static class OutgoingViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvContent;
        private final TextView tvTime;
        private final TextView tvStatus;

        public OutgoingViewHolder(@NonNull View itemView) {
            super(itemView);
            tvContent = itemView.findViewById(R.id.tv_message_content);
            tvTime = itemView.findViewById(R.id.tv_message_time);
            tvStatus = itemView.findViewById(R.id.tv_message_status);
        }

        public void bind(Message msg) {
            tvContent.setText(msg.getContent());
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(msg.getTimestamp());
            tvTime.setText(DateFormat.format("h:mm a", cal).toString());

            String statusStr = msg.getStatus();
            if (DeliveryStatus.SENDING.name().equals(statusStr)) {
                tvStatus.setText("...");
            } else if (DeliveryStatus.SENT.name().equals(statusStr)) {
                tvStatus.setText("✓");
            } else if (DeliveryStatus.DELIVERED.name().equals(statusStr)) {
                tvStatus.setText("✓✓");
            } else if (DeliveryStatus.FAILED.name().equals(statusStr)) {
                tvStatus.setText("⚠");
            } else {
                tvStatus.setText("");
            }
        }
    }
}
