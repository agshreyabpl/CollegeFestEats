package com.collegefest.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AccountService.createAccount() itself needs a live MongoDB connection
 * (it builds a real UserDAO internally with no dependency-injection seam),
 * so it can't be exercised as a pure unit test here. What CAN be tested
 * without a database is the public USERNAME_PATTERN regex it validates
 * against first - these are exactly the boundary cases createAccount()
 * relies on before it ever touches the network.
 */
public class AccountServiceTest {

    @Test
    public void exactlyThreeCharactersIsTheShortestValidUsername() {
        assertTrue(AccountService.USERNAME_PATTERN.matcher("abc").matches());
        assertTrue(AccountService.USERNAME_PATTERN.matcher("ab_").matches());
    }

    @Test
    public void twoCharactersIsOneShortOfValid() {
        assertFalse(AccountService.USERNAME_PATTERN.matcher("ab").matches());
    }

    @Test
    public void exactlyTwentyCharactersIsTheLongestValidUsername() {
        String twenty = "a".repeat(20);
        assertEquals(20, twenty.length());
        assertTrue(AccountService.USERNAME_PATTERN.matcher(twenty).matches());
    }

    @Test
    public void twentyOneCharactersIsOneTooMany() {
        String tooLong = "a".repeat(21);
        assertFalse(AccountService.USERNAME_PATTERN.matcher(tooLong).matches());
    }

    @Test
    public void lettersDigitsAndUnderscoreAreAllAllowed() {
        assertTrue(AccountService.USERNAME_PATTERN.matcher("STU001").matches());
        assertTrue(AccountService.USERNAME_PATTERN.matcher("vendor_2").matches());
        assertTrue(AccountService.USERNAME_PATTERN.matcher("___").matches());
        assertTrue(AccountService.USERNAME_PATTERN.matcher("123456").matches());
    }

    @Test
    public void spacesAndPunctuationAreRejected() {
        assertFalse(AccountService.USERNAME_PATTERN.matcher("stu 001").matches());
        assertFalse(AccountService.USERNAME_PATTERN.matcher("vendor@1").matches());
        assertFalse(AccountService.USERNAME_PATTERN.matcher("john.doe").matches());
        assertFalse(AccountService.USERNAME_PATTERN.matcher("name-1").matches());
    }

    @Test
    public void emptyOrNullTextNeverMatches() {
        assertFalse(AccountService.USERNAME_PATTERN.matcher("").matches());
        assertThrows(NullPointerException.class,
                () -> AccountService.USERNAME_PATTERN.matcher(null).matches());
    }

    @Test
    public void partialMatchesInTheMiddleOfLongerTextAreRejectedByMatches() {
        // matches() requires the WHOLE string to conform, unlike find();
        // a valid-looking username embedded in junk must still fail.
        assertFalse(AccountService.USERNAME_PATTERN.matcher("  STU001  ").matches());
        assertFalse(AccountService.USERNAME_PATTERN.matcher("STU001\n").matches());
    }

    @Test
    public void rolesAreTheExpectedLowercaseLiterals() {
        assertEquals("student", AccountService.ROLE_STUDENT);
        assertEquals("vendor", AccountService.ROLE_VENDOR);
    }
}
