# FCM Token Implementation Guide

## Overview

This document describes the Firebase Cloud Messaging (FCM) token implementation for the Nurtra Android app. The mechanism automatically generates and saves FCM tokens to Firestore after user login, enabling push notification capabilities.

## Architecture

### Components

#### 1. **FCMTokenManager** (`data/FCMTokenManager.kt`)
Manages FCM token operations:
- **`getToken()`**: Retrieves the current FCM token for the device
- **`subscribeToTopic(topic)`**: Subscribes the device to a topic for push notifications
- **`unsubscribeFromTopic(topic)`**: Unsubscribes from a topic

```
├── Handles token retrieval
├── Manages topic subscriptions
└── Provides error logging and feedback
```

#### 2. **MyFirebaseMessagingService** (`data/MyFirebaseMessagingService.kt`)
Extends `FirebaseMessagingService` to handle:
- **`onNewToken(token)`**: Called when a new FCM token is generated
  - Automatically saves the token to Firestore for the current user
  - Handles cases when no user is authenticated
- **`onMessageReceived(message)`**: Called when a message is received while app is in foreground
  - Logs message details for debugging
  - Can be extended for custom notification handling

```
├── Token refresh handling
├── Message reception handling
└── Automatic Firestore persistence
```

#### 3. **Updated FirestoreManager** (`data/FirestoreManager.kt`)
New method:
- **`updateFCMToken(userId, fcmToken)`**: Updates the FCM token field in Firestore
  - Updates the user document with the new token
  - Sets `updatedAt` timestamp

#### 4. **Updated NurtraUser Model** (`data/DataModels.kt`)
Added field:
- **`fcmToken: String?`**: Stores the user's FCM token
- Updated `toMap()` and `fromMap()` for serialization/deserialization

#### 5. **Updated AuthViewModel** (`auth/AuthViewModel.kt`)
Integrated FCM token handling in all login methods:
- **`signInWithEmail()`**: Gets and saves FCM token after sign-in
- **`signUpWithEmail()`**: Gets and saves FCM token after sign-up
- **`signInWithGoogleCredential()`**: Gets and saves FCM token after Google sign-in

```
Login Flow:
1. User authenticates with Firebase
2. Firestore user document is created/updated
3. FCM token is retrieved
4. FCM token is saved to Firestore under fcmToken field
```

## Data Structure

### Firestore User Document
```json
{
  "userId": "user123",
  "email": "user@example.com",
  "displayName": "John Doe",
  "photoUrl": "https://...",
  "fcmToken": "eO2bI_example_fcm_token_12345...",
  "createdAt": "2024-11-01T10:00:00Z",
  "updatedAt": "2024-11-01T10:05:00Z",
  "onboardingCompleted": true,
  "onboardingCompletedAt": "2024-11-01T10:02:00Z",
  "onboardingResponses": { ... }
}
```

## Implementation Details

### Dependencies Added
```kotlin
// gradle/libs.versions.toml
firebase-messaging = { group = "com.google.firebase", name = "firebase-messaging" }

// app/build.gradle.kts
implementation(libs.firebase.messaging)
```

### AndroidManifest Changes
```xml
<!-- Added permissions -->
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

<!-- Registered FCM service -->
<service
    android:name=".data.MyFirebaseMessagingService"
    android:exported="false">
    <intent-filter>
        <action android:name="com.google.firebase.MESSAGING_EVENT" />
    </intent-filter>
</service>
```

## Token Flow

### Initial Setup (First App Launch)
1. App starts → Firebase initializes
2. `MyFirebaseMessagingService` is registered
3. FCM generates initial token
4. `onNewToken()` is called
5. Token is saved to Firestore if user is authenticated

### After Login
1. User completes authentication
2. `AuthViewModel` calls FCM token retrieval
3. `FirestoreManager.updateFCMToken()` saves token to Firestore
4. Token is now available for sending push notifications

### Token Refresh (Automatic)
1. Firebase detects need for token refresh (app data cleared, etc.)
2. FCM generates new token
3. `onNewToken()` is automatically called
4. New token is saved to Firestore (overwriting old one)

## Testing

Comprehensive tests are provided in `data/FCMTokenTest.kt`:

1. **`testNurtraUserWithFCMToken()`**
   - Tests user creation with FCM token

2. **`testNurtraUserMapConversionWithFCMToken()`**
   - Tests serialization/deserialization with FCM token

3. **`testNurtraUserWithNullFCMToken()`**
   - Tests null token handling

4. **`testNurtraUserFromMapWithoutFCMToken()`**
   - Tests backward compatibility with old documents

5. **`testFCMTokenUpdateInMap()`**
   - Tests token update scenarios

## Usage Examples

### Manual Token Retrieval
```kotlin
private val fcmTokenManager = FCMTokenManager()

suspend fun getCurrentToken(): String? {
    return fcmTokenManager.getToken()
}
```

### Subscribe to Topic
```kotlin
fcmTokenManager.subscribeToTopic("breaking_news").onSuccess {
    Log.d("FCM", "Subscribed to breaking_news")
}.onFailure { error ->
    Log.e("FCM", "Failed to subscribe: ${error.message}")
}
```

### Unsubscribe from Topic
```kotlin
fcmTokenManager.unsubscribeFromTopic("breaking_news").onSuccess {
    Log.d("FCM", "Unsubscribed from breaking_news")
}
```

## Error Handling

All FCM operations include error handling:
- Token retrieval failures log errors but don't crash the app
- Token save failures to Firestore are logged with user ID for debugging
- Graceful handling when no user is authenticated

## Security Considerations

1. **Token Encryption**: FCM tokens are transmitted over HTTPS to Firestore
2. **Firestore Security Rules**: Ensure only the user can read/write their own token
3. **Token Rotation**: FCM automatically rotates tokens; always use the latest token from `onNewToken()`

### Recommended Firestore Security Rules
```javascript
match /users/{userId} {
  allow read: if request.auth.uid == userId;
  allow write: if request.auth.uid == userId;
}
```

## Future Enhancements

1. **Topic-Based Messaging**: Implement topic-based groups (e.g., all users get daily challenges)
2. **Custom Notification Handler**: Extend `onMessageReceived()` to show custom notifications
3. **Token Lifecycle Tracking**: Log token creation/refresh events
4. **Analytics Integration**: Track FCM engagement metrics
5. **Deep Linking**: Add intent filters for navigation from notifications

## Troubleshooting

### Token Not Saving
- Ensure user is authenticated before token is retrieved
- Check Firestore security rules allow write access
- Verify `MyFirebaseMessagingService` is registered in manifest

### Token Not Generated
- Ensure FCM dependency is added
- Check that Google Services plugin is applied
- Verify `google-services.json` is in `app/` directory

### Missing Notifications
- Verify FCM token exists in Firestore for the user
- Check app notification permissions (Android 13+)
- Ensure correct topic name if using topic-based messaging

## Related Files

- `app/src/main/java/com/example/nurtra_android/data/FCMTokenManager.kt`
- `app/src/main/java/com/example/nurtra_android/data/MyFirebaseMessagingService.kt`
- `app/src/main/java/com/example/nurtra_android/data/FirestoreManager.kt`
- `app/src/main/java/com/example/nurtra_android/data/DataModels.kt`
- `app/src/main/java/com/example/nurtra_android/auth/AuthViewModel.kt`
- `app/src/test/java/com/example/nurtra_android/data/FCMTokenTest.kt`
- `app/src/main/AndroidManifest.xml`
