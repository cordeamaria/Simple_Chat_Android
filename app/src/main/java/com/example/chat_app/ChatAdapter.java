package com.example.chat_app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;

    private List<Message> messageList;
    private int currentUserId;

    public ChatAdapter(List<Message> messageList, int currentUserId) {
        this.messageList = messageList;
        this.currentUserId = currentUserId;
    }

    @Override
    public int getItemViewType(int position) {
        return messageList.get(position).getSenderId() == currentUserId ? VIEW_TYPE_SENT : VIEW_TYPE_RECEIVED;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_SENT) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.chat_item_sent, parent, false);
            return new SentViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.chat_item_received, parent, false);
            return new ReceivedViewHolder(view);
        }
    }

    private String formatTime(String isoTime) {
        try {
            // Remove the Z if present
            isoTime = isoTime.replace("Z", "");

            // Parse the incoming ISO string
            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault());
            Date date = isoFormat.parse(isoTime);

            // Format it to something nicer
            SimpleDateFormat displayFormat = new SimpleDateFormat("MMM dd HH:mm", Locale.getDefault());
            return displayFormat.format(date);

        } catch (Exception e) {
            e.printStackTrace();
            return isoTime; // fallback
        }
    }



    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        Message message = messageList.get(position);
        String formattedTime = formatTime(message.getSentAt()); // formatează ora

        if (holder instanceof SentViewHolder) {
            ((SentViewHolder) holder).txtMessage.setText(message.getMessageContent());
            ((SentViewHolder) holder).txtTime.setText(formattedTime);
        } else {
            ((ReceivedViewHolder) holder).txtMessage.setText(message.getMessageContent());
            ((ReceivedViewHolder) holder).txtTime.setText(formattedTime);
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    static class SentViewHolder extends RecyclerView.ViewHolder {
        TextView txtMessage, txtTime;
        SentViewHolder(View view) {
            super(view);
            txtMessage = view.findViewById(R.id.txtMessage);
            txtTime = view.findViewById(R.id.txtTime);
        }
    }

    static class ReceivedViewHolder extends RecyclerView.ViewHolder {
        TextView txtMessage, txtTime;
        ReceivedViewHolder(View view) {
            super(view);
            txtMessage = view.findViewById(R.id.txtMessage);
            txtTime = view.findViewById(R.id.txtTime);
        }
    }
}
