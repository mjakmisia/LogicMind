package com.example.logicmind.common


/**
 * GameStatsManager
 * TYLKO KLASY NIEZALEŹNE OD FIREBASE
 * Klasa przechowująca i obliczająca statystyki gry, niezależna od kontekstu Android Activity.
 * Łatwiej jest ją testować
 */
open class GameStatsManager {

    /** Uruchamia licznik czasu po opóźnieniu (wywoływana przez Activity) */
    fun setGameStartTime(context: android.content.Context) {
        gameStartTime = System.currentTimeMillis()
    }

    protected var gameStartTime: Long = 0L
    protected var totalActiveTime: Long = 0L
    protected var pauseStartTime: Long = 0L
    protected var isPaused: Boolean = false

    fun getStartTime(): Long {
        return gameStartTime
    }

    fun restoreStartTime(time: Long) {
        this.gameStartTime = time
    }

    fun getPauseData(): Pair<Boolean, Long> {
        return Pair(isPaused, pauseStartTime)
    }

    fun restorePauseData(paused: Boolean, pauseTime: Long) {
        this.isPaused = paused
        this.pauseStartTime = pauseTime
    }

    fun startReactionTracking() {
        totalActiveTime = 0L
        isPaused = false
        pauseStartTime = 0L
        resetAccuracyCounters()
    }

    fun onGamePaused() {
        if (!isPaused) {
            pauseStartTime = System.currentTimeMillis()
            isPaused = true
        }
    }

    fun onGameResumed() {
        if (isPaused) {
            val pauseDuration = System.currentTimeMillis() - pauseStartTime
            gameStartTime += pauseDuration
            isPaused = false
        }
    }

    /**
     * Zwraca całkowity czas gry w sekundach z wyłączeniem pauz
     */
    fun getPlayedTimeSec(): Double {
        val currentTime = System.currentTimeMillis()
        var duration = (currentTime - gameStartTime).coerceAtLeast(0L)

        if (isPaused) {
            val currentPauseDuration = currentTime - pauseStartTime
            duration -= currentPauseDuration
        }

        return duration.toDouble() / 1000.0
    }

    /** Oblicza średni czas reakcji.
     * Jest testowalna jednostkowo.
     */
    fun calculateAvgReactionTime(stars: Int = 0): Double {

        val currentTime = System.currentTimeMillis()
        var duration = (currentTime - gameStartTime).coerceAtLeast(1L)

        if (isPaused) {
            val currentPauseDuration = currentTime - pauseStartTime
            duration -= currentPauseDuration
        }

        val starsEarned = stars.coerceAtLeast(1)

        return duration.toDouble() / starsEarned.toDouble() // w milisekundach
    }

    /**
     * Liczenie poprawności
     * protected żeby mozna bylo testowac
     */

    protected var totalAttempts: Int = 0
    protected var successfulAttempts: Int = 0

    fun resetAccuracyCounters() {
        totalAttempts = 0
        successfulAttempts = 0
    }

    /**
     * Rejestruje próbę gracza
     * @param isSuccessful - true jeśli była poprawna np. trafienie pary
     */
    fun registerAttempt(isSuccessful: Boolean) {
        totalAttempts++
        if (isSuccessful) successfulAttempts++
    }

    /**
     * Oblicza procent poprawnych prób
     * Zwraca wartość od 0.0 do 100.0
     */
    fun calculateAccuracy(): Double {
        return if (totalAttempts > 0) (successfulAttempts.toDouble() / totalAttempts.toDouble()) * 100.0
        else 0.0
    }
}