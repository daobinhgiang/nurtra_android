package com.example.nurtra_android.data

import org.junit.Test
import org.junit.Assert.*

/**
 * Tests for FCM token functionality
 */
class FCMTokenTest {

    /**
     * Test that NurtraUser can be created with an FCM token
     */
    @Test
    fun testNurtraUserWithFCMToken() {
        val email = "test@example.com"
        val fcmToken = "eO2bI_example_fcm_token_12345"

        val user = NurtraUser(
            email = email,
            fcmToken = fcmToken
        )

        assertEquals(email, user.email)
        assertEquals(fcmToken, user.fcmToken)
    }

    /**
     * Test that NurtraUser can be converted to and from Map with FCM token
     */
    @Test
    fun testNurtraUserMapConversionWithFCMToken() {
        val email = "test2@example.com"
        val fcmToken = "another_example_fcm_token_67890"
        val name = "Test User"

        val originalUser = NurtraUser(
            email = email,
            name = name,
            fcmToken = fcmToken
        )

        // Convert to map
        val userMap = originalUser.toMap()

        // Verify FCM token is in map
        assertEquals(fcmToken, userMap["fcmToken"])
        assertEquals(email, userMap["email"])
        assertEquals(name, userMap["name"])

        // Convert back from map
        @Suppress("UNCHECKED_CAST")
        val reconstructedUser = NurtraUser.fromMap(userMap as Map<String, Any>)

        // Verify all fields match
        assertEquals(originalUser.email, reconstructedUser.email)
        assertEquals(originalUser.name, reconstructedUser.name)
        assertEquals(originalUser.fcmToken, reconstructedUser.fcmToken)
    }

    /**
     * Test that NurtraUser handles null FCM token
     */
    @Test
    fun testNurtraUserWithNullFCMToken() {
        val user = NurtraUser(
            email = "test3@example.com",
            fcmToken = null
        )

        assertNull(user.fcmToken)

        // Convert to and from map
        val userMap = user.toMap()
        @Suppress("UNCHECKED_CAST")
        val reconstructedUser = NurtraUser.fromMap(userMap as Map<String, Any>)

        assertNull(reconstructedUser.fcmToken)
    }

    /**
     * Test that map without FCM token defaults to null
     */
    @Test
    fun testNurtraUserFromMapWithoutFCMToken() {
        val map = mapOf(
            "email" to "test4@example.com"
        )

        val user = NurtraUser.fromMap(map)

        assertEquals("test4@example.com", user.email)
        assertNull(user.fcmToken)
    }

    /**
     * Test that FCM token can be updated in map representation
     */
    @Test
    fun testFCMTokenUpdateInMap() {
        val initialToken = "initial_fcm_token"
        val updatedToken = "updated_fcm_token"

        val user1 = NurtraUser(
            email = "user@example.com",
            fcmToken = initialToken
        )

        var userMap = user1.toMap()
        assertEquals(initialToken, userMap["fcmToken"])

        // Simulate token update
        userMap = userMap.toMutableMap().apply {
            this["fcmToken"] = updatedToken
        }

        @Suppress("UNCHECKED_CAST")
        val user2 = NurtraUser.fromMap(userMap as Map<String, Any>)
        assertEquals(updatedToken, user2.fcmToken)
    }
}
