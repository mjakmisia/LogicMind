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
        // zerujemy liczniki żeby testy były oddzielne
        manager.callResetAccuracyCounters()
    }

    /*
    * Sprawdzanie poprawności przy zerowej liczbie prób
     */
    @Test
    fun calculateAccuracy_zeroAttempts_returnsZero() {
        val expected = 0.0
        val actual = manager.callCalculateAccuracy()
        assertEquals(expected, actual, 0.001, "Celność powinna wynieść 0.0 przy braku prób")
    }

    /*
    * Sprawdzanie poprawności przy max sukcesu
     */
    @Test
    fun calculateAccuracy_fullSucces_returns100() {
        // 5 prób, 5 sukcesów
        manager.testTotalAttempts = 5
        manager.testSuccessfulAttempts = 5
        val expected = 100.0
        val actual = manager.callCalculateAccuracy()
        assertEquals(expected, actual, 0.001, "Celność powinna wynieść 100.0 przy pełnym sukcesie")
    }

    /*
    * Sprawdzanie poprawności przy połowie sukcesu
     */
    @Test
    fun calculateAccuracy_halfSucces_returnsFifty() {
        // 10 prób, 5 sukcesów
        manager.testTotalAttempts = 10
        manager.testSuccessfulAttempts = 5
        val expected = 50.0
        val actual = manager.callCalculateAccuracy()
        assertEquals(expected, actual, 0.001, "Celność powinna wynieść 50.0 przy połowie sukcesów")
    }

    /*
    * Sprawdzanie czy zwiększa się attempts przy jednej porażce ale nie successful attempts
     */
    @Test
    fun registerAttempts_unsuccessful_increaseTotalOnly() {
        // 0 prób, 0 sukcesów
        manager.testTotalAttempts = 0
        manager.testSuccessfulAttempts = 0

        // rejestracja nieudanej próby
        manager.callRegisterAttempt(false)

        assertEquals(1, manager.testTotalAttempts, "totalAttempts powinno wzrosnąć do 1")
        assertEquals(0, manager.testSuccessfulAttempts, "successfulAttempts powinno pozostać na 0")
    }

    /*
    * Sprawdzanie czy zwiększa się attempts przy jednej porażce i jednym sukcesie
     */
    @Test
    fun registerAttempts_successful_increaseBoth() {
        // 0 prób, 0 sukcesów
        manager.testTotalAttempts = 0
        manager.testSuccessfulAttempts = 0

        // rejestracja udanej próby
        manager.callRegisterAttempt(true)

        assertEquals(1, manager.testTotalAttempts, "totalAttempts powinno wzrosnąć do 1")
        assertEquals(1, manager.testSuccessfulAttempts, "successfulAttempts powinno wzrosnąć do 1")
    }
}

private class TestableGameStatsManager : GameStatsManager() {

    // Ujawniamy pola totalAttempts i successfulAttempts
    var testTotalAttempts: Int
        get() = totalAttempts // totalAttempts jest teraz w GameStatsManager
        set(value) {
            totalAttempts = value
        }

    var testSuccessfulAttempts: Int
        get() = successfulAttempts
        set(value) {
            successfulAttempts = value
        }

    // ujawniamy metody
    fun callRegisterAttempt(isSuccessful: Boolean) = registerAttempt(isSuccessful)
    fun callCalculateAccuracy(): Double = calculateAccuracy()
    fun callResetAccuracyCounters() = resetAccuracyCounters()
}