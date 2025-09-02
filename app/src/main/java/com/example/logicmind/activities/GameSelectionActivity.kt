package com.example.logicmind.activities

import android.content.Intent
import android.os.Bundle
import android.view.SoundEffectConstants
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.logicmind.R

//TODO: Opcja wyjścia z aktywności
class GameSelectionActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_selection)
        supportActionBar?.hide()

        val categoryId = intent.getStringExtra("CATEGORY_ID") ?: "default"
        val config = GameConfigProvider.getConfig(categoryId)

        // Pasek z pytaniem
        val questionBar = findViewById<TextView>(R.id.tvQuestionChooseGame)
        questionBar.setBackgroundColor(ContextCompat.getColor(this, config.colorRes))
        questionBar.text = getString(R.string.choose_game_request, getString(config.nameRes))

        // Gra 1
        setupGame(
            containerId = R.id.game1Container,
            iconId = R.id.game1Icon,
            titleId = R.id.game1Title,
            gameOption = config.games.getOrNull(0)
        )

        // Gra 2
        setupGame(
            containerId = R.id.game2Container,
            iconId = R.id.game2Icon,
            titleId = R.id.game2Title,
            gameOption = config.games.getOrNull(1)
        )
    }

    private fun setupGame(containerId: Int, iconId: Int, titleId: Int, gameOption: GameOption?) {
        if (gameOption == null) return

        val container = findViewById<LinearLayout>(containerId)
        val icon = findViewById<ImageView>(iconId)
        val title = findViewById<TextView>(titleId)

        icon.setImageResource(gameOption.iconRes)
        title.text = getString(gameOption.title)

        container.setOnClickListener { view ->
            view.playSoundEffect(SoundEffectConstants.CLICK)
            gameOption.introActivity?.let {
                val intent = Intent(this, it)
                intent.putExtra("GAME_ID", gameOption.gameId)
                startActivity(intent)
            }
        }
    }
}
