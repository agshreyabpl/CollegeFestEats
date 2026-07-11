package com.collegefest.exceptions;

public class UserNotFoundException extends CollegeFestException {
    public UserNotFoundException(String userId) {
        super("User not found: " + userId);
    }
}