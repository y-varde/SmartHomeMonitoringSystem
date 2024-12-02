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
int delayCount = 0;
int currentSample = 0;
String peripheralWarning = ""; // Store warning message from peripheral
int phoneCommandCount = 0; // Counter for commands from phone app
int peripheralCommandCount = 0; // Counter for commands to peripheral device

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

void setup() {
  initializeSerial();
  initializePins();
  initializeLCD();
  delayCount = samplingRate / 100;
  clearBluetoothBuffer();
  Serial.println("Central ready");
}

void loop() {

  currentSample++;
  if (currentSample >= delayCount) {

    currentSample = 0;
    fetchSensorReadings(); // Transmit data after the full delay

  }

  checkBluetoothCommand();
  handlePeripheralWarning();

  delay(100); // Delay for 100 milliseconds
}

void clearBluetoothBuffer() {
  while (Serial1.available()) {
    Serial1.read();
  }
}

// Loop Functions
void handlePeripheralWarning() {
  if (HC12.available()) {
    String message = HC12.readStringUntil('\n');
    Serial.println("HC-12 Data: " + message);
    if (message.startsWith("CmdCount: ")) {
      peripheralCommandCount = message.substring(10).toInt();
      updateCommandCount();
    } else {
      peripheralWarning = message;
      displayPeripheralMessage(peripheralWarning);
    }
  }
}

void checkBluetoothCommand() {
  if (Serial1.available()) {
    char command = Serial1.read();
    Serial.print("BLE Command: ");
    Serial.println(command);
    phoneCommandCount++;
    updateCommandCount();
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
        fetchSensorReadings();
        break;
      case 'R': // Refresh command
        resetCommandCounts();
        break;
    }
  }
}

void resetCommandCounts() {
  phoneCommandCount = 0;
  peripheralCommandCount = 0;
  HC12.println("R"); // Send refresh command to peripheral
  updateCommandCount();
  Serial.println("Command counts reset");
}

// Helper Functions
void fetchSensorReadings() {
  float temp = getTemp();
  float gas = getGas();
  float humidity = getHumidity();

  String dataPhone = readTemperature();
  dataPhone += readGasConcentration();
  dataPhone += readHumidity();
  
  if (peripheralWarning.length() > 0) {
    dataPhone += "Warning: " + peripheralWarning + "\n";
    peripheralWarning = ""; // Clear the warning after sending
  }

  dataPhone += '\0'; // Add null terminator at the end of the data

  Serial.println("Sending data to phone:");
  Serial.println(dataPhone); // Print to Serial Monitor for debugging

  Serial1.print(dataPhone); // Transmit data via Bluetooth Low Energy

  String dataLCD = "T:" + String((int)temp) + "C," + "H:" + String((int)humidity) + "%," + "C:" + String((int)gas) + "ppm";
  updateLCDLine(0, dataLCD);
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

  Serial.print(humidityData);

  return humidityData;
}

void updateLCDLine(int line, String message) {
  lcd.setCursor(0, line);
  lcd.print("                    "); // Clear the line
  lcd.setCursor(0, line);
  lcd.print(message);
}

void updateCommandCount() {
  String dataLCD = "MC:" + String(phoneCommandCount) + ",PC:" + String(peripheralCommandCount);
  updateLCDLine(1, dataLCD);
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
    delayCount = samplingRate / 100;
    Serial.print("Sampling rate set to ");
    Serial.print(samplingRate / 1000);
    Serial.println(" seconds");
    updateLCDLine(3, "Sampling rate: " + String(samplingRate / 1000) + " sec");
  } else {
    Serial.println("Invalid sampling rate");
    updateLCDLine(3, "Invalid sampling rate");
  }
  HC12.println("S" + rateStr); // Send sampling rate command to peripheral
}