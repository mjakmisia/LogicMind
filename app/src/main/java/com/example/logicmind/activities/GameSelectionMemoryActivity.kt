package com.example.logicmind.activities

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.example.logicmind.R

class GameSelectionMemoryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_selection_memory)

        supportActionBar?.hide()

        val game1Container = findViewById<LinearLayout>(R.id.gameColorSequenceContainer)
        val game2Container = findViewById<LinearLayout>(R.id.gameMatchingPairsContainer)

        game1Container.setOnClickListener {
            it.playSoundEffect(android.view.SoundEffectConstants.CLICK)
            // TODO: dodaj odpowiednią aktywność dla gry 1
        }

        game2Container.setOnClickListener {
            it.playSoundEffect(android.view.SoundEffectConstants.CLICK)
            intent = Intent(this, IntroMatchingPairsActivity::class.java)
            startActivity(intent)
        }
    }

}