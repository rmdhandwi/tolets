package com.example.tolets.Admin

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.example.tolets.Adapter.ToiletAdapter
import com.example.tolets.R
import com.example.tolets.databinding.ActivityAddBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class AddActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddBinding
    private lateinit var database: DatabaseReference
    private lateinit var toiletAdapter: ToiletAdapter

    private var editMode = false
    private var editToiletId: String? = null
    private val toiletList = mutableListOf<Pair<String, String>>() // Pair<id, location>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Window insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        database = FirebaseDatabase.getInstance().reference

        setupRecyclerView()
        setupListeners()
        getFullName()
    }

    private fun setupRecyclerView() {
        toiletAdapter = ToiletAdapter(
            toiletList,
            onEditClicked = { id, location ->
                editMode = true
                editToiletId = id
                binding.edtLocation.setText(location)
                binding.addToiletButton.text = "Update"
            },
            onDeleteClicked = { id ->
                AlertDialog.Builder(this)
                    .setTitle("Hapus Toilet")
                    .setMessage("Yakin ingin menghapus data toilet ini?")
                    .setPositiveButton("Ya") { _, _ -> deleteToilet(id) }
                    .setNegativeButton("Batal", null)
                    .show()
            }
        )
        binding.recyclerViewToilet.layoutManager = GridLayoutManager(this, 1)
        binding.recyclerViewToilet.adapter = toiletAdapter
        binding.recyclerViewToilet.visibility = View.GONE
    }

    private fun setupListeners() {
        binding.addToiletButton.setOnClickListener {
            val location = binding.edtLocation.text.toString().trim()
            if (location.isEmpty()) {
                Toast.makeText(this, "Silakan masukkan lokasi", Toast.LENGTH_SHORT).show()
            } else {
                if (editMode && editToiletId != null) {
                    updateToilet(editToiletId!!, location)
                } else {
                    addToilet(location)
                }
            }
        }

        binding.showToiletListButton.setOnClickListener {
            if (binding.recyclerViewToilet.visibility == View.GONE) {
                fetchToiletList()
            } else {
                binding.recyclerViewToilet.visibility = View.GONE
                binding.showToiletListButton.text = "Lihat Daftar Toilet"
            }
        }

        binding.backButton.setOnClickListener {
            startActivity(Intent(this, AdminActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            finish()
        }
    }

    // Fungsi untuk menambahkan data toilet baru dan sensor yang terkait ke Firebase Realtime Database
    private fun addToilet(location: String) {
        val toiletRef = database.child("toilet")

        // Ambil semua data toilet terlebih dahulu
        toiletRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val inputLocation = location.trim().lowercase()

                // Cek apakah lokasi sudah terdaftar
                for (child in snapshot.children) {
                    val existingLocation = child.child("location").getValue(String::class.java)?.trim()?.lowercase()
                    if (existingLocation == inputLocation) {
                        Toast.makeText(this@AddActivity, "Lokasi toilet sudah terdaftar", Toast.LENGTH_SHORT).show()
                        return  // Hentikan proses karena lokasi duplikat
                    }
                }

                // Jika lokasi belum ada, lanjutkan proses tambah toilet
                val newToiletId = toiletRef.push().key ?: return
                val data = mapOf("location" to location)

                toiletRef.child(newToiletId).setValue(data).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val sensorRef = database.child("sensor")

                        sensorRef.addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                var lastId = 0
                                for (child in snapshot.children) {
                                    val key = child.key?.toIntOrNull()
                                    if (key != null && key > lastId) lastId = key
                                }

                                val newSensorId = (lastId + 1).toString()
                                val sensorData = mapOf(
                                    "temperature" to 0,
                                    "humidity" to 0,
                                    "pir" to 0,
                                    "airQuality" to 0,
                                    "relay" to 0,
                                    "toiletId" to newToiletId
                                )

                                sensorRef.child(newSensorId).setValue(sensorData).addOnCompleteListener { sensorTask ->
                                    if (sensorTask.isSuccessful) {
                                        Toast.makeText(this@AddActivity, "Toilet & sensor berhasil ditambahkan", Toast.LENGTH_SHORT).show()
                                        binding.edtLocation.text?.clear()
                                        fetchToiletList()
                                    } else {
                                        Toast.makeText(this@AddActivity, "Toilet ditambah, tapi gagal buat sensor", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Toast.makeText(this@AddActivity, "Gagal membaca data sensor", Toast.LENGTH_SHORT).show()
                            }
                        })
                    } else {
                        Toast.makeText(this@AddActivity, "Gagal menambahkan toilet", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@AddActivity, "Gagal memeriksa lokasi toilet", Toast.LENGTH_SHORT).show()
            }
        })
    }


    private fun updateToilet(id: String, newLocation: String) {
        val toiletRef = database.child("toilet")

        toiletRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val inputLocation = newLocation.trim().lowercase()

                for (child in snapshot.children) {
                    val existingId = child.key
                    val existingLocation = child.child("location").getValue(String::class.java)?.trim()?.lowercase()

                    // Cek apakah lokasi sudah ada di toilet lain (bukan dirinya sendiri)
                    if (existingId != id && existingLocation == inputLocation) {
                        Toast.makeText(this@AddActivity, "Lokasi toilet sudah digunakan", Toast.LENGTH_SHORT).show()
                        return  // Hentikan update
                    }
                }

                // Jika valid, update lokasi
                toiletRef.child(id).child("location").setValue(newLocation).addOnCompleteListener {
                    if (it.isSuccessful) {
                        Toast.makeText(this@AddActivity, "Data berhasil diperbarui", Toast.LENGTH_SHORT).show()
                        resetEditState()
                        fetchToiletList()
                    } else {
                        Toast.makeText(this@AddActivity, "Gagal memperbarui data", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@AddActivity, "Gagal memeriksa data toilet", Toast.LENGTH_SHORT).show()
            }
        })
    }


    private fun deleteToilet(id: String) {
        // Hapus data toilet
        database.child("toilet").child(id).removeValue().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // Setelah toilet dihapus, cari dan hapus sensor terkait
                val sensorRef = database.child("sensor")
                sensorRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        for (child in snapshot.children) {
                            val sensorToiletId = child.child("toiletId").getValue(String::class.java)
                            if (sensorToiletId == id) {
                                // Hapus sensor yang terhubung dengan toiletId ini
                                child.ref.removeValue()
                            }
                        }

                        // Tampilkan pesan sukses setelah semua penghapusan
                        Toast.makeText(this@AddActivity, "Data toilet & sensor terkait berhasil dihapus", Toast.LENGTH_SHORT).show()
                        fetchToiletList()
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(this@AddActivity, "Gagal menghapus sensor terkait", Toast.LENGTH_SHORT).show()
                    }
                })
            } else {
                Toast.makeText(this, "Gagal menghapus data toilet", Toast.LENGTH_SHORT).show()
            }
        }
    }


    // Fungsi untuk memuat dan menampilkan daftar toilet dari Firebase Realtime Database
    private fun fetchToiletList() {
        // Ambil data dari node "toilet"
        database.child("toilet").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                toiletList.clear()  // Bersihkan daftar sebelum menambahkan data baru

                // Iterasi semua data toilet
                for (child in snapshot.children) {
                    val id = child.key ?: continue  // Ambil key sebagai ID
                    val location = child.child("location").value?.toString() ?: "Tidak diketahui"  // Ambil lokasi atau beri nilai default
                    toiletList.add(Pair(id, location))  // Tambahkan ke list sebagai pasangan ID dan lokasi
                }

                // Beri tahu adapter bahwa data sudah berubah
                toiletAdapter.notifyDataSetChanged()

                // Tampilkan RecyclerView & ubah teks tombol
                binding.recyclerViewToilet.visibility = View.VISIBLE
                binding.showToiletListButton.text = "Sembunyikan Daftar Toilet"
            }

            override fun onCancelled(error: DatabaseError) {
                // Gagal memuat data dari database
                Toast.makeText(this@AddActivity, "Gagal memuat daftar toilet", Toast.LENGTH_SHORT).show()
            }
        })
    }


    private fun resetEditState() {
        editMode = false
        editToiletId = null
        binding.edtLocation.text?.clear()
        binding.addToiletButton.text = "Simpan"
    }

    private fun getFullName() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val uid = currentUser.uid
            database.child("users").child(uid).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val fullName = snapshot.child("fullName").value?.toString() ?: "Tidak dikenal"
                    binding.txtuser.text = fullName
                }

                override fun onCancelled(error: DatabaseError) {
                    binding.txtuser.text = "Gagal memuat nama"
                }
            })
        } else {
            binding.txtuser.text = "Tidak login"
        }
    }
}
