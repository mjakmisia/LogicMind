package com.example.logicmind.activities

import android.content.Context
import android.content.res.Configuration
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.util.Locale

/**
 * BaseActivity
 *
 * Klasa bazowa dla wszystkich aktywności w aplikacji.
 * Jej zadaniem jest wymuszenie języka aplikacji zapisanego w SharedPreferences,
 * tak aby po restarcie aktywności (np. obrót ekranu) język się nie zmieniał na systemowy.
 *
 * Każda aktywność powinna dziedziczyć po BaseActivity zamiast AppCompatActivity.
 */
open class BaseActivity : AppCompatActivity() {

    // Inicjalizacja Firebase dla wszystkich aktywności dziedziczących
    protected lateinit var auth: FirebaseAuth
    protected lateinit var db: FirebaseDatabase

    override fun attachBaseContext(newBase: Context) {
        // Pobieramy aktualnie zapisany język z ustawień aplikacji
        val sharedPrefs = newBase.getSharedPreferences("Settings", MODE_PRIVATE)
        val lang = sharedPrefs.getString("My_Lang", "pl") ?: "pl"

        // Tworzymy obiekt Locale dla wybranego języka
        val locale = Locale(lang)
        Locale.setDefault(locale)

        // Modyfikujemy konfigurację kontekstu tak, aby używała naszego języka
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)

        // Tworzymy nowy kontekst z naszą konfiguracją i przekazujemy go do AppCompatActivity
        val context = newBase.createConfigurationContext(config)
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        // Inicjalizacja Firebase w każdej aktywności
        auth = FirebaseAuth.getInstance()
        db = FirebaseDatabase.getInstance("https://logicmind-default-rtdb.europe-west1.firebasedatabase.app")
    }

    /**
     * Aktualizuje pole lastPlayed dla danej gry w Realtime Database
     *
     * @param category Kategoria gry (np. "Koordynacja", "Skupienie")
     * @param gameName Nazwa gry (np. "Cards_on_the_Roads", "Word_Search")
     * @param uid Identyfikator użytkownika z Firebase Authentication
     */
    protected fun updateLastPlayed(category: String, gameName: String, uid: String) {
        // Aktualizacja pola lastPlayed w bazie z bieżącym timestampem
        db.getReference("users").child(uid).child("categories").child(category).child("games").child(gameName)
            .child("lastPlayed")
            .setValue(System.currentTimeMillis())
            .addOnSuccessListener {
                // Logowanie sukcesu aktualizacji
                Log.d("GAME", "Zaktualizowano lastPlayed dla $gameName")
            }
            .addOnFailureListener { e ->
                // Logowanie błędu w przypadku niepowodzenia
                Log.e("GAME", "Błąd aktualizacji lastPlayed dla $gameName: ${e.message}")
            }
    }
}
