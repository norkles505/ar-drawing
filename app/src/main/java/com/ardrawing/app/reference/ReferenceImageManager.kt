package com.ardrawing.app.reference

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Outline
import android.graphics.PorterDuff
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.ardrawing.app.R
import com.ardrawing.app.camera.OverlayView

/**
 * Administra las imágenes de referencia de una sesión de dibujo: miniaturas,
 * selección, borrado y sincronización con [overlayView].
 *
 * Extraído de DrawingActivity para aislar esta funcionalidad autocontenida
 * (antes vivía mezclada con cámara, timer y guardado en un solo archivo).
 * El comportamiento reproduce exactamente el original, incluyendo sus
 * particularidades (por ejemplo las dos variantes de tamaño del botón "+",
 * una para el arranque y otra para después de borrar una referencia).
 */
class ReferenceImageManager(
    private val activity: Activity,
    private val container: LinearLayout,
    private val overlayView: OverlayView,
    private val onReferenceApplied: (bitmap: Bitmap, scale: Float) -> Unit,
    private val onLockStateChanged: (locked: Boolean) -> Unit,
    private val onImagePickRequested: () -> Unit
) {
    private val bitmaps = mutableListOf<Bitmap>()
    private val states = mutableMapOf<Int, OverlayView.OverlayState>()
    private val lockStates = mutableMapOf<Int, Boolean>()
    private var activeIndex = -1

    /** Naranja de acento de la app, leído del recurso en vez de hardcodeado. */
    private val accentColor = ContextCompat.getColor(activity, R.color.mc_accent)

    /** Lista de referencias actuales, en el mismo orden que se muestran. */
    val referenceBitmaps: List<Bitmap> get() = bitmaps

    /** Precarga bitmaps ya existentes (ej. al reabrir un proyecto guardado), sin crear miniaturas todavía. */
    fun preload(bmps: List<Bitmap>) {
        bitmaps.addAll(bmps)
    }

    /** Construye las miniaturas de arranque y el botón "+". Llamar una sola vez, después de [preload] si aplica. */
    fun setup(fallbackInitial: Bitmap?) {
        if (bitmaps.isEmpty()) {
            fallbackInitial?.let { bitmaps.add(it) }
        }

        bitmaps.forEachIndexed { index, bmp -> addThumbnail(bmp, index) }

        if (bitmaps.isNotEmpty()) {
            activeIndex = 0
            for (i in 0 until container.childCount) {
                val v = container.getChildAt(i)
                if (v is ImageView) v.alpha = if (i == 0) 1f else 0.5f
            }
        }

        val addBtn = ImageButton(activity).apply {
            tag = "btnAddReference"
            layoutParams = LinearLayout.LayoutParams(60.dpToPx(), 60.dpToPx()).also { it.marginEnd = 4.dpToPx() }
            setImageResource(android.R.drawable.ic_input_add)
            setColorFilter(accentColor, PorterDuff.Mode.SRC_IN)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 10.dpToPx().toFloat()
                setColor(Color.parseColor("#1A1A1A"))
                setStroke(1.dpToPx(), accentColor)
            }
            setOnClickListener { onImagePickRequested() }
        }
        container.addView(addBtn)
    }

    /** Se llama cuando el usuario elige una imagen nueva desde el picker del sistema. */
    fun addNew(bmp: Bitmap) {
        if (bitmaps.size >= 5) {
            Toast.makeText(activity, "Máximo 5 imágenes de referencia", Toast.LENGTH_SHORT).show()
            return
        }
        bitmaps.add(bmp)
        val index = bitmaps.size - 1
        addThumbnail(bmp, index)
        select(index)
    }

    private fun addThumbnail(bmp: Bitmap, index: Int) {
        val thumb = ImageView(activity).apply {
            val size = 60.dpToPx()
            val margin = 4.dpToPx()
            layoutParams = LinearLayout.LayoutParams(size, size).also { it.marginEnd = margin }
            scaleType = ImageView.ScaleType.CENTER_CROP
            setImageBitmap(bmp)
            clipToOutline = true

            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 10.dpToPx().toFloat()
                setColor(Color.BLACK)
                setStroke(2.dpToPx(), Color.parseColor("#444444"))
            }

            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setRoundRect(0, 0, view.width, view.height, 10.dpToPx().toFloat())
                }
            }

            setOnClickListener { select(index) }
            setOnLongClickListener {
                confirmDelete(index)
                true
            }
        }

        val addBtn = container.findViewWithTag<ImageButton>("btnAddReference")
        val insertPos = if (addBtn != null) container.indexOfChild(addBtn) else container.childCount
        container.addView(thumb, insertPos)
    }

    private fun select(index: Int) {
        if (activeIndex >= 0) {
            states[activeIndex] = overlayView.getState()
            lockStates[activeIndex] = overlayView.isLocked()
        }

        activeIndex = index
        val bmp = bitmaps[index]
        overlayView.setImage(bmp)

        val savedState = states[index]
        val scale: Float
        if (savedState != null) {
            overlayView.restoreState(savedState)
            scale = savedState.scale
        } else {
            overlayView.resetTransform()
            scale = 1f
        }

        val locked = lockStates[index] ?: false
        overlayView.setLocked(locked)

        highlightThumbnail(index)

        onReferenceApplied(bmp, scale)
        onLockStateChanged(locked)
    }

    private fun highlightThumbnail(index: Int) {
        for (i in 0 until container.childCount) {
            val v = container.getChildAt(i)
            if (v is ImageView) {
                v.alpha = 1f
                v.background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = 10.dpToPx().toFloat()
                    setColor(Color.BLACK)
                    setStroke(
                        if (i == index) 3.dpToPx() else 1.dpToPx(),
                        if (i == index) accentColor else Color.parseColor("#444444")
                    )
                }
            }
        }
    }

    private fun confirmDelete(index: Int) {
        if (bitmaps.size <= 1) {
            Toast.makeText(activity, "Debe haber al menos una imagen", Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(activity)
            .setTitle("Eliminar imagen")
            .setMessage("¿Eliminar esta imagen de referencia?")
            .setPositiveButton("Eliminar") { _, _ -> performDelete(index) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun performDelete(index: Int) {
        bitmaps.removeAt(index)

        val newStates = mutableMapOf<Int, OverlayView.OverlayState>()
        states.forEach { (i, state) ->
            when {
                i < index -> newStates[i] = state
                i > index -> newStates[i - 1] = state
            }
        }
        states.clear()
        states.putAll(newStates)

        val newLocks = mutableMapOf<Int, Boolean>()
        lockStates.forEach { (i, lock) ->
            when {
                i < index -> newLocks[i] = lock
                i > index -> newLocks[i - 1] = lock
            }
        }
        lockStates.clear()
        lockStates.putAll(newLocks)

        container.removeAllViews()
        bitmaps.forEachIndexed { i, bmp -> addThumbnail(bmp, i) }

        val addBtn = ImageButton(activity).apply {
            tag = "btnAddReference"
            layoutParams = LinearLayout.LayoutParams(56.dpToPx(), 56.dpToPx()).also { it.marginEnd = 4.dpToPx() }
            setImageResource(android.R.drawable.ic_input_add)
            setBackgroundResource(android.R.drawable.btn_default)
            setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
            alpha = 0.7f
            setOnClickListener { onImagePickRequested() }
        }
        container.addView(addBtn)

        val newIndex = (index - 1).coerceAtLeast(0)
        activeIndex = newIndex
        select(newIndex)

        Toast.makeText(activity, "Imagen eliminada", Toast.LENGTH_SHORT).show()
    }

    private fun Int.dpToPx(): Int = (this * activity.resources.displayMetrics.density).toInt()
}
