package com.example.tolets

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import java.util.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import com.example.tolets.Auth.LoginActivity
import com.example.tolets.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var penempatanRef: DatabaseReference
    private lateinit var sensorRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 100)
            }
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference
        penempatanRef = database.child("penempatan")
        sensorRef = database.child("sensor")

        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        checkUserRole()
        getFullName()
        getUserLocation()
        fetchSensorData()

        binding.fanButton.setOnCheckedChangeListener { _, isChecked ->
            val relayValue = if (isChecked) 1 else 0
            binding.fanButton.text = if (isChecked) "ON" else "OFF"
        }

        binding.btnMenu.selectedItemId = R.id.menu_home
        binding.btnMenu.setOnNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_home -> true
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


    private fun checkUserRole() {
        val uid = auth.currentUser?.uid ?: return

        val userRef = database.child("users").child(uid)
        val penempatanRef = database.child("penempatan")

        // Cek apakah user memiliki role "petugas"
        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    Toast.makeText(this@MainActivity, "Data pengguna tidak ditemukan.", Toast.LENGTH_SHORT).show()
                    logoutUser()
                    return
                }

                val role = snapshot.child("role").getValue(String::class.java)
                if (role != "petugas") {
                    Toast.makeText(this@MainActivity, "Akses ditolak. Anda bukan petugas.", Toast.LENGTH_SHORT).show()
                    logoutUser()
                    return
                }

                // Jika role petugas, lanjut cek apakah terdaftar di penempatan
                penempatanRef.orderByChild("petugasUid").equalTo(uid)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(penempatanSnapshot: DataSnapshot) {
                            if (!penempatanSnapshot.exists()) {
                                Toast.makeText(this@MainActivity, "Anda belum terdaftar pada penempatan.", Toast.LENGTH_SHORT).show()
                                logoutUser()
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Toast.makeText(this@MainActivity, "Gagal membaca data penempatan: ${error.message}", Toast.LENGTH_SHORT).show()
                            logoutUser()
                        }
                    })
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MainActivity, "Gagal memuat data pengguna: ${error.message}", Toast.LENGTH_SHORT).show()
                logoutUser()
            }
        })
    }


    private fun fetchSensorData() {
        val uid = auth.currentUser?.uid ?: return

        val penempatanRef = FirebaseDatabase.getInstance().reference.child("penempatan")
        val sensorRef = FirebaseDatabase.getInstance().reference.child("sensor")

        penempatanRef.orderByChild("petugasUid").equalTo(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) {
                        Toast.makeText(this@MainActivity, "Penempatan tidak ditemukan", Toast.LENGTH_SHORT).show()
                        return
                    }

                    val lokasiId = snapshot.children.first().child("lokasiId").getValue(String::class.java)
                    if (lokasiId.isNullOrEmpty()) {
                        Toast.makeText(this@MainActivity, "Lokasi ID tidak valid", Toast.LENGTH_SHORT).show()
                        return
                    }

                    // Cari sensor dengan toiletId yang cocok
                    sensorRef.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(sensorSnapshot: DataSnapshot) {
                            for (sensor in sensorSnapshot.children) {
                                val toiletId = sensor.child("toiletId").getValue(String::class.java)
                                if (toiletId == lokasiId) {
                                    // Pasang realtime listener hanya pada sensor yang cocok
                                    monitorSensorRealtime(sensor.ref)
                                    return
                                }
                            }
                            Toast.makeText(this@MainActivity, "Sensor tidak ditemukan untuk lokasi ini", Toast.LENGTH_SHORT).show()
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Toast.makeText(this@MainActivity, "Gagal membaca sensor: ${error.message}", Toast.LENGTH_SHORT).show()
                        }
                    })
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@MainActivity, "Gagal membaca penempatan: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun monitorSensorRealtime(sensorRef: DatabaseReference) {
        sensorRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (isRecentlyLogged()) return

                val temperature = snapshot.child("temperature").getValue(Double::class.java) ?: 0.0
                val humidity = snapshot.child("humidity").getValue(Double::class.java) ?: 0.0
                val airQuality = snapshot.child("airQuality").getValue(Int::class.java) ?: 0
                val pir = snapshot.child("pir").getValue(Int::class.java) ?: 0
                val relay = snapshot.child("relay").getValue(Int::class.java) ?: 0

                binding.txtTemperature.text = "${temperature}°C"
                binding.txtHumidity.text = "$humidity %"
                binding.txtairQuality.text = "$airQuality ppm"

                binding.fanButton.setOnCheckedChangeListener(null)
                binding.fanButton.isChecked = relay == 1
                binding.fanButton.text = if (relay == 1) "ON" else "OFF"

                binding.fanButton.setOnCheckedChangeListener { _, isChecked ->
                    val newRelay = if (isChecked) 1 else 0
                    binding.fanButton.text = if (isChecked) "ON" else "OFF"
                    sensorRef.child("relay").setValue(newRelay)
                }

                val batasTemperature = 35.0
                val batasHumidity = 80.0
                val batasAirQuality = 25

                val pelanggaran = mutableListOf<String>()

                if (temperature > batasTemperature) pelanggaran.add("Suhu > $batasTemperature°C")
                if (humidity > batasHumidity) pelanggaran.add("Kelembaban > $batasHumidity%")
                if (airQuality > batasAirQuality) pelanggaran.add("Gas Amonia > $batasAirQuality ppm")

                if (pelanggaran.isNotEmpty()) {
                    if (relay != 1) {
                        sensorRef.child("relay").setValue(1)
                        binding.fanButton.isChecked = true
                        binding.fanButton.text = "ON"
                    }

                    simpanRiwayatDenganLokasi(
                        uid = auth.currentUser?.uid ?: return,
                        temperature = temperature,
                        humidity = humidity,
                        airQuality = airQuality,
                        pelanggaran = pelanggaran
                    )
                }

                val suhuImage = when {
                    temperature > 35 -> R.drawable.hot
                    temperature < 20 -> R.drawable.cold
                    else -> R.drawable.thermometer
                }
                binding.imgTemperature.setImageResource(suhuImage)

                if (pir == 1) {
                    binding.imgPeople.setImageResource(R.drawable.people)
                    binding.txtPeople.text = "Ada Orang"
                } else {
                    binding.imgPeople.setImageResource(R.drawable.wrong)
                    binding.txtPeople.text = "Tidak Ada Orang"
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MainActivity, "Gagal monitoring: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun simpanRiwayatDenganLokasi(
        uid: String,
        temperature: Double,
        humidity: Double,
        airQuality: Int,
        pelanggaran: List<String>
    ) {
        val penempatanRef = FirebaseDatabase.getInstance().reference.child("penempatan")
        val toiletRef = FirebaseDatabase.getInstance().reference.child("toilet")

        penempatanRef.orderByChild("petugasUid").equalTo(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val lokasiId = snapshot.children.firstOrNull()?.child("lokasiId")?.getValue(String::class.java)
                    if (lokasiId.isNullOrEmpty()) return

                    toiletRef.child(lokasiId).addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(toiletSnapshot: DataSnapshot) {
                            val lokasiNama = toiletSnapshot.child("location").getValue(String::class.java) ?: "Tidak diketahui"
                            val timestamp = System.currentTimeMillis()
                            val keyRiwayat = "$uid-$timestamp"
                            val riwayatRef = FirebaseDatabase.getInstance().reference.child("riwayat").child(keyRiwayat)

                            val dataRiwayat = mapOf(
                                "uid" to uid,
                                "tanggal" to getCurrentDate(),
                                "temperature" to temperature,
                                "humidity" to humidity,
                                "airQuality" to airQuality,
                                "lokasi" to lokasiNama,
                                "keterangan" to pelanggaran.joinToString(", ")
                            )

                            riwayatRef.setValue(dataRiwayat).addOnSuccessListener {
                                updateLastSavedTime()
                                showNotification("Lokasi: $lokasiNama | ${pelanggaran.joinToString(", ")}")
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {}
                    })
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }


    private fun isRecentlyLogged(): Boolean {
        val prefs = getSharedPreferences("riwayat_pref", MODE_PRIVATE)
        val lastSavedTime = prefs.getLong("last_saved", 0)
        val currentTime = System.currentTimeMillis()
        return currentTime - lastSavedTime < 5 * 1000 // 1 menit
    }

    private fun updateLastSavedTime() {
        val prefs = getSharedPreferences("riwayat_pref", MODE_PRIVATE)
        prefs.edit().putLong("last_saved", System.currentTimeMillis()).apply()
    }


    private fun showNotification(keterangan: String) {
        val channelId = "sensor_alert_channel"
        val channelName = "Sensor Alert"

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.icon) // ganti dengan icon yang ada
            .setContentTitle("Peringatan Sensor!")
            .setContentText("Pelanggaran terdeteksi: $keterangan")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1001, notification)
    }

    private fun showNotificationWithLocation(keterangan: String) {
        val uid = auth.currentUser?.uid ?: return

        val penempatanRef = FirebaseDatabase.getInstance().reference.child("penempatan")
        val toiletRef = FirebaseDatabase.getInstance().reference.child("toilet")

        penempatanRef.orderByChild("petugasUid").equalTo(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val lokasiId = snapshot.children.firstOrNull()?.child("lokasiId")?.getValue(String::class.java)
                    if (lokasiId.isNullOrEmpty()) {
                        showNotification("Pelanggaran di lokasi tidak diketahui: $keterangan")
                        return
                    }

                    // Ambil nama lokasi dari toilet
                    toiletRef.child(lokasiId).addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(toiletSnapshot: DataSnapshot) {
                            val lokasiNama = toiletSnapshot.child("location").getValue(String::class.java) ?: "Lokasi Tidak Diketahui"
                            val fullKeterangan = "Lokasi: $lokasiNama | $keterangan"
                            showNotification(fullKeterangan)
                        }

                        override fun onCancelled(error: DatabaseError) {
                            showNotification("Pelanggaran (lokasi gagal dimuat): $keterangan")
                        }
                    })
                }

                override fun onCancelled(error: DatabaseError) {
                    showNotification("Pelanggaran (penempatan gagal dimuat): $keterangan")
                }
            })
    }


    private fun getFullName() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val uid = currentUser.uid
            database.child("users").child(uid)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val fullName = snapshot.child("fullName").value?.toString() ?: "Tidak dikenal"
                        binding.txtUserName.text = fullName
                    }

                    override fun onCancelled(error: DatabaseError) {
                        binding.txtUserName.text = "Gagal memuat nama"
                    }
                })
        } else {
            binding.txtUserName.text = "Tidak login"
        }
    }

    private fun getUserLocation() {
        val uid = auth.currentUser?.uid ?: return

        val penempatanRef = FirebaseDatabase.getInstance().reference.child("penempatan")

        penempatanRef.orderByChild("petugasUid").equalTo(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) {
                        binding.txtLokasi.text = "Lokasi tidak ditemukan"
                        return
                    }

                    val lokasiId = snapshot.children.first().child("lokasiId").getValue(String::class.java)

                    if (lokasiId.isNullOrEmpty()) {
                        binding.txtLokasi.text = "Lokasi ID kosong"
                        return
                    }

                    // Cari lokasi dari tabel toilet
                    val toiletRef = FirebaseDatabase.getInstance().reference.child("toilet").child(lokasiId)
                    toiletRef.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(toiletSnapshot: DataSnapshot) {
                            val lokasiNama = toiletSnapshot.child("location").getValue(String::class.java)
                            binding.txtLokasi.text = lokasiNama ?: "Nama lokasi tidak ditemukan"
                        }

                        override fun onCancelled(error: DatabaseError) {
                            binding.txtLokasi.text = "Gagal memuat nama lokasi"
                        }
                    })
                }

                override fun onCancelled(error: DatabaseError) {
                    binding.txtLokasi.text = "Gagal membaca data penempatan"
                }
            })
    }


    // Tambahkan fungsi format tanggal
    private fun getCurrentDate(): String {
        val dateFormat = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault())
        return dateFormat.format(Date())
    }

    private fun logoutUser() {
        auth.signOut()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        moveTaskToBack(true)
    }
}
