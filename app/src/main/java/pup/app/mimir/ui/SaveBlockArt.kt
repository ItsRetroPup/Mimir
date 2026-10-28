package pup.app.mimir.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp

/** The object each tool is drawn as in its memory-card slot; the drawing says what the tool does. */
enum class ToolArt {
    /** Multi-disc organizer: a fanned stack of discs. */
    DiscStack,
    /** Vita shortcuts: a Vita game card. */
    VitaCard,
    /** ES-DE systems: nested folders with system rows. */
    Folders,
    /** NSZ to NSP: a Switch game card. */
    SwitchCart,
    /** RomZipper: a cartridge closed by a zip. */
    ZipCart,
    /** CHDMan: a disc packed into tight bands. */
    PackedDisc,
    /** Dolphin RVZ: an 8 cm GameCube mini disc. */
    MiniDisc,
    /** Azahar ZCCI: a 3DS card. */
    HandheldCart,
    /** Xbox XISO: a disc with its unused slice trimmed off. */
    TrimmedDisc,
    /** ROM Patcher: a cartridge with a bandage stuck across its label. */
    PatchedCart,
}

/** A tool's ink in the night and daylight palettes. */
@Immutable
data class ToolInk(val night: Color, val day: Color) {
    fun resolve(palette: MimirPalette): Color = if (palette.isDark) night else day
}

/**
 * Draws [art] in [ink]. When [turning] is set the object rocks slowly about its vertical axis, like
 * a save icon under the cursor; [still] parks it for reduced motion.
 */
@Composable
internal fun SaveBlockArt(
    art: ToolArt,
    ink: Color,
    hole: Color,
    modifier: Modifier = Modifier,
    turning: Boolean = false,
) {
    val still = reduceMotion()
    val angle = if (turning && !still) {
        val transition = rememberInfiniteTransition(label = "saveBlockTurn")
        transition.animateFloat(
            initialValue = -18f,
            targetValue = 18f,
            animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "saveBlockAngle",
        ).value
    } else {
        0f
    }
    Canvas(
        modifier.graphicsLayer {
            rotationY = angle
            cameraDistance = 12f * density
        },
    ) {
        when (art) {
            ToolArt.DiscStack -> discStack(ink, hole)
            ToolArt.VitaCard -> vitaCard(ink)
            ToolArt.Folders -> folders(ink)
            ToolArt.SwitchCart -> switchCart(ink)
            ToolArt.ZipCart -> zipCart(ink, hole)
            ToolArt.PackedDisc -> packedDisc(ink, hole)
            ToolArt.MiniDisc -> miniDisc(ink, hole)
            ToolArt.HandheldCart -> handheldCart(ink)
            ToolArt.TrimmedDisc -> trimmedDisc(ink, hole)
            ToolArt.PatchedCart -> patchedCart(ink)
        }
    }
}

private fun shade(ink: Color, amount: Float) = lerp(ink, Color.Black, amount)
private fun light(ink: Color, amount: Float) = lerp(ink, Color.White, amount)

private fun DrawScope.disc(center: Offset, radius: Float, ink: Color, hole: Color, hub: Float = 0.26f) {
    drawCircle(shade(ink, 0.5f), radius, center + Offset(0f, radius * 0.09f))
    drawCircle(
        Brush.radialGradient(
            listOf(light(ink, 0.35f), ink, shade(ink, 0.18f)),
            center = center - Offset(radius * 0.35f, radius * 0.35f),
            radius = radius * 1.6f,
        ),
        radius,
        center,
    )
    drawCircle(Color.White.copy(alpha = 0.16f), radius * 0.8f, center, style = Stroke(radius * 0.025f))
    drawCircle(Color.White.copy(alpha = 0.12f), radius * 0.62f, center, style = Stroke(radius * 0.025f))
    drawArc(
        Color.White.copy(alpha = 0.45f),
        startAngle = 200f,
        sweepAngle = 55f,
        useCenter = false,
        topLeft = center - Offset(radius * 0.9f, radius * 0.9f),
        size = Size(radius * 1.8f, radius * 1.8f),
        style = Stroke(radius * 0.07f, cap = StrokeCap.Round),
    )
    drawCircle(light(ink, 0.55f), radius * hub, center)
    drawCircle(hole, radius * hub * 0.4f, center)
}

private fun DrawScope.discStack(ink: Color, hole: Color) {
    val r = size.minDimension * 0.3f
    val c = center
    disc(c + Offset(-r * 0.55f, -r * 0.45f), r, shade(ink, 0.35f), hole)
    disc(c + Offset(0f, 0f), r, shade(ink, 0.15f), hole)
    disc(c + Offset(r * 0.55f, r * 0.45f), r, ink, hole)
}

private fun DrawScope.card(rect: Rect, ink: Color, corner: Float, notch: Float) {
    val side = Rect(rect.left, rect.top + rect.height * 0.05f, rect.right, rect.bottom + rect.height * 0.05f)
    drawPath(notchedRect(side, corner, notch), shade(ink, 0.5f))
    drawPath(
        notchedRect(rect, corner, notch),
        Brush.linearGradient(listOf(light(ink, 0.25f), ink, shade(ink, 0.12f)), rect.topLeft, rect.bottomRight),
    )
}

/** A rounded rectangle with its top-right corner cut, the shape of most game cards. */
private fun notchedRect(rect: Rect, corner: Float, notch: Float) = Path().apply {
    moveTo(rect.left + corner, rect.top)
    lineTo(rect.right - notch, rect.top)
    lineTo(rect.right, rect.top + notch)
    lineTo(rect.right, rect.bottom - corner)
    quadraticTo(rect.right, rect.bottom, rect.right - corner, rect.bottom)
    lineTo(rect.left + corner, rect.bottom)
    quadraticTo(rect.left, rect.bottom, rect.left, rect.bottom - corner)
    lineTo(rect.left, rect.top + corner)
    quadraticTo(rect.left, rect.top, rect.left + corner, rect.top)
    close()
}

private fun DrawScope.label(rect: Rect, ink: Color) {
    drawRoundRect(light(ink, 0.78f), rect.topLeft, rect.size, CornerRadius(rect.width * 0.08f))
    val line = shade(ink, 0.2f).copy(alpha = 0.55f)
    val h = rect.height * 0.1f
    drawRoundRect(line, Offset(rect.left + rect.width * 0.12f, rect.top + rect.height * 0.28f), Size(rect.width * 0.6f, h), CornerRadius(h))
    drawRoundRect(line, Offset(rect.left + rect.width * 0.12f, rect.top + rect.height * 0.52f), Size(rect.width * 0.4f, h), CornerRadius(h))
}

private fun DrawScope.vitaCard(ink: Color) {
    val w = size.width * 0.46f
    val h = size.height * 0.6f
    val rect = Rect(center.x - w / 2, center.y - h / 2, center.x + w / 2, center.y + h / 2)
    card(rect, ink, corner = w * 0.12f, notch = w * 0.2f)
    label(Rect(rect.left + w * 0.12f, rect.top + h * 0.12f, rect.right - w * 0.12f, rect.top + h * 0.6f), ink)
    val pad = shade(ink, 0.35f)
    repeat(4) { i ->
        val x = rect.left + w * (0.16f + i * 0.19f)
        drawRoundRect(pad, Offset(x, rect.bottom - h * 0.2f), Size(w * 0.1f, h * 0.12f), CornerRadius(w * 0.02f))
    }
}

private fun DrawScope.folder(rect: Rect, ink: Color) {
    val tab = Path().apply {
        moveTo(rect.left, rect.top + rect.height * 0.12f)
        lineTo(rect.left, rect.top + rect.height * 0.04f)
        quadraticTo(rect.left, rect.top, rect.left + rect.width * 0.05f, rect.top)
        lineTo(rect.left + rect.width * 0.36f, rect.top)
        lineTo(rect.left + rect.width * 0.46f, rect.top + rect.height * 0.12f)
        close()
    }
    drawPath(tab, shade(ink, 0.2f))
    val body = Rect(rect.left, rect.top + rect.height * 0.12f, rect.right, rect.bottom)
    drawRoundRect(shade(ink, 0.5f), body.topLeft + Offset(0f, rect.height * 0.05f), body.size, CornerRadius(rect.width * 0.06f))
    drawRoundRect(
        Brush.linearGradient(listOf(light(ink, 0.25f), ink), body.topLeft, body.bottomRight),
        body.topLeft,
        body.size,
        CornerRadius(rect.width * 0.06f),
    )
}

private fun DrawScope.folders(ink: Color) {
    val w = size.width * 0.62f
    val h = size.height * 0.48f
    val back = Rect(center.x - w / 2 - w * 0.08f, center.y - h / 2 - h * 0.2f, center.x + w / 2 - w * 0.08f, center.y + h / 2 - h * 0.2f)
    folder(back, shade(ink, 0.35f))
    val front = Rect(center.x - w / 2 + w * 0.06f, center.y - h / 2 + h * 0.12f, center.x + w / 2 + w * 0.06f, center.y + h / 2 + h * 0.12f)
    folder(front, ink)
    val row = Color.White.copy(alpha = 0.7f)
    repeat(3) { i ->
        val y = front.top + front.height * (0.38f + i * 0.2f)
        drawCircle(row, front.width * 0.035f, Offset(front.left + front.width * 0.14f, y))
        drawRoundRect(row, Offset(front.left + front.width * 0.22f, y - front.height * 0.04f), Size(front.width * (0.55f - i * 0.1f), front.height * 0.08f), CornerRadius(front.height * 0.04f))
    }
}

private fun DrawScope.switchCart(ink: Color) {
    val w = size.width * 0.5f
    val h = size.height * 0.58f
    val rect = Rect(center.x - w / 2, center.y - h / 2 + h * 0.04f, center.x + w / 2, center.y + h / 2 + h * 0.04f)
    drawRoundRect(shade(ink, 0.35f), Offset(rect.left + w * 0.12f, rect.top - h * 0.1f), Size(w * 0.76f, h * 0.16f), CornerRadius(w * 0.05f))
    card(rect, ink, corner = w * 0.1f, notch = w * 0.001f)
    label(Rect(rect.left + w * 0.12f, rect.top + h * 0.14f, rect.right - w * 0.12f, rect.bottom - h * 0.14f), ink)
}

private fun DrawScope.zipCart(ink: Color, hole: Color) {
    val w = size.width * 0.54f
    val h = size.height * 0.62f
    val rect = Rect(center.x - w / 2, center.y - h / 2, center.x + w / 2, center.y + h / 2)
    card(rect, ink, corner = w * 0.08f, notch = w * 0.18f)
    label(Rect(rect.left + w * 0.14f, rect.top + h * 0.1f, rect.right - w * 0.14f, rect.top + h * 0.48f), ink)
    // The zip: alternating teeth down to a pull tab.
    val x = center.x
    val tooth = h * 0.045f
    var y = rect.top + h * 0.56f
    var left = true
    while (y < rect.bottom - h * 0.12f) {
        val tx = if (left) x - tooth * 1.1f else x
        drawRoundRect(light(ink, 0.7f), Offset(tx, y), Size(tooth * 1.1f, tooth * 0.8f), CornerRadius(tooth * 0.2f))
        y += tooth
        left = !left
    }
    drawRoundRect(light(ink, 0.85f), Offset(x - tooth * 0.8f, y), Size(tooth * 1.6f, tooth * 1.8f), CornerRadius(tooth * 0.5f))
    drawCircle(hole, tooth * 0.3f, Offset(x, y + tooth * 1.1f))
}

private fun DrawScope.packedDisc(ink: Color, hole: Color) {
    val r = size.minDimension * 0.36f
    disc(center, r, ink, hole, hub = 0.22f)
    // Tight bands: the disc squeezed into less space.
    listOf(0.88f, 0.72f, 0.56f, 0.4f).forEachIndexed { i, f ->
        drawCircle(
            (if (i % 2 == 0) shade(ink, 0.3f) else light(ink, 0.3f)).copy(alpha = 0.7f),
            r * f,
            center,
            style = Stroke(r * 0.07f),
        )
    }
}

private fun DrawScope.miniDisc(ink: Color, hole: Color) {
    val r = size.minDimension * 0.27f
    drawCircle(shade(ink, 0.4f), r * 1.14f, center + Offset(0f, r * 0.1f))
    drawCircle(light(ink, 0.15f).copy(alpha = 0.5f), r * 1.14f, center, style = Stroke(r * 0.06f))
    disc(center, r, ink, hole, hub = 0.42f)
}

private fun DrawScope.handheldCart(ink: Color) {
    val w = size.width * 0.52f
    val h = size.height * 0.5f
    val rect = Rect(center.x - w / 2, center.y - h / 2 + h * 0.08f, center.x + w / 2, center.y + h / 2 + h * 0.08f)
    // The 3DS card's raised grip along the top edge.
    drawRoundRect(shade(ink, 0.3f), Offset(rect.left + w * 0.05f, rect.top - h * 0.16f), Size(w * 0.9f, h * 0.24f), CornerRadius(w * 0.06f))
    card(rect, ink, corner = w * 0.08f, notch = w * 0.14f)
    label(Rect(rect.left + w * 0.12f, rect.top + h * 0.16f, rect.right - w * 0.12f, rect.bottom - h * 0.16f), ink)
}

private fun DrawScope.trimmedDisc(ink: Color, hole: Color) {
    val r = size.minDimension * 0.34f
    val c = center - Offset(r * 0.08f, 0f)
    val box = Size(r * 2, r * 2)
    drawArc(shade(ink, 0.5f), startAngle = 20f, sweepAngle = 300f, useCenter = true, topLeft = c - Offset(r, r) + Offset(0f, r * 0.09f), size = box)
    drawArc(
        Brush.radialGradient(listOf(light(ink, 0.35f), ink, shade(ink, 0.18f)), center = c - Offset(r * 0.35f, r * 0.35f), radius = r * 1.6f),
        startAngle = 20f,
        sweepAngle = 300f,
        useCenter = true,
        topLeft = c - Offset(r, r),
        size = box,
    )
    drawArc(Color.White.copy(alpha = 0.45f), 200f, 55f, false, c - Offset(r * 0.9f, r * 0.9f), Size(r * 1.8f, r * 1.8f), style = Stroke(r * 0.07f, cap = StrokeCap.Round))
    drawCircle(light(ink, 0.55f), r * 0.26f, c)
    drawCircle(hole, r * 0.1f, c)
    // The trimmed-off slice, drifting away.
    val slice = c + Offset(r * 0.32f, -r * 0.02f)
    drawArc(ink.copy(alpha = 0.35f), startAngle = -40f, sweepAngle = 60f, useCenter = true, topLeft = slice - Offset(r, r), size = box)
}

private fun DrawScope.patchedCart(ink: Color) {
    val w = size.width * 0.54f
    val h = size.height * 0.62f
    val rect = Rect(center.x - w / 2, center.y - h / 2, center.x + w / 2, center.y + h / 2)
    card(rect, ink, corner = w * 0.08f, notch = w * 0.18f)
    label(Rect(rect.left + w * 0.14f, rect.top + h * 0.1f, rect.right - w * 0.14f, rect.top + h * 0.5f), ink)
    // The patch: a bandage stuck on the slant across the cartridge, with its pad and air holes.
    val pivot = Offset(center.x, rect.top + h * 0.56f)
    rotate(-32f, pivot) {
        val bw = w * 1.02f
        val bh = h * 0.2f
        val topLeft = Offset(pivot.x - bw / 2, pivot.y - bh / 2)
        drawRoundRect(shade(ink, 0.5f), topLeft + Offset(0f, bh * 0.14f), Size(bw, bh), CornerRadius(bh / 2))
        drawRoundRect(light(ink, 0.82f), topLeft, Size(bw, bh), CornerRadius(bh / 2))
        drawRoundRect(light(ink, 0.5f), topLeft + Offset(bw * 0.36f, bh * 0.18f), Size(bw * 0.28f, bh * 0.64f), CornerRadius(bh * 0.14f))
        val holes = shade(ink, 0.3f).copy(alpha = 0.5f)
        listOf(0.12f, 0.22f, 0.78f, 0.88f).forEach { f ->
            drawCircle(holes, bh * 0.08f, Offset(topLeft.x + bw * f, pivot.y))
        }
    }
}
