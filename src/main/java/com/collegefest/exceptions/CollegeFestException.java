package com.collegefest.exceptions;

public class CollegeFestException extends RuntimeException {
    public CollegeFestException(String message) {
        super(message);
    }
    public CollegeFestException(String message, Throwable cause) {
        super(message, cause);
    }
}