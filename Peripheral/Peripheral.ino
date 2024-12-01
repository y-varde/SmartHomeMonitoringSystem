#include <Wire.h>
#include <Adafruit_Sensor.h>
#include <Adafruit_LSM303_U.h>
#include <SoftwareSerial.h>

// Create sensor object
Adafruit_LSM303_Accel_Unified accel = Adafruit_LSM303_Accel_Unified(54321);
SoftwareSerial HC12(10, 11);

// Define LED pin
#define LED_PIN 13

// Define threshold for movement detection
const float movementThreshold = 1.0; // Adjust as needed

// Variables to store initial position
float initialX, initialY, initialZ;

// Variables for arming device and LED control
bool armed = false;
bool ledBlinking = false;
bool ledSolid = false;
unsigned long previousMillis = 0; // Store the last time the LED was updated
const long interval = 1000; // Interval at which to blink (milliseconds)
unsigned long solidStartMillis = 0; // Store the start time of the solid LED state

void setup() {
  // Start serial communication
  Serial.begin(9600);
  HC12.begin(9600);

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
  Serial.println("Peripheral Ready");
}

void loop() {
  handleCommands();
  blinkLED();
  readAccelerometerData();
  checkMovement();
  delay(100); // Small delay for loop stability
}

void handleCommands() {
  if (HC12.available()) {
    char command = HC12.read();
    Serial.print("Received command: ");
    Serial.println(command);

    if (command == 'A') {
      armed = true;
      ledBlinking = true;
      ledSolid = false; // Ensure LED is not solid
    } else if (command == 'D') {
      armed = false;
      ledBlinking = false;
      ledSolid = false;
      digitalWrite(LED_PIN, LOW); // Turn off LED
    }
  }
}

void blinkLED() {
  if (ledBlinking && !ledSolid) {
    unsigned long currentMillis = millis();
    if (currentMillis - previousMillis >= interval) {
      previousMillis = currentMillis;
      // If the LED is off, turn it on, and vice-versa
      if (digitalRead(LED_PIN) == LOW) {
        digitalWrite(LED_PIN, HIGH);
      } else {
        digitalWrite(LED_PIN, LOW);
      }
    }
  }
}

void readAccelerometerData() {
  sensors_event_t event;
  accel.getEvent(&event);

  // Print acceleration values for testing
  Serial.print(event.acceleration.x);
  Serial.print("X ");
  Serial.print(event.acceleration.y);
  Serial.print("Y ");
  Serial.print(event.acceleration.z);
  Serial.println("Z ");
}

void checkMovement() {
  sensors_event_t event;
  accel.getEvent(&event);

  // Calculate the change in position
  float deltaX = abs(event.acceleration.x - initialX);
  float deltaY = abs(event.acceleration.y - initialY);
  float deltaZ = abs(event.acceleration.z - initialZ);

  // Check for significant movement
  if (armed && (deltaX > movementThreshold || deltaY > movementThreshold || deltaZ > movementThreshold)) {
    Serial.println("Object has been opened or moved!");
    HC12.println("Object has been opened or moved!");

    // Turn the LED on solid for 10 seconds
    digitalWrite(LED_PIN, HIGH);
    ledSolid = true;
    solidStartMillis = millis();

    // Update initial position after detection
    initialX = event.acceleration.x;
    initialY = event.acceleration.y;
    initialZ = event.acceleration.z;
  }

  // Check if the solid LED state should end
  if (ledSolid && (millis() - solidStartMillis >= 10000)) {
    ledSolid = false;
    digitalWrite(LED_PIN, LOW); // Turn off LED after 10 seconds
  }
}