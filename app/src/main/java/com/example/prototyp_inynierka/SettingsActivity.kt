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
import androidx.appcompat.widget.SwitchCompat
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
                        editor.putBoolean("Sound_Enabled", true)
                        editor.putBoolean("DarkMode_Enabled", false)
                        editor.putBoolean("Notifications_Enabled", true)
                        editor.apply()

                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            }

        }

        val resetBtn = findViewById<Button>(R.id.resetButton)
        val switchSound = findViewById<SwitchCompat>(R.id.switchSound)
        val switchDarkMode = findViewById<SwitchCompat>(R.id.switchDarkMode)
        val switchNotification = findViewById<SwitchCompat>(R.id.switchNotification)

        resetBtn.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Przywracanie ustawień")
                .setMessage("Czy na pewno chcesz zresetować ustawienia do domyślnych?")
                .setPositiveButton("Tak") { _, _ ->
                    // Resetuj ustawienia
                    val editor = getSharedPreferences("Settings", MODE_PRIVATE).edit()
                    editor.putString("My_Lang", "pl")
                    editor.apply()

                    switchSound.isChecked = true
                    switchDarkMode.isChecked = false
                    switchNotification.isChecked = true

                    selectedLanguage = "pl"
                    setLocale("pl")
                    updateLanguageSelectionUI("pl")

                    recreate() // Przeładuj widok, by język się zaktualizował
                    editor.putBoolean("Sound_Enabled", true)
                    editor.putBoolean("DarkMode_Enabled", false)
                    editor.putBoolean("Notifications_Enabled", true)
                    editor.apply()

                }

                .setNegativeButton("Anuluj", null)
                .show()
            switchSound.isChecked = sharedPrefs.getBoolean("Sound_Enabled", true)
            switchDarkMode.isChecked = sharedPrefs.getBoolean("DarkMode_Enabled", false)
            switchNotification.isChecked = sharedPrefs.getBoolean("Notifications_Enabled", true)

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
