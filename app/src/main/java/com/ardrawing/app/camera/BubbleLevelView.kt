package com.ardrawing.app.camera

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.*

class BubbleLevelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private var rollAngle  = 0f
    private var pitchAngle = 0f

    fun updateAngles(roll: Float, pitch: Float) {
        rollAngle  = roll
        pitchAngle = pitch
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val cx = width / 2f
        val cy = height / 2f
        val r  = (width.coerceAtMost(height) / 2f) * 0.80f

        val totalAngle = sqrt(rollAngle * rollAngle + pitchAngle * pitchAngle)
        val isLevel    = totalAngle < 2f

        // Offset: a 0° los círculos se superponen (offset = 0)
        // A mayor ángulo, más se separan
        val maxOffset = r * 0.55f
        val offsetX   = (rollAngle  / 30f * maxOffset).coerceIn(-maxOffset, maxOffset)
        val offsetY   = (-pitchAngle / 30f * maxOffset).coerceIn(-maxOffset, maxOffset)

        // ── Círculo fijo (gris claro) — siempre en el centro ─────────────────
        val fixedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(230, 190, 190, 190)
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, cy, r, fixedPaint)

        // ── Círculo móvil (gris oscuro) — se aleja según el ángulo ───────────
        val movePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isLevel) Color.argb(230, 60, 60, 60)
                    else         Color.argb(230, 60, 60, 60)
            style = Paint.Style.FILL
        }
        val moveCx = cx + offsetX
        val moveCy = cy + offsetY
        canvas.drawCircle(moveCx, moveCy, r, movePaint)

        // ── Número en el centro de la intersección ────────────────────────────
        val interCx = (cx + moveCx) / 2f
        val interCy = (cy + moveCy) / 2f

        val degPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color     = if (isLevel) Color.argb(255, 80, 255, 80)
                        else         Color.WHITE
            textSize  = r * 0.55f
            textAlign = Paint.Align.CENTER
            typeface  = Typeface.DEFAULT_BOLD
            setShadowLayer(6f, 0f, 0f, Color.BLACK)
        }

        canvas.drawText("%.0f°".format(totalAngle), interCx, interCy + degPaint.textSize * 0.35f, degPaint)
    }
}