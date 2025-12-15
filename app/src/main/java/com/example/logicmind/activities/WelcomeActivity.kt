package com.example.logicmind.activities

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.core.widget.doOnTextChanged
import com.example.logicmind.R
import com.example.logicmind.databinding.ActivityWelcomeBinding
import com.google.firebase.auth.FirebaseAuthUserCollisionException

class WelcomeActivity : BaseActivity() {

    private lateinit var binding: ActivityWelcomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWelcomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.etPassword.doOnTextChanged { text, _, _, _ ->
            val password = text.toString()
            val context = binding.root.context

            fun updateColor(view: TextView, isValid: Boolean) {
                val colorRes = if (isValid) R.color.green else R.color.red
                view.setTextColor(ContextCompat.getColor(context, colorRes))
            }

            updateColor(binding.tvLength, password.length >= 8)
            updateColor(binding.tvUppercase, password.any { it.isUpperCase() })
            updateColor(binding.tvDigit, password.any { it.isDigit() })
        }

        binding.etPassword.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) binding.passwordRequirementsLayout.visibility = View.VISIBLE
        }

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            if (email.isEmpty() || password.isEmpty()) {
                showToast(getString(R.string.fill_all_fields))
                return@setOnClickListener
            }
            if (!isEmailValid(email)) {
                showToast(getString(R.string.invalid_email))
                return@setOnClickListener
            }

            if (!isNetworkAvailable()) {
                Toast.makeText(this, getString(R.string.no_internet), Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            loginUser(email, password)
        }

        binding.btnRegister.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                showToast(getString(R.string.fill_all_fields))
                return@setOnClickListener
            }
            if (!isEmailValid(email)) {
                showToast(getString(R.string.invalid_email))
                return@setOnClickListener
            }
            if (!isNetworkAvailable()) {
                Toast.makeText(this, getString(R.string.no_internet), Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            showUsernameDialog(email, password)
        }
        binding.btnGuest.setOnClickListener {
            auth.signInAnonymously()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) goToMain()
                    else showToast(getString(R.string.guest_login_error))
                }
        }
    }

    private fun showUsernameDialog(email: String, password: String) {
        val dialogView: View = layoutInflater.inflate(R.layout.dialog_username, null)
        val input = dialogView.findViewById<EditText>(R.id.etUsername)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
        val btnProceed = dialogView.findViewById<Button>(R.id.btnProceed)

        val dialog = AlertDialog.Builder(this, R.style.CustomDialogStyle)
            .setView(dialogView)
            .create()

        dialog.show()
        dialog.window?.setBackgroundDrawable(android.graphics.Color.TRANSPARENT.toDrawable())
        dialog.window?.decorView?.setPadding(0, 0, 0, 0)

        val params = dialog.window?.attributes
        params?.width = WindowManager.LayoutParams.WRAP_CONTENT
        params?.height = WindowManager.LayoutParams.WRAP_CONTENT
        params?.gravity = Gravity.CENTER

        dialog.window?.attributes = params

        btnCancel.setOnClickListener { dialog.dismiss() }
        btnProceed.setOnClickListener {
            val username = input.text.toString().trim()
            if (username.isEmpty()) {
                input.error = getString(R.string.login_empty_error)
            } else {
                dialog.dismiss()
                registerUser(username, email, password)
            }
        }
    }

    private fun registerUser(username: String, email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser!!.uid
                    val userRef = db.getReference("users").child(userId)
                    val userData = mapOf(
                        "username" to username,
                        "email" to email,
                        "streak" to 0,
                        "bestStreak" to 0,
                        "statistics" to mapOf(
                            "avgReactionTime" to 0.0,
                            "avgAccuracy" to 0.0,
                            "totalStars" to 0,
                            "gamesPlayed" to 0,
                            "sumAccuracy" to 0.0,
                            "sumReactionTime" to 0.0
                        )
                    )
                    userRef.setValue(userData)
                        .addOnSuccessListener {
                            createDefaultCategoriesAndGames(userId)
                            showToast(getString(R.string.registration_success))
                            goToMain()
                        }
                        .addOnFailureListener { e ->
                            Log.e("REGISTER", "Błąd zapisu użytkownika: ${e.message}")
                            showToast(getString(R.string.user_save_error))
                        }
                } else {
                    if (task.exception is FirebaseAuthUserCollisionException) {
                        showToast(getString(R.string.email_collision_error))
                    } else {
                        Toast.makeText(
                            this,
                            getString(R.string.registration_error_prefix, task.exception?.message),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
    }

    private fun createDefaultCategoriesAndGames(userId: String) {
        val userRef = db.getReference("users").child(userId)
        val categories = listOf(
            GameKeys.CATEGORY_COORDINATION,
            GameKeys.CATEGORY_REASONING,
            GameKeys.CATEGORY_FOCUS,
            GameKeys.CATEGORY_MEMORY
        )
        val defaultGames = mapOf(
            GameKeys.CATEGORY_COORDINATION to listOf(
                GameKeys.GAME_ROAD_DASH,
                GameKeys.GAME_SYMBOL_RACE
            ),
            GameKeys.CATEGORY_REASONING to listOf(
                GameKeys.GAME_NUMBER_ADDITION,
                GameKeys.GAME_PATH_CHANGE
            ),
            GameKeys.CATEGORY_FOCUS to listOf(
                GameKeys.GAME_WORD_SEARCH,
                GameKeys.GAME_LEFT_OR_RIGHT
            ),
            GameKeys.CATEGORY_MEMORY to listOf(
                GameKeys.GAME_COLOR_SEQUENCE,
                GameKeys.GAME_CARD_MATCH
            )
        )

        for (category in categories) {
            val catRef = userRef.child("categories").child(category)

            for (game in defaultGames[category]!!) {
                val gameData = mapOf(
                    "bestStars" to 0,
                    "avgReactionTime" to 0.0,
                    "accuracy" to 0.0,
                    "starsEarned" to 0,
                    "lastPlayed" to null,
                    "gamesPlayed" to 0,
                    "sumAccuracy" to 0.0,
                    "sumReactionTime" to 0.0,
                )
                catRef.child("games").child(game).setValue(gameData)
            }
        }
    }

    private fun loginUser(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    binding.tvErrorMessage.visibility = View.GONE
                    goToMain()
                } else {
                    binding.tvErrorMessage.text = getString(R.string.login_validation)
                    binding.tvErrorMessage.visibility = View.VISIBLE
                }
            }
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    private fun isEmailValid(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}