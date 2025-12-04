package com.example.logicmind.common

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test


@DisplayName("Testy statystyk - metod calculateAccuracy i registerAttempt")
class GameStatsManagerTest {

    private lateinit var manager: TestableGameStatsManager

    @BeforeEach
    fun setup() {
        manager = TestableGameStatsManager()
        manager.callResetAccuracyCounters()
    }

    @Test
    fun calculateAccuracy_zeroAttempts_returnsZero() {
        val expected = 0.0
        val actual = manager.callCalculateAccuracy()
        assertEquals(expected, actual, 0.001, "Celność powinna wynieść 0.0 przy braku prób")
    }


    @Test
    fun calculateAccuracy_fullSucces_returns100() {
        manager.testTotalAttempts = 5
        manager.testSuccessfulAttempts = 5
        val expected = 100.0
        val actual = manager.callCalculateAccuracy()
        assertEquals(expected, actual, 0.001, "Celność powinna wynieść 100.0 przy pełnym sukcesie")
    }

    @Test
    fun calculateAccuracy_halfSucces_returnsFifty() {
        manager.testTotalAttempts = 10
        manager.testSuccessfulAttempts = 5
        val expected = 50.0
        val actual = manager.callCalculateAccuracy()
        assertEquals(expected, actual, 0.001, "Celność powinna wynieść 50.0 przy połowie sukcesów")
    }

    @Test
    fun registerAttempts_unsuccessful_increaseTotalOnly() {
        manager.testTotalAttempts = 0
        manager.testSuccessfulAttempts = 0

        manager.callRegisterAttempt(false)

        assertEquals(1, manager.testTotalAttempts, "totalAttempts powinno wzrosnąć do 1")
        assertEquals(0, manager.testSuccessfulAttempts, "successfulAttempts powinno pozostać na 0")
    }

    @Test
    fun registerAttempts_successful_increaseBoth() {
        manager.testTotalAttempts = 0
        manager.testSuccessfulAttempts = 0

        manager.callRegisterAttempt(true)

        assertEquals(1, manager.testTotalAttempts, "totalAttempts powinno wzrosnąć do 1")
        assertEquals(1, manager.testSuccessfulAttempts, "successfulAttempts powinno wzrosnąć do 1")
    }
}

private class TestableGameStatsManager : GameStatsManager() {
    var testTotalAttempts: Int
        get() = totalAttempts
        set(value) {
            totalAttempts = value
        }

    var testSuccessfulAttempts: Int
        get() = successfulAttempts
        set(value) {
            successfulAttempts = value
        }

    fun callRegisterAttempt(isSuccessful: Boolean) = registerAttempt(isSuccessful)
    fun callCalculateAccuracy(): Double = calculateAccuracy()
    fun callResetAccuracyCounters() = resetAccuracyCounters()
}