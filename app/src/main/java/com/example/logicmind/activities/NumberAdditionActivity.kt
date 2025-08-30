package com.example.logicmind.activities

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.gridlayout.widget.GridLayout
import com.example.logicmind.R
import com.example.logicmind.common.GameCountdownManager
import com.example.logicmind.common.GameTimerProgressBar
import com.google.android.material.button.MaterialButton
import com.google.android.material.shape.CornerFamily

class NumberAdditionActivity : AppCompatActivity() {

    private lateinit var targetNumberText: TextView
    private lateinit var numberGrid: GridLayout
    private lateinit var timerProgressBar: GameTimerProgressBar
    private lateinit var pauseButton: View
    private lateinit var pauseOverlay: View
    private lateinit var countdownText: TextView
    private lateinit var countdownManager: GameCountdownManager
    private lateinit var starCountText: TextView

    private val numbers = mutableListOf<Int>()
    private val selectedButtons = mutableListOf<Button>()
    private var level = 1
    private var remainingTime: Long = 60_000
    private var starCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_number_addition)
        supportActionBar?.hide()

        // 🔹 Inicjalizacja widoków
        targetNumberText = findViewById(R.id.targetNumberText)
        numberGrid = findViewById(R.id.numberGrid)
        timerProgressBar = findViewById(R.id.gameTimerProgressBar)
        pauseButton = findViewById(R.id.pauseButton)
        pauseOverlay = findViewById(R.id.pauseOverlay)
        countdownText = findViewById(R.id.countdownText)
        starCountText = findViewById(R.id.starCountText)
        //gameIntro = findViewById(R.id.gameIntro)

        targetNumberText.visibility = View.GONE
        numberGrid.visibility = View.GONE
        updateStarCountUI()

        // Timer gry
        timerProgressBar.setTotalTime(60)
        timerProgressBar.setOnFinishCallback {
            runOnUiThread {
                Toast.makeText(this, "Czas minął! Koniec gry!", Toast.LENGTH_LONG).show()
                endGame()
            }
        }

        // Odliczanie startowe
        countdownManager = GameCountdownManager(
            countdownText = countdownText,
            gameView = numberGrid,
            pauseButton = pauseButton,
            onCountdownFinished = {
                starCount = 0
                updateStarCountUI()
                timerProgressBar.start()
                targetNumberText.visibility = View.VISIBLE
                numberGrid.visibility = View.VISIBLE
                startLevel()
            }
        )

        // Pauza
        pauseButton.setOnClickListener {
            pauseOverlay.visibility = View.VISIBLE
            timerProgressBar.pause()
            numberGrid.isEnabled = false
        }

        setupPauseMenu()

        if (savedInstanceState == null) {
            countdownManager.startCountdown()
        } else {
            restoreGameState(savedInstanceState)
        }
    }

    private fun setupPauseMenu() {
        pauseOverlay.findViewById<Button>(R.id.btnResume).setOnClickListener {
            pauseOverlay.visibility = View.GONE
            timerProgressBar.start()
            numberGrid.isEnabled = true
        }
        pauseOverlay.findViewById<Button>(R.id.btnRestart).setOnClickListener {
            pauseOverlay.visibility = View.GONE
            starCount = 0
            updateStarCountUI()
            countdownManager.startCountdown()
        }
        pauseOverlay.findViewById<Button>(R.id.btnExit).setOnClickListener {
            finish()
        }
    }

    private fun startLevel() {
        targetNumberText.background = getDrawable(R.drawable.circle_bg)
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


    private fun setupNumberGrid() {
        numberGrid.removeAllViews()
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        val (cols, rows) = when (level) {
            1 -> 4 to 3
            2 -> 4 to 4
            else -> 4 to 3
        }

        val margin = 16 * 2
        val buttonWidth = (screenWidth / cols) - margin
        val buttonHeight = (screenHeight / rows) - margin
        val buttonSize = minOf(buttonWidth, buttonHeight)

        numberGrid.columnCount = cols
        numberGrid.rowCount = rows

        for (i in numbers.indices) {
            val button = MaterialButton(this).apply {
                text = if (numbers[i] == -1) "" else numbers[i].toString()
                layoutParams = GridLayout.LayoutParams().apply {
                    width = buttonSize
                    height = buttonSize
                    setMargins(12, 12, 12, 12)
                }
                textSize = (buttonSize * 0.3f) / resources.displayMetrics.scaledDensity
                setTextColor(Color.BLACK)
                shapeAppearanceModel = shapeAppearanceModel
                    .toBuilder()
                    .setAllCorners(CornerFamily.ROUNDED, buttonSize * 0.15f)
                    .build()
            }

            if (numbers[i] == -1) {
                button.isEnabled = false
                button.backgroundTintList = ColorStateList.valueOf(Color.LTGRAY)
                button.setTextColor(Color.DKGRAY)
            } else {
                button.setOnClickListener { handleNumberClick(button, i) }
                button.backgroundTintList = ColorStateList.valueOf(Color.WHITE)
                button.setTextColor(Color.BLACK)
            }

            numberGrid.addView(button)
        }
    }


    private fun handleNumberClick(button: Button, index: Int) {
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
                }
                starCount += 1
                updateStarCountUI()
                selectedButtons.clear()

                if (!generateTarget()) {
                    proceedToNextLevel()
                    return
                }
                setupNumberGrid()
            } else {
                selectedButtons.forEach { btn -> btn.setBackgroundColor(Color.RED) }
                Handler(Looper.getMainLooper()).postDelayed({
                    selectedButtons.forEach { btn -> btn.setBackgroundColor(Color.WHITE) }
                    selectedButtons.clear()
                }, 1000)
            }
        }

    }

    private fun updateStarCountUI() {
        starCountText.text = starCount.toString()
    }

    private fun numbersToSelect(): Int {
        return if (level < 3) 2 else 3
    }


    private fun proceedToNextLevel() {
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


    private fun endGame() {
        targetNumberText.background = null
        targetNumberText.text = "Koniec!"
        numberGrid.removeAllViews()

        val message = TextView(this)
        message.text = "Gratulacje! Twój wynik: $starCount"
        message.textSize = 24f
        message.layoutParams = GridLayout.LayoutParams().apply {
            width = GridLayout.LayoutParams.WRAP_CONTENT
            height = GridLayout.LayoutParams.WRAP_CONTENT
            setMargins(8, 8, 8, 8)
        }
        numberGrid.addView(message)
        timerProgressBar.pause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("level", level)
        outState.putIntegerArrayList("numbers", ArrayList(numbers))
        outState.putInt("remainingTime", timerProgressBar.getRemainingTimeSeconds())
        outState.putBoolean("countdownInProgress", countdownManager.isInProgress())
        outState.putInt("countdownIndex", countdownManager.getIndex())
        outState.putInt("starCount", starCount)
    }

    private fun restoreGameState(savedInstanceState: Bundle) {
        level = savedInstanceState.getInt("level", 1)
        numbers.clear()
        numbers.addAll(savedInstanceState.getIntegerArrayList("numbers") ?: mutableListOf())
        remainingTime = savedInstanceState.getLong("remainingTime", 60)
        starCount = savedInstanceState.getInt("starCount", 0)
        updateStarCountUI()

        timerProgressBar.setTotalTime((remainingTime / 1000).toInt())

        val countdownInProgress = savedInstanceState.getBoolean("countdownInProgress", false)
        val countdownIndex = savedInstanceState.getInt("countdownIndex", 0)
        if (countdownInProgress) {
            countdownManager.startCountdown(countdownIndex)
        } else {
            startLevel()
            timerProgressBar.start()
        }
    }
}
