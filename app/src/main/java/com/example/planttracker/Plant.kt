package com.example.planttracker

data class Plant(
    val id: String = "",
    val name: String = "",
    val species: String = "",
    val wateringIntervalDays: Int = 7,
    val lastWateringDate: Long = System.currentTimeMillis(),
    val nextWateringDate: Long = System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000L),
    val userId: String = "",
    val photoUrl: String = "",
    val isFavorite: Boolean = false,
    val mood: String = "happy"
) {
    val needsWatering: Boolean
        get() = nextWateringDate <= System.currentTimeMillis()

    val daysOverdue: Int
        get() = ((System.currentTimeMillis() - nextWateringDate) / (24 * 60 * 60 * 1000)).toInt()

    val moodText: String
        get() = when {
            daysOverdue > 5 -> "Умирает от жажды!"
            daysOverdue > 2 -> "Очень хочет пить!"
            needsWatering -> "Хочу пить!"
            else -> "Счастлив!"
        }

    val moodMessage: String
        get() = when {
            daysOverdue > 5 -> "Пожалуйста, полей меня! Я засыхаю"
            daysOverdue > 2 -> "Срочно нужен полив!"
            needsWatering -> "Пора меня полить"
            else -> "Спасибо за заботу!"
        }
}