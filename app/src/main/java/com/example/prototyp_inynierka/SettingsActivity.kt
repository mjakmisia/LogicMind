package com.example.prototyp_inynierka

import android.app.AlertDialog
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.util.*

class SettingsActivity : AppCompatActivity() {

    private var selectedLanguage: String? = "pl"  // "pl" lub "en"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val sharedPrefs = getSharedPreferences("Settings", MODE_PRIVATE)
        val currentLang = sharedPrefs.getString("My_Lang", "pl") ?: "pl"
        setLocale(currentLang)
        updateLanguageSelectionUI(currentLang) // PODŚWIETL NA START WYBRANY JĘZYK

        supportActionBar?.hide()

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNav.selectedItemId = R.id.nav_settings

        val langPl = findViewById<LinearLayout>(R.id.langPl)
        val langEn = findViewById<LinearLayout>(R.id.langEn)

        val textLangPl = langPl.findViewById<TextView>(R.id.textLangPl)
        val textLangEn = langEn.findViewById<TextView>(R.id.textLangEn)

        val saveBtn = findViewById<Button>(R.id.saveButton)

        selectedLanguage = currentLang //ustaw wybrany język

        langPl.setOnClickListener {
            selectedLanguage = "pl"
            updateLanguageSelectionUI("pl") // podświetl wybór lokalnie
        }

        langEn.setOnClickListener {
            selectedLanguage = "en"
            updateLanguageSelectionUI("en")
        }


        saveBtn.setOnClickListener {
            if (selectedLanguage != null) {
                val confirmMessage = if (selectedLanguage == "pl") {
                    "Are you sure you want to save changes?"
                } else {
                    "Czy na pewno chcesz zapisać ustawienia?"
                }

                AlertDialog.Builder(this)
                    .setTitle(getString(R.string.changes))
                    .setMessage(confirmMessage)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        val editor = getSharedPreferences("Settings", MODE_PRIVATE).edit()
                        editor.putString("My_Lang", selectedLanguage)
                        editor.apply()

                        setLocale(selectedLanguage!!)
                        recreate()
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            }
        }


        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    true
                }
                R.id.nav_statistics -> true
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    true
                }
                R.id.nav_settings -> true
                else -> false
            }
        }
    }

    private fun setLocale(lang: String) {
        val locale = Locale(lang)
        Locale.setDefault(locale)

        val config = Configuration()
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)

        val editor = getSharedPreferences("Settings", MODE_PRIVATE).edit()
        editor.putString("My_Lang", lang)
        editor.apply()
    }


    private fun applyLanguageChange() {
        if (selectedLanguage != null) {
            setLocale(selectedLanguage!!)
            recreate()
        }
    }


    private fun updateLanguageSelectionUI(currentLang: String) {
        val langPl = findViewById<LinearLayout>(R.id.langPl)
        val langEn = findViewById<LinearLayout>(R.id.langEn)
        val textLangPl = langPl.findViewById<TextView>(R.id.textLangPl)
        val textLangEn = langEn.findViewById<TextView>(R.id.textLangEn)

        if (currentLang == "pl") {
            langPl.setBackgroundResource(R.drawable.lang_button_selected)
            textLangPl.setTextColor(Color.BLACK)
            langEn.setBackgroundResource(R.drawable.lang_button_unselected)
            textLangEn.setTextColor(Color.GRAY)
        } else {
            langEn.setBackgroundResource(R.drawable.lang_button_selected)
            textLangEn.setTextColor(Color.BLACK)
            langPl.setBackgroundResource(R.drawable.lang_button_unselected)
            textLangPl.setTextColor(Color.GRAY)
        }
    }

}
