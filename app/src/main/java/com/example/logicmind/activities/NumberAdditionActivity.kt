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
    private lateinit var targetNumberText: TextView
    private lateinit var numberGrid: GridLayout
    private lateinit var countdownText: TextView
    private lateinit var pauseButton: ImageView
    private lateinit var pauseOverlay: View
    private lateinit var countdownManager: GameCountdownManager
    private lateinit var timerProgressBar: GameTimerProgressBar
    private lateinit var starManager: StarManager
    private lateinit var pauseMenu: PauseMenu
    private val numbers = mutableListOf<Int>()
    private val selectedButtons = mutableListOf<MaterialButton>()
    private var currentLevel = 1
    private var isGameEnding = false
    private var isGameActive = false
    private var isShowingError = false
    private var currentBestScore = 0

    companion object {
        private const val BASE_TIME_SECONDS = 90
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_number_addition)

        supportActionBar?.hide()

        targetNumberText = findViewById(R.id.targetNumberText)
        numberGrid = findViewById(R.id.numberGrid)
        countdownText = findViewById(R.id.countdownText)
        pauseButton = findViewById(R.id.pauseButton)
        pauseOverlay = findViewById(R.id.pauseOverlay)
        timerProgressBar = findViewById(R.id.gameTimerProgressBar)
        starManager = StarManager()
        starManager.init(findViewById(R.id.starCountText))

        if (isUserLoggedIn()) {
            val uid = auth.currentUser!!.uid
            db.getReference("users").child(uid).child("categories")
                .child(GameKeys.CATEGORY_REASONING).child(GameKeys.GAME_NUMBER_ADDITION)
                .child("bestStars").get().addOnSuccessListener { snapshot ->
                    currentBestScore = snapshot.getValue(Int::class.java) ?: 0
                }
        }

        timerProgressBar.setTotalTime(BASE_TIME_SECONDS)
        timerProgressBar.setOnFinishCallback {
            runOnUiThread {
                handleGameOver()
            }
        }

        countdownManager = GameCountdownManager(
            countdownText = countdownText,
            gameView = numberGrid,
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
                targetNumberText.visibility = View.VISIBLE
                numberGrid.visibility = View.VISIBLE
                isGameActive = true

                gameStatsManager.startReactionTracking()
                gameStatsManager.setGameStartTime()

                startLevel()
            }
        )

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
                numberGrid.removeAllViews()
                numbers.clear()
                selectedButtons.clear()

                updateLayoutForOrientation()

                targetNumberText.visibility = View.GONE
                numberGrid.visibility = View.GONE

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
                handleGameOver()
            },
            instructionTitle = getString(R.string.instructions),
            instructionMessage = getString(R.string.number_addition_instruction),
        )

        targetNumberText.visibility = View.GONE
        numberGrid.visibility = View.GONE

        if (savedInstanceState == null) {
            countdownManager.startCountdown()
        } else {
            restoreGameState(savedInstanceState)
        }
        updateLayoutForOrientation()
    }

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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("currentLevel", currentLevel)
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

    private fun startLevel() {
        gameStatsManager.startReactionTracking()
        isGameActive = true
        targetNumberText.background = AppCompatResources.getDrawable(this, R.drawable.circle_bg)
        numberGrid.isEnabled = true

        val (cols, rows) = getGridDimensions(currentLevel)
        numberGrid.columnCount = cols
        numberGrid.rowCount = rows

        generateNumbers()
        generateTarget()
        setupNumberGrid()

        showLevelInstruction()
    }

    private fun generateNumbers() {
        numbers.clear()
        val gridSize = getGridSize(currentLevel)
        repeat(gridSize) {
            numbers.add((1..9).random())
        }
    }

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

    private fun setupNumberGrid() {
        if (numbers.isEmpty()) return
        numberGrid.removeAllViews()
        val (cols, rows) = getGridDimensions(currentLevel)
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

    private fun handleNumberClick(button: MaterialButton, index: Int) {
        if (pauseMenu.isPaused || !numberGrid.isEnabled) return
        if (isShowingError) return

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
                    resetError()
                }
            }
        }
    }

    private fun numbersToSelect(): Int {
        return when {
            currentLevel < 4 -> 2
            currentLevel < 7 -> 3
            else -> 4
        }
    }

    private fun showLevelInstruction() {
        if (currentLevel == 1 || currentLevel == 4 || currentLevel == 7) {
            val count = numbersToSelect()
            val msg = getString(R.string.instruction_pick_n_numbers, count)
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }
    }

    private fun proceedToNextLevel() {
        isGameActive = true
        if (timerProgressBar.getRemainingTimeSeconds() > 0) {
            timerProgressBar.addTime(20)
            currentLevel = (currentLevel + 1).coerceAtMost(9)
            generateNumbers()
            generateTarget()
            setupNumberGrid()
            showLevelInstruction()
        } else {
            handleGameOver()
        }
    }

    private fun getGridDimensions(currentLevel: Int): Pair<Int, Int> = when (currentLevel) {
        1, 4, 7 -> 4 to 3
        2, 5, 8 -> 4 to 4
        3, 6, 9 -> 4 to 5
        else -> 4 to 5
    }

    private fun getGridSize(currentLevel: Int): Int = when (currentLevel) {
        1, 4, 7 -> 12
        2, 5, 8 -> 16
        3, 6, 9 -> 20
        else -> 20
    }

    private fun runDelayed(action: () -> Unit) {
        var remaining = 1000L
        val handler = Handler(Looper.getMainLooper())
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

    private fun resetError() {
        selectedButtons.forEach { btn -> btn.setBackgroundColor(Color.WHITE) }
        selectedButtons.clear()
        isShowingError = false
    }

    private fun restoreGameState(savedInstanceState: Bundle) {
        isGameActive = savedInstanceState.getBoolean("isGameActive", false)
        currentLevel = savedInstanceState.getInt("currentLevel", 1)
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

        val timerRemainingTimeMs = savedInstanceState.getLong("timerRemainingTimeMs", BASE_TIME_SECONDS * 1000L)
        val timerIsRunning = savedInstanceState.getBoolean("timerIsRunning", false)

        timerProgressBar.setRemainingTimeMs(timerRemainingTimeMs.coerceAtLeast(1L))

        if (timerIsRunning && pauseOverlay.visibility != View.VISIBLE) {
            timerProgressBar.start()
        }

        if (!countdownInProgress) {
            val (cols, rows) = getGridDimensions(currentLevel)
            numberGrid.columnCount = cols
            numberGrid.rowCount = rows
            targetNumberText.text = target
            targetNumberText.background = AppCompatResources.getDrawable(this, R.drawable.circle_bg)
            setupNumberGrid()
            showLevelInstruction()

            val savedSelectedIndices = savedInstanceState.getIntegerArrayList("selectedIndices") ?: ArrayList()
            selectedButtons.clear()
            savedSelectedIndices.forEach { index ->
                if (index >= 0 && index < numberGrid.childCount) {
                    val btn = numberGrid.getChildAt(index) as? MaterialButton
                    if (btn != null && btn.isEnabled && numbers[index] != -1) {
                        selectedButtons.add(btn)
                        btn.setBackgroundColor(Color.rgb(106, 27, 154))
                    }
                }
            }

            isShowingError = savedInstanceState.getBoolean("isShowingError", false)

            if (isShowingError && selectedButtons.isNotEmpty()) {
                selectedButtons.forEach { it.setBackgroundColor(Color.RED) }
                runDelayed { resetError() }
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

    private fun handleGameOver() {
        isGameActive = false
        isGameEnding = true
        numberGrid.isEnabled = false
        pauseOverlay.visibility = View.GONE

        showGameOverDialog(
            categoryKey = GameKeys.CATEGORY_REASONING,
            gameKey = GameKeys.GAME_NUMBER_ADDITION,
            starManager = starManager,
            timerProgressBar = timerProgressBar,
            countdownManager = countdownManager,
            currentBestScore = currentBestScore,
            onRestartAction = {
                if (starManager.starCount > currentBestScore) {
                    currentBestScore = starManager.starCount
                }
                currentLevel = 1
                numberGrid.removeAllViews()
                numbers.clear()
                selectedButtons.clear()
                targetNumberText.visibility = View.GONE
                numberGrid.visibility = View.GONE
                updateLayoutForOrientation()
            }
        )
    }

    override fun onPause() {
        super.onPause()
        if (!isGameEnding && !pauseMenu.isPaused && !isChangingConfigurations) {
            pauseMenu.pause()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        timerProgressBar.stop()
        countdownManager.cancel()
    }
}