package com.example.logicmind.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.example.logicmind.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.util.Calendar

class ProfileActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        supportActionBar?.hide()

        // Przykładowe dane (w praktyce możesz wczytać z SharedPreferences lub bazy)
        val username = "JanKowalski"
        val currentStreak = 5
        val bestStreak = 10

        // Ustaw wartości w widokach
        findViewById<TextView>(R.id.textUsername).text = username
        findViewById<TextView>(R.id.textCurrentStreak).text = "$currentStreak dni"
        findViewById<TextView>(R.id.textBestStreak).text = "$bestStreak dni"

        val calendar = Calendar.getInstance()
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)  // 1=Sunday, 2=Monday, ... 7=Saturday

        val arrows = mapOf(
            Calendar.MONDAY to findViewById<ImageView>(R.id.arrowMon),
            Calendar.TUESDAY to findViewById<ImageView>(R.id.arrowTue),
            Calendar.WEDNESDAY to findViewById<ImageView>(R.id.arrowWed),
            Calendar.THURSDAY to findViewById<ImageView>(R.id.arrowThu),
            Calendar.FRIDAY to findViewById<ImageView>(R.id.arrowFri),
            Calendar.SATURDAY to findViewById<ImageView>(R.id.arrowSat),
            Calendar.SUNDAY to findViewById<ImageView>(R.id.arrowSun)
        )

        arrows[dayOfWeek]?.visibility = View.VISIBLE

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNav.selectedItemId = R.id.nav_profile

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    true
                }
                R.id.nav_statistics -> {
                    // startActivity(Intent(this, StatisticsActivity::class.java)) // jeśli masz taką
                    true
                }
                R.id.nav_profile -> true
                R.id.nav_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }
}
