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

    // UI elementy gry
    private lateinit var trackContainer: FrameLayout            // Kontener na koła (tor)
    private lateinit var trackLine: View                        // Szara linia pod kołami
    private lateinit var countdownText: TextView                // Pole tekstowe odliczania
    private lateinit var pauseButton: ImageButton               // Przycisk pauzy
    private lateinit var pauseOverlay: ConstraintLayout         // Nakładka z menu pauzy
    private lateinit var timerProgressBar: GameTimerProgressBar // Pasek postępu czasu gry
    private lateinit var starManager: StarManager               // Manager gwiazdek
    private lateinit var pauseMenu: PauseMenu                   // Menu pauzy gry
    private lateinit var countdownManager: GameCountdownManager // Manager odliczania początkowego
    private lateinit var blueContainer: FrameLayout             // Lewa strefa dotyku – niebieski
    private lateinit var redContainer: FrameLayout              // Prawa strefa dotyku – czerwony
    private lateinit var tempoInfoText: TextView                // Tekst combo i czasu reakcji

    // Stan gry
    private val circleQueue = mutableListOf<Circle>()           // Kolejka kół (ostatnie = dolne)
    private var isProcessing = false                            // Flaga: trwa animacja/usuwanie
    private var isGameRunning = false                           // Flaga: gra aktywna
    private var isGameEnding = false                            // Flaga: gra kończy się (czas minął)

    // Double-tap
    private var awaitingDoubleClick = false                     // Flaga: oczekujemy drugiego tapu
    private var awaitingDoubleSide: Int? = null                 // 1 = lewo, -1 = prawo
    private var awaitingDoubleForId: Int? = null                // ID koła oczekiwanego na double-tap
    private var nextCircleId = 1                                // ID kolejnego koła

    // BOTH tap
    private var lastTapTime: Long = 0                           // Czas ostatniego tapu (BOTH)
    private var lastTapSide: Boolean? = null                    // Strona ostatniego tapu (BOTH)
    private val bothTapWindowMs = 300L                          // Okno czasowe dla BOTH (ms)

    // Tempo i combo
    private var currentReactionTimeMs = 5000L                   // Aktualny czas życia koła (ms)
    private var successfulStreak = 0                            // Combo (poprawne ruchy pod rząd)
    private var totalMoves = 0                                  // Licznik wszystkich ruchów
    private val MIN_REACTION_TIME_MS = 1500L                    // Minimalny czas życia koła
    private val SPEEDUP_STEP_MS = 400L                          // Skrócenie czasu co krok
    private val MOVES_PER_SPEEDUP = 12                          // Ruchów na jeden przyspiesznik

    // Stałe gry
    companion object {
        const val BASE_TIME_SECONDS = 90                        // Całkowity czas gry
        private const val BLOCK_DELAY_MS = 1500L                // Opóźnienie auto-usunięcia – BLOCK
        private const val CIRCLE_SIZE_DP = 130                  // Rozmiar koła w dp
        private const val VISIBLE_CIRCLES = 4                   // Liczba widocznych kół
        private const val ANIMATION_DURATION_MS = 300L          // Czas trwania animacji (ms)
    }

    // Kolory kół i obramowań
    private val redColor = "#EF5350".toColorInt()               // Czerwony
    private val blueColor = "#4FC3F7".toColorInt()              // Niebieski
    private val redStroke = "#D32F2F".toColorInt()              // Obramowanie czerwone
    private val blueStroke = "#0288D1".toColorInt()             // Obramowanie niebieskie
    private val strokeWidthDp = 6                               // Szerokość obramowania (dp)

    // Symbole na kołach
    private enum class Symbol {
        EMPTY, LEFT, RIGHT, BOTH, DOUBLE_LEFT, DOUBLE_RIGHT, SWAP, TARGET, BLOCK
    }

    // Reprezentacja koła
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

        // Inicjalizacja widoków
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

        // Inicjalizacja paska czasu
        timerProgressBar.setTotalTime(BASE_TIME_SECONDS)
        timerProgressBar.setOnFinishCallback {
            runOnUiThread {
                isGameEnding = true
                Toast.makeText(this, "Czas minął!", Toast.LENGTH_LONG).show()
                trackContainer.isEnabled = false
                onGameFinished(GameKeys.CATEGORY_COORDINATION, GameKeys.GAME_SYMBOL_RACE, getString(R.string.path_change))
                finish()
            }
        }

        // Inicjalizacja managera odliczania
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
                starManager.reset()
                startNewGame()
            }
        )

        // Inicjalizacja menu pauzy
        pauseMenu = PauseMenu(
            context = this,
            pauseOverlay = pauseOverlay,
            pauseButton = pauseButton,
            onRestart = {
                if (pauseMenu.isPaused) pauseMenu.resume()
                timerProgressBar.reset()
                trackLine.visibility = View.GONE
                blueContainer.visibility = View.INVISIBLE
                redContainer.visibility = View.INVISIBLE
                tempoInfoText.visibility = View.GONE
                pauseOverlay.visibility = View.GONE
                countdownManager.startCountdown()
            },
            onResume = {
                if (isGameRunning && !isProcessing) {
                    timerProgressBar.start()
                    startAutoShift()
                    if (circleQueue.isNotEmpty()) startActiveTimer()
                }
            },
            onPause = {
                timerProgressBar.pause()
            },
            onExit = {
                onGameFinished(GameKeys.CATEGORY_COORDINATION, GameKeys.GAME_SYMBOL_RACE, getString(R.string.path_change))
                finish()
            },
            instructionTitle = getString(R.string.instructions),
            instructionMessage = getString(R.string.path_change_instruction)
        )

        // Nasłuchiwanie dotyku w strefach
        blueContainer.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) blueContainer.performClick()
            handleTouch(isBlue = true, event)
            true
        }
        redContainer.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) redContainer.performClick()
            handleTouch(isBlue = false, event)
            true
        }

        // Pierwsze uruchomienie – odliczanie
        if (savedInstanceState == null) {
            trackLine.visibility = View.GONE
            blueContainer.visibility = View.INVISIBLE
            redContainer.visibility = View.INVISIBLE
            tempoInfoText.visibility = View.GONE
            countdownManager.startCountdown()
        } else {
            restoreGameState(savedInstanceState)
        }
    }

    // Zapisuje stan gry
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

    // Przywraca stan gry po rotacji
    private fun restoreGameState(savedInstanceState: Bundle) {
        pauseOverlay.visibility = savedInstanceState.getInt("pauseOverlayVisibility")
        countdownText.visibility = savedInstanceState.getInt("countdownTextVisibility")
        trackLine.visibility = savedInstanceState.getInt("trackLineVisibility")
        blueContainer.visibility = savedInstanceState.getInt("blueContainerVisibility")
        redContainer.visibility = savedInstanceState.getInt("redContainerVisibility")
        tempoInfoText.visibility = savedInstanceState.getInt("tempoInfoTextVisibility")

        // Przywracanie stanu timera
        val timerRemainingMs = savedInstanceState.getLong("timerRemainingTimeMs", BASE_TIME_SECONDS * 1000L)
        val timerRunning = savedInstanceState.getBoolean("timerIsRunning", false)
        timerProgressBar.setRemainingTimeMs(timerRemainingMs.coerceAtLeast(1L))

        // Przywracanie odliczania początkowego
        val countdownInProgress = savedInstanceState.getBoolean("countdownInProgress", false)
        val countdownIndex = savedInstanceState.getInt("countdownIndex", 0)

        currentReactionTimeMs = savedInstanceState.getLong("currentReactionTimeMs", 5000L).coerceAtLeast(MIN_REACTION_TIME_MS)
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

        starManager.restoreState(savedInstanceState)
        pauseMenu.syncWithOverlay()
        updateTempoDisplay()

        if (countdownInProgress) {
            countdownManager.startCountdown(countdownIndex)
            return
        }

        if (timerRunning && pauseOverlay.visibility != View.VISIBLE && !isProcessing) {
            timerProgressBar.start()
        }

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
        if (pauseMenu.isPaused) pauseMenu.resume()
        pauseOverlay.visibility = View.GONE

        circleQueue.clear()
        trackContainer.removeAllViews()
        lastTapTime = 0
        lastTapSide = null
        clearAwaitingDouble()
        currentReactionTimeMs = 5000L
        successfulStreak = 0
        totalMoves = 0
        isGameRunning = true
        isProcessing = false

        trackLine.visibility = View.VISIBLE
        blueContainer.visibility = View.VISIBLE
        redContainer.visibility = View.VISIBLE
        tempoInfoText.visibility = View.VISIBLE

        repeat(VISIBLE_CIRCLES) { createCircle() }
        updateCirclePositions()
        timerProgressBar.start()
        startAutoShift()
        startActiveTimer()
    }

    // Tworzy nowe koło i dodaje do toru
    private fun createCircle() {
        val isRed = Random.nextBoolean()
        val color = if (isRed) redColor else blueColor
        val symbol = Symbol.entries.random()

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

        // Kliknięcie tylko na dolnym kole
        view.setOnClickListener {
            if (isGameRunning && !isProcessing && circleQueue.isNotEmpty() && circleQueue.last() == circle) {
                when (symbol) {
                    Symbol.TARGET -> animateInstantSuccess(circle)
                    else -> animateFailure(circle)
                }
            }
        }

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

        // Specjalny przypadek: BOTH
        if (symbol == Symbol.BOTH) {
            if (lastTapTime > 0 && (currentTime - lastTapTime) <= bothTapWindowMs && lastTapSide != isBlue) {
                lastTapTime = 0
                lastTapSide = null
                animateBoth(bottomCircle)
                return
            } else {
                lastTapTime = currentTime
                lastTapSide = isBlue
                runDelayed(bothTapWindowMs + 50) {
                    if (lastTapTime == currentTime && circleQueue.isNotEmpty() && circleQueue.last() == bottomCircle && !isProcessing) {
                        animateFailure(bottomCircle)
                    }
                }
                return
            }
        } else {
            handleContainerPress(isBlue)
        }
    }

    // Sprawdza poprawność nacisku względem symbolu
    private fun handleContainerPress(isBlue: Boolean) {
        if (!isGameRunning || circleQueue.isEmpty() || isProcessing) return
        val bottomCircle = circleQueue.last()
        val color = bottomCircle.color
        val symbol = bottomCircle.symbol
        val isRedCircle = color == redColor

        if (awaitingDoubleClick && awaitingDoubleForId != bottomCircle.id) {
            clearAwaitingDouble()
        }

        var correct = false

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

    // Animacja przesunięcia w stronę strefy (poprawny ruch)
    private fun animateMoveToSide(circle: Circle, toBlue: Boolean) {
        if (isProcessing) return
        isProcessing = true

        val view = circle.view
        val targetX = if (toBlue)
            blueContainer.x + blueContainer.width / 2 - view.width / 2
        else
            redContainer.x + redContainer.width / 2 - view.width / 2

        view.animate()
            .x(targetX)
            .setDuration(ANIMATION_DURATION_MS)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction { removeCircleAfterAnimation(circle, true) }
            .start()
    }

    // Animacja gestu BOTH – podział na dwa
    private fun animateBoth(circle: Circle) {
        if (isProcessing) return
        isProcessing = true

        val view = circle.view
        val centerX = view.x
        val leftX = blueContainer.x + blueContainer.width / 2 - view.width / 2
        val rightX = redContainer.x + redContainer.width / 2 - view.width / 2

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

    // Natychmiastowy sukces (TARGET)
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

    // Animacja błędu
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
            starManager.increment()
            handleCorrectMove()
        } else {
            handleError()
        }

        if (awaitingDoubleForId == removedCircle.id) clearAwaitingDouble()

        createCircle()
        updateCirclePositions()
        startActiveTimer()
        isProcessing = false
    }

    // Obsługuje poprawne dopasowanie
    private fun handleCorrectMove() {
        successfulStreak++
        totalMoves++
        checkComboBonus()
        accelerateIfNeeded()
        updateTempoDisplay()
    }

    // Obsługuje błąd
    private fun handleError() {
        timerProgressBar.subtractTime(6) //Odjęcie czasu
        successfulStreak = 0
        totalMoves++
        accelerateIfNeeded()
        updateTempoDisplay()
    }

    // Sprawdza bonusy za combo
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

    // Dodaje czas i pokazuje toast
    private fun addTime(seconds: Int) {
        if (seconds > 0) {
            timerProgressBar.addTime(seconds)
            showComboToast("+$seconds s!")
        }
    }

    // Pokazuje toast bonusowy
    private fun showComboToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).apply {
            setGravity(Gravity.TOP or Gravity.CENTER_HORIZONTAL, 0, 120)
            show()
        }
    }

    // Przyspiesza grę co podaną ilość ruchów
    private fun accelerateIfNeeded() {
        if (totalMoves % MOVES_PER_SPEEDUP == 0) {
            currentReactionTimeMs = (currentReactionTimeMs - SPEEDUP_STEP_MS).coerceAtLeast(MIN_REACTION_TIME_MS)
            updateTempoDisplay()
        }
    }

    // Aktualizuje wyświetlanie tempa i combo
    private fun updateTempoDisplay() {
        val tempoSec = currentReactionTimeMs / 1000.0
        val comboText = if (successfulStreak > 0) "x$successfulStreak" else ""
        tempoInfoText.text = "$comboText  ${"%.1f".format(tempoSec)}s"
    }

    // Uruchamia timer życia dla dolnego koła
    private fun startActiveTimer() {
        if (circleQueue.isEmpty() || !isGameRunning) return
        val bottomCircle = circleQueue.last()

        runDelayed(currentReactionTimeMs) {
            if (circleQueue.isNotEmpty() && circleQueue.last() == bottomCircle && !isProcessing) {
                animateFailure(bottomCircle)
            }
        }

        if (bottomCircle.symbol == Symbol.BLOCK) {
            runDelayed(BLOCK_DELAY_MS) {
                if (circleQueue.isNotEmpty() && circleQueue.last() == bottomCircle && !isProcessing) {
                    animateInstantSuccess(bottomCircle)
                }
            }
        }
    }

    // Rozpoczyna oczekiwanie na drugi tap (double-tap)
    private fun beginAwaitingDouble(forId: Int, side: Int) {
        awaitingDoubleClick = true
        awaitingDoubleSide = side
        awaitingDoubleForId = forId

        runDelayed(600L) {
            if (circleQueue.isNotEmpty() && circleQueue.last().id == forId && !isProcessing) {
                animateFailure(circleQueue.last())
            }
            clearAwaitingDouble()
        }
    }

    // Czyści stan oczekiwania na double-tap
    private fun clearAwaitingDouble() {
        awaitingDoubleClick = false
        awaitingDoubleSide = null
        awaitingDoubleForId = null
    }

    // Uruchamia automatyczne przesuwanie (rytm gry)
    private fun startAutoShift() {
        if (!isGameRunning) return

        fun scheduleNext() {
            runDelayed(currentReactionTimeMs) {
                if (isGameRunning && !isProcessing && circleQueue.size >= VISIBLE_CIRCLES) {
                    scheduleNext()
                } else {
                    runDelayed(100L) { scheduleNext() }
                }
            }
        }
        runDelayed(currentReactionTimeMs) { scheduleNext() }
    }

    // Ustawia pozycje kół na torze
    private fun updateCirclePositions() {
        val trackHeight = trackContainer.height
        if (trackHeight == 0) {
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

    // Zwraca drawable dla symbolu
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

    // Tworzy tło koła z obramowaniem
    private fun createCircleBackground(color: Int): Drawable {
        val strokeColor = if (color == redColor) redStroke else blueStroke
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
            setStroke(dpToPx(strokeWidthDp), strokeColor)
        }
    }

    // Konwersja dp → px
    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()

    // Uruchamia akcję z opóźnieniem, uwzględniając pauzę
    private fun runDelayed(delay: Long, action: () -> Unit) {
        val activityRef = java.lang.ref.WeakReference(this)
        var remaining = delay
        val interval = 16L

        val runnable = object : Runnable {
            override fun run() {
                val activity = activityRef.get() ?: return
                if (pauseMenu.isPaused || activity.isFinishing || activity.isDestroyed) {
                    Handler(Looper.getMainLooper()).postDelayed(this, interval)
                    return
                }
                remaining -= interval
                if (remaining <= 0) {
                    activity.runOnUiThread { action() }
                } else {
                    Handler(Looper.getMainLooper()).postDelayed(this, interval)
                }
            }
        }
        Handler(Looper.getMainLooper()).postDelayed(runnable, interval)
    }

    override fun onPause() {
        super.onPause()
        if (!isGameEnding && !pauseMenu.isPaused && !isChangingConfigurations) {
            pauseMenu.pause()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        timerProgressBar.cancel()
        countdownManager.cancel()
    }
}