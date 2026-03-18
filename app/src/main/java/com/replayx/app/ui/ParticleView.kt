package com.replayx.app.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.abs
import kotlin.random.Random

class ParticleView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private data class Particle(
        var x: Float, var y: Float,
        var vx: Float, var vy: Float,
        var alpha: Int, var size: Float,
        var fadeSpeed: Int, val type: Int
    )

    private val particles = mutableListOf<Particle>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val chars = listOf("0","1","0","1","A","F","E","B","C","D","0","1")
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00FF41")
        textSize = 28f
        typeface = android.graphics.Typeface.MONOSPACE
    }
    private var initialized = false

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (!initialized && w > 0 && h > 0) {
            initialized = true
            repeat(60) { spawnParticle(w, h, true) }
        }
    }

    private fun spawnParticle(w: Int, h: Int, anywhere: Boolean = false) {
        val type = Random.nextInt(3)
        particles.add(Particle(
            x = Random.nextFloat() * w,
            y = if (anywhere) Random.nextFloat() * h else -20f,
            vx = (Random.nextFloat() - 0.5f) * 0.8f,
            vy = Random.nextFloat() * 1.5f + 0.5f,
            alpha = Random.nextInt(80, 200),
            size = Random.nextFloat() * 3f + 1f,
            fadeSpeed = Random.nextInt(1, 3),
            type = type
        ))
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width == 0 || height == 0) return
        val toRemove = mutableListOf<Particle>()
        for (p in particles) {
            p.x += p.vx
            p.y += p.vy
            p.alpha -= p.fadeSpeed
            if (p.alpha <= 0 || p.y > height + 30f) {
                toRemove.add(p)
                continue
            }
            when (p.type) {
                0 -> {
                    paint.color = Color.argb(p.alpha, 0, 255, 65)
                    canvas.drawCircle(p.x, p.y, p.size, paint)
                }
                1 -> {
                    paint.color = Color.argb(p.alpha / 2, 0, 200, 50)
                    paint.strokeWidth = 1f
                    canvas.drawLine(p.x, p.y, p.x + p.vx * 8, p.y + p.vy * 8, paint)
                }
                else -> {
                    textPaint.alpha = p.alpha / 3
                    val ch = chars[abs(p.x.toInt()) % chars.size]
                    canvas.drawText(ch, p.x, p.y, textPaint)
                }
            }
        }
        particles.removeAll(toRemove)
        repeat(2) { spawnParticle(width, height) }
        postInvalidateDelayed(16)
    }
}
