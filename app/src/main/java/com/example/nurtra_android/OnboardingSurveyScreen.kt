package com.example.nurtra_android

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nurtra_android.auth.AuthViewModel
import com.example.nurtra_android.data.FirestoreManager
import com.example.nurtra_android.data.OnboardingSurveyResponses
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.util.Log

// Survey question data
data class SurveyStep(
    val stepNumber: Int,
    val title: String,
    val question: String,
    val options: List<String>,
    val isMultipleChoice: Boolean,
    val isInformational: Boolean = false,
    val informationalContent: String? = null
)

// Survey steps configuration
private val surveySteps = listOf(
    // Step 0: Your Journey
    SurveyStep(
        stepNumber = 0,
        title = "Your Journey",
        question = "How long have you struggled with binge eating?",
        options = listOf(
            "Less than 6 months",
            "6 months to 1 year",
            "1-2 years",
            "2-5 years",
            "5-10 years",
            "More than 10 years",
            "Other"
        ),
        isMultipleChoice = false
    ),
    // Step 1: Understanding Patterns
    SurveyStep(
        stepNumber = 1,
        title = "Understanding Patterns",
        question = "How often do binges typically happen?",
        options = listOf(
            "Daily",
            "Several times a week",
            "Weekly",
            "Bi-weekly",
            "Monthly",
            "Occasionally",
            "Other"
        ),
        isMultipleChoice = false
    ),
    // Step 2: Your Motivation
    SurveyStep(
        stepNumber = 2,
        title = "Your Motivation",
        question = "Why is it important for you to overcome binge eating?",
        options = listOf(
            "Physical health",
            "Mental well-being",
            "Self-confidence",
            "Relationships",
            "Career goals",
            "Financial stability",
            "Other"
        ),
        isMultipleChoice = true
    ),
    // Step 3: Your Vision
    SurveyStep(
        stepNumber = 3,
        title = "Your Vision",
        question = "What would your life look like without binge eating?",
        options = listOf(
            "More energy",
            "Better self-esteem",
            "Healthier relationships",
            "Career advancement",
            "Financial freedom",
            "Inner peace",
            "Other"
        ),
        isMultipleChoice = true
    ),
    // Step 4: Your Thoughts
    SurveyStep(
        stepNumber = 4,
        title = "Your Thoughts",
        question = "What thoughts usually come up before or during a binge?",
        options = listOf(
            "I deserve this",
            "I'll start fresh tomorrow",
            "I can't control myself",
            "This is the last time",
            "I'm already failing",
            "Food will make me feel better",
            "Other"
        ),
        isMultipleChoice = true
    ),
    // Step 5: Your Triggers
    SurveyStep(
        stepNumber = 5,
        title = "Your Triggers",
        question = "Are there common situations or feelings that trigger it?",
        options = listOf(
            "Stress",
            "Boredom",
            "Loneliness",
            "Anger",
            "Sadness",
            "Celebration",
            "Other"
        ),
        isMultipleChoice = true
    ),
    // Step 6: Your Priorities
    SurveyStep(
        stepNumber = 6,
        title = "Your Priorities",
        question = "What matters most to you in life?",
        options = listOf(
            "Family",
            "Health",
            "Career",
            "Personal growth",
            "Relationships",
            "Helping others",
            "Other"
        ),
        isMultipleChoice = true
    ),
    // Step 7: Your Values
    SurveyStep(
        stepNumber = 7,
        title = "Your Values",
        question = "What personal values would you like your recovery to align with?",
        options = listOf(
            "Self-compassion",
            "Authenticity",
            "Resilience",
            "Growth",
            "Balance",
            "Integrity",
            "Other"
        ),
        isMultipleChoice = true
    ),
    // Step 8: App Blocking Setup (Informational)
    SurveyStep(
        stepNumber = 8,
        title = "App Blocking Setup",
        question = "",
        options = emptyList(),
        isMultipleChoice = false,
        isInformational = true,
        informationalContent = """
            App Blocking Feature
            
            Apps will be blocked ONLY when using the 'Craving!' feature.
            
            During normal usage, when you're not experiencing a craving, all apps will work as usual.
            
            This feature helps you stay focused and avoid distractions when you're working through a craving.
        """.trimIndent()
    ),
    // Step 9: Select Apps to Block (Placeholder for future implementation)
    SurveyStep(
        stepNumber = 9,
        title = "Select Apps to Block",
        question = "",
        options = emptyList(),
        isMultipleChoice = false,
        isInformational = true,
        informationalContent = """
            App Selection
            
            You can select which apps to block when using the 'Craving!' feature.
            
            This feature will be available in a future update.
        """.trimIndent()
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingSurveyScreen(
    onComplete: () -> Unit,
    authViewModel: AuthViewModel = viewModel()
) {
    var currentStep by remember { mutableStateOf(0) }
    var responses by remember { mutableStateOf<Map<Int, Set<String>>>(emptyMap()) }
    var otherTexts by remember { mutableStateOf<Map<Int, String>>(emptyMap()) }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val currentStepData = surveySteps[currentStep]
    val progress = (currentStep + 1) / surveySteps.size.toFloat()

    // Current step responses
    val currentResponses = responses[currentStep] ?: emptySet()
    val currentOtherText = otherTexts[currentStep] ?: ""

    // Check if can proceed to next step
    val canProceed = when {
        currentStepData.isInformational -> true
        currentStepData.isMultipleChoice -> currentResponses.isNotEmpty() && 
            !(currentResponses.contains("Other") && currentOtherText.isBlank())
        else -> currentResponses.isNotEmpty() && 
            !(currentResponses.contains("Other") && currentOtherText.isBlank())
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top App Bar (only show back button if not on first step)
            TopAppBar(
                title = { Text(currentStepData.title) },
                navigationIcon = {
                    if (currentStep > 0) {
                        IconButton(onClick = {
                            if (currentStep > 0) {
                                currentStep--
                            }
                        }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                }
            )

            // Progress bar
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary
            )

            // Step indicator
            Text(
                text = "Step ${currentStep + 1} of ${surveySteps.size}",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                textAlign = TextAlign.Center,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (currentStepData.isInformational) {
                    // Informational step
                    Text(
                        text = currentStepData.informationalContent ?: "",
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    // Question step
                    Text(
                        text = currentStepData.question,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    // Options
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        currentStepData.options.forEach { option ->
                            val isSelected = currentResponses.contains(option)

                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    val newResponses = if (currentStepData.isMultipleChoice) {
                                        // Multiple choice: toggle selection
                                        if (isSelected) {
                                            currentResponses - option
                                        } else {
                                            currentResponses + option
                                        }
                                    } else {
                                        // Single choice: replace selection
                                        if (isSelected) {
                                            // Deselect if clicking the same option
                                            emptySet()
                                        } else {
                                            // Select this option (replaces any previous selection)
                                            setOf(option)
                                        }
                                    }

                                    responses = responses + (currentStep to newResponses)

                                    // Handle "Other" text field
                                    if (option == "Other") {
                                        if (!isSelected && newResponses.contains("Other")) {
                                            // "Other" was just selected - keep text if exists
                                        } else if (!newResponses.contains("Other")) {
                                            // "Other" was deselected - clear text
                                            otherTexts = otherTexts - currentStep
                                        }
                                    } else if (!currentStepData.isMultipleChoice) {
                                        // For single choice, if selecting a non-"Other" option, clear "Other" text
                                        if (currentResponses.contains("Other")) {
                                            otherTexts = otherTexts - currentStep
                                        }
                                    }
                                },
                                label = { Text(option) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = Color.White
                                ),
                                trailingIcon = if (isSelected) {
                                    {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = Color.White
                                        )
                                    }
                                } else null
                            )
                        }
                    }

                    // Other input field (if "Other" is selected)
                    if (currentResponses.contains("Other")) {
                        OutlinedTextField(
                            value = currentOtherText,
                            onValueChange = {
                                otherTexts = otherTexts + (currentStep to it)
                            },
                            label = { Text("Please specify") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = false,
                            minLines = 2
                        )
                    }
                }

                // Error message
                errorMessage?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 14.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Navigation buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (currentStep > 0) {
                        OutlinedButton(
                            onClick = { currentStep-- }
                        ) {
                            Text("Previous")
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    Button(
                        onClick = {
                            if (currentStep < surveySteps.size - 1) {
                                // Move to next step
                                currentStep++
                                errorMessage = null
                            } else {
                                // Last step (Step 9) - submit survey and complete onboarding
                                submitSurvey(
                                    responses = responses,
                                    otherTexts = otherTexts,
                                    authViewModel = authViewModel,
                                    onSuccess = {
                                        onComplete()
                                    },
                                    onError = { error ->
                                        errorMessage = error
                                    },
                                    isLoading = { isSubmitting = it }
                                )
                            }
                        },
                        enabled = canProceed && !isSubmitting
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White
                            )
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = when {
                                        currentStep == surveySteps.size - 1 -> "Complete" // Last step
                                        else -> "Next"
                                    },
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                                if (currentStep < surveySteps.size - 1) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowForward,
                                        contentDescription = "Next"
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun submitSurvey(
    responses: Map<Int, Set<String>>,
    otherTexts: Map<Int, String>,
    authViewModel: AuthViewModel,
    onSuccess: () -> Unit,
    onError: (String) -> Unit,
    isLoading: (Boolean) -> Unit
) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    if (currentUser == null) {
        onError("User not authenticated")
        return
    }

    isLoading(true)

    // Map responses to OnboardingSurveyResponses
    val surveyResponses = OnboardingSurveyResponses(
        struggleDuration = formatResponses(responses[0], otherTexts[0]),
        bingeFrequency = formatResponses(responses[1], otherTexts[1]),
        importanceReason = formatResponses(responses[2], otherTexts[2]),
        lifeWithoutBinge = formatResponses(responses[3], otherTexts[3]),
        bingeThoughts = formatResponses(responses[4], otherTexts[4]),
        bingeTriggers = formatResponses(responses[5], otherTexts[5]),
        whatMattersMost = formatResponses(responses[6], otherTexts[6]),
        recoveryValues = formatResponses(responses[7], otherTexts[7]),
        copingActivities = emptyList() // Not in survey, keep empty for now
    )

    CoroutineScope(Dispatchers.IO).launch {
        try {
            val firestoreManager = FirestoreManager()
            val result = firestoreManager.updateOnboardingData(
                userId = currentUser.uid,
                responses = surveyResponses
            )

            result.onSuccess {
                Log.d("OnboardingSurvey", "Survey submitted successfully")
                CoroutineScope(Dispatchers.Main).launch {
                    isLoading(false)
                    // Refresh user data
                    authViewModel.refreshNurtraUser()
                    onSuccess()
                }
            }.onFailure { error ->
                Log.e("OnboardingSurvey", "Failed to submit survey: ${error.message}", error)
                CoroutineScope(Dispatchers.Main).launch {
                    isLoading(false)
                    onError("Failed to save survey: ${error.message}")
                }
            }
        } catch (e: Exception) {
            Log.e("OnboardingSurvey", "Error submitting survey", e)
            CoroutineScope(Dispatchers.Main).launch {
                isLoading(false)
                onError("An error occurred: ${e.message}")
            }
        }
    }
}

/**
 * Formats responses by replacing "Other" with the actual text if provided
 */
private fun formatResponses(responses: Set<String>?, otherText: String?): List<String> {
    if (responses == null || responses.isEmpty()) return emptyList()

    return responses.map { response ->
        if (response == "Other" && !otherText.isNullOrBlank()) {
            otherText
        } else if (response == "Other") {
            "Other"
        } else {
            response
        }
    }
}

