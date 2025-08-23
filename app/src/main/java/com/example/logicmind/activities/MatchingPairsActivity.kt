package com.example.logicmind.activities
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.gridlayout.widget.GridLayout
import com.example.logicmind.R
import com.example.logicmind.common.GameCountdownManager
import com.example.logicmind.common.GameTimerProgressBar

//TODO: Przycisk pauzy i okna z pauzą do oddzilnego pliku w common, odliczanie gwiazdek tak samo
//TODO: Ukryj widok gwiazdki z licznikiem na czas ekranu odliczania
class MatchingPairsActivity : AppCompatActivity() {

    private lateinit var cards: MutableList<Card> // Lista wszystkich kart w grze
    private var firstCard: Card? = null // Pierwsza wybrana karta do dopasowania
    private var secondCard: Card? = null // Druga wybrana karta do dopasowania
    private var isFlipping = false // Flaga zapobiegająca wielokrotnemu odwracaniu kart
    private lateinit var gridLayout: GridLayout // Siatka do wyświetlania kart
    private lateinit var countdownText: TextView // Pole tekstowe dla odliczania
    private lateinit var pauseButton: ImageView // Przycisk pauzy
    private lateinit var pauseOverlay: View // Nakładka menu pauzy
    private var boardRows: Int = 4
    private var boardCols: Int = 4
    private lateinit var countdownManager: GameCountdownManager // Manager odliczania
    private var currentLevel = 1
    private lateinit var timerProgressBar: GameTimerProgressBar
    private var starCount: Int = 0
    private lateinit var starCountText: TextView

    // Lista obrazów kart używanych w grze
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

    private lateinit var cardValues: List<Int> // Lista wartości kart (ID obrazów)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_matching_pairs)
        supportActionBar?.hide()

        gridLayout = findViewById(R.id.gridLayout)
        countdownText = findViewById(R.id.countdownText)
        pauseButton = findViewById(R.id.pauseButton)
        pauseOverlay = findViewById(R.id.pauseOverlay)
        timerProgressBar = findViewById(R.id.gameTimerProgressBar)
        starCountText = findViewById(R.id.starCountText)
        updateStarCountUI()

        // Inicjalizacja paska czasu
        timerProgressBar.setTotalTime(60) // Ustaw czas na 60 sekund
        timerProgressBar.setOnFinishCallback {
            runOnUiThread {
                Toast.makeText(this, "Czas minął! Koniec gry!", Toast.LENGTH_LONG).show()
                gridLayout.isEnabled = false
                cards.forEach { it.view.isEnabled = false }
                finish()
            }
        }

        // Inicjalizacja managera odliczania
        countdownManager = GameCountdownManager(
            countdownText = countdownText,
            gameView = gridLayout,
            pauseButton = pauseButton
        ) {
            starCount = 0
            updateStarCountUI()
            startNewGame()
        }

        setupPauseMenu()

        pauseButton.setOnClickListener {
            pauseOverlay.visibility = View.VISIBLE
            timerProgressBar.pause()
        }

        if (savedInstanceState == null) {
            countdownManager.startCountdown() // Odliczanie tylko na samym początku
        } else {
            restoreGameState(savedInstanceState)
        }
    }


    // Zapisuje stan gry, gdy aktywność jest pauzowana lub niszczona
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
        outState.putInt("firstCardIndex", cards.indexOf(firstCard))
        outState.putInt("secondCardIndex", cards.indexOf(secondCard))
        outState.putIntArray("cardValues", cardValues.toIntArray())
        outState.putInt("countdownIndex", countdownManager.getIndex())
        outState.putBoolean("countdownInProgress", countdownManager.isInProgress())
        outState.putLong("timerRemainingTimeMs", timerProgressBar.getRemainingTimeSeconds() * 1000L)
        outState.putBoolean("timerIsRunning", timerProgressBar.isRunning())
    }

    // Konfiguruje menu pauzy z przyciskami wznów, restart, wyjście i pomoc
    private fun setupPauseMenu() {
        pauseOverlay.findViewById<Button>(R.id.btnResume).setOnClickListener {
            pauseOverlay.visibility = View.GONE // Wznów grę
            timerProgressBar.start() // Wznów timer
        }

        pauseOverlay.findViewById<Button>(R.id.btnRestart).setOnClickListener {
            pauseOverlay.visibility = View.GONE // Restart gry
            timerProgressBar.reset() // Resetuj timer
            countdownManager.startCountdown()
        }

        pauseOverlay.findViewById<Button>(R.id.btnExit).setOnClickListener {
            finish() // Zakończ aktywność
        }

        pauseOverlay.findViewById<Button>(R.id.btnHelp).setOnClickListener {
            Log.d("MatchingPairs", "Przycisk pomocy kliknięty") // Placeholder dla akcji pomocy
        }
    }

    // Inicjalizuje nową grę
    private fun startNewGame() {
        gridLayout.removeAllViews() // Wyczyść poprzednie karty
        updateStarCountUI()
        gridLayout.isEnabled = true // Włącz interakcje
        firstCard = null
        secondCard = null
        isFlipping = false

        // Ustaw wymiary siatki
        gridLayout.rowCount = boardRows
        gridLayout.columnCount = boardCols

        // Tworzenie par kart
        val cardCount = boardRows * boardCols
        val pairCount = cardCount / 2
        val selectedImages = cardImages.shuffled().take(pairCount)
        cardValues = (selectedImages + selectedImages).shuffled()

        cards = mutableListOf()

        // Tworzenie kart i dodawanie ich do siatki
        for (i in 0 until boardRows) {
            for (j in 0 until boardCols) {
                val index = i * boardCols + j
                if (index >= cardValues.size) continue

                val imageView = ImageView(this).apply {
                    setImageResource(0) // Początkowo brak obrazu
                    setBackgroundResource(R.drawable.bg_rounded_card) // Tył karty
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
                imageView.setOnClickListener { onCardClick(card) }

                gridLayout.addView(imageView)
                cards.add(card)
            }
        }

        // Pokaż wszystkie karty na początku, a potem je ukryj
        showAllCardsInitially()
    }

    private fun updateStarCountUI() {
        starCountText.text = starCount.toString()
    }

    // Przywraca stan gry z zapisanego Bundle
    private fun restoreGameState(savedInstanceState: Bundle) {
        boardRows = savedInstanceState.getInt("boardRows")
        boardCols = savedInstanceState.getInt("boardCols")
        isFlipping = savedInstanceState.getBoolean("isFlipping")
        pauseOverlay.visibility = savedInstanceState.getInt("pauseOverlayVisibility")
        countdownText.visibility = savedInstanceState.getInt("countdownTextVisibility")
        gridLayout.visibility = savedInstanceState.getInt("gridLayoutVisibility")
        pauseButton.visibility = savedInstanceState.getInt("pauseButtonVisibility")
        val countdownIndex = savedInstanceState.getInt("countdownIndex", 0)
        val countdownInProgress = savedInstanceState.getBoolean("countdownInProgress", false)

        val savedCardValues = savedInstanceState.getIntArray("cardValues")
        cardValues = savedCardValues?.toList() ?: emptyList()

        // Przywróć stan timera
        val timerRemainingTimeMs = savedInstanceState.getLong("timerRemainingTimeMs", 60000L)
        val timerIsRunning = savedInstanceState.getBoolean("timerIsRunning", false)
        timerProgressBar.setTotalTime((timerRemainingTimeMs / 1000).toInt())
        timerProgressBar.reset()
        timerProgressBar.addTime(-(60 - (timerRemainingTimeMs / 1000).toInt())) // Ustaw pozostały czas
        if (timerIsRunning && pauseOverlay.visibility != View.VISIBLE) {
            timerProgressBar.start()
        }

        if (boardRows != 0 && boardCols != 0 && cardValues.isNotEmpty()) {
            gridLayout.removeAllViews()
            gridLayout.rowCount = boardRows
            gridLayout.columnCount = boardCols

            val cardCount = boardRows * boardCols
            val flippedArray = savedInstanceState.getBooleanArray("cardsFlipped") ?: BooleanArray(cardCount)
            val matchedArray = savedInstanceState.getBooleanArray("cardsMatched") ?: BooleanArray(cardCount)

            cards = mutableListOf()

            // Odtwórz karty
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
                    }

                    val card = Card(imageView, cardValues[index])
                    card.isFlipped = flippedArray.getOrElse(index) { false }
                    card.isMatched = matchedArray.getOrElse(index) { false }

                    // Ustaw stan wizualny karty
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

            // Przywróć wybrane karty
            val firstCardIndex = savedInstanceState.getInt("firstCardIndex", -1)
            val secondCardIndex = savedInstanceState.getInt("secondCardIndex", -1)
            if (firstCardIndex in cards.indices) firstCard = cards[firstCardIndex]
            if (secondCardIndex in cards.indices) secondCard = cards[secondCardIndex]

            // Kontynuuj sprawdzanie pary, jeśli była w trakcie
            if (firstCard != null && secondCard != null) {
                Handler(Looper.getMainLooper()).postDelayed({
                    checkMatch()
                }, 500)
            }

            // Kontynuuj odliczanie, jeśli było aktywne
            if (countdownInProgress) {
                countdownManager.startCountdown(countdownIndex)
            }
        }
    }

    // Pokazuje wszystkie karty na początku gry przez 2 sekundy
    private fun showAllCardsInitially() {
        cards.forEach { flipCard(it, true) }
        Handler(Looper.getMainLooper()).postDelayed({
            cards.forEach { flipCard(it, false) }
            timerProgressBar.start()
        }, 2000)
    }

    // Obsługuje kliknięcie karty
    private fun onCardClick(card: Card) {
        if (isFlipping || card.isFlipped || card.isMatched || !gridLayout.isEnabled) return // Ignoruj, jeśli karta jest zablokowana

        flipCard(card, true) // Odwróć kartę

        if (firstCard == null) {
            firstCard = card // Zapisz pierwszą kartę
        } else if (secondCard == null) {
            secondCard = card // Zapisz drugą kartę
            checkMatch() // Sprawdź dopasowanie
        }
    }

    // Sprawdza, czy wybrane karty pasują do siebie
    private fun checkMatch() {
        if (firstCard == null || secondCard == null) return

        if (firstCard!!.value == secondCard!!.value) {
            // Karty pasują
            firstCard!!.isMatched = true
            secondCard!!.isMatched = true
            starCount += 1
            updateStarCountUI()
            firstCard = null
            secondCard = null

            if (cards.all { it.isMatched }) {
                currentLevel++

                timerProgressBar.pause()  // wstrzymaj odliczanie
                timerProgressBar.addTime(10) // dodaj czas

                Handler(Looper.getMainLooper()).postDelayed({
                    startNewGame()
                    timerProgressBar.start() // wznow odliczanie
                }, 2200)
            }
        } else {
            // Karty nie pasują, odwróć je z powrotem po sekundzie
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

    // Odwraca kartę
    private fun flipCard(card: Card, showFront: Boolean) {
        if (showFront) {
            card.view.setImageResource(card.value) // Pokaż obraz karty
            card.view.background = null
            card.isFlipped = true
            card.view.animate().scaleX(1.1f).scaleY(1.1f).setDuration(150).start() // Animacja powiększenia
        } else {
            card.view.setImageResource(0) // Ukryj obraz
            card.view.setBackgroundResource(R.drawable.bg_rounded_card) // Pokaż tło
            card.isFlipped = false
            card.view.animate().scaleX(1f).scaleY(1f).setDuration(150).start() // Animacja powrotu do normalnego rozmiaru
        }
    }

    // Klasa danych dla karty
    data class Card(val view: ImageView, val value: Int) {
        var isFlipped: Boolean = false // Czy karta jest odwrócona
        var isMatched: Boolean = false // Czy karta jest dopasowana
    }
}