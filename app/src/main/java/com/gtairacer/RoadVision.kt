package com.gtairacer

import android.media.Image
import kotlin.math.abs

/**
 * Lightweight, deliberately conservative asphalt-region detector.
 * It is a prototype heuristic, not a trained GTA scene-understanding model.
 */
object RoadVision {
    data class Result(val steering: Float, val confidence: Float, val roadCenter: Float)

    fun analyze(image: Image): Result? {
        val plane = image.planes.firstOrNull() ?: return null
        val buffer = plane.buffer
        val width = image.width
        val height = image.height
        val pixelStride = plane.pixelStride
        val rowStride = plane.rowStride
        val columns =  sixtyColumns(width)
        val rows = intArrayOf((height * 0.56f).toInt(), (height * 0.64f).toInt(), (height * 0.72f).toInt())
        val scores = IntArray(columns.size)

        for (r in rows) {
            for (i in columns.indices) {
                val x = columns[i]
                val y = r.coerceIn(0, height - 1)
                val offset = y * rowStride + x * pixelStride
                if (offset < 0 || offset + 2 >= buffer.limit()) continue
                val red = buffer.get(offset).toInt() and 0xff
                val green = buffer.get(offset + 1).toInt() and 0xff
                val blue = buffer.get(offset + 2).toInt() and 0xff
                val max = maxOf(red, green, blue)
                val min = minOf(red, green, blue)
                val brightness = (red + green + blue) / 3
                val saturation = max - min
                // Asphalt is often relatively neutral and mid/dark; avoid very dark UI.
                if (brightness in 38..190 && saturation < 48) scores[i]++
            }
        }

        val road = BooleanArray(scores.size) { scores[it] >= 2 }
        var bestStart = -1
        var bestEnd = -1
        var start = -1
        for (i in road.indices) {
            if (road[i]) {
                if (start < 0) start = i
            } else if (start >= 0) {
                if (bestStart < 0 || i - start > bestEnd - bestStart) {
                    bestStart = start
                    bestEnd = i
                }
                start = -1
            }
        }
        if (start >= 0 && (bestStart < 0 || road.size - start > bestEnd - bestStart)) {
            bestStart = start
            bestEnd = road.size
        }
        if (bestStart < 0) return null

        val span = bestEnd - bestStart
        val confidence = span.toFloat() / road.size.toFloat()
        if (confidence < 0.22f) return null
        val centerColumn = (bestStart + bestEnd - 1) / 2
        val roadCenter = columns[centerColumn].toFloat() / width.toFloat()
        // Positive means steer right. Limit corrections to avoid abrupt swerves.
        val steering = ((roadCenter - 0.5f) * 2.4f).coerceIn(-0.55f, 0.55f)
        return Result(steering, confidence, roadCenter)
    }

    private fun sixtyColumns(width: Int): IntArray =
        IntArray(60) { index -> ((width * (0.12f + index * (0.76f / 59f))).toInt()).coerceIn(0, width - 1) }
}
