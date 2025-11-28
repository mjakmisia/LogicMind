package com.example.logicmind.activities

import android.animation.ValueAnimator
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
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

class RoadDashActivity : BaseActivity() {

    // Widoki
    private lateinit var gameContainer: LinearLayout
    private lateinit var leftLaneContainer: FrameLayout
    private lateinit var rightLaneContainer: FrameLayout
    private lateinit var countdownText: TextView
    private lateinit var pauseButton: ImageButton
    private lateinit var pauseOverlay: ConstraintLayout
    private lateinit var timerProgressBar: GameTimerProgressBar

    // Samochody
    private lateinit var carLeft: ImageView
    private lateinit var carRight: ImageView

    // Elementy Drogi
    private lateinit var roadLeft1: ImageView
    private lateinit var roadLeft2: ImageView
    private lateinit var roadRight1: ImageView
    private lateinit var roadRight2: ImageView

    // Logika
    private lateinit var starManager: StarManager
    private lateinit var pauseMenu: PauseMenu
    private lateinit var countdownManager: GameCountdownManager

    private var roadAnimator: ValueAnimator? = null
    private var isGameEnding = false
    private var currentLevel = 1

    // Sterowanie
    private var isLeftCarOnLeft = true
    private var isRightCarOnLeft = false
    private var laneOffset = 0f

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

        // Czekamy na narysowanie layoutu, by pobrać wymiary
        gameContainer.post {
            calculateDimensions()
            resetCarsPosition()

            // Ustawiamy drugie kawałki drogi NAD ekranem przed startem
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
        // Obliczamy o ile przesunąć auto (ćwierć szerokości pasa)
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
                stopGameLoop()
                isGameEnding = true
                Toast.makeText(this, "Czas minął!", Toast.LENGTH_LONG).show()
                gameContainer.isEnabled = false
                pauseOverlay.visibility = View.GONE
                finish()
            }
        }

        countdownManager = GameCountdownManager(
            countdownText = countdownText,
            gameView = gameContainer,
            viewsToHide = listOf(pauseButton, findViewById(R.id.starCountText), findViewById(R.id.starIcon), timerProgressBar),
            onCountdownFinished = {
                currentLevel = 1
                starManager.reset()
                timerProgressBar.reset()
                timerProgressBar.start()
                startRoadAnimation()
            }
        )

        pauseMenu = PauseMenu(
            context = this,
            pauseOverlay = pauseOverlay,
            pauseButton = pauseButton,
            onRestart = {
                if (pauseMenu.isPaused) pauseMenu.resume()
                stopGameLoop()
                resetCarsPosition()
                currentLevel = 1
                starManager.reset()
                timerProgressBar.reset()
                countdownManager.startCountdown()
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
            onExit = { finish() },
            instructionTitle = getString(R.string.instructions),
            instructionMessage = getString(R.string.road_dash_instruction),
        )
    }

    private fun startRoadAnimation() {
        val height = gameContainer.height.toFloat()
        if (height == 0f) {
            gameContainer.post { startRoadAnimation() }
            return
        }

        roadAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1500L
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener { animation ->
                val progress = animation.animatedValue as Float
                val translation = height * progress

                moveRoadSegment(roadLeft1, roadLeft2, translation, height)
                moveRoadSegment(roadRight1, roadRight2, translation, height)
            }
            start()
        }
    }

    private fun moveRoadSegment(view1: View, view2: View, translation: Float, height: Float) {
        var newY1 = translation
        var newY2 = translation - height

        // Resetowanie pozycji (zapętlanie)
        if (newY1 > height) newY1 -= 2 * height
        if (newY2 > height) newY2 -= 2 * height

        view1.translationY = newY1
        view2.translationY = newY2
    }

    private fun stopGameLoop() {
        roadAnimator?.cancel()
        roadAnimator = null
    }

    private fun pauseGameLoop() {
        roadAnimator?.pause()
    }

    private fun resumeGameLoop() {
        roadAnimator?.resume()
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
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        // Uwaga: super.onRestoreInstanceState przywraca widoki, ale my robimy to ręcznie
        // poniżej, więc nie wywołujemy super, albo wywołujemy na końcu.
        // Tutaj używam sprawdzonego wzorca z Twoich innych gier:
    }

    // Zamiast onRestoreInstanceState używamy logiki w onCreate z savedInstanceState
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

        if (timerIsRunning && pauseOverlay.visibility != View.VISIBLE) {
            timerProgressBar.start()
            gameContainer.post { startRoadAnimation() }
        }

        gameContainer.post {
            calculateDimensions()
            updateCarPosition(carLeft, isLeftCarOnLeft, false)
            updateCarPosition(carRight, isRightCarOnLeft, false)
        }

        val countdownIndex = savedInstanceState.getInt("countdownIndex", 0)
        val countdownInProgress = savedInstanceState.getBoolean("countdownInProgress", false)
        if (countdownInProgress) {
            countdownManager.startCountdown(countdownIndex)
        }

        pauseMenu.syncWithOverlay()
    }

    override fun onPause() {
        super.onPause()
        if (!pauseMenu.isPaused && !isChangingConfigurations) {
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