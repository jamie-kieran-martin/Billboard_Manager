package server.services;

import common.models.User;
import common.utils.HashingFactory;
import common.utils.RandomFactory;
import server.sql.CollectionFactory;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.*;

/**
 * This class handles the how the server responds to the authenticated required requests.
 *
 * @author Perdana Bailey
 * @author Kevin Huynh
 */
public class TokenService {

    private Set<Session> sessions = new HashSet<>();

    private static class TokenServiceHolder {
        private final static TokenService INSTANCE = new TokenService();
    }

    public static TokenService getInstance() {
        return TokenService.TokenServiceHolder.INSTANCE;
    }

    /**
     * This is the function called when a client attempts to login.
     *
     * @param user: This is the user object of the user trying to login
     * @param password: The attempted password
     * @return String token: Null if failed, token if valid Session exists or new Session created.
     * @throws Exception: Pass through the server error from the tryLogout function.
     */
    public String tryLogin(User user, String password) throws Exception {
        // Convert the users saved password and salt as a hex to a byte array
        byte[] storedPassword = HashingFactory.decodeHex(user.password);
        byte[] userSalt = HashingFactory.decodeHex(user.salt);

        // Attempt to create a hash based on the given password and the salt/password already in the database
        byte[] testHash = HashingFactory.hashPassword(password, userSalt, storedPassword.length)

        // Ensure the testHash is the same as the hash in the database
        if (!Arrays.equals(storedPassword, testHash)) return null;

        // Checks if there is a valid session already and returns token if so
        Optional<Session> existingSession = getSessionByUsername(user.username);
        if (existingSession.isPresent()) return existingSession.get().token;

        // Generate new session and save it to sessions set
        Session newSession = new Session(user.id, user.username);
        sessions.add(newSession);

        return newSession.token;
    }

    /**
     * This function checks if the user exists and returns an optional user.
     *
     * @param username: The username of the user using the client.
     * @return Optional<User>: The user optional after attempting to fetch the user based off username.
     * @throws Exception: Pass through the server error.
     */
    public Optional<User> checkUserExists(String username) throws Exception {
        return CollectionFactory.getInstance(User.class).get(u -> u.username.equals(username)).stream().findFirst();
    }

    /**
     * This function logs the user out by removing the token from the session and removing them
     * from the logged in list.
     *
     * @param token: The supplied user token.
     */
    public void tryLogout(String token) {
        sessions.remove(token);
    }

    /**
     * This function ensures the token is valid.
     *
     * @param token: The supplied user token.
     * @return boolean: Token valid or invalid.
     */
    public boolean verify(String token) {
        Optional<Session> session = sessions.stream().filter(x -> x.token == token).findFirst();

        // verify the session isn't empty or expired
        if (session.isEmpty() || expired(token)) return false;

        return true;
    }

    /**
     * This function gets the session details by username.
     *
     * @param username: The username of the logged-in user.
     * @return Optional<Session>: The Session object related to the logged-in user.
     */
    private Optional<Session> getSessionByUsername(String username) {
        return sessions.stream().filter(x -> x.username == username).findFirst();
    }

    /**
     * This function gets the session details by token.
     *
     * @param token: The token of the logged-in user.
     * @return Optional<Session>: The Session object related to the logged-in user.
     */
    public Optional<Session> getSessionByToken(String token) {
        return sessions.stream().filter(session -> token.equals(session.token)).findFirst();
    }

    /**
     * This function ensures the token is not expired.
     *
     * @param token: The token of the logged-in/ex-logged in user.
     * @return boolean: True if expired, False is not expired.
     */
    public boolean expired(String token) {
        // Get the session information from the token.
        Optional<Session> session = getSessionByToken(token);

        // Session is empty so logical equivalent of being expired.
        if (session.isEmpty()) return true;

        // If session is expired remove session and return true.
        if (session.get().expireTime.compareTo(LocalDateTime.now()) <= 0) {
            sessions.remove(session);
            return true;
        }

        // If the session is present and valid.
        return false;
    }

}
