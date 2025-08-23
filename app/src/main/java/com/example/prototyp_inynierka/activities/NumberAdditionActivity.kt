package com.example.prototyp_inynierka.activities

import android.os.Bundle
import android.widget.Button
import android.widget.GridLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.prototyp_inynierka.R

class NumberAdditionActivity : AppCompatActivity() {

    private lateinit var targetNumberText: TextView
    private lateinit var numberGrid: GridLayout
    private val numbers = mutableListOf<Int>()
    private val selectedNumbers = mutableListOf<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_number_addition)

        supportActionBar?.hide()

        targetNumberText = findViewById(R.id.targetNumberText)
        numberGrid = findViewById(R.id.numberGrid)

        generateNumbers()
        generateTargetNumber()
        setupNumberGrid()
    }

    private fun generateNumbers() {
        numbers.clear()
        // losujemy 16 cyfr od 1 do 9
        repeat(16) {
            numbers.add((1..9).random())
        }
    }

    private fun generateTargetNumber() {
        // losujemy liczbę od 5 do 20
        val target = (5..20).random()
        targetNumberText.text = target.toString()
    }

    private fun setupNumberGrid() {
        numberGrid.removeAllViews()
        for (number in numbers) {
            val button = Button(this)
            button.text = number.toString()
            button.layoutParams = GridLayout.LayoutParams().apply {
                width = 150
                height = 150
                setMargins(8, 8, 8, 8)
            }

            button.setOnClickListener {
                handleNumberClick(number, button)
            }
            numberGrid.addView(button)
        }
    }

    private fun handleNumberClick(number: Int, button: Button) {
        if (selectedNumbers.size < 2) {
            selectedNumbers.add(number)
            button.isEnabled = false
        }

        if (selectedNumbers.size == 2) {
            val sum = selectedNumbers.sum()
            if (sum == targetNumberText.text.toString().toInt()) {
                // poprawna odpowiedź
                selectedNumbers.clear()
                generateTargetNumber()
            } else {
                // błędna odpowiedź – przywracamy przyciski
                for (i in 0 until numberGrid.childCount) {
                    numberGrid.getChildAt(i).isEnabled = true
                }
                selectedNumbers.clear()
            }
        }
    }
}
