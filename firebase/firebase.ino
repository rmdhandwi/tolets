#include <ESP8266WiFi.h>          // Library koneksi WiFi untuk ESP8266
#include <FirebaseESP8266.h>      // Library untuk koneksi ke Firebase Realtime Database
#include <DHT.h>                  // Library untuk sensor suhu & kelembaban DHT11

// Konfigurasi WiFi
const char* ssid = "Monitoring";         // Nama WiFi
const char* password = "12345678";       // Password WiFi


// Konfigurasi Firebase
#define FIREBASE_HOST "https://tolets-71175-default-rtdb.firebaseio.com/"   // Alamat database Firebase
#define FIREBASE_AUTH "AIzaSyCA8Fm-C_fT7Z7Wyy4s7xPicxqupdxi8kI"             // Token otentikasi Firebase

FirebaseData firebaseData;
FirebaseAuth auth;
FirebaseConfig config;

// Konfigurasi ID Sensor
String sensorID = "1";  // Ganti sesuai lokasi: toilet1, toilet2, dll

// Pin konfigurasi
#define RELAY D4       // Pin kontrol relay
#define DHTPIN D2      // Pin sensor DHT
#define PIRPIN D3      // Pin sensor PIR (gerakan)
#define MQ135 A0       // Pin sensor kualitas udara (MQ-135)

#define DHTTYPE DHT11  // Tipe sensor DHT
DHT dht(DHTPIN, DHTTYPE);


// Waktu non-blocking
unsigned long previousMillis = 0;
const long interval = 5000;

void setup() {
    Serial.begin(115200);

    // Setup pin
    pinMode(RELAY, OUTPUT);
    pinMode(PIRPIN, INPUT);
    dht.begin();

    // Koneksi ke WiFi
    WiFi.begin(ssid, password);
    Serial.print("Menghubungkan ke WiFi");
    while (WiFi.status() != WL_CONNECTED) {
        delay(500);
        Serial.print(".");
    }
    Serial.println("\nTerhubung ke WiFi");
    Serial.print("IP Address: ");
    Serial.println(WiFi.localIP());

    // Konfigurasi Firebase
    config.host = FIREBASE_HOST;
    config.signer.tokens.legacy_token = FIREBASE_AUTH;
    Firebase.begin(&config, &auth);
    Firebase.reconnectWiFi(true);
}

void loop() {
    unsigned long currentMillis = millis();
  
    // Kirim data setiap 5 detik
    if (currentMillis - previousMillis >= interval) {
        previousMillis = currentMillis;

        // Baca sensor
        float temperature = dht.readTemperature();
        float humidity = dht.readHumidity();
        int pirState = digitalRead(PIRPIN);
        int airQuality = analogRead(MQ135);

        // Buat path data di Firebase
        String basePath = "/sensor/" + sensorID;

        // Ambil status relay dari Firebase
        if (Firebase.getInt(firebaseData, basePath + "/relay")) {
            int relayStatus = firebaseData.intData();
            digitalWrite(RELAY, relayStatus == 1 ? LOW : HIGH);
            Serial.println(relayStatus == 1 ? "Relay ON" : "Relay OFF");
        } else {
            Serial.println("Gagal membaca status relay");
            Serial.println(firebaseData.errorReason());
        }

        // Kirim data suhu
        if (Firebase.setFloat(firebaseData, basePath + "/temperature", temperature)) {
            Serial.println("Temperature sent!");
        } else {
            Serial.println("Failed to send temperature");
            Serial.println(firebaseData.errorReason());
        }

        // Kirim data kelembaban
        if (Firebase.setFloat(firebaseData, basePath + "/humidity", humidity)) {
            Serial.println("Humidity sent!");
        } else {
            Serial.println("Failed to send humidity");
            Serial.println(firebaseData.errorReason());
        }

        // Kirim data PIR (gerakan)
        if (Firebase.setInt(firebaseData, basePath + "/pir", pirState)) {
            Serial.println("PIR state sent!");
        } else {
            Serial.println("Failed to send PIR state");
            Serial.println(firebaseData.errorReason());
        }

        // Kirim data kualitas udara
        if (Firebase.setInt(firebaseData, basePath + "/airQuality", airQuality)) {
            Serial.println("Air quality sent!");
        } else {
            Serial.println("Failed to send air quality");
            Serial.println(firebaseData.errorReason());
        } 
     } 
}
