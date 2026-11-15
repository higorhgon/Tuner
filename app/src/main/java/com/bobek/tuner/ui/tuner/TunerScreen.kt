/*
 * This file is part of Tuner.
 * Copyright (C) 2026 Philipp Bobek <philipp.bobek@mailbox.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Tuner is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.bobek.tuner.ui.tuner

import android.Manifest.permission.RECORD_AUDIO
import android.content.pm.PackageManager.PERMISSION_GRANTED
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.bobek.tuner.R
import com.bobek.tuner.domain.DetectedNote
import com.bobek.tuner.domain.displayName
import com.bobek.tuner.domain.noteDisplayName
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** Chromatic note row, in the order shown at the bottom of the screen. */
private val NOTE_ROW = listOf("A", "A#", "B", "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#")

/** A note within this many cents of its target counts as in tune (green border). */
internal const val IN_TUNE_CENTS = 5

private const val MAX_CENTS = 50f
private const val MAX_NEEDLE_ANGLE_DEG = 48f
private const val HELD_ALPHA = 0.45f
private const val INACTIVE_ALPHA = 0.4f

private val InTuneBorderColor = Color(0xFF4CAF50)
private val InTuneBorderWidth = 6.dp

@Composable
@PreviewScreenSizes
fun TunerScreen(
    @PreviewParameter(TunerScreenViewModelProvider::class) viewModel: ITunerViewModel,
    onSettingsClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val state by viewModel.getTunerStateFlow().collectAsState()
    val referencePitch by viewModel.getReferencePitchFlow().collectAsState()
    val solfegeNotation by viewModel.getSolfegeNotationFlow().collectAsState()

    var showReferencePitchDialog by rememberSaveable { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.startListening()
    }

    DisposableEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, RECORD_AUDIO) == PERMISSION_GRANTED) {
            viewModel.startListening()
        }
        onDispose { viewModel.stopListening() }
    }

    val listening = state as? TunerState.Listening
    val note = listening?.note
    val isLive = listening?.isLive == true
    val inTune = isLive && note != null && abs(note.cents) < IN_TUNE_CENTS

    val borderColor by animateColorAsState(
        targetValue = if (inTune) InTuneBorderColor else Color.Transparent,
        animationSpec = tween(durationMillis = 150),
        label = "border"
    )

    Scaffold { padding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .border(InTuneBorderWidth, borderColor)
                .padding(padding)
                .padding(InTuneBorderWidth)
        ) {
            val isLandscape = maxWidth > maxHeight

            val header: @Composable () -> Unit = {
                HeaderRow(
                    inputLevel = listening?.inputLevel ?: 0f,
                    referencePitch = referencePitch,
                    onReferencePitchClick = { showReferencePitchDialog = true },
                    onSettingsClick = onSettingsClick
                )
            }
            val noteContent: @Composable (Boolean) -> Unit = { compact ->
                when (state) {
                    is TunerState.Idle -> IdleContent(
                        onGrantPermission = { permissionLauncher.launch(RECORD_AUDIO) },
                        compact = compact
                    )

                    is TunerState.Listening -> NoteHeading(
                        note = note,
                        isLive = isLive,
                        solfegeNotation = solfegeNotation,
                        compact = compact
                    )
                }
            }

            if (isLandscape) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        header()
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) { noteContent(true) }
                    }
                    Column(
                        modifier = Modifier
                            .weight(1.4f)
                            .fillMaxHeight()
                    ) {
                        NeedleGauge(
                            note = note,
                            isLive = isLive,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        )
                        NoteRow(
                            activeNote = note?.name,
                            solfegeNotation = solfegeNotation,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    header()
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 16.dp),
                        contentAlignment = Alignment.Center
                    ) { noteContent(false) }
                    NeedleGauge(
                        note = note,
                        isLive = isLive,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    )
                    NoteRow(
                        activeNote = note?.name,
                        solfegeNotation = solfegeNotation,
                        modifier = Modifier.padding(top = 16.dp, bottom = 24.dp)
                    )
                }
            }
        }
    }

    if (showReferencePitchDialog) {
        ReferencePitchDialog(
            viewModel = viewModel,
            onDismiss = { showReferencePitchDialog = false }
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun HeaderRow(
    inputLevel: Float,
    referencePitch: Int,
    onReferencePitchClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val settingsLabel = stringResource(R.string.settings)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 4.dp, top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_mic),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            modifier = Modifier.size(28.dp)
        )
        InputLevelBar(level = inputLevel, modifier = Modifier.padding(start = 4.dp))
        Spacer(modifier = Modifier.weight(1f))
        TextButton(onClick = onReferencePitchClick) {
            Text(
                text = stringResource(R.string.reference_pitch_display, referencePitch),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        TooltipBox(
            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Below),
            tooltip = { PlainTooltip { Text(settingsLabel) } },
            state = rememberTooltipState()
        ) {
            IconButton(onClick = onSettingsClick) {
                Icon(
                    painter = painterResource(R.drawable.ic_settings),
                    contentDescription = settingsLabel,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun InputLevelBar(level: Float, modifier: Modifier = Modifier) {
    val animatedLevel by animateFloatAsState(
        targetValue = level,
        animationSpec = tween(durationMillis = 80),
        label = "inputLevel"
    )
    val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
    val levelColor = MaterialTheme.colorScheme.tertiary
    Canvas(
        modifier = modifier
            .width(4.dp)
            .height(28.dp)
    ) {
        val radius = size.width / 2f
        drawLine(
            color = trackColor,
            start = Offset(radius, radius),
            end = Offset(radius, size.height - radius),
            strokeWidth = size.width,
            cap = StrokeCap.Round
        )
        if (animatedLevel > 0.01f) {
            val top = size.height - radius - (size.height - 2 * radius) * animatedLevel
            drawLine(
                color = levelColor,
                start = Offset(radius, top),
                end = Offset(radius, size.height - radius),
                strokeWidth = size.width,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun IdleContent(
    onGrantPermission: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    Column(
        modifier = modifier.padding(if (compact) 12.dp else 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.tuner_permission_rationale),
            style = if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = if (compact) 12.dp else 24.dp)
        )
        Button(onClick = onGrantPermission) {
            Text(stringResource(R.string.tuner_grant_permission))
        }
    }
}

/** Big note name with the octave as a subscript, and the frequency below it. */
@Composable
private fun NoteHeading(
    note: DetectedNote?,
    isLive: Boolean,
    solfegeNotation: Boolean,
    compact: Boolean
) {
    val noteColor = noteColor(note, isLive)
    val nameSize: TextUnit = if (compact) 72.sp else 112.sp
    val octaveSize: TextUnit = if (compact) 22.sp else 30.sp
    val minHeight: Dp = if (compact) 96.dp else 148.dp

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier.height(minHeight),
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = note?.displayName(solfegeNotation) ?: "–",
                fontSize = nameSize,
                fontWeight = FontWeight.Light,
                color = noteColor,
                maxLines = 1,
                style = TextStyle(lineHeight = nameSize)
            )
            if (note != null) {
                Text(
                    text = note.octave.toString(),
                    fontSize = octaveSize,
                    color = noteColor,
                    modifier = Modifier.padding(start = 2.dp, bottom = if (compact) 8.dp else 14.dp)
                )
            }
        }
        Text(
            text = if (note != null) "%.1f Hz".format(note.frequency) else stringResource(R.string.tuner_listening),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (isLive) 0.8f else 0.5f)
        )
    }
}

/**
 * Analog meter: ticks at −50, −25, 0, +25 and +50 cents on an arc, with a needle swinging from a
 * pivot at the bottom. A held (not live) note keeps the needle where it was, in a dimmed color.
 */
@Composable
private fun NeedleGauge(
    note: DetectedNote?,
    isLive: Boolean,
    modifier: Modifier = Modifier
) {
    val cents = note?.cents?.toFloat() ?: 0f
    val animatedCents by animateFloatAsState(
        targetValue = cents,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "needle"
    )
    val needleColor by animateColorAsState(
        targetValue = noteColor(note, isLive),
        label = "needleColor"
    )
    val tickColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
    val pivotColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    val ringColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(color = tickColor, fontSize = 26.sp)

    Canvas(modifier = modifier) {
        val ringRadius = 22.dp.toPx()
        val pivot = Offset(size.width / 2f, size.height - ringRadius - 4.dp.toPx())
        val labelMargin = 36.dp.toPx()
        val maxAngleRad = Math.toRadians(MAX_NEEDLE_ANGLE_DEG.toDouble()).toFloat()
        val labelAngleRad = Math.toRadians((MAX_NEEDLE_ANGLE_DEG + 7).toDouble()).toFloat()
        val radius = min(
            (pivot.y - 8.dp.toPx()),
            (size.width / 2f - labelMargin) / sin(labelAngleRad)
        ).coerceAtLeast(ringRadius * 2)

        fun pointAt(angleRad: Float, r: Float) = Offset(
            pivot.x + r * sin(angleRad),
            pivot.y - r * cos(angleRad)
        )

        // Ticks: outer and middle ones, and a double tick at the center.
        val tickLength = 22.dp.toPx()
        val tickStroke = 1.5.dp.toPx()
        listOf(-1f, -0.5f, 0.5f, 1f).forEach { fraction ->
            val angle = maxAngleRad * fraction
            val r = if (abs(fraction) == 1f) radius * 0.86f else radius
            drawLine(tickColor, pointAt(angle, r - tickLength), pointAt(angle, r), tickStroke)
        }
        val centerGap = 9.dp.toPx()
        listOf(-1, 1).forEach { side ->
            val x = pivot.x + side * centerGap
            drawLine(
                tickColor,
                Offset(x, pivot.y - radius),
                Offset(x, pivot.y - radius + tickLength),
                tickStroke
            )
        }

        // ♭ and ♯ labels beyond the outer ticks.
        listOf(-1 to "♭", 1 to "♯").forEach { (side, label) ->
            val layout = textMeasurer.measure(label, labelStyle)
            val anchor = pointAt(side * labelAngleRad, radius * 0.80f)
            drawText(
                layout,
                topLeft = Offset(
                    anchor.x - layout.size.width / 2f,
                    anchor.y - layout.size.height / 2f
                )
            )
        }

        drawNeedle(
            angleRad = maxAngleRad * (animatedCents / MAX_CENTS).coerceIn(-1f, 1f),
            pivot = pivot,
            length = radius * 0.97f,
            color = needleColor
        )

        drawCircle(ringColor, radius = ringRadius, center = pivot, style = Stroke(1.5.dp.toPx()))
        drawCircle(pivotColor, radius = 4.dp.toPx(), center = pivot)
    }
}

private fun DrawScope.drawNeedle(angleRad: Float, pivot: Offset, length: Float, color: Color) {
    val tip = Offset(pivot.x + length * sin(angleRad), pivot.y - length * cos(angleRad))
    drawLine(
        brush = Brush.linearGradient(
            colors = listOf(color, color.copy(alpha = 0.15f)),
            start = pivot,
            end = tip
        ),
        start = pivot,
        end = tip,
        strokeWidth = 3.dp.toPx(),
        cap = StrokeCap.Round
    )
}

/** All twelve note names, with the current one highlighted and marked underneath. */
@Composable
private fun NoteRow(
    activeNote: String?,
    solfegeNotation: Boolean,
    modifier: Modifier = Modifier
) {
    val activeColor = MaterialTheme.colorScheme.onSurface
    val inactiveColor = MaterialTheme.colorScheme.onSurface.copy(alpha = INACTIVE_ALPHA)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    ) {
        NOTE_ROW.forEach { name ->
            val active = name == activeNote
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = noteDisplayName(name, solfegeNotation),
                    fontSize = if (solfegeNotation) 13.sp else 18.sp,
                    color = if (active) activeColor else inactiveColor,
                    maxLines = 1,
                    softWrap = false
                )
                Box(
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .width(3.dp)
                        .height(16.dp)
                        .background(
                            color = if (active) activeColor else Color.Transparent,
                            shape = RoundedCornerShape(1.5.dp)
                        )
                )
            }
        }
    }
}

/** Live notes use the tuning colors; a held note and the empty state are greyed out. */
@Composable
private fun noteColor(note: DetectedNote?, isLive: Boolean): Color = when {
    note == null -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
    !isLive -> MaterialTheme.colorScheme.onSurface.copy(alpha = HELD_ALPHA)
    else -> centsColor(note.cents)
}

@Composable
private fun centsColor(cents: Int): Color = when {
    abs(cents) < IN_TUNE_CENTS -> MaterialTheme.colorScheme.primary
    abs(cents) < 20 -> MaterialTheme.colorScheme.tertiary
    else -> MaterialTheme.colorScheme.error
}

private class TunerScreenViewModelProvider : PreviewParameterProvider<ITunerViewModel> {
    override val values: Sequence<ITunerViewModel> = sequenceOf(
        ComposeTunerViewModel(TunerState.Idle),
        ComposeTunerViewModel(TunerState.Listening(null)),
        ComposeTunerViewModel(
            TunerState.Listening(
                DetectedNote(name = "A", octave = 4, frequency = 440.0f, cents = 0),
                inputLevel = 0.6f
            )
        ),
        ComposeTunerViewModel(
            TunerState.Listening(
                DetectedNote(name = "G#", octave = 4, frequency = 415.3f, cents = 15),
                inputLevel = 0.5f
            )
        ),
        ComposeTunerViewModel(
            TunerState.Listening(
                DetectedNote(name = "C", octave = 4, frequency = 257.5f, cents = -27),
                isLive = false
            )
        ),
    )

    override fun getDisplayName(index: Int): String? = when (index) {
        0 -> "Idle"
        1 -> "Listening - No Note"
        2 -> "Listening - In Tune"
        3 -> "Listening - Slightly Sharp"
        4 -> "Listening - Held Note"
        else -> null
    }
}
