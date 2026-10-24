package com.example.planttracker

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var ivAvatar: ImageView
    private lateinit var tvUsername: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvPlantsCount: TextView
    private lateinit var tvFavoritesCount: TextView
    private lateinit var tvTotalWaterings: TextView
    private lateinit var btnEditProfile: Button
    private lateinit var btnChangePassword: Button
    private lateinit var btnLogout: ImageButton
    private lateinit var btnBack: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        initViews()
        loadUserData()
        loadStats()
        setupListeners()
    }

    private fun initViews() {
        ivAvatar = findViewById(R.id.ivAvatar)
        tvUsername = findViewById(R.id.tvUsername)
        tvEmail = findViewById(R.id.tvEmail)
        tvPlantsCount = findViewById(R.id.tvPlantsCount)
        tvFavoritesCount = findViewById(R.id.tvFavoritesCount)
        tvTotalWaterings = findViewById(R.id.tvTotalWaterings)
        btnEditProfile = findViewById(R.id.btnEditProfile)
        btnChangePassword = findViewById(R.id.btnChangePassword)
        btnLogout = findViewById(R.id.btnLogout)
        btnBack = findViewById(R.id.btnBack)
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { finish() }

        btnEditProfile.setOnClickListener {
            showEditUsernameDialog()
        }

        btnChangePassword.setOnClickListener {
            val email = auth.currentUser?.email
            if (email != null && email.isNotEmpty()) {
                auth.sendPasswordResetEmail(email)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Письмо для сброса пароля отправлено на $email", Toast.LENGTH_LONG).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            } else {
                Toast.makeText(this, "Email не найден", Toast.LENGTH_SHORT).show()
            }
        }

        btnLogout.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Выход из аккаунта")
                .setMessage("Вы уверены, что хотите выйти?")
                .setPositiveButton("Да") { _, _ ->
                    auth.signOut()
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
                .setNegativeButton("Нет", null)
                .show()
        }
    }

    private fun loadUserData() {
        val user = auth.currentUser
        if (user == null) {
            tvUsername.text = "Пользователь"
            tvEmail.text = "email@example.com"
            return
        }

        tvEmail.text = user.email ?: "email@example.com"

        val userId = user.uid
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val username = document.getString("username")
                    if (!username.isNullOrEmpty()) {
                        tvUsername.text = username
                    } else {
                        tvUsername.text = "Пользователь"
                    }
                } else {
                    val newUser = hashMapOf(
                        "email" to user.email,
                        "username" to "Пользователь",
                        "createdAt" to System.currentTimeMillis()
                    )
                    db.collection("users").document(userId).set(newUser)
                    tvUsername.text = "Пользователь"
                }
            }
            .addOnFailureListener { e ->
                tvUsername.text = "Пользователь"
                Toast.makeText(this, "Ошибка загрузки: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadStats() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            tvPlantsCount.text = "0"
            tvFavoritesCount.text = "0"
            tvTotalWaterings.text = "0"
            return
        }

        db.collection("plants")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { plants ->
                tvPlantsCount.text = "${plants.size()}"

                val favorites = plants.documents.count { doc ->
                    doc.getBoolean("isFavorite") == true
                }
                tvFavoritesCount.text = "$favorites"
            }
            .addOnFailureListener {
                tvPlantsCount.text = "0"
                tvFavoritesCount.text = "0"
            }

        db.collection("plants")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { plants ->
                var totalWaterings = 0
                for (doc in plants) {
                    val lastWatering = doc.getLong("lastWateringDate")
                    if (lastWatering != null && lastWatering > 0) {
                        totalWaterings++
                    }
                }
                tvTotalWaterings.text = "$totalWaterings"
            }
            .addOnFailureListener {
                tvTotalWaterings.text = "0"
            }
    }

    private fun showEditUsernameDialog() {
        val currentUsername = tvUsername.text.toString()
        val input = EditText(this)
        input.hint = "Новое имя"
        input.setText(currentUsername)
        input.inputType = android.text.InputType.TYPE_CLASS_TEXT

        AlertDialog.Builder(this)
            .setTitle("Редактировать профиль")
            .setMessage("Введите новое имя пользователя")
            .setView(input)
            .setPositiveButton("Сохранить") { _, _ ->
                val newUsername = input.text.toString().trim()
                if (newUsername.isNotEmpty() && newUsername != currentUsername) {
                    updateUsername(newUsername)
                } else if (newUsername.isEmpty()) {
                    Toast.makeText(this, "Имя не может быть пустым", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun updateUsername(newUsername: String) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "Пользователь не найден", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("users").document(userId)
            .update("username", newUsername)
            .addOnSuccessListener {
                tvUsername.text = newUsername
                Toast.makeText(this, "Имя обновлено!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}