package com.collegefest.service;

import java.util.regex.Pattern;

import org.mindrot.jbcrypt.BCrypt;

import com.collegefest.db.UserDAO;
import com.collegefest.exceptions.UserNotFoundException;
import com.collegefest.models.User;

/**
 * Account creation - the LOGIC behind the "Create account" dialogs.
 *
 * Kept separate from the GUI so it can be unit-tested and reused. The
 * dialogs collect the text; this class enforces every rule and writes to
 * MongoDB. Students and vendors both live in the "users" collection,
 * separated by the role field - that field is what routes each login to
 * the right dashboard (this is effectively the "two lists").
 *
 * For a VENDOR, the "name" field stores the STALL NAME - that is exactly
 * the string students see in the ordering dropdown, so a newly created
 * stall becomes orderable immediately.
 *
 * Rules enforced here (mirrors the dialog hints):
 *  - username: 3-20 letters/numbers/underscore  (REGEX requirement)
 *  - password: minimum 6 characters, stored ONLY as a BCrypt hash
 *  - display/stall name: not blank; stall names must also be UNIQUE,
 *    because the ordering UI maps stall name -> vendor id
 *  - username must not already exist
 */
public class AccountService {

    public static final Pattern USERNAME_PATTERN =
            Pattern.compile("^[A-Za-z0-9_]{3,20}$");

    public static final String ROLE_STUDENT = "student";
    public static final String ROLE_VENDOR  = "vendor";

    /**
     * Creates an account or throws IllegalArgumentException with a
     * user-showable message explaining exactly what to fix.
     * MUST be called from a background thread (it talks to MongoDB).
     */
    public static void createAccount(String role, String username,
                                     String displayName, String password) {
        UserDAO userDAO = new UserDAO();

        if (!USERNAME_PATTERN.matcher(username).matches()) {
            throw new IllegalArgumentException(
                    "Username must be 3-20 letters, numbers or _.");
        }
        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException(
                    "Password must be at least 6 characters.");
        }
        if (displayName == null || displayName.trim().isEmpty()) {
            throw new IllegalArgumentException(ROLE_VENDOR.equals(role)
                    ? "Please enter your stall name."
                    : "Please enter a display name.");
        }

        // Username taken? findByUserId throws UserNotFoundException when
        // free - so the HAPPY path here is the exception path.
        boolean taken;
        try {
            userDAO.findByUserId(username);
            taken = true;
        } catch (UserNotFoundException e) {
            taken = false;
        }
        if (taken) {
            throw new IllegalArgumentException(
                    "That username is already taken - pick another.");
        }

        // Stall names must be unique (students order BY stall name).
        if (ROLE_VENDOR.equals(role)) {
            for (User v : userDAO.findAllVendors()) {
                if (v.getName().equalsIgnoreCase(displayName.trim())) {
                    throw new IllegalArgumentException(
                            "A stall with that name already exists.");
                }
            }
        }

        // Store the HASH, never the password itself.
        String hash = BCrypt.hashpw(password, BCrypt.gensalt());
        userDAO.insertUser(new User(username, hash, displayName.trim(), role));
    }
}
