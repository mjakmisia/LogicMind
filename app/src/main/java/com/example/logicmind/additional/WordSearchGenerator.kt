package com.example.logicmind.additional

import android.util.Log

object WordSearchGenerator {

    // Możliwe kierunki słów
    private enum class Direction(val dx: Int, val dy: Int) {
        HORIZONTAL(1, 0),
        VERTICAL(0, 1),
        DIAGONAL_DOWN(1, 1)
    }

    // Klasa do przechowywania wyniku
    data class Board(
        val size: Int,
        val grid: List<List<Char>>, // Gotowa siatka 2D z literami
        val placedWords: List<String> // Słowa które umieszczono
    )

    fun generate(size: Int, words: List<String>): Board? {
        var attempts = 0
        while (attempts < 10) { // Próbuj wygenerować planszę max 10 razy
            val grid: Array<Array<Char?>> = Array(size) { Array(size) { null } }
            val placedWords = mutableListOf<String>()

            var allWordsPlaced = true
            for (word in words.shuffled()) { // Tasujemy, by kolejność była różna
                if (!tryPlaceWord(grid, word, size)) {
                    allWordsPlaced = false
                    break // Nie udało się umieścić słowa, przerwij i spróbuj od nowa
                }
                placedWords.add(word)
            }

            if (allWordsPlaced) {
                // Wypełnij puste pola i zwróć planszę
                val finalGrid = fillEmptyCells(grid, size)
                return Board(size, finalGrid, placedWords)
            }
            attempts++
        }

        Log.e("WordSearchGenerator", "Nie udało się wygenerować planszy po $attempts próbach.")
        return null // Nie udało się
    }

    private fun tryPlaceWord(grid: Array<Array<Char?>>, word: String, size: Int): Boolean {

        val positions = (0 until size * size).toList().shuffled()

        for (pos in positions) {
            val startRow = pos / size
            val startCol = pos % size

            // Teraz dla każdej nowej pozycji losujemy nową kolejność kierunków
            val directions = Direction.entries.shuffled()

            for (dir in directions) {
                if (canPlace(grid, word, size, startRow, startCol, dir)) {
                    place(grid, word, startRow, startCol, dir)
                    return true // Słowo umieszczone pomyślnie
                }
            }
        }
        return false // Nie znaleziono miejsca dla tego słowa
    }

    // Sprawdza, czy słowo zmieści się i nie koliduje
    private fun canPlace(
        grid: Array<Array<Char?>>,
        word: String,
        size: Int,
        startRow: Int,
        startCol: Int,
        dir: Direction
    ): Boolean {
        for (i in word.indices) {
            val row = startRow + i * dir.dy
            val col = startCol + i * dir.dx

            // Sprawdzenie granic planszy
            if (row !in 0 until size || col !in 0 until size) {
                return false
            }

            // Sprawdzenie kolizji
            val existingChar = grid[row][col]
            if (existingChar != null && existingChar != word[i]) {
                return false
            }
        }
        return true // Miejsce jest dobre
    }

    // Fizycznie umieszcza słowo na siatce
    private fun place(
        grid: Array<Array<Char?>>,
        word: String,
        startRow: Int,
        startCol: Int,
        dir: Direction
    ) {
        for (i in word.indices) {
            val row = startRow + i * dir.dy
            val col = startCol + i * dir.dx
            grid[row][col] = word[i]
        }
    }

    // Wypełnia puste komórki losowymi literami A-Z
    private fun fillEmptyCells(grid: Array<Array<Char?>>, size: Int): List<List<Char>> {
        return List(size) { row ->
            List(size) { col ->
                grid[row][col] ?: ('A'..'Z').random()
            }
        }
    }
}