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
    private lateinit var trackContainer: FrameLayout // Kontener na koła w grze
    private lateinit var trackLine: View // Linia pod kołami
    private lateinit var countdownText: TextView // Tekst odliczania przed startem
    private lateinit var pauseButton: ImageButton // Przycisk pauzy
    private lateinit var pauseOverlay: ConstraintLayout // Nakładka menu pauzy
    private lateinit var timerProgressBar: GameTimerProgressBar // Pasek czasu gry
    private lateinit var starManager: StarManager // Zarządzanie gwiazdkami
    private lateinit var pauseMenu: PauseMenu // Logika menu pauzy
    private lateinit var countdownManager: GameCountdownManager // Odliczanie startowe
    private lateinit var blueContainer: FrameLayout // Lewy kontener (niebieski)
    private lateinit var redContainer: FrameLayout // Prawy kontener (czerwony)

    // Stan gry
    private val circleQueue = mutableListOf<Circle>() // Kolejka kół, ostatnie = dolne
    private var isProcessing = false // Blokada podczas animacji
    private val handler = Handler(Looper.getMainLooper()) // Handler UI dla runDelayed

    // Podwójne kliknięcia
    private var awaitingDoubleClick = false // Czy oczekujemy drugiego tapnięcia
    private var awaitingDoubleSide: Int? = null // 1 = lewo, -1 = prawo
    private var awaitingDoubleForId: Int? = null // ID koła dla double-tap

    private var nextCircleId = 1 // Unikalne ID dla nowych kół

    // Gest BOTH – tap-tap w 300ms
    private var lastTapTime: Long = 0 // Czas ostatniego tapnięcia
    private var lastTapSide: Boolean? = null // Strona ostatniego tapnięcia
    private val bothTapWindowMs = 300L // Okno czasowe dla gestu BOTH
    private val animationDurationMs = 300L // Czas trwania animacji

    companion object {
        private const val BASE_TIME_SECONDS = 90 // Całkowity czas gry
        private const val REACTION_TIME_MS = 10000L // Maksymalny czas na reakcję
        private const val BLOCK_DELAY_MS = 2000L // Czas auto-usunięcia BLOCK
        private const val CIRCLE_SIZE_DP = 130 // Rozmiar koła w dp
        private const val VISIBLE_CIRCLES = 4 // Liczba widocznych kół
    }

    private val redColor = "#EF5350".toColorInt() // Kolor czerwony
    private val blueColor = "#4FC3F7".toColorInt() // Kolor niebieski
    private val redStroke = "#D32F2F".toColorInt() // Obramowanie czerwone
    private val blueStroke = "#0288D1".toColorInt() // Obramowanie niebieskie
    private val strokeWidthDp = 6 // Grubość obramowania koła

    // Typy symboli
    private enum class Symbol {
        EMPTY, LEFT, RIGHT, BOTH, DOUBLE_LEFT, DOUBLE_RIGHT, SWAP, TARGET, BLOCK
    }

    private data class Circle(
        val id: Int,            // Unikalne ID koła
        val view: ImageView,    // Widok koła
        val color: Int,         // Kolor tła koła
        val symbol: Symbol      // Symbol na kole
    )

    // Inicjalizacja aktywności
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_symbol_race)
        supportActionBar?.hide()

        initViews()
        setupTimer()
        setupCountdown()
        setupPauseMenu()
        setupTouchListeners()

        if (savedInstanceState == null) {
            trackLine.visibility = View.GONE
            blueContainer.visibility = View.INVISIBLE
            redContainer.visibility = View.INVISIBLE
            countdownManager.startCountdown()
        } else {
            restoreGameState(savedInstanceState)
        }
    }

    // Inicjalizacja widoków UI
    private fun initViews() {
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
        trackContainer.clipChildren = false
    }

    // Ustawia nasłuchiwanie dotyku na kontenerach z performClick
    private fun setupTouchListeners() {
        blueContainer.setOnTouchListener { view, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                view.performClick() // Accessibility
            }
            handleContainerTouch(isBlue = true, event)
            true
        }
        redContainer.setOnTouchListener { view, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                view.performClick() // Accessibility
            }
            handleContainerTouch(isBlue = false, event)
            true
        }
    }

    // Obsługuje dotyk na niebieskim/czerwonym kontenerze
    private fun handleContainerTouch(isBlue: Boolean, event: MotionEvent) {
        if (circleQueue.isEmpty() || isProcessing || pauseMenu.isPaused) return

        val bottomCircle = circleQueue.last()
        val symbol = bottomCircle.symbol

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val currentTime = System.currentTimeMillis()

                if (symbol == Symbol.BOTH) {
                    if (lastTapTime > 0 && (currentTime - lastTapTime) <= bothTapWindowMs && lastTapSide != isBlue) {
                        lastTapTime = 0
                        lastTapSide = null
                        animateCorrectBoth(bottomCircle)
                        return
                    } else {
                        lastTapTime = currentTime
                        lastTapSide = isBlue
                        runDelayed(bothTapWindowMs + 50) {
                            if (lastTapTime == currentTime && circleQueue.isNotEmpty() && circleQueue.last() == bottomCircle && !isProcessing) {
                                animateError(bottomCircle)
                            }
                        }
                        return
                    }
                } else {
                    onContainerPressed(isBlue)
                }
            }
        }
    }

    // Sprawdza poprawność ruchu po dotknięciu
    private fun onContainerPressed(isBlue: Boolean) {
        if (circleQueue.isEmpty() || isProcessing || pauseMenu.isPaused) return

        val bottomCircle = circleQueue.last()
        val color = bottomCircle.color
        val symbol = bottomCircle.symbol
        val isRedCircle = color == redColor

        var correct: Boolean

        if (awaitingDoubleClick && awaitingDoubleForId != bottomCircle.id) {
            clearAwaitingDouble()
        }

        when (symbol) {
            Symbol.EMPTY -> correct = (isBlue && color == blueColor) || (!isBlue && color == redColor)
            Symbol.LEFT -> correct = isBlue
            Symbol.RIGHT -> correct = !isBlue
            Symbol.DOUBLE_LEFT -> {
                if (!awaitingDoubleClick && isBlue) {
                    startAwaitingDouble(forId = bottomCircle.id, side = 1)
                    return
                }
                if (awaitingDoubleClick && awaitingDoubleForId == bottomCircle.id && isBlue && awaitingDoubleSide == 1) {
                    correct = true
                    clearAwaitingDouble()
                } else {
                    clearAwaitingDouble()
                    correct = false
                }
            }
            Symbol.DOUBLE_RIGHT -> {
                if (!awaitingDoubleClick && !isBlue) {
                    startAwaitingDouble(forId = bottomCircle.id, side = -1)
                    return
                }
                if (awaitingDoubleClick && awaitingDoubleForId == bottomCircle.id && !isBlue && awaitingDoubleSide == -1) {
                    correct = true
                    clearAwaitingDouble()
                } else {
                    clearAwaitingDouble()
                    correct = false
                }
            }
            Symbol.SWAP -> correct = (isBlue && isRedCircle) || (!isBlue && !isRedCircle)
            Symbol.TARGET -> { animateError(bottomCircle); return }
            Symbol.BLOCK -> { animateError(bottomCircle); return }
            Symbol.BOTH -> { animateError(bottomCircle); return }
        }

        if (correct) {
            animateCorrectMove(bottomCircle, isBlue)
        } else {
            animateError(bottomCircle)
        }
    }

    // Konfiguruje pasek czasu gry
    private fun setupTimer() {
        timerProgressBar.setTotalTime(BASE_TIME_SECONDS)
        timerProgressBar.setOnFinishCallback {
            runOnUiThread {
                Toast.makeText(this, "Czas minął!", Toast.LENGTH_LONG).show()
                endGame()
            }
        }
    }

    // Ustawia odliczanie przed startem gry
    private fun setupCountdown() {
        countdownManager = GameCountdownManager(
            countdownText = countdownText,
            gameView = trackContainer,
            viewsToHide = listOf(pauseButton, findViewById(R.id.starCountText), findViewById(R.id.starIcon), timerProgressBar),
            onCountdownFinished = {
                starManager.reset()
                timerProgressBar.start()
                trackLine.visibility = View.VISIBLE
                blueContainer.visibility = View.VISIBLE
                redContainer.visibility = View.VISIBLE
                startNewGame()
            }
        )
    }

    // Konfiguruje menu pauzy
    private fun setupPauseMenu() {
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
                countdownManager.startCountdown()
            },
            onResume = {
                timerProgressBar.start()
                startAutoShift() // Wznów auto-shift po pauzie
            },
            onPause = {
                timerProgressBar.pause()
                // runDelayed automatycznie się zatrzyma
            },
            onExit = { finish() },
            instructionTitle = getString(R.string.instructions),
            instructionMessage = getString(R.string.path_change_instruction)
        )
    }

    // Rozpoczyna nową grę
    private fun startNewGame() {
        circleQueue.clear()
        trackContainer.removeAllViews()
        lastTapTime = 0
        lastTapSide = null
        repeat(VISIBLE_CIRCLES) { createCircle() }
        updatePositionsInstantly()
        startAutoShift()
        startActiveCircleTimer()
    }

    // Tworzy nowe koło
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

        if (circleQueue.last() == circle) {
            lastTapTime = 0
            lastTapSide = null
            startActiveCircleTimer()
        }

        view.setOnClickListener {
            if (circleQueue.isNotEmpty() && circleQueue.last() == circle) {
                when (symbol) {
                    Symbol.TARGET -> animateCorrectInstant(circle)
                    else -> animateError(circle)
                }
            }
        }
    }

    // Animuje poprawny ruch koła
    private fun animateCorrectMove(circle: Circle, toBlue: Boolean) {
        if (isProcessing) return
        isProcessing = true

        val view = circle.view
        val targetX = if (toBlue) blueContainer.x + blueContainer.width / 2 - view.width / 2
        else redContainer.x + redContainer.width / 2 - view.width / 2

        view.animate()
            .x(targetX)
            .setDuration(animationDurationMs)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                removeBottomCircleAfterAnimation(circle, showStar = true)
            }
            .start()
    }

    // Animuje poprawny gest BOTH
    private fun animateCorrectBoth(circle: Circle) {
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

        leftCopy.animate().x(leftX).setDuration(animationDurationMs)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction { leftCopy.visibility = View.GONE }.start()

        rightCopy.animate().x(rightX).setDuration(animationDurationMs)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                rightCopy.visibility = View.GONE
                removeBottomCircleAfterAnimation(circle, showStar = true)
            }.start()
    }

    // Animuje natychmiastowy sukces (TARGET)
    private fun animateCorrectInstant(circle: Circle) {
        if (isProcessing) return
        isProcessing = true

        circle.view.animate()
            .alpha(0f)
            .scaleX(0.8f)
            .scaleY(0.8f)
            .setDuration(animationDurationMs)
            .withEndAction {
                removeBottomCircleAfterAnimation(circle, showStar = true)
            }
            .start()
    }

    // Animuje błąd
    private fun animateError(circle: Circle) {
        if (isProcessing) return
        isProcessing = true

        circle.view.animate()
            .alpha(0f)
            .scaleX(0.5f)
            .scaleY(0.5f)
            .setDuration(animationDurationMs)
            .withEndAction {
                removeBottomCircleAfterAnimation(circle, showStar = false)
            }
            .start()
    }

    // Usuwa koło po animacji
    private fun removeBottomCircleAfterAnimation(removedCircle: Circle, showStar: Boolean) {
        trackContainer.removeView(removedCircle.view)
        circleQueue.remove(removedCircle)

        if (showStar) starManager.increment()

        if (awaitingDoubleForId == removedCircle.id) clearAwaitingDouble()

        createCircle()
        updatePositionsInstantly()
        startActiveCircleTimer()
        isProcessing = false
    }

    // Uruchamia timer reakcji dla dolnego koła
    private fun startActiveCircleTimer() {
        if (circleQueue.isEmpty()) return
        val bottomCircle = circleQueue.last()

        runDelayed(REACTION_TIME_MS) {
            if (circleQueue.isNotEmpty() && circleQueue.last() == bottomCircle && !isProcessing) {
                animateError(bottomCircle)
            }
        }

        if (bottomCircle.symbol == Symbol.BLOCK) {
            runDelayed(BLOCK_DELAY_MS) {
                if (circleQueue.isNotEmpty() && circleQueue.last() == bottomCircle && !isProcessing) {
                    animateCorrectInstant(bottomCircle)
                }
            }
        }
    }

    // Rozpoczyna oczekiwanie na double-tap
    private fun startAwaitingDouble(forId: Int, side: Int) {
        awaitingDoubleClick = true
        awaitingDoubleSide = side
        awaitingDoubleForId = forId

        runDelayed(600L) {
            if (circleQueue.isNotEmpty() && circleQueue.last().id == forId && !isProcessing) {
                animateError(circleQueue.last())
            }
            clearAwaitingDouble()
        }
    }

    // Czyści stan double-tap
    private fun clearAwaitingDouble() {
        awaitingDoubleClick = false
        awaitingDoubleSide = null
        awaitingDoubleForId = null
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

    // Aktualizuje pozycje kół na ekranie
    private fun updatePositionsInstantly() {
        val trackHeight = trackContainer.height
        if (trackHeight == 0) {
            trackContainer.post { updatePositionsInstantly() }
            return
        }

        val circleSize = dpToPx(CIRCLE_SIZE_DP).toFloat()
        val topMargin = dpToPx(32).toFloat()
        val actualHeight = trackHeight.toFloat()
        val bottomTargetY = actualHeight * 0.65f

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

    // Uruchamia timer auto-przesuwania kół
    private fun startAutoShift() {
        fun shiftAction() {
            if (isProcessing || circleQueue.size < VISIBLE_CIRCLES) {
                runDelayed(REACTION_TIME_MS) { shiftAction() }
                return
            }
            runDelayed(REACTION_TIME_MS) { shiftAction() }
        }
        runDelayed(REACTION_TIME_MS) { shiftAction() }
    }

    // Kończy grę
    private fun endGame() {
        trackContainer.isEnabled = false
        finish()
    }

    // Zapisuje stan gry
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("pauseOverlayVisibility", pauseOverlay.visibility)
        outState.putInt("countdownTextVisibility", countdownText.visibility)
        outState.putLong("timerRemainingTimeMs", timerProgressBar.getRemainingTimeSeconds() * 1000L)
        outState.putBoolean("timerIsRunning", timerProgressBar.isRunning())
        outState.putInt("countdownIndex", countdownManager.getIndex())
        outState.putBoolean("countdownInProgress", countdownManager.isInProgress())
        starManager.saveState(outState)
    }

    // Przywraca stan gry
    private fun restoreGameState(savedInstanceState: Bundle) {
        pauseOverlay.visibility = savedInstanceState.getInt("pauseOverlayVisibility", View.GONE)
        countdownText.visibility = savedInstanceState.getInt("countdownTextVisibility", View.GONE)
        val timeMs = savedInstanceState.getLong("timerRemainingTimeMs", BASE_TIME_SECONDS * 1000L)
        val running = savedInstanceState.getBoolean("timerIsRunning", false)
        timerProgressBar.setRemainingTimeMs(timeMs.coerceAtLeast(1L))
        if (running && pauseOverlay.visibility != View.VISIBLE) timerProgressBar.start()
        if (savedInstanceState.getBoolean("countdownInProgress", false)) {
            countdownManager.startCountdown(savedInstanceState.getInt("countdownIndex", 0))
        }
        pauseMenu.syncWithOverlay()
        starManager.restoreState(savedInstanceState)
        trackContainer.post {
            if (circleQueue.isNotEmpty()) {
                updatePositionsInstantly()
                startActiveCircleTimer()
            }
        }
    }

    // Automatyczna pauza przy wyjściu z aplikacji
    override fun onPause() {
        super.onPause()
        if (!pauseMenu.isPaused && !isChangingConfigurations) {
            pauseMenu.pause()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        timerProgressBar.cancel()
        countdownManager.cancel()
    }

    // Konwertuje dp na px
    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()

    // Uniwersalna funkcja opóźnienia
    private fun runDelayed(delay: Long, action: () -> Unit) {
        var remaining = delay
        val interval = 16L // ~60fps

        val runnable = object : Runnable {
            override fun run() {
                if (pauseMenu.isPaused) {
                    handler.postDelayed(this, interval)
                    return
                }
                remaining -= interval
                if (remaining <= 0) {
                    action()
                } else {
                    handler.postDelayed(this, interval)
                }
            }
        }
        handler.postDelayed(runnable, interval)
    }
}