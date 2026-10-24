package com.example.planttracker

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import android.util.Log
import com.example.planttracker.SettingsActivity

class PlantListActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var btnAddPlant: Button
    private lateinit var tvWelcome: TextView
    private lateinit var searchView: SearchView
    private lateinit var bottomNavigation: BottomNavigationView

    private lateinit var repository: PlantRepository
    private lateinit var adapter: PlantAdapter
    private var plants = mutableListOf<Plant>()
    private var filteredPlants = mutableListOf<Plant>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_plant_list)

        repository = PlantRepository()

        initViews()
        setupListeners()
        loadUsername()
        loadPlants()
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.recyclerView)
        btnAddPlant = findViewById(R.id.btnAddPlant)
        tvWelcome = findViewById(R.id.tvWelcome)
        searchView = findViewById(R.id.searchView)
        bottomNavigation = findViewById(R.id.bottomNavigation)

        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = PlantAdapter(
            filteredPlants,
            onWaterClick = { plant -> waterPlant(plant) },
            onEditClick = { plant -> editPlant(plant) },
            onDeleteClick = { plant -> deletePlant(plant) },
            onFavoriteClick = { plant -> toggleFavorite(plant) }
        )
        recyclerView.adapter = adapter

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                filterPlants(newText ?: "")
                return true
            }
        })
    }

    private fun setupListeners() {
        btnAddPlant.setOnClickListener {
            startActivity(Intent(this, AddPlantActivity::class.java))
        }

        bottomNavigation.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> true
                R.id.nav_favorites -> {
                    startActivity(Intent(this, FavoritesActivity::class.java))
                    true
                }
                R.id.nav_calendar -> {
                    startActivity(Intent(this, CalendarActivity::class.java))
                    true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    true
                }
                R.id.nav_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }

    private fun filterPlants(query: String) {
        filteredPlants.clear()
        if (query.isEmpty()) {
            filteredPlants.addAll(plants)
        } else {
            filteredPlants.addAll(plants.filter {
                it.name.contains(query, ignoreCase = true) ||
                        it.species.contains(query, ignoreCase = true)
            })
        }
        adapter.updateList(filteredPlants)
    }

    private fun loadUsername() {
        repository.getUsername { username ->
            tvWelcome.text = "Добро пожаловать, $username!"
        }
    }

    private fun loadPlants() {
        repository.getPlants { plantList ->
            plants.clear()
            plants.addAll(plantList)
            filterPlants(searchView.query?.toString() ?: "")
        }
    }

    private fun waterPlant(plant: Plant) {
        repository.waterPlant(plant.id, plant.wateringIntervalDays) { success ->
            if (success) {
                repository.updateMood(plant.id, "happy") { _ -> }
                Toast.makeText(this, "${plant.name} полит!", Toast.LENGTH_SHORT).show()
                loadPlants()
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
            .setMessage("Вы уверены, что хотите удалить ${plant.name}?")
            .setPositiveButton("Да") { _, _ ->
                repository.deletePlant(plant.id) { success ->
                    if (success) {
                        Toast.makeText(this, "Растение удалено", Toast.LENGTH_SHORT).show()
                        loadPlants()
                    } else {
                        Toast.makeText(this, "Ошибка", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Нет", null)
            .show()
    }

    private fun toggleFavorite(plant: Plant) {
        val newFavoriteState = !plant.isFavorite
        Log.d("PlantListActivity", "Toggling favorite for ${plant.name} to $newFavoriteState")

        repository.toggleFavorite(plant.id, newFavoriteState) { success ->
            if (success) {
                Log.d("PlantListActivity", "Success! Reloading plants")
                loadPlants()
                val message = if (newFavoriteState) "Добавлено в избранное" else "Избранное удалено"
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            } else {
                Log.e("PlantListActivity", "Failed to toggle favorite")
                Toast.makeText(this, "Ошибка", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadPlants()
    }
}