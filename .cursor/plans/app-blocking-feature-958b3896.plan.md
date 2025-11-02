<!-- 958b3896-589e-4934-a4cf-5f7633c4fd9d 6b34b33b-d4b2-431b-873c-7a1b47cb00f8 -->
# App Blocking Feature Implementation Plan

## Overview

Implement an app blocking feature that activates when users click "I'm Craving!" and navigate to CameraScreen. The feature will detect when blocked apps are launched and prevent access with motivational content.

## Technical Approach

### Android Implementation Method

Since Android 10+ restricts direct app blocking, we'll use an **AccessibilityService** to:

- Monitor app launches in the background
- Detect when blocked apps are opened
- Show a blocking overlay and redirect to Nurtra app

### Components to Create/Modify

1. **AppBlockingService.kt** (New)

- AccessibilityService that monitors app launches
- Detects when blocked apps are opened
- Shows overlay and redirects to Nurtra

2. **AppBlockingManager.kt** (New)

- Manages blocking state (active/inactive)
- Stores list of blocked apps
- Handles start/stop blocking operations

3. **AppSelectionScreen.kt** (New)

- UI for selecting apps to block (install Step 9 of onboarding)
- Lists installed apps with checkboxes
- Saves selection to Firestore/local storage

4. **DataModels.kt** (Modify)

- Add `blockedApps: List<String>` field to `NurtraUser` model

5. **FirestoreManager.kt** (Modify)

- Add method to save/update blocked apps list
- Add method to retrieve blocked apps list

6. **CameraScreen.kt** (Modify)

- Activate app blocking when screen appears
- Deactivate when user clicks "I overcame it" or "I just binged"

7. **OnboardingSurveyScreen.kt** (Modify)

- Replace Step 9 placeholder with actual app selection UI

8. **AndroidManifest.xml** (Modify)

- Add AccessibilityService declaration
- Add required permissions (BIND_ACCESSIBILITY_SERVICE, SYSTEM_ALERT_WINDOW)
- Add QUERY_ALL_PACKAGES permission for app listing

9. **BlockingOverlay.kt** (New)

- Composable overlay UI showing motivational message
- "Return to Nurtra" button

## Implementation Steps

### Step 1: Add Required Permissions & Service

- Update AndroidManifest.xml with AccessibilityService and permissions
- Request runtime permissions in MainActivity

### Step 2: Create App Blocking Infrastructure

- Implement AppBlockingManager to handle blocking logic
- Create AppBlockingService (AccessibilityService)
- Implement app launch detection and blocking

### Step 3: Create App Selection UI

- Build AppSelectionScreen composable
- Integrate into OnboardingSurveyScreen Step 9
- Save selections to Firestore

### Step 4: Integrate with CameraScreen

- Start blocking when CameraScreen appears
- Stop blocking when user navigates away
- Handle blocking state lifecycle

### Step 5: Create Blocking Overlay

- Design overlay UI with motivational content
- Implement redirect to Nurtra functionality

### Step 6: Update Data Models

- Add blockedApps field to NurtraUser
- Update FirestoreManager with CRUD operations

## Technical Considerations

- **AccessibilityService**: Requires user to manually enable in Android Settings
- **Permission Handling**: Request SYSTEM_ALERT_WINDOW permission for overlay
- **Performance**: Efficient app detection without battery drain
- **User Experience**: Clear instructions for enabling accessibility service

## Files to Modify

- `app/src/main/java/com/example/nurtra_android/CameraScreen.kt`
- `app/src/main/java/com/example/nurtra_android/OnboardingSurveyScreen.kt`
- `app/src/main/java/com/example/nurtra_android/data/DataModels.kt`
- `app/src/main/java/com/example/nurtra_android/data/FirestoreManager.kt`
- `app/src/main/AndroidManifest.xml`

## Files to Create

- `app/src/main/java/com/example/nurtra_android/blocking/AppBlockingService.kt`
- `app/src/main/java/com/example/nurtra_android/blocking/AppBlockingManager.kt`
- `app/src/main/java/com/example/nurtra_android/blocking/AppSelectionScreen.kt`
- `app/src/main/java/com/example/nurtra_android/blocking/BlockingOverlay.kt`
- `app/src/main/res/xml/app_blocking_service_config.xml`