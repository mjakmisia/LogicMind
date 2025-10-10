package com.example.logicmind.activities

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.logicmind.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.core.view.isGone

class StatisticsActivity : BaseActivity() {

    // Firebase: autoryzacja i baza danych
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // Dolne menu nawigacyjne
    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_statistics)

        // Inicjalizacja Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Inicjalizacja dolnego menu
        bottomNav = findViewById(R.id.bottomNavigationView)
        setupBottomMenu()

        // Ustawienie kliknięć do rozwijania statystyk
        setupExpandableStats()

        // Pobranie danych użytkownika (lub wyświetlenie pustych / testowych danych)
        val user = auth.currentUser
        if (user != null) {
            loadUserStats(user.uid)
        } else {
            displayMockData()
        }
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
            // --- KATEGORIA: COORDINATION ---
            R.id.layoutCoordinationGame1 to R.id.layoutCoordinationGame1Stats,
            R.id.layoutCoordinationGame2 to R.id.layoutCoordinationGame2Stats,

            // --- KATEGORIA: ATTENTION ---
            R.id.layoutAttentionGame1 to R.id.layoutAttentionGame1Stats,
            R.id.layoutAttentionGame2 to R.id.layoutAttentionGame2Stats,

            // --- KATEGORIA: MEMORY ---
            R.id.layoutMemoryGame1 to R.id.layoutMemoryGame1Stats,
            R.id.layoutMemoryGame2 to R.id.layoutMemoryGame2Stats,

            // --- KATEGORIA: REASONING ---
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
     * Wczytuje dane statystyk użytkownika z Firestore
     * Struktura danych powinna wyglądać np. tak:
     * userStats/{uid}/coordination_game1_reaction = "1.2s"
     */
    private fun loadUserStats(uid: String) {
        db.collection("userStats").document(uid).get()
            .addOnSuccessListener { doc ->
                if (doc != null && doc.exists()) {
                    val data = doc.data ?: return@addOnSuccessListener

                    // === KATEGORIA: KOORDYNACJA ===
                    setStatsForGame(
                        R.id.tvCoordinationGame1Reaction,
                        R.id.tvCoordinationGame1Accuracy,
                        R.id.tvCoordinationGame1Total,
                        R.id.tvCoordinationGame1Best,
                        data["coordination_game1_reaction"],
                        data["coordination_game1_accuracy"],
                        data["coordination_game1_total"],
                        data["coordination_game1_best"]
                    )

                    setStatsForGame(
                        R.id.tvCoordinationGame2Reaction,
                        R.id.tvCoordinationGame2Accuracy,
                        R.id.tvCoordinationGame2Total,
                        R.id.tvCoordinationGame2Best,
                        data["coordination_game2_reaction"],
                        data["coordination_game2_accuracy"],
                        data["coordination_game2_total"],
                        data["coordination_game2_best"]
                    )

                    // TODO: Dodaj kolejne kategorie: Attention, Memory, Reasoning
                } else {
                    // Jeśli nie ma danych, pokaż testowe wartości
                    displayMockData()
                }
            }
            .addOnFailureListener {
                // Jeśli wystąpi błąd, również pokazujemy dane przykładowe
                displayMockData()
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
    }
}
