package com.example.tolets.Adapter

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.tolets.Model.DataRiwayat
import com.example.tolets.databinding.ItemRiwayatBinding
import com.google.firebase.database.FirebaseDatabase

class AdapterDetailRiwayat(
    private val riwayatList: List<DataRiwayat>,
    private val onItemDeleted: () -> Unit
) : RecyclerView.Adapter<AdapterDetailRiwayat.ViewHolder>() {

    inner class ViewHolder(val binding: ItemRiwayatBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(riwayat: DataRiwayat) {
            // Pisahkan tanggal dan waktu
            val parts = riwayat.tanggal.split(" ")
            val tanggalOnly = parts.getOrNull(0) ?: "-"
            val waktuOnly = parts.getOrNull(1) ?: "-"

            binding.txtTanggal.text = "Tanggal: $tanggalOnly"
            binding.txtWaktu.text = "Waktu: $waktuOnly"
            binding.txtLokasi.text = "Toilet ${riwayat.lokasi}"
            binding.txtKeterangan.text = "${riwayat.keterangan}"
            binding.txtSuhu.text = "Suhu: ${riwayat.temperature} °C"
            binding.txtKelembaban.text = "Kelembapan: ${riwayat.humidity} %"
            binding.txtGas.text = "Gas Amonia: ${riwayat.airQuality} ppm"

            binding.ivDelete.setOnClickListener {
                AlertDialog.Builder(binding.root.context).apply {
                    setTitle("Konfirmasi Hapus")
                    setMessage("Apakah Anda yakin ingin menghapus data ini?")
                    setPositiveButton("Hapus") { _, _ ->
                        val key = riwayat.key ?: return@setPositiveButton
                        FirebaseDatabase.getInstance().reference
                            .child("riwayat").child(key)
                            .removeValue()
                            .addOnSuccessListener { onItemDeleted() }
                    }
                    setNegativeButton("Batal", null)
                    show()
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRiwayatBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = riwayatList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(riwayatList[position])
    }
}
