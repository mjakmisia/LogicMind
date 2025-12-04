package com.example.logicmind.activities
import android.animation.ObjectAnimator
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.gridlayout.widget.GridLayout
import com.example.logicmind.R
import com.example.logicmind.common.GameCountdownManager
import com.example.logicmind.common.GameTimerProgressBar
import com.example.logicmind.common.PauseMenu
import com.example.logicmind.common.SoundManager
import com.example.logicmind.common.StarManager
import kotlin.random.Random

class ColorSequenceActivity : BaseActivity() {
    private var keyButtons: List<KeyButton> = emptyList()
    private var currentSequence = mutableListOf<Int>()
    private var userSequence = mutableListOf<Int>()
    private var sequenceDelayRemaining: Long = 0L
    private var pendingShowSequenceDelay: Long = 0L
    private var isShowingSequence = false
    private var isUserTurn = false
    private var isGameEnding = false
    private var currentLevel = 1
    private var numKeys = 0
    private var currentSeqLength = 0
    private var sequenceShowIndex = 0
    private var currentBestScore = 0
    private lateinit var gridLayout: GridLayout
    private lateinit var countdownText: TextView
    private lateinit var pauseButton: ImageButton
    private lateinit var pauseOverlay: ConstraintLayout
    private lateinit var timerProgressBar: GameTimerProgressBar
    private lateinit var starManager: StarManager
    private lateinit var pauseMenu: PauseMenu
    private lateinit var countdownManager: GameCountdownManager

    private val soundOrder4 = listOf(2, 5, 7, 1)              // C E G B
    private val soundOrder6 = listOf(2, 5, 6, 7, 1, 3)        // C E F G B C (wyższe C)
    private val soundOrder8 = listOf(2, 4, 5, 6, 7, 0, 1, 3)  // C D E F G A B C (wyższe C)
    private val soundResources = listOf(
        R.raw.key_sound_a,
        R.raw.key_sound_b,
        R.raw.key_sound_c,
        R.raw.key_sound_c_higher,
        R.raw.key_sound_d,
        R.raw.key_sound_e,
        R.raw.key_sound_f,
        R.raw.key_sound_g
    )

    private data class KeyButton(val view: Button, val index: Int)

    private data class SequenceConfig(
        val numKeys: Int,
        val startLength: Int,
        val step: Int,
        val maxLength: Int
    )

    companion object {
        const val BASE_TIME_SECONDS = 90
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_color_sequence)
        supportActionBar?.hide()

        SoundManager.init(this)

        gridLayout = findViewById(R.id.gridLayout)
        countdownText = findViewById(R.id.countdownText)
        pauseButton = findViewById(R.id.pauseButton)
        pauseOverlay = findViewById(R.id.pauseOverlay)
        timerProgressBar = findViewById(R.id.gameTimerProgressBar)
        starManager = StarManager()
        starManager.init(findViewById(R.id.starCountText))

        if (isUserLoggedIn()) {
            val uid = auth.currentUser!!.uid
            db.getReference("users").child(uid).child("categories")
                .child(GameKeys.CATEGORY_MEMORY).child(GameKeys.GAME_COLOR_SEQUENCE)
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
            gameView = gridLayout,
            viewsToHide = listOf(
                pauseButton,
                findViewById<TextView>(R.id.starCountText),
                findViewById<ImageView>(R.id.starIcon),
                timerProgressBar),
            onCountdownFinished = {
                currentLevel = 1
                starManager.reset()

                gameStatsManager.startReactionTracking()
                gameStatsManager.setGameStartTime()
                startNewGame()
            }
        )

        pauseMenu = PauseMenu(
            context = this,
            pauseOverlay = pauseOverlay,
            pauseButton = pauseButton,
            onRestart = {
                if (pauseMenu.isPaused) pauseMenu.resume()
                currentLevel = 1
                timerProgressBar.stop()
                timerProgressBar.reset()
                countdownManager.startCountdown()
            },
            onResume = {
                onGameResumed()
                if (!isShowingSequence && !isUserTurn) {
                    timerProgressBar.start()
                }
                if (isUserTurn) {
                    gridLayout.isEnabled = true
                    keyButtons.forEach { it.view.isEnabled = true }
                }
            },
            onPause = {
                onGamePaused()
                if (!isShowingSequence) {
                    timerProgressBar.pause()
                }
                gridLayout.isEnabled = false
                keyButtons.forEach { it.view.isEnabled = false }
            },
            onExit = {
                handleGameOver()
            },
            instructionTitle = getString(R.string.instructions),
            instructionMessage = getString(R.string.color_sequence_instruction),
        )

        if (savedInstanceState == null) {
            countdownManager.startCountdown()
        } else {
            restoreGameState(savedInstanceState)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("currentLevel", currentLevel)
        outState.putInt("numKeys", numKeys)
        outState.putInt("currentSeqLength", currentSeqLength)
        outState.putInt("sequenceShowIndex", sequenceShowIndex)
        outState.putBoolean("isShowingSequence", isShowingSequence)
        outState.putBoolean("isUserTurn", isUserTurn)
        outState.putIntArray("currentSequence", currentSequence.toIntArray())
        outState.putIntArray("userSequence", userSequence.toIntArray())
        outState.putInt("pauseOverlayVisibility", pauseOverlay.visibility)
        outState.putInt("countdownTextVisibility", countdownText.visibility)
        outState.putInt("gridLayoutVisibility", gridLayout.visibility)
        outState.putInt("pauseButtonVisibility", pauseButton.visibility)
        outState.putLong("timerRemainingTimeMs", timerProgressBar.getRemainingTimeSeconds() * 1000L)
        outState.putBoolean("timerIsRunning", timerProgressBar.isRunning())
        outState.putInt("countdownIndex", countdownManager.getIndex())
        outState.putBoolean("countdownInProgress", countdownManager.isInProgress())
        outState.putLong("sequenceDelayRemaining", sequenceDelayRemaining)
        outState.putLong("pendingShowSequenceDelay", pendingShowSequenceDelay)
        starManager.saveState(outState)
        saveGameStats(outState)
    }

    private fun restoreGameState(savedInstanceState: Bundle) {
        currentLevel = savedInstanceState.getInt("currentLevel", 1)
        numKeys = savedInstanceState.getInt("numKeys", 4)
        currentSeqLength = savedInstanceState.getInt("currentSeqLength", 1)
        sequenceShowIndex = savedInstanceState.getInt("sequenceShowIndex", 0)
        isShowingSequence = savedInstanceState.getBoolean("isShowingSequence", false)
        isUserTurn = savedInstanceState.getBoolean("isUserTurn", false)
        currentSequence = savedInstanceState.getIntArray("currentSequence")?.toMutableList() ?: mutableListOf()
        userSequence = savedInstanceState.getIntArray("userSequence")?.toMutableList() ?: mutableListOf()
        pauseOverlay.visibility = savedInstanceState.getInt("pauseOverlayVisibility")
        countdownText.visibility = savedInstanceState.getInt("countdownTextVisibility")
        gridLayout.visibility = savedInstanceState.getInt("gridLayoutVisibility")
        pauseButton.visibility = savedInstanceState.getInt("pauseButtonVisibility")
        sequenceDelayRemaining = savedInstanceState.getLong("sequenceDelayRemaining", 0L)
        pendingShowSequenceDelay = savedInstanceState.getLong("pendingShowSequenceDelay", 0L)
        starManager.restoreState(savedInstanceState)

        val timerRemainingTimeMs = savedInstanceState.getLong("timerRemainingTimeMs", BASE_TIME_SECONDS * 1000L)
        val timerIsRunning = savedInstanceState.getBoolean("timerIsRunning", false)
        timerProgressBar.setRemainingTimeMs(timerRemainingTimeMs.coerceAtLeast(1L))

        val countdownIndex = savedInstanceState.getInt("countdownIndex", 0)
        val countdownInProgress = savedInstanceState.getBoolean("countdownInProgress", false)

        if (numKeys != 0) {
            keyButtons = createKeys(numKeys)
        }

        pauseMenu.syncWithOverlay()

        if (countdownInProgress) {
            countdownManager.startCountdown(countdownIndex)
            return
        }

        if (timerIsRunning && !isShowingSequence && pauseOverlay.visibility != View.VISIBLE) {
            timerProgressBar.start()
        }

        if (isShowingSequence) {
            gridLayout.isEnabled = false
            keyButtons.forEach { it.view.isEnabled = false }

            if (sequenceDelayRemaining > 0) {
                runDelayed(sequenceDelayRemaining) {
                    if (sequenceShowIndex < currentSequence.size) {
                        highlightKey(currentSequence[sequenceShowIndex], false)
                        sequenceShowIndex++
                        playSequenceStep()
                    } else {
                        endSequenceShow()
                    }
                }
            } else if (sequenceShowIndex >= currentSequence.size) {
                endSequenceShow()
            } else {
                showSequence()
            }
        } else if (isUserTurn) {
            gridLayout.isEnabled = true
            keyButtons.forEach { it.view.isEnabled = true }
        }

        if (pendingShowSequenceDelay > 0L) {
            runDelayed(pendingShowSequenceDelay) {
                if (!isFinishing && !isDestroyed) {
                    showSequence()
                    pendingShowSequenceDelay = 0L
                }
            }
        }
        restoreGameStats(savedInstanceState)
    }

    private fun startNewGame() {
        gameStatsManager.startReactionTracking()
        if (pauseMenu.isPaused) {
            pauseMenu.resume()
        } else {
            pauseOverlay.visibility = View.GONE
        }

        timerProgressBar.stop()
        timerProgressBar.reset()

        gridLayout.isEnabled = true

        val config = getSequenceConfig(currentLevel)
        numKeys = config.numKeys
        currentSeqLength = config.startLength

        keyButtons = createKeys(numKeys)
        userSequence.clear()
        currentSequence.clear()
        generateNewSequence()
        showSequence()
    }

    private fun getSequenceConfig(level: Int): SequenceConfig = when (level) {
        1 -> SequenceConfig(4, 1, 1, 6)
        2 -> SequenceConfig(4, 1, 1, 8)
        3 -> SequenceConfig(4, 2, 2, 10)
        4 -> SequenceConfig(6, 1, 1, 6)
        5 -> SequenceConfig(6, 1, 1, 8)
        6 -> SequenceConfig(6, 2, 2, 10)
        7 -> SequenceConfig(8, 1, 1, 6)
        8 -> SequenceConfig(8, 1, 1, 8)
        9 -> SequenceConfig(8, 2, 2, 10)
        else -> SequenceConfig(8, 2, 2, 10)
    }

    private fun getKeyColors(numKeys: Int): List<Int> = when (numKeys) {
        4 -> listOf(
            Color.RED,
            Color.YELLOW,
            Color.rgb(84, 237, 56),
            Color.rgb(18, 26, 255),
        )
        6 -> listOf(
            Color.RED,
            Color.rgb(255, 165, 0),
            Color.YELLOW,
            Color.rgb(84, 237, 56),
            Color.rgb(18, 26, 255),
            Color.rgb(124, 9, 181)
        )
        else -> listOf(
            Color.rgb(234, 83, 185),
            Color.RED,
            Color.rgb(255, 165, 0),
            Color.YELLOW,
            Color.rgb(84, 237, 56),
            Color.rgb(78, 255, 242),
            Color.rgb(18, 26, 255),
            Color.rgb(124, 9, 181)
        )
    }

    private fun getSoundOrder(numKeys: Int): List<Int> = when (numKeys) {
        4 -> soundOrder4
        6 -> soundOrder6
        else -> soundOrder8
    }

    private fun createKeys(numKeys: Int): List<KeyButton> {
        val buttonsList = mutableListOf<KeyButton>()
        val colors = getKeyColors(numKeys)
        gridLayout.removeAllViews()

        val cols = when (numKeys) {
            4 -> 2
            6 -> 3
            else -> 4
        }
        gridLayout.columnCount = cols
        gridLayout.rowCount = 2

        gridLayout.clipToPadding = false
        gridLayout.clipChildren = false

        for (i in 0 until numKeys) {
            val button = Button(this).apply {
                val drawable = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = 40f
                    setColor(colors[i])
                }
                background = drawable

                text = ""
                setTextColor(Color.WHITE)
                textSize = 28f
                gravity = Gravity.CENTER

                layoutParams = GridLayout.LayoutParams().apply {
                    width = 0
                    height = 0
                    rowSpec = GridLayout.spec(i / cols, 1f)
                    columnSpec = GridLayout.spec(i % cols, 1f)
                    setMargins(16, 16, 16, 16)
                }
                isEnabled = isUserTurn && !isShowingSequence

                elevation = 8f
            }

            button.setOnClickListener {
                if (isUserTurn && !isShowingSequence && gridLayout.isEnabled) {
                    onKeyPress(i)
                }
            }

            gridLayout.addView(button)
            buttonsList.add(KeyButton(button, i))
        }
        return buttonsList
    }

    private fun generateNewSequence() {
        val config = getSequenceConfig(currentLevel)

        if (currentSequence.isEmpty()) {
            repeat(config.startLength) {
                currentSequence.add(Random.nextInt(0, numKeys))
            }
        }
        else if (currentSequence.size < config.maxLength) {
            val remaining = config.maxLength - currentSequence.size
            val toAdd = minOf(config.step, remaining)

            repeat(toAdd) {
                currentSequence.add(Random.nextInt(0, numKeys))
            }
        }
        currentSeqLength = currentSequence.size
    }

    private fun showSequence() {
        isUserTurn = false
        isShowingSequence = true
        gridLayout.isEnabled = false
        keyButtons.forEach { it.view.isEnabled = false }

        onGamePaused()

        timerProgressBar.pause()
        sequenceShowIndex = 0
        playSequenceStep()
    }

    private fun playSequenceStep() {
        if (sequenceShowIndex >= currentSequence.size) {
            endSequenceShow()
            return
        }

        val keyIndex = currentSequence[sequenceShowIndex]
        val soundOrder = getSoundOrder(numKeys)
        val soundIndex = soundOrder[keyIndex]
        highlightKey(keyIndex, true)
        playKeySound(soundIndex)

        runDelayed(500L) {
            highlightKey(keyIndex, false)
            sequenceShowIndex++
            runDelayed(200L) { playSequenceStep() }
        }
    }

    private fun endSequenceShow() {
        isShowingSequence = false
        gridLayout.isEnabled = true
        keyButtons.forEach { it.view.isEnabled = true }
        isUserTurn = true
        userSequence.clear()

        onGameResumed()

        timerProgressBar.start()
    }

    private fun onKeyPress(keyIndex: Int) {
        val soundOrder = getSoundOrder(numKeys)
        val soundIndex = soundOrder[keyIndex]
        playKeySound(soundIndex)
        highlightKey(keyIndex, true)
        runDelayed(200L) { highlightKey(keyIndex, false) }

        gameStatsManager.registerAttempt(true)

        userSequence.add(keyIndex)

        if (userSequence.size > currentSequence.size ||
            userSequence[userSequence.size - 1] != currentSequence[userSequence.size - 1]) {
            gameStatsManager.registerAttempt(false)
            checkUserSequence()
            return
        }

        if (userSequence.size == currentSequence.size) {
            checkUserSequence()
        }
    }

    private fun checkUserSequence() {
        isUserTurn = false
        gridLayout.isEnabled = false
        keyButtons.forEach { it.view.isEnabled = false }
        timerProgressBar.pause()

        if (userSequence == currentSequence) {
            starManager.increment()
            val config = getSequenceConfig(currentLevel)

            if (currentSequence.size >= config.maxLength) {
                currentLevel++
                timerProgressBar.addTime(15)
                Toast.makeText(this, "Świetnie! Nowy poziom: $currentLevel +15s BONUS", Toast.LENGTH_SHORT).show()

                val newConfig = getSequenceConfig(currentLevel)
                numKeys = newConfig.numKeys
                keyButtons = createKeys(numKeys)
                currentSequence.clear()
            }

            generateNewSequence()

            pendingShowSequenceDelay = 1500L
            runDelayed(1500L) {
                if (!isFinishing && !isDestroyed) {
                    showSequence()
                    pendingShowSequenceDelay = 0L
                }
            }
        } else {
            Toast.makeText(this, "Błąd! Powtórka sekwencji.", Toast.LENGTH_SHORT).show()
            userSequence.clear()
            pendingShowSequenceDelay = 1500L
            runDelayed(1500L) {
                if (!isFinishing && !isDestroyed) {
                    showSequence()
                    pendingShowSequenceDelay = 0L
                }
            }
        }
    }

    private fun playKeySound(soundIndex: Int) {
        SoundManager.play(this, soundResources[soundIndex])
    }

    private fun highlightKey(keyIndex: Int, highlight: Boolean) {
        val button = keyButtons[keyIndex].view
        val bg = button.background as GradientDrawable
        val colors = getKeyColors(numKeys)

        if (highlight) {
            bg.setColor(makeFaded(colors[keyIndex]))
            ObjectAnimator.ofFloat(button, "scaleX", 1f, 0.95f).setDuration(100).start()
            ObjectAnimator.ofFloat(button, "scaleY", 1f, 0.95f).setDuration(100).start()
        } else {
            bg.setColor(colors[keyIndex])
            ObjectAnimator.ofFloat(button, "scaleX", 0.95f, 1f).setDuration(150).start()
            ObjectAnimator.ofFloat(button, "scaleY", 0.95f, 1f).setDuration(150).start()
        }
    }

    private fun makeFaded(color: Int): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        hsv[1] *= 0.8f // nasycenie
        hsv[2] *= 1.1f // jasność
        return Color.HSVToColor(hsv)
    }

    private fun runDelayed(delay: Long, action: () -> Unit) {
        val activityRef = java.lang.ref.WeakReference(this)
        var remaining = delay
        val interval = 16L

        val runnable = object : Runnable {
            override fun run() {
                val activity = activityRef.get() ?: return
                if (pauseMenu.isPaused) {
                    Handler(Looper.getMainLooper()).postDelayed(this, interval)
                    return
                }
                remaining -= interval
                if (pendingShowSequenceDelay == 0L) {
                    sequenceDelayRemaining = remaining.coerceAtLeast(0L)
                }
                if (remaining <= 0) {
                    if (activity.isFinishing || activity.isDestroyed) return
                    activity.runOnUiThread { action() }
                    sequenceDelayRemaining = 0L
                } else {
                    Handler(Looper.getMainLooper()).postDelayed(this, interval)
                }
            }
        }
        Handler(Looper.getMainLooper()).postDelayed(runnable, interval)
    }

    private fun handleGameOver() {
        isGameEnding = true
        gridLayout.isEnabled = false
        keyButtons.forEach { it.view.isEnabled = false }
        pauseOverlay.visibility = View.GONE

        showGameOverDialog(
            categoryKey = GameKeys.CATEGORY_MEMORY,
            gameKey = GameKeys.GAME_COLOR_SEQUENCE,
            starManager = starManager,
            timerProgressBar = timerProgressBar,
            countdownManager = countdownManager,
            currentBestScore = currentBestScore,
            onRestartAction = {
                if (starManager.starCount > currentBestScore) {
                    currentBestScore = starManager.starCount
                }
                currentLevel = 1
                userSequence.clear()
                currentSequence.clear()
                startNewGame()
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