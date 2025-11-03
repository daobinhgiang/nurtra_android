# Binge Survey Questions

This document lists all questions and answer options from the `BingeSurveyView.swift` survey.

## Survey Overview

The binge survey consists of 3 steps, each with a question and multiple choice options (users can select multiple answers). Each step also includes an "Other" free-text field option.

---

## Step 1: How do you feel?

**Question:** How do you feel?

**Answer Options:**
- Guilty
- Ashamed
- Anxious
- Sad
- Numb
- Stressed
- Other (free text field)

**Note:** Users can select multiple feelings. The title displayed is "How do you feel?"

---

## Step 2: What led you to the binge?

**Question:** What led you to the binge?

**Answer Options:**
- Stress
- Boredom
- Loneliness
- Fatigue
- Social pressure
- Restricting earlier
- Other (free text field)

**Note:** Users can select multiple triggers. The title displayed is "What led to the binge?"

---

## Step 3: What would you have done differently next time?

**Question:** What would you have done differently next time?

**Answer Options:**
- Call a friend
- Go for a walk
- Have a balanced snack
- Journal feelings
- Practice mindfulness
- Delay 10 minutes
- Other (free text field)

**Note:** Users can select multiple strategies. The title displayed is "Next time, I could…"

---

## Implementation Notes

- All questions support multiple selections (using `Set<String>`)
- Each question has a corresponding "Other" free-text field for custom responses
- The survey uses a 3-step flow with navigation buttons (Back/Next/Submit)
- Progress indicator shows step 1 of 3, 2 of 3, 3 of 3

