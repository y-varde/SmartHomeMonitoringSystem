#include <Adafruit_Sensor.h>
#include <Adafruit_LSM303_U.h>
#include <SoftwareSerial.h>

// Create sensor object
Adafruit_LSM303_Accel_Unified accel = Adafruit_LSM303_Accel_Unified(54321);

// Bluetooth module pins
#define BT_RX 10
#define BT_TX 11

SoftwareSerial bluetooth(BT_RX, BT_TX); // RX, TX

// Define threshold for movement detection
const float movementThreshold = 0.5; // Adjust as needed

// Variables to store initial position
float initialX, initialY, initialZ;

// Variable for arming device
bool armed = false;

void setup() {
  // Start serial communication
  Serial.begin(9600);
  bluetooth.begin(9600);
  
  // Start the sensor
  if (!accel.begin()) {
    Serial.println("Could not find a valid LSM303DLHC sensor, check wiring!");
    while (1);
  }
  Serial.println("LSM303DLHC detected!");

  // Get initial position
  sensors_event_t event;
  accel.getEvent(&event);
  initialX = event.acceleration.x;
  initialY = event.acceleration.y;
  initialZ = event.acceleration.z;
}

void loop() {
  // Read accelerometer data
  sensors_event_t event;
  accel.getEvent(&event);

  // Calculate the change in position
  float deltaX = abs(event.acceleration.x - initialX);
  float deltaY = abs(event.acceleration.y - initialY);
  float deltaZ = abs(event.acceleration.z - initialZ);

  if(bluetooth.available()) {
    char value = bluetooth.read();
    if(value == 'A') {armed = true;}
    else if (value == 'D') {armed = false;}
    Serial.println(value);
  }

  if(armed == true) {
    // Check for significant movement
    if (deltaX > movementThreshold || deltaY > movementThreshold || deltaZ > movementThreshold) {
      Serial.println("M");
      bluetooth.write('M');

      // Update initial position after detection
      initialX = event.acceleration.x;
      initialY = event.acceleration.y;
      initialZ = event.acceleration.z;

      // Small delay before next reading
      delay(500);
    }
  }
  

  delay(1000); // Small delay for loop stability
}

