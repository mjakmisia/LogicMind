package com.example.prototyp_inynierka.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.prototyp_inynierka.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.util.Locale

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        //odczytanie języka
        val sharedPrefs = getSharedPreferences("Settings", MODE_PRIVATE)
        val lang = sharedPrefs.getString("My_Lang", "pl") ?: "pl"

        // Ustaw język przed ustawieniem widoku
        setLocale(lang)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        supportActionBar?.hide()

        // Kategorie gier
        val btnKoordynacja = findViewById<Button>(R.id.btnKoordynacja)
        val btnRozwiazywanie = findViewById<Button>(R.id.btnRozwiazywanieProblemow)
        val btnSkupienie = findViewById<Button>(R.id.btnSkupienie)
        val btnPamiec = findViewById<Button>(R.id.btnPamiec)

        btnKoordynacja.setOnClickListener {
            it.playSoundEffect(android.view.SoundEffectConstants.CLICK)
        }

        btnRozwiazywanie.setOnClickListener {
            it.playSoundEffect(android.view.SoundEffectConstants.CLICK)
            val intent = Intent(this, GameSelectionProblemSolvingActivity::class.java)
            startActivity(intent)
        }

        btnSkupienie.setOnClickListener {
            it.playSoundEffect(android.view.SoundEffectConstants.CLICK)
            val intent = Intent(this, GameSelectionAttentionActivity::class.java)
            startActivity(intent)
        }

        btnPamiec.setOnClickListener {
            it.playSoundEffect(android.view.SoundEffectConstants.CLICK)
            val intent = Intent(this, GameSelectionMemoryActivity::class.java)
            startActivity(intent)
        }

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    // Już jesteśmy na stronie głównej
                    true
                }
                R.id.nav_statistics -> {
                    // Tu możesz otworzyć ekran statystyk
                    // startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    true
                }
                R.id.nav_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }

    //funkcja do ustawiania języka
    private fun setLocale(lang: String) {
        val locale = Locale(lang)
        Locale.setDefault(locale)

        val config = resources.configuration
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
    }
}
