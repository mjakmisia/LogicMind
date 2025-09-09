package com.example.logicmind.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.logicmind.R
import com.example.logicmind.common.InstructionDialogFragment

class GameIntroActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_intro)
        supportActionBar?.hide()

        val gameId = intent.getStringExtra("GAME_ID") ?: return
        val config = GameIntroProvider.getConfig(gameId)

        //Ikona i tytuł
        findViewById<ImageView>(R.id.gameIcon).setImageResource(config.imageRes)
        findViewById<TextView>(R.id.gameTitle).text = getString(config.titleRes)

        //Dynamiczne kafelki
        val container = findViewById<LinearLayout>(R.id.sectionsContainer)
        container.removeAllViews()
        config.sections.forEach { section ->
            val view = layoutInflater.inflate(R.layout.item_intro_section, container, false)
            view.findViewById<TextView>(R.id.sectionTitle).text = getString(section.titleRes)
            view.findViewById<TextView>(R.id.sectionDescription).text = getString(section.descriptionRes)
            container.addView(view)
        }

        //Przycisk start (kolor kategorii)
        val btnStart = findViewById<Button>(R.id.btnStartGame)
        btnStart.setBackgroundColor(ContextCompat.getColor(this, config.colorRes))
        btnStart.setOnClickListener {
            startActivity(Intent(this, config.gameActivity))
        }

        //Zamknięcie (powrót do MainActivity)
        findViewById<ImageButton>(R.id.btnClose).setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        // Pomoc
        findViewById<ImageButton>(R.id.btnHelp).setOnClickListener {
            val config = GameIntroProvider.getConfig(gameId)
            if (config.instructionRes != 0) {
                InstructionDialogFragment
                    .newInstance(getString(config.titleRes), getString(config.instructionRes))
                    .show(supportFragmentManager, "InstructionDialog")
            }
        }
    }
}