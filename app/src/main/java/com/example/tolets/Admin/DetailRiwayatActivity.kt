package com.example.tolets.Admin

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tolets.Adapter.AdapterDetailRiwayat
import com.example.tolets.Auth.LoginActivity
import com.example.tolets.HistoryActivity
import com.example.tolets.Model.DataRiwayat
import com.example.tolets.R
import com.example.tolets.databinding.ActivityDetailRiwayatBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class DetailRiwayatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailRiwayatBinding
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var adapter: AdapterDetailRiwayat
    private val riwayatList = mutableListOf<DataRiwayat>()
    private lateinit var uid: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailRiwayatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        uid = intent.getStringExtra("uid") ?: return

        database = FirebaseDatabase.getInstance().reference
        adapter = AdapterDetailRiwayat(riwayatList) {
            loadRiwayatByUid(uid) // refresh after delete
        }

        binding.rvRiwayat.layoutManager = LinearLayoutManager(this)
        binding.rvRiwayat.adapter = adapter

        loadRiwayatByUid(uid)

        binding.btnDeleteAll.setOnClickListener {
            showDeleteAllDialog()
        }

        binding.btnMenu.selectedItemId = R.id.menu_history
        binding.btnMenu.setOnNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_home -> {
                    startActivity(Intent(this, AdminActivity::class.java))
                    true
                }
                R.id.menu_history -> {
                    startActivity(Intent(this, HistoryActivity::class.java))
                    true
                }
                R.id.menu_logout -> {
                    logoutUser()
                    true
                }
                else -> false
            }
        }

        binding.btnAmbangBatas.setOnClickListener {
            showAmbangBatasDialog()
        }
    }

    private fun showAmbangBatasDialog() {
        val message = """
        Ambang Batas Sensor:
        
        • Suhu (temperature): ≥ 35°C → Panas Berlebih
        • Kelembapan (humidity): ≥ 80% → Lembab Tinggi
        • Kualitas Udara (airQuality): ≥ 200 → Tidak Sehat
        
        Data di atas digunakan sebagai referensi deteksi kondisi kritis.
    """.trimIndent()

        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Kondisi Ambang Batas")
        builder.setMessage(message)
        builder.setPositiveButton("Tutup", null)
        builder.show()
    }

    private fun loadRiwayatByUid(uid: String) {
        database.child("riwayat").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                riwayatList.clear()
                for (item in snapshot.children) {
                    val itemUid = item.child("uid").value.toString()
                    if (itemUid == uid) {
                        val riwayat = item.getValue(DataRiwayat::class.java)
                        if (riwayat != null) {
                            riwayatList.add(riwayat.copy(key = item.key ?: ""))
                        }
                    }
                }

                // Tampilkan atau sembunyikan teks kosong
                if (riwayatList.isEmpty()) {
                    binding.tvKosong.visibility = View.VISIBLE
                    binding.rvRiwayat.visibility = View.GONE
                } else {
                    binding.tvKosong.visibility = View.GONE
                    binding.rvRiwayat.visibility = View.VISIBLE
                }

                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun showDeleteAllDialog() {
        AlertDialog.Builder(this).apply {
            setTitle("Konfirmasi Hapus Semua")
            setMessage("Apakah Anda yakin ingin menghapus semua riwayat untuk pengguna ini?")
            setPositiveButton("Hapus Semua") { _, _ ->
                deleteAllByUid(uid)
            }
            setNegativeButton("Batal", null)
            show()
        }
    }

    private fun deleteAllByUid(uid: String) {
        database.child("riwayat").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (item in snapshot.children) {
                    val itemUid = item.child("uid").value.toString()
                    if (itemUid == uid) {
                        database.child("riwayat").child(item.key!!).removeValue()
                    }
                }
                loadRiwayatByUid(uid)
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun logoutUser() {
        auth.signOut()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}
