package com.example.logicmind.common


/**
 * GameStatsManager
 * TYLKO KLASY NIEZALEŹNE OD FIREBASE
 * Klasa przechowująca i obliczająca statystyki gry, niezależna od kontekstu Android Activity.
 * Łatwiej jest ją testować
 */
open class GameStatsManager {

    /** Uruchamia licznik czasu po opóźnieniu (wywoływana przez Activity). */
    fun setGameStartTime() {
        gameStartTime = System.currentTimeMillis()
    }

    /*
    Liczymy średni czas reakcji jako czas trwania gry / liczba gwiazdek

    - startReactionTracking() — startuje licznik czasu.
    - pauseReactionTracking() — zatrzymuje czas gry (np. gdy gracz pauzuje).
    - resumeReactionTracking() — wznawia licznik po pauzie.
    - getAverageReactionTime() — zwraca średni czas reakcji (w sekundach)
     */


    //protected żeby umożliwić dziedziczenie w testach
    protected var gameStartTime: Long = 0L
    protected var totalActiveTime: Long = 0L
    protected var pauseStartTime: Long = 0L
    protected var isPaused: Boolean = false

    //śledzenie gry
    //wywoływana na początku gry
    fun startReactionTracking() {
        totalActiveTime = 0L
        isPaused = false
        pauseStartTime = 0L

        //resetuje liczniki poprawnosci i prob
        resetAccuracyCounters()

        //nie mozna z handlerem testowac jednostkowo trzeba opóźnienie w klasie dziedziczącej opisać
//        Handler(Looper.getMainLooper()).postDelayed({
//            gameStartTime = System.currentTimeMillis()
//            //Toast.makeText(this, "Rozpoczęcie gry", Toast.LENGTH_SHORT).show()
//        }, 4000)
    }

    //pauzowanie gry
    fun onGamePaused() {
        if (!isPaused) {
            pauseStartTime = System.currentTimeMillis()
            isPaused = true
            //Toast.makeText(this, "Gra zapauzowana o ${pauseStartTime}", Toast.LENGTH_SHORT).show()
        }
    }

    //wznowienie gry
    fun onGameResumed() {
        if (isPaused) {
            //przesuwamy startTime żeby nie liczyc pauzy
            val pauseDuration = System.currentTimeMillis() - pauseStartTime
            gameStartTime += pauseDuration
            isPaused = false
            //Toast.makeText(this, "Gra wznowiona o ${pauseDuration}", Toast.LENGTH_SHORT).show()
        }
    }

    /** Oblicza średni czas reakcji.
     * Jest testowalna jednostkowo.
     */
    fun calculateAvgReactionTime(stars: Int = 0): Double {
        val currentTime = System.currentTimeMillis()
        val duration = (currentTime - gameStartTime).coerceAtLeast(1L)

        val starsEarned = stars.coerceAtLeast(1)

        return duration.toDouble() / starsEarned.toDouble() / 1000.0 // w sekundach
    }

    /**
     * Liczenie poprawności
     * protected żeby mozna bylo testowac
     */

    protected var totalAttempts: Int = 0 //liczba prób
    protected var successfulAttempts: Int = 0 //liczba poprawnych prób

    //resetuje licznik na start
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