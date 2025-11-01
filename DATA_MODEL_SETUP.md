# Nurtra Android Data Model Setup Guide

## Overview

This guide explains how the Android app's data model has been set up to match the Firebase backend structure used by other platforms (iOS, etc.). This ensures consistent data handling across all platforms sharing the same Firebase Firestore database.

## What's Been Implemented

### 1. Data Models (`DataModels.kt`)

Created strongly-typed Kotlin data classes that mirror the Firestore structure:

- **`NurtraUser`**: Main user profile with onboarding status
- **`OnboardingSurveyResponses`**: User's survey answers
- **`MotivationalQuote`**: Inspirational quotes
- **`TimerData`**: Timer session tracking
- **`BingeFreePeriod`**: Binge-free periods tracking

Each data model includes:
- Serialization to Firestore-compatible maps (`toMap()`)
- Deserialization from Firestore maps (`fromMap()`)
- Default values for all fields
- Proper handling of optional fields

### 2. Firestore Manager (`FirestoreManager.kt`)

Centralized management of all Firestore operations:

```kotlin
val firestoreManager = FirestoreManager()

// User management
firestoreManager.createOrUpdateUser(userId, email, displayName, photoUrl)
firestoreManager.getUser(userId)
firestoreManager.deleteUser(userId)

// Onboarding
firestoreManager.updateOnboardingData(userId, responses)

// Timer sessions
firestoreManager.saveTimerSession(userId, timerData)
firestoreManager.getTimerSessions(userId)

// Binge-free periods
firestoreManager.saveBingeFreePeriod(userId, bingeFreePeriod)
firestoreManager.getBingeFreePeriods(userId)

// Quotes
firestoreManager.getMotivationalQuotes()
```

### 3. AuthViewModel Integration

Updated `AuthViewModel` to automatically create/update Firestore user documents on authentication:

- Email sign up → Creates Firestore user document
- Email sign in → Updates Firestore user document
- Google sign in/up → Creates/updates with Google profile data

The view model now tracks both:
- Firebase Auth user (`user: FirebaseUser`)
- Firestore user data (`nurtraUser: NurtraUser`)

### 4. Dependencies

Added Firebase Firestore to the project:

```gradle
implementation(libs.firebase.firestore)
implementation(libs.firebase.firestore.ktx)
```

## Usage Examples

### Creating a User After Sign Up

```kotlin
// Called automatically in AuthViewModel.signUpWithEmail()
viewModel.signUpWithEmail("user@example.com", "password123") { success, error ->
    if (success) {
        // User document automatically created in Firestore
        val nurtraUser = viewModel.uiState.value.nurtraUser
        println("User created: ${nurtraUser?.email}")
    }
}
```

### Saving Onboarding Data

```kotlin
val responses = OnboardingSurveyResponses(
    struggleDuration = listOf("1-2 years"),
    bingeFrequency = listOf("daily"),
    importanceReason = listOf("health"),
    lifeWithoutBinge = listOf("peaceful"),
    bingeThoughts = listOf("I can't stop"),
    bingeTriggers = listOf("stress"),
    whatMattersMost = listOf("family"),
    recoveryValues = listOf("self-care")
)

firestoreManager.updateOnboardingData(userId, responses)
```

### Saving a Timer Session

```kotlin
val timerData = TimerData(
    startTime = Timestamp.now(),
    isRunning = false,
    stopTime = Timestamp.now()
)

firestoreManager.saveTimerSession(userId, timerData)
```

### Saving a Binge-Free Period

```kotlin
val bingeFreePeriod = BingeFreePeriod(
    startTime = Timestamp(startDate),
    endTime = Timestamp(endDate),
    duration = durationInMillis,
    createdAt = Timestamp.now()
)

firestoreManager.saveBingeFreePeriod(userId, bingeFreePeriod)
```

### Retrieving User Data

```kotlin
firestoreManager.getUser(userId).let { result ->
    result.onSuccess { nurtraUser ->
        println("Loaded user: ${nurtraUser?.displayName}")
    }.onFailure { error ->
        println("Error: ${error.message}")
    }
}
```

## Firestore Collection Structure

```
firestore/
├── users/{userId}
│   ├── userId
│   ├── email
│   ├── displayName (optional)
│   ├── photoUrl (optional)
│   ├── createdAt
│   ├── updatedAt
│   ├── onboardingCompleted
│   ├── onboardingCompletedAt (optional)
│   ├── onboardingResponses (nested map)
│   ├── timerSessions/ (subcollection)
│   │   └── {sessionId}
│   └── bingeFreePeriods/ (subcollection)
│       └── {periodId}
└── motivationalQuotes/{quoteId}
```

## Setting Up Firestore Rules

Configure the following security rules in Firebase Console:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Users can only access their own documents
    match /users/{userId} {
      allow read, write: if request.auth.uid == userId;
      
      // All subcollections under user document
      match /{document=**} {
        allow read, write: if request.auth.uid == userId;
      }
    }
    
    // Everyone can read motivational quotes (no authentication needed)
    match /motivationalQuotes/{document=**} {
      allow read: if request.auth != null;
      allow write: if false; // Admin only
    }
  }
}
```

## Testing

Run the unit tests to verify data model serialization:

```bash
./gradlew test
```

Tests include:
- Serialization/deserialization round-trips for all models
- Default value handling
- Missing field handling
- Timestamp preservation

## Error Handling

All Firestore operations return `Result<T>` for consistent error handling:

```kotlin
val result = firestoreManager.getUser(userId)

result.onSuccess { user ->
    // Handle success
}.onFailure { error ->
    // Handle error
    when (error) {
        is FirebaseFirestoreException -> {
            // Network error
        }
        else -> {
            // Other errors
        }
    }
}
```

## Data Consistency with iOS

The Android data model exactly mirrors the iOS (Swift) implementation:

| Field | Android Type | iOS Type | Firestore Type |
|-------|---|---|---|
| userId | String | String | String |
| email | String | String | String |
| createdAt | Timestamp | Timestamp | Timestamp |
| onboardingResponses | Map | Dictionary | Map |
| timerSessions | List<TimerData> | [TimerData] | Array of documents |

## Next Steps

1. **Implement Onboarding Flow**: Create onboarding screens that collect survey responses
2. **Implement Timer Screen**: Save timer sessions to Firestore
3. **Implement Statistics**: Retrieve and display binge-free period analytics
4. **Implement Motivational Quotes**: Display quotes from Firestore
5. **Add Real-time Listeners**: Use `addSnapshotListener` for live updates

## Troubleshooting

### User Document Not Created
- Check Firebase Authentication is enabled
- Verify Firestore security rules allow write to `/users/{userId}`
- Check device console logs for Firestore errors

### Timestamp Issues
- Always use `com.google.firebase.Timestamp`, not `java.util.Date`
- Use `Timestamp.now()` for current time
- Use `Timestamp(date: Date)` for specific dates

### Data Not Appearing in Console
- Wait a few seconds after write (slight delay possible)
- Check Firestore Rules - may be blocking reads
- Verify userId matches Firebase Auth UID
- Check network connectivity

## Performance Considerations

- **Subcollections**: Timer sessions and binge-free periods stored as subcollections for better scalability
- **Indexes**: Firestore will automatically create indexes as needed
- **Caching**: Consider implementing local caching for frequently accessed data
- **Pagination**: For users with many historical records, implement pagination

## See Also

- `DATA_MODEL_DOCUMENTATION.md` - Complete API reference
- `DataModels.kt` - Kotlin data class definitions
- `FirestoreManager.kt` - Firestore operations
- `AuthViewModel.kt` - Authentication and user loading
