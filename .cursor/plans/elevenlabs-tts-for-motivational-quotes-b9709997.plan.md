<!-- b9709997-f74b-4f62-9c00-a59b969ff082 e30f1613-3bbe-42ef-ab61-bde6d2da7802 -->
# ElevenLabs TTS Integration for Motivational Quotes

## Overview

Add text-to-speech functionality using ElevenLabs API to vocalize motivational quotes in CameraScreen. Audio will be generated when quotes are created and stored in Firestore, then played synchronously with quote display.

## Implementation Steps

### 1. Add ElevenLabs API Key Configuration

- Add `ELEVENLABS_API_KEY` to `local.properties` (similar to OpenAI key)
- Add BuildConfig field in `app/build.gradle.kts` to expose API key
- Update build configuration to include the key

### 2. Create ElevenLabs Service

- Create `app/src/main/java/com/example/nurtra_android/services/ElevenLabsService.kt`
- Implement `generateSpeech(text: String, voiceId: String?): Result<ByteArray>` method
- Use ElevenLabs text-to-speech API endpoint: `https://api.elevenlabs.io/v1/text-to-speech/{voice_id}`
- Handle API authentication and error responses
- Return audio as ByteArray for upload to Firebase Storage

### 3. Update Firestore Data Model

- Modify `FirestoreManager.saveMotivationalQuotes()` to accept audio URLs
- Store audio URLs alongside quote text in user document:
- Change structure from `motivationalQuotes: Map<String, String>` to support both text and audio URL
- Add `motivationalQuoteAudioUrls: Map<String, String>` field (quote ID -> audio URL)
- Add `saveMotivationalQuoteAudioUrls()` method to FirestoreManager
- Update `getUserMotivationalQuotes()` to return both quotes and audio URLs

### 4. Integrate Audio Generation in Quote Creation Flow

- Update `OnboardingSurveyScreen.kt` after quotes are generated:
- After saving quotes, call ElevenLabsService for each quote
- Upload audio files to Firebase Storage (or save ElevenLabs URLs directly)
- Save audio URLs to Firestore using new FirestoreManager method
- Handle errors gracefully (fallback to text-only if audio generation fails)

### 5. Add Firebase Storage Integration

- Add Firebase Storage dependency to `app/build.gradle.kts`
- Create utility to upload audio files to Firebase Storage
- Generate storage paths: `users/{userId}/quotes/{quoteId}.mp3`
- Return download URLs for storage in Firestore

### 6. Update CameraScreen for Audio Playback

- Modify `CameraScreen.kt` to:
- Load audio URLs when fetching quotes
- Use Android `MediaPlayer` or `ExoPlayer` for audio playback
- Replace 3-second rotation with audio-based timing:
- Play audio for current quote
- Wait for audio completion + 1 second delay
- Then rotate to next quote
- Handle audio loading errors gracefully (fallback to text-only display)
- Manage MediaPlayer lifecycle (release on dispose)

### 7. Update DataModels

- Update `NurtraUser` data class to include `motivationalQuoteAudioUrls: Map<String, String>`
- Ensure proper serialization/deserialization in `DataModels.kt`

## Files to Modify

1. `app/build.gradle.kts` - Add BuildConfig field, Firebase Storage dependency
2. `app/src/main/java/com/example/nurtra_android/services/ElevenLabsService.kt` - New service file
3. `app/src/main/java/com/example/nurtra_android/data/FirestoreManager.kt` - Add audio URL methods
4. `app/src/main/java/com/example/nurtra_android/data/DataModels.kt` - Update user model
5. `app/src/main/java/com/example/nurtra_android/OnboardingSurveyScreen.kt` - Add audio generation after quote creation
6. `app/src/main/java/com/example/nurtra_android/CameraScreen.kt` - Implement audio playback and timing

## Technical Details

- ElevenLabs API: Use default voice (or configurable voice ID)
- Audio format: MP3 (default from ElevenLabs)
- Storage: Firebase Storage with public download URLs
- Playback: Android MediaPlayer (lightweight for simple use case)
- Error handling: Graceful fallback to text-only if audio unavailable

### To-dos

- [ ] Add ElevenLabs API key to local.properties and BuildConfig in build.gradle.kts, add Firebase Storage dependency
- [ ] Create ElevenLabsService.kt with generateSpeech method to call ElevenLabs TTS API
- [ ] Update NurtraUser data model to include motivationalQuoteAudioUrls field
- [ ] Add methods to FirestoreManager for saving/retrieving audio URLs alongside quotes
- [ ] Create utility to upload audio files to Firebase Storage and get download URLs
- [ ] Update OnboardingSurveyScreen to generate and save audio after quotes are created
- [ ] Modify CameraScreen to load audio URLs, play audio with MediaPlayer, and update rotation timing to wait for audio completion + 1 second