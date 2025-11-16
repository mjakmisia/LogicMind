package com.example.logicmind.additional

import com.example.logicmind.R

data class GameOption(
    val gameId: String,
    val title: Int,
    val iconRes: Int
)

data class GameCategoryConfig(
    val colorRes: Int,
    val nameRes: Int,
    val games: List<GameOption>
)

object GameConfigProvider {
    fun getConfig(categoryId: String): GameCategoryConfig {
        return when (categoryId) {
            "memory" -> GameCategoryConfig(
                colorRes = R.color.category_green_dark_mode,
                nameRes = R.string.category_memory,
                games = listOf(
                    GameOption("card_match", R.string.card_match, R.drawable.image_card_match),
                    GameOption("color_sequence", R.string.color_sequence, R.drawable.image_color_sequence)
                )
            )
            "reasoning" -> GameCategoryConfig(
                colorRes = R.color.category_pink_dark_mode,
                nameRes = R.string.category_reasoning,
                games = listOf(
                    GameOption("number_addition", R.string.number_addition, R.drawable.image_number_addition),
                    GameOption("path_change", R.string.path_change, R.drawable.image_path_change)
                )
            )
            "attention" -> GameCategoryConfig(
                colorRes = R.color.category_yellow_dark_mode,
                nameRes = R.string.category_attention,
                games = listOf(
                    GameOption(gameId = "word_search", R.string.word_search, R.drawable.image_word_search),
                    GameOption(gameId = "left_or_right", R.string.left_or_right, R.drawable.image_left_or_right)
                )
            )
            "coordination" -> GameCategoryConfig(
                colorRes = R.color.category_blue_dark_mode,
                nameRes = R.string.category_coordination,
                games = listOf(
                    GameOption("road_dash", R.string.road_dash, R.drawable.ic_close), //obrazek do ustawienia
                    GameOption("symbol_race", R.string.symbol_race, R.drawable.image_symbol_race)
                )
            )
            else -> GameCategoryConfig(
                colorRes = R.color.gray,
                nameRes = R.string.unknown,
                games = emptyList()
            )
        }
    }
}