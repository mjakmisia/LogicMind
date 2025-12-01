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
import androidx.core.view.children
import androidx.core.view.isEmpty
import androidx.core.view.isVisible

class LeftOrRightActivity : BaseActivity() {
    private lateinit var countdownText: TextView // Pole tekstowe dla odliczania
    private lateinit var pauseButton: ImageButton // Przycisk pauzy
    private lateinit var pauseOverlay: ConstraintLayout // Nakładka menu pauzy
    private lateinit var timerProgressBar: GameTimerProgressBar // Pasek postępu czasu gry
    private lateinit var starManager: StarManager // Manager gwiazdek
    private lateinit var pauseMenu: PauseMenu // Menu pauzy gry
    private lateinit var countdownManager: GameCountdownManager // Manager odliczania
    private var isGameEnding = false // Flaga końca gry
    private var isProcessingMove = false // Flaga blokująca szybkie kliknięcia
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

    // Wymiary dla owoców w kolejce
    private var fruitSizePx: Int = 0
    private var overlapMarginPx: Int = 0

    // Konfiguracja poziomu
    private data class LevelConfig(
        val leftTargets: Int,
        val rightTargets: Int,
        val totalQueue: Int,
        val timeBonus: Int
    )

    // Zmienne do śledzenia postępu w poziomie
    private var fruitsSpawnedTotal = 0
    private var fruitsToSpawnLimit = 0
    private val maxVisibleFruits = 6

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

        updateLayoutForOrientation()

        // Ustawienie nasłuchiwania na kliknięcia koszyków
        leftBasket.setOnClickListener {
            handleBasketClick(isLeftBasket = true)
        }

        rightBasket.setOnClickListener {
            handleBasketClick(isLeftBasket = false)
        }

        // Inicjalizacja paska czasu
        timerProgressBar.setTotalTime(BASE_TIME_SECONDS)
        timerProgressBar.setOnFinishCallback {
            runOnUiThread {
                isGameEnding = true
                Toast.makeText(this, "Czas minął! Koniec gry!", Toast.LENGTH_LONG).show()
                gameContainer.isEnabled = false
                pauseOverlay.visibility = View.GONE
                
                updateUserStatistics(
                    categoryKey = GameKeys.CATEGORY_FOCUS,
                    gameKey = GameKeys.GAME_LEFT_OR_RIGHT,
                    starsEarned = starManager.starCount,
                    accuracy = gameStatsManager.calculateAccuracy(),
                    reactionTime = getAverageReactionTime(stars = starManager.starCount),
                )

                lastPlayedGame(
                    GameKeys.CATEGORY_FOCUS,
                    GameKeys.GAME_LEFT_OR_RIGHT
                )
                
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

                gameStatsManager.startReactionTracking()
                gameStatsManager.setGameStartTime(this@LeftOrRightActivity)
                
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
            onResume = {
                onGameResumed() 
                timerProgressBar.start() 
            },
            onPause = { 
                onGamePaused() 
                timerProgressBar.pause() 
            },
            onExit = { 
                updateUserStatistics(
                    categoryKey = GameKeys.CATEGORY_FOCUS,
                    gameKey = GameKeys.GAME_LEFT_OR_RIGHT,
                    starsEarned = starManager.starCount,
                    accuracy = gameStatsManager.calculateAccuracy(),
                    reactionTime = getAverageReactionTime(stars = starManager.starCount),
                )

                lastPlayedGame(
                    GameKeys.CATEGORY_FOCUS,
                    GameKeys.GAME_LEFT_OR_RIGHT
                )
                finish() 
            }, 
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
        outState.putInt("fruitsSpawnedTotal", fruitsSpawnedTotal)
        outState.putInt("fruitsToSpawnLimit", fruitsToSpawnLimit)
        outState.putIntegerArrayList("fruitQueue", ArrayList(fruitQueue))
        outState.putIntegerArrayList("leftBasketTargets", ArrayList(leftBasketTargets))
        outState.putIntegerArrayList("rightBasketTargets", ArrayList(rightBasketTargets))
        outState.putIntegerArrayList("activeGameFruits", ArrayList(activeGameFruits))
        starManager.saveState(outState)
    }

    // Przywraca stan aktywności
    private fun restoreGameState(savedInstanceState: Bundle) {
        pauseOverlay.visibility = savedInstanceState.getInt("pauseOverlayVisibility", View.GONE)
        countdownText.visibility = savedInstanceState.getInt("countdownTextVisibility", View.GONE)
        gameContainer.visibility = savedInstanceState.getInt("gameContainerVisibility", View.VISIBLE)
        pauseButton.visibility = savedInstanceState.getInt("pauseButtonVisibility", View.VISIBLE)
        currentLevel = savedInstanceState.getInt("currentLevel", 1)
        fruitsSpawnedTotal = savedInstanceState.getInt("fruitsSpawnedTotal", 0)
        fruitsToSpawnLimit = savedInstanceState.getInt("fruitsToSpawnLimit", 0)
        starManager.restoreState(savedInstanceState)

        // Przywracanie stanu timera
        val timerRemainingTimeMs = savedInstanceState.getLong("timerRemainingTimeMs", BASE_TIME_SECONDS * 1000L)
        val timerIsRunning = savedInstanceState.getBoolean("timerIsRunning", false)
        timerProgressBar.setRemainingTimeMs(timerRemainingTimeMs.coerceAtLeast(1L))

        if (timerIsRunning && pauseOverlay.visibility != View.VISIBLE) {
            timerProgressBar.start()
        }

        // Przywracanie stanu odliczania
        val countdownIndex = savedInstanceState.getInt("countdownIndex", 0)
        val countdownInProgress = savedInstanceState.getBoolean("countdownInProgress", false)
        // Kontynuowanie odliczania, jeśli było aktywne
        if (countdownInProgress) {
            countdownManager.startCountdown(countdownIndex)
        }

        pauseMenu.syncWithOverlay()

        fruitQueue.clear()
        savedInstanceState.getIntegerArrayList("fruitQueue")?.let { fruitQueue.addAll(it) }

        leftBasketTargets.clear()
        savedInstanceState.getIntegerArrayList("leftBasketTargets")?.let { leftBasketTargets.addAll(it) }

        rightBasketTargets.clear()
        savedInstanceState.getIntegerArrayList("rightBasketTargets")?.let { rightBasketTargets.addAll(it) }

        activeGameFruits.clear()
        savedInstanceState.getIntegerArrayList("activeGameFruits")?.let { activeGameFruits.addAll(it) }

        // Odtwarzanie widoku na podstawie danych
        if (gameContainer.isVisible && !countdownInProgress) {
            displayBasketTargets()
            displayFruitQueue()
        }
    }

    // Dostosowuje wygląd gry do orientacji ekranu (pion/poziom)
    private fun updateLayoutForOrientation() {
        val isLandscape = resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

        // Ustaw rozmiar owoców w kolejce
        if (isLandscape) {
            fruitSizePx = (70 * resources.displayMetrics.density).toInt()
            overlapMarginPx = -(35 * resources.displayMetrics.density).toInt()
        } else {
            fruitSizePx = (120 * resources.displayMetrics.density).toInt()
            overlapMarginPx = -(60 * resources.displayMetrics.density).toInt()
        }

        // Dostosuj marginesy i pozycje (tylko dla poziomu)
        if (isLandscape) {
            // Podnieś kolejkę
            val queueParams = fruitQueueContainer.layoutParams as ConstraintLayout.LayoutParams
            queueParams.topMargin = 0
            queueParams.verticalBias = 0.15f
            fruitQueueContainer.layoutParams = queueParams

            // Obniż koszyki
            val smallBottomMargin = (4 * resources.displayMetrics.density).toInt()

            val leftParams = leftBasket.layoutParams as ConstraintLayout.LayoutParams
            leftParams.bottomMargin = smallBottomMargin
            leftBasket.layoutParams = leftParams

            val rightParams = rightBasket.layoutParams as ConstraintLayout.LayoutParams
            rightParams.bottomMargin = smallBottomMargin
            rightBasket.layoutParams = rightParams
        }
    }

    private fun getLevelConfig(level: Int): LevelConfig {
        return when (level) {
            1 -> LevelConfig(1, 1, 8, 0)
            2 -> LevelConfig(2, 1, 12, 5)
            3 -> LevelConfig(2, 2, 16, 8)
            4 -> LevelConfig(3, 2, 20, 10)
            5 -> LevelConfig(3, 3, 25, 12)
            else -> LevelConfig(3, 3, 25 + (level - 5) * 5, 15)
        }
    }

    private fun startNewGame() {
        fruitQueue.clear()
        fruitQueueContainer.removeAllViews()

        val config = getLevelConfig(currentLevel)

        // Ustaw limity
        fruitsToSpawnLimit = config.totalQueue
        fruitsSpawnedTotal = 0

        // Ustaw cele dla koszyków
        setupBaskets(config.leftTargets, config.rightTargets)
        displayBasketTargets()

        Toast.makeText(this, "Poziom $currentLevel", Toast.LENGTH_SHORT).show()

        // Wygeneruj początkową kolejkę
        val initialFruits = minOf(maxVisibleFruits, fruitsToSpawnLimit)
        generateFruitQueue(initialFruits)

        // Zaktualizuj licznik stworzonych owoców
        fruitsSpawnedTotal = initialFruits

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

        // Przejdź przez kolejkę (odwrotnie), stwórz widoki i dodaj je
        fruitQueue.reversed().forEach { fruitId ->
            fruitQueueContainer.addView(createFruitView(fruitId))
        }

        updateQueueVisuals()
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
        val iconMarginPx = (1 * resources.displayMetrics.density).toInt()
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

    // Obsługuje kliknięcie w koszyk
    private fun handleBasketClick(isLeftBasket: Boolean) {
        if (fruitQueue.isEmpty() || isProcessingMove) return
        isProcessingMove = true

        val currentFruit = fruitQueue.first()

        // Sprawdź czy owoc pasuje do wybranego koszyka
        val targetList = if (isLeftBasket) leftBasketTargets else rightBasketTargets
        val isCorrect = targetList.contains(currentFruit)

        // Wybór wizualnego celu (koszyka)
        val targetBasketView = if (isLeftBasket) leftBasket else rightBasket

        // Przejdź do następnego owocu z animacją
        advanceToNextFruit(isCorrect, targetBasketView)
    }

    // Przesuwa kolejkę do następnego owocu z animacją lotu
    private fun advanceToNextFruit(isCorrect: Boolean, targetBasket: View) {

        gameStatsManager.registerAttempt(isCorrect)

        gameContainer.post {
            if (isCorrect) {
                starManager.increment()
            } else {
                timerProgressBar.subtractTime(3)
            }
        }

        if (fruitQueueContainer.isEmpty()) {
            isProcessingMove = false
            return
        }

        // Pobierz widok owocu do animacji
        val viewToAnimate = fruitQueueContainer.getChildAt(fruitQueueContainer.childCount - 1)

        // Uruchom animację
        animateFruitToBasket(viewToAnimate, targetBasket) {}

        // Usuń stary owoc z kolejki
        if (fruitQueue.isNotEmpty()) fruitQueue.removeAt(0)
        fruitQueueContainer.removeView(viewToAnimate)

        // Sprawdź limit i dodaj nowy owoc
        if (activeGameFruits.isNotEmpty() && fruitsSpawnedTotal < fruitsToSpawnLimit) {
            val newFruitId = activeGameFruits.random()
            fruitQueue.add(newFruitId)
            fruitsSpawnedTotal++

            val newFruitView = createFruitView(newFruitId)
            newFruitView.alpha = 0f
            fruitQueueContainer.addView(newFruitView, 0) // Dodaj na spód

            newFruitView.animate().alpha(1f).setDuration(200).start()
        }

        // Płynna animacja zmiany rozmiarów
        android.transition.TransitionManager.beginDelayedTransition(fruitQueueContainer)
        updateQueueVisuals()

        // Sprawdź koniec poziomu
        if (fruitQueueContainer.isEmpty()) {
            val config = getLevelConfig(currentLevel)
            if (config.timeBonus > 0) {
                timerProgressBar.addTime(config.timeBonus)
            }

            currentLevel++

            // Zrestartuj grę po krótkiej pauzie
            gameContainer.postDelayed({
                startNewGame()
            }, 1000)
        }

        isProcessingMove = false
    }

    // Tworzy pojedynczy widok dla owocu
    private fun createFruitView(fruitId: Int): ImageView {
        return ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                fruitSizePx,
                fruitSizePx
            )
            setImageResource(fruitId)
        }
    }

    // Aktualizuje marginesy, elewację i rozmiar dla wszystkich owoców w kolejce
    private fun updateQueueVisuals() {
        // Ustawiamy rozmiar owocu na końcu kolejki
        val minScale = 0.6f // 60%
        val maxScale = 1.0f // 100%
        val scaleRange = maxScale - minScale
        val totalItems = fruitQueueContainer.childCount

        // Zabezpieczenie przed dzieleniem przez zero
        val maxIndex = if (totalItems > 1) totalItems - 1 else 1

        // Pętla po wszystkich widokach owoców w kontenerze
        fruitQueueContainer.children.forEachIndexed { index, view ->

            // Obliczanie skali
            val percentage = if (totalItems == 1) 1.0f else index.toFloat() / maxIndex.toFloat()
            val scale = minScale + (percentage * scaleRange)

            // Obliczanie nowego rozmiaru na podstawie skali
            val newSize = (fruitSizePx * scale).toInt()

            view.elevation = (index + 1).toFloat()

            // Pobierz parametry layoutu i zaktualizuj
            (view.layoutParams as LinearLayout.LayoutParams).also {

                it.topMargin = if (index > 0) overlapMarginPx else 0

                it.width = newSize
                it.height = newSize
            }
        }
    }

    // Animacja owocu wpadającego do koszyka
    private fun animateFruitToBasket(fruitView: View, targetBasket: View, onAnimationEnd: () -> Unit) {

        // Pobierz pozycję owocu i koszyka na ekranie
        val startLoc = IntArray(2)
        fruitView.getLocationOnScreen(startLoc)

        val endLoc = IntArray(2)
        targetBasket.getLocationOnScreen(endLoc)

        // Pobierz pozycję kontenera gry
        val parentLoc = IntArray(2)
        gameContainer.getLocationOnScreen(parentLoc)

        // Oblicz pozycję startową względem kontenera
        val startX = startLoc[0] - parentLoc[0]
        val startY = startLoc[1] - parentLoc[1]

        // Utwórz tymczasowy obraz owocu do animacji
        val flyingFruit = ImageView(this).apply {
            setImageDrawable((fruitView as ImageView).drawable)
            layoutParams = ConstraintLayout.LayoutParams(fruitView.width, fruitView.height)
            x = startX.toFloat()
            y = startY.toFloat()
            elevation = 20f // Ustawienie wyżej niż inne elementy
        }

        // Dodaj tymczasowy owoc do kontenera gry
        gameContainer.addView(flyingFruit)

        // Ukryj oryginalny owoc
        fruitView.visibility = View.INVISIBLE

        // Oblicz pozycję docelową (środek koszyka)
        val targetX = (endLoc[0] - parentLoc[0]) + (targetBasket.width / 2) - (fruitView.width / 2)
        val targetY = (endLoc[1] - parentLoc[1]) + (targetBasket.height / 2) - (fruitView.height / 2)

        // Rozpocznij animację lotu
        flyingFruit.animate()
            .x(targetX.toFloat())
            .y(targetY.toFloat())
            .rotation(360f) // Pojedynczy obrót
            .scaleX(0.5f)   // Delikatne zmniejszenie
            .scaleY(0.5f)
            .alpha(0f)      // Zanikanie
            .setDuration(400)
            .withEndAction {
                // Usuń tymczasowy owoc i wywołaj callback
                gameContainer.removeView(flyingFruit)
                onAnimationEnd()
            }
            .start()
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