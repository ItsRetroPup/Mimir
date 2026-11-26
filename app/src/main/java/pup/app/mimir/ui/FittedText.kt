package pup.app.mimir.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints

/**
 * Text that never splits a word across lines. When [text] would need more than [maxLines] lines, or
 * one of its words is wider than the space, the font steps down towards [minScale] of [style]'s size.
 * Only if it still does not fit at the smallest size does it ellipsize.
 */
@Composable
internal fun FittedText(
    text: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    maxLines: Int = 1,
    minLines: Int = 1,
    textAlign: TextAlign? = null,
    minScale: Float = 0.7f,
) {
    val measurer = rememberTextMeasurer()
    BoxWithConstraints(modifier) {
        val maxWidth = constraints.maxWidth
        val fitted = remember(text, style, maxWidth, maxLines, minScale, measurer) {
            fitStyle(measurer, text, style, maxWidth, maxLines, minScale)
        }
        Text(
            text,
            style = fitted,
            color = color,
            textAlign = textAlign,
            maxLines = maxLines,
            minLines = minLines,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private const val SCALE_STEP = 0.05f

private fun fitStyle(
    measurer: TextMeasurer,
    text: String,
    style: TextStyle,
    maxWidth: Int,
    maxLines: Int,
    minScale: Float,
): TextStyle {
    if (maxWidth == Constraints.Infinity || text.isEmpty()) return style
    val words = unbreakableWords(text)
    var scale = 1f
    while (true) {
        val candidate = if (scale == 1f) style else style.copy(
            fontSize = style.fontSize * scale,
            lineHeight = style.lineHeight * scale,
        )
        if (fits(measurer, text, words, candidate, maxWidth, maxLines)) return candidate
        val next = scale - SCALE_STEP
        if (next < minScale - 0.001f) return candidate
        scale = next
    }
}

private fun fits(
    measurer: TextMeasurer,
    text: String,
    words: List<String>,
    style: TextStyle,
    maxWidth: Int,
    maxLines: Int,
): Boolean {
    val layout = measurer.measure(
        text,
        style,
        overflow = TextOverflow.Clip,
        maxLines = maxLines,
        constraints = Constraints(maxWidth = maxWidth),
    )
    if (layout.hasVisualOverflow) return false
    // A word wider than the line would be split inside it without overflowing, so check each one.
    return words.all { word -> measurer.measure(word, style, softWrap = false, maxLines = 1).size.width <= maxWidth }
}

/**
 * The pieces of [text] a line may not break inside: words split at spaces and after hyphens.
 * Chinese and Japanese break between characters by design, so their words are left out.
 */
private fun unbreakableWords(text: String): List<String> =
    text.split(Regex("\\s+|(?<=-)"))
        .filter { it.isNotBlank() && it.none(::breaksBetweenCharacters) }

private fun breaksBetweenCharacters(char: Char): Boolean = when (Character.UnicodeScript.of(char.code)) {
    Character.UnicodeScript.HAN,
    Character.UnicodeScript.HIRAGANA,
    Character.UnicodeScript.KATAKANA,
    -> true
    else -> false
}
