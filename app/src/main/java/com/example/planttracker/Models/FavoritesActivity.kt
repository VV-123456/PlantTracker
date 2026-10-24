package com.example.planttracker

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class FavoritesActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var btnBack: ImageButton
    private lateinit var tvEmpty: TextView

    private lateinit var repository: PlantRepository
    private lateinit var adapter: PlantAdapter
    private var plants = mutableListOf<Plant>()
    private var favoritePlants = mutableListOf<Plant>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorites)

        repository = PlantRepository()

        initViews()
        setupListeners()
        loadFavorites()
    }
    private fun initViews() {
        recyclerView = findViewById(R.id.recyclerView)
        btnBack = findViewById(R.id.btnBack)
        tvEmpty = findViewById(R.id.tvEmpty)

        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = PlantAdapter(
            favoritePlants,
            onWaterClick = { plant -> waterPlant(plant) },
            onEditClick = { plant -> editPlant(plant) },
            onDeleteClick = { plant -> deletePlant(plant) },
            onFavoriteClick = { plant -> toggleFavorite(plant) }
        )
        recyclerView.adapter = adapter
    }
    private fun setupListeners() {
        btnBack.setOnClickListener {
            finish()
        }
    }
    private fun loadFavorites() {
        repository.getPlants { plantList ->
            plants.clear()
            plants.addAll(plantList)
            favoritePlants.clear()
            favoritePlants.addAll(plants.filter { it.isFavorite })

            if (favoritePlants.isEmpty()) {
                tvEmpty.visibility = android.view.View.VISIBLE
                recyclerView.visibility = android.view.View.GONE
            } else {
                tvEmpty.visibility = android.view.View.GONE
                recyclerView.visibility = android.view.View.VISIBLE
                adapter.updateList(favoritePlants)
            }
        }
    }
    private fun waterPlant(plant: Plant) {
        repository.waterPlant(plant.id, plant.wateringIntervalDays) { success ->
            if (success) {
                repository.updateMood(plant.id, "happy") { _ -> }
                Toast.makeText(this, "${plant.name} полит!", Toast.LENGTH_SHORT).show()
                loadFavorites()
            } else {
                Toast.makeText(this, "Ошибка", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun editPlant(plant: Plant) {
        val intent = Intent(this, AddPlantActivity::class.java)
        intent.putExtra("plant_id", plant.id)
        intent.putExtra("plant_name", plant.name)
        intent.putExtra("plant_species", plant.species)
        intent.putExtra("plant_interval", plant.wateringIntervalDays)
        intent.putExtra("plant_photo_path", plant.photoUrl)
        startActivity(intent)
    }
    private fun deletePlant(plant: Plant) {
        AlertDialog.Builder(this)
            .setTitle("Удалить растение")
            .setMessage("Вы уверены?")
            .setPositiveButton("Да") { _, _ ->
                repository.deletePlant(plant.id) { success ->
                    if (success) {
                        Toast.makeText(this, "Растение удалено", Toast.LENGTH_SHORT).show()
                        loadFavorites()
                    }
                }
            }
            .setNegativeButton("Нет", null)
            .show()
    }
    private fun toggleFavorite(plant: Plant) {
        val newFavoriteState = !plant.isFavorite
        repository.toggleFavorite(plant.id, newFavoriteState) { success ->
            if (success) {
                loadFavorites()
                val message = if (newFavoriteState) "Добавлено в избранное" else "Избранное удалено"
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
        }
    }
    override fun onResume() {
        super.onResume()
        loadFavorites()
    }
}