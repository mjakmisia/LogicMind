package com.example.logicmind.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.logicmind.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.util.Locale

class MainActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // odczytanie zapisanych ustawień
        val sharedPrefs = getSharedPreferences("Settings", MODE_PRIVATE)

        // odczytanie języka
        val lang = sharedPrefs.getString("My_Lang", "pl") ?: "pl"

        // odczytanie motywu (ciemny/jasny)
        val darkModeEnabled = sharedPrefs.getBoolean("DarkMode_Enabled", false)

        // ✅ Ustaw motyw przed wywołaniem super.onCreate
        if (darkModeEnabled) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        // ✅ Ustaw język przed ustawieniem widoku
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
            val intent = Intent(this, GameSelectionActivity::class.java)
            intent.putExtra("CATEGORY_ID", "coordination")
            startActivity(intent)
        }

        btnRozwiazywanie.setOnClickListener {
            it.playSoundEffect(android.view.SoundEffectConstants.CLICK)
            val intent = Intent(this, GameSelectionActivity::class.java)
            intent.putExtra("CATEGORY_ID", "reasoning")
            startActivity(intent)
        }

        btnSkupienie.setOnClickListener {
            it.playSoundEffect(android.view.SoundEffectConstants.CLICK)
            val intent = Intent(this, GameSelectionActivity::class.java)
            intent.putExtra("CATEGORY_ID", "attention")
            startActivity(intent)
        }

        btnPamiec.setOnClickListener {
            it.playSoundEffect(android.view.SoundEffectConstants.CLICK)
            val intent = Intent(this, GameSelectionActivity::class.java)
            intent.putExtra("CATEGORY_ID", "memory")
            startActivity(intent)
        }

        // Obsługa bottom navigation
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    // Już jesteśmy na stronie głównej
                    true
                }
                R.id.nav_statistics -> {
                    // np. otwórz ekran statystyk (dodasz później)
                    // startActivity(Intent(this, StatisticsActivity::class.java))
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

    // funkcja do ustawiania języka
    private fun setLocale(lang: String) {
        val locale = Locale(lang)
        Locale.setDefault(locale)

        val config = resources.configuration
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
    }
}
