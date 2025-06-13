package com.example.prototyp_inynierka.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.example.prototyp_inynierka.R

class IntroMatchingPairsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_intro_matching_pairs)

        // Ukrycie paska akcji
        supportActionBar?.hide()

        // Inicjalizacja przycisków
        val btnClose = findViewById<ImageButton>(R.id.btnClose)
        val btnHelp = findViewById<ImageButton>(R.id.btnHelp)
        val btnStartGame = findViewById<Button>(R.id.btnStartGame)

        // Powrót do MainActivity po kliknięciu btnClose
        btnClose.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        // Obsługa przycisków
        btnHelp.setOnClickListener {
            // TODO: Dodać ekran pomocy
        }
        btnStartGame.setOnClickListener {
            val intent = Intent(this, MatchingPairsActivity::class.java)
            startActivity(intent)
        }
    }
}