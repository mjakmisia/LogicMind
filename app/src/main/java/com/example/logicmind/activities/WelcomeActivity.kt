package com.example.logicmind.activities

import android.app.AlertDialog
import android.content.Intent
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
import androidx.core.graphics.drawable.toDrawable
import com.example.logicmind.R
import com.example.logicmind.databinding.ActivityWelcomeBinding
import com.google.firebase.auth.FirebaseAuthUserCollisionException

class WelcomeActivity : BaseActivity() {

    private lateinit var binding: ActivityWelcomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWelcomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //TextWatcher to interfejs ma 3 metody: beforeTextChanged, onTextChanged, afterTextChanged
        //trzeba je wszystkie zastosować

        // hasło – walidacja w czasie rzeczywistym
        //zmienia kolor komunikatów jeśli spełniają wymagania
        binding.etPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            //pobiera aktualne hasło:
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                //s - aktualny fragment tekstu
                val password = s.toString() //aktualny tekst hasła

                //zmienia na zielony jeżeli hasło ma co najmniej 8 znaków
                binding.tvLength.setTextColor(
                    ContextCompat.getColor(
                        this@WelcomeActivity, //aktualny kontekst
                        if (password.length >= 8) R.color.green else R.color.red
                    )
                )
                //zmienia na zielony jeżeli hasło ma co najmniej 1 dużą literę
                binding.tvUppercase.setTextColor(
                    ContextCompat.getColor(
                        this@WelcomeActivity,
                        if (password.any { it.isUpperCase() }) R.color.green else R.color.red
                    )
                )
                //zmienia na zielony jeżeli hasło ma co najmniej 1 cyfrę
                binding.tvDigit.setTextColor(
                    ContextCompat.getColor(
                        this@WelcomeActivity,
                        if (password.any { it.isDigit() }) R.color.green else R.color.red
                    )
                )
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        //pokazuje layout z wymaganiami hasła
        //pojawia sie dopiero po wpisaniu cokolwiek do pola
        binding.etPassword.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) binding.passwordRequirementsLayout.visibility = View.VISIBLE
        }

        // logowanie
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim() //trim usuwa białe znaki
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

        // wejście do aplikacji jako gość
        binding.btnGuest.setOnClickListener {
            auth.signInAnonymously()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) goToMain()
                    else showToast("Nie udało się kontynuować jako gość")
                }
        }
    }

    private fun showUsernameDialog(email: String, password: String) {
        //inflacja widoku = zamiana xml na obiekty View
        val dialogView: View = layoutInflater.inflate(R.layout.dialog_username, null)
        val input = dialogView.findViewById<EditText>(R.id.etUsername)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
        val btnProceed = dialogView.findViewById<Button>(R.id.btnProceed)

        //ładuje dialog z moim custom stylem
        val dialog = AlertDialog.Builder(this, R.style.CustomDialogStyle)
            .setView(dialogView)
            .create()

        dialog.show()
        //przezroczyste tło
        dialog.window?.setBackgroundDrawable(android.graphics.Color.TRANSPARENT.toDrawable())
        dialog.window?.decorView?.setPadding(0, 0, 0, 0)

        //parametry okna dialogu - ? chroni przed błędem jeżeli dialog.window jest null
        val params = dialog.window?.attributes
        params?.width = WindowManager.LayoutParams.WRAP_CONTENT
        params?.height = WindowManager.LayoutParams.WRAP_CONTENT
        params?.gravity = Gravity.CENTER
        //przypisuje zmodyfikowane parametry z powrotem do okna dialogu
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

            //!! - wymusza aby wartosc nie była null
            //jeżeli jest null to wyrzuci NullPointerException
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
