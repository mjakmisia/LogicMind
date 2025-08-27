package com.example.logicmind.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.logicmind.R

//kod ktory domyslnie chce uzywac do kazdego intro gry

class GameIntroView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val gameIcon: ImageView
    private val gameTitle: TextView
    private val gameCategory: TextView
    private val gameDescription: TextView
    val btnClose: ImageButton
    val btnHelp: ImageButton

    init {
        LayoutInflater.from(context).inflate(R.layout.view_game_intro, this, true)

        gameIcon = findViewById(R.id.gameIcon)
        gameTitle = findViewById(R.id.gameTitle)
        gameCategory = findViewById(R.id.gameCategory)
        gameDescription = findViewById(R.id.gameDescription)
        btnClose = findViewById(R.id.btnClose)
        btnHelp = findViewById(R.id.btnHelp)

        // pobieramy atrybuty z XML
        attrs?.let {
            val typedArray = context.obtainStyledAttributes(it, R.styleable.GameIntroView, 0, 0)

            gameTitle.text = typedArray.getString(R.styleable.GameIntroView_gameTitle) ?: ""
            gameCategory.text = typedArray.getString(R.styleable.GameIntroView_gameCategory) ?: ""
            gameDescription.text = typedArray.getString(R.styleable.GameIntroView_gameDescription) ?: ""

            val iconResId = typedArray.getResourceId(R.styleable.GameIntroView_gameIcon, -1)
            if (iconResId != -1) {
                gameIcon.setImageResource(iconResId)
            }

            typedArray.recycle()
        }
    }

    // dodatkowe metody do zmiany w kodzie
    fun setTitle(title: String) = run { gameTitle.text = title }
    fun setIcon(resId: Int) = run { gameIcon.setImageResource(resId) }
    fun setCategory(category: String) = run { gameCategory.text = category }
    fun setDescription(description: String) = run { gameDescription.text = description }
}
