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
    private val PERMISSION_REQUEST_CODE = 1001 // stały kod żądania

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()

        //żądanie uprawnień powiadomień
        requestNotificationPermission()

        //wyświetlanie streak pod ogniem
        loadUserStreak()

        // Kategorie gier

        binding.cardKoordynacja.setOnClickListener {
            it.playSoundEffect(android.view.SoundEffectConstants.CLICK)
            val intent = Intent(this, GameSelectionActivity::class.java)
            //putExtra - przekazywanie danych miedzy aktywnosciami
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

        // Obsługa bottom navigation
        setupBottomNavigation(binding.includeBottomNav.bottomNavigationView, R.id.nav_home)
    }

    private fun loadUserStreak() {
        val user = auth.currentUser

        //sprawdzamy czy jest gościem
        if (user == null || !isUserLoggedIn()) {
            binding.streakText.text = getString(R.string.zero_days)
            return
        }

        // pobranie z bazy
        //odniesienie do konkretnego usera w bazie
        val userRef = db.getReference("users").child(user.uid)

        //get - pobiera jednorazowo dane z bazy
        userRef.get() //pobranie całego węzła żeby mieć datę
            .addOnSuccessListener { snapshot ->
                val streak = calculateDisplayStreak(snapshot)
                binding.streakText.text = getString(R.string.current_streak_text, streak)
            }
            .addOnFailureListener {
                binding.streakText.text = "błąd"
                Log.e("STREAK_DEBUG", "Błąd pobierania streaka", it)
            }
    }

    // Metoda do obsługi żądania uprawnienia
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Jeśli uprawnienie nie jest przyznane, wyświetl okno systemowe
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    PERMISSION_REQUEST_CODE
                )
            }
        }
    }

    // Obsługa wyniku żądania uprawnienia
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Powiadomienia są włączone
                Log.d("NOTIF_PERM", "Uprawnienie do powiadomień przyznane.")
            } else {
                // Użytkownik odrzucił uprawnienie
                Log.d("NOTIF_PERM", "Uprawnienie do powiadomień odrzucone.")
            }
        }
    }

}
