package com.sismics.docs.core.model.jpa;

import jakarta.persistence.*; // Using jakarta persistence as in the example
import java.util.Date;
import java.util.UUID;

/**
 * Represents a single chat message stored in the database.
 */
@Entity
@Table(name = "T_CHAT_MESSAGE") // Define the table name
public class ChatMessage {

    /**
     * Primary key (UUID).
     */
    @Id
    @Column(name = "MSG_ID_C", length = 36) // Consistent naming convention (MSG prefix, C for Character/String)
    private String id;

    /**
     * Timestamp when the message was created/sent.
     */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "MSG_TIMESTAMP_D", nullable = false) // D for Date, not nullable
    private Date timestamp;

    /**
     * Username of the user who sent the message.
     * Consider using a @ManyToOne relationship to a User entity if you have one.
     * @ManyToOne
     * @JoinColumn(name = "MSG_SENDER_USER_ID_C", referencedColumnName = "USR_ID_C")
     * private User sender;
     */
    @Column(name = "MSG_SENDER_USERNAME_C", nullable = false, length = 50) // Store username directly for simplicity based on JS
    private String senderUsername;

    /**
     * Username of the user who received the message.
     * Similar to sender, consider a @ManyToOne relationship to a User entity.
     * @ManyToOne
     * @JoinColumn(name = "MSG_RECIPIENT_USER_ID_C", referencedColumnName = "USR_ID_C")
     * private User recipient;
     */
    @Column(name = "MSG_RECIPIENT_USERNAME_C", nullable = false, length = 50) // Store username directly
    private String recipientUsername;

    /**
     * The actual content of the chat message.
     * Use @Lob for potentially very long messages if needed.
     */
    @Column(name = "MSG_CONTENT_C", columnDefinition = "TEXT") // Use TEXT type for potentially longer messages
    private String content;

    /**
     * Default constructor.
     * Generates a unique ID and sets the current timestamp upon creation.
     */
    public ChatMessage() {
        this.id = UUID.randomUUID().toString();
        this.timestamp = new Date();
    }

    // --- Getters and Setters ---

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Date getTimestamp() {
        // Return a defensive copy to prevent modification of the internal Date object
        return timestamp == null ? null : new Date(timestamp.getTime());
    }

    public void setTimestamp(Date timestamp) {
        // Store a defensive copy
        this.timestamp = timestamp == null ? null : new Date(timestamp.getTime());
    }

    public String getSenderUsername() {
        return senderUsername;
    }

    public void setSenderUsername(String senderUsername) {
        this.senderUsername = senderUsername;
    }

    public String getRecipientUsername() {
        return recipientUsername;
    }

    public void setRecipientUsername(String recipientUsername) {
        this.recipientUsername = recipientUsername;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    // --- Optional: toString(), equals(), hashCode() ---

    @Override
    public String toString() {
        return "ChatMessage{" +
                "id='" + id + '\'' +
                ", timestamp=" + timestamp +
                ", senderUsername='" + senderUsername + '\'' +
                ", recipientUsername='" + recipientUsername + '\'' +
                ", content='" + (content != null ? content.substring(0, Math.min(content.length(), 30)) + "..." : "null") + '\'' +
                '}';
    }

    // Basic equals/hashCode based on ID
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChatMessage that = (ChatMessage) o;
        return id != null ? id.equals(that.id) : that.id == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}