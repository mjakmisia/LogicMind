package com.example.logicmind.activities

import android.app.AlertDialog
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.util.Patterns
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.logicmind.R
import com.example.logicmind.databinding.ActivityWelcomeBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.database.FirebaseDatabase

class WelcomeActivity : BaseActivity() {

    private lateinit var binding: ActivityWelcomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWelcomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseDatabase.getInstance("https://logicmind-default-rtdb.europe-west1.firebasedatabase.app")

        // hasło – walidacja
        binding.etPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val password = s.toString()
                binding.tvLength.setTextColor(
                    ContextCompat.getColor(
                        this@WelcomeActivity,
                        if (password.length >= 8) android.R.color.holo_green_dark else android.R.color.holo_red_dark
                    )
                )
                binding.tvUppercase.setTextColor(
                    ContextCompat.getColor(
                        this@WelcomeActivity,
                        if (password.any { it.isUpperCase() }) android.R.color.holo_green_dark else android.R.color.holo_red_dark
                    )
                )
                binding.tvDigit.setTextColor(
                    ContextCompat.getColor(
                        this@WelcomeActivity,
                        if (password.any { it.isDigit() }) android.R.color.holo_green_dark else android.R.color.holo_red_dark
                    )
                )
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        binding.etPassword.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) binding.passwordRequirementsLayout.visibility = View.VISIBLE
        }

        // logowanie
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            if (email.isEmpty() || password.isEmpty()) {
                showToast("Wypełnij wszystkie pola")
                return@setOnClickListener
            }
            if (!isEmailValid(email)) {
                showToast("Niepoprawny adres e-mail")
                return@setOnClickListener
            }
            loginUser(email, password)
        }

        // rejestracja
        binding.btnRegister.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                showToast("Wypełnij wszystkie pola")
                return@setOnClickListener
            }
            if (!isEmailValid(email)) {
                showToast("Niepoprawny adres e-mail")
                return@setOnClickListener
            }

            showUsernameDialog(email, password)
        }

        // gość
        binding.btnGuest.setOnClickListener {
            auth.signInAnonymously()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) goToMain()
                    else showToast("Nie udało się kontynuować jako gość")
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
        dialog.window?.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
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
                input.error = "Login nie może być pusty"
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
                        "bestStreak" to 0, // Dodano bestStreak
                        "statistics" to mapOf(
                            "avgReactionTime" to 0.0,
                            "avgAccuracy" to 0.0,
                            "avgScore" to 0,
                            "totalStars" to 0
                        )
                    )

                    userRef.setValue(userData)
                        .addOnSuccessListener {
                            createDefaultCategoriesAndGames(userId)
                            Toast.makeText(this, "Rejestracja powiodła się!", Toast.LENGTH_SHORT)
                                .show()
                            goToMain()
                        }
                        .addOnFailureListener { e ->
                            Log.e("REGISTER", "Błąd zapisu użytkownika: ${e.message}")
                            Toast.makeText(this, "Błąd przy zapisie użytkownika", Toast.LENGTH_SHORT)
                                .show()
                        }
                } else {
                    if (task.exception is FirebaseAuthUserCollisionException) {
                        Toast.makeText(this, "Ten e-mail jest już zarejestrowany", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Błąd rejestracji: ${task.exception?.message}", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            }
    }

    private fun createDefaultCategoriesAndGames(userId: String) {
        val userRef = db.getReference("users").child(userId)
        val categories = listOf(GameKeys.CATEGORY_COORDINATION, GameKeys.CATEGORY_REASONING, GameKeys.CATEGORY_FOCUS, GameKeys.CATEGORY_MEMORY)
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
                GameKeys.GAME_FRUIT_SORT
            ),
            GameKeys.CATEGORY_MEMORY to listOf(
                GameKeys.GAME_COLOR_SEQUENCE,
                GameKeys.GAME_CARD_MATCH
            )
        )

        for (category in categories) {
            val catRef = userRef.child("categories").child(category)
            catRef.child("description").setValue("")

            for (game in defaultGames[category]!!) {
                val gameData = mapOf(
                    "bestScore" to 0,
                    "avgReactionTime" to 0.0,
                    "accuracy" to 0.0,
                    "starsEarned" to 0,
                    "lastPlayed" to null,
                    "gamesPlayed" to 0
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
                    binding.tvErrorMessage.text = "Nieprawidłowy e-mail lub hasło"
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
