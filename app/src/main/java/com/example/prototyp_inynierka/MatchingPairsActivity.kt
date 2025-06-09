package com.example.prototyp_inynierka

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.gridlayout.widget.GridLayout

class MatchingPairsActivity : AppCompatActivity() {

    private lateinit var cards: MutableList<Card>
    private var firstCard: Card? = null
    private var secondCard: Card? = null
    private var isFlipping = false
    private lateinit var gridLayout: GridLayout
    private lateinit var countdownText: TextView
    private lateinit var pauseButton: ImageView
    private lateinit var pauseOverlay: View
    private var boardRows: Int = 0
    private var boardCols: Int = 0

    private val cardImages = listOf(
        R.drawable.fruit_card_apple,
        R.drawable.fruit_card_banana,
        R.drawable.fruit_card_blueberry,
        R.drawable.fruit_card_lemon,
        R.drawable.fruit_card_orange,
        R.drawable.fruit_card_pineapple,
        R.drawable.fruit_card_strawberry,
        R.drawable.fruit_card_watermelon
    )

    private lateinit var cardValues: List<Int>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("MatchingPairs", "onCreate called")
        setContentView(R.layout.activity_matching_pairs)
        supportActionBar?.hide()

        gridLayout = findViewById(R.id.gridLayout)
        countdownText = findViewById(R.id.countdownText)
        pauseButton = findViewById(R.id.pauseButton)
        pauseOverlay = findViewById(R.id.pauseOverlay)

        setupPauseMenu()

        pauseButton.setOnClickListener {
            pauseOverlay.visibility = View.VISIBLE
            Log.d("MatchingPairs", "Pause overlay shown")
        }

        if (savedInstanceState == null) {
            Log.d("MatchingPairs", "No saved state, starting new game")
            startCountdown()
        } else {
            Log.d("MatchingPairs", "Restoring state")
            restoreGameState(savedInstanceState)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        Log.d("MatchingPairs", "Saving instance state")

        outState.putInt("boardRows", boardRows)
        outState.putInt("boardCols", boardCols)
        outState.putBoolean("isFlipping", isFlipping)
        outState.putInt("pauseOverlayVisibility", pauseOverlay.visibility)
        outState.putInt("countdownTextVisibility", countdownText.visibility)
        outState.putInt("gridLayoutVisibility", gridLayout.visibility)
        outState.putInt("pauseButtonVisibility", pauseButton.visibility)
        outState.putBooleanArray("cardsFlipped", cards.map { it.isFlipped }.toBooleanArray())
        outState.putBooleanArray("cardsMatched", cards.map { it.isMatched }.toBooleanArray())
        outState.putInt("firstCardIndex", cards.indexOf(firstCard))
        outState.putInt("secondCardIndex", cards.indexOf(secondCard))
        outState.putIntArray("cardValues", cardValues.toIntArray())
    }

    private fun setupPauseMenu() {
        pauseOverlay.findViewById<Button>(R.id.btnResume).setOnClickListener {
            pauseOverlay.visibility = View.GONE
            Log.d("MatchingPairs", "Game resumed")
        }

        pauseOverlay.findViewById<Button>(R.id.btnRestart).setOnClickListener {
            pauseOverlay.visibility = View.GONE
            Log.d("MatchingPairs", "Restarting game")
            startCountdown()
        }

        pauseOverlay.findViewById<Button>(R.id.btnExit).setOnClickListener {
            Log.d("MatchingPairs", "Exiting game")
            finish()
        }

        pauseOverlay.findViewById<Button>(R.id.btnHelp).setOnClickListener {
            // Tutaj możesz dodać dialog lub Toast z instrukcjami
            Log.d("MatchingPairs", "Help button clicked")
        }
    }

    private fun startCountdown() {
        Log.d("MatchingPairs", "Starting countdown")
        countdownText.visibility = View.VISIBLE
        gridLayout.visibility = View.INVISIBLE
        pauseButton.visibility = View.INVISIBLE

        val countdownValues = listOf("3", "2", "1", "Start!")
        var index = 0

        val handler = Handler(Looper.getMainLooper())
        val countdownRunnable = object : Runnable {
            override fun run() {
                if (index < countdownValues.size) {
                    countdownText.text = countdownValues[index]
                    index++
                    handler.postDelayed(this, 1000)
                } else {
                    countdownText.visibility = View.GONE
                    gridLayout.visibility = View.VISIBLE
                    pauseButton.visibility = View.VISIBLE
                    startNewGame()
                }
            }
        }
        handler.post(countdownRunnable)
    }

    private fun startNewGame() {
        Log.d("MatchingPairs", "Starting new game")
        gridLayout.removeAllViews()
        firstCard = null
        secondCard = null
        isFlipping = false

        val boardSizes = listOf(Pair(3, 4), Pair(4, 4), Pair(4, 5))
        val (rows, cols) = boardSizes.random()
        boardRows = rows
        boardCols = cols

        gridLayout.rowCount = rows
        gridLayout.columnCount = cols

        val cardCount = rows * cols
        val pairCount = cardCount / 2
        val selectedImages = cardImages.shuffled().take(pairCount)
        cardValues = (selectedImages + selectedImages).shuffled()

        cards = mutableListOf()

        for (i in 0 until rows) {
            for (j in 0 until cols) {
                val imageView = ImageView(this).apply {
                    setImageResource(0) // brak obrazka na start, tło z bg_rounded_card.xml
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
                }

                val card = Card(imageView, cardValues[i * cols + j])
                imageView.setOnClickListener { onCardClick(card) }

                gridLayout.addView(imageView)
                cards.add(card)
            }
        }

        showAllCardsInitially()
    }

    private fun restoreGameState(savedInstanceState: Bundle) {
        Log.d("MatchingPairs", "Restoring game state")
        boardRows = savedInstanceState.getInt("boardRows")
        boardCols = savedInstanceState.getInt("boardCols")
        isFlipping = savedInstanceState.getBoolean("isFlipping")
        pauseOverlay.visibility = savedInstanceState.getInt("pauseOverlayVisibility")
        countdownText.visibility = savedInstanceState.getInt("countdownTextVisibility")
        gridLayout.visibility = savedInstanceState.getInt("gridLayoutVisibility")
        pauseButton.visibility = savedInstanceState.getInt("pauseButtonVisibility")

        val savedCardValues = savedInstanceState.getIntArray("cardValues")
        cardValues = savedCardValues?.toList() ?: emptyList()

        if (boardRows != 0 && boardCols != 0 && cardValues.isNotEmpty()) {
            gridLayout.removeAllViews()
            gridLayout.rowCount = boardRows
            gridLayout.columnCount = boardCols

            val cardCount = boardRows * boardCols
            val flippedArray = savedInstanceState.getBooleanArray("cardsFlipped") ?: BooleanArray(cardCount)
            val matchedArray = savedInstanceState.getBooleanArray("cardsMatched") ?: BooleanArray(cardCount)

            cards = mutableListOf()

            for (i in 0 until boardRows) {
                for (j in 0 until boardCols) {
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
                    }

                    val index = i * boardCols + j
                    val card = Card(imageView, cardValues[index])
                    card.isFlipped = flippedArray.getOrElse(index) { false }
                    card.isMatched = matchedArray.getOrElse(index) { false }

                    if (card.isFlipped || card.isMatched) {
                        imageView.setImageResource(card.value)
                        imageView.background = null
                        imageView.animate().scaleX(1.1f).scaleY(1.1f).setDuration(150).start()
                    } else {
                        imageView.scaleX = 1f
                        imageView.scaleY = 1f
                        imageView.setBackgroundResource(R.drawable.bg_rounded_card)
                        imageView.setImageResource(0)
                    }

                    imageView.setOnClickListener { onCardClick(card) }

                    gridLayout.addView(imageView)
                    cards.add(card)
                }
            }

            val firstCardIndex = savedInstanceState.getInt("firstCardIndex", -1)
            val secondCardIndex = savedInstanceState.getInt("secondCardIndex", -1)
            if (firstCardIndex != -1) firstCard = cards[firstCardIndex]
            if (secondCardIndex != -1) secondCard = cards[secondCardIndex]
        }
    }

    private fun showAllCardsInitially() {
        Log.d("MatchingPairs", "Showing all cards initially")
        cards.forEach { flipCard(it, true) }

        Handler(Looper.getMainLooper()).postDelayed({
            cards.forEach { flipCard(it, false) }
        }, 2000)
    }

    private fun onCardClick(card: Card) {
        if (isFlipping || card.isFlipped || card.isMatched) return

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

        if (firstCard!!.value == secondCard!!.value) {
            firstCard!!.isMatched = true
            secondCard!!.isMatched = true
            firstCard = null
            secondCard = null
        } else {
            isFlipping = true
            Handler(Looper.getMainLooper()).postDelayed({
                flipCard(firstCard!!, false)
                flipCard(secondCard!!, false)
                firstCard = null
                secondCard = null
                isFlipping = false
            }, 1000)
        }
    }

    private fun flipCard(card: Card, showFront: Boolean) {
        if (showFront) {
            card.view.setImageResource(card.value)
            card.view.background = null
            card.isFlipped = true
            card.view.animate().scaleX(1.1f).scaleY(1.1f).setDuration(150).start()
        } else {
            card.view.setImageResource(0)
            card.view.setBackgroundResource(R.drawable.bg_rounded_card)
            card.isFlipped = false
            card.view.animate().scaleX(1f).scaleY(1f).setDuration(150).start()
        }
    }

    data class Card(val view: ImageView, val value: Int) {
        var isFlipped: Boolean = false
        var isMatched: Boolean = false
    }
}


