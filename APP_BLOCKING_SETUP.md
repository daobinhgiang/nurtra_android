# App Blocking Feature - Setup Guide

## Overview

The app blocking feature helps users stay focused during cravings by blocking distracting apps. Apps are only blocked when the user clicks "I'm Craving!" and enters the CameraScreen.

## How It Works

1. **User Onboarding**: During onboarding (Step 9), users select which apps to block
2. **Data Storage**: Selected apps are saved to Firestore and synced to local storage
3. **Activation**: When user clicks "I'm Craving!", app blocking activates
4. **Monitoring**: AccessibilityService monitors app launches
5. **Blocking**: If user tries to open a blocked app, they're redirected to Nurtra with a motivational message
6. **Deactivation**: Blocking stops when user clicks "I overcame it" or "I just binged"

## Required Setup Steps

### 1. Enable Accessibility Service

Users must manually enable the accessibility service for app blocking to work:

1. Go to **Settings** → **Accessibility**
2. Find **Nurtra** or **App Blocking Service**
3. Toggle it **ON**
4. Accept the permission prompt

**Important**: This is a system requirement for monitoring app launches. The app cannot enable this automatically.

### 2. Grant Overlay Permission

For Android 6.0+ (API 23+), the app needs overlay permission to show the blocking screen:

1. Go to **Settings** → **Apps** → **Nurtra**
2. Find **Display over other apps** or **Draw over other apps**
3. Toggle it **ON**

The app will request this permission when needed.

## Technical Architecture

### Components

1. **AppBlockingManager** (`blocking/AppBlockingManager.kt`)
   - Manages blocking state (active/inactive)
   - Stores blocked apps list in SharedPreferences
   - Provides methods to check if an app should be blocked

2. **AppBlockingService** (`blocking/AppBlockingService.kt`)
   - AccessibilityService that monitors app launches
   - Detects when blocked apps are opened
   - Redirects to Nurtra with blocking overlay

3. **AppSelectionScreen** (`blocking/AppSelectionScreen.kt`)
   - UI for selecting apps to block
   - Lists all non-system apps
   - Saves selection to Firestore

4. **BlockingOverlay** (`blocking/BlockingOverlay.kt`)
   - UI shown when a blocked app is attempted
   - Displays motivational messages
   - "Return to Nurtra" button

### Data Flow

```
User selects apps (Onboarding Step 9)
    ↓
Saved to Firestore (blockedApps field)
    ↓
Loaded to AppBlockingManager on login
    ↓
User clicks "I'm Craving!"
    ↓
Blocking activated (CameraScreen appears)
    ↓
User tries to open blocked app
    ↓
AccessibilityService detects launch
    ↓
Redirects to Nurtra with blocking overlay
    ↓
User clicks "I overcame it" or "I just binged"
    ↓
Blocking deactivated (returns to timer)
```

## Database Schema

### Firestore Structure

```
users/{userId}
  ├── blockedApps: List<String>  // Package names of blocked apps
  ├── onboardingCompleted: Boolean
  └── ... (other user fields)
```

### Local Storage (SharedPreferences)

```
app_blocking_prefs
  ├── blocking_active: Boolean
  └── blocked_apps: String  // Comma-separated package names
```

## Testing the Feature

### Test Plan

1. **Install & Run**: Build and install the app on a device
2. **Complete Onboarding**: Go through all steps including app selection
3. **Enable Accessibility**: Manually enable in Android Settings
4. **Start Timer**: Click "Start Urge timer"
5. **Activate Blocking**: Click "I'm Craving!"
6. **Test Blocking**: Try to open a blocked app
7. **Verify Redirect**: Should see blocking overlay and return to Nurtra
8. **Deactivate**: Click "I overcame it" or "I just binged"
9. **Verify Apps Work**: Blocked apps should now open normally

### Known Limitations

1. **Manual Setup**: Accessibility service must be enabled manually by user
2. **Android Restrictions**: Some manufacturers may restrict AccessibilityService
3. **System Apps**: Cannot block system apps for safety reasons
4. **Performance**: May consume slightly more battery when active

## Troubleshooting

### App Blocking Not Working

1. **Check Accessibility Service**:
   - Go to Settings → Accessibility
   - Verify Nurtra service is enabled
   
2. **Check Blocked Apps List**:
   - Ensure apps were selected during onboarding
   - Check Firestore to verify apps were saved

3. **Check Blocking State**:
   - Blocking only activates on CameraScreen
   - Verify you clicked "I'm Craving!" button

4. **Check Logs**:
   ```
   adb logcat | grep -E "AppBlocking|CameraScreen|MainActivity"
   ```

### Blocking Overlay Not Showing

1. **Check Overlay Permission**:
   - Settings → Apps → Nurtra → Display over other apps

2. **Check Intent Extras**:
   - Verify AppBlockingService is passing correct extras

## Code Modifications

### Files Modified

- `AndroidManifest.xml` - Added permissions and service declaration
- `DataModels.kt` - Added `blockedApps` field to NurtraUser
- `FirestoreManager.kt` - Added `updateBlockedApps()` method
- `OnboardingSurveyScreen.kt` - Integrated app selection in Step 9
- `CameraScreen.kt` - Activate/deactivate blocking
- `MainActivity.kt` - Handle blocking overlay and load blocked apps

### Files Created

- `blocking/AppBlockingManager.kt`
- `blocking/AppBlockingService.kt`
- `blocking/AppSelectionScreen.kt`
- `blocking/BlockingOverlay.kt`
- `res/xml/app_blocking_service_config.xml`

## Future Enhancements

Potential improvements for the feature:

1. **Settings Screen**: Allow users to edit blocked apps list after onboarding
2. **Whitelist Mode**: Block all apps except whitelisted ones
3. **Time-based Blocking**: Schedule automatic blocking periods
4. **Usage Statistics**: Track how often apps are blocked
5. **Custom Messages**: Allow users to write their own motivational messages
6. **Smart Blocking**: Learn patterns and suggest apps to block

## Privacy & Permissions

### What We Access

- **Installed Apps List**: To show apps for selection
- **App Launch Events**: To detect when blocked apps are opened

### What We Don't Access

- **App Content**: We never read what's inside the apps
- **Personal Data**: No access to messages, photos, contacts, etc.
- **Usage History**: We only monitor while blocking is active

### Data Storage

- **Local**: Package names stored in SharedPreferences
- **Cloud**: Package names synced to Firestore
- **No Tracking**: We don't track app usage outside of blocking sessions


