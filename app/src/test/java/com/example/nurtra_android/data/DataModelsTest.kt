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
            recoveryValues = listOf("discipline", "self-care"),
            copingActivities = listOf("exercise", "meditation")
        )

        // Serialize to map
        val map = responses.toMap()

        // Verify map contains all fields
        assertEquals(listOf("1-2 years", "3-5 years"), map["struggleDuration"])
        assertEquals(listOf("daily", "several times a week"), map["bingeFrequency"])
        assertEquals(listOf("exercise", "meditation"), map["copingActivities"])

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
        @Suppress("UNCHECKED_CAST")
        val recovered = TimerData.fromMap(map as Map<String, Any>)

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
        @Suppress("UNCHECKED_CAST")
        val recovered = BingeFreePeriod.fromMap(map as Map<String, Any>)

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
            recoveryValues = listOf("discipline"),
            copingActivities = listOf("exercise")
        )

        val user = NurtraUser(
            email = "user@example.com",
            name = "John Doe",
            fcmToken = "test-fcm-token",
            fcmTokenUpdatedAt = now,
            updatedAt = now,
            onboardingCompleted = true,
            onboardingCompletedAt = now,
            onboardingResponses = responses,
            motivationalQuotesGeneratedAt = now,
            overcomeCount = 1,
            platform = "Android",
            timerIsRunning = true,
            timerLastUpdated = now,
            timerStartTime = now
        )

        val map = user.toMap()
        @Suppress("UNCHECKED_CAST")
        val recovered = NurtraUser.fromMap(map as Map<String, Any>)

        assertEquals(user.email, recovered.email)
        assertEquals(user.name, recovered.name)
        assertEquals(user.onboardingCompleted, recovered.onboardingCompleted)
        assertEquals(user.overcomeCount, recovered.overcomeCount)
        assertEquals(user.platform, recovered.platform)
        assertEquals(user.timerIsRunning, recovered.timerIsRunning)
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

        assertEquals("", user.email)
        assertNull(user.name)
        assertNull(user.fcmToken)
        assertEquals(0, user.overcomeCount)
        assertFalse(user.onboardingCompleted)
        assertFalse(user.timerIsRunning)
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
