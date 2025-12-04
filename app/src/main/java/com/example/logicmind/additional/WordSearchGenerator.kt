package com.example.logicmind.additional

import java.util.Locale

object WordSearchGenerator {
    private enum class Direction(val dx: Int, val dy: Int) {
        HORIZONTAL(1, 0),
        VERTICAL(0, 1),
        DIAGONAL_DOWN(1, 1)
    }
    data class Board(
        val size: Int,
        val grid: List<List<Char>>,
        val placedWords: List<String>
    )

    fun generate(size: Int, words: List<String>, lang: String): Board? {
        var attempts = 0
        while (attempts < 10) {
            val grid: Array<Array<Char?>> = Array(size) { Array(size) { null } }
            val placedWords = mutableListOf<String>()

            var allWordsPlaced = true
            for (word in words.shuffled()) {
                if (!tryPlaceWord(grid, word, size)) {
                    allWordsPlaced = false
                    break
                }
                placedWords.add(word)
            }

            if (allWordsPlaced) {
                val finalGrid = fillEmptyCells(grid, size, lang)
                return Board(size, finalGrid, placedWords)
            }
            attempts++
        }

        return null
    }

    private fun tryPlaceWord(grid: Array<Array<Char?>>, word: String, size: Int): Boolean {
        val positions = (0 until size * size).toList().shuffled()
        for (pos in positions) {
            val startRow = pos / size
            val startCol = pos % size
            val directions = Direction.entries.shuffled()
            for (dir in directions) {
                if (canPlace(grid, word, size, startRow, startCol, dir)) {
                    place(grid, word, startRow, startCol, dir)
                    return true
                }
            }
        }
        return false
    }

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

            if (row !in 0 until size || col !in 0 until size) {
                return false
            }

            val existingChar = grid[row][col]
            if (existingChar != null && existingChar != word[i]) {
                return false
            }
        }
        return true
    }

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

    private fun fillEmptyCells(grid: Array<Array<Char?>>, size: Int, lang: String): List<List<Char>> {

        val alphabetEN = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        val alphabetPL = "ABCDEFGHIJKLMNOPRSTUWYZĄĆĘŁŃÓŚŹŻ"

        val alphabetChars = if (lang.lowercase(Locale.ROOT) == "pl") {
            alphabetPL.toList()
        } else {
            alphabetEN.toList()
        }

        return List(size) { row ->
            List(size) { col ->
                grid[row][col] ?: alphabetChars.random()
            }
        }
    }
}