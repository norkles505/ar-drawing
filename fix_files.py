overlay = """package com.ardrawing.app.camera

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
        val cm = ColorMatrix(floatArrayOf(
            c, 0f, 0f, 0f, b,
            0f, c, 0f, 0f, b,
            0f, 0f, c, 0f, b,
            0f, 0f, 0f, 1f, 0f
        ))
        paint.colorFilter = ColorMatrixColorFilter(cm)
    }

    fun setImage(bmp: Bitmap) { bitmap = bmp; invalidate() }
    fun setOpacity(opacity: Float) { paint.alpha = (opacity * 255).toInt().coerceIn(0, 255); invalidate() }
    fun getOpacity(): Float = paint.alpha / 255f
    fun setScale(scale: Float) { if (isLocked) return; scaleFactor = scale.coerceIn(0.3f, 5f); invalidate() }
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

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val bmp = bitmap ?: return

        canvas.save()
        canvas.translate(translateX, translateY)
        canvas.scale(scaleFactor, scaleFactor, width / 2f, height / 2f)
        canvas.rotate(rotationDeg, width / 2f, height / 2f)
        if (isMirror) canvas.scale(-1f, 1f, width / 2f, height / 2f)
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
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                activePointerId = MotionEvent.INVALID_POINTER_ID
                isScaling       = false
            }
            MotionEvent.ACTION_POINTER_UP -> {
                if (event.pointerCount <= 2) isScaling = false
            }
        }
        return true
    }
}
"""

drawing = """package com.ardrawing.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.EditText
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.ardrawing.app.camera.OverlayView
import com.ardrawing.app.data.ProjectManager
import com.ardrawing.app.databinding.ActivityDrawingBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class DrawingActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_RESOURCE_ID = "extra_resource_id"
        const val EXTRA_IMAGE_URI   = "extra_image_uri"
        const val EXTRA_PROJECT_ID  = "extra_project_id"
    }

    private lateinit var binding: ActivityDrawingBinding
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var scaleGestureDetector: ScaleGestureDetector
    private lateinit var overlayView: OverlayView
    private lateinit var projectManager: ProjectManager
    private lateinit var sensorManager: SensorManager

    private var camera: Camera? = null
    private var accelerometer: Sensor? = null

    private var currentScale      = 1f
    private var currentZoom       = 1f
    private var isGridVisible     = false
    private var isMirrorOn        = false
    private var isLocked          = false
    private var isControlsVisible = true
    private var isRulerVisible    = false
    private var isFlashOn         = false
    private var isLevelVisible    = false
    private var isBrightnessOpen  = false
    private var isSizeVisible     = false

    private var currentBitmap: Bitmap? = null
    private var originalUri: String?   = null
    private var activeProjectId: String? = null

    private val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            binding.bubbleLevelView.updateAngles(event.values[0] * 3f, event.values[1] * 3f)
        }
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startCamera()
        else { Toast.makeText(this, "Se necesita permiso de camara", Toast.LENGTH_LONG).show(); finish() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDrawingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        overlayView    = binding.overlayView
        cameraExecutor = Executors.newSingleThreadExecutor()
        projectManager = ProjectManager(this)
        sensorManager  = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer  = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        activeProjectId = intent.getStringExtra(EXTRA_PROJECT_ID)
        if (activeProjectId != null) loadExistingProject(activeProjectId!!)
        else loadImageFromIntent()

        setupControls()
        setupGestureDetector()
        checkCameraPermission()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        sensorManager.unregisterListener(sensorListener)
    }

    private fun loadImageFromIntent() {
        val resourceId = intent.getIntExtra(EXTRA_RESOURCE_ID, -1)
        val uriString  = intent.getStringExtra(EXTRA_IMAGE_URI)
        originalUri    = uriString

        val bitmap: Bitmap? = when {
            resourceId != -1  -> BitmapFactory.decodeResource(resources, resourceId)
            uriString != null -> decodeBitmapFromUri(Uri.parse(uriString))
            else              -> null
        }

        if (bitmap != null) { currentBitmap = bitmap; overlayView.setImage(bitmap) }
        else Toast.makeText(this, "No se pudo cargar la imagen", Toast.LENGTH_SHORT).show()
    }

    private fun decodeBitmapFromUri(uri: Uri): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, _, _ -> decoder.isMutableRequired = true }
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(contentResolver, uri)
            }
        } catch (e: Exception) {
            try { contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) } }
            catch (e2: Exception) { null }
        }
    }

    private fun loadExistingProject(id: String) {
        val project = projectManager.getAllProjects().find { it.id == id }
        if (project == null) { Toast.makeText(this, "Proyecto no encontrado", Toast.LENGTH_SHORT).show(); finish(); return }

        originalUri = project.imageUri
        val bitmap = projectManager.loadBitmap(project)
        if (bitmap != null) { currentBitmap = bitmap; overlayView.setImage(bitmap) }
        else Toast.makeText(this, "No se encontro la imagen del proyecto", Toast.LENGTH_SHORT).show()

        val state = projectManager.toOverlayState(project)
        overlayView.restoreState(state)
        currentScale  = state.scale
        isGridVisible = state.isGrid
        isMirrorOn    = state.isMirror

        binding.seekbarOpacity.progress = (state.opacity * 100).toInt()
        binding.btnGrid.alpha           = if (isGridVisible) 1f else 0.5f
        binding.btnMirror.alpha         = if (isMirrorOn) 1f else 0.5f
        Toast.makeText(this, "Proyecto cargado", Toast.LENGTH_SHORT).show()
    }

    private fun setupControls() {

        // Opacidad
        binding.seekbarOpacity.progress = 50
        binding.seekbarOpacity.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) { overlayView.setOpacity(progress / 100f) }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        // Brillo
        binding.seekbarBrightness.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) { overlayView.setBrightness((progress - 100) / 100f) }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        // Contraste
        binding.seekbarContrast.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) { overlayView.setContrast(progress / 100f) }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        // Zoom camara + overlay sincronizado
        binding.seekbarZoom.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                currentZoom  = 1f + (progress / 100f) * 9f
                currentScale = currentZoom
                camera?.cameraControl?.setZoomRatio(currentZoom)
                overlayView.setScale(currentZoom)
                binding.tvZoomLevel.text = "%.1fx".format(currentZoom)
                updateSizeLabel()
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        binding.btnZoomReset.setOnClickListener {
            currentZoom = 1f; currentScale = 1f
            binding.seekbarZoom.progress = 0
            camera?.cameraControl?.setZoomRatio(1f)
            overlayView.setScale(1f)
            binding.tvZoomLevel.text = "1.0x"
            updateSizeLabel()
        }

        // Grid
        binding.btnGrid.setOnClickListener {
            isGridVisible = !isGridVisible
            overlayView.setGridVisible(isGridVisible)
            binding.btnGrid.alpha = if (isGridVisible) 1f else 0.5f
        }

        // Mirror
        binding.btnMirror.setOnClickListener {
            isMirrorOn = !isMirrorOn
            overlayView.setMirror(isMirrorOn)
            binding.btnMirror.alpha = if (isMirrorOn) 1f else 0.5f
        }

        // Lock
        binding.btnLock.setOnClickListener {
            isLocked = !isLocked
            overlayView.setLocked(isLocked)
            binding.btnLock.alpha = if (isLocked) 1f else 0.5f
            Toast.makeText(this, if (isLocked) "Imagen bloqueada" else "Imagen desbloqueada", Toast.LENGTH_SHORT).show()
        }

        // Rotar
        binding.btnRotate.setOnClickListener { overlayView.rotateClockwise() }

        // Atras
        binding.btnBack.setOnClickListener { confirmDiscard() }

        // Toggle controles
        binding.btnToggleControls.setOnClickListener {
            isControlsVisible = !isControlsVisible
            binding.controlsPanel.visibility = if (isControlsVisible) View.VISIBLE else View.GONE
            binding.btnToggleControls.setImageResource(
                if (isControlsVisible) android.R.drawable.arrow_down_float
                else android.R.drawable.arrow_up_float
            )
        }

        // Reset
        binding.btnReset.setOnClickListener {
            currentScale = 1f; currentZoom = 1f
            isGridVisible = false; isMirrorOn = false; isLocked = false
            overlayView.resetTransform()
            binding.seekbarOpacity.progress    = 50
            binding.seekbarBrightness.progress = 100
            binding.seekbarContrast.progress   = 100
            binding.seekbarZoom.progress       = 0
            binding.tvZoomLevel.text           = "1.0x"
            camera?.cameraControl?.setZoomRatio(1f)
            binding.btnGrid.alpha   = 0.5f
            binding.btnMirror.alpha = 0.5f
            binding.btnLock.alpha   = 0.5f
        }

        // Guardar
        binding.btnSaveProject.setOnClickListener { showSaveProjectDialog() }

        // Regla
        binding.btnRuler.setOnClickListener {
            isRulerVisible = !isRulerVisible
            binding.rulerView.visibility = if (isRulerVisible) View.VISIBLE else View.GONE
            binding.btnRuler.alpha = if (isRulerVisible) 1f else 0.5f
        }

        // Flash
        binding.btnFlash.setOnClickListener {
            isFlashOn = !isFlashOn
            camera?.cameraControl?.enableTorch(isFlashOn)
            binding.btnFlash.alpha = if (isFlashOn) 1f else 0.5f
        }

        // Brillo/Contraste panel
        binding.btnBrightness.setOnClickListener {
            isBrightnessOpen = !isBrightnessOpen
            binding.brightnessPanel.visibility = if (isBrightnessOpen) View.VISIBLE else View.GONE
            binding.btnBrightness.alpha = if (isBrightnessOpen) 1f else 0.5f
        }

        // Zoom panel
        binding.btnZoom.setOnClickListener {
            binding.zoomPanel.visibility =
                if (binding.zoomPanel.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }

        // Tamano en cm
        binding.btnSize.setOnClickListener {
            isSizeVisible = !isSizeVisible
            binding.tvImageSize.visibility = if (isSizeVisible) View.VISIBLE else View.GONE
            binding.btnSize.alpha = if (isSizeVisible) 1f else 0.5f
            if (isSizeVisible) binding.overlayView.post { updateSizeLabel() }
        }

        // Nivel de burbuja
        binding.btnLevel.setOnClickListener {
            isLevelVisible = !isLevelVisible
            binding.bubbleLevelView.visibility = if (isLevelVisible) View.VISIBLE else View.GONE
            binding.btnLevel.alpha = if (isLevelVisible) 1f else 0.5f
            if (isLevelVisible) accelerometer?.let { sensorManager.registerListener(sensorListener, it, SensorManager.SENSOR_DELAY_UI) }
            else sensorManager.unregisterListener(sensorListener)
        }
    }

    private fun updateSizeLabel() {
        val bmp = currentBitmap ?: return
        val dm  = resources.displayMetrics
        val imageAspect = bmp.width.toFloat() / bmp.height.toFloat()
        val viewW = binding.overlayView.width.toFloat()
        val viewH = binding.overlayView.height.toFloat()
        if (viewW == 0f || viewH == 0f) return

        val baseW: Float
        val baseH: Float
        if (imageAspect > viewW / viewH) { baseW = viewW; baseH = viewW / imageAspect }
        else { baseH = viewH; baseW = viewH * imageAspect }

        val realW = (baseW * currentScale) / (dm.xdpi / 2.54f)
        val realH = (baseH * currentScale) / (dm.ydpi / 2.54f)
        binding.tvImageSize.text = "%.1f x %.1f cm".format(realW, realH)
    }

    private fun setupGestureDetector() {
        scaleGestureDetector = ScaleGestureDetector(this,
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    if (overlayView.isLocked()) return true
                    currentScale *= detector.scaleFactor
                    currentScale  = currentScale.coerceIn(0.3f, 5f)
                    overlayView.setScale(currentScale)
                    if (isSizeVisible) updateSizeLabel()
                    return true
                }
            })

        binding.overlayView.setOnTouchListener { _, event ->
            scaleGestureDetector.onTouchEvent(event)
            overlayView.onTouchEvent(event)
            true
        }

        binding.previewView.setOnClickListener { v ->
            val factory = binding.previewView.meteringPointFactory
            val point   = factory.createPoint(v.width / 2f, v.height / 2f)
            val action  = FocusMeteringAction.Builder(point).disableAutoCancel().build()
            camera?.cameraControl?.startFocusAndMetering(action)
        }
    }

    private fun showSaveProjectDialog() {
        val existingName = activeProjectId?.let { id -> projectManager.getAllProjects().find { it.id == id }?.name } ?: ""
        val editText = EditText(this).apply { hint = "Nombre del proyecto"; setText(existingName); setSingleLine() }
        val container = android.widget.FrameLayout(this).apply {
            val pad = (20 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad / 2, pad, 0); addView(editText)
        }
        AlertDialog.Builder(this)
            .setTitle(if (activeProjectId != null) "Actualizar proyecto" else "Guardar proyecto")
            .setView(container)
            .setPositiveButton("Guardar") { _, _ -> doSaveProject(editText.text.toString().ifBlank { "Proyecto sin nombre" }) }
            .setNegativeButton("Cancelar", null).show()
    }

    private fun doSaveProject(name: String) {
        val saved = projectManager.saveProject(activeProjectId, name, currentBitmap, originalUri, overlayView.getState())
        activeProjectId = saved.id
        Toast.makeText(this, "Guardado: ${saved.name}", Toast.LENGTH_SHORT).show()
    }

    private fun confirmDiscard() {
        AlertDialog.Builder(this)
            .setTitle("Salir")
            .setMessage("Guardar antes de salir?")
            .setPositiveButton("Guardar y salir") { _, _ ->
                val name = activeProjectId?.let { id -> projectManager.getAllProjects().find { it.id == id }?.name } ?: ""
                if (name.isNotBlank()) { doSaveProject(name); finish() } else showSaveProjectDialog()
            }
            .setNegativeButton("Salir sin guardar") { _, _ -> finish() }
            .setNeutralButton("Cancelar", null).show()
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) startCamera()
        else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    private fun startCamera() {
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener({
            val provider = future.get()
            val preview  = Preview.Builder().build().also { it.setSurfaceProvider(binding.previewView.surfaceProvider) }
            try {
                provider.unbindAll()
                camera = provider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview)
                binding.previewView.post {
                    val factory = binding.previewView.meteringPointFactory
                    val point   = factory.createPoint(binding.previewView.width / 2f, binding.previewView.height / 2f)
                    camera?.cameraControl?.startFocusAndMetering(FocusMeteringAction.Builder(point).disableAutoCancel().build())
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Error de camara: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }
}
"""

with open(r'app\src\main\java\com\ardrawing\app\camera\OverlayView.kt', 'w', encoding='utf-8') as f:
    f.write(overlay)
with open(r'app\src\main\java\com\ardrawing\app\ui\DrawingActivity.kt', 'w', encoding='utf-8') as f:
    f.write(drawing)
print('OK')