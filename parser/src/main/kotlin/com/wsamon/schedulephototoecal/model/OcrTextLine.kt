package com.wsamon.schedulephototoecal.model

/**
 * A single line of OCR-recognized text with its pixel bounding box.
 *
 * Deliberately independent of any Android/ML Kit type so the parser package
 * can be compiled and unit-tested as plain JVM code.
 */
data class OcrTextLine(
    val text: String,
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
) {
    fun verticalCenter(): Int = (top + bottom) / 2
}
