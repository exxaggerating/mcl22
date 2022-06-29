package com.example.speechmood.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import com.example.speechmood.audio.AudioRecorderDataReceiveListener
import com.example.speechmood.util.RingArrayList
import kotlin.math.abs

class SoundVisualiser: View, AudioRecorderDataReceiveListener {

    private val coordinatesPerLine: Int = 4
    private val visualSamplingRate: Int = 16
    private val maxLineHeightInPercentage: Float = 0.95F

    private var samplesToVisualise: RingArrayList<Float>? = null
    private var linesCoordinates: FloatArray = FloatArray(0)

    private val rect: Rect = Rect()
    private val forePaint: Paint = Paint()

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int)
            : super(context, attrs, defStyleAttr)

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        samplesToVisualise?.let {
            if (it.size > 0) {
                rect.set(0, 0, width, height)
                canvas?.drawLines(
                    linesCoordinates,
                    0,
                    it.size * coordinatesPerLine,
                    forePaint
                )
            }
        }
    }

    override fun onDataReceive(buffer: Array<Float>, samples: Int) {
        if (samplesToVisualise == null) {
            samplesToVisualise = RingArrayList(if (width > 0) width else samples)

            linesCoordinates = FloatArray(samplesToVisualise!!.capacity() * coordinatesPerLine)
        } else if (samplesToVisualise!!.size != width) {
            // TODO: Resize
        }

        for (i in 0 until samples step visualSamplingRate) {
            var max: Float = buffer[i];

            for (j in i until (i + visualSamplingRate)) {
                if (j < samples && max < buffer[j]) {
                    max = buffer[j]
                }
            }

            samplesToVisualise?.add(max)
        }

        convertSamplesToLinesCoordinates()
        invalidate()
    }

    private fun convertSamplesToLinesCoordinates() {
        samplesToVisualise?.let {

            for (i in 0 until samplesToVisualise!!.size) {
                /* Draw a series of lines. Each line is taken from 4 consecutive values in the pts array. Thus
                 * to draw 1 line, the array must contain at least 4 values. This is logically the same as
                 * drawing the array as follows: drawLine(pts[0], pts[1], pts[2], pts[3]) followed by
                 * drawLine(pts[4], pts[5], pts[6], pts[7]) and so on.
                 *
                 * @param pts Array of points to draw [x0 y0 x1 y1 x2 y2 ...]
                 *
                 * * (x0, y0)
                 * |
                 * | lineLength
                 * |
                 * * (x1, y1) ...x0 = x1
                 */

                /* lineLength */
                val lineLength: Float = rect.height() * maxLineHeightInPercentage * abs(it[i])
                /* x0 */
                linesCoordinates[i * 4] = rect.width() * (i.toFloat() / it.size)
                /* y0 */
                linesCoordinates[i * 4 + 1] = rect.height() / 2.0f - lineLength / 2.0f
                /* x1 = x0 */
                linesCoordinates[i * 4 + 2] = linesCoordinates[i * 4]
                /* y1 = y0 - lineLength */
                linesCoordinates[i * 4 + 3] = linesCoordinates[i * 4 + 1] + lineLength
            }
        }
    }
}
