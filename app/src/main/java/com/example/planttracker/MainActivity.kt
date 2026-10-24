package com.example.planttracker

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etUsername: EditText
    private lateinit var btnTogglePassword: ImageButton
    private lateinit var btnLogin: Button
    private lateinit var btnRegister: Button
    private lateinit var btnForgotPassword: Button
    private lateinit var tvStatus: TextView

    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        if (auth.currentUser != null && auth.currentUser?.isEmailVerified == true) {
            startActivity(Intent(this, PlantListActivity::class.java))
            finish()
            return
        } else if (auth.currentUser != null && auth.currentUser?.isEmailVerified == false) {
            Toast.makeText(this, "Подтвердите email перед входом. Проверьте почту!", Toast.LENGTH_LONG).show()
        }

        initViews()
        setupListeners()
        setLoginMode(true)
    }

    private fun initViews() {
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        etUsername = findViewById(R.id.etUsername)
        btnTogglePassword = findViewById(R.id.btnTogglePassword)
        btnLogin = findViewById(R.id.btnLogin)
        btnRegister = findViewById(R.id.btnRegister)
        btnForgotPassword = findViewById(R.id.btnForgotPassword)
        tvStatus = findViewById(R.id.tvStatus)
    }

    private fun setupListeners() {
        btnTogglePassword.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            if (isPasswordVisible) {
                etPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                btnTogglePassword.setImageResource(R.drawable.ic_eye_open)
            } else {
                etPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                btnTogglePassword.setImageResource(R.drawable.ic_eye_closed)
            }
            etPassword.setSelection(etPassword.text.length)
        }

        btnLogin.setOnClickListener {
            setLoginMode(true)
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                tvStatus.text = "Введите email и пароль"
                return@setOnClickListener
            }
            loginUser(email, password)
        }

        btnRegister.setOnClickListener {
            setLoginMode(false)
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val username = etUsername.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                tvStatus.text = "Введите email и пароль"
                return@setOnClickListener
            }
            if (username.isEmpty()) {
                tvStatus.text = "Введите ваше имя"
                return@setOnClickListener
            }
            if (password.length < 6) {
                tvStatus.text = "Пароль должен быть не менее 6 символов"
                return@setOnClickListener
            }
            registerUser(email, password, username)
        }

        btnForgotPassword.setOnClickListener {
            val email = etEmail.text.toString().trim()
            if (email.isEmpty()) {
                tvStatus.text = "Введите email для восстановления"
                return@setOnClickListener
            }
            auth.sendPasswordResetEmail(email)
                .addOnSuccessListener {
                    tvStatus.text = "Письмо для сброса пароля отправлено на $email"
                    Toast.makeText(this, "Проверьте почту", Toast.LENGTH_LONG).show()
                }
                .addOnFailureListener {
                    tvStatus.text = "Ошибка: ${it.message}"
                }
        }
    }

    private fun setLoginMode(isLogin: Boolean) {
        if (isLogin) {
            btnLogin.backgroundTintList = ContextCompat.getColorStateList(this, android.R.color.holo_green_dark)
            btnLogin.setTextColor(ContextCompat.getColor(this, android.R.color.white))
            btnRegister.backgroundTintList = ContextCompat.getColorStateList(this, android.R.color.darker_gray)
            btnRegister.setTextColor(ContextCompat.getColor(this, android.R.color.white))
            etUsername.visibility = android.view.View.GONE
            tvStatus.text = ""
        } else {
            btnRegister.backgroundTintList = ContextCompat.getColorStateList(this, android.R.color.holo_green_dark)
            btnRegister.setTextColor(ContextCompat.getColor(this, android.R.color.white))
            btnLogin.backgroundTintList = ContextCompat.getColorStateList(this, android.R.color.darker_gray)
            btnLogin.setTextColor(ContextCompat.getColor(this, android.R.color.white))
            etUsername.visibility = android.view.View.VISIBLE
            tvStatus.text = ""
        }
    }

    private fun registerUser(email: String, password: String, username: String) {
        btnRegister.isEnabled = false
        tvStatus.text = "Регистрация..."

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser

                    user?.sendEmailVerification()
                        ?.addOnSuccessListener {
                            tvStatus.text = "Письмо с подтверждением отправлено на $email"
                            Toast.makeText(this, "Проверьте почту ($email) и папку СПАМ!", Toast.LENGTH_LONG).show()

                            val userId = user.uid
                            val userData = hashMapOf(
                                "email" to email,
                                "username" to username,
                                "createdAt" to System.currentTimeMillis(),
                                "emailVerified" to false
                            )
                            db.collection("users").document(userId).set(userData)

                            etPassword.text.clear()
                            etUsername.text.clear()
                            setLoginMode(true)
                        }
                        ?.addOnFailureListener { e ->
                            tvStatus.text = "Не удалось отправить письмо: ${e.message}"
                        }
                } else {
                    tvStatus.text = "Ошибка: ${task.exception?.message}"
                }
                btnRegister.isEnabled = true
            }
    }

    private fun loginUser(email: String, password: String) {
        btnLogin.isEnabled = false
        tvStatus.text = "Вход..."

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                btnLogin.isEnabled = true
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user?.isEmailVerified == true) {
                        db.collection("users").document(user.uid)
                            .update("emailVerified", true)

                        Toast.makeText(this, "Добро пожаловать!", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, PlantListActivity::class.java))
                        finish()
                    } else {
                        tvStatus.text = "Подтвердите email. Проверьте почту!"
                        Toast.makeText(this, "Пожалуйста, подтвердите email перед входом", Toast.LENGTH_LONG).show()
                        auth.signOut()
                    }
                } else {
                    tvStatus.text = "Ошибка: ${task.exception?.message}"
                }
            }
    }
}