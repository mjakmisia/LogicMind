package com.example.logicmind.additional

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.util.AttributeSet
import android.view.View

/**
 * Widok-nakładka do rysowania linii zaznaczenia (tymczasowych i stałych) nad siatką WordSearch.
 */

class SelectionOverlayView(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    data class LineData(val start: PointF, val end: PointF, val color: Int)
    private val permanentLines = mutableListOf<LineData>()
    private var tempLine: LineData? = null
    private val linePaint = Paint().apply {
        isAntiAlias = true
        strokeCap = Paint.Cap.ROUND
        style = Paint.Style.STROKE
    }
    private var lineStrokeWidth: Float = 60f

    fun setLineStrokeWidth(width: Float) {
        lineStrokeWidth = width * 0.75f
    }

    fun setTemporaryLine(start: PointF, end: PointF, color: Int) {
        tempLine = LineData(start, end, color)
        invalidate()
    }

    fun clearTemporaryLine() {
        if (tempLine != null) {
            tempLine = null
            invalidate()
        }
    }

    fun addPermanentLine(start: PointF, end: PointF, color: Int) {
        permanentLines.add(LineData(start, end, color))
        invalidate()
    }

    fun clearAllLines() {
        permanentLines.clear()
        tempLine = null
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        linePaint.strokeWidth = lineStrokeWidth

        permanentLines.forEach { line ->
            linePaint.color = line.color
            linePaint.alpha = 150 // ok. 60% przezroczystości
            canvas.drawLine(line.start.x, line.start.y, line.end.x, line.end.y, linePaint)
        }

        tempLine?.let { line ->
            linePaint.color = line.color
            linePaint.alpha = 150
            canvas.drawLine(line.start.x, line.start.y, line.end.x, line.end.y, linePaint)
        }
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}