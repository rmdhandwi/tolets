package com.example.tolets.Admin

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tolets.Adapter.AdapterPenempatan
import com.example.tolets.Model.DataPenempatan
import com.example.tolets.R
import com.example.tolets.databinding.ActivityPenempatanBinding
import com.google.firebase.database.*

class PenempatanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPenempatanBinding
    private lateinit var database: DatabaseReference

    private val listAssign = mutableListOf<DataPenempatan>()
    private val petugasMap = mutableMapOf<String, String>()  // uid -> fullName
    private val lokasiMap = mutableMapOf<String, String>()   // id -> location
    private val petugasUidList = mutableListOf<String>()     // Indexing untuk spinner

    private var editMode = false
    private var editId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPenempatanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = FirebaseDatabase.getInstance().reference

        setupRecyclerView()
        loadPetugas()
        loadLokasi()
        loadAssignments()

        binding.btnSimpan.setOnClickListener {
            val petugasPos = binding.spinnerPetugas.selectedItemPosition
            val lokasiPos = binding.spinnerLokasi.selectedItemPosition

            if (petugasPos >= 0 && lokasiPos >= 0) {
                val petugasUid = petugasUidList[petugasPos]
                val lokasiId = lokasiMap.keys.elementAt(lokasiPos)

                if (editMode && editId != null) {
                    updateAssignment(editId!!, petugasUid, lokasiId)
                } else {
                    addAssignment(petugasUid, lokasiId)
                }
            } else {
                Toast.makeText(this, "Semua pilihan harus dipilih", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnBack.setOnClickListener {
            startActivity(Intent(this, AdminActivity::class.java))
        }
    }

    private fun setupRecyclerView() {
        binding.recyclerViewAssign.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewAssign.adapter = AdapterPenempatan(
            listAssign,
            petugasMap,
            lokasiMap,
            onEdit = { assign ->
                editMode = true
                editId = assign.id

                val petugasIndex = petugasUidList.indexOf(assign.petugasUid)
                if (petugasIndex >= 0) binding.spinnerPetugas.setSelection(petugasIndex)

                val lokasiIndex = lokasiMap.keys.toList().indexOf(assign.lokasiId)
                if (lokasiIndex >= 0) binding.spinnerLokasi.setSelection(lokasiIndex)

                binding.btnSimpan.text = "Update"
            },
            onDelete = { assign ->
                AlertDialog.Builder(this)
                    .setTitle("Konfirmasi Hapus")
                    .setMessage("Apakah Anda yakin ingin menghapus data penempatan ini?")
                    .setPositiveButton("Hapus") { _, _ ->
                        database.child("penempatan").child(assign.id!!).removeValue()
                            .addOnSuccessListener {
                                Toast.makeText(this, "Data berhasil dihapus", Toast.LENGTH_SHORT).show()
                                loadAssignments()
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, "Gagal menghapus data", Toast.LENGTH_SHORT).show()
                            }
                    }
                    .setNegativeButton("Batal", null)
                    .show()
            }
        )
    }


    // Fungsi untuk memuat daftar pengguna dengan peran "petugas" dari Firebase dan mengisi Spinner
    private fun loadPetugas() {
        // Query ke node "users" yang role-nya sama dengan "petugas"
        database.child("users").orderByChild("role").equalTo("petugas")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    petugasMap.clear()      // Bersihkan map petugas sebelumnya
                    petugasUidList.clear()  // Bersihkan list UID petugas sebelumnya
                    val namaList = mutableListOf<String>()  // List nama petugas untuk Spinner

                    // Iterasi semua anak yang role-nya "petugas"
                    for (child in snapshot.children) {
                        val uid = child.key ?: continue  // Ambil key sebagai UID
                        val fullName = child.child("fullName").getValue(String::class.java) ?: "Tidak Diketahui"  // Ambil nama lengkap
                        petugasMap[uid] = fullName      // Simpan dalam map UID ke nama
                        petugasUidList.add(uid)         // Simpan UID ke list
                        namaList.add(fullName)          // Tambah nama ke list untuk Spinner
                    }

                    // Buat adapter untuk Spinner dengan daftar nama petugas
                    val adapter = ArrayAdapter(this@PenempatanActivity, android.R.layout.simple_spinner_item, namaList)
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    binding.spinnerPetugas.adapter = adapter  // Set adapter ke spinner
                }

                override fun onCancelled(error: DatabaseError) {
                    // Jika gagal memuat data, tampilkan pesan error
                    Toast.makeText(this@PenempatanActivity, "Gagal memuat data petugas", Toast.LENGTH_SHORT).show()
                }
            })
    }

    // Fungsi untuk memuat daftar lokasi toilet dari Firebase dan mengisi Spinner
    private fun loadLokasi() {
        // Ambil data dari node "toilet"
        database.child("toilet")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    lokasiMap.clear()  // Bersihkan map lokasi sebelumnya
                    val lokasiList = mutableListOf<String>()  // List lokasi untuk Spinner

                    // Iterasi setiap anak node "toilet"
                    for (child in snapshot.children) {
                        val id = child.key ?: continue  // Ambil key sebagai ID toilet
                        val location = child.child("location").getValue(String::class.java) ?: "Tidak Diketahui"  // Ambil lokasi toilet
                        lokasiMap[id] = location  // Simpan dalam map ID ke lokasi
                        lokasiList.add(location)  // Tambah lokasi ke list untuk Spinner
                    }

                    // Buat adapter untuk Spinner dengan daftar lokasi toilet
                    val adapter = ArrayAdapter(this@PenempatanActivity, android.R.layout.simple_spinner_item, lokasiList)
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    binding.spinnerLokasi.adapter = adapter  // Set adapter ke spinner
                }

                override fun onCancelled(error: DatabaseError) {
                    // Jika gagal memuat data, tampilkan pesan error
                    Toast.makeText(this@PenempatanActivity, "Gagal memuat data lokasi", Toast.LENGTH_SHORT).show()
                }
            })
    }

    // Fungsi untuk memuat data penempatan petugas ke lokasi dan menampilkan di RecyclerView
    private fun loadAssignments() {
        // Ambil data dari node "penempatan"
        database.child("penempatan")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    listAssign.clear()  // Bersihkan list penempatan sebelumnya

                    // Iterasi setiap anak node "penempatan"
                    for (child in snapshot.children) {
                        val assign = child.getValue(DataPenempatan::class.java)  // Konversi data ke objek DataPenempatan
                        assign?.id = child.key  // Simpan key sebagai id objek
                        assign?.let { listAssign.add(it) }  // Tambahkan objek ke list jika tidak null
                    }

                    // Beri tahu adapter RecyclerView bahwa data sudah diperbarui
                    binding.recyclerViewAssign.adapter?.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    // Jika gagal memuat data, tampilkan pesan error
                    Toast.makeText(this@PenempatanActivity, "Gagal memuat data penempatan", Toast.LENGTH_SHORT).show()
                }
            })
    }


    // Fungsi untuk menambahkan data penempatan petugas ke lokasi ke Firebase Realtime Database
    private fun addAssignment(petugasUid: String, lokasiId: String) {
        val penempatanRef = database.child("penempatan")

        // Ambil data penempatan terlebih dahulu untuk pengecekan duplikat
        penempatanRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (child in snapshot.children) {
                    val existingPetugasUid = child.child("petugasUid").getValue(String::class.java)
                    val existingLokasiId = child.child("lokasiId").getValue(String::class.java)

                    // Cek apakah kombinasi petugas dan lokasi sudah ada
                    if (existingPetugasUid == petugasUid && existingLokasiId == lokasiId) {
                        Toast.makeText(this@PenempatanActivity, "Petugas sudah ditempatkan di lokasi ini", Toast.LENGTH_SHORT).show()
                        return
                    }
                }

                // Jika tidak ada duplikat, lanjutkan menambahkan data
                val id = penempatanRef.push().key ?: return
                val data = DataPenempatan(id, petugasUid, lokasiId, null)

                penempatanRef.child(id).setValue(data)
                    .addOnSuccessListener {
                        Toast.makeText(this@PenempatanActivity, "Data berhasil ditambahkan", Toast.LENGTH_SHORT).show()
                        loadAssignments()
                        resetForm()
                    }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@PenempatanActivity, "Gagal memeriksa data penempatan", Toast.LENGTH_SHORT).show()
            }
        })
    }



    private fun updateAssignment(id: String, petugasUid: String, lokasiId: String) {
        val data = DataPenempatan(id, petugasUid, lokasiId, null)
        database.child("penempatan").child(id).setValue(data)
            .addOnSuccessListener {
                Toast.makeText(this, "Data berhasil diperbarui", Toast.LENGTH_SHORT).show()
                loadAssignments()
                resetForm()
            }
    }

    private fun resetForm() {
        editMode = false
        editId = null
        binding.spinnerPetugas.setSelection(0)
        binding.spinnerLokasi.setSelection(0)
        binding.btnSimpan.text = "Simpan"
    }
}
