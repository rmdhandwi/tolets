package com.example.tolets.Admin

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tolets.Adapter.AdapterAdminRiwayat
import com.example.tolets.Auth.LoginActivity
import com.example.tolets.HistoryActivity
import com.example.tolets.R
import com.example.tolets.databinding.ActivityAdminRiwayatBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class AdminRiwayatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminRiwayatBinding
    private lateinit var database: DatabaseReference
    private lateinit var adapter: AdapterAdminRiwayat
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminRiwayatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = FirebaseDatabase.getInstance().reference

        adapter = AdapterAdminRiwayat(emptyList()) { uid ->
            val intent = Intent(this, DetailRiwayatActivity::class.java)
            intent.putExtra("uid", uid)
            startActivity(intent)
        }

        binding.rvUid.layoutManager = LinearLayoutManager(this)
        binding.rvUid.adapter = adapter

        getFullName()
        loadUidList()

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
    }

    private fun loadUidList() {
        database.child("riwayat").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val uidSet = mutableSetOf<String>()
                for (item in snapshot.children) {
                    val uid = item.child("uid").value?.toString().orEmpty()
                    if (uid.isNotEmpty()) {
                        uidSet.add(uid)
                    }
                }

                val userList = mutableListOf<Pair<String, String>>()

                for (uid in uidSet) {
                    database.child("users").child(uid).child("fullName")
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                val fullName = snapshot.getValue(String::class.java) ?: "Tidak Diketahui"
                                userList.add(uid to fullName)

                                // Cek jika semua data sudah diambil
                                if (userList.size == uidSet.size) {
                                    adapter = AdapterAdminRiwayat(userList) { clickedUid ->
                                        val intent = Intent(this@AdminRiwayatActivity, DetailRiwayatActivity::class.java)
                                        intent.putExtra("uid", clickedUid)
                                        startActivity(intent)
                                    }
                                    binding.rvUid.adapter = adapter
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Log.e("AdminRiwayat", "Gagal ambil fullName untuk UID: $uid", error.toException())
                            }
                        })
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("AdminRiwayat", "Gagal ambil riwayat", error.toException())
            }
        })
    }

    private fun getFullName() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val uid = currentUser.uid
            database.child("users").child(uid).child("fullName")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val fullName = snapshot.getValue(String::class.java) ?: "Tidak dikenal"
                        binding.txtuser.text = fullName
                    }

                    override fun onCancelled(error: DatabaseError) {
                        binding.txtuser.text = "Gagal memuat nama"
                        Log.e("AdminRiwayat", "Gagal ambil nama admin", error.toException())
                    }
                })
        } else {
            binding.txtuser.text = "Tidak login"
        }
    }

    private fun logoutUser() {
        auth.signOut()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}
