#include <SoftwareSerial.h>

SoftwareSerial HC05(10, 11);  // RX, TX pins for HC-05 communication

void setup() {
  Serial.begin(9600);
  HC05.begin(38400);  // AT mode baud rate is 38400
  delay(1000);

  HC05.println("AT+ADDR?");  // Get the Bluetooth address
}

void loop() {
  if (HC05.available()) {
    Serial.write(HC05.read());
  }
}
