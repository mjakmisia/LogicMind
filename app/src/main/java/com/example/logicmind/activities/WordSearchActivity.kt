package com.example.logicmind.activities
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.gridlayout.widget.GridLayout
import com.example.logicmind.R
import com.example.logicmind.additional.WordBank
import com.example.logicmind.additional.WordSearchGenerator
import com.example.logicmind.common.GameCountdownManager
import com.example.logicmind.common.GameTimerProgressBar
import com.example.logicmind.common.PauseMenu
import com.example.logicmind.common.StarManager
import java.util.Locale

class WordSearchActivity : BaseActivity() {

    private lateinit var gridLayout: GridLayout
    private lateinit var countdownText: TextView
    private lateinit var pauseButton: ImageButton
    private lateinit var pauseOverlay: ConstraintLayout
    private lateinit var timerProgressBar: GameTimerProgressBar
    private lateinit var starManager: StarManager
    private lateinit var pauseMenu: PauseMenu
    private lateinit var countdownManager: GameCountdownManager
    private var isGameEnding = false
    private var currentLevel = 1
    private lateinit var wordsToFindLayout: LinearLayout // Kontener na słowa
    private var currentBoard: WordSearchGenerator.Board? = null // Logika planszy
    private var wordsToFind: List<String> = emptyList() // Słowa do znalezienia w tej rundzie
    private val foundWords = mutableSetOf<String>() // Słowa już znalezione
    private val letterViews = mutableMapOf<Pair<Int, Int>, TextView>() // Mapa przechowująca widoki liter (Row, Col) -> TextView

    companion object {
        private const val BASE_TIME_SECONDS = 90
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_word_search)
        supportActionBar?.hide()

        // Inicjalizacja widoków
        gridLayout = findViewById(R.id.gridLayout)
        countdownText = findViewById(R.id.countdownText)
        pauseButton = findViewById(R.id.pauseButton)
        pauseOverlay = findViewById(R.id.pauseOverlay)
        timerProgressBar = findViewById(R.id.gameTimerProgressBar)
        wordsToFindLayout = findViewById(R.id.wordsToFindLayout)
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
                timerProgressBar,
                wordsToFindLayout
            ),
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
            onResume = { timerProgressBar.start() },
            onPause = { timerProgressBar.pause() },
            onExit = { finish() },
            instructionTitle = getString(R.string.instructions),
            instructionMessage = getString(R.string.word_search_instruction),
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

        outState.putStringArrayList("wordsToFind", ArrayList(wordsToFind))
        outState.putStringArrayList("foundWords", ArrayList(foundWords))
        currentBoard?.let { board ->
            outState.putInt("boardSize", board.size)
            val flatGrid = board.grid.flatten().toCharArray() // Zapisujemy siatkę jako płaską tablicę znaków
            outState.putCharArray("flatGrid", flatGrid)
        }
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

        //Przywracanie stanu planszy
        val savedWords = savedInstanceState.getStringArrayList("wordsToFind")
        val savedFoundWords = savedInstanceState.getStringArrayList("foundWords")
        val savedFlatGrid = savedInstanceState.getCharArray("flatGrid")
        val savedBoardSize = savedInstanceState.getInt("boardSize", 0)

        if (countdownInProgress) {
            // Jeśli odliczanie trwa, po prostu je kontynuuj
            countdownManager.startCountdown(countdownIndex)
        } else if (savedWords != null && savedFoundWords != null && savedFlatGrid != null && savedBoardSize > 0) {
            // Jeśli mamy zapisany stan gry (i odliczanie się skończyło), odbuduj planszę
            wordsToFind = savedWords
            foundWords.addAll(savedFoundWords)

            // Odbuduj siatkę 2D z płaskiej tablicy
            val grid = List(savedBoardSize) { row ->
                List(savedBoardSize) { col ->
                    savedFlatGrid[row * savedBoardSize + col]
                }
            }
            currentBoard = WordSearchGenerator.Board(savedBoardSize, grid, wordsToFind)

            // Odbuduj UI
            rebuildUiFromState()
        } else {
            countdownManager.startCountdown()
        }

        pauseMenu.syncWithOverlay()
    }

    // Odbudowa UI (po rotacji)
    private fun rebuildUiFromState() {
        val board = currentBoard ?: return // Bezpiecznik

        // Odbuduj listę słów
        populateWordListUi()

        // Odbuduj siatkę
        populateGridUi(board)

        // Zastosuj przekreślenia dla znalezionych słów
        updateFoundWordsUi()
    }

    private fun startNewGame() {
        // Wyczyść stany
        currentBoard = null
        wordsToFind = emptyList()
        foundWords.clear()
        letterViews.clear()
        gridLayout.removeAllViews()
        wordsToFindLayout.removeAllViews()

        // Ustawienia poziomu
        val boardSize = 5
        val wordCount = 4
        val lang = Locale.getDefault().language.take(2)

        // Generuj planszę
        while (currentBoard == null) {
            val words = WordBank.getWords(lang, maxLength = boardSize, count = wordCount)
            if (words.size < wordCount) {
                Toast.makeText(this, "Błąd: Za mało słów w banku!", Toast.LENGTH_SHORT).show()
                return
            }

            currentBoard = WordSearchGenerator.generate(boardSize, words)
            wordsToFind = currentBoard?.placedWords ?: emptyList()
        }

        // Wypełnij planszę
        populateWordListUi()
        populateGridUi(currentBoard!!)

        // TODO: Logika dotyku
        // gridLayout.setOnTouchListener { ... }
    }

    // Wypełnie tekstu nad planszą ze słowami
    private fun populateWordListUi() {
        wordsToFindLayout.removeAllViews()
        wordsToFind.forEach { word ->
            val textView = TextView(this).apply {
                text = word
                textSize = 16f
                setTextColor(Color.WHITE)
                setPadding(16, 8, 16, 8)
                tag = word
            }
            wordsToFindLayout.addView(textView)
        }
    }

    // Wypełnia GridLayout z literami
    private fun populateGridUi(board: WordSearchGenerator.Board) {
        gridLayout.removeAllViews()
        letterViews.clear()

        gridLayout.rowCount = board.size
        gridLayout.columnCount = board.size

        for (row in 0 until board.size) {
            for (col in 0 until board.size) {
                val letter = board.grid[row][col]

                val textView = TextView(this).apply {
                    text = letter.toString()
                    textSize = 20f
                    setTextColor(Color.WHITE)
                    gravity = Gravity.CENTER
                    setBackgroundResource(R.drawable.bg_grid_cell)

                    // Ustawienie parametrów GridLayout
                    val params = GridLayout.LayoutParams().apply {
                        width = 0
                        height = 0
                        rowSpec = GridLayout.spec(row, 1f) // 1f = równe rozłożenie
                        columnSpec = GridLayout.spec(col, 1f)
                        setMargins(0, 0, 0, 0)
                    }
                    layoutParams = params
                }

                gridLayout.addView(textView)
                letterViews[Pair(row, col)] = textView // Zapisz widok w mapie
            }
        }
    }

    // Aktualizuje UI, przekreślając znalezione słowa
    private fun updateFoundWordsUi() {
        for (i in 0 until wordsToFindLayout.childCount) {
            val view = wordsToFindLayout.getChildAt(i)
            if (view is TextView) {
                val word = view.tag as? String
                if (word != null && word in foundWords) {
                    // Zastosuj przekreślenie
                    view.paintFlags = view.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                    view.setTextColor(Color.GRAY) // Zmień kolor
                }
            }
        }
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