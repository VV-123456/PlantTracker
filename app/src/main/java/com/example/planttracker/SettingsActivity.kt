package com.example.planttracker

import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.Switch
import android.widget.TextView
import android.widget.TimePicker
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.work.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar
import java.util.concurrent.TimeUnit

class SettingsActivity : AppCompatActivity() {

    private lateinit var sharedPrefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        requestNotificationPermission()

        sharedPrefs = getSharedPreferences("planttracker_prefs", Context.MODE_PRIVATE)

        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        val switchNotifications = findViewById<Switch>(R.id.switchNotifications)
        val btnReminderTime = findViewById<Button>(R.id.btnReminderTime)
        val tvReminderTime = findViewById<TextView>(R.id.tvReminderTime)
        val btnClearData = findViewById<Button>(R.id.btnClearData)
        val btnAbout = findViewById<Button>(R.id.btnAbout)
        val btnDeleteAccount = findViewById<Button>(R.id.btnDeleteAccount)
        val btnCheckWatering = findViewById<Button>(R.id.btnCheckWatering)

        val notificationsEnabled = sharedPrefs.getBoolean("notifications_enabled", true)
        switchNotifications.isChecked = notificationsEnabled

        val reminderHour = sharedPrefs.getInt("reminder_hour", 9)
        val reminderMinute = sharedPrefs.getInt("reminder_minute", 0)
        tvReminderTime.text = String.format("%02d:%02d", reminderHour, reminderMinute)

        btnBack.setOnClickListener { finish() }


        btnCheckWatering.setOnClickListener {
            checkAndSendWateringNotifications()
        }

        switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            sharedPrefs.edit().putBoolean("notifications_enabled", isChecked).apply()
            if (isChecked) {
                scheduleNotification()
                Toast.makeText(this, "Уведомления включены", Toast.LENGTH_SHORT).show()
            } else {
                cancelNotification()
                Toast.makeText(this, "Уведомления выключены", Toast.LENGTH_SHORT).show()
            }
        }

        btnReminderTime.setOnClickListener {
            val calendar = Calendar.getInstance()
            val timePicker = TimePickerDialog(
                this,
                { _: TimePicker, hourOfDay: Int, minuteOfHour: Int ->
                    sharedPrefs.edit().apply {
                        putInt("reminder_hour", hourOfDay)
                        putInt("reminder_minute", minuteOfHour)
                    }.apply()
                    tvReminderTime.text = String.format("%02d:%02d", hourOfDay, minuteOfHour)
                    if (switchNotifications.isChecked) {
                        scheduleNotification()
                    }
                    Toast.makeText(this, "Время сохранено: ${String.format("%02d:%02d", hourOfDay, minuteOfHour)}", Toast.LENGTH_SHORT).show()
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true
            )
            timePicker.show()
        }

        btnClearData.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Очистка данных")
                .setMessage("Очистить локальный кэш?")
                .setPositiveButton("Да") { _, _ ->
                    Toast.makeText(this, "Данные очищены", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Нет", null)
                .show()
        }

        btnAbout.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("🌿 PlantTracker")
                .setMessage("""
                    Версия 1.0
                    
                    Приложение для ухода за растениями
                    
                    Особенности:
                    • График полива
                    • Избранное
                    • Уведомления
                    • База видов (20+)
                """.trimIndent())
                .setPositiveButton("OK", null)
                .show()
        }

        btnDeleteAccount.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Удаление аккаунта")
                .setMessage("Вы уверены? Все ваши растения и данные будут удалены БЕЗВОЗВРАТНО!")
                .setPositiveButton("Да, удалить") { _, _ ->
                    deleteUserAccount()
                }
                .setNegativeButton("Отмена", null)
                .show()
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                androidx.core.app.ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    100
                )
            }
        }
    }

    private fun scheduleNotification() {
        val hour = sharedPrefs.getInt("reminder_hour", 9)
        val minute = sharedPrefs.getInt("reminder_minute", 0)

        WorkManager.getInstance(this).cancelUniqueWork("watering_reminder")

        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        var initialDelay = target.timeInMillis - now.timeInMillis
        if (initialDelay < 0) {
            initialDelay += TimeUnit.DAYS.toMillis(1)
        }

        val workRequest = PeriodicWorkRequestBuilder<WateringWorker>(
            1, TimeUnit.DAYS
        ).setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "watering_reminder",
            ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
            workRequest
        )

        Toast.makeText(this, "Уведомления запланированы на ${String.format("%02d:%02d", hour, minute)}", Toast.LENGTH_LONG).show()
    }

    private fun cancelNotification() {
        WorkManager.getInstance(this).cancelUniqueWork("watering_reminder")
    }

    private fun checkAndSendWateringNotifications() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "Пользователь не авторизован", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(this, "Проверка растений...", Toast.LENGTH_SHORT).show()

        FirebaseFirestore.getInstance().collection("plants")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { documents ->
                val needWatering = mutableListOf<String>()
                for (doc in documents) {
                    val nextWateringDate = doc.getLong("nextWateringDate") ?: 0
                    if (nextWateringDate <= System.currentTimeMillis()) {
                        val plantName = doc.getString("name") ?: "Растение"
                        needWatering.add(plantName)
                    }
                }

                if (needWatering.isNotEmpty()) {
                    NotificationHelper(this).showWateringNotification(needWatering)
                    Toast.makeText(this, "Уведомление отправлено для ${needWatering.size} растений", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "Все растения политы!", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun deleteUserAccount() {
        val user = FirebaseAuth.getInstance().currentUser
        val userId = user?.uid

        if (userId == null) {
            Toast.makeText(this, "Пользователь не найден", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(this, "⏳ Удаление аккаунта...", Toast.LENGTH_LONG).show()

        FirebaseFirestore.getInstance().collection("plants")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { documents ->
                for (doc in documents) {
                    doc.reference.delete()
                }
                FirebaseFirestore.getInstance().collection("users")
                    .document(userId)
                    .delete()
                    .addOnSuccessListener {
                        user.delete()
                            .addOnSuccessListener {
                                Toast.makeText(this, "Аккаунт успешно удалён", Toast.LENGTH_LONG).show()
                                startActivity(Intent(this, MainActivity::class.java))
                                finish()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Ошибка удаления аккаунта: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Ошибка удаления данных пользователя: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Ошибка удаления растений: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}