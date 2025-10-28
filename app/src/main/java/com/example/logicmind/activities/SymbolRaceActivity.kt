package com.example.logicmind.activities

import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
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
    private lateinit var trackContainer: FrameLayout        // Kontener na kółka (ścieżka)
    private lateinit var trackLine: View                     // Linia toru (wizualna)
    private lateinit var countdownText: TextView            // Tekst odliczania początkowego
    private lateinit var pauseButton: ImageButton           // Przycisk pauzy
    private lateinit var pauseOverlay: ConstraintLayout     // Nakładka menu pauzy
    private lateinit var timerProgressBar: GameTimerProgressBar // Pasek postępu czasu gry
    private lateinit var starManager: StarManager           // Manager gwiazdek (punkty)
    private lateinit var pauseMenu: PauseMenu               // Menu pauzy
    private lateinit var countdownManager: GameCountdownManager // Manager odliczania startowego
    private lateinit var blueContainer: FrameLayout         // Lewy pojemnik (klik = niebieski)
    private lateinit var redContainer: FrameLayout          // Prawy pojemnik (klik = czerwony)

    // Stan gry
    private val circleQueue = mutableListOf<Circle>()       // Kolejka kółek: indeks 0 = góra (najnowsze), ostatni = dół (do kliknięcia)
    private var isProcessing = false                        // Flaga: trwa usuwanie kółka (blokada autoShift)
    private val handler = Handler(Looper.getMainLooper())   // Handler do opóźnień (autoShift, BLOCK, itp.)
    private var autoShiftRunnable: Runnable? = null         // Zadanie cykliczne: automatyczne przesuwanie
    private var awaitingDoubleClick = false                 // Flaga: oczekujemy drugiego kliknięcia (DOUBLE_LEFT/RIGHT)

    companion object {
        private const val BASE_TIME_SECONDS = 90             // Czas gry w sekundach
        private const val REACTION_TIME_MS = 5000L           // Co ile ms przesuwa się kółko (jeśli nie kliknięto)
        private const val CIRCLE_SIZE_DP = 130               // Rozmiar kółka w dp
        private const val VISIBLE_CIRCLES = 4                // Ile kółek widać na ekranie
    }

    // Kolory kółek i ich obramowania
    private val RED_COLOR = "#EF5350".toColorInt()      // Kolor czerwonego kółka
    private val BLUE_COLOR = "#4FC3F7".toColorInt()     // Kolor niebieskiego kółka
    private val RED_STROKE = "#D32F2F".toColorInt()     // Obramowanie czerwonego
    private val BLUE_STROKE = "#0288D1".toColorInt()    // Obramowanie niebieskiego
    private val STROKE_WIDTH_DP = 6                          // Grubość obramowania w dp

    // Typy symboli na kółkach
    private enum class Symbol {
        EMPTY,       // Puste – dopasuj kolor
        LEFT,        // Strzałka w lewo – kliknij lewy pojemnik
        RIGHT,       // Strzałka w prawo – kliknij prawy pojemnik
        BOTH,        // Zakaz – nie klikaj nigdzie
        DOUBLE_LEFT, // Podwójna lewa – dwa szybkie kliki w lewo
        DOUBLE_RIGHT,// Podwójna prawa – dwa szybkie kliki w prawo
        SWAP,        // Zamiana – kliknij przeciwny kolor
        TARGET,      // Cel – kliknij kółko, gdy jest na dole
        BLOCK        // Blokada – poczekaj 2s, aż zniknie
    }

    // Klasa danych kółka
    private data class Circle(
        val view: ImageView,                                // Widok ImageView kółka
        val color: Int,                                     // Kolor tła (czerwony/niebieski)
        val symbol: Symbol                                  // Symbol na kółku
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_symbol_race)
        supportActionBar?.hide()

        initViews()                  // Inicjalizacja widoków
        setupTimer()                 // Konfiguracja paska czasu
        setupCountdown()             // Konfiguracja odliczania startowego
        setupPauseMenu()             // Konfiguracja menu pauzy
        setupClickListeners()        // Nasłuchiwanie kliknięć na pojemniki

        if (savedInstanceState == null) {
            // Pierwsze uruchomienie – ukryj elementy przed odliczaniem
            trackLine.visibility = View.GONE
            blueContainer.visibility = View.INVISIBLE
            redContainer.visibility = View.INVISIBLE
            countdownManager.startCountdown()
        } else {
            restoreGameState(savedInstanceState) // Przywróć stan po rotacji
        }
    }

    // Inicjalizacja widoków
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

    // Nasłuchiwanie kliknięć na lewy/prawy pojemnik
    private fun setupClickListeners() {
        blueContainer.setOnClickListener { onContainerClick(isBlue = true) }
        redContainer.setOnClickListener { onContainerClick(isBlue = false) }
    }

    // Konfiguracja paska czasu
    private fun setupTimer() {
        timerProgressBar.setTotalTime(BASE_TIME_SECONDS)
        timerProgressBar.setOnFinishCallback {
            runOnUiThread {
                Toast.makeText(this, "Czas minął!", Toast.LENGTH_LONG).show()
                endGame()
            }
        }
    }

    // Konfiguracja odliczania początkowego
    private fun setupCountdown() {
        countdownManager = GameCountdownManager(
            countdownText = countdownText,
            gameView = trackContainer,
            viewsToHide = listOf(
                pauseButton,
                findViewById(R.id.starCountText),
                findViewById(R.id.starIcon),
                timerProgressBar
            ),
            onCountdownFinished = {
                starManager.reset()
                timerProgressBar.start()
                // Przywracamy widoczność po odliczaniu
                trackLine.visibility = View.VISIBLE
                blueContainer.visibility = View.VISIBLE
                redContainer.visibility = View.VISIBLE
                startNewGame()
            }
        )
    }

    // Konfiguracja menu pauzy
    private fun setupPauseMenu() {
        pauseMenu = PauseMenu(
            context = this,
            pauseOverlay = pauseOverlay,
            pauseButton = pauseButton,
            onRestart = {
                if (pauseMenu.isPaused) pauseMenu.resume()
                timerProgressBar.reset()
                // Ukrywamy przed odliczaniem
                trackLine.visibility = View.GONE
                blueContainer.visibility = View.INVISIBLE
                redContainer.visibility = View.INVISIBLE
                countdownManager.startCountdown()
            },
            onResume = { timerProgressBar.start() },
            onPause = { timerProgressBar.pause() },
            onExit = { finish() },
            instructionTitle = getString(R.string.instructions),
            instructionMessage = getString(R.string.path_change_instruction)
        )
    }

    // Rozpoczyna nową grę
    private fun startNewGame() {
        circleQueue.clear()
        trackContainer.removeAllViews()
        trackLine.visibility = View.VISIBLE

        repeat(VISIBLE_CIRCLES) { createCircle() }
        updatePositionsInstantly()
        startAutoShift()
    }

    // Tworzy nowe kółko i dodaje je na górę kolejki
    private fun createCircle() {
        val isRed = Random.nextBoolean()
        val color = if (isRed) RED_COLOR else BLUE_COLOR
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

        trackContainer.addView(view)
        val circle = Circle(view, color, symbol)

        // Dodajemy nowe kółko na górę kolejki
        circleQueue.add(0, circle)

        // Obsługa specjalnych symboli
        when (symbol) {
            Symbol.BLOCK -> handler.postDelayed({
                // Gwiazdka za przeczekanie 2s
                if (circleQueue.isNotEmpty() && circleQueue.last() == circle) {
                    removeBottomCircle(showStar = true)
                }
            }, 2000L)
            Symbol.TARGET -> view.setOnClickListener {
                if (circleQueue.isNotEmpty() && circleQueue.last() == circle) {
                    removeBottomCircle(showStar = true)
                }
            }
            else -> {}
        }
    }

    // Obsługuje kliknięcie na lewy (isBlue=true) lub prawy (isBlue=false) pojemnik
    private fun onContainerClick(isBlue: Boolean) {
        if (circleQueue.isEmpty()) return
        val bottomCircle = circleQueue.last()
        val color = bottomCircle.color
        val symbol = bottomCircle.symbol
        val isRedCircle = color == RED_COLOR

        var correct = false

        when (symbol) {
            Symbol.EMPTY -> correct = (isBlue && color == BLUE_COLOR) || (!isBlue && color == RED_COLOR)
            Symbol.LEFT -> correct = isBlue
            Symbol.RIGHT -> correct = !isBlue
            Symbol.DOUBLE_LEFT -> {
                if (!awaitingDoubleClick && isBlue) {
                    awaitingDoubleClick = true
                    handler.postDelayed({ awaitingDoubleClick = false }, 600L)
                    return
                } else if (awaitingDoubleClick && isBlue) {
                    correct = true
                    awaitingDoubleClick = false
                }
            }
            Symbol.DOUBLE_RIGHT -> {
                if (!awaitingDoubleClick && !isBlue) {
                    awaitingDoubleClick = true
                    handler.postDelayed({ awaitingDoubleClick = false }, 600L)
                    return
                } else if (awaitingDoubleClick && !isBlue) {
                    correct = true
                    awaitingDoubleClick = false
                }
            }
            Symbol.BOTH -> correct = false
            Symbol.SWAP -> correct = (isBlue && isRedCircle) || (!isBlue && !isRedCircle)
            Symbol.TARGET, Symbol.BLOCK -> return
        }

        if (correct) starManager.increment()
        removeBottomCircle(showStar = false)
    }

    // Usuwa kółko z dołu kolejki i tworzy nowe na górze
    private fun removeBottomCircle(showStar: Boolean) {
        if (circleQueue.isEmpty()) return
        val removed = circleQueue.removeAt(circleQueue.lastIndex)
        trackContainer.removeView(removed.view)
        if (showStar) starManager.increment()

        // Tworzymy nowe kółko na górze
        createCircle()
        updatePositionsInstantly()
    }

    // Mapuje symbol na odpowiedni drawable
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

    // Tworzy tło kółka z kolorem i obramowaniem
    private fun createCircleBackground(color: Int): Drawable {
        val strokeColor = if (color == RED_COLOR) RED_STROKE else BLUE_STROKE
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
            setStroke(dpToPx(STROKE_WIDTH_DP), strokeColor)
        }
    }

    // Aktualizuje pozycje Y wszystkich widocznych kółek (natychmiastowo)
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

    // Uruchamia automatyczne przesuwanie kółek co REACTION_TIME_MS
    private fun startAutoShift() {
        cancelAutoShift()
        autoShiftRunnable = object : Runnable {
            override fun run() {
                if (isProcessing || circleQueue.size < VISIBLE_CIRCLES) {
                    handler.postDelayed(this, REACTION_TIME_MS)
                    return
                }
                isProcessing = true
                removeBottomCircle(showStar = false)
                isProcessing = false
                handler.postDelayed(this, REACTION_TIME_MS)
            }
        }
        handler.postDelayed(autoShiftRunnable!!, REACTION_TIME_MS)
    }

    // Anuluje automatyczne przesuwanie
    private fun cancelAutoShift() {
        autoShiftRunnable?.let { handler.removeCallbacks(it) }
        autoShiftRunnable = null
    }

    // Kończy grę – wyłącza wszystko i zamyka aktywność
    private fun endGame() {
        cancelAutoShift()
        trackContainer.isEnabled = false
        finish()
    }

    // Zapisuje stan gry (do rotacji ekranu)
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

    // Przywraca stan gry po rotacji
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
            if (circleQueue.isNotEmpty()) updatePositionsInstantly()
        }
    }

    // Automatyczna pauza przy wyjściu z aktywności
    override fun onPause() {
        super.onPause()
        if (!pauseMenu.isPaused && !isChangingConfigurations) pauseMenu.pause()
    }

    // Czyszczenie zasobów
    override fun onDestroy() {
        super.onDestroy()
        timerProgressBar.cancel()
        countdownManager.cancel()
        cancelAutoShift()
    }

    // Konwersja dp na px
    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()
}