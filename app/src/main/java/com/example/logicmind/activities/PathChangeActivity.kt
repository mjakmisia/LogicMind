package com.example.logicmind.activities

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.children
import androidx.gridlayout.widget.GridLayout
import com.example.logicmind.R
import com.example.logicmind.common.GameCountdownManager
import com.example.logicmind.common.GameTimerProgressBar
import com.example.logicmind.common.PauseMenu
import com.example.logicmind.common.StarManager
import java.io.Serializable
import java.util.Locale

class PathChangeActivity : BaseActivity() {
    private lateinit var gridLayout: GridLayout
    private lateinit var countdownText: TextView
    private lateinit var pauseButton: ImageButton
    private lateinit var pauseOverlay: ConstraintLayout
    private lateinit var rootLayout: ConstraintLayout
    private lateinit var streakCountText: TextView
    private lateinit var timerProgressBar: GameTimerProgressBar
    private lateinit var starManager: StarManager
    private lateinit var pauseMenu: PauseMenu
    private lateinit var countdownManager: GameCountdownManager
    private var isGameEnding = false
    private var isGameRunning = false
    private var currentLevel = 1
    private val switchStates = HashMap<String, Int>()
    private val switchViews = mutableListOf<FrameLayout>()
    private val cellViewMapByCoords = mutableMapOf<Pair<Int, Int>, View>()
    private val activeBalls = mutableListOf<BallState>()
    private val spawnHandler = Handler(Looper.getMainLooper())
    private val ballColors = listOf("red", "yellow", "green", "blue")
    private val ballSizePx: Int by lazy {
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            BALL_SIZE_DP.toFloat(),
            this.resources.displayMetrics
        ).toInt()
    }
    private var remainingSpawnDelayMs: Long = 0L
    private var isSpawnLoopRunning = false
    private val spawnLoopInterval = 100L
    private var currentBestScore = 0

    private data class BallState(
        @Transient val imageView: ImageView,
        val color: String,
        var currentRow: Int,
        var currentCol: Int,
        var prevRow: Int,
        var prevCol: Int,
        var isMoving: Boolean = false,
        @Transient var currentAnimator: ObjectAnimator? = null
    )
    private data class SerializableBallState(
        val color: String,
        val currentRow: Int,
        val currentCol: Int,
        val prevRow: Int,
        val prevCol: Int,
        val isMoving: Boolean
    ) : Serializable

    private var currentAnimDuration: Long = INITIAL_ANIM_DURATION
    private var currentSpawnDelayMs: Long = INITIAL_SPAWN_DELAY
    private var totalMoves = 0
    private var successfulStreak = 0

    companion object {
        private const val BASE_TIME_SECONDS = 90
        private const val BALL_SIZE_DP = 28
        private const val MOVES_PER_SPEEDUP = 6

        private const val MIN_ANIM_DURATION = 220L
        private const val MIN_SPAWN_DELAY = 1200L
        private const val ANIM_SPEEDUP_STEP = 20L
        private const val SPAWN_SPEEDUP_STEP = 150L

        private const val INITIAL_ANIM_DURATION = 400L
        private const val INITIAL_SPAWN_DELAY = 2500L

        private const val PENALTY_TIME_SECONDS = 5
        private const val COMBO_BONUS_5 = 5
        private const val COMBO_BONUS_10 = 8
        private const val COMBO_BONUS_15 = 12
        private const val COMBO_BONUS_20 = 15
        private const val COMBO_SUPER_BONUS = 20
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_path_change)
        supportActionBar?.hide()

        rootLayout = findViewById(R.id.rootLayout)
        gridLayout = findViewById(R.id.gridLayout)
        countdownText = findViewById(R.id.countdownText)
        pauseButton = findViewById(R.id.pauseButton)
        pauseOverlay = findViewById(R.id.pauseOverlay)
        timerProgressBar = findViewById(R.id.gameTimerProgressBar)
        starManager = StarManager()
        starManager.init(findViewById(R.id.starCountText))
        streakCountText = findViewById(R.id.streakCountText)

        if (isUserLoggedIn()) {
            val uid = auth.currentUser!!.uid
            db.getReference("users").child(uid).child("categories")
                .child(GameKeys.CATEGORY_REASONING).child(GameKeys.GAME_PATH_CHANGE)
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
                timerProgressBar,
                streakCountText
            ),
            onCountdownFinished = {
                currentLevel = 1
                starManager.reset()
                timerProgressBar.stop()
                timerProgressBar.reset()
                timerProgressBar.start()
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
                starManager.reset()
                timerProgressBar.stop()
                timerProgressBar.reset()
                stopSpawningBalls()
                clearAllBalls()
                countdownManager.startCountdown()
            },
            onResume = {
                if (isGameRunning) {
                    timerProgressBar.start()
                    startSpawningBalls()
                    resumeAllBalls()
                }
                onGameResumed()
            },
            onPause = {
                if (isGameRunning) {
                    timerProgressBar.pause()
                    stopSpawningBalls()
                    pauseAllBalls()
                }
                onGamePaused()
            },
            onExit = {
                handleGameOver()
            },
            instructionTitle = getString(R.string.instructions),
            instructionMessage = getString(R.string.path_change_instruction),
        )

        if (savedInstanceState == null) {
            countdownManager.startCountdown()

        } else {
            restoreGameState(savedInstanceState)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("pauseOverlayVisibility", pauseOverlay.visibility)
        outState.putInt("countdownTextVisibility", countdownText.visibility)
        outState.putInt("gridLayoutVisibility", gridLayout.visibility)
        outState.putInt("pauseButtonVisibility", pauseButton.visibility)
        outState.putLong("timerRemainingTimeMs", timerProgressBar.getRemainingTimeSeconds() * 1000L)
        outState.putBoolean("timerIsRunning", timerProgressBar.isRunning())
        outState.putInt("countdownIndex", countdownManager.getIndex())
        outState.putBoolean("countdownInProgress", countdownManager.isInProgress())
        outState.putInt("currentLevel", currentLevel)
        outState.putBoolean("isGameRunning", isGameRunning)
        outState.putSerializable("switchStates", switchStates)
        starManager.saveState(outState)

        val serializableBalls = ArrayList(activeBalls.map { ball ->
            SerializableBallState(
                color = ball.color,
                currentRow = ball.currentRow,
                currentCol = ball.currentCol,
                prevRow = ball.prevRow,
                prevCol = ball.prevCol,
                isMoving = ball.isMoving
            )
        })

        outState.putSerializable("activeBalls", serializableBalls)
        outState.putLong("remainingSpawnDelayMs", remainingSpawnDelayMs)
        outState.putLong("currentAnimDuration", currentAnimDuration)
        outState.putLong("currentSpawnDelayMs", currentSpawnDelayMs)
        outState.putInt("totalMoves", totalMoves)
        outState.putInt("successfulStreak", successfulStreak)
        saveGameStats(outState)
    }

    private fun restoreGameState(savedInstanceState: Bundle) {
        pauseOverlay.visibility = savedInstanceState.getInt("pauseOverlayVisibility", View.GONE)
        countdownText.visibility = savedInstanceState.getInt("countdownTextVisibility", View.GONE)
        gridLayout.visibility = savedInstanceState.getInt("gridLayoutVisibility", View.VISIBLE)
        pauseButton.visibility = savedInstanceState.getInt("pauseButtonVisibility", View.VISIBLE)
        currentLevel = savedInstanceState.getInt("currentLevel", 1)
        starManager.restoreState(savedInstanceState)

        val timerRemainingTimeMs = savedInstanceState.getLong("timerRemainingTimeMs", BASE_TIME_SECONDS * 1000L)
        val timerIsRunning = savedInstanceState.getBoolean("timerIsRunning", false)
        timerProgressBar.setRemainingTimeMs(timerRemainingTimeMs.coerceAtLeast(1L))

        isGameRunning = savedInstanceState.getBoolean("isGameRunning", false)

        val savedStates = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            @Suppress("UNCHECKED_CAST")
            savedInstanceState.getSerializable("switchStates", HashMap::class.java) as? HashMap<String, Int>
        } else {
            @Suppress("DEPRECATION", "UNCHECKED_CAST")
            savedInstanceState.getSerializable("switchStates") as? HashMap<String, Int>
        }
        if (savedStates != null) {
            switchStates.clear()
            switchStates.putAll(savedStates)
        }

        remainingSpawnDelayMs = savedInstanceState.getLong("remainingSpawnDelayMs", INITIAL_SPAWN_DELAY)
        currentAnimDuration = savedInstanceState.getLong("currentAnimDuration", INITIAL_ANIM_DURATION)
        currentSpawnDelayMs = savedInstanceState.getLong("currentSpawnDelayMs", INITIAL_SPAWN_DELAY)
        totalMoves = savedInstanceState.getInt("totalMoves", 0)
        successfulStreak = savedInstanceState.getInt("successfulStreak", 0)
        updateStreakDisplay()

        val countdownIndex = savedInstanceState.getInt("countdownIndex", 0)
        val countdownInProgress = savedInstanceState.getBoolean("countdownInProgress", false)

        if (countdownInProgress) {
            countdownManager.startCountdown(countdownIndex)
        } else if (isGameRunning) {
            if (timerIsRunning && pauseOverlay.visibility != View.VISIBLE) {
                timerProgressBar.start()
            }

            gridLayout.post {
                setupGrid()

                val savedBalls = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    @Suppress("UNCHECKED_CAST")
                    savedInstanceState.getSerializable("activeBalls", ArrayList::class.java) as? ArrayList<SerializableBallState>
                } else {
                    @Suppress("DEPRECATION", "UNCHECKED_CAST")
                    savedInstanceState.getSerializable("activeBalls") as? ArrayList<SerializableBallState>
                }

                savedBalls?.forEach { savedBall ->
                    restoreBall(savedBall)
                }
            }

            if (pauseOverlay.visibility != View.VISIBLE) {
                startSpawningBalls()
            }
        }

        pauseMenu.syncWithOverlay()
        restoreGameStats(savedInstanceState)
    }

    private fun startNewGame() {
        gameStatsManager.startReactionTracking()
        if (pauseMenu.isPaused) pauseMenu.resume()

        isGameRunning = true
        isGameEnding = false
        gridLayout.isEnabled = true
        switchStates.clear()
        clearAllBalls()

        currentAnimDuration = INITIAL_ANIM_DURATION
        currentSpawnDelayMs = INITIAL_SPAWN_DELAY
        totalMoves = 0
        successfulStreak = 0
        updateStreakDisplay()

        setupGrid()

        remainingSpawnDelayMs = 500L
        startSpawningBalls()
    }

    private fun setupGrid() {
        switchViews.clear()
        cellViewMapByCoords.clear()
        val colCount = gridLayout.columnCount

        for ((index, child) in gridLayout.children.withIndex()) {
            val cell = child as? FrameLayout ?: continue
            val tag = cell.tag as? String ?: continue

            val row = index / colCount
            val col = index % colCount
            cellViewMapByCoords[Pair(row, col)] = cell

            if (tag.startsWith("switch_")) {
                switchViews.add(cell)
                val state = switchStates.getOrPut(tag) { 0 }

                val pathImage = cell.getChildAt(1) as? ImageView
                if (pathImage != null) {
                    updateSwitchImage(pathImage, tag, state)
                }

                cell.setOnClickListener {
                    onSwitchClicked(it as FrameLayout)
                }
            }
        }
    }

    private fun onSwitchClicked(cell: FrameLayout) {
        if (!isGameRunning || isGameEnding) return

        val tag = cell.tag as String
        val currentState = switchStates[tag] ?: 0
        val newState = (currentState + 1) % 2
        switchStates[tag] = newState

        val pathImage = cell.getChildAt(1) as? ImageView
        if (pathImage != null) {
            updateSwitchImage(pathImage, tag, newState)
        }
    }

    private fun updateSwitchImage(imageView: ImageView, tag: String, state: Int) {
        when (tag) {
            "switch_A" -> {
                if (state == 0) {
                    imageView.setImageResource(R.drawable.path_straight)
                    imageView.rotation = 0f
                } else {
                    imageView.setImageResource(R.drawable.path_corner)
                    imageView.rotation = 180f
                }
            }
            "switch_B" -> {
                if (state == 0) {
                    imageView.setImageResource(R.drawable.path_corner)
                    imageView.rotation = 0f
                } else {
                    imageView.setImageResource(R.drawable.path_corner)
                    imageView.rotation = 270f
                }
            }
            "switch_C" -> {
                if (state == 0) {
                    imageView.setImageResource(R.drawable.path_corner)
                    imageView.rotation = 270f
                } else {
                    imageView.setImageResource(R.drawable.path_straight)
                    imageView.rotation = 0f
                }
            }
            else -> imageView.setImageResource(R.drawable.bg_rounded_card)
        }
    }

    private fun startSpawningBalls() {
        if (isSpawnLoopRunning) return
        isSpawnLoopRunning = true

        spawnHandler.post(object : Runnable {
            override fun run() {
                if (!isSpawnLoopRunning) return

                if (!isGameRunning || isGameEnding) {
                    isSpawnLoopRunning = false
                    return
                }

                if (pauseMenu.isPaused) {
                    spawnHandler.postDelayed(this, spawnLoopInterval)
                    return
                }

                remainingSpawnDelayMs -= spawnLoopInterval

                if (remainingSpawnDelayMs <= 0) {
                    spawnBall()
                    remainingSpawnDelayMs = currentSpawnDelayMs
                }

                spawnHandler.postDelayed(this, spawnLoopInterval)
            }
        })
    }

    private fun stopSpawningBalls() {
        isSpawnLoopRunning = false
        spawnHandler.removeCallbacksAndMessages(null)
    }

    private fun createBallImageView(color: String): ImageView {
        val drawableId = when (color) {
            "red" -> R.drawable.path_ball_red
            "yellow" -> R.drawable.path_ball_yellow
            "green" -> R.drawable.path_ball_green
            "blue" -> R.drawable.path_ball_blue
            else -> R.drawable.path_ball_red
        }

        val layoutParams = ConstraintLayout.LayoutParams(ballSizePx, ballSizePx)

        return ImageView(this).apply {
            setImageResource(drawableId)
            this.layoutParams = layoutParams
            rootLayout.addView(this)
        }
    }

    private fun spawnBall() {
        if (!isGameRunning || isGameEnding) return

        val randomColor = ballColors.random()
        val imageView = createBallImageView(randomColor)

        imageView.x = -1000f
        imageView.y = -1000f

        val newBall = BallState(
            imageView = imageView,
            color = randomColor,
            currentRow = 5,
            currentCol = 4,
            prevRow = -1,
            prevCol = -1
        )

        activeBalls.add(newBall)

        val startCell = cellViewMapByCoords[Pair(newBall.currentRow, newBall.currentCol)]
        if (startCell != null) {
            moveToStartCell(newBall)
            startMovement(newBall)
        } else {
            removeBall(newBall)
        }
    }

    private fun restoreBall(savedBall: SerializableBallState) {
        if (!isGameRunning) return

        val imageView = createBallImageView(savedBall.color)

        val newBall = BallState(
            imageView = imageView,
            color = savedBall.color,
            currentRow = savedBall.currentRow,
            currentCol = savedBall.currentCol,
            prevRow = savedBall.prevRow,
            prevCol = savedBall.prevCol,
            isMoving = savedBall.isMoving
        )

        activeBalls.add(newBall)

        moveToStartCell(newBall)

        if (newBall.isMoving && !pauseMenu.isPaused) {
            executeNextMove(newBall)
        }
    }

    private fun pauseAllBalls() {
        activeBalls.forEach { ball ->
            ball.currentAnimator?.pause()
        }
    }

    private fun resumeAllBalls() {
        activeBalls.forEach { ball ->
            ball.currentAnimator?.resume()
            if (ball.isMoving && ball.currentAnimator == null) {
                executeNextMove(ball)
            }
        }
    }

    private fun removeBall(ball: BallState) {
        ball.isMoving = false
        ball.currentAnimator?.cancel()
        (ball.imageView.parent as? ViewGroup)?.removeView(ball.imageView)
        activeBalls.remove(ball)
    }

    private fun clearAllBalls() {
        activeBalls.toList().forEach { removeBall(it) }
    }

    private fun moveToStartCell(ball: BallState) {
        val cell = cellViewMapByCoords[Pair(ball.currentRow, ball.currentCol)] ?: return
        val (startX, startY) = getCenterCoords(cell, ball.imageView)
        ball.imageView.x = startX
        ball.imageView.y = startY
    }

    private fun startMovement(ball: BallState) {
        if (ball.isMoving || pauseMenu.isPaused) return

        ball.isMoving = true
        Handler(Looper.getMainLooper()).postDelayed({
            if (ball.isMoving && !pauseMenu.isPaused) {
                executeNextMove(ball)
            }
        }, 100)
    }

    private fun executeNextMove(ball: BallState) {
        if (!ball.isMoving || pauseMenu.isPaused) return

        val (nextRow, nextCol) = findNextStep(ball.currentRow, ball.currentCol)

        if (nextRow == -1) {
            removeBall(ball)
            return
        }

        val nextCellView = cellViewMapByCoords[Pair(nextRow, nextCol)] ?: run {
            removeBall(ball)
            return
        }

        val nextCellTag = nextCellView.tag as? String ?: ""
        val isGoal = nextCellTag.startsWith("goal_")

        val (targetX, targetY) = getCenterCoords(nextCellView, ball.imageView)
        animateMove(ball, targetX, targetY) {
            if (isGoal) {
                val goalColor = nextCellTag.substringAfter("goal_")
                handleGoalReached(ball.color, goalColor)
                removeBall(ball)
            } else {
                ball.prevRow = ball.currentRow
                ball.prevCol = ball.currentCol
                ball.currentRow = nextRow
                ball.currentCol = nextCol
                executeNextMove(ball)
            }
        }
    }

    private fun handleGoalReached(ballColor: String, goalColor: String) {
        totalMoves++
        accelerateIfNeeded()

        if (ballColor == goalColor) {
            starManager.increment()
            successfulStreak++
            checkComboBonus()
            gameStatsManager.registerAttempt(true)
        } else {
            successfulStreak = 0
            timerProgressBar.subtractTime(PENALTY_TIME_SECONDS)
            Toast.makeText(this, String.format(Locale.US, "-%ds!", PENALTY_TIME_SECONDS), Toast.LENGTH_SHORT).show()
            gameStatsManager.registerAttempt(false)
        }

        updateStreakDisplay()
    }

    private fun findNextStep(r: Int, c: Int): Pair<Int, Int> {

        return when (Pair(r, c)) {
            Pair(5, 4) -> Pair(5, 3)
            Pair(5, 3) -> Pair(5, 2)
            Pair(5, 2) -> Pair(4, 2)

            Pair(4, 2) -> {
                val state = switchStates["switch_C"] ?: 0
                if (state == 0) Pair(4, 1) else Pair(3, 2)
            }

            Pair(4, 1) -> Pair(5, 1)
            Pair(5, 1) -> Pair(5, 0)
            Pair(5, 0) -> Pair(4, 0)
            Pair(4, 0) -> Pair(3, 0)
            Pair(3, 0) -> Pair(2, 0)

            Pair(2, 0) -> {
                val state = switchStates["switch_A"] ?: 0
                if (state == 0) Pair(1, 0) else Pair(2, 1)
            }
            Pair(1, 0) -> Pair(0, 0)
            Pair(0, 0) -> Pair(0, 1)

            Pair(3, 2) -> Pair(2, 2)
            Pair(2, 2) -> Pair(2, 3)

            Pair(2, 3) -> {
                val state = switchStates["switch_B"] ?: 0
                if (state == 0) Pair(1, 3) else Pair(3, 3)
            }
            Pair(1, 3) -> Pair(0, 3)
            Pair(0, 3) -> Pair(0, 4)
            Pair(3, 3) -> Pair(3, 4)

            else -> Pair(-1, -1)
        }
    }

    private fun animateMove(ball: BallState, targetX: Float, targetY: Float, onEnd: () -> Unit) {
        val pvhX = PropertyValuesHolder.ofFloat(View.X, targetX)
        val pvhY = PropertyValuesHolder.ofFloat(View.Y, targetY)
        ball.currentAnimator = ObjectAnimator.ofPropertyValuesHolder(ball.imageView, pvhX, pvhY).apply {
            duration = currentAnimDuration
            interpolator = android.view.animation.LinearInterpolator()
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    ball.currentAnimator = null
                    if (ball.isMoving && !pauseMenu.isPaused) {
                        onEnd()
                    }
                }
            })
            start()
        }
    }

    private fun getCenterCoords(cell: View, ballImageView: ImageView): Pair<Float, Float> {
        val cellRect = Rect()
        cell.getGlobalVisibleRect(cellRect)

        val rootRect = Rect()
        rootLayout.getGlobalVisibleRect(rootRect)

        val cellXInRoot = cellRect.left - rootRect.left
        val cellYInRoot = cellRect.top - rootRect.top

        val ballSize = ballImageView.layoutParams.width
        val centerX = cellXInRoot + (cell.width / 2f) - (ballSize / 2f)
        val centerY = cellYInRoot + (cell.height / 2f) - (ballSize / 2f)

        return Pair(centerX, centerY)
    }

    private fun accelerateIfNeeded() {
        if (totalMoves > 0 && totalMoves % MOVES_PER_SPEEDUP == 0) {
            var accelerated = false

            if (currentAnimDuration > MIN_ANIM_DURATION) {
                currentAnimDuration = (currentAnimDuration - ANIM_SPEEDUP_STEP).coerceAtLeast(MIN_ANIM_DURATION)
                accelerated = true
            }

            if (currentSpawnDelayMs > MIN_SPAWN_DELAY) {
                currentSpawnDelayMs = (currentSpawnDelayMs - SPAWN_SPEEDUP_STEP).coerceAtLeast(MIN_SPAWN_DELAY)
                accelerated = true
            }

            if (accelerated) {
                Toast.makeText(this, "Tempo rośnie!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkComboBonus() {
        val bonusSeconds = when (successfulStreak) {
            5 -> COMBO_BONUS_5
            10 -> COMBO_BONUS_10
            15 -> COMBO_BONUS_15
            20 -> COMBO_BONUS_20
            else -> 0
        }

        if (bonusSeconds > 0) {
            timerProgressBar.addTime(bonusSeconds)
            Toast.makeText(this, String.format(Locale.US, "+%ds Combo!", bonusSeconds), Toast.LENGTH_SHORT).show()
        }

        if (successfulStreak > 20 && successfulStreak % 20 == 0) {
            timerProgressBar.addTime(COMBO_SUPER_BONUS)
            Toast.makeText(this, String.format(Locale.US, "+%ds EXTRA COMBO!", COMBO_SUPER_BONUS), Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateStreakDisplay() {
        if (successfulStreak > 1) {
            streakCountText.text = String.format(Locale.US, "x%d", successfulStreak)
            streakCountText.visibility = View.VISIBLE
        } else {
            streakCountText.visibility = View.GONE
        }
    }

    private fun handleGameOver() {
        isGameEnding = true
        isGameRunning = false
        stopSpawningBalls()
        clearAllBalls()
        gridLayout.isEnabled = false
        pauseOverlay.visibility = View.GONE

        showGameOverDialog(
            categoryKey = GameKeys.CATEGORY_REASONING,
            gameKey = GameKeys.GAME_PATH_CHANGE,
            starManager = starManager,
            timerProgressBar = timerProgressBar,
            countdownManager = countdownManager,
            currentBestScore = currentBestScore,
            onRestartAction = {
                if (starManager.starCount > currentBestScore) {
                    currentBestScore = starManager.starCount
                }
                currentLevel = 1
                stopSpawningBalls()
                clearAllBalls()
            }
        )
    }

    override fun onPause() {
        super.onPause()
        if (isGameRunning && !isGameEnding && !pauseMenu.isPaused && !isChangingConfigurations) {
            pauseMenu.pause()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        timerProgressBar.stop()
        countdownManager.cancel()
        stopSpawningBalls()
        clearAllBalls()
    }
}