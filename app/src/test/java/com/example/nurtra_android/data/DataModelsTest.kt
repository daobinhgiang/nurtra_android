package com.example.nurtra_android.data

import com.google.firebase.Timestamp
import org.junit.Test
import org.junit.Assert.*
import java.util.Date

class DataModelsTest {

    @Test
    fun testOnboardingSurveyResponsesSerializationRoundTrip() {
        val responses = OnboardingSurveyResponses(
            struggleDuration = listOf("1-2 years", "3-5 years"),
            bingeFrequency = listOf("daily", "several times a week"),
            importanceReason = listOf("health", "relationships"),
            lifeWithoutBinge = listOf("peaceful", "productive"),
            bingeThoughts = listOf("I can't control it", "I deserve this"),
            bingeTriggers = listOf("stress", "boredom"),
            whatMattersMost = listOf("family", "career"),
            recoveryValues = listOf("discipline", "self-care")
        )

        // Serialize to map
        val map = responses.toMap()

        // Verify map contains all fields
        assertEquals(listOf("1-2 years", "3-5 years"), map["struggleDuration"])
        assertEquals(listOf("daily", "several times a week"), map["bingeFrequency"])

        // Deserialize back
        val recovered = OnboardingSurveyResponses.fromMap(map)

        // Verify round trip
        assertEquals(responses, recovered)
    }

    @Test
    fun testMotivationalQuoteSerializationRoundTrip() {
        val timestamp = Timestamp(Date(1000000000))
        val quote = MotivationalQuote(
            id = "quote-1",
            text = "You are stronger than you think",
            order = 1,
            createdAt = timestamp
        )

        val map = quote.toMap()
        val recovered = MotivationalQuote.fromMap(map)

        assertEquals(quote, recovered)
    }

    @Test
    fun testTimerDataSerializationRoundTrip() {
        val startTime = Timestamp(Date(1000000000))
        val stopTime = Timestamp(Date(1000010000))
        val timerData = TimerData(
            startTime = startTime,
            isRunning = false,
            stopTime = stopTime
        )

        val map = timerData.toMap()
        val recovered = TimerData.fromMap(map)

        assertEquals(timerData, recovered)
    }

    @Test
    fun testBingeFreePeriodSerializationRoundTrip() {
        val now = Timestamp.now()
        val period = BingeFreePeriod(
            id = "period-1",
            startTime = now,
            endTime = Timestamp(Date(now.toDate().time + 86400000)), // +1 day
            duration = 86400000L, // 1 day in milliseconds
            createdAt = now
        )

        val map = period.toMap()
        val recovered = BingeFreePeriod.fromMap(map)

        assertEquals(period.id, recovered.id)
        assertEquals(period.duration, recovered.duration)
    }

    @Test
    fun testNurtraUserSerializationRoundTrip() {
        val now = Timestamp.now()
        val responses = OnboardingSurveyResponses(
            struggleDuration = listOf("1-2 years"),
            bingeFrequency = listOf("daily"),
            importanceReason = listOf("health"),
            lifeWithoutBinge = listOf("peaceful"),
            bingeThoughts = listOf("I can't control it"),
            bingeTriggers = listOf("stress"),
            whatMattersMost = listOf("family"),
            recoveryValues = listOf("discipline")
        )

        val user = NurtraUser(
            userId = "user-123",
            email = "user@example.com",
            displayName = "John Doe",
            photoUrl = "https://example.com/photo.jpg",
            createdAt = now,
            updatedAt = now,
            onboardingCompleted = true,
            onboardingCompletedAt = now,
            onboardingResponses = responses
        )

        val map = user.toMap()
        val recovered = NurtraUser.fromMap(map as Map<String, Any>)

        assertEquals(user.userId, recovered.userId)
        assertEquals(user.email, recovered.email)
        assertEquals(user.displayName, recovered.displayName)
        assertEquals(user.onboardingCompleted, recovered.onboardingCompleted)
        assertNotNull(recovered.onboardingResponses)
    }

    @Test
    fun testOnboardingSurveyResponsesFromMapWithMissingFields() {
        val map = mapOf(
            "struggleDuration" to listOf("1-2 years"),
            // Other fields missing
        )

        val responses = OnboardingSurveyResponses.fromMap(map)

        assertEquals(listOf("1-2 years"), responses.struggleDuration)
        assertEquals(emptyList<String>(), responses.bingeFrequency)
        assertEquals(emptyList<String>(), responses.importanceReason)
    }

    @Test
    fun testNurtraUserDefaultValues() {
        val user = NurtraUser()

        assertEquals("", user.userId)
        assertEquals("", user.email)
        assertNull(user.displayName)
        assertNull(user.photoUrl)
        assertFalse(user.onboardingCompleted)
    }

    @Test
    fun testTimerDataDefaultValues() {
        val timerData = TimerData()

        assertNull(timerData.startTime)
        assertFalse(timerData.isRunning)
        assertNull(timerData.stopTime)
    }

    @Test
    fun testBingeFreePeriodDefaultValues() {
        val period = BingeFreePeriod()

        assertEquals("", period.id)
        assertNull(period.startTime)
        assertNull(period.endTime)
        assertEquals(0L, period.duration)
    }
}
