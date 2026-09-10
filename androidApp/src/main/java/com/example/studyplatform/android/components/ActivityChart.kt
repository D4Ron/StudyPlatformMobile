package com.example.studyplatform.android.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.studyplatform.android.theme.Border
import com.example.studyplatform.android.theme.Primary
import com.example.studyplatform.android.theme.PrimaryLight
import com.example.studyplatform.android.theme.TextMuted
import com.example.studyplatform.model.DayActivity
import tg.edunova.app.R

/**
 * Study minutes per day, as bars.
 *
 * <p>Empty days are drawn as empty bars rather than skipped. Omitting them would put
 * four studied days side by side and turn a sporadic fortnight into what looks like a
 * habit — the gaps are the part of this chart worth seeing.
 *
 * <p>Scaled against the busiest day in the window rather than a fixed ceiling, so the
 * shape stays readable whether someone studies twenty minutes a day or three hours.
 * The trade is that the bar heights mean nothing between two different windows, which
 * is why the totals are written out beside it.
 */
@Composable
fun ActivityChart(
    days: List<DayActivity>,
    modifier: Modifier = Modifier,
    height: Int = 96
) {
    if (days.isEmpty()) return

    val peak = days.maxOf { it.minutes }.coerceAtLeast(1)

    Row(
        modifier = modifier.fillMaxWidth().height(height.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        days.forEach { day ->
            val fraction by animateFloatAsState(
                targetValue = day.minutes.toFloat() / peak,
                animationSpec = tween(400),
                label = "bar-${day.date}"
            )

            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.BottomCenter
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        // A day with no study still gets a sliver, so the row reads as a
                        // continuous timeline rather than as missing data.
                        .fillMaxHeight(if (day.minutes == 0) 0.02f else fraction.coerceAtLeast(0.06f))
                        .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                        .background(if (day.minutes == 0) Border else Primary)
                )
            }
        }
    }
}

/** Minutes as a person would say them: "45m", or "2h 15m" once it passes an hour. */
@Composable
fun formatMinutes(minutes: Int): String =
    if (minutes >= 60) {
        stringResource(R.string.activity_hours_minutes, minutes / 60, minutes % 60)
    } else {
        stringResource(R.string.activity_minutes, minutes)
    }
