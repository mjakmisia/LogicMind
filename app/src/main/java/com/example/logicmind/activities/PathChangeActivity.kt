package com.example.logicmind.activities

import android.os.Build
import android.os.Bundle
import android.view.View
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

class PathChangeActivity : BaseActivity() {

    private lateinit var gridLayout: GridLayout
    private lateinit var countdownText: TextView
    private lateinit var pauseButton: ImageButton
    private lateinit var pauseOverlay: ConstraintLayout
    private lateinit var timerProgressBar: GameTimerProgressBar
    private lateinit var starManager: StarManager
    private lateinit var pauseMenu: PauseMenu
    private lateinit var countdownManager: GameCountdownManager
    private var isGameEnding = false
    private var isGameRunning = false
    private var currentLevel = 1

    private val switchStates = HashMap<String, Int>()
    private val switchViews = mutableListOf<FrameLayout>()

    companion object {
        private const val BASE_TIME_SECONDS = 90
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_path_change)
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
                isGameRunning = false
                Toast.makeText(this, "Czas minął! Koniec gry!", Toast.LENGTH_LONG).show()
                gridLayout.isEnabled = false
                pauseOverlay.visibility = View.GONE
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
            onResume = { if (isGameRunning) timerProgressBar.start() },
            onPause = { if (isGameRunning) timerProgressBar.pause() },
            onExit = {
                finish()
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
        // Zapisujemy całą mapę stanów przełączników
        outState.putSerializable("switchStates", switchStates)

        starManager.saveState(outState)
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

        // Bezpieczne odtwarzanie HashMapy
        val savedStates = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // API 33+
            @Suppress("UNCHECKED_CAST")
            savedInstanceState.getSerializable("switchStates", HashMap::class.java) as? HashMap<String, Int>
        } else {
            // API < 33
            @Suppress("DEPRECATION", "UNCHECKED_CAST")
            savedInstanceState.getSerializable("switchStates") as? HashMap<String, Int>
        }

        if (savedStates != null) {
            switchStates.clear()
            switchStates.putAll(savedStates)
        }

        val countdownIndex = savedInstanceState.getInt("countdownIndex", 0)
        val countdownInProgress = savedInstanceState.getBoolean("countdownInProgress", false)

        if (countdownInProgress) {
            countdownManager.startCountdown(countdownIndex)
        } else if (isGameRunning) {
            if (timerIsRunning && pauseOverlay.visibility != View.VISIBLE) {
                timerProgressBar.start()
            }
            // Inicjalizacja planszy
            gridLayout.post { setupGrid() }
        }

        pauseMenu.syncWithOverlay()
    }

    // Funkcja wywoływana po odliczaniu
    private fun startNewGame() {
        if (pauseMenu.isPaused) pauseMenu.resume()

        isGameRunning = true
        isGameEnding = false
        gridLayout.isEnabled = true
        switchStates.clear()

        setupGrid()
        spawnBall()
    }

    private fun setupGrid() {
        switchViews.clear()

        for (child in gridLayout.children) {
            val cell = child as? FrameLayout ?: continue
            val tag = cell.tag as? String ?: continue

            if (tag.startsWith("switch_")) {
                switchViews.add(cell)
                val state = switchStates.getOrPut(tag) { 0 } // Ustaw 0 jako domyślny

                // Znajdź właściwy ImageView
                val pathImage = cell.getChildAt(1) as? ImageView
                if (pathImage != null) {
                    updateSwitchImage(pathImage, tag, state) // Zaktualizuj obrazek
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

        // Znajdź właściwy ImageView
        val pathImage = cell.getChildAt(1) as? ImageView
        if (pathImage != null) {
            updateSwitchImage(pathImage, tag, newState)
        }
    }

    private fun updateSwitchImage(imageView: ImageView, tag: String, state: Int) {
        // Logika wyboru odpowiedniej grafiki i rotacji
        when (tag) {
            "switch_A" -> {
                if (state == 0) {
                    imageView.setImageResource(R.drawable.path_straight)
                    imageView.rotation = 0f // góra dół
                } else {
                    imageView.setImageResource(R.drawable.path_corner)
                    imageView.rotation = 180f // dół prawa
                }
            }
            "switch_B" -> {
                if (state == 0) {
                    imageView.setImageResource(R.drawable.path_corner)
                    imageView.rotation = 0f // góra lewo
                } else {
                    imageView.setImageResource(R.drawable.path_corner)
                    imageView.rotation = 270f // dół lewo
                }
            }
            "switch_C" -> {
                if (state == 0) {
                    imageView.setImageResource(R.drawable.path_corner)
                    imageView.rotation = 270f // dół lewo
                } else {
                    imageView.setImageResource(R.drawable.path_straight)
                    imageView.rotation = 0f // góra dół
                }
            }
            // Domyślna grafika błędu
            else -> imageView.setImageResource(R.drawable.bg_rounded_card)
        }
    }

    private fun spawnBall() {
        // TODO: Logika spawnowania kulki
        Toast.makeText(this, "Spawn kulki (level $currentLevel)", Toast.LENGTH_SHORT).show()
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
    }
}