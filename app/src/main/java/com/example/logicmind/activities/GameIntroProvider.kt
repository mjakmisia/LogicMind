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
                colorRes = R.color.category_green_dark_mode,
                titleRes = R.string.card_match,
                imageRes = R.drawable.image_card_match,
                sections = listOf(
                    IntroSection(R.string.cm_section_working_memory, R.string.cm_desc_working_memory),
                    IntroSection(R.string.cm_section_selective_attention, R.string.cm_desc_selective_attention)
                ),
                gameActivity = CardMatchActivity::class.java
            )
            "number_addition" -> GameIntroConfig(
                colorRes = R.color.category_pink_dark_mode,
                titleRes = R.string.number_addition,
                imageRes = R.drawable.image_number_addition,
                sections = listOf(
                    IntroSection(R.string.na_section_number_memory, R.string.na_desc_number_memory),
                    IntroSection(R.string.na_section_flexibility, R.string.na_desc_flexibility),
                    IntroSection(R.string.na_section_calculation, R.string.na_desc_calculation)
                ),
                gameActivity = NumberAdditionActivity::class.java
            )
            "word_search" -> GameIntroConfig(
                colorRes = R.color.category_yellow_dark_mode,
                titleRes = R.string.word_search,
                imageRes = R.drawable.image_word_search,
                sections = listOf(
                    IntroSection(R.string.ws_section_active_vocab, R.string.ws_desc_active_vocab),
                    IntroSection(R.string.ws_section_attention, R.string.ws_desc_attention),
                    IntroSection(R.string.ws_section_mind_speed, R.string.ws_desc_mind_speed)
                ),
                gameActivity = NumberAdditionActivity::class.java // ZMIEŃ
            )
            else -> throw IllegalArgumentException("Unknown gameId: $gameId")
        }
    }
}