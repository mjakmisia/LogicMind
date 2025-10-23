package com.example.logicmind.activities
import android.animation.ObjectAnimator
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.Button
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
import kotlin.random.Random

class ColorSequenceActivity : BaseActivity() {
    private var keyButtons: List<KeyButton> = emptyList()   // Lista klawiszy z ich widokami i indeksami
    private var currentSequence = mutableListOf<Int>()      // Sekwencja pokazana przez grę
    private var userSequence = mutableListOf<Int>()         // Sekwencja wpisana przez gracza

    //Flagi
    private var isShowingSequence = false     // Flaga: gra pokazuje sekwencję
    private var isUserTurn = false            // Flaga: tura gracza do wpisania sekwencji
    private var isGameEnding = false          // Flaga zakończenia gry

    // Parametry sekwencji
    private var currentLevel = 1           // Aktualny poziom gry
    private var numKeys = 0                // Liczba klawiszy
    private var currentSeqLength = 0       // Aktualna długość sekwencji do zapamiętania
    private var sequenceShowIndex = 0      // Indeks aktualnie odtwarzanego klawisza w sekwencji

    // UI elementy gry
    private lateinit var gridLayout: GridLayout                  // Siatka do wyświetlania klawiszy
    private lateinit var countdownText: TextView                 // Pole tekstowe dla odliczania
    private lateinit var pauseButton: ImageButton                // Przycisk pauzy
    private lateinit var pauseOverlay: ConstraintLayout          // Nakładka z menu pauzy
    private lateinit var timerProgressBar: GameTimerProgressBar  // Pasek postępu czasu gry
    private lateinit var starManager: StarManager                // Manager gwiazdek
    private lateinit var pauseMenu: PauseMenu                    // Menu pauzy gry
    private lateinit var countdownManager: GameCountdownManager  // Manager odliczania początkowego

    // Pola dla SoundPool – obsługa dźwięków
    private lateinit var soundPool: SoundPool          // Player dla dźwięków nut muzycznych
    private lateinit var sequenceHandler: Handler      // Handler do opóźnień i animacji sekwencji
    private val soundIds = mutableMapOf<Int, Int>()    // Mapa indeks dźwięku -> ID w SoundPool
    private var sequenceDelayRemaining: Long = 0L      // Pozostały czas opóźnienia

    // Mapowanie klawiszy na dźwięki
    private val soundOrder4 = listOf(2, 5, 7, 1)              // C E G B
    private val soundOrder6 = listOf(2, 5, 6, 7, 1, 3)        // C E F G B C (wyższe C)
    private val soundOrder8 = listOf(2, 4, 5, 6, 7, 0, 1, 3)  // C D E F G A B C (wyższe C)

    // Klasy danych pomocnicze
    private data class KeyButton(val view: Button, val index: Int) // Klawisz z widokiem i indeksem
    private data class SequenceConfig(
        val numKeys: Int,      // Liczba klawiszy
        val startLength: Int,  // Początkowa długość sekwencji
        val step: Int,         // Krok zwiększania długości
        val maxLength: Int     // Maksymalna długość sekwencji
    )

    companion object {
        const val BASE_TIME_SECONDS = 90 // Czas gry w sekundach
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_color_sequence)
        supportActionBar?.hide()

        // Inicjalizacja SoundPool
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(8)  // Maksymalnie 8 nakładających się dźwięków
            .setAudioAttributes(audioAttributes)
            .build()

        // Preload dźwięków nut muzycznych
        val soundResources = listOf(
            R.raw.key_sound_a,        // 0: A
            R.raw.key_sound_b,        // 1: B
            R.raw.key_sound_c,        // 2: C
            R.raw.key_sound_c_higher, // 3: C higher
            R.raw.key_sound_d,        // 4: D
            R.raw.key_sound_e,        // 5: E
            R.raw.key_sound_f,        // 6: F
            R.raw.key_sound_g         // 7: G
        )

        for (i in soundResources.indices) {
            soundIds[i] = soundPool.load(this, soundResources[i], 1)
        }

        sequenceHandler = Handler(Looper.getMainLooper())

        // Inicjalizacja widoków
        gridLayout = findViewById(R.id.gridLayout)
        countdownText = findViewById(R.id.countdownText)
        pauseButton = findViewById(R.id.pauseButton)
        pauseOverlay = findViewById(R.id.pauseOverlay)
        timerProgressBar = findViewById(R.id.gameTimerProgressBar)
        starManager = StarManager()
        starManager.init(findViewById(R.id.starCountText))

        // Inicjalizacja paska czasu
        timerProgressBar.setTotalTime(BASE_TIME_SECONDS) // Ustaw czas na 1,5 minuty
        timerProgressBar.setOnFinishCallback {
            runOnUiThread {
                isGameEnding = true
                Toast.makeText(this, "Czas minął! Koniec gry!", Toast.LENGTH_LONG).show()
                gridLayout.isEnabled = false
                keyButtons.forEach { it.view.isEnabled = false }
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
                if (!isShowingSequence) {
                    timerProgressBar.start()
                }
                if (isUserTurn) {
                    gridLayout.isEnabled = true
                    keyButtons.forEach { it.view.isEnabled = true }
                }
            }, // Wznawia timer po pauzie
            onPause = {
                if (!isShowingSequence) {
                    timerProgressBar.pause()
                }
                gridLayout.isEnabled = false
                keyButtons.forEach { it.view.isEnabled = false }
            }, // Zatrzymuje timer podczas pauzy
            onExit = {
                finish() },
            instructionTitle = getString(R.string.instructions),
            instructionMessage = getString(R.string.color_sequence_instruction),
        )

        // Sprawdzenie, czy gra jest uruchamiana po raz pierwszy
        if (savedInstanceState == null) {
            countdownManager.startCountdown() // Rozpoczyna odliczanie początkowe
        } else {
            restoreGameState(savedInstanceState) // Przywraca stan gry
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("currentLevel", currentLevel)
        outState.putInt("numKeys", numKeys)
        outState.putInt("currentSeqLength", currentSeqLength)
        outState.putInt("sequenceShowIndex", sequenceShowIndex)
        outState.putBoolean("isShowingSequence", isShowingSequence)
        outState.putBoolean("isUserTurn", isUserTurn)
        outState.putIntArray("currentSequence", currentSequence.toIntArray())
        outState.putIntArray("userSequence", userSequence.toIntArray())
        outState.putInt("pauseOverlayVisibility", pauseOverlay.visibility)
        outState.putInt("countdownTextVisibility", countdownText.visibility)
        outState.putInt("gridLayoutVisibility", gridLayout.visibility)
        outState.putInt("pauseButtonVisibility", pauseButton.visibility)
        outState.putLong("timerRemainingTimeMs", timerProgressBar.getRemainingTimeSeconds() * 1000L)
        outState.putBoolean("timerIsRunning", timerProgressBar.isRunning())
        outState.putInt("countdownIndex", countdownManager.getIndex())
        outState.putBoolean("countdownInProgress", countdownManager.isInProgress())
        outState.putLong("sequenceDelayRemaining", sequenceDelayRemaining)
        starManager.saveState(outState)
    }

    private fun restoreGameState(savedInstanceState: Bundle) {
        // Przywracanie zmiennych
        currentLevel = savedInstanceState.getInt("currentLevel", 1)
        numKeys = savedInstanceState.getInt("numKeys", 4)
        currentSeqLength = savedInstanceState.getInt("currentSeqLength", 1)
        sequenceShowIndex = savedInstanceState.getInt("sequenceShowIndex", 0)
        isShowingSequence = savedInstanceState.getBoolean("isShowingSequence", false)
        isUserTurn = savedInstanceState.getBoolean("isUserTurn", false)
        currentSequence = savedInstanceState.getIntArray("currentSequence")?.toMutableList() ?: mutableListOf()
        userSequence = savedInstanceState.getIntArray("userSequence")?.toMutableList() ?: mutableListOf()
        pauseOverlay.visibility = savedInstanceState.getInt("pauseOverlayVisibility")
        countdownText.visibility = savedInstanceState.getInt("countdownTextVisibility")
        gridLayout.visibility = savedInstanceState.getInt("gridLayoutVisibility")
        pauseButton.visibility = savedInstanceState.getInt("pauseButtonVisibility")
        sequenceDelayRemaining = savedInstanceState.getLong("sequenceDelayRemaining", 0L)
        starManager.restoreState(savedInstanceState)

        // Przywracanie stanu timera
        val timerRemainingTimeMs = savedInstanceState.getLong("timerRemainingTimeMs", BASE_TIME_SECONDS * 1000L)
        val timerIsRunning = savedInstanceState.getBoolean("timerIsRunning", false)
        timerProgressBar.setRemainingTimeMs(timerRemainingTimeMs.coerceAtLeast(1L))

        // Przywracanie odliczania początkowego
        val countdownIndex = savedInstanceState.getInt("countdownIndex", 0)
        val countdownInProgress = savedInstanceState.getBoolean("countdownInProgress", false)

        // Odtwarzanie planszy gry
        if (numKeys != 0) {
            keyButtons = createKeys(numKeys)
        }

        // Synchronizacja menu pauzy
        pauseMenu.syncWithOverlay()

        // Wznowienie odliczania, jeśli było aktywne
        if (countdownInProgress) {
            countdownManager.startCountdown(countdownIndex)
            return
        }

        // Wznowienie timera, jeśli gra była aktywna
        if (timerIsRunning && !isShowingSequence && pauseOverlay.visibility != View.VISIBLE) {
            timerProgressBar.start()
        }

        // Wznowienie pokazu sekwencji lub tury gracza
        if (isShowingSequence && sequenceDelayRemaining > 0) {
            gridLayout.isEnabled = false
            keyButtons.forEach { it.view.isEnabled = false }
            runDelayed(sequenceDelayRemaining, {
                highlightKey(currentSequence[sequenceShowIndex], false)
                sequenceShowIndex++
                playSequenceStep()
            })
        } else if (isShowingSequence) {
            gridLayout.isEnabled = false
            keyButtons.forEach { it.view.isEnabled = false }
            showSequence()
        } else if (isUserTurn) {
            gridLayout.isEnabled = true
            keyButtons.forEach { it.view.isEnabled = true }
        }
    }

    // Inicjalizuje nową grę na podanym poziomie
    private fun startNewGame() {
        // Upewnij się że menu pauzy jest schowane
        if (pauseMenu.isPaused) {
            pauseMenu.resume()
        } else {
            pauseOverlay.visibility = View.GONE
        }

        gridLayout.isEnabled = true // Włącz interakcje

        // Ustaw konfigurację poziomu
        val config = getSequenceConfig(currentLevel)
        numKeys = config.numKeys
        currentSeqLength = config.startLength

        // Tworzenie klawiszy i sekwencji
        keyButtons = createKeys(numKeys)
        userSequence.clear()
        currentSequence.clear()
        generateNewSequence()
        showSequence()
    }

    // Pobiera konfigurację sekwencji dla danego poziomu
    private fun getSequenceConfig(level: Int): SequenceConfig = when (level) {
        1 -> SequenceConfig(4, 1, 1, 6)   // 1→2→3→4→5→6
        2 -> SequenceConfig(4, 1, 1, 8)   // 1→2→3→4→5→6→7→8
        3 -> SequenceConfig(4, 2, 2, 10)  // 2→4→6→8→10
        4 -> SequenceConfig(6, 1, 1, 6)   // 1→2→3→4→5→6
        5 -> SequenceConfig(6, 1, 1, 8)   // 1→2→3→4→5→6→7→8
        6 -> SequenceConfig(6, 2, 2, 10)  // 2→4→6→8→10
        7 -> SequenceConfig(8, 1, 1, 6)   // 1→2→3→4→5→6
        8 -> SequenceConfig(8, 1, 1, 8)   // 1→2→3→4→5→6→7→8
        9 -> SequenceConfig(8, 2, 2, 10)  // 2→4→6→8→10
        else -> SequenceConfig(8, 2, 2, 10) // 2→4→6→8→10
    }

    // Pobiera kolory klawiszy
    private fun getKeyColors(numKeys: Int): List<Int> = when (numKeys) {
        4 -> listOf(
            Color.RED,              // Czerwony
            Color.YELLOW,           // Żółty
            Color.GREEN,            // Zielony
            Color.BLUE              // Niebieski
        )
        6 -> listOf(
            Color.RED,              // Czerwony
            Color.rgb(255, 165, 0), // Pomarańczowy
            Color.YELLOW,           // Żółty
            Color.GREEN,            // Zielony
            Color.BLUE,             // Niebieski
            Color.rgb(128, 0, 128)  // Fioletowy
        )
        else -> listOf(
            Color.MAGENTA,          // Magenta
            Color.RED,              // Czerwony
            Color.rgb(255, 165, 0), // Pomarańczowy
            Color.YELLOW,           // Żółty
            Color.GREEN,            // Zielony
            Color.CYAN,             // Cyan
            Color.BLUE,             // Niebieski
            Color.rgb(128, 0, 128)  // Fiolet
        )
    }

    // Pobiera mapowanie dźwięków dla danej liczby klawiszy
    private fun getSoundOrder(numKeys: Int): List<Int> = when (numKeys) {
        4 -> soundOrder4
        6 -> soundOrder6
        else -> soundOrder8
    }

    // Tworzy siatkę klawiszy z kolorami i listenerami
    private fun createKeys(numKeys: Int): List<KeyButton> {
        val buttonsList = mutableListOf<KeyButton>()
        val colors = getKeyColors(numKeys)
        gridLayout.removeAllViews() // Wyczyść siatkę

        val cols = when (numKeys) {
            4 -> 2
            6 -> 3
            else -> 4
        }
        gridLayout.columnCount = cols
        gridLayout.rowCount = 2

        gridLayout.clipToPadding = false
        gridLayout.clipChildren = false

        for (i in 0 until numKeys) {
            val button = Button(this).apply {
                val drawable = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = 40f
                    setColor(colors[i])
                }
                background = drawable

                text = ""
                setTextColor(Color.WHITE)
                textSize = 28f
                gravity = Gravity.CENTER

                layoutParams = GridLayout.LayoutParams().apply {
                    width = 0
                    height = 0
                    rowSpec = GridLayout.spec(i / cols, 1f)
                    columnSpec = GridLayout.spec(i % cols, 1f)
                    setMargins(16, 16, 16, 16)
                }
                isEnabled = isUserTurn && !isShowingSequence

                elevation = 8f  // Cień
            }

            button.setOnClickListener {
                if (isUserTurn && !isShowingSequence && gridLayout.isEnabled) {
                    onKeyPress(i)
                }
            }

            gridLayout.addView(button)
            buttonsList.add(KeyButton(button, i))
        }
        return buttonsList
    }

    // Generuje sekwencję klawiszy
    private fun generateNewSequence() {
        val config = getSequenceConfig(currentLevel)

        // Jeśli to pierwsza runda – generujemy od zera
        if (currentSequence.isEmpty()) {
            repeat(config.startLength) {
                currentSequence.add(Random.nextInt(0, numKeys))
            }
        }
        // Jeśli już mamy sekwencję – dodajemy nowe elementy
        else if (currentSequence.size < config.maxLength) {
            val remaining = config.maxLength - currentSequence.size
            val toAdd = minOf(config.step, remaining)

            repeat(toAdd) {
                currentSequence.add(Random.nextInt(0, numKeys))
            }
        }

        // Aktualizuj długość sekwencji
        currentSeqLength = currentSequence.size
    }

    // Rozpoczyna pokaz sekwencji (timer zatrzymany)
    private fun showSequence() {
        isUserTurn = false
        isShowingSequence = true
        gridLayout.isEnabled = false
        keyButtons.forEach { it.view.isEnabled = false }
        timerProgressBar.pause()
        sequenceShowIndex = 0
        playSequenceStep()
    }

    // Odtwarza kolejny krok sekwencji
    private fun playSequenceStep() {
        if (sequenceShowIndex >= currentSequence.size) {
            endSequenceShow()
            return
        }

        val keyIndex = currentSequence[sequenceShowIndex]
        val soundOrder = getSoundOrder(numKeys)
        val soundIndex = soundOrder[keyIndex]  // Mapowanie na właściwy dźwięk
        highlightKey(keyIndex, true)
        playKeySound(soundIndex)

        runDelayed(500L, {
            highlightKey(keyIndex, false)
            sequenceShowIndex++
            runDelayed(200L, { playSequenceStep() })
        })
    }

    // Kończy pokaz sekwencji, rozpoczyna turę gracza
    private fun endSequenceShow() {
        isShowingSequence = false
        gridLayout.isEnabled = true
        keyButtons.forEach { it.view.isEnabled = true }
        isUserTurn = true
        userSequence.clear()
        timerProgressBar.start()
    }

    // Obsługuje kliknięcie klawisza
    private fun onKeyPress(keyIndex: Int) {
        val soundOrder = getSoundOrder(numKeys)
        val soundIndex = soundOrder[keyIndex]
        playKeySound(soundIndex)
        highlightKey(keyIndex, true)
        sequenceHandler.postDelayed({ highlightKey(keyIndex, false) }, 200)

        userSequence.add(keyIndex)

        // Sprawdź błąd (po każdym kliknięciu)
        if (userSequence.size > currentSequence.size ||
            userSequence[userSequence.size - 1] != currentSequence[userSequence.size - 1]) {
            checkUserSequence()
            return
        }

        // Tylko jeśli sekwencja kompletna - kontynuuj
        if (userSequence.size == currentSequence.size) {
            checkUserSequence()
        }
    }

    // Sprawdza poprawność sekwencji wpisanej przez gracza
    private fun checkUserSequence() {
        isUserTurn = false
        gridLayout.isEnabled = false
        keyButtons.forEach { it.view.isEnabled = false }
        timerProgressBar.pause()

        if (userSequence == currentSequence) {
            // Poprawna sekwencja
            starManager.increment()

            val config = getSequenceConfig(currentLevel)

            // Sprawdź, czy osiągnięto maksymalną długość sekwencji
            if (currentSequence.size >= config.maxLength) {
                // Osiągnięto max długość – przejście na nowy poziom
                currentLevel++
                timerProgressBar.addTime(15)
                Toast.makeText(this, "Świetnie! Nowy poziom: $currentLevel +15s BONUS", Toast.LENGTH_SHORT).show()

                val newConfig = getSequenceConfig(currentLevel)
                numKeys = newConfig.numKeys
                keyButtons = createKeys(numKeys)
                currentSequence.clear()
            }

            generateNewSequence() // Dodaj krok

            runDelayed(1500L, { showSequence() })
        } else {
            // Błędna sekwencja - powtórz
            Toast.makeText(this, "Błąd! Powtórka sekwencji.", Toast.LENGTH_SHORT).show()
            userSequence.clear()
            runDelayed(1500L, { showSequence() })
        }
    }

    // Odtwarza dźwięk dla danego indeksu nuty
    private fun playKeySound(soundIndex: Int) {
        soundIds[soundIndex]?.let { soundPool.play(it, 1.0f, 1.0f, 1, 0, 1.0f) }
    }

    // Podświetla klawisz (animacja skalowania + jaśniejszy kolor)
    private fun highlightKey(keyIndex: Int, highlight: Boolean) {
        val button = keyButtons[keyIndex].view
        val bg = button.background as GradientDrawable
        val colors = getKeyColors(numKeys)

        if (highlight) {
            // Podświetlenie: jaśniejszy kolor + skalowanie
            bg.setColor(makeLighter(colors[keyIndex]))
            ObjectAnimator.ofFloat(button, "scaleX", 1f, 1.1f).setDuration(150).start()
            ObjectAnimator.ofFloat(button, "scaleY", 1f, 1.1f).setDuration(150).start()
        } else {
            // Powrót do normalnego stanu
            bg.setColor(colors[keyIndex])
            ObjectAnimator.ofFloat(button, "scaleX", 1.1f, 1f).setDuration(150).start()
            ObjectAnimator.ofFloat(button, "scaleY", 1.1f, 1f).setDuration(150).start()
        }
    }

    // Tworzy jaśniejszą wersję koloru do podświetlenia
    private fun makeLighter(color: Int): Int {
        val r = (Color.red(color) * 1.3f).coerceAtMost(255f).toInt()
        val g = (Color.green(color) * 1.3f).coerceAtMost(255f).toInt()
        val b = (Color.blue(color) * 1.3f).coerceAtMost(255f).toInt()
        return Color.rgb(r, g, b)
    }

    // Uruchamia akcję z opóźnieniem, uwzględniając pauzę
    private fun runDelayed(delay: Long, action: () -> Unit) {
        var remaining = delay
        val interval = 16L // ~60fps, aby odliczanie było płynne
        val runnable = object : Runnable {
            override fun run() {
                if (pauseMenu.isPaused) {
                    // Gra w pauzie – czekamy do wznowienia
                    sequenceHandler.postDelayed(this, interval)
                    return
                }

                remaining -= interval
                sequenceDelayRemaining = remaining.coerceAtLeast(0L)
                if (remaining <= 0) {
                    action() // Wykonanie akcji po upłynięciu czasu
                    sequenceDelayRemaining = 0L
                } else {
                    sequenceHandler.postDelayed(this, interval) // Kolejna iteracja
                }
            }
        }
        sequenceHandler.postDelayed(runnable, interval)
    }

    override fun onPause() {
        super.onPause()
        if (!isGameEnding && !pauseMenu.isPaused && !isChangingConfigurations) {
            pauseMenu.pause()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        soundPool.release()
        timerProgressBar.cancel() // Zatrzymaj CountDownTimer
        countdownManager.cancel() // Usuń handlery odliczania
        sequenceHandler.removeCallbacksAndMessages(null)
    }
}