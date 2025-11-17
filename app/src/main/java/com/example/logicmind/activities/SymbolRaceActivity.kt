package com.example.logicmind.activities

import android.content.res.Configuration
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
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
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.graphics.toColorInt
import com.example.logicmind.R
import com.example.logicmind.common.GameCountdownManager
import com.example.logicmind.common.GameTimerProgressBar
import com.example.logicmind.common.PauseMenu
import com.example.logicmind.common.StarManager
import java.io.Serializable
import kotlin.random.Random

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
        private const val CIRCLE_SIZE_DP = 130                  // Rozmiar koła w dp (pion)
        private const val VISIBLE_CIRCLES = 4                   // Liczba widocznych kół (pion)
        private const val CIRCLE_SIZE_DP_LANDSCAPE = 100        // Rozmiar koła w dp (poziom)
        private const val VISIBLE_CIRCLES_LANDSCAPE = 3         // Liczba widocznych kół (poziom)
        private const val ANIMATION_DURATION_MS = 300L          // Czas animacji ruchu (ms)
        private const val MIN_REACTION_TIME_MS = 800L          // Minimalny czas życia koła
        private const val SPEEDUP_STEP_MS = 300L                // Skrócenie czasu co przyspieszenie
        private const val MOVES_PER_SPEEDUP = 12                // Ruchów na przyspieszenie
    }

    // Zmienne stanu widoku (zależne od orientacji)
    private var currentCircleSizeDp: Int = CIRCLE_SIZE_DP
    private var currentVisibleCircles: Int = VISIBLE_CIRCLES

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

    // Struktura danych dla koła - zawiera widok
    private data class Circle(
        val id: Int,
        val view: ImageView,
        val color: Int,
        val symbol: Symbol
    )

    // Struktura do zapisu czystego stanu koła
    private data class CircleState(
        val id: Int,
        val color: Int,
        val symbol: Symbol
    ) : Serializable

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
                updateUserStatistics(
                    categoryKey = GameKeys.CATEGORY_COORDINATION,
                    gameKey = GameKeys.GAME_SYMBOL_RACE,
                    starsEarned = starManager.starCount,
                    accuracy = calculateAccuracy(),
                    reactionTime = getAverageReactionTime(stars = starManager.starCount),
                )

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
                gameStatsManager.startReactionTracking()
                gameStatsManager.setGameStartTime(this@SymbolRaceActivity)
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
                onGameResumed()
            },
            onPause = {
                // Pauza zatrzymuje zegar
                timerProgressBar.pause()
                onGamePaused()
            },
            onExit = {
                // Wyjście z gry

                updateUserStatistics(
                    categoryKey = GameKeys.CATEGORY_COORDINATION,
                    gameKey = GameKeys.GAME_SYMBOL_RACE,
                    starsEarned = starManager.starCount,
                    accuracy = calculateAccuracy(),
                    reactionTime = getAverageReactionTime(stars = starManager.starCount),
                )

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

        updateLayoutForOrientation() // Ustawiamy parametry widoku (zależne od zmiany orientacji)

        // Pierwsze uruchomienie – pokazujemy odliczanie
        if (savedInstanceState == null) {
            trackLine.visibility = View.GONE
            blueContainer.visibility = View.INVISIBLE
            redContainer.visibility = View.INVISIBLE
            tempoInfoText.visibility = View.GONE
            countdownManager.startCountdown()
            startReactionTracking()
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
        outState.putBoolean("isGameRunning", isGameRunning)
        outState.putInt("nextCircleId", nextCircleId)
        starManager.saveState(outState)

        // Zapisz stan kolejki kół jako listę obiektów
        val queueToSave = ArrayList(circleQueue.map {
            CircleState(it.id, it.color, it.symbol)
        })
        outState.putSerializable("circleQueueState", queueToSave)
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
        isGameRunning = savedInstanceState.getBoolean("isGameRunning", false)
        nextCircleId = savedInstanceState.getInt("nextCircleId", 1)

        // Resetujemy stany przejściowe do wartości domyślnych
        isProcessing = false
        lastTapTime = 0
        lastTapSide = null
        clearAwaitingDouble()

        // Przywraca licznik gwiazdek i synchronizuje menu pauzy
        starManager.restoreState(savedInstanceState)
        pauseMenu.syncWithOverlay()
        updateTempoDisplay()

        // Jeśli trwało odliczanie – kontynuuje je
        if (countdownInProgress) {
            countdownManager.startCountdown(countdownIndex)
            return
        }

        // Odtwarza kolejkę kół
        @Suppress("UNCHECKED_CAST")
        val savedQueue = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Sposób pobierania Serializable dla API 33+
            savedInstanceState.getSerializable("circleQueueState", ArrayList::class.java)
        } else {
            // Stary sposób dla API < 33
            @Suppress("DEPRECATION")
            savedInstanceState.getSerializable("circleQueueState")
        } as? ArrayList<CircleState> // Rzutujemy bezpiecznie na końcu

        if (savedQueue != null) {
            circleQueue.clear()
            trackContainer.removeAllViews() // Wyczyść kontener

            // Przejdź przez zapisany stan i stwórz koła od nowa
            for (state in savedQueue) {
                // Odtwórz widok
                val view = ImageView(this).apply {
                    layoutParams = FrameLayout.LayoutParams(dpToPx(currentCircleSizeDp), dpToPx(currentCircleSizeDp)).apply {
                        gravity = Gravity.CENTER_HORIZONTAL
                    }
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    setImageResource(getSymbolDrawable(state.symbol))
                    background = createCircleBackground(state.color)
                    elevation = 8f
                    setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16))
                }

                // Odtwórz obiekt koła
                val circle = Circle(state.id, view, state.color, state.symbol)

                // Dodaj do listy i widoku
                circleQueue.add(circle)
                trackContainer.addView(view)

                // Ustaw listener
                view.setOnClickListener {
                    if (isGameRunning && !isProcessing && circleQueue.isNotEmpty() && circleQueue.last() == circle) {
                        when (state.symbol) {
                            Symbol.TARGET -> animateInstantSuccess(circle)
                            else -> animateFailure(circle)
                        }
                    }
                }
            }
        }

        // Jeśli gra była aktywna – wznawia licznik
        if (timerRunning && pauseOverlay.visibility != View.VISIBLE && !isProcessing) {
            timerProgressBar.start()
        }

        // Przywraca logikę gry
        trackContainer.post {
            if (isGameRunning) {
                adjustCircleQueueToView() // Dostosuj liczbę kół
                startAutoShift() // Uruchom logikę gry (timery, przesuwanie)
                if (circleQueue.isNotEmpty()) startActiveTimer()
            }
        }
    }

    // Dynamicznie dostosowuje layout do orientacji
    private fun updateLayoutForOrientation() {
        val currentConfig = resources.configuration
        val constraintLayout = findViewById<ConstraintLayout>(R.id.rootLayout)
        val constraintSet = ConstraintSet()
        constraintSet.clone(constraintLayout)

        if (currentConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // --- TRYB POZIOMY ---

            // Zmień parametry kół (mniejsze i mniej)
            currentCircleSizeDp = CIRCLE_SIZE_DP_LANDSCAPE
            currentVisibleCircles = VISIBLE_CIRCLES_LANDSCAPE

            // Zmień szerokość i wysokość kontenerów
            val offscreenMargin = dpToPx(-30)
            val landscapeWidth = dpToPx(220)
            val landscapeHeight = dpToPx(260)

            // Niebieski (lewy)
            constraintSet.constrainWidth(R.id.blueContainer, landscapeWidth)
            constraintSet.constrainHeight(R.id.blueContainer, landscapeHeight)
            constraintSet.clear(R.id.blueContainer, ConstraintSet.START)
            constraintSet.connect(R.id.blueContainer, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, offscreenMargin)

            // Czerwony (prawy)
            constraintSet.constrainWidth(R.id.redContainer, landscapeWidth)
            constraintSet.constrainHeight(R.id.redContainer, landscapeHeight)
            constraintSet.clear(R.id.redContainer, ConstraintSet.END)
            constraintSet.connect(R.id.redContainer, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, offscreenMargin)

        } else {
            // --- TRYB PIONOWY (domyślny) ---

            // Przywróć domyślne parametry kół
            currentCircleSizeDp = CIRCLE_SIZE_DP
            currentVisibleCircles = VISIBLE_CIRCLES

            // Przywróć oryginalną szerokość i wysokość kontenerów
            val originalMargin = dpToPx(-30)
            val originalWidth = dpToPx(120)
            val originalHeight = dpToPx(320)

            // Niebieski (lewy)
            constraintSet.constrainWidth(R.id.blueContainer, originalWidth)
            constraintSet.constrainHeight(R.id.blueContainer, originalHeight)
            constraintSet.clear(R.id.blueContainer, ConstraintSet.START)
            constraintSet.connect(R.id.blueContainer, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, originalMargin)

            // Czerwony (prawy)
            constraintSet.constrainWidth(R.id.redContainer, originalWidth)
            constraintSet.constrainHeight(R.id.redContainer, originalHeight)
            constraintSet.clear(R.id.redContainer, ConstraintSet.END)
            constraintSet.connect(R.id.redContainer, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, originalMargin)
        }

        // Zastosuj zmiany w layoucie
        constraintSet.applyTo(constraintLayout)

        // Wymuś ponowne przeliczenie pozycji kół, jeśli gra już trwa (po obrocie)
        if (isGameRunning && !countdownManager.isInProgress()) {
            updateCirclePositions()
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
        repeat(currentVisibleCircles) { createCircle() }
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
            layoutParams = FrameLayout.LayoutParams(dpToPx(currentCircleSizeDp), dpToPx(currentCircleSizeDp)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
            }
            scaleType = ImageView.ScaleType.FIT_CENTER
            setImageResource(getSymbolDrawable(symbol))
            background = createCircleBackground(color)
            elevation = 8f
            setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16))
        }

        val circle = Circle(nextCircleId++, view, color, symbol)
        circleQueue.add(0, circle) // Dodaje na górę listy
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

        // Dodaje nowe koło (na górę) i kontynuuje grę
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

        registerAttempt(true)
    }

    // Obsługuje błąd gracza
    private fun handleError() {
        timerProgressBar.subtractTime(6) // Kara: odjęcie czasu
        successfulStreak = 0
        totalMoves++
        accelerateIfNeeded()
        updateTempoDisplay()

        registerAttempt(false)
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
                if (isGameRunning && !isProcessing && circleQueue.size >= currentVisibleCircles) {
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
            // Layout jeszcze niezmierzony, spróbuj ponownie za chwilę
            trackContainer.post { updateCirclePositions() }
            return
        }

        val circleSize = dpToPx(currentCircleSizeDp).toFloat()

        val startY: Float
        val spacing: Float

        // Sprawdza czy jesteśmy w trybie pionowym na podstawie liczby kół
        if (currentVisibleCircles == VISIBLE_CIRCLES) {
            // --- TRYB PIONOWY ---
            val topMargin = dpToPx(32).toFloat()
            val bottomTargetY = trackHeight * 0.65f
            val totalSpace = bottomTargetY - topMargin
            val totalCirclesHeight = circleSize * currentVisibleCircles
            val totalGaps = currentVisibleCircles - 1
            val minSpacing = dpToPx(30).toFloat()

            spacing = if (totalSpace < totalCirclesHeight + minSpacing * totalGaps) minSpacing
            else (totalSpace - totalCirclesHeight) / totalGaps.toFloat()

            startY = topMargin

        } else {
            // --- TRYB POZIOMY ---
            val topMargin = dpToPx(2).toFloat()
            val bottomMargin = dpToPx(80).toFloat() // Margines od dołu
            spacing = dpToPx(8).toFloat() // Mniejszy odstęp

            // Całkowita wysokość zajmowana przez koła i odstępy
            val totalHeightOfCirclesAndGaps = (currentVisibleCircles * circleSize) +
                    ((currentVisibleCircles - 1).coerceAtLeast(0) * spacing)

            // Oblicz pozycję górnego koła tak aby dolne było nad marginesem
            val topYOfTopCircle = (trackHeight - bottomMargin) - totalHeightOfCirclesAndGaps
            startY = topYOfTopCircle.coerceAtLeast(topMargin)
        }

        // Rozmieszcza widoczne koła w pionie
        circleQueue.forEachIndexed { index, circle ->
            if (index < currentVisibleCircles) {
                // Ustaw pozycję i pokaż koło
                val y = startY + (circleSize + spacing) * index
                circle.view.y = y
                circle.view.visibility = View.VISIBLE
                circle.view.elevation = 8f
            } else {
                // Ukryj nadmiarowe koła (przejście z pionu na poziom)
                circle.view.visibility = View.GONE
            }
        }
    }

    // Dostosowuje liczbę kół w kolejce do wymagań widoku (pion/poziom)
    private fun adjustCircleQueueToView() {
        if (!isGameRunning) return

        val needed = currentVisibleCircles
        val current = circleQueue.size

        if (current < needed) {
            // Dodajemy brakujące koła na górę
            val difference = needed - current
            repeat(difference) {
                createCircle()
            }
        } else if (current > needed) {
            // Usuwamy nadmiarowe koła z góry
            val difference = current - needed
            repeat(difference) {
                if (circleQueue.isNotEmpty()) {
                    val circleToRemove = circleQueue.first() // Pobierz pierwsze (górne) koło
                    circleQueue.remove(circleToRemove)
                    trackContainer.removeView(circleToRemove.view)
                }
            }
        }

        // Po każdej zmianie, przelicz pozycje
        updateCirclePositions()
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
        // Pauzuj grę tylko jeśli nie jest to zmiana konfiguracji (obrót)
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