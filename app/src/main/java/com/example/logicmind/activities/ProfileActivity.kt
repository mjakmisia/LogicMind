package com.example.logicmind.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.example.logicmind.R
import com.example.logicmind.databinding.ActivityProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import java.util.Calendar.DAY_OF_WEEK
import java.util.Calendar.FRIDAY
import java.util.Calendar.MONDAY
import java.util.Calendar.SATURDAY
import java.util.Calendar.SUNDAY
import java.util.Calendar.THURSDAY
import java.util.Calendar.TUESDAY
import java.util.Calendar.WEDNESDAY
import java.util.Calendar.getInstance

class ProfileActivity : BaseActivity() {

    private lateinit var binding: ActivityProfileBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()

        //bottomNav = findViewById(R.id.bottomNavigationView)
        // Ustawienie menu na dole
        setupBottomNavigation(binding.includeBottomNav.bottomNavigationView, R.id.nav_profile)


        // Logika kalendarza
        val calendar = getInstance()
        val dayOfWeek = calendar.get(DAY_OF_WEEK) // 1=Sunday, 2=Monday, ... 7=Saturday

        val arrows = mapOf(
            MONDAY to findViewById(R.id.arrowMon),
            TUESDAY to findViewById(R.id.arrowTue),
            WEDNESDAY to findViewById(R.id.arrowWed),
            THURSDAY to findViewById(R.id.arrowThu),
            FRIDAY to findViewById(R.id.arrowFri),
            SATURDAY to findViewById(R.id.arrowSat),
            SUNDAY to findViewById<ImageView>(R.id.arrowSun))

        arrows[dayOfWeek]?.visibility = View.VISIBLE

        Log.d(
            "PROFILE",
            "Aktualny użytkownik: ${FirebaseAuth.getInstance().currentUser?.uid ?: "brak"}"
        )

        // Odwołania do widoków
        val scrollView = findViewById<View>(R.id.scrollView)
        val textLoginPrompt = findViewById<TextView>(R.id.textLoginPrompt)
        val buttonLogin = findViewById<Button>(R.id.buttonLogin)

        // Na start ukryj wszystkie widoki
        scrollView.visibility = View.GONE
        textLoginPrompt.visibility = View.GONE
        buttonLogin.visibility = View.GONE
        binding.progressBar.visibility = View.VISIBLE

        // Pobranie danych użytkownika
        val user = auth.currentUser

        binding.progressBar.visibility = View.GONE

        //poczekaj na wynik czy jest gościem
        if (!isUserLoggedIn()) {
            showLoginPrompt()
        } else {
            binding.scrollView.visibility = View.VISIBLE
            loadUserData((user!!.uid))

        }
        binding.buttonResetProgress.setOnClickListener {
            resetUserProgress()
        }

        // Obsługa przycisku zmiany hasła
        val btnChangePassword = findViewById<Button>(R.id.buttonChangePassword)

        btnChangePassword.setOnClickListener {
            showChangePasswordDialog()
        }


        // Ustawienie koloru przycisku programowo
        val btnDeleteAccount = binding.buttonDeleteAccount
        // usuń wpływ motywu
        btnDeleteAccount.backgroundTintList = null
        // pobierz drawable i zmień jego kolor
        val drawable = ContextCompat.getDrawable(this, R.drawable.bg_rounded_light_gray)?.mutate()
        drawable?.setTint(ContextCompat.getColor(this, R.color.red_lighter))
        btnDeleteAccount.background = drawable

        btnDeleteAccount.setOnClickListener {
            //popup czy na pewno chcesz usunąć konto
            val builder = AlertDialog.Builder(this)
            builder.setTitle(R.string.delete_account)
            builder.setMessage(R.string.delete_account_popup)

            builder.setPositiveButton("Tak") { _, _ ->
                deleteAccount()
            }
            builder.setNegativeButton("Nie") { dialog, _ ->
                dialog.dismiss()
            }
            val dialog = builder.create()
            dialog.show()
        }

        binding.buttonLogout.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Wylogowanie")
                .setMessage("Czy na pewno chcesz się wylogować?")
                .setPositiveButton("Tak") { _, _ ->
                    auth.signOut()
                    startActivity(Intent(this, WelcomeActivity::class.java))
                    finish()
                }
                .setNegativeButton("Nie", null)
                .show()
        }
    }

    /**
     * Metoda obsługująca zmianę hasła
     */

    private fun showChangePasswordDialog() {
        val user = auth.currentUser
        if (user == null || !isUserLoggedIn()) return

        // pole tekstowe do wpisania hasła
        val input = android.widget.EditText(this).apply {
            hint = "Nowe hasło (min. 8 znaków, wielka litera i cyfra)"
            inputType =
                android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            setPadding(50, 30, 50, 30)
            background = ContextCompat.getDrawable(context, R.drawable.bg_edittext_rounded)
        }

        // Kontener dla marginesów
        val container = android.widget.FrameLayout(this).apply {
            setPadding(40, 20, 40, 20)
            addView(input)
        }

        AlertDialog.Builder(this)
            .setTitle("Zmiana hasła")
            .setView(container)
            .setPositiveButton("Zmień") { dialog, _ ->
                val newPassword = input.text.toString().trim()

                if (newPassword.length < 8) {
                    Toast.makeText(
                        this,
                        getString(R.string.password_error_length),
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setPositiveButton
                }
                if (!newPassword.any { it.isUpperCase() }) {
                    Toast.makeText(
                        this,
                        getString(R.string.password_error_uppercase),
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setPositiveButton
                }

                if (!newPassword.any { it.isDigit() }) {
                    Toast.makeText(
                        this,
                        getString(R.string.password_error_digit),
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setPositiveButton
                }

                // Firebase: Zmiana hasła
                user.updatePassword(newPassword)
                    .addOnSuccessListener {
                        Toast.makeText(
                            this,
                            getString(R.string.password_changed_success),
                            Toast.LENGTH_SHORT
                        ).show()
                        dialog.dismiss()
                    }
                    .addOnFailureListener { e ->
                        // Firebase wymaga "świeżego" logowania do zmiany hasła.
                        if (e is FirebaseAuthRecentLoginRequiredException) {
                            Toast.makeText(
                                this,
                                getString(R.string.password_change_reauth_error),
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            Toast.makeText(
                                this,
                                getString(R.string.error_prefix, e.message),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /**
     * Pokazuje komunikat i przycisk, gdy użytkownik nie jest zalogowany
     */
    private fun showLoginPrompt() {
        val scrollView = findViewById<View>(R.id.scrollView)
        val textLoginPrompt = findViewById<TextView>(R.id.textLoginPrompt)
        val buttonLogin = findViewById<Button>(R.id.buttonLogin)

        // Ukryj główną zawartość profilu
        scrollView.visibility = View.GONE

        // Pokaż komunikat i przycisk logowania
        textLoginPrompt.visibility = View.VISIBLE
        buttonLogin.visibility = View.VISIBLE

        // Po kliknięciu — przekieruj do WelcomeActivity
        buttonLogin.setOnClickListener {
            startActivity(Intent(this, WelcomeActivity::class.java))
            finish()
        }
    }

    /**
     * Pobiera dane użytkownika (username, streak, bestStreak) z Realtime Database
     */
    private fun loadUserData(uid: String) {
        //pokazuje progress bar podczas ładowania danych użytkownika
        binding.progressBar.visibility = View.VISIBLE
        binding.scrollView.visibility = View.GONE

        db.getReference("users").child(uid)
            .get()
            .addOnSuccessListener { snapshot ->
                //dane pobrane: pokaż reszte
                binding.progressBar.visibility = View.GONE
                binding.scrollView.visibility = View.VISIBLE

                if (snapshot.exists()) {
                    val username = snapshot.child("username").value as? String ?: "Brak nazwy"
                    val currentStreak = calculateDisplayStreak(snapshot)
                    val bestStreak = snapshot.child("bestStreak").value as? Long ?: 0

                    findViewById<TextView>(R.id.textUsername).text = username
                    findViewById<TextView>(R.id.textCurrentStreak).text = getString(R.string.current_streak_text, currentStreak)
                    findViewById<TextView>(R.id.textBestStreak).text = getString(R.string.best_streak_text, bestStreak)
                } else {
                    Log.e("PROFILE", "Brak danych użytkownika w bazie dla UID: $uid")
                    // Wyloguj użytkownika i pokaż widok logowania
                    FirebaseAuth.getInstance().signOut()
                    showLoginPrompt()
                }
            }
            .addOnFailureListener { e ->
                // ukryj progress bar także w przypadku błędu
                binding.progressBar.visibility = View.GONE
                binding.scrollView.visibility = View.VISIBLE

                Log.e("PROFILE", "Błąd pobierania danych użytkownika: ${e.message}")
                Toast.makeText(this, "Błąd pobierania danych: ${e.message}", Toast.LENGTH_SHORT)
                    .show()
                // Ustaw domyślne wartości w razie błędu
                findViewById<TextView>(R.id.textUsername).text = getString(R.string.error_fetching_user_data)
                findViewById<TextView>(R.id.textCurrentStreak).text = getString(R.string.zero_days)
                findViewById<TextView>(R.id.textBestStreak).text = getString(R.string.zero_days)
            }
    }

    /**
     * Usuwa konto użytkownika
     * Firebase Auth wymaga, aby użytkownik był zalogowany ostatnio – jeśli sesja jest za stara, trzeba go ponownie uwierzytelnić
     */
    private fun deleteAccount() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            val uid = user.uid

            // usunięcie danych z Realtime Database
            db.getReference("users").child(uid)
                .removeValue()
                .addOnSuccessListener {
                    Log.d("PROFILE", "Dane użytkownika usunięte z bazy: $uid")

                    // usunięcie konta z Firebase Authentication
                    user.delete()
                        .addOnSuccessListener {
                            Log.d("PROFILE", "Konto użytkownika usunięte: $uid")
                            Toast.makeText(this, "Konto zostało usunięte", Toast.LENGTH_SHORT)
                                .show()

                            // przekierowanie do WelcomeActivity
                            startActivity(Intent(this, WelcomeActivity::class.java))
                            finish()

                        }
                        .addOnFailureListener { e ->
                            Log.e("PROFILE", "Błąd usuwania konta użytkownika: ${e.message}")
                            Toast.makeText(
                                this,
                                "Nie udało się usunąć konta: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                }
                .addOnFailureListener { e ->
                    Log.e("PROFILE", "Błąd usuwania danych użytkownika: ${e.message}")
                    Toast.makeText(
                        this,
                        "Nie udało się usunąć danych: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        } else {
            Toast.makeText(this, "Brak zalogowanego użytkownika", Toast.LENGTH_SHORT).show()

        }
    }

    private fun resetUserProgress() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            Toast.makeText(this, "Brak zalogowanego użytkownika", Toast.LENGTH_SHORT).show()
            return
        }

        val uid = user.uid
        val userRef = db.getReference("users").child(uid)

        AlertDialog.Builder(this)
            .setTitle(R.string.reset_progress_title)
            .setMessage(getString(R.string.reset_progress_message))
            .setPositiveButton("Tak") { _, _ ->
                // Pobranie całego węzła użytkownika w celu przygotowania mapy resetu
                userRef.get().addOnSuccessListener { snapshot ->
                    if (!snapshot.exists()) return@addOnSuccessListener

                    val updates = mutableMapOf<String, Any>()

                    // Reset głównych danych
                    updates["streak"] = 0
                    updates["bestStreak"] = 0
                    updates["statistics/totalStars"] = 0
                    updates["statistics/gamesPlayed"] = 0
                    updates["statistics/avgAccuracy"] = 0.0
                    updates["statistics/avgReactionTime"] = 0.0
                    updates["statistics/sumAccuracy"] = 0.0
                    updates["statistics/sumReactionTime"] = 0.0

                    // Reset wszystkich gier w każdej kategorii
                    val categoriesSnap = snapshot.child("categories")
                    categoriesSnap.children.forEach { categorySnap ->
                        val categoryKey = categorySnap.key ?: return@forEach
                        val gamesSnap = categorySnap.child("games")
                        gamesSnap.children.forEach { gameSnap ->
                            val gameKey = gameSnap.key ?: return@forEach
                            val basePath = "categories/$categoryKey/games/$gameKey"
                            updates["$basePath/bestStars"] = 0
                            updates["$basePath/starsEarned"] = 0
                            updates["$basePath/gamesPlayed"] = 0
                            updates["$basePath/accuracy"] = 0.0
                            updates["$basePath/avgReactionTime"] = 0.0
                            updates["$basePath/sumAccuracy"] = 0.0
                            updates["$basePath/sumReactionTime"] = 0.0
                            updates["$basePath/lastPlayed"] = 0L
                        }
                    }

                    // Wysyłamy wszystkie zmiany w jednym updateChildren
                    userRef.updateChildren(updates)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Postępy zostały zresetowane", Toast.LENGTH_SHORT)
                                .show()
                            loadUserData(uid) // odśwież UI
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(
                                this,
                                "Błąd resetowania danych: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                }.addOnFailureListener { e ->
                    Toast.makeText(this, "Błąd pobierania danych: ${e.message}", Toast.LENGTH_SHORT)
                        .show()
                }
            }
            .setNegativeButton("Nie", null)
            .show()
    }

}
