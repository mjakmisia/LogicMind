package com.example.logicmind.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.example.logicmind.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.util.Calendar

class ProfileActivity : BaseActivity() {

    private lateinit var database: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        supportActionBar?.hide()

        // Inicjalizacja Firebase
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

        Log.d("PROFILE", "Aktualny użytkownik: ${FirebaseAuth.getInstance().currentUser?.uid ?: "brak"}")

        // Odwołania do widoków
        val scrollView = findViewById<View>(R.id.scrollView)
        val textLoginPrompt = findViewById<TextView>(R.id.textLoginPrompt)
        val buttonLogin = findViewById<Button>(R.id.buttonLogin)

        // Na start ukryj wszystkie widoki
        scrollView.visibility = View.GONE
        textLoginPrompt.visibility = View.GONE
        buttonLogin.visibility = View.GONE

        // Pobranie danych użytkownika
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            // Użytkownik zalogowany - pokaż profil i pobierz dane
            scrollView.visibility = View.VISIBLE
            loadUserData(user.uid)
        } else {
            // Użytkownik niezalogowany - pokaż komunikat i przycisk logowania
            showLoginPrompt()
        }
    }

    /**
     * Pokazuje komunikat i przycisk, gdy użytkownik nie jest zalogowany
     */
    private fun showLoginPrompt() {
        val scrollView = findViewById<View>(R.id.scrollView)
        val textLoginPrompt = findViewById<TextView>(R.id.textLoginPrompt)
        val buttonLogin = findViewById<Button>(R.id.buttonLogin)

        // Ukryj główną zawartość profilu
        scrollView.visibility = View.GONE

        // Pokaż komunikat i przycisk logowania
        textLoginPrompt.visibility = View.VISIBLE
        buttonLogin.visibility = View.VISIBLE

        // Po kliknięciu — przekieruj do WelcomeActivity
        buttonLogin.setOnClickListener {
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

                    findViewById<TextView>(R.id.textUsername).text = username
                    findViewById<TextView>(R.id.textCurrentStreak).text = "$currentStreak dni"
                    findViewById<TextView>(R.id.textBestStreak).text = "$bestStreak dni"
                } else {
                    Log.e("PROFILE", "Brak danych użytkownika w bazie dla UID: $uid")
                    // Wyloguj użytkownika i pokaż widok logowania
                    FirebaseAuth.getInstance().signOut()
                    showLoginPrompt()
                }
            }
            .addOnFailureListener { e ->
                Log.e("PROFILE", "Błąd pobierania danych użytkownika: ${e.message}")
                Toast.makeText(this, "Błąd pobierania danych: ${e.message}", Toast.LENGTH_SHORT).show()
                // Ustaw domyślne wartości w razie błędu
                findViewById<TextView>(R.id.textUsername).text = "Błąd pobierania danych użytkownik"
                findViewById<TextView>(R.id.textCurrentStreak).text = "0 dni"
                findViewById<TextView>(R.id.textBestStreak).text = "0 dni"
            }
    }

    /**
     * Usuwa konto użytkownika
     * Firebase Auth wymaga, aby użytkownik był zalogowany ostatnio – jeśli sesja jest za stara, trzeba go ponownie uwierzytelnić
     */
    private fun deleteAccount(){
        val user = FirebaseAuth.getInstance().currentUser
        if(user != null) {
            val uid = user.uid

            // usunięcie danych z Realtime Database
            db.getReference("users").child(uid)
                .removeValue()
                .addOnSuccessListener {
                    Log.d("PROFILE", "Dane użytkownika usunięte z bazy: $uid")

                    // usunięcie konta z Firebase Authentication
                    user.delete()
                        .addOnSuccessListener {
                            Log.d("PROFILE", "Konto użytkownika usunięte: $uid")
                            Toast.makeText(this, "Konto zostało usunięte", Toast.LENGTH_SHORT).show()

                            // przekierowanie do WelcomeActivity
                            startActivity(Intent(this, WelcomeActivity::class.java))
                            finish()

                        }
                        .addOnFailureListener { e ->
                            Log.e("PROFILE", "Błąd usuwania konta użytkownika: ${e.message}")
                            Toast.makeText(this, "Nie udało się usunąć konta: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener { e ->
                    Log.e("PROFILE", "Błąd usuwania danych użytkownika: ${e.message}")
                    Toast.makeText(this, "Nie udało się usunąć danych: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(this, "Brak zalogowanego użytkownika", Toast.LENGTH_SHORT).show()

        }
    }
}
