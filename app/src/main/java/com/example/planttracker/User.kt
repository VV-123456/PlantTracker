package com.example.planttracker

data class User(
    val username: String = "",
    val email: String = "",
    val createdAt: Long = System.currentTimeMillis()
)