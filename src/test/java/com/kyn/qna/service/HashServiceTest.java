package com.kyn.qna.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HashServiceTest {

    private HashService hashService;

    @BeforeEach
    void setUp() {
        hashService = new HashService();
    }

    @Test
    void testGenerateSha256Hash_Success() {
        String input = "test";
        String expectedHash = "9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08";
        String actualHash = hashService.generateSha256Hash(input);
        assertEquals(expectedHash, actualHash, "SHA-256 hash for 'test' was incorrect.");
    }

    @Test
    void testGenerateSha256Hash_AnotherSuccessCase() {
        String input = "Hello World";
        // SHA-256 hash of "Hello World"
        String expectedHash = "a591a6d40bf420404a011733cfb7b190d62c65bf0bcda32b57b277d9ad9f146e";
        String actualHash = hashService.generateSha256Hash(input);
        assertEquals(expectedHash, actualHash, "SHA-256 hash for 'Hello World' was incorrect.");
    }

    @Test
    void testGenerateSha256Hash_EmptyInput() {
        String input = "";
        // SHA-256 hash of an empty string
        String expectedHash = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
        String actualHash = hashService.generateSha256Hash(input);
        assertEquals(expectedHash, actualHash, "SHA-256 hash for an empty string was incorrect.");
    }

    @Test
    void testGenerateSha256Hash_NullInput() {
        // As per current implementation, generateSha256Hash returns null for null input
        // based on: if (text == null) { return null; }
        String actualHash = hashService.generateSha256Hash(null);
        assertNull(actualHash, "SHA-256 hash for null input should be null.");
    }
    
    @Test
    void testGenerateSha256Hash_Consistency() {
        String input = "consistencyTest123!@#";
        String hash1 = hashService.generateSha256Hash(input);
        String hash2 = hashService.generateSha256Hash(input);
        assertEquals(hash1, hash2, "SHA-256 hash should be consistent for the same input.");
    }
}
