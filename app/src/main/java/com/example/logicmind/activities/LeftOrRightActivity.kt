package com.example.logicmind.activities
import android.os.Bundle
import android.view.View
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
import com.example.logicmind.common.StarManager

class LeftOrRightActivity : BaseActivity() {

    private lateinit var gridLayout: GridLayout // Siatka do wyświetlania elementów
    private lateinit var countdownText: TextView // Pole tekstowe dla odliczania
    private lateinit var pauseButton: ImageButton // Przycisk pauzy
    private lateinit var pauseOverlay: ConstraintLayout // Nakładka menu pauzy
    private lateinit var timerProgressBar: GameTimerProgressBar // Pasek postępu czasu gry
    private lateinit var starManager: StarManager // Manager gwiazdek
    private lateinit var pauseMenu: PauseMenu // Menu pauzy gry
    private lateinit var countdownManager: GameCountdownManager // Manager odliczania
    private var isGameEnding = false // Flaga końca gry
    private var currentLevel = 1 // Aktualny poziom gry

    companion object {
        private const val BASE_TIME_SECONDS = 90 // Czas trwania jednej rundy (w sekundach)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_left_or_right)
        supportActionBar?.hide()

        // Inicjalizacja widoków
        gridLayout = findViewById(R.id.gridLayout)
        countdownText = findViewById(R.id.countdownText)
        pauseButton = findViewById(R.id.pauseButton)
        pauseOverlay = findViewById(R.id.pauseOverlay)
        timerProgressBar = findViewById(R.id.gameTimerProgressBar)
        starManager = StarManager()
        starManager.init(findViewById(R.id.starCountText))

        // Inicjalizacja paska czasu
        timerProgressBar.setTotalTime(BASE_TIME_SECONDS)
        timerProgressBar.setOnFinishCallback {
            runOnUiThread {
                isGameEnding = true
                Toast.makeText(this, "Czas minął! Koniec gry!", Toast.LENGTH_LONG).show()
                gridLayout.isEnabled = false
                pauseOverlay.visibility = View.GONE
                updateUserStatistics(
                    categoryKey = GameKeys.CATEGORY_ATTENTION,
                    gameKey = GameKeys.GAME_LEFT_OR_RIGHT,
                    starsEarned = starManager.starCount,
                    accuracy = calculateAccuracy(),
                    reactionTime = getAverageReactionTime(stars = starManager.starCount),
                )
                lastPlayedGame(
                    GameKeys.CATEGORY_ATTENTION,
                    GameKeys.GAME_LEFT_OR_RIGHT,
                    getString(R.string.left_or_right)
                )
                finish()
            }
        }

        // Inicjalizacja managera odliczania
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
                timerProgressBar.stop()
                timerProgressBar.reset()
                timerProgressBar.start()
                startReactionTracking()
                startNewGame()
            }
        )

        // Inicjalizacja menu pauzy
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
                gameStatsManager.onGameResumed()
                timerProgressBar.start()
            }, // Wznawia timer po pauzie
            onPause = {
                gameStatsManager.onGamePaused()
                timerProgressBar.pause()
            },  // Zatrzymuje timer podczas pauzy
            onExit = { finish() }, // Kończy aktywność
            instructionTitle = getString(R.string.instructions),
            instructionMessage = getString(R.string.left_or_right_instruction),
        )

        // Sprawdzenie, czy gra jest uruchamiana po raz pierwszy
        if (savedInstanceState == null) {
            countdownManager.startCountdown() // Rozpoczyna odliczanie początkowe
        } else {
            restoreGameState(savedInstanceState) // Przywraca stan po rotacji
        }
    }

    // Zapisuje stan aktywności
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
        starManager.saveState(outState)
    }

    // Przywraca stan aktywności
    private fun restoreGameState(savedInstanceState: Bundle) {
        pauseOverlay.visibility = savedInstanceState.getInt("pauseOverlayVisibility", View.GONE)
        countdownText.visibility = savedInstanceState.getInt("countdownTextVisibility", View.GONE)
        gridLayout.visibility = savedInstanceState.getInt("gridLayoutVisibility", View.VISIBLE)
        pauseButton.visibility = savedInstanceState.getInt("pauseButtonVisibility", View.VISIBLE)
        currentLevel = savedInstanceState.getInt("currentLevel", 1)
        starManager.restoreState(savedInstanceState)

        // Przywracanie stanu timera
        val timerRemainingTimeMs = savedInstanceState.getLong("timerRemainingTimeMs", BASE_TIME_SECONDS * 1000L)
        val timerIsRunning = savedInstanceState.getBoolean("timerIsRunning", false)
        timerProgressBar.setRemainingTimeMs(timerRemainingTimeMs.coerceAtLeast(1L))

        if (timerIsRunning && pauseOverlay.visibility != View.VISIBLE) {
            timerProgressBar.start()
        }

        //Przywracanie stanu odliczania
        val countdownIndex = savedInstanceState.getInt("countdownIndex", 0)
        val countdownInProgress = savedInstanceState.getBoolean("countdownInProgress", false)
        // Kontynuowanie odliczania, jeśli było aktywne
        if (countdownInProgress) {
            countdownManager.startCountdown(countdownIndex)
        }

        pauseMenu.syncWithOverlay()
    }

    private fun startNewGame() {
        gameStatsManager.startReactionTracking()

    }

    override fun onPause() {
        super.onPause()
        // Pauzuje grę automatycznie przy wyjściu z aktywności
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
