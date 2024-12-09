The Smart Home Monitoring System aims to create a robust and user-friendly home
automation system that integrates various sensors/actuators to monitor and control
environmental conditions. Our design philosophy emphasizes simplicity, reliability, and the
seamless integration of components to create a functional, user-friendly system. The system
uses an Arduino MEGA as the central device, which processes environmental data and
communicates with a custom Bluetooth-enable phone application. An Arduino Uno that
serves as a peripheral device, equipped with an LSM303DLHC accelerometer to detect
significant orientation changes and send acknowledgment signals to the central system.

Key Features:

    ● Central Device: Collects data from sensors, displays it on an LCD, and communicates
    with the phone application.
    
    ● Peripheral Device: Monitors environmental conditions using sensors and controls
    actuators (i.e. LEDs and buzzers)
    
    ● Phone application: Allows users to monitor sensor data, control actuators, and receive
    alerts.

Significant Algorithms and Implementations:

    ● Sensor Data Collection: The central device collects data from temperature, humidity,
    and gas sensors via a TMP36, DHT11 and MQ-2, respectively. The data is formatted
    and sent to the phone app via Bluetooth.
    
    ● Command Handling: The central and peripheral devices handle various commands
    from the phone app, such as arming/disarming the system, setting the sampling rate and
    controlling the LED. Both devices also keep track of how many commands have been
    sent since the application was launched. The user can click “Reset” from the application
    to clear the command history.
    
    ● Automatic LED Control: The peripheral device automatically turns on the LED if the
    luminosity is below a certain threshold and no command is received from the user within
    a specified timeout period.
    
    ● Bluetooth Discovery: This involves initializing the Bluetooth adapter and scanner,
    requesting necessary permissions for Bluetooth and location access, and starting the
    Bluetooth LE scan to find nearby devices. The results of the scan are handled to update
    the user interface with discovered devices, allowing users to select and connect to the
    central device.
    
    ● Bluetooth GATT Connectivity: This enables reading/writing of data via Bluetooth from
    the Central device to the phone application after being connected. It was necessary to
    identify the relevant service UUID and characteristic UUID that support both reading and
    writing operations, and contain the transmitted message in the “value” section.

Developer Tasks:

Yash Varde

 ● Central Device Development
 
     ○ Integrated the LCD display, Bluetooth communication, and HC-12 communication
     to the existing central circuit created by Noah.
     
     ○ Integrated command handling algorithms.
     
     ○ Developed logic for formatting and processing sensor data and sending it to the
     phone app.
   
 ● Phone Application and Bluetooth Communication:
 
     ○ Designed and developed the phone application.
     
     ○ Implemented Bluetooth connectivity for transmitting and receiving data wirelessly.
     
     ○ Enhanced application functionality with bidirectional communication and
     command tracking.
   
     ○ Integrated sensor data into the phone application for real-time monitoring.
     
     ○ Added features such as voluntary LED enabling and alert dialogs for warnings.
   
 ● System Testing and Integration:
 
     ○ Verified and optimized wireless communication between the central device,
     peripheral device and the phone application.
     
     ○ Conducted system-wide tests to ensure reliable functionality and accurate data
     transmission

Noah Fagerlie

 ● Sensor Logic and Circuitry:
 
     ○ Developed logic for sensor data processing and tested sensors for accuracy.
     
     ○ Rebuilt and optimized circuits for enhanced functionality.
     
     ○ Implemented thresholds for sensor readings and formatted outputs for clarity.
   
 ● Peripheral Device Development:
 
     ○ Built the peripheral circuit, incorporating the LSM303DLHC accelerometer.
     
     ○ Programmed knowledgment signals for orientation change detection.
     
     ○ Implemented and tested wired acknowledgment functionality to ensure reliable
     motion detection and communication.
