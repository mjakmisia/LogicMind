package com.example.logicmind.additional

import java.util.Locale

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
        val grid: List<List<Char>>,
        val placedWords: List<String>
    )

    //Tworzy planszę
    fun generate(size: Int, words: List<String>, lang: String): Board? {
        var attempts = 0
        while (attempts < 10) {
            val grid: Array<Array<Char?>> = Array(size) { Array(size) { null } }
            val placedWords = mutableListOf<String>()

            var allWordsPlaced = true
            // Spróbuj umieścić każde słowo
            for (word in words.shuffled()) {
                if (!tryPlaceWord(grid, word, size)) {
                    allWordsPlaced = false
                    break // nie udało się - zacznij od nowa
                }
                placedWords.add(word)
            }

            if (allWordsPlaced) {
                // Wszystkie słowa na miejscu, wypełnij puste pola
                val finalGrid = fillEmptyCells(grid, size, lang)
                return Board(size, finalGrid, placedWords)
            }
            attempts++
        }

        return null
    }

    // Próbuje znaleźć losowe, pasujące miejsce dla słowa
    private fun tryPlaceWord(grid: Array<Array<Char?>>, word: String, size: Int): Boolean {
        val positions = (0 until size * size).toList().shuffled()
        for (pos in positions) {
            val startRow = pos / size
            val startCol = pos % size
            val directions = Direction.entries.shuffled()
            for (dir in directions) {
                // Sprawdza czy dane miejsce pasuje
                if (canPlace(grid, word, size, startRow, startCol, dir)) {
                    place(grid, word, startRow, startCol, dir)
                    return true
                }
            }
        }
        return false
    }

    // Sprawdza czy słowo zmieści się w danym miejscu
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

            // Sprawdzenie kolizji - dopuszczalne jeśli litery się zgadzają
            val existingChar = grid[row][col]
            if (existingChar != null && existingChar != word[i]) {
                return false
            }
        }
        return true
    }

    // Umieszcza litery słowa na siatce
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

    // Wypełnia puste pola losowymi literami
    private fun fillEmptyCells(grid: Array<Array<Char?>>, size: Int, lang: String): List<List<Char>> {

        val alphabetEN = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        val alphabetPL = "ABCDEFGHIJKLMNOPQRSTUVWXYZĄĆĘŁŃÓŚŹŻ"

        val alphabetChars = if (lang.lowercase(Locale.ROOT) == "pl") {
            alphabetPL.toList()
        } else {
            alphabetEN.toList()
        }

        return List(size) { row ->
            List(size) { col ->
                // Losuje literę z wybranego alfabetu
                grid[row][col] ?: alphabetChars.random()
            }
        }
    }
}