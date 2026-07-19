package com.ardrawing.app.camera

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.view.View

/**
 * Regla física precisa dibujada sobre los bordes izquierdo y superior.
 * Usa xdpi / ydpi para convertir píxeles → cm reales.
 */
class RulerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private val pxPerCmX: Float
    private val pxPerCmY: Float

    private val rulerWidth = 48f  // dp del ancho de la regla

    private val bgPaint = Paint().apply {
        color = Color.TRANSPARENT
        style = Paint.Style.FILL
    }
    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color       = Color.argb(220, 30, 30, 30)
        strokeWidth = 1.5f
        style       = Paint.Style.STROKE
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color    = Color.argb(220, 30, 30, 30)
        textSize = 22f
        typeface = Typeface.DEFAULT_BOLD
    }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color       = Color.argb(180, 100, 100, 100)
        strokeWidth = 1f
        style       = Paint.Style.STROKE
    }

    init {
        val dm = context.resources.displayMetrics
        // DisplayMetrics.xdpi = píxeles por pulgada real del hardware
        pxPerCmX = dm.xdpi / 2.54f
        pxPerCmY = dm.ydpi / 2.54f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val rw = rulerWidth * resources.displayMetrics.density

        drawHorizontalRuler(canvas, rw)
        drawVerticalRuler(canvas, rw)

        // Esquina tapada (cuadrado crema para que no se superpongan las dos reglas)
        canvas.drawRect(0f, 0f, rw, rw, bgPaint)
    }

    /** Regla superior — mide a lo largo del eje X */
    private fun drawHorizontalRuler(canvas: Canvas, rw: Float) {
        canvas.drawRect(0f, 0f, width.toFloat(), rw, bgPaint)
        canvas.drawLine(0f, rw, width.toFloat(), rw, borderPaint)

        val totalCm = width / pxPerCmX
        val totalMm = (totalCm * 10).toInt()

        for (mm in 0..totalMm) {
            val x = mm * pxPerCmX / 10f
            if (x > width) break

            val isCm   = mm % 10 == 0
            val isMidCm = mm % 5 == 0

            val tickH = when {
                isCm    -> rw * 0.65f
                isMidCm -> rw * 0.45f
                else    -> rw * 0.25f
            }

            canvas.drawLine(x, 0f, x, tickH, tickPaint)

            if (isCm && mm > 0) {
                val label = (mm / 10).toString()
                val tw = textPaint.measureText(label)
                canvas.drawText(label, x - tw / 2f, rw * 0.92f, textPaint)
            }
        }
    }

    /** Regla izquierda — mide a lo largo del eje Y */
    private fun drawVerticalRuler(canvas: Canvas, rw: Float) {
        canvas.drawRect(0f, 0f, rw, height.toFloat(), bgPaint)
        canvas.drawLine(rw, 0f, rw, height.toFloat(), borderPaint)

        val totalCm = height / pxPerCmY
        val totalMm = (totalCm * 10).toInt()

        for (mm in 0..totalMm) {
            val y = mm * pxPerCmY / 10f
            if (y > height) break

            val isCm    = mm % 10 == 0
            val isMidCm = mm % 5 == 0

            val tickH = when {
                isCm    -> rw * 0.65f
                isMidCm -> rw * 0.45f
                else    -> rw * 0.25f
            }

            canvas.drawLine(0f, y, tickH, y, tickPaint)

            if (isCm && mm > 0) {
                canvas.save()
                canvas.rotate(-90f, rw * 0.75f, y)
                val label = (mm / 10).toString()
                val tw = textPaint.measureText(label)
                canvas.drawText(label, rw * 0.75f - tw / 2f, y + 7f, textPaint)
                canvas.restore()
            }
        }
    }
}