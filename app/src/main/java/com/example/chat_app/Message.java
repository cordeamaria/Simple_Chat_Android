package com.example.chat_app;

public class Message {
    private int senderId;
    private int receiverId;
    private String messageContent;
    private String sentAt;

    public Message(int senderId, int receiverId, String messageContent,String sentAt) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.messageContent = messageContent;
        this.sentAt=sentAt;
    }

    public int getSenderId() { return senderId; }
    public int getReceiverId() { return receiverId; }
    public String getMessageContent() { return messageContent; }
    public String getSentAt() { return sentAt; }

}

