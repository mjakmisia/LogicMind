package com.example.logicmind.activities

import com.example.logicmind.R

//TODO: każda z gier będzie miała docelowo takie samo introActivity, popraw jak wszystkie gry będą gotowe
data class GameOption(
    val gameId: String,
    val title: Int,
    val iconRes: Int,
    val introActivity: Class<*>?
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
                    GameOption("card_match", R.string.card_match, R.drawable.image_card_match,
                        GameIntroActivity::class.java),
                    GameOption("color_sequence", R.string.color_sequence, R.drawable.image_color_sequence,
                        GameIntroActivity::class.java)
                )
            )
            "reasoning" -> GameCategoryConfig(
                colorRes = R.color.category_pink_dark_mode,
                nameRes = R.string.category_reasoning,
                games = listOf(
                    GameOption("number_addition", R.string.number_addition, R.drawable.image_number_addition,
                        GameIntroActivity::class.java),
                    GameOption("path_change", R.string.path_change, R.drawable.image_path_change,
                        GameIntroActivity::class.java) //obrazek do zmiany
                )
            )
            "attention" -> GameCategoryConfig(
                colorRes = R.color.category_yellow_dark_mode,
                nameRes = R.string.category_attention,
                games = listOf(
                    GameOption(gameId = "word_search", R.string.word_search, R.drawable.image_word_search,
                        GameIntroActivity::class.java),
                    GameOption(gameId = "fruit_sort", R.string.fruit_sort, R.drawable.image_fruit_sort, null) //obrazek do zmiany
                )
            )
            "coordination" -> GameCategoryConfig(
                colorRes = R.color.category_blue_dark_mode,
                nameRes = R.string.category_coordination,
                games = listOf(
                    GameOption("road_dash", R.string.road_dash, R.drawable.ic_close, null),
                    GameOption("symbol_race", R.string.symbol_race, R.drawable.image_symbol_race,
                        GameIntroActivity::class.java)
                )
            )
            else -> GameCategoryConfig(
                colorRes = R.color.grey,
                nameRes = R.string.unknown,
                games = emptyList()
            )
        }
    }
}