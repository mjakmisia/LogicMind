package com.example.logicmind.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import android.util.Patterns
import com.example.logicmind.databinding.ActivityWelcomeBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import android.text.Editable
import android.text.TextWatcher
import androidx.core.content.ContextCompat

class WelcomeActivity : BaseActivity() {

    private lateinit var binding: ActivityWelcomeBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWelcomeBinding.inflate(layoutInflater)

        binding.etPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val password = s.toString()

                // Minimalna długość 8
                if (password.length >= 8) {
                    binding.tvLength.setTextColor(
                        ContextCompat.getColor(this@WelcomeActivity, android.R.color.holo_green_dark)
                    )
                } else {
                    binding.tvLength.setTextColor(
                        ContextCompat.getColor(this@WelcomeActivity, android.R.color.holo_red_dark)
                    )
                }

                // Przynajmniej 1 duża litera
                if (password.any { it.isUpperCase() }) {
                    binding.tvUppercase.setTextColor(
                        ContextCompat.getColor(this@WelcomeActivity, android.R.color.holo_green_dark)
                    )
                } else {
                    binding.tvUppercase.setTextColor(
                        ContextCompat.getColor(this@WelcomeActivity, android.R.color.holo_red_dark)
                    )
                }

                // Przynajmniej 1 cyfra
                if (password.any { it.isDigit() }) {
                    binding.tvDigit.setTextColor(
                        ContextCompat.getColor(this@WelcomeActivity, android.R.color.holo_green_dark)
                    )
                } else {
                    binding.tvDigit.setTextColor(
                        ContextCompat.getColor(this@WelcomeActivity, android.R.color.holo_red_dark)
                    )
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // pokaż wymagania dopiero po kliknięciu w pole hasła
        binding.etPassword.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.passwordRequirementsLayout.visibility = android.view.View.VISIBLE
            }
        }

        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Jeśli użytkownik jest już zalogowany
        //if (auth.currentUser != null) goToMain()

        // Obsługa przycisków
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

            loginOrRegister(email, password)
        }

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

            registerUser(email, password)
        }


        binding.btnGuest.setOnClickListener {
            auth.signInAnonymously()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) goToMain()
                    else showToast("Nie udało się kontynuować jako gość")
                }
        }
    }

    // Funkcja logowania z automatycznym sprawdzeniem istnienia użytkownika
    private fun loginOrRegister(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { loginTask ->
                if (loginTask.isSuccessful) {
                    goToMain()
                } else {
                    // Jeśli użytkownik nie istnieje, zarejestruj go
                    if (loginTask.exception is FirebaseAuthUserCollisionException ||
                        loginTask.exception?.message?.contains("no user") == true
                    ) {
                        registerUser(email, password)
                    } else {
                        showToast("Błąd logowania: ${loginTask.exception?.message}")
                    }
                }
            }
    }

    private fun registerUser(email: String, password: String) {
        val username = binding.etEmail.text.toString().substringBefore("@") // np. z emaila
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser!!.uid

                    // Tworzymy dokument użytkownika w Firestore
                    val userData = hashMapOf(
                        "username" to username,
                        "email" to email,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "streak" to 0,
                        "statistics" to hashMapOf(
                            "avgReactionTime" to 0.0,
                            "avgAccuracy" to 0.0,
                            "avgScore" to 0,
                            "totalStars" to 0
                        )
                    )

                    db.collection("users").document(userId)
                        .set(userData)
                        .addOnSuccessListener {
                            // Dodanie kategorii i gier
                            createDefaultCategoriesAndGames(userId)

                            // Wyświetlenie komunikatu o sukcesie
                            Toast.makeText(this, "Rejestracja powiodła się!", Toast.LENGTH_SHORT).show()

                            // Automatyczne przejście do głównej aktywności
                            goToMain()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Błąd przy zapisie użytkownika: $e", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(this, "Błąd rejestracji: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }


    private fun createDefaultCategoriesAndGames(userId: String) {
        val categories = listOf("Koordynacja", "Rozwiazywanie_problemow", "Skupienie", "Pamiec")
        val defaultGames = mapOf(
            "Koordynacja" to listOf("Cards_on_the_Roads", "Symbol_Race"),
            "Rozwiazywanie_problemow" to listOf("Number_Addition", "Path_Change"),
            "Skupienie" to listOf("Word_Search", "Left_or_Right"),
            "Pamiec" to listOf("Color_Sequence", "Memory_Game")
        )

        for (category in categories) {
            val catRef = db.collection("users").document(userId)
                .collection("categories").document(category)

            val catData = hashMapOf("description" to "")
            catRef.set(catData)

            for (game in defaultGames[category]!!) {
                val gameData = hashMapOf(
                    "bestScore" to 0,
                    "avgReactionTime" to 0.0,
                    "accuracy" to 0.0,
                    "starsEarned" to 0,
                    "lastPlayed" to null,
                    "gamesPlayed" to 0
                )
                catRef.collection("games").document(game).set(gameData)
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

    private fun isPasswordValid(password: String): Boolean {
        // minimalna długość 8, przynajmniej 1 duża litera, przynajmniej 1 cyfra
        val regex = Regex("^(?=.*[A-Z])(?=.*\\d).{8,}$")
        return regex.matches(password)
    }
    private fun isEmailValid(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

}
