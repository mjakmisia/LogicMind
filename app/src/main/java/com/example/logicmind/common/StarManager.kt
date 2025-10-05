package com.example.logicmind.common

import android.os.Bundle
import android.widget.TextView

class StarManager {
    var starCount: Int = 0 // Licznik zdobytych gwiazdek

    private lateinit var starCountText: TextView // Pole tekstowe wyświetlające liczbę gwiazdek

    // Inicjalizuje manager z TextView
    fun init(textView: TextView) {
        starCountText = textView
        updateUI()
    }

    // Zwiększa licznik o 1 i aktualizuje UI
    fun increment() {
        starCount += 1
        updateUI()
    }

    // Resetuje licznik do 0 i aktualizuje UI (dla nowej gry)
    fun reset() {
        starCount = 0
        updateUI()
    }

    // Aktualizuje tekst wyświetlający liczbę gwiazdek
    private fun updateUI() {
        starCountText.text = starCount.toString()
    }

    // Zapisuje stan do Bundle (domyślny key, ale możesz podać własny)
    fun saveState(outState: Bundle, key: String = "starCount") {
        outState.putInt(key, starCount)
    }

    // Przywraca stan z Bundle i aktualizuje UI (domyślny key)
    fun restoreState(savedInstanceState: Bundle, key: String = "starCount") {
        starCount = savedInstanceState.getInt(key, 0)
        updateUI()
    }
}