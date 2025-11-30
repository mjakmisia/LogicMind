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
        //pozniej usun
//        android.widget.Toast.makeText(
//            context,
//            "Pomiar Czasu się zaczyna (GameTime: ${gameStartTime})",
//            android.widget.Toast.LENGTH_SHORT
//        ).show()
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

    //pobranie stanu czasu
    fun getStartTime(): Long {
        return gameStartTime
    }

    //przywrócenie stanu czasu
    fun restoreStartTime(time: Long) {
        this.gameStartTime = time
    }

    //pobranie danych o pauzie
    fun getPauseData(): Pair<Boolean, Long> {
        return Pair(isPaused, pauseStartTime)
    }

    //przywrócenie danych o pauzie
    fun restorePauseData(paused: Boolean, pauseTime: Long) {
        this.isPaused = paused
        this.pauseStartTime = pauseTime
    }

    //śledzenie gry
    //wywoływana na początku gry
    fun startReactionTracking() {
        totalActiveTime = 0L
        isPaused = false
        pauseStartTime = 0L
        //gameStartTime = 0L
        resetAccuracyCounters()
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

    /**
     * Zwraca całkowity czas gry w sekundach z wyłączeniem pauz
     */
    fun getPlayedTimeSec(): Double {
        val currentTime = System.currentTimeMillis()
        var duration = (currentTime - gameStartTime).coerceAtLeast(0L)

        // Jeśli gra jest aktualnie zapauzowana,
        // odejmujemy czas trwania obecnej pauzy, aby wynik był dokładny.
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

        // Jeśli gra jest aktualnie zapauzowana,
        // trzeba odjąć bieżący czas trwania pauzy, aby obliczyć poprawny czas gry.
        if (isPaused) {
            // Oblicz, ile czasu minęło od rozpoczęcia pauzy
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