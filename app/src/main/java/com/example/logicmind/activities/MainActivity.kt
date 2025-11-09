package com.example.logicmind.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.example.logicmind.R
import com.example.logicmind.databinding.ActivityMainBinding

class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()

        //wyświetlanie streak pod ogniem
        loadUserStreak()

        // Kategorie gier

        binding.btnKoordynacja.setOnClickListener {
            it.playSoundEffect(android.view.SoundEffectConstants.CLICK)
            val intent = Intent(this, GameSelectionActivity::class.java)
            //putExtra - przekazywanie danych miedzy aktywnosciami
            intent.putExtra("CATEGORY_ID", "coordination")
            startActivity(intent)
        }

        binding.btnRozwiazywanieProblemow.setOnClickListener {
            it.playSoundEffect(android.view.SoundEffectConstants.CLICK)
            val intent = Intent(this, GameSelectionActivity::class.java)
            intent.putExtra("CATEGORY_ID", "reasoning")
            startActivity(intent)
        }

        binding.btnSkupienie.setOnClickListener {
            it.playSoundEffect(android.view.SoundEffectConstants.CLICK)
            val intent = Intent(this, GameSelectionActivity::class.java)
            intent.putExtra("CATEGORY_ID", "attention")
            startActivity(intent)
        }

        binding.btnPamiec.setOnClickListener {
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

        if (user == null) {
            binding.streakText.text = "0 dni"
            return
        }

        // czy jest gościem
        if (!isUserLoggedIn()) {
            binding.streakText.text = "0 dni"
            return
        }

        // pobranie z bazy
        //odniesienie do konkretnego usera w bazie
        val userRef = db.getReference("users").child(user.uid)

        //get - pobiera jednorazowo dane z bazy
        userRef.child("streak").get()
            .addOnSuccessListener { snapshot ->
                //próbujemy rzutować na Longa jeśli nie zadziała to użyj 0 i konwertujemy na int
                val streak = (snapshot.value as? Long ?: 0L).toInt()
                binding.streakText.text = "$streak dni"
            }
            .addOnFailureListener {
                binding.streakText.text = "błąd"
                Log.e("STREAK_DEBUG", "Błąd pobierania streaka", it)
            }
    }
}
