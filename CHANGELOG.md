# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [2.3] - 2025-12-27

### Added
- **BleManager class**: New abstraction layer for all Bluetooth Low Energy operations
  - Connection state management with listeners
  - Automatic service discovery
  - Data reception callbacks
  - Comprehensive error handling and logging
- **ScooterCommandRepository**: Centralized command generation with algorithmic approach
  - Driving mode commands (ECO, Normal, Sport, Developer)
  - Speed limit commands (8-30 km/h)
  - Lock/unlock commands
  - Advanced mode commands (0-254) generated algorithmically
  - Input validation for all command types
- **Unit tests**: 13 comprehensive test cases for command generation logic
  - Validates correct hex format generation
  - Tests boundary conditions and edge cases
  - Verifies checksum calculations
- **Helper methods** in Device model:
  - `getDisplayName()`: Returns user-friendly device name
  - `hasName()`: Checks if device has advertised name
- **MAX_MODE constant**: Named constant for mode range validation

### Changed
- **MainActivity refactored** from 606 to 462 lines (-24% reduction)
  - Separated BLE operations into BleManager
  - Delegated command generation to ScooterCommandRepository
  - Improved code organization with extracted helper methods
  - All UI text standardized to English (was mixed German/English)
- **DeviceSelectionActivity1 enhanced**:
  - Comprehensive KDoc documentation for all methods
  - Improved error handling with try-catch blocks
  - Better code organization with method extraction
  - Enhanced user feedback messages
- **Device and DeviceAdapter** improved with comprehensive documentation
- **README.md** completely rewritten with:
  - Professional technical documentation
  - Architecture overview and component descriptions
  - Build requirements and instructions
  - Troubleshooting guide
  - Code structure documentation

### Removed
- **254-element hardcoded MODE_CMDS array** replaced with algorithmic generation
- **Raw BLE handling code** from MainActivity (moved to BleManager)
- **Mixed German/English text** replaced with consistent English

### Fixed
- **Advanced mode command generation**: Corrected hex formatting to match protocol specification
- **Null safety**: Removed unsafe null assertions (!!) in tests
- **Error handling**: Added comprehensive try-catch blocks for all BLE operations
- **Permission handling**: Proper runtime permission checks for Android 12+

### Documented
- All public APIs with KDoc comments explaining parameters, return values, and behavior
- Protocol-specific quirks (e.g., ECO mode uses different command format)
- Architecture decisions and design patterns
- Error handling strategies
- Testing approach and coverage

### Security
- Input validation for all user inputs (speed, mode, hex commands)
- Range checking for command parameters (speed: 8-30 km/h, mode: 0-254)
- Safe hex string parsing with error handling

## [2.1] - Previous Release

### Changed
- Improved stability and error handling

## [2.0] - Initial Release

### Added
- Initial public release
- Basic BLE connectivity
- Scooter control features
- Mode selection
- Speed adjustment
