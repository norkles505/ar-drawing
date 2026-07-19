package com.ardrawing.app.camera

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.View
import java.io.File
import java.nio.ByteBuffer

import android.app.Activity
import android.view.PixelCopy
import android.view.Window


class TimelapseRecorder(
    private val context: Context,
    private val window: Window,
    private val overlayView: View
){
    private val frames = mutableListOf<Bitmap>()
    private var isRecording = false
    private val handler = Handler(Looper.getMainLooper())
    private var frameInterval = 2000L // captura cada 2 segundos

    private val captureRunnable = object : Runnable {
        override fun run() {
            if (isRecording) {
                captureFrame()
                handler.postDelayed(this, frameInterval)
            }
        }
    }

    fun start() {
        frames.clear()
        isRecording = true
        handler.post(captureRunnable)
    }

    fun stop(onDone: (Boolean) -> Unit) {
        isRecording = false
        handler.removeCallbacks(captureRunnable)
        if (frames.isEmpty()) { onDone(false); return }
        Thread {
            val success = encodeToVideo()
            Handler(Looper.getMainLooper()).post { onDone(success) }
        }.start()
    }

    fun getFrameCount() = frames.size

    private fun captureFrame() {
        try {
            val decorView = window.decorView
            val w = decorView.width
            val h = decorView.height
            if (w <= 0 || h <= 0) return
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                PixelCopy.request(window, bmp, { result ->
                    if (result == PixelCopy.SUCCESS) {
                        frames.add(bmp)
                    }
                }, handler)
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun encodeToVideo(): Boolean {
        if (frames.isEmpty()) return false
        val w = makeEven(frames[0].width)
        val h = makeEven(frames[0].height)
        val fps = 30
        val bitrate = 4_000_000

        val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, w, h).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
            setInteger(MediaFormat.KEY_FRAME_RATE, fps)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        }

        val tempFile = File(context.cacheDir, "timelapse_${System.currentTimeMillis()}.mp4")

        return try {
            val codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            val surface = codec.createInputSurface()
            codec.start()

            val muxer = MediaMuxer(tempFile.absolutePath,
                MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            var trackIndex = -1
            var muxerStarted = false
            val bufferInfo = MediaCodec.BufferInfo()

            // Dibujar cada frame en el surface
            frames.forEachIndexed { index, bmp ->
                val canvas = surface.lockCanvas(null)
                canvas?.let {
                    it.drawBitmap(bmp, 0f, 0f, null)
                    surface.unlockCanvasAndPost(it)
                }
                // Presentar el frame con timestamp
                val presentationTimeUs = index * (1_000_000L / fps)
                drainEncoder(codec, muxer, bufferInfo, presentationTimeUs,
                    false, { trackIndex }, { idx -> trackIndex = idx },
                    { muxerStarted }, { muxerStarted = true })
            }

            // Finalizar
            drainEncoder(codec, muxer, bufferInfo, -1,
                true, { trackIndex }, { idx -> trackIndex = idx },
                { muxerStarted }, { muxerStarted = true })

            codec.stop()
            codec.release()
            surface.release()
            if (muxerStarted) muxer.stop()
            muxer.release()

            // Guardar en galería
            saveToGallery(tempFile)
            tempFile.delete()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            tempFile.delete()
            false
        }
    }

    private fun drainEncoder(
        codec: MediaCodec,
        muxer: MediaMuxer,
        bufferInfo: MediaCodec.BufferInfo,
        presentationTimeUs: Long,
        endOfStream: Boolean,
        getTrackIndex: () -> Int,
        setTrackIndex: (Int) -> Unit,
        getMuxerStarted: () -> Boolean,
        setMuxerStarted: () -> Unit
    ) {
        if (endOfStream) codec.signalEndOfInputStream()
        var tries = 0
        while (tries < 30) {
            val encoderStatus = codec.dequeueOutputBuffer(bufferInfo, 10_000)
            when {
                encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                    if (!endOfStream) break
                    tries++
                }
                encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    val newFormat = codec.outputFormat
                    setTrackIndex(muxer.addTrack(newFormat))
                    muxer.start()
                    setMuxerStarted()
                }
                encoderStatus >= 0 -> {
                    val encodedData: ByteBuffer = codec.getOutputBuffer(encoderStatus) ?: continue
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                        bufferInfo.size = 0
                    }
                    if (bufferInfo.size > 0 && getMuxerStarted()) {
                        bufferInfo.presentationTimeUs = if (presentationTimeUs >= 0)
                            presentationTimeUs else bufferInfo.presentationTimeUs
                        muxer.writeSampleData(getTrackIndex(), encodedData, bufferInfo)
                    }
                    codec.releaseOutputBuffer(encoderStatus, false)
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) return
                }
            }
        }
    }

    private fun saveToGallery(file: File): Boolean {
        return try {
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME,
                    "ARDrawing_${System.currentTimeMillis()}.mp4")
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/ARDrawing")
            }
            val uri = context.contentResolver.insert(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
            uri?.let {
                context.contentResolver.openOutputStream(it)?.use { out ->
                    file.inputStream().use { inp -> inp.copyTo(out) }
                }
            }
            true
        } catch (e: Exception) { e.printStackTrace(); false }
    }

    private fun makeEven(n: Int) = if (n % 2 == 0) n else n - 1
}