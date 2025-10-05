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

class CardMatchActivity : BaseActivity() {

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
    private var currentLevel = 1 // Aktualny poziom gry
    private lateinit var timerProgressBar: GameTimerProgressBar // Pasek postępu czasu gry
    private var starCount: Int = 0 // Licznik zdobytych gwiazdek
    private lateinit var starCountText: TextView // Pole tekstowe wyświetlające liczbę gwiazdek
    private lateinit var pauseMenu: PauseMenu // Menu pauzy gry
    private var previewRemaining: Long = 0L // Pozostały czas do ukrycia kart w fazie preview (ms)
    private var isPreviewPhase: Boolean = false // Flaga aktywnej fazy preview (karty przodem na starcie rundy)

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
        setContentView(R.layout.activity_card_match)
        supportActionBar?.hide()

        // Inicjalizacja widoków
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
            // Callback wywoływany po zakończeniu odliczania początkowego
            starCount = 0
            updateStarCountUI()
            startNewGame()
        }

        // Inicjalizacja menu pauzy
        pauseMenu = PauseMenu(
            context = this,
            pauseOverlay = pauseOverlay,
            pauseButton = pauseButton,
            onRestart = {
                if (pauseMenu.isPaused) pauseMenu.resume()
                timerProgressBar.reset() // Resetuje timer
                countdownManager.startCountdown() // Rozpoczyna odliczanie początkowe
            },
            onResume = {
                if (!isPreviewPhase) {
                    timerProgressBar.start()
                }
            }, // Wznawia timer po pauzie pod warunkiem że nie jesteśmy w preview
            onPause = {
                if (!isPreviewPhase) {
                    timerProgressBar.pause()
                }
            }, // Zatrzymuje timer podczas pauzy pod warunkiem że nie jesteśmy w preview
            onExit = { finish() }, // Kończy aktywność
            instructionTitle = getString(R.string.instructions),
            instructionMessage = getString(R.string.card_match_instruction),
        )

        // Sprawdzenie, czy gra jest uruchamiana po raz pierwszy
        if (savedInstanceState == null) {
            countdownManager.startCountdown() // Rozpoczyna odliczanie początkowe
        } else {
            restoreGameState(savedInstanceState) // Przywraca stan gry
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
        outState.putInt("starCount", starCount)
        outState.putLong("previewRemaining", previewRemaining)
        outState.putBoolean("isPreviewPhase", isPreviewPhase)
    }

    override fun onDestroy() {
        super.onDestroy()
        timerProgressBar.cancel() // Zatrzymaj CountDownTimer
        countdownManager.cancel() // Usuń handlery odliczania
    }

    // Inicjalizuje nową grę
    private fun startNewGame() {
        // Upewnij się że menu pauzy jest schowane
        if (pauseMenu.isPaused) {
            pauseMenu.resume()
        } else {
            pauseOverlay.visibility = View.GONE
        }

        updateStarCountUI()
        gridLayout.isEnabled = true // Włącz interakcje
        firstCard = null
        secondCard = null
        isFlipping = false

        // Tworzenie par kart
        val cardCount = boardRows * boardCols
        val pairCount = cardCount / 2
        val selectedImages = cardImages.shuffled().take(pairCount)
        cardValues = (selectedImages + selectedImages).shuffled()

        cards = createBoard(cardValues) // Tworzy, ustawia wymiary i czyści siatkę

        isPreviewPhase = true
        showAllCardsInitially() // Pokaż wszystkie karty na początku rundy
    }

    // Aktualizuje tekst wyświetlający liczbę gwiazdek
    private fun updateStarCountUI() {
        starCountText.text = starCount.toString()
    }

    // Tworzy planszę z kartami; w trybie restore ustawia stany flipped/matched
    private fun createBoard(cardValues: List<Int>, flippedStates: BooleanArray? = null, matchedStates: BooleanArray? = null): MutableList<Card> {
        val cards = mutableListOf<Card>()
        gridLayout.removeAllViews() // Wyczyść siatkę (jeśli nie zrobione wcześniej)
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

                // Ustaw stan wizualny (dla restore) lub domyślny
                if (card.isFlipped || card.isMatched) {
                    flipCard(card, true) // Użyj flipCard dla spójności animacji
                }

                imageView.setOnClickListener { onCardClick(card) }
                gridLayout.addView(imageView)
                cards.add(card)
            }
        }
        return cards
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
        isPreviewPhase = savedInstanceState.getBoolean("isPreviewPhase", false)

        val countdownIndex = savedInstanceState.getInt("countdownIndex", 0)
        val countdownInProgress = savedInstanceState.getBoolean("countdownInProgress", false)

        // Przywracanie wartości kart
        val savedCardValues = savedInstanceState.getIntArray("cardValues")
        cardValues = savedCardValues?.toList() ?: emptyList()

        // Przywracanie stanu timera
        val timerRemainingTimeMs = savedInstanceState.getLong("timerRemainingTimeMs", 60000L)
        val timerIsRunning = savedInstanceState.getBoolean("timerIsRunning", false)

        timerProgressBar.reset()
        timerProgressBar.setRemainingTimeMs(timerRemainingTimeMs.coerceAtLeast(1L))

        if (timerIsRunning && pauseOverlay.visibility != View.VISIBLE) {
            timerProgressBar.start()
        }

        // Odtwarzanie planszy gry
        if (boardRows != 0 && boardCols != 0 && cardValues.isNotEmpty()) {
            val cardCount = boardRows * boardCols
            val flippedArray = savedInstanceState.getBooleanArray("cardsFlipped") ?: BooleanArray(cardCount)
            val matchedArray = savedInstanceState.getBooleanArray("cardsMatched") ?: BooleanArray(cardCount)

            cards = createBoard(cardValues, flippedArray, matchedArray)

            // Przywracanie wybranych kart
            val firstCardIndex = savedInstanceState.getInt("firstCardIndex", -1)
            val secondCardIndex = savedInstanceState.getInt("secondCardIndex", -1)
            if (firstCardIndex in cards.indices) firstCard = cards[firstCardIndex]
            if (secondCardIndex in cards.indices) secondCard = cards[secondCardIndex]

            // Kontynuowanie sprawdzania pary, jeśli była w trakcie
            if (firstCard != null && secondCard != null) {
                Handler(Looper.getMainLooper()).postDelayed({
                    checkMatch()
                }, 500)
            }

            // Kontynuowanie odliczania, jeśli było aktywne
            if (countdownInProgress) {
                countdownManager.startCountdown(countdownIndex)
            }
        }

        pauseMenu.syncWithOverlay()

        // Przywracanie pozostałego czasu preview i wznowienie fazy, jeśli była aktywna
        previewRemaining = savedInstanceState.getLong("previewRemaining", 0L)
        if (previewRemaining > 0L) {
            isPreviewPhase = true
            cards.forEach { flipCard(it, true) }

            runDelayed(previewRemaining) {
                cards.forEach { flipCard(it, false) }
                previewRemaining = 0L
                isPreviewPhase = false
                timerProgressBar.start()
            }
        }
    }

    // Pokazuje wszystkie karty na początku gry przez 2 sekundy
    private fun showAllCardsInitially() {
        isPreviewPhase = true
        previewRemaining = 2000L
        cards.forEach { flipCard(it, true) }

        runDelayed(previewRemaining) {
            cards.forEach { flipCard(it, false) }
            previewRemaining = 0L
            isPreviewPhase = false
            timerProgressBar.start() // Start timera po ukryciu kart
        }
    }

    // Obsługuje kliknięcie karty
    private fun onCardClick(card: Card) {
        if (pauseMenu.isPaused) return // Ignoruj kliknięcia, gdy gra jest wstrzymana
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

            // Sprawdzenie, czy wszystkie karty zostały dopasowane
            if (cards.all { it.isMatched }) {
                currentLevel++

                timerProgressBar.pause()
                timerProgressBar.addTime(10)

                runDelayed(2200) {
                    startNewGame()
                }
            }
        } else {
            // Karty nie pasują, odwróć je z powrotem po sekundzie
            isFlipping = true
            runDelayed(1000) {
                flipCard(firstCard!!, false)
                flipCard(secondCard!!, false)
                firstCard = null
                secondCard = null
                isFlipping = false
            }
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

    // Uruchamia akcję z opóźnieniem, uwzględniając pauzę – jeśli gra jest wstrzymana, akcja zostanie wykonana po wznowieniu
    private fun runDelayed(delay: Long, action: () -> Unit) {
        var remaining = delay
        val handler = Handler(Looper.getMainLooper())
        val interval = 16L // ~60fps, aby odliczanie było płynne

        val runnable = object : Runnable {
            override fun run() {
                if (pauseMenu.isPaused) {
                    // Gra w pauzie – nie zmniejszamy remaining, czekamy do wznowienia
                    handler.postDelayed(this, interval)
                    return
                }

                remaining -= interval
                previewRemaining = remaining.coerceAtLeast(0L)
                if (remaining <= 0) {
                    action() // Wykonanie akcji po upłynięciu czasu
                } else {
                    handler.postDelayed(this, interval) // Kolejna iteracja
                }
            }
        }
        handler.postDelayed(runnable, interval)
    }

    // Klasa danych dla karty
    data class Card(val view: ImageView, val value: Int) {
        var isFlipped: Boolean = false // Czy karta jest odwrócona
        var isMatched: Boolean = false // Czy karta jest dopasowana
    }

    override fun onPause() {
        super.onPause()
        if (!pauseMenu.isPaused && !isChangingConfigurations) {
            pauseMenu.pause()
        }
    }
}