#include "DHT11.h"
#include <SoftwareSerial.h>
#include <Wire.h>
#include "LiquidCrystal_I2C.h"

// Initialize the DHT11 sensor
DHT11 dht11(2);
const int gasPin = A3;
const int tempPin = A1;
const int buzzerPin = 8; // Define the buzzer pin

// Initialize Bluetooth serial communication
SoftwareSerial btSerial(4, 3); // RX, TX

// Create an LCD object with I2C address 0x27, 20 columns, and 4 rows
LiquidCrystal_I2C lcd(0x27, 20, 4);

bool isArmed = false;
int samplingRate = 1000; // Default sampling rate in milliseconds

void setup() {
  Serial.begin(9600);
  btSerial.begin(9600); // Initialize Bluetooth serial communication

  Wire.begin();    // Initialize I2C communication
  lcd.init();      // Initialize the LCD
  lcd.backlight(); // Turn on the backlight
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
  lcd.setCursor(0, 3);
  lcd.print("                    "); // Clear the line
  lcd.setCursor(0, 3);
  lcd.print("System is armed");
}

void disarmSystem() {
  isArmed = false;
  Serial.println("System is disarmed");
  lcd.setCursor(0, 3);
  lcd.print("                    "); // Clear the line
  lcd.setCursor(0, 3);
  lcd.print("System is disarmed");
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
    lcd.setCursor(0, 3);
    lcd.print("                    "); // Clear the line
    lcd.setCursor(0, 3);
    lcd.print("Sampling rate: ");
    lcd.print(samplingRate / 1000);
    lcd.print(" sec");
  } else {
    Serial.println("Invalid sampling rate");
    lcd.setCursor(0, 3);
    lcd.print("                    "); // Clear the line
    lcd.setCursor(0, 3);
    lcd.print("Invalid sampling rate");
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

  Serial.print(tempData);
  lcd.setCursor(0, 0);
  lcd.print("Temp: ");
  lcd.print((int)temp);
  lcd.print(" C");

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

  Serial.print(gasData);
  lcd.setCursor(0, 1);
  lcd.print("Gas: ");
  lcd.print((int)gas);
  lcd.print(" ppm");

  return gasData;
}

String readHumidity() {
  String humidityData = "Humidity: ";
  float humidity = getHumidity();
  humidityData += String((int)humidity);
  humidityData += "%\n";

  Serial.print(humidityData);
  lcd.setCursor(0, 2);
  lcd.print("Humidity: ");
  lcd.print((int)humidity);
  lcd.print("%");

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