package com.example.ui

import android.hardware.SensorManager
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.IslamicEmerald
import com.example.ui.theme.IslamicGold
import com.example.ui.theme.IslamicGoldBright
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun QiblaScreen(
    uiState: MainUiState,
    viewModel: MainViewModel
) {
    val compassState by viewModel.compassState.collectAsState()

    DisposableEffect(Unit) {
        viewModel.compassManager.startListening()
        onDispose {
            viewModel.compassManager.stopListening()
        }
    }

    val qiblaAngle = uiState.qiblaBearing
    val isFacing = compassState.isFacingQibla
    val needleAngle by animateFloatAsState(
        targetValue = compassState.needleAngleDegrees,
        animationSpec = tween(durationMillis = 150),
        label = "NeedleAngle"
    )

    val dialBorderColor by animateColorAsState(
        targetValue = if (isFacing) IslamicEmerald else IslamicGold,
        animationSpec = tween(durationMillis = 300),
        label = "DialColor"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("qibla_screen_scroll"),
        contentPadding = PaddingValues(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Qibla Degree & Status Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isFacing) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isFacing) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = IslamicEmerald,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Facing the Holy Kaaba! 🕋",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    } else {
                        Text(
                            text = "Face This Direction",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "${String.format("%.1f", qiblaAngle)}° from True North",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = "Current Heading: ${String.format("%.0f", compassState.azimuthDegrees)}°",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 2. Compass Dial Canvas
        item {
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(4.dp, dialBorderColor, CircleShape)
                    .testTag("qibla_compass_dial"),
                contentAlignment = Alignment.Center
            ) {
                // Background ticks on dial
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val radius = size.minDimension / 2f
                    val center = Offset(size.width / 2f, size.height / 2f)

                    for (i in 0 until 360 step 15) {
                        val angleRad = Math.toRadians((i - 90).toDouble())
                        val tickLen = if (i % 90 == 0) 18f else if (i % 30 == 0) 12f else 6f
                        val start = Offset(
                            (center.x + (radius - 10f) * cos(angleRad)).toFloat(),
                            (center.y + (radius - 10f) * sin(angleRad)).toFloat()
                        )
                        val end = Offset(
                            (center.x + (radius - 10f - tickLen) * cos(angleRad)).toFloat(),
                            (center.y + (radius - 10f - tickLen) * sin(angleRad)).toFloat()
                        )
                        drawLine(
                            color = if (i % 90 == 0) Color.Gray else Color.LightGray,
                            start = start,
                            end = end,
                            strokeWidth = if (i % 90 == 0) 3f else 1.5f
                        )
                    }
                }

                // Rotating Needle Pointer
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .rotate(needleAngle),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Kaaba icon pointing at the top of the needle
                        Surface(
                            shape = CircleShape,
                            color = if (isFacing) IslamicEmerald else IslamicGold,
                            modifier = Modifier
                                .padding(top = 16.dp)
                                .size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "🕋",
                                    fontSize = 22.sp
                                )
                            }
                        }

                        // Top pointer arrow
                        Icon(
                            imageVector = Icons.Default.Navigation,
                            contentDescription = "Qibla pointer",
                            tint = if (isFacing) IslamicEmerald else IslamicGoldBright,
                            modifier = Modifier.size(36.dp)
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        // South bottom tail
                        Box(
                            modifier = Modifier
                                .padding(bottom = 24.dp)
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        )
                    }
                }

                // Center pivot cap
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                ) {}
            }
        }

        // 3. Distance to Kaaba info card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🕋", fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Holy Kaaba, Makkah",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "21.4225° N, 39.8262° E",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }

                    Text(
                        text = "${String.format("%,d", uiState.distanceToKaabaKm.toLong())} km",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // 4. Sensor status & calibration instructions
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Compass Sensor & Calibration",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (!compassState.hasCompassSensor) {
                            "⚠️ Magnetic compass sensor is not detected on this device. You can face ${String.format("%.1f", qiblaAngle)}° clockwise from true North using a physical compass or landmarks."
                        } else {
                            "Keep your phone flat horizontally. If the direction seems inaccurate, wave your device in a figure-8 motion to calibrate the internal magnetic compass."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
