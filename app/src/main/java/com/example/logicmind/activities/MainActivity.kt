package com.example.logicmind.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.logicmind.R
import com.example.logicmind.databinding.ActivityMainBinding

class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding
    companion object {
        private const val PERMISSION_REQUEST_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()

        requestNotificationPermission()

        binding.cardKoordynacja.setOnClickListener {
            it.playSoundEffect(android.view.SoundEffectConstants.CLICK)
            val intent = Intent(this, GameSelectionActivity::class.java)
            intent.putExtra("CATEGORY_ID", "coordination")
            startActivity(intent)
        }

        binding.cardRozwiazywanieProblemow.setOnClickListener {
            it.playSoundEffect(android.view.SoundEffectConstants.CLICK)
            val intent = Intent(this, GameSelectionActivity::class.java)
            intent.putExtra("CATEGORY_ID", "reasoning")
            startActivity(intent)
        }

        binding.cardSkupienie.setOnClickListener {
            it.playSoundEffect(android.view.SoundEffectConstants.CLICK)
            val intent = Intent(this, GameSelectionActivity::class.java)
            intent.putExtra("CATEGORY_ID", "attention")
            startActivity(intent)
        }

        binding.cardPamiec.setOnClickListener {
            it.playSoundEffect(android.view.SoundEffectConstants.CLICK)
            val intent = Intent(this, GameSelectionActivity::class.java)
            intent.putExtra("CATEGORY_ID", "memory")
            startActivity(intent)
        }

        setupBottomNavigation(binding.includeBottomNav.bottomNavigationView, R.id.nav_home)
    }

    override fun onResume() {
        super.onResume()
        loadUserStreak()
    }

    private fun loadUserStreak() {
        val user = auth.currentUser

        if (user == null || !isUserLoggedIn()) {
            binding.streakText.text = getString(R.string.zero_days)
            return
        }

        val userRef = db.getReference("users").child(user.uid)

        userRef.get()
            .addOnSuccessListener { snapshot ->
                val streak = calculateDisplayStreak(snapshot)
                binding.streakText.text = getString(R.string.current_streak_text, streak)
            }
            .addOnFailureListener {
                binding.streakText.text = "błąd"
                Log.e("STREAK_DEBUG", "Błąd pobierania streaka", it)
            }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    PERMISSION_REQUEST_CODE
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}