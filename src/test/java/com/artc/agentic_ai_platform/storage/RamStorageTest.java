package com.artc.agentic_ai_platform.storage;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class RamStorageTest {

    @Test
    void saveAndGet_ShouldPersistData() {
        // Arrange
        RamStorage storage = new RamStorage(100);

        // Act
        storage.save("k1", "value1");

        // Assert
        Optional<String> result = storage.get("k1", String.class);
        assertTrue(result.isPresent());
        assertEquals("value1", result.get());
    }

    @Test
    void delete_ShouldRemoveData() {
        // Arrange
        RamStorage storage = new RamStorage(100);
        storage.save("k1", "value1");

        // Act
        storage.delete("k1");

        // Assert
        assertTrue(storage.get("k1", String.class).isEmpty());
    }

    @Test
    void lruEviction_ShouldRemoveOldestAccessed_WhenLimitExceeded() {
        // --- ARRANGE ---
        // Create a tiny storage with limit of 2 items
        RamStorage storage = new RamStorage(2);

        storage.save("A", "val-A");
        storage.save("B", "val-B"); // latest entry

        // --- Access A to make it "new" ---
        storage.get("A", String.class);

        // Current State: [B, A] (A is now newest because we accessed it)

        // --- Add C (overflow) ---
        storage.save("C", "val-C");

        // --- ASSERT ---
        // Expected State: [A, C] (B should be evicted)
        assertTrue(storage.get("B", String.class).isEmpty(), "Item B should have been evicted");
        assertTrue(storage.get("A", String.class).isPresent(), "Item A should remain");
        assertTrue(storage.get("C", String.class).isPresent(), "Item C should exist");
    }

    @Test
    void get_ShouldThrowException_WhenTypeMismatch() {
        // Edge case: Safety check for class casting
        RamStorage storage = new RamStorage(10);
        storage.save("k1", 123); // Save Integer

        // Act & Assert
        // Attempting to get Integer as String should handle cast cleanly or throw ClassCastException
        assertThrows(ClassCastException.class, () -> {
            storage.get("k1", String.class);
        });
    }
}
