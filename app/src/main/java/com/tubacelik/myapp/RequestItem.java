package com.tubacelik.myapp;

public class RequestItem {

    private String requestId;
    private String skillId;
    private String skillTitle;
    private String senderId;
    private String senderName;
    private String receiverId;
    private String receiverName;
    private String message;
    private String status;

    public RequestItem() {
        // Wird von Firestore benötigt.
    }

    public String getRequestId() {
        return requestId;
    }

    public String getSkillId() {
        return skillId;
    }

    public String getSkillTitle() {
        return skillTitle;
    }

    public String getSenderId() {
        return senderId;
    }

    public String getSenderName() {
        return senderName;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public String getReceiverName() {
        return receiverName;
    }

    public String getMessage() {
        return message;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
