package com.example.logicmind.activities
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.logicmind.R
import com.example.logicmind.common.GameCountdownManager
import com.example.logicmind.common.GameTimerProgressBar
import com.example.logicmind.common.PauseMenu
import com.example.logicmind.common.StarManager

class LeftOrRightActivity : BaseActivity() {
    private lateinit var countdownText: TextView // Pole tekstowe dla odliczania
    private lateinit var pauseButton: ImageButton // Przycisk pauzy
    private lateinit var pauseOverlay: ConstraintLayout // Nakładka menu pauzy
    private lateinit var timerProgressBar: GameTimerProgressBar // Pasek postępu czasu gry
    private lateinit var starManager: StarManager // Manager gwiazdek
    private lateinit var pauseMenu: PauseMenu // Menu pauzy gry
    private lateinit var countdownManager: GameCountdownManager // Manager odliczania
    private var isGameEnding = false // Flaga końca gry
    private var currentLevel = 1 // Aktualny poziom gry
    private lateinit var gameContainer: ConstraintLayout // Główny kontener gry
    private lateinit var fruitQueueContainer: LinearLayout // Kontener na owoce w kolejce
    private lateinit var leftBasket: ImageView
    private lateinit var rightBasket: ImageView

    // Lista obrazów owoców używanych w grze
    private val fruitDrawables = listOf(
        R.drawable.fruit_card_apple,
        R.drawable.fruit_card_banana,
        R.drawable.fruit_card_blueberry,
        R.drawable.fruit_card_lemon,
        R.drawable.fruit_card_orange,
        R.drawable.fruit_card_pineapple,
        R.drawable.fruit_card_strawberry,
        R.drawable.fruit_card_watermelon,
        R.drawable.fruit_card_avocado,
        R.drawable.fruit_card_cherries,
        R.drawable.fruit_card_coconut,
        R.drawable.fruit_card_dragonfruit,
        R.drawable.fruit_card_grapes,
        R.drawable.fruit_card_mango,
        R.drawable.fruit_card_pear,
        R.drawable.fruit_card_raspberries
    )
    private val fruitQueue = mutableListOf<Int>() // Lista przechowująca wygenerowaną kolejkę

    // Listy celów dla koszyków
    private val leftBasketTargets = mutableListOf<Int>()
    private val rightBasketTargets = mutableListOf<Int>()

    private val activeGameFruits = mutableListOf<Int>() // Lista owoców które mogą pojawić się w danej rundzie

    // Kontenery kółka z owocami na koszykach
    private lateinit var leftBasketTargetContainer: LinearLayout
    private lateinit var rightBasketTargetContainer: LinearLayout

    companion object {
        private const val BASE_TIME_SECONDS = 90 // Czas trwania jednej rundy (w sekundach)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_left_or_right)
        supportActionBar?.hide()

        // Inicjalizacja widoków
        countdownText = findViewById(R.id.countdownText)
        pauseButton = findViewById(R.id.pauseButton)
        pauseOverlay = findViewById(R.id.pauseOverlay)
        timerProgressBar = findViewById(R.id.gameTimerProgressBar)
        starManager = StarManager()
        starManager.init(findViewById(R.id.starCountText))
        gameContainer = findViewById(R.id.gameContainer)
        fruitQueueContainer = findViewById(R.id.fruitQueueContainer)
        leftBasket = findViewById(R.id.leftBasket)
        rightBasket = findViewById(R.id.rightBasket)
        leftBasketTargetContainer = findViewById(R.id.leftBasketTargetContainer)
        rightBasketTargetContainer = findViewById(R.id.rightBasketTargetContainer)

        // Inicjalizacja paska czasu
        timerProgressBar.setTotalTime(BASE_TIME_SECONDS)
        timerProgressBar.setOnFinishCallback {
            runOnUiThread {
                isGameEnding = true
                Toast.makeText(this, "Czas minął! Koniec gry!", Toast.LENGTH_LONG).show()
                gameContainer.isEnabled = false
                pauseOverlay.visibility = View.GONE
                finish()
            }
        }

        // Inicjalizacja managera odliczania
        countdownManager = GameCountdownManager(
            countdownText = countdownText,
            gameView = gameContainer,
            viewsToHide = listOf(
                pauseButton,
                findViewById<TextView>(R.id.starCountText),
                findViewById<ImageView>(R.id.starIcon),
                timerProgressBar),
            onCountdownFinished = {
                currentLevel = 1
                starManager.reset()
                timerProgressBar.stop()
                timerProgressBar.reset()
                timerProgressBar.start()
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
                currentLevel = 1
                starManager.reset()
                timerProgressBar.stop()
                timerProgressBar.reset()

                countdownManager.startCountdown()
            },
            onResume = { timerProgressBar.start() }, // Wznawia timer po pauzie
            onPause = { timerProgressBar.pause() },  // Zatrzymuje timer podczas pauzy
            onExit = { finish() }, // Kończy aktywność
            instructionTitle = getString(R.string.instructions),
            instructionMessage = getString(R.string.left_or_right_instruction),
        )

        // Sprawdzenie, czy gra jest uruchamiana po raz pierwszy
        if (savedInstanceState == null) {
            countdownManager.startCountdown() // Rozpoczyna odliczanie początkowe
        } else {
            restoreGameState(savedInstanceState) // Przywraca stan po rotacji
        }
    }

    // Zapisuje stan aktywności
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("pauseOverlayVisibility", pauseOverlay.visibility)
        outState.putInt("countdownTextVisibility", countdownText.visibility)
        outState.putInt("gameContainerVisibility", gameContainer.visibility)
        outState.putInt("pauseButtonVisibility", pauseButton.visibility)
        outState.putLong("timerRemainingTimeMs", timerProgressBar.getRemainingTimeSeconds() * 1000L)
        outState.putBoolean("timerIsRunning", timerProgressBar.isRunning())
        outState.putInt("countdownIndex", countdownManager.getIndex())
        outState.putBoolean("countdownInProgress", countdownManager.isInProgress())
        outState.putInt("currentLevel", currentLevel)
        starManager.saveState(outState)
    }

    // Przywraca stan aktywności
    private fun restoreGameState(savedInstanceState: Bundle) {
        pauseOverlay.visibility = savedInstanceState.getInt("pauseOverlayVisibility", View.GONE)
        countdownText.visibility = savedInstanceState.getInt("countdownTextVisibility", View.GONE)
        gameContainer.visibility = savedInstanceState.getInt("gameContainerVisibility", View.VISIBLE)
        pauseButton.visibility = savedInstanceState.getInt("pauseButtonVisibility", View.VISIBLE)
        currentLevel = savedInstanceState.getInt("currentLevel", 1)
        starManager.restoreState(savedInstanceState)

        // Przywracanie stanu timera
        val timerRemainingTimeMs = savedInstanceState.getLong("timerRemainingTimeMs", BASE_TIME_SECONDS * 1000L)
        val timerIsRunning = savedInstanceState.getBoolean("timerIsRunning", false)
        timerProgressBar.setRemainingTimeMs(timerRemainingTimeMs.coerceAtLeast(1L))

        if (timerIsRunning && pauseOverlay.visibility != View.VISIBLE) {
            timerProgressBar.start()
        }

        //Przywracanie stanu odliczania
        val countdownIndex = savedInstanceState.getInt("countdownIndex", 0)
        val countdownInProgress = savedInstanceState.getBoolean("countdownInProgress", false)
        // Kontynuowanie odliczania, jeśli było aktywne
        if (countdownInProgress) {
            countdownManager.startCountdown(countdownIndex)
        }

        pauseMenu.syncWithOverlay()
    }

    private fun startNewGame() {
        fruitQueue.clear()
        fruitQueueContainer.removeAllViews()

        // Ustaw cele dla koszyków
        setupBaskets(leftCount = 2, rightCount = 1)

        displayBasketTargets()

        generateFruitQueue(5)

        displayFruitQueue()
    }

    // Generuje losową kolejkę owoców
    private fun generateFruitQueue(count: Int) {
        if (activeGameFruits.isEmpty()) return

        repeat(count) {
            fruitQueue.add(activeGameFruits.random())
        }
    }

    // Wyświetla kolejkę owoców
    private fun displayFruitQueue() {
        fruitQueueContainer.removeAllViews()

        val fruitSizePx = (100 * resources.displayMetrics.density).toInt() // Rozmiar owoców
        val overlapMarginPx = -(55 * resources.displayMetrics.density).toInt() // Nachodzenie na siebie ikon

        // Przejdź przez kolejkę owoców aby dodać je na ekran
        fruitQueue.reversed().forEachIndexed { index, fruitId ->
            val fruitView = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    fruitSizePx,
                    fruitSizePx
                ).also {
                    if (index > 0) {
                        it.topMargin = overlapMarginPx
                    }
                }

                setImageResource(fruitId)

                elevation = (index + 1).toFloat()
            }

            fruitQueueContainer.addView(fruitView)
        }
    }

    // Losuje i ustawia cele dla koszyków oraz tworzy listę aktywnych owoców
    private fun setupBaskets(leftCount: Int, rightCount: Int) {
        leftBasketTargets.clear()
        rightBasketTargets.clear()
        activeGameFruits.clear()

        val shuffledFruits = fruitDrawables.shuffled()

        // Przypisz cele
        val leftTargets = shuffledFruits.take(leftCount)
        val rightTargets = shuffledFruits.drop(leftCount).take(rightCount)

        leftBasketTargets.addAll(leftTargets)
        rightBasketTargets.addAll(rightTargets)

        // Stwórz listę aktywnych owoców
        activeGameFruits.addAll(leftBasketTargets)
        activeGameFruits.addAll(rightBasketTargets)
        activeGameFruits.distinct()
    }

     // Dynamicznie tworzy i wyświetla ikony celów w koszykach
    private fun displayBasketTargets() {
        leftBasketTargetContainer.removeAllViews()
        rightBasketTargetContainer.removeAllViews()

        leftBasketTargets.forEach { fruitId ->
            val icon = createTargetIcon(fruitId)
            leftBasketTargetContainer.addView(icon)
        }

        rightBasketTargets.forEach { fruitId ->
            val icon = createTargetIcon(fruitId)
            rightBasketTargetContainer.addView(icon)
        }
    }

    // Tworzy pojedynczą ikonę celu
    private fun createTargetIcon(fruitId: Int): ImageView {
        val iconSizePx = (64 * resources.displayMetrics.density).toInt()
        val iconMarginPx = (4 * resources.displayMetrics.density).toInt()
        val paddingPx = (12 * resources.displayMetrics.density).toInt() // Wewnętrzny padding

        return ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(iconSizePx, iconSizePx).also {
                it.setMargins(iconMarginPx, iconMarginPx, iconMarginPx, iconMarginPx)
            }

            setBackgroundResource(R.drawable.bg_white_circle)
            setPadding(paddingPx, paddingPx, paddingPx, paddingPx)
            setImageResource(fruitId)
            elevation = 2f
        }
    }

    override fun onPause() {
        super.onPause()
        // Pauzuje grę automatycznie przy wyjściu z aktywności
        if (!pauseMenu.isPaused && !isChangingConfigurations) {
            pauseMenu.pause()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        timerProgressBar.stop()
        countdownManager.cancel()
    }
}