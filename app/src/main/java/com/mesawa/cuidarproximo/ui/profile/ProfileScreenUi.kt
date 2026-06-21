package com.mesawa.cuidarproximo.ui.profile

import android.graphics.Color
import android.graphics.Typeface
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import android.content.Context

internal fun Context.dp(value: Int): Int =
    (value * resources.displayMetrics.density).toInt()

internal fun screenRoot(context: Context): LinearLayout =
    LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setBackgroundColor(Color.parseColor("#F4F7FB"))
        setPadding(context.dp(20), context.dp(20), context.dp(20), context.dp(28))
    }

internal fun Context.title(text: String, size: Float = 28f): TextView =
    TextView(this).apply {
        this.text = text
        textSize = size
        setTextColor(Color.parseColor("#111827"))
        typeface = Typeface.DEFAULT_BOLD
    }

internal fun Context.subtitle(text: String): TextView =
    TextView(this).apply {
        this.text = text
        textSize = 14f
        setTextColor(Color.parseColor("#6B7280"))
        setPadding(0, 6, 0, 0)
    }

internal fun sectionCard(context: Context, color: String = "#FFFFFF"): CardView =
    CardView(context).apply {
        radius = context.dp(18).toFloat()
        cardElevation = context.dp(2).toFloat()
        setCardBackgroundColor(Color.parseColor(color))
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = context.dp(14)
        }
    }

internal fun cardContent(context: Context): LinearLayout =
    LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(context.dp(18), context.dp(18), context.dp(18), context.dp(18))
    }

internal fun Context.label(text: String): TextView =
    TextView(this).apply {
        this.text = text
        textSize = 13f
        setTextColor(Color.parseColor("#6B7280"))
        typeface = Typeface.DEFAULT_BOLD
    }

internal fun Context.value(text: String, color: String = "#111827", size: Float = 16f): TextView =
    TextView(this).apply {
        this.text = text
        textSize = size
        setTextColor(Color.parseColor(color))
        setPadding(0, 6, 0, 0)
    }

internal fun divider(context: Context): View =
    View(context).apply {
        setBackgroundColor(Color.parseColor("#E5E7EB"))
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            context.dp(1)
        ).apply {
            topMargin = context.dp(14)
            bottomMargin = context.dp(14)
        }
    }
