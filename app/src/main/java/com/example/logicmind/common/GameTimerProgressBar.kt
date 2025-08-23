package com.example.logicmind.common

import android.content.Context
import android.os.CountDownTimer
import android.util.AttributeSet
import android.widget.ProgressBar
import androidx.core.content.ContextCompat

/**
 * Custom ProgressBar z odliczaniem czasu gry.
 * Pokazuje pasek postępu, który zmniejsza się wraz z upływem czasu.
 */
class GameTimerProgressBar @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : ProgressBar(context, attrs) {

    private var totalTimeMs: Long = 60000 // Domyślny czas całkowity 60 sekund
    private var currentTimeMs: Long = totalTimeMs // Aktualny pozostały czas
    private var maxTimeMs: Long = 120000 // Maksymalny limit czasu

    private val warningThresholdPercent = 25 // Próg, poniżej którego pasek zmienia kolor na czerwony

    private var timer: CountDownTimer? = null // Androidowy timer odliczający czas
    private var isRunning = false // Flaga czy timer jest aktywny

    // Callback wywoływany po zakończeniu odliczania
    private var onFinishCallback: (() -> Unit)? = null

    init {
        max = 100  // Maksymalna wartość paska postępu (procent)
        progress = 100  // Startowy postęp (pełny pasek)
        // Ustaw kolor paska (biały domyślnie)
        progressTintList = ContextCompat.getColorStateList(context, android.R.color.white)
    }

    /** Ustawia całkowity czas odliczania w sekundach */
    fun setTotalTime(seconds: Int) {
        totalTimeMs = seconds * 1000L
        currentTimeMs = totalTimeMs
        updateProgress()
    }

    /** Ustawia callback wywoływany po zakończeniu odliczania */
    fun setOnFinishCallback(callback: () -> Unit) {
        onFinishCallback = callback
    }

    /** Startuje odliczanie od aktualnego czasu */
    fun start() {
        if (isRunning) return

        timer?.cancel()
        isRunning = true
        timer = object : CountDownTimer(currentTimeMs, 100L) {
            override fun onTick(millisUntilFinished: Long) {
                currentTimeMs = millisUntilFinished
                updateProgress()
            }

            override fun onFinish() {
                currentTimeMs = 0
                updateProgress()
                isRunning = false
                onFinishCallback?.invoke()
            }
        }.start()
    }

    /** Pauzuje odliczanie */
    fun pause() {
        if (!isRunning) return
        timer?.cancel()
        isRunning = false
    }

    /** Dodaje czas (sekundy) do aktualnego czasu */
    fun addTime(seconds: Int) {
        currentTimeMs = (currentTimeMs + seconds * 1000L).coerceAtMost(maxTimeMs)
        if (isRunning) {
            timer?.cancel()
            start()
        } else {
            updateProgress()
        }
    }

    /** Odejmuje czas (sekundy) od aktualnego czasu */
    fun subtractTime(seconds: Int) {
        currentTimeMs = (currentTimeMs - seconds * 1000L).coerceAtLeast(0)
        if (currentTimeMs == 0L) {
            timer?.cancel()
            isRunning = false
            updateProgress()
            onFinishCallback?.invoke()
        } else {
            if (isRunning) {
                timer?.cancel()
                start()
            } else {
                updateProgress()
            }
        }
    }

    /** Resetuje timer do wartości początkowej */
    fun reset() {
        timer?.cancel()
        isRunning = false
        currentTimeMs = totalTimeMs
        updateProgress()
    }

    /** Aktualizuje pasek postępu (procentowo) i kolor */
    private fun updateProgress() {
        val percent = ((currentTimeMs.toFloat() / totalTimeMs) * 100).toInt().coerceIn(0, 100)
        progress = percent
        updateColor(percent)
    }

    /** Zmienia kolor paska na czerwony poniżej progu ostrzeżenia */
    private fun updateColor(percent: Int) {
        progressTintList = if (percent <= warningThresholdPercent) {
            ContextCompat.getColorStateList(context, android.R.color.holo_red_light)
        } else {
            ContextCompat.getColorStateList(context, android.R.color.white)
        }
    }

    /** Zwraca true jeśli timer jest aktualnie uruchomiony */
    fun isRunning() = isRunning

    /** Zwraca pozostały czas w sekundach */
    fun getRemainingTimeSeconds() = (currentTimeMs / 1000).toInt()
}
