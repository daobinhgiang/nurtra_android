package com.example.nurtra_android.data

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * Manages Firestore operations for user data
 * Ensures consistency with the Swift app's data structure
 */
class FirestoreManager {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val TAG = "FirestoreManager"

    companion object {
        private const val USERS_COLLECTION = "users"
        private const val TIMER_SESSIONS_COLLECTION = "timerSessions"
        private const val BINGE_FREE_PERIODS_COLLECTION = "bingeFreePeriods"
        private const val MOTIVATIONAL_QUOTES_COLLECTION = "motivationalQuotes"
    }

    /**
     * Creates or updates a user document in Firestore after authentication
     * This should be called after successful sign up or sign in
     * Uses merge to preserve existing fields like onboardingCompleted
     */
    suspend fun createOrUpdateUser(
        userId: String,
        email: String,
        name: String? = null,
        platform: String = "Android"
    ): Result<NurtraUser> = try {
        val now = Timestamp.now()
        
        val userData: Map<String, Any?> = mapOf(
            "email" to email,
            "name" to name,
            "updatedAt" to now,
            "platform" to platform
        )

        // Use merge: true to preserve existing fields like onboardingCompleted
        db.collection(USERS_COLLECTION)
            .document(userId)
            .set(userData, com.google.firebase.firestore.SetOptions.merge())
            .await()

        Log.d(TAG, "User document created/updated: $userId")
        
        // Fetch the complete user data after merge to return accurate state
        val getUserResult = getUser(userId)
        val nurtraUser = getUserResult.getOrNull() ?: NurtraUser(
            email = email,
            name = name,
            updatedAt = now,
            platform = platform
        )
        
        Result.success(nurtraUser)
    } catch (e: Exception) {
        Log.e(TAG, "Error creating/updating user: ${e.message}", e)
        Result.failure(e)
    }

    /**
     * Retrieves user data from Firestore
     */
    suspend fun getUser(userId: String): Result<NurtraUser?> = try {
        val snapshot = db.collection(USERS_COLLECTION)
            .document(userId)
            .get()
            .await()

        val user = snapshot.data?.let { NurtraUser.fromMap(it) }
        Result.success(user)
    } catch (e: Exception) {
        Log.e(TAG, "Error fetching user: ${e.message}", e)
        Result.failure(e)
    }

    /**
     * Updates user onboarding status and responses
     */
    suspend fun updateOnboardingData(
        userId: String,
        responses: OnboardingSurveyResponses
    ): Result<Unit> = try {
        val updateData: Map<String, Any> = mapOf(
            "onboardingCompleted" to true,
            "onboardingCompletedAt" to Timestamp.now(),
            "onboardingResponses" to responses.toMap(),
            "updatedAt" to Timestamp.now()
        )

        db.collection(USERS_COLLECTION)
            .document(userId)
            .update(updateData)
            .await()

        Log.d(TAG, "Onboarding data updated for user: $userId")
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "Error updating onboarding data: ${e.message}", e)
        Result.failure(e)
    }

    /**
     * Saves a timer session
     */
    suspend fun saveTimerSession(
        userId: String,
        timerData: TimerData
    ): Result<String> = try {
        val sessionId = UUID.randomUUID().toString()
        val sessionData = timerData.toMap()

        db.collection(USERS_COLLECTION)
            .document(userId)
            .collection(TIMER_SESSIONS_COLLECTION)
            .document(sessionId)
            .set(sessionData)
            .await()

        Log.d(TAG, "Timer session saved: $sessionId for user: $userId")
        Result.success(sessionId)
    } catch (e: Exception) {
        Log.e(TAG, "Error saving timer session: ${e.message}", e)
        Result.failure(e)
    }

    /**
     * Saves a binge-free period
     */
    suspend fun saveBingeFreePeriod(
        userId: String,
        bingeFreePeriod: BingeFreePeriod
    ): Result<String> = try {
        val periodId = UUID.randomUUID().toString()
        val periodData = bingeFreePeriod.copy(id = periodId).toMap()

        db.collection(USERS_COLLECTION)
            .document(userId)
            .collection(BINGE_FREE_PERIODS_COLLECTION)
            .document(periodId)
            .set(periodData)
            .await()

        Log.d(TAG, "Binge-free period saved: $periodId for user: $userId")
        Result.success(periodId)
    } catch (e: Exception) {
        Log.e(TAG, "Error saving binge-free period: ${e.message}", e)
        Result.failure(e)
    }

    /**
     * Retrieves all binge-free periods for a user
     */
    suspend fun getBingeFreePeriods(userId: String): Result<List<BingeFreePeriod>> = try {
        val snapshot = db.collection(USERS_COLLECTION)
            .document(userId)
            .collection(BINGE_FREE_PERIODS_COLLECTION)
            .orderBy("createdAt")
            .get()
            .await()

        val periods = snapshot.documents.mapNotNull { doc ->
            doc.data?.let { BingeFreePeriod.fromMap(it) }
        }
        Result.success(periods)
    } catch (e: Exception) {
        Log.e(TAG, "Error fetching binge-free periods: ${e.message}", e)
        Result.failure(e)
    }

    /**
     * Retrieves the latest N binge-free periods for a user, ordered by creation date (newest first)
     */
    suspend fun getLatestBingeFreePeriods(userId: String, limit: Int = 3): Result<List<BingeFreePeriod>> = try {
        val snapshot = db.collection(USERS_COLLECTION)
            .document(userId)
            .collection(BINGE_FREE_PERIODS_COLLECTION)
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .get()
            .await()

        val periods = snapshot.documents.mapNotNull { doc ->
            doc.data?.let { BingeFreePeriod.fromMap(it) }
        }
        Result.success(periods)
    } catch (e: Exception) {
        Log.e(TAG, "Error fetching latest binge-free periods: ${e.message}", e)
        Result.failure(e)
    }

    /**
     * Retrieves all timer sessions for a user
     */
    suspend fun getTimerSessions(userId: String): Result<List<TimerData>> = try {
        val snapshot = db.collection(USERS_COLLECTION)
            .document(userId)
            .collection(TIMER_SESSIONS_COLLECTION)
            .orderBy("startTime")
            .get()
            .await()

        val sessions = snapshot.documents.mapNotNull { doc ->
            doc.data?.let { TimerData.fromMap(it) }
        }
        Result.success(sessions)
    } catch (e: Exception) {
        Log.e(TAG, "Error fetching timer sessions: ${e.message}", e)
        Result.failure(e)
    }

    /**
     * Retrieves all motivational quotes
     */
    suspend fun getMotivationalQuotes(): Result<List<MotivationalQuote>> = try {
        val snapshot = db.collection(MOTIVATIONAL_QUOTES_COLLECTION)
            .orderBy("order")
            .get()
            .await()

        val quotes = snapshot.documents.mapNotNull { doc ->
            doc.data?.let { MotivationalQuote.fromMap(it) }
        }
        Result.success(quotes)
    } catch (e: Exception) {
        Log.e(TAG, "Error fetching motivational quotes: ${e.message}", e)
        Result.failure(e)
    }

    /**
     * Updates the FCM token for a user in Firestore
     */
    suspend fun updateFCMToken(userId: String, fcmToken: String): Result<Unit> = try {
        val now = Timestamp.now()
        val updateData: Map<String, Any> = mapOf(
            "fcmToken" to fcmToken,
            "fcmTokenUpdatedAt" to now,
            "updatedAt" to now
        )

        db.collection(USERS_COLLECTION)
            .document(userId)
            .update(updateData)
            .await()

        Log.d(TAG, "FCM token updated for user: $userId")
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "Error updating FCM token: ${e.message}", e)
        Result.failure(e)
    }

    /**
     * Increments the overcomeCount for a user atomically
     * Uses FieldValue.increment() for thread-safe atomic increment operations
     */
    suspend fun incrementOvercomeCount(userId: String): Result<Int> = try {
        val userRef = db.collection(USERS_COLLECTION).document(userId)
        
        // Use FieldValue.increment() for atomic increment
        userRef.update(
            "overcomeCount", FieldValue.increment(1),
            "updatedAt", Timestamp.now()
        ).await()

        // Fetch the updated count to return it
        val snapshot = userRef.get().await()
        val newCount = (snapshot.get("overcomeCount") as? Number)?.toInt() ?: 0

        Log.d(TAG, "Overcome count incremented for user: $userId, new count: $newCount")
        Result.success(newCount)
    } catch (e: Exception) {
        Log.e(TAG, "Error incrementing overcome count: ${e.message}", e)
        Result.failure(e)
    }

    /**
     * Starts the timer by saving start time to Firebase
     */
    suspend fun startTimer(userId: String): Result<Timestamp> = try {
        val now = Timestamp.now()
        val updateData: Map<String, Any> = mapOf(
            "timerIsRunning" to true,
            "timerStartTime" to now,
            "timerLastUpdated" to now,
            "updatedAt" to now
        )

        db.collection(USERS_COLLECTION)
            .document(userId)
            .update(updateData)
            .await()

        Log.d(TAG, "Timer started for user: $userId at $now")
        Result.success(now)
    } catch (e: Exception) {
        Log.e(TAG, "Error starting timer: ${e.message}", e)
        Result.failure(e)
    }

    /**
     * Stops the timer by updating Firebase
     */
    suspend fun stopTimer(userId: String): Result<Unit> = try {
        val now = Timestamp.now()
        val updateData: Map<String, Any> = mapOf(
            "timerIsRunning" to false,
            "timerLastUpdated" to now,
            "updatedAt" to now
        )

        db.collection(USERS_COLLECTION)
            .document(userId)
            .update(updateData)
            .await()

        Log.d(TAG, "Timer stopped for user: $userId")
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "Error stopping timer: ${e.message}", e)
        Result.failure(e)
    }

    /**
     * Resets the timer by clearing timer fields
     */
    suspend fun resetTimer(userId: String): Result<Unit> = try {
        val now = Timestamp.now()
        val updateData: Map<String, Any?> = mapOf(
            "timerIsRunning" to false,
            "timerStartTime" to null,
            "timerLastUpdated" to now,
            "updatedAt" to now
        )

        db.collection(USERS_COLLECTION)
            .document(userId)
            .update(updateData)
            .await()

        Log.d(TAG, "Timer reset for user: $userId")
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "Error resetting timer: ${e.message}", e)
        Result.failure(e)
    }

    /**
     * Updates the list of blocked apps for a user
     */
    suspend fun updateBlockedApps(userId: String, blockedApps: List<String>): Result<Unit> = try {
        val now = Timestamp.now()
        val updateData: Map<String, Any> = mapOf(
            "blockedApps" to blockedApps,
            "updatedAt" to now
        )

        db.collection(USERS_COLLECTION)
            .document(userId)
            .update(updateData)
            .await()

        Log.d(TAG, "Blocked apps updated for user: $userId, count: ${blockedApps.size}")
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "Error updating blocked apps: ${e.message}", e)
        Result.failure(e)
    }

    /**
     * Saves motivational quotes for a user
     */
    suspend fun saveMotivationalQuotes(userId: String, quotes: List<String>): Result<Unit> = try {
        val now = Timestamp.now()
        
        // Create a map of quotes with IDs (1-10)
        val quotesMap = quotes.mapIndexed { index, quote ->
            (index + 1).toString() to quote
        }.toMap()
        
        val updateData: Map<String, Any> = mapOf(
            "motivationalQuotes" to quotesMap,
            "motivationalQuotesGeneratedAt" to now,
            "updatedAt" to now
        )

        db.collection(USERS_COLLECTION)
            .document(userId)
            .update(updateData)
            .await()

        Log.d(TAG, "Motivational quotes saved for user: $userId, count: ${quotes.size}")
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "Error saving motivational quotes: ${e.message}", e)
        Result.failure(e)
    }

    /**
     * Retrieves motivational quotes for a user
     */
    suspend fun getUserMotivationalQuotes(userId: String): Result<List<String>> = try {
        val snapshot = db.collection(USERS_COLLECTION)
            .document(userId)
            .get()
            .await()

        val quotesMap = snapshot.data?.get("motivationalQuotes") as? Map<*, *>
        val quotes = quotesMap?.values
            ?.filterIsInstance<String>()
            ?.toList()
            ?: emptyList()

        Log.d(TAG, "Retrieved ${quotes.size} quotes for user: $userId")
        Result.success(quotes)
    } catch (e: Exception) {
        Log.e(TAG, "Error fetching user motivational quotes: ${e.message}", e)
        Result.failure(e)
    }

    /**
     * Deletes user data from Firestore
     */
    suspend fun deleteUser(userId: String): Result<Unit> = try {
        db.collection(USERS_COLLECTION)
            .document(userId)
            .delete()
            .await()

        Log.d(TAG, "User document deleted: $userId")
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "Error deleting user: ${e.message}", e)
        Result.failure(e)
    }

    /**
     * Deletes all user data including subcollections
     * This is used when a user deletes their account
     */
    suspend fun deleteUserCompletely(userId: String): Result<Unit> = try {
        val userRef = db.collection(USERS_COLLECTION).document(userId)
        
        // Delete all timer sessions
        val timerSessions = userRef.collection(TIMER_SESSIONS_COLLECTION).get().await()
        timerSessions.documents.forEach { doc ->
            doc.reference.delete().await()
        }
        Log.d(TAG, "Deleted ${timerSessions.size()} timer sessions for user: $userId")
        
        // Delete all binge-free periods
        val bingeFreePeriods = userRef.collection(BINGE_FREE_PERIODS_COLLECTION).get().await()
        bingeFreePeriods.documents.forEach { doc ->
            doc.reference.delete().await()
        }
        Log.d(TAG, "Deleted ${bingeFreePeriods.size()} binge-free periods for user: $userId")
        
        // Delete the user document itself
        userRef.delete().await()
        
        Log.d(TAG, "User and all associated data deleted completely: $userId")
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "Error deleting user completely: ${e.message}", e)
        Result.failure(e)
    }
}
