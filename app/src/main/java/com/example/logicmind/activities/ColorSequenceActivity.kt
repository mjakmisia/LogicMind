package com.example.logicmind.activities
import android.os.Bundle
import com.example.logicmind.R

class ColorSequenceActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_color_sequence)
        supportActionBar?.hide()
    }
}