package com.example.logicmind.activities

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.gridlayout.widget.GridLayout
import com.example.logicmind.R
import com.example.logicmind.common.GameCountdownManager
import com.example.logicmind.common.GameTimerProgressBar
import com.example.logicmind.common.PauseMenu
import com.example.logicmind.common.StarManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.shape.CornerFamily
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import android.content.res.Configuration
import androidx.core.view.isVisible

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
    private var isGameActive = false // Flaga, czy gra jest w trakcie

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
        timerProgressBar.setTotalTime(60) // Ustaw czas na 1 minutę
        timerProgressBar.setOnFinishCallback {
            runOnUiThread {
                isGameEnding = true
                Toast.makeText(this, "Czas minął! Koniec gry!", Toast.LENGTH_LONG).show()
                numberGrid.isEnabled = false
                pauseOverlay.visibility = View.GONE
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
                timerProgressBar.start()
                targetNumberText.visibility = View.VISIBLE
                numberGrid.visibility = View.VISIBLE
                isGameActive = true
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
                timerProgressBar.reset() // Resetuje timer

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
                timerProgressBar.start()
            },
            onPause = {
                timerProgressBar.pause()
            },
            onExit = { finish() }, // Kończy aktywność
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
            // liczba z lewej, plansza z prawej
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
        starManager.saveState(outState)
    }

    override fun onDestroy() {
        super.onDestroy()
        timerProgressBar.cancel() // Zatrzymaj CountDownTimer
        countdownManager.cancel() // Usuń handlery odliczania
    }

    override fun onPause() {
        super.onPause()
        if (!isGameEnding && !pauseMenu.isPaused && !isChangingConfigurations) {
            pauseMenu.pause()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        updateLayoutForOrientation()

        // Przelicz siatkę przycisków, jeśli gra jest aktywna (dopasuj do nowej orientacji)
        if (isGameActive && numberGrid.isVisible) {
            setupNumberGrid()
        }
    }

    // Rozpoczyna nowy poziom gry
    private fun startLevel() {
        isGameActive = true
        targetNumberText.background = AppCompatResources.getDrawable(this, R.drawable.circle_bg)
        numberGrid.isEnabled = true

        when (level) {
            1 -> {
                numberGrid.columnCount = 4
                numberGrid.rowCount = 3
            }
            2 -> {
                numberGrid.columnCount = 4
                numberGrid.rowCount = 4
            }
            else -> {
                numberGrid.columnCount = 4
                numberGrid.rowCount = 3
            }
        }

        generateNumbers()
        generateTarget()
        setupNumberGrid()
    }

    // Generuje nowe liczby na siatce w zależności od poziomu
    private fun generateNumbers() {
        numbers.clear()
        val gridSize = when (level) {
            1 -> 12   // 4x3
            2 -> 16   // 4x4
            else -> 12 // 4x3
        }
        repeat(gridSize) {
            numbers.add((1..9).random())
        }
    }

    // Generuje docelową sumę na podstawie możliwych kombinacji liczb
    private fun generateTarget(): Boolean {
        val availableNumbers = numbers.filter { it != -1 }
        if (availableNumbers.size < numbersToSelect()) return false

        val possibleSums = mutableListOf<Int>()

        if (numbersToSelect() == 2) {
            for (i in availableNumbers.indices) {
                for (j in i + 1 until availableNumbers.size) {
                    possibleSums.add(availableNumbers[i] + availableNumbers[j])
                }
            }
        } else { // 3 liczby
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

        if (possibleSums.isEmpty()) return false

        targetNumberText.text = possibleSums.random().toString()
        return true
    }

    // Tworzy siatkę przycisków z liczbami – wywoływane tylko przy starcie poziomu
    private fun setupNumberGrid() {
        if (numbers.isEmpty()) return
        numberGrid.removeAllViews()
        val (cols, rows) = when (level) {
            1 -> 4 to 3
            2 -> 4 to 4
            else -> 4 to 3
        }
        numberGrid.columnCount = cols
        numberGrid.rowCount = rows

        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels
        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val margin = if (isLandscape) 32 * 2 else 16 * 2
        val buttonWidth = (screenWidth / cols) - margin
        val buttonHeight = (screenHeight / rows) - margin
        val buttonSize = minOf(buttonWidth, buttonHeight)

        val buttonMargin = if (isLandscape) 12 else 8
        val textSizeMultiplier = if (isLandscape) 0.22f else 0.28f

        for (i in numbers.indices) {
            val button = MaterialButton(this).apply {
                layoutParams = GridLayout.LayoutParams().apply {
                    width = buttonSize
                    height = buttonSize
                    setMargins(buttonMargin, buttonMargin, buttonMargin, buttonMargin)
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
                starManager.increment()
                selectedButtons.clear()

                if (!generateTarget()) {
                    proceedToNextLevel()
                    return
                }
            } else {
                selectedButtons.forEach { btn -> btn.setBackgroundColor(Color.RED) }
                Handler(Looper.getMainLooper()).postDelayed({
                    selectedButtons.forEach { btn -> btn.setBackgroundColor(Color.WHITE) }
                    selectedButtons.clear()
                }, 1000)
            }
        }
    }

    // Zwraca liczbę liczb do wybrania w zależności od poziomu
    private fun numbersToSelect(): Int {
        return if (level < 3) 2 else 3
    }

    // Przechodzi do następnego poziomu
    private fun proceedToNextLevel() {
        isGameActive = true
        if (timerProgressBar.getRemainingTimeSeconds() > 0) {
            level = when (level) {
                1 -> 2
                2 -> 3
                else -> 3
            }
            generateNumbers()
            generateTarget()
            setupNumberGrid()
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
        finish()
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
        val timerRemainingTimeMs = savedInstanceState.getLong("timerRemainingTimeMs", 60 * 1000L)
        val timerIsRunning = savedInstanceState.getBoolean("timerIsRunning", false)

        timerProgressBar.setRemainingTimeMs(timerRemainingTimeMs.coerceAtLeast(1L))

        if (timerIsRunning && pauseOverlay.visibility != View.VISIBLE) {
            timerProgressBar.start()
        }

        // Odtwarzanie stanu gry
        if (!countdownInProgress) {
            when (level) {
                1 -> {
                    numberGrid.columnCount = 4
                    numberGrid.rowCount = 3
                }
                2 -> {
                    numberGrid.columnCount = 4
                    numberGrid.rowCount = 4
                }
                else -> {
                    numberGrid.columnCount = 4
                    numberGrid.rowCount = 3
                }
            }
            targetNumberText.text = target
            targetNumberText.background = AppCompatResources.getDrawable(this, R.drawable.circle_bg)
            setupNumberGrid()
            numberGrid.isEnabled = true
            targetNumberText.visibility = View.VISIBLE
            numberGrid.visibility = View.VISIBLE
            isGameActive = true
        } else {
            isGameActive = false
            countdownManager.startCountdown(countdownIndex)
        }

        pauseMenu.syncWithOverlay()
        selectedButtons.clear()
    }
}