package com.example.logicmind.activities

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.logicmind.R
import com.example.logicmind.common.GameCountdownManager
import com.example.logicmind.common.GameOverDialogFragment
import com.example.logicmind.common.GameStatsManager
import com.example.logicmind.common.GameTimerProgressBar
import com.example.logicmind.common.StarManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import java.util.Calendar
import java.util.Locale
import kotlin.math.max

open class BaseActivity : AppCompatActivity() {

    protected lateinit var auth: FirebaseAuth
    protected lateinit var db: FirebaseDatabase
    protected val gameStatsManager = GameStatsManager()

    override fun attachBaseContext(newBase: Context) {
        val sharedPrefs = newBase.getSharedPreferences("Settings", MODE_PRIVATE)
        val lang = sharedPrefs.getString("My_Lang", "pl") ?: "pl"

        val locale = Locale.forLanguageTag(lang)
        Locale.setDefault(locale)

        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)

        val context = newBase.createConfigurationContext(config)
        super.attachBaseContext(context)
    }

    companion object {
        private var isPersistenceEnabled = false
        private const val KEY_STATS_START_TIME = "STATS_START_TIME"
        private const val KEY_STATS_IS_PAUSED = "STATS_IS_PAUSED"
        private const val KEY_STATS_PAUSE_TIME = "STATS_PAUSE_TIME"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPrefs = getSharedPreferences("Settings", MODE_PRIVATE)
        val isDarkMode = sharedPrefs.getBoolean("DarkMode_Enabled", false)
        AppCompatDelegate.setDefaultNightMode(
            if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.attributes.layoutInDisplayCutoutMode =
            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES

        val insetsController = WindowInsetsControllerCompat(window, window.decorView)
        insetsController.hide(
            WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars()
        )
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        auth = FirebaseAuth.getInstance()
        db = FirebaseDatabase.getInstance("https://logicmind-default-rtdb.europe-west1.firebasedatabase.app")
        if (!isPersistenceEnabled) {
            try {
                db.setPersistenceEnabled(true)
                isPersistenceEnabled = true
            } catch (e: Exception) {
                android.util.Log.w("FIREBASE_INIT", "Błąd włączenia persystencji Firebase: ${e.message}")
            }
        }
        db.getReference("users").keepSynced(true)
    }

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

    protected fun isUserLoggedIn(): Boolean {
        val user = auth.currentUser
        return !(user == null || user.isAnonymous)
    }

    protected fun lastPlayedGame(
        categoryKey: String, gameKey: String
    ) {
        if (!isUserLoggedIn()) return
        val user = auth.currentUser!!

        val dbRef = db.getReference("users").child(user.uid).child("categories").child(categoryKey)
            .child("games").child(gameKey)

        val timestamp = System.currentTimeMillis()

        dbRef.child("lastPlayed").setValue(timestamp).addOnSuccessListener {
            updateStreak()
        }.addOnFailureListener { e ->
            android.util.Log.e("GAME_DEBUG", "Błąd aktualizacji lastPlayed dla $gameKey", e)
        }
    }

    private fun updateStreak() {
        if (!isUserLoggedIn()) return

        val uid = auth.currentUser!!.uid
        val userRef = db.getReference("users").child(uid)

        userRef.get().addOnSuccessListener { snapshot ->
            val currentStreak = (snapshot.child("streak").value as? Long)?.toInt() ?: 0
            val bestStreak = (snapshot.child("bestStreak").value as? Long)?.toInt() ?: 0
            val lastPlayTimestamp = snapshot.child("lastPlayDate").getValue(Long::class.java)

            val today = Calendar.getInstance()
            stripTime(today)

            val newStreak = if (lastPlayTimestamp == null) {
                1
            } else {
                val lastPlayDay = Calendar.getInstance().apply { timeInMillis = lastPlayTimestamp }
                stripTime(lastPlayDay)

                val diffMillis = today.timeInMillis - lastPlayDay.timeInMillis
                val daysBetween = (diffMillis / (1000 * 60 * 60 * 24)).toInt()

                when (daysBetween) {
                    0 -> currentStreak
                    1 -> currentStreak + 1
                    else -> 1
                }
            }

            if (newStreak != currentStreak || lastPlayTimestamp == null) {
                val updates = hashMapOf<String, Any>(
                    "streak" to newStreak,
                    "lastPlayDate" to System.currentTimeMillis()
                )

                if (newStreak > bestStreak) {
                    updates["bestStreak"] = newStreak
                }

                userRef.updateChildren(updates)
                    .addOnFailureListener { e ->
                        android.util.Log.e("STREAK_DEBUG", "Błąd zapisu streaka", e)
                    }
            }

        }.addOnFailureListener { e ->
            android.util.Log.e("STREAK_DEBUG", "Błąd pobierania danych użytkownika", e)
        }
    }

    private fun stripTime(calendar: Calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
    }

    protected fun calculateDisplayStreak(snapshot: DataSnapshot): Int {
        val savedStreak = (snapshot.child("streak").value as? Long)?.toInt() ?: 0
        val lastPlayTimestamp =
            snapshot.child("lastPlayDate").getValue(Long::class.java) ?: return 0

        val today = Calendar.getInstance()
        stripTime(today)

        val lastPlayDay = Calendar.getInstance().apply { timeInMillis = lastPlayTimestamp }
        stripTime(lastPlayDay)

        val diffMillis = today.timeInMillis - lastPlayDay.timeInMillis
        val daysBetween = (diffMillis / (1000 * 60 * 60 * 24)).toInt()

        return if (daysBetween <= 1) {
            savedStreak
        } else {
            0
        }
    }

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

        val statsRef = userRef.child("statistics")
        statsRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val stats = currentData.value as? Map<*, *> ?: emptyMap<String, Any>()
                val currentStars = (stats["totalStars"] as? Long)?.toInt() ?: 0
                val currentGamesPlayed = (stats["gamesPlayed"] as? Long)?.toInt() ?: 0
                val currentSumAccuracy = (stats["sumAccuracy"] as? Double) ?: 0.0
                val currentSumReaction = (stats["sumReactionTime"] as? Double) ?: 0.0
                val newGamesPlayed = currentGamesPlayed + 1
                val newSumAccuracy = currentSumAccuracy + accuracy
                val newSumReaction = currentSumReaction + reactionTime
                val updatedStats = mapOf<String, Any>(
                    "totalStars" to (currentStars + starsEarned),
                    "gamesPlayed" to currentGamesPlayed + 1,
                    "sumAccuracy" to currentSumAccuracy + accuracy,
                    "sumReactionTime" to currentSumReaction + reactionTime,
                    "avgAccuracy" to
                            if (newGamesPlayed > 0)
                                newSumAccuracy / newGamesPlayed else 0.0,
                    "avgReactionTime" to
                            if (newGamesPlayed > 0)
                                newSumReaction / newGamesPlayed else 0.0
                )
                currentData.value = updatedStats
                return Transaction.success(currentData)
            }
            override fun onComplete(
                error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?
            ) {
                if (error != null) {
                    android.util.Log.e("STATS_DEBUG", "Błąd aktualizacji globalnych statystyk: ${error.message}")
                }
            }
        })

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

            val updatedGameData = mapOf<String, Any>(
                "starsEarned" to (currentStars + starsEarned),
                "bestStars" to max(currentBestStars, starsEarned),
                "sumAccuracy" to newSumAccuracy,
                "sumReactionTime" to newSumReaction,
                "accuracy" to newAvgAccuracy,
                "avgReactionTime" to newAvgReaction,
                "gamesPlayed" to newGamesPlayed,
                "lastPlayed" to System.currentTimeMillis()
            )

            gameRef.updateChildren(updatedGameData).addOnFailureListener {
                android.util.Log.e("STATS_DEBUG", "Błąd zapisu danych statystyk dla gry $gameKey: ${it.message}")
            }
        }.addOnFailureListener {
            android.util.Log.e("STATS_DEBUG", "Błąd pobierania danych gry $gameKey: ${it.message}")
        }
    }

    protected fun onGamePaused() = gameStatsManager.onGamePaused()
    protected fun onGameResumed() = gameStatsManager.onGameResumed()

    protected fun getAverageReactionTime(stars: Int = 0): Double {
        val avgReactionSec = gameStatsManager.calculateAvgReactionTime(stars)

        if (isUserLoggedIn()) {
            val userId = auth.currentUser!!.uid
            val statsRef =
                db.getReference("users").child(userId).child("statistics").child("avgReactionTime")

            statsRef.get().addOnSuccessListener {}
        }

        return avgReactionSec
    }

    protected fun showGameOverDialog(
        categoryKey: String,
        gameKey: String,
        starManager: StarManager,
        timerProgressBar: GameTimerProgressBar,
        countdownManager: GameCountdownManager,
        currentBestScore: Int,
        onRestartAction: () -> Unit
    ) {
        timerProgressBar.stop()
        gameStatsManager.onGamePaused()

        val currentScore = starManager.starCount
        val durationSec = gameStatsManager.getPlayedTimeSec()

        val timeDisplay = if (durationSec >= 60) {
            val mins = (durationSec / 60).toInt()
            val secs = (durationSec % 60).toInt()
            "${mins}m ${secs}s"
        } else {
            "${durationSec.toInt()}s"
        }

        val displayBestScore = max(currentBestScore, currentScore)

        updateUserStatistics(
            categoryKey = categoryKey,
            gameKey = gameKey,
            starsEarned = currentScore,
            accuracy = gameStatsManager.calculateAccuracy(),
            reactionTime = getAverageReactionTime(stars = currentScore)
        )

        lastPlayedGame(categoryKey, gameKey)

        val dialog = GameOverDialogFragment.newInstance(
            score = currentScore,
            timeFormatted = timeDisplay,
            bestScore = displayBestScore
        )

        dialog.onRestartListener = {
            starManager.reset()
            gameStatsManager.startReactionTracking()
            gameStatsManager.setGameStartTime()
            timerProgressBar.reset()

            onRestartAction()

            countdownManager.startCountdown()
        }


        dialog.onExitListener = {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)

            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }

        dialog.show(supportFragmentManager, "GameOverDialog")
    }

    protected fun saveGameStats(outState: Bundle) {
        outState.putLong(KEY_STATS_START_TIME, gameStatsManager.getStartTime())

        val (isPaused, pauseTime) = gameStatsManager.getPauseData()
        outState.putBoolean(KEY_STATS_IS_PAUSED, isPaused)
        outState.putLong(KEY_STATS_PAUSE_TIME, pauseTime)
    }

    protected fun restoreGameStats(savedInstanceState: Bundle) {
        val savedStartTime = savedInstanceState.getLong(KEY_STATS_START_TIME, 0L)
        if (savedStartTime != 0L) {
            gameStatsManager.restoreStartTime(savedStartTime)
        }

        val wasPaused = savedInstanceState.getBoolean(KEY_STATS_IS_PAUSED, false)
        val savedPauseTime = savedInstanceState.getLong(KEY_STATS_PAUSE_TIME, 0L)
        gameStatsManager.restorePauseData(wasPaused, savedPauseTime)
    }

    protected fun isNetworkAvailable(): Boolean {
        val connectivityManager =
            getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager

        val network = connectivityManager.activeNetwork ?: return false

        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false

        return activeNetwork.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}