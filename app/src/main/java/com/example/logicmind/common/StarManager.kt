package com.example.logicmind.common

import android.os.Bundle
import android.widget.TextView

class StarManager {
    var starCount: Int = 0
        private set

    private lateinit var starCountText: TextView

    fun init(textView: TextView) {
        starCountText = textView
        updateUI()
    }

    fun increment() {
        starCount += 1
        updateUI()
    }

    fun reset() {
        starCount = 0
        updateUI()
    }

    private fun updateUI() {
        starCountText.text = starCount.toString()
    }

    fun saveState(outState: Bundle, key: String = "starCount") {
        outState.putInt(key, starCount)
    }

    fun restoreState(savedInstanceState: Bundle, key: String = "starCount") {
        starCount = savedInstanceState.getInt(key, 0)
        updateUI()
    }

}