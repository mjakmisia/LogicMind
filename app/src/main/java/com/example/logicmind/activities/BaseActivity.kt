package com.example.logicmind.activities

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

/**
 * BaseActivity
 *
 * Klasa bazowa dla wszystkich aktywności w aplikacji.
 * Jej zadaniem jest wymuszenie języka aplikacji zapisanego w SharedPreferences,
 * tak aby po restarcie aktywności (np. obrót ekranu) język się nie zmieniał na systemowy.
 *
 * Każda aktywność powinna dziedziczyć po BaseActivity zamiast AppCompatActivity.
 */
open class BaseActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        // Pobieramy aktualnie zapisany język z ustawień aplikacji
        val sharedPrefs = newBase.getSharedPreferences("Settings", MODE_PRIVATE)
        val lang = sharedPrefs.getString("My_Lang", "pl") ?: "pl"

        // Tworzymy obiekt Locale dla wybranego języka
        val locale = Locale(lang)
        Locale.setDefault(locale)

        // Modyfikujemy konfigurację kontekstu tak, aby używała naszego języka
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)

        // Tworzymy nowy kontekst z naszą konfiguracją i przekazujemy go do AppCompatActivity
        val context = newBase.createConfigurationContext(config)
        super.attachBaseContext(context)
    }
}
