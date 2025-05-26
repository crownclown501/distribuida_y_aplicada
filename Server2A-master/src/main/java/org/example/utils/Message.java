package org.example.utils;

public class Message {
    private User sender;
    private String content;

    public Message(User sender, String content) {
        this.sender = sender;
        this.content = content;
    }

    public User getSender() {
        return sender;
    }

    public void setSender(User sender) {
        this.sender = sender;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public String toString() {
        return "Message{" +
                "sender=" + sender +
                ", content='" + content + '\'' +
                '}';
    }
}
