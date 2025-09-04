package com.example.logicmind.activities

import android.app.AlertDialog
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SwitchCompat
import com.example.logicmind.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.util.*

class SettingsActivity : BaseActivity() {

    private var selectedLanguage: String? = "pl"  // "pl" lub "en"

    override fun onCreate(savedInstanceState: Bundle?) {
        val sharedPrefs = getSharedPreferences("Settings", MODE_PRIVATE)
        val currentLang = sharedPrefs.getString("My_Lang", "pl") ?: "pl"
        val darkModeEnabled = sharedPrefs.getBoolean("DarkMode_Enabled", false)

        // ustaw język i tryb nocny przed setContentView
        setLocale(currentLang)
        AppCompatDelegate.setDefaultNightMode(
            if (darkModeEnabled) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        supportActionBar?.hide()

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNav.selectedItemId = R.id.nav_settings

        val langPl = findViewById<LinearLayout>(R.id.langPl)
        val langEn = findViewById<LinearLayout>(R.id.langEn)

        val saveBtn = findViewById<Button>(R.id.saveButton)
        val resetBtn = findViewById<Button>(R.id.resetButton)

        val switchSound = findViewById<SwitchCompat>(R.id.switchSound)
        val switchDarkMode = findViewById<SwitchCompat>(R.id.switchDarkMode)
        val switchNotification = findViewById<SwitchCompat>(R.id.switchNotification)

        // ustaw stan przełączników
        switchSound.isChecked = sharedPrefs.getBoolean("Sound_Enabled", true)
        switchDarkMode.isChecked = darkModeEnabled
        switchNotification.isChecked = sharedPrefs.getBoolean("Notifications_Enabled", true)

        // język
        selectedLanguage = currentLang
        updateLanguageSelectionUI(currentLang)

        langPl.setOnClickListener {
            selectedLanguage = "pl"
            updateLanguageSelectionUI("pl")
        }

        langEn.setOnClickListener {
            selectedLanguage = "en"
            updateLanguageSelectionUI("en")
        }

        // tryb nocny
        switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            val editor = sharedPrefs.edit()
            editor.putBoolean("DarkMode_Enabled", isChecked)
            editor.apply()

            val mode = if (isChecked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            AppCompatDelegate.setDefaultNightMode(mode)
            recreate()
        }

        // zapis ustawień
        saveBtn.setOnClickListener {
            val currentLangBeforeChange = sharedPrefs.getString("My_Lang", "pl") ?: "pl"

            if (selectedLanguage != null) {
                val confirmMessage = if (currentLangBeforeChange == "pl") {
                    "Czy na pewno chcesz zapisać ustawienia?"
                } else {
                    "Are you sure you want to save changes?"
                }

                AlertDialog.Builder(this)
                    .setTitle(getString(R.string.changes))
                    .setMessage(confirmMessage)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        val editor = sharedPrefs.edit()
                        editor.putString("My_Lang", selectedLanguage)
                        editor.putBoolean("Sound_Enabled", switchSound.isChecked)
                        editor.putBoolean("DarkMode_Enabled", switchDarkMode.isChecked)
                        editor.putBoolean("Notifications_Enabled", switchNotification.isChecked)
                        editor.apply()

                        // tylko jeśli język się zmienił
                        if (selectedLanguage != currentLangBeforeChange) {
                            setLocale(selectedLanguage!!)
                            recreate()   // przeładowanie całej aktywności
                        }

                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            }
        }

        // reset ustawień
        resetBtn.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.reset_settings_title))
                .setMessage(getString(R.string.reset_settings_message))
                .setPositiveButton("Tak") { _, _ ->
                    val editor = sharedPrefs.edit()
                    editor.putString("My_Lang", "pl")
                    editor.putBoolean("Sound_Enabled", true)
                    editor.putBoolean("DarkMode_Enabled", false)
                    editor.putBoolean("Notifications_Enabled", true)
                    editor.apply()

                    switchSound.isChecked = true
                    switchDarkMode.isChecked = false
                    switchNotification.isChecked = true

                    selectedLanguage = "pl"
                    setLocale("pl")
                    updateLanguageSelectionUI("pl")
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

                    recreate()
                }
                .setNegativeButton(getString(R.string.cancel), null)
                .show()
        }

        // bottom navigation
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> { startActivity(Intent(this, MainActivity::class.java)); true }
                R.id.nav_statistics -> true
                R.id.nav_profile -> { startActivity(Intent(this, ProfileActivity::class.java)); true }
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
