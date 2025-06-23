package com.example.tolets.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.tolets.R

class ToiletAdapter(
    private val toiletList: List<Pair<String, String>>, // Pair<id, location>
    private val onEditClicked: (String, String) -> Unit,
    private val onDeleteClicked: (String) -> Unit
) : RecyclerView.Adapter<ToiletAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.txtToiletTitle)
        val location: TextView = view.findViewById(R.id.txtToiletLocation)
        val btnEdit: ImageView = view.findViewById(R.id.btnEdit)
        val btnDelete: ImageView = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_toilet, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = toiletList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val (id, location) = toiletList[position]
        holder.title.text = "Toilet"
        holder.location.text = location

        holder.btnEdit.setOnClickListener { onEditClicked(id, location) }
        holder.btnDelete.setOnClickListener { onDeleteClicked(id) }
    }
}

