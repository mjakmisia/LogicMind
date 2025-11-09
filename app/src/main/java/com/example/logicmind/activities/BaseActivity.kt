package com.example.logicmind.activities

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.logicmind.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import java.util.Calendar
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
     * Sprawdza, czy użytkownik jest zalogowany i nie jest anonimowy.
     * @return true jeśli użytkownik jest zalogowany i nie anonimowy, false w przeciwnym wypadku
     */
    protected fun isUserLoggedIn(): Boolean {
        val user = auth.currentUser
        return if (user == null || user.isAnonymous) {
            Log.w("AUTH_DEBUG", "Użytkownik niezalogowany lub anonimowy — pomijam zapis do bazy")
            false
        } else {
            true
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
        if (!isUserLoggedIn()) return
        val user = auth.currentUser!!

        //jeśli nie jest gościem
        //zapis do bazy danych - operacja asynchroniczna, czyli nie blokuje wątku głównego
        val dbRef = db.getReference("users").child(user.uid).child("categories").child(categoryKey)
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
    }

    /**
     * Aktualizuje streak oraz bestStreak użytkownika w bazie
     */
    protected fun updateStreak() {
        if (!isUserLoggedIn()) return


        val uid = auth.currentUser!!.uid
        val userRef = db.getReference("users").child(uid)

        userRef.get().addOnSuccessListener { snapshot ->
            //pobieramy aktualny streak i bestStreak
            val streak = (snapshot.child("streak").value as? Long ?: 0L).toInt()
            val bestStreak = (snapshot.child("bestStreak").value as? Long ?: 0L).toInt()
            val lastPlayTimestamp = snapshot.child("lastPlayDate").getValue(Long::class.java)

            val today = Calendar.getInstance()

            val newStreak = if (lastPlayTimestamp == null) {
                // pierwsza gra użytkownika
                1
            } else {
                val lastPlayDay = Calendar.getInstance().apply { timeInMillis = lastPlayTimestamp }

                //obliczenie różnicy dni między dzisiejszym dniem a ostatnią grą
                val daysBetween =
                    ((today.timeInMillis - lastPlayDay.timeInMillis) / (1000 * 60 * 60 * 24)).toInt()

                when {
                    daysBetween == 0 -> streak // gra w tym samym dniu - pozostaje bez zmian
                    daysBetween == 1 -> streak + 1 // gra dzień po dniu — streak zwiększa się o 1
                    else -> 0 // opuścił jeden dzień — streak resetuje się do 0
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
     */

    protected fun updateUserStatistics(
        categoryKey: String,
        gameKey: String,
        starsEarned: Int = 0,
        accuracy: Double = 0.0,
        reactionTime: Double = 0.0
    ) {
        if (!isUserLoggedIn()) return

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
            val currentAvgReaction =
                snapshot.child("avgReactionTime").getValue(Double::class.java) ?: 0.0

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
    Liczymy średni czas reakcji jako czas trwania gry / liczba gwiazdek

    - startReactionTracking() — startuje licznik czasu.
    - pauseReactionTracking() — zatrzymuje czas gry (np. gdy gracz pauzuje).
    - resumeReactionTracking() — wznawia licznik po pauzie.
    - getAverageReactionTime() — zwraca średni czas reakcji (w sekundach)
     */

    private var gameStartTime: Long = 0L
    private var totalActiveTime: Long = 0L
    private var pauseStartTime: Long = 0L
    private var gameClicks: Long = 0L
    private var isPaused: Boolean = false

    //śledzenie gry
    //wywoływana na początku gry
    protected fun startReactionTracking() {
        totalActiveTime = 0L
        gameClicks = 0L
        isPaused = false
        pauseStartTime = 0L

        Handler(Looper.getMainLooper()).postDelayed({
            gameStartTime = System.currentTimeMillis()
            Toast.makeText(this, "Rozpoczęcie gry", Toast.LENGTH_SHORT).show()
        }, 4000)
    }

    //pauzowanie gry
    protected fun onGamePaused() {
        if (!isPaused) {
            pauseStartTime = System.currentTimeMillis()
            isPaused = true
            //Toast.makeText(this, "Gra zapauzowana o ${pauseStartTime}", Toast.LENGTH_SHORT).show()
        }
    }

    //wznowienie gry
    protected fun onGameResumed() {
        if (isPaused) {
            //przesuwamy startTime żeby nie liczyc pauzy
            val pauseDuration = System.currentTimeMillis() - pauseStartTime
            gameStartTime += pauseDuration
            isPaused = false
            //Toast.makeText(this, "Gra wznowiona o ${pauseDuration}", Toast.LENGTH_SHORT).show()
        }
    }

    //kliknięcia gracza
    protected fun registerPlayerAction() {
        gameClicks++
        Toast.makeText(this, "Kliknięcia: ${gameClicks}", Toast.LENGTH_SHORT).show()
    }

    //TODO: trzeba zrobic tak żeby to był rzeczywisty średni czas a nie ostatniej gry
    //zmiana żeby wyswietlaly sie 2 miejsca po przecinku
    /**obliczanie średniego czasu reakcji = czas gry / liczba gwiazdek
     * gdy gra się zaczyna = startReactionTracking(),

     * gdy użytkownik kliknie pauzę = onGamePaused() zapisuje czas rozpoczęcia pauzy,

     * gdy wznawia = onGameResumed() przesuwa gameStartTime o długość pauzy -ten okres nie liczy się do średniego czasu,

     * getAverageReactionTime() używa już tylko aktywnego czasu.
     */
    protected fun getAverageReactionTime(stars: Int = 0): Double {
        val currentTime = System.currentTimeMillis()
        val duration = (currentTime - gameStartTime).coerceAtLeast(1L)

        //jezeli przekazemy w argumatrze gwiazdki to wykorzytsa gwiazdki, jeżeli nie - użyje kliknięć
        val starsEarned = if (stars > 0) stars else gameClicks.coerceAtLeast(1)

        val avgReactionSec = duration.toDouble() / starsEarned.toDouble() / 1000.0 //w sekundach

        //coerceAtLeast - upewnie sie ze liczba nie bedzie mniejsza niz dana wartość
        //przez to unikamy dzielenia przez 0 jezeli gra bedzie trwała krótko

        Toast.makeText(
            this,
            "Rzeczywisty czas gry: %.2f s".format(duration.toDouble() / 1000),
            Toast.LENGTH_SHORT
        ).show()

        return avgReactionSec
    }

}

