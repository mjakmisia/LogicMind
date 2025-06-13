package com.example.prototyp_inynierka.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.example.prototyp_inynierka.R

class IntroWordSearchActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_intro_word_search)

        supportActionBar?.hide()

        val btnClose = findViewById<ImageButton>(R.id.btnClose)
        val btnHelp = findViewById<ImageButton>(R.id.btnHelp)
        val btnStartGame = findViewById<Button>(R.id.btnStartGame)

        btnClose.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        btnHelp.setOnClickListener {
            // Brak aktywności
        }

        btnStartGame.setOnClickListener {
            // Brak aktywności
        }
    }
}
