package com.collegefest.exceptions;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Custom exception hierarchy — everything roots in CollegeFestException. */
public class ExceptionsTest {

    @Test
    public void userNotFoundIsACollegeFestException() {
        UserNotFoundException e = new UserNotFoundException("STU404");
        assertTrue(e instanceof CollegeFestException);
        assertTrue(e instanceof RuntimeException, "unchecked by design");
    }

    @Test
    public void orderNotFoundIsACollegeFestException() {
        OrderNotFoundException e = new OrderNotFoundException("665f0000badbadbadbad0000");
        assertTrue(e instanceof CollegeFestException);
    }

    @Test
    public void messagesNameTheMissingThing() {
        assertTrue(new UserNotFoundException("STU404").getMessage().contains("STU404"));
        assertTrue(new OrderNotFoundException("missing-id").getMessage().contains("missing-id"));
    }

    @Test
    public void baseExceptionPreservesMessageAndCause() {
        Exception cause = new IllegalStateException("network down");
        CollegeFestException e = new CollegeFestException("db unreachable", cause);
        assertEquals("db unreachable", e.getMessage());
        assertEquals(cause, e.getCause());
    }
}
