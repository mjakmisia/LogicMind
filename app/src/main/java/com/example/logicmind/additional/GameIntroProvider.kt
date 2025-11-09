package com.example.logicmind.additional

import com.example.logicmind.R
import com.example.logicmind.activities.CardMatchActivity
import com.example.logicmind.activities.ColorSequenceActivity
import com.example.logicmind.activities.LeftOrRightActivity
import com.example.logicmind.activities.NumberAdditionActivity
import com.example.logicmind.activities.PathChangeActivity
import com.example.logicmind.activities.RoadDashActivity
import com.example.logicmind.activities.SymbolRaceActivity
import com.example.logicmind.activities.WordSearchActivity

data class IntroSection(
    val titleRes: Int,
    val descriptionRes: Int
)

data class GameIntroConfig(
    val colorRes: Int,
    val titleRes: Int,
    val imageRes: Int,
    val sections: List<IntroSection>,
    val gameActivity: Class<*>,
    val instructionRes: Int = 0
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
                gameActivity = CardMatchActivity::class.java,
                instructionRes = R.string.card_match_instruction
            )
            "color_sequence" -> GameIntroConfig(
                colorRes = R.color.category_green_dark_mode,
                titleRes = R.string.color_sequence,
                imageRes = R.drawable.image_color_sequence,
                sections = listOf(
                    IntroSection(R.string.cs_section_working_memory, R.string.cs_desc_working_memory),
                    IntroSection(R.string.cs_section_selective_attention, R.string.cs_desc_selective_attention),
                    IntroSection(R.string.cs_section_visual_auditory_sync, R.string.cs_desc_visual_auditory_sync)
                ),
                gameActivity = ColorSequenceActivity::class.java,
                instructionRes = R.string.color_sequence_instruction
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
                gameActivity = NumberAdditionActivity::class.java,
                instructionRes = R.string.number_addition_instruction
            )
            "path_change" -> GameIntroConfig(
                colorRes = R.color.category_pink_dark_mode,
                titleRes = R.string.path_change,
                imageRes = R.drawable.image_path_change,
                sections = listOf(
                    IntroSection(R.string.pc_section_planning, R.string.pc_desc_planning),
                    IntroSection(R.string.pc_section_divided_attention, R.string.pc_desc_divided_attention)
                ),
                gameActivity = PathChangeActivity::class.java,
                instructionRes = R.string.path_change_instruction
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
                gameActivity = WordSearchActivity::class.java,
                instructionRes = R.string.word_search_instruction
            )
            "left_or_right" -> GameIntroConfig(
                colorRes = R.color.category_yellow_dark_mode,
                titleRes = R.string.left_or_right,
                imageRes = R.drawable.ic_close, //obrazek do zmiany
                sections = listOf(
                    //do uzupełnienia
                ),
                gameActivity = LeftOrRightActivity::class.java,
                instructionRes = R.string.left_or_right_instruction
            )
            "symbol_race" -> GameIntroConfig(
                colorRes = R.color.category_blue_dark_mode,
                titleRes = R.string.symbol_race,
                imageRes = R.drawable.image_symbol_race,
                sections = listOf(
                    IntroSection(R.string.sr_section_visual_motor, R.string.sr_desc_visual_motor),
                    IntroSection(R.string.sr_section_processing_speed, R.string.sr_desc_processing_speed),
                    IntroSection(R.string.sr_section_selective_attention, R.string.sr_desc_selective_attention)
                ),
                gameActivity = SymbolRaceActivity::class.java,
                instructionRes = R.string.symbol_race_instruction
            )
            "road_dash" -> GameIntroConfig(
                colorRes = R.color.category_blue_dark_mode,
                titleRes = R.string.road_dash,
                imageRes = R.drawable.ic_close, //obrazek do zmiany
                sections = listOf(
                    //do uzupełnienia
                ),
                gameActivity = RoadDashActivity::class.java,
                instructionRes = R.string.road_dash_instruction
            )
            else -> throw IllegalArgumentException("Unknown gameId: $gameId")
        }
    }
}