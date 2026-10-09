package com.quickscan.core.ui.component

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder

import androidx.compose.ui.unit.dp

/*
 * Lucide (ISC / MIT), redrawn as stroke-only ImageVectors so `Icon` can tint
 * them the way the mockups tint their inlined SVGs. Viewport 24x24, stroke
 * width 2, round caps and joins — Lucide's defaults.
 */

private fun PathBuilder.circle(cx: Float, cy: Float, r: Float) {
    moveTo(cx - r, cy)
    arcTo(r, r, 0f, true, true, cx + r, cy)
    arcTo(r, r, 0f, true, true, cx - r, cy)
    close()
}

private fun PathBuilder.roundedRect(x: Float, y: Float, w: Float, h: Float, r: Float) {
    moveTo(x + r, y)
    horizontalLineTo(x + w - r)
    arcToRelative(r, r, 0f, true, false, r, r)
    horizontalLineTo(x + w)
    verticalLineTo(y + h - r)
    arcToRelative(r, r, 0f, true, false, -r, r)
    horizontalLineTo(x + r)
    arcToRelative(r, r, 0f, true, false, -r, -r)
    verticalLineTo(y + r)
    arcToRelative(r, r, 0f, true, false, r, -r)
    close()
}

private fun lucide(name: String, block: PathBuilder.() -> Unit): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).addPath(
        pathData = PathBuilder().apply(block).nodes,
        fill = null,
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 2f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
    ).build()

val LucideChevronLeft = lucide("ChevronLeft") {
    moveTo(15f, 18f); lineTo(9f, 12f); lineTo(15f, 6f)
}

val LucideChevronRight = lucide("ChevronRight") {
    moveTo(9f, 18f); lineTo(15f, 12f); lineTo(9f, 6f)
}

val LucideChevronDown = lucide("ChevronDown") {
    moveTo(6f, 9f); lineTo(12f, 15f); lineTo(18f, 9f)
}

val LucideArrowRight = lucide("ArrowRight") {
    moveTo(5f, 12f); horizontalLineTo(19f)
    moveTo(12f, 5f); lineTo(19f, 12f); lineTo(12f, 19f)
}

val LucideCheck = lucide("Check") {
    moveTo(20f, 6f); lineTo(9f, 17f); lineTo(4f, 12f)
}

val LucideEllipsis = lucide("Ellipsis") {
    moveTo(12f, 12f); horizontalLineTo(12.01f)
    moveTo(19f, 12f); horizontalLineTo(19.01f)
    moveTo(5f, 12f); horizontalLineTo(5.01f)
}

val LucideCircleX = lucide("CircleX") {
    circle(12f, 12f, 10f)
    moveTo(15f, 9f); lineTo(9f, 15f)
    moveTo(9f, 9f); lineTo(15f, 15f)
}

val LucideSearch = lucide("Search") {
    circle(11f, 11f, 8f)
    moveTo(21f, 21f); lineTo(16.65f, 16.65f)
}

val LucideLink = lucide("Link") {
    moveTo(10f, 13f)
    arcToRelative(5f, 5f, 0f, true, true, 7.54f, 0.54f)
    lineTo(20.54f, 13.54f)
    moveTo(14f, 11f)
    arcToRelative(5f, 5f, 0f, true, false, -7.54f, -0.54f)
    lineTo(3.46f, 7.46f)
}

val LucideExternalLink = lucide("ExternalLink") {
    moveTo(15f, 3f); horizontalLineTo(21f); verticalLineTo(9f)
    moveTo(10f, 14f); lineTo(21f, 3f)
    moveTo(18f, 13f); verticalLineTo(19f)
    arcToRelative(2f, 2f, 0f, true, true, 2f, 2f)
    verticalLineTo(21f)
    horizontalLineTo(5f)
    arcToRelative(2f, 2f, 0f, true, false, -2f, -2f)
    verticalLineTo(8f)
    arcToRelative(2f, 2f, 0f, true, true, 2f, -2f)
    horizontalLineTo(14f)
}

val LucideShare2 = lucide("Share2") {
    circle(18f, 5f, 3f)
    circle(6f, 12f, 3f)
    circle(18f, 19f, 3f)
    moveTo(8.59f, 13.51f); lineTo(15.42f, 17.49f)
    moveTo(15.41f, 6.51f); lineTo(8.59f, 10.49f)
}

val LucideCopy = lucide("Copy") {
    roundedRect(9f, 9f, 13f, 13f, 2f)
    moveTo(5f, 15f); horizontalLineTo(4f)
    arcToRelative(2f, 2f, 0f, true, false, 2f, -2f)
    verticalLineTo(4f)
    arcToRelative(2f, 2f, 0f, true, true, 2f, -2f)
    horizontalLineTo(15f)
    arcToRelative(2f, 2f, 0f, true, false, 2f, 2f)
    verticalLineTo(5f)
}

val LucideTrash2 = lucide("Trash2") {
    moveTo(3f, 6f); horizontalLineTo(21f)
    moveTo(19f, 6f); verticalLineTo(20f)
    arcToRelative(2f, 2f, 0f, true, false, -2f, 2f)
    horizontalLineTo(7f)
    arcToRelative(2f, 2f, 0f, true, false, -2f, -2f)
    verticalLineTo(6f)
    moveTo(8f, 6f); verticalLineTo(4f)
    arcToRelative(2f, 2f, 0f, true, true, 2f, -2f)
    horizontalLineTo(14f)
    arcToRelative(2f, 2f, 0f, true, true, 2f, 2f)
    verticalLineTo(6f)
    moveTo(10f, 11f); verticalLineTo(17f)
    moveTo(14f, 11f); verticalLineTo(17f)
}

val LucideBookmark = lucide("Bookmark") {
    moveTo(19f, 21f); lineTo(12f, 17f); lineTo(5f, 21f); verticalLineTo(5f)
    arcToRelative(2f, 2f, 0f, true, false, -2f, -2f)
    verticalLineTo(5f)
    arcToRelative(2f, 2f, 0f, true, true, 2f, -2f)
    horizontalLineTo(19f)
    arcToRelative(2f, 2f, 0f, true, true, 2f, 2f)
    verticalLineTo(19f)
    close()
}

val LucideLock = lucide("Lock") {
    roundedRect(3f, 11f, 18f, 11f, 2f)
    moveTo(7f, 11f); verticalLineTo(7f)
    arcTo(5f, 5f, 0f, true, true, 10f, 0f)
    arcTo(5f, 5f, 0f, true, true, 17f, 7f)
    verticalLineTo(11f)
}

val LucideEye = lucide("Eye") {
    moveTo(2.06f, 12.35f)
    arcToRelative(10.75f, 10.75f, 0f, true, true, 19.88f, 0f)
    arcToRelative(10.75f, 10.75f, 0f, true, true, -19.88f, 0f)
    circle(12f, 12f, 3f)
}

val LucideEyeOff = lucide("EyeOff") {
    moveTo(10.73f, 5.08f)
    arcToRelative(10.75f, 10.75f, 0f, true, true, 11.21f, 6.57f)
    arcToRelative(1f, 1f, 0f, true, true, 0f, 0.7f)
    arcToRelative(10.75f, 10.75f, 0f, true, true, -1.44f, 2.49f)
    moveTo(14.08f, 14.16f)
    arcToRelative(3f, 3f, 0f, true, true, -4.24f, -4.24f)
    moveTo(17.48f, 17.5f)
    arcToRelative(10.75f, 10.75f, 0f, true, true, -15.42f, -5.15f)
    arcToRelative(1f, 1f, 0f, true, true, 0f, -0.7f)
    arcToRelative(10.75f, 10.75f, 0f, true, true, 1.44f, -2.49f)
    moveTo(2f, 2f); lineTo(22f, 22f)
}

val LucideWifi = lucide("Wifi") {
    moveTo(12f, 20f); horizontalLineTo(12.01f)
    moveTo(2f, 8.82f)
    arcToRelative(15f, 15f, 0f, true, true, 20f, 0f)
    moveTo(5f, 12.86f)
    arcToRelative(10f, 10f, 0f, true, true, 14f, 0f)
    moveTo(8.5f, 16.43f)
    arcToRelative(5f, 5f, 0f, true, true, 7f, 0f)
}

val LucideWifiOff = lucide("WifiOff") {
    moveTo(12f, 20f); horizontalLineTo(12.01f)
    moveTo(8.5f, 16.43f)
    arcToRelative(5f, 5f, 0f, true, true, 7f, 0f)
    moveTo(5f, 12.86f)
    arcToRelative(10f, 10f, 0f, true, true, 5.17f, -2.69f)
    moveTo(19f, 12.86f)
    arcToRelative(10f, 10f, 0f, true, false, -2.01f, -1.52f)
    moveTo(2f, 8.82f)
    arcToRelative(15f, 15f, 0f, true, true, 4.18f, -2.64f)
    moveTo(22f, 8.82f)
    arcToRelative(15f, 15f, 0f, true, false, -11.29f, -3.77f)
    moveTo(2f, 2f); lineTo(22f, 22f)
}

val LucideZap = lucide("Zap") {
    moveTo(4f, 14f)
    arcToRelative(1f, 1f, 0f, true, true, -0.78f, -1.63f)
    lineTo(13.12f, 2.17f)
    arcToRelative(0.5f, 0.5f, 0f, true, true, 0.86f, 0.46f)
    lineTo(12.06f, 8.65f)
    arcToRelative(1f, 1f, 0f, true, false, 1.04f, 1.35f)
    horizontalLineTo(20f)
    arcToRelative(1f, 1f, 0f, true, true, -0.78f, 1.63f)
    lineTo(9.32f, 20.03f)
    arcToRelative(0.5f, 0.5f, 0f, true, true, -0.86f, -0.46f)
    lineTo(10.38f, 13.55f)
    arcToRelative(1f, 1f, 0f, true, false, -1.04f, -1.35f)
    close()
}

val LucideScanLine = lucide("ScanLine") {
    moveTo(3f, 7f); verticalLineTo(5f)
    arcToRelative(2f, 2f, 0f, true, true, 2f, -2f)
    horizontalLineTo(7f)
    moveTo(17f, 3f); horizontalLineTo(19f)
    arcToRelative(2f, 2f, 0f, true, true, 2f, 2f)
    verticalLineTo(7f)
    moveTo(21f, 17f); verticalLineTo(19f)
    arcToRelative(2f, 2f, 0f, true, false, -2f, 2f)
    horizontalLineTo(17f)
    moveTo(7f, 21f); horizontalLineTo(5f)
    arcToRelative(2f, 2f, 0f, true, false, -2f, -2f)
    verticalLineTo(17f)
    moveTo(7f, 12f); horizontalLineTo(17f)
}

val LucideScanSearch = lucide("ScanSearch") {
    moveTo(3f, 7f); verticalLineTo(5f)
    arcToRelative(2f, 2f, 0f, true, true, 2f, -2f)
    horizontalLineTo(7f)
    moveTo(17f, 3f); horizontalLineTo(19f)
    arcToRelative(2f, 2f, 0f, true, true, 2f, 2f)
    verticalLineTo(7f)
    moveTo(21f, 17f); verticalLineTo(19f)
    arcToRelative(2f, 2f, 0f, true, false, -2f, 2f)
    horizontalLineTo(17f)
    moveTo(7f, 21f); horizontalLineTo(5f)
    arcToRelative(2f, 2f, 0f, true, false, -2f, -2f)
    verticalLineTo(17f)
    circle(11f, 11f, 3f)
    moveTo(16f, 16f); lineTo(22f, 22f)
}

val LucideQrCode = lucide("QrCode") {
    roundedRect(3f, 3f, 5f, 5f, 1f)
    roundedRect(16f, 3f, 5f, 5f, 1f)
    roundedRect(3f, 16f, 5f, 5f, 1f)
    moveTo(21f, 16f); horizontalLineTo(18f)
    arcToRelative(2f, 2f, 0f, true, false, -2f, 2f)
    verticalLineTo(21f)
    moveTo(21f, 21f); verticalLineTo(21.01f)
    moveTo(12f, 7f); verticalLineTo(10f)
    arcToRelative(2f, 2f, 0f, true, false, -2f, 2f)
    horizontalLineTo(7f)
    moveTo(3f, 12f); horizontalLineTo(3.01f)
    moveTo(12f, 3f); horizontalLineTo(12.01f)
    moveTo(12f, 16f); verticalLineTo(12.01f)
    moveTo(16f, 12f); horizontalLineTo(17f)
    moveTo(21f, 12f); verticalLineTo(21.01f)
    moveTo(12f, 21f); verticalLineTo(20f)
}

val LucideHistory = lucide("History") {
    moveTo(3f, 12f)
    arcToRelative(9f, 9f, 0f, true, false, 9f, -9f)
    arcToRelative(9.75f, 9.75f, 0f, true, false, -6.74f, 2.74f)
    lineTo(3f, 8f)
    moveTo(3f, 3f); verticalLineTo(8f); horizontalLineTo(8f)
    moveTo(12f, 7f); verticalLineTo(12f); lineTo(16f, 14f)
}

val LucideSettings = lucide("Settings") {
    moveTo(12.22f, 2f); horizontalLineTo(11.78f)
    arcToRelative(2f, 2f, 0f, true, true, 2f, 2f)
    verticalLineTo(4.18f)
    moveTo(11.78f, 4f)
    arcToRelative(1f, 1f, 0f, true, true, -1f, 1.73f)
    lineTo(10.35f, 5.98f)
    arcToRelative(2f, 2f, 0f, true, true, -2f, 0f)
    lineTo(8.2f, 5.9f)
    arcToRelative(2f, 2f, 0f, true, false, -2.73f, 0.73f)
    lineTo(5.25f, 7.01f)
    arcToRelative(2f, 2f, 0f, true, false, 0.73f, 2.73f)
    lineTo(6.13f, 9.84f)
    arcToRelative(2f, 2f, 0f, true, true, 0f, 2f)
    verticalLineTo(12.35f)
    arcToRelative(2f, 2f, 0f, true, true, 0f, 2f)
    lineTo(6.13f, 16.18f)
    arcToRelative(2f, 2f, 0f, true, false, -0.73f, 2.73f)
    lineTo(5.47f, 19.29f)
    arcToRelative(2f, 2f, 0f, true, false, 2.73f, 0.73f)
    lineTo(8.35f, 19.11f)
    arcToRelative(2f, 2f, 0f, true, true, 2f, 0f)
    lineTo(10.78f, 20.36f)
    arcToRelative(2f, 2f, 0f, true, true, 1f, 1.73f)
    verticalLineTo(22f)
    moveTo(11.78f, 22f)
    arcToRelative(2f, 2f, 0f, true, false, 2f, -2f)
    verticalLineTo(19.82f)
    moveTo(14.78f, 19.09f)
    arcToRelative(1f, 1f, 0f, true, true, 1f, -1.73f)
    lineTo(16.21f, 16.84f)
    arcToRelative(2f, 2f, 0f, true, true, 2f, 0f)
    lineTo(18.36f, 16.92f)
    arcToRelative(2f, 2f, 0f, true, false, 2.73f, -0.73f)
    lineTo(21.31f, 14.79f)
    arcToRelative(2f, 2f, 0f, true, false, -0.73f, -2.73f)
    lineTo(20.16f, 11.98f)
    arcToRelative(2f, 2f, 0f, true, true, -1f, -1.74f)
    verticalLineTo(9.74f)
    arcToRelative(2f, 2f, 0f, true, true, 1f, -1.74f)
    lineTo(10.31f, 6.25f)
    arcToRelative(2f, 2f, 0f, true, false, -0.73f, -2.73f)
    lineTo(8.15f, 3.3f)
    arcToRelative(2f, 2f, 0f, true, false, 2.73f, -0.73f)
    lineTo(11.01f, 2.65f)
    arcToRelative(2f, 2f, 0f, true, true, 2f, 0f)
    lineTo(12.78f, 4.09f)
    arcToRelative(2f, 2f, 0f, true, true, 1f, -1.73f)
    verticalLineTo(2f)
    close()
    circle(12f, 12f, 3f)
}

val LucideRepeat = lucide("Repeat") {
    moveTo(17f, 2f); lineTo(21f, 6f); lineTo(17f, 10f)
    moveTo(3f, 11f); verticalLineTo(10f)
    arcToRelative(4f, 4f, 0f, true, true, 4f, -4f)
    horizontalLineTo(21f)
    moveTo(7f, 22f); lineTo(3f, 18f); lineTo(7f, 14f)
    moveTo(21f, 13f); verticalLineTo(14f)
    arcToRelative(4f, 4f, 0f, true, false, -4f, 4f)
    horizontalLineTo(3f)
}

val LucideImage = lucide("Image") {
    roundedRect(3f, 3f, 18f, 18f, 2f)
    circle(8.5f, 9.5f, 1.5f)
    moveTo(21f, 15f); lineTo(17.91f, 11.91f)
    arcToRelative(2f, 2f, 0f, true, false, -2.82f, 0f)
    lineTo(6f, 21f)
}

val LucideClipboardPaste = lucide("ClipboardPaste") {
    moveTo(16f, 4f); horizontalLineTo(18f)
    arcToRelative(2f, 2f, 0f, true, true, 2f, 2f)
    verticalLineTo(20f)
    arcToRelative(2f, 2f, 0f, true, true, -2f, 2f)
    horizontalLineTo(6f)
    arcToRelative(2f, 2f, 0f, true, true, -2f, -2f)
    verticalLineTo(6f)
    arcToRelative(2f, 2f, 0f, true, true, 2f, -2f)
    horizontalLineTo(8f)
    roundedRect(8f, 2f, 8f, 4f, 1f)
    moveTo(9f, 14f); horizontalLineTo(15f)
    moveTo(9f, 12f)
    arcToRelative(6f, 6f, 0f, true, false, -6f, 6f)
}

val LucideClipboardCheck = lucide("ClipboardCheck") {
    moveTo(16f, 4f); horizontalLineTo(18f)
    arcToRelative(2f, 2f, 0f, true, true, 2f, 2f)
    verticalLineTo(20f)
    arcToRelative(2f, 2f, 0f, true, true, -2f, 2f)
    horizontalLineTo(6f)
    arcToRelative(2f, 2f, 0f, true, true, -2f, -2f)
    verticalLineTo(6f)
    arcToRelative(2f, 2f, 0f, true, true, 2f, -2f)
    horizontalLineTo(8f)
    roundedRect(8f, 2f, 8f, 4f, 1f)
    moveTo(9f, 14f); lineTo(11f, 16f); lineTo(15f, 12f)
}

val LucideShieldCheck = lucide("ShieldCheck") {
    moveTo(20f, 13f)
    arcToRelative(0f, 0f, 0f, true, false, -7.66f, 8.95f)
    arcToRelative(1f, 1f, 0f, true, true, -0.67f, -0.01f)
    arcToRelative(0f, 0f, 0f, true, true, -7.67f, -8.95f)
    verticalLineTo(6f)
    arcToRelative(1f, 1f, 0f, true, true, 1f, -1f)
    arcToRelative(0f, 0f, 0f, true, true, 4.24f, -1.72f)
    arcToRelative(1.17f, 1.17f, 0f, true, true, 1.52f, 0f)
    arcToRelative(0f, 0f, 0f, true, true, 4.24f, 1.72f)
    arcToRelative(1f, 1f, 0f, true, true, 1f, 1f)
    close()
    moveTo(9f, 12f); lineTo(11f, 14f); lineTo(15f, 10f)
}

val LucideType = lucide("Type") {
    moveTo(4f, 7f); verticalLineTo(4f); lineTo(20f, 4f)
    moveTo(9f, 20f); horizontalLineTo(15f)
    moveTo(12f, 4f); verticalLineTo(20f)
}

val LucideUser = lucide("User") {
    moveTo(19f, 21f); verticalLineTo(19f)
    arcToRelative(4f, 4f, 0f, true, false, -4f, -4f)
    horizontalLineTo(9f)
    arcToRelative(4f, 4f, 0f, true, false, -4f, 4f)
    verticalLineTo(19f)
    circle(12f, 7f, 4f)
}

val LucidePalette = lucide("Palette") {
    moveTo(12f, 22f)
    arcToRelative(1f, 1f, 0f, true, true, 0f, -20f)
    arcToRelative(10f, 9f, 0f, true, true, 10f, 9f)
    arcToRelative(5f, 5f, 0f, true, true, -5f, 5f)
    horizontalLineTo(14.75f)
    arcToRelative(1.75f, 1.75f, 0f, true, false, -1.4f, 2.8f)
    lineTo(13.65f, 20.2f)
    arcToRelative(1.75f, 1.75f, 0f, true, true, -1.4f, 2.8f)
    close()
    circle(13.5f, 6.5f, 1.125f)
    circle(17.5f, 10.5f, 1.125f)
    circle(6.5f, 12.5f, 1.125f)
    circle(8.5f, 7.5f, 1.125f)
}

val LucideVideo = lucide("Video") {
    moveTo(16f, 13f); lineTo(21.22f, 16.48f)
    arcToRelative(0.5f, 0.5f, 0f, true, false, 0.78f, -0.42f)
    verticalLineTo(7.87f)
    arcToRelative(0.5f, 0.5f, 0f, true, false, -0.75f, -0.43f)
    lineTo(16f, 10.5f)
    moveTo(2f, 6f)
    arcToRelative(2f, 2f, 0f, true, true, 2f, -2f)
    horizontalLineTo(16f)
    arcToRelative(2f, 2f, 0f, true, true, 2f, 2f)
    verticalLineTo(18f)
    arcToRelative(2f, 2f, 0f, true, true, -2f, 2f)
    horizontalLineTo(4f)
    arcToRelative(2f, 2f, 0f, true, true, -2f, -2f)
    close()
}

val LucideVolume2 = lucide("Volume2") {
    moveTo(11f, 4.7f)
    arcToRelative(0.71f, 0.71f, 0f, true, false, -1.2f, -0.5f)
    lineTo(6.41f, 7.59f)
    arcToRelative(1.4f, 1.4f, 0f, true, true, -1f, 0.41f)
    horizontalLineTo(3f)
    arcToRelative(1f, 1f, 0f, true, false, 1f, 1f)
    verticalLineTo(10f)
    arcToRelative(1f, 1f, 0f, true, false, -1f, 1f)
    horizontalLineTo(5.41f)
    arcToRelative(1.4f, 1.4f, 0f, true, true, 1f, 0.41f)
    lineTo(9.8f, 14.8f)
    arcToRelative(0.71f, 0.71f, 0f, true, false, 1.2f, -0.5f)
    close()
    moveTo(16f, 9f)
    arcToRelative(5f, 5f, 0f, true, true, 0f, 6f)
    moveTo(19.36f, 18.36f)
    arcToRelative(9f, 9f, 0f, true, false, 0f, -12.72f)
}

val LucideSmartphone = lucide("Smartphone") {
    roundedRect(5f, 2f, 14f, 20f, 2f)
    moveTo(12f, 18f); horizontalLineTo(12.01f)
}

val LucideCamera = lucide("Camera") {
    moveTo(14.5f, 4f); horizontalLineTo(9.5f); lineTo(7f, 7f); horizontalLineTo(4f)
    arcToRelative(2f, 2f, 0f, true, false, -2f, 2f)
    verticalLineTo(18f)
    arcToRelative(2f, 2f, 0f, true, false, 2f, 2f)
    horizontalLineTo(20f)
    arcToRelative(2f, 2f, 0f, true, true, 2f, -2f)
    verticalLineTo(9f)
    arcToRelative(2f, 2f, 0f, true, false, -2f, -2f)
    horizontalLineTo(14.5f)
    close()
    circle(12f, 13f, 3f)
}

val LucideBell = lucide("Bell") {
    moveTo(10.27f, 21f)
    arcToRelative(2f, 2f, 0f, true, false, 3.46f, 0f)
    moveTo(3.26f, 15.33f)
    arcToRelative(1f, 1f, 0f, true, false, 0.74f, 1.67f)
    horizontalLineTo(20f)
    arcToRelative(1f, 1f, 0f, true, true, 0.74f, -1.67f)
    arcToRelative(0f, 0f, 0f, true, true, -1.74f, -2.67f)
    arcToRelative(0f, 0f, 0f, true, true, -1f, -1.5f)
    arcToRelative(6f, 6f, 0f, true, false, -12f, 0f)
    arcToRelative(0f, 0f, 0f, true, true, -1f, 1.5f)
    arcToRelative(0f, 0f, 0f, true, true, -1.74f, 2.67f)
}

val LucideInfo = lucide("Info") {
    circle(12f, 12f, 10f)
    moveTo(12f, 16f); verticalLineTo(12f)
    moveTo(12f, 8f); horizontalLineTo(12.01f)
}

val LucideSparkles = lucide("Sparkles") {
    moveTo(9.94f, 3.5f); lineTo(11.1f, 8.3f)
    arcToRelative(2f, 2f, 0f, true, true, 1.29f, 1.29f)
    lineTo(16.9f, 10.6f)
    moveTo(12.1f, 21f); lineTo(14f, 15.2f)
    arcToRelative(2f, 2f, 0f, true, true, 1.29f, -1.29f)
    lineTo(19.7f, 11f)
    moveTo(5f, 3f); verticalLineTo(7f)
    moveTo(19f, 17f); verticalLineTo(21f)
    moveTo(3f, 5f); horizontalLineTo(7f)
    moveTo(17f, 19f); horizontalLineTo(21f)
}

val LucideHardDrive = lucide("HardDrive") {
    moveTo(22f, 12f); horizontalLineTo(2f)
    moveTo(5.45f, 5.11f); lineTo(2f, 12f); verticalLineTo(18f)
    arcToRelative(2f, 2f, 0f, true, true, 2f, 2f)
    horizontalLineTo(20f)
    arcToRelative(2f, 2f, 0f, true, true, 2f, -2f)
    verticalLineTo(12f)
    lineTo(3.45f, 5.11f)
    arcToRelative(2f, 2f, 0f, true, false, -1.79f, 1.11f)
    horizontalLineTo(16.76f)
    arcToRelative(2f, 2f, 0f, true, true, 1.79f, -1.11f)
    close()
    moveTo(6f, 16f); horizontalLineTo(6.01f)
    moveTo(10f, 16f); horizontalLineTo(10.01f)
}

val LucideMoon = lucide("Moon") {
    moveTo(12f, 3f)
    arcToRelative(6f, 6f, 0f, true, false, 9f, 9f)
    arcToRelative(9f, 9f, 0f, true, true, -9f, -9f)
}

val LucideGlobe = lucide("Globe") {
    circle(12f, 12f, 10f)
    moveTo(12f, 2f)
    arcToRelative(14.5f, 14.5f, 0f, true, true, 0f, 20f)
    arcToRelative(14.5f, 14.5f, 0f, true, true, 0f, -20f)
    moveTo(2f, 12f); horizontalLineTo(22f)
}

val LucideDownload = lucide("Download") {
    moveTo(21f, 15f); verticalLineTo(19f)
    arcToRelative(2f, 2f, 0f, true, true, -2f, 2f)
    horizontalLineTo(5f)
    arcToRelative(2f, 2f, 0f, true, true, -2f, -2f)
    verticalLineTo(15f)
    moveTo(7f, 10f); lineTo(12f, 15f); lineTo(17f, 10f)
    moveTo(12f, 15f); verticalLineTo(3f)
}

val LucideMail = lucide("Mail") {
    roundedRect(2f, 4f, 20f, 16f, 2f)
    moveTo(22f, 7f); lineTo(13.03f, 12.7f)
    arcToRelative(1.94f, 1.94f, 0f, true, true, -2.06f, 0f)
    lineTo(2f, 7f)
}

val LucidePlus = lucide("Plus") {
    moveTo(5f, 12f); horizontalLineTo(19f)
    moveTo(12f, 5f); verticalLineTo(19f)
}

/** Status-bar glyphs, drawn on the same 24-unit grid as the rest of the set. */
val LucideSignal = lucide("Signal") {
    moveTo(2f, 20f); horizontalLineTo(2.01f)
    moveTo(7f, 20f); verticalLineTo(16f)
    moveTo(17f, 20f); verticalLineTo(12f)
    moveTo(22f, 20f); verticalLineTo(4f)
}