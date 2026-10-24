package com.example.planttracker

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.io.File

class PlantAdapter(
    private var plants: List<Plant>,
    private val onWaterClick: (Plant) -> Unit,
    private val onEditClick: (Plant) -> Unit,
    private val onDeleteClick: (Plant) -> Unit,
    private val onFavoriteClick: (Plant) -> Unit
) : RecyclerView.Adapter<PlantAdapter.PlantViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlantViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_plant, parent, false)
        return PlantViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlantViewHolder, position: Int) {
        holder.bind(plants[position])
    }

    override fun getItemCount(): Int = plants.size
    fun updateList(newList: List<Plant>) {
        plants = newList
        notifyDataSetChanged()
    }

    inner class PlantViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivPhoto: ImageView = itemView.findViewById(R.id.ivPhoto)
        private val tvName: TextView = itemView.findViewById(R.id.tvName)
        private val tvSpecies: TextView = itemView.findViewById(R.id.tvSpecies)
        private val tvMoodStatus: TextView = itemView.findViewById(R.id.tvMoodStatus)
        private val tvMoodMessage: TextView = itemView.findViewById(R.id.tvMoodMessage)
        private val btnWater: Button = itemView.findViewById(R.id.btnWater)
        private val btnEdit: Button = itemView.findViewById(R.id.btnEdit)
        private val btnDelete: Button = itemView.findViewById(R.id.btnDelete)
        private val btnFavorite: ImageButton = itemView.findViewById(R.id.btnFavorite)
        fun bind(plant: Plant) {
            tvName.text = plant.name
            tvSpecies.text = plant.species

            tvMoodStatus.text = plant.moodText

            if (plant.needsWatering) {
                tvMoodMessage.visibility = View.VISIBLE
                tvMoodMessage.text = plant.moodMessage
            } else {
                tvMoodMessage.visibility = View.GONE
            }
            if (plant.isFavorite) {
                btnFavorite.setImageResource(R.drawable.ic_star_filled)
            } else {
                btnFavorite.setImageResource(R.drawable.ic_star_outline)
            }
            if (plant.photoUrl.isNotEmpty()) {
                val photoFile = File(plant.photoUrl)
                if (photoFile.exists()) {
                    Glide.with(itemView.context)
                        .load(photoFile)
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .into(ivPhoto)
                } else {
                    ivPhoto.setImageResource(android.R.drawable.ic_menu_gallery)
                }
            } else {
                ivPhoto.setImageResource(android.R.drawable.ic_menu_gallery)
            }
            btnWater.setOnClickListener { onWaterClick(plant) }
            btnEdit.setOnClickListener { onEditClick(plant) }
            btnDelete.setOnClickListener { onDeleteClick(plant) }
            btnFavorite.setOnClickListener { onFavoriteClick(plant)}
        }
    }
}
