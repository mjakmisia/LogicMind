package com.example.logicmind.activities

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.gridlayout.widget.GridLayout
import com.example.logicmind.R
import com.example.logicmind.common.GameCountdownManager
import com.example.logicmind.common.GameTimerProgressBar
import com.example.logicmind.common.PauseMenu
import com.example.logicmind.common.SoundManager
import com.example.logicmind.common.StarManager

class CardMatchActivity : BaseActivity() {

    private lateinit var cards: MutableList<Card>
    private var firstCard: Card? = null
    private var secondCard: Card? = null
    private var isFlipping = false
    private lateinit var gridLayout: GridLayout
    private lateinit var countdownText: TextView
    private lateinit var pauseButton: ImageView
    private lateinit var pauseOverlay: View
    private var boardRows: Int = 4
    private var boardCols: Int = 4
    private var bombCount: Int = 0
    private lateinit var countdownManager: GameCountdownManager
    private var currentLevel = 1
    private var isGameEnding = false
    private lateinit var timerProgressBar: GameTimerProgressBar
    private lateinit var starManager: StarManager
    private lateinit var pauseMenu: PauseMenu
    private var previewRemaining: Long = 0L
    private var isPreviewPhase: Boolean = false
    private var pendingFlipCardIndex: Int = -1
    private var currentBestScore = 0

    companion object {
        const val BOMB_VALUE = -1
        const val BASE_TIME_SECONDS = 90
    }

    private val cardImages = listOf(
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

    private lateinit var cardValues: List<Int>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_card_match)

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
            db.getReference("users")
                .child(uid)
                .child("categories")
                .child(GameKeys.CATEGORY_MEMORY)
                .child(GameKeys.GAME_CARD_MATCH)
                .child("bestStars")
                .get()
                .addOnSuccessListener { snapshot ->
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
                timerProgressBar
            ),
            onCountdownFinished = {
                currentLevel = 1
                starManager.reset()

                gameStatsManager.startReactionTracking()
                gameStatsManager.setGameStartTime(this@CardMatchActivity)
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
                if (!isPreviewPhase) {
                    timerProgressBar.start()
                }
                onGameResumed()
            },
            onPause = {
                if (!isPreviewPhase) {
                    timerProgressBar.pause()
                }
                gameStatsManager.onGamePaused()
            },
            onExit = {
                handleGameOver()
            },
            instructionTitle = getString(R.string.instructions),
            instructionMessage = getString(R.string.card_match_instruction),
        )

        if (savedInstanceState == null) {
            countdownManager.startCountdown()
        } else {
            restoreGameState(savedInstanceState)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("boardRows", boardRows)
        outState.putInt("boardCols", boardCols)
        outState.putBoolean("isFlipping", isFlipping)
        outState.putInt("pauseOverlayVisibility", pauseOverlay.visibility)
        outState.putInt("countdownTextVisibility", countdownText.visibility)
        outState.putInt("gridLayoutVisibility", gridLayout.visibility)
        outState.putInt("pauseButtonVisibility", pauseButton.visibility)
        outState.putBooleanArray("cardsFlipped", cards.map { it.isFlipped }.toBooleanArray())
        outState.putBooleanArray("cardsMatched", cards.map { it.isMatched }.toBooleanArray())
        outState.putInt("firstCardIndex", firstCard?.let { cards.indexOf(it) } ?: -1)
        outState.putInt("secondCardIndex", secondCard?.let { cards.indexOf(it) } ?: -1)
        outState.putIntArray("cardValues", cardValues.toIntArray())
        outState.putInt("countdownIndex", countdownManager.getIndex())
        outState.putBoolean("countdownInProgress", countdownManager.isInProgress())
        outState.putLong("timerRemainingTimeMs", timerProgressBar.getRemainingTimeSeconds() * 1000L)
        outState.putBoolean("timerIsRunning", timerProgressBar.isRunning())
        outState.putLong("previewRemaining", previewRemaining)
        outState.putBoolean("isPreviewPhase", isPreviewPhase)
        outState.putInt("currentLevel", currentLevel)
        outState.putInt("pendingFlipCardIndex", pendingFlipCardIndex)
        starManager.saveState(outState)
        outState.putLong("STATS_START_TIME", gameStatsManager.getStartTime())
        saveGameStats(outState)
    }

    private fun startNewGame() {
        if (pauseMenu.isPaused) {
            pauseMenu.resume()
        } else {
            pauseOverlay.visibility = View.GONE
        }

        timerProgressBar.stop()
        timerProgressBar.reset()

        gridLayout.isEnabled = true
        firstCard = null
        secondCard = null
        isFlipping = false

        when (currentLevel) {
            1 -> {
                boardRows = 4
                boardCols = 4
                bombCount = 0
            }

            2 -> {
                boardRows = 4
                boardCols = 4
                bombCount = 2
            }

            3 -> {
                boardRows = 4
                boardCols = 4
                bombCount = 4
            }

            4 -> {
                boardRows = 5
                boardCols = 5
                bombCount = 1
            }

            5 -> {
                boardRows = 5
                boardCols = 5
                bombCount = 3
            }

            else -> {
                boardRows = 5
                boardCols = 5
                bombCount = 5
            }
        }

        val cardCount = boardRows * boardCols

        val pairCount = (cardCount - bombCount) / 2
        val selectedImages = cardImages.shuffled().take(pairCount)
        cardValues =
            (selectedImages + selectedImages + List(bombCount) { BOMB_VALUE }).shuffled()

        cards = createBoard(cardValues)

        isPreviewPhase = true
        showAllCardsInitially()
    }

    private fun createBoard(
        cardValues: List<Int>,
        flippedStates: BooleanArray? = null,
        matchedStates: BooleanArray? = null
    ): MutableList<Card> {
        val cards = mutableListOf<Card>()
        gridLayout.removeAllViews()
        gridLayout.rowCount = boardRows
        gridLayout.columnCount = boardCols

        for (i in 0 until boardRows) {
            for (j in 0 until boardCols) {
                val index = i * boardCols + j
                if (index >= cardValues.size) continue

                val imageView = ImageView(this).apply {
                    setImageResource(0)
                    setBackgroundResource(R.drawable.bg_rounded_card)
                    layoutParams = GridLayout.LayoutParams().apply {
                        width = 0
                        height = 0
                        rowSpec = GridLayout.spec(i, 1f)
                        columnSpec = GridLayout.spec(j, 1f)
                        setMargins(8, 8, 8, 8)
                    }
                    scaleType = ImageView.ScaleType.CENTER_INSIDE
                    adjustViewBounds = true
                    isEnabled = true
                }

                val card = Card(imageView, cardValues[index])
                if (flippedStates != null) card.isFlipped = flippedStates.getOrElse(index) { false }
                if (matchedStates != null) card.isMatched = matchedStates.getOrElse(index) { false }

                if (card.isFlipped || card.isMatched) {
                    flipCard(card, true)
                } else {
                    flipCard(card, false)
                }

                imageView.setOnClickListener { onCardClick(card) }
                gridLayout.addView(imageView)
                cards.add(card)
            }
        }
        return cards
    }

    private fun restoreGameState(savedInstanceState: Bundle) {
        boardRows = savedInstanceState.getInt("boardRows")
        boardCols = savedInstanceState.getInt("boardCols")
        isFlipping = savedInstanceState.getBoolean("isFlipping")
        pauseOverlay.visibility = savedInstanceState.getInt("pauseOverlayVisibility")
        countdownText.visibility = savedInstanceState.getInt("countdownTextVisibility")
        gridLayout.visibility = savedInstanceState.getInt("gridLayoutVisibility")
        pauseButton.visibility = savedInstanceState.getInt("pauseButtonVisibility")
        isPreviewPhase = savedInstanceState.getBoolean("isPreviewPhase", false)
        currentLevel = savedInstanceState.getInt("currentLevel", 1)
        pendingFlipCardIndex = savedInstanceState.getInt("pendingFlipCardIndex", -1)
        starManager.restoreState(savedInstanceState)

        val countdownIndex = savedInstanceState.getInt("countdownIndex", 0)
        val countdownInProgress = savedInstanceState.getBoolean("countdownInProgress", false)

        val savedCardValues = savedInstanceState.getIntArray("cardValues")
        cardValues = savedCardValues?.toList() ?: emptyList()

        val timerRemainingTimeMs = savedInstanceState.getLong("timerRemainingTimeMs", BASE_TIME_SECONDS * 1000L)
        val timerIsRunning = savedInstanceState.getBoolean("timerIsRunning", false)

        timerProgressBar.setRemainingTimeMs(timerRemainingTimeMs.coerceAtLeast(1L))

        if (timerIsRunning && pauseOverlay.visibility != View.VISIBLE) {
            timerProgressBar.start()
        }

        if (boardRows != 0 && boardCols != 0 && cardValues.isNotEmpty()) {
            val cardCount = boardRows * boardCols
            val flippedArray =
                savedInstanceState.getBooleanArray("cardsFlipped") ?: BooleanArray(cardCount)
            val matchedArray =
                savedInstanceState.getBooleanArray("cardsMatched") ?: BooleanArray(cardCount)

            cards = createBoard(cardValues, flippedArray, matchedArray)

            val firstCardIndex = savedInstanceState.getInt("firstCardIndex", -1)
            val secondCardIndex = savedInstanceState.getInt("secondCardIndex", -1)
            if (firstCardIndex in cards.indices) firstCard = cards[firstCardIndex]
            if (secondCardIndex in cards.indices) secondCard = cards[secondCardIndex]

            if (firstCard != null && secondCard != null) {
                Handler(Looper.getMainLooper()).postDelayed({
                    checkMatch()
                }, 500)
            } else if (firstCard != null && !isFlipping) {
                flipCard(firstCard!!, true)
            }

            if (countdownInProgress) {
                countdownManager.startCountdown(countdownIndex)
            }
        }

        pauseMenu.syncWithOverlay()

        previewRemaining = savedInstanceState.getLong("previewRemaining", 0L)
        if (previewRemaining > 0L) {
            isPreviewPhase = true
            cards.forEach {
                flipCard(it, true)
                it.view.isEnabled = false
            }

            runDelayed(previewRemaining, {
                cards.forEach {
                    flipCard(it, false)
                    it.view.isEnabled = true
                }
                previewRemaining = 0L
                isPreviewPhase = false
                timerProgressBar.start()
            }, true)
        }

        if (pendingFlipCardIndex != -1 && pendingFlipCardIndex in cards.indices) {
            val card = cards[pendingFlipCardIndex]
            flipCard(card, false)
            card.isFlipped = false
            pendingFlipCardIndex = -1
            isFlipping = false
            gridLayout.isEnabled = true
        }
        restoreGameStats(savedInstanceState)
    }

    private fun showAllCardsInitially() {
        isPreviewPhase = true
        previewRemaining = 2000L
        cards.forEach {
            flipCard(it, true)
            it.view.isEnabled = false
        }

        runDelayed(previewRemaining, {
            cards.forEach {
                flipCard(it, false)
                it.view.isEnabled = true
            }
            previewRemaining = 0L
            isPreviewPhase = false
            timerProgressBar.start()
        }, true)
    }

    private fun onCardClick(card: Card) {

        if (pauseMenu.isPaused || isPreviewPhase) return
        if (isFlipping || card.isFlipped || card.isMatched || !gridLayout.isEnabled) return

        if (card.value == BOMB_VALUE) {
            isFlipping = true
            gridLayout.isEnabled = false

            flipCard(card, true)
            timerProgressBar.subtractTime(10)
            card.isMatched = true
            Toast.makeText(this, "Bomba! -10s", Toast.LENGTH_SHORT).show()
            SoundManager.play(this, R.raw.explosion)

            val firstToFlip = firstCard
            if (firstToFlip != null && firstToFlip != card) {
                pendingFlipCardIndex = cards.indexOf(firstToFlip)

                runDelayed(1200, {
                    flipCard(firstToFlip, false)
                    firstToFlip.isFlipped = false
                    pendingFlipCardIndex = -1
                    isFlipping = false
                    gridLayout.isEnabled = true
                }, updatePreview = false)
            } else {
                runDelayed(150, {
                    isFlipping = false
                    gridLayout.isEnabled = true
                }, updatePreview = false)
            }

            firstCard = null
            secondCard = null
            return
        }

        flipCard(card, true)

        if (firstCard == null) {
            firstCard = card
        } else if (secondCard == null) {
            secondCard = card
            checkMatch()
        }
    }

    private fun checkMatch() {
        if (firstCard == null || secondCard == null) return

        val isMatch = firstCard!!.value == secondCard!!.value

        gameStatsManager.registerAttempt(isMatch)

        if (isMatch) {
            firstCard!!.isMatched = true
            secondCard!!.isMatched = true
            starManager.increment()
            firstCard = null
            secondCard = null

            if (cards.all { it.isMatched || it.value == BOMB_VALUE }) {
                currentLevel++

                timerProgressBar.pause()
                timerProgressBar.addTime(30)

                runDelayed(delay = 2200, action = { startNewGame() }, updatePreview = false)
            }
        } else {
            isFlipping = true
            runDelayed(1000, {
                flipCard(firstCard!!, false)
                flipCard(secondCard!!, false)
                firstCard = null
                secondCard = null
                isFlipping = false
            }, updatePreview = false)
        }
    }

    private fun flipCard(card: Card, showFront: Boolean) {
        if (showFront) {
            if (card.value == BOMB_VALUE) {
                card.view.setImageResource(R.drawable.bomb_card)
            } else {
                card.view.setImageResource(card.value)
            }
            card.view.background = null
            card.isFlipped = true
            card.view.animate().scaleX(1.1f).scaleY(1.1f).setDuration(150)
                .start()
        } else {
            card.view.setImageResource(0)
            card.view.setBackgroundResource(R.drawable.bg_rounded_card)
            card.isFlipped = false
            card.view.animate().scaleX(1f).scaleY(1f).setDuration(150)
                .start()
        }
    }

    private fun runDelayed(delay: Long, action: () -> Unit, updatePreview: Boolean = false) {
        var remaining = delay
        val handler = Handler(Looper.getMainLooper())
        val interval = 16L // ~60fps

        val runnable = object : Runnable {
            override fun run() {
                if (pauseMenu.isPaused) {
                    if (isGameEnding) return
                    handler.postDelayed(this, interval)
                    return
                }

                remaining -= interval
                if (updatePreview) {
                    previewRemaining = remaining.coerceAtLeast(0L)
                }
                if (remaining <= 0) {
                    action()
                } else {
                    handler.postDelayed(this, interval)
                }
            }
        }
        handler.postDelayed(runnable, interval)
    }

    data class Card(val view: ImageView, val value: Int) {
        var isFlipped: Boolean = false
        var isMatched: Boolean = false
    }

    private fun handleGameOver() {
        isGameEnding = true

        gridLayout.isEnabled = false
        cards.forEach { it.view.isEnabled = false }
        pauseOverlay.visibility = View.GONE

        showGameOverDialog(
            categoryKey = GameKeys.CATEGORY_MEMORY,
            gameKey = GameKeys.GAME_CARD_MATCH,
            starManager = starManager,
            timerProgressBar = timerProgressBar,
            countdownManager = countdownManager,
            currentBestScore = currentBestScore,
            onRestartAction = {

                if (starManager.starCount > currentBestScore) {
                    currentBestScore = starManager.starCount
                }

                currentLevel = 1
                pendingFlipCardIndex = -1

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