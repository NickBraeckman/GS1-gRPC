package be.msec.labgrpc;

import java.sql.Timestamp;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class Message {

    private User sender;
    private final MessageType messageType;
    private final String text;
    private String textFormat;
    private Set<User> group;
    private final Timestamp timestamp;

    /* ----------------------------- CONSTRUCTOR ----------------------------- */
    public Message(User sender, MessageType messageType, String text) {
        this.sender = sender;
        this.messageType = messageType;
        this.text = text;
        this.group = new HashSet<>();
        this.group.add(sender);
        this.timestamp = new Timestamp(new Date().getTime());
    }

    /* CONNECT / DISCONNECT MESSAGE */
    public Message(MessageType messageType) {
        this.messageType = messageType;
        this.text = "I want to disconnect !!!";
        this.timestamp = new Timestamp(new Date().getTime());
    }

    public Message(User sender, MessageType messageType, String text, Set<User> group) {
        this.sender = sender;
        this.messageType = messageType;
        this.text = text;
        this.group = group;
        this.group.add(sender);
        this.timestamp = new Timestamp(new Date().getTime());
    }

    public Message(MessageType messageType, String text) {
        this.messageType = messageType;
        this.text = text;
        this.timestamp = new Timestamp(new Date().getTime());
    }

    /* ----------------------------- GETTERS ----------------------------- */
    public User getSender() {
        return sender;
    }

    public MessageType getType() {
        return messageType;
    }

    public Set<User> getGroup() {
        return group;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public String getTextFormat() {
        switch (messageType){
            case CONNECTION_SUCCESS:
                return  "Welcome to the chat " + sender + " !";
            case CONNECTION_SUCCESS_BROADCAST:
                return sender + " has entered the chat";
            case BROADCAST:
                return  "[" + sender.getName() +"]: " + text;
            default:
                return text;
        }
    }

    public void setSender(User user) {
        this.sender=user;
    }


    /* ----------------------------- OVERRIDE ----------------------------- */
    @Override
    public String toString() {
        return "Message{" +
                "sender=" + sender +
                ", messageType=" + messageType +
                ", group=" + group +
                ", timestamp=" + timestamp +
                '}';
    }
}
