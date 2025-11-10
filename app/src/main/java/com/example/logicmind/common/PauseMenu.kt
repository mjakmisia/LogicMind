package com.example.logicmind.common

import android.content.Context
import android.view.View
import android.widget.Button
import android.widget.ImageView
import androidx.fragment.app.FragmentActivity
import com.example.logicmind.R
import androidx.core.view.isVisible

/**
 * Uniwersalne menu pauzy dla gier.
 * Obsługuje wstrzymanie gry, wznowienie, restart, wyjście i pokazanie instrukcji.
 */
class PauseMenu(
    private val context: Context,
    private val pauseOverlay: View,
    pauseButton: ImageView,
    private val onRestart: () -> Unit,
    private val onResume: () -> Unit,
    private val onPause: () -> Unit, // Dodajemy callback dla pauzy
    private val onExit: () -> Unit,
    private val instructionTitle: String? = null,
    private val instructionMessage: String? = null
) {
    var isPaused: Boolean = false
        private set

    init {
        setupButtons()
        pauseButton.setOnClickListener { pause() }
    }

    private fun setupButtons() {
        pauseOverlay.findViewById<Button>(R.id.btnResume).setOnClickListener {
            resume()
        }

        pauseOverlay.findViewById<Button>(R.id.btnRestart).setOnClickListener {
            if (isPaused) {
                resume()
            }
            onRestart()
        }

        pauseOverlay.findViewById<Button>(R.id.btnExit).setOnClickListener {
            if (isPaused) {
                resume()
            }
            onExit()
        }

        pauseOverlay.findViewById<Button>(R.id.btnHelp).setOnClickListener {
            instructionTitle?.let { title ->
                instructionMessage?.let { message ->
                    if (context is FragmentActivity) {
                        val dialog = InstructionDialogFragment.newInstance(title, message)
                        dialog.show(context.supportFragmentManager, "instructionDialog")
                    }
                }
            }
        }
    }

    /** Włącza tryb pauzy i pokazuje menu. */
    fun pause() {
        if (!isPaused) {
            pauseOverlay.visibility = View.VISIBLE
            isPaused = true
            onPause() // Wywołujemy callback przy pauzie
        }
    }

    /** Wyłącza tryb pauzy i wznawia grę. */
    fun resume() {
        if (isPaused) {
            pauseOverlay.visibility = View.GONE
            isPaused = false
            onResume()
        }
    }

    fun syncWithOverlay() {
        isPaused = (pauseOverlay.isVisible)
    }
}