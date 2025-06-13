package com.example.prototyp_inynierka.activities

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.example.prototyp_inynierka.R

class GameSelectionAttentionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_selection_attention)

        supportActionBar?.hide()

        val game1Container = findViewById<LinearLayout>(R.id.gameWordSearchContainer)
        val game2Container = findViewById<LinearLayout>(R.id.gameFruitSortingContainer)

        game1Container.setOnClickListener {
            it.playSoundEffect(android.view.SoundEffectConstants.CLICK)
            val intent = Intent(this, IntroWordSearchActivity::class.java)
            startActivity(intent)
        }

        game2Container.setOnClickListener {
            it.playSoundEffect(android.view.SoundEffectConstants.CLICK)
            // TODO: dodaj odpowiednią aktywność dla gry 2
        }
    }
}