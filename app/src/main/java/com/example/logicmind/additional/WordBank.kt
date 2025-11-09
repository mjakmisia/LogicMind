package com.example.logicmind.additional

import java.util.Locale

object WordBank {
    private val words = mapOf(
        "pl" to mapOf(
            3 to listOf("KOT", "LAS", "DOM", "RAK"),
            4 to listOf("MAMA", "TATA", "KAWA", "RYBA"),
            5 to listOf("BANAN", "STATEK", "DROGA", "FALA")
        ),
        "en" to mapOf(
            3 to listOf("CAT", "SUN", "DOG", "RUN"),
            4 to listOf("FOUR", "TREE", "ROAD", "FISH"),
            5 to listOf("APPLE", "WATER", "BOARD", "GAME")
        )
    )

    /**
     * Pobiera losowe słowa, filtrując je wg maksymalnej długości.
     *
     * @param lang Język (np. "pl", "en").
     * @param maxLength Maksymalna długość słowa (np. 5 dla planszy 5x5).
     * @param count Ile słów wylosować.
     * @return Lista unikalnych, losowych słów.
     */
    fun getWords(lang: String, maxLength: Int, count: Int): List<String> {
        val langKey = lang.lowercase(Locale.ROOT)
        val availableWords = words[langKey]?.flatMap { (length, wordList) ->
            if (length <= maxLength) wordList else emptyList()
        } ?: emptyList()

        if (availableWords.isEmpty()) {
            return emptyList() // Nie znaleziono słów
        }

        // Tasujemy i bierzemy 'count' unikalnych słów
        return availableWords.shuffled().take(count)
    }
}