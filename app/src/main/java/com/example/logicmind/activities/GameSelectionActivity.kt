package com.example.logicmind.activities

import android.content.Intent
import android.os.Bundle
import android.view.SoundEffectConstants
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.logicmind.R
import com.example.logicmind.additional.GameConfigProvider
import com.example.logicmind.additional.GameOption

class GameSelectionActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_selection)
        supportActionBar?.hide()

        val categoryId = intent.getStringExtra("CATEGORY_ID") ?: "default"
        val config = GameConfigProvider.getConfig(categoryId)

        val questionBar = findViewById<TextView>(R.id.tvQuestionChooseGame)
        questionBar.setBackgroundColor(ContextCompat.getColor(this, config.colorRes))
        questionBar.text = getString(R.string.choose_game_request, getString(config.nameRes))

        setupGame(
            containerId = R.id.game1Container,
            iconId = R.id.game1Icon,
            titleId = R.id.game1Title,
            gameOption = config.games.getOrNull(0)
        )

        setupGame(
            containerId = R.id.game2Container,
            iconId = R.id.game2Icon,
            titleId = R.id.game2Title,
            gameOption = config.games.getOrNull(1)
        )
    }

    private fun setupGame(containerId: Int, iconId: Int, titleId: Int, gameOption: GameOption?) {
        gameOption?.let { option ->
            val container = findViewById<LinearLayout>(containerId)
            val icon = findViewById<ImageView>(iconId)
            val title = findViewById<TextView>(titleId)

            icon.setImageResource(option.iconRes)
            title.text = getString(option.title)

            icon.contentDescription = getString(option.title)

            container.setOnClickListener { view ->
                view.playSoundEffect(SoundEffectConstants.CLICK)

                val intent = Intent(this, GameIntroActivity::class.java)
                intent.putExtra("GAME_ID", option.gameId)
                startActivity(intent)
            }
        }
    }
}