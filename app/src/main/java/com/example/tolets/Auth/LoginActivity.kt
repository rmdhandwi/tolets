package com.example.tolets.Auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.tolets.Admin.AdminActivity
import com.example.tolets.MainActivity
import com.example.tolets.R
import com.example.tolets.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

//
        binding.loginButton.setOnClickListener {
            val email = binding.emailLogin.text.toString().trim()
            val password = binding.passLogin.text.toString().trim()
            loginUser(email, password)
        }

        binding.tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun loginUser(email: String, password: String) {
        // Periksa apakah email atau password kosong
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Email dan Password harus diisi!", Toast.LENGTH_SHORT).show()
            return
        }

        // Proses login menggunakan Firebase Authentication
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Jika login berhasil, ambil user saat ini
                    val user = auth.currentUser
                    val uid = user?.uid

                    // Pastikan UID tidak null
                    if (uid != null) {
                        // Ambil referensi ke node user di Firebase Realtime Database
                        val dbRef = FirebaseDatabase.getInstance().getReference("users").child(uid)

                        // Ambil data user dari database
                        dbRef.get().addOnSuccessListener { dataSnapshot ->
                            // Cek apakah data user dan role tersedia
                            if (dataSnapshot.exists() && dataSnapshot.hasChild("role")) {
                                val role = dataSnapshot.child("role").getValue(String::class.java)

                                // Arahkan pengguna ke aktivitas berdasarkan perannya
                                when (role) {
                                    "admin" -> {
                                        Toast.makeText(this, "Login sebagai Admin", Toast.LENGTH_SHORT).show()
                                        val intent = Intent(this, AdminActivity::class.java)
                                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                        startActivity(intent)
                                        finish()
                                    }
                                    "petugas" -> {
                                        Toast.makeText(this, "Login sebagai Petugas", Toast.LENGTH_SHORT).show()
                                        val intent = Intent(this, MainActivity::class.java)
                                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                        startActivity(intent)
                                        finish()
                                    }
                                    else -> {
                                        // Jika peran tidak dikenali
                                        Toast.makeText(this, "Peran tidak dikenali: $role", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } else {
                                // Jika data role tidak ditemukan
                                Toast.makeText(this, "Data role tidak ditemukan di database", Toast.LENGTH_SHORT).show()
                            }
                        }.addOnFailureListener {
                            // Jika gagal mengambil data dari database
                            Toast.makeText(this, "Gagal mendapatkan data role: ${it.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    // Penanganan error login berdasarkan jenis exception
                    val errorMessage = when (task.exception) {
                        is FirebaseAuthInvalidUserException -> "Akun tidak ditemukan. Periksa kembali email Anda."
                        is FirebaseAuthInvalidCredentialsException -> "Password salah. Coba lagi."
                        else -> "Login gagal: ${task.exception?.message}"
                    }
                    Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
                }
            }
    }
}