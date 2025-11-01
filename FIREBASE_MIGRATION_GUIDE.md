# Firebase Backend Migration & Setup Guide

## Overview

This guide helps you set up and migrate your Firebase Firestore backend to ensure consistency across Android and iOS implementations. Both platforms will share the same database, so data synchronization across devices and platforms is seamless.

## Prerequisites

- Firebase project already created (nurtra-75777)
- Firebase Console access
- Firestore database enabled (not Realtime Database)
- Firebase Authentication enabled

## Step 1: Verify Firebase Configuration

### Check Current Setup

1. Go to [Firebase Console](https://console.firebase.google.com)
2. Select project "nurtra-75777"
3. Navigate to **Firestore Database**
4. Verify:
   - Firestore is enabled
   - Database location is set to a region close to your users
   - Database is in production mode

### Enable Required Services

If not already enabled, enable:
- **Cloud Firestore**: Database service
- **Firebase Authentication**: For user sign-in
- **Cloud Storage**: For profile photos (optional)

## Step 2: Configure Firestore Collections

Create the required collections and indexes in Firestore:

### Collection 1: `users` (Top-level collection)

Each document represents one user, with document ID = user's Firebase Auth UID.

**Document Structure:**
```json
{
  "userId": "auth-uid-here",
  "email": "user@example.com",
  "displayName": "John Doe",
  "photoUrl": "https://example.com/photo.jpg",
  "createdAt": Timestamp(2024-01-15 10:30:00),
  "updatedAt": Timestamp(2024-01-15 10:30:00),
  "onboardingCompleted": true,
  "onboardingCompletedAt": Timestamp(2024-01-15 11:00:00),
  "onboardingResponses": {
    "struggleDuration": ["1-2 years"],
    "bingeFrequency": ["daily"],
    "importanceReason": ["health", "relationships"],
    "lifeWithoutBinge": ["peaceful", "productive"],
    "bingeThoughts": ["I can't control it"],
    "bingeTriggers": ["stress", "boredom"],
    "whatMattersMost": ["family", "career"],
    "recoveryValues": ["discipline", "self-care"]
  }
}
```

### Sub-collection 1: `users/{userId}/timerSessions`

Timer session tracking.

**Document Structure:**
```json
{
  "startTime": Timestamp(2024-01-15 10:30:00),
  "isRunning": false,
  "stopTime": Timestamp(2024-01-15 11:30:00)
}
```

### Sub-collection 2: `users/{userId}/bingeFreePeriods`

Binge-free period tracking.

**Document Structure:**
```json
{
  "id": "period-uuid-here",
  "startTime": Timestamp(2024-01-01 00:00:00),
  "endTime": Timestamp(2024-01-15 23:59:59),
  "duration": 1209600000,
  "createdAt": Timestamp(2024-01-15 10:30:00)
}
```

### Collection 2: `motivationalQuotes` (Top-level collection)

Shared motivational quotes for all users.

**Document Structure:**
```json
{
  "id": "quote-uuid-here",
  "text": "You are stronger than you think",
  "order": 1,
  "createdAt": Timestamp(2024-01-15 10:30:00)
}
```

## Step 3: Set Up Security Rules

Configure Firestore security rules in Firebase Console:

### Security Rules

Navigate to **Firestore Database → Rules** and replace with:

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
    
    // Everyone authenticated can read motivational quotes
    match /motivationalQuotes/{document=**} {
      allow read: if request.auth != null;
      // Only admin can write (via Cloud Functions or manually)
      allow write: if false;
    }
  }
}
```

**Click "Publish" to apply the rules.**

## Step 4: Create Firestore Indexes

If you plan to query data with multiple conditions, create indexes:

### Recommended Indexes

1. **Timer Sessions Index**
   - Collection: `users/{userId}/timerSessions`
   - Fields: `startTime (Desc)`

2. **Binge-Free Periods Index**
   - Collection: `users/{userId}/bingeFreePeriods`
   - Fields: `createdAt (Desc)`

3. **Motivational Quotes Index**
   - Collection: `motivationalQuotes`
   - Fields: `order (Asc)`

**To create indexes:**
1. Go to **Firestore Database → Indexes**
2. Click **Create Index**
3. Fill in the collection and fields
4. Wait for index to build (usually completes in seconds)

## Step 5: Migrate Existing Data (If Applicable)

If you have existing user data from a different structure:

### Option A: Manual Migration via Cloud Functions

Create a Cloud Function to transform old data to new schema:

```javascript
// functions/src/index.ts
import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

admin.initializeApp();
const db = admin.firestore();

export const migrateUserData = functions.https.onRequest(async (req, res) => {
  try {
    // Get all users from old structure
    const usersRef = db.collection("old_users");
    const snapshot = await usersRef.get();
    
    let migratedCount = 0;
    
    for (const doc of snapshot.docs) {
      const oldData = doc.data();
      const newData = {
        userId: doc.id,
        email: oldData.email,
        displayName: oldData.displayName || null,
        photoUrl: oldData.photoUrl || null,
        createdAt: oldData.createdAt || admin.firestore.Timestamp.now(),
        updatedAt: admin.firestore.Timestamp.now(),
        onboardingCompleted: oldData.onboardingCompleted || false,
        onboardingCompletedAt: oldData.onboardingCompletedAt || null,
        onboardingResponses: oldData.onboardingResponses || null
      };
      
      await db.collection("users").doc(doc.id).set(newData);
      migratedCount++;
    }
    
    res.json({ success: true, migratedCount });
  } catch (error) {
    res.status(500).json({ error: error.toString() });
  }
});
```

### Option B: Manual Migration via Firebase Console

1. Download existing data from Firestore
2. Transform JSON to match new schema
3. Re-upload to new collection structure

### Option C: Write Script Using Android App

Create a one-time migration screen/function in the Android app to transform data on first login.

## Step 6: Enable Authentication Methods

### Email/Password Authentication

1. Go to **Authentication → Sign-in method**
2. Enable **Email/Password**

### Google Sign-In

1. Go to **Authentication → Sign-in method**
2. Enable **Google**
3. Configure consent screen:
   - Go to **OAuth consent screen**
   - Select "External"
   - Fill in app information
   - Add scopes (email, profile)

## Step 7: Create Motivational Quotes (Initial Data)

Populate the `motivationalQuotes` collection with some initial quotes:

### Method 1: Firebase Console

1. Go to **Firestore Database → Collections**
2. Click **Start collection**
3. Collection ID: `motivationalQuotes`
4. Create documents with the structure shown above

### Method 2: Cloud Function

```javascript
export const seedMotivationalQuotes = functions.https.onRequest(async (req, res) => {
  const quotes = [
    { text: "You are stronger than you think", order: 1 },
    { text: "Recovery is possible", order: 2 },
    { text: "One day at a time", order: 3 },
    { text: "You deserve to be healthy", order: 4 },
    { text: "Progress, not perfection", order: 5 }
  ];
  
  try {
    for (const quote of quotes) {
      await db.collection("motivationalQuotes").add({
        id: admin.firestore.FieldValue.serverTimestamp(),
        text: quote.text,
        order: quote.order,
        createdAt: admin.firestore.FieldValue.serverTimestamp()
      });
    }
    res.json({ success: true, count: quotes.length });
  } catch (error) {
    res.status(500).json({ error: error.toString() });
  }
});
```

## Step 8: Test the Android Integration

### 1. Run Unit Tests

```bash
cd /Users/giangmichaeldao/project/nurtra_android
./gradlew test
```

### 2. Manual Testing

1. Build and run the Android app
2. Create a new account
3. Verify user document appears in Firestore
4. Sign out and sign in again
5. Verify user data is loaded correctly

## Step 9: Synchronize with iOS App

### Ensure Consistency

1. **Use same Firebase project**: Both iOS and Android must use the same Firebase project ID
2. **Match data models**: Verify iOS OnboardingSurveyResponses, MotivationalQuote, etc. match Android
3. **Use same collection names**: "users", "timerSessions", "bingeFreePeriods", "motivationalQuotes"
4. **Use Firebase Timestamps**: Both platforms must use Firebase Timestamp type

### Cross-Platform Testing

1. Sign up on Android, verify data in Firestore
2. Sign in on iOS with same account
3. Verify iOS reads Android-created data correctly
4. Create data on iOS, verify Android reads it
5. Test onboarding data synchronization

## Step 10: Monitor and Maintain

### Enable Monitoring

1. Go to **Project Settings → Monitoring**
2. Enable monitoring for:
   - Firestore read/write counts
   - Authentication events
   - Performance metrics

### Set Up Alerts

1. Go to **Project Settings → Notifications**
2. Set up alerts for:
   - Firestore quota limits
   - Authentication anomalies
   - Billing alerts

### Backups

Firestore automatically backs up data, but you can:
1. Enable automated backups in **Firestore Settings**
2. Export data periodically via Cloud Functions
3. Keep local copies of critical data

## Troubleshooting

### Users Can't Create Documents

**Issue**: New users can sign in but no document appears in Firestore

**Solution**:
1. Check security rules allow write to `/users/{uid}`
2. Verify app has internet connection
3. Check Android device has proper permissions
4. Look at logcat for Firestore errors

### Data Not Syncing Between Platforms

**Issue**: iOS and Android see different data

**Solution**:
1. Verify both use same Firebase project
2. Check userId is Firebase Auth UID, not email
3. Verify Firestore rules allow read from both platforms
4. Check timestamp formats match

### Performance Issues

**Issue**: Firestore queries are slow

**Solution**:
1. Create appropriate indexes (see Step 4)
2. Use pagination for large result sets
3. Enable local caching (offline persistence)
4. Avoid N+1 query patterns

## Data Export & Backups

### Export All User Data

```bash
gcloud firestore export gs://nurtra-75777.appspot.com/backups/$(date +%Y%m%d_%H%M%S)
```

### Import from Backup

```bash
gcloud firestore import gs://nurtra-75777.appspot.com/backups/BACKUP_NAME
```

## Cost Optimization

Firestore pricing is based on:
- **Read operations**: 0.06 per 100,000 reads
- **Write operations**: 0.18 per 100,000 writes
- **Delete operations**: 0.02 per 100,000 deletes
- **Storage**: $0.18 per GB per month

### To optimize:
- Use batch operations where possible
- Implement client-side caching
- Avoid unnecessary reads
- Archive old data to storage

## Reference

- [Firestore Documentation](https://firebase.google.com/docs/firestore)
- [Firebase Security Rules](https://firebase.google.com/docs/database/security)
- [Firestore Best Practices](https://firebase.google.com/docs/firestore/best-practices)
- [Firebase CLI Reference](https://firebase.google.com/docs/cli)
