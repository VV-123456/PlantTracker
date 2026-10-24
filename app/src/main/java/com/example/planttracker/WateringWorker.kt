package com.example.planttracker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class WateringWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            if (userId == null) {
                return Result.success()
            }

            val plants = getPlantsFromFirestore(userId)
            val overduePlants = plants.filter { it.nextWateringDate <= System.currentTimeMillis() }

            if (overduePlants.isNotEmpty()) {
                val plantNames = overduePlants.map { it.name }
                NotificationHelper(applicationContext).showWateringNotification(plantNames)
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    private suspend fun getPlantsFromFirestore(userId: String): List<Plant> {
        val snapshot = FirebaseFirestore.getInstance()
            .collection("plants")
            .whereEqualTo("userId", userId)
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(Plant::class.java)?.copy(id = doc.id)
        }
    }
}