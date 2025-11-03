# OpenAI API Setup Guide

This guide explains how to set up the OpenAI API key for generating personalized motivational quotes.

## Features

The app now uses OpenAI's GPT-4 to generate **10 personalized motivational quotes** based on each user's onboarding survey responses. These quotes are:

- Tailored to the user's specific triggers, values, and recovery goals
- Saved to Firebase after onboarding completion
- Displayed on the Camera Screen (Craving feature) with 3-second intervals
- Automatically rotated to keep the user engaged and motivated

## Setup Instructions

### 1. Get Your OpenAI API Key

1. Visit [OpenAI Platform](https://platform.openai.com/)
2. Sign up or log in to your account
3. Navigate to [API Keys](https://platform.openai.com/api-keys)
4. Click "Create new secret key"
5. Copy the generated API key (you won't be able to see it again!)

### 2. Add API Key to local.properties

1. Open the `local.properties` file in your project root
2. Add the following line:

```properties
OPENAI_API_KEY="your-api-key-here"
```

**Important:** Replace `your-api-key-here` with your actual OpenAI API key.

### 3. Rebuild the Project

After adding the API key:

1. Clean the project: `./gradlew clean`
2. Rebuild the project: `./gradlew build`
3. Run the app

## How It Works

### During Onboarding

1. User completes the onboarding survey with their responses about:
   - How long they've struggled with binge eating
   - Frequency of binges
   - Why recovery is important to them
   - Their vision for life without binge eating
   - Common thoughts and triggers
   - What matters most to them
   - Their recovery values

2. After survey submission:
   - Survey responses are saved to Firebase
   - OpenAI API is called to generate 10 personalized quotes
   - Quotes are saved to the user's Firebase document in the `motivationalQuotes` field
   - Timestamp is recorded in `motivationalQuotesGeneratedAt`

### During Camera Screen (Craving Feature)

1. When user clicks "Craving!" button and enters the camera view:
   - Personalized quotes are fetched from Firebase
   - If quotes exist, they are displayed
   - If quotes don't exist or fail to load, default quotes are shown
   
2. Quotes rotate every **3 seconds** to give users time to read and reflect

3. Each quote is personalized to:
   - Address their specific triggers
   - Reinforce their personal values
   - Connect to their vision for recovery
   - Use compassionate, non-judgmental language

## API Costs

- Model used: `gpt-4o-mini` (cost-effective and fast)
- Quotes are generated **once per user** during onboarding
- Average cost per user: ~$0.002-0.005 (very low cost)
- Typical token usage: ~500-800 tokens per request

## Fallback Behavior

The app gracefully handles API failures:

- If OpenAI API fails, onboarding still completes successfully
- Default motivational quotes are shown in the Camera Screen
- Errors are logged but don't block the user experience
- Quotes generation is non-blocking and happens in the background

## Data Model

### User Document Structure

```kotlin
data class NurtraUser(
    // ... other fields ...
    val motivationalQuotes: Map<String, String> = emptyMap(), // Map of quote ID to quote text
    val motivationalQuotesGeneratedAt: Timestamp? = null
)
```

### Firebase Structure

```
users/
  └── {userId}/
      ├── motivationalQuotes: {
      │     "1": "First personalized quote...",
      │     "2": "Second personalized quote...",
      │     ...
      │     "10": "Tenth personalized quote..."
      │   }
      └── motivationalQuotesGeneratedAt: Timestamp
```

## Troubleshooting

### Quotes Not Generating

1. **Check API Key:**
   - Ensure `OPENAI_API_KEY` is correctly set in `local.properties`
   - Verify the key has no extra spaces or quotes (except the outer quotes)

2. **Check API Credits:**
   - Log in to [OpenAI Platform](https://platform.openai.com/)
   - Check your [Usage page](https://platform.openai.com/usage) for remaining credits

3. **Check Logs:**
   - Look for errors in Logcat with tag "OpenAIService"
   - Common issues: network connectivity, invalid API key, rate limits

### Default Quotes Showing Instead of Personalized

1. User might not have completed onboarding survey
2. Quote generation might have failed (check logs)
3. User might be offline when quotes are being fetched

## Security Notes

- **Never commit** `local.properties` to version control
- The `.gitignore` file should include `local.properties`
- API key is stored in BuildConfig and not exposed in the APK
- For production, consider using Firebase Remote Config or a backend service for API key management

## Future Enhancements

Potential improvements:

- Allow users to regenerate quotes
- Add more quote variations based on time of day or user mood
- Track which quotes users find most helpful
- Support for multiple languages
- Allow users to add their own custom quotes

