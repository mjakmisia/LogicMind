package com.example.logicmind.activities

import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.logicmind.R
import com.example.logicmind.common.GameCountdownManager
import com.example.logicmind.common.GameTimerProgressBar
import com.example.logicmind.common.PauseMenu
import com.example.logicmind.common.StarManager
import kotlin.random.Random
import androidx.core.graphics.toColorInt

class SymbolRaceActivity : BaseActivity() {

    // Handler do zarządzania opóźnionymi akcjami
    private val handler = Handler(Looper.getMainLooper())

    // Elementy interfejsu gry
    private lateinit var trackContainer: FrameLayout            // Kontener na koła (tor gry)
    private lateinit var trackLine: View                        // Linia toru pod kołami
    private lateinit var countdownText: TextView                // Tekst odliczania startowego
    private lateinit var pauseButton: ImageButton               // Przycisk pauzy
    private lateinit var pauseOverlay: ConstraintLayout         // Nakładka z menu pauzy
    private lateinit var timerProgressBar: GameTimerProgressBar // Pasek czasu gry
    private lateinit var starManager: StarManager               // Licznik gwiazdek
    private lateinit var pauseMenu: PauseMenu                   // Logika menu pauzy
    private lateinit var countdownManager: GameCountdownManager // Logika odliczania przed grą
    private lateinit var blueContainer: FrameLayout             // Lewa strefa dotyku (niebieska)
    private lateinit var redContainer: FrameLayout              // Prawa strefa dotyku (czerwona)
    private lateinit var tempoInfoText: TextView                // Tekst tempa i combo

    // Stan gry
    private val circleQueue = mutableListOf<Circle>()           // Kolejka aktywnych kół
    private var isProcessing = false                            // Flaga: trwa przetwarzanie ruchu
    private var isGameRunning = false                           // Flaga: gra jest aktywna
    private var isGameEnding = false                            // Flaga: gra kończy się (czas upłynął)

    // Obsługa double-tap
    private var awaitingDoubleClick = false                     // Flaga: oczekiwanie na drugi tap
    private var awaitingDoubleSide: Int? = null                 // 1 = lewa, -1 = prawa
    private var awaitingDoubleForId: Int? = null                // ID koła oczekującego na double-tap
    private var nextCircleId = 1                                // ID dla kolejnych kół

    // Obsługa BOTH tap (oba naraz)
    private var lastTapTime: Long = 0                           // Czas ostatniego tapu
    private var lastTapSide: Boolean? = null                    // Strona ostatniego tapu
    private val bothTapWindowMs = 300L                          // Okno czasowe dla BOTH (ms)

    // Parametry tempa i combo
    private var currentReactionTimeMs = 3500L                   // Czas życia koła (ms)
    private var successfulStreak = 0                            // Licznik poprawnych ruchów (combo)
    private var totalMoves = 0                                  // Licznik wszystkich ruchów

    // Stałe gry
    companion object {
        const val BASE_TIME_SECONDS = 90                        // Całkowity czas gry
        private const val BLOCK_DELAY_MS = 1300L                // Opóźnienie auto-usunięcia – BLOCK
        private const val CIRCLE_SIZE_DP = 130                  // Rozmiar koła w dp
        private const val VISIBLE_CIRCLES = 4                   // Liczba widocznych kół
        private const val ANIMATION_DURATION_MS = 300L          // Czas animacji ruchu (ms)
        private const val MIN_REACTION_TIME_MS = 800L          // Minimalny czas życia koła
        private const val SPEEDUP_STEP_MS = 300L                // Skrócenie czasu co przyspieszenie
        private const val MOVES_PER_SPEEDUP = 12                // Ruchów na przyspieszenie
    }

    // Kolory
    private val redColor = "#EF5350".toColorInt()
    private val blueColor = "#4FC3F7".toColorInt()
    private val redStroke = "#D32F2F".toColorInt()
    private val blueStroke = "#0288D1".toColorInt()
    private val strokeWidthDp = 6                   // Szerokość obramowania w dp

    // Rodzaje symboli na kołach
    private enum class Symbol {
        EMPTY, LEFT, RIGHT, BOTH, DOUBLE_LEFT, DOUBLE_RIGHT, SWAP, TARGET, BLOCK
    }

    // Struktura danych dla koła
    private data class Circle(
        val id: Int,
        val view: ImageView,
        val color: Int,
        val symbol: Symbol
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_symbol_race)
        supportActionBar?.hide()

        // Inicjalizacja elementów interfejsu
        trackContainer = findViewById(R.id.trackContainer)
        trackLine = findViewById(R.id.trackLine)
        countdownText = findViewById(R.id.countdownText)
        pauseButton = findViewById(R.id.pauseButton)
        pauseOverlay = findViewById(R.id.pauseOverlay)
        timerProgressBar = findViewById(R.id.gameTimerProgressBar)
        starManager = StarManager()
        starManager.init(findViewById(R.id.starCountText))
        blueContainer = findViewById(R.id.blueContainer)
        redContainer = findViewById(R.id.redContainer)
        tempoInfoText = findViewById(R.id.tempoInfoText)
        trackContainer.clipChildren = false

        // Inicjalizacja paska czasu gry
        timerProgressBar.setTotalTime(BASE_TIME_SECONDS)
        timerProgressBar.setOnFinishCallback {
            // Gdy czas minie - koniec gry
            runOnUiThread {
                isGameEnding = true
                Toast.makeText(this, "Czas minął!", Toast.LENGTH_LONG).show()
                trackContainer.isEnabled = false
                lastPlayedGame(GameKeys.CATEGORY_COORDINATION, GameKeys.GAME_SYMBOL_RACE, getString(R.string.symbol_race))
                finish()
            }
        }

        // Konfiguracja odliczania startowego
        countdownManager = GameCountdownManager(
            countdownText = countdownText,
            gameView = trackContainer,
            viewsToHide = listOf(
                pauseButton,
                findViewById(R.id.starCountText),
                findViewById(R.id.starIcon),
                timerProgressBar,
                tempoInfoText
            ),
            onCountdownFinished = {
                // Po odliczaniu startujemy grę
                starManager.reset()
                startNewGame()
                timerProgressBar.start()
            }
        )

        // Konfiguracja menu pauzy
        pauseMenu = PauseMenu(
            context = this,
            pauseOverlay = pauseOverlay,
            pauseButton = pauseButton,
            onRestart = {
                // Restart gry po pauzie
                if (pauseMenu.isPaused) pauseMenu.resume()

                // Zatrzymaj i zresetuj timer
                timerProgressBar.stop()
                timerProgressBar.reset()

                // Ukryj elementy UI
                trackLine.visibility = View.GONE
                blueContainer.visibility = View.INVISIBLE
                redContainer.visibility = View.INVISIBLE
                tempoInfoText.visibility = View.GONE
                pauseOverlay.visibility = View.GONE

                // Uruchom odliczanie
                countdownManager.startCountdown()
                updateTempoDisplay()
            },
            onResume = {
                // Wznowienie gry
                if (isGameRunning && !isProcessing) {
                    timerProgressBar.start()
                    startAutoShift()
                    updateTempoDisplay()
                    if (circleQueue.isNotEmpty()) startActiveTimer()
                }
            },
            onPause = {
                // Pauza zatrzymuje zegar
                timerProgressBar.pause()
            },
            onExit = {
                // Wyjście z gry
                lastPlayedGame(GameKeys.CATEGORY_COORDINATION, GameKeys.GAME_SYMBOL_RACE, getString(R.string.symbol_race))
                finish()
            },
            instructionTitle = getString(R.string.instructions),
            instructionMessage = getString(R.string.symbol_race_instruction)
        )

        // Obsługa dotyku w strefach gry (lewa/prawa)
        @Suppress("ClickableViewAccessibility")
        blueContainer.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) blueContainer.performClick()
            handleTouch(isBlue = true, event)
            true
        }
        @Suppress("ClickableViewAccessibility")
        redContainer.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) redContainer.performClick()
            handleTouch(isBlue = false, event)
            true
        }

        // Pierwsze uruchomienie – pokazujemy odliczanie
        if (savedInstanceState == null) {
            trackLine.visibility = View.GONE
            blueContainer.visibility = View.INVISIBLE
            redContainer.visibility = View.INVISIBLE
            tempoInfoText.visibility = View.GONE
            countdownManager.startCountdown()
        } else {
            // Jeśli gra była już aktywna – przywracamy stan
            restoreGameState(savedInstanceState)
        }
    }

    // Zapis aktualnego stanu gry przed zniszczeniem aktywności
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("pauseOverlayVisibility", pauseOverlay.visibility)
        outState.putInt("countdownTextVisibility", countdownText.visibility)
        outState.putInt("trackLineVisibility", trackLine.visibility)
        outState.putInt("blueContainerVisibility", blueContainer.visibility)
        outState.putInt("redContainerVisibility", redContainer.visibility)
        outState.putInt("tempoInfoTextVisibility", tempoInfoText.visibility)
        outState.putBoolean("timerIsRunning", timerProgressBar.isRunning())
        outState.putLong("timerRemainingTimeMs", timerProgressBar.getRemainingTimeSeconds() * 1000L)
        outState.putInt("countdownIndex", countdownManager.getIndex())
        outState.putBoolean("countdownInProgress", countdownManager.isInProgress())
        outState.putLong("currentReactionTimeMs", currentReactionTimeMs)
        outState.putInt("successfulStreak", successfulStreak)
        outState.putInt("totalMoves", totalMoves)
        outState.putBoolean("isProcessing", isProcessing)
        outState.putBoolean("isGameRunning", isGameRunning)
        outState.putLong("lastTapTime", lastTapTime)
        lastTapSide?.let { outState.putBoolean("lastTapSide", it) }
        outState.putBoolean("awaitingDoubleClick", awaitingDoubleClick)
        awaitingDoubleSide?.let { outState.putInt("awaitingDoubleSide", it) }
        awaitingDoubleForId?.let { outState.putInt("awaitingDoubleForId", it) }
        outState.putInt("nextCircleId", nextCircleId)
        starManager.saveState(outState)
    }

    // Przywrócenie stanu gry po zmianie konfiguracji
    private fun restoreGameState(savedInstanceState: Bundle) {
        pauseOverlay.visibility = savedInstanceState.getInt("pauseOverlayVisibility")
        countdownText.visibility = savedInstanceState.getInt("countdownTextVisibility")
        trackLine.visibility = savedInstanceState.getInt("trackLineVisibility")
        blueContainer.visibility = savedInstanceState.getInt("blueContainerVisibility")
        redContainer.visibility = savedInstanceState.getInt("redContainerVisibility")
        tempoInfoText.visibility = savedInstanceState.getInt("tempoInfoTextVisibility")

        // Przywraca stan timera
        val timerRemainingMs = savedInstanceState.getLong("timerRemainingTimeMs", BASE_TIME_SECONDS * 1000L)
        val timerRunning = savedInstanceState.getBoolean("timerIsRunning", false)
        timerProgressBar.setRemainingTimeMs(timerRemainingMs.coerceAtLeast(1L))

        // Przywraca odliczanie początkowe
        val countdownInProgress = savedInstanceState.getBoolean("countdownInProgress", false)
        val countdownIndex = savedInstanceState.getInt("countdownIndex", 0)

        // Przywraca parametry gry
        currentReactionTimeMs = savedInstanceState.getLong("currentReactionTimeMs", 3500L).coerceAtLeast(MIN_REACTION_TIME_MS)
        successfulStreak = savedInstanceState.getInt("successfulStreak", 0)
        totalMoves = savedInstanceState.getInt("totalMoves", 0)
        isProcessing = savedInstanceState.getBoolean("isProcessing", false)
        isGameRunning = savedInstanceState.getBoolean("isGameRunning", false)
        lastTapTime = savedInstanceState.getLong("lastTapTime", 0)
        lastTapSide = if (savedInstanceState.containsKey("lastTapSide")) savedInstanceState.getBoolean("lastTapSide") else null
        awaitingDoubleClick = savedInstanceState.getBoolean("awaitingDoubleClick", false)
        awaitingDoubleSide = if (savedInstanceState.containsKey("awaitingDoubleSide")) savedInstanceState.getInt("awaitingDoubleSide") else null
        awaitingDoubleForId = if (savedInstanceState.containsKey("awaitingDoubleForId")) savedInstanceState.getInt("awaitingDoubleForId") else null
        nextCircleId = savedInstanceState.getInt("nextCircleId", 1)

        // Przywraca licznik gwiazdek i synchronizuje menu pauzy
        starManager.restoreState(savedInstanceState)
        pauseMenu.syncWithOverlay()
        updateTempoDisplay()

        // Jeśli trwało odliczanie – kontynuuje je
        if (countdownInProgress) {
            countdownManager.startCountdown(countdownIndex)
            return
        }

        // Jeśli gra była aktywna – wznawia licznik i ruch
        if (timerRunning && pauseOverlay.visibility != View.VISIBLE && !isProcessing) {
            timerProgressBar.start()
        }

        // Przywraca układ kół po załadowaniu widoku
        trackContainer.post {
            if (isGameRunning) {
                updateCirclePositions()
                startAutoShift()
                if (circleQueue.isNotEmpty()) startActiveTimer()
            }
        }
    }

    // Rozpoczyna nową grę
    private fun startNewGame() {
        cancelAllDelayedActions() // Czyści wszystkie zaplanowane zadania

        if (pauseMenu.isPaused) pauseMenu.resume() // Wznawia, jeśli gra była zapauzowana
        pauseOverlay.visibility = View.GONE // Ukrywa nakładkę pauzy

        timerProgressBar.stop()
        timerProgressBar.reset()

        // Resetuje stan gry
        circleQueue.clear()
        trackContainer.removeAllViews()
        lastTapTime = 0
        lastTapSide = null
        clearAwaitingDouble()
        currentReactionTimeMs = 3500L
        successfulStreak = 0
        totalMoves = 0
        isGameRunning = true
        isProcessing = false

        // Przywraca widoczność elementów UI
        trackLine.visibility = View.VISIBLE
        blueContainer.visibility = View.VISIBLE
        redContainer.visibility = View.VISIBLE
        tempoInfoText.visibility = View.VISIBLE

        // Tworzy początkowe koła
        repeat(VISIBLE_CIRCLES) { createCircle() }
        updateCirclePositions()
        startAutoShift() // Uruchamia automatyczne przesuwanie
        startActiveTimer() // Uruchamia timer dla pierwszego koła

        updateTempoDisplay()
    }

    // Tworzy nowe koło i dodaje je do toru
    private fun createCircle() {
        val isRed = Random.nextBoolean()
        val color = if (isRed) redColor else blueColor
        val symbol = Symbol.entries.random()

        // Tworzy graficzną reprezentację koła
        val view = ImageView(this).apply {
            layoutParams = FrameLayout.LayoutParams(dpToPx(CIRCLE_SIZE_DP), dpToPx(CIRCLE_SIZE_DP)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
            }
            scaleType = ImageView.ScaleType.FIT_CENTER
            setImageResource(getSymbolDrawable(symbol))
            background = createCircleBackground(color)
            elevation = 8f
            setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16))
        }

        val circle = Circle(nextCircleId++, view, color, symbol)
        circleQueue.add(0, circle)
        trackContainer.addView(view)

        // Pozwala kliknąć tylko dolne (aktywne) koło
        view.setOnClickListener {
            if (isGameRunning && !isProcessing && circleQueue.isNotEmpty() && circleQueue.last() == circle) {
                when (symbol) {
                    Symbol.TARGET -> animateInstantSuccess(circle)
                    else -> animateFailure(circle)
                }
            }
        }

        // Jeśli dodane koło jest dolnym – uruchamia jego timer
        if (circleQueue.last() == circle) {
            lastTapTime = 0
            lastTapSide = null
            startActiveTimer()
        }
    }

    // Obsługuje dotyk w strefie (lewa/prawa)
    private fun handleTouch(isBlue: Boolean, event: MotionEvent) {
        if (!isGameRunning || circleQueue.isEmpty() || isProcessing || pauseMenu.isPaused) return
        if (event.action != MotionEvent.ACTION_DOWN) return

        val bottomCircle = circleQueue.last()
        val symbol = bottomCircle.symbol
        val currentTime = System.currentTimeMillis()

        // Obsługa symbolu BOTH (naciśnięcie obu stron)
        if (symbol == Symbol.BOTH) {
            if (lastTapTime > 0 && (currentTime - lastTapTime) <= bothTapWindowMs && lastTapSide != isBlue) {
                // Drugi tap w oknie czasowym – sukces
                lastTapTime = 0
                lastTapSide = null
                animateBoth(bottomCircle)
                return
            } else {
                // Pierwszy tap – czekamy na drugi
                lastTapTime = currentTime
                lastTapSide = isBlue
                runDelayed(bothTapWindowMs + 50) {
                    // Jeśli nie było drugiego tapu – błąd
                    if (lastTapTime == currentTime && circleQueue.isNotEmpty() && circleQueue.last() == bottomCircle && !isProcessing) {
                        animateFailure(bottomCircle)
                    }
                }
                return
            }
        } else {
            // Zwykłe symbole – przekazanie do obsługi
            handleContainerPress(isBlue)
        }
    }

    // Sprawdza poprawność naciśnięcia względem symbolu
    private fun handleContainerPress(isBlue: Boolean) {
        if (!isGameRunning || circleQueue.isEmpty() || isProcessing) return
        val bottomCircle = circleQueue.last()
        val color = bottomCircle.color
        val symbol = bottomCircle.symbol
        val isRedCircle = color == redColor

        if (awaitingDoubleClick && awaitingDoubleForId != bottomCircle.id) {
            clearAwaitingDouble() // Reset, jeśli czekaliśmy na inne koło
        }

        var correct = false

        // Logika sprawdzania poprawności
        when (symbol) {
            Symbol.EMPTY -> correct = (isBlue && color == blueColor) || (!isBlue && color == redColor)
            Symbol.LEFT -> correct = isBlue
            Symbol.RIGHT -> correct = !isBlue
            Symbol.DOUBLE_LEFT -> {
                if (!awaitingDoubleClick && isBlue) {
                    beginAwaitingDouble(bottomCircle.id, 1)
                    return
                }
                if (awaitingDoubleClick && awaitingDoubleForId == bottomCircle.id && isBlue && awaitingDoubleSide == 1) {
                    correct = true
                    clearAwaitingDouble()
                }
            }
            Symbol.DOUBLE_RIGHT -> {
                if (!awaitingDoubleClick && !isBlue) {
                    beginAwaitingDouble(bottomCircle.id, -1)
                    return
                }
                if (awaitingDoubleClick && awaitingDoubleForId == bottomCircle.id && !isBlue && awaitingDoubleSide == -1) {
                    correct = true
                    clearAwaitingDouble()
                }
            }
            Symbol.SWAP -> correct = (isBlue && isRedCircle) || (!isBlue && !isRedCircle)
            Symbol.TARGET, Symbol.BLOCK, Symbol.BOTH -> {
                animateFailure(bottomCircle)
                return
            }
        }

        if (correct) animateMoveToSide(bottomCircle, isBlue) else animateFailure(bottomCircle)
    }

    // Animacja poprawnego przesunięcia w stronę odpowiedniej strefy
    private fun animateMoveToSide(circle: Circle, toBlue: Boolean) {
        if (isProcessing) return
        isProcessing = true

        val view = circle.view
        val targetX = if (toBlue)
            blueContainer.x + blueContainer.width / 2 - view.width / 2
        else
            redContainer.x + redContainer.width / 2 - view.width / 2

        // Animacja przesunięcia i usunięcia po zakończeniu
        view.animate()
            .x(targetX)
            .setDuration(ANIMATION_DURATION_MS)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction { removeCircleAfterAnimation(circle, true) }
            .start()
    }

    // Animacja BOTH – rozdzielenie koła na dwie strony
    private fun animateBoth(circle: Circle) {
        if (isProcessing) return
        isProcessing = true

        val view = circle.view
        val centerX = view.x
        val leftX = blueContainer.x + blueContainer.width / 2 - view.width / 2
        val rightX = redContainer.x + redContainer.width / 2 - view.width / 2

        // Tworzy dwie kopie koła
        val leftCopy = ImageView(this).apply {
            layoutParams = view.layoutParams
            setImageDrawable(view.drawable)
            background = view.background
            scaleType = ImageView.ScaleType.FIT_CENTER
            x = centerX
            y = view.y
            elevation = 10f
        }
        val rightCopy = ImageView(this).apply {
            layoutParams = view.layoutParams
            setImageDrawable(view.drawable)
            background = view.background
            scaleType = ImageView.ScaleType.FIT_CENTER
            x = centerX
            y = view.y
            elevation = 10f
        }

        trackContainer.addView(leftCopy)
        trackContainer.addView(rightCopy)
        view.visibility = View.INVISIBLE

        // Animuje obie kopie w przeciwne strony
        leftCopy.animate().x(leftX).setDuration(ANIMATION_DURATION_MS)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction { leftCopy.visibility = View.GONE }.start()

        rightCopy.animate().x(rightX).setDuration(ANIMATION_DURATION_MS)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                rightCopy.visibility = View.GONE
                removeCircleAfterAnimation(circle, true)
            }.start()
    }

    // Animacja natychmiastowego sukcesu (dla TARGET)
    private fun animateInstantSuccess(circle: Circle) {
        if (isProcessing) return
        isProcessing = true

        circle.view.animate()
            .alpha(0f)
            .scaleX(0.8f)
            .scaleY(0.8f)
            .setDuration(ANIMATION_DURATION_MS)
            .withEndAction { removeCircleAfterAnimation(circle, true) }
            .start()
    }

    // Animacja błędu – znika i pomniejsza się
    private fun animateFailure(circle: Circle) {
        if (isProcessing) return
        isProcessing = true

        circle.view.animate()
            .alpha(0f)
            .scaleX(0.5f)
            .scaleY(0.5f)
            .setDuration(ANIMATION_DURATION_MS)
            .withEndAction { removeCircleAfterAnimation(circle, false) }
            .start()
    }

    // Usuwa koło po animacji i kontynuuje grę
    private fun removeCircleAfterAnimation(removedCircle: Circle, success: Boolean) {
        trackContainer.removeView(removedCircle.view)
        circleQueue.remove(removedCircle)

        if (success) {
            starManager.increment() // Dodaje punkt
            handleCorrectMove()
        } else {
            handleError()
        }

        // Czyści stan oczekiwania jeśli dotyczyło tego koła
        if (awaitingDoubleForId == removedCircle.id) clearAwaitingDouble()

        // Dodaje nowe koło i kontynuuje grę
        createCircle()
        updateCirclePositions()
        startActiveTimer()
        isProcessing = false
    }

    // Obsługuje poprawny ruch gracza
    private fun handleCorrectMove() {
        successfulStreak++
        totalMoves++
        checkComboBonus() // Sprawdza bonusy za serię
        accelerateIfNeeded() // Przyspiesza tempo
        updateTempoDisplay()
    }

    // Obsługuje błąd gracza
    private fun handleError() {
        timerProgressBar.subtractTime(6) // Kara: odjęcie czasu
        successfulStreak = 0
        totalMoves++
        accelerateIfNeeded()
        updateTempoDisplay()
    }

    // Sprawdza, czy osiągnięto próg combo i przyznaje bonus
    private fun checkComboBonus() {
        when (successfulStreak) {
            5 -> addTime(2)
            10 -> addTime(4)
            15 -> addTime(7)
            20 -> addTime(10)
        }
        if (successfulStreak > 20 && successfulStreak % 20 == 0) {
            addTime(15)
        }
    }

    // Dodaje określoną ilość czasu do paska i pokazuje komunikat
    private fun addTime(seconds: Int) {
        if (seconds > 0) {
            timerProgressBar.addTime(seconds)
            showComboToast("+$seconds s!")
        }
    }

    // Wyświetla komiunikat z informacją o bonusie
    private fun showComboToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    // Przyspiesza grę po określonej liczbie ruchów
    private fun accelerateIfNeeded() {
        if (totalMoves % MOVES_PER_SPEEDUP == 0) {
            currentReactionTimeMs = (currentReactionTimeMs - SPEEDUP_STEP_MS).coerceAtLeast(MIN_REACTION_TIME_MS)
            updateTempoDisplay()
        }
    }

    // Aktualizuje tekst wyświetlający tempo reakcji i combo
    private fun updateTempoDisplay() {
        val tempoSec = currentReactionTimeMs / 1000.0
        val comboText = if (successfulStreak > 0) "x$successfulStreak" else ""
        tempoInfoText.text = getString(R.string.tempo_display, comboText, tempoSec)
    }

    // Uruchamia timer dla dolnego koła (brak reakcji = błąd)
    private fun startActiveTimer() {
        if (circleQueue.isEmpty() || !isGameRunning) return
        val bottomCircle = circleQueue.last()

        // Symbol BLOCK usuwa się automatycznie po krótkim czasie
        if (bottomCircle.symbol == Symbol.BLOCK) {
            runDelayed(BLOCK_DELAY_MS) {
                if (circleQueue.isNotEmpty() && circleQueue.last() == bottomCircle && !isProcessing) {
                    animateInstantSuccess(bottomCircle)
                }
            }
            return
        }

        // Dla pozostałych symboli – standardowy timeout reakcji
        runDelayed(currentReactionTimeMs) {
            if (circleQueue.isNotEmpty() && circleQueue.last() == bottomCircle && !isProcessing) {
                animateFailure(bottomCircle)
            }
        }
    }

    // Rozpoczyna oczekiwanie na drugi tap (dla symboli DOUBLE)
    private fun beginAwaitingDouble(forId: Int, side: Int) {
        awaitingDoubleClick = true
        awaitingDoubleSide = side
        awaitingDoubleForId = forId

        // Jeśli gracz nie wykona drugiego tapu na czas – błąd
        runDelayed(600L) {
            if (circleQueue.isNotEmpty() && circleQueue.last().id == forId && !isProcessing) {
                animateFailure(circleQueue.last())
            }
            clearAwaitingDouble()
        }
    }

    // Czyści stan oczekiwania na podwójny tap
    private fun clearAwaitingDouble() {
        awaitingDoubleClick = false
        awaitingDoubleSide = null
        awaitingDoubleForId = null
    }

    // Uruchamia automatyczne przesuwanie kół
    private fun startAutoShift() {
        if (!isGameRunning) return

        fun scheduleNext() {
            runDelayed(currentReactionTimeMs) {
                if (isGameRunning && !isProcessing && circleQueue.size >= VISIBLE_CIRCLES) {
                    scheduleNext()
                } else {
                    // Jeśli coś przerwało – spróbuj ponownie po krótkiej chwili
                    runDelayed(100L) { scheduleNext() }
                }
            }
        }
        // Inicjuje pierwsze przesunięcie
        runDelayed(currentReactionTimeMs) { scheduleNext() }
    }

    // Ustawia pozycje kół na torze
    private fun updateCirclePositions() {
        val trackHeight = trackContainer.height
        if (trackHeight == 0) {
            // Jeśli layout jeszcze się nie zmierzył – odłóż wykonanie
            trackContainer.post { updateCirclePositions() }
            return
        }

        val circleSize = dpToPx(CIRCLE_SIZE_DP).toFloat()
        val topMargin = dpToPx(32).toFloat()
        val bottomTargetY = trackHeight * 0.65f
        val totalSpace = bottomTargetY - topMargin
        val totalCirclesHeight = circleSize * VISIBLE_CIRCLES
        val totalGaps = VISIBLE_CIRCLES - 1
        val minSpacing = dpToPx(30).toFloat()

        val spacing = if (totalSpace < totalCirclesHeight + minSpacing * totalGaps) minSpacing
        else (totalSpace - totalCirclesHeight) / totalGaps.toFloat()

        // Rozmieszcza widoczne koła w pionie
        circleQueue.forEachIndexed { index, circle ->
            if (index < VISIBLE_CIRCLES) {
                val y = topMargin + (circleSize + spacing) * index
                circle.view.y = y
                circle.view.visibility = View.VISIBLE
                circle.view.elevation = 8f
            } else {
                circle.view.visibility = View.GONE
            }
        }
    }

    // Zwraca odpowiednią grafikę dla danego symbolu
    private fun getSymbolDrawable(symbol: Symbol): Int = when (symbol) {
        Symbol.EMPTY -> 0
        Symbol.LEFT -> R.drawable.ic_arrow_left
        Symbol.RIGHT -> R.drawable.ic_arrow_right
        Symbol.BOTH -> R.drawable.ic_both_sides
        Symbol.DOUBLE_LEFT -> R.drawable.ic_double_left
        Symbol.DOUBLE_RIGHT -> R.drawable.ic_double_right
        Symbol.SWAP -> R.drawable.ic_swap
        Symbol.TARGET -> R.drawable.ic_target
        Symbol.BLOCK -> R.drawable.ic_close
    }

    // Tworzy tło koła z kolorem i obramowaniem
    private fun createCircleBackground(color: Int): Drawable {
        val strokeColor = if (color == redColor) redStroke else blueStroke
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
            setStroke(dpToPx(strokeWidthDp), strokeColor)
        }
    }

    // Konwersja dp na piksele
    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()

    // Uruchamia akcję z opóźnieniem, uwzględniając pauzę
    private fun runDelayed(delay: Long, action: () -> Unit) {
        val activityRef = java.lang.ref.WeakReference(this)
        var remaining = delay
        val interval = 16L // odświeżanie co klatkę (~60 FPS)

        val runnable = object : Runnable {
            override fun run() {
                val activity = activityRef.get() ?: return

                // Jeśli aktywność została zniszczona – przerwij
                if (activity.isFinishing || activity.isDestroyed) return

                // Gdy gra wstrzymana, nie zmniejszaj licznika
                if (pauseMenu.isPaused) {
                    handler.postDelayed(this, interval)
                    return
                }

                remaining -= interval
                if (remaining <= 0) {
                    // Wykonaj akcję po czasie
                    activity.runOnUiThread { action() }
                } else {
                    // Kontynuuj odliczanie w małych krokach
                    handler.postDelayed(this, interval)
                }
            }
        }
        handler.postDelayed(runnable, interval)
    }

    // Anuluje wszystkie zaplanowane akcje
    private fun cancelAllDelayedActions() {
        handler.removeCallbacksAndMessages(null)
    }

    // Zatrzymuje grę, gdy aplikacja przechodzi w tło
    override fun onPause() {
        super.onPause()
        if (!isGameEnding && !pauseMenu.isPaused && !isChangingConfigurations) {
            pauseMenu.pause()
            timerProgressBar.pause()
        }
    }

    // Sprząta zasoby po zakończeniu aktywności
    override fun onDestroy() {
        super.onDestroy()
        timerProgressBar.stop()
        countdownManager.cancel()
        handler.removeCallbacksAndMessages(null)
    }
}