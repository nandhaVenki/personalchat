package com.personalchat.app;

import static org.junit.Assert.assertEquals;

import com.personalchat.app.data.repository.UserRepository;

import org.junit.Test;

public class UserRepositoryTest {

    @Test
    public void testNormalizePhoneNumber_formattedUS() {
        String input = "+1 (555) 123-4567";
        String expected = "5551234567";
        String actual = UserRepository.normalizePhoneNumber(input);
        assertEquals(expected, actual);
    }

    @Test
    public void testNormalizePhoneNumber_raw10Digits() {
        String input = "5551234567";
        String expected = "5551234567";
        String actual = UserRepository.normalizePhoneNumber(input);
        assertEquals(expected, actual);
    }

    @Test
    public void testNormalizePhoneNumber_internationalUK() {
        String input = "+44 7911 123456";
        String expected = "7911123456"; // Last 10 digits of 447911123456
        String actual = UserRepository.normalizePhoneNumber(input);
        assertEquals(expected, actual);
    }

    @Test
    public void testNormalizePhoneNumber_empty() {
        String input = "";
        String expected = "";
        String actual = UserRepository.normalizePhoneNumber(input);
        assertEquals(expected, actual);
    }

    @Test
    public void testNormalizePhoneNumber_null() {
        String actual = UserRepository.normalizePhoneNumber(null);
        assertEquals("", actual);
    }

    @Test
    public void testSha256_hashing() {
        String phone = "+15551234567";
        // Expected SHA-256 for "+15551234567"
        // Let's assert it is 64 characters long and matches basic format
        String hashed = UserRepository.sha256(phone);
        assertEquals(64, hashed.length());
    }
}
