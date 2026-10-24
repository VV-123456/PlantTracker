package com.example.planttracker

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject

class PlantRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun getPlants(callback: (List<Plant>) -> Unit) {
        val userId = auth.currentUser?.uid ?: run {
            callback(emptyList())
            return
        }
        db.collection("plants")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { result ->
                val plants = result.documents.mapNotNull { doc ->
                    val plant = doc.toObject(Plant::class.java)
                    plant?.copy(
                        id = doc.id,
                        isFavorite = doc.getBoolean("isFavorite") ?: false
                    )
                }
                callback(plants)
            }
            .addOnFailureListener {
                callback(emptyList())
            }
    }
    fun addPlant(plant: Plant, callback: (String?) -> Unit) {
        db.collection("plants").add(plant)
            .addOnSuccessListener { documentRef ->
                callback(documentRef.id)
            }
            .addOnFailureListener {
                callback(null)
            }
    }

    fun updatePlant(plant: Plant, callback: (Boolean) -> Unit) {
        val updates = hashMapOf<String, Any>(
            "name" to plant.name,
            "species" to plant.species,
            "wateringIntervalDays" to plant.wateringIntervalDays,
            "photoUrl" to plant.photoUrl,
            "isFavorite" to plant.isFavorite
        )
        db.collection("plants").document(plant.id)
            .update(updates)
            .addOnSuccessListener { callback(true) }
            .addOnFailureListener { callback(false) }
    }

    fun updatePhotoUrl(plantId: String, photoUrl: String, callback: (Boolean) -> Unit) {
        db.collection("plants").document(plantId)
            .update("photoUrl", photoUrl)
            .addOnSuccessListener { callback(true) }
            .addOnFailureListener { callback(false) }
    }

    fun waterPlant(plantId: String, intervalDays: Int, callback: (Boolean) -> Unit) {
        val now = System.currentTimeMillis()
        val nextWatering = now + (intervalDays.toLong() * 24 * 60 * 60 * 1000)
        val updates = hashMapOf<String, Any>(
            "lastWateringDate" to now,
            "nextWateringDate" to nextWatering
        )
        db.collection("plants").document(plantId)
            .update(updates)
            .addOnSuccessListener { callback(true) }
            .addOnFailureListener { callback(false) }
    }
    fun deletePlant(plantId: String, callback: (Boolean) -> Unit) {
        db.collection("plants").document(plantId).delete()
            .addOnSuccessListener { callback(true) }
            .addOnFailureListener { callback(false) }
    }
    fun getUsername(callback: (String) -> Unit) {
        val userId = auth.currentUser?.uid ?: run {
            callback("Пользователь")
            return
        }
        db.collection("users").document(userId).get()
            .addOnSuccessListener { doc ->
                val username = doc.getString("username")
                callback(username ?: "Пользователь")
            }
            .addOnFailureListener {
                callback("Пользователь")
            }
    }

    fun toggleFavorite(plantId: String, isFavorite: Boolean, callback: (Boolean) -> Unit) {
        db.collection("plants").document(plantId)
            .update("isFavorite", isFavorite)
            .addOnSuccessListener { callback(true) }
            .addOnFailureListener { callback(false) }
    }

    fun updateMood(plantId: String, mood: String, callback: (Boolean) -> Unit) {
        db.collection("plants").document(plantId)
            .update("mood", mood)
            .addOnSuccessListener { callback(true) }
            .addOnFailureListener { callback(false) }
    }

    fun getAllSpecies(callback: (List<PlantSpecies>) -> Unit) {
        db.collection("plant_species")
            .get()
            .addOnSuccessListener { result ->
                val species = result.documents.mapNotNull { doc ->
                    val plantSpecies = doc.toObject(PlantSpecies::class.java)
                    plantSpecies?.copy(id = doc.id)
                }
                callback(species)
            }
            .addOnFailureListener { e ->
                Log.e("PlantRepository", "Error getting species: ${e.message}")
                callback(emptyList())
            }
    }

    fun getSpeciesById(speciesId: String, callback: (PlantSpecies?) -> Unit) {
        db.collection("plant_species").document(speciesId)
            .get()
            .addOnSuccessListener { doc ->
                val species = doc.toObject(PlantSpecies::class.java)?.copy(id = doc.id)
                callback(species)
            }
            .addOnFailureListener {
                callback(null)
            }
    }
}