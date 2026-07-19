package com.ardrawing.app.ui

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
import android.widget.LinearLayout
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


import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.util.Consumer
import java.io.File

class DrawingActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_RESOURCE_ID = "extra_resource_id"
        const val EXTRA_IMAGE_URI   = "extra_image_uri"
        const val EXTRA_PROJECT_ID  = "extra_project_id"
    }

    
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    
    private lateinit var binding: ActivityDrawingBinding
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var scaleGestureDetector: ScaleGestureDetector
    private lateinit var overlayView: OverlayView
    private lateinit var projectManager: ProjectManager
    private lateinit var sensorManager: SensorManager

    private var camera: Camera? = null
    private var accelerometer: Sensor? = null

    private var currentScale       = 1f
    private var currentZoom        = 1f 
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


    private var zoomBaseScale      = 1f
    private var zoomBaseTranslateX = 0f
    private var zoomBaseTranslateY = 0f
    private var fingerScale        = 1f


    private var isInverted    = false
    private var isNightMode   = false
    private var isTimerActive = false
    private var countDownTimer: android.os.CountDownTimer? = null


    private var isRecording = false
    private var timelapseRecorder: com.ardrawing.app.camera.TimelapseRecorder? = null

    private val scaleHistory = ArrayDeque<OverlayView.OverlayState>(10)

    private val referenceBitmaps = mutableListOf<Bitmap>()
    private val referenceStates = mutableMapOf<Int, OverlayView.OverlayState>()
    private val referenceLockStates = mutableMapOf<Int, Boolean>()
    private var activeReferenceIndex = -1

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri ?: return@registerForActivityResult
        val bmp = decodeBitmapFromUri(uri) ?: return@registerForActivityResult
        addReferenceImage(bmp, uri.toString())
    }



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

    private val audioPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) binding.btnRecord.performClick()
        else Toast.makeText(this, "Se necesita permiso de micrófono", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDrawingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        overlayView    = binding.overlayView
        overlayView.onDragEnd = { pushState() }
        cameraExecutor = Executors.newSingleThreadExecutor()
        projectManager = ProjectManager(this)
        sensorManager  = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer  = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        activeProjectId = intent.getStringExtra(EXTRA_PROJECT_ID)
        if (activeProjectId != null) loadExistingProject(activeProjectId!!)
        else loadImageFromIntent()

        setupControls()
        setupGestureDetector()
        setupReferences()
        checkCameraPermission()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        sensorManager.unregisterListener(sensorListener)

        countDownTimer?.cancel()

        recording?.stop()
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

        // Cargar imágenes de referencia guardadas
        project.referenceImagePaths.forEachIndexed { i, path ->
            val refBmp = BitmapFactory.decodeFile(path) ?: return@forEachIndexed
            referenceBitmaps.add(refBmp)
        }
        Toast.makeText(this, "Proyecto cargado", Toast.LENGTH_SHORT).show()
    }


    private fun pushState() {
        if (scaleHistory.size >= 10) scaleHistory.removeFirst()
        scaleHistory.addLast(overlayView.getState())
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
                val zoomFactor = 1f + (progress / 100f) * 9f
                currentZoom = zoomFactor
                val newScale = zoomBaseScale * zoomFactor

                // Límite imagen: 50x absoluto
                val clampedScale = newScale.coerceAtMost(50f)
                // Factor real aplicado (puede ser menor si llegó al límite)
                val realFactor = clampedScale / zoomBaseScale

                // Cámara sincronizada: solo avanza mientras la imagen puede avanzar
                val maxCameraZoom = camera?.cameraInfo?.zoomState?.value?.maxZoomRatio ?: 5f
                camera?.cameraControl?.setZoomRatio(realFactor.coerceAtMost(maxCameraZoom))

                overlayView.setScaleKeepPosition(
                    clampedScale,
                    zoomBaseTranslateX,
                    zoomBaseTranslateY,
                    zoomBaseScale
                )
                binding.tvZoomLevel.text = "%.1fx".format(realFactor)
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        binding.btnZoomReset.setOnClickListener {
            currentZoom = 1f
            fingerScale = zoomBaseScale
            currentScale = zoomBaseScale
            binding.seekbarZoom.progress = 0
            camera?.cameraControl?.setZoomRatio(1f)
            binding.tvZoomLevel.text = "1.0x"
            overlayView.setScaleKeepPosition(
                zoomBaseScale, zoomBaseTranslateX, zoomBaseTranslateY, zoomBaseScale
            )
        }


        binding.btnZoom.setOnLongClickListener {
            binding.seekbarZoom.progress = 0
            currentZoom = 1f
            camera?.cameraControl?.setZoomRatio(1f)
            binding.tvZoomLevel.text = "1.0x"
            overlayView.setScaleKeepPosition(
                zoomBaseScale, zoomBaseTranslateX, zoomBaseTranslateY, zoomBaseScale
            )
            Toast.makeText(this, "Zoom reseteado", Toast.LENGTH_SHORT).show()
            true
        }

        // Grid
        binding.btnGrid.setOnClickListener {
            isGridVisible = !isGridVisible
            overlayView.setGridVisible(isGridVisible)
            binding.btnGrid.alpha = if (isGridVisible) 1f else 0.5f
        }

        // Mirror
        binding.btnMirror.setOnClickListener {
            pushState()
            isMirrorOn = !isMirrorOn
            overlayView.setMirror(isMirrorOn)
            binding.btnMirror.alpha = if (isMirrorOn) 1f else 0.5f
        }
        binding.btnMirror.setOnLongClickListener {
            if (!isMirrorOn) return@setOnLongClickListener true
            pushState()
            isMirrorOn = false
            overlayView.setMirror(false)
            binding.btnMirror.alpha = 0.5f
            Toast.makeText(this, "Espejo desactivado", Toast.LENGTH_SHORT).show()
            true
        }

        // Lock
        binding.btnLock.setOnClickListener {
            isLocked = !isLocked
            overlayView.setLocked(isLocked)
            binding.btnLock.alpha = if (isLocked) 1f else 0.5f
            Toast.makeText(this, if (isLocked) "Imagen bloqueada" else "Imagen desbloqueada", Toast.LENGTH_SHORT).show()
        }

        // Lock
        binding.btnLock.setOnLongClickListener {
            if (overlayView.isLocked()) return@setOnLongClickListener true
            pushState()
            // Solo resetea la traslación, no la escala ni rotación
            overlayView.restoreState(
                overlayView.getState().copy(translateX = 0f, translateY = 0f)
            )
            Toast.makeText(this, "Posición reseteada al centro", Toast.LENGTH_SHORT).show()
            true
        }

        // Rotar
        binding.btnRotate.setOnClickListener {
            pushState()
            overlayView.rotateClockwise()
        }
        binding.btnRotate.setOnLongClickListener {
            pushState()
            overlayView.setRotation90(0f)
            Toast.makeText(this, "Rotación reseteada", Toast.LENGTH_SHORT).show()
            true
        }

        // Atras
        binding.btnBack.setOnClickListener { confirmDiscard() }

        // Toggle controles
        binding.btnToggleControls.setOnClickListener {
            isControlsVisible = !isControlsVisible
            binding.controlsPanel.visibility = if (isControlsVisible) View.VISIBLE else View.GONE
            binding.topBar.visibility = if (isControlsVisible) View.VISIBLE else View.GONE
            binding.referencesScrollView.visibility = if (isControlsVisible) View.VISIBLE else View.GONE
            binding.btnToggleControls.setImageResource(
                if (isControlsVisible) android.R.drawable.arrow_down_float
                else android.R.drawable.arrow_up_float
            )
        }

        // Reset
        binding.btnReset.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Resetear todo")
                .setMessage("¿Resetear posición, escala, brillo y demás ajustes?")
                .setPositiveButton("Resetear") { _, _ ->
                    pushState()
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
                .setNegativeButton("Cancelar", null)
                .show()
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

        binding.btnResetBrightness.setOnClickListener {
            overlayView.setBrightness(0f)
            binding.seekbarBrightness.progress = 100
            Toast.makeText(this, "Brillo reseteado", Toast.LENGTH_SHORT).show()
        }

        binding.btnResetContrast.setOnClickListener {
            overlayView.setContrast(1f)
            binding.seekbarContrast.progress = 100
            Toast.makeText(this, "Contraste reseteado", Toast.LENGTH_SHORT).show()
        }

        binding.btnBrightness.setOnLongClickListener {
            overlayView.setBrightness(0f)
            overlayView.setContrast(1f)
            binding.seekbarBrightness.progress = 100
            binding.seekbarContrast.progress   = 100
            Toast.makeText(this, "Brillo y contraste reseteados", Toast.LENGTH_SHORT).show()
            true
        }

        // Zoom panel
        // Zoom panel
        binding.btnZoom.setOnClickListener {
            if (binding.zoomPanel.visibility != View.VISIBLE) {
                // Solo actualizar la base si no hay zoom activo
                if (binding.seekbarZoom.progress == 0) {
                    zoomBaseScale      = currentScale
                    zoomBaseTranslateX = overlayView.getState().translateX
                    zoomBaseTranslateY = overlayView.getState().translateY
                }
            } else {
                currentScale = overlayView.getState().scale
                fingerScale  = currentScale
            }
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



        // Invertir colores
        binding.btnInvert.setOnClickListener {
            isInverted = !isInverted
            overlayView.setInverted(isInverted)
            binding.btnInvert.alpha = if (isInverted) 1f else 0.5f
        }

        // Modo nocturno
        binding.btnNightMode.setOnClickListener {
            isNightMode = !isNightMode
            val lp = window.attributes
            lp.screenBrightness = if (isNightMode) 0.01f else -1f
            window.attributes = lp
            // Tinte rojo sobre toda la UI cuando está activo
            binding.root.setBackgroundColor(
                if (isNightMode) android.graphics.Color.argb(30, 255, 0, 0)
                else android.graphics.Color.TRANSPARENT
            )
            binding.btnNightMode.alpha = if (isNightMode) 1f else 0.5f
            Toast.makeText(this,
                if (isNightMode) "Modo nocturno ON" else "Modo nocturno OFF",
                Toast.LENGTH_SHORT).show()
        }

        // Timer Pomodoro
        binding.btnTimer.setOnClickListener {
            if (isTimerActive) {
                countDownTimer?.cancel()
                isTimerActive = false
                binding.btnTimer.alpha = 0.5f
                Toast.makeText(this, "Timer cancelado", Toast.LENGTH_SHORT).show()
            } else {
                isTimerActive = true
                binding.btnTimer.alpha = 1f
                val minutes = 25
                val millis = minutes * 60 * 1000L
                countDownTimer = object : android.os.CountDownTimer(millis, 60000) {
                    override fun onTick(millisUntilFinished: Long) {
                        val minLeft = (millisUntilFinished / 60000).toInt() + 1
                        Toast.makeText(this@DrawingActivity,
                            "⏱ $minLeft min restantes", Toast.LENGTH_SHORT).show()
                    }
                    override fun onFinish() {
                        isTimerActive = false
                        binding.btnTimer.alpha = 0.5f
                        val vibrator = getSystemService(VIBRATOR_SERVICE) as android.os.Vibrator
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            vibrator.vibrate(android.os.VibrationEffect.createWaveform(
                                longArrayOf(0, 500, 200, 500, 200, 500), -1))
                        } else {
                            @Suppress("DEPRECATION")
                            vibrator.vibrate(longArrayOf(0, 500, 200, 500, 200, 500), -1)
                        }
                        Toast.makeText(this@DrawingActivity,
                            "⏱ Tiempo! Descansá 5 minutos", Toast.LENGTH_LONG).show()
                    }
                }.start()
                Toast.makeText(this, "⏱ Timer 25 min iniciado", Toast.LENGTH_SHORT).show()
            }
        }


        // Grabación timelapse
        binding.btnRecord.setOnClickListener {
            if (!isRecording) {
                // Pedir permiso de audio si no lo tiene
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {
                    audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    return@setOnClickListener
                }
                isRecording = true
                binding.btnRecord.alpha = 1f
                binding.btnRecord.setColorFilter(
                    android.graphics.Color.RED,
                    android.graphics.PorterDuff.Mode.SRC_IN
                )
                val file = File(getExternalFilesDir(null),
                    "ARDrawing_${System.currentTimeMillis()}.mp4")
                val outputOptions = FileOutputOptions.Builder(file).build()
                recording = videoCapture?.output
                    ?.prepareRecording(this, outputOptions)
                    ?.withAudioEnabled()
                    ?.start(ContextCompat.getMainExecutor(this)) { event ->
                        when (event) {
                            is VideoRecordEvent.Finalize -> {
                                if (!event.hasError()) {
                                    // Copiar a galería
                                    val values = android.content.ContentValues().apply {
                                        put(android.provider.MediaStore.Video.Media.DISPLAY_NAME,
                                            file.name)
                                        put(android.provider.MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                                        put(android.provider.MediaStore.Video.Media.RELATIVE_PATH,
                                            "Movies/ARDrawing")
                                    }
                                    val uri = contentResolver.insert(
                                        android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                                        values)
                                    uri?.let {
                                        contentResolver.openOutputStream(it)?.use { out ->
                                            file.inputStream().use { inp -> inp.copyTo(out) }
                                        }
                                    }
                                    file.delete()
                                    Toast.makeText(this, "✓ Video guardado en Galería",
                                        Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(this, "✗ Error al grabar", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                Toast.makeText(this, "⏺ Grabando...", Toast.LENGTH_SHORT).show()
            } else {
                isRecording = false
                binding.btnRecord.alpha = 0.5f
                binding.btnRecord.clearColorFilter()
                recording?.stop()
                recording = null
            }
        }



        // Escala imagen con botones
        var isScalePanelOpen = false

        binding.btnScale.setOnClickListener {
            isScalePanelOpen = !isScalePanelOpen
            binding.scalePanel.visibility = if (isScalePanelOpen) View.VISIBLE else View.GONE
            binding.btnScale.alpha = if (isScalePanelOpen) 1f else 0.5f
            if (isScalePanelOpen) binding.tvScaleValue.text = "%.2fx".format(currentScale)
        }
        binding.btnScale.setOnLongClickListener {
            if (overlayView.isLocked()) return@setOnLongClickListener true
            pushState()
            currentScale = 1f
            fingerScale  = 1f
            val cx = binding.overlayView.width / 2f
            val cy = binding.overlayView.height / 2f
            overlayView.setScaleAt(1f, cx, cy)
            binding.tvScaleValue.text = "1.00x"
            if (isSizeVisible) updateSizeLabel()
            Toast.makeText(this, "Tamaño reseteado", Toast.LENGTH_SHORT).show()
            true
        }

        binding.btnScaleUp.setOnClickListener {
            if (overlayView.isLocked()) return@setOnClickListener
            pushState()
            currentScale = (currentScale + 0.05f).coerceAtMost(10f)
            fingerScale  = currentScale
            // Usar centro de la vista como punto focal
            val cx = binding.overlayView.width / 2f
            val cy = binding.overlayView.height / 2f
            overlayView.setScaleAt(currentScale, cx, cy)
            binding.tvScaleValue.text = "%.2fx".format(currentScale)
            if (isSizeVisible) updateSizeLabel()
        }

        binding.btnScaleDown.setOnClickListener {
            if (overlayView.isLocked()) return@setOnClickListener
            pushState()
            currentScale = (currentScale - 0.05f).coerceAtLeast(0.3f)
            fingerScale  = currentScale
            val cx = binding.overlayView.width / 2f
            val cy = binding.overlayView.height / 2f
            overlayView.setScaleAt(currentScale, cx, cy)
            binding.tvScaleValue.text = "%.2fx".format(currentScale)
            if (isSizeVisible) updateSizeLabel()
        }

        binding.btnScaleReset.setOnClickListener {
            if (overlayView.isLocked()) return@setOnClickListener
            pushState()
            currentScale = 1f
            fingerScale  = 1f
            val cx = binding.overlayView.width / 2f
            val cy = binding.overlayView.height / 2f
            overlayView.setScaleAt(1f, cx, cy)
            binding.tvScaleValue.text = "1.00x"
            if (isSizeVisible) updateSizeLabel()
        }

        // Botón undo — agregalo junto a los demás botones
        binding.btnUndo.setOnClickListener {
            if (scaleHistory.isEmpty()) {
                Toast.makeText(this, "Nada que deshacer", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val prev = scaleHistory.removeLast()
            overlayView.restoreState(prev)
            currentScale = prev.scale
            fingerScale  = prev.scale
            binding.tvScaleValue?.text = "%.2fx".format(currentScale)
            if (isSizeVisible) updateSizeLabel()
        }


    }


    private fun addReferenceImage(bmp: Bitmap, uriString: String? = null) {
        if (referenceBitmaps.size >= 5) {
            Toast.makeText(this, "Máximo 5 imágenes de referencia", Toast.LENGTH_SHORT).show()
            return
        }
        referenceBitmaps.add(bmp)
        val index = referenceBitmaps.size - 1
        addReferenceThumbnail(bmp, index)
        selectReference(index)
    }

    private fun addReferenceThumbnail(bmp: Bitmap, index: Int) {
        val container = binding.referencesContainer

        val thumb = android.widget.ImageView(this).apply {
            val size = 60.dpToPx()
            val margin = 4.dpToPx()
            layoutParams = LinearLayout.LayoutParams(size, size).also {
                it.marginEnd = margin
            }
            scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
            setImageBitmap(bmp)
            clipToOutline = true

            // Fondo redondeado negro sin borde (inactiva)
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadius = 10.dpToPx().toFloat()
                setColor(android.graphics.Color.BLACK)
                setStroke(2.dpToPx(), android.graphics.Color.parseColor("#444444"))
            }

            // Redondear la imagen también
            outlineProvider = object : android.view.ViewOutlineProvider() {
                override fun getOutline(view: android.view.View, outline: android.graphics.Outline) {
                    outline.setRoundRect(0, 0, view.width, view.height, 10.dpToPx().toFloat())
                }
            }

            setOnClickListener { selectReference(index) }
            setOnLongClickListener {
                showDeleteReferenceDialog(index)
                true
            }
        }

        val addBtn = container.findViewWithTag<android.widget.ImageButton>("btnAddReference")
        val insertPos = if (addBtn != null) container.indexOfChild(addBtn) else container.childCount
        container.addView(thumb, insertPos)
    }


    private fun showDeleteReferenceDialog(index: Int) {
        if (referenceBitmaps.size <= 1) {
            Toast.makeText(this, "Debe haber al menos una imagen", Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(this)
            .setTitle("Eliminar imagen")
            .setMessage("¿Eliminar esta imagen de referencia?")
            .setPositiveButton("Eliminar") { _, _ ->
                referenceBitmaps.removeAt(index)
                // Rearmar índices del mapa de estados
                val newStates = mutableMapOf<Int, OverlayView.OverlayState>()
                referenceStates.forEach { (i, state) ->
                    when {
                        i < index -> newStates[i] = state
                        i > index -> newStates[i - 1] = state
                    }
                }
                referenceStates.clear()
                referenceStates.putAll(newStates)

                val newLocks = mutableMapOf<Int, Boolean>()
                referenceLockStates.forEach { (i, lock) ->
                    when {
                        i < index -> newLocks[i] = lock
                        i > index -> newLocks[i - 1] = lock
                    }
                }
                referenceLockStates.clear()
                referenceLockStates.putAll(newLocks)

                // Reconstruir miniaturas
                val container = binding.referencesContainer
                container.removeAllViews()
                referenceBitmaps.forEachIndexed { i, bmp ->
                    addReferenceThumbnail(bmp, i)
                }

                // Volver a agregar el botón +
                val addBtn = android.widget.ImageButton(this).apply {
                    tag = "btnAddReference"
                    layoutParams = LinearLayout.LayoutParams(56.dpToPx(), 56.dpToPx()).also {
                        it.marginEnd = 4.dpToPx()
                    }
                    setImageResource(android.R.drawable.ic_input_add)
                    setBackgroundResource(android.R.drawable.btn_default)
                    setColorFilter(android.graphics.Color.WHITE, android.graphics.PorterDuff.Mode.SRC_IN)
                    alpha = 0.7f
                    setOnClickListener { pickImageLauncher.launch("image/*") }
                }
                container.addView(addBtn)

                // Seleccionar la anterior o la primera
                val newIndex = (index - 1).coerceAtLeast(0)
                activeReferenceIndex = newIndex
                selectReference(newIndex)

                Toast.makeText(this, "Imagen eliminada", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun selectReference(index: Int) {
        // Guardar estado actual de la referencia activa antes de cambiar
        if (activeReferenceIndex >= 0) {
            referenceStates[activeReferenceIndex] = overlayView.getState()
                .copy() // ya incluye todo menos isLocked
            referenceLockStates[activeReferenceIndex] = isLocked
        }

        activeReferenceIndex = index
        val bmp = referenceBitmaps[index]
        currentBitmap = bmp
        overlayView.setImage(bmp)

        // Restaurar estado de la nueva referencia si existe, si no resetear
        val savedState = referenceStates[index]
        if (savedState != null) {
            overlayView.restoreState(savedState)
            currentScale = savedState.scale
            fingerScale  = savedState.scale
        } else {
            overlayView.resetTransform()
            currentScale = 1f
            fingerScale  = 1f
        }

        // Restaurar lock de esta referencia
        isLocked = referenceLockStates[index] ?: false
        overlayView.setLocked(isLocked)
        binding.btnLock.alpha = if (isLocked) 1f else 0.5f

        // Resaltar miniatura activa
        // Resaltar miniatura activa
        val container = binding.referencesContainer
        for (i in 0 until container.childCount) {
            val v = container.getChildAt(i)
            if (v is android.widget.ImageView) {
                v.alpha = 1f
                v.background = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                    cornerRadius = 10.dpToPx().toFloat()
                    setColor(android.graphics.Color.BLACK)
                    setStroke(
                        if (i == index) 3.dpToPx() else 1.dpToPx(),
                        if (i == index) android.graphics.Color.parseColor("#FF6B35")
                        else android.graphics.Color.parseColor("#444444")
                    )
                }
            }
        }
    }

    private fun setupReferences() {
        val container = binding.referencesContainer

        if (referenceBitmaps.isEmpty()) {
            // Proyecto nuevo — agregar la imagen inicial como primera referencia
            currentBitmap?.let {
                referenceBitmaps.add(it)
            }
        }

        // Armar miniaturas para todas las referencias ya cargadas
        referenceBitmaps.forEachIndexed { index, bmp ->
            addReferenceThumbnail(bmp, index)
        }

        if (referenceBitmaps.isNotEmpty()) {
            activeReferenceIndex = 0
            // Resaltar la primera
            val container2 = binding.referencesContainer
            for (i in 0 until container2.childCount) {
                val v = container2.getChildAt(i)
                if (v is android.widget.ImageView) {
                    v.alpha = if (i == 0) 1f else 0.5f
                }
            }
        }

        // Botón +
        val addBtn = android.widget.ImageButton(this).apply {
            tag = "btnAddReference"
            layoutParams = LinearLayout.LayoutParams(60.dpToPx(), 60.dpToPx()).also {
                it.marginEnd = 4.dpToPx()
            }
            setImageResource(android.R.drawable.ic_input_add)
            setColorFilter(android.graphics.Color.parseColor("#FF6B35"), android.graphics.PorterDuff.Mode.SRC_IN)
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadius = 10.dpToPx().toFloat()
                setColor(android.graphics.Color.parseColor("#1A1A1A"))
                setStroke(1.dpToPx(), android.graphics.Color.parseColor("#FF6B35"))
            }
            setOnClickListener { pickImageLauncher.launch("image/*") }
        }
        container.addView(addBtn)
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()

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
                private var savedForThisGesture = false  // ← agregar

                override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                    savedForThisGesture = false  // ← agregar
                    return true
                }

                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    if (overlayView.isLocked()) return true
                    if (!savedForThisGesture) {  // ← agregar bloque
                        pushState()
                        savedForThisGesture = true
                    }
                    currentScale *= detector.scaleFactor
                    currentScale  = currentScale.coerceIn(0.3f, 10f)
                    fingerScale   = currentScale
                    overlayView.setScaleAt(currentScale, detector.focusX, detector.focusY)
                    if (isSizeVisible) updateSizeLabel()
                    return true
                }
            })

        binding.previewView.setOnClickListener { v ->
            val factory = binding.previewView.meteringPointFactory
            val point   = factory.createPoint(v.width / 2f, v.height / 2f)
            val action  = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF)
                .disableAutoCancel()
                .build()
            camera?.cameraControl?.startFocusAndMetering(action)
        }

        
    }

    private fun showSaveProjectDialog() {
        val existingName = activeProjectId?.let { id -> projectManager.getAllProjects().find { it.id == id }?.name } ?: ""
        val autoName = if (existingName.isBlank()) {
        val sdf = java.text.SimpleDateFormat("dd MMM", java.util.Locale("es"))
            "Dibujo ${sdf.format(java.util.Date())}"
        } else existingName

        val editText = EditText(this).apply { hint = "Nombre del proyecto"; setText(autoName); setSingleLine() }
        val container = android.widget.FrameLayout(this).apply {
            val pad = (20 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad / 2, pad, 0); addView(editText)
        }
        AlertDialog.Builder(this)
            .setTitle(if (activeProjectId != null) "Actualizar proyecto" else "Guardar proyecto")
            .setView(container)
            .setPositiveButton("Guardar") { _, _ -> doSaveProject(editText.text.toString().ifBlank { autoName }) }
            .setNegativeButton("Cancelar", null).show()
    }

    private fun doSaveProject(name: String) {
        val saved = projectManager.saveProject(activeProjectId, name, currentBitmap, originalUri, overlayView.getState(), referenceBitmaps)
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
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }
            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.HD))
                .build()
            videoCapture = VideoCapture.withOutput(recorder)

            try {
                provider.unbindAll()
                camera = provider.bindToLifecycle(
                    this, CameraSelector.DEFAULT_BACK_CAMERA, preview, videoCapture
                )
                binding.previewView.post {
                    camera?.cameraControl?.cancelFocusAndMetering()
                    val factory = binding.previewView.meteringPointFactory
                    val point = factory.createPoint(
                        binding.previewView.width / 2f,
                        binding.previewView.height / 2f
                    )
                    camera?.cameraControl?.startFocusAndMetering(
                        FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF)
                            .disableAutoCancel()
                            .build()
                    )
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Error de camara: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }


}
