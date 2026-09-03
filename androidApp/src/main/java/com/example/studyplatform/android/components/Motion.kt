package com.example.studyplatform.android.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.studyplatform.android.theme.Background
import com.example.studyplatform.android.theme.Border
import com.example.studyplatform.android.theme.Surface

/**
 * The app's motion vocabulary.
 *
 * Every animation here is short and eased rather than sprung, and none of them delays an
 * interaction — a phone this app targets may be several generations old, and motion that
 * looks smooth on a flagship reads as lag on a budget device. The point is to explain
 * what changed, not to decorate.
 */
object Motion {
    const val QUICK = 160
    const val NORMAL = 260
    const val SLOW = 420

    /** Content arriving: rises slightly as it fades in. */
    fun enter(delayMillis: Int = 0): EnterTransition =
        fadeIn(tween(NORMAL, delayMillis, EaseOutCubic)) +
                slideInVertically(tween(NORMAL, delayMillis, EaseOutCubic)) { it / 8 }

    /** Content leaving: fades without moving, so nothing appears to fly off screen. */
    fun exit(): ExitTransition = fadeOut(tween(QUICK))

    /** Panels that open in place, e.g. an inline form. */
    fun expand(): EnterTransition =
        fadeIn(tween(NORMAL)) + expandVertically(tween(NORMAL, easing = EaseOutCubic))

    fun collapse(): ExitTransition =
        fadeOut(tween(QUICK)) + shrinkVertically(tween(QUICK, easing = EaseInCubic))
}

/**
 * Fades and lifts its content in on first composition.
 *
 * @param index position in a list — each item starts a beat after the one above it, so a
 *              screen resolves in a readable order instead of appearing all at once.
 */
@Composable
fun AnimatedEntry(
    index: Int = 0,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }

    val alpha by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        // Capped so a long list does not make the last row wait: past the eighth item
        // the stagger stops accumulating.
        animationSpec = tween(Motion.NORMAL, minOf(index, 8) * 45, EaseOutCubic),
        label = "entry-alpha"
    )
    val offset by animateFloatAsState(
        targetValue = if (shown) 0f else 18f,
        animationSpec = tween(Motion.NORMAL, minOf(index, 8) * 45, EaseOutCubic),
        label = "entry-offset"
    )

    Box(modifier.alpha(alpha).offset(y = offset.dp)) { content() }
}

/**
 * Presses in slightly when touched.
 *
 * Applied to cards that act as buttons. Without it a tap on a large card gives no
 * feedback at all until the next screen loads, which on a slow connection is long enough
 * to make people tap again.
 */
@Composable
fun Modifier.pressScale(interactionSource: MutableInteractionSource): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.975f else 1f,
        animationSpec = tween(Motion.QUICK, easing = EaseOutCubic),
        label = "press"
    )
    return this.scale(scale)
}

/**
 * A placeholder that sweeps while real content loads.
 *
 * Shown instead of a spinner where the shape of the result is already known: it says
 * "three cards are coming" rather than "something is happening", which makes a slow
 * connection feel like progress rather than a stall.
 */
@Composable
fun ShimmerBox(modifier: Modifier = Modifier, height: Int = 72) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val shift by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing)),
        label = "shimmer-shift"
    )

    Box(
        modifier
            .fillMaxWidth()
            .height(height.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(
                Brush.horizontalGradient(
                    colors = listOf(Border.copy(alpha = 0.35f), Surface, Border.copy(alpha = 0.35f)),
                    startX = shift * 600f,
                    endX = (shift + 1f) * 600f
                )
            )
    )
}

/** Several shimmer rows, for a list that has not arrived yet. */
@Composable
fun ShimmerList(count: Int = 4, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(count) { ShimmerBox() }
    }
}

/**
 * Centres content and stops it from stretching on a wide screen.
 *
 * Text set edge to edge across a tablet is genuinely hard to read — the eye loses the
 * line on the way back. This caps the measure and centres what is left.
 */
@Composable
fun CenteredContent(
    modifier: Modifier = Modifier,
    maxWidth: Int = 560,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier.widthIn(max = maxWidth.dp).fillMaxWidth(),
            horizontalAlignment = horizontalAlignment,
            content = content
        )
    }
}

/**
 * A one-line banner that slides down when it has something to say.
 *
 * Used for sync state and errors: it takes no room when there is nothing to report,
 * which matters on a screen already full of content.
 */
@Composable
fun StatusBanner(
    message: String?,
    background: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = message != null,
        enter = Motion.expand(),
        exit = Motion.collapse(),
        modifier = modifier
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.small)
                .background(background)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                message.orEmpty(),
                style = MaterialTheme.typography.labelMedium,
                color = contentColor
            )
        }
    }
}

/** Background used by every screen, kept in one place. */
val ScreenBackground: Color get() = Background
