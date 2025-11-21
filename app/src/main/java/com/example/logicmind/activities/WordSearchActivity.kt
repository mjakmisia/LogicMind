package com.example.logicmind.activities
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.graphics.toColorInt
import androidx.gridlayout.widget.GridLayout
import com.example.logicmind.R
import com.example.logicmind.additional.SelectionOverlayView
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
    private lateinit var overlayView: SelectionOverlayView
    private var isGameEnding = false
    private var currentLevel = 1
    private lateinit var rootLayout: ConstraintLayout // Layout do którego dynamicznie dodajemy widoki słów
    private val wordViews = mutableListOf<TextView>() // Lista przechowująca widoki słów (dla Flow)
    private var currentBoard: WordSearchGenerator.Board? = null // Logika planszy
    private var wordsToFind: List<String> = emptyList() // Słowa do znalezienia w danej rundzie
    private val foundWords = mutableSetOf<String>() // Słowa już znalezione
    private val letterViews = mutableMapOf<Pair<Int, Int>, TextView>() // Mapa przechowująca widoki liter (Row, Col) -> TextView

    // Przechowuje dane linii na potrzeby rotacji
    data class PermanentLineData(
        val startRow: Int, val startCol: Int,
        val endRow: Int, val endCol: Int,
        val color: Int
    )

    private val permanentLinesList = mutableListOf<PermanentLineData>() // Lista narysowanych linii

    // Zmienne do zarządzania kolorami
    private val pastelColors = listOf(
        "#FFC0CB".toColorInt(), "#B0E0E6".toColorInt(),
        "#98FB98".toColorInt(), "#FFFF99".toColorInt(),
        "#A8B1DD".toColorInt(), "#FFDAB9".toColorInt(),
        "#E6E6FA".toColorInt(), "#DDA0DD".toColorInt()
    )
    private val availableColors = mutableListOf<Int>() // Kolory nieużyte w danej rundzie
    private val assignedWordColors = mutableMapOf<String, Int>() // Mapa: słowo -> kolor

    // Zmienne stanu dotyku
    private var startCell: Pair<Int, Int>? = null
    private var currentSelectionCoords = mutableListOf<Pair<Int, Int>>()
    private var currentTempColor: Int = Color.GRAY

    companion object {
        private const val BASE_TIME_SECONDS = 90
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_word_search)
        supportActionBar?.hide()

        // Inicjalizacja widoków
        gridLayout = findViewById(R.id.gridLayout)
        overlayView = findViewById(R.id.selectionOverlayView)
        countdownText = findViewById(R.id.countdownText)
        pauseButton = findViewById(R.id.pauseButton)
        pauseOverlay = findViewById(R.id.pauseOverlay)
        timerProgressBar = findViewById(R.id.gameTimerProgressBar)
        rootLayout = findViewById(R.id.rootLayout)
        starManager = StarManager()
        starManager.init(findViewById(R.id.starCountText))

        // Dynamiczne dostosowanie layoutu do orientacji ekranu
        val flow = findViewById<androidx.constraintlayout.helper.widget.Flow>(R.id.wordsToFindFlow)
        val orientation = resources.configuration.orientation
        val gridContainer = findViewById<android.widget.FrameLayout>(R.id.gridContainer)
        val params = gridContainer.layoutParams as ConstraintLayout.LayoutParams

        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            // PION - max 2 słowa w rzędzie + grid przymocowany do timera
            flow.setMaxElementsWrap(2)
            params.topToBottom = R.id.gameTimerProgressBar
        } else {
            // POZIOM - max 6 słów w rzędzie + grid przymocowany do paska słów
            flow.setMaxElementsWrap(6)
            params.topToBottom = R.id.wordsToFindFlow
        }

        // Inicjalizacja paska czasu
        timerProgressBar.setTotalTime(BASE_TIME_SECONDS)
        timerProgressBar.setOnFinishCallback {
            runOnUiThread {
                isGameEnding = true
                Toast.makeText(this, "Czas minął! Koniec gry!", Toast.LENGTH_LONG).show()
                gridLayout.isEnabled = false
                pauseOverlay.visibility = View.GONE
                updateUserStatistics(
                    categoryKey = GameKeys.CATEGORY_FOCUS,
                    gameKey = GameKeys.GAME_WORD_SEARCH,
                    starsEarned = starManager.starCount,
                    accuracy = gameStatsManager.calculateAccuracy(),
                    reactionTime = getAverageReactionTime(stars = starManager.starCount),
                )

                lastPlayedGame(
                    GameKeys.CATEGORY_FOCUS,
                    GameKeys.GAME_WORD_SEARCH,
                    getString(R.string.word_search)
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
                timerProgressBar,
                findViewById<androidx.constraintlayout.helper.widget.Flow>(R.id.wordsToFindFlow),
                overlayView
            ),
            onCountdownFinished = {
                currentLevel = 1
                starManager.reset()
                timerProgressBar.stop()
                timerProgressBar.reset()
                timerProgressBar.start()
                gameStatsManager.startReactionTracking()
                gameStatsManager.setGameStartTime(this@WordSearchActivity)
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
                onGameResumed()
                timerProgressBar.start()
            },
            onPause = {
                onGamePaused()
                timerProgressBar.pause()
            },
            onExit = {
                updateUserStatistics(
                    categoryKey = GameKeys.CATEGORY_FOCUS,
                    gameKey = GameKeys.GAME_WORD_SEARCH,
                    starsEarned = starManager.starCount,
                    accuracy = gameStatsManager.calculateAccuracy(),
                    reactionTime = getAverageReactionTime(stars = starManager.starCount),
                )

                lastPlayedGame(
                    GameKeys.CATEGORY_FOCUS,
                    GameKeys.GAME_WORD_SEARCH,
                    getString(R.string.word_search)
                )
                finish()
            },
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

        // Zapisz dane linii
        outState.putInt("permanentLinesCount", permanentLinesList.size)
        permanentLinesList.forEachIndexed { index, line ->
            outState.putInt("line_${index}_sr", line.startRow)
            outState.putInt("line_${index}_sc", line.startCol)
            outState.putInt("line_${index}_er", line.endRow)
            outState.putInt("line_${index}_ec", line.endCol)
            outState.putInt("line_${index}_color", line.color)
        }
        // Zapisz przypisane kolory
        outState.putStringArrayList("assignedWordKeys", ArrayList(assignedWordColors.keys))
        outState.putIntegerArrayList("assignedWordValues", ArrayList(assignedWordColors.values))

        saveGameStats(outState)
    }

    // Przywraca stan aktywności
    private fun restoreGameState(savedInstanceState: Bundle) {
        pauseOverlay.visibility = savedInstanceState.getInt("pauseOverlayVisibility", View.GONE)
        countdownText.visibility = savedInstanceState.getInt("countdownTextVisibility", View.GONE)
        gridLayout.visibility = savedInstanceState.getInt("gridLayoutVisibility", View.VISIBLE)
        pauseButton.visibility = savedInstanceState.getInt("pauseButtonVisibility", View.VISIBLE)
        currentLevel = savedInstanceState.getInt("currentLevel", 1)
        starManager.restoreState(savedInstanceState)

        // Przywróć stan timera
        val timerRemainingTimeMs = savedInstanceState.getLong("timerRemainingTimeMs", BASE_TIME_SECONDS * 1000L)
        val timerIsRunning = savedInstanceState.getBoolean("timerIsRunning", false)
        timerProgressBar.setRemainingTimeMs(timerRemainingTimeMs.coerceAtLeast(1L))

        if (timerIsRunning && pauseOverlay.visibility != View.VISIBLE) {
            timerProgressBar.start()
        }

        // Przywróć stan odliczania
        val countdownIndex = savedInstanceState.getInt("countdownIndex", 0)
        val countdownInProgress = savedInstanceState.getBoolean("countdownInProgress", false)

        // Przywróć przypisane kolory
        val assignedWordKeys = savedInstanceState.getStringArrayList("assignedWordKeys")
        val assignedWordValues = savedInstanceState.getIntegerArrayList("assignedWordValues")
        if (assignedWordKeys != null && assignedWordValues != null) {
            assignedWordColors.clear()
            for (i in assignedWordKeys.indices) {
                assignedWordColors[assignedWordKeys[i]] = assignedWordValues[i]
            }
        }
        availableColors.clear()
        pastelColors.forEach { color ->
            if (!assignedWordColors.containsValue(color)) {
                availableColors.add(color)
            }
        }

        // Przywróć dane linii
        val lineCount = savedInstanceState.getInt("permanentLinesCount", 0)
        permanentLinesList.clear()
        for (i in 0 until lineCount) {
            permanentLinesList.add(PermanentLineData(
                savedInstanceState.getInt("line_${i}_sr"),
                savedInstanceState.getInt("line_${i}_sc"),
                savedInstanceState.getInt("line_${i}_er"),
                savedInstanceState.getInt("line_${i}_ec"),
                savedInstanceState.getInt("line_${i}_color")
            ))
        }

        // Przywróc stan planszy
        val savedWords = savedInstanceState.getStringArrayList("wordsToFind")
        val savedFoundWords = savedInstanceState.getStringArrayList("foundWords")
        val savedFlatGrid = savedInstanceState.getCharArray("flatGrid")
        val savedBoardSize = savedInstanceState.getInt("boardSize", 0)

        if (countdownInProgress) {
            // Jeśli odliczanie trwa, kontynuuj je
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
            rebuildUiFromState()
        } else {
            countdownManager.startCountdown()
        }

        pauseMenu.syncWithOverlay()

        restoreGameStats(savedInstanceState)
    }

    // Odbudowa UI (po rotacji)
    private fun rebuildUiFromState() {
        val board = currentBoard ?: return // Bezpiecznik

        // Odbuduj listę słów
        populateWordListUi()

        // Odbuduj siatkę
        populateGridUi(board)

        // Odbuduj linie na nakładce
        gridLayout.post {
            overlayView.clearAllLines()
            permanentLinesList.forEach { lineData ->
                val startPixels = getCellCenter(lineData.startRow, lineData.startCol)
                val endPixels = getCellCenter(lineData.endRow, lineData.endCol)
                if (startPixels != null && endPixels != null) {
                    overlayView.addPermanentLine(startPixels, endPixels, lineData.color)
                }
            }
        }

        // Zastosuj przekreślenia dla znalezionych słów
        updateFoundWordsUi()

        // Ponownie ustaw nasłuchiwanie dotyku
        setupTouchListener()
    }

    // Zwraca ustawienia dla danego poziomu
    private fun getLevelSettings(level: Int): Pair<Int, Int> {
        return when (level) {
            1 -> Pair(5, 4)
            2 -> Pair(5, 5)
            3 -> Pair(6, 4)
            4 -> Pair(6, 5)
            5 -> Pair(6, 6)
            6 -> Pair(7, 5)
            7 -> Pair(7, 6)
            8 -> Pair(7, 7)
            9 -> Pair(8, 6)
            10 -> Pair(8, 7)
            11 -> Pair(8, 8)
            12 -> Pair(8, 9)
            13 -> Pair(9, 7)
            14 -> Pair(9, 8)
            15 -> Pair(9, 9)
            16 -> Pair(10, 8)
            17 -> Pair(10, 9)
            18 -> Pair(10, 10)
            else -> Pair(10, 10)
        }
    }

    private fun startNewGame() {
        gameStatsManager.startReactionTracking()
        // Wyczyść stany
        currentBoard = null
        wordsToFind = emptyList()
        foundWords.clear()
        letterViews.clear()
        gridLayout.removeAllViews()

        // Wyczyść nakładkę i kolory
        overlayView.clearAllLines()
        permanentLinesList.clear()
        assignedWordColors.clear()
        availableColors.clear()
        availableColors.addAll(pastelColors.shuffled())

        // Ustawienia poziomu pobierane dynamicznie
        val (boardSize, wordCount) = getLevelSettings(currentLevel)
        val lang = Locale.getDefault().language.take(2)

        // Generuj planszę
        while (currentBoard == null) {
            val words = WordBank.getWords(lang, maxLength = boardSize, count = wordCount)
            if (words.size < wordCount) {
                Toast.makeText(this, "Błąd: Za mało słów w banku!", Toast.LENGTH_SHORT).show()
                return
            }

            currentBoard = WordSearchGenerator.generate(boardSize, words, lang)
            wordsToFind = currentBoard?.placedWords ?: emptyList()
        }

        // Wypełnij planszę
        populateWordListUi()
        populateGridUi(currentBoard!!)

        // Ustaw nasłuchiwanie dotyku
        setupTouchListener()
    }

    // Wypełnia listę słów do znalezienia na ekranie
    private fun populateWordListUi() {
        // Usuń stare widoki słów
        wordViews.forEach { rootLayout.removeView(it) }
        wordViews.clear()

        // Pobierz referencję do Flow (automatyczne ułożenie widoków)
        val flow = findViewById<androidx.constraintlayout.helper.widget.Flow>(R.id.wordsToFindFlow)

        // Utwórz nowy widok TextView dla każdego słowa do znalezienia
        wordsToFind.forEach { word ->
            val textView = TextView(this).apply {
                id = View.generateViewId() // unikalne ID do śledzenia widoku
                text = word
                textSize = 16f
                setTextColor(Color.WHITE)
                setPadding(20, 10, 20, 10)
                background = AppCompatResources.getDrawable(context, R.drawable.bg_word_chip)
                tag = word
            }
            // Dodaj widok do głównego layoutu i listy śledzącej
            rootLayout.addView(textView)
            wordViews.add(textView)
        }

        // Przekaż do Flow listę ID widoków
        flow.referencedIds = wordViews.map { it.id }.toIntArray()
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

        // Obliczanie grubości linii po ustawieniu siatki
        gridLayout.post {
            val firstCell = letterViews[Pair(0, 0)]
            if (firstCell != null) {
                overlayView.setLineStrokeWidth(firstCell.height.toFloat())
            }
        }
    }

    // Aktualizuje UI, przekreślając znalezione słowa
    private fun updateFoundWordsUi() {
        wordViews.forEach { view ->
            val word = view.tag as? String
            if (word != null && word in foundWords) {
                // Zastosuj przekreślenie
                view.paintFlags = view.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                view.setTextColor(Color.GRAY)
            }
        }
    }

    // Pobiera środek komórki w pikselach (x, y) względem nakładki
    private fun getCellCenter(row: Int, col: Int): PointF? {
        val textView = letterViews[Pair(row, col)] ?: return null
        val rect = android.graphics.Rect()
        textView.getHitRect(rect)
        return PointF(rect.centerX().toFloat(), rect.centerY().toFloat())
    }

    // Znajduje komórkę (wiersz, kolumna) na podstawie pozycji dotyku (x, y)
    private fun findCellAt(x: Float, y: Float): Pair<Int, Int>? {
        for ((coords, textView) in letterViews) {
            val rect = android.graphics.Rect()
            textView.getHitRect(rect)
            if (rect.contains(x.toInt(), y.toInt())) {
                return coords
            }
        }
        return null
    }

    // Ustawia OnTouchListener dla nakładki do obsługi przesuwania palcem
    private fun setupTouchListener() {
        overlayView.setOnTouchListener { _, event ->
            val x = event.x
            val y = event.y

            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    startCell = findCellAt(x, y)
                    if (startCell == null) return@setOnTouchListener false

                    val startPixels = getCellCenter(startCell!!.first, startCell!!.second) ?: return@setOnTouchListener false

                    // Wybierz nowy tymczasowy kolor dla tego przeciągnięcia
                    currentTempColor = availableColors.firstOrNull() ?: pastelColors.random() // awaryjnie - losowy

                    currentSelectionCoords.clear()
                    currentSelectionCoords.add(startCell!!)

                    // Rysuj linię tymczasową
                    overlayView.setTemporaryLine(startPixels, startPixels, currentTempColor)
                    true
                }

                android.view.MotionEvent.ACTION_MOVE -> {
                    if (startCell == null) return@setOnTouchListener false

                    val startPixels = getCellCenter(startCell!!.first, startCell!!.second) ?: return@setOnTouchListener false
                    val endCell = findCellAt(x, y) ?: startCell!! // Jeśli wyjedziemy, użyj ostatniej znanej

                    // Oblicz prawidłową linię (H, V, D)
                    val lineCoords = calculateLine(startCell!!, endCell)
                    currentSelectionCoords = lineCoords.toMutableList()

                    // Znajdź środek ostatniej komórki w prawidłowej linii
                    val lastValidCell = currentSelectionCoords.last()
                    val endPixels = getCellCenter(lastValidCell.first, lastValidCell.second) ?: startPixels

                    // Zaktualizuj linię tymczasową
                    overlayView.setTemporaryLine(startPixels, endPixels, currentTempColor)
                    true
                }

                android.view.MotionEvent.ACTION_UP -> {
                    overlayView.clearTemporaryLine() // Zawsze czyść tymczasową
                    if (startCell == null) return@setOnTouchListener false

                    checkSelectedWord() // Sprawdź czy słowo jest poprawne

                    // Poinformuj system że akcja się zakończyła
                    overlayView.performClick()

                    startCell = null
                    currentSelectionCoords.clear()
                    true
                }
                else -> false
            }
        }
    }

    // Sprawdza zaznaczone słowo i dodaje stałą linię, jeśli jest poprawne
    private fun checkSelectedWord() {
        // Zbuduj słowo z zaznaczonych komórek
        val selectedWordBuilder = StringBuilder()
        for (coords in currentSelectionCoords) {
            val letter = currentBoard?.grid?.getOrNull(coords.first)?.getOrNull(coords.second)
            letter?.let { selectedWordBuilder.append(it) }
        }
        val selectedWord = selectedWordBuilder.toString()
        val reversedSelectedWord = selectedWord.reversed()

        // Sprawdź czy słowo jest na liście i nie zostało jeszcze znalezione
        val wordToFind = wordsToFind.find {
            (it.equals(selectedWord, ignoreCase = true) ||
                    it.equals(reversedSelectedWord, ignoreCase = true)) &&
                    it !in foundWords
        }

        if (wordToFind != null) {
            // Znaleziono słowo
            foundWords.add(wordToFind)

            starManager.increment()

            gameStatsManager.registerAttempt(true)

            val startCoords = currentSelectionCoords.first()
            val endCoords = currentSelectionCoords.last()
            val startPixels = getCellCenter(startCoords.first, startCoords.second)
            val endPixels = getCellCenter(endCoords.first, endCoords.second)

            if (startPixels != null && endPixels != null) {
                // Użyj koloru który był używany do przeciągania
                val finalColor = currentTempColor

                // Dodaj stałą linię do nakładki
                overlayView.addPermanentLine(startPixels, endPixels, finalColor)

                // Zapisz dane linii na potrzeby rotacji
                permanentLinesList.add(PermanentLineData(
                    startCoords.first, startCoords.second,
                    endCoords.first, endCoords.second,
                    finalColor
                ))

                // Zarządzaj pulą kolorów
                availableColors.remove(finalColor)
                assignedWordColors[wordToFind] = finalColor
            }

            // Zaktualizuj listę słów (przekreślenie)
            updateFoundWordsUi()

            // Sprawdź warunek ukończenia poziomu
            if (foundWords.size == wordsToFind.size) {

                timerProgressBar.addTime(20)

                currentLevel++

                Toast.makeText(this, "+20 sekund! Poziom $currentLevel", Toast.LENGTH_SHORT).show()

                startNewGame()
            }
        } else {
            gameStatsManager.registerAttempt(false)
        }
    }

    // Oblicza listę współrzędnych komórek tworzących linię
    private fun calculateLine(start: Pair<Int, Int>, end: Pair<Int, Int>): List<Pair<Int, Int>> {
        val lineCoords = mutableListOf<Pair<Int, Int>>()
        val (r1, c1) = start
        val (r2, c2) = end

        val dr = r2 - r1
        val dc = c2 - c1

        // Pojedyncza komórka
        if (dr == 0 && dc == 0) {
            lineCoords.add(start)
            return lineCoords
        }

        val isHorizontal = (dr == 0)
        val isVertical = (dc == 0)
        val isDiagonal = (kotlin.math.abs(dr) == kotlin.math.abs(dc))

        if (!isHorizontal && !isVertical && !isDiagonal) {
            // Nieprawidłowa linia, zwróć tylko komórkę startową
            lineCoords.add(start)
            return lineCoords
        }

        val rStep = Integer.signum(dr)
        val cStep = Integer.signum(dc)
        val steps = maxOf(kotlin.math.abs(dr), kotlin.math.abs(dc))

        for (i in 0..steps) {
            lineCoords.add(Pair(r1 + i * rStep, c1 + i * cStep))
        }

        return lineCoords
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