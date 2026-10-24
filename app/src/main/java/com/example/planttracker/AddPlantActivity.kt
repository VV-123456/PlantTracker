package com.example.planttracker

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import java.io.File
import java.io.FileOutputStream

class AddPlantActivity : AppCompatActivity() {

    private lateinit var etName: EditText
    private lateinit var etSpecies: EditText
    private lateinit var seekBarWatering: SeekBar
    private lateinit var tvWateringValue: TextView
    private lateinit var btnSave: Button
    private lateinit var ivPhoto: ImageView
    private lateinit var btnSelectPhoto: Button
    private lateinit var btnDelete: Button
    private lateinit var btnSelectSpecies: Button
    private lateinit var btnBack: ImageButton

    private lateinit var repository: PlantRepository
    private var selectedImageUri: Uri? = null
    private var plantId: String? = null
    private var currentPhotoPath: String = ""

    companion object {
        private const val PICK_IMAGE_REQUEST = 100
        private const val SELECT_SPECIES_REQUEST = 200
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_plant)

        repository = PlantRepository()

        initViews()
        setupListeners()

        plantId = intent.getStringExtra("plant_id")
        if (plantId != null) {
            title = "Редактировать растение"
            btnDelete.visibility = android.view.View.VISIBLE
            loadPlantData()
        } else {
            title = "Новое растение"
            btnDelete.visibility = android.view.View.GONE
        }
    }

    private fun initViews() {
        etName = findViewById(R.id.etName)
        etSpecies = findViewById(R.id.etSpecies)
        seekBarWatering = findViewById(R.id.seekBarWatering)
        tvWateringValue = findViewById(R.id.tvWateringValue)
        btnSave = findViewById(R.id.btnSave)
        ivPhoto = findViewById(R.id.ivPhoto)
        btnSelectPhoto = findViewById(R.id.btnSelectPhoto)
        btnDelete = findViewById(R.id.btnDelete)
        btnSelectSpecies = findViewById(R.id.btnSelectSpecies)
        btnBack = findViewById(R.id.btnBack)
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        seekBarWatering.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvWateringValue.text = "$progress дней"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        btnSelectPhoto.setOnClickListener {
            selectPhoto()
        }

        btnSelectSpecies.setOnClickListener {
            val intent = Intent(this, SpeciesListActivity::class.java)
            startActivityForResult(intent, SELECT_SPECIES_REQUEST)
        }

        btnSave.setOnClickListener {
            savePlant()
        }

        btnDelete.setOnClickListener {
            deletePlant()
        }
    }

    private fun selectPhoto() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data?.data != null) {
            selectedImageUri = data.data
            ivPhoto.setImageURI(selectedImageUri)
        }

        if (requestCode == SELECT_SPECIES_REQUEST && resultCode == RESULT_OK && data != null) {
            val speciesName = data.getStringExtra("species_name") ?: ""
            val speciesScientific = data.getStringExtra("species_scientific") ?: ""
            val defaultWatering = data.getIntExtra("species_default_watering", 7)

            etName.setText(speciesName)
            etSpecies.setText(speciesScientific)
            seekBarWatering.progress = defaultWatering
            tvWateringValue.text = "$defaultWatering дней"

            Toast.makeText(this, "Выбран вид: $speciesName", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadPlantData() {
        etName.setText(intent.getStringExtra("plant_name"))
        etSpecies.setText(intent.getStringExtra("plant_species"))
        val interval = intent.getIntExtra("plant_interval", 7)
        seekBarWatering.progress = interval
        tvWateringValue.text = "$interval дней"
        currentPhotoPath = intent.getStringExtra("plant_photo_path") ?: ""

        if (currentPhotoPath.isNotEmpty()) {
            val file = File(currentPhotoPath)
            if (file.exists()) {
                Glide.with(this)
                    .load(file)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .into(ivPhoto)
            }
        }
    }

    private fun savePhotoLocally(imageUri: Uri, plantId: String): String {
        val appDir = File(filesDir, "plant_photos")
        if (!appDir.exists()) {
            appDir.mkdirs()
        }

        val fileName = "plant_$plantId.jpg"
        val file = File(appDir, fileName)

        try {
            val inputStream = contentResolver.openInputStream(imageUri)
            val outputStream = FileOutputStream(file)
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()
            return file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        }
    }

    private fun savePlant() {
        val name = etName.text.toString().trim()
        if (name.isEmpty()) {
            etName.error = "Введите название"
            return
        }

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "Ошибка: пользователь не найден", Toast.LENGTH_SHORT).show()
            return
        }

        btnSave.isEnabled = false

        if (plantId == null) {
            val plant = Plant(
                name = name,
                species = etSpecies.text.toString().trim(),
                wateringIntervalDays = seekBarWatering.progress,
                userId = userId,
                photoUrl = ""
            )

            repository.addPlant(plant) { newId ->
                if (newId != null) {
                    val localPath = if (selectedImageUri != null) {
                        savePhotoLocally(selectedImageUri!!, newId)
                    } else {
                        ""
                    }
                    repository.updatePhotoUrl(newId, localPath) { _ ->
                        finish()
                    }
                } else {
                    btnSave.isEnabled = true
                    Toast.makeText(this, "Ошибка при сохранении", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            if (selectedImageUri != null) {
                val localPath = savePhotoLocally(selectedImageUri!!, plantId!!)
                currentPhotoPath = localPath
            }

            val plant = Plant(
                id = plantId!!,
                name = name,
                species = etSpecies.text.toString().trim(),
                wateringIntervalDays = seekBarWatering.progress,
                userId = userId,
                photoUrl = currentPhotoPath,
                isFavorite = intent.getBooleanExtra("plant_is_favorite", false)
            )

            repository.updatePlant(plant) { success ->
                btnSave.isEnabled = true
                if (success) {
                    finish()
                } else {
                    Toast.makeText(this, "Ошибка при обновлении", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun deletePlant() {
        AlertDialog.Builder(this)
            .setTitle("Удалить растение")
            .setMessage("Вы уверены?")
            .setPositiveButton("Да") { _, _ ->
                plantId?.let { id ->
                    val photoFile = File(currentPhotoPath)
                    if (photoFile.exists()) {
                        photoFile.delete()
                    }

                    repository.deletePlant(id) { success ->
                        if (success) {
                            Toast.makeText(this, "Растение удалено", Toast.LENGTH_SHORT).show()
                            finish()
                        } else {
                            Toast.makeText(this, "Ошибка при удалении", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton("Нет", null)
            .show()
    }
}