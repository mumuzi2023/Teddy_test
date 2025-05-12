package com.sismics.docs.core.dao;

import com.sismics.docs.core.model.jpa.ChatMessage; // Import the ChatMessage entity
import com.sismics.util.context.ThreadLocalContext;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery; // Use TypedQuery for better type safety
import java.util.Date; // Import Date
import java.util.List;

/**
 * Data Access Object for ChatMessage entities.
 * Provides methods for CRUD operations and specific queries related to chat messages.
 */
public class ChatMessageDao {

    /**
     * Persists a new chat message entity directly.
     * The entity should have its fields already set.
     * The ID and potentially timestamp are usually set in the ChatMessage constructor.
     *
     * @param message The ChatMessage entity to create.
     */
    public void create(ChatMessage message) {
        EntityManager em = ThreadLocalContext.get().getEntityManager();
        em.persist(message);
    }

    /**
     * Creates and persists a new chat message from individual parameters.
     * This is a convenience method that constructs the ChatMessage entity internally.
     *
     * @param senderUsername    The username of the user sending the message.
     * @param recipientUsername The username of the user receiving the message.
     * @param content           The text content of the message.
     * @param timestamp         The date and time when the message was sent. If null, the creation time might default (check ChatMessage constructor).
     * @return The newly created and persisted ChatMessage entity, including its generated ID.
     */
    public ChatMessage createMessage(String senderUsername, String recipientUsername, String content, Date timestamp) {
        EntityManager em = ThreadLocalContext.get().getEntityManager();

        // Create a new ChatMessage instance
        // The constructor will automatically generate a UUID for the ID
        // and set a default timestamp (which we might override).
        ChatMessage newMessage = new ChatMessage();

        // Set the properties from the parameters
        newMessage.setSenderUsername(senderUsername);
        newMessage.setRecipientUsername(recipientUsername);
        newMessage.setContent(content);

        // Explicitly set the timestamp provided in the parameter.
        // This will override the default timestamp set in the constructor if timestamp is not null.
        if (timestamp != null) {
             newMessage.setTimestamp(timestamp);
        }
        // If timestamp is null, the timestamp set in the ChatMessage() constructor (new Date()) will be used.

        // Persist the new entity
        em.persist(newMessage);

        // Return the managed entity
        return newMessage;
    }


    /**
     * Retrieves a specific chat message by its unique ID.
     *
     * @param id The UUID string of the message.
     * @return The found ChatMessage, or null if not found.
     */
    public ChatMessage getById(String id) {
        EntityManager em = ThreadLocalContext.get().getEntityManager();
        return em.find(ChatMessage.class, id);
    }

    /**
     * Finds all messages exchanged between two specific users, ordered by timestamp.
     * This retrieves messages where user1 is the sender and user2 is the recipient,
     * AND messages where user2 is the sender and user1 is the recipient.
     *
     * @param user1Username Username of the first user in the conversation.
     * @param user2Username Username of the second user in the conversation.
     * @param ascending     If true, sort by timestamp ascending (oldest first), otherwise descending (newest first).
     * @return A list of ChatMessage objects representing the conversation, ordered by time.
     */
    public List<ChatMessage> findByConversation(String user1Username, String user2Username, boolean ascending) {
        EntityManager em = ThreadLocalContext.get().getEntityManager();
        
        // Determine sort order
        String sortOrder = ascending ? "ASC" : "DESC";

        // Construct the JPQL query
        // Using named parameters :user1 and :user2 for security and clarity
        String jpql = "SELECT m FROM ChatMessage m WHERE " +
                      "(m.senderUsername = :user1 AND m.recipientUsername = :user2) OR " +
                      "(m.senderUsername = :user2 AND m.recipientUsername = :user1) " +
                      "ORDER BY m.timestamp " + sortOrder;

        // Use TypedQuery for type safety on the result list
        TypedQuery<ChatMessage> query = em.createQuery(jpql, ChatMessage.class);

        // Set the named parameters
        query.setParameter("user1", user1Username);
        query.setParameter("user2", user2Username);

        // Execute the query and return the results
        return query.getResultList();
    }

    /**
     * Finds all messages exchanged between two specific users, ordered ascending by timestamp (oldest first).
     * Convenience method calling findByConversation with ascending = true.
     *
     * @param user1Username Username of the first user.
     * @param user2Username Username of the second user.
     * @return A list of ChatMessage objects representing the conversation, ordered oldest to newest.
     */
    public List<ChatMessage> findConversationAscending(String user1Username, String user2Username) {
        return findByConversation(user1Username, user2Username, true);
    }


    /**
     * Updates an existing chat message in the database.
     * Note: Updating chat messages might be uncommon.
     *
     * @param message The ChatMessage entity with updated information.
     * @return The updated, managed ChatMessage entity.
     */
    public ChatMessage update(ChatMessage message) {
        EntityManager em = ThreadLocalContext.get().getEntityManager();
        return em.merge(message);
    }

    /**
     * Deletes a chat message from the database.
     * Note: Deleting chat messages might be uncommon or require specific logic (soft delete).
     *
     * @param message The ChatMessage entity to delete.
     */
    public void delete(ChatMessage message) {
        EntityManager em = ThreadLocalContext.get().getEntityManager();
        // Ensure the entity is managed before removing, especially if passed directly from outside
        if (em.contains(message)) {
            em.remove(message);
        } else {
            ChatMessage managedMessage = getById(message.getId());
            if (managedMessage != null) {
                em.remove(managedMessage);
            }
        }
    }

    /**
     * Deletes a chat message by its ID.
     *
     * @param id The ID of the message to delete.
     */
    public void delete(String id) {
        EntityManager em = ThreadLocalContext.get().getEntityManager();
        ChatMessage message = getById(id);
        if (message != null) {
            em.remove(message);
        }
    }
}