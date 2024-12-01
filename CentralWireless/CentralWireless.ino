#include <SoftwareSerial.h>

SoftwareSerial HC12(10, 11); // HC-12 TX Pin, HC-12 RX Pin

void setup() {
  Serial.begin(9600); // Start the serial monitor
  Serial1.begin(9600); // Initialize Bluetooth on Serial1
  HC12.begin(9600);    // Serial port to HC12
  Serial.println("CentralWireless setup completed");
}

void loop() {
  // Bluetooth communication
  if (Serial1.available()) {
    String btData = Serial1.readString();
    Serial.println("Bluetooth Data: " + btData);
  }

  // HC-12 communication
  if (HC12.available()) {
    String hc12Data = HC12.readString();
    Serial.println("HC-12 Data: " + hc12Data);
  }

  while (Serial.available()) {      // If Serial monitor has data
    HC12.write(Serial.read());      // Send that data to HC-12
  }

  delay(100); // Small delay to stabilize switching
}
