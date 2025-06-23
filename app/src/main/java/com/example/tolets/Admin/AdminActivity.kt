package com.example.tolets.Admin

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.tolets.Auth.LoginActivity
import com.example.tolets.HistoryActivity
import com.example.tolets.R
import com.example.tolets.databinding.ActivityAdminBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class AdminActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminBinding
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        binding.btnMenu.selectedItemId = R.id.menu_home

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        getFullName()

        // Ketika kartu "Lokasi" ditekan, arahkan pengguna ke aktivitas PenempatanActivity
        binding.cardLokasi.setOnClickListener {
            startActivity(Intent(this, PenempatanActivity::class.java))
        }

        // Ketika kartu "Toilet" ditekan, arahkan pengguna ke aktivitas AddActivity
        binding.cardToilet.setOnClickListener {
            startActivity(Intent(this, AddActivity::class.java))
        }


        binding.btnMenu.setOnNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_home -> true
                R.id.menu_history -> {
                    startActivity(Intent(this, AdminRiwayatActivity::class.java))
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

    private fun getFullName() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val uid = currentUser.uid
            database.child("users").child(uid)
                .addListenerForSingleValueEvent(object : ValueEventListener {
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

    private fun logoutUser() {
        auth.signOut()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}
