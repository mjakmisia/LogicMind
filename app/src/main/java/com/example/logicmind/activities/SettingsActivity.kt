package com.example.logicmind.activities

import android.app.AlertDialog
import android.content.res.Configuration
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import com.example.logicmind.R
import com.example.logicmind.additional.ReminderNotification
import com.example.logicmind.common.SoundManager
import com.example.logicmind.databinding.ActivitySettingsBinding

class SettingsActivity : BaseActivity() {

    private var selectedLanguage: String? = "pl"
    private lateinit var binding :ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()

        val sharedPrefs = getSharedPreferences("Settings", MODE_PRIVATE)
        val currentLang = sharedPrefs.getString("My_Lang", "pl") ?: "pl"
        selectedLanguage = currentLang


        val isDarkModeNow = (resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

        val isSoundEnabledSaved = sharedPrefs.getBoolean("Sound_Enabled", true)
        binding.switchSound.isChecked = isSoundEnabledSaved

        SoundManager.isSoundEnabled = isSoundEnabledSaved

        val isNotificationsEnabledSaved = sharedPrefs.getBoolean("Notifications_Enabled", true)
        binding.switchNotification.isChecked = isNotificationsEnabledSaved

        if (isNotificationsEnabledSaved) {
            ReminderNotification.scheduleNotification(this)
        } else {
            ReminderNotification.cancelNotification(this)
        }

        val savedDarkMode = sharedPrefs.contains("DarkMode_Enabled")
        binding.switchDarkMode.isChecked = if (savedDarkMode) {
            sharedPrefs.getBoolean("DarkMode_Enabled", false)
        } else {
            isDarkModeNow
        }

        binding.switchNotification.isChecked = sharedPrefs.getBoolean("Notification_Enabled", true)

        updateLanguageSelectionUI(currentLang)

        binding.langPl.setOnClickListener {
            selectedLanguage = "pl"
            updateLanguageSelectionUI("pl")
        }

        binding.langEn.setOnClickListener {
            selectedLanguage = "en"
            updateLanguageSelectionUI("en")
        }

        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            sharedPrefs.edit { putBoolean("DarkMode_Enabled", isChecked) }
            AppCompatDelegate.setDefaultNightMode(
                if (isChecked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            )
            recreate()
        }


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

                        SoundManager.isSoundEnabled = binding.switchSound.isChecked

                        if (binding.switchNotification.isChecked) {
                            ReminderNotification.scheduleNotification(this)
                        } else {
                            ReminderNotification.cancelNotification(this)
                        }

                        if (selectedLanguage != currentLangBeforeChange) {
                            recreate()
                        }

                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            }
        }

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

                    SoundManager.isSoundEnabled = true
                    selectedLanguage = "pl"
                    updateLanguageSelectionUI("pl")
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                    ReminderNotification.scheduleNotification(this)

                    recreate()
                }
                .setNegativeButton(getString(R.string.cancel), null)
                .show()
        }
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
