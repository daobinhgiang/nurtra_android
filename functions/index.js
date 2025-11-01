const functions = require('firebase-functions');
const admin = require('firebase-admin');
const OpenAI = require('openai');

// Initialize Firebase Admin
admin.initializeApp();

// Lazy initialization of OpenAI client (only when needed)
let openai = null;
function getOpenAIClient() {
  if (!openai) {
    const config = functions.config();
    const apiKey = config?.openai?.key || process.env.OPENAI_API_KEY;
    
    if (!apiKey) {
      throw new Error('OpenAI API key not configured. Please set functions.config().openai.key or OPENAI_API_KEY environment variable.');
    }
    
    openai = new OpenAI({
      apiKey: apiKey
    });
  }
  return openai;
}

/**
 * Callable Cloud Function to send a personalized motivational push notification
 * This function:
 * 1. Fetches user's main document data from Firestore (onboarding, timer, overcome count)
 * 2. Fetches additional context: recent binge-free periods, current timer status, stats
 * 3. Generates a highly personalized message using OpenAI with rich user context
 * 4. Sends a push notification to the user's device
 * 
 * Personalization includes:
 * - Onboarding survey responses (values, triggers, reasons)
 * - Current timer status and streak
 * - Recent binge-free periods and achievements
 * - Overcome count and progress history
 * - Longest streak and total binge-free time
 */
exports.sendMotivationalNotification = functions.https.onCall(async (data, context) => {
  // Check authentication
  if (!context.auth) {
    throw new functions.https.HttpsError(
      'unauthenticated',
      'User must be authenticated to call this function.'
    );
  }

  const userId = context.auth.uid;
  console.log(`📱 Sending motivational notification to user: ${userId}`);

  try {
    // 1. Fetch comprehensive user data from Firestore
    const userDoc = await admin.firestore().collection('users').doc(userId).get();
    
    if (!userDoc.exists) {
      throw new functions.https.HttpsError(
        'not-found',
        'User data not found in Firestore.'
      );
    }

    const userData = userDoc.data();
    const fcmToken = userData.fcmToken;

    if (!fcmToken) {
      throw new functions.https.HttpsError(
        'failed-precondition',
        'No FCM token found for this user. Please ensure notifications are enabled.'
      );
    }

    // 2. Fetch additional user data for personalization
    const userContext = await fetchUserContext(userId, userData);
    console.log(`📊 User context fetched:`, JSON.stringify(userContext, null, 2));

    // 3. Generate personalized message using OpenAI with rich context
    const personalizedMessage = await generateMotivationalMessage(userData, userContext);
    console.log(`✅ Generated message: ${personalizedMessage}`);

    // 4. Send push notification
    const message = {
      notification: {
        title: '💪 Stay Strong!',
        body: personalizedMessage
      },
      data: {
        type: 'motivational',
        timestamp: Date.now().toString()
      },
      token: fcmToken,
      apns: {
        payload: {
          aps: {
            sound: 'default',
            badge: 1,
            contentAvailable: true
          }
        }
      }
    };

    const response = await admin.messaging().send(message);
    console.log(`✅ Successfully sent notification: ${response}`);

    return {
      success: true,
      message: personalizedMessage,
      messageId: response
    };

  } catch (error) {
    console.error('❌ Error sending motivational notification:', error);
    
    // Handle specific errors
    if (error.code === 'messaging/invalid-registration-token' || 
        error.code === 'messaging/registration-token-not-registered') {
      throw new functions.https.HttpsError(
        'failed-precondition',
        'Invalid or expired FCM token. Please restart the app to refresh your notification token.'
      );
    }

    throw new functions.https.HttpsError(
      'internal',
      `Failed to send notification: ${error.message}`
    );
  }
});

/**
 * Fetch comprehensive user context for personalization
 * @param {string} userId - User ID
 * @param {Object} userData - Basic user data from main document
 * @returns {Promise<Object>} - Rich user context object
 */
async function fetchUserContext(userId, userData) {
  const context = {
    onboarding: userData.onboardingResponses || {},
    timer: null,
    recentPeriods: [],
    stats: {
      overcomeCount: userData.overcomeCount || 0,
      longestStreak: 0,
      currentStreak: 0,
      totalBingeFreeTime: 0
    }
  };

  // Fetch timer data
  if (userData.timerStartTime) {
    const startTime = userData.timerStartTime.toDate();
    const isRunning = userData.timerIsRunning || false;
    const now = new Date();
    const elapsedMs = now - startTime;
    const elapsedHours = Math.floor(elapsedMs / (1000 * 60 * 60));
    const elapsedMinutes = Math.floor((elapsedMs % (1000 * 60 * 60)) / (1000 * 60));

    context.timer = {
      isRunning: isRunning,
      startTime: startTime,
      elapsedHours: elapsedHours,
      elapsedMinutes: elapsedMinutes,
      formattedTime: `${elapsedHours}h ${elapsedMinutes}m`
    };
  }

  // Fetch recent binge-free periods
  try {
    const periodsSnapshot = await admin.firestore()
      .collection('users')
      .doc(userId)
      .collection('bingeFreePeriods')
      .orderBy('createdAt', 'desc')
      .limit(5)
      .get();

    context.recentPeriods = periodsSnapshot.docs.map(doc => {
      const data = doc.data();
      const startTime = data.startTime.toDate();
      const endTime = data.endTime.toDate();
      const duration = data.duration; // in seconds
      const hours = Math.floor(duration / 3600);
      const minutes = Math.floor((duration % 3600) / 60);

      return {
        duration: duration,
        durationHours: hours,
        durationMinutes: minutes,
        formattedDuration: `${hours}h ${minutes}m`,
        startTime: startTime,
        endTime: endTime,
        daysAgo: Math.floor((new Date() - endTime) / (1000 * 60 * 60 * 24))
      };
    });

    // Calculate stats
    if (context.recentPeriods.length > 0) {
      // Calculate longest streak (longest single period)
      context.stats.longestStreak = Math.max(...context.recentPeriods.map(p => p.durationHours));
      
      // Calculate total binge-free time
      context.stats.totalBingeFreeTime = context.recentPeriods.reduce((sum, p) => sum + p.duration, 0);
      
      // Calculate current streak if timer is running
      if (context.timer && context.timer.isRunning) {
        context.stats.currentStreak = context.timer.elapsedHours;
      } else if (context.recentPeriods.length > 0) {
        // Most recent period duration as current streak indicator
        context.stats.currentStreak = context.recentPeriods[0].durationHours;
      }
    }
  } catch (error) {
    console.warn('⚠️ Could not fetch binge-free periods:', error.message);
  }

  return context;
}

/**
 * Generate a personalized motivational message using OpenAI
 * @param {Object} userData - User data from Firestore
 * @param {Object} userContext - Rich user context with stats and recent activity
 * @returns {Promise<string>} - Personalized motivational message
 */
async function generateMotivationalMessage(userData, userContext) {
  // Build comprehensive context from user data
  const context = buildUserContext(userData.onboardingResponses || {}, userContext);

  const systemPrompt = `You are a compassionate and supportive friend helping someone overcome binge eating. 
Your goal is to send a brief, personal, and motivating message that:
- Acknowledges their struggle with empathy
- References their specific progress, achievements, or current situation
- Reminds them of their "why" and what matters to them
- Encourages them to stay strong in the moment
- Feels like it's from a close friend who knows their journey, not a generic therapist

Keep the message to 2-3 sentences maximum. Be warm, personal, and direct. Reference specific details from their journey when possible.`;

  const userPrompt = `Generate a motivational message for someone who is struggling with binge eating right now.

${context}

Create a brief, personal message (2-3 sentences) that will help them resist the urge to binge. Use their specific information to make it feel personal and relevant.`;

  try {
    const openaiClient = getOpenAIClient();
    const completion = await openaiClient.chat.completions.create({
      model: 'gpt-3.5-turbo',
      messages: [
        { role: 'system', content: systemPrompt },
        { role: 'user', content: userPrompt }
      ],
      temperature: 0.8,
      max_tokens: 150
    });

    const message = completion.choices[0].message.content.trim();
    
    // Remove quotes if OpenAI wrapped the message in them
    return message.replace(/^["']|["']$/g, '');
  } catch (error) {
    console.error('❌ OpenAI API Error:', error);
    
    // Fallback message if OpenAI fails
    return "You've got this! Remember why you started this journey. Every moment you resist is a victory. You're stronger than this urge. 💪";
  }
}

/**
 * Build comprehensive context string from user's data
 * @param {Object} responses - User's onboarding responses
 * @param {Object} userContext - Rich user context with stats and activity
 * @returns {string} - Comprehensive context string for OpenAI
 */
function buildUserContext(responses, userContext) {
  const contextParts = [];

  // Basic onboarding information
  if (responses.importanceReason && Array.isArray(responses.importanceReason)) {
    contextParts.push(`Why recovery matters to them: ${responses.importanceReason.join(', ')}`);
  }

  if (responses.lifeWithoutBinge && Array.isArray(responses.lifeWithoutBinge)) {
    contextParts.push(`Their vision without binge eating: ${responses.lifeWithoutBinge.join(', ')}`);
  }

  if (responses.whatMattersMost && Array.isArray(responses.whatMattersMost)) {
    contextParts.push(`What matters most to them: ${responses.whatMattersMost.join(', ')}`);
  }

  if (responses.recoveryValues && Array.isArray(responses.recoveryValues)) {
    contextParts.push(`Their recovery values: ${responses.recoveryValues.join(', ')}`);
  }

  if (responses.bingeTriggers && Array.isArray(responses.bingeTriggers)) {
    contextParts.push(`Common triggers: ${responses.bingeTriggers.join(', ')}`);
  }

  if (responses.struggleDuration && Array.isArray(responses.struggleDuration)) {
    contextParts.push(`How long they've been struggling: ${responses.struggleDuration.join(', ')}`);
  }

  if (responses.bingeFrequency && Array.isArray(responses.bingeFrequency)) {
    contextParts.push(`Binge frequency: ${responses.bingeFrequency.join(', ')}`);
  }

  // Current progress and achievements
  const stats = userContext.stats || {};
  if (stats.overcomeCount > 0) {
    contextParts.push(`They have successfully overcome urges ${stats.overcomeCount} time${stats.overcomeCount > 1 ? 's' : ''} before.`);
  }

  // Timer information (if currently running)
  if (userContext.timer && userContext.timer.isRunning) {
    const time = userContext.timer.formattedTime;
    contextParts.push(`They are currently on a binge-free streak of ${time} (${userContext.timer.elapsedHours} hours). They started their timer and are actively resisting urges right now.`);
  }

  // Recent binge-free periods
  if (userContext.recentPeriods && userContext.recentPeriods.length > 0) {
    const recent = userContext.recentPeriods[0];
    if (recent.daysAgo === 0) {
      contextParts.push(`Their most recent binge-free period was today and lasted ${recent.formattedDuration}.`);
    } else if (recent.daysAgo === 1) {
      contextParts.push(`Their most recent binge-free period was yesterday and lasted ${recent.formattedDuration}.`);
    } else {
      contextParts.push(`Their most recent binge-free period was ${recent.daysAgo} days ago and lasted ${recent.formattedDuration}.`);
    }

    if (stats.longestStreak > 0) {
      contextParts.push(`Their longest binge-free streak so far has been ${stats.longestStreak} hours.`);
    }
  }

  // If no specific context, use general guidance
  if (contextParts.length === 0) {
    return "Use general motivational language focused on staying strong and resisting urges. Be encouraging and remind them that every moment of resistance is progress.";
  }

  return contextParts.join('\n');
}

/**
 * Helper function to send motivational notification to a single user
 * Used by scheduled functions to send notifications to all users
 * @param {string} userId - User ID
 * @returns {Promise<Object>} - Result of sending notification
 */
async function sendNotificationToUser(userId) {
  console.log(`📱 Sending scheduled notification to user: ${userId}`);
  
  try {
    const userDoc = await admin.firestore().collection('users').doc(userId).get();
    
    if (!userDoc.exists) {
      console.warn(`⚠️ User ${userId} not found in Firestore`);
      return { success: false, userId, reason: 'User not found' };
    }

    const userData = userDoc.data();
    const fcmToken = userData.fcmToken;

    if (!fcmToken) {
      console.warn(`⚠️ User ${userId} has no FCM token`);
      return { success: false, userId, reason: 'No FCM token' };
    }

    // Fetch user context and generate personalized message
    const userContext = await fetchUserContext(userId, userData);
    const personalizedMessage = await generateMotivationalMessage(userData, userContext);
    console.log(`✅ Generated message for ${userId}: ${personalizedMessage}`);

    // Send push notification
    const message = {
      notification: {
        title: '💪 Stay Strong!',
        body: personalizedMessage
      },
      data: {
        type: 'motivational',
        timestamp: Date.now().toString()
      },
      token: fcmToken,
      apns: {
        payload: {
          aps: {
            sound: 'default',
            badge: 1,
            contentAvailable: true
          }
        }
      }
    };

    const response = await admin.messaging().send(message);
    console.log(`✅ Successfully sent notification to ${userId}: ${response}`);

    return { success: true, userId, messageId: response };

  } catch (error) {
    console.error(`❌ Error sending notification to ${userId}:`, error.message);
    
    // Handle invalid/expired tokens gracefully
    if (error.code === 'messaging/invalid-registration-token' || 
        error.code === 'messaging/registration-token-not-registered') {
      console.warn(`⚠️ Invalid FCM token for user ${userId}`);
      return { success: false, userId, reason: 'Invalid FCM token' };
    }

    return { success: false, userId, reason: error.message };
  }
}

/**
 * Scheduled Cloud Function: Send motivational notifications at 8am EST
 * Runs daily at 8:00 AM Eastern Time (America/New_York timezone)
 */
exports.sendMorningNotifications = functions.pubsub
  .schedule('0 8 * * *')
  .timeZone('America/New_York')
  .onRun(async (context) => {
    console.log('🌅 Starting morning notification batch (8am EST)');
    const startTime = Date.now();
    
    try {
      const usersSnapshot = await admin.firestore().collection('users').get();
      const totalUsers = usersSnapshot.size;
      console.log(`📊 Found ${totalUsers} users to notify`);
      
      const results = await Promise.allSettled(
        usersSnapshot.docs.map(doc => sendNotificationToUser(doc.id))
      );
      
      const successful = results.filter(r => r.status === 'fulfilled' && r.value.success).length;
      const failed = results.filter(r => r.status === 'rejected' || !r.value.success).length;
      
      const duration = Date.now() - startTime;
      console.log(`✅ Morning notifications complete: ${successful} sent, ${failed} failed (${duration}ms)`);
      
      return { totalUsers, successful, failed, duration };
    } catch (error) {
      console.error('❌ Error in morning notification batch:', error);
      throw error;
    }
  });

/**
 * Scheduled Cloud Function: Send motivational notifications at 12pm EST
 * Runs daily at 12:00 PM (noon) Eastern Time (America/New_York timezone)
 */
exports.sendNoonNotifications = functions.pubsub
  .schedule('0 12 * * *')
  .timeZone('America/New_York')
  .onRun(async (context) => {
    console.log('☀️ Starting noon notification batch (12pm EST)');
    const startTime = Date.now();
    
    try {
      const usersSnapshot = await admin.firestore().collection('users').get();
      const totalUsers = usersSnapshot.size;
      console.log(`📊 Found ${totalUsers} users to notify`);
      
      const results = await Promise.allSettled(
        usersSnapshot.docs.map(doc => sendNotificationToUser(doc.id))
      );
      
      const successful = results.filter(r => r.status === 'fulfilled' && r.value.success).length;
      const failed = results.filter(r => r.status === 'rejected' || !r.value.success).length;
      
      const duration = Date.now() - startTime;
      console.log(`✅ Noon notifications complete: ${successful} sent, ${failed} failed (${duration}ms)`);
      
      return { totalUsers, successful, failed, duration };
    } catch (error) {
      console.error('❌ Error in noon notification batch:', error);
      throw error;
    }
  });

/**
 * Scheduled Cloud Function: Send motivational notifications at 17:00pm EST
 * Runs daily at 17:00 PM Eastern Time (America/New_York timezone)
 */
exports.sendNightNotifications = functions.pubsub
  .schedule('00 17 * * *')
  .timeZone('America/New_York')
  .onRun(async (context) => {
    console.log('🌙 Starting night notification batch (17:00pm EST)');
    const startTime = Date.now();
    
    try {
      const usersSnapshot = await admin.firestore().collection('users').get();
      const totalUsers = usersSnapshot.size;
      console.log(`📊 Found ${totalUsers} users to notify`);
      
      const results = await Promise.allSettled(
        usersSnapshot.docs.map(doc => sendNotificationToUser(doc.id))
      );
      
      const successful = results.filter(r => r.status === 'fulfilled' && r.value.success).length;
      const failed = results.filter(r => r.status === 'rejected' || !r.value.success).length;
      
      const duration = Date.now() - startTime;
      console.log(`✅ Night notifications complete: ${successful} sent, ${failed} failed (${duration}ms)`);
      
      return { totalUsers, successful, failed, duration };
    } catch (error) {
      console.error('❌ Error in night notification batch:', error);
      throw error;
    }
  });

