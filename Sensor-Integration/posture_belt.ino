#include <Wire.h>
#include <WiFi.h>
#include <WebServer.h>
#include <Adafruit_MPU6050.h>
#include <Adafruit_Sensor.h>
#include <LiquidCrystal_I2C.h>

// ==================================================
// EXISTING CONNECTIONS - DO NOT CHANGE
// ==================================================

#define SDA_PIN 26
#define SCL_PIN 27
#define BUZZER_PIN 25

// ==================================================
// LCD
// ==================================================

LiquidCrystal_I2C lcd(0x27, 16, 2);

// ==================================================
// TWO MPU6050 SENSORS
// ==================================================

Adafruit_MPU6050 lowerMPU;
Adafruit_MPU6050 upperMPU;

// ==================================================
// POSTURE VARIABLES
// ==================================================

float lowerReference = 0;
float upperReference = 0;

float upperCurrent = 0;
float lowerCurrent = 0;

float upperDifference = 0;
float lowerDifference = 0;

const float tolerance = 10.0;

bool postureGood = false;

// ==================================================
// ESP32 WIFI ACCESS POINT
// ==================================================

const char* WIFI_NAME = "SmartPostureBelt";
const char* WIFI_PASSWORD = "12345678";

WebServer server(80);

// ==================================================
// READ LOWER SPINE ANGLE
// ==================================================

float readLowerAngle()
{
  sensors_event_t acceleration;
  sensors_event_t gyro;
  sensors_event_t temperature;

  lowerMPU.getEvent(
    &acceleration,
    &gyro,
    &temperature
  );

  float angle = atan2(
    acceleration.acceleration.y,
    acceleration.acceleration.z
  ) * 180.0 / PI;

  return angle;
}

// ==================================================
// READ UPPER SPINE ANGLE
// ==================================================

float readUpperAngle()
{
  sensors_event_t acceleration;
  sensors_event_t gyro;
  sensors_event_t temperature;

  upperMPU.getEvent(
    &acceleration,
    &gyro,
    &temperature
  );

  float angle = atan2(
    acceleration.acceleration.y,
    acceleration.acceleration.z
  ) * 180.0 / PI;

  return angle;
}

// ==================================================
// LOWER SPINE CALIBRATION
// ==================================================

void calibrateLowerSpine()
{
  Serial.println();
  Serial.println("LOWER SPINE CALIBRATION");
  Serial.println("Keep comfortable posture.");
  Serial.println("Collecting for 30 seconds...");

  lcd.clear();

  lcd.setCursor(0, 0);
  lcd.print("Lower Calib...");

  lcd.setCursor(0, 1);
  lcd.print("Keep posture");

  float sum = 0;
  int samples = 0;

  unsigned long startTime = millis();

  while (millis() - startTime < 30000)
  {
    float angle = readLowerAngle();

    sum += angle;
    samples++;

    delay(100);
  }

  lowerReference = sum / samples;

  Serial.println("Lower calibration completed.");

  Serial.print("Lower Reference = ");
  Serial.print(lowerReference, 2);
  Serial.println(" deg");

  lcd.clear();

  lcd.setCursor(0, 0);
  lcd.print("Lower Saved");

  lcd.setCursor(0, 1);
  lcd.print(lowerReference, 1);
  lcd.print(" deg");

  delay(2000);
}

// ==================================================
// UPPER SPINE CALIBRATION
// ==================================================

void calibrateUpperSpine()
{
  Serial.println();
  Serial.println("UPPER SPINE CALIBRATION");
  Serial.println("Keep comfortable posture.");
  Serial.println("Collecting for 30 seconds...");

  lcd.clear();

  lcd.setCursor(0, 0);
  lcd.print("Upper Calib...");

  lcd.setCursor(0, 1);
  lcd.print("Keep posture");

  float sum = 0;
  int samples = 0;

  unsigned long startTime = millis();

  while (millis() - startTime < 30000)
  {
    float angle = readUpperAngle();

    sum += angle;
    samples++;

    delay(100);
  }

  upperReference = sum / samples;

  Serial.println("Upper calibration completed.");

  Serial.print("Upper Reference = ");
  Serial.print(upperReference, 2);
  Serial.println(" deg");

  lcd.clear();

  lcd.setCursor(0, 0);
  lcd.print("Upper Saved");

  lcd.setCursor(0, 1);
  lcd.print(upperReference, 1);
  lcd.print(" deg");

  delay(2000);
}

// ==================================================
// WIFI MAIN PAGE
// ==================================================

void handleRoot()
{
  String page = "";

  page += "<!DOCTYPE html>";
  page += "<html>";
  page += "<head>";
  page += "<meta name='viewport' content='width=device-width,initial-scale=1'>";
  page += "<title>Smart Posture Belt</title>";

  page += "<style>";
  page += "body{font-family:Arial;text-align:center;background:#111;color:white;padding:20px}";
  page += ".box{background:#222;border-radius:20px;padding:20px;margin:15px auto;max-width:500px}";
  page += ".value{font-size:35px;margin:10px}";
  page += ".good{color:#00ff66}";
  page += ".bad{color:#ff4444}";
  page += "</style>";

  page += "</head>";

  page += "<body>";

  page += "<div class='box'>";
  page += "<h1>Smart Posture Belt</h1>";

  page += "<h2 id='status'>Connecting...</h2>";

  page += "<div class='value'>";
  page += "Upper: <span id='upper'>0</span>&deg;";
  page += "</div>";

  page += "<div class='value'>";
  page += "Lower: <span id='lower'>0</span>&deg;";
  page += "</div>";

  page += "<p>Upper Difference: <span id='upperDiff'>0</span>&deg;</p>";
  page += "<p>Lower Difference: <span id='lowerDiff'>0</span>&deg;</p>";

  page += "</div>";

  page += "<script>";

  page += "function updateData(){";
  page += "fetch('/data')";
  page += ".then(response=>response.json())";
  page += ".then(data=>{";

  page += "document.getElementById('upper').innerText=data.upper.toFixed(1);";
  page += "document.getElementById('lower').innerText=data.lower.toFixed(1);";

  page += "document.getElementById('upperDiff').innerText=data.upperDifference.toFixed(1);";
  page += "document.getElementById('lowerDiff').innerText=data.lowerDifference.toFixed(1);";

  page += "let s=document.getElementById('status');";

  page += "s.innerText=data.status;";

  page += "if(data.good){";
  page += "s.className='good';";
  page += "}else{";
  page += "s.className='bad';";
  page += "}";

  page += "});";
  page += "}";

  page += "setInterval(updateData,500);";
  page += "updateData();";

  page += "</script>";

  page += "</body>";
  page += "</html>";

  server.send(200, "text/html", page);
}

// ==================================================
// WIFI DATA API
// ==================================================

void handleData()
{
  String json = "{";

  json += "\"upper\":";
  json += String(upperCurrent, 2);

  json += ",\"lower\":";
  json += String(lowerCurrent, 2);

  json += ",\"upperDifference\":";
  json += String(upperDifference, 2);

  json += ",\"lowerDifference\":";
  json += String(lowerDifference, 2);

  json += ",\"good\":";
  json += postureGood ? "true" : "false";

  json += ",\"status\":\"";
  json += postureGood ? "GOOD POSTURE" : "BAD POSTURE";
  json += "\"";

  json += "}";

  server.send(200, "application/json", json);
}

// ==================================================
// SETUP
// ==================================================

void setup()
{
  Serial.begin(115200);

  // ==================================================
  // I2C - SAME CONNECTION
  // ==================================================

  Wire.begin(SDA_PIN, SCL_PIN);

  // ==================================================
  // BUZZER - SAME CONNECTION
  // ==================================================

  pinMode(BUZZER_PIN, OUTPUT);

  digitalWrite(
    BUZZER_PIN,
    LOW
  );

  // ==================================================
  // LCD - SAME CONNECTION
  // ==================================================

  lcd.begin(16, 2);
  lcd.backlight();

  lcd.setCursor(0, 0);
  lcd.print("POSTURE BELT");

  lcd.setCursor(0, 1);
  lcd.print("Initializing");

  delay(2000);

  // ==================================================
  // LOWER MPU6050 - SAME ADDRESS
  // ==================================================

  if (!lowerMPU.begin(0x68))
  {
    Serial.println(
      "Lower MPU6050 NOT detected!"
    );

    lcd.clear();

    lcd.setCursor(0, 0);
    lcd.print("Lower MPU Error");

    while (1)
    {
      delay(10);
    }
  }

  Serial.println(
    "Lower MPU6050 OK"
  );

  // ==================================================
  // UPPER MPU6050 - SAME ADDRESS
  // ==================================================

  if (!upperMPU.begin(0x69))
  {
    Serial.println(
      "Upper MPU6050 NOT detected!"
    );

    lcd.clear();

    lcd.setCursor(0, 0);
    lcd.print("Upper MPU Error");

    while (1)
    {
      delay(10);
    }
  }

  Serial.println(
    "Upper MPU6050 OK"
  );

  // ==================================================
  // WIFI ACCESS POINT
  // ==================================================

  WiFi.mode(WIFI_AP);

  WiFi.softAP(
    WIFI_NAME,
    WIFI_PASSWORD
  );

  IPAddress IP = WiFi.softAPIP();

  Serial.println();
  Serial.println("==============================");
  Serial.println("SMART POSTURE BELT WIFI");
  Serial.println("==============================");

  Serial.print("WiFi Name: ");
  Serial.println(WIFI_NAME);

  Serial.print("WiFi Password: ");
  Serial.println(WIFI_PASSWORD);

  Serial.print("ESP32 IP Address: ");
  Serial.println(IP);

  // ==================================================
  // WEB SERVER
  // ==================================================

  server.on(
    "/",
    handleRoot
  );

  server.on(
    "/data",
    handleData
  );

  server.begin();

  Serial.println(
    "Web server started"
  );

  // ==================================================
  // SENSORS READY
  // ==================================================

  lcd.clear();

  lcd.setCursor(0, 0);
  lcd.print("WiFi Ready");

  lcd.setCursor(0, 1);
  lcd.print("SmartPosture");

  delay(2000);

  // ==================================================
  // CALIBRATION START
  // ==================================================

  lcd.clear();

  lcd.setCursor(0, 0);
  lcd.print("Sit Comfortably");

  lcd.setCursor(0, 1);
  lcd.print("Starting...");

  Serial.println();
  Serial.println(
    "Sit in comfortable natural posture."
  );

  delay(5000);

  // Lower = first 30 seconds
  calibrateLowerSpine();

  // Upper = next 30 seconds
  calibrateUpperSpine();

  // ==================================================
  // CALIBRATION COMPLETE
  // ==================================================

  Serial.println();
  Serial.println("==============================");
  Serial.println("CALIBRATION COMPLETED");
  Serial.println("==============================");

  Serial.print(
    "Lower Reference = "
  );

  Serial.print(
    lowerReference,
    2
  );

  Serial.println(" deg");

  Serial.print(
    "Upper Reference = "
  );

  Serial.print(
    upperReference,
    2
  );

  Serial.println(" deg");

  Serial.println();
  Serial.println(
    "Connect phone to SmartPostureBelt"
  );

  Serial.println(
    "Open: http://192.168.4.1"
  );

  lcd.clear();

  lcd.setCursor(0, 0);
  lcd.print("WiFi Ready");

  lcd.setCursor(0, 1);
  lcd.print("192.168.4.1");

  delay(3000);
}

// ==================================================
// LOOP
// ==================================================

void loop()
{
  // ==================================================
  // READ CURRENT ANGLES
  // ==================================================

  upperCurrent = readUpperAngle();

  lowerCurrent = readLowerAngle();

  // ==================================================
  // CALCULATE DIFFERENCE
  // ==================================================

  upperDifference =
    abs(
      upperCurrent -
      upperReference
    );

  lowerDifference =
    abs(
      lowerCurrent -
      lowerReference
    );

  // ==================================================
  // POSTURE DECISION
  // ==================================================

  bool upperOK =
    upperDifference <= tolerance;

  bool lowerOK =
    lowerDifference <= tolerance;

  postureGood =
    upperOK &&
    lowerOK;

  // ==================================================
  // SERIAL MONITOR
  // ==================================================

  Serial.println();
  Serial.println(
    "-----------------------------"
  );

  Serial.print(
    "Upper Ref : "
  );

  Serial.print(
    upperReference,
    1
  );

  Serial.println(" deg");

  Serial.print(
    "Upper Now : "
  );

  Serial.print(
    upperCurrent,
    1
  );

  Serial.println(" deg");

  Serial.print(
    "Upper Diff: "
  );

  Serial.print(
    upperDifference,
    1
  );

  Serial.println(" deg");

  Serial.print(
    "Lower Ref : "
  );

  Serial.print(
    lowerReference,
    1
  );

  Serial.println(" deg");

  Serial.print(
    "Lower Now : "
  );

  Serial.print(
    lowerCurrent,
    1
  );

  Serial.println(" deg");

  Serial.print(
    "Lower Diff: "
  );

  Serial.print(
    lowerDifference,
    1
  );

  Serial.println(" deg");

  // ==================================================
  // LCD
  // ==================================================

  lcd.clear();

  lcd.setCursor(0, 0);

  lcd.print("U:");
  lcd.print(
    upperCurrent,
    1
  );

  lcd.print(" L:");
  lcd.print(
    lowerCurrent,
    1
  );

  if (postureGood)
  {
    Serial.println(
      "STATUS: GOOD POSTURE"
    );

    lcd.setCursor(0, 1);

    lcd.print(
      "GOOD POSTURE"
    );

    digitalWrite(
      BUZZER_PIN,
      LOW
    );
  }
  else
  {
    Serial.println(
      "STATUS: BAD POSTURE"
    );

    lcd.setCursor(0, 1);

    lcd.print(
      "BAD POSTURE!"
    );

    digitalWrite(
      BUZZER_PIN,
      HIGH
    );

    delay(500);

    digitalWrite(
      BUZZER_PIN,
      LOW
    );
  }

  // ==================================================
  // HANDLE PHONE REQUESTS
  // ==================================================

  server.handleClient();

  delay(500);
}
