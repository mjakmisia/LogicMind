package com.example.logicmind.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isGone
import com.example.logicmind.R
import com.example.logicmind.databinding.ActivityStatisticsBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StatisticsActivity : BaseActivity() {
    private lateinit var binding: ActivityStatisticsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStatisticsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()

        setupBottomNavigation(binding.includeBottomNav.bottomNavigationView, R.id.nav_statistics)

        binding.statisticsScrollView.visibility = View.GONE
        binding.layoutNotLoggedIn.visibility = View.GONE
        binding.progressBar.visibility = View.VISIBLE

        if (!isUserLoggedIn()) {
            binding.progressBar.visibility = View.GONE
            binding.statisticsScrollView.visibility = View.GONE
            binding.layoutNotLoggedIn.visibility = View.VISIBLE
            binding.buttonLogin.visibility = View.VISIBLE
            binding.buttonLogin.setOnClickListener {
                startActivity(Intent(this, WelcomeActivity::class.java))
                finish()
            }
        } else {
            binding.progressBar.visibility = View.GONE
            binding.statisticsScrollView.visibility = View.VISIBLE
            binding.layoutNotLoggedIn.visibility = View.GONE
            setupExpandableStats()
            auth.currentUser?.let {
                loadUserStats(it.uid)
                loadLastPlayedGame(it.uid)
                loadGlobalStats(it.uid)
            }
        }
    }

    private fun setupExpandableStats() {
        val pairs = listOf(
            Pair(R.id.layoutCoordinationGame1, R.id.layoutCoordinationGame1Stats),
            Pair(R.id.layoutCoordinationGame2, R.id.layoutCoordinationGame2Stats),
            Pair(R.id.layoutAttentionGame1, R.id.layoutAttentionGame1Stats),
            Pair(R.id.layoutAttentionGame2, R.id.layoutAttentionGame2Stats),
            Pair(R.id.layoutMemoryGame1, R.id.layoutMemoryGame1Stats),
            Pair(R.id.layoutMemoryGame2, R.id.layoutMemoryGame2Stats),
            Pair(R.id.layoutReasoningGame1, R.id.layoutReasoningGame1Stats),
            Pair(R.id.layoutReasoningGame2, R.id.layoutReasoningGame2Stats)
        )

        pairs.forEach { (layoutId, statsId) ->
            val layout = binding.root.findViewById<LinearLayout>(layoutId)
            val stats = binding.root.findViewById<LinearLayout>(statsId)

            layout.setOnClickListener {
                stats.visibility = if (stats.isGone) View.VISIBLE else View.GONE
            }
        }
    }

    private fun loadUserStats(uid: String) {
        val gameMapping =
            listOf(
                Triple(
                    "Koordynacja", "road_dash", listOf(
                        R.id.tvCoordinationGame1Reaction,
                        R.id.tvCoordinationGame1Accuracy,
                        R.id.tvCoordinationGame1Total,
                        R.id.tvCoordinationGame1Best
                    )
                ),
                Triple(
                    "Koordynacja", "symbol_race", listOf(
                        R.id.tvCoordinationGame2Reaction,
                        R.id.tvCoordinationGame2Accuracy,
                        R.id.tvCoordinationGame2Total,
                        R.id.tvCoordinationGame2Best
                    )
                ),
                Triple(
                    "Skupienie", "word_search", listOf(
                        R.id.tvAttentionGame1Reaction,
                        R.id.tvAttentionGame1Accuracy,
                        R.id.tvAttentionGame1Total,
                        R.id.tvAttentionGame1Best
                    )
                ),
                Triple(
                    "Skupienie", "left_or_right", listOf(
                        R.id.tvAttentionGame2Reaction,
                        R.id.tvAttentionGame2Accuracy,
                        R.id.tvAttentionGame2Total,
                        R.id.tvAttentionGame2Best
                    )
                ),
                Triple(
                    "Pamiec", "color_sequence", listOf(
                        R.id.tvMemoryGame1Reaction,
                        R.id.tvMemoryGame1Accuracy,
                        R.id.tvMemoryGame1Total,
                        R.id.tvMemoryGame1Best
                    )
                ),
                Triple(
                    "Pamiec", "card_match", listOf(
                        R.id.tvMemoryGame2Reaction,
                        R.id.tvMemoryGame2Accuracy,
                        R.id.tvMemoryGame2Total,
                        R.id.tvMemoryGame2Best
                    )
                ),
                Triple(
                    "Rozwiazywanie_problemow", "number_addition", listOf(
                        R.id.tvReasoningGame1Reaction,
                        R.id.tvReasoningGame1Accuracy,
                        R.id.tvReasoningGame1Total,
                        R.id.tvReasoningGame1Best
                    )
                ),
                Triple(
                    "Rozwiazywanie_problemow", "path_change", listOf(
                        R.id.tvReasoningGame2Reaction,
                        R.id.tvReasoningGame2Accuracy,
                        R.id.tvReasoningGame2Total,
                        R.id.tvReasoningGame2Best
                    )
                )
            )

        gameMapping.forEach { (category, gameName, viewIds) ->
            db.getReference("users").child(uid).child("categories").child(category).child("games")
                .child(gameName)
                .get()
                .addOnSuccessListener { snapshot ->
                    val messageIfEmpty = "Zagraj w grę aby zobaczyć statystyki"

                    if (snapshot.exists()) {
                        val data = snapshot.value as? Map<*, *>

                        if (data != null) {
                            val reaction = when (val r = data["avgReactionTime"]) {
                                is Double -> r
                                is Long -> r.toDouble()
                                is Int -> r.toDouble()
                                is String -> r.toDoubleOrNull() ?: -1.0
                                else -> -1.0
                            }
                            val accuracy = when (val acc = data["accuracy"]) {
                                is Double -> acc
                                is Long -> acc.toDouble()
                                is Int -> acc.toDouble()
                                is String -> acc.toDoubleOrNull() ?: -1.0
                                else -> -1.0
                            }
                            val total = data["starsEarned"]?.toString() ?: messageIfEmpty
                            val best = data["bestStars"]?.toString() ?: messageIfEmpty

                            setStatsForGame(
                                viewIds[0], viewIds[1], viewIds[2], viewIds[3],
                                reaction, accuracy, total, best
                            )
                        } else {
                            setStatsForGame(
                                viewIds[0], viewIds[1], viewIds[2], viewIds[3],
                                messageIfEmpty, messageIfEmpty, messageIfEmpty, messageIfEmpty
                            )
                        }
                    } else {
                        setStatsForGame(
                            viewIds[0], viewIds[1], viewIds[2], viewIds[3],
                            messageIfEmpty, messageIfEmpty, messageIfEmpty, messageIfEmpty
                        )
                    }
                }
                .addOnFailureListener {
                    val messageIfError = "Błąd pobierania danych"
                    setStatsForGame(
                        viewIds[0], viewIds[1], viewIds[2], viewIds[3],
                        messageIfError, messageIfError, messageIfError, messageIfError
                    )
                }
        }
    }

    private fun loadLastPlayedGame(uid: String) {
        val gameMapping = listOf(
            Pair("Koordynacja", "road_dash"),
            Pair("Koordynacja", "symbol_race"),
            Pair("Skupienie", "word_search"),
            Pair("Skupienie", "left_or_right"),
            Pair("Pamiec", "color_sequence"),
            Pair("Pamiec", "card_match"),
            Pair("Rozwiazywanie_problemow", "number_addition"),
            Pair("Rozwiazywanie_problemow", "path_change")
        )

        var latestGameKey: String? = null
        var latestTimestamp: Long? = null
        var completedRequests = 0

        gameMapping.forEach { (category, gameName) ->
            db.getReference("users").child(uid).child("categories").child(category).child("games")
                .child(gameName)
                .child("lastPlayed")
                .get()
                .addOnSuccessListener { snapshot ->
                    completedRequests++
                    if (snapshot.exists()) {
                        val timestamp = snapshot.value as? Long

                        if (timestamp != null && (latestTimestamp == null || timestamp > latestTimestamp!!)) {
                            latestTimestamp = timestamp
                            latestGameKey = gameName
                        }
                    }
                    if (completedRequests == gameMapping.size) {
                        updateLastPlayedText(latestGameKey, latestTimestamp)
                    }
                }
                .addOnFailureListener {
                    completedRequests++
                    if (completedRequests == gameMapping.size) {
                        updateLastPlayedText(latestGameKey, latestTimestamp)
                    }
                }
        }
    }

    @SuppressLint("DiscouragedApi")
    private fun updateLastPlayedText(gameKey: String?, timestamp: Long?) {
        if (gameKey == null || timestamp == null) {
            binding.tvLastPlayedGame.text = getString(R.string.last_played_game_none)
            return
        }

        val resID = resources.getIdentifier(gameKey, "string", packageName)
        val displayName = if (resID != 0) {
            getString(resID)
        } else {
            gameKey
        }
        val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        val lastPlayedDate = dateFormat.format(Date(timestamp))
        binding.tvLastPlayedGame.text = getString(R.string.last_played_game, displayName, lastPlayedDate)
    }

    @SuppressLint("DefaultLocale")
    private fun setStatsForGame(
        reactionId: Int, accuracyId: Int, totalId: Int, bestId: Int,
        reactionValue: Any?, accuracyValue: Any?, totalValue: Any?, bestValue: Any?
    ) {
        val formattedReaction = if (reactionValue is Double && reactionValue >= 0) {
            String.format("%.2f s", reactionValue / 1000.0)
        } else {
            "0"
        }

        val formattedAccuracy = if (accuracyValue is Double && accuracyValue >= 0) {
            String.format("%.2f", accuracyValue)
        } else {
            "0"
        }

        binding.root.findViewById<TextView>(reactionId).text =
            getString(R.string.avg_reaction_time_value, formattedReaction)
        binding.root.findViewById<TextView>(accuracyId).text =
            getString(R.string.accuracy_value, formattedAccuracy)
        binding.root.findViewById<TextView>(totalId).text =
            getString(R.string.total_points_value, totalValue ?: "0")
        binding.root.findViewById<TextView>(bestId).text =
            getString(R.string.highest_score_value, bestValue ?: "0")
    }

    private fun loadGlobalStats(uid: String) {
        db.getReference("users").child(uid).child("statistics")
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val avgReactionMs =
                        snapshot.child("avgReactionTime").getValue(Double::class.java) ?: 0.0
                    val avgAccuracyVal =
                        snapshot.child("avgAccuracy").getValue(Double::class.java) ?: 0.0

                    val formattedReaction = if (avgReactionMs > 0) {
                        String.format(Locale.getDefault(), "%.2f s", avgReactionMs / 1000.0)
                    } else {
                        "-"
                    }

                    val formattedAccuracy = if (avgAccuracyVal > 0) {
                        String.format(Locale.getDefault(), "%.1f%", avgAccuracyVal)
                    } else {
                        "-"
                    }

                    binding.tvGlobalReactionTime.text = formattedReaction
                    binding.tvGlobalAccuracy.text = formattedAccuracy

                } else {
                    binding.tvGlobalReactionTime.text = "-"
                    binding.tvGlobalAccuracy.text = "-"
                }
            }
            .addOnFailureListener {
                Log.e("STATS_DEBUG", "Błąd pobierania globalnych statystyk", it)
                binding.tvGlobalReactionTime.text = "Błąd"
                binding.tvGlobalAccuracy.text = "Błąd"
            }
    }
}
