package com.example.logicmind.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatDelegate
import com.example.logicmind.R
import com.example.logicmind.databinding.ActivityMainBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.util.Locale

class MainActivity : BaseActivity() {

    private lateinit var binding : ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()

        // Kategorie gier

        binding.btnKoordynacja.setOnClickListener {
            it.playSoundEffect(android.view.SoundEffectConstants.CLICK)
            val intent = Intent(this, GameSelectionActivity::class.java)
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


//    }
}
