package com.example.logicmind.common

import android.content.Context
import android.os.CountDownTimer
import android.util.AttributeSet
import android.widget.ProgressBar
import androidx.core.content.ContextCompat

class GameTimerProgressBar @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : ProgressBar(context, attrs) {

    private var totalTimeMs: Long = 60000
    private var currentTimeMs: Long = totalTimeMs
    private var maxTimeMs: Long = 120000
    private val warningThresholdPercent = 25
    private var startTimeMs: Long = 0L
    private var endTimeMs: Long = 0L
    private var timer: CountDownTimer? = null
    private var isRunning = false
    private var onFinishCallback: (() -> Unit)? = null

    init {
        max = 100
        progress = 100
        progressTintList = ContextCompat.getColorStateList(context, android.R.color.white)
    }

    fun setTotalTime(seconds: Int) {
        totalTimeMs = seconds * 1000L
        currentTimeMs = totalTimeMs
        maxTimeMs = totalTimeMs
        updateProgress()
    }

    fun setOnFinishCallback(callback: () -> Unit) {
        onFinishCallback = callback
    }

    fun start() {
        stop()

        if (currentTimeMs <= 0) {
            currentTimeMs = totalTimeMs
        }

        isRunning = true
        startTimeMs = System.currentTimeMillis()
        endTimeMs = startTimeMs + currentTimeMs

        timer = object : CountDownTimer(currentTimeMs, 50L) {
            override fun onTick(millisUntilFinished: Long) {
                val now = System.currentTimeMillis()
                currentTimeMs = (endTimeMs - now).coerceAtLeast(0)
                updateProgress()
            }

            override fun onFinish() {
                if (!isRunning) return
                currentTimeMs = 0
                updateProgress()
                isRunning = false
                onFinishCallback?.invoke()
            }
        }.start()
    }

    fun stop() {
        timer?.cancel()
        timer = null
        isRunning = false
    }

    fun pause() {
        if (!isRunning) return
        timer?.cancel()
        timer = null
        isRunning = false
    }

    fun addTime(seconds: Int) {
        currentTimeMs = (currentTimeMs + seconds * 1000L).coerceAtMost(maxTimeMs)
        if (isRunning) {
            start()
        } else {
            updateProgress()
        }
    }

    fun subtractTime(seconds: Int) {
        currentTimeMs = (currentTimeMs - seconds * 1000L).coerceAtLeast(0)
        if (currentTimeMs == 0L) {
            stop()
            updateProgress()
            onFinishCallback?.invoke()
        } else {
            if (isRunning) {
                start()
            } else {
                updateProgress()
            }
        }
    }

    fun reset() {
        stop()
        currentTimeMs = totalTimeMs
        updateProgress()
    }

    private fun updateProgress() {
        val percent = ((currentTimeMs.toFloat() / totalTimeMs) * 100).toInt().coerceIn(0, 100)
        progress = percent
        updateColor(percent)
    }

    private fun updateColor(percent: Int) {
        progressTintList = if (percent <= warningThresholdPercent) {
            ContextCompat.getColorStateList(context, android.R.color.holo_red_light)
        } else {
            ContextCompat.getColorStateList(context, android.R.color.white)
        }
    }

    fun isRunning() = isRunning

    fun getRemainingTimeSeconds() = (currentTimeMs / 1000).toInt()

    fun setRemainingTimeMs(ms: Long) {
        currentTimeMs = ms.coerceAtLeast(0)
        if (isRunning) {
            startTimeMs = System.currentTimeMillis()
            endTimeMs = startTimeMs + currentTimeMs
        }
        updateProgress()
    }

    fun cancel() {
        stop()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stop()
    }
}