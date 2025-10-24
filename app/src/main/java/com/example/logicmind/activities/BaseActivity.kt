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
     * Aktualizuje pole lastPlayed dla danej gry
     *
     * @param category Kategoria gry (np. "Koordynacja", "Skupienie")
     * @param gameName Nazwa gry (np. "Cards_on_the_Roads", "Word_Search")
     * @param uid Identyfikator użytkownika z Firebase Authentication

    protected fun updateLastPlayed(category: String, gameName: String, uid: String, onSuccess: (() -> Unit)? = null) {
        // Aktualizacja pola lastPlayed w bazie z bieżącym timestampem
        db.getReference("users").child(uid).child("categories").child(category).child("games").child(gameName)
            .child("lastPlayed")
            .setValue(System.currentTimeMillis())
            .addOnSuccessListener {
                // Logowanie sukcesu aktualizacji
                Log.d("GAME", "Zaktualizowano lastPlayed dla $gameName")
                onSuccess?.invoke() // wywołanie aktywonsci dopiero po zapisie
            }
            .addOnFailureListener { e ->
                // Logowanie błędu w przypadku niepowodzenia
                Log.e("GAME", "Błąd aktualizacji lastPlayed dla $gameName: ${e.message}")
            }
    }
     */

    /**
     * Ustawia daną grę jako lastPlayed w bazie
     *
     * Unit - odpowiednik void w Kotlinie, ? - opcjonalna funkcja
     */
    protected fun onGameFinished(categoryKey: String, gameKey: String, displayName: String, onSuccess: (() -> Unit)? = null){
        val user = auth.currentUser

        if (user != null){
            val uid = user.uid
            //zapis do bazy danych - operacja asynchroniczna, czyli nie blokuje wątku głównego
            val dbRef = db.getReference("users").child(uid).child("categories").child(categoryKey)
                .child("games").child(gameKey)

            val timestamp = System.currentTimeMillis()

            dbRef.child("lastPlayed").setValue(timestamp)
                .addOnSuccessListener {
                    Log.d("GAME_DEBUG", "Zaktualizowano lastPlayed dla $gameKey")
                    onSuccess?.invoke() //callback - domyślnie null
                    //używa się aby wykonać akcję dopiero po zapisie do bazy
                }
                .addOnFailureListener{ e ->
                    Log.e("GAME_DEBUG", "Błąd aktualizacji lastPlayed dla $gameKey", e)
                }
        } else {
            Log.w("GAME_DEBUG", "Brak zalogowanego użytkownika, lastPlayed nie zaktualizowany")
        }
    }

    /*
    Stałe używane do dostępu do kategorii i gier w bazie danych Firebase.
    Nazwy muszą zgadzać się z tym co jest w bazie
     */
    object GameKeys {
        const val CATEGORY_MEMORY = "Pamiec"
        const val CATEGORY_FOCUS = "Skupienie"
        const val CATEGORY_COORDINATION = "Koordynacja"
        const val CATEGORY_REASONING = "Rozwiazywanie_problemow"

        const val GAME_CARD_MATCH = "card_match"
        const val GAME_COLOR_SEQUENCE = "color_sequence"
        const val GAME_WORD_SEARCH = "word_search"
        const val GAME_SYMBOL_RACE = "symbol_race"
        const val GAME_FRUIT_SORT = "fruit_sort"
        const val GAME_NUMBER_ADDITION = "number_addition"
        const val GAME_PATH_CHANGE = "path_change"
        const val GAME_ROAD_DASH = "road_dash"

    }

}
