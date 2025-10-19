package com.example.logicmind.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.example.logicmind.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.util.Calendar

class ProfileActivity : BaseActivity() {

    //private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        supportActionBar?.hide()

        // Inicjalizacja Firebase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance("https://logicmind-default-rtdb.europe-west1.firebasedatabase.app")

        // Inicjalizacja dolnego menu
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNav.selectedItemId = R.id.nav_profile

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    true
                }
                R.id.nav_statistics -> {
                    startActivity(Intent(this, StatisticsActivity::class.java))
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

        // Logika kalendarza
        val calendar = Calendar.getInstance()
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) // 1=Sunday, 2=Monday, ... 7=Saturday

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

        // Pobranie danych użytkownika
        val user = auth.currentUser
        if (user != null) {
            loadUserData(user.uid)
        } else {
            Toast.makeText(this, "Brak zalogowanego użytkownika", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, WelcomeActivity::class.java))
            finish()
        }
    }

    /**
     * Pobiera dane użytkownika (username, streak, bestStreak) z Realtime Database
     */
    private fun loadUserData(uid: String) {
        database.getReference("users").child(uid)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val username = snapshot.child("username").value as? String ?: "Brak nazwy"
                    val currentStreak = snapshot.child("streak").value as? Long ?: 0
                    val bestStreak = snapshot.child("bestStreak").value as? Long ?: 0

                    // Ustaw wartości w widokach
                    findViewById<TextView>(R.id.textUsername).text = username
                    findViewById<TextView>(R.id.textCurrentStreak).text = "$currentStreak dni"
                    findViewById<TextView>(R.id.textBestStreak).text = "$bestStreak dni"
                } else {
                    Log.e("PROFILE", "Brak danych użytkownika w bazie dla UID: $uid")
                    Toast.makeText(this, "Nie znaleziono danych użytkownika", Toast.LENGTH_SHORT).show()
                    // Ustaw domyślne wartości
                    findViewById<TextView>(R.id.textUsername).text = "Gość"
                    findViewById<TextView>(R.id.textCurrentStreak).text = "0 dni"
                    findViewById<TextView>(R.id.textBestStreak).text = "0 dni"
                }
            }
            .addOnFailureListener { e ->
                Log.e("PROFILE", "Błąd pobierania danych użytkownika: ${e.message}")
                Toast.makeText(this, "Błąd pobierania danych: ${e.message}", Toast.LENGTH_SHORT).show()
                // Ustaw domyślne wartości w razie błędu
                findViewById<TextView>(R.id.textUsername).text = "Gość"
                findViewById<TextView>(R.id.textCurrentStreak).text = "0 dni"
                findViewById<TextView>(R.id.textBestStreak).text = "0 dni"
            }
    }
}