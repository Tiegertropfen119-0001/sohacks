package com.hackerman.sohacksrev2.ble

/**
 * Repository for scooter BLE commands.
 * 
 * This class centralizes all command generation logic, providing a clean API
 * for generating various scooter control commands. Commands are generated
 * algorithmically where possible to reduce code duplication and improve maintainability.
 */
object ScooterCommandRepository {

    /**
     * Available driving modes for the scooter.
     */
    enum class DrivingMode(val modeValue: Int) {
        ECO(0),
        NORMAL(1),
        SPORT(2),
        DEVELOPER(3)
    }

    /**
     * Lock states for the scooter.
     */
    enum class LockState {
        LOCKED,
        UNLOCKED
    }

    /**
     * Generates a command to set the driving mode.
     * 
     * @param mode The desired driving mode
     * @return The hex command string
     */
    fun getDrivingModeCommand(mode: DrivingMode): String {
        return when (mode) {
            DrivingMode.ECO -> "D707A45A00005"
            DrivingMode.NORMAL -> "D706A30001AA"
            DrivingMode.SPORT -> "D706A30002AB"
            DrivingMode.DEVELOPER -> "D706A30003AC"
        }
    }

    /**
     * Generates a command to lock or unlock the scooter.
     * 
     * @param lockState The desired lock state
     * @return The hex command string
     */
    fun getLockCommand(lockState: LockState): String {
        return when (lockState) {
            LockState.LOCKED -> "D707A0000101A9"
            LockState.UNLOCKED -> "D707A0000301AB"
        }
    }

    /**
     * Generates a command to set the speed limit.
     * 
     * @param speedKmh The desired speed limit in km/h (8-30)
     * @return The hex command string, or a default command if speed is out of range
     */
    fun getSpeedLimitCommand(speedKmh: Int): String {
        return when (speedKmh) {
            8 -> "D707A900005000"
            9 -> "D707A900005A0A"
            10 -> "D707A900006414"
            11 -> "D707A900006E1E"
            12 -> "D707A900007828"
            13 -> "D707A900008232"
            14 -> "D707A900008C3C"
            15 -> "D707A900009646"
            16 -> "D707A90000A050"
            17 -> "D707A90000AA5A"
            18 -> "D707A90000B464"
            19 -> "D707A90000BE6E"
            20 -> "D707A90000C878"
            21 -> "D707A90000D282"
            22 -> "D707A90000DC8C"
            23 -> "D707A90000E696"
            24 -> "D707A90000F0A0"
            25 -> "D707A90000FAAA"
            26 -> "D707A9000104B5"
            27 -> "D707A900010EBF"
            28 -> "D707A9000118C9"
            29 -> "D707A9000122D3"
            30 -> "D707A900012CDD"
            else -> "D707A900005000" // Default to 8 km/h for safety
        }
    }

    /**
     * Generates a mode command for advanced users.
     * 
     * This generates commands for modes 0-254. The command format follows a specific
     * pattern where the mode value and checksum are embedded in the command string.
     * 
     * @param mode The mode number (0-254)
     * @return The hex command string, or null if mode is out of range
     */
    fun getAdvancedModeCommand(mode: Int): String? {
        if (mode !in 0..254) {
            return null
        }

        // Generate command: D706A3[2-byte mode][checksum]0D0A
        val modeHex = String.format("%04X", mode)
        val checksum = calculateChecksum(mode)
        val checksumHex = String.format("%02X", checksum)
        
        return "D706A3$modeHex$checksumHex 0D0A".replace(" ", "")
    }

    /**
     * Calculates the checksum for a given mode value.
     * The checksum algorithm is specific to the scooter's protocol.
     * 
     * @param mode The mode value
     * @return The calculated checksum
     */
    private fun calculateChecksum(mode: Int): Int {
        // Based on analysis of the original MODE_CMDS array:
        // The checksum appears to be 0xA9 + mode
        return (0xA9 + mode) and 0xFF
    }

    /**
     * Validates if a speed value is within the acceptable range.
     * 
     * @param speedKmh The speed to validate
     * @return true if the speed is valid (8-30 km/h), false otherwise
     */
    fun isValidSpeed(speedKmh: Int): Boolean {
        return speedKmh in 8..30
    }

    /**
     * Validates if a mode value is within the acceptable range.
     * 
     * @param mode The mode to validate
     * @return true if the mode is valid (0-254), false otherwise
     */
    fun isValidMode(mode: Int): Boolean {
        return mode in 0..254
    }

    /**
     * Gets a human-readable description of a driving mode.
     * 
     * @param mode The driving mode
     * @return A string description of the mode
     */
    fun getDrivingModeDescription(mode: DrivingMode): String {
        return when (mode) {
            DrivingMode.ECO -> "ECO Mode - Maximum efficiency"
            DrivingMode.NORMAL -> "Normal Mode - Balanced performance"
            DrivingMode.SPORT -> "Sport Mode - Maximum performance"
            DrivingMode.DEVELOPER -> "Developer Mode - Advanced settings"
        }
    }
}
