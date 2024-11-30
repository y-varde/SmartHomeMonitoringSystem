#include <Wire.h>
#include <Adafruit_Sensor.h>
#include <Adafruit_LSM303_U.h>
#include <SoftwareSerial.h>

// Create sensor object
Adafruit_LSM303_Accel_Unified accel = Adafruit_LSM303_Accel_Unified(54321);

// Bluetooth module pins
#define BT_RX 10
#define BT_TX 11

SoftwareSerial bluetooth(BT_RX, BT_TX); // RX, TX

// Define LED pin
#define LED_PIN 13

// Define threshold for movement detection
const float movementThreshold = 1.0; // Adjust as needed

// Variables to store initial position
float initialX, initialY, initialZ;

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

  // Set up LED pin
  pinMode(LED_PIN, OUTPUT);

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

  // Print acceleration values for testing
  Serial.print(event.acceleration.x);
  Serial.print("X ");
  Serial.print(event.acceleration.y);
  Serial.print("Y ");
  Serial.print(event.acceleration.z);
  Serial.println("Z ");

  // Blink the LED
  digitalWrite(LED_PIN, HIGH);
  delay(500);
  digitalWrite(LED_PIN, LOW);
  delay(500);

  // Calculate the change in position
  float deltaX = abs(event.acceleration.x - initialX);
  float deltaY = abs(event.acceleration.y - initialY);
  float deltaZ = abs(event.acceleration.z - initialZ);

  // Check for significant movement
  if (deltaX > movementThreshold || deltaY > movementThreshold || deltaZ > movementThreshold) {
    Serial.println("Object has been opened or moved!");
    bluetooth.println("Object has been opened or moved!");

    // Turn the LED on solid for 10 seconds
    digitalWrite(LED_PIN, HIGH);
    delay(10000);

    // Update initial position after detection
    initialX = event.acceleration.x;
    initialY = event.acceleration.y;
    initialZ = event.acceleration.z;

    // Small delay before next reading
    delay(500);
  }

  delay(1000); // Small delay for loop stability
}
