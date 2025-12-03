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
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var trackContainer: FrameLayout
    private lateinit var trackLine: View
    private lateinit var countdownText: TextView
    private lateinit var pauseButton: ImageButton
    private lateinit var pauseOverlay: ConstraintLayout
    private lateinit var timerProgressBar: GameTimerProgressBar
    private lateinit var starManager: StarManager
    private lateinit var pauseMenu: PauseMenu
    private lateinit var countdownManager: GameCountdownManager
    private lateinit var blueContainer: FrameLayout
    private lateinit var redContainer: FrameLayout
    private lateinit var tempoInfoText: TextView
    private val circleQueue = mutableListOf<Circle>()
    private var isProcessing = false
    private var isGameRunning = false
    private var isGameEnding = false
    private var awaitingDoubleClick = false
    private var awaitingDoubleSide: Int? = null
    private var awaitingDoubleForId: Int? = null
    private var nextCircleId = 1
    private var lastTapTime: Long = 0
    private var lastTapSide: Boolean? = null
    private val bothTapWindowMs = 300L
    private var currentReactionTimeMs = 3500L
    private var successfulStreak = 0
    private var totalMoves = 0

    private var currentBestScore = 0

    companion object {
        const val BASE_TIME_SECONDS = 90
        private const val BLOCK_DELAY_MS = 1300L
        private const val CIRCLE_SIZE_DP = 130
        private const val VISIBLE_CIRCLES = 4
        private const val CIRCLE_SIZE_DP_LANDSCAPE = 100
        private const val VISIBLE_CIRCLES_LANDSCAPE = 3
        private const val ANIMATION_DURATION_MS = 300L
        private const val MIN_REACTION_TIME_MS = 800L
        private const val SPEEDUP_STEP_MS = 300L
        private const val MOVES_PER_SPEEDUP = 12
    }

    private var currentCircleSizeDp: Int = CIRCLE_SIZE_DP
    private var currentVisibleCircles: Int = VISIBLE_CIRCLES

    private val redColor = "#EF5350".toColorInt()
    private val blueColor = "#4FC3F7".toColorInt()
    private val redStroke = "#D32F2F".toColorInt()
    private val blueStroke = "#0288D1".toColorInt()
    private val strokeWidthDp = 6

    private enum class Symbol {
        EMPTY, LEFT, RIGHT, BOTH, DOUBLE_LEFT, DOUBLE_RIGHT, SWAP, TARGET, BLOCK
    }
    private data class Circle(
        val id: Int,
        val view: ImageView,
        val color: Int,
        val symbol: Symbol
    )
    private data class CircleState(
        val id: Int,
        val color: Int,
        val symbol: Symbol
    ) : Serializable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_symbol_race)

        supportActionBar?.hide()

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

        if (isUserLoggedIn()) {
            val uid = auth.currentUser!!.uid
            db.getReference("users").child(uid).child("categories")
                .child(GameKeys.CATEGORY_COORDINATION).child(GameKeys.GAME_SYMBOL_RACE)
                .child("bestStars").get().addOnSuccessListener { snapshot ->
                    currentBestScore = snapshot.getValue(Int::class.java) ?: 0
                }
        }

        timerProgressBar.setTotalTime(BASE_TIME_SECONDS)
        timerProgressBar.setOnFinishCallback {
            handleGameOver()
        }

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
                gameStatsManager.startReactionTracking()
                gameStatsManager.setGameStartTime()
                starManager.reset()
                startNewGame()
                timerProgressBar.start()
            }
        )

        pauseMenu = PauseMenu(
            context = this,
            pauseOverlay = pauseOverlay,
            pauseButton = pauseButton,
            onRestart = {
                if (pauseMenu.isPaused) pauseMenu.resume()

                timerProgressBar.stop()
                timerProgressBar.reset()

                trackLine.visibility = View.GONE
                blueContainer.visibility = View.INVISIBLE
                redContainer.visibility = View.INVISIBLE
                tempoInfoText.visibility = View.GONE
                pauseOverlay.visibility = View.GONE

                countdownManager.startCountdown()
                updateTempoDisplay()
            },
            onResume = {
                if (isGameRunning && !isProcessing) {
                    timerProgressBar.start()
                    startAutoShift()
                    updateTempoDisplay()
                    if (circleQueue.isNotEmpty()) startActiveTimer()
                }
                onGameResumed()
            },
            onPause = {
                timerProgressBar.pause()
                onGamePaused()
            },
            onExit = {
                handleGameOver()
            },
            instructionTitle = getString(R.string.instructions),
            instructionMessage = getString(R.string.symbol_race_instruction)
        )

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

        updateLayoutForOrientation()

        if (savedInstanceState == null) {
            trackLine.visibility = View.GONE
            blueContainer.visibility = View.INVISIBLE
            redContainer.visibility = View.INVISIBLE
            tempoInfoText.visibility = View.GONE
            countdownManager.startCountdown()
        } else {
            restoreGameState(savedInstanceState)
        }
    }

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

        val queueToSave = ArrayList(circleQueue.map {
            CircleState(it.id, it.color, it.symbol)
        })
        outState.putSerializable("circleQueueState", queueToSave)

        saveGameStats(outState)
    }

    private fun restoreGameState(savedInstanceState: Bundle) {
        pauseOverlay.visibility = savedInstanceState.getInt("pauseOverlayVisibility")
        countdownText.visibility = savedInstanceState.getInt("countdownTextVisibility")
        trackLine.visibility = savedInstanceState.getInt("trackLineVisibility")
        blueContainer.visibility = savedInstanceState.getInt("blueContainerVisibility")
        redContainer.visibility = savedInstanceState.getInt("redContainerVisibility")
        tempoInfoText.visibility = savedInstanceState.getInt("tempoInfoTextVisibility")

        val timerRemainingMs = savedInstanceState.getLong("timerRemainingTimeMs", BASE_TIME_SECONDS * 1000L)
        val timerRunning = savedInstanceState.getBoolean("timerIsRunning", false)
        timerProgressBar.setRemainingTimeMs(timerRemainingMs.coerceAtLeast(1L))

        val countdownInProgress = savedInstanceState.getBoolean("countdownInProgress", false)
        val countdownIndex = savedInstanceState.getInt("countdownIndex", 0)

        currentReactionTimeMs = savedInstanceState.getLong("currentReactionTimeMs", 3500L).coerceAtLeast(MIN_REACTION_TIME_MS)
        successfulStreak = savedInstanceState.getInt("successfulStreak", 0)
        totalMoves = savedInstanceState.getInt("totalMoves", 0)
        isGameRunning = savedInstanceState.getBoolean("isGameRunning", false)
        nextCircleId = savedInstanceState.getInt("nextCircleId", 1)

        isProcessing = false
        lastTapTime = 0
        lastTapSide = null
        clearAwaitingDouble()

        starManager.restoreState(savedInstanceState)
        pauseMenu.syncWithOverlay()
        updateTempoDisplay()

        if (countdownInProgress) {
            countdownManager.startCountdown(countdownIndex)
            return
        }

        @Suppress("UNCHECKED_CAST")
        val savedQueue = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // API 33+
            savedInstanceState.getSerializable("circleQueueState", ArrayList::class.java)
        } else {
            @Suppress("DEPRECATION")
            savedInstanceState.getSerializable("circleQueueState")
        } as? ArrayList<CircleState>

        if (savedQueue != null) {
            circleQueue.clear()
            trackContainer.removeAllViews()

            for (state in savedQueue) {
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

                val circle = Circle(state.id, view, state.color, state.symbol)

                circleQueue.add(circle)
                trackContainer.addView(view)

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

        if (timerRunning && pauseOverlay.visibility != View.VISIBLE && !isProcessing) {
            timerProgressBar.start()
        }

        trackContainer.post {
            if (isGameRunning) {
                adjustCircleQueueToView()
                startAutoShift()
                if (circleQueue.isNotEmpty()) startActiveTimer()
            }
        }
        restoreGameStats(savedInstanceState)
    }

    private fun updateLayoutForOrientation() {
        val currentConfig = resources.configuration
        val constraintLayout = findViewById<ConstraintLayout>(R.id.rootLayout)
        val constraintSet = ConstraintSet()
        constraintSet.clone(constraintLayout)

        if (currentConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {

            currentCircleSizeDp = CIRCLE_SIZE_DP_LANDSCAPE
            currentVisibleCircles = VISIBLE_CIRCLES_LANDSCAPE

            val offscreenMargin = dpToPx(-30)
            val landscapeWidth = dpToPx(220)
            val landscapeHeight = dpToPx(260)

            constraintSet.constrainWidth(R.id.blueContainer, landscapeWidth)
            constraintSet.constrainHeight(R.id.blueContainer, landscapeHeight)
            constraintSet.clear(R.id.blueContainer, ConstraintSet.START)
            constraintSet.connect(R.id.blueContainer, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, offscreenMargin)

            constraintSet.constrainWidth(R.id.redContainer, landscapeWidth)
            constraintSet.constrainHeight(R.id.redContainer, landscapeHeight)
            constraintSet.clear(R.id.redContainer, ConstraintSet.END)
            constraintSet.connect(R.id.redContainer, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, offscreenMargin)

        } else {

            currentCircleSizeDp = CIRCLE_SIZE_DP
            currentVisibleCircles = VISIBLE_CIRCLES

            val originalMargin = dpToPx(-30)
            val originalWidth = dpToPx(120)
            val originalHeight = dpToPx(320)

            constraintSet.constrainWidth(R.id.blueContainer, originalWidth)
            constraintSet.constrainHeight(R.id.blueContainer, originalHeight)
            constraintSet.clear(R.id.blueContainer, ConstraintSet.START)
            constraintSet.connect(R.id.blueContainer, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, originalMargin)

            constraintSet.constrainWidth(R.id.redContainer, originalWidth)
            constraintSet.constrainHeight(R.id.redContainer, originalHeight)
            constraintSet.clear(R.id.redContainer, ConstraintSet.END)
            constraintSet.connect(R.id.redContainer, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, originalMargin)
        }

        constraintSet.applyTo(constraintLayout)

        if (isGameRunning && !countdownManager.isInProgress()) {
            updateCirclePositions()
        }
    }

    private fun startNewGame() {
        gameStatsManager.startReactionTracking()
        cancelAllDelayedActions()

        if (pauseMenu.isPaused) pauseMenu.resume()
        pauseOverlay.visibility = View.GONE

        timerProgressBar.stop()
        timerProgressBar.reset()

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

        trackLine.visibility = View.VISIBLE
        blueContainer.visibility = View.VISIBLE
        redContainer.visibility = View.VISIBLE
        tempoInfoText.visibility = View.VISIBLE

        repeat(currentVisibleCircles) { createCircle() }
        updateCirclePositions()
        startAutoShift()
        startActiveTimer()

        updateTempoDisplay()
    }

    private fun createCircle() {
        val isRed = Random.nextBoolean()
        val color = if (isRed) redColor else blueColor
        val symbol = Symbol.entries.random()

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
        circleQueue.add(0, circle)
        trackContainer.addView(view)

        view.setOnClickListener {
            if (isGameRunning && !isProcessing && circleQueue.isNotEmpty() && circleQueue.last() == circle) {
                when (symbol) {
                    Symbol.TARGET -> animateInstantSuccess(circle)
                    else -> animateFailure(circle)
                }
            }
        }

        if (circleQueue.last() == circle) {
            lastTapTime = 0
            lastTapSide = null
            startActiveTimer()
        }
    }

    private fun handleTouch(isBlue: Boolean, event: MotionEvent) {
        if (!isGameRunning || circleQueue.isEmpty() || isProcessing || pauseMenu.isPaused) return
        if (event.action != MotionEvent.ACTION_DOWN) return

        val bottomCircle = circleQueue.last()
        val symbol = bottomCircle.symbol
        val currentTime = System.currentTimeMillis()

        if (symbol == Symbol.BOTH) {
            if (lastTapTime > 0 && (currentTime - lastTapTime) <= bothTapWindowMs && lastTapSide != isBlue) {
                lastTapTime = 0
                lastTapSide = null
                animateBoth(bottomCircle)
                return
            } else {
                lastTapTime = currentTime
                lastTapSide = isBlue
                runDelayed(bothTapWindowMs + 50) {
                    if (lastTapTime == currentTime && circleQueue.isNotEmpty() && circleQueue.last() == bottomCircle && !isProcessing) {
                        animateFailure(bottomCircle)
                    }
                }
                return
            }
        } else {
            handleContainerPress(isBlue)
        }
    }

    private fun handleContainerPress(isBlue: Boolean) {
        if (!isGameRunning || circleQueue.isEmpty() || isProcessing) return
        val bottomCircle = circleQueue.last()
        val color = bottomCircle.color
        val symbol = bottomCircle.symbol
        val isRedCircle = color == redColor

        if (awaitingDoubleClick && awaitingDoubleForId != bottomCircle.id) {
            clearAwaitingDouble()
        }

        var correct = false

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

    private fun animateMoveToSide(circle: Circle, toBlue: Boolean) {
        if (isProcessing) return
        isProcessing = true

        val view = circle.view
        val targetX = if (toBlue)
            blueContainer.x + blueContainer.width / 2 - view.width / 2
        else
            redContainer.x + redContainer.width / 2 - view.width / 2

        view.animate()
            .x(targetX)
            .setDuration(ANIMATION_DURATION_MS)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction { removeCircleAfterAnimation(circle, true) }
            .start()
    }

    private fun animateBoth(circle: Circle) {
        if (isProcessing) return
        isProcessing = true

        val view = circle.view
        val centerX = view.x
        val leftX = blueContainer.x + blueContainer.width / 2 - view.width / 2
        val rightX = redContainer.x + redContainer.width / 2 - view.width / 2

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

    private fun removeCircleAfterAnimation(removedCircle: Circle, success: Boolean) {
        trackContainer.removeView(removedCircle.view)
        circleQueue.remove(removedCircle)

        if (success) {
            starManager.increment()
            handleCorrectMove()
        } else {
            handleError()
        }

        if (awaitingDoubleForId == removedCircle.id) clearAwaitingDouble()

        createCircle()
        updateCirclePositions()
        startActiveTimer()
        isProcessing = false
    }

    private fun handleCorrectMove() {
        successfulStreak++
        totalMoves++
        checkComboBonus()
        accelerateIfNeeded()
        updateTempoDisplay()

        gameStatsManager.registerAttempt(true)
    }

    private fun handleError() {
        timerProgressBar.subtractTime(6)
        successfulStreak = 0
        totalMoves++
        accelerateIfNeeded()
        updateTempoDisplay()

        gameStatsManager.registerAttempt(false)
    }

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

    //!
    private fun addTime(seconds: Int) {
        if (seconds > 0) {
            timerProgressBar.addTime(seconds)
            showComboToast("+$seconds s!")
        }
    }

    private fun showComboToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun accelerateIfNeeded() {
        if (totalMoves % MOVES_PER_SPEEDUP == 0) {
            currentReactionTimeMs = (currentReactionTimeMs - SPEEDUP_STEP_MS).coerceAtLeast(MIN_REACTION_TIME_MS)
            updateTempoDisplay()
        }
    }

    private fun updateTempoDisplay() {
        val tempoSec = currentReactionTimeMs / 1000.0
        val comboText = if (successfulStreak > 0) "x$successfulStreak" else ""
        tempoInfoText.text = getString(R.string.tempo_display, comboText, tempoSec)
    }

    private fun startActiveTimer() {
        if (circleQueue.isEmpty() || !isGameRunning) return
        val bottomCircle = circleQueue.last()

        if (bottomCircle.symbol == Symbol.BLOCK) {
            runDelayed(BLOCK_DELAY_MS) {
                if (circleQueue.isNotEmpty() && circleQueue.last() == bottomCircle && !isProcessing) {
                    animateInstantSuccess(bottomCircle)
                }
            }
            return
        }

        runDelayed(currentReactionTimeMs) {
            if (circleQueue.isNotEmpty() && circleQueue.last() == bottomCircle && !isProcessing) {
                animateFailure(bottomCircle)
            }
        }
    }

    private fun beginAwaitingDouble(forId: Int, side: Int) {
        awaitingDoubleClick = true
        awaitingDoubleSide = side
        awaitingDoubleForId = forId

        runDelayed(600L) {
            if (circleQueue.isNotEmpty() && circleQueue.last().id == forId && !isProcessing) {
                animateFailure(circleQueue.last())
            }
            clearAwaitingDouble()
        }
    }

    private fun clearAwaitingDouble() {
        awaitingDoubleClick = false
        awaitingDoubleSide = null
        awaitingDoubleForId = null
    }

    private fun startAutoShift() {
        if (!isGameRunning) return

        fun scheduleNext() {
            runDelayed(currentReactionTimeMs) {
                if (isGameRunning && !isProcessing && circleQueue.size >= currentVisibleCircles) {
                    scheduleNext()
                } else {
                    runDelayed(100L) { scheduleNext() }
                }
            }
        }
        runDelayed(currentReactionTimeMs) { scheduleNext() }
    }

    private fun updateCirclePositions() {
        val trackHeight = trackContainer.height
        if (trackHeight == 0) {
            trackContainer.post { updateCirclePositions() }
            return
        }

        val circleSize = dpToPx(currentCircleSizeDp).toFloat()

        val startY: Float
        val spacing: Float

        if (currentVisibleCircles == VISIBLE_CIRCLES) {
            // TRYB PIONOWY
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
            // TRYB POZIOMY
            val topMargin = dpToPx(2).toFloat()
            val bottomMargin = dpToPx(80).toFloat()
            spacing = dpToPx(8).toFloat()

            val totalHeightOfCirclesAndGaps = (currentVisibleCircles * circleSize) +
                    ((currentVisibleCircles - 1).coerceAtLeast(0) * spacing)

            val topYOfTopCircle = (trackHeight - bottomMargin) - totalHeightOfCirclesAndGaps
            startY = topYOfTopCircle.coerceAtLeast(topMargin)
        }

        circleQueue.forEachIndexed { index, circle ->
            if (index < currentVisibleCircles) {
                val y = startY + (circleSize + spacing) * index
                circle.view.y = y
                circle.view.visibility = View.VISIBLE
                circle.view.elevation = 8f
            } else {
                circle.view.visibility = View.GONE
            }
        }
    }

    private fun adjustCircleQueueToView() {
        if (!isGameRunning) return

        val needed = currentVisibleCircles
        val current = circleQueue.size

        if (current < needed) {
            val difference = needed - current
            repeat(difference) {
                createCircle()
            }
        } else if (current > needed) {
            val difference = current - needed
            repeat(difference) {
                if (circleQueue.isNotEmpty()) {
                    val circleToRemove = circleQueue.first()
                    circleQueue.remove(circleToRemove)
                    trackContainer.removeView(circleToRemove.view)
                }
            }
        }

        updateCirclePositions()
    }

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

    private fun createCircleBackground(color: Int): Drawable {
        val strokeColor = if (color == redColor) redStroke else blueStroke
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
            setStroke(dpToPx(strokeWidthDp), strokeColor)
        }
    }

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()

    private fun runDelayed(delay: Long, action: () -> Unit) {
        val activityRef = java.lang.ref.WeakReference(this)
        var remaining = delay
        val interval = 16L // ~60 fps

        val runnable = object : Runnable {
            override fun run() {
                val activity = activityRef.get() ?: return

                if (activity.isFinishing || activity.isDestroyed) return

                if (pauseMenu.isPaused) {
                    handler.postDelayed(this, interval)
                    return
                }

                remaining -= interval
                if (remaining <= 0) {
                    activity.runOnUiThread { action() }
                } else {
                    handler.postDelayed(this, interval)
                }
            }
        }
        handler.postDelayed(runnable, interval)
    }

    private fun cancelAllDelayedActions() {
        handler.removeCallbacksAndMessages(null)
    }

    private fun handleGameOver() {
        isGameEnding = true
        isGameRunning = false
        trackContainer.isEnabled = false
        blueContainer.isEnabled = false
        redContainer.isEnabled = false
        pauseOverlay.visibility = View.GONE

        circleQueue.clear()
        trackContainer.removeAllViews()

        showGameOverDialog(
            categoryKey = GameKeys.CATEGORY_COORDINATION,
            gameKey = GameKeys.GAME_SYMBOL_RACE,
            starManager = starManager,
            timerProgressBar = timerProgressBar,
            countdownManager = countdownManager,
            currentBestScore = currentBestScore,
            onRestartAction = {
                if (starManager.starCount > currentBestScore) {
                    currentBestScore = starManager.starCount
                }
                currentReactionTimeMs = 3500L
                successfulStreak = 0
                totalMoves = 0
                nextCircleId = 1

                trackContainer.isEnabled = true
                blueContainer.isEnabled = true
                redContainer.isEnabled = true

                startNewGame()
            }
        )
    }

    override fun onPause() {
        super.onPause()
        if (!isGameEnding && !pauseMenu.isPaused && !isChangingConfigurations) {
            pauseMenu.pause()
            timerProgressBar.pause()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        timerProgressBar.stop()
        countdownManager.cancel()
        handler.removeCallbacksAndMessages(null)
    }
}