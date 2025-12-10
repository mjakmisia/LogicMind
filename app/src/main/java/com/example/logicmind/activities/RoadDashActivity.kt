package com.example.logicmind.activities
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
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
import java.util.Random

class RoadDashActivity : BaseActivity() {

    private lateinit var gameContainer: LinearLayout
    private lateinit var leftLaneContainer: FrameLayout
    private lateinit var rightLaneContainer: FrameLayout
    private lateinit var countdownText: TextView
    private lateinit var pauseButton: ImageButton
    private lateinit var pauseOverlay: ConstraintLayout
    private lateinit var timerProgressBar: GameTimerProgressBar

    private lateinit var carLeft: ImageView
    private lateinit var carRight: ImageView
    private lateinit var roadLeft1: ImageView
    private lateinit var roadLeft2: ImageView
    private lateinit var roadRight1: ImageView
    private lateinit var roadRight2: ImageView

    private lateinit var starManager: StarManager
    private lateinit var pauseMenu: PauseMenu
    private lateinit var countdownManager: GameCountdownManager

    private var isGameEnding = false
    private var currentLevel = 1
    private val random = Random()

    private var isLeftCarOnLeft = true
    private var isRightCarOnLeft = false
    private var laneOffset = 0f

    private val handler = Handler(Looper.getMainLooper())
    private var gameSpeed = 15f
    private var spawnInterval = 1200L
    private var objectDuration = 2500L
    private val activeObjects = mutableListOf<View>()
    private var currentBestScore = 0

    companion object {
        private const val BASE_TIME_SECONDS = 90
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_road_dash)
        supportActionBar?.hide()

        initViews()
        setupGameLogic()
        setupCarControls()

        gameContainer.post {
            calculateDimensions()
            resetCarsPosition()

            val height = gameContainer.height.toFloat()
            roadLeft2.translationY = -height
            roadRight2.translationY = -height

            if (savedInstanceState == null) {
                countdownManager.startCountdown()
            }
        }

        if (savedInstanceState != null) {
            restoreGameState(savedInstanceState)
        }
    }

    private fun initViews() {
        gameContainer = findViewById(R.id.gameContainer)
        leftLaneContainer = findViewById(R.id.leftLaneContainer)
        rightLaneContainer = findViewById(R.id.rightLaneContainer)

        carLeft = findViewById(R.id.carLeft)
        carRight = findViewById(R.id.carRight)

        roadLeft1 = findViewById(R.id.roadLeft1)
        roadLeft2 = findViewById(R.id.roadLeft2)
        roadRight1 = findViewById(R.id.roadRight1)
        roadRight2 = findViewById(R.id.roadRight2)

        countdownText = findViewById(R.id.countdownText)
        pauseButton = findViewById(R.id.pauseButton)
        pauseOverlay = findViewById(R.id.pauseOverlay)
        timerProgressBar = findViewById(R.id.gameTimerProgressBar)

        starManager = StarManager()
        starManager.init(findViewById(R.id.starCountText))
    }

    private fun calculateDimensions() {
        val laneWidth = leftLaneContainer.width.toFloat()
        laneOffset = laneWidth / 4
    }

    private fun resetCarsPosition() {
        isLeftCarOnLeft = true
        isRightCarOnLeft = false
        updateCarPosition(carLeft, isLeftCarOnLeft, animate = false)
        updateCarPosition(carRight, isRightCarOnLeft, animate = false)
    }

    private fun setupCarControls() {
        leftLaneContainer.setOnClickListener {
            if (isGameRunning()) {
                isLeftCarOnLeft = !isLeftCarOnLeft
                updateCarPosition(carLeft, isLeftCarOnLeft, animate = true)
            }
        }
        rightLaneContainer.setOnClickListener {
            if (isGameRunning()) {
                isRightCarOnLeft = !isRightCarOnLeft
                updateCarPosition(carRight, isRightCarOnLeft, animate = true)
            }
        }
    }

    private fun isGameRunning(): Boolean {
        return !countdownManager.isInProgress() && !pauseMenu.isPaused && !isGameEnding
    }

    private fun updateCarPosition(car: ImageView, isLeft: Boolean, animate: Boolean) {
        val targetX = if (isLeft) -laneOffset else laneOffset
        if (animate) {
            car.animate()
                .translationX(targetX)
                .setDuration(150)
                .setInterpolator(DecelerateInterpolator())
                .start()
        } else {
            car.translationX = targetX
        }
    }

    private fun setupGameLogic() {
        timerProgressBar.setTotalTime(BASE_TIME_SECONDS)
        timerProgressBar.setOnFinishCallback {
            runOnUiThread {
                handleGameOver()
            }
        }

        countdownManager = GameCountdownManager(
            countdownText = countdownText,
            gameView = gameContainer,
            viewsToHide = listOf(pauseButton, findViewById(R.id.starCountText), findViewById(R.id.starIcon), timerProgressBar),
            onCountdownFinished = {
                resetGameVariables()
                timerProgressBar.start()
                gameStatsManager.startReactionTracking()
                gameStatsManager.setGameStartTime()
                startGameLoop()
                startSpawningObjects()
            }
        )

        pauseMenu = PauseMenu(
            context = this,
            pauseOverlay = pauseOverlay,
            pauseButton = pauseButton,
            onRestart = {
                if (pauseMenu.isPaused) pauseMenu.resume()
                restartGame()
            },
            onResume = {
                gameStatsManager.onGameResumed()
                timerProgressBar.start()
                resumeGameLoop()
            },
            onPause = {
                gameStatsManager.onGamePaused()
                timerProgressBar.pause()
                pauseGameLoop()
            },
            onExit = { handleGameOver() },
            instructionTitle = getString(R.string.instructions),
            instructionMessage = getString(R.string.road_dash_instruction),
        )
    }

    private fun resetGameVariables() {
        currentLevel = 1
        starManager.reset()
        timerProgressBar.reset()
        isGameEnding = false
        gameSpeed = 15f
        spawnInterval = 1200L
        removeAllObjects()
    }

    private fun restartGame() {
        stopGameLoop()
        resetCarsPosition()
        resetGameVariables()
        countdownManager.startCountdown()
    }

    private val gameLoopRunnable = object : Runnable {
        override fun run() {
            if (!isGameRunning()) return
            moveRoads()
            moveObjects()
            handler.postDelayed(this, 16)
        }
    }

    private fun startGameLoop() {
        handler.post(gameLoopRunnable)
    }

    private fun moveRoads() {
        val height = gameContainer.height.toFloat()
        moveSingleRoadPair(roadLeft1, roadLeft2, height)
        moveSingleRoadPair(roadRight1, roadRight2, height)
    }

    private fun moveSingleRoadPair(view1: View, view2: View, height: Float) {
        view1.translationY += gameSpeed
        view2.translationY += gameSpeed

        if (view1.translationY >= height) {
            view1.translationY = view2.translationY - height
        }
        if (view2.translationY >= height) {
            view2.translationY = view1.translationY - height
        }
    }

    private fun moveObjects() {
        val screenHeight = gameContainer.height.toFloat()
        val iterator = activeObjects.iterator()
        while (iterator.hasNext()) {
            val obj = iterator.next()
            obj.translationY += gameSpeed

            if (obj.isEnabled) {
                val data = obj.tag as ObjectData
                checkCollision(obj, data.laneIndex, data.type)
            }

            if (obj.translationY > screenHeight) {
                (obj.parent as? ViewGroup)?.removeView(obj)
                iterator.remove()
            }
        }
    }

    private val spawnRunnable = object : Runnable {
        override fun run() {
            if (!isGameRunning()) return
            spawnObject()
            handler.postDelayed(this, spawnInterval)
        }
    }

    private fun startSpawningObjects() {
        handler.post(spawnRunnable)
    }

    data class ObjectData(val type: String, val laneIndex: Int)

    private fun spawnObject() {
        val laneIndex = random.nextInt(4)
        val isStar = random.nextInt(4) != 0
        val type = if (isStar) "STAR" else "CONE"

        val parentContainer = if (laneIndex < 2) leftLaneContainer else rightLaneContainer
        val containerWidth = parentContainer.width.toFloat()
        val centerX = containerWidth / 2f

        val objectSize = 180
        val halfSize = objectSize / 2f

        val targetCenterX = centerX + (if (laneIndex % 2 == 0) -laneOffset else laneOffset)
        val finalTranslationX = targetCenterX - halfSize

        val objectView = ImageView(this).apply {
            layoutParams = FrameLayout.LayoutParams(objectSize, objectSize).apply {
                gravity = Gravity.TOP or Gravity.START
            }
            setImageResource(if (isStar) R.drawable.icon_star else R.drawable.traffic_cone)

            translationX = finalTranslationX
            translationY = -objectSize.toFloat()

            tag = ObjectData(type, laneIndex)
            isEnabled = true
        }

        parentContainer.addView(objectView)
        activeObjects.add(objectView)
    }

    private fun checkCollision(objectView: View, laneIndex: Int, type: String) {
        val carToCheck = if (laneIndex < 2) carLeft else carRight
        val carIsOnLeft = if (laneIndex < 2) isLeftCarOnLeft else isRightCarOnLeft
        val objectIsOnLeft = (laneIndex % 2 == 0)

        if (carIsOnLeft == objectIsOnLeft) {
            val carTopY = carToCheck.y
            val carBottomY = carTopY + carToCheck.height

            val objectBottomY = objectView.y + objectView.height
            val objectTopY = objectView.y

            val hitboxCarTop = carTopY + (carToCheck.height * 0.3f)
            val hitboxCarBottom = carBottomY - (carToCheck.height * 0.1f)

            if (objectBottomY > hitboxCarTop && objectTopY < hitboxCarBottom) {
                objectView.isEnabled = false
                handleCollision(objectView, type)
            }
        }
    }

    private fun handleCollision(view: View, type: String) {
        view.animate()
            .alpha(0f)
            .scaleX(0.5f)
            .scaleY(0.5f)
            .setDuration(200)
            .start()

        if (type == "STAR") {
            starManager.increment()
        } else {
            timerProgressBar.subtractTime(3)
            Toast.makeText(this, getString(R.string.mistake_penalty_toast), Toast.LENGTH_SHORT).show()
        }
    }

    private fun removeAllObjects() {
        activeObjects.clear()
        cleanContainer(leftLaneContainer)
        cleanContainer(rightLaneContainer)
    }

    private fun cleanContainer(container: FrameLayout) {
        val toRemove = mutableListOf<View>()
        for (i in 0 until container.childCount) {
            val child = container.getChildAt(i)
            if (child.tag is ObjectData) {
                toRemove.add(child)
            }
        }
        toRemove.forEach { container.removeView(it) }
    }

    private fun handleGameOver() {
        if (isGameEnding) return
        isGameEnding = true

        stopGameLoop()
        removeAllObjects()

        leftLaneContainer.isEnabled = false
        rightLaneContainer.isEnabled = false
        pauseOverlay.visibility = View.GONE

        showGameOverDialog(
            categoryKey = GameKeys.CATEGORY_COORDINATION,
            gameKey = GameKeys.GAME_ROAD_DASH,
            starManager = starManager,
            timerProgressBar = timerProgressBar,
            countdownManager = countdownManager,
            currentBestScore = currentBestScore,
            onRestartAction = {
                if (starManager.starCount > currentBestScore) {
                    currentBestScore = starManager.starCount
                }
                currentLevel = 1
                resetCarsPosition()
                resetGameVariables()

                leftLaneContainer.isEnabled = true
                rightLaneContainer.isEnabled = true

            }
        )
    }

    private fun stopGameLoop() {
        handler.removeCallbacks(spawnRunnable)
        handler.removeCallbacks(gameLoopRunnable)
    }

    private fun pauseGameLoop() {
        handler.removeCallbacks(spawnRunnable)
        handler.removeCallbacks(gameLoopRunnable)
    }

    private fun resumeGameLoop() {
        startSpawningObjects()
        startGameLoop()
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
        outState.putBoolean("isLeftCarOnLeft", isLeftCarOnLeft)
        outState.putBoolean("isRightCarOnLeft", isRightCarOnLeft)
        starManager.saveState(outState)
        saveGameStats(outState)
    }

    private fun restoreGameState(savedInstanceState: Bundle) {
        pauseOverlay.visibility = savedInstanceState.getInt("pauseOverlayVisibility", View.GONE)
        countdownText.visibility = savedInstanceState.getInt("countdownTextVisibility", View.GONE)
        gameContainer.visibility = savedInstanceState.getInt("gameContainerVisibility", View.VISIBLE)
        pauseButton.visibility = savedInstanceState.getInt("pauseButtonVisibility", View.VISIBLE)
        currentLevel = savedInstanceState.getInt("currentLevel", 1)

        isLeftCarOnLeft = savedInstanceState.getBoolean("isLeftCarOnLeft")
        isRightCarOnLeft = savedInstanceState.getBoolean("isRightCarOnLeft")

        starManager.restoreState(savedInstanceState)

        val timerRemainingTimeMs = savedInstanceState.getLong("timerRemainingTimeMs", BASE_TIME_SECONDS * 1000L)
        val timerIsRunning = savedInstanceState.getBoolean("timerIsRunning", false)
        timerProgressBar.setRemainingTimeMs(timerRemainingTimeMs.coerceAtLeast(1L))

        gameContainer.post {
            calculateDimensions()
            updateCarPosition(carLeft, isLeftCarOnLeft, false)
            updateCarPosition(carRight, isRightCarOnLeft, false)

            if (timerIsRunning && pauseOverlay.visibility != View.VISIBLE) {
                timerProgressBar.start()
                startGameLoop()
                startSpawningObjects()
            }
        }

        val countdownIndex = savedInstanceState.getInt("countdownIndex", 0)
        val countdownInProgress = savedInstanceState.getBoolean("countdownInProgress", false)
        if (countdownInProgress) {
            countdownManager.startCountdown(countdownIndex)
        }

        pauseMenu.syncWithOverlay()
        restoreGameStats(savedInstanceState)
    }

    override fun onPause() {
        super.onPause()
        if (!isGameEnding && !pauseMenu.isPaused && !isChangingConfigurations) {
            pauseMenu.pause()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopGameLoop()
        timerProgressBar.stop()
        countdownManager.cancel()
    }
}