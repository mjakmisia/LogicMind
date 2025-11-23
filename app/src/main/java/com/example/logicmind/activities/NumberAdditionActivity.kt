package com.example.logicmind.activities

import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.gridlayout.widget.GridLayout
import com.example.logicmind.R
import com.example.logicmind.common.GameCountdownManager
import com.example.logicmind.common.GameTimerProgressBar
import com.example.logicmind.common.PauseMenu
import com.example.logicmind.common.StarManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.shape.CornerFamily

class NumberAdditionActivity : BaseActivity() {

    private lateinit var targetNumberText: TextView // Pole tekstowe z docelową sumą
    private lateinit var numberGrid: GridLayout // Siatka z przyciskami liczb
    private lateinit var countdownText: TextView // Pole tekstowe dla odliczania
    private lateinit var pauseButton: ImageView // Przycisk pauzy
    private lateinit var pauseOverlay: View // Nakładka menu pauzy
    private lateinit var countdownManager: GameCountdownManager // Manager odliczania
    private lateinit var timerProgressBar: GameTimerProgressBar // Pasek postępu czasu gry
    private lateinit var starManager: StarManager // Manager gwiazdek
    private lateinit var pauseMenu: PauseMenu // Menu pauzy gry
    private val numbers = mutableListOf<Int>() // Lista liczb na siatce
    private val selectedButtons = mutableListOf<MaterialButton>() // Wybrane przyciski
    private var level = 1 // Aktualny poziom gry
    private var isGameEnding = false // Flaga końca gry
    private var isGameActive = false // Flaga, sprawdzająca czy gra jest w trakcie
    private var isShowingError = false  // Flaga blokująca kliki podczas błędu

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_number_addition)

        supportActionBar?.hide()

        // Inicjalizacja widoków
        targetNumberText = findViewById(R.id.targetNumberText)
        numberGrid = findViewById(R.id.numberGrid)
        countdownText = findViewById(R.id.countdownText)
        pauseButton = findViewById(R.id.pauseButton)
        pauseOverlay = findViewById(R.id.pauseOverlay)
        timerProgressBar = findViewById(R.id.gameTimerProgressBar)
        starManager = StarManager()
        starManager.init(findViewById(R.id.starCountText))

        // Inicjalizacja paska czasu
        timerProgressBar.setTotalTime(90) // Ustaw czas na 1,5 minuty
        timerProgressBar.setOnFinishCallback {
            runOnUiThread {
                isGameEnding = true
                Toast.makeText(this, "Czas minął! Koniec gry!", Toast.LENGTH_LONG).show()
                numberGrid.isEnabled = false
                pauseOverlay.visibility = View.GONE
                updateUserStatistics(
                    categoryKey = GameKeys.CATEGORY_REASONING,
                    gameKey = GameKeys.GAME_NUMBER_ADDITION,
                    starsEarned = starManager.starCount,
                    accuracy = gameStatsManager.calculateAccuracy(),
                    reactionTime = getAverageReactionTime(stars = starManager.starCount),
                )
                lastPlayedGame(GameKeys.CATEGORY_REASONING, GameKeys.GAME_NUMBER_ADDITION, getString(R.string.number_addition))
                finish()
            }
        }

        // Inicjalizacja managera odliczania
        countdownManager = GameCountdownManager(
            countdownText = countdownText,
            gameView = numberGrid,
            viewsToHide = listOf(
                pauseButton,
                findViewById<TextView>(R.id.starCountText),
                findViewById<ImageView>(R.id.starIcon),
                timerProgressBar),
            onCountdownFinished = {
                level = 1
                starManager.reset()
                timerProgressBar.stop()
                timerProgressBar.reset()
                timerProgressBar.start()
                targetNumberText.visibility = View.VISIBLE
                numberGrid.visibility = View.VISIBLE
                isGameActive = true

                gameStatsManager.startReactionTracking()
                gameStatsManager.setGameStartTime(this@NumberAdditionActivity)

                startLevel()
            }
        )

        // Inicjalizacja menu pauzy
        pauseMenu = PauseMenu(
            context = this,
            pauseOverlay = pauseOverlay,
            pauseButton = pauseButton,
            onRestart = {
                if (pauseMenu.isPaused) pauseMenu.resume()
                level = 1
                starManager.reset()
                timerProgressBar.stop()
                timerProgressBar.reset()

                // Czyszczenie siatki i stanów
                numberGrid.removeAllViews()
                numbers.clear()
                selectedButtons.clear()

                // Ustaw layout na bieżącą orientację po restarcie
                updateLayoutForOrientation()

                // Ukryj elementy gry przed startem odliczania
                targetNumberText.visibility = View.GONE
                numberGrid.visibility = View.GONE

                countdownManager.startCountdown() // Rozpoczyna odliczanie początkowe
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
                    categoryKey = GameKeys.CATEGORY_REASONING,
                    gameKey = GameKeys.GAME_NUMBER_ADDITION,
                    starsEarned = starManager.starCount,
                    accuracy = gameStatsManager.calculateAccuracy(),
                    reactionTime = getAverageReactionTime(stars = starManager.starCount),
                )
                lastPlayedGame(GameKeys.CATEGORY_REASONING, GameKeys.GAME_NUMBER_ADDITION, getString(R.string.number_addition))
                finish() },
            instructionTitle = getString(R.string.instructions),
            instructionMessage = getString(R.string.number_addition_instruction),
        )

        // Ukryj elementy gry na starcie
        targetNumberText.visibility = View.GONE
        numberGrid.visibility = View.GONE

        // Sprawdzenie, czy gra jest uruchamiana po raz pierwszy
        if (savedInstanceState == null) {
            countdownManager.startCountdown() // Rozpoczyna odliczanie początkowe
        } else {
            restoreGameState(savedInstanceState) // Przywraca stan gry
        }

        // Ustaw layout na startową orientację
        updateLayoutForOrientation()
    }

    // Dostosowuje layout do bieżącej orientacji
    private fun updateLayoutForOrientation() {
        val currentConfig = resources.configuration
        val constraintLayout = findViewById<ConstraintLayout>(R.id.rootLayout)
        val constraintSet = ConstraintSet()
        constraintSet.clone(constraintLayout)

        if (currentConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // poziomo: liczba z lewej, plansza z prawej
            constraintSet.clear(R.id.numberGrid, ConstraintSet.TOP)
            constraintSet.clear(R.id.targetNumberText, ConstraintSet.BOTTOM)

            constraintSet.connect(R.id.targetNumberText, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
            constraintSet.connect(R.id.targetNumberText, ConstraintSet.END, R.id.numberGrid, ConstraintSet.START)
            constraintSet.connect(R.id.targetNumberText, ConstraintSet.TOP, R.id.gameTimerProgressBar, ConstraintSet.BOTTOM)
            constraintSet.connect(R.id.targetNumberText, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)

            constraintSet.connect(R.id.numberGrid, ConstraintSet.START, R.id.targetNumberText, ConstraintSet.END)
            constraintSet.connect(R.id.numberGrid, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
            constraintSet.connect(R.id.numberGrid, ConstraintSet.TOP, R.id.gameTimerProgressBar, ConstraintSet.BOTTOM)
            constraintSet.connect(R.id.numberGrid, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)

        } else {
            // pionowo: liczba nad planszą
            constraintSet.clear(R.id.numberGrid, ConstraintSet.START)
            constraintSet.clear(R.id.targetNumberText, ConstraintSet.END)

            constraintSet.connect(R.id.targetNumberText, ConstraintSet.TOP, R.id.gameTimerProgressBar, ConstraintSet.BOTTOM)
            constraintSet.connect(R.id.targetNumberText, ConstraintSet.BOTTOM, R.id.numberGrid, ConstraintSet.TOP)
            constraintSet.connect(R.id.targetNumberText, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
            constraintSet.connect(R.id.targetNumberText, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)

            constraintSet.connect(R.id.numberGrid, ConstraintSet.TOP, R.id.targetNumberText, ConstraintSet.BOTTOM)
            constraintSet.connect(R.id.numberGrid, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
            constraintSet.connect(R.id.numberGrid, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
            constraintSet.connect(R.id.numberGrid, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
        }

        constraintSet.applyTo(constraintLayout)
        constraintLayout.requestLayout()
    }

    // Zapisuje stan gry, gdy aktywność jest pauzowana lub niszczona
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("level", level)
        outState.putIntegerArrayList("numbers", ArrayList(numbers))
        outState.putString("targetNumber", targetNumberText.text.toString())
        outState.putInt("pauseOverlayVisibility", pauseOverlay.visibility)
        outState.putInt("countdownTextVisibility", countdownText.visibility)
        outState.putInt("numberGridVisibility", numberGrid.visibility)
        outState.putInt("pauseButtonVisibility", pauseButton.visibility)
        outState.putInt("countdownIndex", countdownManager.getIndex())
        outState.putBoolean("countdownInProgress", countdownManager.isInProgress())
        outState.putLong("timerRemainingTimeMs", timerProgressBar.getRemainingTimeSeconds() * 1000L)
        outState.putBoolean("timerIsRunning", timerProgressBar.isRunning())
        outState.putBoolean("isGameActive", isGameActive)
        outState.putIntegerArrayList("selectedIndices", ArrayList(selectedButtons.mapNotNull { it -> numberGrid.indexOfChild(it).takeIf { it >= 0 } }))
        outState.putBoolean("isShowingError", isShowingError)
        starManager.saveState(outState)

        saveGameStats(outState)
    }

    override fun onDestroy() {
        super.onDestroy()
        timerProgressBar.stop()
        countdownManager.cancel()
    }

    override fun onPause() {
        super.onPause()
        if (!isGameEnding && !pauseMenu.isPaused && !isChangingConfigurations) {
            pauseMenu.pause()
        }
    }

    // Rozpoczyna nowy poziom gry
    private fun startLevel() {
        gameStatsManager.startReactionTracking()
        isGameActive = true
        targetNumberText.background = AppCompatResources.getDrawable(this, R.drawable.circle_bg)
        numberGrid.isEnabled = true

        val (cols, rows) = getGridDimensions(level)
        numberGrid.columnCount = cols
        numberGrid.rowCount = rows

        generateNumbers()
        generateTarget()
        setupNumberGrid()

        showLevelInstruction()
    }

    // Generuje nowe liczby na siatce w zależności od poziomu
    private fun generateNumbers() {
        numbers.clear()
        val gridSize = getGridSize(level)
        repeat(gridSize) {
            numbers.add((1..9).random())
        }
    }

    // Generuje docelową sumę na podstawie możliwych kombinacji liczb
    private fun generateTarget(): Boolean {
        val availableNumbers = numbers.filter { it != -1 }
        if (availableNumbers.size < numbersToSelect()) return false

        val possibleSums = mutableListOf<Int>()

        when (numbersToSelect()) {
            2 -> {
                for (i in availableNumbers.indices) {
                    for (j in i + 1 until availableNumbers.size) {
                        possibleSums.add(availableNumbers[i] + availableNumbers[j])
                    }
                }
            }
            3 -> {
                for (i in availableNumbers.indices) {
                    for (j in i + 1 until availableNumbers.size) {
                        for (k in j + 1 until availableNumbers.size) {
                            possibleSums.add(
                                availableNumbers[i] + availableNumbers[j] + availableNumbers[k]
                            )
                        }
                    }
                }
            }
            4 -> {
                for (i in availableNumbers.indices) {
                    for (j in i + 1 until availableNumbers.size) {
                        for (k in j + 1 until availableNumbers.size) {
                            for (l in k + 1 until availableNumbers.size) {
                                possibleSums.add(
                                    availableNumbers[i] + availableNumbers[j] + availableNumbers[k] + availableNumbers[l]
                                )
                            }
                        }
                    }
                }
            }
        }

        if (possibleSums.isEmpty()) return false

        targetNumberText.text = possibleSums.random().toString()
        return true
    }

    // Tworzy siatkę przycisków z liczbami – wywoływane tylko przy starcie poziomu
    private fun setupNumberGrid() {
        if (numbers.isEmpty()) return
        numberGrid.removeAllViews()
        val (cols, rows) = getGridDimensions(level)
        numberGrid.columnCount = cols
        numberGrid.rowCount = rows

        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels
        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val margin = if (isLandscape) {
            if (rows > 4) 16 * 2 else 32 * 2
        } else 16 * 2
        val buttonWidth = (screenWidth / cols) - margin
        val buttonHeight = (screenHeight / rows) - margin
        val buttonSize = minOf(buttonWidth, buttonHeight)

        val horizontalMargin = if (isLandscape) 12 else 8
        val verticalMargin = if (isLandscape && rows > 4) 4 else if (isLandscape) 12 else 8
        val textSizeMultiplier = if (isLandscape) {
            if (rows > 4) 0.28f else 0.26f
        } else 0.28f

        for (i in numbers.indices) {
            val button = MaterialButton(this).apply {
                layoutParams = GridLayout.LayoutParams().apply {
                    width = buttonSize
                    height = buttonSize
                    setMargins(horizontalMargin, verticalMargin, horizontalMargin, verticalMargin)
                }
                textSize = buttonSize * textSizeMultiplier / resources.displayMetrics.density
                setPadding(0, 0, 0, 0)
                shapeAppearanceModel = shapeAppearanceModel
                    .toBuilder()
                    .setAllCorners(CornerFamily.ROUNDED, buttonSize * 0.15f)
                    .build()
            }

            if (numbers[i] == -1) {
                button.isEnabled = false
                button.text = ""
                button.setTextColor(Color.DKGRAY)
                button.setBackgroundColor(Color.LTGRAY)
            } else {
                button.text = numbers[i].toString()
                button.setTextColor(Color.BLACK)
                button.setBackgroundColor(Color.WHITE)
                button.isEnabled = true
                button.setOnClickListener { handleNumberClick(button, i) }
            }

            numberGrid.addView(button)
        }
    }

    // Obsługuje kliknięcie przycisku z liczbą
    private fun handleNumberClick(button: MaterialButton, index: Int) {
        if (pauseMenu.isPaused || !numberGrid.isEnabled) return // Ignoruj kliknięcia podczas pauzy
        if (isShowingError) return // Zablokuj podczas błędu

        if (selectedButtons.contains(button)) {
            selectedButtons.remove(button)
            button.setBackgroundColor(Color.WHITE)
            return
        }

        if (selectedButtons.size < numbersToSelect() && button.isEnabled && numbers[index] != -1) {
            selectedButtons.add(button)
            button.setBackgroundColor(Color.rgb(106, 27, 154))
        }

        if (selectedButtons.size == numbersToSelect()) {
            val indices = selectedButtons.map { numberGrid.indexOfChild(it) }
            val sum = indices.sumOf { numbers[it] }
            val target = targetNumberText.text.toString().toIntOrNull() ?: 0

            if (sum == target) {
                selectedButtons.forEach { btn ->
                    val idx = numberGrid.indexOfChild(btn)
                    numbers[idx] = -1
                    btn.text = ""
                    btn.isEnabled = false
                    btn.setBackgroundColor(Color.LTGRAY)
                    btn.setOnClickListener(null)
                }
                //poprawna próba
                gameStatsManager.registerAttempt(true)
                starManager.increment()
                selectedButtons.clear()

                if (!generateTarget()) {
                    proceedToNextLevel()
                    return
                }
            } else {
                gameStatsManager.registerAttempt(false)
                selectedButtons.forEach { btn -> btn.setBackgroundColor(Color.RED) }
                isShowingError = true
                runDelayed {
                    resetError() // Reset po opóźnieniu
                }
            }
        }
    }

    // Zwraca liczbę liczb do wybrania w zależności od poziomu
    private fun numbersToSelect(): Int {
        return when {
            level < 4 -> 2
            level < 7 -> 3
            else -> 4
        }
    }

    // Pokazuje Toast z instrukcją
    private fun showLevelInstruction() {
        val toastMessage = when (level) {
            1 -> "Wybierz 2 liczby, aby suma dała cel!"
            4 -> "Wybierz 3 liczby, aby suma dała cel!"
            7 -> "Wybierz 4 liczby, aby suma dała cel!"
            else -> null
        }
        toastMessage?.let {
            Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
        }
    }

    // Przechodzi do następnego poziomu
    private fun proceedToNextLevel() {

        isGameActive = true
        if (timerProgressBar.getRemainingTimeSeconds() > 0) {
            timerProgressBar.addTime(20)  // Dodaj 20s bonusu za level
            level = (level + 1).coerceAtMost(9)
            generateNumbers()
            generateTarget()
            setupNumberGrid()
            showLevelInstruction()
        } else {
            endGame()
        }
    }

    // Kończy grę
    private fun endGame() {
        isGameActive = false
        isGameEnding = true
        Toast.makeText(this, "Koniec gry!", Toast.LENGTH_LONG).show()
        numberGrid.isEnabled = false
        pauseOverlay.visibility = View.GONE
        updateUserStatistics(
            categoryKey = GameKeys.CATEGORY_REASONING,
            gameKey = GameKeys.GAME_NUMBER_ADDITION,
            starsEarned = starManager.starCount,
            accuracy = gameStatsManager.calculateAccuracy(),
            reactionTime = getAverageReactionTime(stars = starManager.starCount),
        )
        lastPlayedGame(GameKeys.CATEGORY_REASONING, GameKeys.GAME_NUMBER_ADDITION, getString(R.string.number_addition))
        finish()
    }

    // Zwraca wymiary gridu (cols, rows) dla levelu
    private fun getGridDimensions(level: Int): Pair<Int, Int> = when (level) {
        1, 4, 7 -> 4 to 3   // 4x3
        2, 5, 8 -> 4 to 4   // 4x4
        3, 6, 9 -> 4 to 5   // 4x5
        else -> 4 to 5
    }

    // Zwraca rozmiar gridu (liczba pól) dla levelu
    private fun getGridSize(level: Int): Int = when (level) {
        1, 4, 7 -> 12
        2, 5, 8 -> 16
        3, 6, 9 -> 20
        else -> 20
    }

    // Uruchamia akcję z opóźnieniem, uwzględniając pauzę – jeśli gra jest wstrzymana, akcja zostanie wykonana po wznowieniu
    private fun runDelayed(action: () -> Unit) {
        var remaining = 1000L
        val handler = Handler(Looper.getMainLooper())
        val interval = 16L // ~60fps, aby odliczanie było płynne

        val runnable = object : Runnable {
            override fun run() {
                if (pauseMenu.isPaused) {
                    // Gra w pauzie – nie zmniejszamy remaining, czekamy do wznowienia
                    handler.postDelayed(this, interval)
                    return
                }

                remaining -= interval
                if (remaining <= 0) {
                    action() // Wykonanie akcji po upłynięciu czasu
                } else {
                    handler.postDelayed(this, interval) // Kolejna iteracja
                }
            }
        }
        handler.postDelayed(runnable, interval)
    }

    // Resetuje błędne zaznaczenia (biały kolor + clear)
    private fun resetError() {
        selectedButtons.forEach { btn -> btn.setBackgroundColor(Color.WHITE) }
        selectedButtons.clear()
        isShowingError = false  // Odblokuj po animacji
    }

    // Przywraca stan gry z zapisanego Bundle
    private fun restoreGameState(savedInstanceState: Bundle) {
        isGameActive = savedInstanceState.getBoolean("isGameActive", false)
        level = savedInstanceState.getInt("level", 1)
        numbers.clear()
        numbers.addAll(savedInstanceState.getIntegerArrayList("numbers") ?: mutableListOf())
        val target = savedInstanceState.getString("targetNumber", "")
        pauseOverlay.visibility = savedInstanceState.getInt("pauseOverlayVisibility")
        countdownText.visibility = savedInstanceState.getInt("countdownTextVisibility")
        numberGrid.visibility = savedInstanceState.getInt("numberGridVisibility")
        pauseButton.visibility = savedInstanceState.getInt("pauseButtonVisibility")
        starManager.restoreState(savedInstanceState)

        val countdownIndex = savedInstanceState.getInt("countdownIndex", 0)
        val countdownInProgress = savedInstanceState.getBoolean("countdownInProgress", false)

        // Przywracanie stanu timera
        val timerRemainingTimeMs = savedInstanceState.getLong("timerRemainingTimeMs", 90 * 1000L)
        val timerIsRunning = savedInstanceState.getBoolean("timerIsRunning", false)

        timerProgressBar.setRemainingTimeMs(timerRemainingTimeMs.coerceAtLeast(1L))

        if (timerIsRunning && pauseOverlay.visibility != View.VISIBLE) {
            timerProgressBar.start()
        }

        // Odtwarzanie stanu gry
        if (!countdownInProgress) {
            val (cols, rows) = getGridDimensions(level)
            numberGrid.columnCount = cols
            numberGrid.rowCount = rows
            targetNumberText.text = target
            targetNumberText.background = AppCompatResources.getDrawable(this, R.drawable.circle_bg)
            setupNumberGrid()
            showLevelInstruction()

            // Odtwórz zaznaczone przyciski na podstawie zapisanych indeksów
            val savedSelectedIndices = savedInstanceState.getIntegerArrayList("selectedIndices") ?: ArrayList()
            selectedButtons.clear()
            savedSelectedIndices.forEach { index ->
                if (index >= 0 && index < numberGrid.childCount) {
                    val btn = numberGrid.getChildAt(index) as? MaterialButton
                    if (btn != null && btn.isEnabled && numbers[index] != -1) {  // Sprawdź, czy przycisk jest aktywny
                        selectedButtons.add(btn)
                        btn.setBackgroundColor(Color.rgb(106, 27, 154))  // Przywróć kolor zaznaczenia (fioletowy)
                    }
                }
            }

            // Odtwórz stan błędu po restore, jeśli był aktywny
            isShowingError = savedInstanceState.getBoolean("isShowingError", false)
            if (isShowingError && selectedButtons.isNotEmpty()) {
                selectedButtons.forEach { it.setBackgroundColor(Color.RED) }
                runDelayed { resetError() }  // Kontynuuj błąd po restore
            }

            numberGrid.isEnabled = true
            targetNumberText.visibility = View.VISIBLE
            numberGrid.visibility = View.VISIBLE
            isGameActive = true
            updateLayoutForOrientation()
        } else {
            isGameActive = false
            countdownManager.startCountdown(countdownIndex)
        }

        pauseMenu.syncWithOverlay()
        restoreGameStats(savedInstanceState)
    }
}