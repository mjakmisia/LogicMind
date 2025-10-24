package com.example.logicmind.activities
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.gridlayout.widget.GridLayout
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.Log
import com.example.logicmind.R
import com.example.logicmind.common.GameCountdownManager
import com.example.logicmind.common.GameTimerProgressBar
import com.example.logicmind.common.PauseMenu
import com.example.logicmind.common.StarManager
import com.google.firebase.database.FirebaseDatabase

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
    private var bombCount: Int = 0
    private lateinit var countdownManager: GameCountdownManager // Manager odliczania
    private var currentLevel = 1 // Aktualny poziom gry
    private var isGameEnding = false // Flaga końca gry
    private lateinit var timerProgressBar: GameTimerProgressBar // Pasek postępu czasu gry
    private lateinit var starManager: StarManager // Manager gwiazdek
    private lateinit var pauseMenu: PauseMenu // Menu pauzy gry
    private var previewRemaining: Long = 0L // Pozostały czas do ukrycia kart w fazie preview (ms)
    private var isPreviewPhase: Boolean = false // Flaga aktywnej fazy preview (karty przodem na starcie rundy)

    // Pola dla SoundPool – obsługa nakładających się dźwięków eksplozji
    private lateinit var soundPool: SoundPool
    private var explosionSoundId: Int = 0

    companion object {
        const val BOMB_VALUE = -1 // Specjalna wartość dla bomby
    }

    // Lista obrazów kart używanych w grze
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

    private lateinit var cardValues: List<Int> // Lista wartości kart (ID obrazów)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_card_match)

        // Inicjalizacja SoundPool
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(5)  // Do 5 dźwięków naraz
            .setAudioAttributes(audioAttributes)
            .build()

        // Preload dźwięku – brak opóźnień przy pierwszym play()
        explosionSoundId = soundPool.load(this, R.raw.explosion, 1)
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status != 0) {
                android.util.Log.e("CardMatchActivity", "Błąd ładowania dźwięku: $status")
            }
        }

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
        timerProgressBar.setTotalTime(90) // Ustaw czas na 1,5 minuty
        timerProgressBar.setOnFinishCallback {
            runOnUiThread {
                isGameEnding = true
                Toast.makeText(this, "Czas minął! Koniec gry!", Toast.LENGTH_LONG).show()
                gridLayout.isEnabled = false
                cards.forEach { it.view.isEnabled = false }
                pauseOverlay.visibility = View.GONE
                onGameFinished(GameKeys.CATEGORY_MEMORY, GameKeys.GAME_CARD_MATCH, getString(R.string.card_match))
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
            onExit = {
                onGameFinished(GameKeys.CATEGORY_MEMORY, GameKeys.GAME_CARD_MATCH, getString(R.string.card_match))
                finish() }, // Kończy aktywność
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
        starManager.saveState(outState)
    }

    override fun onDestroy() {
        super.onDestroy()
        timerProgressBar.cancel() // Zatrzymaj CountDownTimer
        countdownManager.cancel() // Usuń handlery odliczania
        soundPool.release()
    }

    // Inicjalizuje nową grę
    private fun startNewGame() {
        // Upewnij się że menu pauzy jest schowane
        if (pauseMenu.isPaused) {
            pauseMenu.resume()
        } else {
            pauseOverlay.visibility = View.GONE
        }

        gridLayout.isEnabled = true // Włącz interakcje
        firstCard = null
        secondCard = null
        isFlipping = false

        //Ustaw wymiary planszy i liczbę bomb
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

        // Tworzenie par kart z bombami w zależności od levelu
        val cardCount = boardRows * boardCols

        val pairCount = (cardCount - bombCount) / 2
        val selectedImages = cardImages.shuffled().take(pairCount)
        cardValues = (selectedImages + selectedImages + List(bombCount) { BOMB_VALUE }).shuffled() // Dodaj bomby i wymieszaj

        cards = createBoard(cardValues) // Tworzy, ustawia wymiary i czyści siatkę

        isPreviewPhase = true
        showAllCardsInitially() // Pokaż wszystkie karty na początku rundy
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
                    flipCard(card, true)
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
        currentLevel = savedInstanceState.getInt("currentLevel", 1)
        starManager.restoreState(savedInstanceState)

        val countdownIndex = savedInstanceState.getInt("countdownIndex", 0)
        val countdownInProgress = savedInstanceState.getBoolean("countdownInProgress", false)

        // Przywracanie wartości kart
        val savedCardValues = savedInstanceState.getIntArray("cardValues")
        cardValues = savedCardValues?.toList() ?: emptyList()

        // Przywracanie stanu timera
        val timerRemainingTimeMs = savedInstanceState.getLong("timerRemainingTimeMs", 90 * 1000L)
        val timerIsRunning = savedInstanceState.getBoolean("timerIsRunning", false)

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
            }, true) // Włącz aktualizację previewRemaining
        }
    }

    // Pokazuje wszystkie karty na początku gry przez 2 sekundy
    private fun showAllCardsInitially() {
        isPreviewPhase = true
        previewRemaining = 2000L
        cards.forEach {
            flipCard(it, true)
            it.view.isEnabled = false // Wyłącz interakcje i dźwięk podczas preview
        }

        runDelayed(previewRemaining, {
            cards.forEach {
                flipCard(it, false)
                it.view.isEnabled = true // Włącz interakcje po ukryciu
            }
            previewRemaining = 0L
            isPreviewPhase = false
            timerProgressBar.start() // Start timera po ukryciu kart
        }, true)
    }

    // Obsługuje kliknięcie karty
    private fun onCardClick(card: Card) {
        if (pauseMenu.isPaused || isPreviewPhase) return // Ignoruj kliknięcia, gdy gra jest w trybie preview lub pauzy
        if (isFlipping || card.isFlipped || card.isMatched || !gridLayout.isEnabled) return // Ignoruj, jeśli karta jest zablokowana

        if (card.value == BOMB_VALUE) {
            isFlipping = true
            gridLayout.isEnabled = false

            flipCard(card, true) // Odwróć kartę
            timerProgressBar.subtractTime(10) // Odejmij 10 sekund
            card.isMatched = true // Odkryta na stałe
            Toast.makeText(this, "Bomba! -10s", Toast.LENGTH_SHORT).show()

            // Odtwarzanie dźwięku eksplozji
            if (explosionSoundId != 0) {
                soundPool.play(explosionSoundId, 1.0f, 1.0f, 1, 0, 1.0f)  // Głośność L/R=1, priorytet=1, bez loopa, prędkość=1
            }

            // Zapisz referencję do pierwszej karty przed resetem
            val firstToFlip = firstCard
            if (firstToFlip != null && firstToFlip != card) {
                // Bomba jest drugą kartą – odwróć pierwszą kartę z powrotem po opóźnieniu
                runDelayed(1200, {
                    flipCard(firstToFlip, false)
                    isFlipping = false
                    gridLayout.isEnabled = true  // Włącz siatkę po hide
                }, updatePreview = false)
            } else {
                // Jeśli bomba jako pierwsza – odblokuj od razu po animacji
                runDelayed(150, {
                    isFlipping = false
                    gridLayout.isEnabled = true
                }, updatePreview = false)
            }

            firstCard = null
            secondCard = null
            return // Nie kontynuuj logiki match
        }

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
            starManager.increment()
            firstCard = null
            secondCard = null

            // Sprawdzenie, czy wszystkie karty zostały dopasowane
            if (cards.all { it.isMatched || it.value == BOMB_VALUE }) {
                currentLevel++

                timerProgressBar.pause()
                timerProgressBar.addTime(30)

                runDelayed(delay = 2200, action = { startNewGame() }, updatePreview = false)
            }
        } else {
            // Karty nie pasują, odwróć je z powrotem po sekundzie
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

    // Odwraca kartę
    private fun flipCard(card: Card, showFront: Boolean) {
        if (showFront) {
            if (card.value == BOMB_VALUE) {
                card.view.setImageResource(R.drawable.bomb_card) // Pokaż ikonę bomby
            } else {
                card.view.setImageResource(card.value) // Pokaż ikonę owocu
            }
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
    private fun runDelayed(delay: Long, action: () -> Unit, updatePreview: Boolean = false) {
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
                if (updatePreview) {
                    previewRemaining = remaining.coerceAtLeast(0L) // Aktualizuj tylko dla preview
                }
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
        if (!isGameEnding && !pauseMenu.isPaused && !isChangingConfigurations) {
            pauseMenu.pause()
        }
    }

}