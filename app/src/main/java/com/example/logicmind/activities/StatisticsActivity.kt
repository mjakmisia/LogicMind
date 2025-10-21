package com.example.logicmind.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.logicmind.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import androidx.core.view.isGone

class StatisticsActivity : BaseActivity() {

    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_statistics)

        supportActionBar?.hide()

        auth = FirebaseAuth.getInstance()
        //trzeba podac url do bazy w regionie bo inaczej uzywa domyślnej bazy w us
        db = FirebaseDatabase.getInstance("https://logicmind-default-rtdb.europe-west1.firebasedatabase.app")

        bottomNav = findViewById(R.id.bottomNavigationView)
        setupBottomMenu()

        val layoutLoggedIn = findViewById<ScrollView>(R.id.statisticsScrollView)
        val layoutNotLoggedIn = findViewById<LinearLayout>(R.id.layoutNotLoggedIn)
        val buttonLogin = findViewById<Button>(R.id.buttonLogin)
        val user = auth.currentUser

        // Na start ukryj wszystkie widoki
        layoutLoggedIn.visibility = View.GONE
        layoutNotLoggedIn.visibility = View.GONE
        buttonLogin.visibility = View.GONE

        // Debugowanie: Logowanie stanu użytkownika
        Log.d("StatisticsActivity", "User: ${user?.uid ?: "null"}")

        if (user != null) {
            // Zalogowany użytkownik
            Log.d("StatisticsActivity", "Showing logged-in view")
            layoutLoggedIn.visibility = View.VISIBLE
            layoutNotLoggedIn.visibility = View.GONE

            setupExpandableStats()
            loadUserStats(user.uid)
            loadLastPlayedGame(user.uid)
        } else {
            // Niezalogowany użytkownik
            Log.d("StatisticsActivity", "Showing not logged-in view")
            showLoginPrompt()
        }

        buttonLogin.setOnClickListener {
            startActivity(Intent(this, WelcomeActivity::class.java))
        }
    }

    /**
     * Pokazuje komunikat i przycisk, gdy użytkownik nie jest zalogowany
     */
    private fun showLoginPrompt() {
        val layoutLoggedIn = findViewById<ScrollView>(R.id.statisticsScrollView)
        val layoutNotLoggedIn = findViewById<LinearLayout>(R.id.layoutNotLoggedIn)
        val buttonLogin = findViewById<Button>(R.id.buttonLogin)

        // Ukryj główną zawartość statystyk
        layoutLoggedIn.visibility = View.GONE

        // Pokaż komunikat i przycisk logowania
        layoutNotLoggedIn.visibility = View.VISIBLE
        buttonLogin.visibility = View.VISIBLE

        // Po kliknięciu — przekieruj do WelcomeActivity
        buttonLogin.setOnClickListener {
            startActivity(Intent(this, WelcomeActivity::class.java))
            finish()
        }

        // Ustaw domyślny tekst dla tvLastPlayedGame
        findViewById<TextView>(R.id.tvLastPlayedGame)?.text = "Ostatnio zagrana gra: Brak"
    }


    /**
     * Konfiguracja dolnego menu nawigacyjnego
     */
    private fun setupBottomMenu() {
        bottomNav.selectedItemId = R.id.nav_statistics

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    true
                }
                R.id.nav_statistics -> true
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

    /**
     * Funkcja pozwalająca rozwijać i zwijać statystyki po kliknięciu w tytuł gry.
     * Każdy wpis to para: (layout gry, układ statystyk dla tej gry)
     */
    private fun setupExpandableStats() {
        val pairs = listOf(
            // --- KATEGORIA: KOORDYNACJA ---
            R.id.layoutCoordinationGame1 to R.id.layoutCoordinationGame1Stats,
            R.id.layoutCoordinationGame2 to R.id.layoutCoordinationGame2Stats,

            // --- KATEGORIA: SKUPIENIE ---
            R.id.layoutAttentionGame1 to R.id.layoutAttentionGame1Stats,
            R.id.layoutAttentionGame2 to R.id.layoutAttentionGame2Stats,

            // --- KATEGORIA: PAMIĘĆ ---
            R.id.layoutMemoryGame1 to R.id.layoutMemoryGame1Stats,
            R.id.layoutMemoryGame2 to R.id.layoutMemoryGame2Stats,

            // --- KATEGORIA: ROZWIĄZYWANIE PROBLEMÓW ---
            R.id.layoutReasoningGame1 to R.id.layoutReasoningGame1Stats,
            R.id.layoutReasoningGame2 to R.id.layoutReasoningGame2Stats
        )

        // Dla każdej pary ustawiamy kliknięcie w layout gry
        pairs.forEach { (layoutId, statsId) ->
            val layout = findViewById<LinearLayout>(layoutId)
            val stats = findViewById<LinearLayout>(statsId)

            layout.setOnClickListener {
                // Jeśli statystyki są ukryte → pokaż, jeśli widoczne → ukryj
                stats.visibility =
                    if (stats.isGone) LinearLayout.VISIBLE else LinearLayout.GONE
            }
        }
    }

    /**
     * Wczytuje dane statystyk użytkownika z Realtime Database
     * Struktura danych: users/[uid]/categories/[category]/games/[gameName]
     */
    private fun loadUserStats(uid: String) {
        // Mapowanie gier na widoki (nazwy gier z bazy na ID widoków)
        val gameMapping = listOf(
            Triple("Koordynacja", "Cards_on_the_Roads", listOf(
                R.id.tvCoordinationGame1Reaction,
                R.id.tvCoordinationGame1Accuracy,
                R.id.tvCoordinationGame1Total,
                R.id.tvCoordinationGame1Best
            )),
            Triple("Koordynacja", "Symbol_Race", listOf(
                R.id.tvCoordinationGame2Reaction,
                R.id.tvCoordinationGame2Accuracy,
                R.id.tvCoordinationGame2Total,
                R.id.tvCoordinationGame2Best
            )),
            Triple("Skupienie", "Word_Search", listOf(
                R.id.tvAttentionGame1Reaction,
                R.id.tvAttentionGame1Accuracy,
                R.id.tvAttentionGame1Total,
                R.id.tvAttentionGame1Best
            )),
            Triple("Skupienie", "Left_or_Right", listOf(
                R.id.tvAttentionGame2Reaction,
                R.id.tvAttentionGame2Accuracy,
                R.id.tvAttentionGame2Total,
                R.id.tvAttentionGame2Best
            )),
            Triple("Pamiec", "Color_Sequence", listOf(
                R.id.tvMemoryGame1Reaction,
                R.id.tvMemoryGame1Accuracy,
                R.id.tvMemoryGame1Total,
                R.id.tvMemoryGame1Best
            )),
            Triple("Pamiec", "Memory_Game", listOf(
                R.id.tvMemoryGame2Reaction,
                R.id.tvMemoryGame2Accuracy,
                R.id.tvMemoryGame2Total,
                R.id.tvMemoryGame2Best
            )),
            Triple("Rozwiazywanie_problemow", "Number_Addition", listOf(
                R.id.tvReasoningGame1Reaction,
                R.id.tvReasoningGame1Accuracy,
                R.id.tvReasoningGame1Total,
                R.id.tvReasoningGame1Best
            )),
            Triple("Rozwiazywanie_problemow", "Path_Change", listOf(
                R.id.tvReasoningGame2Reaction,
                R.id.tvReasoningGame2Accuracy,
                R.id.tvReasoningGame2Total,
                R.id.tvReasoningGame2Best
            ))
        )

        // Pobieranie danych dla każdej gry
        gameMapping.forEach { (category, gameName, viewIds) ->
            db.getReference("users").child(uid).child("categories").child(category).child("games").child(gameName)
                .get()
                .addOnSuccessListener { snapshot ->
                    if (snapshot.exists()) {
                        val data = snapshot.value as? Map<String, Any> ?: return@addOnSuccessListener

                        val reaction = data["avgReactionTime"]?.toString() ?: "N/A"
                        val accuracy = data["accuracy"]?.toString() ?: "N/A"
                        val total = data["gamesPlayed"]?.toString() ?: "0" // Używamy gamesPlayed jako total
                        val best = data["bestScore"]?.toString() ?: "0"

                        setStatsForGame(
                            viewIds[0], viewIds[1], viewIds[2], viewIds[3],
                            reaction, accuracy, total, best
                        )
                    } else {
                        // Jeśli dane gry nie istnieją, ustaw domyślne wartości
                        setStatsForGame(viewIds[0], viewIds[1], viewIds[2], viewIds[3], "N/A", "N/A", "0", "0")
                    }
                }
                .addOnFailureListener {
                    // W razie błędu ustaw domyślne wartości
                    setStatsForGame(viewIds[0], viewIds[1], viewIds[2], viewIds[3], "N/A", "N/A", "0", "0")
                }
        }
    }

    /**
     * Pobiera nazwę ostatnio zagranej gry na podstawie pola lastPlayed
     */
    private fun loadLastPlayedGame(uid: String) {
        // Mapowanie kategorii i gier, zgodne z loadUserStats
        val gameMapping = listOf(
            Pair("Koordynacja", "Cards_on_the_Roads"),
            Pair("Koordynacja", "Symbol_Race"),
            Pair("Skupienie", "Word_Search"),
            Pair("Skupienie", "Left_or_Right"),
            Pair("Pamiec", "Color_Sequence"),
            Pair("Pamiec", "Memory_Game"),
            Pair("Rozwiazywanie_problemow", "Number_Addition"),
            Pair("Rozwiazywanie_problemow", "Path_Change")
        )

        var latestGame: String? = null
        var latestTimestamp: Long? = null
        var completedRequests = 0 // Licznik odpowiedzi z bazy

        // Pobieranie lastPlayed dla każdej gry
        gameMapping.forEach { (category, gameName) ->
            db.getReference("users").child(uid).child("categories").child(category).child("games").child(gameName)
                .child("lastPlayed")
                .get()
                .addOnSuccessListener { snapshot ->
                    completedRequests++
                    if (snapshot.exists()) {
                        val timestamp = snapshot.value as? Long
                        if (timestamp != null && (latestTimestamp == null || timestamp > latestTimestamp!!)) {
                            latestTimestamp = timestamp
                            latestGame = gameName
                        }
                    }
                    // Aktualizuj TextView po przetworzeniu wszystkich gier
                    if (completedRequests == gameMapping.size) {
                        findViewById<TextView>(R.id.tvLastPlayedGame)?.text =
                            if (latestGame != null) "Ostatnio zagrana gra: $latestGame"
                            else "Ostatnio zagrana gra: Brak"
                    }
                }
                .addOnFailureListener {
                    completedRequests++
                    // Aktualizuj TextView po przetworzeniu wszystkich gier
                    if (completedRequests == gameMapping.size) {
                        findViewById<TextView>(R.id.tvLastPlayedGame)?.text =
                            if (latestGame != null) "Ostatnio zagrana gra: $latestGame"
                            else "Ostatnio zagrana gra: Brak"
                    }
                }
        }
    }

    /**
     * Ustawia wartości statystyk dla pojedynczej gry.
     * Każda gra ma 4 wskaźniki: czas reakcji, poprawność, punkty, najlepszy wynik.
     */
    private fun setStatsForGame(
        reactionId: Int, accuracyId: Int, totalId: Int, bestId: Int,
        reactionValue: Any?, accuracyValue: Any?, totalValue: Any?, bestValue: Any?
    ) {
        findViewById<TextView>(reactionId).text =
            getString(R.string.avg_reaction_time_value, reactionValue ?: "N/A")

        findViewById<TextView>(accuracyId).text =
            getString(R.string.accuracy_value, accuracyValue ?: "N/A")

        findViewById<TextView>(totalId).text =
            getString(R.string.total_points_value, totalValue ?: "0")

        findViewById<TextView>(bestId).text =
            getString(R.string.highest_score_value, bestValue ?: "0")
    }

    /**
     * Wyświetla dane przykładowe (mockowe),
     * używane gdy użytkownik nie ma jeszcze zapisanych wyników.
     */
    private fun displayMockData() {
        // Kategoria: Koordynacja - Gra 1
        findViewById<TextView>(R.id.tvCoordinationGame1Reaction).text =
            getString(R.string.avg_reaction_time_value, "1.2s")
        findViewById<TextView>(R.id.tvCoordinationGame1Accuracy).text =
            getString(R.string.accuracy_value, "88")
        findViewById<TextView>(R.id.tvCoordinationGame1Total).text =
            getString(R.string.total_points_value, "1350")
        findViewById<TextView>(R.id.tvCoordinationGame1Best).text =
            getString(R.string.highest_score_value, "260")

        // Kategoria: Koordynacja - Gra 2
        findViewById<TextView>(R.id.tvCoordinationGame2Reaction).text =
            getString(R.string.avg_reaction_time_value, "1.1s")
        findViewById<TextView>(R.id.tvCoordinationGame2Accuracy).text =
            getString(R.string.accuracy_value, "85")
        findViewById<TextView>(R.id.tvCoordinationGame2Total).text =
            getString(R.string.total_points_value, "1200")
        findViewById<TextView>(R.id.tvCoordinationGame2Best).text =
            getString(R.string.highest_score_value, "240")

        // Kategoria: Skupienie - Gra 1
        findViewById<TextView>(R.id.tvAttentionGame1Reaction).text =
            getString(R.string.avg_reaction_time_value, "1.3s")
        findViewById<TextView>(R.id.tvAttentionGame1Accuracy).text =
            getString(R.string.accuracy_value, "90")
        findViewById<TextView>(R.id.tvAttentionGame1Total).text =
            getString(R.string.total_points_value, "1500")
        findViewById<TextView>(R.id.tvAttentionGame1Best).text =
            getString(R.string.highest_score_value, "280")

        // Kategoria: Skupienie - Gra 2
        findViewById<TextView>(R.id.tvAttentionGame2Reaction).text =
            getString(R.string.avg_reaction_time_value, "1.0s")
        findViewById<TextView>(R.id.tvAttentionGame2Accuracy).text =
            getString(R.string.accuracy_value, "87")
        findViewById<TextView>(R.id.tvAttentionGame2Total).text =
            getString(R.string.total_points_value, "1300")
        findViewById<TextView>(R.id.tvAttentionGame2Best).text =
            getString(R.string.highest_score_value, "250")

        // Kategoria: Pamięć - Gra 1
        findViewById<TextView>(R.id.tvMemoryGame1Reaction).text =
            getString(R.string.avg_reaction_time_value, "1.5s")
        findViewById<TextView>(R.id.tvMemoryGame1Accuracy).text =
            getString(R.string.accuracy_value, "85")
        findViewById<TextView>(R.id.tvMemoryGame1Total).text =
            getString(R.string.total_points_value, "1400")
        findViewById<TextView>(R.id.tvMemoryGame1Best).text =
            getString(R.string.highest_score_value, "270")

        // Kategoria: Pamięć - Gra 2
        findViewById<TextView>(R.id.tvMemoryGame2Reaction).text =
            getString(R.string.avg_reaction_time_value, "1.4s")
        findViewById<TextView>(R.id.tvMemoryGame2Accuracy).text =
            getString(R.string.accuracy_value, "89")
        findViewById<TextView>(R.id.tvMemoryGame2Total).text =
            getString(R.string.total_points_value, "1450")
        findViewById<TextView>(R.id.tvMemoryGame2Best).text =
            getString(R.string.highest_score_value, "275")

        // Kategoria: Rozwiązywanie problemów - Gra 1
        findViewById<TextView>(R.id.tvReasoningGame1Reaction).text =
            getString(R.string.avg_reaction_time_value, "1.6s")
        findViewById<TextView>(R.id.tvReasoningGame1Accuracy).text =
            getString(R.string.accuracy_value, "86")
        findViewById<TextView>(R.id.tvReasoningGame1Total).text =
            getString(R.string.total_points_value, "1600")
        findViewById<TextView>(R.id.tvReasoningGame1Best).text =
            getString(R.string.highest_score_value, "290")

        // Kategoria: Rozwiązywanie problemów - Gra 2
        findViewById<TextView>(R.id.tvReasoningGame2Reaction).text =
            getString(R.string.avg_reaction_time_value, "1.7s")
        findViewById<TextView>(R.id.tvReasoningGame2Accuracy).text =
            getString(R.string.accuracy_value, "84")
        findViewById<TextView>(R.id.tvReasoningGame2Total).text =
            getString(R.string.total_points_value, "1550")
        findViewById<TextView>(R.id.tvReasoningGame2Best).text =
            getString(R.string.highest_score_value, "285")
    }
}
