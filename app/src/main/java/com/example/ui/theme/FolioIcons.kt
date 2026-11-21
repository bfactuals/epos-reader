package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Ultra-lightweight subset of Material 24dp vector icons (0 KB binary assets),
 * avoiding the ~9 MB material-icons-extended library to keep total APK size minimal.
 */
object FolioIcons {

    val LightMode: ImageVector by lazy {
        icon("LightMode") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 7f)
                curveTo(9.24f, 7f, 7f, 9.24f, 7f, 12f)
                reflectiveCurveToRelative(2.24f, 5f, 5f, 5f)
                reflectiveCurveToRelative(5f, -2.24f, 5f, -5f)
                reflectiveCurveTo(14.76f, 7f, 12f, 7f)
                close()
                moveTo(12f, 9f)
                curveToRelative(1.65f, 0f, 3f, 1.35f, 3f, 3f)
                reflectiveCurveToRelative(-1.35f, 3f, -3f, 3f)
                reflectiveCurveToRelative(-3f, -1.35f, -3f, -3f)
                reflectiveCurveTo(10.35f, 9f, 12f, 9f)
                close()
                moveTo(11f, 1f); horizontalLineToRelative(2f); verticalLineToRelative(3f); horizontalLineToRelative(-2f); close()
                moveTo(11f, 20f); horizontalLineToRelative(2f); verticalLineToRelative(3f); horizontalLineToRelative(-2f); close()
                moveTo(1f, 11f); horizontalLineToRelative(3f); verticalLineToRelative(2f); horizontalLineTo(1f); close()
                moveTo(20f, 11f); horizontalLineToRelative(3f); verticalLineToRelative(2f); horizontalLineToRelative(-3f); close()
                moveTo(4.22f, 5.64f); lineToRelative(1.42f, -1.42f); lineToRelative(2.12f, 2.12f); lineToRelative(-1.42f, 1.42f); close()
                moveTo(16.24f, 17.66f); lineToRelative(1.42f, -1.42f); lineToRelative(2.12f, 2.12f); lineToRelative(-1.42f, 1.42f); close()
                moveTo(16.24f, 6.34f); lineToRelative(2.12f, -2.12f); lineToRelative(1.42f, 1.42f); lineToRelative(-2.12f, 2.12f); close()
                moveTo(4.22f, 18.36f); lineToRelative(2.12f, -2.12f); lineToRelative(1.42f, 1.42f); lineToRelative(-2.12f, 2.12f); close()
            }
        }
    }

    val SepiaMode: ImageVector by lazy {
        icon("SepiaMode") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 3f)
                curveTo(7.03f, 3f, 3f, 7.03f, 3f, 12f)
                reflectiveCurveToRelative(4.03f, 9f, 9f, 9f)
                reflectiveCurveToRelative(9f, -4.03f, 9f, -9f)
                reflectiveCurveTo(16.97f, 3f, 12f, 3f)
                close()
                moveTo(12f, 19f)
                verticalLineTo(5f)
                curveToRelative(3.86f, 0f, 7f, 3.14f, 7f, 7f)
                reflectiveCurveTo(15.86f, 19f, 12f, 19f)
                close()
            }
        }
    }

    val DarkMode: ImageVector by lazy {
        icon("DarkMode") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(9.37f, 5.51f)
                curveTo(9.19f, 6.15f, 9.1f, 6.82f, 9.1f, 7.5f)
                curveToRelative(0f, 4.08f, 3.32f, 7.4f, 7.4f, 7.4f)
                curveToRelative(0.68f, 0f, 1.35f, -0.09f, 1.99f, -0.27f)
                curveTo(17.45f, 17.19f, 14.93f, 19f, 12f, 19f)
                curveToRelative(-3.86f, 0f, -7f, -3.14f, -7f, -7f)
                curveTo(5f, 9.07f, 6.81f, 6.55f, 9.37f, 5.51f)
                close()
                moveTo(12f, 3f)
                curveToRelative(-4.97f, 0f, -9f, 4.03f, -9f, 9f)
                reflectiveCurveToRelative(4.03f, 9f, 9f, 9f)
                reflectiveCurveToRelative(9f, -4.03f, 9f, -9f)
                curveToRelative(0f, -0.46f, -0.04f, -0.92f, -0.1f, -1.36f)
                curveToRelative(-0.98f, 1.37f, -2.58f, 2.26f, -4.4f, 2.26f)
                curveToRelative(-2.98f, 0f, -5.4f, -2.42f, -5.4f, -5.4f)
                curveToRelative(0f, -1.81f, 0.89f, -3.42f, 2.26f, -4.4f)
                curveTo(12.92f, 3.04f, 12.46f, 3f, 12f, 3f)
                close()
            }
        }
    }

    val OpenBook: ImageVector by lazy {
        icon("OpenBook") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(21f, 5f)
                curveToRelative(-1.11f, -0.35f, -2.33f, -0.5f, -3.5f, -0.5f)
                curveToRelative(-1.95f, 0f, -4.05f, 0.4f, -5.5f, 1.5f)
                curveToRelative(-1.45f, -1.1f, -3.55f, -1.5f, -5.5f, -1.5f)
                reflectiveCurveTo(2.45f, 4.9f, 1f, 6f)
                verticalLineToRelative(14.65f)
                curveToRelative(0f, 0.25f, 0.25f, 0.5f, 0.5f, 0.5f)
                curveToRelative(0.1f, 0f, 0.15f, -0.05f, 0.25f, -0.05f)
                curveTo(3.1f, 20.45f, 5.05f, 20f, 6.5f, 20f)
                curveToRelative(1.95f, 0f, 4.05f, 0.4f, 5.5f, 1.5f)
                curveToRelative(1.35f, -0.85f, 3.8f, -1.5f, 5.5f, -1.5f)
                curveToRelative(1.65f, 0f, 3.35f, 0.3f, 4.75f, 1.05f)
                curveToRelative(0.1f, 0.05f, 0.15f, 0.05f, 0.25f, 0.05f)
                curveToRelative(0.25f, 0f, 0.5f, -0.25f, 0.5f, -0.5f)
                verticalLineTo(6f)
                curveTo(22.4f, 5.55f, 21.75f, 5.25f, 21f, 5f)
                close()
                moveTo(21f, 18.5f)
                curveToRelative(-1.1f, -0.35f, -2.3f, -0.5f, -3.5f, -0.5f)
                curveToRelative(-1.7f, 0f, -4.15f, 0.65f, -5.5f, 1.5f)
                verticalLineTo(8f)
                curveToRelative(1.35f, -0.85f, 3.8f, -1.5f, 5.5f, -1.5f)
                curveToRelative(1.2f, 0f, 2.4f, 0.15f, 3.5f, 0.5f)
                verticalLineTo(18.5f)
                close()
            }
        }
    }

    val BookmarkFilled: ImageVector by lazy {
        icon("BookmarkFilled") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(17f, 3f); horizontalLineTo(7f)
                curveTo(5.9f, 3f, 5f, 3.9f, 5f, 5f); verticalLineToRelative(16f)
                lineToRelative(7f, -3f); lineToRelative(7f, 3f); verticalLineTo(5f)
                curveTo(19f, 3.9f, 18.1f, 3f, 17f, 3f); close()
            }
        }
    }

    val BookmarkOutline: ImageVector by lazy {
        icon("BookmarkOutline") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(17f, 3f); horizontalLineTo(7f)
                curveTo(5.9f, 3f, 5f, 3.9f, 5f, 5f); verticalLineToRelative(16f)
                lineToRelative(7f, -3f); lineToRelative(7f, 3f); verticalLineTo(5f)
                curveTo(19f, 3.9f, 18.1f, 3f, 17f, 3f); close()
                moveTo(17f, 18f); lineToRelative(-5f, -2.18f); lineTo(7f, 18f)
                verticalLineTo(5f); horizontalLineToRelative(10f); verticalLineTo(18f); close()
            }
        }
    }

    val NotebookList: ImageVector by lazy {
        icon("NotebookList") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(3f, 13f); horizontalLineToRelative(2f); verticalLineToRelative(-2f); horizontalLineTo(3f); verticalLineTo(13f); close()
                moveTo(3f, 17f); horizontalLineToRelative(2f); verticalLineToRelative(-2f); horizontalLineTo(3f); verticalLineTo(17f); close()
                moveTo(3f, 9f); horizontalLineToRelative(2f); verticalLineTo(7f); horizontalLineTo(3f); verticalLineTo(9f); close()
                moveTo(7f, 13f); horizontalLineToRelative(14f); verticalLineToRelative(-2f); horizontalLineTo(7f); verticalLineTo(13f); close()
                moveTo(7f, 17f); horizontalLineToRelative(14f); verticalLineToRelative(-2f); horizontalLineTo(7f); verticalLineTo(17f); close()
                moveTo(7f, 7f); verticalLineToRelative(2f); horizontalLineToRelative(14f); verticalLineTo(7f); horizontalLineTo(7f); close()
            }
        }
    }

    val Undo: ImageVector by lazy {
        icon("Undo") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12.5f, 8f)
                curveToRelative(-2.65f, 0f, -5.05f, 0.99f, -6.9f, 2.6f)
                lineTo(2f, 7f); verticalLineToRelative(9f); horizontalLineToRelative(9f)
                lineToRelative(-3.62f, -3.62f)
                curveToRelative(1.39f, -1.16f, 3.16f, -1.88f, 5.12f, -1.88f)
                curveToRelative(3.54f, 0f, 6.55f, 2.31f, 7.6f, 5.5f)
                lineToRelative(2.37f, -0.78f)
                curveTo(21.08f, 11.03f, 17.15f, 8f, 12.5f, 8f); close()
            }
        }
    }

    val EditNote: ImageVector by lazy {
        icon("EditNote") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(3f, 10f); horizontalLineToRelative(11f); verticalLineToRelative(2f); horizontalLineTo(3f); close()
                moveTo(3f, 6f); horizontalLineToRelative(11f); verticalLineToRelative(2f); horizontalLineTo(3f); close()
                moveTo(3f, 14f); horizontalLineToRelative(7f); verticalLineToRelative(2f); horizontalLineTo(3f); close()
                moveTo(20.59f, 11.93f); lineToRelative(-4.25f, 4.24f); lineToRelative(-2.12f, -2.12f)
                lineToRelative(4.24f, -4.24f)
                curveToRelative(0.39f, -0.39f, 1.02f, -0.39f, 1.41f, 0f)
                lineToRelative(0.71f, 0.71f)
                curveTo(20.98f, 10.91f, 20.98f, 11.54f, 20.59f, 11.93f); close()
                moveTo(13.5f, 14.76f); lineToRelative(2.12f, 2.12f); lineTo(12f, 18.38f); verticalLineToRelative(-2.12f); lineTo(13.5f, 14.76f); close()
            }
        }
    }

    val Copy: ImageVector by lazy {
        icon("Copy") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(16f, 1f); horizontalLineTo(4f); curveTo(2.9f, 1f, 2f, 1.9f, 2f, 3f)
                verticalLineToRelative(14f); horizontalLineToRelative(2f); verticalLineTo(3f)
                horizontalLineToRelative(12f); verticalLineTo(1f); close()
                moveTo(19f, 5f); horizontalLineTo(8f); curveTo(6.9f, 5f, 6f, 5.9f, 6f, 7f)
                verticalLineToRelative(14f); curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
                horizontalLineToRelative(11f); curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
                verticalLineTo(7f); curveTo(21f, 5.9f, 20.1f, 5f, 19f, 5f); close()
                moveTo(19f, 21f); horizontalLineTo(8f); verticalLineTo(7f); horizontalLineToRelative(11f); verticalLineTo(21f); close()
            }
        }
    }

    val Delete: ImageVector by lazy {
        icon("Delete") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(6f, 19f); curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
                horizontalLineToRelative(8f); curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
                verticalLineTo(7f); horizontalLineTo(6f); verticalLineTo(19f); close()
                moveTo(8f, 9f); horizontalLineToRelative(8f); verticalLineToRelative(10f); horizontalLineTo(8f); verticalLineTo(9f); close()
                moveTo(15.5f, 4f); lineToRelative(-1f, -1f); horizontalLineToRelative(-5f)
                lineToRelative(-1f, 1f); horizontalLineTo(5f); verticalLineToRelative(2f)
                horizontalLineToRelative(14f); verticalLineTo(4f); horizontalLineTo(15.5f); close()
            }
        }
    }

    val Info: ImageVector by lazy {
        icon("Info") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(11f, 7f); horizontalLineToRelative(2f); verticalLineToRelative(2f); horizontalLineToRelative(-2f); close()
                moveTo(11f, 11f); horizontalLineToRelative(2f); verticalLineToRelative(6f); horizontalLineToRelative(-2f); close()
                moveTo(12f, 2f); curveTo(6.48f, 2f, 2f, 6.48f, 2f, 12f)
                reflectiveCurveToRelative(4.48f, 10f, 10f, 10f); reflectiveCurveToRelative(10f, -4.48f, 10f, -10f)
                reflectiveCurveTo(17.52f, 2f, 12f, 2f); close()
                moveTo(12f, 20f); curveToRelative(-4.41f, 0f, -8f, -3.59f, -8f, -8f)
                reflectiveCurveToRelative(3.59f, -8f, 8f, -8f); reflectiveCurveToRelative(8f, 3.59f, 8f, 8f)
                reflectiveCurveToRelative(-3.59f, 8f, -8f, 8f); close()
            }
        }
    }

    val Lock: ImageVector by lazy {
        icon("Lock") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(18f, 8f); horizontalLineToRelative(-1f); verticalLineTo(6f)
                curveToRelative(0f, -2.76f, -2.24f, -5f, -5f, -5f); reflectiveCurveTo(7f, 3.24f, 7f, 6f)
                verticalLineToRelative(2f); horizontalLineTo(6f); curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
                verticalLineToRelative(10f); curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
                horizontalLineToRelative(12f); curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
                verticalLineTo(10f); curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f); close()
                moveTo(9f, 6f); curveToRelative(0f, -1.66f, 1.34f, -3f, 3f, -3f)
                reflectiveCurveToRelative(3f, 1.34f, 3f, 3f); verticalLineToRelative(2f); horizontalLineTo(9f); verticalLineTo(6f); close()
                moveTo(18f, 20f); horizontalLineTo(6f); verticalLineTo(10f); horizontalLineToRelative(12f); verticalLineTo(20f); close()
                moveTo(12f, 17f); curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
                reflectiveCurveToRelative(-0.9f, -2f, -2f, -2f); reflectiveCurveToRelative(-2f, 0.9f, -2f, 2f)
                reflectiveCurveToRelative(0.9f, 2f, 2f, 2f); close()
            }
        }
    }

    val Translate: ImageVector by lazy {
        icon("Translate") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12.87f, 15.07f); lineToRelative(-2.54f, -2.51f); lineToRelative(0.03f, -0.03f)
                curveToRelative(1.74f, -1.94f, 2.98f, -4.17f, 3.71f, -6.53f)
                horizontalLineTo(17f); verticalLineTo(4f); horizontalLineToRelative(-7f); verticalLineTo(2f)
                horizontalLineTo(8f); verticalLineToRelative(2f); horizontalLineTo(1f); verticalLineToRelative(1.99f)
                horizontalLineToRelative(11.17f)
                curveTo(11.5f, 7.92f, 10.44f, 9.75f, 9f, 11.35f)
                curveTo(8.07f, 10.32f, 7.3f, 9.19f, 6.69f, 8f); horizontalLineToRelative(-2f)
                curveToRelative(0.73f, 1.63f, 1.73f, 3.17f, 2.98f, 4.56f)
                lineToRelative(-5.09f, 5.02f); lineTo(4f, 19f); lineToRelative(5f, -5f)
                lineToRelative(3.11f, 3.11f); lineToRelative(0.76f, -2.04f); close()
                moveTo(18.5f, 10f); horizontalLineToRelative(-2f); lineTo(12f, 22f); horizontalLineToRelative(2f)
                lineToRelative(1.12f, -3f); horizontalLineToRelative(4.75f); lineTo(21f, 22f); horizontalLineToRelative(2f)
                lineToRelative(-4.5f, -12f); close()
                moveTo(15.88f, 17f); lineToRelative(1.62f, -4.33f); lineTo(19.12f, 17f); horizontalLineToRelative(-3.24f); close()
            }
        }
    }

    val ShareCard: ImageVector by lazy {
        icon("ShareCard") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(18f, 16.08f); curveToRelative(-0.76f, 0f, -1.44f, 0.3f, -1.96f, 0.77f)
                lineTo(8.91f, 12.7f); curveToRelative(0.05f, -0.23f, 0.09f, -0.46f, 0.09f, -0.7f)
                reflectiveCurveToRelative(-0.04f, -0.47f, -0.09f, -0.7f); lineToRelative(7.05f, -4.11f)
                curveToRelative(0.54f, 0.5f, 1.25f, 0.81f, 2.04f, 0.81f)
                curveToRelative(1.66f, 0f, 3f, -1.34f, 3f, -3f); reflectiveCurveToRelative(-1.34f, -3f, -3f, -3f)
                reflectiveCurveToRelative(-3f, 1.34f, -3f, 3f); curveToRelative(0f, 0.24f, 0.04f, 0.47f, 0.09f, 0.7f)
                lineTo(8.04f, 9.81f); curveTo(7.5f, 9.31f, 6.79f, 9f, 6f, 9f)
                curveToRelative(-1.66f, 0f, -3f, 1.34f, -3f, 3f); reflectiveCurveToRelative(1.34f, 3f, 3f, 3f)
                curveToRelative(0.79f, 0f, 1.5f, -0.31f, 2.04f, -0.81f); lineToRelative(7.12f, 4.16f)
                curveToRelative(-0.05f, 0.21f, -0.08f, 0.43f, -0.08f, 0.65f)
                curveToRelative(0f, 1.61f, 1.31f, 2.92f, 2.92f, 2.92f)
                reflectiveCurveToRelative(2.92f, -1.31f, 2.92f, -2.92f)
                curveToRelative(0f, -1.61f, -1.31f, -2.92f, -2.92f, -2.92f); close()
            }
        }
    }

    val Download: ImageVector by lazy {
        icon("Download") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(19f, 9f); horizontalLineToRelative(-4f); verticalLineTo(3f)
                horizontalLineTo(9f); verticalLineToRelative(6f); horizontalLineTo(5f)
                lineToRelative(7f, 7f); lineToRelative(7f, -7f); close()
                moveTo(5f, 18f); verticalLineToRelative(2f); horizontalLineToRelative(14f); verticalLineToRelative(-2f); horizontalLineTo(5f); close()
            }
        }
    }

    val Pin: ImageVector by lazy {
        icon("Pin") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(16f, 9f); verticalLineTo(4f); horizontalLineToRelative(1f); verticalLineTo(2f)
                horizontalLineTo(7f); verticalLineToRelative(2f); horizontalLineToRelative(1f); verticalLineToRelative(5f)
                curveToRelative(0f, 1.66f, -1.34f, 3f, -3f, 3f); verticalLineToRelative(2f)
                horizontalLineToRelative(5.97f); verticalLineToRelative(7f); lineToRelative(1f, 1f)
                lineToRelative(1f, -1f); verticalLineToRelative(-7f); horizontalLineTo(19f); verticalLineToRelative(-2f)
                curveToRelative(-1.66f, 0f, -3f, -1.34f, -3f, -3f); close()
            }
        }
    }

    val Play: ImageVector by lazy {
        icon("Play") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(8f, 5f); verticalLineToRelative(14f); lineToRelative(11f, -7f); close()
            }
        }
    }

    val Pause: ImageVector by lazy {
        icon("Pause") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(6f, 19f); horizontalLineToRelative(4f); verticalLineTo(5f); horizontalLineTo(6f); verticalLineToRelative(14f); close()
                moveTo(14f, 5f); verticalLineToRelative(14f); horizontalLineToRelative(4f); verticalLineTo(5f); horizontalLineToRelative(-4f); close()
            }
        }
    }

    val StarFilled: ImageVector by lazy {
        icon("StarFilled") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 17.27f); lineTo(18.18f, 21f); lineToRelative(-1.64f, -7.03f)
                lineTo(22f, 9.24f); lineToRelative(-7.19f, -0.61f); lineTo(12f, 2f)
                lineTo(9.19f, 8.63f); lineTo(2f, 9.24f); lineToRelative(5.46f, 4.73f)
                lineTo(5.82f, 21f); close()
            }
        }
    }

    val MoreVert: ImageVector by lazy {
        icon("MoreVert") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 8f); curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
                reflectiveCurveToRelative(-0.9f, -2f, -2f, -2f); reflectiveCurveToRelative(-2f, 0.9f, -2f, 2f)
                reflectiveCurveToRelative(0.9f, 2f, 2f, 2f); close()
                moveTo(12f, 10f); curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
                reflectiveCurveToRelative(0.9f, 2f, 2f, 2f); reflectiveCurveToRelative(2f, -0.9f, 2f, -2f)
                reflectiveCurveToRelative(-0.9f, -2f, -2f, -2f); close()
                moveTo(12f, 16f); curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
                reflectiveCurveToRelative(0.9f, 2f, 2f, 2f); reflectiveCurveToRelative(2f, -0.9f, 2f, -2f)
                reflectiveCurveToRelative(-0.9f, -2f, -2f, -2f); close()
            }
        }
    }

    val ImageCover: ImageVector by lazy {
        icon("ImageCover") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(21f, 19f); verticalLineTo(5f); curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
                horizontalLineTo(5f); curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f); verticalLineToRelative(14f)
                curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f); horizontalLineToRelative(14f)
                curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f); close()
                moveTo(8.5f, 13.5f); lineToRelative(2.5f, 3.01f); lineTo(14.5f, 12f)
                lineToRelative(4.5f, 6f); horizontalLineTo(5f); lineToRelative(3.5f, -4.5f); close()
            }
        }
    }

    val Flashcards: ImageVector by lazy {
        icon("Flashcards") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(4f, 6f); horizontalLineTo(2f); verticalLineToRelative(14f)
                curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f); horizontalLineToRelative(14f); verticalLineToRelative(-2f)
                horizontalLineTo(4f); verticalLineTo(6f); close()
                moveTo(20f, 2f); horizontalLineTo(8f); curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
                verticalLineToRelative(12f); curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f); horizontalLineToRelative(12f)
                curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f); verticalLineTo(4f)
                curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f); close()
                moveTo(20f, 16f); horizontalLineTo(8f); verticalLineTo(4f); horizontalLineToRelative(12f); verticalLineTo(16f); close()
            }
        }
    }

    private fun icon(name: String, block: ImageVector.Builder.() -> Unit): ImageVector {
        return ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply(block).build()
    }
}
