package com.collegefest.exceptions;

/**
 * Thrown when an order ID cannot be found in MongoDB.
 * Part of the custom exception hierarchy (professor requirement).
 *
 * NOTE: Person A owns the full exception hierarchy (CollegeFestException base class).
 * This stub lets Person B's code compile before the Day 2 merge checkpoint.
 * After Person A merges feature/auth → main, pull main and this class
 * will be replaced by Person A's version — no action needed on your end.
 */
public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException(String message) {
        super(message);
    }
}
