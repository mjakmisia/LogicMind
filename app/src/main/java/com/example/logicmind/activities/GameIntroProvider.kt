package com.example.logicmind.activities

import com.example.logicmind.R

data class IntroSection(
    val titleRes: Int,
    val descriptionRes: Int
)

data class GameIntroConfig(
    val colorRes: Int,
    val titleRes: Int,
    val imageRes: Int,
    val sections: List<IntroSection>,
    val gameActivity: Class<*>
)

object GameIntroProvider {
    fun getConfig(gameId: String): GameIntroConfig {
        return when (gameId) {
            "card_match" -> GameIntroConfig(
                colorRes = R.color.category_green,
                titleRes = R.string.card_match,
                imageRes = R.drawable.image_card_match,
                sections = listOf(

                ),
                gameActivity = CardMatchActivity::class.java
            )
            "number_addition" -> GameIntroConfig(
                colorRes = R.color.category_pink,
                titleRes = R.string.number_addition,
                imageRes = R.drawable.image_number_addition,
                sections = listOf(

                ),
                gameActivity = NumberAdditionActivity::class.java
            )
            "word_search" -> GameIntroConfig(
                colorRes = R.color.category_yellow,
                titleRes = R.string.word_search,
                imageRes = R.drawable.image_word_search,
                sections = listOf(

                ),
                gameActivity = NumberAdditionActivity::class.java // ZMIEŃ
            )
            else -> throw IllegalArgumentException("Unknown gameId: $gameId")
        }
    }
}