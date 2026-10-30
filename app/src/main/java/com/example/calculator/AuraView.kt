package com.example.calculator

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// A transparent layer that draws floating sparks and "=" bursts
class AuraView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private class Spark(
        var x: Float, var y: Float, var vx: Float, var vy: Float,
        val radius: Float, val color: Int, val ambient: Boolean,
        var life: Float = 1f, var phase: Float = 0f
    )

    private val sparks = ArrayList<Spark>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val green = ContextCompat.getColor(context, R.color.hxh_key_equals)
    private val gold = ContextCompat.getColor(context, R.color.hxh_key_action)
    private val density = resources.displayMetrics.density
    private var last = 0L

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0 && sparks.none { it.ambient }) {
            repeat(22) { sparks.add(newAmbient(true)) }
        }
    }

    // A slow drifting spark that floats upward
    private fun newAmbient(randomY: Boolean): Spark {
        val r = (2f + Random.nextFloat() * 3f) * density
        return Spark(
            x = Random.nextFloat() * width,
            y = if (randomY) Random.nextFloat() * height else height + r,
            vx = (Random.nextFloat() - 0.5f) * 12f * density,
            vy = -(14f + Random.nextFloat() * 24f) * density,
            radius = r,
            color = if (Random.nextBoolean()) green else gold,
            ambient = true,
            phase = Random.nextFloat() * 6.28f
        )
    }

    // Called when "=" is pressed: sparks fly out from (cx, cy)
    fun burst(cx: Float, cy: Float) {
        repeat(36) {
            val angle = Random.nextFloat() * 6.2832f
            val speed = (120f + Random.nextFloat() * 280f) * density
            sparks.add(
                Spark(
                    x = cx, y = cy,
                    vx = cos(angle) * speed, vy = sin(angle) * speed,
                    radius = (2f + Random.nextFloat() * 3f) * density,
                    color = if (Random.nextBoolean()) green else gold,
                    ambient = false
                )
            )
        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val now = System.nanoTime()
        val dt = if (last == 0L) 0.016f
        else ((now - last) / 1_000_000_000f).coerceAtMost(0.05f)
        last = now

        val iterator = sparks.iterator()
        while (iterator.hasNext()) {
            val s = iterator.next()
            s.x += s.vx * dt
            s.y += s.vy * dt

            val alpha: Float
            if (s.ambient) {
                s.phase += dt * 2f
                if (s.y < -s.radius) { // left the top: respawn at the bottom
                    s.y = height + s.radius
                    s.x = Random.nextFloat() * width
                }
                alpha = 0.22f + 0.18f * sin(s.phase)
            } else {
                s.vx *= 0.96f
                s.vy *= 0.96f
                s.life -= dt * 1.4f
                if (s.life <= 0f) { iterator.remove(); continue }
                alpha = s.life
            }

            paint.color = s.color
            paint.alpha = (alpha.coerceIn(0f, 1f) * 255).toInt()
            canvas.drawCircle(s.x, s.y, s.radius, paint)
        }
        postInvalidateOnAnimation() // keep animating
    }
}