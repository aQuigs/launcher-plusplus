package com.sqftware.orbitlauncher.ui

import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isSpecified

private const val SCALE_STEP = 0.05f

/**
 * [style] shrunk, font and line height together, just enough that every one of [texts] fits [maxWidth] px in [maxLines]
 * lines that each end between words: one size for a set of labels, so none is cut short, split mid-word or larger than
 * its neighbours at any font or display size. Null when they would need shrinking past [minScale].
 */
fun TextMeasurer.fitWholeWords(texts: List<String>, style: TextStyle, maxWidth: Int, maxLines: Int, minScale: Float): TextStyle? {
    var scale = 1f
    while (scale >= minScale) {
        val scaled = style.copy(fontSize = style.fontSize.scaled(scale), lineHeight = style.lineHeight.scaled(scale))
        val fits = texts.all { text ->
            measure(text, scaled, maxLines = maxLines, constraints = Constraints(maxWidth = maxWidth)).breaksBetweenWords(text)
        }
        if (fits) return scaled
        scale -= SCALE_STEP
    }
    return null
}

private fun TextUnit.scaled(by: Float) = if (isSpecified) this * by else this

/** Whether [text] laid out here shows whole: within its line limit, and every line but the last ends between words. */
internal fun TextLayoutResult.breaksBetweenWords(text: String): Boolean =
    !multiParagraph.didExceedMaxLines && (0 until lineCount - 1).all { line ->
        val last = text[getLineEnd(line) - 1]
        last.isWhitespace() || last == '-'
    }
