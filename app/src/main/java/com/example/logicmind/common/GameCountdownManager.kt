package com.example.logicmind.common

import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.TextView

/**
 * Zarządza odliczaniem przed rozpoczęciem gry.
 * Pokazuje licznik (3, 2, 1, Start!) i uruchamia grę po jego zakończeniu.
 */

class GameCountdownManager(
    private val countdownText: TextView,     // Tekst odliczania
    private val gameView: View,              // Główny widok gry
    private val viewsToHide: List<View>? = null, // Widoki do ukrycia podczas odliczania (pauseButton, timer, starText, starIcon)
    private val onCountdownFinished: () -> Unit // Funkcja wywoływana po zakończeniu odliczania
) {
    private var countdownIndex = 0 // Obecna pozycja w sekwencji odliczania
    private var countdownInProgress = false // Flaga informująca, czy trwa odliczanie
    private val countdownValues = listOf("3", "2", "1", "Start!") // Teksty wyświetlane w trakcie odliczania
    private val handler = Handler(Looper.getMainLooper()) // Handler do opóźnień czasowych na głównym wątku

    /**
     * Rozpoczyna odliczanie od podanego indeksu (domyślnie od początku).
     */
    fun startCountdown(startFromIndex: Int = 0) {
        countdownIndex = startFromIndex
        countdownInProgress = true

        // Anulowanie poprzednich wywołań
        handler.removeCallbacksAndMessages(null)

        // Ukryj planszę i widoki; pokaż licznik
        countdownText.visibility = View.VISIBLE
        gameView.visibility = View.INVISIBLE
        viewsToHide?.forEach { it.visibility = View.INVISIBLE }

        // Uruchom pierwsze wywołanie odliczania
        handler.post(countdownRunnable)
    }

    /**
     * Sprawdza, czy odliczanie jest aktywne.
     */
    fun isInProgress(): Boolean = countdownInProgress

    /**
     * Zwraca aktualny indeks odliczania.
     */
    fun getIndex(): Int = countdownIndex

    /**
     * Runnable wywoływany co sekundę – aktualizuje tekst odliczania.
     */
    private val countdownRunnable = object : Runnable {
        override fun run() {
            if (countdownIndex < countdownValues.size) {
                // Ustaw aktualny tekst odliczania
                countdownText.text = countdownValues[countdownIndex]
                countdownIndex++
                handler.postDelayed(this, 1000) // Wywołaj ponownie za 1 sekundę
            } else {
                // Odliczanie zakończone – pokaż grę i ukryte widoki; ukryj tekst
                countdownText.visibility = View.GONE
                gameView.visibility = View.VISIBLE
                viewsToHide?.forEach { it.visibility = View.VISIBLE }
                countdownInProgress = false
                onCountdownFinished() // Powiadom o zakończeniu odliczania
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
