package com.sismics.docs.rest.resource;

import com.example.model.jpa.*; // Import your Register entity
import com.sismics.docs.core.dao.RegisterDao; // Import your RegisterDao
import com.sismics.docs.core.constant.Constants;
import com.sismics.docs.core.dao.UserDao;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import com.sismics.docs.core.constant.AclTargetType;
import com.sismics.docs.core.constant.ConfigType;
import com.sismics.docs.core.constant.Constants;
import com.sismics.docs.core.dao.*;
import com.sismics.docs.core.dao.criteria.GroupCriteria;
import com.sismics.docs.core.dao.criteria.UserCriteria;
import com.sismics.docs.core.dao.dto.GroupDto;
import com.sismics.docs.core.dao.dto.UserDto;
import com.sismics.docs.core.event.DocumentDeletedAsyncEvent;
import com.sismics.docs.core.event.FileDeletedAsyncEvent;
import com.sismics.docs.core.event.PasswordLostEvent;
import com.sismics.docs.core.model.context.AppContext;
import com.sismics.docs.core.model.jpa.*;
import com.sismics.docs.core.util.ConfigUtil;
import com.sismics.docs.core.util.RoutingUtil;
import com.sismics.docs.core.util.authentication.AuthenticationUtil;
import com.sismics.docs.core.util.jpa.SortCriteria;
import com.sismics.docs.rest.constant.BaseFunction;
import com.sismics.rest.exception.ClientException;
import com.sismics.rest.exception.ForbiddenClientException;
import com.sismics.rest.exception.ServerException;
import com.sismics.rest.util.ValidationUtil;
import com.sismics.security.UserPrincipal;
import com.sismics.util.JsonUtil;
import com.sismics.util.context.ThreadLocalContext;
import com.sismics.util.filter.TokenBasedSecurityFilter;
import com.sismics.util.totp.GoogleAuthenticator;
import com.sismics.util.totp.GoogleAuthenticatorKey;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObjectBuilder;
import jakarta.servlet.http.Cookie;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;

import java.util.Date;
import java.util.List;
import java.util.Set;
// Assuming BaseResource is in a similar package or properly imported
// import com.sismics.docs.rest.BaseResource;

import java.util.List;

/**
 * REST Resource for Registrations.
 * Provides endpoints for managing user registrations.
 * WARNING: This version lacks security checks and detailed input validation.
 */
@Path("/register") // Base path for registration related endpoints
public class RegisterResource extends BaseResource { // Assuming BaseResource exists

    // Helper method to convert a Register entity to a JsonObject
    private JsonObject buildRegisterJson(Register registration) {
        if (registration == null) {
            return null;
        }
        JsonObjectBuilder builder = Json.createObjectBuilder()
                .add("id", registration.getId())
                .add("username", registration.getUsername() != null ? registration.getUsername() : "")
                .add("email", registration.getEmail() != null ? registration.getEmail() : "");
        // For boolean, directly add it.

        
        builder.add("confirmed", registration.isConfirmed());
        return builder.build();
    }

    /**
     * List all registration entries.
     *
     * @api {get} /register List all registrations
     * @apiName GetAllRegistrations
     * @apiGroup Register
     * @apiSuccess {Object[]} registrations List of registration entries
     * @apiSuccess {String} registrations.id Registration ID
     * @apiSuccess {String} registrations.username Username
     * @apiSuccess {String} registrations.email Email
     * @apiSuccess {Boolean} registrations.confirmed Confirmation status
     * @apiPermission none (Validation/Auth removed)
     * @apiVersion 1.0.0-insecure
     *
     * @return Response containing the list of registrations.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllRegistrations() {
        RegisterDao registerDao = new RegisterDao();
        List<Register> registrations = registerDao.findAll();

        JsonArrayBuilder registrationsArray = Json.createArrayBuilder();
        for (Register reg : registrations) {
            registrationsArray.add(buildRegisterJson(reg));
        }

        JsonObject responseJson = Json.createObjectBuilder()
                .add("registrations", registrationsArray)
                .build();

        return Response.ok(responseJson).build();
    }

    /**
     * Add a new registration entry.
     *
     * @api {post} /register Add a new registration
     * @apiName AddRegistration
     * @apiGroup Register
     * @apiParam {String} username User's username
     * @apiParam {String} email User's email
     * @apiSuccess {String} id Registration ID
     * @apiSuccess {String} username Username
     * @apiSuccess {String} email Email
     * @apiSuccess {Boolean} confirmed Confirmation status (will be false)
     * @apiPermission none (Validation/Auth removed)
     * @apiVersion 1.0.0-insecure
     *
     * @param username The username for the registration.
     * @param email    The email for the registration.
     * @return Response containing the newly created registration object.
     */
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addRegistration(
            @FormParam("username") String username,
            @FormParam("email") String email) {

        if (username == null || username.trim().isEmpty() ||
            email == null || email.trim().isEmpty()) {
            throw new BadRequestException("Username and email parameters are required and cannot be empty.");
        }

        RegisterDao registerDao = new RegisterDao();
        Register newRegistration;
        try {
            // The createRegistration method in DAO sets 'confirmed' to false and generates an ID.
            newRegistration = registerDao.createRegistration(username.trim(), email.trim());
        } catch (Exception e) {
            // Log the exception for debugging, e.g., using a logger
            // logger.log(Level.SEVERE, "Failed to create registration", e);
            throw new InternalServerErrorException("Failed to create registration entry.", e);
        }

        return Response.status(Response.Status.CREATED).entity(buildRegisterJson(newRegistration)).build();
    }

    /**
     * Confirm a registration entry by its ID (mark as passed/allowed).
     *
     * @api {put} /register/{id}/confirm Confirm a registration
     * @apiName ConfirmRegistration
     * @apiGroup Register
     * @apiParam {String} id Registration ID to confirm
     * @apiSuccess {String} id Registration ID
     * @apiSuccess {String} username Username
     * @apiSuccess {String} email Email
     * @apiSuccess {Boolean} confirmed Confirmation status (will be true)
     * @apiPermission none (Validation/Auth removed)
     * @apiVersion 1.0.0-insecure
     *
     * @param id The ID of the registration to confirm.
     * @return Response containing the updated registration object or 404 if not found.
     */
    @PUT // Using PUT for idempotent state change
    @Path("/{id}/{username}/{email}/confirm")
    @Produces(MediaType.APPLICATION_JSON)
    public Response confirmRegistration(@PathParam("id") String id,@PathParam("username") String username,
            @PathParam("email") String email) {
        if (id == null || id.trim().isEmpty()) {
            throw new BadRequestException("Registration ID path parameter is required.");
        }

        RegisterDao registerDao = new RegisterDao();
        Register updatedRegistration;
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(username); // 初始密码 or 发送邮件提醒修改
        user.setRoleId(Constants.DEFAULT_USER_ROLE);
        user.setStorageQuota(100000000L); // 默认配额

        UserDao userDao = new UserDao();
        try{userDao.create(user,id);}catch(Exception e){}
        try {
            updatedRegistration = registerDao.confirmRegistrationById(id);
        } catch (Exception e) {
             // logger.log(Level.SEVERE, "Failed to confirm registration " + id, e);
            throw new InternalServerErrorException("Failed to confirm registration entry.", e);
        }

        if (updatedRegistration == null) {
            throw new NotFoundException("Registration entry with ID " + id + " not found.");
        }

        return Response.ok(buildRegisterJson(updatedRegistration)).build();
    }

    /**
     * Delete a registration entry by its ID.
     *
     * @api {delete} /register/{id} Delete a registration
     * @apiName DeleteRegistration
     * @apiGroup Register
     * @apiParam {String} id Registration ID to delete
     * @apiSuccess {String} message Success message
     * @apiPermission none (Validation/Auth removed)
     * @apiVersion 1.0.0-insecure
     *
     * @param id The ID of the registration to delete.
     * @return Response indicating success or failure.
     */
    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteRegistration(@PathParam("id") String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new BadRequestException("Registration ID path parameter is required.");
        }

        RegisterDao registerDao = new RegisterDao();
        Register existingRegistration = registerDao.findById(id); // Check if it exists first for a better 404 message

        if (existingRegistration == null) {
            throw new NotFoundException("Registration entry with ID " + id + " not found.");
        }

        try {
            registerDao.deleteById(id);
        } catch (Exception e) {
            // logger.log(Level.SEVERE, "Failed to delete registration " + id, e);
            throw new InternalServerErrorException("Failed to delete registration entry.", e);
        }

        JsonObject successJson = Json.createObjectBuilder()
                .add("message", "Registration entry with ID " + id + " deleted successfully.")
                .build();
        return Response.ok(successJson).build();
        // Alternatively, for DELETE, you can return Response.Status.NO_CONTENT (204)
        // return Response.noContent().build();
    }
}