@file:android.annotation.SuppressLint("NewApi")
@file:Suppress("NewApi")
package com.example.smartposturebelt

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.Manifest
import android.media.AudioManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.Color as AndroidColor
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import java.io.File
import java.io.FileOutputStream
import android.media.ToneGenerator
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Vibrator
import android.os.VibratorManager
import android.os.VibrationEffect
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Text
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.material3.Switch
import androidx.compose.material3.Scaffold
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.seconds
import java.net.HttpURLConnection
import java.text.SimpleDateFormat
import java.net.URL
import java.util.Locale
import java.util.Date
import java.security.MessageDigest
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min


// ============================================================
// DAILY CHALLENGE + STREAK HELPERS
// ============================================================

private const val STREAK_GOAL_SECONDS = 10 * 60L
private const val STREAK_PREFS = "posture_streak_prefs"
private const val STREAK_DAILY_SECONDS = "daily_good_seconds"
private const val STREAK_DATE = "streak_date"
private const val STREAK_COUNT = "streak_count"
private const val STREAK_COMPLETED = "streak_completed"

private fun todayKey(): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

private fun yesterdayKey(): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.US).format(
        Date(System.currentTimeMillis() - 24L * 60L * 60L * 1000L)
    )

private data class StreakState(
    val date: String,
    val dailyGoodSeconds: Long,
    val streak: Int,
    val completedToday: Boolean
)

private fun loadStreakState(context: Context): StreakState {
    val prefs = context.getSharedPreferences(STREAK_PREFS, Context.MODE_PRIVATE)
    val today = todayKey()
    val storedDate = prefs.getString(STREAK_DATE, today) ?: today

    return if (storedDate != today) {
        StreakState(
            date = today,
            dailyGoodSeconds = 0L,
            streak = prefs.getInt(STREAK_COUNT, 0),
            completedToday = false
        )
    } else {
        StreakState(
            date = storedDate,
            dailyGoodSeconds = prefs.getLong(STREAK_DAILY_SECONDS, 0L),
            streak = prefs.getInt(STREAK_COUNT, 0),
            completedToday = prefs.getBoolean(STREAK_COMPLETED, false)
        )
    }
}

private fun saveStreakState(
    context: Context,
    date: String,
    dailyGoodSeconds: Long,
    streak: Int,
    completedToday: Boolean
) {
    context.getSharedPreferences(STREAK_PREFS, Context.MODE_PRIVATE)
        .edit()
        .putString(STREAK_DATE, date)
        .putLong(STREAK_DAILY_SECONDS, dailyGoodSeconds)
        .putInt(STREAK_COUNT, streak)
        .putBoolean(STREAK_COMPLETED, completedToday)
        .apply()
}

private fun playStreakSound() {
    val tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 90)
    tone.startTone(ToneGenerator.TONE_PROP_ACK, 260)
    Handler(Looper.getMainLooper()).postDelayed(
        { tone.release() },
        320L
    )
}

private fun streakProgress(dailyGoodSeconds: Long): Float =
    (dailyGoodSeconds.toFloat() / STREAK_GOAL_SECONDS.toFloat())
        .coerceIn(0f, 1f)

private fun formatChallengeTime(seconds: Long): String {
    val minutes = seconds / 60L
    val remain = seconds % 60L
    return if (remain == 0L) "${minutes}m" else "${minutes}m ${remain}s"
}

@Composable
private fun DailyChallengeHomeCard(
    dailyGoodSeconds: Long,
    currentStreak: Int,
    completedToday: Boolean
) {
    val progress = streakProgress(dailyGoodSeconds)
    val color = if (completedToday) themedColor(0xFFFF8A00) else themedColor(0xFF7C3AED)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = themedColor(0xFF141B2D))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "DAILY CHALLENGE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = themedColor(0xFF94A3B8)
                    )

                    Text(
                        text = "10 minutes of good posture",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = themedWhite()
                    )
                }

                Text(
                    text = "🔥 $currentStreak",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = themedColor(0xFFFFA23A)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = if (completedToday)
                    "Challenge complete! 🎉"
                else
                    "${formatChallengeTime(dailyGoodSeconds)} of 10m",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (completedToday) themedColor(0xFF4ADE80) else themedColor(0xFFE2E8F0)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(50.dp))
                    .background(themedColor(0xFF293247))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .height(10.dp)
                        .clip(RoundedCornerShape(50.dp))
                        .background(color)
                )
            }
        }
    }
}

// ============================================================
// SMART ALERT HELPERS

// ============================================================

private const val POSTURE_ALERT_CHANNEL_ID = "posture_alerts"
private const val POSTURE_ALERT_NOTIFICATION_ID = 1001
private const val POSTURE_NOTIFICATION_PERMISSION_REQUEST = 2001

private fun createPostureAlertChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            POSTURE_ALERT_CHANNEL_ID,
            "Posture Alerts",
            NotificationManager.IMPORTANCE_HIGH
        )
        channel.description = "Alerts when bad posture continues"

        val manager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }
}

@SuppressLint("MissingPermission")
private fun triggerPostureAlert(context: Context) {
    // Phone vibration
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager =
                context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator.vibrate(
                VibrationEffect.createWaveform(longArrayOf(0, 250, 120, 350), -1)
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            @Suppress("DEPRECATION")
            val vibrator =
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            vibrator.vibrate(
                VibrationEffect.createWaveform(longArrayOf(0, 250, 120, 350), -1)
            )
        } else {
            @Suppress("DEPRECATION")
            val vibrator =
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0, 250, 120, 350), -1)
        }
    } catch (_: Exception) {
        // Notification can still be shown if vibration is unavailable.
    }

    // Android notification
    val notificationsAllowed =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED

    if (notificationsAllowed) {
        val notification =
            NotificationCompat.Builder(context, POSTURE_ALERT_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("Smart Posture Alert")
                .setContentText("Bad posture has continued for 10 seconds. Please correct your posture.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .build()

        NotificationManagerCompat.from(context)
            .notify(POSTURE_ALERT_NOTIFICATION_ID, notification)
    }
}


// ============================================================
// MAIN ACTIVITY
// ============================================================

@Composable
private fun AuthFlow(onLoginSuccess: () -> Unit) {
    var showSignup by remember { mutableStateOf(false) }
    AnimatedContent(
        targetState = showSignup,
        transitionSpec = {
            fadeIn(animationSpec = tween(420)) togetherWith
                    fadeOut(animationSpec = tween(280))
        },
        label = "loginSignupTransition"
    ) { signup ->
        if (signup) {
            CinematicSignupScreen(
                onBack = { showSignup = false },
                onSignupSuccess = {
                    showSignup = false
                    onLoginSuccess()
                }
            )
        } else {
            CinematicLoginScreen(
                onSignup = { showSignup = true },
                onLoginSuccess = onLoginSuccess
            )
        }
    }
}

@Composable
private fun CinematicBackground() {
    val infinite = rememberInfiniteTransition(label = "cinematicBg")
    val pulse by infinite.animateFloat(
        initialValue = 0.80f,
        targetValue = 1.18f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = tween(2200, easing = androidx.compose.animation.core.FastOutSlowInEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Canvas(Modifier.fillMaxSize().background(themedColor(0xFF050814))) {
        drawRect(themedColor(0xFF050814))
        drawCircle(
            color = themedColor(0xFF7C3AED).copy(alpha = 0.09f),
            radius = size.minDimension * 0.42f * pulse,
            center = Offset(size.width * 0.18f, size.height * 0.22f)
        )
        drawCircle(
            color = themedColor(0xFF06B6D4).copy(alpha = 0.08f),
            radius = size.minDimension * 0.36f,
            center = Offset(size.width * 0.90f, size.height * 0.70f)
        )
        val grid = 56f
        var x = 0f
        while (x < size.width) {
            drawLine(themedWhite().copy(alpha = 0.018f), Offset(x, 0f), Offset(x, size.height), 1f)
            x += grid
        }
        var y = 0f
        while (y < size.height) {
            drawLine(themedWhite().copy(alpha = 0.018f), Offset(0f, y), Offset(size.width, y), 1f)
            y += grid
        }
    }
}

@Composable
private fun AuthLogo() {
    val infinite = rememberInfiniteTransition(label = "logoPulse")
    val scale by infinite.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = tween(1300),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "logoScale"
    )

    Box(
        modifier = Modifier
            .size((92 * scale).dp)
            .clip(CircleShape)
            .background(themedColor(0xFF111B36)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(themedColor(0xFF7C3AED).copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "SB",
                fontSize = 25.sp,
                fontWeight = FontWeight.Black,
                color = themedWhite()
            )
        }
    }
}

@Composable
private fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = true,
        visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(18.dp),
        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
            focusedBorderColor = themedColor(0xFF8B5CF6),
            unfocusedBorderColor = themedColor(0xFF334155),
            focusedLabelColor = themedColor(0xFFC4B5FD),
            unfocusedLabelColor = themedColor(0xFF94A3B8),
            focusedTextColor = themedWhite(),
            unfocusedTextColor = themedWhite(),
            cursorColor = themedColor(0xFFA78BFA)
        )
    )
}

@Composable
private fun CinematicLoginScreen(
    onSignup: () -> Unit,
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    Box(Modifier.fillMaxSize()) {
        CinematicBackground()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 34.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AuthLogo()
            Spacer(Modifier.height(16.dp))
            Text("SMART POSTURE BELT", fontSize = 24.sp, fontWeight = FontWeight.Black, color = themedWhite())
            Text("ENTER THE NEXT LEVEL OF POSTURE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = themedColor(0xFFA78BFA))
            Spacer(Modifier.height(26.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(30.dp),
                colors = CardDefaults.cardColors(containerColor = themedColor(0xFF0D1426).copy(alpha = 0.97f))
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text("WELCOME BACK", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = themedColor(0xFF67E8F9))
                    Spacer(Modifier.height(6.dp))
                    Text("Your posture mission continues.", fontSize = 21.sp, fontWeight = FontWeight.ExtraBold, color = themedWhite())
                    Spacer(Modifier.height(17.dp))
                    AuthTextField(email, { email = it }, "Email", keyboardType = KeyboardType.Email)
                    Spacer(Modifier.height(10.dp))
                    AuthTextField(password, { password = it }, "Password", isPassword = true, keyboardType = KeyboardType.Password)

                    if (error.isNotBlank()) {
                        Spacer(Modifier.height(9.dp))
                        Text(error, fontSize = 10.sp, color = themedColor(0xFFFDA4AF))
                    }

                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = {
                            val prefs = context.getSharedPreferences(AUTH_PREFS, Context.MODE_PRIVATE)
                            val savedEmail = prefs.getString(AUTH_EMAIL, "").orEmpty()
                            val savedHash = prefs.getString(AUTH_PASSWORD_HASH, "").orEmpty()
                            when {
                                savedEmail.isBlank() -> error = "No account found. Create your profile first."
                                email.trim().equals(savedEmail, ignoreCase = true) && hashPassword(password) == savedHash -> {
                                    prefs.edit().putBoolean(AUTH_LOGGED_IN, true).apply()
                                    error = ""
                                    onLoginSuccess()
                                }
                                else -> error = "Email or password is incorrect."
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(17.dp)
                    ) {
                        Text("ENTER DASHBOARD  →", fontWeight = FontWeight.ExtraBold)
                    }

                    Spacer(Modifier.height(12.dp))
                    Text("New here?", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontSize = 10.sp, color = themedColor(0xFF94A3B8))
                    Spacer(Modifier.height(5.dp))
                    Button(
                        onClick = onSignup,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(17.dp),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = themedColor(0xFF18233B))
                    ) {
                        Text("CREATE YOUR HERO PROFILE", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = themedWhite())
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            Text("ESP32 • DUAL MPU6050 • VOICE COACH • SMART ANALYTICS", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = themedColor(0xFF64748B), textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun CinematicSignupScreen(
    onBack: () -> Unit,
    onSignupSuccess: () -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    Box(Modifier.fillMaxSize()) {
        CinematicBackground()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                Text("‹", modifier = Modifier.clickable(onClick = onBack).padding(4.dp), fontSize = 32.sp, color = themedWhite())
            }
            AuthLogo()
            Spacer(Modifier.height(12.dp))
            Text("CREATE YOUR PROFILE", fontSize = 23.sp, fontWeight = FontWeight.Black, color = themedWhite())
            Text("Your name becomes your personal voice-coach identity.", fontSize = 10.sp, color = themedColor(0xFFA78BFA), textAlign = TextAlign.Center)
            Spacer(Modifier.height(20.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(30.dp),
                colors = CardDefaults.cardColors(containerColor = themedColor(0xFF0D1426).copy(alpha = 0.97f))
            ) {
                Column(Modifier.padding(20.dp)) {
                    AuthTextField(name, { name = it }, "Your name")
                    Spacer(Modifier.height(10.dp))
                    AuthTextField(email, { email = it }, "Email", keyboardType = KeyboardType.Email)
                    Spacer(Modifier.height(10.dp))
                    AuthTextField(password, { password = it }, "Create password", isPassword = true, keyboardType = KeyboardType.Password)
                    Spacer(Modifier.height(10.dp))
                    AuthTextField(confirm, { confirm = it }, "Confirm password", isPassword = true, keyboardType = KeyboardType.Password)

                    if (error.isNotBlank()) {
                        Spacer(Modifier.height(9.dp))
                        Text(error, fontSize = 10.sp, color = themedColor(0xFFFDA4AF))
                    }

                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = {
                            when {
                                name.trim().length < 2 -> error = "Please enter your name."
                                !email.contains("@") -> error = "Please enter a valid email."
                                password.length < 6 -> error = "Password should contain at least 6 characters."
                                password != confirm -> error = "Passwords do not match."
                                else -> {
                                    context.getSharedPreferences(AUTH_PREFS, Context.MODE_PRIVATE)
                                        .edit()
                                        .putString(AUTH_NAME, name.trim())
                                        .putString(AUTH_EMAIL, email.trim())
                                        .putString(AUTH_PASSWORD_HASH, hashPassword(password))
                                        .putBoolean(AUTH_LOGGED_IN, true)
                                        .apply()
                                    error = ""
                                    onSignupSuccess()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(17.dp)
                    ) {
                        Text("ACTIVATE PROFILE  ✦", fontWeight = FontWeight.ExtraBold)
                    }

                    Spacer(Modifier.height(10.dp))
                    Text("Your local profile is used only by this app. The password is stored as a SHA-256 hash.", fontSize = 8.sp, color = themedColor(0xFF64748B), textAlign = TextAlign.Center)
                }
            }
        }
    }
}

private const val AUTH_PREFS = "smart_posture_auth"
private const val AUTH_NAME = "user_name"
private const val AUTH_EMAIL = "user_email"
private const val AUTH_PASSWORD_HASH = "password_hash"
private const val AUTH_LOGGED_IN = "logged_in"

private fun hashPassword(value: String): String {
    val bytes = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
    return bytes.joinToString("") { "%02x".format(it) }
}

private fun authenticated(context: Context): Boolean =
    context.getSharedPreferences(AUTH_PREFS, Context.MODE_PRIVATE)
        .getBoolean(AUTH_LOGGED_IN, false)

private fun storedUserName(context: Context): String =
    context.getSharedPreferences(AUTH_PREFS, Context.MODE_PRIVATE)
        .getString(AUTH_NAME, "")
        .orEmpty()
        .trim()


// ============================================================
// FINAL GLOBAL THEME ENGINE
// LIGHT MODE = EXACT STEP 22 COLORS/UI
// DARK MODE = NEW CINEMATIC COLOR SYSTEM
// ============================================================
private object ThemeState {
    var isDarkMode: Boolean = false
}

private val darkSurfaceColors = setOf(
    0xFF050814L, 0xFF07101DL, 0xFF070B16L, 0xFF0A1020L, 0xFF0A1323L,
    0xFF0A1423L, 0xFF0D1426L, 0xFF0F192BL, 0xFF101A2DL, 0xFF111B30L,
    0xFF111C35L, 0xFF121D31L, 0xFF121D33L, 0xFF141B2DL, 0xFF142036L,
    0xFF16213BL, 0xFF18233BL, 0xFF18253AL, 0xFF1A3148L, 0xFF20124AL,
    0xFF243149L, 0xFF293247L, 0xFF30445EL, 0xFF351522L, 0xFF3B1D78L,
    0xFF071A2BL, 0xFF17314DL
)

private val lightSurfaceColors = setOf(
    0xFFF4F7FBL, 0xFFF5F7FAL, 0xFFF6F8FCL, 0xFFF7F9FCL, 0xFFF8FAFCL,
    0xFFEEF2F7L, 0xFFF1F5FFL, 0xFFE2E8F0L, 0xFFE5EAF0L, 0xFFEDE9FEL,
    0xFFFFF1F2L, 0xFFE9F9EFL, 0xFFFFECECL, 0xFFFFEBC6L, 0xFFFFDADAL,
    0xFFD1FAE5L, 0xFFCBD5E1L, 0xFFFFFFFFL
)

private val darkTextColors = setOf(
    0xFF0F172AL, 0xFF17202AL, 0xFF1F2937L, 0xFF334155L, 0xFF4B5563L,
    0xFF475569L, 0xFF6B7280L, 0xFF718096L, 0xFF71839AL, 0xFF64748BL
)

private val mediumTextColors = setOf(
    0xFF94A3B8L, 0xFF9AA9BFL, 0xFF6C7A91L, 0xFF7F8EA3L, 0xFFAAB6C9L,
    0xFFB7C2D8L
)

private fun themedColor(hex: Long): androidx.compose.ui.graphics.Color {
    val original = androidx.compose.ui.graphics.Color(hex)
    if (!ThemeState.isDarkMode) return original

    return when {
        hex in darkTextColors -> androidx.compose.ui.graphics.Color(0xFFE8EEF7L)
        hex in mediumTextColors -> androidx.compose.ui.graphics.Color(0xFFAAB6C9L)
        hex in lightSurfaceColors -> when (hex) {
            0xFFFFF1F2L -> androidx.compose.ui.graphics.Color(0xFF351522L)
            0xFFE9F9EFL, 0xFFD1FAE5L -> androidx.compose.ui.graphics.Color(0xFF11291FL)
            else -> androidx.compose.ui.graphics.Color(0xFF101A2DL)
        }
        else -> original
    }
}

private fun themedWhite(): androidx.compose.ui.graphics.Color =
    if (ThemeState.isDarkMode) androidx.compose.ui.graphics.Color.White
    else androidx.compose.ui.graphics.Color.White

private fun themedBlack(): androidx.compose.ui.graphics.Color =
    if (ThemeState.isDarkMode) androidx.compose.ui.graphics.Color.White
    else androidx.compose.ui.graphics.Color.Black

private const val APP_THEME_PREFS = "smart_posture_ui"
private const val APP_THEME_DARK = "dark_mode"

private fun loadDarkMode(context: Context): Boolean =
    context.getSharedPreferences(APP_THEME_PREFS, Context.MODE_PRIVATE)
        .getBoolean(APP_THEME_DARK, false)

private fun saveDarkMode(context: Context, value: Boolean) {
    context.getSharedPreferences(APP_THEME_PREFS, Context.MODE_PRIVATE)
        .edit().putBoolean(APP_THEME_DARK, value).apply()
}


// ============================================================
// SECRET DOUBLE-CLAP PDF REPORT
// ============================================================

private fun generatePostureReportPdf(
    context: Context,
    score: Int,
    goodSeconds: Long,
    badSeconds: Long,
    alertCount: Int,
    upperAngle: Double,
    lowerAngle: Double,
    upperDifference: Double,
    lowerDifference: Double,
    recoveryCount: Int,
    averageRecoverySeconds: Long,
    currentStreak: Int
): File {
    val file = File(context.cacheDir, "smart_posture_report_${System.currentTimeMillis()}.pdf")
    val document = PdfDocument()
    val pageWidth = 595
    val pageHeight = 842
    val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
    val page = document.startPage(pageInfo)
    val canvas = page.canvas
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    fun text(value: String, x: Float, y: Float, size: Float, bold: Boolean = false) {
        paint.color = AndroidColor.rgb(30, 41, 59)
        paint.textSize = size
        paint.typeface = if (bold) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT
        canvas.drawText(value, x, y, paint)
    }

    paint.color = AndroidColor.rgb(79, 70, 229)
    canvas.drawRect(0f, 0f, pageWidth.toFloat(), 105f, paint)
    paint.color = AndroidColor.WHITE
    paint.textSize = 30f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    canvas.drawText("SMART POSTURE BELT", 36f, 48f, paint)
    paint.textSize = 15f
    paint.typeface = Typeface.DEFAULT
    canvas.drawText("Automatic Posture Intelligence Report", 38f, 78f, paint)

    text("POSTURE SUMMARY", 36f, 145f, 20f, true)
    text("Overall posture score", 42f, 185f, 15f)
    text("$score / 100", 410f, 185f, 20f, true)
    text("Good posture time", 42f, 225f, 15f)
    text(formatDuration(goodSeconds), 410f, 225f, 17f, true)
    text("Bad posture time", 42f, 265f, 15f)
    text(formatDuration(badSeconds), 410f, 265f, 17f, true)
    text("Posture alerts", 42f, 305f, 15f)
    text(alertCount.toString(), 410f, 305f, 17f, true)

    text("SPINE SENSOR READINGS", 36f, 365f, 20f, true)
    text("Upper angle", 42f, 405f, 15f)
    text(String.format("%.2f°", upperAngle), 410f, 405f, 17f, true)
    text("Lower angle", 42f, 445f, 15f)
    text(String.format("%.2f°", lowerAngle), 410f, 445f, 17f, true)
    text("Upper deviation", 42f, 485f, 15f)
    text(String.format("%.2f°", upperDifference), 410f, 485f, 17f, true)
    text("Lower deviation", 42f, 525f, 15f)
    text(String.format("%.2f°", lowerDifference), 410f, 525f, 17f, true)

    text("RECOVERY & HABITS", 36f, 585f, 20f, true)
    text("Recovery events", 42f, 625f, 15f)
    text(recoveryCount.toString(), 410f, 625f, 17f, true)
    text("Average recovery", 42f, 665f, 15f)
    text(formatDuration(averageRecoverySeconds), 410f, 665f, 17f, true)
    text("Current streak", 42f, 705f, 15f)
    text("$currentStreak day(s)", 410f, 705f, 17f, true)

    paint.color = AndroidColor.rgb(79, 70, 229)
    canvas.drawRect(36f, 748f, 559f, 750f, paint)
    text("Generated automatically by the Smart Posture Belt", 36f, 782f, 12f)
    text("Keep your spine aligned. Keep moving better.", 36f, 804f, 12f, true)

    document.finishPage(page)
    FileOutputStream(file).use { document.writeTo(it) }
    document.close()
    return file
}

private fun formatDuration(seconds: Long): String {
    val safe = seconds.coerceAtLeast(0L)
    val minutes = safe / 60L
    val secs = safe % 60L
    return String.format("%02d:%02d", minutes, secs)
}

private var globalReportAction: (() -> Unit)? = null
@Volatile private var speechCommandListening = false

@SuppressLint("MissingPermission")
private fun startDoubleClapDetector(
    context: Context,
    onDoubleClap: () -> Unit
): AudioRecord? {
    if (androidx.core.content.ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) != PackageManager.PERMISSION_GRANTED
    ) return null

    return try {
        val sampleRate = 16000
        val frameSize = 512
        val minBuffer = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBuffer <= 0) return null

        val recorder = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            maxOf(minBuffer * 2, frameSize * 4)
        )
        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            recorder.release()
            return null
        }

        recorder.startRecording()
        val mainHandler = Handler(Looper.getMainLooper())

        Thread {
            val buffer = ShortArray(frameSize)
            var noiseFloor = 180.0
            var previousRms = noiseFloor
            var firstClapAt = 0L
            var lastAcceptedClapAt = 0L
            var candidateFrames = 0
            var candidateStart = 0L
            var candidatePeak = 0
            var candidateRms = 0.0
            var candidateFlatness = 0.0
            var candidateHighBandRatio = 0.0
            var candidateZcr = 0.0

            fun resetCandidate() {
                candidateFrames = 0
                candidateStart = 0L
                candidatePeak = 0
                candidateRms = 0.0
                candidateFlatness = 0.0
                candidateHighBandRatio = 0.0
                candidateZcr = 0.0
            }

            // A small FFT-like spectral measurement.  It deliberately uses
            // only 128 samples so it stays lightweight on the phone.
            fun spectralFeatures(samples: ShortArray, count: Int): Pair<Double, Double> {
                val n = minOf(128, count)
                if (n < 64) return Pair(0.0, 0.0)
                val bins = n / 2
                var arithmetic = 0.0
                var logSum = 0.0
                var highEnergy = 0.0
                var totalEnergy = 0.0

                for (k in 2 until bins) {
                    var real = 0.0
                    var imag = 0.0
                    for (i in 0 until n) {
                        val window = 0.5 - 0.5 * kotlin.math.cos(2.0 * Math.PI * i / (n - 1))
                        val x = samples[i].toDouble() * window
                        val angle = 2.0 * Math.PI * k * i / n
                        real += x * kotlin.math.cos(angle)
                        imag -= x * kotlin.math.sin(angle)
                    }
                    val magnitude = kotlin.math.sqrt(real * real + imag * imag) + 1.0
                    arithmetic += magnitude
                    logSum += kotlin.math.ln(magnitude)
                    totalEnergy += magnitude
                    // Above ~3 kHz at 16 kHz sample rate.
                    if (k >= 24) highEnergy += magnitude
                }

                val binCount = (bins - 2).coerceAtLeast(1)
                val mean = arithmetic / binCount
                val geometric = kotlin.math.exp(logSum / binCount)
                val flatness = (geometric / maxOf(mean, 1.0)).coerceIn(0.0, 1.0)
                val highRatio = (highEnergy / maxOf(totalEnergy, 1.0)).coerceIn(0.0, 1.0)
                return Pair(flatness, highRatio)
            }

            try {
                while (recorder.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    val count = recorder.read(buffer, 0, buffer.size)
                    if (count <= 0) continue

                    // Never interpret speech as a clap while the explicit
                    // voice-command recognizer owns the microphone.
                    if (speechCommandListening) {
                        resetCandidate()
                        firstClapAt = 0L
                        previousRms = noiseFloor
                        continue
                    }

                    var peak = 0
                    var sumSquares = 0.0
                    var crossings = 0
                    var prev = buffer[0].toInt()

                    for (i in 0 until count) {
                        val raw = buffer[i].toInt()
                        val absValue = kotlin.math.abs(raw)
                        if (absValue > peak) peak = absValue
                        sumSquares += absValue.toDouble() * absValue.toDouble()
                        if ((raw >= 0 && prev < 0) || (raw < 0 && prev >= 0)) crossings++
                        prev = raw
                    }

                    val rms = kotlin.math.sqrt(sumSquares / count.toDouble())
                    val now = System.currentTimeMillis()
                    val crest = peak.toDouble() / maxOf(rms, 1.0)
                    val zcr = crossings.toDouble() / count.toDouble()
                    val attack = rms / maxOf(previousRms, 1.0)
                    val (flatness, highBandRatio) = spectralFeatures(buffer, count)

                    // Learn only ordinary background/quiet audio.
                    if (rms < noiseFloor * 1.30) {
                        noiseFloor = noiseFloor * 0.985 + rms * 0.015
                    }

                    val threshold = maxOf(520.0, noiseFloor * 3.0)
                    val loudEnough = rms > threshold && peak > maxOf(4200.0, noiseFloor * 8.0)

                    // Clap signature:
                    //  - very fast attack
                    //  - strong crest factor
                    //  - high zero-crossing rate
                    //  - broadband/flat spectrum
                    //  - significant upper-frequency energy
                    // Speech and singing usually have a much less broadband
                    // spectrum and remain active for much longer.
                    val sharpAttack = attack >= 1.75
                    val clapLikeCrest = crest >= 2.8
                    val clapLikeZcr = zcr >= 0.065
                    val clapLikeFlatness = flatness >= 0.28
                    val clapLikeHighBand = highBandRatio >= 0.34

                    val impulseLike = loudEnough &&
                            sharpAttack &&
                            clapLikeCrest &&
                            clapLikeZcr &&
                            clapLikeFlatness &&
                            clapLikeHighBand

                    if (impulseLike) {
                        if (candidateFrames == 0) {
                            candidateStart = now
                            candidatePeak = peak
                            candidateRms = rms
                            candidateFlatness = flatness
                            candidateHighBandRatio = highBandRatio
                            candidateZcr = zcr
                        } else {
                            candidatePeak = maxOf(candidatePeak, peak)
                            candidateRms = maxOf(candidateRms, rms)
                            candidateFlatness = maxOf(candidateFlatness, flatness)
                            candidateHighBandRatio = maxOf(candidateHighBandRatio, highBandRatio)
                            candidateZcr = maxOf(candidateZcr, zcr)
                        }
                        candidateFrames++
                    } else if (candidateFrames > 0) {
                        val duration = now - candidateStart
                        val clearDrop = rms < candidateRms * 0.58

                        // One clap should be a short impulse, not a sustained
                        // voiced/music segment. Allow a few frames for phone
                        // microphone/room reverberation.
                        val validImpulse =
                            duration in 8L..155L &&
                                    candidateFrames in 1..5 &&
                                    clearDrop &&
                                    candidateFlatness >= 0.28 &&
                                    candidateHighBandRatio >= 0.34 &&
                                    candidateZcr >= 0.065

                        if (validImpulse && now - lastAcceptedClapAt >= 260L) {
                            if (firstClapAt != 0L && now - firstClapAt in 220L..1800L) {
                                firstClapAt = 0L
                                lastAcceptedClapAt = now
                                mainHandler.post { onDoubleClap() }
                            } else {
                                firstClapAt = now
                                lastAcceptedClapAt = now
                            }
                        }
                        resetCandidate()
                    }

                    if (firstClapAt != 0L && now - firstClapAt > 2000L) {
                        firstClapAt = 0L
                    }

                    previousRms = previousRms * 0.80 + rms * 0.20
                }
            } catch (_: Exception) {
                // AudioRecord can stop when the app/activity is closed.
            }
        }.apply {
            isDaemon = true
            start()
        }

        recorder
    } catch (_: SecurityException) {
        null
    } catch (_: Exception) {
        null
    }
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        createPostureAlertChannel(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                POSTURE_NOTIFICATION_PERMISSION_REQUEST
            )
        }

        setContent {
            var micPermissionGranted by remember {
                mutableStateOf(
                    androidx.core.content.ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
                )
            }

            val micPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { granted ->
                micPermissionGranted = granted
            }

            LaunchedEffect(Unit) {
                if (!micPermissionGranted) {
                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            }

            var isDarkMode by remember { mutableStateOf(loadDarkMode(this@MainActivity)) }
            ThemeState.isDarkMode = isDarkMode
            val appColors = if (isDarkMode) {
                darkColorScheme(
                    primary = themedColor(0xFF8B5CF6L),
                    secondary = themedColor(0xFF22D3EEL),
                    background = themedColor(0xFF050814L),
                    surface = themedColor(0xFF0D1426L),
                    onBackground = themedWhite(),
                    onSurface = themedWhite()
                )
            } else {
                lightColorScheme(
                    primary = themedColor(0xFF2563EBL),
                    secondary = themedColor(0xFF16A34AL),
                    background = themedColor(0xFFF5F7FAL),
                    surface = themedColor(0xFFFFFFFFL),
                    onBackground = themedColor(0xFF17202AL),
                    onSurface = themedColor(0xFF17202AL)
                )
            }

            MaterialTheme(colorScheme = appColors) {
                var isAuthenticated by remember {
                    mutableStateOf(authenticated(this@MainActivity))
                }

                AnimatedContent(
                    targetState = isAuthenticated,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(500)) togetherWith
                                fadeOut(animationSpec = tween(300))
                    },
                    label = "authTransition"
                ) { loggedIn ->
                    if (loggedIn) {
                        SmartPostureDashboard(
                            micPermissionGranted = micPermissionGranted,
                            onThemeChanged = { newMode ->
                                saveDarkMode(this@MainActivity, newMode)
                                ThemeState.isDarkMode = newMode
                                isDarkMode = newMode
                            },
                            onLogout = {
                                getSharedPreferences(AUTH_PREFS, Context.MODE_PRIVATE)
                                    .edit()
                                    .putBoolean(AUTH_LOGGED_IN, false)
                                    .apply()
                                isAuthenticated = false
                            }
                        )
                    } else {
                        AuthFlow(
                            onLoginSuccess = { isAuthenticated = true }
                        )
                    }
                }
            }
        }
    }
}


// ============================================================
// MAIN DASHBOARD
// ============================================================

@Composable
fun SmartPostureDashboard(
    micPermissionGranted: Boolean = false,
    onThemeChanged: (Boolean) -> Unit = {},
    onLogout: () -> Unit = {}
) {

    val context = androidx.compose.ui.platform.LocalContext.current

    // ========================================================
    // LIVE VALUES
    // ========================================================

    var upperAngle by remember { mutableDoubleStateOf(0.0) }
    var lowerAngle by remember { mutableDoubleStateOf(0.0) }
    var upperDifference by remember { mutableDoubleStateOf(0.0) }
    var lowerDifference by remember { mutableDoubleStateOf(0.0) }
    var goodPosture by remember { mutableStateOf(false) }
    var connected by remember { mutableStateOf(false) }

    // ========================================================
    // SESSION
    // ========================================================

    var goodSeconds by remember { mutableLongStateOf(0L) }
    var badSeconds by remember { mutableLongStateOf(0L) }
    var alertCount by remember { mutableIntStateOf(0) }

    // Recovery tracking: BAD -> GOOD
    var recoveryCount by remember { mutableIntStateOf(0) }
    var bestRecoverySeconds by remember { mutableLongStateOf(Long.MAX_VALUE) }
    var totalRecoverySeconds by remember { mutableLongStateOf(0L) }
    var recoveryStartTime by remember { mutableStateOf<Long?>(null) }
    var lastRecoverySeconds by remember { mutableLongStateOf(0L) }

    // ========================================================
    // DAILY CHALLENGE + STREAK
    // ========================================================

    val streakState = remember { loadStreakState(context) }
    var streakDate by remember { mutableStateOf(streakState.date) }
    var dailyGoodSeconds by remember { mutableLongStateOf(streakState.dailyGoodSeconds) }
    var currentStreak by remember { mutableIntStateOf(streakState.streak) }
    var challengeCompletedToday by remember {
        mutableStateOf(streakState.completedToday)
    }

    // ========================================================
    // SMART BAD-POSTURE TIMER
    // ========================================================

    var badPostureStartTime by remember { mutableStateOf<Long?>(null) }
    var badPostureSeconds by remember { mutableLongStateOf(0L) }
    var postureWarningLevel by remember { mutableIntStateOf(0) }
    var alertTriggered by remember { mutableStateOf(false) }

    // ========================================================
    // GRAPH / HISTORY
    // ========================================================

    val upperHistory = remember { mutableStateListOf<Double>() }
    val lowerHistory = remember { mutableStateListOf<Double>() }
    val upperDeviationHistory = remember { mutableStateListOf<Double>() }
    val lowerDeviationHistory = remember { mutableStateListOf<Double>() }

    // ========================================================
    // NEW MODULAR NAVIGATION
    // ========================================================

    var selectedTab by remember { mutableIntStateOf(0) }
    var smartMirrorOpen by remember { mutableStateOf(false) }
    var correctionCoachOpen by remember { mutableStateOf(false) }
    var anatomyOpen by remember { mutableStateOf(false) }
    var voiceCoachOpen by remember { mutableStateOf(false) }
    var postureIqOpen by remember { mutableStateOf(false) }
    var mlModelLabOpen by remember { mutableStateOf(false) }
    var reportFile by remember { mutableStateOf<File?>(null) }
    var reportGenerating by remember { mutableStateOf(false) }
    var correctionHoldStart by remember { mutableStateOf<Long?>(null) }
    var correctionHoldSeconds by remember { mutableLongStateOf(0L) }

    // Keep the existing ESP32 connection exactly the same.
    val esp32Url = "http://192.168.4.1/data"

    // ========================================================
    // SECRET DOUBLE-CLAP LISTENER
    // ========================================================

    val latestReportAction by rememberUpdatedState {
        if (!reportGenerating && reportFile == null) {
            reportGenerating = true
            val snapshotScore = calculateLivePostureScore(
                connected,
                upperDifference,
                lowerDifference
            )
            reportFile = generatePostureReportPdf(
                context = context,
                score = snapshotScore,
                goodSeconds = goodSeconds,
                badSeconds = badSeconds,
                alertCount = alertCount,
                upperAngle = upperAngle,
                lowerAngle = lowerAngle,
                upperDifference = upperDifference,
                lowerDifference = lowerDifference,
                recoveryCount = recoveryCount,
                averageRecoverySeconds = if (recoveryCount > 0) totalRecoverySeconds / recoveryCount else 0L,
                currentStreak = currentStreak
            )
            reportGenerating = false
        }
    }

    DisposableEffect(latestReportAction) {
        globalReportAction = { latestReportAction() }
        onDispose { globalReportAction = null }
    }

    DisposableEffect(context, micPermissionGranted) {
        var recorder: AudioRecord? = null
        if (micPermissionGranted) {
            recorder = startDoubleClapDetector(context) {
                latestReportAction()
            }
        }
        onDispose {
            try { recorder?.stop() } catch (_: Exception) {}
            try { recorder?.release() } catch (_: Exception) {}
        }
    }

    // ========================================================
    // ESP32 LIVE LOOP
    // ========================================================

    LaunchedEffect(Unit) {
        while (true) {
            try {
                val data = withContext(Dispatchers.IO) {
                    readESP32(esp32Url)
                }

                if (data != null) {
                    connected = true

                    val currentDate = todayKey()
                    if (currentDate != streakDate) {
                        streakDate = currentDate
                        dailyGoodSeconds = 0L
                        challengeCompletedToday = false
                        saveStreakState(
                            context,
                            currentDate,
                            0L,
                            currentStreak,
                            false
                        )
                    }
                    upperAngle = data.upper
                    lowerAngle = data.lower
                    upperDifference = data.upperDifference
                    lowerDifference = data.lowerDifference
                    goodPosture = data.good

                    upperHistory.add(data.upper)
                    lowerHistory.add(data.lower)
                    upperDeviationHistory.add(data.upperDifference)
                    lowerDeviationHistory.add(data.lowerDifference)

                    if (upperHistory.size > 3600) upperHistory.removeAt(0)
                    if (lowerHistory.size > 3600) lowerHistory.removeAt(0)
                    if (upperDeviationHistory.size > 3600) upperDeviationHistory.removeAt(0)
                    if (lowerDeviationHistory.size > 3600) lowerDeviationHistory.removeAt(0)

                    if (data.good) {
                        goodSeconds++

                        dailyGoodSeconds =
                            (dailyGoodSeconds + 1L).coerceAtMost(STREAK_GOAL_SECONDS)

                        if (!challengeCompletedToday &&
                            dailyGoodSeconds >= STREAK_GOAL_SECONDS
                        ) {
                            val prefs = context.getSharedPreferences(
                                STREAK_PREFS,
                                Context.MODE_PRIVATE
                            )

                            val lastCompleted =
                                prefs.getString("last_completed_date", null)

                            currentStreak = when {
                                currentStreak <= 0 -> 1
                                lastCompleted == yesterdayKey() -> currentStreak + 1
                                lastCompleted == todayKey() -> currentStreak
                                else -> 1
                            }

                            challengeCompletedToday = true

                            prefs.edit()
                                .putString("last_completed_date", todayKey())
                                .apply()

                            playStreakSound()
                        }

                        saveStreakState(
                            context,
                            streakDate,
                            dailyGoodSeconds,
                            currentStreak,
                            challengeCompletedToday
                        )

                        if (recoveryStartTime != null) {
                            val elapsed = (
                                    (System.currentTimeMillis() - recoveryStartTime!!) / 1000L
                                    ).coerceAtLeast(1L)

                            recoveryCount++
                            lastRecoverySeconds = elapsed
                            totalRecoverySeconds += elapsed
                            bestRecoverySeconds = minOf(bestRecoverySeconds, elapsed)
                            recoveryStartTime = null
                        }

                        val correctionStart = correctionHoldStart
                        correctionHoldSeconds =
                            if (correctionStart != null) {
                                (System.currentTimeMillis() - correctionStart) / 1000L
                            } else {
                                0L
                            }

                        badPostureStartTime = null
                        badPostureSeconds = 0L
                        postureWarningLevel = 0
                        alertTriggered = false
                    } else {
                        badSeconds++
                        correctionHoldStart = null

                        if (recoveryStartTime == null) {
                            recoveryStartTime = System.currentTimeMillis()
                        }
                        correctionHoldSeconds = 0L

                        if (badPostureStartTime == null) {
                            badPostureStartTime = System.currentTimeMillis()
                            badPostureSeconds = 0L
                            postureWarningLevel = 1
                            alertTriggered = false
                        }

                        val start = badPostureStartTime ?: System.currentTimeMillis()
                        badPostureSeconds = (System.currentTimeMillis() - start) / 1000L

                        postureWarningLevel = when {
                            badPostureSeconds >= 10L -> 3
                            badPostureSeconds >= 5L -> 2
                            else -> 1
                        }

                        if (badPostureSeconds >= 10L && !alertTriggered) {
                            alertCount++
                            alertTriggered = true
                            triggerPostureAlert(context)
                        }
                    }
                } else {
                    connected = false
                }
            } catch (_: Exception) {
                connected = false
            }

            delay(1.seconds)
        }
    }

    val totalSeconds = goodSeconds + badSeconds

    // Current posture score is based on the LIVE sensor deviation,
    // not on how long the session has been good/bad.
    // Session quality remains available separately as goodPercentage/badPercentage.
    val postureScore = calculateLivePostureScore(
        connected = connected,
        upperDeviation = upperDifference,
        lowerDeviation = lowerDifference
    )

    val goodPercentage = if (totalSeconds > 0L) {
        goodSeconds * 100L / totalSeconds
    } else 0L

    val badPercentage = if (totalSeconds > 0L) {
        badSeconds * 100L / totalSeconds
    } else 0L

    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L

    // ========================================================
    // ANDROID SYSTEM NAVIGATION SUPPORT
    // Works with both gesture navigation (swipe back) and
    // classic 3-button navigation on Android.
    // Back closes the top-most app overlay first, then returns
    // to Home, and only then lets Android exit the Activity.
    // ========================================================

    val hasOverlayOpen = smartMirrorOpen || correctionCoachOpen || anatomyOpen ||
            voiceCoachOpen || postureIqOpen || mlModelLabOpen || reportGenerating || reportFile != null

    BackHandler(enabled = hasOverlayOpen || selectedTab != 0) {
        when {
            reportFile != null -> {
                val file = reportFile
                reportFile = null
                if (file != null) {
                    try { file.delete() } catch (_: Exception) {}
                }
            }
            reportGenerating -> reportGenerating = false
            postureIqOpen -> postureIqOpen = false
            mlModelLabOpen -> mlModelLabOpen = false
            voiceCoachOpen -> voiceCoachOpen = false
            correctionCoachOpen -> {
                correctionCoachOpen = false
                correctionHoldStart = null
                correctionHoldSeconds = 0L
            }
            anatomyOpen -> anatomyOpen = false
            smartMirrorOpen -> smartMirrorOpen = false
            selectedTab != 0 -> selectedTab = 0
        }
    }

    Scaffold(
        containerColor = themedColor(0xFFF5F7FA),
        bottomBar = {
            NavigationBar(
                containerColor = themedWhite()
            ) {
                val labels = listOf("Home", "Analytics", "History", "Posture", "More")
                val icons = listOf("⌂", "⌁", "▣", "♙", "•••")

                labels.forEachIndexed { index, label ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = {
                            Text(
                                text = icons[index],
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        label = {
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    )
                }
            }
        }
    ) { innerPadding ->

        when (selectedTab) {

            0 -> HomeScreen(
                connected = connected,
                goodPosture = goodPosture,
                upperAngle = upperAngle,
                lowerAngle = lowerAngle,
                upperDifference = upperDifference,
                lowerDifference = lowerDifference,
                postureScore = postureScore,
                goodSeconds = goodSeconds,
                badSeconds = badSeconds,
                alertCount = alertCount,
                badPostureSeconds = badPostureSeconds,
                warningLevel = postureWarningLevel,
                onOpenSmartMirror = { smartMirrorOpen = true },
                onOpenCorrectionCoach = { correctionCoachOpen = true },
                onOpenPostureIQ = { postureIqOpen = true },
                recoveryCount = recoveryCount,
                bestRecoverySeconds = bestRecoverySeconds,
                averageRecoverySeconds = if (recoveryCount > 0) totalRecoverySeconds / recoveryCount else 0L,
                lastRecoverySeconds = lastRecoverySeconds,
                dailyGoodSeconds = dailyGoodSeconds,
                currentStreak = currentStreak,
                challengeCompletedToday = challengeCompletedToday,
                modifier = Modifier.padding(innerPadding)
            )

            1 -> AnalyticsScreen(
                upperHistory = upperHistory.toList(),
                lowerHistory = lowerHistory.toList(),
                postureScore = postureScore,
                connected = connected,
                modifier = Modifier.padding(innerPadding)
            )

            2 -> HistoryScreen(
                upperHistory = upperHistory.toList(),
                lowerHistory = lowerHistory.toList(),
                upperDeviationHistory = upperDeviationHistory.toList(),
                lowerDeviationHistory = lowerDeviationHistory.toList(),
                goodPercentage = goodPercentage,
                badPercentage = badPercentage,
                alertCount = alertCount,
                minutes = minutes,
                seconds = seconds,
                recoveryCount = recoveryCount,
                bestRecoverySeconds = bestRecoverySeconds,
                averageRecoverySeconds = if (recoveryCount > 0) totalRecoverySeconds / recoveryCount else 0L,
                lastRecoverySeconds = lastRecoverySeconds,
                dailyGoodSeconds = dailyGoodSeconds,
                currentStreak = currentStreak,
                challengeCompletedToday = challengeCompletedToday,
                onOpenAnatomy = { anatomyOpen = true },
                modifier = Modifier.padding(innerPadding)
            )

            3 -> PostureScreen(
                upperAngle = upperAngle,
                lowerAngle = lowerAngle,
                goodPosture = goodPosture,
                connected = connected,
                upperDifference = upperDifference,
                lowerDifference = lowerDifference,
                modifier = Modifier.padding(innerPadding)
            )

            else -> MoreScreen(
                connected = connected,
                onOpenVoiceCoach = { voiceCoachOpen = true },
                onOpenMLModelLab = { mlModelLabOpen = true },
                onThemeChanged = onThemeChanged,
                onLogout = onLogout,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }

    if (smartMirrorOpen) {
        SmartMirrorOverlay(
            goodPosture = goodPosture,
            upperAngle = upperAngle,
            lowerAngle = lowerAngle,
            upperDifference = upperDifference,
            lowerDifference = lowerDifference,
            onClose = { smartMirrorOpen = false }
        )
    }
    if (correctionCoachOpen) {
        CorrectionCoachOverlay(
            goodPosture = goodPosture,
            upperAngle = upperAngle,
            lowerAngle = lowerAngle,
            upperDifference = upperDifference,
            lowerDifference = lowerDifference,
            holdSeconds = correctionHoldSeconds,
            onStartCorrection = {
                correctionHoldStart = System.currentTimeMillis()
                correctionHoldSeconds = 0L
            },
            onClose = {
                correctionCoachOpen = false
                correctionHoldStart = null
                correctionHoldSeconds = 0L
            }
        )
    }

    if (voiceCoachOpen) {
        SmartVoiceCoachOverlay(
            name = getLoginName(context),
            connected = connected,
            goodPosture = goodPosture,
            upperDifference = upperDifference,
            lowerDifference = lowerDifference,
            onClose = { voiceCoachOpen = false }
        )
    }

    if (reportGenerating) {
        ReportGeneratingOverlay()
    }

    reportFile?.let { file ->
        PosturePdfViewerOverlay(
            file = file,
            onClose = {
                reportFile = null
                try { file.delete() } catch (_: Exception) {}
            }
        )
    }

    if (anatomyOpen) {
        SpineAnatomyOverlay(
            upperDifference = upperDifference,
            lowerDifference = lowerDifference,
            onClose = { anatomyOpen = false }
        )
    }

    if (mlModelLabOpen) {
        MLModelLabScreen(
            connected = connected,
            upperAngle = upperAngle,
            lowerAngle = lowerAngle,
            upperDifference = upperDifference,
            lowerDifference = lowerDifference,
            goodPosture = goodPosture,
            upperHistory = upperHistory.toList(),
            lowerHistory = lowerHistory.toList(),
            onClose = { mlModelLabOpen = false }
        )
    }

    if (postureIqOpen) {
        PostureIQOverlay(
            connected = connected,
            goodPosture = goodPosture,
            upperAngle = upperAngle,
            lowerAngle = lowerAngle,
            upperDifference = upperDifference,
            lowerDifference = lowerDifference,
            postureScore = postureScore,
            alertCount = alertCount,
            recoveryCount = recoveryCount,
            averageRecoverySeconds = if (recoveryCount > 0) totalRecoverySeconds / recoveryCount else 0L,
            currentStreak = currentStreak,
            upperDeviationHistory = upperDeviationHistory.toList(),
            lowerDeviationHistory = lowerDeviationHistory.toList(),
            onGenerateReport = {
                postureIqOpen = false
                latestReportAction()
            },
            onClose = { postureIqOpen = false }
        )
    }

}


// ============================================================
// IN-APP PDF REPORT VIEWER
// ============================================================

@Composable
private fun ReportGeneratingOverlay() {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.72f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = themedColor(0xFF101A2D)),
            modifier = Modifier.padding(28.dp)
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(color = themedColor(0xFF22D3EE))
                Spacer(Modifier.height(18.dp))
                Text("Generating Posture Report", color = themedWhite(), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text("Your report is being prepared…", color = themedColor(0xFFB7C3D6), fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun PosturePdfViewerOverlay(
    file: File,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    var pages by remember(file) { mutableStateOf<List<Bitmap>>(emptyList()) }
    var previewError by remember(file) { mutableStateOf<String?>(null) }

    LaunchedEffect(file) {
        pages = emptyList()
        previewError = null
        withContext(Dispatchers.IO) {
            var descriptor: ParcelFileDescriptor? = null
            var renderer: PdfRenderer? = null
            try {
                if (!file.exists() || file.length() == 0L) {
                    throw IllegalStateException("The report file was not created correctly.")
                }

                descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                renderer = PdfRenderer(descriptor)

                val rendered = mutableListOf<Bitmap>()
                for (index in 0 until renderer!!.pageCount) {
                    val page = renderer!!.openPage(index)
                    try {
                        // Render at 1.75x for a sharp in-app preview while avoiding
                        // excessive memory usage on phones with large PDF pages.
                        val scale = 1.75f
                        val width = (page.width * scale).toInt().coerceAtLeast(1)
                        val height = (page.height * scale).toInt().coerceAtLeast(1)
                        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        bitmap.eraseColor(AndroidColor.WHITE)
                        page.render(
                            bitmap,
                            null,
                            null,
                            PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                        )
                        rendered.add(bitmap)
                    } finally {
                        page.close()
                    }
                }

                withContext(Dispatchers.Main) {
                    if (rendered.isEmpty()) {
                        previewError = "The report contains no pages."
                    } else {
                        pages = rendered
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    previewError = e.message ?: "Unable to preview the PDF report."
                }
            } finally {
                try { renderer?.close() } catch (_: Exception) {}
                try { descriptor?.close() } catch (_: Exception) {}
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(themedColor(0xFFF5F7FA))
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().background(themedColor(0xFF101A2D)).padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Posture Report", color = themedWhite(), fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                TextButton(onClick = onClose) {
                    Text("CLOSE", color = themedColor(0xFF22D3EE), fontWeight = FontWeight.Bold)
                }
            }
            if (pages.isEmpty() && previewError == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = themedColor(0xFF6366F1))
                        Spacer(Modifier.height(14.dp))
                        Text(
                            "Preparing report preview…",
                            color = themedColor(0xFF475569),
                            fontSize = 14.sp
                        )
                    }
                }
            } else if (previewError != null) {
                Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            "PDF Preview Unavailable",
                            color = themedColor(0xFFDC2626),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            previewError ?: "Unable to preview the report.",
                            color = themedColor(0xFF475569),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    items(pages) { bitmap ->
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Posture PDF report page",
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).shadow(5.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

// ============================================================
// POSTURE CORRECTION COACH
// ============================================================

@Composable
private fun CorrectionCoachCard(
    goodPosture: Boolean,
    upperDifference: Double,
    lowerDifference: Double,
    onOpen: () -> Unit
) {
    val maxDeviation = max(
        abs(upperDifference),
        abs(lowerDifference)
    )

    val accent = when {
        maxDeviation <= 10.0 -> themedColor(0xFF22C55E)
        maxDeviation <= 20.0 -> themedColor(0xFFF59E0B)
        else -> themedColor(0xFFEF4444)
    }

    val title = when {
        goodPosture -> "POSTURE COACH"
        lowerDifference > upperDifference + 2.0 -> "LOWER SPINE COACH"
        upperDifference > lowerDifference + 2.0 -> "UPPER SPINE COACH"
        else -> "FULL POSTURE COACH"
    }

    val message = when {
        goodPosture ->
            "You're aligned. Hold your natural position."
        lowerDifference > upperDifference + 2.0 ->
            "Your lower spine needs the most attention."
        upperDifference > lowerDifference + 2.0 ->
            "Your upper spine needs the most attention."
        else ->
            "Both regions need a small correction."
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = themedWhite())
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(accent.copy(alpha = 0.11f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "✦",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = accent
                )
            }

            Spacer(modifier = Modifier.size(11.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = themedColor(0xFF0F172A)
                )

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = message,
                    fontSize = 10.sp,
                    color = themedColor(0xFF64748B)
                )

                Spacer(modifier = Modifier.height(5.dp))

                Text(
                    text = if (goodPosture)
                        "✓ Ready"
                    else
                        "Tap for guided correction",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = accent
                )
            }

            Text(
                text = "›",
                fontSize = 27.sp,
                color = themedColor(0xFF94A3B8)
            )
        }
    }
}

@Composable
private fun CorrectionCoachOverlay(
    goodPosture: Boolean,
    upperAngle: Double,
    lowerAngle: Double,
    upperDifference: Double,
    lowerDifference: Double,
    holdSeconds: Long,
    onStartCorrection: () -> Unit,
    onClose: () -> Unit
) {
    val targetRegion =
        if (lowerDifference > upperDifference) "LOWER SPINE"
        else "UPPER SPINE"

    val targetDeviation =
        if (lowerDifference > upperDifference) lowerDifference
        else upperDifference

    val targetColor =
        zoneForDeviation(targetDeviation)

    val correctionText = when {
        goodPosture ->
            "Excellent. Maintain this natural position."
        targetRegion == "LOWER SPINE" ->
            "Gently straighten your lower back. Keep your shoulders relaxed."
        else ->
            "Bring your upper body toward a neutral upright position."
    }

    val progress =
        if (goodPosture)
            (holdSeconds / 5f).coerceIn(0f, 1f)
        else
            0f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(themedColor(0xEE020814))
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "POSTURE COACH",
                        fontSize = 27.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = themedWhite()
                    )

                    Text(
                        text = "Guided live correction",
                        fontSize = 11.sp,
                        color = themedColor(0xFF94A3B8)
                    )
                }

                Text(
                    text = "×",
                    modifier = Modifier
                        .clickable(onClick = onClose)
                        .padding(8.dp),
                    fontSize = 26.sp,
                    color = themedWhite()
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(themedColor(0xFF0B1829))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (goodPosture)
                                    "GOOD POSITION"
                                else
                                    "CORRECTION NEEDED",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = targetColor
                            )

                            Spacer(modifier = Modifier.height(5.dp))

                            Text(
                                text = correctionText,
                                fontSize = 11.sp,
                                color = themedColor(0xFFB7C7D8)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(58.dp)
                                .clip(CircleShape)
                                .background(targetColor.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (goodPosture) "✓" else "!",
                                fontSize = 27.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = targetColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(17.dp))

                    Text(
                        text = "LIVE CORRECTION TARGET",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = themedColor(0xFF71839A)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = targetRegion,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = themedWhite()
                    )

                    Text(
                        text = String.format(
                            Locale.US,
                            "Current deviation %.1f°",
                            targetDeviation
                        ),
                        fontSize = 10.sp,
                        color = themedColor(0xFF94A3B8)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    CoachBodyPreview(
                        upperAngle = upperAngle,
                        lowerAngle = lowerAngle,
                        targetColor = targetColor,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                            .clip(RoundedCornerShape(22.dp))
                    )

                    Spacer(modifier = Modifier.height(15.dp))

                    Text(
                        text = "HOLD GOOD POSTURE FOR 5 SECONDS",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = themedColor(0xFF71839A)
                    )

                    Spacer(modifier = Modifier.height(7.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(50.dp))
                            .background(themedColor(0xFF233247))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress)
                                .height(10.dp)
                                .clip(RoundedCornerShape(50.dp))
                                .background(themedColor(0xFF22C55E))
                        )
                    }

                    Spacer(modifier = Modifier.height(7.dp))

                    Text(
                        text = if (goodPosture)
                            "Correction held: ${holdSeconds}s"
                        else
                            "Correct your posture, then hold."
                        ,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (goodPosture)
                            themedColor(0xFF4ADE80)
                        else
                            themedColor(0xFF94A3B8)
                    )

                    Spacer(modifier = Modifier.height(13.dp))

                    Button(
                        onClick = onStartCorrection,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(17.dp)
                    ) {
                        Text(
                            text = "START GUIDED CORRECTION",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                CoachValueCard(
                    title = "UPPER",
                    value = String.format(Locale.US, "%.1f°", upperAngle),
                    deviation = upperDifference,
                    modifier = Modifier.weight(1f)
                )

                CoachValueCard(
                    title = "LOWER",
                    value = String.format(Locale.US, "%.1f°", lowerAngle),
                    deviation = lowerDifference,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun CoachBodyPreview(
    upperAngle: Double,
    lowerAngle: Double,
    targetColor: Color,
    modifier: Modifier = Modifier
) {
    val u by animateFloatAsState(
        targetValue = upperAngle.coerceIn(-25.0, 25.0).toFloat(),
        animationSpec = tween(280, easing = FastOutSlowInEasing),
        label = "coachUpper"
    )

    val l by animateFloatAsState(
        targetValue = lowerAngle.coerceIn(-25.0, 25.0).toFloat(),
        animationSpec = tween(280, easing = FastOutSlowInEasing),
        label = "coachLower"
    )

    Canvas(modifier = modifier.background(themedColor(0xFF0E2137))) {
        val cx = size.width / 2f
        val s = min(size.width / 240f, size.height / 250f)
        val ur = Math.toRadians(u.toDouble())
        val lr = Math.toRadians(l.toDouble())

        val head = Offset(
            cx + (kotlin.math.sin(ur) * 8f).toFloat() * s,
            30f * s
        )

        val upper = Offset(
            cx + (kotlin.math.sin(ur) * 25f).toFloat() * s,
            88f * s
        )

        val middle = Offset(
            cx + (
                    kotlin.math.sin(ur) * 12f +
                            kotlin.math.sin(lr) * 6f
                    ).toFloat() * s,
            130f * s
        )

        val lower = Offset(
            cx + (kotlin.math.sin(lr) * 28f).toFloat() * s,
            175f * s
        )

        val hip = Offset(
            cx + (kotlin.math.sin(lr) * 38f).toFloat() * s,
            205f * s
        )

        drawCircle(
            color = themedColor(0xFF2C4964),
            radius = 21f * s,
            center = head
        )

        drawLine(
            color = themedColor(0xFF203C55),
            start = Offset(head.x, head.y + 18f * s),
            end = Offset(head.x, 57f * s),
            strokeWidth = 11f * s,
            cap = StrokeCap.Round
        )

        drawLine(
            color = themedColor(0xFF25445E),
            start = Offset(head.x - 43f * s, 67f * s),
            end = Offset(head.x + 43f * s, 67f * s),
            strokeWidth = 17f * s,
            cap = StrokeCap.Round
        )

        val spine = Path().apply {
            moveTo(head.x, 54f * s)
            lineTo(upper.x, upper.y)
            lineTo(middle.x, middle.y)
            lineTo(lower.x, lower.y)
            lineTo(hip.x, hip.y)
        }

        drawPath(
            path = spine,
            color = targetColor.copy(alpha = 0.16f),
            style = Stroke(
                width = 17f * s,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        drawLine(
            color = targetColor,
            start = Offset(head.x, 54f * s),
            end = upper,
            strokeWidth = 6f * s,
            cap = StrokeCap.Round
        )

        drawLine(
            color = targetColor,
            start = upper,
            end = middle,
            strokeWidth = 6f * s,
            cap = StrokeCap.Round
        )

        drawLine(
            color = targetColor,
            start = middle,
            end = lower,
            strokeWidth = 6f * s,
            cap = StrokeCap.Round
        )

        drawLine(
            color = targetColor,
            start = lower,
            end = hip,
            strokeWidth = 6f * s,
            cap = StrokeCap.Round
        )

        listOf(upper, middle, lower).forEach { pt ->
            drawCircle(
                color = targetColor.copy(alpha = 0.15f),
                radius = 12f * s,
                center = pt
            )

            drawCircle(
                color = targetColor,
                radius = 5f * s,
                center = pt
            )
        }
    }
}

@Composable
private fun CoachValueCard(
    title: String,
    value: String,
    deviation: Double,
    modifier: Modifier = Modifier
) {
    val c = zoneForDeviation(deviation)

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(themedColor(0xFF101B29))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = themedColor(0xFF94A3B8)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = themedWhite()
            )

            Text(
                text = String.format(
                    Locale.US,
                    "Δ %.1f° • %s",
                    deviation,
                    mirrorStatus(deviation)
                ),
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = c
            )
        }
    }
}


// ============================================================
// RECOVERY SCORE
// ============================================================

@Composable
private fun RecoveryScoreCard(
    recoveryCount: Int,
    bestRecoverySeconds: Long,
    averageRecoverySeconds: Long,
    lastRecoverySeconds: Long
) {
    val score = when {
        recoveryCount == 0 -> 0
        averageRecoverySeconds <= 3L -> 100
        averageRecoverySeconds <= 5L -> 92
        averageRecoverySeconds <= 8L -> 82
        averageRecoverySeconds <= 12L -> 68
        averageRecoverySeconds <= 20L -> 52
        else -> 35
    }

    val color = when {
        recoveryCount == 0 -> themedColor(0xFF64748B)
        score >= 85 -> themedColor(0xFF22C55E)
        score >= 65 -> themedColor(0xFFF59E0B)
        else -> themedColor(0xFFEF4444)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = themedWhite())
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "RECOVERY SCORE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = themedColor(0xFF64748B)
                    )
                    Text(
                        text = if (recoveryCount == 0)
                            "Waiting for a recovery"
                        else
                            "How quickly you return to good posture",
                        fontSize = 10.sp,
                        color = themedColor(0xFF94A3B8)
                    )
                }

                Text(
                    text = if (recoveryCount == 0) "--" else "$score/100",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = color
                )
            }

            Spacer(modifier = Modifier.height(11.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                RecoveryMetric("RECOVERIES", "$recoveryCount", Modifier.weight(1f))
                RecoveryMetric(
                    "BEST",
                    if (bestRecoverySeconds == Long.MAX_VALUE) "--" else "${bestRecoverySeconds}s",
                    Modifier.weight(1f)
                )
                RecoveryMetric(
                    "AVERAGE",
                    if (recoveryCount == 0) "--" else "${averageRecoverySeconds}s",
                    Modifier.weight(1f)
                )
                RecoveryMetric(
                    "LAST",
                    if (lastRecoverySeconds == 0L) "--" else "${lastRecoverySeconds}s",
                    Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = when {
                    recoveryCount == 0 ->
                        "Recovery is measured from a bad-posture event until the sensors return to good range."
                    averageRecoverySeconds <= 4L ->
                        "Excellent recovery speed."
                    averageRecoverySeconds <= 8L ->
                        "Good recovery speed."
                    else ->
                        "Recovery is taking longer; use the Posture Coach."
                },
                fontSize = 10.sp,
                color = themedColor(0xFF64748B)
            )
        }
    }
}

@Composable
private fun RecoveryMetric(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(13.dp))
            .background(themedColor(0xFFF8FAFC))
            .padding(8.dp)
    ) {
        Text(
            text = title,
            fontSize = 7.sp,
            fontWeight = FontWeight.Bold,
            color = themedColor(0xFF94A3B8)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            color = themedColor(0xFF0F172A)
        )
    }
}

// ============================================================
// SMART MIRROR MODE
// ============================================================

@Composable
private fun SmartMirrorCard(
    goodPosture: Boolean,
    upperAngle: Double,
    lowerAngle: Double,
    upperDifference: Double,
    lowerDifference: Double,
    onOpen: () -> Unit
) {
    val maxDeviation = max(
        abs(upperDifference),
        abs(lowerDifference)
    )

    val accent = when {
        maxDeviation <= 10.0 -> themedColor(0xFF22C55E)
        maxDeviation <= 20.0 -> themedColor(0xFFF59E0B)
        else -> themedColor(0xFFEF4444)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = themedColor(0xFF071425)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(themedColor(0xFF0E2037)),
                contentAlignment = Alignment.Center
            ) {
                SmartMirrorMiniBody(
                    upperAngle = upperAngle,
                    lowerAngle = lowerAngle,
                    accent = accent,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                )
            }

            Spacer(modifier = Modifier.size(13.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SMART MIRROR",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = themedWhite()
                    )

                    Spacer(modifier = Modifier.size(7.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50.dp))
                            .background(accent.copy(alpha = 0.15f))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = if (goodPosture) "ALIGNED" else "WATCH",
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Bold,
                            color = accent
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "See your live posture like a digital mirror.",
                    fontSize = 10.sp,
                    color = themedColor(0xFF94A3B8)
                )

                Spacer(modifier = Modifier.height(7.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = String.format(
                            Locale.US,
                            "U %.1f°",
                            upperAngle
                        ),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = themedColor(0xFF67E8F9)
                    )

                    Text(
                        text = String.format(
                            Locale.US,
                            "L %.1f°",
                            lowerAngle
                        ),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = themedColor(0xFF86EFAC)
                    )
                }
            }

            Text(
                text = "›",
                fontSize = 28.sp,
                color = themedColor(0xFF64748B)
            )
        }
    }
}

@Composable
private fun SmartMirrorOverlay(
    goodPosture: Boolean,
    upperAngle: Double,
    lowerAngle: Double,
    upperDifference: Double,
    lowerDifference: Double,
    onClose: () -> Unit
) {
    val maxDeviation = max(
        abs(upperDifference),
        abs(lowerDifference)
    )

    val accent = when {
        maxDeviation <= 10.0 -> themedColor(0xFF22C55E)
        maxDeviation <= 20.0 -> themedColor(0xFFF59E0B)
        else -> themedColor(0xFFEF4444)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(themedColor(0xEE020814))
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "SMART MIRROR",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = themedWhite()
                    )
                    Text(
                        text = "Real-time posture reflection",
                        fontSize = 11.sp,
                        color = themedColor(0xFF94A3B8)
                    )
                }

                Text(
                    text = "×",
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(onClick = onClose)
                        .padding(8.dp),
                    fontSize = 25.sp,
                    color = themedColor(0xFFCBD5E1)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(30.dp),
                colors = CardDefaults.cardColors(themedColor(0xFF091525))
            ) {
                Column(
                    modifier = Modifier.padding(13.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "LIVE REFLECTION",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = themedColor(0xFF64748B)
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = if (goodPosture)
                                    "GREAT POSTURE"
                                else
                                    "ADJUST YOUR POSTURE",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = accent
                            )
                        }

                        Text(
                            text = when {
                                maxDeviation <= 10.0 -> "GOOD"
                                maxDeviation <= 20.0 -> "WATCH"
                                else -> "BAD"
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = accent
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(500.dp),
                        shape = RoundedCornerShape(25.dp),
                        colors = CardDefaults.cardColors(themedColor(0xFF0C1C30))
                    ) {
                        SmartMirrorFullBody(
                            upperAngle = upperAngle,
                            lowerAngle = lowerAngle,
                            upperDifference = upperDifference,
                            lowerDifference = lowerDifference,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        MirrorValue(
                            title = "UPPER SPINE",
                            value = String.format(Locale.US, "%.1f°", upperAngle),
                            status = mirrorStatus(upperDifference),
                            color = zoneForDeviation(upperDifference),
                            modifier = Modifier.weight(1f)
                        )

                        MirrorValue(
                            title = "LOWER SPINE",
                            value = String.format(Locale.US, "%.1f°", lowerAngle),
                            status = mirrorStatus(lowerDifference),
                            color = zoneForDeviation(lowerDifference),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = accent.copy(alpha = 0.10f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(accent.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (goodPosture) "✓" else "!",
                            fontSize = 21.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = accent
                        )
                    }

                    Spacer(modifier = Modifier.size(11.dp))

                    Column {
                        Text(
                            text = if (goodPosture)
                                "Hold this natural position"
                            else
                                "Small correction recommended",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = themedWhite()
                        )

                        Spacer(modifier = Modifier.height(3.dp))

                        Text(
                            text = "Use the spine markers as your visual guide.",
                            fontSize = 10.sp,
                            color = themedColor(0xFF94A3B8)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SmartMirrorMiniBody(
    upperAngle: Double,
    lowerAngle: Double,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val ur = Math.toRadians(upperAngle.coerceIn(-25.0, 25.0))
        val lr = Math.toRadians(lowerAngle.coerceIn(-25.0, 25.0))

        val upperX = (kotlin.math.sin(ur) * 9f).toFloat()
        val lowerX = (kotlin.math.sin(lr) * 7f).toFloat()

        drawCircle(
            color = themedColor(0xFF334B65),
            radius = 10f,
            center = Offset(cx, 13f)
        )

        drawLine(
            color = themedColor(0xFF29425A),
            start = Offset(cx, 22f),
            end = Offset(cx + upperX, 55f),
            strokeWidth = 7f,
            cap = StrokeCap.Round
        )

        drawLine(
            color = themedColor(0xFF29425A),
            start = Offset(cx - 16f, 30f),
            end = Offset(cx + 16f, 30f),
            strokeWidth = 5f,
            cap = StrokeCap.Round
        )

        drawLine(
            color = accent,
            start = Offset(cx, 39f),
            end = Offset(cx + upperX, 52f),
            strokeWidth = 3f,
            cap = StrokeCap.Round
        )

        drawLine(
            color = accent,
            start = Offset(cx + upperX, 52f),
            end = Offset(cx + lowerX, 69f),
            strokeWidth = 3f,
            cap = StrokeCap.Round
        )

        drawCircle(
            color = accent,
            radius = 3.5f,
            center = Offset(cx + upperX, 52f)
        )

        drawCircle(
            color = accent,
            radius = 3.5f,
            center = Offset(cx + lowerX, 69f)
        )
    }
}

@Composable
private fun SmartMirrorFullBody(
    upperAngle: Double,
    lowerAngle: Double,
    upperDifference: Double,
    lowerDifference: Double,
    modifier: Modifier = Modifier
) {
    val upperColor = zoneForDeviation(upperDifference)
    val lowerColor = zoneForDeviation(lowerDifference)

    val upperTilt by animateFloatAsState(
        targetValue = upperAngle.coerceIn(-30.0, 30.0).toFloat(),
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "mirrorUpper"
    )

    val lowerTilt by animateFloatAsState(
        targetValue = lowerAngle.coerceIn(-30.0, 30.0).toFloat(),
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "mirrorLower"
    )

    Canvas(modifier = modifier.padding(10.dp)) {
        val scale = min(size.width / 360f, size.height / 505f)
        val cx = size.width / 2f

        fun p(dx: Float, y: Float): Offset =
            Offset(cx + dx * scale, y * scale)

        val ur = Math.toRadians(upperTilt.toDouble())
        val lr = Math.toRadians(lowerTilt.toDouble())

        val head = p((kotlin.math.sin(ur) * 8f).toFloat(), 53f)
        val neck = p((kotlin.math.sin(ur) * 9f).toFloat(), 87f)
        val shoulder = p((kotlin.math.sin(ur) * 16f).toFloat(), 108f)
        val upper = p((kotlin.math.sin(ur) * 29f).toFloat(), 164f)
        val middle = p(
            (kotlin.math.sin(ur) * 16f +
                    kotlin.math.sin(lr) * 7f).toFloat(),
            229f
        )
        val lower = p((kotlin.math.sin(lr) * 35f).toFloat(), 302f)
        val hip = p((kotlin.math.sin(lr) * 43f).toFloat(), 350f)

        drawCircle(
            color = themedColor(0xFF13283F),
            radius = 135f * scale,
            center = Offset(cx, 245f * scale)
        )

        // Grid/centerline
        drawLine(
            color = themedColor(0xFF2A3B4F),
            start = p(0f, 20f),
            end = p(0f, 468f),
            strokeWidth = 1f * scale
        )

        // Torso silhouette
        val torso = Path().apply {
            moveTo(cx - 75f * scale, 112f * scale)
            cubicTo(
                cx - 104f * scale,
                150f * scale,
                cx - 103f * scale,
                268f * scale,
                cx - 73f * scale,
                330f * scale
            )
            cubicTo(
                cx - 48f * scale,
                374f * scale,
                cx - 34f * scale,
                423f * scale,
                cx - 25f * scale,
                480f * scale
            )
            lineTo(cx + 25f * scale, 480f * scale)
            cubicTo(
                cx + 34f * scale,
                423f * scale,
                cx + 48f * scale,
                374f * scale,
                cx + 73f * scale,
                330f * scale
            )
            cubicTo(
                cx + 103f * scale,
                268f * scale,
                cx + 104f * scale,
                150f * scale,
                cx + 75f * scale,
                112f * scale
            )
            close()
        }

        drawPath(
            path = torso,
            color = themedColor(0xFF14273B)
        )

        // Shoulder blades / depth
        drawOval(
            color = themedColor(0xFF243C55),
            topLeft = Offset(
                shoulder.x - 51f * scale,
                shoulder.y + 12f * scale
            ),
            size = androidx.compose.ui.geometry.Size(
                40f * scale,
                104f * scale
            )
        )

        drawOval(
            color = themedColor(0xFF243C55),
            topLeft = Offset(
                shoulder.x + 11f * scale,
                shoulder.y + 12f * scale
            ),
            size = androidx.compose.ui.geometry.Size(
                40f * scale,
                104f * scale
            )
        )

        // Head
        drawCircle(
            color = themedColor(0xFF233A52),
            radius = 31f * scale,
            center = head
        )

        drawCircle(
            color = themedColor(0xFF3A5873),
            radius = 9f * scale,
            center = Offset(
                head.x - 7f * scale,
                head.y - 8f * scale
            )
        )

        // Neck and shoulders
        drawLine(
            color = themedColor(0xFF1F3851),
            start = Offset(head.x, head.y + 25f * scale),
            end = neck,
            strokeWidth = 15f * scale,
            cap = StrokeCap.Round
        )

        drawLine(
            color = themedColor(0xFF1C334B),
            start = Offset(
                shoulder.x - 62f * scale,
                shoulder.y
            ),
            end = Offset(
                shoulder.x + 62f * scale,
                shoulder.y
            ),
            strokeWidth = 28f * scale,
            cap = StrokeCap.Round
        )

        // Arms
        drawLine(
            color = themedColor(0xFF193047),
            start = Offset(
                shoulder.x - 53f * scale,
                shoulder.y + 5f * scale
            ),
            end = p(-54f, 250f),
            strokeWidth = 20f * scale,
            cap = StrokeCap.Round
        )

        drawLine(
            color = themedColor(0xFF193047),
            start = Offset(
                shoulder.x + 53f * scale,
                shoulder.y + 5f * scale
            ),
            end = p(54f, 250f),
            strokeWidth = 20f * scale,
            cap = StrokeCap.Round
        )

        // Rib cage
        for (i in 0..5) {
            val y = (140f + i * 22f) * scale
            val half = (43f - i * 3f) * scale

            drawArc(
                color = themedColor(0xFF3A5269),
                startAngle = 200f,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = Offset(cx - half, y),
                size = androidx.compose.ui.geometry.Size(
                    half * 2f,
                    33f * scale
                ),
                style = Stroke(width = 1.5f * scale)
            )
        }

        // Spine shadow
        val spine = Path().apply {
            moveTo(neck.x, neck.y)
            lineTo(
                cx + (kotlin.math.sin(ur) * 18f).toFloat() * scale,
                126f * scale
            )
            lineTo(upper.x, upper.y)
            lineTo(middle.x, middle.y)
            lineTo(lower.x, lower.y)
            lineTo(hip.x, hip.y)
        }

        drawPath(
            path = spine,
            color = themedColor(0xFF03080E),
            style = Stroke(
                width = 22f * scale,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        // Live color-coded spine
        drawLine(
            color = upperColor,
            start = neck,
            end = upper,
            strokeWidth = 9f * scale,
            cap = StrokeCap.Round
        )

        drawLine(
            color = upperColor,
            start = upper,
            end = middle,
            strokeWidth = 9f * scale,
            cap = StrokeCap.Round
        )

        drawLine(
            color = lowerColor,
            start = middle,
            end = lower,
            strokeWidth = 9f * scale,
            cap = StrokeCap.Round
        )

        drawLine(
            color = lowerColor,
            start = lower,
            end = hip,
            strokeWidth = 9f * scale,
            cap = StrokeCap.Round
        )

        // Pelvis + legs
        drawLine(
            color = themedColor(0xFF2D455E),
            start = Offset(
                hip.x - 49f * scale,
                hip.y
            ),
            end = Offset(
                hip.x + 49f * scale,
                hip.y
            ),
            strokeWidth = 24f * scale,
            cap = StrokeCap.Round
        )

        drawLine(
            color = themedColor(0xFF1A3148),
            start = hip,
            end = Offset(
                hip.x - 31f * scale,
                477f * scale
            ),
            strokeWidth = 22f * scale,
            cap = StrokeCap.Round
        )

        drawLine(
            color = themedColor(0xFF1A3148),
            start = hip,
            end = Offset(
                hip.x + 31f * scale,
                477f * scale
            ),
            strokeWidth = 22f * scale,
            cap = StrokeCap.Round
        )

        // Vertebrae
        val vertebrae = listOf(
            p((kotlin.math.sin(ur) * 12f).toFloat(), 112f),
            p((kotlin.math.sin(ur) * 20f).toFloat(), 134f),
            upper,
            p(
                (
                        kotlin.math.sin(ur) * 12f +
                                kotlin.math.sin(lr) * 5f
                        ).toFloat(),
                194f
            ),
            middle,
            p((kotlin.math.sin(lr) * 20f).toFloat(), 265f),
            lower,
            p((kotlin.math.sin(lr) * 38f).toFloat(), 326f)
        )

        vertebrae.forEachIndexed { index, pt ->
            val c =
                if (index <= 4) upperColor
                else lowerColor

            val radius =
                if (index == 2 || index == 6)
                    7f * scale
                else
                    5f * scale

            drawCircle(
                color = c.copy(alpha = 0.17f),
                radius = radius * 2.4f,
                center = pt
            )

            drawCircle(
                color = c,
                radius = radius,
                center = pt
            )
        }
    }

    // Labels
    Column(
        modifier = Modifier
            .padding(start = 10.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "UPPER",
            fontSize = 7.sp,
            fontWeight = FontWeight.Bold,
            color = upperColor
        )
        Text(
            text = String.format(Locale.US, "%.1f°", upperAngle),
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold,
            color = themedWhite()
        )
        Text(
            text = mirrorStatus(upperDifference),
            fontSize = 7.sp,
            fontWeight = FontWeight.Bold,
            color = upperColor
        )
    }

    Column(
        modifier = Modifier
            .padding(end = 10.dp),
        horizontalAlignment = Alignment.End
    ) {
        Text(
            text = "LOWER",
            fontSize = 7.sp,
            fontWeight = FontWeight.Bold,
            color = lowerColor
        )
        Text(
            text = String.format(Locale.US, "%.1f°", lowerAngle),
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold,
            color = themedWhite()
        )
        Text(
            text = mirrorStatus(lowerDifference),
            fontSize = 7.sp,
            fontWeight = FontWeight.Bold,
            color = lowerColor
        )
    }
}

@Composable
private fun MirrorValue(
    title: String,
    value: String,
    status: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(themedColor(0xFF0F172A))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = themedColor(0xFF94A3B8)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = value,
                fontSize = 21.sp,
                fontWeight = FontWeight.ExtraBold,
                color = themedWhite()
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = status,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

private fun zoneForDeviation(deviation: Double): Color =
    when {
        deviation <= 10.0 -> themedColor(0xFF22C55E)
        deviation <= 20.0 -> themedColor(0xFFF59E0B)
        else -> themedColor(0xFFEF4444)
    }

private fun mirrorStatus(deviation: Double): String =
    when {
        deviation <= 10.0 -> "GOOD"
        deviation <= 20.0 -> "WARNING"
        else -> "BAD"
    }

// ============================================================
// HOME SCREEN
// — MODERN MODULAR DESIGN + POSTURE AI
// ============================================================

@Composable
fun HomeScreen(
    connected: Boolean,
    goodPosture: Boolean,
    upperAngle: Double,
    lowerAngle: Double,
    upperDifference: Double,
    lowerDifference: Double,
    postureScore: Int,
    goodSeconds: Long,
    badSeconds: Long,
    alertCount: Int,
    badPostureSeconds: Long,
    warningLevel: Int,
    onOpenSmartMirror: () -> Unit = {},
    onOpenCorrectionCoach: () -> Unit = {},
    onOpenPostureIQ: () -> Unit = {},
    recoveryCount: Int = 0,
    bestRecoverySeconds: Long = Long.MAX_VALUE,
    averageRecoverySeconds: Long = 0L,
    lastRecoverySeconds: Long = 0L,
    dailyGoodSeconds: Long = 0L,
    currentStreak: Int = 0,
    challengeCompletedToday: Boolean = false,
    modifier: Modifier = Modifier
) {
    var aiOpen by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(themedColor(0xFFF4F7FB))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 18.dp)
        ) {
            val totalSeconds = goodSeconds + badSeconds

            // --------------------------------------------------------
            // TOP BAR
            // --------------------------------------------------------
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "SMART POSTURE",
                        fontSize = 27.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = themedColor(0xFF0F172A)
                    )

                    Text(
                        text = "Your posture, understood.",
                        fontSize = 12.sp,
                        color = themedColor(0xFF64748B)
                    )
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50.dp))
                        .background(
                            if (connected) themedColor(0xFFE7F8EE)
                            else themedColor(0xFFFFE8E8)
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                if (connected) themedColor(0xFF16A34A)
                                else themedColor(0xFFDC2626)
                            )
                    )

                    Spacer(modifier = Modifier.size(6.dp))

                    Text(
                        text = if (connected) "LIVE" else "OFFLINE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (connected)
                            themedColor(0xFF16A34A)
                        else
                            themedColor(0xFFDC2626)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // --------------------------------------------------------
            // HERO POSTURE CARD
            // --------------------------------------------------------
            val heroColor =
                if (goodPosture) themedColor(0xFF0F766E)
                else themedColor(0xFFBE123C)

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(30.dp),
                colors = CardDefaults.cardColors(
                    containerColor = heroColor
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "CURRENT POSTURE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = themedWhite().copy(alpha = 0.72f)
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            AnimatedContent(
                                targetState = goodPosture,
                                transitionSpec = {
                                    fadeIn(tween(250)).togetherWith(
                                        fadeOut(tween(180))
                                    )
                                },
                                label = "homePostureTitle"
                            ) { good ->
                                Text(
                                    text = if (good)
                                        "GOOD POSTURE"
                                    else
                                        "CORRECT POSTURE",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = themedWhite()
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = when {
                                    !connected -> "Waiting for ESP32 data"
                                    goodPosture ->
                                        "Your alignment is within your personal range"
                                    warningLevel >= 3 ->
                                        "Bad posture has continued for ${badPostureSeconds}s"
                                    warningLevel >= 2 ->
                                        "You have been out of range for ${badPostureSeconds}s"
                                    else ->
                                        "Small posture change detected"
                                },
                                fontSize = 12.sp,
                                color = themedWhite().copy(alpha = 0.78f)
                            )
                        }

                        Box(
                            modifier = Modifier.size(108.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val stroke = 10.dp.toPx()

                                drawArc(
                                    color = themedWhite().copy(alpha = 0.18f),
                                    startAngle = -90f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    style = Stroke(width = stroke)
                                )

                                drawArc(
                                    color = themedWhite(),
                                    startAngle = -90f,
                                    sweepAngle =
                                        postureScore.coerceIn(0, 100) * 3.6f,
                                    useCenter = false,
                                    style = Stroke(width = stroke)
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$postureScore",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = themedWhite()
                                )

                                Text(
                                    text = "/100",
                                    fontSize = 10.sp,
                                    color = themedWhite().copy(alpha = 0.72f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        QuickMetric(
                            title = "UPPER",
                            value = String.format(Locale.US, "%.1f°", upperAngle),
                            modifier = Modifier.weight(1f)
                        )

                        QuickMetric(
                            title = "LOWER",
                            value = String.format(Locale.US, "%.1f°", lowerAngle),
                            modifier = Modifier.weight(1f)
                        )

                        QuickMetric(
                            title = "ALERTS",
                            value = alertCount.toString(),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // POSTUREIQ AI MODE LAUNCHER
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenPostureIQ() },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF10162D))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier.size(54.dp).clip(RoundedCornerShape(17.dp))
                            .background(Color(0xFF243B70)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✦", fontSize = 29.sp, color = Color(0xFF67E8F9), fontWeight = FontWeight.Bold)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("POSTUREIQ  ·  AI MODE", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("6 intelligent tools • live sensor insights", fontSize = 11.sp, color = Color(0xFFB8C5E8))
                    }
                    Text("OPEN  ›", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF67E8F9))
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            SmartMirrorCard(
                goodPosture = goodPosture,
                upperAngle = upperAngle,
                lowerAngle = lowerAngle,
                upperDifference = upperDifference,
                lowerDifference = lowerDifference,
                onOpen = onOpenSmartMirror
            )

            Spacer(modifier = Modifier.height(14.dp))

            CorrectionCoachCard(
                goodPosture = goodPosture,
                upperDifference = upperDifference,
                lowerDifference = lowerDifference,
                onOpen = onOpenCorrectionCoach
            )

            Spacer(modifier = Modifier.height(14.dp))

            RecoveryScoreCard(
                recoveryCount = recoveryCount,
                bestRecoverySeconds = bestRecoverySeconds,
                averageRecoverySeconds = averageRecoverySeconds,
                lastRecoverySeconds = lastRecoverySeconds
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "SPINE SNAPSHOT",
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = themedColor(0xFF334155)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ModernSensorCard(
                    title = "Upper spine",
                    angle = upperAngle,
                    deviation = upperDifference,
                    good = upperDifference <= 10.0,
                    modifier = Modifier.weight(1f)
                )

                ModernSensorCard(
                    title = "Lower spine",
                    angle = lowerAngle,
                    deviation = lowerDifference,
                    good = lowerDifference <= 10.0,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = themedWhite())
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "SESSION SNAPSHOT",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = themedColor(0xFF64748B)
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = String.format(
                                    Locale.US,
                                    "%02d:%02d",
                                    totalSeconds / 60L,
                                    totalSeconds % 60L
                                ),
                                fontSize = 27.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = themedColor(0xFF0F172A)
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            val pct =
                                if (totalSeconds > 0L)
                                    goodSeconds * 100L / totalSeconds
                                else
                                    0L

                            Text(
                                text = "$pct% good",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = themedColor(0xFF16A34A)
                            )

                            Text(
                                text = "$alertCount alerts",
                                fontSize = 11.sp,
                                color = themedColor(0xFF94A3B8)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(9.dp)
                            .clip(RoundedCornerShape(50.dp))
                            .background(themedColor(0xFFE8EEF5))
                    ) {
                        val progress =
                            if (totalSeconds > 0L)
                                goodSeconds.toFloat() / totalSeconds.toFloat()
                            else
                                0f

                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress.coerceIn(0f, 1f))
                                .height(9.dp)
                                .clip(RoundedCornerShape(50.dp))
                                .background(themedColor(0xFF22C55E))
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(55.dp))
        }

        // --------------------------------------------------------
        // FLOATING AI BUTTON
        // --------------------------------------------------------
        Card(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 18.dp, bottom = 18.dp)
                .clickable { aiOpen = !aiOpen },
            shape = CircleShape,
            colors = CardDefaults.cardColors(
                containerColor = themedColor(0xFF2563EB)
            )
        ) {
            Box(
                modifier = Modifier.size(62.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "AI",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = themedWhite()
                )
            }
        }

        if (aiOpen) {
            PostureAIAssistant(
                postureScore = postureScore,
                goodPosture = goodPosture,
                connected = connected,
                upperAngle = upperAngle,
                lowerAngle = lowerAngle,
                upperDifference = upperDifference,
                lowerDifference = lowerDifference,
                alertCount = alertCount,
                onClose = { aiOpen = false },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 88.dp)
            )
        }
    }
}

@Composable
private fun PostureIQOverlay(
    connected: Boolean,
    goodPosture: Boolean,
    upperAngle: Double,
    lowerAngle: Double,
    upperDifference: Double,
    lowerDifference: Double,
    postureScore: Int,
    alertCount: Int,
    recoveryCount: Int,
    averageRecoverySeconds: Long,
    currentStreak: Int,
    upperDeviationHistory: List<Double>,
    lowerDeviationHistory: List<Double>,
    onGenerateReport: () -> Unit,
    onClose: () -> Unit
) {
    val background = Color(0xFF070B18)
    val panel = Color(0xFF111A30)
    val cyan = Color(0xFF67E8F9)
    val violet = Color(0xFFA78BFA)
    val mainDeviation = max(abs(upperDifference), abs(lowerDifference))
    val recentUpper = upperDeviationHistory.takeLast(8)
    val recentLower = lowerDeviationHistory.takeLast(8)
    val trend = when {
        recentUpper.size < 3 || recentLower.size < 3 -> "Collecting baseline"
        (recentUpper.takeLast(3).average() + recentLower.takeLast(3).average()) / 2.0 >
                (recentUpper.take(3).average() + recentLower.take(3).average()) / 2.0 + 1.5 -> "Risk trend rising"
        else -> "No strong worsening trend"
    }
    val dominantRegion = if (abs(upperDifference) >= abs(lowerDifference)) "upper back" else "lower back"
    val coachMessage = when {
        !connected -> "Connect the belt to receive live personalized guidance."
        goodPosture -> "Your current readings are in the good-posture range. Keep the position comfortable; avoid forcing your spine."
        abs(upperDifference) >= abs(lowerDifference) -> "Your upper-back deviation is currently larger. Gently reset your shoulders and sit tall, without straining."
        else -> "Your lower-back deviation is currently larger. Adjust your sitting support and keep a comfortable neutral position."
    }
    val personalPattern = when {
        upperDeviationHistory.size < 10 || lowerDeviationHistory.size < 10 -> "Building your baseline — keep the belt connected during normal sessions."
        abs(upperDeviationHistory.average() - lowerDeviationHistory.average()) > 2.0 -> "Your recorded sessions show a stronger average deviation in one sensor region."
        else -> "Your recorded upper/lower sensor averages are relatively balanced."
    }

    Box(modifier = Modifier.fillMaxSize().background(background)) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("POSTUREIQ", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = cyan, letterSpacing = 3.sp)
                    Text("AI Intelligence Mode", fontSize = 27.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    Text("Your wearable data, translated into useful insights", fontSize = 12.sp, color = Color(0xFF9EABC9))
                }
                TextButton(onClick = onClose) { Text("CLOSE ✕", color = cyan, fontWeight = FontWeight.Bold) }
            }

            Card(shape = RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF182849))) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(if (connected) "LIVE SENSOR ANALYSIS" else "WAITING FOR SENSOR", color = Color(0xFFB7C9EC), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text("$postureScore", color = Color.White, fontSize = 46.sp, fontWeight = FontWeight.Black)
                            Text("POSTURE SCORE / 100", color = cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(if (goodPosture) "● GOOD" else "● ADJUST", color = if (goodPosture) Color(0xFF6EE7B7) else Color(0xFFFDA4AF), fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                            Text("Upper  ${String.format(Locale.US, "%.1f°", upperAngle)}", color = Color.White, fontSize = 12.sp)
                            Text("Lower  ${String.format(Locale.US, "%.1f°", lowerAngle)}", color = Color.White, fontSize = 12.sp)
                        }
                    }
                    Box(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(Color(0xFF33415F))) {
                        Box(Modifier.fillMaxWidth(postureScore.coerceIn(0, 100) / 100f).height(8.dp).clip(CircleShape).background(cyan))
                    }
                    Text("Prototype insight engine: current insights use sensor trends and rules. A trained ML model is not yet connected.", color = Color(0xFFB7C9EC), fontSize = 11.sp)
                }
            }

            Text("SIX INTELLIGENCE FEATURES", color = violet, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
            PostureIQFeatureCard("01", "Early Slouch Prediction", "TREND MONITOR", "${trend}. Current maximum deviation: ${String.format(Locale.US, "%.1f°", mainDeviation)}. This is a trend heuristic, not a validated prediction model.", cyan)
            PostureIQFeatureCard("02", "Personal Posture DNA", "YOUR PATTERN", personalPattern, violet)
            PostureIQFeatureCard("03", "Adaptive Posture Coach", "PERSONALIZED TIP", coachMessage, cyan)
            PostureIQFeatureCard("04", "Recovery Intelligence", "RECOVERY TRACKING", if (recoveryCount > 0) "Recorded recoveries: $recoveryCount. Average recovery: ${averageRecoverySeconds}s. Current streak: $currentStreak day(s)." else "Recovery statistics will appear after the app records a bad-to-good posture recovery.", violet)
            PostureIQFeatureCard("05", "Live Posture Classification", "SENSOR STATUS", if (!connected) "No live ESP32 data yet. Reconnect to your belt to analyze the latest readings." else if (goodPosture) "Current sensor state: GOOD POSTURE. Continue in a relaxed, comfortable position." else "Current sensor state: POSTURE NEEDS ATTENTION. The $dominantRegion has the larger absolute deviation right now.", cyan)

            Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = panel)) {
                Column(Modifier.fillMaxWidth().padding(17.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("06  ·  AI POSTURE REPORT", color = violet, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Create a report from the app's existing measured session data, including score, sensor angles, alerts and recovery metrics.", color = Color(0xFFD5DDF0), fontSize = 12.sp)
                    Button(onClick = onGenerateReport, modifier = Modifier.fillMaxWidth()) { Text("GENERATE POSTURE REPORT") }
                    Text("Alerts recorded: $alertCount", color = Color(0xFF9EABC9), fontSize = 11.sp)
                }
            }
            Spacer(Modifier.height(22.dp))
        }
    }
}

@Composable
private fun PostureIQFeatureCard(
    number: String,
    title: String,
    tag: String,
    description: String,
    accent: Color
) {
    Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF111A30))) {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
            Box(Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(accent.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
                Text(number, color = accent, fontSize = 13.sp, fontWeight = FontWeight.Black)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(tag, color = accent, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.5.sp)
                Text(title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(description, color = Color(0xFFB8C5E0), fontSize = 12.sp, lineHeight = 18.sp)
            }
        }
    }
}

private data class PostureChatMessage(
    val text: String,
    val fromUser: Boolean
)

@Composable
fun PostureAIAssistant(
    postureScore: Int,
    goodPosture: Boolean,
    connected: Boolean,
    upperAngle: Double,
    lowerAngle: Double,
    upperDifference: Double,
    lowerDifference: Double,
    alertCount: Int,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }
    val listState = rememberScrollState()

    var input by remember { mutableStateOf("") }
    var listening by remember { mutableStateOf(false) }
    var isTyping by remember { mutableStateOf(false) }

    val messages = remember {
        mutableStateListOf(
            PostureChatMessage(
                "Hello! 👋 I’m your Posture AI. I can explain your live posture, score, spine deviation, alerts and improvement tips.",
                false
            )
        )
    }

    fun answerFor(question: String): String {
        val q = question.lowercase(Locale.US)
        val mainRegion = if (lowerDifference >= upperDifference) "lower spine" else "upper spine"
        val maxDeviation = max(upperDifference, lowerDifference)

        return when {
            q.contains("hello") || q.contains("hi") || q.contains("hey") ->
                "Hi! 👋 I’m ready. Ask me anything about your posture session."

            q.contains("score") ->
                "Your current posture score is $postureScore/100. " +
                        if (postureScore >= 90) "That’s excellent." else "I can also explain what is affecting the score."

            q.contains("why") && (q.contains("bad") || q.contains("wrong") || q.contains("low")) -> {
                if (goodPosture) {
                    "Your posture is currently in the good range. No major correction is needed right now."
                } else {
                    String.format(
                        Locale.US,
                        "The %s is contributing more to the current deviation. Upper deviation is %.2f° and lower deviation is %.2f°.",
                        mainRegion,
                        upperDifference,
                        lowerDifference
                    )
                }
            }

            q.contains("upper") ->
                String.format(
                    Locale.US,
                    "Upper spine: %.1f°. Deviation: %.2f°. Status: %s.",
                    upperAngle,
                    upperDifference,
                    postureZoneName(upperDifference)
                )

            q.contains("lower") ->
                String.format(
                    Locale.US,
                    "Lower spine: %.1f°. Deviation: %.2f°. Status: %s.",
                    lowerAngle,
                    lowerDifference,
                    postureZoneName(lowerDifference)
                )

            q.contains("alert") ->
                "You have $alertCount posture alert(s) in the current session. " +
                        if (alertCount == 0) "Great — no alerts so far." else "I recommend correcting persistent deviations early."

            q.contains("improve") || q.contains("fix") || q.contains("correct") ->
                if (goodPosture) {
                    "You’re currently aligned. Keep your shoulders relaxed, avoid leaning forward, and maintain the position that feels natural."
                } else {
                    "Try a small upright correction and hold it for a few seconds. Watch the live spine indicators move back toward green. Your largest current deviation is %.2f°.".format(Locale.US, maxDeviation)
                }

            q.contains("today") || q.contains("summary") ->
                String.format(
                    Locale.US,
                    "Session snapshot: score %d/100, upper %.1f°, lower %.1f°, upper deviation %.2f°, lower deviation %.2f°, alerts %d.",
                    postureScore,
                    upperAngle,
                    lowerAngle,
                    upperDifference,
                    lowerDifference,
                    alertCount
                )

            q.contains("connect") || q.contains("esp32") || q.contains("wifi") ->
                if (connected)
                    "ESP32 is connected and live posture data is arriving."
                else
                    "The ESP32 is offline. Connect your phone to the ESP32 Wi-Fi and then return to the app."

            q.contains("green") ->
                "Green means good alignment: deviation is 10° or less. Yellow means 10°–20°. Red means more than 20° deviation."

            q.contains("yellow") || q.contains("warning") ->
                "Yellow is the warning zone: deviation is above 10° and up to 20°. It means posture is drifting and needs attention."

            q.contains("red") || q.contains("bad posture") ->
                "Red is the bad zone: deviation is above 20°. The affected spine region is the priority for correction."

            q.contains("how are you") ->
                "I’m doing great — especially when your spine stays green. 😄"

            else ->
                "I can help with your score, upper/lower spine, alerts, Wi-Fi/ESP32 status, green-yellow-red zones, daily summary, or posture improvement."
        }
    }

    fun ask(question: String) {
        val clean = question.trim()
        if (clean.isEmpty()) return

        messages.add(PostureChatMessage(clean, true))
        input = ""
        isTyping = true
    }

    LaunchedEffect(messages.size, isTyping) {
        if (messages.size > 1 && isTyping) {
            delay(350)
            val userQuestion = messages.lastOrNull { it.fromUser }?.text.orEmpty()
            messages.add(PostureChatMessage(answerFor(userQuestion), false))
            isTyping = false
        }
        listState.animateScrollTo(listState.maxValue)
    }

    fun speak(text: String) {
        ttsSpeakOnce(context, text)
    }

    val requestAudioPermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startPostureSpeechRecognizer(
                context = context,
                onResult = { heard ->
                    if (Regex("\\b(p\\.?\\s*d\\.?\\s*f)\\b", RegexOption.IGNORE_CASE).containsMatchIn(heard)) {
                        globalReportAction?.invoke()
                        listening = false
                    } else {
                        input = heard
                        messages.add(PostureChatMessage(heard, true))
                        isTyping = true
                        listening = false
                    }
                },
                onListeningChanged = { listening = it }
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose { stopPostureSpeechRecognizer() }
    }

    // Full screen chat sheet. imePadding() keeps the composer above the phone keyboard.
    Card(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 395.dp)
            .imePadding(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = themedColor(0xFF080F1D)),
        elevation = CardDefaults.cardElevation(defaultElevation = 18.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PostureAILogo(modifier = Modifier.size(46.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Posture AI",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = themedWhite()
                            )

                            Spacer(modifier = Modifier.size(6.dp))

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(themedColor(0xFF123A67))
                                    .padding(horizontal = 7.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "PRO",
                                    fontSize = 7.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = themedColor(0xFF67E8F9)
                                )
                            }
                        }

                        Text(
                            text = if (listening) "Listening…" else "Live posture intelligence",
                            fontSize = 9.sp,
                            color = if (listening) themedColor(0xFFF87171) else themedColor(0xFF94A3B8)
                        )
                    }
                }

                Text(
                    text = "×",
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(onClick = onClose)
                        .padding(7.dp),
                    fontSize = 22.sp,
                    color = themedColor(0xFFCBD5E1)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Live context strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(themedColor(0xFF0E1B2D))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MiniAIStatus("SCORE", "$postureScore", themedColor(0xFF67E8F9))
                MiniAIStatus("UPPER", String.format(Locale.US, "%.1f°", upperAngle), themedColor(0xFF60A5FA))
                MiniAIStatus("LOWER", String.format(Locale.US, "%.1f°", lowerAngle), themedColor(0xFF4ADE80))
                MiniAIStatus("ALERTS", alertCount.toString(), themedColor(0xFFFBBF24))
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Conversation area
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 190.dp, max = 370.dp)
                    .verticalScroll(listState),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                messages.forEach { message ->
                    AIMessageBubble(
                        text = message.text,
                        fromUser = message.fromUser
                    )
                }

                if (isTyping) {
                    AITypingBubble()
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Suggested prompts
            Text(
                text = "SUGGESTED",
                fontSize = 8.sp,
                fontWeight = FontWeight.ExtraBold,
                color = themedColor(0xFF64748B)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AIAdvancedChip("My score") {
                    ask("What is my score?")
                }
                AIAdvancedChip("Why bad?") {
                    ask("Why is my posture bad?")
                }
                AIAdvancedChip("Improve") {
                    ask("How can I improve my posture?")
                }
            }

            Spacer(modifier = Modifier.height(9.dp))

            // Composer
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                    placeholder = {
                        Text(
                            text = "Message Posture AI…",
                            fontSize = 11.sp,
                            color = themedColor(0xFF7C8AA0)
                        )
                    },
                    singleLine = false,
                    maxLines = 4,
                    shape = RoundedCornerShape(18.dp),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Send
                    ),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            val q = input.trim()
                            if (q.isNotEmpty()) ask(q)
                        }
                    )
                )

                // Mic button
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(
                            if (listening) themedColor(0xFFDC2626)
                            else themedColor(0xFF123A67)
                        )
                        .clickable {
                            if (listening) {
                                stopPostureSpeechRecognizer()
                                listening = false
                            } else {
                                if (androidx.core.content.ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.RECORD_AUDIO
                                    ) == PackageManager.PERMISSION_GRANTED
                                ) {
                                    startPostureSpeechRecognizer(
                                        context = context,
                                        onResult = { heard ->
                                            if (Regex("\\b(p\\.?\\s*d\\.?\\s*f)\\b", RegexOption.IGNORE_CASE).containsMatchIn(heard)) {
                                                globalReportAction?.invoke()
                                                listening = false
                                            } else {
                                                input = heard
                                                messages.add(PostureChatMessage(heard, true))
                                                isTyping = true
                                                listening = false
                                            }
                                        },
                                        onListeningChanged = { listening = it }
                                    )
                                } else {
                                    requestAudioPermission.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (listening) "■" else "🎙",
                        fontSize = if (listening) 14.sp else 18.sp,
                        color = themedWhite()
                    )
                }

                // Send button
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(themedColor(0xFF16A34A))
                        .clickable {
                            val q = input.trim()
                            if (q.isNotEmpty()) ask(q)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "➤",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = themedWhite()
                    )
                }
            }

            Spacer(modifier = Modifier.height(7.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (connected) "● ESP32 live" else "○ ESP32 offline",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (connected) themedColor(0xFF4ADE80) else themedColor(0xFFF87171)
                )

                Spacer(modifier = Modifier.size(7.dp))

                Text(
                    text = "•",
                    fontSize = 8.sp,
                    color = themedColor(0xFF475569)
                )

                Spacer(modifier = Modifier.size(7.dp))

                Text(
                    text = "Voice + Text",
                    fontSize = 8.sp,
                    color = themedColor(0xFF94A3B8)
                )
            }
        }
    }
}

private fun calculateLivePostureScore(
    connected: Boolean,
    upperDeviation: Double,
    lowerDeviation: Double
): Int {
    if (!connected) return 0

    val deviation = max(
        abs(upperDeviation),
        abs(lowerDeviation)
    )

    return when {
        deviation <= 5.0 -> {
            // 95–100: excellent alignment
            (100.0 - deviation).toInt()
        }

        deviation <= 10.0 -> {
            // 90–95: still good, but moving toward the limit
            (95.0 - (deviation - 5.0)).toInt()
        }

        deviation <= 20.0 -> {
            // 50–89: warning zone
            (89.0 - (deviation - 10.0) * 4.0).toInt()
        }

        else -> {
            // 0–49: bad posture, progressively lower with larger deviation
            (49.0 - (deviation - 20.0) * 2.0)
                .coerceAtLeast(0.0)
                .toInt()
        }
    }.coerceIn(0, 100)
}

private fun postureZoneName(deviation: Double): String =
    when {
        deviation <= 10.0 -> "Good"
        deviation <= 20.0 -> "Warning"
        else -> "Bad"
    }

@Composable
private fun PostureAILogo(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(15.dp))
            .background(themedColor(0xFF102B4C)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f

            drawCircle(
                color = themedColor(0xFF22D3EE),
                radius = size.minDimension * 0.38f,
                center = Offset(cx, cy)
            )

            drawCircle(
                color = themedColor(0xFF08111E),
                radius = size.minDimension * 0.30f,
                center = Offset(cx, cy)
            )

            // Spine / neural path logo
            val path = Path().apply {
                moveTo(cx, cy - 11f)
                cubicTo(
                    cx - 6f, cy - 6f,
                    cx + 6f, cy - 1f,
                    cx, cy + 4f
                )
                cubicTo(
                    cx - 6f, cy + 9f,
                    cx + 5f, cy + 11f,
                    cx, cy + 16f
                )
            }

            drawPath(
                path = path,
                color = themedWhite(),
                style = Stroke(
                    width = 2.2f,
                    cap = StrokeCap.Round
                )
            )

            for (i in -2..2) {
                drawCircle(
                    color = if (i == 0) themedColor(0xFF4ADE80) else themedWhite(),
                    radius = 2.2f,
                    center = Offset(
                        cx + (i % 2) * 4f,
                        cy + i * 5f
                    )
                )
            }
        }
    }
}

@Composable
private fun MiniAIStatus(
    label: String,
    value: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            fontSize = 6.sp,
            fontWeight = FontWeight.Bold,
            color = themedColor(0xFF64748B)
        )
        Text(
            text = value,
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            color = color
        )
    }
}

@Composable
private fun AIMessageBubble(
    text: String,
    fromUser: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (fromUser)
            Arrangement.End
        else
            Arrangement.Start
    ) {
        Card(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (fromUser) 16.dp else 4.dp,
                bottomEnd = if (fromUser) 4.dp else 16.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (fromUser)
                    themedColor(0xFF2563EB)
                else
                    themedColor(0xFF142136)
            )
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.padding(10.dp)
            ) {
                if (!fromUser) {
                    PostureAILogo(modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.size(7.dp))
                }

                Text(
                    text = text,
                    fontSize = 10.sp,
                    lineHeight = 15.sp,
                    color = themedWhite()
                )
            }
        }
    }
}

@Composable
private fun AITypingBubble() {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = themedColor(0xFF142136))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PostureAILogo(modifier = Modifier.size(22.dp))
            for (i in 0..2) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(themedColor(0xFF67E8F9))
                )
            }
        }
    }
}

@Composable
private fun AIAdvancedChip(
    text: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(50.dp),
        colors = CardDefaults.cardColors(containerColor = themedColor(0xFF10243B))
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            color = themedColor(0xFF93C5FD)
        )
    }
}

// ============================================================
// HOME HELPERS
// ============================================================

// ============================================================

@Composable
private fun QuickMetric(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(themedWhite().copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 10.dp)
    ) {
        Text(
            text = title,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = themedWhite().copy(alpha = 0.68f)
        )

        Spacer(modifier = Modifier.height(3.dp))

        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold,
            color = themedWhite()
        )
    }
}

@Composable
private fun ModernSensorCard(
    title: String,
    angle: Double,
    deviation: Double,
    good: Boolean,
    modifier: Modifier = Modifier
) {
    val accent =
        if (good) themedColor(0xFF16A34A)
        else themedColor(0xFFDC2626)

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = themedWhite())
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = themedColor(0xFF64748B)
                )

                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(accent)
                )
            }

            Spacer(modifier = Modifier.height(9.dp))

            Text(
                text = String.format(Locale.US, "%.2f°", angle),
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = themedColor(0xFF0F172A)
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = String.format(Locale.US, "Deviation %.2f°", deviation),
                fontSize = 10.sp,
                color = themedColor(0xFF94A3B8)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (good) "● Within range" else "● Correction needed",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = accent
            )
        }
    }
}

// ============================================================
// ANALYTICS SCREEN — INTERACTIVE TREND MODULE
// ============================================================

@Composable
fun AnalyticsScreen(
    upperHistory: List<Double>,
    lowerHistory: List<Double>,
    postureScore: Int,
    connected: Boolean,
    modifier: Modifier = Modifier
) {
    var selectedWindow by remember { mutableIntStateOf(30) }

    val selectedUpper = when (selectedWindow) {
        30 -> upperHistory.takeLast(30)
        60 -> upperHistory.takeLast(60)
        300 -> upperHistory.takeLast(300)
        else -> upperHistory
    }

    val selectedLower = when (selectedWindow) {
        30 -> lowerHistory.takeLast(30)
        60 -> lowerHistory.takeLast(60)
        300 -> lowerHistory.takeLast(300)
        else -> lowerHistory
    }

    val windowLabel = when (selectedWindow) {
        30 -> "Last 30 readings"
        60 -> "Last 1 minute"
        300 -> "Last 5 minutes"
        else -> "Full session"
    }

    val avgUpper =
        if (selectedUpper.isNotEmpty()) selectedUpper.average() else 0.0

    val avgLower =
        if (selectedLower.isNotEmpty()) selectedLower.average() else 0.0

    val maxSwing = run {
        val combined = selectedUpper.map { abs(it) } +
                selectedLower.map { abs(it) }
        if (combined.isEmpty()) 0.0 else combined.maxOrNull() ?: 0.0
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(themedColor(0xFFF4F7FB))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "ANALYTICS",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = themedColor(0xFF0F172A)
                )
                Text(
                    text = "See how your posture moves over time",
                    fontSize = 12.sp,
                    color = themedColor(0xFF64748B)
                )
            }

            Text(
                text = if (connected) "● LIVE" else "● OFFLINE",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (connected)
                    themedColor(0xFF16A34A)
                else
                    themedColor(0xFFDC2626)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --------------------------------------------------------
        // SCORE + STABILITY
        // --------------------------------------------------------
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AnalyticsBigMetric(
                title = "POSTURE SCORE",
                value = "$postureScore",
                suffix = "/100",
                accent = themedColor(0xFF2563EB),
                modifier = Modifier.weight(1f)
            )

            AnalyticsBigMetric(
                title = "DATA POINTS",
                value = selectedUpper.size.toString(),
                suffix = "",
                accent = themedColor(0xFF7C3AED),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // --------------------------------------------------------
        // WINDOW SELECTOR
        // --------------------------------------------------------
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = themedWhite())
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "TIME WINDOW",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = themedColor(0xFF64748B)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    AnalyticsWindowChip(
                        label = "30",
                        selected = selectedWindow == 30,
                        onClick = { selectedWindow = 30 },
                        modifier = Modifier.weight(1f)
                    )

                    AnalyticsWindowChip(
                        label = "1 MIN",
                        selected = selectedWindow == 60,
                        onClick = { selectedWindow = 60 },
                        modifier = Modifier.weight(1f)
                    )

                    AnalyticsWindowChip(
                        label = "5 MIN",
                        selected = selectedWindow == 300,
                        onClick = { selectedWindow = 300 },
                        modifier = Modifier.weight(1f)
                    )

                    AnalyticsWindowChip(
                        label = "SESSION",
                        selected = selectedWindow == -1,
                        onClick = { selectedWindow = -1 },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // --------------------------------------------------------
        // TREND CHART
        // --------------------------------------------------------
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = themedColor(0xFF0F172A))
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "POSTURE STABILITY",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = themedWhite()
                        )
                        Text(
                            text = windowLabel,
                            fontSize = 11.sp,
                            color = themedColor(0xFF94A3B8)
                        )
                    }

                    Text(
                        text = "${selectedUpper.size} pts",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = themedColor(0xFF67E8F9)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(themedColor(0xFF111C30))
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp)
                    ) {
                        val width = size.width
                        val height = size.height

                        // Horizontal guides
                        for (i in 1..4) {
                            val y = height * i / 5f
                            drawLine(
                                color = themedColor(0xFF26334A),
                                start = Offset(0f, y),
                                end = Offset(width, y),
                                strokeWidth = 1.2f
                            )
                        }

                        if (selectedUpper.size >= 2) {
                            drawGraphLine(
                                values = selectedUpper,
                                width = width,
                                height = height,
                                minValue = -30.0,
                                maxValue = 30.0,
                                lineColor = themedColor(0xFF38BDF8)
                            )
                        }

                        if (selectedLower.size >= 2) {
                            drawGraphLine(
                                values = selectedLower,
                                width = width,
                                height = height,
                                minValue = -30.0,
                                maxValue = 30.0,
                                lineColor = themedColor(0xFF4ADE80)
                            )
                        }
                    }

                    if (selectedUpper.size < 2) {
                        Text(
                            text = "Collecting readings…",
                            modifier = Modifier.align(Alignment.Center),
                            color = themedColor(0xFF94A3B8),
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AnalyticsLegend(
                        label = "Upper spine",
                        dot = themedColor(0xFF38BDF8)
                    )
                    AnalyticsLegend(
                        label = "Lower spine",
                        dot = themedColor(0xFF4ADE80)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // --------------------------------------------------------
        // ANALYTICS INSIGHTS
        // --------------------------------------------------------
        Text(
            text = "WINDOW INSIGHTS",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = themedColor(0xFF64748B)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AnalyticsSmallCard(
                title = "Avg upper",
                value = String.format(Locale.US, "%.1f°", avgUpper),
                modifier = Modifier.weight(1f)
            )
            AnalyticsSmallCard(
                title = "Avg lower",
                value = String.format(Locale.US, "%.1f°", avgLower),
                modifier = Modifier.weight(1f)
            )
            AnalyticsSmallCard(
                title = "Max swing",
                value = String.format(Locale.US, "%.1f°", maxSwing),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun AnalyticsBigMetric(
    title: String,
    value: String,
    suffix: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = themedWhite())
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = themedColor(0xFF64748B)
            )
            Spacer(modifier = Modifier.height(5.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    fontSize = 27.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = accent
                )
                if (suffix.isNotEmpty()) {
                    Text(
                        text = suffix,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = themedColor(0xFF94A3B8),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AnalyticsWindowChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (selected)
                    themedColor(0xFF2563EB)
                else
                    themedColor(0xFFF1F5F9)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (selected) themedWhite() else themedColor(0xFF475569)
        )
    }
}

@Composable
private fun AnalyticsLegend(
    label: String,
    dot: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(dot)
        )
        Text(
            text = label,
            fontSize = 10.sp,
            color = themedColor(0xFFCBD5E1)
        )
    }
}

@Composable
private fun AnalyticsSmallCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = themedWhite())
    ) {
        Column(modifier = Modifier.padding(13.dp)) {
            Text(
                text = title,
                fontSize = 9.sp,
                color = themedColor(0xFF64748B)
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = themedColor(0xFF0F172A)
            )
        }
    }
}

// ============================================================
// HISTORY SCREEN — COLORFUL POSTURE TIMELINE
// ============================================================

private data class HeatReading(
    val upper: Double,
    val lower: Double,
    val upperDeviation: Double,
    val lowerDeviation: Double
)

@Composable
fun HistoryScreen(
    upperHistory: List<Double>,
    lowerHistory: List<Double>,
    upperDeviationHistory: List<Double>,
    lowerDeviationHistory: List<Double>,
    goodPercentage: Long,
    badPercentage: Long,
    alertCount: Int,
    minutes: Long,
    seconds: Long,
    recoveryCount: Int = 0,
    bestRecoverySeconds: Long = Long.MAX_VALUE,
    averageRecoverySeconds: Long = 0L,
    lastRecoverySeconds: Long = 0L,
    dailyGoodSeconds: Long = 0L,
    currentStreak: Int = 0,
    challengeCompletedToday: Boolean = false,
    onOpenAnatomy: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val readingCount = minOf(
        upperHistory.size,
        lowerHistory.size,
        upperDeviationHistory.size,
        lowerDeviationHistory.size
    )

    val recentPairs = buildList {
        val start = maxOf(0, readingCount - 96)
        for (i in start until readingCount) {
            add(
                HeatReading(
                    upper = upperHistory[i],
                    lower = lowerHistory[i],
                    upperDeviation = upperDeviationHistory[i],
                    lowerDeviation = lowerDeviationHistory[i]
                )
            )
        }
    }

    val goodCount = recentPairs.count { item ->
        max(item.upperDeviation, item.lowerDeviation) <= 10.0
    }

    val recentGoodPercent =
        if (recentPairs.isNotEmpty()) {
            goodCount * 100 / recentPairs.size
        } else {
            0
        }

    val postureColor = if (recentGoodPercent >= 80) {
        themedColor(0xFF16A34A)
    } else if (recentGoodPercent >= 55) {
        themedColor(0xFFF59E0B)
    } else {
        themedColor(0xFFDC2626)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(themedColor(0xFFF4F7FB))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 18.dp)
    ) {
        // --------------------------------------------------------
        // HEADER
        // --------------------------------------------------------
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "HISTORY",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = themedColor(0xFF0F172A)
                )
                Text(
                    text = "Your posture journey",
                    fontSize = 12.sp,
                    color = themedColor(0xFF64748B)
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(themedColor(0xFFEDE9FE))
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "LIVE SESSION",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = themedColor(0xFF7C3AED)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --------------------------------------------------------
        // SUMMARY CARD
        // --------------------------------------------------------
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = themedColor(0xFF172554))
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "TODAY'S POSTURE",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = themedWhite()
                )

                Text(
                    text = "Current session overview",
                    fontSize = 10.sp,
                    color = themedColor(0xFFBFDBFE)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HistoryHighlight(
                        title = "GOOD",
                        value = "$goodPercentage%",
                        accent = themedColor(0xFF4ADE80),
                        modifier = Modifier.weight(1f)
                    )

                    HistoryHighlight(
                        title = "BAD",
                        value = "$badPercentage%",
                        accent = themedColor(0xFFF87171),
                        modifier = Modifier.weight(1f)
                    )

                    HistoryHighlight(
                        title = "ALERTS",
                        value = alertCount.toString(),
                        accent = themedColor(0xFFFBBF24),
                        modifier = Modifier.weight(1f)
                    )

                    HistoryHighlight(
                        title = "TIME",
                        value = String.format(
                            Locale.US,
                            "%02d:%02d",
                            minutes,
                            seconds
                        ),
                        accent = themedColor(0xFF60A5FA),
                        modifier = Modifier.weight(1.25f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // --------------------------------------------------------
        // SESSION HEATMAP
        // --------------------------------------------------------
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = themedWhite())
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "POSTURE HEATMAP",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = themedColor(0xFF0F172A)
                        )
                        Text(
                            text = "Recent session readings grouped visually",
                            fontSize = 10.sp,
                            color = themedColor(0xFF64748B)
                        )
                    }

                    Text(
                        text = "${readingCount} readings",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = themedColor(0xFF2563EB)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (readingCount == 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Waiting for posture readings…",
                            fontSize = 12.sp,
                            color = themedColor(0xFF64748B)
                        )
                    }
                } else {
                    // Show up to 12 rows x 8 blocks.
                    val cells = recentPairs.takeLast(96)
                    val rows = (cells.size + 7) / 8

                    for (row in 0 until rows) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = String.format(
                                    Locale.US,
                                    "%02d",
                                    row + 1
                                ),
                                modifier = Modifier.size(24.dp),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = themedColor(0xFF94A3B8)
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val startIndex = row * 8
                                val endIndex = minOf(startIndex + 8, cells.size)

                                for (index in startIndex until endIndex) {
                                    val item = cells[index]
                                    val deviation =
                                        max(item.upperDeviation, item.lowerDeviation)

                                    val cellColor = when {
                                        deviation <= 10.0 -> themedColor(0xFF22C55E)
                                        deviation <= 20.0 -> themedColor(0xFFF59E0B)
                                        else -> themedColor(0xFFEF4444)
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clip(RoundedCornerShape(7.dp))
                                            .background(cellColor)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(9.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HeatmapLegend("Good", themedColor(0xFF22C55E))
                        HeatmapLegend("Watch", themedColor(0xFFF59E0B))
                        HeatmapLegend("Poor", themedColor(0xFFEF4444))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // --------------------------------------------------------
        // CURRENT TREND COACH
        // --------------------------------------------------------
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = themedWhite())
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val coachTilt by animateFloatAsState(
                    targetValue = if (recentGoodPercent >= 80) 0f else 7f,
                    animationSpec = tween(
                        durationMillis = 450,
                        easing = FastOutSlowInEasing
                    ),
                    label = "historyCoachTilt"
                )

                Canvas(
                    modifier = Modifier.size(76.dp)
                ) {
                    val cx = size.width / 2f

                    drawCircle(
                        color = postureColor.copy(alpha = 0.12f),
                        radius = 34f,
                        center = Offset(cx, 38f)
                    )

                    val lineColor = postureColor

                    drawCircle(
                        color = themedColor(0xFF0F172A),
                        radius = 10f,
                        center = Offset(cx, 18f)
                    )

                    val rad = Math.toRadians(coachTilt.toDouble())
                    val spineX = cx + (kotlin.math.sin(rad) * 11f).toFloat()

                    drawLine(
                        color = lineColor,
                        start = Offset(cx, 30f),
                        end = Offset(spineX, 55f),
                        strokeWidth = 7f,
                        cap = StrokeCap.Round
                    )

                    drawLine(
                        color = themedColor(0xFF475569),
                        start = Offset(cx - 17f, 37f),
                        end = Offset(cx + 17f, 37f),
                        strokeWidth = 5f,
                        cap = StrokeCap.Round
                    )

                    drawLine(
                        color = themedColor(0xFF64748B),
                        start = Offset(spineX, 55f),
                        end = Offset(spineX - 10f, 69f),
                        strokeWidth = 5f,
                        cap = StrokeCap.Round
                    )

                    drawLine(
                        color = themedColor(0xFF64748B),
                        start = Offset(spineX, 55f),
                        end = Offset(spineX + 10f, 69f),
                        strokeWidth = 5f,
                        cap = StrokeCap.Round
                    )
                }

                Spacer(modifier = Modifier.size(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when {
                            recentGoodPercent >= 80 -> "Great alignment"
                            recentGoodPercent >= 55 -> "Keep an eye on posture"
                            else -> "Posture needs attention"
                        },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = themedColor(0xFF0F172A)
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = "$recentGoodPercent% of recent readings are in the good range.",
                        fontSize = 11.sp,
                        color = themedColor(0xFF64748B)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // --------------------------------------------------------
        // RECENT READINGS
        // --------------------------------------------------------
        Text(
            text = "RECENT READINGS",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = themedColor(0xFF64748B)
        )

        Spacer(modifier = Modifier.height(8.dp))

        val recentCount = minOf(8, readingCount)

        for (index in 0 until recentCount) {
            val sourceIndex = readingCount - recentCount + index

            val upper =
                upperHistory.getOrNull(sourceIndex) ?: 0.0

            val lower =
                lowerHistory.getOrNull(sourceIndex) ?: 0.0

            val isGood =
                abs(upper) <= 10.0 &&
                        abs(lower) <= 10.0

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
                shape = RoundedCornerShape(17.dp),
                colors = CardDefaults.cardColors(containerColor = themedWhite())
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isGood)
                                        themedColor(0xFF22C55E)
                                    else
                                        themedColor(0xFFEF4444)
                                )
                        )

                        Text(
                            text = "Reading ${index + 1}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = themedColor(0xFF64748B)
                        )
                    }

                    Text(
                        text = String.format(
                            Locale.US,
                            "U %.1f°   L %.1f°",
                            upper,
                            lower
                        ),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = themedColor(0xFF0F172A)
                    )
                }
            }
        }


        Spacer(modifier = Modifier.height(14.dp))

        StreakHistoryCard(
            dailyGoodSeconds = dailyGoodSeconds,
            currentStreak = currentStreak,
            completedToday = challengeCompletedToday
        )

        Spacer(modifier = Modifier.height(10.dp))
    }
    Spacer(modifier = Modifier.height(14.dp))

    SpineAnatomyCard(
        upperDifference = if (upperDeviationHistory.isNotEmpty())
            upperDeviationHistory.last() else 0.0,
        lowerDifference = if (lowerDeviationHistory.isNotEmpty())
            lowerDeviationHistory.last() else 0.0,
        onOpen = onOpenAnatomy
    )


}

@Composable
private fun HistoryHighlight(
    title: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(themedWhite().copy(alpha = 0.10f))
            .padding(horizontal = 8.dp, vertical = 10.dp)
    ) {
        Text(
            text = title,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            color = themedWhite().copy(alpha = 0.65f)
        )

        Spacer(modifier = Modifier.height(3.dp))

        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold,
            color = accent
        )
    }
}

@Composable
private fun HeatmapLegend(
    label: String,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )

        Text(
            text = label,
            fontSize = 9.sp,
            color = themedColor(0xFF64748B)
        )
    }
}

// ============================================================

@Composable
private fun HistoryRecoveryStat(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(themedColor(0xFFF8FAFC))
            .padding(9.dp)
    ) {
        Text(
            text = title,
            fontSize = 7.sp,
            fontWeight = FontWeight.Bold,
            color = themedColor(0xFF94A3B8)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            color = themedColor(0xFF2563EB)
        )
    }
}


@Composable
private fun StreakHistoryCard(
    dailyGoodSeconds: Long,
    currentStreak: Int,
    completedToday: Boolean
) {
    val progress = streakProgress(dailyGoodSeconds)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = themedColor(0xFF141B2D))
    ) {
        Column(modifier = Modifier.padding(17.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "DAILY CHALLENGE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = themedColor(0xFF94A3B8)
                    )
                    Text(
                        text = "Good posture • 10 minutes",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = themedWhite()
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "🔥 $currentStreak",
                        fontSize = 21.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = themedColor(0xFFFFA23A)
                    )
                    Text(
                        text = "day streak",
                        fontSize = 8.sp,
                        color = themedColor(0xFF94A3B8)
                    )
                }
            }

            Spacer(modifier = Modifier.height(11.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (completedToday)
                        "COMPLETE 🎉"
                    else
                        "${formatChallengeTime(dailyGoodSeconds)} / 10m",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (completedToday) themedColor(0xFF4ADE80) else themedWhite()
                )

                Text(
                    text = "${(progress * 100).toInt()}%",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = themedColor(0xFFFFA23A)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(50.dp))
                    .background(themedColor(0xFF293247))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .height(10.dp)
                        .clip(RoundedCornerShape(50.dp))
                        .background(themedColor(0xFFFF8A00))
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                for (day in listOf("M","T","W","T","F","S","S")) {
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(
                                    if (completedToday && currentStreak > 0)
                                        themedColor(0xFFFF8A00)
                                    else
                                        themedColor(0xFF293247)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (completedToday && currentStreak > 0) "✓" else day,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = themedWhite()
                            )
                        }

                        Spacer(modifier = Modifier.height(3.dp))

                        Text(
                            text = day,
                            fontSize = 7.sp,
                            color = themedColor(0xFF64748B)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(9.dp))

            Text(
                text = "Complete today's goal to protect your streak. A sound plays on completion.",
                fontSize = 9.sp,
                color = themedColor(0xFF8491A5)
            )
        }
    }
}


// ============================================================
// SPINE & POSTURE ANATOMY
// ============================================================

private enum class AnatomyRegion(
    val title: String,
    val range: String,
    val subtitle: String
) {
    Cervical("CERVICAL SPINE", "C1–C7", "Neck / upper spine"),
    Thoracic("THORACIC SPINE", "T1–T12", "Upper and middle back"),
    Lumbar("LUMBAR SPINE", "L1–L5", "Lower back"),
    Sacrum("SACRUM", "S1–S5", "Base of the spine")
}

@Composable
private fun SpineAnatomyCard(
    upperDifference: Double,
    lowerDifference: Double,
    onOpen: () -> Unit
) {
    val maxDeviation = max(abs(upperDifference), abs(lowerDifference))
    val accent = when {
        maxDeviation <= 10.0 -> themedColor(0xFF22C55E)
        maxDeviation <= 20.0 -> themedColor(0xFFF59E0B)
        else -> themedColor(0xFFEF4444)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(themedColor(0xFF07111F))
    ) {
        Column(Modifier.padding(17.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(themedColor(0xFF132942)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🦴", fontSize = 20.sp)
                }

                Spacer(Modifier.size(10.dp))

                Column(Modifier.weight(1f)) {
                    Text(
                        "SPINE & POSTURE ANATOMY",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = themedColor(0xFF8FA1B5)
                    )
                    Text(
                        "Explore the regions your belt measures",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = themedWhite()
                    )
                }

                Text("›", fontSize = 28.sp, color = themedColor(0xFF64748B))
            }

            Spacer(Modifier.height(12.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                AnatomyChip("C1–C7", themedColor(0xFF38BDF8), Modifier.weight(1f))
                AnatomyChip("T1–T12", themedColor(0xFF60A5FA), Modifier.weight(1f))
                AnatomyChip("L1–L5", themedColor(0xFF4ADE80), Modifier.weight(1f))
                AnatomyChip("S1–S5", themedColor(0xFFA78BFA), Modifier.weight(1f))
            }

            Spacer(Modifier.height(11.dp))

            Text(
                String.format(
                    Locale.US,
                    "Highest live deviation %.1f°",
                    maxDeviation
                ),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = accent
            )
        }
    }
}

@Composable
private fun AnatomyChip(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(themedColor(0xFF0E1B2B))
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("●", fontSize = 11.sp, color = color)
        Text(
            text,
            fontSize = 7.sp,
            fontWeight = FontWeight.Bold,
            color = themedColor(0xFFD5DEE8)
        )
    }
}

@Composable
private fun SpineAnatomyOverlay(
    upperDifference: Double,
    lowerDifference: Double,
    onClose: () -> Unit
) {
    val maxDeviation = max(abs(upperDifference), abs(lowerDifference))
    val highlighted =
        if (lowerDifference >= upperDifference)
            AnatomyRegion.Lumbar
        else
            AnatomyRegion.Thoracic

    val accent =
        if (highlighted == AnatomyRegion.Lumbar)
            themedColor(0xFF4ADE80)
        else
            themedColor(0xFF60A5FA)

    Box(
        Modifier
            .fillMaxSize()
            .background(themedColor(0xF6020712))
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(15.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "SPINE ANATOMY",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = themedWhite()
                    )
                    Text(
                        "Your live posture anatomy guide",
                        fontSize = 11.sp,
                        color = themedColor(0xFF94A3B8)
                    )
                }

                Text(
                    "×",
                    modifier = Modifier
                        .clickable(onClick = onClose)
                        .padding(7.dp),
                    fontSize = 27.sp,
                    color = themedWhite()
                )
            }

            Spacer(Modifier.height(12.dp))

            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(themedColor(0xFF0B1727))
            ) {
                Column(Modifier.padding(13.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "LIVE SPINE MAP",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = themedColor(0xFF71839A)
                            )
                            Text(
                                highlighted.title,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = accent
                            )
                        }

                        Text(
                            String.format(Locale.US, "%.1f°", maxDeviation),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = themedWhite()
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    AnatomySpineDiagram(
                        upperDifference = upperDifference,
                        lowerDifference = lowerDifference,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(320.dp)
                            .clip(RoundedCornerShape(22.dp))
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Text(
                "SPINE REGIONS",
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = themedColor(0xFF94A3B8)
            )

            Spacer(Modifier.height(7.dp))

            AnatomyDetail(
                region = AnatomyRegion.Cervical,
                description = "Supports the head and contributes to neck movement.",
                role = "Keeps the head supported and helps the neck move, rotate and position the head.",
                causes = listOf(
                    "Prolonged forward-head position",
                    "Looking down at a phone for long periods",
                    "Screen positioned too low",
                    "Slouched sitting that shifts the head forward",
                    "Staying in one position for too long"
                ),
                actions = listOf(
                    "Raise the screen toward eye level",
                    "Bring the head back over the shoulders",
                    "Take short movement breaks",
                    "Avoid holding one fixed position for long periods"
                )
            )
            AnatomyDetail(
                region = AnatomyRegion.Thoracic,
                description = "Supports the upper and middle back and works with the rib cage.",
                role = "Provides stability through the upper and middle back and helps connect the trunk with the rib cage.",
                causes = listOf(
                    "Rounded shoulders",
                    "Prolonged slouched sitting",
                    "Laptop or monitor positioned too low",
                    "Long periods of static desk work",
                    "Repeated forward-leaning positions"
                ),
                actions = listOf(
                    "Keep the screen near a comfortable viewing height",
                    "Let the shoulders relax instead of rounding forward",
                    "Change position regularly",
                    "Use a comfortable supported sitting posture"
                )
            )
            AnatomyDetail(
                region = AnatomyRegion.Lumbar,
                description = "Supports the lower back and carries substantial body load.",
                role = "Helps support the trunk and contributes to controlled bending, extension and everyday movement.",
                causes = listOf(
                    "Prolonged sitting",
                    "Poor workstation setup",
                    "Repeated forward bending",
                    "Poor back or seat support",
                    "Prolonged static posture",
                    "Repeated leaning to one side"
                ),
                actions = listOf(
                    "Keep both feet comfortably supported",
                    "Use suitable back support",
                    "Avoid staying in one position for too long",
                    "Change position and move regularly",
                    "Use controlled posture when bending"
                )
            )
            AnatomyDetail(
                region = AnatomyRegion.Sacrum,
                description = "Connects the spine to the pelvis at its base.",
                role = "Forms the base of the spinal column and provides an important connection between the spine and pelvis.",
                causes = listOf(
                    "Long periods of sitting",
                    "Poor pelvic positioning",
                    "Uneven sitting posture",
                    "Prolonged static posture",
                    "Repeated asymmetrical positions"
                ),
                actions = listOf(
                    "Keep your sitting position balanced",
                    "Keep both feet supported when practical",
                    "Change position regularly",
                    "Avoid prolonged twisting or leaning"
                )
            )

            Spacer(Modifier.height(10.dp))

            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    accent.copy(alpha = 0.09f)
                )
            ) {
                Column(Modifier.padding(15.dp)) {
                    Text(
                        "WHY IS THIS REGION HIGHLIGHTED?",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = accent
                    )

                    Spacer(Modifier.height(5.dp))

                    Text(
                        String.format(
                            Locale.US,
                            "The app highlights the region with the larger live deviation. Current maximum deviation: %.1f°.",
                            maxDeviation
                        ),
                        fontSize = 10.sp,
                        color = themedColor(0xFFCBD5E1)
                    )
                }
            }

            Spacer(Modifier.height(15.dp))
        }
    }
}

@Composable
private fun AnatomySpineDiagram(
    upperDifference: Double,
    lowerDifference: Double,
    modifier: Modifier = Modifier
) {
    val upperColor = zoneForDeviation(upperDifference)
    val lowerColor = zoneForDeviation(lowerDifference)

    Box(modifier.background(themedColor(0xFF091726))) {
        Canvas(Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val s = min(size.width / 270f, size.height / 320f)

            val points = listOf(
                Offset(cx, 65f * s),
                Offset(cx, 105f * s),
                Offset(cx, 145f * s),
                Offset(cx, 185f * s),
                Offset(cx, 225f * s),
                Offset(cx, 270f * s)
            )

            drawCircle(
                themedColor(0xFF17304A),
                30f * s,
                Offset(cx, 36f * s)
            )

            drawRoundRect(
                themedColor(0xFF12283D),
                Offset(cx - 62f * s, 78f * s),
                androidx.compose.ui.geometry.Size(
                    124f * s,
                    185f * s
                ),
                androidx.compose.ui.geometry.CornerRadius(
                    56f * s,
                    56f * s
                )
            )

            drawLine(
                themedColor(0xFF34475A),
                Offset(cx, 64f * s),
                Offset(cx, 292f * s),
                1.5f * s
            )

            for (i in 0 until points.lastIndex) {
                val c = if (i <= 3) upperColor else lowerColor
                drawLine(
                    c,
                    points[i],
                    points[i + 1],
                    7f * s,
                    cap = StrokeCap.Round
                )
            }

            points.forEachIndexed { index, pt ->
                val c = if (index <= 3) upperColor else lowerColor
                drawCircle(c.copy(alpha = 0.16f), 13f * s, pt)
                drawCircle(c, 5f * s, pt)
            }
        }

        Column(
            Modifier
                .align(Alignment.CenterStart)
                .padding(start = 10.dp)
        ) {
            Text(
                "THORACIC",
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = upperColor
            )
            Text(
                "T1–T12",
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                color = themedWhite()
            )

            Spacer(Modifier.height(125.dp))

            Text(
                "LUMBAR",
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = lowerColor
            )
            Text(
                "L1–L5",
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                color = themedWhite()
            )
        }
    }
}

@Composable
private fun AnatomyDetail(
    region: AnatomyRegion,
    description: String,
    role: String,
    causes: List<String>,
    actions: List<String>
) {
    val accent = when (region) {
        AnatomyRegion.Cervical -> themedColor(0xFF38BDF8)
        AnatomyRegion.Thoracic -> themedColor(0xFF60A5FA)
        AnatomyRegion.Lumbar -> themedColor(0xFF4ADE80)
        AnatomyRegion.Sacrum -> themedColor(0xFFA78BFA)
    }

    val tag = when (region) {
        AnatomyRegion.Cervical -> "NECK"
        AnatomyRegion.Thoracic -> "UPPER BACK"
        AnatomyRegion.Lumbar -> "LOWER BACK"
        AnatomyRegion.Sacrum -> "PELVIS LINK"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(23.dp),
        colors = CardDefaults.cardColors(themedColor(0xFF0F1B2A))
    ) {
        Column(Modifier.padding(15.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(accent.copy(alpha = 0.10f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "●",
                        fontSize = 14.sp,
                        color = accent
                    )
                }

                Spacer(Modifier.size(10.dp))

                Column(Modifier.weight(1f)) {
                    Text(
                        text = region.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = themedWhite()
                    )

                    Text(
                        text = "${region.range} • ${region.subtitle}",
                        fontSize = 9.sp,
                        color = themedColor(0xFF94A3B8)
                    )
                }

                Text(
                    text = tag,
                    fontSize = 7.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = accent
                )
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = description,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = themedColor(0xFFDCE5EF)
            )

            Spacer(Modifier.height(10.dp))

            AnatomyInfoSection(
                title = "WHAT DOES IT DO?",
                icon = "●",
                accent = accent,
                body = role
            )

            Spacer(Modifier.height(9.dp))

            Text(
                text = "COMMON POSTURE FACTORS",
                fontSize = 8.sp,
                fontWeight = FontWeight.ExtraBold,
                color = accent
            )

            Spacer(Modifier.height(6.dp))

            causes.forEachIndexed { index, cause ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = "${index + 1}",
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(accent.copy(alpha = 0.12f))
                            .padding(top = 1.dp),
                        textAlign = TextAlign.Center,
                        fontSize = 7.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = accent
                    )

                    Spacer(Modifier.size(7.dp))

                    Text(
                        text = cause,
                        fontSize = 9.sp,
                        color = themedColor(0xFFB7C7D7)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = "WHAT CAN I DO?",
                fontSize = 8.sp,
                fontWeight = FontWeight.ExtraBold,
                color = themedColor(0xFF4ADE80)
            )

            Spacer(Modifier.height(6.dp))

            actions.forEach { action ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = "✓",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = themedColor(0xFF4ADE80)
                    )

                    Spacer(Modifier.size(7.dp))

                    Text(
                        text = action,
                        fontSize = 9.sp,
                        color = themedColor(0xFFB7C7D7)
                    )
                }
            }

            Spacer(Modifier.height(9.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(13.dp))
                    .background(themedColor(0xFF0B1624))
                    .padding(9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ℹ",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = themedColor(0xFF94A3B8)
                )

                Spacer(Modifier.size(7.dp))

                Text(
                    text = "These are general posture factors, not a medical diagnosis.",
                    fontSize = 8.sp,
                    color = themedColor(0xFF8493A5)
                )
            }
        }
    }

    Spacer(Modifier.height(8.dp))
}

@Composable
private fun AnatomyInfoSection(
    title: String,
    icon: String,
    accent: Color,
    body: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = icon,
            fontSize = 10.sp,
            color = accent
        )

        Spacer(Modifier.size(6.dp))

        Column {
            Text(
                text = title,
                fontSize = 8.sp,
                fontWeight = FontWeight.ExtraBold,
                color = accent
            )

            Spacer(Modifier.height(3.dp))

            Text(
                text = body,
                fontSize = 9.sp,
                color = themedColor(0xFFAFC0D1)
            )
        }
    }
}

// ============================================================
// POSTURE SCREEN — REALISTIC ANATOMICAL-STYLE VIEW
// ============================================================

@Composable
fun PostureScreen(
    upperAngle: Double,
    lowerAngle: Double,
    goodPosture: Boolean,
    connected: Boolean,
    upperDifference: Double,
    lowerDifference: Double,
    modifier: Modifier = Modifier
) {
    var viewMode by remember { mutableStateOf("3D") }

    val upperZone = postureZone(upperDifference, connected)
    val lowerZone = postureZone(lowerDifference, connected)

    val overallZone = when {
        !connected -> PostureZone.Offline
        upperZone == PostureZone.Bad || lowerZone == PostureZone.Bad -> PostureZone.Bad
        upperZone == PostureZone.Warning || lowerZone == PostureZone.Warning -> PostureZone.Warning
        else -> PostureZone.Good
    }

    val overallColor = postureZoneColor(overallZone)

    val alignment = when (overallZone) {
        PostureZone.Good ->
            (100.0 - max(upperDifference, lowerDifference) * 1.4)
                .coerceIn(85.0, 100.0)
                .toInt()

        PostureZone.Warning ->
            (80.0 - max(upperDifference, lowerDifference))
                .coerceIn(50.0, 84.0)
                .toInt()

        PostureZone.Bad ->
            (50.0 - max(upperDifference, lowerDifference))
                .coerceIn(10.0, 49.0)
                .toInt()

        PostureZone.Offline -> 0
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(themedColor(0xFFF3F6FA))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 14.dp)
    ) {
        // HEADER
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Posture View",
                    fontSize = 27.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = themedColor(0xFF111827)
                )
                Text(
                    text = "Live anatomical spine analysis",
                    fontSize = 11.sp,
                    color = themedColor(0xFF64748B)
                )
            }

            Text(
                text = if (connected) "● LIVE" else "○ OFFLINE",
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                color = overallColor
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 3D / 2D CONTROL
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(19.dp),
            colors = CardDefaults.cardColors(containerColor = themedWhite())
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(7.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                AnatomyToggle(
                    text = "3D",
                    selected = viewMode == "3D",
                    onClick = { viewMode = "3D" },
                    modifier = Modifier.weight(1f)
                )
                AnatomyToggle(
                    text = "2D",
                    selected = viewMode == "2D",
                    onClick = { viewMode = "2D" },
                    modifier = Modifier.weight(1f)
                )
                AnatomyToggle(
                    text = "BACK",
                    selected = true,
                    onClick = {},
                    enabled = false,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // MAIN ANATOMICAL CARD
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(30.dp),
            colors = CardDefaults.cardColors(containerColor = themedColor(0xFF010914))
        ) {
            Column(modifier = Modifier.padding(11.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "POSTURE VISUALIZATION",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = themedColor(0xFF7E8CA0)
                        )
                        Text(
                            text = when (overallZone) {
                                PostureZone.Good -> "GOOD POSTURE"
                                PostureZone.Warning -> "NORMAL • WATCH"
                                PostureZone.Bad -> "CORRECTION NEEDED"
                                PostureZone.Offline -> "SENSOR OFFLINE"
                            },
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = overallColor
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "$alignment%",
                            fontSize = 25.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = themedWhite()
                        )
                        Text(
                            text = "alignment",
                            fontSize = 8.sp,
                            color = themedColor(0xFF7E8CA0)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                AnatomicalHumanView(
                    upperAngle = upperAngle,
                    lowerAngle = lowerAngle,
                    upperZone = upperZone,
                    lowerZone = lowerZone,
                    viewMode = viewMode,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(505.dp)
                        .clip(RoundedCornerShape(23.dp))
                )

                Spacer(modifier = Modifier.height(9.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    AnatomyLegend("GOOD ≤10°", themedColor(0xFF22C55E))
                    Spacer(modifier = Modifier.size(9.dp))
                    AnatomyLegend("WARNING 10–20°", themedColor(0xFFF59E0B))
                    Spacer(modifier = Modifier.size(9.dp))
                    AnatomyLegend("BAD >20°", themedColor(0xFFEF4444))
                }
            }
        }

        Spacer(modifier = Modifier.height(11.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            AnatomyValueCard(
                title = "Upper Spine",
                angle = upperAngle,
                deviation = upperDifference,
                zone = upperZone,
                accent = themedColor(0xFF38BDF8),
                modifier = Modifier.weight(1f)
            )

            AnatomyValueCard(
                title = "Lower Spine",
                angle = lowerAngle,
                deviation = lowerDifference,
                zone = lowerZone,
                accent = themedColor(0xFF4ADE80),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(11.dp))

        Text(
            text = "BODY REGIONS",
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            color = themedColor(0xFF64748B)
        )

        Spacer(modifier = Modifier.height(7.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            RegionCard("Head", if (goodPosture) PostureZone.Good else PostureZone.Warning, Modifier.weight(1f))
            RegionCard("Shoulders", upperZone, Modifier.weight(1f))
            RegionCard("Lower Back", lowerZone, Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(11.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = overallColor.copy(alpha = 0.08f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(15.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(overallColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when (overallZone) {
                            PostureZone.Good -> "✓"
                            PostureZone.Warning -> "!"
                            PostureZone.Bad -> "×"
                            PostureZone.Offline -> "?"
                        },
                        fontSize = 21.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = overallColor
                    )
                }

                Spacer(modifier = Modifier.size(11.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when (overallZone) {
                            PostureZone.Good -> "Spine alignment looks good"
                            PostureZone.Warning -> "Moderate deviation detected"
                            PostureZone.Bad -> "Correction needed"
                            PostureZone.Offline -> "Waiting for live sensors"
                        },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = themedColor(0xFF0F172A)
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = String.format(
                            Locale.US,
                            "Upper %.1f° • Lower %.1f°",
                            upperDifference,
                            lowerDifference
                        ),
                        fontSize = 10.sp,
                        color = themedColor(0xFF64748B)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
    }
}

private enum class PostureZone {
    Good,
    Warning,
    Bad,
    Offline
}

private fun postureZone(deviation: Double, connected: Boolean): PostureZone {
    if (!connected) return PostureZone.Offline

    return when {
        deviation <= 10.0 -> PostureZone.Good
        deviation <= 20.0 -> PostureZone.Warning
        else -> PostureZone.Bad
    }
}

private fun postureZoneColor(zone: PostureZone): Color =
    when (zone) {
        PostureZone.Good -> themedColor(0xFF22C55E)
        PostureZone.Warning -> themedColor(0xFFF59E0B)
        PostureZone.Bad -> themedColor(0xFFEF4444)
        PostureZone.Offline -> themedColor(0xFF94A3B8)
    }

private fun postureZoneLabel(zone: PostureZone): String =
    when (zone) {
        PostureZone.Good -> "Good"
        PostureZone.Warning -> "Warning"
        PostureZone.Bad -> "Bad"
        PostureZone.Offline -> "Offline"
    }

@Composable
private fun AnatomyToggle(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(13.dp))
            .background(
                if (selected) themedColor(0xFF2563EB)
                else themedColor(0xFFF1F5F9)
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 9.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (selected) themedWhite() else themedColor(0xFF475569)
        )
    }
}

@Composable
private fun AnatomyLegend(
    text: String,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(color)
        )

        Text(
            text = text,
            fontSize = 7.sp,
            color = themedColor(0xFFB7C3D0)
        )
    }
}

@Composable
private fun AnatomyValueCard(
    title: String,
    angle: Double,
    deviation: Double,
    zone: PostureZone,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val c = postureZoneColor(zone)

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = themedColor(0xFF0F172A))
    ) {
        Column(modifier = Modifier.padding(13.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = themedColor(0xFF94A3B8)
                )

                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(c)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = String.format(Locale.US, "%.1f°", angle),
                fontSize = 21.sp,
                fontWeight = FontWeight.ExtraBold,
                color = themedWhite()
            )

            Text(
                text = String.format(Locale.US, "Deviation %.1f°", deviation),
                fontSize = 8.sp,
                color = themedColor(0xFF94A3B8)
            )

            Spacer(modifier = Modifier.height(5.dp))

            Text(
                text = postureZoneLabel(zone),
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = if (zone == PostureZone.Offline) accent else c
            )
        }
    }
}

@Composable
private fun RegionCard(
    title: String,
    state: PostureZone,
    modifier: Modifier = Modifier
) {
    val c = postureZoneColor(state)

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = themedWhite())
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(37.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(c.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (state == PostureZone.Bad) "!" else "●",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = c
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = title,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = themedColor(0xFF334155)
            )

            Text(
                text = postureZoneLabel(state),
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = c
            )
        }
    }
}

@Composable
private fun AnatomicalHumanView(
    upperAngle: Double,
    lowerAngle: Double,
    upperZone: PostureZone,
    lowerZone: PostureZone,
    viewMode: String,
    modifier: Modifier = Modifier
) {
    val upperTilt by animateFloatAsState(
        targetValue = upperAngle.coerceIn(-28.0, 28.0).toFloat(),
        animationSpec = tween(
            durationMillis = 320,
            easing = FastOutSlowInEasing
        ),
        label = "anatomicalUpperTilt"
    )

    val lowerTilt by animateFloatAsState(
        targetValue = lowerAngle.coerceIn(-28.0, 28.0).toFloat(),
        animationSpec = tween(
            durationMillis = 320,
            easing = FastOutSlowInEasing
        ),
        label = "anatomicalLowerTilt"
    )

    val upperColor = postureZoneColor(upperZone)
    val lowerColor = postureZoneColor(lowerZone)

    Box(
        modifier = modifier.background(themedColor(0xFF08111C))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val scale = min(width / 360f, height / 505f)
            val cx = width / 2f

            fun pos(dx: Float, y: Float): Offset =
                Offset(cx + dx * scale, y * scale)

            val ur = Math.toRadians(upperTilt.toDouble())
            val lr = Math.toRadians(lowerTilt.toDouble())

            val neckDx = (kotlin.math.sin(ur) * 8f).toFloat()
            val upperDx = (kotlin.math.sin(ur) * 30f).toFloat()
            val midDx = (
                    kotlin.math.sin(ur) * 17f +
                            kotlin.math.sin(lr) * 6f
                    ).toFloat()
            val lowerDx = (kotlin.math.sin(lr) * 34f).toFloat()
            val hipDx = (kotlin.math.sin(lr) * 44f).toFloat()

            val head = pos(neckDx, 54f)
            val neck = pos(neckDx, 88f)
            val shoulders = pos(neckDx, 109f)
            val upper = pos(upperDx, 165f)
            val middle = pos(midDx, 231f)
            val lower = pos(lowerDx, 303f)
            val hip = pos(hipDx, 350f)

            // Background radial light
            drawCircle(
                color = themedColor(0xFF12283E),
                radius = 150f * scale,
                center = Offset(cx, 235f * scale)
            )

            // Neutral center line
            drawLine(
                color = themedColor(0xFF26384B),
                start = pos(0f, 18f),
                end = pos(0f, 469f),
                strokeWidth = 1.2f * scale
            )

            // Shoulder/back silhouette
            val torso = Path().apply {
                moveTo(cx - 75f * scale, 112f * scale)
                cubicTo(
                    cx - 103f * scale,
                    151f * scale,
                    cx - 104f * scale,
                    270f * scale,
                    cx - 73f * scale,
                    329f * scale
                )
                cubicTo(
                    cx - 48f * scale,
                    370f * scale,
                    cx - 35f * scale,
                    421f * scale,
                    cx - 25f * scale,
                    480f * scale
                )
                lineTo(cx + 25f * scale, 480f * scale)
                cubicTo(
                    cx + 35f * scale,
                    421f * scale,
                    cx + 48f * scale,
                    370f * scale,
                    cx + 73f * scale,
                    329f * scale
                )
                cubicTo(
                    cx + 104f * scale,
                    270f * scale,
                    cx + 103f * scale,
                    151f * scale,
                    cx + 75f * scale,
                    112f * scale
                )
                close()
            }

            drawPath(
                path = torso,
                color = themedColor(0xFF13253A)
            )

            // 3D chest/depth panel
            if (viewMode == "3D") {
                drawOval(
                    color = themedColor(0xFF1B3048),
                    topLeft = Offset(
                        cx - 69f * scale,
                        126f * scale
                    ),
                    size = androidx.compose.ui.geometry.Size(
                        138f * scale,
                        215f * scale
                    )
                )

                // Scapulae
                drawOval(
                    color = themedColor(0xFF29435D),
                    topLeft = Offset(
                        shoulders.x - 50f * scale,
                        shoulders.y + 15f * scale
                    ),
                    size = androidx.compose.ui.geometry.Size(
                        37f * scale,
                        100f * scale
                    )
                )

                drawOval(
                    color = themedColor(0xFF29435D),
                    topLeft = Offset(
                        shoulders.x + 13f * scale,
                        shoulders.y + 15f * scale
                    ),
                    size = androidx.compose.ui.geometry.Size(
                        37f * scale,
                        100f * scale
                    )
                )
            }

            // Head
            drawCircle(
                color = themedColor(0xFF213850),
                radius = 31f * scale,
                center = head
            )

            drawCircle(
                color = themedColor(0xFF385875),
                radius = 9f * scale,
                center = Offset(
                    head.x - 7f * scale,
                    head.y - 8f * scale
                )
            )

            // Neck
            drawLine(
                color = themedColor(0xFF213850),
                start = Offset(
                    head.x,
                    head.y + 25f * scale
                ),
                end = neck,
                strokeWidth = 16f * scale,
                cap = StrokeCap.Round
            )

            // Shoulders
            drawLine(
                color = themedColor(0xFF1D344D),
                start = Offset(
                    shoulders.x - 63f * scale,
                    shoulders.y
                ),
                end = Offset(
                    shoulders.x + 63f * scale,
                    shoulders.y
                ),
                strokeWidth = 27f * scale,
                cap = StrokeCap.Round
            )

            // Arms
            drawLine(
                color = themedColor(0xFF1A3148),
                start = Offset(
                    shoulders.x - 54f * scale,
                    shoulders.y + 3f * scale
                ),
                end = pos(-55f, 252f),
                strokeWidth = 20f * scale,
                cap = StrokeCap.Round
            )

            drawLine(
                color = themedColor(0xFF1A3148),
                start = Offset(
                    shoulders.x + 54f * scale,
                    shoulders.y + 3f * scale
                ),
                end = pos(55f, 252f),
                strokeWidth = 20f * scale,
                cap = StrokeCap.Round
            )

            // Rib cage contours
            if (viewMode == "3D") {
                for (i in 0..5) {
                    val y = (141f + i * 22f) * scale
                    val half = (42f - i * 3f) * scale

                    drawArc(
                        color = themedColor(0xFF38536C),
                        startAngle = 200f,
                        sweepAngle = 140f,
                        useCenter = false,
                        topLeft = Offset(
                            cx - half,
                            y
                        ),
                        size = androidx.compose.ui.geometry.Size(
                            half * 2f,
                            32f * scale
                        ),
                        style = Stroke(
                            width = 1.5f * scale
                        )
                    )
                }
            }

            // Spine shadow
            val spine = Path().apply {
                moveTo(neck.x, neck.y)
                lineTo(
                    cx + (kotlin.math.sin(ur) * 19f).toFloat() * scale,
                    126f * scale
                )
                lineTo(upper.x, upper.y)
                lineTo(middle.x, middle.y)
                lineTo(lower.x, lower.y)
                lineTo(hip.x, hip.y)
            }

            drawPath(
                path = spine,
                color = themedColor(0xFF03080F),
                style = Stroke(
                    width = 24f * scale,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // Segmented live spine
            drawLine(
                color = upperColor,
                start = Offset(neck.x, neck.y),
                end = upper,
                strokeWidth = 9f * scale,
                cap = StrokeCap.Round
            )

            drawLine(
                color = upperColor,
                start = upper,
                end = middle,
                strokeWidth = 9f * scale,
                cap = StrokeCap.Round
            )

            drawLine(
                color = lowerColor,
                start = middle,
                end = lower,
                strokeWidth = 9f * scale,
                cap = StrokeCap.Round
            )

            drawLine(
                color = lowerColor,
                start = lower,
                end = hip,
                strokeWidth = 9f * scale,
                cap = StrokeCap.Round
            )

            // Pelvis
            drawLine(
                color = themedColor(0xFF2C455F),
                start = Offset(
                    hip.x - 50f * scale,
                    hip.y
                ),
                end = Offset(
                    hip.x + 50f * scale,
                    hip.y
                ),
                strokeWidth = 24f * scale,
                cap = StrokeCap.Round
            )

            // Legs
            drawLine(
                color = themedColor(0xFF1A3148),
                start = hip,
                end = Offset(
                    hip.x - 31f * scale,
                    478f * scale
                ),
                strokeWidth = 22f * scale,
                cap = StrokeCap.Round
            )

            drawLine(
                color = themedColor(0xFF1A3148),
                start = hip,
                end = Offset(
                    hip.x + 31f * scale,
                    478f * scale
                ),
                strokeWidth = 22f * scale,
                cap = StrokeCap.Round
            )

            // Vertebrae nodes — colored independently
            val nodes = listOf(
                pos((kotlin.math.sin(ur) * 13f).toFloat(), 111f),
                pos((kotlin.math.sin(ur) * 21f).toFloat(), 133f),
                upper,
                pos(
                    (
                            kotlin.math.sin(ur) * 13f +
                                    kotlin.math.sin(lr) * 5f
                            ).toFloat(),
                    194f
                ),
                middle,
                pos((kotlin.math.sin(lr) * 21f).toFloat(), 266f),
                lower,
                pos((kotlin.math.sin(lr) * 39f).toFloat(), 327f)
            )

            nodes.forEachIndexed { index, node ->
                val c =
                    if (index <= 4) upperColor else lowerColor

                val big =
                    index == 2 || index == 6

                drawCircle(
                    color = c.copy(alpha = 0.16f),
                    radius = if (big) 18f * scale else 10f * scale,
                    center = node
                )

                drawCircle(
                    color = c,
                    radius = if (big) 7f * scale else 5f * scale,
                    center = node
                )
            }

            // Sensor targets
            drawCircle(
                color = upperColor.copy(alpha = 0.08f),
                radius = 31f * scale,
                center = upper
            )

            drawCircle(
                color = upperColor,
                radius = 4.5f * scale,
                center = upper
            )

            drawCircle(
                color = lowerColor.copy(alpha = 0.08f),
                radius = 31f * scale,
                center = lower
            )

            drawCircle(
                color = lowerColor,
                radius = 4.5f * scale,
                center = lower
            )

            // Callout lines
            drawLine(
                color = upperColor,
                start = upper,
                end = Offset(
                    upper.x - 70f * scale,
                    upper.y
                ),
                strokeWidth = 2f * scale,
                cap = StrokeCap.Round
            )

            drawLine(
                color = lowerColor,
                start = lower,
                end = Offset(
                    lower.x + 70f * scale,
                    lower.y
                ),
                strokeWidth = 2f * scale,
                cap = StrokeCap.Round
            )
        }

        // Upper callout panel
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 10.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(themedColor(0xAA07111C))
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Text(
                text = "UPPER SPINE",
                fontSize = 7.sp,
                fontWeight = FontWeight.Bold,
                color = upperColor
            )
            Text(
                text = String.format(Locale.US, "%.1f°", upperAngle),
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = themedWhite()
            )
            Text(
                text = postureZoneLabel(upperZone),
                fontSize = 7.sp,
                fontWeight = FontWeight.Bold,
                color = upperColor
            )
        }

        // Lower callout panel
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 10.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(themedColor(0xAA07111C))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = "LOWER SPINE",
                fontSize = 7.sp,
                fontWeight = FontWeight.Bold,
                color = lowerColor
            )
            Text(
                text = String.format(Locale.US, "%.1f°", lowerAngle),
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = themedWhite()
            )
            Text(
                text = postureZoneLabel(lowerZone),
                fontSize = 7.sp,
                fontWeight = FontWeight.Bold,
                color = lowerColor
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 7.dp)
                .clip(RoundedCornerShape(50.dp))
                .background(themedColor(0xFF15263B))
                .padding(horizontal = 9.dp, vertical = 5.dp)
        ) {
            Text(
                text = if (viewMode == "3D")
                    "3D BACK • LIVE"
                else
                    "2D BACK • LIVE",
                fontSize = 7.sp,
                fontWeight = FontWeight.Bold,
                color = themedColor(0xFFCBD5E1)
            )
        }
    }
}

// ============================================================
// MORE SCREEN — PROFILE & DEVICE CONTROL
// ============================================================

@Composable
fun MoreScreen(
    connected: Boolean,
    onOpenVoiceCoach: () -> Unit = {},
    onOpenMLModelLab: () -> Unit = {},
    onThemeChanged: (Boolean) -> Unit = {},
    onLogout: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var vibrationEnabled by remember { mutableStateOf(true) }
    var notificationEnabled by remember { mutableStateOf(true) }
    var buzzerEnabled by remember { mutableStateOf(true) }
    val isDarkMode = ThemeState.isDarkMode

    val statusColor =
        if (connected) themedColor(0xFF16A34A) else themedColor(0xFFDC2626)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(themedColor(0xFFF4F7FB))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        // --------------------------------------------------------
        // HEADER
        // --------------------------------------------------------
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "MORE",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = themedColor(0xFF0F172A)
                )

                Text(
                    text = "Profile, alerts and device controls",
                    fontSize = 12.sp,
                    color = themedColor(0xFF64748B)
                )
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(themedColor(0xFFEDE9FE)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "•••",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = themedColor(0xFF7C3AED)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // --------------------------------------------------------
        // POSTUREIQ ML MODEL LAB — separate feature entry
        // --------------------------------------------------------
        MoreSectionTitle("POSTUREIQ ML MODEL LAB")
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenMLModelLab),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = themedColor(0xFF10283A))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(52.dp).clip(CircleShape)
                        .background(themedColor(0xFF06B6D4).copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) { Text("🧠", fontSize = 25.sp) }
                Spacer(modifier = Modifier.size(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("ML Model Comparison", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = themedWhite())
                    Spacer(modifier = Modifier.height(3.dp))
                    Text("LSTM • Linear Regression • Random Forest • Isolation Forest", fontSize = 10.sp, color = themedColor(0xFFB7C2D8))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Live peak intelligence and model evaluation status", fontSize = 10.sp, color = themedColor(0xFF67E8F9))
                }
                Text("›", fontSize = 28.sp, color = themedColor(0xFF67E8F9))
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // --------------------------------------------------------
        // PROFILE HERO
        // --------------------------------------------------------
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = themedColor(0xFF3B1D78)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .clip(CircleShape)
                        .background(themedWhite().copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "K",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = themedWhite()
                    )
                }

                Spacer(modifier = Modifier.size(13.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Posture Profile",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = themedWhite()
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = "Personal thresholds and feedback",
                        fontSize = 11.sp,
                        color = themedColor(0xFFD8CCFF)
                    )

                    Spacer(modifier = Modifier.height(7.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(
                                    if (connected)
                                        themedColor(0xFF4ADE80)
                                    else
                                        themedColor(0xFFF87171)
                                )
                        )

                        Spacer(modifier = Modifier.size(5.dp))

                        Text(
                            text = if (connected)
                                "ESP32 connected"
                            else
                                "ESP32 disconnected",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (connected)
                                themedColor(0xFFBBF7D0)
                            else
                                themedColor(0xFFFECACA)
                        )
                    }
                }

                Text(
                    text = "›",
                    fontSize = 30.sp,
                    color = themedWhite().copy(alpha = 0.7f)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // --------------------------------------------------------
        // SMART VOICE COACH
        // --------------------------------------------------------
        MoreSectionTitle("SMART VOICE COACH")

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenVoiceCoach),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = themedColor(0xFF111C35)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(themedColor(0xFF7C3AED).copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🎙", fontSize = 25.sp)
                }

                Spacer(modifier = Modifier.size(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Personal Voice Coach",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = themedWhite()
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        "6 languages • posture tips • exercises",
                        fontSize = 10.sp,
                        color = themedColor(0xFFB7C2D8)
                    )
                }

                Text("›", fontSize = 28.sp, color = themedColor(0xFFA78BFA))
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // --------------------------------------------------------
        // CALIBRATION
        // --------------------------------------------------------
        MoreSectionTitle("CALIBRATION")

        Spacer(modifier = Modifier.height(8.dp))

        MoreActionCard(
            title = "Upper spine calibration",
            subtitle = "Set a new personal upper reference",
            icon = "◎",
            iconColor = themedColor(0xFF2563EB)
        )

        Spacer(modifier = Modifier.height(8.dp))

        MoreActionCard(
            title = "Lower spine calibration",
            subtitle = "Set a new personal lower reference",
            icon = "◎",
            iconColor = themedColor(0xFF16A34A)
        )

        Spacer(modifier = Modifier.height(14.dp))

        // --------------------------------------------------------
        // APPEARANCE
        // --------------------------------------------------------
        MoreSectionTitle("APPEARANCE")
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = themedColor(0xFFFFFFFFL))
        ) {
            Column(Modifier.padding(14.dp)) {
                Text("Choose your preferred mode", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = themedColor(0xFF0F172AL))
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onThemeChanged(false) },
                        modifier = Modifier.weight(1f),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = if (!isDarkMode) themedColor(0xFF2563EBL) else themedColor(0xFFF1F5F9L))
                    ) { Text("☀ Light", color = if (!isDarkMode) themedWhite() else themedColor(0xFF0F172AL), fontWeight = FontWeight.Bold) }
                    Button(
                        onClick = { onThemeChanged(true) },
                        modifier = Modifier.weight(1f),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = if (isDarkMode) themedColor(0xFF5B21B6L) else themedColor(0xFF0F172AL))
                    ) { Text("☾ Dark", color = themedWhite(), fontWeight = FontWeight.Bold) }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    if (isDarkMode) "Dark mode • cinematic neon interface" else "Light mode • exact Step 22 interface",
                    fontSize = 10.sp,
                    color = themedColor(0xFF64748BL),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // --------------------------------------------------------
        // ALERTS
        // --------------------------------------------------------
        MoreSectionTitle("ALERTS & FEEDBACK")

        Spacer(modifier = Modifier.height(8.dp))

        MoreToggleCard(
            title = "Phone vibration",
            subtitle = "Vibrate when persistent bad posture is detected",
            checked = vibrationEnabled,
            onCheckedChange = { vibrationEnabled = it },
            icon = "◌",
            iconColor = themedColor(0xFFEA580C)
        )

        Spacer(modifier = Modifier.height(8.dp))

        MoreToggleCard(
            title = "Android notifications",
            subtitle = "Show posture alerts in the notification shade",
            checked = notificationEnabled,
            onCheckedChange = { notificationEnabled = it },
            icon = "⌁",
            iconColor = themedColor(0xFF7C3AED)
        )

        Spacer(modifier = Modifier.height(8.dp))

        MoreToggleCard(
            title = "Belt buzzer",
            subtitle = "Use the hardware buzzer for immediate feedback",
            checked = buzzerEnabled,
            onCheckedChange = { buzzerEnabled = it },
            icon = "♪",
            iconColor = themedColor(0xFFDB2777)
        )

        Spacer(modifier = Modifier.height(14.dp))

        // --------------------------------------------------------
        // DEVICE
        // --------------------------------------------------------
        MoreSectionTitle("DEVICE")

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = themedWhite())
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                MoreInfoRow(
                    label = "Device",
                    value = "ESP32"
                )

                MoreInfoRow(
                    label = "Wi-Fi",
                    value = if (connected) "Connected" else "Offline",
                    valueColor = statusColor
                )

                MoreInfoRow(
                    label = "Sensors",
                    value = "2 × MPU6050"
                )

                MoreInfoRow(
                    label = "Endpoint",
                    value = "192.168.4.1"
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // --------------------------------------------------------
        // APP SETTINGS
        // --------------------------------------------------------
        MoreSectionTitle("APP SETTINGS")

        Spacer(modifier = Modifier.height(8.dp))

        MoreActionCard(
            title = "Sensitivity",
            subtitle = "Posture tolerance • 10°",
            icon = "◈",
            iconColor = themedColor(0xFF0891B2)
        )

        Spacer(modifier = Modifier.height(8.dp))

        MoreToggleCard(
            title = "Dark mode",
            subtitle = if (isDarkMode) "Cinematic theme is active" else "Switch to the cinematic dark theme",
            checked = isDarkMode,
            onCheckedChange = onThemeChanged,
            icon = "◐",
            iconColor = themedColor(0xFF475569)
        )

        Spacer(modifier = Modifier.height(8.dp))

        MoreActionCard(
            title = "About Smart Posture Belt",
            subtitle = "Dual-sensor posture intelligence system",
            icon = "ⓘ",
            iconColor = themedColor(0xFF2563EB)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onLogout),
            shape = RoundedCornerShape(21.dp),
            colors = CardDefaults.cardColors(containerColor = themedColor(0xFFFFF1F2))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(15.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(themedColor(0xFFE11D48).copy(alpha = 0.10f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("↪", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = themedColor(0xFFE11D48))
                }
                Spacer(modifier = Modifier.size(11.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Sign out", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = themedColor(0xFF9F1239))
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Return to the cinematic login screen", fontSize = 10.sp, color = themedColor(0xFFBE123C))
                }
                Text("›", fontSize = 28.sp, color = themedColor(0xFFE11D48))
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "Smart Posture Belt • v1.0",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            fontSize = 10.sp,
            color = themedColor(0xFF94A3B8)
        )

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun MoreSectionTitle(
    title: String
) {
    Text(
        text = title,
        fontSize = 11.sp,
        fontWeight = FontWeight.ExtraBold,
        color = themedColor(0xFF64748B)
    )
}

@Composable
private fun MoreActionCard(
    title: String,
    subtitle: String,
    icon: String,
    iconColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(21.dp),
        colors = CardDefaults.cardColors(
            containerColor = themedWhite()
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(iconColor.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = icon,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = iconColor
                )
            }

            Spacer(modifier = Modifier.size(11.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = themedColor(0xFF0F172A)
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    color = themedColor(0xFF64748B)
                )
            }

            Text(
                text = "›",
                fontSize = 25.sp,
                color = themedColor(0xFF94A3B8)
            )
        }
    }
}

@Composable
private fun MoreToggleCard(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: String,
    iconColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(21.dp),
        colors = CardDefaults.cardColors(
            containerColor = themedWhite()
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 15.dp, top = 12.dp, bottom = 12.dp, end = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(iconColor.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = icon,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = iconColor
                )
            }

            Spacer(modifier = Modifier.size(11.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = themedColor(0xFF0F172A)
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    color = themedColor(0xFF64748B)
                )
            }

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}

@Composable
private fun MoreInfoRow(
    label: String,
    value: String,
    valueColor: Color = themedColor(0xFF0F172A)
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = themedColor(0xFF64748B)
        )

        Text(
            text = value,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
    }
}

// ============================================================
// HEADER
// ============================================================

@Composable
fun Header(
    connected: Boolean
) {

    Row(

        modifier =
            Modifier.fillMaxWidth(),

        horizontalArrangement =
            Arrangement.SpaceBetween,

        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Column {

            Text(

                text =
                    "SMART POSTURE",

                fontSize =
                    28.sp,

                fontWeight =
                    FontWeight.ExtraBold,

                color =
                    themedColor(0xFF17202A)
            )


            Spacer(
                modifier =
                    Modifier.height(2.dp)
            )


            Text(

                text =
                    "Posture Intelligence System",

                fontSize =
                    13.sp,

                color =
                    themedColor(0xFF6B7280)
            )
        }


        Row(

            verticalAlignment =
                Alignment.CenterVertically,

            horizontalArrangement =
                Arrangement.spacedBy(6.dp)
        ) {

            Box(

                modifier =
                    Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(

                            if (connected)
                                themedColor(0xFF22C55E)
                            else
                                themedColor(0xFFEF4444)
                        )
            )


            Text(

                text =
                    if (connected)
                        "LIVE"
                    else
                        "OFFLINE",

                fontSize =
                    12.sp,

                fontWeight =
                    FontWeight.Bold,

                color =
                    if (connected)
                        themedColor(0xFF16A34A)
                    else
                        themedColor(0xFFDC2626)
            )
        }
    }
}


// ============================================================
// POSTURE STATUS CARD
// ============================================================

@Composable
fun PostureStatusCard(

    goodPosture: Boolean,

    connected: Boolean,

    badPostureSeconds: Long,

    warningLevel: Int

) {

    val backgroundColor by
    animateColorAsState(

        targetValue =
            when {

                goodPosture ->
                    themedColor(0xFFEAF8EF)

                warningLevel == 1 ->
                    themedColor(0xFFFFF7E6)

                warningLevel == 2 ->
                    themedColor(0xFFFFF1DD)

                else ->
                    themedColor(0xFFFFEEEE)
            },

        animationSpec =
            tween(400),

        label =
            "statusBackground"
    )


    val statusColor by
    animateColorAsState(

        targetValue =
            when {

                goodPosture ->
                    themedColor(0xFF15803D)

                warningLevel == 1 ->
                    themedColor(0xFFD97706)

                warningLevel == 2 ->
                    themedColor(0xFFD97706)

                else ->
                    themedColor(0xFFDC2626)
            },

        animationSpec =
            tween(400),

        label =
            "statusColor"
    )


    val heading =
        when {

            !connected ->
                "WAITING"

            goodPosture ->
                "GOOD POSTURE"

            warningLevel == 1 ->
                "POSTURE DETECTED"

            warningLevel == 2 ->
                "CORRECT YOUR POSTURE"

            warningLevel >= 3 ->
                "POSTURE ALERT"

            else ->
                "POSTURE DETECTED"
        }


    val message =
        when {

            !connected ->
                "Waiting for ESP32..."

            goodPosture ->
                "Your posture is within the normal range"

            warningLevel == 1 ->
                "Monitoring your posture • ${badPostureSeconds}s"

            warningLevel == 2 ->
                "You've been in this position for ${badPostureSeconds}s"

            warningLevel >= 3 ->
                "Please correct your posture • ${badPostureSeconds}s"

            else ->
                "Please correct your posture"
        }


    Card(

        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(28.dp),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    backgroundColor
            )
    ) {

        Column(

            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        vertical = 25.dp,
                        horizontal = 20.dp
                    ),

            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Text(

                text =
                    "CURRENT POSTURE",

                fontSize =
                    12.sp,

                fontWeight =
                    FontWeight.Bold,

                color =
                    themedColor(0xFF6B7280)
            )


            Spacer(
                modifier =
                    Modifier.height(12.dp)
            )


            // ====================================================
            // ICON
            // ====================================================

            Box(

                modifier =
                    Modifier
                        .size(70.dp)
                        .clip(CircleShape)
                        .background(

                            when {

                                goodPosture ->
                                    themedColor(0xFFD1FAE5)

                                warningLevel >= 3 ->
                                    themedColor(0xFFFFDADA)

                                else ->
                                    themedColor(0xFFFFEBC6)
                            }
                        ),

                contentAlignment =
                    Alignment.Center
            ) {

                AnimatedContent(

                    targetState =
                        goodPosture to warningLevel,

                    transitionSpec = {

                        fadeIn(
                            animationSpec =
                                tween(300)
                        ).togetherWith(

                            fadeOut(
                                animationSpec =
                                    tween(200)
                            )
                        )
                    },

                    label =
                        "postureIcon"
                ) { state ->

                    val good =
                        state.first

                    val level =
                        state.second


                    Text(

                        text =
                            when {

                                good ->
                                    "✓"

                                level >= 3 ->
                                    "!"

                                else ->
                                    "!"
                            },

                        fontSize =
                            36.sp,

                        fontWeight =
                            FontWeight.ExtraBold,

                        color =
                            statusColor
                    )
                }
            }


            Spacer(
                modifier =
                    Modifier.height(13.dp)
            )


            // ====================================================
            // HEADING
            // ====================================================

            AnimatedContent(

                targetState =
                    heading,

                transitionSpec = {

                    fadeIn(
                        animationSpec =
                            tween(300)
                    ).togetherWith(

                        fadeOut(
                            animationSpec =
                                tween(200)
                        )
                    )
                },

                label =
                    "postureHeading"
            ) { currentHeading ->

                Text(

                    text =
                        currentHeading,

                    fontSize =
                        when {

                            currentHeading ==
                                    "CORRECT YOUR POSTURE" ->
                                22.sp

                            else ->
                                27.sp
                        },

                    fontWeight =
                        FontWeight.ExtraBold,

                    textAlign =
                        TextAlign.Center,

                    color =
                        statusColor
                )
            }


            Spacer(
                modifier =
                    Modifier.height(7.dp)
            )


            // ====================================================
            // MESSAGE
            // ====================================================

            AnimatedContent(

                targetState =
                    message,

                transitionSpec = {

                    fadeIn(
                        animationSpec =
                            tween(250)
                    ).togetherWith(

                        fadeOut(
                            animationSpec =
                                tween(150)
                        )
                    )
                },

                label =
                    "postureMessage"
            ) { currentMessage ->

                Text(

                    text =
                        currentMessage,

                    fontSize =
                        13.sp,

                    textAlign =
                        TextAlign.Center,

                    color =
                        themedColor(0xFF4B5563)
                )
            }


            // ====================================================
            // TIMER
            // ====================================================

            if (
                connected &&
                !goodPosture &&
                badPostureSeconds > 0
            ) {

                Spacer(
                    modifier =
                        Modifier.height(15.dp)
                )


                Text(

                    text =
                        "${badPostureSeconds}s",

                    fontSize =
                        22.sp,

                    fontWeight =
                        FontWeight.ExtraBold,

                    color =
                        statusColor
                )


                Text(

                    text =
                        when {

                            badPostureSeconds < 5 ->
                                "Monitoring"

                            badPostureSeconds < 10 ->
                                "Correction recommended"

                            else ->
                                "Alert threshold reached"
                        },

                    fontSize =
                        11.sp,

                    fontWeight =
                        FontWeight.Medium,

                    color =
                        themedColor(0xFF6B7280)
                )
            }
        }
    }
}


// ============================================================
// LIVE POSTURE VISUALIZATION
// ============================================================

@Composable
fun LivePostureVisualization(
    upperAngle: Double,
    lowerAngle: Double,
    goodPosture: Boolean,
    connected: Boolean
) {
    // These animations only react when the real ESP32 values change.
    val upperTilt by animateFloatAsState(
        targetValue = upperAngle.coerceIn(-35.0, 35.0).toFloat(),
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "upperTilt"
    )
    val lowerTilt by animateFloatAsState(
        targetValue = lowerAngle.coerceIn(-35.0, 35.0).toFloat(),
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "lowerTilt"
    )

    val stateColor = when {
        !connected -> themedColor(0xFF94A3B8)
        goodPosture -> themedColor(0xFF16A34A)
        else -> themedColor(0xFFDC2626)
    }

    val upperMagnitude = abs(upperAngle)
    val lowerMagnitude = abs(lowerAngle)
    val stability = (100.0 - ((upperMagnitude + lowerMagnitude) * 1.35))
        .coerceIn(0.0, 100.0)
        .toInt()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = themedWhite())
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "LIVE POSTURE ANALYSIS",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = themedColor(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Real-time spine alignment",
                        fontSize = 11.sp,
                        color = themedColor(0xFF64748B)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(stateColor)
                    )
                    Text(
                        text = if (connected) "LIVE" else "OFFLINE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = stateColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main visual area: no decorative looping animation.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(330.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(themedColor(0xFFF8FAFC))
            ) {
                // Central alignment visualization.
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 56.dp, vertical = 18.dp)
                ) {
                    val cx = size.width / 2f
                    val centerX = cx
                    val topY = 48f
                    val neckY = 78f
                    val shoulderY = 100f
                    val upperEndY = 142f
                    val waistY = 195f
                    val lowerEndY = 235f
                    val hipY = 258f
                    val legY = 305f

                    val upperRad = Math.toRadians(upperTilt.toDouble())
                    val lowerRad = Math.toRadians(lowerTilt.toDouble())

                    val upperShift = (kotlin.math.sin(upperRad) * 58f).toFloat()
                    val lowerShift = (kotlin.math.sin(lowerRad) * 48f).toFloat()

                    val head = Offset(centerX + upperShift * 0.16f, topY)
                    val neck = Offset(centerX + upperShift * 0.24f, neckY)
                    val upper = Offset(centerX + upperShift * 0.55f, upperEndY)
                    val waist = Offset(centerX + lowerShift * 0.48f, lowerEndY)
                    val hip = Offset(centerX + lowerShift, hipY)

                    // Neutral vertical reference axis.
                    drawLine(
                        color = themedColor(0xFFE2E8F0),
                        start = Offset(centerX, 18f),
                        end = Offset(centerX, legY),
                        strokeWidth = 3f,
                        cap = StrokeCap.Round
                    )

                    // Subtle guide ticks.
                    for (y in listOf(78f, 142f, 195f, 235f, 258f)) {
                        drawLine(
                            color = themedColor(0xFFE2E8F0),
                            start = Offset(centerX - 8f, y),
                            end = Offset(centerX + 8f, y),
                            strokeWidth = 2f
                        )
                    }

                    // Head.
                    drawCircle(
                        color = themedColor(0xFF1E293B),
                        radius = 23f,
                        center = head
                    )

                    // Neck.
                    drawLine(
                        color = themedColor(0xFF475569),
                        start = Offset(head.x, head.y + 22f),
                        end = neck,
                        strokeWidth = 9f,
                        cap = StrokeCap.Round
                    )

                    // Shoulders.
                    val shoulderCenter = Offset(centerX + upperShift * 0.36f, shoulderY)
                    drawLine(
                        color = themedColor(0xFF64748B),
                        start = Offset(shoulderCenter.x - 54f, shoulderY),
                        end = Offset(shoulderCenter.x + 54f, shoulderY),
                        strokeWidth = 11f,
                        cap = StrokeCap.Round
                    )

                    // Arms.
                    drawLine(
                        color = themedColor(0xFF94A3B8),
                        start = Offset(shoulderCenter.x - 47f, shoulderY + 3f),
                        end = Offset(shoulderCenter.x - 30f, 188f),
                        strokeWidth = 8f,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = themedColor(0xFF94A3B8),
                        start = Offset(shoulderCenter.x + 47f, shoulderY + 3f),
                        end = Offset(shoulderCenter.x + 30f, 188f),
                        strokeWidth = 8f,
                        cap = StrokeCap.Round
                    )

                    // Spine shadow / track.
                    val spinePath = Path().apply {
                        moveTo(neck.x, neck.y)
                        lineTo(upper.x, upper.y)
                        lineTo(waist.x, waist.y)
                        lineTo(hip.x, hip.y)
                    }
                    drawPath(
                        path = spinePath,
                        color = themedColor(0xFFCBD5E1),
                        style = Stroke(
                            width = 20f,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )

                    // Live spine.
                    drawPath(
                        path = spinePath,
                        color = stateColor,
                        style = Stroke(
                            width = 11f,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )

                    // Sensor nodes with rings.
                    drawCircle(
                        color = themedColor(0x332563EB),
                        radius = 17f,
                        center = upper
                    )
                    drawCircle(
                        color = themedColor(0xFF2563EB),
                        radius = 8f,
                        center = upper
                    )

                    drawCircle(
                        color = themedColor(0x3316A34A),
                        radius = 17f,
                        center = waist
                    )
                    drawCircle(
                        color = themedColor(0xFF16A34A),
                        radius = 8f,
                        center = waist
                    )

                    // Hips.
                    drawLine(
                        color = themedColor(0xFF64748B),
                        start = Offset(hip.x - 38f, hip.y),
                        end = Offset(hip.x + 38f, hip.y),
                        strokeWidth = 10f,
                        cap = StrokeCap.Round
                    )

                    // Legs.
                    drawLine(
                        color = themedColor(0xFF64748B),
                        start = hip,
                        end = Offset(hip.x - 25f, legY),
                        strokeWidth = 9f,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = themedColor(0xFF64748B),
                        start = hip,
                        end = Offset(hip.x + 25f, legY),
                        strokeWidth = 9f,
                        cap = StrokeCap.Round
                    )
                }

                // Upper sensor label.
                Column(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                ) {
                    Text(
                        text = "UPPER SPINE",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = themedColor(0xFF2563EB)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = String.format(Locale.US, "%+.1f°", upperAngle),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = themedColor(0xFF0F172A)
                    )
                    Text(
                        text = "Sensor 01",
                        fontSize = 9.sp,
                        color = themedColor(0xFF94A3B8)
                    )
                }

                // Lower sensor label.
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    Text(
                        text = "LOWER SPINE",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = themedColor(0xFF16A34A)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = String.format(Locale.US, "%+.1f°", lowerAngle),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = themedColor(0xFF0F172A)
                    )
                    Text(
                        text = "Sensor 02",
                        fontSize = 9.sp,
                        color = themedColor(0xFF94A3B8)
                    )
                }

                // Status badge.
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(14.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(stateColor.copy(alpha = 0.10f))
                        .padding(horizontal = 10.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = when {
                            !connected -> "NO SIGNAL"
                            goodPosture -> "ALIGNED"
                            else -> "CORRECT POSTURE"
                        },
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = stateColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Compact live stability meter.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "ALIGNMENT STABILITY",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = themedColor(0xFF64748B)
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = if (connected) "$stability% stable" else "Waiting for ESP32",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = themedColor(0xFF0F172A)
                    )
                }

                Text(
                    text = when {
                        !connected -> "● OFFLINE"
                        goodPosture -> "✓ ALIGNED"
                        else -> "! ADJUST"
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = stateColor
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(50.dp))
                    .background(themedColor(0xFFE2E8F0))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(stability / 100f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(50.dp))
                        .background(stateColor)
                )
            }
        }
    }
}


// ============================================================
// SCORE CARD
// ============================================================

@Composable
fun ScoreCard(
    score: Int
) {

    val animatedScore by
    animateFloatAsState(

        targetValue =
            score.toFloat(),

        animationSpec =
            tween(
                durationMillis = 600,
                easing =
                    FastOutSlowInEasing
            ),

        label =
            "score"
    )


    val scoreText =
        when {

            score >= 90 ->
                "Excellent"

            score >= 75 ->
                "Good"

            score >= 50 ->
                "Needs Improvement"

            else ->
                "Poor"
        }


    val progress =
        score.coerceIn(0, 100) / 100f


    val animatedProgress by
    animateFloatAsState(

        targetValue =
            progress,

        animationSpec =
            tween(
                durationMillis = 700,
                easing =
                    FastOutSlowInEasing
            ),

        label =
            "progress"
    )


    Card(

        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(25.dp),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    themedWhite()
            )
    ) {

        Column(

            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
        ) {

            Row(

                modifier =
                    Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.SpaceBetween,

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Column {

                    Text(

                        text =
                            "POSTURE SCORE",

                        fontSize =
                            12.sp,

                        fontWeight =
                            FontWeight.Bold,

                        color =
                            themedColor(0xFF6B7280)
                    )


                    Spacer(
                        modifier =
                            Modifier.height(4.dp)
                    )


                    Row(
                        verticalAlignment =
                            Alignment.Bottom
                    ) {

                        Text(

                            text =
                                "${animatedScore.toInt()}",

                            fontSize =
                                38.sp,

                            fontWeight =
                                FontWeight.ExtraBold,

                            color =
                                themedColor(0xFF17202A)
                        )


                        Text(

                            text =
                                " / 100",

                            fontSize =
                                19.sp,

                            fontWeight =
                                FontWeight.Bold,

                            color =
                                themedColor(0xFF6B7280),

                            modifier =
                                Modifier.padding(
                                    bottom = 5.dp
                                )
                        )
                    }


                    Text(

                        text =
                            scoreText,

                        fontSize =
                            13.sp,

                        fontWeight =
                            FontWeight.Bold,

                        color =
                            when {

                                score >= 90 ->
                                    themedColor(0xFF16A34A)

                                score >= 75 ->
                                    themedColor(0xFF2563EB)

                                score >= 50 ->
                                    themedColor(0xFFD97706)

                                else ->
                                    themedColor(0xFFDC2626)
                            }
                    )
                }


                Text(

                    text =
                        "★",

                    fontSize =
                        38.sp,

                    color =
                        themedColor(0xFF2563EB)
                )
            }


            Spacer(
                modifier =
                    Modifier.height(15.dp)
            )


            Box(

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(
                            RoundedCornerShape(10.dp)
                        )
                        .background(
                            themedColor(0xFFE8EDF3)
                        )
            ) {

                Box(

                    modifier =
                        Modifier
                            .fillMaxWidth(
                                animatedProgress
                            )
                            .height(8.dp)
                            .clip(
                                RoundedCornerShape(10.dp)
                            )
                            .background(
                                themedColor(0xFF2563EB)
                            )
                )
            }
        }
    }
}


// ============================================================
// SENSOR CARD
// ============================================================

@Composable
fun SensorCard(

    title: String,

    angle: Double,

    difference: Double,

    good: Boolean,

    modifier: Modifier

) {

    val animatedAngle by
    animateFloatAsState(

        targetValue =
            angle.toFloat(),

        animationSpec =
            tween(450),

        label =
            "angleAnimation"
    )


    val animatedDifference by
    animateFloatAsState(

        targetValue =
            difference.toFloat(),

        animationSpec =
            tween(450),

        label =
            "differenceAnimation"
    )


    val dotColor by
    animateColorAsState(

        targetValue =
            if (good)
                themedColor(0xFF22C55E)
            else
                themedColor(0xFFEF4444),

        animationSpec =
            tween(350),

        label =
            "dotColor"
    )


    Card(

        modifier =
            modifier,

        shape =
            RoundedCornerShape(22.dp),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    themedWhite()
            )
    ) {

        Column(

            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(17.dp)
        ) {

            Row(

                modifier =
                    Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.SpaceBetween,

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Text(

                    text =
                        title,

                    fontSize =
                        11.sp,

                    fontWeight =
                        FontWeight.Bold,

                    color =
                        themedColor(0xFF6B7280)
                )


                Box(

                    modifier =
                        Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(dotColor)
                )
            }


            Spacer(
                modifier =
                    Modifier.height(12.dp)
            )


            Text(

                text =
                    String.format(
                        Locale.US,
                        "%.2f°",
                        animatedAngle
                    ),

                fontSize =
                    25.sp,

                fontWeight =
                    FontWeight.ExtraBold,

                color =
                    themedColor(0xFF17202A)
            )


            Spacer(
                modifier =
                    Modifier.height(3.dp)
            )


            Text(

                text =
                    String.format(
                        Locale.US,
                        "Deviation %.2f°",
                        animatedDifference
                    ),

                fontSize =
                    11.sp,

                color =
                    themedColor(0xFF6B7280)
            )


            Spacer(
                modifier =
                    Modifier.height(9.dp)
            )


            Text(

                text =
                    if (good)
                        "●  Within range"
                    else
                        "●  Attention needed",

                fontSize =
                    11.sp,

                fontWeight =
                    FontWeight.Bold,

                color =
                    if (good)
                        themedColor(0xFF16A34A)
                    else
                        themedColor(0xFFDC2626)
            )
        }
    }
}


// ============================================================
// GRAPH CARD
// ============================================================

@Composable
fun PostureGraphCard(

    upperHistory: List<Double>,

    lowerHistory: List<Double>

) {

    Card(

        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(25.dp),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    themedWhite()
            )
    ) {

        Column(

            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
        ) {

            Row(

                modifier =
                    Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.SpaceBetween,

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Column {

                    Text(

                        text =
                            "LIVE POSTURE TREND",

                        fontSize =
                            14.sp,

                        fontWeight =
                            FontWeight.Bold,

                        color =
                            themedColor(0xFF17202A)
                    )


                    Text(

                        text =
                            "Last 30 readings",

                        fontSize =
                            11.sp,

                        color =
                            themedColor(0xFF6B7280)
                    )
                }


                Text(

                    text =
                        "● LIVE",

                    fontSize =
                        10.sp,

                    fontWeight =
                        FontWeight.Bold,

                    color =
                        themedColor(0xFF16A34A)
                )
            }


            Spacer(
                modifier =
                    Modifier.height(14.dp)
            )


            Box(

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .clip(
                            RoundedCornerShape(18.dp)
                        )
                        .background(
                            themedColor(0xFFF8FAFC)
                        )
            ) {

                Canvas(

                    modifier =
                        Modifier.fillMaxSize()
                ) {

                    val width =
                        size.width

                    val height =
                        size.height


                    val gridColor =
                        themedColor(0xFFE5EAF0)


                    // ====================================================
                    // GRID
                    // ====================================================

                    for (i in 1..4) {

                        val y =
                            height * i / 5f


                        drawLine(

                            color =
                                gridColor,

                            start =
                                Offset(
                                    0f,
                                    y
                                ),

                            end =
                                Offset(
                                    width,
                                    y
                                ),

                            strokeWidth =
                                1f
                        )
                    }


                    // ====================================================
                    // UPPER
                    // ====================================================

                    if (upperHistory.size >= 2) {

                        drawGraphLine(

                            values =
                                upperHistory,

                            width =
                                width,

                            height =
                                height,

                            minValue =
                                -90.0,

                            maxValue =
                                90.0,

                            lineColor =
                                themedColor(0xFF2563EB)
                        )
                    }


                    // ====================================================
                    // LOWER
                    // ====================================================

                    if (lowerHistory.size >= 2) {

                        drawGraphLine(

                            values =
                                lowerHistory,

                            width =
                                width,

                            height =
                                height,

                            minValue =
                                -90.0,

                            maxValue =
                                90.0,

                            lineColor =
                                themedColor(0xFF16A34A)
                        )
                    }
                }
            }


            Spacer(
                modifier =
                    Modifier.height(12.dp)
            )


            Row(

                horizontalArrangement =
                    Arrangement.spacedBy(20.dp)
            ) {

                LegendItem(

                    label =
                        "Upper spine",

                    color =
                        themedColor(0xFF2563EB)
                )


                LegendItem(

                    label =
                        "Lower spine",

                    color =
                        themedColor(0xFF16A34A)
                )
            }
        }
    }
}


// ============================================================
// GRAPH LINE
// ============================================================

fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGraphLine(

    values: List<Double>,

    width: Float,

    height: Float,

    minValue: Double,

    maxValue: Double,

    lineColor: Color

) {

    if (values.size < 2) {
        return
    }


    val path =
        Path()


    val step =
        width /
                max(
                    1,
                    values.size - 1
                )


    values.forEachIndexed { index, value ->

        val safeValue =
            min(
                max(
                    value,
                    minValue
                ),
                maxValue
            )


        val normalized =
            (
                    safeValue - minValue
                    ) /
                    (
                            maxValue - minValue
                            )


        val x =
            index * step


        val y =
            height -
                    (
                            normalized *
                                    height
                            ).toFloat()


        if (index == 0) {

            path.moveTo(
                x,
                y
            )

        } else {

            path.lineTo(
                x,
                y
            )
        }
    }


    drawPath(

        path =
            path,

        color =
            lineColor,

        style =
            Stroke(
                width =
                    4.5f,

                cap =
                    StrokeCap.Round,

                join =
                    StrokeJoin.Round
            )
    )
}


// ============================================================
// LEGEND
// ============================================================

@Composable
fun LegendItem(

    label: String,

    color: Color

) {

    Row(

        verticalAlignment =
            Alignment.CenterVertically,

        horizontalArrangement =
            Arrangement.spacedBy(6.dp)
    ) {

        Box(

            modifier =
                Modifier
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(color)
        )


        Text(

            text =
                label,

            fontSize =
                11.sp,

            color =
                themedColor(0xFF6B7280)
        )
    }
}


// ============================================================
// LIVE SENSOR DATA CARD
// ============================================================

@Composable
fun LiveDataCard(

    upperAngle: Double,

    lowerAngle: Double,

    upperDifference: Double,

    lowerDifference: Double

) {

    Card(

        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(25.dp),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    themedWhite()
            )
    ) {

        Column(

            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
        ) {

            Text(

                text =
                    "LIVE SENSOR DATA",

                fontSize =
                    14.sp,

                fontWeight =
                    FontWeight.Bold,

                color =
                    themedColor(0xFF17202A)
            )


            Spacer(
                modifier =
                    Modifier.height(12.dp)
            )


            AnimatedDataRow(
                label =
                    "Upper angle",

                value =
                    upperAngle
            )


            AnimatedDataRow(
                label =
                    "Lower angle",

                value =
                    lowerAngle
            )


            AnimatedDataRow(
                label =
                    "Upper deviation",

                value =
                    upperDifference
            )


            AnimatedDataRow(
                label =
                    "Lower deviation",

                value =
                    lowerDifference
            )
        }
    }
}


// ============================================================
// ANIMATED DATA ROW
// ============================================================

@Composable
fun AnimatedDataRow(

    label: String,

    value: Double

) {

    val animatedValue by
    animateFloatAsState(

        targetValue =
            value.toFloat(),

        animationSpec =
            tween(400),

        label =
            label
    )


    Row(

        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    vertical = 7.dp
                ),

        horizontalArrangement =
            Arrangement.SpaceBetween
    ) {

        Text(

            text =
                label,

            fontSize =
                13.sp,

            color =
                themedColor(0xFF6B7280)
        )


        Text(

            text =
                String.format(
                    Locale.US,
                    "%.2f°",
                    animatedValue
                ),

            fontSize =
                14.sp,

            fontWeight =
                FontWeight.Bold,

            color =
                themedColor(0xFF17202A)
        )
    }
}


// ============================================================
// SESSION CARD
// ============================================================

@Composable
fun SessionCard(

    minutes: Long,

    seconds: Long,

    goodPercentage: Long,

    badPercentage: Long,

    alertCount: Int

) {

    Card(

        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(25.dp),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    themedWhite()
            )
    ) {

        Column(

            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
        ) {

            Text(

                text =
                    "SESSION",

                fontSize =
                    14.sp,

                fontWeight =
                    FontWeight.Bold,

                color =
                    themedColor(0xFF17202A)
            )


            Spacer(
                modifier =
                    Modifier.height(7.dp)
            )


            Text(

                text =
                    String.format(
                        Locale.US,
                        "%02d:%02d",
                        minutes,
                        seconds
                    ),

                fontSize =
                    32.sp,

                fontWeight =
                    FontWeight.ExtraBold,

                color =
                    themedColor(0xFF2563EB)
            )


            Spacer(
                modifier =
                    Modifier.height(15.dp)
            )


            Row(

                modifier =
                    Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.SpaceBetween
            ) {

                Statistic(

                    title =
                        "GOOD",

                    value =
                        "$goodPercentage%",

                    valueColor =
                        themedColor(0xFF16A34A)
                )


                Statistic(

                    title =
                        "BAD",

                    value =
                        "$badPercentage%",

                    valueColor =
                        themedColor(0xFFDC2626)
                )


                Statistic(

                    title =
                        "ALERTS",

                    value =
                        "$alertCount",

                    valueColor =
                        themedColor(0xFFD97706)
                )
            }
        }
    }
}


// ============================================================
// STATISTIC
// ============================================================

@Composable
fun Statistic(

    title: String,

    value: String,

    valueColor: Color

) {

    Column {

        Text(

            text =
                title,

            fontSize =
                10.sp,

            fontWeight =
                FontWeight.Bold,

            color =
                themedColor(0xFF6B7280)
        )


        Spacer(
            modifier =
                Modifier.height(3.dp)
        )


        AnimatedContent(

            targetState =
                value,

            transitionSpec = {

                fadeIn(
                    animationSpec =
                        tween(250)
                ).togetherWith(

                    fadeOut(
                        animationSpec =
                            tween(150)
                    )
                )
            },

            label =
                "statistic"
        ) { newValue ->

            Text(

                text =
                    newValue,

                fontSize =
                    20.sp,

                fontWeight =
                    FontWeight.ExtraBold,

                color =
                    valueColor
            )
        }
    }
}


// ============================================================
// FOOTER
// ============================================================

@Composable
fun Footer(
    connected: Boolean
) {

    Text(

        text =
            if (connected)

                "ESP32  •  Wi-Fi connected  •  Sensors active"

            else

                "Waiting for ESP32 connection...",

        modifier =
            Modifier.fillMaxWidth(),

        textAlign =
            TextAlign.Center,

        fontSize =
            11.sp,

        fontWeight =
            FontWeight.Medium,

        color =
            if (connected)
                themedColor(0xFF16A34A)
            else
                themedColor(0xFF6B7280)
    )
}


// ============================================================
// ESP32 DATA MODEL
// ============================================================

data class ESP32Data(

    val upper: Double,

    val lower: Double,

    val upperDifference: Double,

    val lowerDifference: Double,

    val good: Boolean,

    val status: String

)


// ============================================================
// READ ESP32
// ============================================================

fun readESP32(

    address: String

): ESP32Data? {

    val connection =
        URL(address)
            .openConnection()
                as HttpURLConnection


    return try {

        connection.requestMethod =
            "GET"

        connection.connectTimeout =
            3000

        connection.readTimeout =
            3000

        connection.setRequestProperty(
            "Accept",
            "application/json"
        )


        val response =
            connection
                .inputStream
                .bufferedReader()
                .use {
                    it.readText()
                }


        val upper =
            getNumber(
                response,
                "upper"
            )
                ?: return null


        val lower =
            getNumber(
                response,
                "lower"
            )
                ?: return null


        val upperDifference =
            getNumber(
                response,
                "upperDifference"
            )
                ?: return null


        val lowerDifference =
            getNumber(
                response,
                "lowerDifference"
            )
                ?: return null


        val good =
            getBoolean(
                response,
                "good"
            )
                ?: false


        val status =
            getText(
                response,
                "status"
            )
                ?: ""


        ESP32Data(

            upper =
                upper,

            lower =
                lower,

            upperDifference =
                upperDifference,

            lowerDifference =
                lowerDifference,

            good =
                good,

            status =
                status
        )

    } finally {

        connection.disconnect()
    }
}



// ============================================================
// SMART VOICE COACH + EXERCISE ENGINE
// ============================================================

enum class CoachLanguage(
    val label: String,
    val locale: Locale
) {
    ENGLISH("English", Locale.US),
    TAMIL("தமிழ்", Locale("ta", "IN")),
    TELUGU("తెలుగు", Locale("te", "IN")),
    HINDI("हिन्दी", Locale("hi", "IN")),
    KANNADA("ಕನ್ನಡ", Locale("kn", "IN")),
    MALAYALAM("മലയാളം", Locale("ml", "IN"))
}

data class VoiceCoachAdvice(
    val title: String,
    val recommendation: String,
    val exercise: String,
    val exerciseInstruction: String,
    val duration: String
)

private fun getLoginName(context: Context): String {
    val authName = context.getSharedPreferences(AUTH_PREFS, Context.MODE_PRIVATE)
        .getString(AUTH_NAME, null)
        ?.trim()
        .orEmpty()
    if (authName.isNotBlank()) return authName

    val candidates = listOf("user_name", "login_name", "username", "name")
    val stores = listOf("login_prefs", "user_prefs", "auth_prefs", "smart_posture_prefs")
    for (store in stores) {
        val prefs = context.getSharedPreferences(store, Context.MODE_PRIVATE)
        for (key in candidates) {
            val value = prefs.getString(key, null)?.trim().orEmpty()
            if (value.isNotBlank()) return value
        }
    }
    return "there"
}

private fun coachAdvice(
    connected: Boolean,
    goodPosture: Boolean,
    upperDifference: Double,
    lowerDifference: Double
): VoiceCoachAdvice {
    if (!connected) {
        return VoiceCoachAdvice(
            "Belt disconnected",
            "Reconnect the Smart Posture Belt to receive live posture guidance.",
            "Easy walk",
            "Take a comfortable two minute walk while you reconnect.",
            "2 min"
        )
    }

    val upper = abs(upperDifference)
    val lower = abs(lowerDifference)

    return when {
        goodPosture && max(upper, lower) <= 10.0 -> VoiceCoachAdvice(
            "Great posture",
            "Your posture is currently in the good range. Keep your head, shoulders and back comfortable.",
            "Shoulder blade squeeze",
            "Gently draw your shoulder blades back and down, hold briefly, then relax.",
            "10 reps"
        )
        upper >= lower -> VoiceCoachAdvice(
            "Upper spine needs attention",
            "Relax your shoulders, bring your head gently over your shoulders and avoid prolonged forward leaning.",
            "Shoulder rolls",
            "Slowly roll both shoulders backward in a smooth, comfortable circle.",
            "10 reps"
        )
        else -> VoiceCoachAdvice(
            "Lower spine needs attention",
            "Sit or stand tall, keep your pelvis balanced and change position regularly.",
            "Pelvic tilts",
            "Gently tilt your pelvis forward and backward through a small comfortable range.",
            "8 reps"
        )
    }
}

private fun localizedCoach(
    language: CoachLanguage,
    name: String,
    connected: Boolean,
    goodPosture: Boolean,
    upperDifference: Double,
    lowerDifference: Double
): Pair<String, VoiceCoachAdvice> {
    val a = coachAdvice(connected, goodPosture, upperDifference, lowerDifference)
    return when (language) {
        CoachLanguage.ENGLISH -> {
            val msg = if (!connected)
                "Hello $name. The belt is disconnected. Reconnect it for live posture guidance."
            else if (goodPosture && max(abs(upperDifference), abs(lowerDifference)) <= 10.0)
                "Great job, $name! Your posture is looking good. Keep it comfortable. You can do ${a.exercise} for ${a.duration}."
            else if (abs(upperDifference) >= abs(lowerDifference))
                "$name, your upper spine needs some attention. Relax your shoulders, bring your head gently over your shoulders, and do ${a.exercise} for ${a.duration}."
            else
                "$name, your lower spine needs some attention. Sit or stand tall, keep your pelvis balanced, and do ${a.exercise} for ${a.duration}."
            msg to a
        }
        CoachLanguage.TAMIL -> {
            val x = when (a.exercise) {
                "Shoulder rolls" -> "தோள் சுழற்சி"
                "Pelvic tilts" -> "இடுப்பு சாய்வு"
                else -> "தோள்பட்டை சுருக்கம்"
            }
            val msg = if (!connected)
                "$name, பெல்ட் இணைக்கப்படவில்லை. நேரடி வழிகாட்டுதலுக்கு பெல்ட்டை மீண்டும் இணைக்கவும்."
            else if (goodPosture && max(abs(upperDifference), abs(lowerDifference)) <= 10.0)
                "அருமை $name! உங்கள் உட்காரும் நிலை நன்றாக உள்ளது. இதைத் தொடருங்கள். $x பயிற்சியை ${a.duration} செய்யலாம்."
            else if (abs(upperDifference) >= abs(lowerDifference))
                "$name, உங்கள் மேல் முதுகு பகுதியில் கவனம் தேவை. தோள்களை தளர்த்தி, தலையை தோள்களுக்கு நேராக வைத்துக்கொள்ளுங்கள். $x பயிற்சியை ${a.duration} மெதுவாக செய்யுங்கள்."
            else
                "$name, உங்கள் கீழ் முதுகு பகுதியில் கவனம் தேவை. நிமிர்ந்து அமர்ந்து, இடுப்பை சமநிலையில் வைத்துக்கொள்ளுங்கள். $x பயிற்சியை ${a.duration} மெதுவாக செய்யுங்கள்."
            msg to a.copy(title = when { !connected -> "பெல்ட் இணைப்பு"; goodPosture && max(abs(upperDifference), abs(lowerDifference)) <= 10.0 -> "நல்ல நிலை"; abs(upperDifference) >= abs(lowerDifference) -> "மேல் முதுகு கவனம்"; else -> "கீழ் முதுகு கவனம்" })
        }
        CoachLanguage.TELUGU -> {
            val x = when (a.exercise) {
                "Shoulder rolls" -> "భుజాల రొటేషన్"
                "Pelvic tilts" -> "పెల్విక్ టిల్ట్స్"
                else -> "భుజాల బ్లేడ్ స్క్వీజ్"
            }
            val msg = if (!connected)
                "$name, బెల్ట్ కనెక్ట్ కాలేదు. లైవ్ పోస్టర్ గైడెన్స్ కోసం బెల్ట్‌ను మళ్లీ కనెక్ట్ చేయండి."
            else if (goodPosture && max(abs(upperDifference), abs(lowerDifference)) <= 10.0)
                "బాగా చేస్తున్నారు $name! మీ భంగిమ బాగుంది. ఇదే విధంగా కొనసాగండి. $x ను ${a.duration} చేయండి."
            else if (abs(upperDifference) >= abs(lowerDifference))
                "$name, మీ పై వెన్నుపై కొంచెం శ్రద్ధ అవసరం. భుజాలను సడలించి, తలను భుజాలపై సరిగ్గా ఉంచండి. $x ను ${a.duration} నెమ్మదిగా చేయండి."
            else
                "$name, మీ కింది వెన్నుపై శ్రద్ధ అవసరం. నిటారుగా కూర్చుని, నడుమును సమతుల్యంగా ఉంచండి. $x ను ${a.duration} నెమ్మదిగా చేయండి."
            msg to a.copy(title = "వాయిస్ కోచ్")
        }
        CoachLanguage.HINDI -> {
            val x = when (a.exercise) {
                "Shoulder rolls" -> "कंधे घुमाना"
                "Pelvic tilts" -> "पेल्विक टिल्ट"
                else -> "शोल्डर ब्लेड स्क्वीज़"
            }
            val msg = if (!connected)
                "$name, बेल्ट कनेक्ट नहीं है। लाइव पोस्ट्चर गाइडेंस के लिए बेल्ट को फिर से कनेक्ट करें।"
            else if (goodPosture && max(abs(upperDifference), abs(lowerDifference)) <= 10.0)
                "बहुत बढ़िया $name! आपका पोश्चर अच्छा है। इसे बनाए रखें। $x को ${a.duration} करें।"
            else if (abs(upperDifference) >= abs(lowerDifference))
                "$name, आपकी ऊपरी पीठ पर थोड़ा ध्यान चाहिए। कंधों को ढीला रखें और सिर को कंधों के ऊपर रखें। $x को ${a.duration} धीरे करें।"
            else
                "$name, आपकी निचली पीठ पर ध्यान चाहिए। सीधा बैठें और पेल्विस को संतुलित रखें। $x को ${a.duration} धीरे करें।"
            msg to a.copy(title = "वॉइस कोच")
        }
        CoachLanguage.KANNADA -> {
            val x = when (a.exercise) {
                "Shoulder rolls" -> "ಭುಜ ಸುತ್ತಿಸುವ ವ್ಯಾಯಾಮ"
                "Pelvic tilts" -> "ಪೆಲ್ವಿಕ್ ಟಿಲ್ಟ್"
                else -> "ಭುಜದ ಬ್ಲೇಡ್ ಸ್ಕ್ವೀಸ್"
            }
            val msg = if (!connected)
                "$name, ಬೆಲ್ಟ್ ಸಂಪರ್ಕಗೊಂಡಿಲ್ಲ. ಲೈವ್ ಪೋಶ್ಚರ್ ಮಾರ್ಗದರ್ಶನಕ್ಕಾಗಿ ಬೆಲ್ಟ್ ಅನ್ನು ಮರುಸಂಪರ್ಕಿಸಿ."
            else if (goodPosture && max(abs(upperDifference), abs(lowerDifference)) <= 10.0)
                "ಚೆನ್ನಾಗಿದೆ $name! ನಿಮ್ಮ ಪೋಶ್ಚರ್ ಉತ್ತಮವಾಗಿದೆ. ಹೀಗೆ ಮುಂದುವರಿಸಿ. $x ಅನ್ನು ${a.duration} ಮಾಡಿ."
            else if (abs(upperDifference) >= abs(lowerDifference))
                "$name, ನಿಮ್ಮ ಮೇಲಿನ ಬೆನ್ನಿಗೆ ಸ್ವಲ್ಪ ಗಮನ ಬೇಕು. ಭುಜಗಳನ್ನು ಸಡಿಲಗೊಳಿಸಿ ಮತ್ತು ತಲೆಯನ್ನು ಭುಜಗಳ ಮೇಲೆ ಸರಿಯಾಗಿ ಇಡಿ. $x ಅನ್ನು ${a.duration} ನಿಧಾನವಾಗಿ ಮಾಡಿ."
            else
                "$name, ನಿಮ್ಮ ಕೆಳಗಿನ ಬೆನ್ನಿಗೆ ಗಮನ ಬೇಕು. ನೇರವಾಗಿ ಕುಳಿತು ಸೊಂಟವನ್ನು ಸಮತೋಲನದಲ್ಲಿ ಇಡಿ. $x ಅನ್ನು ${a.duration} ನಿಧಾನವಾಗಿ ಮಾಡಿ."
            msg to a.copy(title = "ವಾಯ್ಸ್ ಕೋಚ್")
        }
        CoachLanguage.MALAYALAM -> {
            val x = when (a.exercise) {
                "Shoulder rolls" -> "തോൾ ചുറ്റിക്കൽ"
                "Pelvic tilts" -> "പെൽവിക് ടിൽറ്റ്"
                else -> "ഷോൾഡർ ബ്ലേഡ് സ്ക്വീസ്"
            }
            val msg = if (!connected)
                "$name, ബെൽറ്റ് കണക്റ്റ് ചെയ്തിട്ടില്ല. ലൈവ് പോസ്ചർ ഗൈഡൻസിനായി ബെൽറ്റ് വീണ്ടും കണക്റ്റ് ചെയ്യുക."
            else if (goodPosture && max(abs(upperDifference), abs(lowerDifference)) <= 10.0)
                "വളരെ നല്ലത് $name! നിങ്ങളുടെ പോസ്ചർ നല്ലതാണ്. ഇത് തുടരുക. $x ${a.duration} ചെയ്യൂ."
            else if (abs(upperDifference) >= abs(lowerDifference))
                "$name, നിങ്ങളുടെ മുകളിലെ പിന്നിലേക്ക് കുറച്ച് ശ്രദ്ധ വേണം. തോളുകൾ ശാന്തമാക്കി തല തോളുകൾക്ക് മുകളിൽ നേരെ വയ്ക്കുക. $x ${a.duration} പതുക്കെ ചെയ്യൂ."
            else
                "$name, നിങ്ങളുടെ താഴത്തെ പിന്നിലേക്ക് ശ്രദ്ധ വേണം. നേരെ ഇരുന്ന് പെൽവിസ് സമതുലിതമായി വയ്ക്കുക. $x ${a.duration} പതുക്കെ ചെയ്യൂ."
            msg to a.copy(title = "വോയ്സ് കോച്ച്")
        }
    }
}

@android.annotation.TargetApi(android.os.Build.VERSION_CODES.LOLLIPOP)
private fun speakInLanguage(context: Context, language: CoachLanguage, text: String): Boolean {
    val appContext = context.applicationContext

    postureTextToSpeech?.stop()
    postureTextToSpeech?.shutdown()
    postureTextToSpeech = null

    postureTextToSpeech = TextToSpeech(appContext) { status ->
        if (status != TextToSpeech.SUCCESS) {
            Toast.makeText(
                context,
                "Text-to-Speech engine could not start.",
                Toast.LENGTH_LONG
            ).show()
            return@TextToSpeech
        }

        val tts = postureTextToSpeech ?: return@TextToSpeech
        val target = language.locale

        // First try the exact requested locale.
        val exactResult = tts.isLanguageAvailable(target)
        var usable = exactResult != TextToSpeech.LANG_MISSING_DATA &&
                exactResult != TextToSpeech.LANG_NOT_SUPPORTED

        // Some Android TTS engines expose a Voice even when isLanguageAvailable()
        // does not report the exact country/region combination. Search the installed
        // voices by language and then by language+country.
        if (!usable) {
            val voices = tts.voices.orEmpty()

            val exactVoice = voices.firstOrNull { voice ->
                voice.locale.language.equals(target.language, ignoreCase = true) &&
                        voice.locale.country.equals(target.country, ignoreCase = true) &&
                        !voice.isNetworkConnectionRequired
            }

            val languageVoice = voices.firstOrNull { voice ->
                voice.locale.language.equals(target.language, ignoreCase = true) &&
                        !voice.isNetworkConnectionRequired
            }

            val networkLanguageVoice = voices.firstOrNull { voice ->
                voice.locale.language.equals(target.language, ignoreCase = true)
            }

            val selectedVoice: Voice? = exactVoice ?: languageVoice ?: networkLanguageVoice

            if (selectedVoice != null) {
                tts.voice = selectedVoice
                usable = true
            }
        } else {
            tts.language = target
        }

        if (usable) {
            tts.setSpeechRate(0.92f)
            tts.setPitch(1.0f)
            tts.speak(
                text,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "smart_posture_${language.name}"
            )
        } else {
            Toast.makeText(
                context,
                "${language.label} voice is not installed on this device. Opening Text-to-Speech settings…",
                Toast.LENGTH_LONG
            ).show()

            try {
                val intent = Intent("com.android.settings.TTS_SETTINGS")
                context.startActivity(intent)
            } catch (_: Exception) {
                try {
                    context.startActivity(Intent("android.settings.TTS_SETTINGS"))
                } catch (_: Exception) {
                    // Settings activity is optional on some devices.
                }
            }
        }
    }

    return true
}

@Composable
private fun SmartVoiceCoachOverlay(
    name: String,
    connected: Boolean,
    goodPosture: Boolean,
    upperDifference: Double,
    lowerDifference: Double,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    var languageIndex by remember { mutableIntStateOf(0) }
    var exerciseOpen by remember { mutableStateOf(false) }
    var exerciseLibraryOpen by remember { mutableStateOf(false) }
    var selectedExerciseId by remember { mutableStateOf<String?>(null) }
    val language = CoachLanguage.values()[languageIndex]
    val (spokenMessage, advice) = localizedCoach(language, name, connected, goodPosture, upperDifference, lowerDifference)
    val regionColor = if (abs(upperDifference) >= abs(lowerDifference)) themedColor(0xFF60A5FA) else themedColor(0xFF4ADE80)

    val greeting = when (language) {
        CoachLanguage.ENGLISH -> "Hello $name! I am your Smart Voice Coach. I will guide you with posture recommendations and exercises."
        CoachLanguage.TAMIL -> "வணக்கம் $name! நான் உங்கள் ஸ்மார்ட் வாய்ஸ் கோச். உங்கள் உட்காரும் நிலை மற்றும் பயிற்சிகளில் நான் வழிகாட்டுவேன்."
        CoachLanguage.TELUGU -> "హలో $name! నేను మీ స్మార్ట్ వాయిస్ కోచ్‌ని. మీ భంగిమ మరియు వ్యాయామాలలో నేను మార్గదర్శనం చేస్తాను."
        CoachLanguage.HINDI -> "नमस्ते $name! मैं आपका स्मार्ट वॉइस कोच हूँ। मैं आपके पोश्चर और व्यायाम में मार्गदर्शन करूँगा।"
        CoachLanguage.KANNADA -> "ನಮಸ್ಕಾರ $name! ನಾನು ನಿಮ್ಮ ಸ್ಮಾರ್ಟ್ ವಾಯ್ಸ್ ಕೋಚ್. ನಿಮ್ಮ ಪೋಶ್ಚರ್ ಮತ್ತು ವ್ಯಾಯಾಮದಲ್ಲಿ ನಾನು ಮಾರ್ಗದರ್ಶನ ನೀಡುತ್ತೇನೆ."
        CoachLanguage.MALAYALAM -> "നമസ്കാരം $name! ഞാൻ നിങ്ങളുടെ സ്മാർട്ട് വോയ്സ് കോച്ചാണ്. നിങ്ങളുടെ പോസ്ചറും വ്യായാമവും മെച്ചപ്പെടുത്താൻ ഞാൻ മാർഗ്ഗനിർദ്ദേശം നൽകും."
    }

    LaunchedEffect(language) {
        delay(350)
        speakInLanguage(context, language, greeting)
    }

    Box(Modifier.fillMaxSize().background(themedColor(0xF9050A16))) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("SMART VOICE COACH", fontSize = 25.sp, fontWeight = FontWeight.ExtraBold, color = themedWhite())
                    Text("Personal posture guidance + exercise", fontSize = 11.sp, color = themedColor(0xFF94A3B8))
                }
                Text("×", Modifier.clickable(onClick = onClose).padding(8.dp), fontSize = 28.sp, color = themedWhite())
            }

            Spacer(Modifier.height(14.dp))

            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(themedColor(0xFF121D33))) {
                Column(Modifier.padding(16.dp)) {
                    Text("HELLO", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = themedColor(0xFFA78BFA))
                    Spacer(Modifier.height(2.dp))
                    Text("$name 👋", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = themedWhite())
                    Spacer(Modifier.height(4.dp))
                    Text(if (connected) "Live belt data available" else "Waiting for ESP32 data", fontSize = 10.sp, color = if (connected) themedColor(0xFF4ADE80) else themedColor(0xFFFBBF24))
                }
            }

            Spacer(Modifier.height(12.dp))
            Text("LANGUAGE", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = themedColor(0xFF94A3B8))
            Spacer(Modifier.height(7.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CoachLanguage.values().take(3).forEachIndexed { i, lang ->
                    Card(Modifier.weight(1f).clickable { languageIndex = i }, shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(if (languageIndex == i) themedColor(0xFF5B21B6) else themedColor(0xFF142036))) {
                        Text(lang.label, Modifier.fillMaxWidth().padding(vertical = 11.dp), textAlign = TextAlign.Center, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = themedWhite())
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CoachLanguage.values().drop(3).forEachIndexed { i, lang ->
                    val idx = i + 3
                    Card(Modifier.weight(1f).clickable { languageIndex = idx }, shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(if (languageIndex == idx) themedColor(0xFF5B21B6) else themedColor(0xFF142036))) {
                        Text(lang.label, Modifier.fillMaxWidth().padding(vertical = 11.dp), textAlign = TextAlign.Center, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = themedWhite())
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(themedColor(0xFF101A2D))) {
                Column(Modifier.padding(16.dp)) {
                    Text("LIVE RECOMMENDATION", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = regionColor)
                    Spacer(Modifier.height(5.dp))
                    Text(advice.title, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, color = themedWhite())
                    Spacer(Modifier.height(6.dp))
                    Text(advice.recommendation, fontSize = 12.sp, color = themedColor(0xFFD6DEEA))
                    Spacer(Modifier.height(12.dp))
                    Text("EXERCISE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = themedColor(0xFFFBBF24))
                    Text(advice.exercise, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = themedWhite())
                    Text("${advice.duration} • ${advice.exerciseInstruction}", fontSize = 10.sp, color = themedColor(0xFF94A3B8))
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(modifier = Modifier.weight(1f), onClick = { speakInLanguage(context, language, spokenMessage) }) { Text("🔊 Speak") }
                Button(modifier = Modifier.weight(1f), onClick = { exerciseLibraryOpen = true }) { Text("💪 Exercises") }
            }

            Spacer(Modifier.height(12.dp))
            Text("Safety: stop any exercise if it causes pain or discomfort. This app provides general wellness guidance, not medical treatment.", fontSize = 9.sp, color = themedColor(0xFF7F8EA3))
            Spacer(Modifier.height(18.dp))
        }

        if (exerciseLibraryOpen) {
            ExerciseLibraryOverlay(
                language = language,
                name = name,
                recommendedExercise = advice.exercise,
                onClose = { exerciseLibraryOpen = false },
                onStartExercise = { exercise ->
                    selectedExerciseId = exercise.id
                    exerciseLibraryOpen = false
                    exerciseOpen = true
                }
            )
        }

        if (exerciseOpen) {
            val selected = exerciseCatalog.firstOrNull { it.id == selectedExerciseId }
                ?: exerciseCatalog.firstOrNull { it.name == advice.exercise }
                ?: exerciseCatalog.first()
            GuidedExerciseOverlay(
                exercise = selected,
                language = language,
                name = name,
                onBack = {
                    exerciseOpen = false
                    exerciseLibraryOpen = true
                },
                onClose = { exerciseOpen = false }
            )
        }
    }
}

// ============================================================
// EXERCISE LIBRARY + ANIMATED VISUAL GUIDES
// ============================================================

enum class ExerciseCategory(val title: String, val icon: String) {
    HEAD_NECK("HEAD & NECK", "🧠"),
    SHOULDERS("SHOULDERS", "💪"),
    UPPER_SPINE("UPPER SPINE", "🦴"),
    LOWER_SPINE("LOWER SPINE", "◉")
}

enum class ExerciseAnimation {
    CHIN_TUCK,
    NECK_SIDE,
    NECK_ROTATION,
    SHOULDER_ROLL,
    SHOULDER_SQUEEZE,
    ARM_CIRCLE,
    THORACIC_EXTENSION,
    CAT_COW,
    UPPER_ROTATION,
    PELVIC_TILT,
    SEATED_MOBILITY,
    STANDING_EXTENSION
}

data class ExerciseItem(
    val id: String,
    val category: ExerciseCategory,
    val name: String,
    val subtitle: String,
    val target: Int,
    val unit: String,
    val animation: ExerciseAnimation,
    val caution: String = "Use a small, comfortable range. Stop if you feel pain or discomfort."
)

private val exerciseCatalog = listOf(
    ExerciseItem("chin_tuck", ExerciseCategory.HEAD_NECK, "Chin Tuck", "Gentle head alignment", 8, "reps", ExerciseAnimation.CHIN_TUCK),
    ExerciseItem("neck_side", ExerciseCategory.HEAD_NECK, "Neck Side Stretch", "Gentle left and right stretch", 6, "cycles", ExerciseAnimation.NECK_SIDE),
    ExerciseItem("neck_rotation", ExerciseCategory.HEAD_NECK, "Neck Rotation", "Slow controlled turns", 6, "cycles", ExerciseAnimation.NECK_ROTATION),
    ExerciseItem("shoulder_roll", ExerciseCategory.SHOULDERS, "Shoulder Rolls", "Smooth backward circles", 10, "reps", ExerciseAnimation.SHOULDER_ROLL),
    ExerciseItem("shoulder_squeeze", ExerciseCategory.SHOULDERS, "Shoulder Blade Squeeze", "Gentle shoulder setting", 10, "reps", ExerciseAnimation.SHOULDER_SQUEEZE),
    ExerciseItem("arm_circle", ExerciseCategory.SHOULDERS, "Arm Circles", "Controlled arm mobility", 8, "reps", ExerciseAnimation.ARM_CIRCLE),
    ExerciseItem("thoracic_extension", ExerciseCategory.UPPER_SPINE, "Thoracic Extension", "Open the upper back gently", 8, "reps", ExerciseAnimation.THORACIC_EXTENSION),
    ExerciseItem("cat_cow", ExerciseCategory.UPPER_SPINE, "Cat–Cow", "Gentle spinal movement", 8, "cycles", ExerciseAnimation.CAT_COW),
    ExerciseItem("upper_rotation", ExerciseCategory.UPPER_SPINE, "Upper-Back Rotation", "Slow torso rotation", 6, "cycles", ExerciseAnimation.UPPER_ROTATION),
    ExerciseItem("pelvic_tilt", ExerciseCategory.LOWER_SPINE, "Pelvic Tilts", "Small pelvic movement", 8, "reps", ExerciseAnimation.PELVIC_TILT),
    ExerciseItem("seated_mobility", ExerciseCategory.LOWER_SPINE, "Seated Spinal Mobility", "Gentle seated movement", 6, "cycles", ExerciseAnimation.SEATED_MOBILITY),
    ExerciseItem("standing_extension", ExerciseCategory.LOWER_SPINE, "Standing Back Extension", "Small comfortable extension", 6, "reps", ExerciseAnimation.STANDING_EXTENSION)
)

private fun translatedExerciseName(language: CoachLanguage, exercise: ExerciseItem): String = when (language) {
    CoachLanguage.ENGLISH -> exercise.name
    CoachLanguage.TAMIL -> when (exercise.id) {
        "chin_tuck" -> "தாடை பின்னோக்கி நிலை"
        "neck_side" -> "கழுத்து பக்க நீட்டிப்பு"
        "neck_rotation" -> "கழுத்து சுழற்சி"
        "shoulder_roll" -> "தோள் சுழற்சி"
        "shoulder_squeeze" -> "தோள்பட்டை சுருக்கம்"
        "arm_circle" -> "கை வட்ட சுழற்சி"
        "thoracic_extension" -> "மேல் முதுகு நீட்டிப்பு"
        "cat_cow" -> "கேட்–கௌ"
        "upper_rotation" -> "மேல் முதுகு சுழற்சி"
        "pelvic_tilt" -> "இடுப்பு சாய்வு"
        "seated_mobility" -> "அமர்ந்த முதுகு இயக்கம்"
        else -> "நின்று முதுகு நீட்டிப்பு"
    }
    CoachLanguage.TELUGU -> when (exercise.id) {
        "chin_tuck" -> "చిన్ టక్"
        "neck_side" -> "మెడ పక్క స్ట్రెచ్"
        "neck_rotation" -> "మెడ రొటేషన్"
        "shoulder_roll" -> "భుజాల రోల్"
        "shoulder_squeeze" -> "భుజాల బ్లేడ్ స్క్వీజ్"
        "arm_circle" -> "చేతుల వృత్తాలు"
        "thoracic_extension" -> "పై వెన్ను విస్తరణ"
        "cat_cow" -> "క్యాట్–కౌ"
        "upper_rotation" -> "పై వెన్ను రొటేషన్"
        "pelvic_tilt" -> "పెల్విక్ టిల్ట్స్"
        "seated_mobility" -> "కూర్చుని వెన్ను కదలిక"
        else -> "నిలబడి వెన్ను విస్తరణ"
    }
    CoachLanguage.HINDI -> when (exercise.id) {
        "chin_tuck" -> "चिन टक"
        "neck_side" -> "गर्दन साइड स्ट्रेच"
        "neck_rotation" -> "गर्दन रोटेशन"
        "shoulder_roll" -> "शोल्डर रोल"
        "shoulder_squeeze" -> "शोल्डर ब्लेड स्क्वीज़"
        "arm_circle" -> "आर्म सर्कल"
        "thoracic_extension" -> "ऊपरी पीठ एक्सटेंशन"
        "cat_cow" -> "कैट–काउ"
        "upper_rotation" -> "ऊपरी पीठ रोटेशन"
        "pelvic_tilt" -> "पेल्विक टिल्ट"
        "seated_mobility" -> "बैठकर रीढ़ की गतिशीलता"
        else -> "खड़े होकर पीठ एक्सटेंशन"
    }
    CoachLanguage.KANNADA -> when (exercise.id) {
        "chin_tuck" -> "ಚಿನ್ ಟಕ್"
        "neck_side" -> "ಕುತ್ತಿಗೆ ಬದಿ ಸ್ಟ್ರೆಚ್"
        "neck_rotation" -> "ಕುತ್ತಿಗೆ ಸುತ್ತು"
        "shoulder_roll" -> "ಭುಜ ಸುತ್ತು"
        "shoulder_squeeze" -> "ಭುಜದ ಬ್ಲೇಡ್ ಸ್ಕ್ವೀಸ್"
        "arm_circle" -> "ಕೈ ವೃತ್ತಗಳು"
        "thoracic_extension" -> "ಮೇಲಿನ ಬೆನ್ನು ವಿಸ್ತರಣೆ"
        "cat_cow" -> "ಕ್ಯಾಟ್–ಕೌ"
        "upper_rotation" -> "ಮೇಲಿನ ಬೆನ್ನು ಸುತ್ತು"
        "pelvic_tilt" -> "ಪೆಲ್ವಿಕ್ ಟಿಲ್ಟ್"
        "seated_mobility" -> "ಕುಳಿತು ಬೆನ್ನು ಚಲನೆ"
        else -> "ನಿಂತು ಬೆನ್ನು ವಿಸ್ತರಣೆ"
    }
    CoachLanguage.MALAYALAM -> when (exercise.id) {
        "chin_tuck" -> "ചിൻ ടക്ക്"
        "neck_side" -> "കഴുത്തിന്റെ വശ സ്‌ട്രെച്ച്"
        "neck_rotation" -> "കഴുത്ത് റൊട്ടേഷൻ"
        "shoulder_roll" -> "തോൾ റൂൾ"
        "shoulder_squeeze" -> "ഷോൾഡർ ബ്ലേഡ് സ്‌ക്വീസ്"
        "arm_circle" -> "കൈ വട്ടങ്ങൾ"
        "thoracic_extension" -> "മുകളിലെ പുറം നീട്ടൽ"
        "cat_cow" -> "ക്യാറ്റ്–കൗ"
        "upper_rotation" -> "മുകളിലെ പുറം റൊട്ടേഷൻ"
        "pelvic_tilt" -> "പെൽവിക് ടിൽറ്റ്"
        "seated_mobility" -> "ഇരുന്നുള്ള പിന്നിലെ ചലനം"
        else -> "നിൽക്കുന്ന പുറം നീട്ടൽ"
    }
}

private fun localizedExerciseInstruction(language: CoachLanguage, exercise: ExerciseItem): String = when (language) {
    CoachLanguage.ENGLISH -> when (exercise.id) {
        "chin_tuck" -> "Gently draw your chin straight back without looking down."
        "neck_side" -> "Tilt your head gently to one side, return to center, then switch sides."
        "neck_rotation" -> "Turn your head slowly left and right without forcing the movement."
        "shoulder_roll" -> "Roll both shoulders slowly backward in a smooth circle."
        "shoulder_squeeze" -> "Gently bring your shoulder blades back and down, hold briefly, then relax."
        "arm_circle" -> "Make small, controlled circles with your arms and keep your shoulders relaxed."
        "thoracic_extension" -> "Open your chest gently and extend through the upper back without forcing it."
        "cat_cow" -> "Move slowly between a comfortable rounded back and a comfortable open-back position."
        "upper_rotation" -> "Rotate your upper body gently to one side, return to center, then switch."
        "pelvic_tilt" -> "Gently tilt your pelvis forward and backward through a small comfortable range."
        "seated_mobility" -> "Sit tall and make a small comfortable movement through your upper and lower back."
        else -> "Stand tall and move gently into a small comfortable back extension, then return."
    }
    CoachLanguage.TAMIL -> when (exercise.id) {
        "chin_tuck" -> "கீழே பார்க்காமல், தாடையை மெதுவாக நேராக பின்னால் கொண்டு செல்லுங்கள்."
        "neck_side" -> "தலையை மெதுவாக ஒரு பக்கமாக சாய்த்து நடுவிற்கு கொண்டு வந்து மறுபக்கமாக செய்யுங்கள்."
        "neck_rotation" -> "கட்டாயப்படுத்தாமல் தலையை மெதுவாக இடது வலமாக திருப்புங்கள்."
        "shoulder_roll" -> "இரு தோள்களையும் மெதுவாக பின்னால் வட்டமாக சுழற்றுங்கள்."
        "shoulder_squeeze" -> "தோள்பட்டை எலும்புகளை மெதுவாக பின்னால் மற்றும் கீழே கொண்டு சென்று சிறிது நேரம் பிடித்து தளர்த்துங்கள்."
        "arm_circle" -> "தோள்களை தளர்த்தி, கைகளை சிறிய கட்டுப்படுத்தப்பட்ட வட்டங்களில் சுழற்றுங்கள்."
        "thoracic_extension" -> "மார்பை மெதுவாக திறந்து, மேல் முதுகை வலியின்றி சிறிதளவு நீட்டுங்கள்."
        "cat_cow" -> "வசதியான அளவில் முதுகை மெதுவாக வளைத்து திறந்த நிலைக்கு மாற்றுங்கள்."
        "upper_rotation" -> "மேல் உடலை மெதுவாக ஒரு பக்கமாக திருப்பி நடுவிற்கு கொண்டு வந்து மறுபக்கமாக செய்யுங்கள்."
        "pelvic_tilt" -> "இடுப்பை சிறிய வசதியான அளவில் மெதுவாக முன் மற்றும் பின் சாய்த்துக் கொள்ளுங்கள்."
        "seated_mobility" -> "நிமிர்ந்து அமர்ந்து, மேல் மற்றும் கீழ் முதுகை சிறிய வசதியான அளவில் நகர்த்துங்கள்."
        else -> "நிமிர்ந்து நின்று, முதுகை சிறிய வசதியான அளவில் மெதுவாக நீட்டி மீண்டும் திரும்புங்கள்."
    }
    CoachLanguage.TELUGU -> when (exercise.id) {
        "chin_tuck" -> "కిందికి చూడకుండా, మీ చిన్ని నెమ్మదిగా నేరుగా వెనక్కి తీసుకెళ్లండి."
        "neck_side" -> "తలను నెమ్మదిగా ఒక వైపుకు వంచి మధ్యకు తీసుకువచ్చి, తర్వాత మరో వైపు చేయండి."
        "neck_rotation" -> "బలవంతం చేయకుండా తలను నెమ్మదిగా ఎడమ కుడి వైపులకు తిప్పండి."
        "shoulder_roll" -> "రెండు భుజాలను నెమ్మదిగా వెనక్కి వృత్తాకారంగా తిప్పండి."
        "shoulder_squeeze" -> "భుజాల బ్లేడ్లను నెమ్మదిగా వెనక్కి మరియు కిందికి తీసుకెళ్లి కొద్దిసేపు పట్టుకుని వదలండి."
        "arm_circle" -> "భుజాలను సడలించి చేతులతో చిన్న నియంత్రిత వృత్తాలు చేయండి."
        "thoracic_extension" -> "ఛాతీని నెమ్మదిగా తెరిచి పై వెన్నును బలవంతం చేయకుండా కొద్దిగా విస్తరించండి."
        "cat_cow" -> "సౌకర్యవంతమైన పరిధిలో వెన్నును నెమ్మదిగా గుండ్రంగా చేసి తెరిచిన స్థితికి మార్చండి."
        "upper_rotation" -> "పై శరీరాన్ని నెమ్మదిగా ఒక వైపుకు తిప్పి మధ్యకు వచ్చి మరో వైపు చేయండి."
        "pelvic_tilt" -> "నడుమును చిన్న సౌకర్యవంతమైన పరిధిలో నెమ్మదిగా ముందుకు వెనక్కి వంచండి."
        "seated_mobility" -> "నిటారుగా కూర్చుని పై మరియు కింది వెన్నును చిన్న సౌకర్యవంతమైన పరిధిలో కదిలించండి."
        else -> "నిటారుగా నిలబడి వెన్నును చిన్న సౌకర్యవంతమైన పరిధిలో నెమ్మదిగా విస్తరించి తిరిగి రండి."
    }
    CoachLanguage.HINDI -> when (exercise.id) {
        "chin_tuck" -> "नीचे देखे बिना अपनी ठुड्डी को धीरे से सीधा पीछे ले जाएँ।"
        "neck_side" -> "सिर को धीरे से एक तरफ झुकाएँ, बीच में लाएँ और फिर दूसरी तरफ करें।"
        "neck_rotation" -> "जोर लगाए बिना सिर को धीरे-धीरे बाएँ और दाएँ घुमाएँ।"
        "shoulder_roll" -> "दोनों कंधों को धीरे-धीरे पीछे की ओर गोल घुमाएँ।"
        "shoulder_squeeze" -> "कंधे की ब्लेड्स को धीरे से पीछे और नीचे लाएँ, थोड़ी देर रोकें और फिर ढीला छोड़ें।"
        "arm_circle" -> "कंधों को ढीला रखते हुए हाथों से छोटे नियंत्रित गोल घुमाएँ।"
        "thoracic_extension" -> "छाती को धीरे से खोलें और ऊपरी पीठ को बिना जोर लगाए थोड़ा फैलाएँ।"
        "cat_cow" -> "आरामदायक सीमा में पीठ को धीरे-धीरे गोल और फिर खुली स्थिति में ले जाएँ।"
        "upper_rotation" -> "ऊपरी शरीर को धीरे से एक तरफ घुमाएँ, बीच में आएँ और फिर दूसरी तरफ करें।"
        "pelvic_tilt" -> "पेल्विस को छोटी और आरामदायक सीमा में धीरे-धीरे आगे और पीछे झुकाएँ।"
        "seated_mobility" -> "सीधे बैठें और ऊपरी तथा निचली पीठ को छोटी आरामदायक सीमा में धीरे से चलाएँ।"
        else -> "सीधे खड़े होकर पीठ को छोटी आरामदायक सीमा में धीरे से फैलाएँ और वापस आएँ।"
    }
    CoachLanguage.KANNADA -> when (exercise.id) {
        "chin_tuck" -> "ಕೆಳಗೆ ನೋಡದೆ ನಿಮ್ಮ ಚಿನ್ ಅನ್ನು ನಿಧಾನವಾಗಿ ನೇರವಾಗಿ ಹಿಂದೆ ತೆಗೆದುಕೊಳ್ಳಿ."
        "neck_side" -> "ತಲೆಯನ್ನು ನಿಧಾನವಾಗಿ ಒಂದು ಬದಿಗೆ ತಗ್ಗಿಸಿ ಮಧ್ಯಕ್ಕೆ ತಂದು ನಂತರ ಇನ್ನೊಂದು ಬದಿಗೆ ಮಾಡಿ."
        "neck_rotation" -> "ಬಲವಂತಪಡಿಸದೆ ತಲೆಯನ್ನು ನಿಧಾನವಾಗಿ ಎಡ ಮತ್ತು ಬಲಕ್ಕೆ ತಿರುಗಿಸಿ."
        "shoulder_roll" -> "ಎರಡೂ ಭುಜಗಳನ್ನು ನಿಧಾನವಾಗಿ ಹಿಂದೆ ವೃತ್ತಾಕಾರವಾಗಿ ಸುತ್ತಿಸಿ."
        "shoulder_squeeze" -> "ಭುಜದ ಬ್ಲೇಡ್‌ಗಳನ್ನು ನಿಧಾನವಾಗಿ ಹಿಂದೆ ಮತ್ತು ಕೆಳಕ್ಕೆ ತಂದು ಸ್ವಲ್ಪ ಹಿಡಿದು ನಂತರ ಸಡಿಲಿಸಿ."
        "arm_circle" -> "ಭುಜಗಳನ್ನು ಸಡಿಲವಾಗಿ ಇಟ್ಟು ಕೈಗಳಿಂದ ಸಣ್ಣ ನಿಯಂತ್ರಿತ ವೃತ್ತಗಳನ್ನು ಮಾಡಿ."
        "thoracic_extension" -> "ಎದೆಯನ್ನು ನಿಧಾನವಾಗಿ ತೆರೆದು ಮೇಲಿನ ಬೆನ್ನು ಬಲವಂತಪಡಿಸದೆ ಸ್ವಲ್ಪ ವಿಸ್ತರಿಸಿ."
        "cat_cow" -> "ಆರಾಮದಾಯಕ ವ್ಯಾಪ್ತಿಯಲ್ಲಿ ಬೆನ್ನನ್ನು ನಿಧಾನವಾಗಿ ವೃತ್ತಾಕಾರವಾಗಿ ಮಾಡಿ ನಂತರ ತೆರೆಯಿರಿ."
        "upper_rotation" -> "ಮೇಲಿನ ದೇಹವನ್ನು ನಿಧಾನವಾಗಿ ಒಂದು ಬದಿಗೆ ತಿರುಗಿಸಿ ಮಧ್ಯಕ್ಕೆ ಬಂದು ಇನ್ನೊಂದು ಬದಿಗೆ ಮಾಡಿ."
        "pelvic_tilt" -> "ಸೊಂಟವನ್ನು ಸಣ್ಣ ಆರಾಮದಾಯಕ ವ್ಯಾಪ್ತಿಯಲ್ಲಿ ನಿಧಾನವಾಗಿ ಮುಂದೆ ಮತ್ತು ಹಿಂದೆ ತಿರುಗಿಸಿ."
        "seated_mobility" -> "ನೇರವಾಗಿ ಕುಳಿತು ಮೇಲಿನ ಮತ್ತು ಕೆಳಗಿನ ಬೆನ್ನನ್ನು ಸಣ್ಣ ಆರಾಮದಾಯಕ ವ್ಯಾಪ್ತಿಯಲ್ಲಿ ಚಲಿಸಿ."
        else -> "ನೇರವಾಗಿ ನಿಂತು ಬೆನ್ನನ್ನು ಸಣ್ಣ ಆರಾಮದಾಯಕ ವ್ಯಾಪ್ತಿಯಲ್ಲಿ ನಿಧಾನವಾಗಿ ವಿಸ್ತರಿಸಿ ಹಿಂದಿರುಗಿ."
    }
    CoachLanguage.MALAYALAM -> when (exercise.id) {
        "chin_tuck" -> "താഴേക്ക് നോക്കാതെ താടി പതുക്കെ നേരെ പിന്നിലേക്ക് കൊണ്ടുപോകുക."
        "neck_side" -> "തല പതുക്കെ ഒരു വശത്തേക്ക് ചായിച്ച് മധ്യത്തിലേക്ക് കൊണ്ടുവന്ന് മറുവശത്തേക്ക് ചെയ്യുക."
        "neck_rotation" -> "ബലം പ്രയോഗിക്കാതെ തല പതുക്കെ ഇടത്തേക്കും വലത്തേക്കും തിരിക്കുക."
        "shoulder_roll" -> "രണ്ട് തോളുകളും പതുക്കെ പിന്നിലേക്ക് വൃത്താകാരമായി ചുറ്റിക്കുക."
        "shoulder_squeeze" -> "തോളിലെ ബ്ലേഡുകൾ പതുക്കെ പിന്നിലേക്കും താഴേക്കും കൊണ്ടുവന്ന് കുറച്ച് നേരം പിടിച്ച് ശേഷം ശാന്തമാക്കുക."
        "arm_circle" -> "തോളുകൾ ശാന്തമാക്കി കൈകൾ ചെറിയ നിയന്ത്രിത വൃത്തങ്ങളിൽ ചുറ്റിക്കുക."
        "thoracic_extension" -> "നെഞ്ച് പതുക്കെ തുറന്ന് മുകളിലെ പുറം ബലം പ്രയോഗിക്കാതെ അല്പം നീട്ടുക."
        "cat_cow" -> "സുഖകരമായ പരിധിയിൽ പുറം പതുക്കെ വളച്ച് പിന്നീട് തുറന്ന നിലയിലേക്ക് കൊണ്ടുവരുക."
        "upper_rotation" -> "മുകളിലെ ശരീരം ഒരു വശത്തേക്ക് പതുക്കെ തിരിച്ച് മധ്യത്തിലേക്ക് വന്ന് മറുവശത്തേക്ക് ചെയ്യുക."
        "pelvic_tilt" -> "പെൽവിസ് ചെറിയ സുഖകരമായ പരിധിയിൽ പതുക്കെ മുന്നോട്ടും പിന്നോട്ടും ചായ്ക്കുക."
        "seated_mobility" -> "നേരെ ഇരുന്ന് മുകളിലെയും താഴെയുമുള്ള പുറം ചെറിയ സുഖകരമായ പരിധിയിൽ ചലിപ്പിക്കുക."
        else -> "നേരെ നിന്ന് പുറം ചെറിയ സുഖകരമായ പരിധിയിൽ പതുക്കെ നീട്ടി തിരിച്ചുവരുക."
    }
}

@Composable
private fun ExerciseLibraryOverlay(
    language: CoachLanguage,
    name: String,
    recommendedExercise: String,
    onClose: () -> Unit,
    onStartExercise: (ExerciseItem) -> Unit
) {
    var selectedCategory by remember { mutableStateOf(ExerciseCategory.HEAD_NECK) }
    val recommended = exerciseCatalog.firstOrNull { it.name == recommendedExercise }
    val visibleExercises = exerciseCatalog.filter { it.category == selectedCategory }

    Box(Modifier.fillMaxSize().background(themedColor(0xFF050A14))) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("EXERCISE LIBRARY", fontSize = 25.sp, fontWeight = FontWeight.ExtraBold, color = themedWhite())
                    Text("Animated guidance for $name", fontSize = 11.sp, color = themedColor(0xFF94A3B8))
                }
                Text("×", Modifier.clickable(onClick = onClose).padding(8.dp), fontSize = 28.sp, color = themedWhite())
            }

            Spacer(Modifier.height(14.dp))
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(themedColor(0xFF111B30))) {
                Column(Modifier.padding(15.dp)) {
                    Text("SMART RECOMMENDATION", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = themedColor(0xFFA78BFA))
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = recommended?.name ?: "Choose an exercise",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = themedWhite()
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = "Based on your current posture data.",
                        fontSize = 10.sp,
                        color = themedColor(0xFF94A3B8)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Text("TARGET AREA", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = themedColor(0xFF94A3B8))
            Spacer(Modifier.height(7.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(ExerciseCategory.HEAD_NECK, ExerciseCategory.SHOULDERS).forEach { category ->
                    Card(
                        Modifier.weight(1f).clickable { selectedCategory = category },
                        shape = RoundedCornerShape(15.dp),
                        colors = CardDefaults.cardColors(if (selectedCategory == category) themedColor(0xFF5B21B6) else themedColor(0xFF121D31))
                    ) {
                        Column(Modifier.fillMaxWidth().padding(11.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(category.icon, fontSize = 16.sp)
                            Text(category.title, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = themedWhite(), textAlign = TextAlign.Center)
                        }
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(ExerciseCategory.UPPER_SPINE, ExerciseCategory.LOWER_SPINE).forEach { category ->
                    Card(
                        Modifier.weight(1f).clickable { selectedCategory = category },
                        shape = RoundedCornerShape(15.dp),
                        colors = CardDefaults.cardColors(if (selectedCategory == category) themedColor(0xFF5B21B6) else themedColor(0xFF121D31))
                    ) {
                        Column(Modifier.fillMaxWidth().padding(11.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(category.icon, fontSize = 16.sp)
                            Text(category.title, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = themedWhite(), textAlign = TextAlign.Center)
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            visibleExercises.forEach { exercise ->
                ExerciseLibraryCard(
                    exercise = exercise,
                    language = language,
                    recommended = exercise.name == recommendedExercise,
                    onClick = { onStartExercise(exercise) }
                )
                Spacer(Modifier.height(9.dp))
            }

            Spacer(Modifier.height(4.dp))
            Text(
                "Wellness guidance only. Keep movements gentle, breathe normally, and stop if you feel pain or discomfort.",
                fontSize = 9.sp,
                color = themedColor(0xFF7F8EA3)
            )
            Spacer(Modifier.height(18.dp))
        }
    }
}

@Composable
private fun ExerciseLibraryCard(
    exercise: ExerciseItem,
    language: CoachLanguage,
    recommended: Boolean,
    onClick: () -> Unit
) {
    Card(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(themedColor(0xFF0F192B))
    ) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(if (recommended) themedColor(0xFF5B21B6).copy(alpha = 0.22f) else themedColor(0xFF18253A)),
                contentAlignment = Alignment.Center
            ) {
                Text(exercise.category.icon, fontSize = 22.sp)
            }
            Spacer(Modifier.size(11.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(translatedExerciseName(language, exercise), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = themedWhite())
                    if (recommended) {
                        Spacer(Modifier.widthIn(0.dp, 7.dp))
                        Text("SMART", fontSize = 7.sp, fontWeight = FontWeight.ExtraBold, color = themedColor(0xFFC4B5FD))
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(exercise.subtitle, fontSize = 9.sp, color = themedColor(0xFF94A3B8))
                Spacer(Modifier.height(4.dp))
                Text("${exercise.target} ${exercise.unit}", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = themedColor(0xFF4ADE80))
            }
            Text("▶", fontSize = 18.sp, color = themedColor(0xFFA78BFA))
        }
    }
}

@Composable
private fun AnimatedExerciseVisualizer(
    animation: ExerciseAnimation,
    phase: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier.clip(RoundedCornerShape(24.dp)).background(themedColor(0xFF0A1423))) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val scale = min(size.width / 260f, size.height / 260f)
        val p = phase.coerceIn(0f, 1f)
        val wave = kotlin.math.sin(p * Math.PI).toFloat()
        val dir = kotlin.math.sin(p * Math.PI * 2.0).toFloat()
        val accent = themedColor(0xFF8B5CF6)
        val soft = accent.copy(alpha = 0.18f)

        fun joint(x: Float, y: Float, r: Float = 8f) {
            drawCircle(soft, r * 2.2f, Offset(x, y))
            drawCircle(accent, r, Offset(x, y))
        }

        when (animation) {
            ExerciseAnimation.CHIN_TUCK -> {
                val headX = cx + (dir * 13f * scale)
                val neckX = cx + (dir * 2f * scale)
                drawCircle(themedColor(0xFF30445E), 24f * scale, Offset(headX, 60f * scale))
                drawLine(themedWhite().copy(alpha = 0.75f), Offset(neckX, 84f * scale), Offset(cx, 145f * scale), 10f * scale, cap = StrokeCap.Round)
                drawLine(themedWhite().copy(alpha = 0.75f), Offset(cx, 145f * scale), Offset(cx - 42f * scale, 200f * scale), 10f * scale, cap = StrokeCap.Round)
                drawLine(themedWhite().copy(alpha = 0.75f), Offset(cx, 145f * scale), Offset(cx + 42f * scale, 200f * scale), 10f * scale, cap = StrokeCap.Round)
                joint(headX, 60f * scale)
            }
            ExerciseAnimation.NECK_SIDE -> {
                val headX = cx + dir * 24f * scale
                drawLine(themedWhite().copy(alpha = 0.8f), Offset(cx, 82f * scale), Offset(cx, 155f * scale), 10f * scale, cap = StrokeCap.Round)
                drawLine(themedWhite().copy(alpha = 0.8f), Offset(cx, 155f * scale), Offset(cx - 42f * scale, 205f * scale), 10f * scale, cap = StrokeCap.Round)
                drawLine(themedWhite().copy(alpha = 0.8f), Offset(cx, 155f * scale), Offset(cx + 42f * scale, 205f * scale), 10f * scale, cap = StrokeCap.Round)
                drawCircle(themedColor(0xFF30445E), 24f * scale, Offset(headX, 58f * scale + wave * 7f * scale))
                drawArc(accent, 190f, 160f, false, Offset(cx - 50f * scale, 18f * scale), size = androidx.compose.ui.geometry.Size(100f * scale, 80f * scale), style = Stroke(width = 4f * scale))
            }
            ExerciseAnimation.NECK_ROTATION -> {
                drawLine(themedWhite().copy(alpha = 0.8f), Offset(cx, 84f * scale), Offset(cx, 155f * scale), 10f * scale, cap = StrokeCap.Round)
                drawLine(themedWhite().copy(alpha = 0.8f), Offset(cx, 155f * scale), Offset(cx - 42f * scale, 205f * scale), 10f * scale, cap = StrokeCap.Round)
                drawLine(themedWhite().copy(alpha = 0.8f), Offset(cx, 155f * scale), Offset(cx + 42f * scale, 205f * scale), 10f * scale, cap = StrokeCap.Round)
                drawCircle(themedColor(0xFF30445E), 24f * scale, Offset(cx, 58f * scale))
                drawArc(accent, -55f + dir * 35f, 110f, false, Offset(cx - 55f * scale, 20f * scale), size = androidx.compose.ui.geometry.Size(110f * scale, 80f * scale), style = Stroke(width = 4f * scale))
            }
            ExerciseAnimation.SHOULDER_ROLL -> {
                drawCircle(themedColor(0xFF30445E), 22f * scale, Offset(cx, 42f * scale))
                drawLine(themedWhite().copy(alpha = 0.75f), Offset(cx, 65f * scale), Offset(cx, 145f * scale), 11f * scale, cap = StrokeCap.Round)
                val sy = 92f * scale + wave * 16f * scale
                drawLine(themedWhite().copy(alpha = 0.75f), Offset(cx, 88f * scale), Offset(cx - 52f * scale, sy), 10f * scale, cap = StrokeCap.Round)
                drawLine(themedWhite().copy(alpha = 0.75f), Offset(cx, 88f * scale), Offset(cx + 52f * scale, sy), 10f * scale, cap = StrokeCap.Round)
                drawArc(accent, 210f, 210f, false, Offset(cx - 86f * scale, 55f * scale), size = androidx.compose.ui.geometry.Size(172f * scale, 120f * scale), style = Stroke(width = 5f * scale))
            }
            ExerciseAnimation.SHOULDER_SQUEEZE -> {
                drawCircle(themedColor(0xFF30445E), 22f * scale, Offset(cx, 42f * scale))
                drawLine(themedWhite().copy(alpha = 0.75f), Offset(cx, 65f * scale), Offset(cx, 150f * scale), 11f * scale, cap = StrokeCap.Round)
                val inward = 55f - wave * 18f
                drawLine(themedWhite().copy(alpha = 0.75f), Offset(cx, 90f * scale), Offset(cx - inward * scale, 118f * scale), 10f * scale, cap = StrokeCap.Round)
                drawLine(themedWhite().copy(alpha = 0.75f), Offset(cx, 90f * scale), Offset(cx + inward * scale, 118f * scale), 10f * scale, cap = StrokeCap.Round)
                joint(cx - inward * scale, 118f * scale)
                joint(cx + inward * scale, 118f * scale)
            }
            ExerciseAnimation.ARM_CIRCLE -> {
                drawCircle(themedColor(0xFF30445E), 22f * scale, Offset(cx, 42f * scale))
                drawLine(themedWhite().copy(alpha = 0.75f), Offset(cx, 65f * scale), Offset(cx, 150f * scale), 11f * scale, cap = StrokeCap.Round)
                val angle = dir * 0.65f
                val ax = kotlin.math.cos(angle.toDouble()).toFloat() * 72f * scale
                val ay = kotlin.math.sin(angle.toDouble()).toFloat() * 60f * scale
                drawLine(themedWhite().copy(alpha = 0.75f), Offset(cx, 88f * scale), Offset(cx - ax, 88f * scale - ay), 10f * scale, cap = StrokeCap.Round)
                drawLine(themedWhite().copy(alpha = 0.75f), Offset(cx, 88f * scale), Offset(cx + ax, 88f * scale - ay), 10f * scale, cap = StrokeCap.Round)
                drawCircle(accent.copy(alpha = 0.15f), 62f * scale, Offset(cx, 95f * scale))
            }
            ExerciseAnimation.THORACIC_EXTENSION -> {
                drawCircle(themedColor(0xFF30445E), 22f * scale, Offset(cx + wave * 18f * scale, 42f * scale))
                drawLine(themedWhite().copy(alpha = 0.75f), Offset(cx, 65f * scale), Offset(cx - wave * 15f * scale, 150f * scale), 11f * scale, cap = StrokeCap.Round)
                drawLine(themedWhite().copy(alpha = 0.75f), Offset(cx, 92f * scale), Offset(cx - 58f * scale - wave * 16f * scale, 130f * scale), 10f * scale, cap = StrokeCap.Round)
                drawLine(themedWhite().copy(alpha = 0.75f), Offset(cx, 92f * scale), Offset(cx + 58f * scale + wave * 16f * scale, 130f * scale), 10f * scale, cap = StrokeCap.Round)
                drawArc(accent, 200f, 140f, false, Offset(cx - 75f * scale, 30f * scale), size = androidx.compose.ui.geometry.Size(150f * scale, 180f * scale), style = Stroke(width = 4f * scale))
            }
            ExerciseAnimation.CAT_COW -> {
                val arch = wave * 28f * scale
                drawCircle(themedColor(0xFF30445E), 20f * scale, Offset(cx - 68f * scale, 110f * scale - arch))
                val spine = Path().apply {
                    moveTo(cx - 58f * scale, 120f * scale - arch)
                    lineTo(cx - 10f * scale, 145f * scale + arch)
                    lineTo(cx + 42f * scale, 128f * scale - arch)
                    lineTo(cx + 72f * scale, 145f * scale + arch)
                }
                drawPath(spine, themedWhite().copy(alpha = 0.82f), style = Stroke(width = 11f * scale, cap = StrokeCap.Round, join = StrokeJoin.Round))
                drawLine(themedWhite().copy(alpha = 0.8f), Offset(cx + 42f * scale, 128f * scale - arch), Offset(cx + 63f * scale, 195f * scale), 10f * scale, cap = StrokeCap.Round)
                drawLine(themedWhite().copy(alpha = 0.8f), Offset(cx - 10f * scale, 145f * scale + arch), Offset(cx - 18f * scale, 205f * scale), 10f * scale, cap = StrokeCap.Round)
            }
            ExerciseAnimation.UPPER_ROTATION -> {
                val torsoX = dir * 20f * scale
                drawCircle(themedColor(0xFF30445E), 21f * scale, Offset(cx + torsoX, 45f * scale))
                drawLine(themedWhite().copy(alpha = 0.8f), Offset(cx, 68f * scale), Offset(cx + torsoX, 145f * scale), 11f * scale, cap = StrokeCap.Round)
                drawLine(themedWhite().copy(alpha = 0.75f), Offset(cx + torsoX, 95f * scale), Offset(cx + torsoX - 60f * scale, 125f * scale), 10f * scale, cap = StrokeCap.Round)
                drawLine(themedWhite().copy(alpha = 0.75f), Offset(cx + torsoX, 95f * scale), Offset(cx + torsoX + 60f * scale, 125f * scale), 10f * scale, cap = StrokeCap.Round)
                drawLine(themedWhite().copy(alpha = 0.75f), Offset(cx + torsoX, 145f * scale), Offset(cx - 40f * scale, 205f * scale), 10f * scale, cap = StrokeCap.Round)
                drawLine(themedWhite().copy(alpha = 0.75f), Offset(cx + torsoX, 145f * scale), Offset(cx + 40f * scale, 205f * scale), 10f * scale, cap = StrokeCap.Round)
                drawArc(accent, -35f + dir * 40f, 70f, false, Offset(cx - 85f * scale, 65f * scale), size = androidx.compose.ui.geometry.Size(170f * scale, 120f * scale), style = Stroke(width = 4f * scale))
            }
            ExerciseAnimation.PELVIC_TILT -> {
                val pelvisY = 128f * scale + dir * 12f * scale
                drawCircle(themedColor(0xFF30445E), 21f * scale, Offset(cx, 42f * scale))
                drawLine(themedWhite().copy(alpha = 0.78f), Offset(cx, 65f * scale), Offset(cx, pelvisY), 11f * scale, cap = StrokeCap.Round)
                drawLine(themedWhite().copy(alpha = 0.78f), Offset(cx, pelvisY), Offset(cx - 44f * scale, 205f * scale), 11f * scale, cap = StrokeCap.Round)
                drawLine(themedWhite().copy(alpha = 0.78f), Offset(cx, pelvisY), Offset(cx + 44f * scale, 205f * scale), 11f * scale, cap = StrokeCap.Round)
                drawArc(accent, 200f, 140f, false, Offset(cx - 65f * scale, 85f * scale), size = androidx.compose.ui.geometry.Size(130f * scale, 95f * scale), style = Stroke(width = 5f * scale))
            }
            ExerciseAnimation.SEATED_MOBILITY -> {
                val shift = dir * 18f * scale
                drawCircle(themedColor(0xFF30445E), 21f * scale, Offset(cx + shift, 48f * scale))
                drawLine(themedWhite().copy(alpha = 0.8f), Offset(cx + shift, 72f * scale), Offset(cx + shift, 146f * scale), 11f * scale, cap = StrokeCap.Round)
                drawLine(themedWhite().copy(alpha = 0.75f), Offset(cx + shift, 92f * scale), Offset(cx - 56f * scale + shift, 118f * scale), 10f * scale, cap = StrokeCap.Round)
                drawLine(themedWhite().copy(alpha = 0.75f), Offset(cx + shift, 92f * scale), Offset(cx + 56f * scale + shift, 118f * scale), 10f * scale, cap = StrokeCap.Round)
                drawLine(themedWhite().copy(alpha = 0.78f), Offset(cx + shift, 146f * scale), Offset(cx - 48f * scale, 198f * scale), 10f * scale, cap = StrokeCap.Round)
                drawLine(themedWhite().copy(alpha = 0.78f), Offset(cx + shift, 146f * scale), Offset(cx + 48f * scale, 198f * scale), 10f * scale, cap = StrokeCap.Round)
                drawArc(accent, 190f + dir * 28f, 160f, false, Offset(cx - 82f * scale, 62f * scale), size = androidx.compose.ui.geometry.Size(164f * scale, 120f * scale), style = Stroke(width = 4f * scale))
            }
            ExerciseAnimation.STANDING_EXTENSION -> {
                val lean = wave * 18f * scale
                drawCircle(themedColor(0xFF30445E), 21f * scale, Offset(cx + lean, 42f * scale))
                drawLine(themedWhite().copy(alpha = 0.8f), Offset(cx, 66f * scale), Offset(cx - lean, 148f * scale), 11f * scale, cap = StrokeCap.Round)
                drawLine(themedWhite().copy(alpha = 0.75f), Offset(cx - lean, 92f * scale), Offset(cx - 58f * scale - lean, 122f * scale), 10f * scale, cap = StrokeCap.Round)
                drawLine(themedWhite().copy(alpha = 0.75f), Offset(cx - lean, 92f * scale), Offset(cx + 58f * scale - lean, 122f * scale), 10f * scale, cap = StrokeCap.Round)
                drawLine(themedWhite().copy(alpha = 0.78f), Offset(cx - lean, 148f * scale), Offset(cx - 42f * scale, 205f * scale), 10f * scale, cap = StrokeCap.Round)
                drawLine(themedWhite().copy(alpha = 0.78f), Offset(cx - lean, 148f * scale), Offset(cx + 42f * scale, 205f * scale), 10f * scale, cap = StrokeCap.Round)
                drawArc(accent, 200f, 140f, false, Offset(cx - 74f * scale, 24f * scale), size = androidx.compose.ui.geometry.Size(148f * scale, 190f * scale), style = Stroke(width = 4f * scale))
            }
        }
    }
}

@Composable
private fun GuidedExerciseOverlay(
    exercise: ExerciseItem,
    language: CoachLanguage,
    name: String,
    onBack: () -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    var count by remember { mutableIntStateOf(0) }
    var running by remember { mutableStateOf(false) }
    var phase by remember { mutableStateOf(0f) }
    val instruction = localizedExerciseInstruction(language, exercise)
    val translatedName = translatedExerciseName(language, exercise)

    LaunchedEffect(running) {
        while (running && count < exercise.target) {
            phase = if (phase < 0.5f) 1f else 0f
            delay(900)
            count++
        }
        if (count >= exercise.target) {
            running = false
            val done = when (language) {
                CoachLanguage.ENGLISH -> "Well done, $name! ${exercise.name} is complete."
                CoachLanguage.TAMIL -> "அருமை $name! ${translatedName} பயிற்சி முடிந்தது."
                CoachLanguage.TELUGU -> "చాలా బాగుంది $name! $translatedName పూర్తైంది."
                CoachLanguage.HINDI -> "बहुत बढ़िया $name! $translatedName पूरा हुआ।"
                CoachLanguage.KANNADA -> "ತುಂಬಾ ಚೆನ್ನಾಗಿದೆ $name! $translatedName ಪೂರ್ಣಗೊಂಡಿದೆ."
                CoachLanguage.MALAYALAM -> "വളരെ നല്ലത് $name! $translatedName പൂർത്തിയായി."
            }
            speakInLanguage(context, language, done)
        }
    }

    val animatedPhase by animateFloatAsState(
        targetValue = phase,
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "exercisePhase"
    )

    Box(Modifier.fillMaxSize().background(themedColor(0xFF040811))) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("GUIDED EXERCISE", fontSize = 23.sp, fontWeight = FontWeight.ExtraBold, color = themedWhite())
                Text("×", Modifier.clickable(onClick = onClose).padding(8.dp), fontSize = 28.sp, color = themedWhite())
            }

            Spacer(Modifier.height(8.dp))
            Text(exercise.category.title, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = themedColor(0xFFA78BFA))
            Text(translatedName, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold, color = themedWhite(), textAlign = TextAlign.Center)
            Text(exercise.subtitle, fontSize = 10.sp, color = themedColor(0xFF94A3B8), textAlign = TextAlign.Center)

            Spacer(Modifier.height(14.dp))
            AnimatedExerciseVisualizer(
                animation = exercise.animation,
                phase = animatedPhase,
                modifier = Modifier.fillMaxWidth().height(290.dp)
            )

            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Card(Modifier.weight(1f), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(themedColor(0xFF101A2C))) {
                    Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("PROGRESS", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = themedColor(0xFF7F8EA3))
                        Spacer(Modifier.height(2.dp))
                        Text("$count / ${exercise.target}", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = themedWhite())
                    }
                }
                Card(Modifier.weight(1f), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(themedColor(0xFF101A2C))) {
                    Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("TYPE", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = themedColor(0xFF7F8EA3))
                        Spacer(Modifier.height(2.dp))
                        Text(exercise.unit.uppercase(), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = themedColor(0xFF4ADE80))
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(themedColor(0xFF101A2C))) {
                Column(Modifier.padding(14.dp)) {
                    Text("HOW TO MOVE", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = themedColor(0xFF94A3B8))
                    Spacer(Modifier.height(5.dp))
                    Text(instruction, fontSize = 12.sp, color = themedWhite(), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        if (!running && count >= exercise.target) count = 0
                        running = !running
                        speakInLanguage(context, language, instruction)
                    }
                ) { Text(if (running) "Pause" else if (count == 0) "Start" else "Resume") }
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = { count = 0; phase = 0f; running = false }
                ) { Text("Reset") }
            }

            Spacer(Modifier.height(8.dp))
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onBack
            ) { Text("← Exercise Library") }

            Spacer(Modifier.height(10.dp))
            Text(exercise.caution, fontSize = 9.sp, color = themedColor(0xFF7F8EA3), textAlign = TextAlign.Center)
            Spacer(Modifier.height(18.dp))
        }
    }
}

// ============================================================
// VOICE + SPEECH HELPERS
// ============================================================

private var postureSpeechRecognizer: SpeechRecognizer? = null
private var postureTextToSpeech: TextToSpeech? = null

@SuppressLint("MissingPermission", "NewApi")
private fun startPostureSpeechRecognizer(
    context: Context,
    onResult: (String) -> Unit,
    onListeningChanged: (Boolean) -> Unit
) {
    if (!SpeechRecognizer.isRecognitionAvailable(context)) {
        speechCommandListening = false
        onListeningChanged(false)
        return
    }

    postureSpeechRecognizer?.destroy()

    val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
    postureSpeechRecognizer = recognizer
    speechCommandListening = true

    recognizer.setRecognitionListener(object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            onListeningChanged(true)
        }

        override fun onBeginningOfSpeech() {
            onListeningChanged(true)
        }

        override fun onRmsChanged(rmsdB: Float) = Unit

        override fun onBufferReceived(buffer: ByteArray?) = Unit

        override fun onEndOfSpeech() {
            speechCommandListening = false
            onListeningChanged(false)
        }

        override fun onError(error: Int) {
            speechCommandListening = false
            onListeningChanged(false)
        }

        override fun onResults(results: Bundle?) {
            speechCommandListening = false
            onListeningChanged(false)

            val matches =
                results?.getStringArrayList(
                    SpeechRecognizer.RESULTS_RECOGNITION
                )

            val heard = matches?.firstOrNull().orEmpty()

            if (heard.isNotBlank()) {
                onResult(heard)
            }
        }

        override fun onPartialResults(partialResults: Bundle?) = Unit

        override fun onEvent(
            eventType: Int,
            params: Bundle?
        ) = Unit
    })

    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        putExtra(
            RecognizerIntent.EXTRA_LANGUAGE,
            Locale.getDefault()
        )
        putExtra(
            RecognizerIntent.EXTRA_PARTIAL_RESULTS,
            false
        )
    }

    recognizer.startListening(intent)
}

@SuppressLint("NewApi")
private fun stopPostureSpeechRecognizer() {
    speechCommandListening = false
    postureSpeechRecognizer?.stopListening()
    postureSpeechRecognizer?.destroy()
    postureSpeechRecognizer = null
}

@android.annotation.TargetApi(android.os.Build.VERSION_CODES.LOLLIPOP)
private fun ttsSpeakOnce(
    context: Context,
    text: String
) {
    postureTextToSpeech?.stop()
    postureTextToSpeech?.shutdown()

    postureTextToSpeech = TextToSpeech(
        context.applicationContext
    ) { status ->
        if (status == TextToSpeech.SUCCESS) {
            postureTextToSpeech?.language = Locale.getDefault()
            postureTextToSpeech?.speak(
                text,
                TextToSpeech.QUEUE_FLUSH,
                java.util.HashMap<String, String>()
            )
        }
    }
}

// ============================================================
// GET NUMBER
// ============================================================

fun getNumber(

    response: String,

    key: String

): Double? {

    return try {

        val pattern =
            "\"$key\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)"


        val match =
            Regex(pattern)
                .find(response)


        match
            ?.groupValues
            ?.get(1)
            ?.toDouble()

    } catch (_: Exception) {

        null
    }
}


// ============================================================
// GET BOOLEAN
// ============================================================

fun getBoolean(

    response: String,

    key: String

): Boolean? {

    return try {

        val pattern =
            "\"$key\"\\s*:\\s*(true|false)"


        val match =
            Regex(
                pattern,
                RegexOption.IGNORE_CASE
            )
                .find(response)


        match
            ?.groupValues
            ?.get(1)
            ?.equals(
                "true",
                ignoreCase = true
            )

    } catch (_: Exception) {

        null
    }
}


// ============================================================
// GET TEXT
// ============================================================

fun getText(

    response: String,

    key: String

): String? {

    return try {

        val pattern =
            "\"$key\"\\s*:\\s*\"([^\"]*)\""


        val match =
            Regex(pattern)
                .find(response)


        match
            ?.groupValues
            ?.get(1)

    } catch (_: Exception) {

        null
    }
}
