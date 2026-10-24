package com.example.planttracker

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class SpeciesListActivity : AppCompatActivity() {

    private lateinit var repository: PlantRepository
    private lateinit var rvSpecies: RecyclerView
    private lateinit var adapter: SpeciesAdapter
    private var speciesList = mutableListOf<PlantSpecies>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_species_list)

        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        btnBack.setOnClickListener {
            finish()
        }

        repository = PlantRepository()
        initViews()
        loadSpecies()
    }

    private fun initViews() {
        rvSpecies = findViewById(R.id.rvSpecies)
        rvSpecies.layoutManager = LinearLayoutManager(this)

        adapter = SpeciesAdapter(speciesList) { species ->
            val resultIntent = Intent().apply {
                putExtra("species_name", species.name)
                putExtra("species_scientific", species.scientificName)
                putExtra("species_default_watering", species.getDefaultWateringDaysInt())
                putExtra("species_description", species.description)
                putExtra("species_light", species.lightNeeds)
                putExtra("species_humidity", species.humidity)
                putExtra("species_difficulty", species.difficulty)
                putExtra("species_photo", species.samplePhotoUrl)
            }
            setResult(RESULT_OK, resultIntent)
            finish()
        }
        rvSpecies.adapter = adapter
    }

    private fun loadSpecies() {
        repository.getAllSpecies { species ->
            speciesList.clear()
            speciesList.addAll(species)
            adapter.updateList(speciesList)
        }
    }
}