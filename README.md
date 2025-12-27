# Scooter BLE Tuning App

A Bluetooth Low Energy (BLE) application for connecting to and configuring electric scooters. This app provides an interface for adjusting scooter settings including speed limits, driving modes, and lock states.

## ⚠️ Important Disclaimer

- Use at **your own risk**
- The developer assumes **no liability** for any damages, malfunctions, or consequences
- Modifications may **void your warranty** and **invalidate insurance coverage**
- Modified scooters may be **illegal on public roads** - check local regulations
- **Not for use in public traffic**

## Technical Overview

### Architecture

The application follows a clean architecture pattern with clear separation of concerns:

- **UI Layer** (`MainActivity`, `DeviceSelectionActivity1`): Handles user interaction and display
- **BLE Layer** (`BleManager`): Manages Bluetooth connections and communication
- **Data Layer** (`ScooterCommandRepository`): Centralizes command generation logic
- **Model Layer** (`Device`): Defines data structures

### Key Components

#### BleManager
Encapsulates all Bluetooth Low Energy operations:
- Connection management (connect/disconnect)
- Service discovery
- Characteristic read/write operations
- GATT callback handling
- Error handling and logging

#### ScooterCommandRepository
Provides a clean API for generating scooter commands:
- Driving mode commands (ECO, Normal, Sport, Developer)
- Speed limit commands (8-30 km/h)
- Lock/unlock commands
- Advanced mode commands (0-254)
- Command validation

#### DeviceAdapter
RecyclerView adapter for displaying discovered BLE devices with automatic de-duplication.

### Features

- **Device Scanning**: Discover nearby BLE devices with advanced filtering options
- **Connection Management**: Persistent connection with auto-reconnect capability
- **Driving Modes**: Switch between ECO, Normal, Sport, and Developer modes
- **Speed Control**: Set speed limits from 8-30 km/h with slider or preset buttons
- **Lock Control**: Lock and unlock the scooter remotely
- **Advanced Mode**: Access to 254 additional configuration modes
- **Manual Commands**: Send custom hex commands for advanced users

### Build Requirements

- Android SDK (API 25+)
- Gradle 8.7+
- Kotlin 1.9.0
- Android Gradle Plugin 8.5.1

### Dependencies

```kotlin
// Core Android libraries
androidx.core:core-ktx:1.13.1
androidx.appcompat:appcompat:1.7.0
com.google.android.material:material:1.12.0
androidx.activity:activity:1.9.1
androidx.constraintlayout:constraintlayout:2.1.4

// Testing
junit:junit:4.13.2
androidx.test.ext:junit:1.2.1
androidx.test.espresso:espresso-core:3.6.1
```

### Permissions

The app requires the following permissions:
- `BLUETOOTH_SCAN` (Android 12+)
- `BLUETOOTH_CONNECT` (Android 12+)
- `ACCESS_FINE_LOCATION` (Android 11 and below)

### Building the Project

```bash
# Clone the repository
git clone https://github.com/Tiegertropfen119-0001/sohacks.git
cd sohacks

# Make gradlew executable
chmod +x gradlew

# Build the project
./gradlew build

# Run tests
./gradlew test

# Build APK
./gradlew assembleDebug
```

### Running Tests

```bash
# Run unit tests
./gradlew test

# Run instrumented tests (requires connected device/emulator)
./gradlew connectedAndroidTest
```

### Code Structure

```
app/src/main/java/com/hackerman/sohacksrev2/
├── ble/
│   ├── BleManager.kt              # BLE connection and communication
│   └── ScooterCommandRepository.kt # Command generation logic
├── MainActivity.kt                 # Main UI and controller
├── DeviceSelectionActivity1.kt    # Device scanning and selection
├── DeviceAdapter.kt               # RecyclerView adapter for devices
└── Device.kt                      # Device data model
```

### Error Handling

The application includes comprehensive error handling:
- Connection failures are caught and reported to the UI
- Invalid commands are validated before sending
- Bluetooth adapter availability is checked
- Permission issues are handled gracefully
- All BLE operations include try-catch blocks

### Logging

The app uses Android's Log system with the following tags:
- `BleManager`: BLE connection and communication logs
- `SCAN`: Device scanning logs

Enable verbose logging with:
```bash
adb logcat -s BleManager:V SCAN:V
```

## Download

**SOHacksRev2.1.apk - Latest**  
https://mega.nz/file/WZclDCyQ#xpBxOxs2kqmBCe3byaXaSk4YEAGjYEIbs26WR0hTTPo  
**SHA256:** `60DF0DF52366A4BA35D8F42A43032679E841C3786FF00A56D164D930735FC985`

**SOHacksRev2.0.apk - Previous**  
https://mega.nz/file/zRkUULRZ#Dr4IT4mq5uV0l0vknA0xueJ9GO4yRuJnnB6IgvvJ60Q  
**SHA256:** `5780F4BE9AC85254C045ED0EF99FA741FFD8477C18885B04A1B06DD92ACE1128`

## Contributing

Contributions are welcome! Please:
1. Fork the repository
2. Create a feature branch
3. Add tests for new functionality
4. Ensure all tests pass
5. Submit a pull request

### Code Standards

- Use KDoc comments for public APIs
- Follow Kotlin coding conventions
- Write unit tests for business logic
- Handle errors gracefully with proper logging
- Maintain separation of concerns

## License

See [LICENSE](LICENSE) file for details.

## Security

- Never commit sensitive data or credentials
- All BLE communications should be validated
- Input sanitization is performed on all user inputs
- Commands are validated before transmission

## Troubleshooting

### Connection Issues
- Ensure Bluetooth is enabled
- Check that location services are enabled (required for BLE scanning on some devices)
- Verify the scooter is powered on and in range
- Try disconnecting and reconnecting

### Permission Issues
- Grant all requested permissions in Android settings
- On Android 12+, ensure BLUETOOTH_SCAN and BLUETOOTH_CONNECT are granted
- On older Android versions, ensure ACCESS_FINE_LOCATION is granted

### Scanning Issues
- Enable location services
- Ensure Bluetooth is enabled
- Check that the scooter is advertising
- Try restarting the app

## Version History

- **Rev 2.1**: Improved stability and error handling
- **Rev 2.0**: Initial public release
