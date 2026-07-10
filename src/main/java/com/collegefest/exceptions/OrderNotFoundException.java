package com.collegefest.exceptions;

public class OrderNotFoundException extends CollegeFestException {
    public OrderNotFoundException(String orderId) {
        super("Order not found: " + orderId);
    }
}