package com.example.studyplatform.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * The icons shared UI draws, defined here rather than depended on.
 *
 * Compose Multiplatform stopped publishing `material-icons-core` and
 * `material-icons-extended` after 1.7.3, so common code has no Material icon set at all —
 * the Android app still uses the androidx artifact, but nothing in `commonMain` can.
 * Pinning the discontinued 1.7.3 against Compose 1.11 would drag a stale runtime in
 * behind it, so the handful of glyphs shared screens need live here as plain vectors.
 *
 * Expect this to grow as screens move across. That is the cost of the icon set being
 * withdrawn, and it is a smaller cost than an abandoned dependency.
 */
object AppIcons {

    /**
     * A filled circle with an "i" cut out of it.
     *
     * The counter is cut with [PathFillType.EvenOdd] rather than drawn in a second
     * colour, so the glyph takes the `tint` like any other icon instead of assuming what
     * is behind it.
     */
    val Info: ImageVector by lazy {
        ImageVector.Builder(
            name = "Info",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.EvenOdd
            ) {
                // Disc.
                moveTo(12f, 2f)
                curveTo(6.477f, 2f, 2f, 6.477f, 2f, 12f)
                curveTo(2f, 17.523f, 6.477f, 22f, 12f, 22f)
                curveTo(17.523f, 22f, 22f, 17.523f, 22f, 12f)
                curveTo(22f, 6.477f, 17.523f, 2f, 12f, 2f)
                close()

                // Stem.
                moveTo(11f, 11f)
                horizontalLineTo(13f)
                verticalLineTo(17f)
                horizontalLineTo(11f)
                close()

                // Tittle.
                moveTo(11f, 7f)
                horizontalLineTo(13f)
                verticalLineTo(9f)
                horizontalLineTo(11f)
                close()
            }
        }.build()
    }
}
