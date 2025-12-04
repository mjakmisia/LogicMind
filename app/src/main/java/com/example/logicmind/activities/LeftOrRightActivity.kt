package com.example.logicmind.activities
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintLayout.LayoutParams as ConstraintLayoutParams
import com.example.logicmind.R
import com.example.logicmind.common.GameCountdownManager
import com.example.logicmind.common.GameTimerProgressBar
import com.example.logicmind.common.PauseMenu
import com.example.logicmind.common.StarManager
import androidx.core.view.children
import androidx.core.view.isEmpty
import androidx.core.view.isVisible

class LeftOrRightActivity : BaseActivity() {
    private lateinit var countdownText: TextView
    private lateinit var pauseButton: ImageButton
    private lateinit var pauseOverlay: ConstraintLayout
    private lateinit var timerProgressBar: GameTimerProgressBar
    private lateinit var starManager: StarManager
    private lateinit var pauseMenu: PauseMenu
    private lateinit var countdownManager: GameCountdownManager
    private var isGameEnding = false
    private var isProcessingMove = false
    private var currentLevel = 1
    private lateinit var gameContainer: ConstraintLayout
    private lateinit var fruitQueueContainer: LinearLayout
    private lateinit var leftBasket: ImageView
    private lateinit var rightBasket: ImageView
    private var currentBestScore = 0
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
    private val fruitQueue = mutableListOf<Int>()
    private val leftBasketTargets = mutableListOf<Int>()
    private val rightBasketTargets = mutableListOf<Int>()
    private val activeGameFruits = mutableListOf<Int>()
    private lateinit var leftBasketTargetContainer: LinearLayout
    private lateinit var rightBasketTargetContainer: LinearLayout
    private lateinit var leftBasketTargetTopRow: LinearLayout
    private lateinit var leftBasketTargetBottomRow: LinearLayout
    private lateinit var rightBasketTargetTopRow: LinearLayout
    private lateinit var rightBasketTargetBottomRow: LinearLayout
    private var fruitSizePx: Int = 0
    private var overlapMarginPx: Int = 0
    private data class LevelConfig(
        val leftTargets: Int,
        val rightTargets: Int,
        val totalQueue: Int,
        val timeBonus: Int
    )
    private var fruitsSpawnedTotal = 0
    private var fruitsToSpawnLimit = 0
    private val maxVisibleFruits = 6

    companion object {
        private const val BASE_TIME_SECONDS = 90
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_left_or_right)
        supportActionBar?.hide()

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
        leftBasketTargetTopRow = findViewById(R.id.leftBasketTargetTopRow)
        leftBasketTargetBottomRow = findViewById(R.id.leftBasketTargetBottomRow)
        rightBasketTargetTopRow = findViewById(R.id.rightBasketTargetTopRow)
        rightBasketTargetBottomRow = findViewById(R.id.rightBasketTargetBottomRow)

        if (isUserLoggedIn()) {
            val uid = auth.currentUser!!.uid
            db.getReference("users")
                .child(uid)
                .child("categories")
                .child(GameKeys.CATEGORY_FOCUS)
                .child(GameKeys.GAME_LEFT_OR_RIGHT)
                .child("bestStars")
                .get()
                .addOnSuccessListener { snapshot ->
                    currentBestScore = snapshot.getValue(Int::class.java) ?: 0
                }
        }

        updateLayoutForOrientation()

        leftBasket.setOnClickListener {
            handleBasketClick(isLeftBasket = true)
        }

        rightBasket.setOnClickListener {
            handleBasketClick(isLeftBasket = false)
        }

        timerProgressBar.setTotalTime(BASE_TIME_SECONDS)
        timerProgressBar.setOnFinishCallback {
            runOnUiThread {
                handleGameOver()
            }
        }

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
            instructionMessage = getString(R.string.left_or_right_instruction),
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
        outState.putInt("gameContainerVisibility", gameContainer.visibility)
        outState.putInt("pauseButtonVisibility", pauseButton.visibility)
        outState.putLong("timerRemainingTimeMs", timerProgressBar.getRemainingTimeSeconds() * 1000L)
        outState.putBoolean("timerIsRunning", timerProgressBar.isRunning())
        outState.putInt("countdownIndex", countdownManager.getIndex())
        outState.putBoolean("countdownInProgress", countdownManager.isInProgress())
        outState.putInt("currentLevel", currentLevel)
        outState.putInt("fruitsSpawnedTotal", fruitsSpawnedTotal)
        outState.putInt("fruitsToSpawnLimit", fruitsToSpawnLimit)
        outState.putInt("currentBestScore", currentBestScore)
        outState.putIntegerArrayList("fruitQueue", ArrayList(fruitQueue))
        outState.putIntegerArrayList("leftBasketTargets", ArrayList(leftBasketTargets))
        outState.putIntegerArrayList("rightBasketTargets", ArrayList(rightBasketTargets))
        outState.putIntegerArrayList("activeGameFruits", ArrayList(activeGameFruits))
        starManager.saveState(outState)
    }

    private fun restoreGameState(savedInstanceState: Bundle) {
        pauseOverlay.visibility = savedInstanceState.getInt("pauseOverlayVisibility", View.GONE)
        countdownText.visibility = savedInstanceState.getInt("countdownTextVisibility", View.GONE)
        gameContainer.visibility = savedInstanceState.getInt("gameContainerVisibility", View.VISIBLE)
        pauseButton.visibility = savedInstanceState.getInt("pauseButtonVisibility", View.VISIBLE)
        currentLevel = savedInstanceState.getInt("currentLevel", 1)
        fruitsSpawnedTotal = savedInstanceState.getInt("fruitsSpawnedTotal", 0)
        fruitsToSpawnLimit = savedInstanceState.getInt("fruitsToSpawnLimit", 0)
        currentBestScore = savedInstanceState.getInt("currentBestScore", 0)
        starManager.restoreState(savedInstanceState)

        val timerRemainingTimeMs = savedInstanceState.getLong("timerRemainingTimeMs", BASE_TIME_SECONDS * 1000L)
        val timerIsRunning = savedInstanceState.getBoolean("timerIsRunning", false)
        timerProgressBar.setRemainingTimeMs(timerRemainingTimeMs.coerceAtLeast(1L))

        if (timerIsRunning && pauseOverlay.visibility != View.VISIBLE) {
            timerProgressBar.start()
        }

        val countdownIndex = savedInstanceState.getInt("countdownIndex", 0)
        val countdownInProgress = savedInstanceState.getBoolean("countdownInProgress", false)
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

        if (gameContainer.isVisible && !countdownInProgress) {
            displayBasketTargets()
            displayFruitQueue()
        }
    }

    private fun updateLayoutForOrientation() {
        val isLandscape = resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

        if (isLandscape) {
            fruitSizePx = (70 * resources.displayMetrics.density).toInt()
            overlapMarginPx = -(35 * resources.displayMetrics.density).toInt()
        } else {
            fruitSizePx = (120 * resources.displayMetrics.density).toInt()
            overlapMarginPx = -(60 * resources.displayMetrics.density).toInt()
        }

        if (isLandscape) {
            val queueParams = fruitQueueContainer.layoutParams as ConstraintLayoutParams
            queueParams.topMargin = 0
            queueParams.verticalBias = 0.15f
            fruitQueueContainer.layoutParams = queueParams

            val smallBottomMargin = (4 * resources.displayMetrics.density).toInt()

            val leftParams = leftBasket.layoutParams as ConstraintLayoutParams
            leftParams.bottomMargin = smallBottomMargin
            leftBasket.layoutParams = leftParams

            val rightParams = rightBasket.layoutParams as ConstraintLayoutParams
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
        isGameEnding = false
        gameContainer.isEnabled = true
        leftBasket.isEnabled = true
        rightBasket.isEnabled = true

        fruitQueue.clear()
        fruitQueueContainer.removeAllViews()

        val config = getLevelConfig(currentLevel)

        fruitsToSpawnLimit = config.totalQueue
        fruitsSpawnedTotal = 0

        setupBaskets(config.leftTargets, config.rightTargets)
        displayBasketTargets()

        val initialFruits = minOf(maxVisibleFruits, fruitsToSpawnLimit)
        generateFruitQueue(initialFruits)

        fruitsSpawnedTotal = initialFruits

        displayFruitQueue()
    }

    private fun generateFruitQueue(count: Int) {
        if (activeGameFruits.isEmpty()) return

        repeat(count) {
            fruitQueue.add(activeGameFruits.random())
        }
    }

    private fun setupBaskets(leftCount: Int, rightCount: Int) {
        leftBasketTargets.clear()
        rightBasketTargets.clear()
        activeGameFruits.clear()

        val shuffledFruits = fruitDrawables.shuffled()

        val leftTargets = shuffledFruits.take(leftCount)
        val rightTargets = shuffledFruits.drop(leftCount).take(rightCount)

        leftBasketTargets.addAll(leftTargets)
        rightBasketTargets.addAll(rightTargets)

        activeGameFruits.addAll(leftBasketTargets)
        activeGameFruits.addAll(rightBasketTargets)
        activeGameFruits.distinct()
    }

    private fun displayBasketTargets() {
        val targetsSize = leftBasketTargets.size.coerceAtLeast(rightBasketTargets.size)

        val dynamicBias = when (targetsSize) {
            1, 2 -> 0.5f
            else -> 0.55f
        }

        val leftContainer = findViewById<LinearLayout>(R.id.leftBasketTargetContainer)
        val rightContainer = findViewById<LinearLayout>(R.id.rightBasketTargetContainer)

        (leftContainer.layoutParams as? ConstraintLayoutParams)?.let { params ->
            params.verticalBias = dynamicBias
            leftContainer.layoutParams = params
        }

        (rightContainer.layoutParams as? ConstraintLayoutParams)?.let { params ->
            params.verticalBias = dynamicBias
            rightContainer.layoutParams = params
        }

        val leftTopRow = leftContainer.findViewById<LinearLayout>(R.id.leftBasketTargetTopRow)
        val leftBottomRow = leftContainer.findViewById<LinearLayout>(R.id.leftBasketTargetBottomRow)
        val rightTopRow = rightContainer.findViewById<LinearLayout>(R.id.rightBasketTargetTopRow)
        val rightBottomRow = rightContainer.findViewById<LinearLayout>(R.id.rightBasketTargetBottomRow)

        leftTopRow.removeAllViews()
        leftBottomRow.removeAllViews()
        rightTopRow.removeAllViews()
        rightBottomRow.removeAllViews()

        leftBasketTargets.forEachIndexed { index, fruitId ->
            val icon = createTargetIcon(fruitId)
            when (index) {
                0, 1 -> leftTopRow.addView(icon)
                2 -> leftBottomRow.addView(icon)
            }
        }

        rightBasketTargets.forEachIndexed { index, fruitId ->
            val icon = createTargetIcon(fruitId)
            when (index) {
                0, 1 -> rightTopRow.addView(icon)
                2 -> rightBottomRow.addView(icon)
            }
        }
    }

    private fun createTargetIcon(fruitId: Int): ImageView {
        val iconSizePx = (64 * resources.displayMetrics.density).toInt()
        val iconMarginPx = (1 * resources.displayMetrics.density).toInt()
        val paddingPx = (12 * resources.displayMetrics.density).toInt()

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

    private fun handleBasketClick(isLeftBasket: Boolean) {
        if (fruitQueue.isEmpty() || isProcessingMove) return
        isProcessingMove = true

        val currentFruit = fruitQueue.first()

        val targetList = if (isLeftBasket) leftBasketTargets else rightBasketTargets
        val isCorrect = targetList.contains(currentFruit)

        val targetBasketView = if (isLeftBasket) leftBasket else rightBasket

        advanceToNextFruit(isCorrect, targetBasketView)
    }

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

        val viewToAnimate = fruitQueueContainer.getChildAt(fruitQueueContainer.childCount - 1)

        animateFruitToBasket(viewToAnimate, targetBasket) {}

        if (fruitQueue.isNotEmpty()) fruitQueue.removeAt(0)
        fruitQueueContainer.removeView(viewToAnimate)

        if (activeGameFruits.isNotEmpty() && fruitsSpawnedTotal < fruitsToSpawnLimit) {
            val newFruitId = activeGameFruits.random()
            fruitQueue.add(newFruitId)
            fruitsSpawnedTotal++

            val newFruitView = createFruitView(newFruitId)
            newFruitView.alpha = 0f
            fruitQueueContainer.addView(newFruitView, 0)

            newFruitView.animate().alpha(1f).setDuration(200).start()
        }

        android.transition.TransitionManager.beginDelayedTransition(fruitQueueContainer)
        updateQueueVisuals()

        if (fruitQueueContainer.isEmpty()) {
            val config = getLevelConfig(currentLevel)
            if (config.timeBonus > 0) {
                timerProgressBar.addTime(config.timeBonus)
            }

            currentLevel++

            gameContainer.postDelayed({
                startNewGame()
            }, 1000)
        }
        isProcessingMove = false
    }

    private fun createFruitView(fruitId: Int): ImageView {
        return ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                fruitSizePx,
                fruitSizePx
            )
            setImageResource(fruitId)
        }
    }

    private fun displayFruitQueue() {
        fruitQueueContainer.removeAllViews()

        fruitQueue.reversed().forEach { fruitId ->
            fruitQueueContainer.addView(createFruitView(fruitId))
        }
        updateQueueVisuals()
    }

    private fun updateQueueVisuals() {
        val minScale = 0.6f
        val maxScale = 1.0f
        val scaleRange = maxScale - minScale
        val totalItems = fruitQueueContainer.childCount

        val maxIndex = if (totalItems > 1) totalItems - 1 else 1

        fruitQueueContainer.children.forEachIndexed { index, view ->
            val percentage = if (totalItems == 1) 1.0f else index.toFloat() / maxIndex.toFloat()
            val scale = minScale + (percentage * scaleRange)

            val newSize = (fruitSizePx * scale).toInt()

            view.elevation = (index + 1).toFloat()

            (view.layoutParams as LinearLayout.LayoutParams).also {
                it.topMargin = if (index > 0) overlapMarginPx else 0
                it.width = newSize
                it.height = newSize
            }
        }
    }

    private fun animateFruitToBasket(fruitView: View, targetBasket: View, onAnimationEnd: () -> Unit) {
        val startLoc = IntArray(2)
        fruitView.getLocationOnScreen(startLoc)

        val endLoc = IntArray(2)
        targetBasket.getLocationOnScreen(endLoc)

        val parentLoc = IntArray(2)
        gameContainer.getLocationOnScreen(parentLoc)

        val startX = startLoc[0] - parentLoc[0]
        val startY = startLoc[1] - parentLoc[1]

        val flyingFruit = ImageView(this).apply {
            setImageDrawable((fruitView as ImageView).drawable)
            layoutParams = ConstraintLayout.LayoutParams(fruitView.width, fruitView.height)
            x = startX.toFloat()
            y = startY.toFloat()
            elevation = 20f
        }

        gameContainer.addView(flyingFruit)

        fruitView.visibility = View.INVISIBLE

        val targetX = (endLoc[0] - parentLoc[0]) + (targetBasket.width / 2) - (fruitView.width / 2)
        val targetY = (endLoc[1] - parentLoc[1]) + (targetBasket.height / 2) - (fruitView.height / 2)

        flyingFruit.animate()
            .x(targetX.toFloat())
            .y(targetY.toFloat())
            .rotation(360f)
            .scaleX(0.5f)
            .scaleY(0.5f)
            .alpha(0f)
            .setDuration(400)
            .withEndAction {
                gameContainer.removeView(flyingFruit)
                onAnimationEnd()
            }
            .start()
    }

    private fun handleGameOver() {
        if (isGameEnding) return
        isGameEnding = true

        gameContainer.isEnabled = false
        leftBasket.isEnabled = false
        rightBasket.isEnabled = false
        pauseOverlay.visibility = View.GONE

        showGameOverDialog(
            categoryKey = GameKeys.CATEGORY_FOCUS,
            gameKey = GameKeys.GAME_LEFT_OR_RIGHT,
            starManager = starManager,
            timerProgressBar = timerProgressBar,
            countdownManager = countdownManager,
            currentBestScore = currentBestScore,
            onRestartAction = {
                if (starManager.starCount > currentBestScore) {
                    currentBestScore = starManager.starCount
                }
                currentLevel = 1
                fruitsSpawnedTotal = 0
                fruitsToSpawnLimit = 0
                fruitQueue.clear()
                fruitQueueContainer.removeAllViews()
                startNewGame()
            }
        )
    }

    override fun onPause() {
        super.onPause()
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