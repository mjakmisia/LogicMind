package com.example.logicmind.activities

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import androidx.gridlayout.widget.GridLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.logicmind.R
import com.example.logicmind.common.GameCountdownManager
import com.example.logicmind.common.GameTimerProgressBar

class NumberAdditionActivity : AppCompatActivity() {

    /*TODO: dodanie "odklikania" liczby w grze
        - dodanie gwiazdek-punktow
        - dodanie wiecej poziomów
        - rozbudowanie wyglądu

    */

    // Deklaracja elementów interfejsu użytkownika i zmiennych gry
    private lateinit var targetNumberText: TextView // Tekst wyświetlający docelową sumę liczb
    private lateinit var numberGrid: GridLayout // Siatka przycisków z liczbami
    private lateinit var timerProgressBar: GameTimerProgressBar // Pasek postępu czasu gry
    private lateinit var pauseButton: Button // Przycisk do wstrzymania gry
    private lateinit var pauseOverlay: View // Nakładka wyświetlana podczas pauzy
    private lateinit var countdownText: TextView // Tekst odliczania początkowego (3, 2, 1, Start!)
    private lateinit var countdownManager: GameCountdownManager // Manager odliczania początkowego
    private val numbers = mutableListOf<Int>() // Lista przechowująca liczby w siatce
    private val selectedButtons = mutableListOf<Button>() // Lista wybranych przycisków
    private var level = 1 // Aktualny poziom gry (1: siatka 3x4, 2: siatka 4x4)
    private var remainingTime: Long = 60_000 // Pozostały czas gry w milisekundach (60 sekund)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_number_addition)

        // Ukrycie paska akcji dla pełnoekranowego widoku gry
        supportActionBar?.hide()

        // Inicjalizacja elementów interfejsu z layoutu
        targetNumberText = findViewById(R.id.targetNumberText)
        numberGrid = findViewById(R.id.numberGrid)
        timerProgressBar = findViewById(R.id.gameTimerProgressBar)
        pauseButton = findViewById(R.id.pauseButton)
        pauseOverlay = findViewById(R.id.pauseOverlay)
        countdownText = findViewById(R.id.countdownText)

        // Konfiguracja paska czasu gry
        timerProgressBar.setTotalTime(60) // Ustawienie całkowitego czasu gry na 60 sekund
        timerProgressBar.setOnFinishCallback {
            // Wywoływane, gdy czas gry się kończy
            runOnUiThread {
                Toast.makeText(this, "Czas minął! Koniec gry!", Toast.LENGTH_LONG).show()
                // Wyłączenie interakcji z siatką
                numberGrid.isEnabled = false
                // Wyłączenie wszystkich przycisków w siatce
                for (i in 0 until numberGrid.childCount) {
                    numberGrid.getChildAt(i).isEnabled = false
                }
                // Restart poziomu po 2 sekundach
                Handler(Looper.getMainLooper()).postDelayed({ startLevel() }, 2000)
            }
        }

        // Inicjalizacja managera odliczania początkowego
        countdownManager = GameCountdownManager(
            countdownText = countdownText,
            gameView = numberGrid, // Siatka liczb jako główny widok gry
            pauseButton = pauseButton,
            onCountdownFinished = {
                // Po zakończeniu odliczania uruchom timer gry i poziom
                timerProgressBar.start()
                startLevel()
            }
        )

        // Konfiguracja przycisku pauzy
        pauseButton.setOnClickListener {
            // Pokazanie nakładki pauzy, zatrzymanie timera i wyłączenie siatki
            pauseOverlay.visibility = View.VISIBLE
            timerProgressBar.pause()
            numberGrid.isEnabled = false
        }

        // Konfiguracja menu pauzy (przycisk wznawiania)
        setupPauseMenu()

        // Sprawdzenie, czy przywracamy stan gry (np. po obrocie ekranu)
        if (savedInstanceState != null) {
            restoreGameState(savedInstanceState)
        } else {
            // Rozpoczęcie odliczania początkowego dla nowej gry
            countdownManager.startCountdown()
        }
    }

    // Konfiguracja menu pauzy z przyciskiem wznawiania
    private fun setupPauseMenu() {
        val resumeButton: Button = findViewById(R.id.resumeButton)
        resumeButton.setOnClickListener {
            // Ukrycie nakładki pauzy, wznowienie timera i włączenie siatki
            pauseOverlay.visibility = View.GONE
            timerProgressBar.start()
            numberGrid.isEnabled = true
        }
    }

    // Rozpoczęcie nowego poziomu gry
    private fun startLevel() {
        // Włączenie interakcji z siatką
        numberGrid.isEnabled = true
        // Ustawienie liczby kolumn i wierszy w zależności od poziomu
        numberGrid.columnCount = if (level == 1) 4 else 4
        numberGrid.rowCount = if (level == 1) 3 else 4

        // Generowanie liczb i docelowej sumy
        generateNumbers()
        if (!generateTarget()) {
            // Jeśli nie można wygenerować sumy, przejdź do następnego poziomu
            proceedToNextLevel()
            return
        }
        // Konfiguracja siatki przycisków
        setupNumberGrid()
    }

    // Generowanie losowych liczb dla siatki
    private fun generateNumbers() {
        numbers.clear()
        // Określenie rozmiaru siatki (12 dla poziomu 1, 16 dla poziomu 2)
        val gridSize = if (level == 1) 12 else 16
        // Wypełnienie listy losowymi liczbami od 1 do 9
        repeat(gridSize) {
            numbers.add((1..9).random())
        }
    }

    // Generowanie docelowej sumy na podstawie dostępnych liczb
    private fun generateTarget(): Boolean {
        // Filtrowanie dostępnych liczb (różnych od -1)
        val availableNumbers = numbers.filter { it != -1 }
        if (availableNumbers.size < 2) return false // Zbyt mało liczb do utworzenia sumy

        // Obliczenie wszystkich możliwych sum par liczb
        val possibleSums = mutableListOf<Int>()
        for (i in availableNumbers.indices) {
            for (j in i + 1 until availableNumbers.size) {
                possibleSums.add(availableNumbers[i] + availableNumbers[j])
            }
        }
        if (possibleSums.isEmpty()) return false // Brak możliwych sum

        // Losowanie jednej z możliwych sum i ustawienie jej w TextView
        val target = possibleSums.random()
        targetNumberText.text = target.toString()
        return true
    }

    // Konfiguracja siatki przycisków z liczbami
    private fun setupNumberGrid() {
        // Usunięcie wszystkich istniejących widoków z siatki
        numberGrid.removeAllViews()
        for (i in numbers.indices) {
            val button = Button(this)
            // Ustawienie tekstu przycisku (pusty dla -1, liczba w przeciwnym razie)
            button.text = if (numbers[i] == -1) "" else numbers[i].toString()
            // Ustawienie parametrów układu przycisku
            button.layoutParams = GridLayout.LayoutParams().apply {
                width = 150
                height = 150
                setMargins(8, 8, 8, 8)
            }

            if (numbers[i] == -1) {
                // Dezaktywacja przycisku dla pustych miejsc
                button.isEnabled = false
                button.setBackgroundColor(Color.LTGRAY)
            } else {
                // Dodanie obsługi kliknięcia dla aktywnych przycisków
                button.setOnClickListener { handleNumberClick(button, i) }
                button.setBackgroundColor(Color.WHITE) // Domyślny kolor tła
            }

            // Dodanie przycisku do siatki
            numberGrid.addView(button)
        }
    }

    // Obsługa kliknięcia przycisku z liczbą
    private fun handleNumberClick(button: Button, index: Int) {
        // Sprawdzenie, czy można wybrać przycisk (mniej niż 2 wybrane, przycisk aktywny, liczba nie jest -1)
        if (selectedButtons.size < 2 && button.isEnabled && numbers[index] != -1) {
            selectedButtons.add(button)
            button.isEnabled = false
            // Podświetlenie na fioletowo-niebiesko dla wybranych przycisków
            button.setBackgroundColor(Color.rgb(106, 27, 154))
        }

        // Sprawdzenie, czy wybrano dokładnie 2 przyciski
        if (selectedButtons.size == 2) {
            // Pobranie indeksów wybranych przycisków w siatce
            val firstIndex = numberGrid.indexOfChild(selectedButtons[0])
            val secondIndex = numberGrid.indexOfChild(selectedButtons[1])
            // Obliczenie sumy wybranych liczb
            val sum = numbers[firstIndex] + numbers[secondIndex]
            val target = targetNumberText.text.toString().toIntOrNull() ?: 0

            if (sum == target) {
                // Poprawna suma: usunięcie wybranych liczb z siatki
                selectedButtons.forEach { btn ->
                    val idx = numberGrid.indexOfChild(btn)
                    numbers[idx] = -1 // Oznaczenie miejsca jako pustego
                    btn.text = ""
                    btn.isEnabled = false
                    btn.setBackgroundColor(Color.LTGRAY) // Szary kolor dla pustych miejsc
                }
                selectedButtons.clear()
                // Próba wygenerowania nowej sumy docelowej
                if (!generateTarget()) {
                    proceedToNextLevel()
                    return
                }
                // Odświeżenie siatki
                setupNumberGrid()
            } else {
                // Niepoprawna suma: podświetlenie na czerwono
                selectedButtons.forEach { btn ->
                    btn.setBackgroundColor(Color.RED)
                }
                // Opóźnienie 1 sekunda przed zresetowaniem przycisków
                Handler(Looper.getMainLooper()).postDelayed({
                    selectedButtons.forEach { btn ->
                        if (!btn.isEnabled) {
                            btn.isEnabled = true
                            btn.setBackgroundColor(Color.WHITE) // Przywrócenie domyślnego koloru
                        }
                    }
                    selectedButtons.clear()
                }, 1000)
            }
        }
    }

    // Przejście do następnego poziomu lub zakończenie gry
    private fun proceedToNextLevel() {
        if (level == 1) {
            // Przejście z poziomu 1 (3x4) do poziomu 2 (4x4)
            level = 2
            startLevel()
        } else {
            // Zakończenie gry po poziomie 2
            endGame()
        }
    }

    // Zakończenie gry i wyświetlenie komunikatu
    private fun endGame() {
        // Ustawienie komunikatu o końcu gry
        targetNumberText.text = "Koniec!"
        numberGrid.removeAllViews()
        // Stworzenie TextView z gratulacjami
        val message = TextView(this)
        message.text = "Gratulacje! Ukończyłeś wszystkie poziomy!"
        message.textSize = 24f
        message.layoutParams = GridLayout.LayoutParams().apply {
            width = GridLayout.LayoutParams.WRAP_CONTENT
            height = GridLayout.LayoutParams.WRAP_CONTENT
            setMargins(8, 8, 8, 8)
        }
        // Dodanie komunikatu do siatki
        numberGrid.addView(message)
        // Zatrzymanie timera na końcu gry
        timerProgressBar.pause()
    }

    // Zapis stanu gry przed zmianą konfiguracji (np. obrót ekranu)
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("level", level)
        outState.putIntegerArrayList("numbers", ArrayList(numbers))
        outState.putInt("remainingTime", timerProgressBar.getRemainingTimeSeconds())
        outState.putBoolean("countdownInProgress", countdownManager.isInProgress())
        outState.putInt("countdownIndex", countdownManager.getIndex())
    }

    // Przywrócenie stanu gry po zmianie konfiguracji
    private fun restoreGameState(savedInstanceState: Bundle) {
        // Przywrócenie poziomu, liczb i pozostałego czasu
        level = savedInstanceState.getInt("level", 1)
        numbers.clear()
        numbers.addAll(savedInstanceState.getIntegerArrayList("numbers") ?: mutableListOf())
        remainingTime = savedInstanceState.getLong("remainingTime", 60_000)
        // Ustawienie timera na pozostały czas
        timerProgressBar.setTotalTime((remainingTime / 1000).toInt())

        // Przywrócenie stanu odliczania
        val countdownInProgress = savedInstanceState.getBoolean("countdownInProgress", false)
        val countdownIndex = savedInstanceState.getInt("countdownIndex", 0)
        if (countdownInProgress) {
            // Wznowienie odliczania od zapisanego indeksu
            countdownManager.startCountdown(countdownIndex)
        } else {
            // Jeśli odliczanie zakończone, uruchom grę i timer
            timerProgressBar.start()
            setupNumberGrid()
            generateTarget()
        }
    }
}