package com.ardrawing.app.util

import android.content.Context
import android.os.Build
import android.os.CountDownTimer
import android.os.VibrationEffect
import android.os.Vibrator

/**
 * Timer tipo Pomodoro (25 min por defecto) con feedback por callback y
 * vibración al finalizar. Extraído de DrawingActivity para aislar esta
 * lógica autocontenida del resto de la pantalla de dibujo.
 *
 * Quien lo use decide cómo mostrar feedback (Toast, cambio de alpha de un
 * botón, etc.) a través de los callbacks — este objeto no toca UI directamente.
 */
class PomodoroTimer(
    private val context: Context,
    private val minutes: Int = 25,
    private val onStart: () -> Unit,
    private val onTick: (minutesLeft: Int) -> Unit,
    private val onFinish: () -> Unit,
    private val onCancel: () -> Unit
) {
    private var countDownTimer: CountDownTimer? = null

    var isActive = false
        private set

    /** Inicia el timer si está inactivo, o lo cancela si ya está corriendo. */
    fun toggle() {
        if (isActive) cancelByUser() else start()
    }

    private fun start() {
        isActive = true
        onStart()
        val millis = minutes * 60_000L
        countDownTimer = object : CountDownTimer(millis, 60_000) {
            override fun onTick(millisUntilFinished: Long) {
                onTick((millisUntilFinished / 60_000).toInt() + 1)
            }
            override fun onFinish() {
                isActive = false
                vibrate()
                onFinish()
            }
        }.start()
    }

    private fun cancelByUser() {
        countDownTimer?.cancel()
        isActive = false
        onCancel()
    }

    /** Detiene el timer sin disparar callbacks — para liberar recursos (ej. Activity.onDestroy). */
    fun release() {
        countDownTimer?.cancel()
        countDownTimer = null
        isActive = false
    }

    private fun vibrate() {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        val pattern = longArrayOf(0, 500, 200, 500, 200, 500)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, -1)
        }
    }
}
