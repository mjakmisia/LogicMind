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
import com.example.logicmind.databinding.ActivitySettingsBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.util.*

class SettingsActivity : BaseActivity() {

    private var selectedLanguage: String? = "pl"  // "pl" lub "en"
    private lateinit var binding :ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()

        //aktualne ustawienia
        val sharedPrefs = getSharedPreferences("Settings", MODE_PRIVATE)
        val currentLang = sharedPrefs.getString("My_Lang", "pl") ?: "pl"
        selectedLanguage = currentLang

        // ustaw język i tryb nocny przed setContentView
        //setLocale(currentLang)
//        AppCompatDelegate.setDefaultNightMode(
//            if (darkModeEnabled) AppCompatDelegate.MODE_NIGHT_YES
//            else AppCompatDelegate.MODE_NIGHT_NO
//        )

        //stan przełączników na start
        binding.switchSound.isChecked = sharedPrefs.getBoolean("Sound_Enabled", true)
        binding.switchDarkMode.isChecked = sharedPrefs.getBoolean("DarkMode_Enabled", false)
        binding.switchNotification.isChecked = sharedPrefs.getBoolean("Notification_Enabled", true)

        //val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        //bottomNav.selectedItemId = R.id.nav_settings

        //wybór języka
        updateLanguageSelectionUI(currentLang)

        binding.langPl.setOnClickListener {
            selectedLanguage = "pl"
            updateLanguageSelectionUI("pl")
        }

        binding.langEn.setOnClickListener {
            selectedLanguage = "en"
            updateLanguageSelectionUI("en")
        }

        // Tryb nocny
        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            sharedPrefs.edit().putBoolean("DarkMode_Enabled", isChecked).apply()
            AppCompatDelegate.setDefaultNightMode(
                if (isChecked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            )
            recreate()
        }


        // przycisk zapisz
        binding.saveButton.setOnClickListener {
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
                        sharedPrefs.edit().apply {
                            putString("My_Lang", selectedLanguage)
                            putBoolean("Sound_Enabled", binding.switchSound.isChecked)
                            putBoolean("DarkMode_Enabled", binding.switchDarkMode.isChecked)
                            putBoolean("Notifications_Enabled", binding.switchNotification.isChecked)
                            apply()
                        }

                        // tylko jeśli język się zmienił
                        if (selectedLanguage != currentLangBeforeChange) {
                            recreate()   // przeładowanie całej aktywności
                        }

                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            }
        }

        // reset ustawień
        binding.resetButton.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.reset_settings_title))
                .setMessage(getString(R.string.reset_settings_message))
                .setPositiveButton("Tak") { _, _ ->
                    sharedPrefs.edit().apply {
                        putString("My_Lang", "pl")
                        putBoolean("Sound_Enabled", true)
                        putBoolean("DarkMode_Enabled", false)
                        putBoolean("Notifications_Enabled", true)
                        apply()
                    }

                    binding.switchSound.isChecked = true
                    binding.switchDarkMode.isChecked = false
                    binding.switchNotification.isChecked = true

                    selectedLanguage = "pl"
                    updateLanguageSelectionUI("pl")
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

                    recreate()
                }
                .setNegativeButton(getString(R.string.cancel), null)
                .show()
        }

        // bottom navigation
        setupBottomNavigation(binding.includeBottomNav.bottomNavigationView, R.id.nav_settings)
    }


    private fun updateLanguageSelectionUI(currentLang: String) {
        val selectedBg = R.drawable.lang_button_selected
        val unselectedBg = R.drawable.lang_button_unselected

        if (currentLang == "pl") {
            binding.langPl.setBackgroundResource(selectedBg)
            binding.langEn.setBackgroundResource(unselectedBg)
        } else {
            binding.langEn.setBackgroundResource(selectedBg)
            binding.langPl.setBackgroundResource(unselectedBg)
        }
    }
}
