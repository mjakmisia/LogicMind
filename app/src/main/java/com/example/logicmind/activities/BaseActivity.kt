package com.example.logicmind.activities

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.logicmind.R
import com.example.logicmind.common.GameOverDialogFragment
import com.example.logicmind.common.GameStatsManager
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

    protected val gameStatsManager = GameStatsManager()

    override fun attachBaseContext(newBase: Context) {
        // aktualnie zapisany język z ustawień
        //MODE_PRIVATE - tylko dla tej aplikacji (nie dla innychna urządzeniu)
        val sharedPrefs = newBase.getSharedPreferences("Settings", MODE_PRIVATE)
        val lang = sharedPrefs.getString("My_Lang", "pl") ?: "pl"

        //obiekt Locale dla wybranego języka
        val locale = Locale.forLanguageTag(lang)
        Locale.setDefault(locale)

        //modyfikacja konfiguracji kontekstu tak, aby używała wybranego języka
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)

        //nowy kontekst z konfiguracją i przekazuje go do AppCompatActivity
        val context = newBase.createConfigurationContext(config)
        super.attachBaseContext(context)
    }

    companion object {
        private var isPersistenceEnabled = false

        //stałe do zapisywania i odczytywania danych z bundle
        private const val KEY_STATS_START_TIME = "STATS_START_TIME"
        private const val KEY_STATS_IS_PAUSED = "STATS_IS_PAUSED"
        private const val KEY_STATS_PAUSE_TIME = "STATS_PAUSE_TIME"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //przywracamy zapisany motyw jeśli był
        val sharedPrefs = getSharedPreferences("Settings", MODE_PRIVATE)
        val isDarkMode = sharedPrefs.getBoolean("DarkMode_Enabled", false)
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
            if (isDarkMode) androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
            else androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
        )

        // Ustawiamy pełny ekran i pozwalamy layoutowi wchodzić w wycięcia (notch)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.attributes.layoutInDisplayCutoutMode =
            android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES //pod kamerą na górze

        // Ukrywamy paski systemowe
        val insetsController = WindowInsetsControllerCompat(window, window.decorView)
        insetsController.hide( //ukrywa dolny i górny pasek systemowy
            androidx.core.view.WindowInsetsCompat.Type.statusBars() or androidx.core.view.WindowInsetsCompat.Type.navigationBars()
        )
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        // ustawienie persystencji tylko raz
        //używamy do zapisu danych jeżeli rozłączy się internet
        if (!isPersistenceEnabled) {
            try {
                // Pobierz instancję
                val database =
                    FirebaseDatabase.getInstance("https://logicmind-default-rtdb.europe-west1.firebasedatabase.app")
                database.setPersistenceEnabled(true) // Włącz tryb offline
                isPersistenceEnabled = true // Zablokuj ponowne wywołanie
            } catch (e: Exception) {
                Log.w("FIREBASE_INIT", "${e.message}")
            }
        }

        // Inicjalizacja Firebase w każdej aktywności
        auth = FirebaseAuth.getInstance()
        db =
            FirebaseDatabase.getInstance("https://logicmind-default-rtdb.europe-west1.firebasedatabase.app")

        //synchronizacja danych po powrocie online
        db.getReference("users").keepSynced(true)
    }

    /**
     * Metoda do ustawiania menu na dole
     */
    protected fun setupBottomNavigation(bottomNav: BottomNavigationView, selectedItemId: Int) {
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
        categoryKey: String, gameKey: String, onSuccess: String? = null
    ) {
        if (!isUserLoggedIn()) return
        val user = auth.currentUser!!

        //jeśli nie jest gościem
        //zapis do bazy danych - operacja asynchroniczna, czyli nie blokuje wątku głównego
        val dbRef = db.getReference("users").child(user.uid).child("categories").child(categoryKey)
            .child("games").child(gameKey)

        val timestamp = System.currentTimeMillis()

        dbRef.child("lastPlayed").setValue(timestamp).addOnSuccessListener {
                Log.d("GAME_DEBUG", "Zaktualizowano lastPlayed dla $gameKey")
                if (onSuccess != null) {
                    Log.d("GAME_DEBUG", onSuccess)
                }
                updateStreak() //wywołanie tutaj aby nie powtarzać kodu
        }.addOnFailureListener { e ->
                Log.e("GAME_DEBUG", "Błąd aktualizacji lastPlayed dla $gameKey", e)
            }
    }

    /**
     * Aktualizuje streak oraz bestStreak użytkownika w bazie
     */
    private fun updateStreak() {
        if (!isUserLoggedIn()) return

        val uid = auth.currentUser!!.uid
        val userRef = db.getReference("users").child(uid)

        userRef.get().addOnSuccessListener { snapshot ->
            //pobieramy aktualny streak i bestStreak
            val streak = (snapshot.child("streak").value as? Long ?: 0L).toInt()
            val bestStreak = (snapshot.child("bestStreak").value as? Long ?: 0L).toInt()
            val lastPlayTimestamp = snapshot.child("lastPlayDate").getValue(Long::class.java)

            val today = Calendar.getInstance()
            //zerujemy tu czas z today zeby porownac tylko daty
            stripTime(today)

            val newStreak = if (lastPlayTimestamp == null) {
                // pierwsza gra użytkownika
                1
            } else {
                val lastPlayDay = Calendar.getInstance().apply { timeInMillis = lastPlayTimestamp }
                stripTime(lastPlayDay)

                //obliczenie różnicy dni między dzisiejszym dniem a ostatnią grą
                val diffMillis = today.timeInMillis - lastPlayDay.timeInMillis
                val daysBetween = (diffMillis / (1000 * 60 * 60 * 24)).toInt()

                when (daysBetween) {
                    0 -> streak // gra w tym samym dniu - pozostaje bez zmian
                    1 -> streak + 1 // gra dzień po dniu — streak zwiększa się o 1
                    else -> 1 // opuścił jeden dzień — streak resetuje się do 1 bo właśnie zagrał
                }
            }

            // Zapis tylko jeśli streak się zmienił lub to pierwsza gra dzisiaj
            if (newStreak != streak || lastPlayTimestamp == null) {
                val updates = hashMapOf<String, Any>(
                    "streak" to newStreak,
                    "lastPlayDate" to System.currentTimeMillis() // Zapisujemy dokładny czas gry
                )

                if (newStreak > bestStreak) {
                    updates["bestStreak"] = newStreak
                }

                userRef.updateChildren(updates)
                    .addOnSuccessListener {
                        Log.d(
                            "STREAK_DEBUG",
                            "Streak zaktualizowany: $newStreak"
                        )
                    }
                    .addOnFailureListener { e -> Log.e("STREAK_DEBUG", "Błąd zapisu streaka", e) }
            }

        }.addOnFailureListener { e ->
            Log.e("STREAK_DEBUG", "Błąd pobierania danych użytkownika", e)
        }
    }

    /** Metoda pomocnicza do zerowania godzin, minut, sekund
     * Cofa zegarek zawsze do północy  */
    private fun stripTime(calendar: Calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
    }

    /**
     * Oblicza streak do wyświetlenia.
     * Jeśli minął więcej niż 1 dzień od ostatniej gry, zwraca 0 nawet jeśli w bazie jest stara liczba.
     */
    protected fun calculateDisplayStreak(snapshot: DataSnapshot): Int {
        val savedStreak = (snapshot.child("streak").value as? Long ?: 0L).toInt()
        //jeśli nigdy nie gra zwraca 0
        val lastPlayTimestamp =
            snapshot.child("lastPlayDate").getValue(Long::class.java) ?: return 0

        val today = Calendar.getInstance()
        stripTime(today) // resetujemy do północy

        val lastPlayDay = Calendar.getInstance().apply { timeInMillis = lastPlayTimestamp }
        stripTime(lastPlayDay)

        val diffMillis = today.timeInMillis - lastPlayDay.timeInMillis
        val daysBetween = (diffMillis / (1000 * 60 * 60 * 24)).toInt()

        // 0 dni różnicy czyli grał dzisiaj -> pokazujemy zapisany streak
        // 1 dzień różnicy czyli grał wczoraj -> pokazujemy zapisany streak
        // >1 dzień przerwy -> pokazujemy 0 (ale w bazie zresetuje się dopiero jak zagra)
        return if (daysBetween <= 1) {
            savedStreak
        } else {
            0
        }
    }


    /**
     * Stałe używane do dostępu do kategorii i gier w bazie danych Firebase.
     * Nazwy muszą zgadzać się z tym co jest w bazie
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
        const val GAME_LEFT_OR_RIGHT = "left_or_right"
        const val GAME_NUMBER_ADDITION = "number_addition"
        const val GAME_PATH_CHANGE = "path_change"
        const val GAME_ROAD_DASH = "road_dash"

    }

    /**
     * Aktualizuje statystyki gracza w Firestore po zakończeniu gry
     * @param categoryKey: String  - id kategorii gry
     * @param gameKey: String - id gry
     * @param starsEarned: Int - liczba zdobytych gwiazdek
     * @param accuracy: Double - celność
     * @param reactionTime: Double - średni czas reakcji
     *
     * Średni czas reakcji i celność są obliczane jako średnia ważona:
     * newAvg = (oldAvg * gamesPlayed + newValue) / (gamesPlayed + 1)
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

        //runTransaction wykonuje aktualizacje "atomowo" czyli jedną grę na raz
        //używany do odczytu danych które są zależne od poprzednich danych
        //wykonuje się w pętli retry - jeśli ktoś inny zmienił dane między odczytem i zapisem to odczyt jest wykonywany ponownie
        statsRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                //mutableData - tymczasowy lokalny obiekt reprezentuje dane węzła na którym robimy transakcje

                val currentStats = currentData.value as? Map<*, *> ?: emptyMap<String, Any>()

                val currentStars = (currentStats["totalStars"] as? Long ?: 0L).toInt()
                val currentGamesPlayed = (currentStats["gamesPlayed"] as? Long ?: 0L).toInt()
                val currentSumAccuracy = (currentStats["sumAccuracy"] as? Double ?: 0.0)
                val currentSumReaction = (currentStats["sumReactionTime"] as? Double ?: 0.0)

                // dodanie wartości z obecnej gry
                val newGamesPlayed = currentGamesPlayed + 1
                val newSumAccuracy = currentSumAccuracy + accuracy
                val newSumReaction = currentSumReaction + reactionTime

                // obliczenie rzeczywistej średniej globalnie
                val newAvgAccuracy =
                    if (newGamesPlayed > 0) newSumAccuracy / newGamesPlayed else 0.0
                val newAvgReaction =
                    if (newGamesPlayed > 0) newSumReaction / newGamesPlayed else 0.0

                val updatedStats = mapOf(
                    "totalStars" to (currentStars + starsEarned),
                    "gamesPlayed" to newGamesPlayed,
                    "sumAccuracy" to newSumAccuracy,       // suma używana do dokładnej średniej
                    "sumReactionTime" to newSumReaction,  // suma używana do dokładnej średniej
                    "avgAccuracy" to newAvgAccuracy,      // rzeczywista średnia
                    "avgReactionTime" to newAvgReaction   // rzeczywista średnia
                )

                currentData.value = updatedStats
                return Transaction.success(currentData)
            }

            override fun onComplete(
                error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?
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
            val currentSumAccuracy =
                snapshot.child("sumAccuracy").getValue(Double::class.java) ?: 0.0
            val currentSumReaction =
                snapshot.child("sumReactionTime").getValue(Double::class.java) ?: 0.0

            val newGamesPlayed = currentGamesPlayed + 1
            val newSumAccuracy = currentSumAccuracy + accuracy
            val newSumReaction = currentSumReaction + reactionTime

            val newAvgAccuracy = if (newGamesPlayed > 0) newSumAccuracy / newGamesPlayed else 0.0
            val newAvgReaction = if (newGamesPlayed > 0) newSumReaction / newGamesPlayed else 0.0


            val updatedGameData = mapOf(
                "starsEarned" to (currentStars + starsEarned),
                "bestStars" to maxOf(currentBestStars, starsEarned),
                "sumAccuracy" to newSumAccuracy,       // sumy używana do średniej
                "sumReactionTime" to newSumReaction,
                "accuracy" to newAvgAccuracy,
                "avgReactionTime" to newAvgReaction,
                "gamesPlayed" to newGamesPlayed,
                "lastPlayed" to System.currentTimeMillis()
            )

            Log.d(
                "STATS_DEBUG", """
                Statystyki przed aktualizacją dla gry $gameKey:
                starsEarned: $currentStars
                bestStars: $currentBestStars
                gamesPlayed: $currentGamesPlayed
                sumAccuracy: $currentSumAccuracy
                sumReactionTime: $currentSumReaction
                """.trimIndent()
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


    /**obliczanie średniego czasu reakcji = czas gry / liczba gwiazdek
     * gdy gra się zaczyna = startReactionTracking(),

     * gdy użytkownik kliknie pauzę = onGamePaused() zapisuje czas rozpoczęcia pauzy,

     * gdy wznawia = onGameResumed() przesuwa gameStartTime o długość pauzy -ten okres nie liczy się do średniego czasu,

     * getAverageReactionTime() używa już tylko aktywnego czasu.
     */
    protected fun onGamePaused() = gameStatsManager.onGamePaused()
    protected fun onGameResumed() = gameStatsManager.onGameResumed()

    protected fun getAverageReactionTime(stars: Int = 0): Double {
//        Toast.makeText(
//            this,
//            "AVG: ${gameStatsManager.calculateAvgReactionTime()}",
//            Toast.LENGTH_SHORT
//        ).show()

        val avgReactionSec = gameStatsManager.calculateAvgReactionTime(stars)

        // Pobranie globalnej średniej z bazy
        //pożniej usuń Toasty
        if (isUserLoggedIn()) {
            val userId = auth.currentUser!!.uid
            val statsRef =
                db.getReference("users").child(userId).child("statistics").child("avgReactionTime")

            statsRef.get().addOnSuccessListener { snapshot ->
                //val globalAvg = snapshot.getValue(Double::class.java) ?: 0.0
//                Toast.makeText(
//                    this,
//                    "Czas trwania gry: %.2f s\nŚredni czas reakcji (tej gry): %.2f s\nŚredni czas reakcji (globalny): %.2f s".format(durationSec, avgReactionSec, globalAvg),
//                    Toast.LENGTH_SHORT
//                ).show()
            }
        } else {
            // Dla niezalogowanego użytkownika pokazujemy tylko średni czas tej gry
//            Toast.makeText(
//                this, "Średni czas reakcji (tej gry): %.2f s".format(avgReactionSec), Toast.LENGTH_SHORT
//            ).show()
        }

        return avgReactionSec
    }

    /**
     * Uniwersalna metoda wyświetlająca dialog końca gry.
     * Zajmuje się zatrzymaniem czasu, zapisem do bazy i obsługą przycisków dialogu.
     *
     * @param categoryKey - Kategoria gry (np. GameKeys.CATEGORY_MEMORY)
     * @param gameKey - Klucz gry (np. GameKeys.GAME_CARD_MATCH)
     * @param gameName - Wyświetlana nazwa gry (do lastPlayedGame)
     * @param starManager - Menedżer gwiazdek danej gry
     * @param timerProgressBar - Pasek czasu danej gry
     * @param countdownManager - Menedżer odliczania (potrzebny do restartu)
     * @param currentBestScore - Rekord pobrany na początku gry
     * @param onRestartAction - Kod specyficzny dla danej gry, który musi się wykonać przy restarcie (np. tasowanie kart)
     */
    protected fun showGameOverDialog(
        categoryKey: String,
        gameKey: String,
        gameName: String,
        starManager: com.example.logicmind.common.StarManager,
        timerProgressBar: com.example.logicmind.common.GameTimerProgressBar,
        countdownManager: com.example.logicmind.common.GameCountdownManager,
        currentBestScore: Int,
        onRestartAction: () -> Unit
    ) {
        // zatrzymanie liczników
        timerProgressBar.stop()
        gameStatsManager.onGamePaused()

        // obliczenie danych
        val currentScore = starManager.starCount
        val durationSec = gameStatsManager.getPlayedTimeSec() // Używamy Twojej nowej metody

        // formatowanie czasu
        val timeDisplay = if (durationSec >= 60) {
            val mins = (durationSec / 60).toInt()
            val secs = (durationSec % 60).toInt()
            "${mins}m ${secs}s"
        } else {
            "${durationSec.toInt()}s"
        }

        val displayBestScore = maxOf(currentBestScore, currentScore)

        // zapis do bazy
        updateUserStatistics(
            categoryKey = categoryKey,
            gameKey = gameKey,
            starsEarned = currentScore,
            accuracy = gameStatsManager.calculateAccuracy(),
            reactionTime = getAverageReactionTime(stars = currentScore)
        )

        lastPlayedGame(categoryKey, gameKey, gameName)

        //Wyświetlenie dialogu
        val dialog = GameOverDialogFragment.newInstance(
            score = currentScore,
            timeFormatted = timeDisplay,
            bestScore = displayBestScore
        )

        starManager.reset()
        gameStatsManager.startReactionTracking()
        gameStatsManager.setGameStartTime(this)
        timerProgressBar.reset()

        // Specyficzny reset dla danej gry
        onRestartAction()

        // Start odliczania
        countdownManager.startCountdown()


        dialog.onExitListener = {
            val intent = Intent(this, MainActivity::class.java)

            // FLAG_ACTIVITY_CLEAR_TOP: MainActivity już jest w pamięci,
            // więc zamknij wszystko co jest po nim
            // i wróć do tej istniejącej instancji.
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK

            startActivity(intent)
            finish()
        }

        dialog.show(supportFragmentManager, "GameOverDialog")
    }

    /**
     * Zapisuje aktualny stan licznika czasu gry (czas startu, stan pauzy) do obiektu Bundle.
     * Używany, aby przy obrocie ekranu czas sie nie resetował
     *
     * @param outState Bundle, do którego zostaną zapisane dane.
     */
    protected fun saveGameStats(outState: Bundle) {
        //zapisz oryginalny czas rozpoczecia gry
        outState.putLong(KEY_STATS_START_TIME, gameStatsManager.getStartTime())

        //pobiera info o pauzie
        val (isPaused, pauseTime) = gameStatsManager.getPauseData()
        //zapisuje stan pauzy
        outState.putBoolean(KEY_STATS_IS_PAUSED, isPaused)
        outState.putLong(KEY_STATS_PAUSE_TIME, pauseTime)
    }

    /**
     * Odczytuje zapisane dane licznika czasu z Bundle i przywraca je w GameStatsManagerze.
     * Przywracamy czas startu na ten przed obrotem ekranu
     *
     * @param savedInstanceState Bundle zawierający zapisany stan gry
     */
    protected fun restoreGameStats(savedInstanceState: Bundle) {
        //odczytaj oryginalny czas rozpoczecia gry jesli nie mozesz ustaw 0
        val savedStartTime = savedInstanceState.getLong(KEY_STATS_START_TIME, 0L)
        //jesli go znaleziono to przywroc
        if (savedStartTime != 0L) {
            gameStatsManager.restoreStartTime(savedStartTime)
        }

        //odczytuje stan pauzy
        val wasPaused = savedInstanceState.getBoolean(KEY_STATS_IS_PAUSED, false)
        val savedPauseTime = savedInstanceState.getLong(KEY_STATS_PAUSE_TIME, 0L)
        gameStatsManager.restorePauseData(wasPaused, savedPauseTime)
    }

    /** Funkcja do sprawdzania połączenia z internetem */
    protected fun isNetworkAvailable(): Boolean {
        //poproszenie o dostęp do servisu sieciowego
        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        //sprawdzenie aktualnie używanej sieci, jeśli nie ma zwraca false
        val network = connectivityManager.activeNetwork ?: return false

        //jakie parametry ma ta sieć
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false

        // czy sieć ma zdolność (Capability) łączenia z internetem
        return activeNetwork.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}

