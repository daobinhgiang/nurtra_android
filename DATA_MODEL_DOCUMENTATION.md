# Nurtra Data Model Documentation

## Overview

This document describes the unified user data model for the Nurtra application across all platforms (Android, iOS, etc.). All platforms share the same Firebase Firestore backend, ensuring data consistency and cross-platform compatibility.

## Firestore Collection Structure

```
firestore
├── users/
│   ├── {userId}/
│   │   ├── userId (string)
│   │   ├── email (string)
│   │   ├── displayName (string, optional)
│   │   ├── photoUrl (string, optional)
│   │   ├── createdAt (Timestamp)
│   │   ├── updatedAt (Timestamp)
│   │   ├── onboardingCompleted (boolean)
│   │   ├── onboardingCompletedAt (Timestamp, optional)
│   │   ├── onboardingResponses (map, optional)
│   │   │   ├── struggleDuration (array of strings)
│   │   │   ├── bingeFrequency (array of strings)
│   │   │   ├── importanceReason (array of strings)
│   │   │   ├── lifeWithoutBinge (array of strings)
│   │   │   ├── bingeThoughts (array of strings)
│   │   │   ├── bingeTriggers (array of strings)
│   │   │   ├── whatMattersMost (array of strings)
│   │   │   └── recoveryValues (array of strings)
│   │   ├── timerSessions/ (subcollection)
│   │   │   └── {sessionId}/
│   │   │       ├── startTime (Timestamp)
│   │   │       ├── isRunning (boolean)
│   │   │       └── stopTime (Timestamp, optional)
│   │   └── bingeFreePeriods/ (subcollection)
│   │       └── {periodId}/
│   │           ├── id (string)
│   │           ├── startTime (Timestamp)
│   │           ├── endTime (Timestamp)
│   │           ├── duration (number, milliseconds)
│   │           └── createdAt (Timestamp)
├── motivationalQuotes/
│   └── {quoteId}/
│       ├── id (string)
│       ├── text (string)
│       ├── order (number)
│       └── createdAt (Timestamp)
```

## Data Models

### 1. NurtraUser

Main user profile document stored in Firestore.

**Fields:**
- `userId` (String): Firebase Authentication UID
- `email` (String): User's email address
- `displayName` (String, optional): User's display name
- `photoUrl` (String, optional): URL to user's profile photo
- `createdAt` (Timestamp): Account creation timestamp
- `updatedAt` (Timestamp): Last update timestamp
- `onboardingCompleted` (Boolean): Whether user completed onboarding
- `onboardingCompletedAt` (Timestamp, optional): When onboarding was completed
- `onboardingResponses` (OnboardingSurveyResponses, optional): User's onboarding survey answers

**Kotlin Class:**
```kotlin
data class NurtraUser(
    val userId: String,
    val email: String,
    val displayName: String?,
    val photoUrl: String?,
    val createdAt: Timestamp?,
    val updatedAt: Timestamp?,
    val onboardingCompleted: Boolean,
    val onboardingCompletedAt: Timestamp?,
    val onboardingResponses: OnboardingSurveyResponses?
)
```

### 2. OnboardingSurveyResponses

User's answers to the onboarding survey questions.

**Fields:**
- `struggleDuration` (List<String>): Responses about struggle duration
- `bingeFrequency` (List<String>): Responses about binge frequency
- `importanceReason` (List<String>): Reasons why recovery is important
- `lifeWithoutBinge` (List<String>): Vision for life without binge eating
- `bingeThoughts` (List<String>): Common thoughts during binges
- `bingeTriggers` (List<String>): Identified triggers for binge eating
- `whatMattersMost` (List<String>): What matters most to the user
- `recoveryValues` (List<String>): Values related to recovery

**Kotlin Class:**
```kotlin
data class OnboardingSurveyResponses(
    val struggleDuration: List<String>,
    val bingeFrequency: List<String>,
    val importanceReason: List<String>,
    val lifeWithoutBinge: List<String>,
    val bingeThoughts: List<String>,
    val bingeTriggers: List<String>,
    val whatMattersMost: List<String>,
    val recoveryValues: List<String>
)
```

### 3. MotivationalQuote

Motivational quotes displayed to users.

**Fields:**
- `id` (String): Unique identifier for the quote
- `text` (String): The quote text
- `order` (Int): Display order in the list
- `createdAt` (Timestamp): When the quote was created

**Kotlin Class:**
```kotlin
data class MotivationalQuote(
    val id: String,
    val text: String,
    val order: Int,
    val createdAt: Timestamp?
)
```

### 4. TimerData

Individual timer session data.

**Fields:**
- `startTime` (Timestamp): When the timer session started
- `isRunning` (Boolean): Whether the timer is currently running
- `stopTime` (Timestamp, optional): When the timer was stopped

**Kotlin Class:**
```kotlin
data class TimerData(
    val startTime: Timestamp?,
    val isRunning: Boolean,
    val stopTime: Timestamp?
)
```

**Storage:**
- Stored as documents in subcollection: `users/{userId}/timerSessions/{sessionId}`

### 5. BingeFreePeriod

Tracks periods when the user has been binge-free.

**Fields:**
- `id` (String): Unique identifier for the period
- `startTime` (Timestamp): When the binge-free period started
- `endTime` (Timestamp): When the binge-free period ended
- `duration` (Long): Duration in milliseconds
- `createdAt` (Timestamp): When this record was created

**Kotlin Class:**
```kotlin
data class BingeFreePeriod(
    val id: String,
    val startTime: Timestamp?,
    val endTime: Timestamp?,
    val duration: Long,
    val createdAt: Timestamp?
)
```

**Storage:**
- Stored as documents in subcollection: `users/{userId}/bingeFreePeriods/{periodId}`

## FirestoreManager API

The `FirestoreManager` class provides methods for all Firestore operations.

### User Management

```kotlin
// Create or update user after authentication
suspend fun createOrUpdateUser(
    userId: String,
    email: String,
    displayName: String? = null,
    photoUrl: String? = null
): Result<NurtraUser>

// Retrieve user data
suspend fun getUser(userId: String): Result<NurtraUser?>

// Delete user data
suspend fun deleteUser(userId: String): Result<Unit>
```

### Onboarding

```kotlin
// Update onboarding data
suspend fun updateOnboardingData(
    userId: String,
    responses: OnboardingSurveyResponses
): Result<Unit>
```

### Timer Sessions

```kotlin
// Save a timer session
suspend fun saveTimerSession(
    userId: String,
    timerData: TimerData
): Result<String>

// Get all timer sessions for a user
suspend fun getTimerSessions(userId: String): Result<List<TimerData>>
```

### Binge-Free Periods

```kotlin
// Save a binge-free period
suspend fun saveBingeFreePeriod(
    userId: String,
    bingeFreePeriod: BingeFreePeriod
): Result<String>

// Get all binge-free periods for a user
suspend fun getBingeFreePeriods(userId: String): Result<List<BingeFreePeriod>>
```

### Motivational Quotes

```kotlin
// Get all motivational quotes
suspend fun getMotivationalQuotes(): Result<List<MotivationalQuote>>
```

## Integration with AuthViewModel

The `AuthViewModel` automatically creates/updates Firestore user documents when users authenticate:

1. **Sign Up with Email**: Creates new Firestore document
2. **Sign In with Email**: Updates existing Firestore document
3. **Sign In with Google**: Creates or updates Firestore document with Google profile info

```kotlin
// AuthViewModel now tracks both Firebase Auth user and Firestore user data
data class AuthUiState(
    val isLoading: Boolean,
    val user: FirebaseUser?,          // Firebase Auth user
    val nurtraUser: NurtraUser?,      // Firestore user document
    val errorMessage: String?
)
```

## Firestore Security Rules

Recommended security rules for protecting user data:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Users can only access their own documents
    match /users/{userId} {
      allow read, write: if request.auth.uid == userId;
      
      // Subcollections (timerSessions, bingeFreePeriods)
      match /{document=**} {
        allow read, write: if request.auth.uid == userId;
      }
    }
    
    // Everyone can read motivational quotes
    match /motivationalQuotes/{document=**} {
      allow read: if request.auth != null;
      allow write: if false; // Only admins via Cloud Functions
    }
  }
}
```

## Data Consistency Across Platforms

### iOS (Swift)
- Uses Firebase Auth for authentication
- Stores user data in Firestore with same schema
- Uses `Timestamp` for date fields
- Data models match the Android Kotlin implementation

### Android (Kotlin)
- Uses Firebase Auth for authentication
- Stores user data in Firestore with same schema
- Uses `com.google.firebase.Timestamp` for date fields
- Data models defined in `DataModels.kt`

### Common Patterns

1. **User ID**: Always uses Firebase Authentication UID
2. **Timestamps**: Uses Firebase Timestamp type across all platforms
3. **Collections**: Top-level `users` collection with document per user
4. **Subcollections**: User-specific data in subcollections under user document
5. **Error Handling**: All operations return `Result<T>` type for consistent error handling

## Migration Notes

If migrating from an existing data structure:

1. Run one-time migration script to transform existing data to new schema
2. Ensure all timestamp fields use Firebase Timestamp format
3. Test data access from both iOS and Android simultaneously
4. Verify that subcollections are properly created for existing users

## Testing

Example test for creating a user:

```kotlin
@Test
fun testCreateUser() = runTest {
    val manager = FirestoreManager()
    val result = manager.createOrUpdateUser(
        userId = "test123",
        email = "test@example.com",
        displayName = "Test User"
    )
    
    assertTrue(result.isSuccess)
    val user = result.getOrNull()
    assertEquals("test123", user?.userId)
    assertEquals("test@example.com", user?.email)
}
```

## Future Enhancements

- Add pagination for large datasets (timer sessions, binge-free periods)
- Implement real-time listeners for live data updates
- Add data aggregation (statistics, trends)
- Implement offline-first caching strategy
- Add data export functionality for user privacy

