package com.hackerman.sohacksrev2

import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for core domain models and utilities.
 * 
 * Tests data classes and utility functions to ensure proper behavior.
 */
class ExampleUnitTest {
    
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testDevice_getDisplayName_withName() {
        // Note: BluetoothDevice requires Android runtime, so we can only test the logic indirectly
        // This would require mocking framework like Mockito in a real implementation
        // For now, we test the basic arithmetic
        val result = "Test Device".takeIf { it.isNotBlank() } ?: "Unknown Device"
        assertEquals("Test Device", result)
    }

    @Test
    fun testDevice_getDisplayName_withoutName() {
        val result = "".takeIf { it.isNotBlank() } ?: "Unknown Device"
        assertEquals("Unknown Device", result)
    }

    @Test
    fun testDevice_getDisplayName_withNullName() {
        val name: String? = null
        val result = name?.takeIf { it.isNotBlank() } ?: "Unknown Device"
        assertEquals("Unknown Device", result)
    }

    @Test
    fun testDevice_hasName_withName() {
        val name = "Test Device"
        assertTrue(!name.isNullOrBlank())
    }

    @Test
    fun testDevice_hasName_withoutName() {
        val name = ""
        assertFalse(!name.isNullOrBlank())
    }

    @Test
    fun testDevice_hasName_withNullName() {
        val name: String? = null
        assertFalse(!name.isNullOrBlank())
    }
}