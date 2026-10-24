package com.example.planttracker

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SpeciesAdapter(
    private var speciesList: List<PlantSpecies>,
    private val onItemClick: (PlantSpecies) -> Unit
) : RecyclerView.Adapter<SpeciesAdapter.SpeciesViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SpeciesViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_2, parent, false)
        return SpeciesViewHolder(view)
    }

    override fun onBindViewHolder(holder: SpeciesViewHolder, position: Int) {
        holder.bind(speciesList[position])
        holder.itemView.setOnClickListener {
            onItemClick(speciesList[position])
        }
    }

    override fun getItemCount(): Int = speciesList.size

    fun updateList(newList: List<PlantSpecies>) {
        speciesList = newList
        notifyDataSetChanged()
    }

    inner class SpeciesViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val text1: TextView = itemView.findViewById(android.R.id.text1)
        private val text2: TextView = itemView.findViewById(android.R.id.text2)

        fun bind(species: PlantSpecies) {
            text1.text = species.name
            val info = "${species.getDefaultWateringDaysInt()} дней • ${species.difficulty}"
            text2.text = info
        }
    }
}