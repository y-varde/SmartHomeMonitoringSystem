#include <SoftwareSerial.h>

// SH-HC-08     Nano
// Rx           2
// Tx           3
const int rxPinBLE = 3;
const int txPinBLE = 2;

SoftwareSerial BT05(rxPinBLE, txPinBLE);
char cSend[] = "DSD Tech Connected";
bool bReceived = false;

void setup() 
{
    Serial.begin(9600);
    BT05.begin(9600);
    Serial.println("Bluetooth device ready!");
}

void loop() 
{
  BT05.println(cSend);
  Serial.println("Message sent to Bluetooth device: DSD Tech Connected");
  delay(1000); // Delay to avoid spamming the message
}