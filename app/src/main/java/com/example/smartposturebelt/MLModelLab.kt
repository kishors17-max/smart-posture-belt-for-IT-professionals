package com.example.smartposturebelt

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.max

private val MlBg = Color(0xFF07111F)
private val MlCard = Color(0xFF111F33)
private val MlText = Color(0xFFF1F5F9)
private val MlMuted = Color(0xFF9CAEC4)
private val MlCyan = Color(0xFF22D3EE)
private val MlPurple = Color(0xFFA78BFA)
private val MlGreen = Color(0xFF4ADE80)
private val MlOrange = Color(0xFFFB923C)

/**
 * Separate ML Model Lab UI. It consumes the existing live ESP32 values and does not
 * change the firmware, sensor wiring, endpoint, or the app's existing dashboard.
 * Model scores are deliberately not fabricated: evaluation becomes available after
 * trained models and labelled/recorded data are supplied.
 */
@Composable
fun MLModelLabScreen(
    connected: Boolean,
    upperAngle: Double,
    lowerAngle: Double,
    upperDifference: Double,
    lowerDifference: Double,
    goodPosture: Boolean,
    upperHistory: List<Double>,
    lowerHistory: List<Double>,
    onClose: () -> Unit
) {
    var selectedSection by remember { mutableIntStateOf(0) }
    BackHandler(onBack = onClose)

    val upperPeak = upperHistory.maxOfOrNull { abs(it) } ?: abs(upperAngle)
    val lowerPeak = lowerHistory.maxOfOrNull { abs(it) } ?: abs(lowerAngle)
    val maxDeviation = max(abs(upperDifference), abs(lowerDifference))
    val peakLabel = when {
        !connected -> "Waiting for ESP32 data"
        maxDeviation >= 20.0 -> "High deviation peak"
        maxDeviation >= 10.0 -> "Moderate deviation peak"
        else -> "Low deviation peak"
    }
    val peakColor = when {
        !connected -> MlMuted
        maxDeviation >= 20.0 -> Color(0xFFF87171)
        maxDeviation >= 10.0 -> MlOrange
        else -> MlGreen
    }

    Column(
        modifier = Modifier.fillMaxSize().background(MlBg)
            .verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("POSTUREIQ", color = MlCyan, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                Text("ML Model Lab", color = MlText, fontSize = 27.sp, fontWeight = FontWeight.Black)
                Text("Four-model research and live peak intelligence", color = MlMuted, fontSize = 11.sp)
            }
            Text("✕", color = MlText, fontSize = 23.sp, modifier = Modifier.clickable { onClose() }.padding(8.dp))
        }

        Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MlCard)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).background(if (connected) MlGreen else Color(0xFFF87171), CircleShape))
                    Spacer(Modifier.size(8.dp))
                    Text(if (connected) "ESP32 CONNECTED" else "ESP32 NOT CONNECTED", color = if (connected) MlGreen else Color(0xFFF87171), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Text("Live sensor snapshot", color = MlText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MlMetric("Upper angle", "${fmt(upperAngle)}°", MlCyan, Modifier.weight(1f))
                    MlMetric("Lower angle", "${fmt(lowerAngle)}°", MlPurple, Modifier.weight(1f))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MlMetric("Upper deviation", "${fmt(upperDifference)}°", MlOrange, Modifier.weight(1f))
                    MlMetric("Lower deviation", "${fmt(lowerDifference)}°", MlOrange, Modifier.weight(1f))
                }
                Text("Posture state: ${if (goodPosture) "GOOD" else "CORRECT POSTURE"}", color = if (goodPosture) MlGreen else MlOrange, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }

        Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MlCard)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("PEAK INTELLIGENCE", color = MlCyan, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                Text(peakLabel, color = peakColor, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                Text("Peak absolute angle in current session history", color = MlMuted, fontSize = 11.sp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MlMetric("Upper peak", "${fmt(upperPeak)}°", MlCyan, Modifier.weight(1f))
                    MlMetric("Lower peak", "${fmt(lowerPeak)}°", MlPurple, Modifier.weight(1f))
                }
                Text("Samples in history: ${max(upperHistory.size, lowerHistory.size)}", color = MlMuted, fontSize = 10.sp)
                Text("Peak levels are informational thresholds, not a medical assessment.", color = MlMuted, fontSize = 10.sp)
            }
        }

        Text("MODEL COMPARISON", color = MlCyan, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            MlSectionChip("Overview", selectedSection == 0, { selectedSection = 0 }, Modifier.weight(1f))
            MlSectionChip("Metrics", selectedSection == 1, { selectedSection = 1 }, Modifier.weight(1f))
            MlSectionChip("Peak logic", selectedSection == 2, { selectedSection = 2 }, Modifier.weight(1f))
            MlSectionChip("Charts", selectedSection == 3, { selectedSection = 3 }, Modifier.weight(1f))
        }

        when (selectedSection) {
            0 -> {
                MlModelCard("Random Forest", "Posture classification", "Trained in Python and saved as posture_rf_model.pkl. Android inference is not connected yet.", MlGreen)
                MlModelCard("Decision Tree", "Posture classification", "Preliminary test accuracy: 79.49%. This score uses a random row split and needs session-based validation.", MlPurple)
                MlModelCard("Logistic Regression", "Posture classification", "Preliminary test accuracy: 60.26%. This score uses a random row split and needs session-based validation.", MlCyan)
                MlModelCard("SVM", "Posture classification", "Preliminary test accuracy: 56.41%. This score uses a random row split and needs session-based validation.", MlOrange)
            }
            1 -> {
                MlModelCard("Random Forest", "Accuracy 82.05% · Macro-F1 0.798", "Best preliminary result among the four compared models. Not independent session validation.", MlGreen)
                MlModelCard("Decision Tree", "Accuracy 79.49% · Macro-F1 0.770", "Preliminary result from the same stratified random test split.", MlPurple)
                MlModelCard("Logistic Regression", "Accuracy 60.26% · Macro-F1 0.467", "Preliminary result from the same stratified random test split.", MlCyan)
                MlModelCard("SVM", "Accuracy 56.41% · Macro-F1 0.571", "Preliminary result from the same stratified random test split.", MlOrange)
            }
            2 -> {
                MlModelCard("Peak deviation", "Absolute deviation", "Tracks the largest absolute upper/lower angle in the current in-memory session history.", MlCyan)
                MlModelCard("Sudden change", "Change between samples", "Live sensor history is shown in the Charts tab. Thresholds should be calibrated from verified recordings.", MlPurple)
                MlModelCard("Recovery time", "Bad-to-good transition", "Uses the existing dashboard's recovery tracking; it is not a separate ML prediction yet.", MlGreen)
            }
            else -> {
                Text("LIVE SENSOR HISTORY", color = MlCyan, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                Text("Recent upper and lower angles", color = MlText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                if (upperHistory.isEmpty() && lowerHistory.isEmpty()) {
                    Text("Waiting for sensor samples from ESP32…", color = MlMuted, fontSize = 12.sp)
                } else {
                    MlLineChart("Upper angle", upperHistory.takeLast(120), MlCyan)
                    MlLineChart("Lower angle", lowerHistory.takeLast(120), MlPurple)
                }
                Spacer(Modifier.height(4.dp))
                Text("MODEL ACCURACY COMPARISON", color = MlCyan, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                Text("Preliminary random-split results", color = MlMuted, fontSize = 10.sp)
                MlBarChart("Random Forest", 82.05, MlGreen)
                MlBarChart("Decision Tree", 79.49, MlPurple)
                MlBarChart("Logistic Regression", 60.26, MlCyan)
                MlBarChart("SVM", 56.41, MlOrange)
                Text("Scores are experimental benchmarks. Randomly splitting neighbouring samples can overestimate generalization. The trained Python .pkl model is not running in Android yet.", color = MlMuted, fontSize = 10.sp, lineHeight = 14.sp)
            }
        }

        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF2B213A))) {
            Text(
                "Live charts use ESP32 readings. Model comparison charts use the recorded Python experiment. Live ML prediction on the phone is a separate integration step.",
                modifier = Modifier.padding(14.dp), color = Color(0xFFE9D5FF), fontSize = 11.sp
            )
        }
        Button(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text("Back to Smart Posture Belt") }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun MlLineChart(title: String, values: List<Double>, lineColor: Color) {
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF0A1728))) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, color = MlText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("${values.size} recent samples · degrees", color = MlMuted, fontSize = 9.sp)
            Canvas(Modifier.fillMaxWidth().height(120.dp)) {
                if (values.size >= 2) {
                    val minValue = values.minOrNull() ?: 0.0
                    val maxValue = values.maxOrNull() ?: 1.0
                    val range = (maxValue - minValue).takeIf { it > 0.001 } ?: 1.0
                    val left = 4f
                    val right = size.width - 4f
                    val top = 8f
                    val bottom = size.height - 8f
                    for (i in 0..3) {
                        val y = top + (bottom - top) * i / 3f
                        drawLine(MlMuted.copy(alpha = 0.18f), Offset(left, y), Offset(right, y), 1f)
                    }
                    for (i in 0 until values.lastIndex) {
                        val x1 = left + (right - left) * i / (values.size - 1).toFloat()
                        val x2 = left + (right - left) * (i + 1) / (values.size - 1).toFloat()
                        val y1 = bottom - ((values[i] - minValue) / range).toFloat() * (bottom - top)
                        val y2 = bottom - ((values[i + 1] - minValue) / range).toFloat() * (bottom - top)
                        drawLine(lineColor, Offset(x1, y1), Offset(x2, y2), 2.5f)
                    }
                }
            }
            if (values.isNotEmpty()) {
                Text("Latest: ${fmt(values.last())}°  ·  Min: ${fmt(values.minOrNull() ?: 0.0)}°  ·  Max: ${fmt(values.maxOrNull() ?: 0.0)}°", color = MlMuted, fontSize = 9.sp)
            }
        }
    }
}

@Composable
private fun MlBarChart(label: String, accuracy: Double, accent: Color) {
    Column(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = MlText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text("${fmt(accuracy)}%", color = accent, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
        }
        Box(Modifier.fillMaxWidth().height(9.dp).background(Color(0xFF26364D), RoundedCornerShape(50))) {
            Box(Modifier.fillMaxWidth((accuracy / 100.0).toFloat().coerceIn(0f, 1f)).height(9.dp).background(accent, RoundedCornerShape(50)))
        }
    }
}

@Composable
private fun MlMetric(label: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(15.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF0A1728))) {
        Column(Modifier.fillMaxWidth().padding(11.dp)) {
            Text(label, color = MlMuted, fontSize = 9.sp)
            Spacer(Modifier.height(4.dp))
            Text(value, color = accent, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun MlModelCard(name: String, task: String, description: String, accent: Color) {
    Card(shape = RoundedCornerShape(19.dp), colors = CardDefaults.cardColors(containerColor = MlCard)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.Top) {
            Box(Modifier.size(8.dp).padding(top = 3.dp).background(accent, CircleShape))
            Spacer(Modifier.size(10.dp))
            Column {
                Text(name, color = MlText, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                Text(task, color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(5.dp))
                Text(description, color = MlMuted, fontSize = 11.sp, lineHeight = 16.sp)
            }
        }
    }
}

@Composable
private fun MlSectionChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(modifier = modifier.clickable(onClick = onClick), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = if (selected) Color(0xFF164E63) else MlCard)) {
        Text(label, modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp, horizontal = 3.dp), textAlign = TextAlign.Center, color = if (selected) MlCyan else MlMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

private fun fmt(value: Double): String = String.format(java.util.Locale.US, "%.1f", value)

