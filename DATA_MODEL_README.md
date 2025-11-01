# Nurtra Android Data Model Implementation

## Quick Summary

The Android app now has a **unified data model** that's consistent with other platforms (iOS, etc.) sharing the same Firebase backend. All user data is stored in Firestore with strongly-typed Kotlin classes.

## What's New

### 1. **Data Models** (`app/src/main/java/com/example/nurtra_android/data/DataModels.kt`)
   - `NurtraUser` - Main user profile
   - `OnboardingSurveyResponses` - Survey answers
   - `MotivationalQuote` - App quotes
   - `TimerData` - Timer sessions
   - `BingeFreePeriod` - Binge-free tracking

### 2. **Firestore Manager** (`app/src/main/java/com/example/nurtra_android/data/FirestoreManager.kt`)
   - Centralized all Firestore operations
   - User CRUD operations
   - Onboarding data management
   - Timer and period tracking
   - Quote retrieval

### 3. **Updated AuthViewModel** (`app/src/main/java/com/example/nurtra_android/auth/AuthViewModel.kt`)
   - Automatically creates Firestore user documents on sign up/sign in
   - Loads user data from Firestore after authentication
   - Tracks both Firebase Auth and Firestore data

### 4. **Unit Tests** (`app/src/test/java/com/example/nurtra_android/data/DataModelsTest.kt`)
   - Comprehensive serialization tests
   - Default value tests
   - Missing field handling tests

### 5. **Documentation**
   - `DATA_MODEL_DOCUMENTATION.md` - Complete API reference
   - `DATA_MODEL_SETUP.md` - Implementation guide
   - `FIREBASE_MIGRATION_GUIDE.md` - Backend setup guide

## Project Structure

```
app/src/main/java/com/example/nurtra_android/
├── auth/
│   ├── AuthViewModel.kt (updated)
│   ├── GoogleSignInHelper.kt
│   ├── LoginScreen.kt
│   └── SignUpScreen.kt
├── data/
│   ├── DataModels.kt (new)
│   ├── FirestoreManager.kt (new)
│   └── ...
├── MainActivity.kt
└── ...

app/src/test/java/com/example/nurtra_android/data/
├── DataModelsTest.kt (new)
└── ...

gradle/
└── libs.versions.toml (updated with Firestore dependencies)
```

## Getting Started

### 1. Add Firebase Dependencies

The project already has the required dependencies added:
- `firebase-firestore` - Firestore database
- `firebase-firestore-ktx` - Kotlin extensions

### 2. Test Locally

```bash
# Run unit tests
./gradlew test

# Build debug APK
./gradlew assembleDebug

# Run on device/emulator
./gradlew installDebug
```

### 3. Set Up Firebase Backend

Follow **FIREBASE_MIGRATION_GUIDE.md** to:
- Configure Firestore collections
- Set security rules
- Create necessary indexes
- Seed initial data

## Usage Examples

### Creating/Updating User on Sign Up

```kotlin
// Automatically happens in AuthViewModel.signUpWithEmail()
viewModel.signUpWithEmail("user@example.com", "password123") { success, error ->
    if (success) {
        // User document created in Firestore automatically
        val user = viewModel.uiState.value.nurtraUser
        println("User: ${user?.email}")
    }
}
```

### Saving Onboarding Data

```kotlin
val firestoreManager = FirestoreManager()
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

firestoreManager.updateOnboardingData(userId, responses)
```

### Tracking Timer Sessions

```kotlin
val timerData = TimerData(
    startTime = Timestamp.now(),
    isRunning = false,
    stopTime = Timestamp.now()
)

firestoreManager.saveTimerSession(userId, timerData)
```

### Tracking Binge-Free Periods

```kotlin
val period = BingeFreePeriod(
    startTime = Timestamp(startDate),
    endTime = Timestamp(endDate),
    duration = durationInMillis,
    createdAt = Timestamp.now()
)

firestoreManager.saveBingeFreePeriod(userId, period)
```

## Data Flow

### Sign Up Flow
```
User enters email/password
         ↓
FirebaseAuth.createUserWithEmailAndPassword()
         ↓
FirestoreManager.createOrUpdateUser() (automatic)
         ↓
NurtraUser document created in Firestore
         ↓
AuthViewModel loads user data
         ↓
UI displays authenticated state
```

### Data Reading Flow
```
User authenticates
         ↓
AuthViewModel listens to auth state changes
         ↓
FirestoreManager.getUser() called
         ↓
NurtraUser document fetched
         ↓
AuthViewModel.nurtraUser updated
         ↓
UI reads from AuthViewModel.uiState
```

## Firestore Collection Map

```
firestore/
├── users/{userId}
│   ├── userId (String)
│   ├── email (String)
│   ├── displayName (String, nullable)
│   ├── photoUrl (String, nullable)
│   ├── createdAt (Timestamp)
│   ├── updatedAt (Timestamp)
│   ├── onboardingCompleted (Boolean)
│   ├── onboardingCompletedAt (Timestamp, nullable)
│   ├── onboardingResponses (Object)
│   │   ├── struggleDuration (Array<String>)
│   │   ├── bingeFrequency (Array<String>)
│   │   ├── importanceReason (Array<String>)
│   │   ├── lifeWithoutBinge (Array<String>)
│   │   ├── bingeThoughts (Array<String>)
│   │   ├── bingeTriggers (Array<String>)
│   │   ├── whatMattersMost (Array<String>)
│   │   └── recoveryValues (Array<String>)
│   ├── timerSessions/ (Subcollection)
│   │   └── {sessionId}
│   │       ├── startTime (Timestamp)
│   │       ├── isRunning (Boolean)
│   │       └── stopTime (Timestamp, nullable)
│   └── bingeFreePeriods/ (Subcollection)
│       └── {periodId}
│           ├── id (String)
│           ├── startTime (Timestamp)
│           ├── endTime (Timestamp)
│           ├── duration (Long)
│           └── createdAt (Timestamp)
└── motivationalQuotes/{quoteId}
    ├── id (String)
    ├── text (String)
    ├── order (Number)
    └── createdAt (Timestamp)
```

## Cross-Platform Compatibility

### Android ↔ iOS Data Sync

| Feature | Android | iOS | Firestore |
|---------|---------|-----|-----------|
| Authentication | Firebase Auth | Firebase Auth | User UID |
| User Data | NurtraUser | User | users/{uid} |
| Timestamps | Timestamp | Timestamp | Timestamp |
| Serialization | toMap() | Encodable | Maps |
| Error Handling | Result<T> | Result<T> | Native errors |

### Testing Cross-Platform

1. **Create user on Android**: Sign up with email
2. **Verify in Firestore**: Check user document exists
3. **Read on iOS**: Sign in with same account
4. **Edit on iOS**: Update onboarding responses
5. **Read on Android**: Sign out and back in
6. **Verify changes**: Onboarding data should match

## Migration from Old Structure

If migrating from existing Firestore structure:

1. **Backup existing data** (via Firebase Console)
2. **Run migration script** (follow FIREBASE_MIGRATION_GUIDE.md)
3. **Test on staging** (verify data intact)
4. **Deploy to production** (follow steps 4-10 in guide)
5. **Monitor for issues** (check logs and Firestore)

## Security Considerations

### Firestore Rules in Place
```javascript
// Users can only access their own data
match /users/{userId} {
  allow read, write: if request.auth.uid == userId;
}

// Everyone can read quotes
match /motivationalQuotes/{document=**} {
  allow read: if request.auth != null;
  allow write: if false;
}
```

### Best Practices
- ✅ Validate data on both client and server
- ✅ Never trust client-side authentication state alone
- ✅ Use serverTimestamp() for createdAt/updatedAt
- ✅ Index queries that filter/sort on multiple fields
- ✅ Regular security rule audits

## Monitoring & Debugging

### View Logs
```bash
# Android logcat
adb logcat | grep "FirestoreManager"

# Check specific operations
adb logcat | grep "User document"
```

### Firebase Console
1. Go to **Firestore Database**
2. Navigate to **Collections**
3. Inspect user documents
4. Check subcollections
5. View operation timestamps

### Debugging Tips
- Check `updatedAt` timestamps in Firestore
- Verify user IDs match Firebase Auth UIDs
- Look for null/missing fields in documents
- Test with both email and Google sign-in

## Performance Tips

### Optimize Queries
- Use indexes for complex queries
- Limit results with pagination
- Use subcollections for large datasets
- Cache frequently accessed data

### Reduce Reads
- Batch multiple reads together
- Use local state when possible
- Implement offline persistence
- Avoid watching entire collections

### Example: Batch Read
```kotlin
// Get user and their timer sessions together
val user = firestoreManager.getUser(userId)
val sessions = firestoreManager.getTimerSessions(userId)
```

## Troubleshooting

### User Document Not Created
- Check Firestore Rules allow write to `/users/{uid}`
- Verify network connectivity
- Check device permissions
- Look at logcat for errors

### Data Not Syncing
- Verify same Firebase project ID in Android and iOS
- Check user ID is Firebase Auth UID
- Confirm Firestore Rules allow cross-platform access
- Verify timestamp formats match

### Queries Returning Empty
- Check Firestore indexes are built
- Verify data exists in database
- Check query parameters are correct
- Look for case sensitivity issues

## Next Steps

1. **Implement Onboarding Flow**
   - Create survey screens
   - Save responses to Firestore
   - Mark onboarding as completed

2. **Implement Timer Persistence**
   - Save timer sessions on completion
   - Load historical sessions
   - Display statistics

3. **Implement Statistics Screen**
   - Query binge-free periods
   - Calculate streaks and trends
   - Display achievements

4. **Add Real-time Updates**
   - Implement listeners for data changes
   - Show live update indicators
   - Sync across devices

5. **Optimize Performance**
   - Add pagination for large datasets
   - Implement offline persistence
   - Cache frequently accessed data

## Documentation Files

| File | Purpose |
|------|---------|
| `DATA_MODEL_DOCUMENTATION.md` | Complete API reference and schema |
| `DATA_MODEL_SETUP.md` | Implementation guide with examples |
| `FIREBASE_MIGRATION_GUIDE.md` | Backend setup and migration steps |
| `DATA_MODEL_README.md` | This file - quick reference |

## Files Modified/Created

### Modified
- `app/build.gradle.kts` - Added Firestore dependencies
- `gradle/libs.versions.toml` - Added Firestore library definitions
- `app/src/main/java/com/example/nurtra_android/auth/AuthViewModel.kt` - Integrated Firestore

### Created
- `app/src/main/java/com/example/nurtra_android/data/DataModels.kt` - Data classes
- `app/src/main/java/com/example/nurtra_android/data/FirestoreManager.kt` - Firestore operations
- `app/src/test/java/com/example/nurtra_android/data/DataModelsTest.kt` - Unit tests
- `DATA_MODEL_DOCUMENTATION.md` - Complete documentation
- `DATA_MODEL_SETUP.md` - Setup guide
- `FIREBASE_MIGRATION_GUIDE.md` - Migration guide
- `DATA_MODEL_README.md` - This file

## Support

For issues or questions:
1. Check the troubleshooting section above
2. Review the error logs in Android Studio
3. Check Firebase Console for data and errors
4. Refer to documentation files for detailed information

## References

- [Firebase Android Documentation](https://firebase.google.com/docs/android/setup)
- [Firestore Documentation](https://firebase.google.com/docs/firestore)
- [Firestore Best Practices](https://firebase.google.com/docs/firestore/best-practices)
- [Firebase Security Rules](https://firebase.google.com/docs/rules)
