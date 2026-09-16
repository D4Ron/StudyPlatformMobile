package com.example.studyplatform.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.studyplatform.ui.theme.Warning
import com.example.studyplatform.ui.theme.WarningLight
import com.example.studyplatform.data.Offline
import studyplatform.shared.generated.resources.*

/**
 * Says that what is on screen was downloaded earlier.
 *
 * Shown whenever a read fell back to the cache. Presenting a stale library as though it
 * were current is how an offline app quietly loses someone's trust: the student needs to
 * know why their new guide is missing, and that nothing is broken.
 */
@Composable
fun OfflineBanner(state: Offline<*>, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = state.fromCache,
        enter = Motion.expand(),
        exit = Motion.collapse(),
        modifier = modifier
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.small)
                .background(WarningLight)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Was Icons.Default.CloudOff. Compose Multiplatform withdrew both Material
            // icon artifacts after 1.7.3, so shared code defines its own — see AppIcons.
            // Decorative either way: the text carries the meaning.
            Icon(AppIcons.Info, null, Modifier.size(16.dp), tint = Warning)
            Spacer(Modifier.width(8.dp))
            Text(
                stringResource(Res.string.common_offline_banner),
                style = MaterialTheme.typography.labelMedium,
                color = Warning
            )
        }
    }
}

/**
 * The message for a read that found neither network nor cache.
 *
 * Distinct from "you have nothing here": one means the student has not made anything
 * yet, the other that this device has never downloaded it. Telling someone their empty
 * library is empty when they are simply offline is the more annoying of the two
 * mistakes.
 */
@Composable
fun OfflineEmpty(
    state: Offline<*>,
    emptyMessage: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    val message = state.error?.let {
        stringResource(Res.string.common_not_downloaded)
    } ?: emptyMessage

    Text(
        message,
        style = MaterialTheme.typography.bodyMedium,
        color = color,
        modifier = modifier
    )
}
