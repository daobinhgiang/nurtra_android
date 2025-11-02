package com.example.nurtra_android.data

import com.google.firebase.Timestamp
import java.util.Date

/**
 * Main User data model for Firestore
 * Mirrors the structure used in the Swift app's FirestoreManager
 */
data class NurtraUser(
    val email: String = "",
    val name: String? = null,
    val fcmToken: String? = null,
    val fcmTokenUpdatedAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
    val onboardingCompleted: Boolean = false,
    val onboardingCompletedAt: Timestamp? = null,
    val onboardingResponses: OnboardingSurveyResponses? = null,
    val motivationalQuotesGeneratedAt: Timestamp? = null,
    val overcomeCount: Int = 0,
    val platform: String? = null,
    val timerIsRunning: Boolean = false,
    val timerLastUpdated: Timestamp? = null,
    val timerStartTime: Timestamp? = null,
    val blockedApps: List<String> = emptyList()
) {
    companion object {
        fun fromMap(map: Map<String, Any>): NurtraUser {
            return NurtraUser(
                email = map["email"] as? String ?: "",
                name = map["name"] as? String,
                fcmToken = map["fcmToken"] as? String,
                fcmTokenUpdatedAt = map["fcmTokenUpdatedAt"] as? Timestamp,
                updatedAt = map["updatedAt"] as? Timestamp,
                onboardingCompleted = map["onboardingCompleted"] as? Boolean ?: false,
                onboardingCompletedAt = map["onboardingCompletedAt"] as? Timestamp,
                onboardingResponses = @Suppress("UNCHECKED_CAST") (map["onboardingResponses"] as? Map<String, Any>)?.let {
                    OnboardingSurveyResponses.fromMap(it)
                },
                motivationalQuotesGeneratedAt = map["motivationalQuotesGeneratedAt"] as? Timestamp,
                overcomeCount = (map["overcomeCount"] as? Number)?.toInt() ?: 0,
                platform = map["platform"] as? String,
                timerIsRunning = map["timerIsRunning"] as? Boolean ?: false,
                timerLastUpdated = map["timerLastUpdated"] as? Timestamp,
                timerStartTime = map["timerStartTime"] as? Timestamp,
                blockedApps = (map["blockedApps"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            )
        }
    }

    fun toMap(): Map<String, Any?> {
        return mapOf(
            "email" to email,
            "name" to name,
            "fcmToken" to fcmToken,
            "fcmTokenUpdatedAt" to fcmTokenUpdatedAt,
            "updatedAt" to updatedAt,
            "onboardingCompleted" to onboardingCompleted,
            "onboardingCompletedAt" to onboardingCompletedAt,
            "onboardingResponses" to onboardingResponses?.toMap(),
            "motivationalQuotesGeneratedAt" to motivationalQuotesGeneratedAt,
            "overcomeCount" to overcomeCount,
            "platform" to platform,
            "timerIsRunning" to timerIsRunning,
            "timerLastUpdated" to timerLastUpdated,
            "timerStartTime" to timerStartTime,
            "blockedApps" to blockedApps
        )
    }
}

/**
 * Onboarding survey responses from user
 * Mirrors the Swift app's OnboardingSurveyResponses structure
 */
data class OnboardingSurveyResponses(
    val struggleDuration: List<String> = emptyList(),
    val bingeFrequency: List<String> = emptyList(),
    val importanceReason: List<String> = emptyList(),
    val lifeWithoutBinge: List<String> = emptyList(),
    val bingeThoughts: List<String> = emptyList(),
    val bingeTriggers: List<String> = emptyList(),
    val whatMattersMost: List<String> = emptyList(),
    val recoveryValues: List<String> = emptyList(),
    val copingActivities: List<String> = emptyList()
) {
    companion object {
        fun fromMap(map: Map<String, Any>): OnboardingSurveyResponses {
            return OnboardingSurveyResponses(
                struggleDuration = (map["struggleDuration"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                bingeFrequency = (map["bingeFrequency"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                importanceReason = (map["importanceReason"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                lifeWithoutBinge = (map["lifeWithoutBinge"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                bingeThoughts = (map["bingeThoughts"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                bingeTriggers = (map["bingeTriggers"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                whatMattersMost = (map["whatMattersMost"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                recoveryValues = (map["recoveryValues"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                copingActivities = (map["copingActivities"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            )
        }
    }

    fun toMap(): Map<String, Any> {
        return mapOf(
            "struggleDuration" to struggleDuration,
            "bingeFrequency" to bingeFrequency,
            "importanceReason" to importanceReason,
            "lifeWithoutBinge" to lifeWithoutBinge,
            "bingeThoughts" to bingeThoughts,
            "bingeTriggers" to bingeTriggers,
            "whatMattersMost" to whatMattersMost,
            "recoveryValues" to recoveryValues,
            "copingActivities" to copingActivities
        )
    }
}

/**
 * Motivational quote model
 * Mirrors the Swift app's MotivationalQuote structure
 */
data class MotivationalQuote(
    val id: String = "",
    val text: String = "",
    val order: Int = 0,
    val createdAt: Timestamp? = null
) {
    companion object {
        fun fromMap(map: Map<String, Any>): MotivationalQuote {
            return MotivationalQuote(
                id = map["id"] as? String ?: "",
                text = map["text"] as? String ?: "",
                order = (map["order"] as? Number)?.toInt() ?: 0,
                createdAt = map["createdAt"] as? Timestamp
            )
        }
    }

    fun toMap(): Map<String, Any> {
        @Suppress("UNCHECKED_CAST")
        return mapOf(
            "id" to id,
            "text" to text,
            "order" to order,
            "createdAt" to createdAt
        ) as Map<String, Any>
    }
}

/**
 * Timer session data model
 * Mirrors the Swift app's TimerData structure
 */
data class TimerData(
    val startTime: Timestamp? = null,
    val isRunning: Boolean = false,
    val stopTime: Timestamp? = null
) {
    companion object {
        fun fromMap(map: Map<String, Any>): TimerData {
            return TimerData(
                startTime = map["startTime"] as? Timestamp,
                isRunning = map["isRunning"] as? Boolean ?: false,
                stopTime = map["stopTime"] as? Timestamp
            )
        }
    }

    fun toMap(): Map<String, Any?> {
        return mapOf(
            "startTime" to startTime,
            "isRunning" to isRunning,
            "stopTime" to stopTime
        )
    }
}

/**
 * Binge-free period tracking model
 * Mirrors the Swift app's BingeFreePeriod structure
 */
data class BingeFreePeriod(
    val id: String = "",
    val startTime: Timestamp? = null,
    val endTime: Timestamp? = null,
    val duration: Long = 0L, // in milliseconds
    val createdAt: Timestamp? = null
) {
    companion object {
        fun fromMap(map: Map<String, Any>): BingeFreePeriod {
            return BingeFreePeriod(
                id = map["id"] as? String ?: "",
                startTime = map["startTime"] as? Timestamp,
                endTime = map["endTime"] as? Timestamp,
                duration = (map["duration"] as? Number)?.toLong() ?: 0L,
                createdAt = map["createdAt"] as? Timestamp
            )
        }
    }

    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "startTime" to startTime,
            "endTime" to endTime,
            "duration" to duration,
            "createdAt" to createdAt
        )
    }
}
