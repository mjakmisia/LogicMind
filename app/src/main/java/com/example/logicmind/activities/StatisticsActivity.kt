package com.example.logicmind.activities
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.example.logicmind.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.core.view.isGone

class StatisticsActivity : BaseActivity() {

    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_statistics)

        supportActionBar?.hide()

        //auth = FirebaseAuth.getInstance()
        //trzeba podac url do bazy w regionie bo inaczej uzywa domyślnej bazy w us
        //db = FirebaseDatabase.getInstance("https://logicmind-default-rtdb.europe-west1.firebasedatabase.app")

        bottomNav = findViewById(R.id.bottomNavigationView)
        setupBottomMenu()

        val layoutLoggedIn = findViewById<ScrollView>(R.id.statisticsScrollView)
        val layoutNotLoggedIn = findViewById<LinearLayout>(R.id.layoutNotLoggedIn)
        val buttonLogin = findViewById<Button>(R.id.buttonLogin)
        val user = auth.currentUser //pobranie bieżącego użytkownika

        // Na start ukryj wszystkie widoki
        layoutLoggedIn.visibility = View.GONE
        layoutNotLoggedIn.visibility = View.GONE
        buttonLogin.visibility = View.GONE

        // Debugowanie: Logowanie stanu użytkownika
        Log.d("StatisticsActivity", "User: ${user?.uid ?: "null"}")

        if (user != null) {
            // Zalogowany użytkownik
            Log.d("StatisticsActivity", "Widok dla zalogowanego użytkownika")
            layoutLoggedIn.visibility = View.VISIBLE
            layoutNotLoggedIn.visibility = View.GONE

            setupExpandableStats() //rozwijanie statystyk
            loadUserStats(user.uid) //ładowanie statystyk z bazy
            loadLastPlayedGame(user.uid) //ostatnio zagrana gra
        } else {
            // Niezalogowany użytkownik
            Log.d("StatisticsActivity", "Widok dla niezalogowanego użytkownika")
            showLoginPrompt() //prompt do logowania
        }

        //przejscie z przycisku do strony logowania/rejestracji
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
        val pairs = listOf( //lista par (List<Pair<String, String>>)
            //  KOORDYNACJA
            Pair(R.id.layoutCoordinationGame1, R.id.layoutCoordinationGame1Stats),
            R.id.layoutCoordinationGame2 to R.id.layoutCoordinationGame2Stats,

            //  SKUPIENIE
            R.id.layoutAttentionGame1 to R.id.layoutAttentionGame1Stats,
            R.id.layoutAttentionGame2 to R.id.layoutAttentionGame2Stats,

            //  PAMIĘĆ
            R.id.layoutMemoryGame1 to R.id.layoutMemoryGame1Stats,
            R.id.layoutMemoryGame2 to R.id.layoutMemoryGame2Stats,

            // ROZWIĄZYWANIE PROBLEMÓW
            R.id.layoutReasoningGame1 to R.id.layoutReasoningGame1Stats,
            R.id.layoutReasoningGame2 to R.id.layoutReasoningGame2Stats
        )

        // Dla każdej pary ustawienie kliknięcia w layout gry
        pairs.forEach { (layoutId, statsId) ->
            val layout = findViewById<LinearLayout>(layoutId)
            val stats = findViewById<LinearLayout>(statsId)

            layout.setOnClickListener {
                // Jeśli statystyki są ukryte to pokaż, jeśli widoczne to ukryj
                stats.visibility =
                    if (stats.isGone) LinearLayout.VISIBLE else LinearLayout.GONE
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

        //Słownik tłumaczący nazwy gier do wyświetlenia dla użytkownika
//        val gameDisplayNames = mapOf(
//            "road_dash" to "Unikanie przeszkód",
//            "symbol_race" to "Wyścig symboli",
//            "word_search" to "Wyszukiwanie słów",
//            "fruit_sort" to "Sortowanie owoców",
//            "color_sequence" to "Kolorowa sekwencja",
//            "card_match" to "Dopasowywanie kart",
//            "number_addition" to "Dodawanie liczb",
//            "path_change" to "Zmiana ścieżki"
//        )

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
                        updateLastPlayedText(latestGameKey)
                    }
                }
                .addOnFailureListener {
                    completedRequests++
                    // Aktualizuj TextView po przetworzeniu wszystkich gier
                    if (completedRequests == gameMapping.size) {
                        findViewById<TextView>(R.id.tvLastPlayedGame)?.text =
                            if (latestGameKey != null) "Ostatnio zagrana gra: $latestGameKey"
                            else "Ostatnio zagrana gra: Brak"
                    }
                }
        }
    }

    /**
     * Ustawia tekst ostatnio zagranej gry
     */
    private fun updateLastPlayedText(gameKey: String?){ //gameKey - klucz zapisany w Firebase
        val textView = findViewById<TextView>(R.id.tvLastPlayedGame)
        if(gameKey == null){
            textView.text = "Ostatnio zagrana gra: Brak"
            return
        }

        //szukanie stringa po nazwie klucza
        val resID = resources.getIdentifier(gameKey,"string", packageName)
        val displayName = if(resID != 0){
            getString(resID)
        } else {
            gameKey
        }
        textView.text = "Ostatnio zagrana gra: $displayName"
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

}
