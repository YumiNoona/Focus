package dev.still.focus

import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable

/** Uses only the bundled landscape; never samples the contents of another app. */
fun nativeGlassScene(context: Context, dark: Boolean, radius: Float): Drawable = object : Drawable() {
    private val bitmap = (context.getDrawable(R.drawable.glass_lake) as BitmapDrawable).bitmap
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val path = Path()
    override fun draw(canvas: Canvas) {
        val rect = RectF(bounds)
        val scale = maxOf(rect.width() / bitmap.width, rect.height() / bitmap.height)
        val width = bitmap.width * scale
        val height = bitmap.height * scale
        val destination = RectF(rect.centerX() - width / 2, rect.centerY() - height / 2, rect.centerX() + width / 2, rect.centerY() + height / 2)
        path.reset(); path.addRoundRect(rect, radius, radius, Path.Direction.CW)
        val save = canvas.save(); canvas.clipPath(path)
        paint.style = Paint.Style.FILL; paint.shader = null; paint.alpha = 255
        canvas.drawBitmap(bitmap, null, destination, paint)
        paint.color = Color.parseColor(if (dark) "#E6102139" else "#BA233D5C")
        canvas.drawRect(rect, paint)
        paint.shader = LinearGradient(0f, rect.top, rect.right, rect.bottom,
            Color.parseColor("#66FFFFFF"), Color.parseColor("#1AFFFFFF"), Shader.TileMode.CLAMP)
        paint.style = Paint.Style.STROKE; paint.strokeWidth = context.resources.displayMetrics.density
        canvas.drawRoundRect(RectF(rect.left + 1, rect.top + 1, rect.right - 1, rect.bottom - 1), radius, radius, paint)
        paint.shader = null; canvas.restoreToCount(save)
    }
    override fun setAlpha(alpha: Int) { }
    override fun setColorFilter(colorFilter: ColorFilter?) { }
    @Deprecated("Drawable opacity is no longer used") override fun getOpacity() = PixelFormat.TRANSLUCENT
}
