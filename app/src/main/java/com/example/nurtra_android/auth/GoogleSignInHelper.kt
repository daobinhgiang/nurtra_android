package com.example.nurtra_android.auth

import android.accounts.AccountManager
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.GoogleAuthProvider

sealed class GoogleSignInResult {
    data class Success(val credential: AuthCredential) : GoogleSignInResult()
    data class Error(val message: String) : GoogleSignInResult()
    data class NoAccountsFound(val addAccountIntent: Intent) : GoogleSignInResult()
    object Cancelled : GoogleSignInResult()
}

object GoogleSignInHelper {
    // Note: This should be the Web Client ID (OAuth 2.0) from Firebase Console
    // Get it from: Firebase Console > Authentication > Sign-in method > Google > Web client ID
    private const val WEB_CLIENT_ID = "420916737489-vnessdbq6tj94j5lkc4t7ehg4bjb4p61.apps.googleusercontent.com"
    private const val TAG = "GoogleSignInHelper"
    
    suspend fun signInWithGoogle(context: Context): GoogleSignInResult {
        return try {
            // Get Activity context - Credential Manager requires Activity for UI
            val activityContext = context as? Activity ?: run {
                Log.e(TAG, "Context is not an Activity context")
                return GoogleSignInResult.Error("Internal error: Invalid context")
            }
            
            val credentialManager = CredentialManager.create(activityContext)
            
            Log.d(TAG, "Starting Google Sign-In flow")
            
            // Create GetGoogleIdOption to request Google ID token
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)  // Allow any Google account
                .setServerClientId(WEB_CLIENT_ID)
                .setAutoSelectEnabled(false)  // Always show account picker
                .build()
            
            // Build credential request with the Google ID option
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()
            
            Log.d(TAG, "Requesting credential from Credential Manager")
            val result = credentialManager.getCredential(activityContext, request)
            val credential = result.credential
            
            Log.d(TAG, "Received credential of type: ${credential.type}")
            
            // Check if we got a Google ID Token credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                try {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val idToken = googleIdTokenCredential.idToken
                    
                    Log.d(TAG, "Successfully retrieved Google ID token")
                    GoogleSignInResult.Success(GoogleAuthProvider.getCredential(idToken, null))
                } catch (e: GoogleIdTokenParsingException) {
                    Log.e(TAG, "Failed to parse Google ID token", e)
                    GoogleSignInResult.Error("Failed to parse Google credentials")
                }
            } else {
                Log.e(TAG, "Unexpected credential type: ${credential.type}")
                GoogleSignInResult.Error("Received unexpected credential type")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.d(TAG, "User cancelled the sign-in flow")
            GoogleSignInResult.Cancelled
        } catch (e: NoCredentialException) {
            Log.e(TAG, "No credentials available - offering to add Google account", e)
            // Create intent to add Google account to device
            val addAccountIntent = AccountManager.newChooseAccountIntent(
                null, // selected account
                null, // allowable accounts
                arrayOf("com.google"), // allowable account types (Google accounts only)
                null, // description
                null, // add account auth token type
                null, // add account features
                null  // options
            )
            GoogleSignInResult.NoAccountsFound(addAccountIntent)
        } catch (e: GetCredentialException) {
            Log.e(TAG, "Get credential exception: ${e.message}", e)
            GoogleSignInResult.Error("Sign-in failed: ${e.message ?: "Unknown error"}")
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected exception during Google sign in", e)
            GoogleSignInResult.Error("Unexpected error: ${e.message ?: "Unknown error"}")
        }
    }
}

