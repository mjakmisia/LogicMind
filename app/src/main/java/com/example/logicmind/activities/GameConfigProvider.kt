package com.example.logicmind.activities

import com.example.logicmind.R

data class GameOption(
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
            "attention" -> GameCategoryConfig(
                colorRes = R.color.category_yellow,
                nameRes = R.string.category_attention,
                games = listOf(
                    GameOption(R.string.word_search, R.drawable.image_word_search, IntroWordSearchActivity::class.java),
                    GameOption(R.string.fruit_sorting, R.drawable.image_fruit_sorting, null) //obrazek do zmiany
                )
            )
            "memory" -> GameCategoryConfig(
                colorRes = R.color.category_green,
                nameRes = R.string.category_memory,
                games = listOf(
                    GameOption(R.string.color_sequence, R.drawable.image_color_sequence, null),
                    GameOption(R.string.matching_pairs, R.drawable.image_matching_pairs,
                        IntroMatchingPairsActivity::class.java)
                )
            )
            "reasoning" -> GameCategoryConfig(
                colorRes = R.color.category_pink,
                nameRes = R.string.category_reasoning,
                games = listOf(
                    GameOption(R.string.number_addition, R.drawable.image_number_addition,
                        IntroNumberAdditionActivity::class.java),
                    GameOption(R.string.unknown, R.drawable.image_path_change, null) //obrazek do zmiany (?)
                )
            )
            "coordination" -> GameCategoryConfig(
                colorRes = R.color.category_blue,
                nameRes = R.string.category_coordination,
                games = listOf(
                    GameOption(R.string.unknown, R.drawable.ic_close, null),
                    GameOption(R.string.unknown, R.drawable.ic_close, null)
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