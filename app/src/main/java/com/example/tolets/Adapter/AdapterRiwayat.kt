package com.example.tolets.Adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.tolets.Model.DataRiwayat
import com.example.tolets.databinding.ItemRiwayatBinding

class RiwayatAdapter(
    private val list: List<DataRiwayat>,
    private val onDeleteClick: (String) -> Unit
) : RecyclerView.Adapter<RiwayatAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemRiwayatBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRiwayatBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount() = list.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val data = list[position]
        with(holder.binding) {
            // Pisahkan tanggal dan waktu
            val parts = data.tanggal.split(" ")
            val tanggalOnly = parts.getOrNull(0) ?: "-"
            val waktuOnly = parts.getOrNull(1) ?: "-"

            txtTanggal.text = "Tanggal: $tanggalOnly"
            txtWaktu.text = "Waktu: $waktuOnly"
            txtLokasi.text = "Toilet ${data.lokasi}"
            txtKeterangan.text = "${data.keterangan}"
            txtSuhu.text = "Suhu: ${data.temperature} °C"
            txtKelembaban.text = "Kelembapan: ${data.humidity} %"
            txtGas.text = "Gas Amonia: ${data.airQuality} ppm"

            ivDelete.setOnClickListener {
                onDeleteClick(data.key)
            }
        }
    }

}
