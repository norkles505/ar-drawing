package com.ardrawing.app.camera

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs

class OverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private var bitmap: Bitmap? = null
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(80, 255, 255, 255)
        strokeWidth = 1.5f
        style = Paint.Style.STROKE
    }

    private var scaleFactor = 1f
    private var translateX  = 0f
    private var translateY  = 0f
    private var isMirror    = false
    private var isGrid      = false
    private var rotationDeg = 0f
    private var isLocked    = false

    private var lastTouchX      = 0f
    private var lastTouchY      = 0f
    private var activePointerId = MotionEvent.INVALID_POINTER_ID
    private var isScaling       = false

    private var brightness = 0f
    private var contrast   = 1f



    private var isInverted = false

    var onDragStart: (() -> Unit)? = null
    var onDragEnd: (() -> Unit)? = null

    init {
        paint.alpha = 128
    }

    fun setBrightness(value: Float) {
        brightness = value.coerceIn(-1f, 1f)
        updateColorFilter()
        invalidate()
    }

    fun setContrast(value: Float) {
        contrast = value.coerceIn(0f, 2f)
        updateColorFilter()
        invalidate()
    }

    private fun updateColorFilter() {
        val b = brightness * 255f
        val c = contrast
        val brightnessContrast = ColorMatrix(floatArrayOf(
            c, 0f, 0f, 0f, b,
            0f, c, 0f, 0f, b,
            0f, 0f, c, 0f, b,
            0f, 0f, 0f, 1f, 0f
        ))
        if (isInverted) {
            val invert = ColorMatrix(floatArrayOf(
                -1f,  0f,  0f, 0f, 255f,
                0f, -1f,  0f, 0f, 255f,
                0f,  0f, -1f, 0f, 255f,
                0f,  0f,  0f, 1f,   0f
            ))
            invert.preConcat(brightnessContrast)
            paint.colorFilter = ColorMatrixColorFilter(invert)
        } else {
            paint.colorFilter = ColorMatrixColorFilter(brightnessContrast)
        }
    }

    fun setImage(bmp: Bitmap) { bitmap = bmp; invalidate() }
    fun setOpacity(opacity: Float) { paint.alpha = (opacity * 255).toInt().coerceIn(0, 255); invalidate() }
    fun getOpacity(): Float = paint.alpha / 255f
    fun setScale(scale: Float) {
        if (isLocked) return
        scaleFactor = scale.coerceIn(0.3f, 10f)
        invalidate()
    }

    fun setScaleAt(scale: Float, focusX: Float, focusY: Float) {
        if (isLocked) return
        val factor = scale / scaleFactor
        val cx = width / 2f
        val cy = height / 2f
        // focusX/Y en coordenadas de pantalla → convertir al espacio del pivot
        translateX = focusX - cx - factor * (focusX - cx - translateX)
        translateY = focusY - cy - factor * (focusY - cy - translateY)
        scaleFactor = scale.coerceIn(0.3f, 10f)
        invalidate()
    }


    fun setMirror(mirror: Boolean) { isMirror = mirror; invalidate() }
    fun isMirrored(): Boolean = isMirror
    fun setGridVisible(visible: Boolean) { isGrid = visible; invalidate() }
    fun isGridVisible(): Boolean = isGrid
    fun rotateClockwise() { rotationDeg = (rotationDeg + 90f) % 360f; invalidate() }
    fun getRotation90(): Float = rotationDeg
    fun setRotation90(deg: Float) { rotationDeg = deg % 360f; invalidate() }
    fun setLocked(locked: Boolean) { isLocked = locked; invalidate() }
    fun isLocked(): Boolean = isLocked

    fun resetTransform() {
        scaleFactor = 1f
        translateX  = 0f
        translateY  = 0f
        isMirror    = false
        isGrid      = false
        rotationDeg = 0f
        isLocked    = false
        brightness  = 0f
        contrast    = 1f
        paint.alpha = 128
        paint.colorFilter = null
        invalidate()
    }

    data class OverlayState(
        val opacity: Float,
        val scale: Float,
        val translateX: Float,
        val translateY: Float,
        val isMirror: Boolean,
        val isGrid: Boolean,
        val rotationDeg: Float
    )

    fun getState() = OverlayState(
        opacity     = paint.alpha / 255f,
        scale       = scaleFactor,
        translateX  = translateX,
        translateY  = translateY,
        isMirror    = isMirror,
        isGrid      = isGrid,
        rotationDeg = rotationDeg
    )

    fun restoreState(state: OverlayState) {
        paint.alpha = (state.opacity * 255).toInt().coerceIn(0, 255)
        scaleFactor = state.scale
        translateX  = state.translateX
        translateY  = state.translateY
        isMirror    = state.isMirror
        isGrid      = state.isGrid
        rotationDeg = state.rotationDeg
        isLocked    = false
        invalidate()
    }

    fun setScaleKeepPosition(
        newScale: Float,
        baseTranslateX: Float,
        baseTranslateY: Float,
        baseScale: Float
    ) {
        if (isLocked) return
        // El centro de la imagen en pantalla cuando estaba en punto 1:
        // screenPos = pivot + baseTranslate = (cx + baseTranslateX, cy + baseTranslateY)
        // Al hacer zoom desde ese centro, la traslación escala proporcionalmente
        val factor = newScale / baseScale
        translateX = baseTranslateX * factor
        translateY = baseTranslateY * factor
        scaleFactor = newScale.coerceIn(0.3f, 50f)
        invalidate()
    }


    fun setInverted(inverted: Boolean) {
        isInverted = inverted
        updateColorFilter()
        invalidate()
    }


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val bmp = bitmap ?: return

        canvas.save()

        // Pivot = centro de la vista
        val cx = width / 2f
        val cy = height / 2f

        // Aplicar transformaciones desde el centro, luego trasladar
        canvas.translate(cx + translateX, cy + translateY)
        canvas.scale(scaleFactor, scaleFactor)
        canvas.rotate(rotationDeg)
        if (isMirror) canvas.scale(-1f, 1f)
        canvas.translate(-cx, -cy)

        val destRect = computeDestRect(bmp)
        canvas.drawBitmap(bmp, null, destRect, paint)
        canvas.restore()

        if (isGrid) drawGrid(canvas)
        if (isLocked) drawLockIndicator(canvas)
    }
    private fun computeDestRect(bmp: Bitmap): RectF {
        val imageAspect = bmp.width.toFloat() / bmp.height.toFloat()
        val viewAspect  = width.toFloat() / height.toFloat()
        return if (imageAspect > viewAspect) {
            val scaledHeight = width / imageAspect
            val top = (height - scaledHeight) / 2f
            RectF(0f, top, width.toFloat(), top + scaledHeight)
        } else {
            val scaledWidth = height * imageAspect
            val left = (width - scaledWidth) / 2f
            RectF(left, 0f, left + scaledWidth, height.toFloat())
        }
    }

    private fun drawGrid(canvas: Canvas) {
        val bmp = bitmap ?: return
        val dest = computeDestRect(bmp)
        val cols = 4
        val rows = 4
        val cellW = dest.width()  / cols
        val cellH = dest.height() / rows

        for (i in 1 until cols) {
            val x = dest.left + i * cellW
            canvas.drawLine(x, dest.top, x, dest.bottom, gridPaint)
        }
        for (i in 1 until rows) {
            val y = dest.top + i * cellH
            canvas.drawLine(dest.left, y, dest.right, y, gridPaint)
        }

        val borderPaint = Paint(gridPaint).apply {
            color = Color.argb(120, 255, 255, 255)
            strokeWidth = 2f
        }
        canvas.drawRect(dest, borderPaint)

        val cx = dest.centerX()
        val cy = dest.centerY()
        val crossPaint = Paint(gridPaint).apply {
            color = Color.argb(150, 255, 80, 80)
            strokeWidth = 2f
        }
        canvas.drawLine(cx - 20, cy, cx + 20, cy, crossPaint)
        canvas.drawLine(cx, cy - 20, cx, cy + 20, crossPaint)
    }

    private fun drawLockIndicator(canvas: Canvas) {
        val lockPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(200, 255, 60, 60)
            style = Paint.Style.FILL
        }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color     = Color.WHITE
            textSize  = 28f
            textAlign = Paint.Align.CENTER
            typeface  = Typeface.DEFAULT_BOLD
        }
        val r = 28f
        val cx = width - r - 16f
        val cy = r + 16f
        canvas.drawCircle(cx, cy, r, lockPaint)
        canvas.drawText("L", cx, cy + 10f, textPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (isLocked) return true

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (event.pointerCount == 1) {
                    onDragStart?.invoke()
                    lastTouchX      = event.x
                    lastTouchY      = event.y
                    activePointerId = event.getPointerId(0)
                    isScaling       = false
                }
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                isScaling = true
            }
            MotionEvent.ACTION_MOVE -> {
                if (!isScaling && event.pointerCount == 1) {
                    val idx = event.findPointerIndex(activePointerId)
                    if (idx >= 0) {
                        val dx = event.getX(idx) - lastTouchX
                        val dy = event.getY(idx) - lastTouchY
                        if (abs(dx) > 2 || abs(dy) > 2) {
                            translateX += dx
                            translateY += dy
                            lastTouchX  = event.getX(idx)
                            lastTouchY  = event.getY(idx)
                            invalidate()
                        }
                    }
                }
            }
            MotionEvent.ACTION_UP -> {
                if (!isScaling) onDragEnd?.invoke()
                activePointerId = MotionEvent.INVALID_POINTER_ID
                isScaling = false
            }
            MotionEvent.ACTION_POINTER_UP -> {
                if (event.pointerCount <= 2) isScaling = false
            }
        }
        return true
    }
}
