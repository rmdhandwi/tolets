#include <ESP8266WiFi.h>
#include <FirebaseESP8266.h>
#include <DHT.h>

// WiFi
const char* ssid = "tes";
const char* password = "12345678";

// Firebase
#define FIREBASE_HOST "https://tolets-71175-default-rtdb.firebaseio.com/"
#define FIREBASE_AUTH "AIzaSyCA8Fm-C_fT7Z7Wyy4s7xPicxqupdxi8kI"

FirebaseData firebaseData;
FirebaseAuth auth;
FirebaseConfig config;

// Sensor dan pin
String sensorID = "1";
#define RELAY D8
#define DHTPIN D7
#define PIRPIN D5
#define MQ135 A0

#define DHTTYPE DHT11
DHT dht(DHTPIN, DHTTYPE);

// Ambang batas
float batasTemperature = 35.0;
float batasHumidity = 65.0;
int batasAirQuality = 25;

unsigned long previousMillis = 0;
const long interval = 5000;

void setup() {
    Serial.begin(115200);
    pinMode(RELAY, OUTPUT);
    pinMode(PIRPIN, INPUT);
    dht.begin();

    WiFi.begin(ssid, password);
    Serial.print("Menghubungkan ke WiFi");
    while (WiFi.status() != WL_CONNECTED) {
        delay(500);
        Serial.print(".");
    }
    Serial.println("\nTerhubung ke WiFi");
    Serial.println(WiFi.localIP());

    config.host = FIREBASE_HOST;
    config.signer.tokens.legacy_token = FIREBASE_AUTH;
    Firebase.begin(&config, &auth);
    Firebase.reconnectWiFi(true);

    String basePath = "/sensor/" + sensorID;
    Firebase.setInt(firebaseData, basePath + "/relay", 0);
    Firebase.setBool(firebaseData, basePath + "/manual", false);
    digitalWrite(RELAY, LOW);
}

void loop() {
    unsigned long currentMillis = millis();
    if (currentMillis - previousMillis >= interval) {
        previousMillis = currentMillis;

        float temperature = dht.readTemperature();
        float humidity = dht.readHumidity();
        int pirState = digitalRead(PIRPIN);
        int airQuality = analogRead(MQ135);
        String basePath = "/sensor/" + sensorID;

        // Kirim data sensor ke Firebase
        Firebase.setFloat(firebaseData, basePath + "/temperature", temperature);
        Firebase.setFloat(firebaseData, basePath + "/humidity", humidity);
        Firebase.setInt(firebaseData, basePath + "/pir", pirState);
        Firebase.setInt(firebaseData, basePath + "/airQuality", airQuality);

        // Cek mode manual/otomatis
        bool manualMode = false;
        if (Firebase.getBool(firebaseData, basePath + "/manual")) {
            manualMode = firebaseData.boolData();
        } else {
            Serial.println("Gagal membaca mode manual");
        }

        if (manualMode) {
            // Mode Manual
            if (Firebase.getInt(firebaseData, basePath + "/relay")) {
                int relayStatus = firebaseData.intData();
                digitalWrite(RELAY, relayStatus == 1 ? HIGH : LOW);
                Serial.println(relayStatus == 1 ? "Manual: Relay ON" : "Manual: Relay OFF");
            } else {
                Serial.println("Gagal membaca status relay (manual)");
            }
        } else {
            // Mode Otomatis
            bool overThreshold = (temperature >= batasTemperature) ||
                                 (humidity >= batasHumidity) ||
                                 (airQuality >= batasAirQuality);

            if (overThreshold) {
                digitalWrite(RELAY, HIGH);
                Firebase.setInt(firebaseData, basePath + "/relay", 1);
                Serial.println("AUTO: Ambang batas terlampaui - Relay ON");
            } else {
                digitalWrite(RELAY, LOW);
                Firebase.setInt(firebaseData, basePath + "/relay", 0);
                Serial.println("AUTO: Kondisi normal - Relay OFF");
            }
        }

        Serial.println("Data sensor dikirim!\n");
    }
}
