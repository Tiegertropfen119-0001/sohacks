package com.hackerman.sohacksrev2.ble

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for ScooterCommandRepository.
 * 
 * Tests the command generation logic to ensure commands are generated correctly
 * for various modes, speeds, and states.
 */
class ScooterCommandRepositoryTest {

    @Test
    fun testGetDrivingModeCommand_ECO() {
        val command = ScooterCommandRepository.getDrivingModeCommand(
            ScooterCommandRepository.DrivingMode.ECO
        )
        assertEquals("D707A45A00005", command)
    }

    @Test
    fun testGetDrivingModeCommand_NORMAL() {
        val command = ScooterCommandRepository.getDrivingModeCommand(
            ScooterCommandRepository.DrivingMode.NORMAL
        )
        assertEquals("D706A30001AA", command)
    }

    @Test
    fun testGetDrivingModeCommand_SPORT() {
        val command = ScooterCommandRepository.getDrivingModeCommand(
            ScooterCommandRepository.DrivingMode.SPORT
        )
        assertEquals("D706A30002AB", command)
    }

    @Test
    fun testGetDrivingModeCommand_DEVELOPER() {
        val command = ScooterCommandRepository.getDrivingModeCommand(
            ScooterCommandRepository.DrivingMode.DEVELOPER
        )
        assertEquals("D706A30003AC", command)
    }

    @Test
    fun testGetLockCommand_LOCKED() {
        val command = ScooterCommandRepository.getLockCommand(
            ScooterCommandRepository.LockState.LOCKED
        )
        assertEquals("D707A0000101A9", command)
    }

    @Test
    fun testGetLockCommand_UNLOCKED() {
        val command = ScooterCommandRepository.getLockCommand(
            ScooterCommandRepository.LockState.UNLOCKED
        )
        assertEquals("D707A0000301AB", command)
    }

    @Test
    fun testGetSpeedLimitCommand_MinSpeed() {
        val command = ScooterCommandRepository.getSpeedLimitCommand(8)
        assertEquals("D707A900005000", command)
    }

    @Test
    fun testGetSpeedLimitCommand_MaxSpeed() {
        val command = ScooterCommandRepository.getSpeedLimitCommand(30)
        assertEquals("D707A900012CDD", command)
    }

    @Test
    fun testGetSpeedLimitCommand_MidSpeed() {
        val command = ScooterCommandRepository.getSpeedLimitCommand(20)
        assertEquals("D707A90000C878", command)
    }

    @Test
    fun testGetSpeedLimitCommand_InvalidSpeed_TooLow() {
        // Should return default (8 km/h) for out of range values
        val command = ScooterCommandRepository.getSpeedLimitCommand(5)
        assertEquals("D707A900005000", command)
    }

    @Test
    fun testGetSpeedLimitCommand_InvalidSpeed_TooHigh() {
        // Should return default (8 km/h) for out of range values
        val command = ScooterCommandRepository.getSpeedLimitCommand(50)
        assertEquals("D707A900005000", command)
    }

    @Test
    fun testGetAdvancedModeCommand_Mode0() {
        val command = ScooterCommandRepository.getAdvancedModeCommand(0)
        assertNotNull(command)
        assertTrue(command!!.startsWith("D706A3"))
        assertTrue(command.endsWith("0D0A"))
    }

    @Test
    fun testGetAdvancedModeCommand_Mode1() {
        val command = ScooterCommandRepository.getAdvancedModeCommand(1)
        assertNotNull(command)
        // Expected: D706A30001AA0D0A (mode 1, checksum A9+1=AA)
        assertEquals("D706A30001AA0D0A", command)
    }

    @Test
    fun testGetAdvancedModeCommand_Mode254() {
        val command = ScooterCommandRepository.getAdvancedModeCommand(254)
        assertNotNull(command)
        assertTrue(command!!.startsWith("D706A3"))
    }

    @Test
    fun testGetAdvancedModeCommand_InvalidMode_Negative() {
        val command = ScooterCommandRepository.getAdvancedModeCommand(-1)
        assertNull(command)
    }

    @Test
    fun testGetAdvancedModeCommand_InvalidMode_TooHigh() {
        val command = ScooterCommandRepository.getAdvancedModeCommand(255)
        assertNull(command)
    }

    @Test
    fun testIsValidSpeed_ValidRanges() {
        assertTrue(ScooterCommandRepository.isValidSpeed(8))
        assertTrue(ScooterCommandRepository.isValidSpeed(15))
        assertTrue(ScooterCommandRepository.isValidSpeed(30))
    }

    @Test
    fun testIsValidSpeed_InvalidRanges() {
        assertFalse(ScooterCommandRepository.isValidSpeed(7))
        assertFalse(ScooterCommandRepository.isValidSpeed(31))
        assertFalse(ScooterCommandRepository.isValidSpeed(0))
        assertFalse(ScooterCommandRepository.isValidSpeed(-5))
    }

    @Test
    fun testIsValidMode_ValidRanges() {
        assertTrue(ScooterCommandRepository.isValidMode(0))
        assertTrue(ScooterCommandRepository.isValidMode(127))
        assertTrue(ScooterCommandRepository.isValidMode(254))
    }

    @Test
    fun testIsValidMode_InvalidRanges() {
        assertFalse(ScooterCommandRepository.isValidMode(-1))
        assertFalse(ScooterCommandRepository.isValidMode(255))
        assertFalse(ScooterCommandRepository.isValidMode(1000))
    }

    @Test
    fun testGetDrivingModeDescription_AllModes() {
        val ecoDesc = ScooterCommandRepository.getDrivingModeDescription(
            ScooterCommandRepository.DrivingMode.ECO
        )
        val normalDesc = ScooterCommandRepository.getDrivingModeDescription(
            ScooterCommandRepository.DrivingMode.NORMAL
        )
        val sportDesc = ScooterCommandRepository.getDrivingModeDescription(
            ScooterCommandRepository.DrivingMode.SPORT
        )
        val devDesc = ScooterCommandRepository.getDrivingModeDescription(
            ScooterCommandRepository.DrivingMode.DEVELOPER
        )

        assertNotNull(ecoDesc)
        assertNotNull(normalDesc)
        assertNotNull(sportDesc)
        assertNotNull(devDesc)
        
        assertTrue(ecoDesc.contains("ECO"))
        assertTrue(normalDesc.contains("Normal"))
        assertTrue(sportDesc.contains("Sport"))
        assertTrue(devDesc.contains("Developer"))
    }
}
