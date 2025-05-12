package com.sismics.docs.rest.resource;

import com.sismics.docs.core.dao.ChatMessageDao; // Import the DAO
import com.sismics.docs.core.model.jpa.ChatMessage; // Import the Model
// Note: Imports for validation, authorization, and related exceptions were intentionally removed based on previous requests.

import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.InternalServerErrorException; // Keep for DAO errors
import jakarta.ws.rs.BadRequestException; // Keep for basic null checks

import java.text.SimpleDateFormat; // For timestamp formatting
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/**
 * REST Resource for Chat Messages. (Validation and Authorization Removed)
 * Provides endpoints for retrieving conversations and sending messages.
 * WARNING: This version lacks security checks and input validation. Sender is specified by client. Use with caution.
 *
 * @author Your Name (or AI Assistant)
 */
@Path("/chat") // Base path for chat related endpoints
public class ChatMessageResource extends BaseResource { // Still extends BaseResource

    // Helper to format dates as ISO 8601 String
    private String formatTimestamp(Date date) {
        if (date == null) {
            return null;
        }
        // Use ISO 8601 format (UTC)
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(date);
    }

    /**
     * Get the conversation history between two users. (No validation/authorization)
     *
     * @api {get} /chat/conversation/{user1}/{user2} Get conversation between two users
     * @apiName GetConversation
     * @apiGroup Chat
     * @apiParam {String} user1 Username of the first user
     * @apiParam {String} user2 Username of the second user
     * @apiParam {String="asc","desc"} [sort="asc"] Sort order for messages (asc: oldest first, desc: newest first)
     * @apiSuccess {Object[]} messages List of chat messages
     * @apiSuccess {String} messages.id Message ID
     * @apiSuccess {String} messages.senderUsername Sender's username
     * @apiSuccess {String} messages.recipientUsername Recipient's username
     * @apiSuccess {String} messages.content Message content
     * @apiSuccess {String} messages.timestamp ISO 8601 formatted timestamp (UTC)
     * @apiPermission none (Validation/Auth removed)
     * @apiVersion 1.0.0-insecure
     *
     * @param user1Username Username of the first participant
     * @param user2Username Username of the second participant
     * @param sortOrder     Sort order ("asc" or "desc")
     * @return Response containing the list of messages
     */
    @GET
    @Path("/conversation/{user1}/{user2}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getConversation(
            @PathParam("user1") String user1Username,
            @PathParam("user2") String user2Username,
            @QueryParam("sort") @DefaultValue("asc") String sortOrder) {

        // --- Authentication Check Removed ---
        // --- Authorization Check Removed ---
        // --- Input Validation Removed ---

        // Core logic: Determine sort order
        boolean ascending = !"desc".equalsIgnoreCase(sortOrder);

        // Core logic: Fetch data from DAO
        ChatMessageDao chatMessageDao = new ChatMessageDao();
        List<ChatMessage> messages = chatMessageDao.findByConversation(user1Username, user2Username, ascending);

        // Core logic: Build JSON response
        JsonArrayBuilder messagesArray = Json.createArrayBuilder();
        for (ChatMessage msg : messages) {
            messagesArray.add(Json.createObjectBuilder()
                    .add("id", msg.getId())
                    .add("senderUsername", msg.getSenderUsername())
                    .add("recipientUsername", msg.getRecipientUsername())
                    .add("content", msg.getContent() != null ? msg.getContent() : "")
                    .add("timestamp", formatTimestamp(msg.getTimestamp())));
        }

        JsonObject response = Json.createObjectBuilder()
                .add("messages", messagesArray)
                .build();

        // Core logic: Return response
        return Response.ok().entity(response).build();
    }

    /**
     * Send a new chat message. (No validation/authorization - Sender specified by client)
     *
     * @api {post} /chat/messages Send a new chat message
     * @apiName PostChatMessage
     * @apiGroup Chat
     * @apiParam {String} sender The username of the sender (Specified by client)
     * @apiParam {String} recipient The username of the recipient
     * @apiParam {String} content The message content
     * @apiSuccess {String} id Message ID
     * @apiSuccess {String} senderUsername Sender's username (as provided in input)
     * @apiSuccess {String} recipientUsername Recipient's username
     * @apiSuccess {String} content Message content
     * @apiSuccess {String} timestamp ISO 8601 formatted timestamp (UTC) of creation
     * @apiPermission none (Validation/Auth removed)
     * @apiVersion 1.0.0-insecure-sender-param
     *
     * @param senderUsername    The username claiming to send the message (from client input)
     * @param recipientUsername The username to send the message to
     * @param content           The message text
     * @return Response containing the newly created message object
     */
    @POST
    @Path("/messages")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response sendMessage(
            @FormParam("sender") String senderUsername, // Added sender as FormParam
            @FormParam("recipient") String recipientUsername,
            @FormParam("content") String content) {

        // --- Authentication Check Removed ---
        // --- Principal Usage for Sender Removed ---
        // --- Input Validation Removed ---

        // Core logic: Prepare message data
        Date timestamp = new Date(); // Server sets the timestamp

        // Core logic: Use DAO to create the message
        ChatMessageDao chatMessageDao = new ChatMessageDao();
        ChatMessage newMessage;
        try {
            // Basic check for required parameters being null/empty
            if (senderUsername == null || senderUsername.trim().isEmpty() ||
                recipientUsername == null || recipientUsername.trim().isEmpty() ||
                content == null) { // Allow empty content string, but not null
                 throw new BadRequestException("Sender, recipient, and content parameters are required and cannot be empty");
            }
            newMessage = chatMessageDao.createMessage(senderUsername.trim(), recipientUsername.trim(), content, timestamp);
        } catch (BadRequestException e) {
            // Re-throw bad request exceptions
            throw e;
        }
         catch (Exception e) {
            // Handle potential persistence errors (kept)
             // Log the exception details for debugging
             // logger.log(Level.SEVERE, "Failed to save message", e); // Assuming a logger is available
             throw new InternalServerErrorException("Failed to save message", e);
        }

        // Core logic: Build the response object
        JsonObjectBuilder responseBuilder = Json.createObjectBuilder()
                .add("id", newMessage.getId())
                .add("senderUsername", newMessage.getSenderUsername()) // Use the value stored in the message
                .add("recipientUsername", newMessage.getRecipientUsername())
                .add("content", newMessage.getContent() != null ? newMessage.getContent() : "")
                .add("timestamp", formatTimestamp(newMessage.getTimestamp()));

        // Core logic: Return the response
        return Response.ok().entity(responseBuilder.build()).build();
    }
}