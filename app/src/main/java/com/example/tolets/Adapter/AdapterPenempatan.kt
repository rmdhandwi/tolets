package com.example.tolets.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.tolets.Model.DataPenempatan
import com.example.tolets.R

class AdapterPenempatan(
    private val list: List<DataPenempatan>,
    private val petugasMap: Map<String, String>,
    private val lokasiMap: Map<String, String>,
    private val onEdit: (DataPenempatan) -> Unit,
    private val onDelete: (DataPenempatan) -> Unit
) : RecyclerView.Adapter<AdapterPenempatan.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtNama: TextView = view.findViewById(R.id.txtNama)
        val txtLokasi: TextView = view.findViewById(R.id.txtLokasi)
        val btnEdit: ImageView = view.findViewById(R.id.btnEdit)
        val btnDelete: ImageView = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_penempatan, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        val petugasName = petugasMap[item.petugasUid] ?: "Tidak Diketahui"
        val lokasiName = lokasiMap[item.lokasiId] ?: "Tidak Diketahui"

        holder.txtNama.text = petugasName
        holder.txtLokasi.text = "Lokasi: $lokasiName"

        holder.btnEdit.setOnClickListener { onEdit(item) }
        holder.btnDelete.setOnClickListener { onDelete(item) }
    }
}
