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
        val userId = "test-user-123"
        val email = "test@example.com"
        val fcmToken = "eO2bI_example_fcm_token_12345"

        val user = NurtraUser(
            userId = userId,
            email = email,
            fcmToken = fcmToken
        )

        assertEquals(userId, user.userId)
        assertEquals(email, user.email)
        assertEquals(fcmToken, user.fcmToken)
    }

    /**
     * Test that NurtraUser can be converted to and from Map with FCM token
     */
    @Test
    fun testNurtraUserMapConversionWithFCMToken() {
        val userId = "test-user-456"
        val email = "test2@example.com"
        val fcmToken = "another_example_fcm_token_67890"
        val displayName = "Test User"

        val originalUser = NurtraUser(
            userId = userId,
            email = email,
            displayName = displayName,
            fcmToken = fcmToken
        )

        // Convert to map
        val userMap = originalUser.toMap()

        // Verify FCM token is in map
        assertEquals(fcmToken, userMap["fcmToken"])
        assertEquals(userId, userMap["userId"])
        assertEquals(email, userMap["email"])
        assertEquals(displayName, userMap["displayName"])

        // Convert back from map
        val reconstructedUser = NurtraUser.fromMap(userMap)

        // Verify all fields match
        assertEquals(originalUser.userId, reconstructedUser.userId)
        assertEquals(originalUser.email, reconstructedUser.email)
        assertEquals(originalUser.displayName, reconstructedUser.displayName)
        assertEquals(originalUser.fcmToken, reconstructedUser.fcmToken)
    }

    /**
     * Test that NurtraUser handles null FCM token
     */
    @Test
    fun testNurtraUserWithNullFCMToken() {
        val user = NurtraUser(
            userId = "test-user-789",
            email = "test3@example.com",
            fcmToken = null
        )

        assertNull(user.fcmToken)

        // Convert to and from map
        val userMap = user.toMap()
        val reconstructedUser = NurtraUser.fromMap(userMap)

        assertNull(reconstructedUser.fcmToken)
    }

    /**
     * Test that map without FCM token defaults to null
     */
    @Test
    fun testNurtraUserFromMapWithoutFCMToken() {
        val map = mapOf(
            "userId" to "test-user-000",
            "email" to "test4@example.com"
        )

        val user = NurtraUser.fromMap(map)

        assertEquals("test-user-000", user.userId)
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
            userId = "user-123",
            email = "user@example.com",
            fcmToken = initialToken
        )

        var userMap = user1.toMap()
        assertEquals(initialToken, userMap["fcmToken"])

        // Simulate token update
        userMap = userMap.toMutableMap().apply {
            this["fcmToken"] = updatedToken
        }

        val user2 = NurtraUser.fromMap(userMap)
        assertEquals(updatedToken, user2.fcmToken)
    }
}
