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
import java.text.SimpleDateFormat
import java.text.ParseException
import java.util.Date
import java.util.Locale

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
        • Kelembapan (humidity): ≥ 65% → Lembab Tinggi
        • Kualitas Udara (airQuality): ≥ 25 ppm → Tidak Sehat
        
        Data di atas digunakan sebagai referensi deteksi kondisi kritis.
    """.trimIndent()

        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Kondisi Ambang Batas")
        builder.setMessage(message)
        builder.setPositiveButton("Tutup", null)
        builder.show()
    }

    private fun parseTanggal(tanggal: String): Date? {
        return try {
            val format = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault())
            format.parse(tanggal)
        } catch (e: ParseException) {
            null
        }
    }



    private fun loadRiwayat() {
        val uid = auth.currentUser?.uid ?: return

        database.child("riwayat").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                listRiwayat.clear()

                for (item in snapshot.children) {
                    val dataUid = item.child("uid").value.toString()

                    if (dataUid == uid) {
                        val riwayat = item.getValue(DataRiwayat::class.java)
                        if (riwayat != null) {
                            listRiwayat.add(riwayat.copy(key = item.key ?: ""))
                        }
                    }
                }

                // 🔽 Urutkan berdasarkan tanggal terbaru (parsing dari string)
                listRiwayat.sortByDescending { parseTanggal(it.tanggal)?.time ?: 0L }

                if (listRiwayat.isEmpty()) {
                    binding.tvKosong.visibility = View.VISIBLE
                    binding.rvRiwayat.visibility = View.GONE
                    binding.btnDeleteAll.visibility = View.GONE
                } else {
                    binding.tvKosong.visibility = View.GONE
                    binding.rvRiwayat.visibility = View.VISIBLE
                    binding.btnDeleteAll.visibility = View.VISIBLE
                }

                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
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
