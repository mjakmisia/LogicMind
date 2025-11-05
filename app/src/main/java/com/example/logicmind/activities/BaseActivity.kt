package com.example.logicmind.activities

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.util.Locale
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.logicmind.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction

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
        // aktualnie zapisany język z ustawień
        //MODE_PRIVATE - tylko dla tej aplikacji (nie dla innychna urządzeniu)
        val sharedPrefs = newBase.getSharedPreferences("Settings", MODE_PRIVATE)
        val lang = sharedPrefs.getString("My_Lang", "pl") ?: "pl"

        //obiekt Locale dla wybranego języka
        val locale = Locale(lang)
        Locale.setDefault(locale)

        //modyfikacja konfiguracji kontekstu tak, aby używała wybranego języka
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)

        //nowy kontekst z konfiguracją i przekazuje go do AppCompatActivity
        val context = newBase.createConfigurationContext(config)
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)

        // Ustawiamy pełny ekran i pozwalamy layoutowi wchodzić w wycięcia (notch)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.attributes.layoutInDisplayCutoutMode =
            android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES

        // Ukrywamy paski systemowe
        val insetsController = WindowInsetsControllerCompat(window, window.decorView)
        insetsController.hide(
            androidx.core.view.WindowInsetsCompat.Type.statusBars() or
                    androidx.core.view.WindowInsetsCompat.Type.navigationBars()
        )
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE


        // Inicjalizacja Firebase w każdej aktywności
        auth = FirebaseAuth.getInstance()
        db =
            FirebaseDatabase.getInstance("https://logicmind-default-rtdb.europe-west1.firebasedatabase.app")
    }

    /**
     * Metoda do ustawiania menu na dole
     */
    protected fun setupBottomNavigation(bottomNav: BottomNavigationView, selectedItemId: Int) {
        if (bottomNav == null) {
            Log.e("NAV_DEBUG", "BottomNavigationView is null")
            return
        }
        bottomNav.selectedItemId = selectedItemId
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    if (this !is MainActivity) {
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    }
                    true
                }

                R.id.nav_statistics -> {
                    if (this !is StatisticsActivity) {
                        startActivity(Intent(this, StatisticsActivity::class.java))
                        finish()
                    }
                    true
                }

                R.id.nav_profile -> {
                    if (this !is ProfileActivity) {
                        startActivity(Intent(this, ProfileActivity::class.java))
                        finish()
                    }
                    true
                }

                R.id.nav_settings -> {
                    if (this !is SettingsActivity) {
                        startActivity(Intent(this, SettingsActivity::class.java))
                        finish()
                    }
                    true
                }

                else -> false
            }
        }
    }


    /**
     * Ustawia daną grę jako lastPlayed w bazie
     *
     * Unit - odpowiednik void w Kotlinie, ? - opcjonalna funkcja
     */
    protected fun lastPlayedGame(
        categoryKey: String,
        gameKey: String,
        displayName: String,
        onSuccess: (() -> Unit)? = null
    ) {
        val user = auth.currentUser

        if (user != null) {
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
                    updateStreak() //wywołanie tutaj aby nie powtarzać kodu
                }
                .addOnFailureListener { e ->
                    Log.e("GAME_DEBUG", "Błąd aktualizacji lastPlayed dla $gameKey", e)
                }
        } else {
            Log.w("GAME_DEBUG", "Brak zalogowanego użytkownika, lastPlayed nie zaktualizowany")
        }
    }

    /**
     * Aktualizuje streak oraz bestStreak użytkownika w bazie
     */
    protected fun updateStreak() {
        val user = auth.currentUser

        if (user != null) {
            val uid = user.uid
            val userRef = db.getReference("users").child(uid)

            userRef.get().addOnSuccessListener { snapshot ->
                //pobieramy aktualny streak i bestStreak
                val streak = (snapshot.child("streak").value as? Long ?: 0L).toInt()
                val bestStreak = (snapshot.child("bestStreak").value as? Long ?: 0L).toInt()
                val lastPlayTimestamp = snapshot.child("lastPlayDate").getValue(Long::class.java)

                val today = java.util.Calendar.getInstance()

                val isSameDay = lastPlayTimestamp?.let {
                    val lastPlayDay = java.util.Calendar.getInstance().apply { timeInMillis = it }
                    lastPlayDay.get(java.util.Calendar.YEAR) == today.get(java.util.Calendar.YEAR) &&
                            lastPlayDay.get(java.util.Calendar.DAY_OF_YEAR) == today.get(java.util.Calendar.DAY_OF_YEAR)

                } ?: false

                val newStreak = if (isSameDay) {
                    streak // gra w tym samym dniu - pozostaje bez zmian
                } else {
                    if (lastPlayTimestamp != null) {
                        val lastPlayDay = java.util.Calendar.getInstance()
                            .apply { timeInMillis = lastPlayTimestamp }
                        val diffDays =
                            today.get(java.util.Calendar.DAY_OF_YEAR) - lastPlayDay.get(java.util.Calendar.DAY_OF_YEAR)
                        val diffYears =
                            today.get(java.util.Calendar.YEAR) - lastPlayDay.get(java.util.Calendar.YEAR)
                        if (diffDays == 1 && diffYears == 0) {
                            // user zagrał następnego dnia – zwiększ streak
                            streak + 1
                        } else {
                            // Przerwa w grze – resetuj streak
                            1
                        }
                    } else {
                        // Pierwsza gra usera ever
                        1
                    }
                }

                //zapisanie do bazy
                userRef.child("streak").setValue(newStreak)
                    .addOnSuccessListener {
                        Log.d("STREAK_DEBUG", "Zaktualizowano streak dla $uid")
                    }
                    .addOnFailureListener {
                        Log.e("STREAK_DEBUG", "Błąd aktualizacji streak dla $uid", it)
                    }

                if (newStreak > bestStreak) {
                    userRef.child("bestStreak").setValue(newStreak)
                        .addOnSuccessListener {
                            Log.d("STREAK_DEBUG", "Zaktualizowano bestStreak dla $uid")
                        }
                        .addOnFailureListener {
                            Log.e("STREAK_DEBUG", "Błąd aktualizacji bestStreak dla $uid", it)
                        }
                }
                //aktualizacja lastPlayDate
                userRef.child("lastPlayDate").setValue(today.timeInMillis)
                Log.d("STREAK_DEBUG", "Nowy streak:  $newStreak, BestStreak: $bestStreak")

            }.addOnFailureListener { e ->
                Log.e("STREAK_DEBUG", "Błąd pobierania danych użytkownika", e)
            }
        }
    }

    protected fun isGuestUser(onResult: (Boolean) -> Unit) {
        val user = auth.currentUser

        if (user == null || !user.isAnonymous) {
            onResult(false)
            return
        }

        //sprawdza czy w bazie istnieje wpisa dla tego uid
        db.getReference("users").child(user.uid).get()
            .addOnSuccessListener { snapshot ->
                //jeżeli snapshot nie istnieje = gość
                onResult(!snapshot.exists())
            }
            .addOnFailureListener {
                onResult(true)
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

    /**
     * Aktualizuje statystyki gracza w Firestore po zakończeniu gry
     * @param gameId - id gry
     * @param starsEarned - liczba zdobytych gwiazdek
     * @param score - punktacja w grze
     * @param accuracy - celność
     * @param reactionTime - średni czas reakcji
     *
     * TODO: score zamienic na star bo to jest to samo
     */

    protected fun updateUserStatistics(
        categoryKey: String,
        gameKey: String,
        starsEarned: Int = 0,
        accuracy: Double = 0.0,
        reactionTime: Double = 0.0
    ) {
        val userId = auth.currentUser?.uid ?: return
        val userRef = db.getReference("users").child(userId)
        //aktualizacja globalnych statystyk usera
        val statsRef = userRef.child("statistics")

        //AKTUALIZACJA GLOBALNYCH STATYSTYK

        //runTransaction wykonuje aktualizacje "atomowo" czyli jedną grę na raz
        //używany do odczytu danych które są zależne od poprzednich danych
        //wykonuje się w pętli retry - jeśli ktoś inny zmienił dane między odczytem i zapisem to odczyt jest wykonywany ponownie
        statsRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                //mutableData - tymczasowy lokalny obiekt reprezentuje dane węzła na którym robimy transakcje

                val currentStats = currentData.value as? Map<String, Any> ?: emptyMap()

                val currentStars = (currentStats["totalStars"] as? Long ?: 0L).toInt()
                val currentAvgAccuracy = (currentStats["avgAccuracy"] as? Double ?: 0.0)
                val currentAvgReactionTime = (currentStats["avgReactionTime"] as? Double ?: 0.0)
                val gamesPlayed = (currentStats["gamesPlayed"] as? Long ?: 0L).toInt()

                //obliczanie nowych średnich ważonych
                val newGamesPlayed = gamesPlayed + 1
                val newAvgAccuracy =
                    if (accuracy > 0) (currentAvgAccuracy * gamesPlayed + accuracy) / newGamesPlayed
                    else currentAvgAccuracy
                val newAvgReaction =
                    if (reactionTime > 0) (currentAvgReactionTime * gamesPlayed + reactionTime) / newGamesPlayed
                    else currentAvgReactionTime

                val updatedStats = mapOf(
                    "avgAccuracy" to newAvgAccuracy,
                    "avgReactionTime" to newAvgReaction,
                    "totalStars" to currentStars + starsEarned,
                    "gamesPlayed" to newGamesPlayed
                )

                currentData.value = updatedStats
                return Transaction.success(currentData)
            }

            override fun onComplete(
                error: DatabaseError?,
                committed: Boolean,
                currentData: DataSnapshot?
            ) {
                if (error != null) {
                    Log.e("STATS_DEBUG", "Błąd aktualizacji statystyk: ${error.message}")
                } else {
                    Log.d("STATS_DEBUG", "Zaktualizowane globalne statystyki użytkownika")
                }
            }
        })

        //aktualizacja statystyk konkretnej gry
        val gameRef = userRef.child("categories").child(categoryKey).child("games").child(gameKey)

        gameRef.get().addOnSuccessListener { snapshot ->
            val currentStars = snapshot.child("starsEarned").getValue(Int::class.java) ?: 0
            val currentGamesPlayed = snapshot.child("gamesPlayed").getValue(Int::class.java) ?: 0
            val currentBestStars = snapshot.child("bestStars").getValue(Int::class.java) ?: 0
            val currentAvgAccuracy = snapshot.child("accuracy").getValue(Double::class.java) ?: 0.0
            val currentAvgReaction = snapshot.child("avgReactionTime").getValue(Double::class.java) ?: 0.0

            val newGamesPlayed = currentGamesPlayed + 1

            //obliczanie rzeczywistych średnich dla gry
            val newAvgAccuracy =
                if (accuracy > 0) ((currentAvgAccuracy * currentGamesPlayed + accuracy) / newGamesPlayed)
                else currentAvgAccuracy
            val newAvgReaction =
                if (reactionTime > 0) ((currentAvgReaction * currentGamesPlayed + reactionTime) / newGamesPlayed)
                else currentAvgReaction

            val updatedGameData = mapOf(
                "starsEarned" to (currentStars + starsEarned),
                "bestStars" to maxOf(currentBestStars, starsEarned),
                "accuracy" to newAvgAccuracy,
                "avgReactionTime" to newAvgReaction,
                "gamesPlayed" to newGamesPlayed,
                "lastPlayed" to System.currentTimeMillis()
            )

            gameRef.updateChildren(updatedGameData).addOnSuccessListener {
                Log.d("STATS_DEBUG", "Zaktualizowane statystyki dla gry $gameKey: $updatedGameData")
            }.addOnFailureListener {
                Log.e("STATS_DEBUG", "Błąd zapisu danych statystyk dla gry $gameKey: ${it.message}")
            }
        }.addOnFailureListener {
            Log.e("STATS_DEBUG", "Błąd pobierania danych gry $gameKey: ${it.message}")
        }
    }

    /*
    Liczymy średni czas reakcji jako czas trwania rundy / liczba interakcji

    - Zapamiętanie czas startu gry (startTime).
    - W momencie zapisu statystyk (updateUserStatistics) liczenie ile trwała gra
    - Obliczenia średniego czasu reakcji - czas gry / liczba kliknięć
     */

    private var gameStartTime: Long = 0L
    private var gameClicks: Long = 0L

    //śledzenie gry
    //wywoływana na początku gry
    protected fun startReactionTracking(){
        gameStartTime = System.currentTimeMillis()
        gameClicks = 0
    }

    //wywoływana na końcu gry
    protected fun registerPlayerAction(){
        gameClicks++
    }

    //TODO: trzeba zrobic tak żeby to był rzeczywisty średni czas a nie ostatniej gry
    //zmiana żeby wyswietlaly sie 2 miejsca po przecinku
    //obliczanie średniego czasu reakcji
    protected fun getAverageReactionTime(): Double{
        val duration = (System.currentTimeMillis() - gameStartTime).coerceAtLeast(1L)
        //coerceAtLeast - upewnie sie ze liczba nie bedzie mniejsza niz dana wartość
        //przez to unikamy dzielenia przez 0 jezeli gra bedzie trwała krótko
        val clicks = gameClicks.coerceAtLeast(1)
        //clicks nigdy nie bedzie 0
        return duration.toDouble() / clicks / 1000.0 //sekundy
    }

}

