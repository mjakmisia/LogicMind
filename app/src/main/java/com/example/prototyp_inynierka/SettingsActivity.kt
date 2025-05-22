package com.example.prototyp_inynierka

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        supportActionBar?.hide()

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNav.selectedItemId = R.id.nav_settings

        val langPl = findViewById<LinearLayout>(R.id.langPl)
        val langEn = findViewById<LinearLayout>(R.id.langEn)

        val textLangPl = langPl.findViewById<TextView>(R.id.textLangPl)
        val textLangEn = langEn.findViewById<TextView>(R.id.textLangEn)

        langPl.setOnClickListener {
            langPl.setBackgroundResource(R.drawable.lang_button_selected)
            textLangPl.setTextColor(Color.BLACK)

            langEn.setBackgroundResource(R.drawable.lang_button_unselected)
            textLangEn.setTextColor(Color.GRAY)

            // TODO: Zmień język na PL
        }

        langEn.setOnClickListener {
            langEn.setBackgroundResource(R.drawable.lang_button_selected)
            textLangEn.setTextColor(Color.BLACK)

            langPl.setBackgroundResource(R.drawable.lang_button_unselected)
            textLangPl.setTextColor(Color.GRAY)

            // TODO: Zmień język na EN
        }


        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    true
                }
                R.id.nav_statistics -> {
                    // startActivity(Intent(this, StatisticsActivity::class.java))
                    true
                }
                R.id.nav_profile -> {
                    // startActivity(Intent(this, ProfileActivity::class.java))
                    true
                }
                R.id.nav_settings -> true
                else -> false
            }
        }
    }
}
