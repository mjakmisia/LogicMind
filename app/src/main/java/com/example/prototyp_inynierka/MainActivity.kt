package com.example.prototyp_inynierka

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        supportActionBar?.hide()

        // Kategorie gier
        val btnKoordynacja = findViewById<Button>(R.id.btnKoordynacja)
        val btnRozwiazywanie = findViewById<Button>(R.id.btnRozwiazywanie)
        val btnSkupienie = findViewById<Button>(R.id.btnSkupienie)
        val btnPamiec = findViewById<Button>(R.id.btnPamiec)

        //btnKoordynacja.setBackgroundColor(ContextCompat.getColor(this, R.color.niebieski))
        //btnRozwiazywanie.setBackgroundColor(ContextCompat.getColor(this, R.color.rozowy))
        //btnSkupienie.setBackgroundColor(ContextCompat.getColor(this, R.color.zolty))
        //btnPamiec.setBackgroundColor(ContextCompat.getColor(this, R.color.zielony))

        btnKoordynacja.setOnClickListener {
            // Tu dodaj później np. start gry z kategorii Koordynacja
            it.playSoundEffect(android.view.SoundEffectConstants.CLICK)
        }

        btnRozwiazywanie.setOnClickListener {
            it.playSoundEffect(android.view.SoundEffectConstants.CLICK)
        }

        btnSkupienie.setOnClickListener {
            it.playSoundEffect(android.view.SoundEffectConstants.CLICK)
        }

        btnPamiec.setOnClickListener {
            it.playSoundEffect(android.view.SoundEffectConstants.CLICK)
        }

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    // Już jesteśmy na stronie głównej
                    true
                }
                R.id.nav_profile -> {
                    // Tu możesz otworzyć ekran profilu
                    // startActivity(Intent(this, ProfileActivity::class.java))
                    true
                }
                R.id.nav_settings -> {
                    // startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }
}
