package com.example.logicmind.activities
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.logicmind.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.core.view.isGone
import com.example.logicmind.databinding.ActivityStatisticsBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StatisticsActivity : BaseActivity() {

    private lateinit var bottomNav: BottomNavigationView
    private lateinit var binding: ActivityStatisticsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStatisticsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()


        //dolne menu
        setupBottomNavigation(binding.includeBottomNav.bottomNavigationView, R.id.nav_statistics)

//        val layoutLoggedIn = findViewById<ScrollView>(R.id.statisticsScrollView)
//        val layoutNotLoggedIn = findViewById<LinearLayout>(R.id.layoutNotLoggedIn)
//        val buttonLogin = findViewById<Button>(R.id.buttonLogin)
//
//        // Na start ukryj wszystkie widoki
//        layoutLoggedIn.visibility = View.GONE
//        layoutNotLoggedIn.visibility = View.GONE
//        buttonLogin.visibility = View.GONE

        val user = auth.currentUser //pobranie bieżącego użytkownika
        // Debugowanie: Logowanie stanu użytkownika
        Log.d("StatisticsActivity", "User: ${user?.uid ?: "null"}")

        if (user != null) {
            // Zalogowany użytkownik
            Log.d("StatisticsActivity", "Widok dla zalogowanego użytkownika")
            binding.statisticsScrollView.visibility = View.VISIBLE
            binding.layoutNotLoggedIn.visibility = View.GONE

            setupExpandableStats() //rozwijanie statystyk
            loadUserStats(user.uid) //ładowanie statystyk z bazy
            loadLastPlayedGame(user.uid) //ostatnio zagrana gra
        } else {
            // Niezalogowany użytkownik
            Log.d("StatisticsActivity", "Widok dla niezalogowanego użytkownika")
            binding.statisticsScrollView.visibility = View.GONE
            binding.layoutNotLoggedIn.visibility = View.VISIBLE
            binding.buttonLogin.visibility = View.VISIBLE
            binding.buttonLogin.setOnClickListener {
                startActivity(Intent(this, WelcomeActivity::class.java))
                finish()
            }
        }
    }

    /**
     * Pokazuje komunikat i przycisk, gdy użytkownik nie jest zalogowany
     */
    private fun showLoginPrompt() {
        val layoutLoggedIn = findViewById<ScrollView>(R.id.statisticsScrollView)
        val layoutNotLoggedIn = findViewById<LinearLayout>(R.id.layoutNotLoggedIn)
        val buttonLogin = findViewById<Button>(R.id.buttonLogin)

        // Ukrywa zawartość statystyk
        layoutLoggedIn.visibility = View.GONE

        // Pokaż komunikat i przycisk
        layoutNotLoggedIn.visibility = View.VISIBLE
        buttonLogin.visibility = View.VISIBLE

        //przejscie z przycisku do strony logowania/rejestracji
        buttonLogin.setOnClickListener {
            startActivity(Intent(this, WelcomeActivity::class.java))
            finish()
        }

//        // Ustaw domyślny tekst dla tvLastPlayedGame
//        findViewById<TextView>(R.id.tvLastPlayedGame)?.text = "Ostatnio zagrana gra: Brak"
    }


    /**
     * Funkcja pozwalająca rozwijać i zwijać statystyki po kliknięciu w tytuł gry.
     * Każdy wpis to para: (layout gry, układ statystyk dla tej gry)
     */
    private fun setupExpandableStats() {
        val pairs = listOf( //lista par (List<Pair<Int, Int>>)
            Pair(R.id.layoutCoordinationGame1, R.id.layoutCoordinationGame1Stats),
            Pair(R.id.layoutCoordinationGame2, R.id.layoutCoordinationGame2Stats),
            Pair(R.id.layoutAttentionGame1, R.id.layoutAttentionGame1Stats),
            Pair(R.id.layoutAttentionGame2, R.id.layoutAttentionGame2Stats),
            Pair(R.id.layoutMemoryGame1, R.id.layoutMemoryGame1Stats),
            Pair(R.id.layoutMemoryGame2, R.id.layoutMemoryGame2Stats),
            Pair(R.id.layoutReasoningGame1, R.id.layoutReasoningGame1Stats),
            Pair(R.id.layoutReasoningGame2, R.id.layoutReasoningGame2Stats)
        )

        // Dla każdej pary ustawienie kliknięcia w layout gry
        pairs.forEach { (layoutId, statsId) ->
            val layout = binding.root.findViewById<LinearLayout>(layoutId)
            val stats = binding.root.findViewById<LinearLayout>(statsId)

            layout.setOnClickListener {
                // Jeśli statystyki są ukryte to pokaż, jeśli widoczne to ukryj
                stats.visibility = if (stats.isGone) View.VISIBLE else View.GONE
            }
        }
    }

    /**
     * Wczytuje dane statystyk użytkownika z Firebase
     * Struktura danych: users/[uid]/categories/[category]/games/[gameName]
     */
    private fun loadUserStats(uid: String) {
        // Mapowanie gier na widoki (nazwy gier z bazy na ID widoków)
        val gameMapping = listOf( //gameMapping - lista par (List<Triple<String, String, List<Int>>>)
            Triple("Koordynacja", "road_dash", listOf(
                R.id.tvCoordinationGame1Reaction,
                R.id.tvCoordinationGame1Accuracy,
                R.id.tvCoordinationGame1Total,
                R.id.tvCoordinationGame1Best
            )),
            Triple("Koordynacja", "symbol_race", listOf(
                R.id.tvCoordinationGame2Reaction,
                R.id.tvCoordinationGame2Accuracy,
                R.id.tvCoordinationGame2Total,
                R.id.tvCoordinationGame2Best
            )),
            Triple("Skupienie", "word_search", listOf(
                R.id.tvAttentionGame1Reaction,
                R.id.tvAttentionGame1Accuracy,
                R.id.tvAttentionGame1Total,
                R.id.tvAttentionGame1Best
            )),
            Triple("Skupienie", "fruit_sort", listOf(
                R.id.tvAttentionGame2Reaction,
                R.id.tvAttentionGame2Accuracy,
                R.id.tvAttentionGame2Total,
                R.id.tvAttentionGame2Best
            )),
            Triple("Pamiec", "color_sequence", listOf(
                R.id.tvMemoryGame1Reaction,
                R.id.tvMemoryGame1Accuracy,
                R.id.tvMemoryGame1Total,
                R.id.tvMemoryGame1Best
            )),
            Triple("Pamiec", "card_match", listOf(
                R.id.tvMemoryGame2Reaction,
                R.id.tvMemoryGame2Accuracy,
                R.id.tvMemoryGame2Total,
                R.id.tvMemoryGame2Best
            )),
            Triple("Rozwiazywanie_problemow", "number_addition", listOf(
                R.id.tvReasoningGame1Reaction,
                R.id.tvReasoningGame1Accuracy,
                R.id.tvReasoningGame1Total,
                R.id.tvReasoningGame1Best
            )),
            Triple("Rozwiazywanie_problemow", "path_change", listOf(
                R.id.tvReasoningGame2Reaction,
                R.id.tvReasoningGame2Accuracy,
                R.id.tvReasoningGame2Total,
                R.id.tvReasoningGame2Best
            ))
        )

        // Pobieranie danych dla każdej gry
        //gameMapping - lista par (List<Triple<String, String, List<Int>>>)
        gameMapping.forEach { (category, gameName, viewIds) ->
            db.getReference("users").child(uid).child("categories").child(category).child("games").child(gameName)
                .get() //pobiera jednorazowo dane z bazy
                .addOnSuccessListener { snapshot ->
                    val messageIfEmpty = "Zagraj w grę aby zobaczyć statystyki"

                    if (snapshot.exists()) {
                        //snaphot - obiekt typu DataSnapshot zawierający dane z bazy w danym miejscu w momencie pobrania
                        //zwraca całą strukturę danych dla jednej gry w postaci mapy

                        //sprawdza czy snapshot jest mapą
                        val data = snapshot.value as? Map<*, *>

                        if(data != null){
                            val reaction = data["avgReactionTime"]?.toString() ?: messageIfEmpty
                            val accuracy = data["accuracy"]?.toString() ?: messageIfEmpty
                            val total = data["gamesPlayed"]?.toString() ?: messageIfEmpty
                            val best = data["bestScore"]?.toString() ?: messageIfEmpty

                            //Ustawia wartości statystyk dla pojedynczej gry.
                            //Każda gra ma 4 wskaźniki: czas reakcji, poprawność, punkty, najlepszy wynik.
                            setStatsForGame(
                                viewIds[0], viewIds[1], viewIds[2], viewIds[3],
                                reaction, accuracy, total, best
                            )
                        } else {
                            // Dane istnieją ale nie są mapą
                            //czyli są w złym formacie
                            setStatsForGame(viewIds[0], viewIds[1], viewIds[2], viewIds[3],
                                messageIfEmpty, messageIfEmpty, messageIfEmpty, messageIfEmpty
                            )
                        }
                    } else {
                        //Dane nie są w bazie
                        //czyli gra nie została rozegrana
                        setStatsForGame(viewIds[0], viewIds[1], viewIds[2], viewIds[3],
                            messageIfEmpty, messageIfEmpty, messageIfEmpty, messageIfEmpty)

                    }
                }
                .addOnFailureListener {
                    val messageIfError = "Błąd pobierania danych"
                    setStatsForGame(viewIds[0], viewIds[1], viewIds[2], viewIds[3],
                        messageIfError, messageIfError, messageIfError, messageIfError
                    )
                }
        }
    }

    /**
     * Pobiera nazwę ostatnio zagranej gry na podstawie pola lastPlayed
     */
    private fun loadLastPlayedGame(uid: String) {
        // Mapowanie kategorii i gier, zgodne z loadUserStats
        val gameMapping = listOf(
            Pair("Koordynacja", "road_dash"),
            Pair("Koordynacja", "symbol_race"),
            Pair("Skupienie", "word_search"),
            Pair("Skupienie", "fruit_sort"),
            Pair("Pamiec", "color_sequence"),
            Pair("Pamiec", "card_match"),
            Pair("Rozwiazywanie_problemow", "number_addition"),
            Pair("Rozwiazywanie_problemow", "path_change")
        )

        var latestGameKey: String? = null
        var latestTimestamp: Long? = null
        var completedRequests = 0 // ile zapytań z bazy już się zakończyło
        //wyświetlamy wynik dopiero jak wszystkie pytania się zakończą (sukcesem lub błędem)


        // Pobieranie lastPlayed dla każdej gry
        gameMapping.forEach { (category, gameName) ->
            db.getReference("users").child(uid).child("categories").child(category).child("games").child(gameName)
                .child("lastPlayed")
                .get() //pobiera jednorazowo dane z bazy
                .addOnSuccessListener { snapshot ->
                    completedRequests++
                    if (snapshot.exists()) {
                        //w timestamp większa liczba = nowsza gra
                        //można wyswietlic dokładną datę i godzinę ostatniej gry
                        //jeżeli timestamp nie jest Long to będzie null
                        val timestamp = snapshot.value as? Long
                        Log.d("LAST_PLAYED_DEBUG (StatisticsActivity)", "Gra: $gameName | Timestamp: $timestamp")

                        if (timestamp != null && (latestTimestamp == null || timestamp > latestTimestamp!!)) {
                            latestTimestamp = timestamp
                            latestGameKey = gameName
                        }
                    }
                    //wszystkie pytania zakończone - aktualizuje textview
                    if (completedRequests == gameMapping.size) {
                        Log.d("LAST_PLAYED_DEBUG (StatisticsActivity)", "Wybrana gra (LastPlayedGame): $latestGameKey | Timestamp: $latestTimestamp")
                        updateLastPlayedText(latestGameKey, latestTimestamp)
                    }
                }
                .addOnFailureListener {
                    completedRequests++
                    // Aktualizuj TextView po przetworzeniu wszystkich gier
                    if (completedRequests == gameMapping.size) {
                        updateLastPlayedText(latestGameKey, latestTimestamp)
                    }
                }
        }
    }

    /**
     * Ustawia tekst ostatnio zagranej gry
     */
     //gameKey - klucz zapisany w Firebase
    private fun updateLastPlayedText(gameKey: String?, timestamp: Long?) {
            if (gameKey == null || timestamp == null) {
                binding.tvLastPlayedGame.text = "Ostatnio zagrana gra: Brak"
                return
            }

            val resID = resources.getIdentifier(gameKey, "string", packageName)
            val displayName = if (resID != 0) {
                getString(resID)
            } else {
                gameKey
            }
            val dateFormat = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault())
            val lastPlayedDate = dateFormat.format(Date(timestamp))
            binding.tvLastPlayedGame.text = "Ostatnio zagrana gra: $displayName ($lastPlayedDate)"
        }

    /**
     * Ustawia wartości statystyk dla pojedynczej gry.
     * Każda gra ma 4 wskaźniki: czas reakcji, poprawność, punkty, najlepszy wynik.
     */
    private fun setStatsForGame(
        reactionId: Int, accuracyId: Int, totalId: Int, bestId: Int,
        reactionValue: Any?, accuracyValue: Any?, totalValue: Any?, bestValue: Any?
    ) {
        binding.root.findViewById<TextView>(reactionId).text =
            getString(R.string.avg_reaction_time_value, reactionValue ?: "N/A")
        binding.root.findViewById<TextView>(accuracyId).text =
            getString(R.string.accuracy_value, accuracyValue ?: "N/A")
        binding.root.findViewById<TextView>(totalId).text =
            getString(R.string.total_points_value, totalValue ?: "0")
        binding.root.findViewById<TextView>(bestId).text =
            getString(R.string.highest_score_value, bestValue ?: "0")
    }
}
