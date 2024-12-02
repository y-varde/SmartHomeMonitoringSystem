#include "DHT11.h"
#include <SoftwareSerial.h>
#include <Wire.h>
#include "LiquidCrystal_I2C.h"

// Pin Definitions
const int gasPin = A3;
const int tempPin = A1;
const int buzzerPin = 8;

// Bluetooth Serial Communication
SoftwareSerial HC12(10, 11); // RX, TX

// LCD Display
LiquidCrystal_I2C lcd(0x27, 20, 4);

// Sensor and System State
DHT11 dht11(2);
bool isArmed = false;
bool fetchSensorReadingsFlag = false;
int samplingRate = 1000; // Default sampling rate in milliseconds
String peripheralWarning = ""; // Store warning message from peripheral
int phoneCommandCount = 0; // Counter for commands from phone app
int peripheralCommandCount = 0; // Counter for commands to peripheral device

void setup() {
  initializeSerial();
  initializePins();
  initializeLCD();
  Serial.println("Central ready");
}

void loop() {
  handlePeripheralWarning();
  checkBluetoothCommand();
  handleFetchSensorReadings();
  transmitSensorDataWithCheck();
}

// Setup Functions
void initializeSerial() {
  Serial.begin(9600);
  Serial1.begin(9600); // Initialize Bluetooth serial communication
  HC12.begin(9600); // Initialize HC-12 serial communication
}

void initializePins() {
  pinMode(buzzerPin, OUTPUT);
}

void initializeLCD() {
  Wire.begin();    // Initialize I2C communication
  lcd.init();      // Initialize the LCD
  lcd.backlight(); // Turn on the backlight
}

// Loop Functions
void handlePeripheralWarning() {
  if (HC12.available()) {
    peripheralWarning = HC12.readString();
    Serial.println("HC-12 Data: " + peripheralWarning);
    displayPeripheralMessage(peripheralWarning);
    peripheralCommandCount++;
    updatePeripheralCommandCount();
  }
}

void checkBluetoothCommand() {
  if (Serial1.available()) {
    char command = Serial1.read();
    Serial.print("BLE Command: ");
    Serial.println(command);
    phoneCommandCount++;
    updatePhoneCommandCount();
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
      case 'F': // New command to fetch sensor readings
        fetchSensorReadingsFlag = true;
        break;
    }
  }
}

void handleFetchSensorReadings() {
  if (fetchSensorReadingsFlag) {
    fetchSensorReadings();
    fetchSensorReadingsFlag = false; // Reset the flag
  }
}

void transmitSensorDataWithCheck() {
  int delayCount = samplingRate / 100; // Number of 100ms intervals in the sampling rate

  for (int i = 0; i < delayCount; i++) {
    delay(100); // Delay for 100 milliseconds
    if (Serial1.available()) {
      char command = Serial1.read();
      Serial.print("BLE Command: ");
      Serial.println(command);
      phoneCommandCount++;
      updatePhoneCommandCount();
      switch (command) {
        case 'A':
          armSystem();
          break;
        case 'D':
          disarmSystem();
          break;
        case 'S':
          setSamplingRate();
          return;
        case 'F': // New command to fetch sensor readings
          fetchSensorReadings();
          return; // Exit the function after sending the fetched readings
      }
    }
  }

  fetchSensorReadings(); // Fetch sensor readings after the delay
}

void transmitSensorData() {
  String data = readTemperature() + readGasConcentration() + readHumidity();

  if (peripheralWarning.length() > 0) {
    data += "Warning: " + peripheralWarning + "\n";
    peripheralWarning = ""; // Clear the warning after sending
  }

  Serial1.print(data); // Transmit data via Bluetooth
}

// Helper Functions
void fetchSensorReadings() {
  String data = readTemperature() + readGasConcentration() + readHumidity();
  Serial1.print(data); // Transmit data via Bluetooth Low Energy
  delay(100); // Add delay after Bluetooth transmission
}

String readTemperature() {
  float temp = getTemp();
  String tempData = "Temperature: " + String((int)temp) + " C\n";
  updateLCDLine(0, "Temp: " + String((int)temp) + " C");
  checkCritical(temp, 43, 0);
  return tempData;
}

String readGasConcentration() {
  float gas = getGas();
  String gasData = "Concentration: " + String((int)gas) + " ppm\n";
  updateLCDLine(1, "Gas: " + String((int)gas) + " ppm");
  checkCritical(gas, 400, 0);
  return gasData;
}

String readHumidity() {
  float humidity = getHumidity();
  String humidityData = "Humidity: " + String((int)humidity) + "%\n";
  updateLCDLine(2, "Humidity: " + String((int)humidity) + "%");
  return humidityData;
}

void updateLCDLine(int line, String message) {
  lcd.setCursor(0, line);
  lcd.print("                    "); // Clear the line
  lcd.setCursor(0, line);
  lcd.print(message);
}

void updatePhoneCommandCount() {
  updateLCDLine(1, "Phone Cmds: " + String(phoneCommandCount));
}

void updatePeripheralCommandCount() {
  updateLCDLine(2, "Peripheral Cmds: " + String(peripheralCommandCount));
}

void displayPeripheralMessage(String message) {
  updateLCDLine(3, message);
}

void checkCritical(float value, float upperLimit, float lowerLimit) {
  if (value > upperLimit || value < lowerLimit) {
    playBuzzer();
  } else {
    stopBuzzer();
  }
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

void armSystem() {
  isArmed = true;
  Serial.println("System is armed");
  updateLCDLine(3, "System is armed");
  HC12.println("A"); // Send arm command to peripheral
}

void disarmSystem() {
  isArmed = false;
  Serial.println("System is disarmed");
  updateLCDLine(3, "System is disarmed");
  HC12.println("D"); // Send disarm command to peripheral
}

void setSamplingRate() {
  // Read the next characters for the sampling rate
  String rateStr = "";
  while (Serial1.available()) {
    char c = Serial1.read();
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
    updateLCDLine(3, "Sampling rate: " + String(samplingRate / 1000) + " sec");
  } else {
    Serial.println("Invalid sampling rate");
    updateLCDLine(3, "Invalid sampling rate");
  }
}