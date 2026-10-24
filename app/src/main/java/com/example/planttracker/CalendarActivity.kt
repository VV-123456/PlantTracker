package com.example.planttracker

import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class CalendarActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var tvMonth: TextView
    private lateinit var btnPrev: Button
    private lateinit var btnNext: Button
    private lateinit var btnBack: ImageButton
    private lateinit var calendarGrid: GridLayout
    private lateinit var recyclerViewPlants: RecyclerView
    private lateinit var adapter: PlantAdapter
    private val plantsList = mutableListOf<Plant>()
    private val plantsForSelectedDate = mutableListOf<Plant>()

    private var calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("MMMM yyyy", Locale("ru"))
    private val wateringDates = mutableMapOf<String, MutableList<Plant>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calendar)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        initViews()
        setupListeners()
        loadPlants()
    }

    private fun initViews() {
        tvMonth = findViewById(R.id.tvMonth)
        btnPrev = findViewById(R.id.btnPrev)
        btnNext = findViewById(R.id.btnNext)
        btnBack = findViewById(R.id.btnBack)
        calendarGrid = findViewById(R.id.calendarGrid)
        recyclerViewPlants = findViewById(R.id.recyclerViewPlants)

        recyclerViewPlants.layoutManager = LinearLayoutManager(this)
        adapter = PlantAdapter(
            plantsForSelectedDate,
            onWaterClick = {},
            onEditClick = {},
            onDeleteClick = {},
            onFavoriteClick = {}
        )
        recyclerViewPlants.adapter = adapter
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { finish() }

        btnPrev.setOnClickListener {
            calendar.add(Calendar.MONTH, -1)
            updateCalendar()
        }

        btnNext.setOnClickListener {
            calendar.add(Calendar.MONTH, 1)
            updateCalendar()
        }
    }

    private fun loadPlants() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("plants")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Toast.makeText(this, "Ошибка загрузки: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                plantsList.clear()
                snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Plant::class.java)?.copy(id = doc.id)
                }?.let { plantsList.addAll(it) }

                calculateWateringDates()
                updateCalendar()
            }
    }

    private fun calculateWateringDates() {
        wateringDates.clear()

        for (plant in plantsList) {
            val nextWatering = plant.nextWateringDate
            if (nextWatering > 0) {
                val dateKey = getDateKey(nextWatering)
                if (!wateringDates.containsKey(dateKey)) {
                    wateringDates[dateKey] = mutableListOf()
                }
                wateringDates[dateKey]?.add(plant)
            }
        }

        Log.d("Calendar", "Найдено дней с поливом: ${wateringDates.size}")
        for ((date, plants) in wateringDates) {
            Log.d("Calendar", "$date: ${plants.size} растений")
        }
    }

    private fun getDateKey(timestamp: Long): String {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
    }

    private fun updateCalendar() {
        tvMonth.text = dateFormat.format(calendar.time)
        calendarGrid.removeAllViews()
        calendarGrid.rowCount = 6
        calendarGrid.columnCount = 7

        val dayNames = arrayOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
        for (i in 0 until 7) {
            val header = TextView(this)
            header.text = dayNames[i]
            header.gravity = android.view.Gravity.CENTER
            header.setTextColor(resources.getColor(R.color.emerald))
            header.textSize = 12f
            header.setPadding(4, 8, 4, 8)

            val params = GridLayout.LayoutParams()
            params.width = 0
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            header.layoutParams = params
            calendarGrid.addView(header)
        }

        val firstDay = calendar.clone() as Calendar
        firstDay.set(Calendar.DAY_OF_MONTH, 1)
        var firstDayOfWeek = firstDay.get(Calendar.DAY_OF_WEEK) - 2
        if (firstDayOfWeek < 0) firstDayOfWeek = 6

        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val today = Calendar.getInstance()

        var day = 1
        for (i in 0 until 42) {
            val cell = TextView(this)
            cell.setPadding(8, 12, 8, 12)
            cell.gravity = android.view.Gravity.CENTER
            cell.textSize = 14f

            if (i >= firstDayOfWeek && day <= daysInMonth) {
                cell.text = day.toString()

                val dateKey = String.format("%d-%02d-%02d",
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH) + 1,
                    day)

                if (wateringDates.containsKey(dateKey)) {
                    cell.setBackgroundColor(resources.getColor(R.color.wave_crest))
                    cell.setTextColor(resources.getColor(R.color.white))
                    cell.setOnClickListener {
                        showPlantsForDate(dateKey)
                    }
                } else {
                    cell.setBackgroundColor(resources.getColor(android.R.color.transparent))
                    cell.setTextColor(resources.getColor(R.color.black))
                }
                if (today.get(Calendar.YEAR) == calendar.get(Calendar.YEAR) &&
                    today.get(Calendar.MONTH) == calendar.get(Calendar.MONTH) &&
                    today.get(Calendar.DAY_OF_MONTH) == day) {
                    cell.setBackgroundColor(resources.getColor(R.color.emerald))
                    cell.setTextColor(resources.getColor(R.color.white))
                }
                day++
            } else {
                cell.text = ""
                cell.setBackgroundColor(resources.getColor(android.R.color.transparent))
            }

            val params = GridLayout.LayoutParams()
            params.width = 0
            params.height = GridLayout.LayoutParams.WRAP_CONTENT
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            params.setMargins(2, 2, 2, 2)
            cell.layoutParams = params
            calendarGrid.addView(cell)
        }
    }

    private fun showPlantsForDate(dateKey: String) {
        val plants = wateringDates[dateKey]
        if (plants == null || plants.isEmpty()) {
            Toast.makeText(this, "Нет растений для полива в этот день", Toast.LENGTH_SHORT).show()
            return
        }

        plantsForSelectedDate.clear()
        plantsForSelectedDate.addAll(plants)
        adapter.updateList(plantsForSelectedDate)

        Toast.makeText(this, "🌿 ${plants.size} растений требуют полива", Toast.LENGTH_SHORT).show()
    }
}