package com.example.logicmind.activities

import android.content.Intent
import android.os.Bundle
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.logicmind.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class StatisticsActivity : BaseActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // Kategorie
    private lateinit var tvCoordinationReaction: TextView
    private lateinit var tvCoordinationAccuracy: TextView
    private lateinit var tvCoordinationTotal: TextView
    private lateinit var tvCoordinationBest: TextView

    private lateinit var tvFocusReaction: TextView
    private lateinit var tvFocusAccuracy: TextView
    private lateinit var tvFocusTotal: TextView
    private lateinit var tvFocusBest: TextView

    private lateinit var tvMemoryReaction: TextView
    private lateinit var tvMemoryAccuracy: TextView
    private lateinit var tvMemoryTotal: TextView
    private lateinit var tvMemoryBest: TextView

    private lateinit var tvLogicReaction: TextView
    private lateinit var tvLogicAccuracy: TextView
    private lateinit var tvLogicTotal: TextView
    private lateinit var tvLogicBest: TextView

    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_statistics)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Powiązania z widokiem (przykład dla 4 kategorii)
        tvCoordinationReaction = findViewById(R.id.tvCoordinationReaction)
        tvCoordinationAccuracy = findViewById(R.id.tvCoordinationAccuracy)
        tvCoordinationTotal = findViewById(R.id.tvCoordinationTotal)
        tvCoordinationBest = findViewById(R.id.tvCoordinationBest)

        tvFocusReaction = findViewById(R.id.tvFocusReaction)
        tvFocusAccuracy = findViewById(R.id.tvFocusAccuracy)
        tvFocusTotal = findViewById(R.id.tvFocusTotal)
        tvFocusBest = findViewById(R.id.tvFocusBest)

        tvMemoryReaction = findViewById(R.id.tvMemoryReaction)
        tvMemoryAccuracy = findViewById(R.id.tvMemoryAccuracy)
        tvMemoryTotal = findViewById(R.id.tvMemoryTotal)
        tvMemoryBest = findViewById(R.id.tvMemoryBest)

        tvLogicReaction = findViewById(R.id.tvLogicReaction)
        tvLogicAccuracy = findViewById(R.id.tvLogicAccuracy)
        tvLogicTotal = findViewById(R.id.tvLogicTotal)
        tvLogicBest = findViewById(R.id.tvLogicBest)

        bottomNav = findViewById(R.id.bottomNavigationView)

        // Dolne menu
        setupBottomMenu()

        val user = auth.currentUser
        if (user != null) {
            loadUserStats(user.uid)
        } else {
            displayEmptyStats()
        }
    }

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

    private fun loadUserStats(uid: String) {
        val docRef = db.collection("userStats").document(uid)
        docRef.get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    // Dane z Firebase (jeśli są)
                    val data = document.data ?: return@addOnSuccessListener

                    setStatsForCategory(
                        "Koordynacja",
                        tvCoordinationReaction, tvCoordinationAccuracy,
                        tvCoordinationTotal, tvCoordinationBest,
                        data["coordinationReaction"]?.toString(),
                        data["coordinationAccuracy"]?.toString(),
                        data["coordinationTotal"]?.toString(),
                        data["coordinationBest"]?.toString()
                    )

                    setStatsForCategory(
                        "Skupienie",
                        tvFocusReaction, tvFocusAccuracy,
                        tvFocusTotal, tvFocusBest,
                        data["focusReaction"]?.toString(),
                        data["focusAccuracy"]?.toString(),
                        data["focusTotal"]?.toString(),
                        data["focusBest"]?.toString()
                    )

                    setStatsForCategory(
                        "Pamięć",
                        tvMemoryReaction, tvMemoryAccuracy,
                        tvMemoryTotal, tvMemoryBest,
                        data["memoryReaction"]?.toString(),
                        data["memoryAccuracy"]?.toString(),
                        data["memoryTotal"]?.toString(),
                        data["memoryBest"]?.toString()
                    )

                    setStatsForCategory(
                        "Rozwiązywanie problemów",
                        tvLogicReaction, tvLogicAccuracy,
                        tvLogicTotal, tvLogicBest,
                        data["logicReaction"]?.toString(),
                        data["logicAccuracy"]?.toString(),
                        data["logicTotal"]?.toString(),
                        data["logicBest"]?.toString()
                    )
                } else {
                    displayMockData()
                }
            }
            .addOnFailureListener {
                displayMockData()
            }
    }

    private fun setStatsForCategory(
        category: String,
        reaction: TextView, accuracy: TextView, total: TextView, best: TextView,
        reactionValue: String?, accuracyValue: String?, totalValue: String?, bestValue: String?
    ) {
        reaction.text = "Średni czas reakcji: ${reactionValue ?: "N/A"}"
        accuracy.text = "Poprawność odpowiedzi: ${accuracyValue ?: "N/A"}%"
        total.text = "Łączna liczba punktów: ${totalValue ?: "0"}"
        best.text = "Najlepszy wynik: ${bestValue ?: "0"}"
    }

    private fun displayEmptyStats() {
        val fields = listOf(
            tvCoordinationReaction, tvCoordinationAccuracy, tvCoordinationTotal, tvCoordinationBest,
            tvFocusReaction, tvFocusAccuracy, tvFocusTotal, tvFocusBest,
            tvMemoryReaction, tvMemoryAccuracy, tvMemoryTotal, tvMemoryBest,
            tvLogicReaction, tvLogicAccuracy, tvLogicTotal, tvLogicBest
        )
        fields.forEach { it.text = "Brak danych" }
    }

    private fun displayMockData() {
        // Dane przykładowe (możesz później pobrać z bazy)
        tvCoordinationReaction.text = "Średni czas reakcji: 1.2s"
        tvCoordinationAccuracy.text = "Poprawność: 88%"
        tvCoordinationTotal.text = "Łączna liczba punktów: 1350"
        tvCoordinationBest.text = "Najlepszy wynik: 260"

        tvFocusReaction.text = "Średni czas reakcji: 1.1s"
        tvFocusAccuracy.text = "Poprawność: 85%"
        tvFocusTotal.text = "Łączna liczba punktów: 1200"
        tvFocusBest.text = "Najlepszy wynik: 240"

        tvMemoryReaction.text = "Średni czas reakcji: 1.3s"
        tvMemoryAccuracy.text = "Poprawność: 90%"
        tvMemoryTotal.text = "Łączna liczba punktów: 1420"
        tvMemoryBest.text = "Najlepszy wynik: 300"

        tvLogicReaction.text = "Średni czas reakcji: 1.0s"
        tvLogicAccuracy.text = "Poprawność: 83%"
        tvLogicTotal.text = "Łączna liczba punktów: 1300"
        tvLogicBest.text = "Najlepszy wynik: 250"
    }
}
