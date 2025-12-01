package com.example.logicmind.additional

import android.content.Context
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import android.text.Html
import android.text.Spanned
import androidx.core.content.ContextCompat

object SymbolRaceInstructionHelper {

    fun getSpanned(context: Context, html: String): Spanned {
        val imageGetter = Html.ImageGetter { source ->

            if (source == "ic_empty_circle") {
                return@ImageGetter ShapeDrawable(OvalShape()).apply {
                    intrinsicWidth = 72
                    intrinsicHeight = 72
                    paint.color = 0xFFBBBBBB.toInt()
                    setBounds(0, 0, 72, 72)
                }
            }

            val resId = context.resources.getIdentifier(source, "drawable", context.packageName)
            val icon = ContextCompat.getDrawable(context, resId) ?: return@ImageGetter null

            val circle = ShapeDrawable(OvalShape()).apply {
                intrinsicWidth = 72
                intrinsicHeight = 72
                paint.color = 0xFFBBBBBB.toInt()
            }

            icon.setBounds(12, 12, 60, 60)
            circle.setBounds(0, 0, 72, 72)

            LayerDrawable(arrayOf(circle, icon)).apply {
                setBounds(0, 0, 72, 72)
            }
        }

        return Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY, imageGetter, null)
    }
}