package com.origin.urlshortener.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

class SnowflakeIdGeneratorTest {
    private SnowflakeIdGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new SnowflakeIdGenerator();
    }

    @Test
    void testGenerateShortCode() {
        String shortCode = generator.generateShortCode();
        assertNotNull(shortCode);
        assertEquals(11, shortCode.length(), "Short code should be 11 characters long");
        assertTrue(shortCode.matches("[0-9A-Za-z]+"), "Short code should only contain Base62 characters");
    }

    @Test
    void testGenerateUniqueIds() {
        String code1 = generator.generateShortCode();
        String code2 = generator.generateShortCode();
        assertNotEquals(code1, code2, "Generated codes should be unique");
    }

    @Test
    void testIdOrdering() {
        long id1 = generator.nextId();
        long id2 = generator.nextId();
        assertTrue(id2 > id1, "IDs should be monotonically increasing");
    }

    @Test
    void testBase62Encoding() {
        // Test with a known value
        long testId = 123456789L;
        String encoded = generator.generateShortCode();
        assertNotNull(encoded);
        assertEquals(11, encoded.length(), "Encoded string should be 11 characters long");
        
        // Verify all characters are valid Base62
        for (char c : encoded.toCharArray()) {
            assertTrue(
                (c >= '0' && c <= '9') || 
                (c >= 'A' && c <= 'Z') || 
                (c >= 'a' && c <= 'z'),
                "Character '" + c + "' is not a valid Base62 character"
            );
        }
    }

    @Test
    void testConcurrentGeneration() throws InterruptedException {
        // Test that multiple threads can generate IDs without conflicts
        Thread[] threads = new Thread[10];
        String[] codes = new String[10];
        
        for (int i = 0; i < threads.length; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                codes[index] = generator.generateShortCode();
            });
        }

        // Start all threads
        for (Thread thread : threads) {
            thread.start();
        }

        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }

        // Verify all codes are unique
        for (int i = 0; i < codes.length; i++) {
            assertNotNull(codes[i], "Generated code should not be null");
            assertEquals(11, codes[i].length(), "Generated code should be 11 characters long");
            for (int j = i + 1; j < codes.length; j++) {
                assertNotEquals(codes[i], codes[j], 
                    String.format("Codes at indices %d and %d should be different", i, j));
            }
        }
    }
} 