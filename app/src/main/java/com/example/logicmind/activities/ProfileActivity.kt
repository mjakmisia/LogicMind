package com.example.logicmind.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
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

        setupBottomNavigation(binding.includeBottomNav.bottomNavigationView, R.id.nav_profile)

        val calendar = getInstance()
        val dayOfWeek = calendar.get(DAY_OF_WEEK)

        val arrows = mapOf(
            MONDAY to binding.arrowMon,
            TUESDAY to binding.arrowTue,
            WEDNESDAY to binding.arrowWed,
            THURSDAY to binding.arrowThu,
            FRIDAY to binding.arrowFri,
            SATURDAY to binding.arrowSat,
            SUNDAY to binding.arrowSun
        )

        arrows[dayOfWeek]?.visibility = View.VISIBLE

        binding.scrollView.visibility = View.GONE
        binding.textLoginPrompt.visibility = View.GONE
        binding.buttonLogin.visibility = View.GONE
        binding.progressBar.visibility = View.VISIBLE

        val user = auth.currentUser

        binding.progressBar.visibility = View.GONE

        if (!isUserLoggedIn()) {
            showLoginPrompt()
        } else {
            binding.scrollView.visibility = View.VISIBLE
            loadUserData((user!!.uid))
        }
        binding.buttonResetProgress.setOnClickListener {
            resetUserProgress()
        }

        binding.btnEditUsernameSmall.setOnClickListener {
            showChangeUsernameDialog()
        }

        binding.buttonChangePassword.setOnClickListener {
            showChangePasswordDialog()
        }

        val btnDeleteAccount = binding.buttonDeleteAccount

        btnDeleteAccount.backgroundTintList = null

        val drawable = ContextCompat.getDrawable(this, R.drawable.bg_rounded_light_gray)?.mutate()
        drawable?.setTint(ContextCompat.getColor(this, R.color.red_lighter))
        btnDeleteAccount.background = drawable

        btnDeleteAccount.setOnClickListener {

        val builder = AlertDialog.Builder(this)
            builder.setTitle(R.string.delete_account)
            builder.setMessage(R.string.delete_account_popup)

            builder.setPositiveButton(R.string.yes) { _, _ ->
                deleteAccount()
            }
            builder.setNegativeButton(R.string.no) { dialog, _ ->
                dialog.dismiss()
            }
            val dialog = builder.create()
            dialog.show()
        }

        binding.buttonLogout.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.logout_title))
                .setMessage(getString(R.string.logout_message))
                .setPositiveButton(R.string.yes) { _, _ ->
                    auth.signOut()
                    startActivity(Intent(this, WelcomeActivity::class.java))
                    finish()
                }
                .setNegativeButton(R.string.no, null)
                .show()
        }
    }

    private fun showChangePasswordDialog() {
        val user = auth.currentUser
        if (user == null || !isUserLoggedIn()) return

        val input = android.widget.EditText(this).apply {
            hint = getString(R.string.new_password_hint_full)
            inputType =
                android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            setPadding(50, 30, 50, 30)
            background = ContextCompat.getDrawable(context, R.drawable.bg_edittext_rounded)
        }

        val container = android.widget.FrameLayout(this).apply {
            setPadding(40, 20, 40, 20)
            addView(input)
        }

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.change_password_title))
            .setView(container)
            .setPositiveButton(getString(R.string.change_btn)) { dialog, _ ->
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

    private fun showChangeUsernameDialog() {
        val user = auth.currentUser
        if (user == null || !isUserLoggedIn()) return

        val input = android.widget.EditText(this).apply {
            hint = getString(R.string.new_username_hint)

            setText(binding.textUsername.text.toString())
            inputType =
                android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PERSON_NAME
            setPadding(50, 30, 50, 30)
            background = ContextCompat.getDrawable(context, R.drawable.bg_edittext_rounded)
        }

        val container = android.widget.FrameLayout(this).apply {
            setPadding(40, 20, 40, 20)
            addView(input)
        }

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.change_username_title))
            .setView(container)
            .setPositiveButton(getString(R.string.change_btn)) { dialog, _ ->
                val newUsername = input.text.toString().trim()

                if (newUsername.isEmpty()) {
                    Toast.makeText(
                        this,
                        getString(R.string.username_empty_error),
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setPositiveButton
                }

                val uid = user.uid
                db.getReference("users").child(uid).child("username").setValue(newUsername)
                    .addOnSuccessListener {
                        binding.textUsername.text = newUsername
                        Toast.makeText(
                            this,
                            getString(R.string.username_changed_success),
                            Toast.LENGTH_SHORT
                        ).show()
                        dialog.dismiss()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(
                            this,
                            getString(R.string.error_prefix, e.message),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showLoginPrompt() {
        binding.scrollView.visibility = View.GONE

        binding.textLoginPrompt.visibility = View.VISIBLE
        binding.buttonLogin.visibility = View.VISIBLE

        binding.buttonLogin.setOnClickListener {
            startActivity(Intent(this, WelcomeActivity::class.java))
            finish()
        }
    }

    private fun loadUserData(uid: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.scrollView.visibility = View.GONE

        db.getReference("users").child(uid)
            .get()
            .addOnSuccessListener { snapshot ->
                binding.progressBar.visibility = View.GONE
                binding.scrollView.visibility = View.VISIBLE

                if (snapshot.exists()) {
                    val username = snapshot.child("username").value as? String ?: getString(R.string.no_name)
                    val currentStreak = calculateDisplayStreak(snapshot)
                    val bestStreak = snapshot.child("bestStreak").value as? Long ?: 0

                    val totalStars = snapshot.child("statistics")
                        .child("totalStars")
                        .value as? Long ?: 0

                    binding.textUsername.text = username

                    binding.textCurrentStreak.text = resources.getQuantityString(
                        R.plurals.streak_days_format,
                        currentStreak,
                        currentStreak
                    )
                    binding.textBestStreak.text = resources.getQuantityString(
                        R.plurals.streak_days_format,
                        bestStreak.toInt(),
                        bestStreak
                    )

                    binding.tvProfileTotalStars.text = totalStars.toString()
                } else {
                    Log.e("PROFILE", "Brak danych użytkownika w bazie dla UID: $uid")
                    FirebaseAuth.getInstance().signOut()
                    showLoginPrompt()
                }
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                binding.scrollView.visibility = View.VISIBLE

                Log.e("PROFILE", "Błąd pobierania danych użytkownika: ${e.message}")
                Toast.makeText(this, getString(R.string.data_fetch_error, e.message), Toast.LENGTH_SHORT)
                    .show()
                binding.textUsername.text = getString(R.string.error_fetching_user_data)

                binding.textCurrentStreak.text = resources.getQuantityString(R.plurals.streak_days_format, 0, 0)
                binding.textBestStreak.text = resources.getQuantityString(R.plurals.streak_days_format, 0, 0)

                binding.tvProfileTotalStars.text = "-"
            }
    }

    private fun deleteAccount() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            val uid = user.uid

            db.getReference("users").child(uid)
                .removeValue()
                .addOnSuccessListener {
                    user.delete()
                        .addOnSuccessListener {
                            Toast.makeText(this, getString(R.string.account_deleted), Toast.LENGTH_SHORT)
                                .show()
                            startActivity(Intent(this, WelcomeActivity::class.java))
                            finish()
                        }
                        .addOnFailureListener { e ->
                            Log.e("PROFILE", "Błąd usuwania konta użytkownika: ${e.message}")
                            Toast.makeText(
                                this,
                                getString(R.string.account_delete_error, e.message),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                }
                .addOnFailureListener { e ->
                    Log.e("PROFILE", "Błąd usuwania danych użytkownika: ${e.message}")
                    Toast.makeText(
                        this,
                        getString(R.string.data_delete_error, e.message),
                        Toast.LENGTH_SHORT
                    ).show()
                }
        } else {
            Toast.makeText(this, getString(R.string.no_user_logged_in), Toast.LENGTH_SHORT).show()
        }
    }

    private fun resetUserProgress() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            Toast.makeText(this, getString(R.string.no_user_logged_in), Toast.LENGTH_SHORT).show()
            return
        }

        val uid = user.uid
        val userRef = db.getReference("users").child(uid)

        AlertDialog.Builder(this)
            .setTitle(R.string.reset_progress_title)
            .setMessage(getString(R.string.reset_progress_message))
            .setPositiveButton(R.string.yes) { _, _ ->
                userRef.get().addOnSuccessListener { snapshot ->
                    if (!snapshot.exists()) return@addOnSuccessListener

                    val updates = mutableMapOf<String, Any>()

                    updates["streak"] = 0
                    updates["bestStreak"] = 0
                    updates["statistics/totalStars"] = 0
                    updates["statistics/gamesPlayed"] = 0
                    updates["statistics/avgAccuracy"] = 0.0
                    updates["statistics/avgReactionTime"] = 0.0
                    updates["statistics/sumAccuracy"] = 0.0
                    updates["statistics/sumReactionTime"] = 0.0

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

                    userRef.updateChildren(updates)
                        .addOnSuccessListener {
                            Toast.makeText(this, getString(R.string.progress_reset_success), Toast.LENGTH_SHORT)
                                .show()
                            loadUserData(uid)
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(
                                this,
                                getString(R.string.data_reset_error, e.message),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                }.addOnFailureListener { e ->
                    Toast.makeText(this, getString(R.string.data_fetch_error, e.message), Toast.LENGTH_SHORT)
                        .show()
                }
            }
            .setNegativeButton(R.string.no, null)
            .show()
    }
}
