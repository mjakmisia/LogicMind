package com.example.logicmind.activities

import android.content.Intent
import android.os.Bundle
import android.view.SoundEffectConstants
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.example.logicmind.R

class GameSelectionProblemSolvingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_selection_problem_solving)

        supportActionBar?.hide()

        val game1Container = findViewById<LinearLayout>(R.id.gameNumberAdditionContainer)
        val game2Container = findViewById<LinearLayout>(R.id.gameOtherProblemGameContainer)

        game1Container.setOnClickListener {
            it.playSoundEffect(SoundEffectConstants.CLICK)
            val intent = Intent(this, IntroNumberAdditionActivity::class.java)
            startActivity(intent)
        }

        game2Container.setOnClickListener {
            it.playSoundEffect(SoundEffectConstants.CLICK)
            // TODO: dodaj odpowiednią aktywność dla gry 2 w tej kategorii
        }
    }
}