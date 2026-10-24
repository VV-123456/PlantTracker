package com.example.planttracker

data class PlantSpecies(
    val id: String = "",
    val name: String = "",
    val scientificName: String = "",
    val defaultWateringDays: String = "7",
    val description: String = "",
    val lightNeeds: String = "",
    val humidity: String = "",
    val difficulty: String = "",
    val samplePhotoUrl: String = ""
) {
    fun getDefaultWateringDaysInt(): Int = defaultWateringDays.toIntOrNull() ?: 7
}