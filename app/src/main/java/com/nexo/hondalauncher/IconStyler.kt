package com.nexo.hondalauncher

import android.graphics.*
import android.graphics.drawable.Drawable
import kotlin.math.abs

/** Har app icon ko dark glass plate + neon gradient ring wala stylish look deta hai. */
object IconStyler {
    private val rings = arrayOf(
        intArrayOf(0xFFFF2D2D.toInt(), 0xFFFF8A00.toInt()),
        intArrayOf(0xFF00E5FF.toInt(), 0xFF2979FF.toInt()),
        intArrayOf(0xFFFF4081.toInt(), 0xFF7C4DFF.toInt()),
        intArrayOf(0xFF00E676.toInt(), 0xFF00B8D4.toInt()),
        intArrayOf(0xFFFFEA00.toInt(), 0xFFFF6D00.toInt())
    )

    fun style(icon: Drawable, key: String): Bitmap {
        val size = 192
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val pad = 10f
        val rect = RectF(pad, pad, size - pad, size - pad)
        val radius = size * 0.32f
        val colors = rings[abs(key.hashCode()) % rings.size]
        val p = Paint(Paint.ANTI_ALIAS_FLAG)

        // plate with soft shadow
        p.color = 0xE6121218.toInt()
        p.setShadowLayer(10f, 0f, 5f, 0x99000000.toInt())
        c.drawRoundRect(rect, radius, radius, p)
        p.clearShadowLayer()

        // neon ring
        p.style = Paint.Style.STROKE
        p.strokeWidth = 5f
        p.shader = LinearGradient(0f, 0f, size.toFloat(), size.toFloat(), colors[0], colors[1], Shader.TileMode.CLAMP)
        c.drawRoundRect(rect, radius, radius, p)
        p.shader = null

        // gloss on top half
        p.style = Paint.Style.FILL
        p.color = 0x14FFFFFF
        c.save()
        c.clipRect(0f, 0f, size.toFloat(), size / 2f)
        c.drawRoundRect(rect, radius, radius, p)
        c.restore()

        // the app icon itself
        val inset = (size * 0.20f).toInt()
        icon.setBounds(inset, inset, size - inset, size - inset)
        icon.draw(c)
        return bmp
    }
}
