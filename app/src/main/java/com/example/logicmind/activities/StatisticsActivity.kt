package com.example.logicmind.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.example.logicmind.R
import com.example.logicmind.databinding.ActivityStatisticsBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.core.view.isVisible

class StatisticsActivity : BaseActivity() {
    private lateinit var binding: ActivityStatisticsBinding

    private data class GameConfig(
        val category: String,
        val gameNameKey: String,
        val gameTitleResId: Int,
        val containerId: Int
    )

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
                calculateGlobalStatsFromGames(it.uid)
            }
        }
    }

    private fun setupExpandableStats() {
        val pairs = listOf(
            Pair(R.id.layoutCoordinationHeader, R.id.contentCoordination),
            Pair(R.id.layoutAttentionHeader, R.id.contentAttention),
            Pair(R.id.layoutMemoryHeader, R.id.contentMemory),
            Pair(R.id.layoutReasoningHeader, R.id.contentReasoning)
        )

        pairs.forEach { (headerId, contentId) ->
            val header = binding.root.findViewById<LinearLayout>(headerId)
            val content = binding.root.findViewById<LinearLayout>(contentId)

            var arrowIcon: ImageView? = null
            for (i in 0 until header.childCount) {
                val child = header.getChildAt(i)
                if (child is ImageView) {
                    arrowIcon = child
                    break
                }
            }

            header.setOnClickListener {
                val isExpanded = content.isVisible

                content.visibility = if (isExpanded) View.GONE else View.VISIBLE

                val targetRotation = if (isExpanded) 0f else 180f
                arrowIcon?.animate()?.rotation(targetRotation)?.setDuration(300)?.start()
            }
        }
    }

    private fun loadUserStats(uid: String) {
        val games = listOf(
            GameConfig("Koordynacja", "road_dash", R.string.road_dash, R.id.statsCoordination1),
            GameConfig("Koordynacja", "symbol_race", R.string.symbol_race, R.id.statsCoordination2),
            GameConfig("Skupienie", "word_search", R.string.word_search, R.id.statsAttention1),
            GameConfig("Skupienie", "left_or_right", R.string.left_or_right, R.id.statsAttention2),
            GameConfig("Pamiec", "color_sequence", R.string.color_sequence, R.id.statsMemory1),
            GameConfig("Pamiec", "card_match", R.string.card_match, R.id.statsMemory2),
            GameConfig("Rozwiazywanie_problemow", "number_addition", R.string.number_addition, R.id.statsReasoning1),
            GameConfig("Rozwiazywanie_problemow", "path_change", R.string.path_change, R.id.statsReasoning2)
        )

        games.forEach { config ->
            val container = binding.root.findViewById<View>(config.containerId)
            val titleTv = container.findViewById<TextView>(R.id.tvGameTitle)
            titleTv.text = getString(config.gameTitleResId)

            db.getReference("users").child(uid).child("categories")
                .child(config.category).child("games").child(config.gameNameKey)
                .get()
                .addOnSuccessListener { snapshot ->
                    val messageIfEmpty = getString(R.string.stats_empty_message)

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

                            setStatsForGame(config.containerId, reaction, accuracy, total, best)
                        } else {
                            setStatsForGame(config.containerId, messageIfEmpty, messageIfEmpty, messageIfEmpty, messageIfEmpty)
                        }
                    } else {
                        setStatsForGame(config.containerId, messageIfEmpty, messageIfEmpty, messageIfEmpty, messageIfEmpty)
                    }
                }
                .addOnFailureListener {
                    val messageIfError = getString(R.string.data_error_short)
                    setStatsForGame(config.containerId, messageIfError, messageIfError, messageIfError, messageIfError)
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

    private fun updateLastPlayedText(gameKey: String?, timestamp: Long?) {
        if (gameKey == null || timestamp == null) {
            binding.tvLastPlayedGame.text = getString(R.string.last_played_game_none)
            return
        }

        val resID = getGameTitleResId(gameKey)
        val displayName = if (resID != 0) {
            getString(resID)
        } else {
            gameKey
        }
        val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        val lastPlayedDate = dateFormat.format(Date(timestamp))
        binding.tvLastPlayedGame.text = getString(R.string.last_played_game, displayName, lastPlayedDate)
    }

    private fun getGameTitleResId(gameKey: String): Int {
        return when (gameKey) {
            "road_dash" -> R.string.road_dash
            "symbol_race" -> R.string.symbol_race
            "word_search" -> R.string.word_search
            "left_or_right" -> R.string.left_or_right
            "color_sequence" -> R.string.color_sequence
            "card_match" -> R.string.card_match
            "number_addition" -> R.string.number_addition
            "path_change" -> R.string.path_change
            else -> 0
        }
    }

    @SuppressLint("DefaultLocale")
    private fun setStatsForGame(
        containerId: Int,
        reactionValue: Any?, accuracyValue: Any?, totalValue: Any?, bestValue: Any?
    ) {
        val container = binding.root.findViewById<View>(containerId)

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

        container.findViewById<TextView>(R.id.tvReaction).text =
            getString(R.string.avg_reaction_time_value, formattedReaction)
        container.findViewById<TextView>(R.id.tvAccuracy).text =
            getString(R.string.accuracy_value, formattedAccuracy)
        container.findViewById<TextView>(R.id.tvTotal).text =
            getString(R.string.total_points_value, totalValue ?: "0")
        container.findViewById<TextView>(R.id.tvBest).text =
            getString(R.string.highest_score_value, bestValue ?: "0")
    }

    private fun calculateGlobalStatsFromGames(uid: String) {
        db.getReference("users").child(uid).child("categories")
            .get()
            .addOnSuccessListener { snapshot ->
                var totalGamesPlayed = 0
                var totalSumAccuracy = 0.0
                var totalSumReaction = 0.0

                snapshot.children.forEach { categorySnapshot ->
                    val gamesSnapshot = categorySnapshot.child("games")
                    gamesSnapshot.children.forEach { gameSnapshot ->
                        val gamesPlayed = gameSnapshot.child("gamesPlayed").getValue(Long::class.java)?.toInt() ?: 0

                        if (gamesPlayed > 0) {
                            val sumAccuracy = gameSnapshot.child("sumAccuracy").getValue(Double::class.java) ?: 0.0
                            val sumReaction = gameSnapshot.child("sumReactionTime").getValue(Double::class.java) ?: 0.0

                            totalGamesPlayed += gamesPlayed
                            totalSumAccuracy += sumAccuracy
                            totalSumReaction += sumReaction
                        }
                    }
                }

                val avgAccuracy = if (totalGamesPlayed > 0) totalSumAccuracy / totalGamesPlayed else 0.0
                val avgReactionMs = if (totalGamesPlayed > 0) totalSumReaction / totalGamesPlayed else 0.0

                val formattedReaction = if (avgReactionMs > 0) {
                    String.format(Locale.getDefault(), "%.2f s", avgReactionMs / 1000.0)
                } else {
                    "-"
                }

                val formattedAccuracy = if (avgAccuracy > 0) {
                    String.format(Locale.getDefault(), "%.1f%%", avgAccuracy)
                } else {
                    "-"
                }

                binding.tvGlobalReactionTime.text = formattedReaction
                binding.tvGlobalAccuracy.text = formattedAccuracy
            }
            .addOnFailureListener {
                val errorMsg = getString(R.string.error_short)
                binding.tvGlobalReactionTime.text = errorMsg
                binding.tvGlobalAccuracy.text = errorMsg
            }
    }
}