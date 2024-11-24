#include "DHT11.h"
#include <SoftwareSerial.h>

DHT11 dht11(2);
const int gasPin = A5;
const int tempPin = A1;
const int buzzerPin = 8; // Define the buzzer pin

SoftwareSerial btSerial(4, 3); // RX, TX

bool isArmed = false;
int samplingRate = 1000; // Default sampling rate in milliseconds

void setup() 
{
  Serial.begin(9600);
  btSerial.begin(9600); // Initialize Bluetooth serial communication
}

void loop() {
  checkBluetoothCommand();

  String data = "";

  data += readTemperature();
  data += readGasConcentration();
  data += readHumidity();
  
  data += '\0'; // Add null terminator at the end of the data

  btSerial.print(data); // Transmit data via Bluetooth

  delay(samplingRate); // Transmit every samplingRate milliseconds
}

void checkBluetoothCommand() {
  if (btSerial.available()) {
    char command = btSerial.read();
    switch (command) {
      case 'A':
        armSystem();
        break;
      case 'D':
        disarmSystem();
        break;
      case 'S':
        setSamplingRate();
        break;
    }
  }
}

void armSystem() {
  isArmed = true;
  Serial.println("System is armed");
}

void disarmSystem() {
  isArmed = false;
  Serial.println("System is disarmed");
}

void setSamplingRate() {
  // Read the next characters for the sampling rate
  String rateStr = "";
  while (btSerial.available()) {
    char c = btSerial.read();
    if (isDigit(c)) {
      rateStr += c;
    } else {
      break;
    }
  }
  if (rateStr.length() > 0) {
    samplingRate = rateStr.toInt() * 1000; // Convert to milliseconds
    Serial.print("Sampling rate set to ");
    Serial.print(samplingRate / 1000);
    Serial.println(" seconds");
  } else {
    Serial.println("Invalid sampling rate");
  }
}

String readTemperature() {
  String tempData = "Temperature: ";
  float temp = getTemp();
  tempData += String((int)temp);
  tempData += " C\n";

  if(temp > 43 || temp < 0) {
    tempData += "Critical Temp!\n";
    playBuzzer();
  } else {
    stopBuzzer();
  }

  return tempData;
}

String readGasConcentration() {
  String gasData = "Concentration: ";
  float gas = getGas();
  gasData += String((int)gas);
  gasData += " ppm\n";

  if(gas > 400) {
    gasData += "Critical Gas!\n";
    playBuzzer();
  } else {
    stopBuzzer();
  }

  return gasData;
}

String readHumidity() {
  String humidityData = "Humidity: ";
  float humidity = getHumidity();
  humidityData += String((int)humidity);
  humidityData += "%\n";

  return humidityData;
}

void playBuzzer() {
  tone(buzzerPin, 1000); // Play tone at 1000 Hz
}

void stopBuzzer() {
  noTone(buzzerPin); // Stop tone
}

float getTemp() {
  float offset = 0.08;
  return (getVoltage(tempPin) - offset - 0.5) * 100.0;
}

float getHumidity() {
  return dht11.readHumidity();
}

float getGas() {
  return analogRead(gasPin); // Assuming analogRead returns the gas concentration
}

float getVoltage(int pin) {
  return analogRead(pin) * (5.0 / 1023.0);
}