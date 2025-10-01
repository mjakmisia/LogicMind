package com.example.logicmind.activities

import android.content.Intent
import android.os.Bundle
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.logicmind.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth

class StatisticsActivity : BaseActivity() {

    private lateinit var tvMessage: TextView
    private lateinit var tvGamesCount: TextView
    private lateinit var progressGames: ProgressBar
    private lateinit var auth: FirebaseAuth

    private val maxGames = 8
    private var gamesPlayed = 0 // w przyszłości pobierane z bazy

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_statistics)

        tvMessage = findViewById(R.id.tvMessage)
        tvGamesCount = findViewById(R.id.tvGamesCount)
        progressGames = findViewById(R.id.progressGames)
        auth = FirebaseAuth.getInstance()

        val user = auth.currentUser

        if (user == null) {
            tvMessage.text = "Zaloguj się, żeby zobaczyć statystyki"
            gamesPlayed = 0
        } else {
            // mockowane dane
            gamesPlayed = 5 // przykładowa liczba rozegranych gier
            tvMessage.text = "Najczęściej grasz w: Sudoku"
        }

        updateProgress()

        // bottom navigation
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNav.selectedItemId = R.id.nav_statistics

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> { startActivity(Intent(this, MainActivity::class.java)); true }
                R.id.nav_statistics -> true
                R.id.nav_profile -> { startActivity(Intent(this, ProfileActivity::class.java)); true }
                R.id.nav_settings -> { startActivity(Intent(this, SettingsActivity::class.java)); true }
                else -> false
            }
        }
    }

    private fun updateProgress() {
        progressGames.max = maxGames
        progressGames.progress = gamesPlayed
        tvGamesCount.text = "$gamesPlayed/$maxGames"
    }
}

