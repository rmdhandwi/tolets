package com.example.tolets

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tolets.Adapter.RiwayatAdapter
import com.example.tolets.Auth.LoginActivity
import com.example.tolets.Model.DataRiwayat
import com.example.tolets.databinding.ActivityHistoryBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class HistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private val listRiwayat = mutableListOf<DataRiwayat>()
    private lateinit var adapter: RiwayatAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        adapter = RiwayatAdapter(listRiwayat) { key -> deleteSingleRiwayat(key) }
        binding.rvRiwayat.layoutManager = LinearLayoutManager(this)
        binding.rvRiwayat.adapter = adapter

        binding.btnDeleteAll.setOnClickListener {
            deleteAllRiwayat()
        }

        loadRiwayat()

        binding.btnMenu.selectedItemId = R.id.menu_history
        binding.btnMenu.setOnNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    true
                }
                R.id.menu_history -> {
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


    private fun loadRiwayat() {
        // Ambil UID pengguna yang sedang login
        val uid = auth.currentUser?.uid ?: return

        // Menambahkan listener ke node "riwayat" di Firebase Realtime Database
        database.child("riwayat").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Bersihkan listRiwayat agar tidak terjadi duplikasi data
                listRiwayat.clear()

                // Iterasi setiap item di node "riwayat"
                for (item in snapshot.children) {
                    // Ambil nilai UID dari item
                    val dataUid = item.child("uid").value.toString()

                    // Cocokkan data dengan UID pengguna saat ini
                    if (dataUid == uid) {
                        // Konversi data snapshot menjadi objek DataRiwayat
                        val riwayat = item.getValue(DataRiwayat::class.java)

                        // Jika data tidak null, tambahkan ke list dengan menyimpan key-nya
                        if (riwayat != null) {
                            listRiwayat.add(riwayat.copy(key = item.key ?: ""))
                        }
                    }
                }

                // Tampilkan tampilan kosong jika tidak ada riwayat
                if (listRiwayat.isEmpty()) {
                    binding.tvKosong.visibility = View.VISIBLE      // Tampilkan teks "Kosong"
                    binding.rvRiwayat.visibility = View.GONE        // Sembunyikan RecyclerView
                    binding.btnDeleteAll.visibility = View.GONE     // Sembunyikan tombol "Hapus Semua"
                } else {
                    binding.tvKosong.visibility = View.GONE         // Sembunyikan teks "Kosong"
                    binding.rvRiwayat.visibility = View.VISIBLE     // Tampilkan RecyclerView
                    binding.btnDeleteAll.visibility = View.VISIBLE  // Tampilkan tombol "Hapus Semua"
                }

                // Beri tahu adapter bahwa data telah diperbarui
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                // Callback ini dipanggil jika permintaan ke database dibatalkan atau gagal
                // Dapat ditambahkan log error atau pesan Toast di sini jika diperlukan
            }
        })
    }


    // Fungsi untuk menghapus satu item riwayat berdasarkan key
    private fun deleteSingleRiwayat(key: String) {
        // Tampilkan dialog konfirmasi kepada pengguna
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Konfirmasi")
        builder.setMessage("Apakah Anda yakin ingin menghapus riwayat ini?")

        // Jika pengguna memilih "Hapus"
        builder.setPositiveButton("Hapus") { _, _ ->
            // Hapus data dari database berdasarkan key
            database.child("riwayat").child(key).removeValue().addOnSuccessListener {
                Toast.makeText(this, "Riwayat dihapus", Toast.LENGTH_SHORT).show()
                loadRiwayat() // Muat ulang data riwayat setelah penghapusan
            }
        }

        // Jika pengguna memilih "Batal"
        builder.setNegativeButton("Batal", null)

        // Tampilkan dialog
        builder.show()
    }

    // Fungsi untuk menghapus semua riwayat milik pengguna yang sedang login
    private fun deleteAllRiwayat() {
        // Tampilkan dialog konfirmasi kepada pengguna
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Konfirmasi")
        builder.setMessage("Apakah Anda yakin ingin menghapus semua riwayat?")

        // Jika pengguna memilih "Hapus Semua"
        builder.setPositiveButton("Hapus Semua") { _, _ ->
            val uid = auth.currentUser?.uid ?: return@setPositiveButton

            // Ambil semua data dari node "riwayat"
            database.child("riwayat").addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    // Iterasi setiap item riwayat
                    for (item in snapshot.children) {
                        // Hapus hanya riwayat yang memiliki UID sesuai dengan pengguna saat ini
                        if (item.child("uid").value == uid) {
                            item.ref.removeValue()
                        }
                    }
                    Toast.makeText(this@HistoryActivity, "Semua riwayat dihapus", Toast.LENGTH_SHORT).show()
                    loadRiwayat() // Muat ulang data riwayat setelah penghapusan
                }

                override fun onCancelled(error: DatabaseError) {
                    // Penanganan jika akses ke database gagal
                }
            })
        }

        // Jika pengguna memilih "Batal"
        builder.setNegativeButton("Batal", null)

        // Tampilkan dialog
        builder.show()
    }



    private fun logoutUser() {
        auth.signOut()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}
