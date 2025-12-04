package com.example.logicmind.common

import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.TextView

class GameCountdownManager(
    private val countdownText: TextView,
    private val gameView: View,
    private val viewsToHide: List<View>? = null,
    private val onCountdownFinished: () -> Unit
) {
    private var countdownIndex = 0
    private var countdownInProgress = false
    private val countdownValues = listOf("3", "2", "1", "Start!")
    private val handler = Handler(Looper.getMainLooper())

    fun startCountdown(startFromIndex: Int = 0) {
        countdownIndex = startFromIndex
        countdownInProgress = true

        handler.removeCallbacksAndMessages(null)

        countdownText.visibility = View.VISIBLE
        gameView.visibility = View.INVISIBLE
        viewsToHide?.forEach { it.visibility = View.INVISIBLE }

        handler.post(countdownRunnable)
    }

    fun isInProgress(): Boolean = countdownInProgress

    fun getIndex(): Int = countdownIndex

    private val countdownRunnable = object : Runnable {
        override fun run() {
            if (countdownIndex < countdownValues.size) {
                countdownText.text = countdownValues[countdownIndex]
                countdownIndex++
                handler.postDelayed(this, 1000)
            } else {
                countdownText.visibility = View.GONE
                gameView.visibility = View.VISIBLE
                viewsToHide?.forEach { it.visibility = View.VISIBLE }
                countdownInProgress = false
                onCountdownFinished()
            }
        }
    }

    fun cancel() {
        handler.removeCallbacksAndMessages(null)
        countdownInProgress = false
        countdownText.visibility = View.GONE
        gameView.visibility = View.VISIBLE
        viewsToHide?.forEach { it.visibility = View.VISIBLE }
    }
}
