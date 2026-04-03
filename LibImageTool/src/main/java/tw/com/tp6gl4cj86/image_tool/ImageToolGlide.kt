package tw.com.tp6gl4cj86.image_tool

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.annotation.ColorInt
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.material.imageview.ShapeableImageView
import tw.com.tp6gl4cj86.image_tool.ImageToolGlide.fitWidth
import tw.com.tp6gl4cj86.image_tool.ImageToolGlide.loadImage
import tw.com.tp6gl4cj86.image_tool.ImageToolGlide.setBorder
import tw.com.tp6gl4cj86.image_tool.ImageToolGlide.setCornersRadius
import kotlin.math.max

object ImageToolGlide {

    private fun isValidContext(context: Context?): Boolean {
        return !(context == null || (context is android.app.Activity && (context.isDestroyed || context.isFinishing)))
    }

    @JvmStatic
    fun setCornersRadius(image: ShapeableImageView, radius: Any) {
        // 動態設定 ShapeableImageView 的圓角
        when (radius) {
            is Number -> {
                image.shapeAppearanceModel = image.shapeAppearanceModel.toBuilder().apply {
                    setAllCornerSizes(radius.toFloat())
                }.build()
            }

            is IntArray -> {
                if (radius.size >= 4) {
                    image.shapeAppearanceModel = image.shapeAppearanceModel.toBuilder().apply {
                        setTopLeftCornerSize(radius[0].toFloat())
                        setTopRightCornerSize(radius[1].toFloat())
                        setBottomRightCornerSize(radius[2].toFloat())
                        setBottomLeftCornerSize(radius[3].toFloat())
                    }.build()
                }
            }

            is FloatArray -> {
                // 如果是 FloatArray，設定四角獨立
                if (radius.size >= 4) {
                    image.shapeAppearanceModel = image.shapeAppearanceModel.toBuilder().apply {
                        setTopLeftCornerSize(radius[0])
                        setTopRightCornerSize(radius[1])
                        setBottomRightCornerSize(radius[2])
                        setBottomLeftCornerSize(radius[3])
                    }.build()
                }
            }
        }
    }

    @JvmStatic
    fun setBorder(image: ShapeableImageView, borderWidthPx: Float, @ColorInt borderColor: Int) {
        setBorder(image, borderWidthPx, intArrayOf(borderColor))
    }

    @JvmStatic
    fun setBorder(image: ShapeableImageView, borderWidthPx: Float, @ColorInt borderColor: IntArray) {
        if (borderWidthPx > 0) {
            val shapeModel = image.shapeAppearanceModel
            val tl = shapeModel.topLeftCornerSize.getCornerSize(RectF())
            val tr = shapeModel.topRightCornerSize.getCornerSize(RectF())
            val br = shapeModel.bottomRightCornerSize.getCornerSize(RectF())
            val bl = shapeModel.bottomLeftCornerSize.getCornerSize(RectF())

            // 轉換為 Drawable 需要的 8 個數值陣列 [tl_x, tl_y, tr_x, tr_y, ...]
            val radii = floatArrayOf(tl, tl, tr, tr, br, br, bl, bl)

            // 將自定義的邊框 Drawable 塞入前景
            image.foreground = InnerBorderDrawable(radii, borderWidthPx, borderColor)
        } else {
            image.strokeWidth = 0f
            image.foreground = null
        }
    }

    @JvmStatic
    fun fitWidth(image: ImageView, url: String?) {
        if (url == null || !isValidContext(image.context)) {
            return
        }
        Glide.with(image)
            .asBitmap()
            .load(url)
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    if (!isValidContext(image.context)) {
                        return
                    }
                    val ratio = resource.width.toFloat() / resource.height.toFloat()
                    image.setImageBitmap(resource)
                    image.post {
                        if (!isValidContext(image.context)) {
                            return@post
                        }
                        val width = image.width
                        val height = (width / ratio).toInt()
                        val params = image.layoutParams
                        if (params != null) {
                            params.height = height
                            image.layoutParams = params
                        }
                    }
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    if (isValidContext(image.context)) {
                        image.setImageDrawable(placeholder)
                    }
                }
            })
    }

    @JvmStatic
    fun loadImage(image: ShapeableImageView, url: String?) {
        if (url == null || !isValidContext(image.context)) {
            return
        }

        /// 空網址，清除圖片
        if (url.isBlank() || !isValidContext(image.context)) {
            image.setImageDrawable(null)
            return
        }

        Glide.with(image)
            .load(url)
            .into(image)
    }


    /*
          ##      ###     ##     ##     ###
          ##     ## ##    ##     ##    ## ##
          ##    ##   ##   ##     ##   ##   ##
          ##   ##     ##  ##     ##  ##     ##
    ##    ##   #########   ##   ##   #########
    ##    ##   ##     ##    ## ##    ##     ##
     ######    ##     ##     ###     ##     ##
    */
    @JvmStatic
    fun imageView2Bitmap(image: ImageView?): Bitmap? {
        if (image == null) {
            return null
        }
        return drawable2Bitmap(image.drawable)
    }

    @JvmStatic
    fun drawable2Bitmap(drawable: Drawable?): Bitmap? {
        if (drawable == null) {
            return null
        }
        if (drawable is BitmapDrawable) {
            return drawable.bitmap
        }

        return try {
            val bitmap = createBitmap(max(1, drawable.intrinsicWidth), max(1, drawable.intrinsicHeight))
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap
        } catch (e: Exception) {
            null
        }
    }

    @JvmStatic
    fun bitmap2Drawable(context: Context, bitmap: Bitmap?): Drawable? {
        if (bitmap == null) {
            return null
        }
        return bitmap.toDrawable(context.resources)
    }
}

/// Glide 繪製內 border 工具
class InnerBorderDrawable(
    private val originalRadii: FloatArray, // 保存原始半徑
    private val borderSize: Float,
    private val borderColors: IntArray
) : Drawable() {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = borderSize
    }

    private val path = Path()
    private val rect = RectF()

    // 用來存放計算內縮後的半徑
    private val drawRadii = FloatArray(8)

    override fun onBoundsChange(bounds: Rect) {
        super.onBoundsChange(bounds)
        // 取得 View 實際的邊界大小
        rect.set(bounds)

        // 🔥 2. 核心修正：將繪圖矩形往內縮「邊框寬度的一半」
        // 這樣畫出來的線條向外擴展時，會剛好貼齊 bounds 邊緣，完全不需要依賴裁切！
        val inset = borderSize / 2f
        rect.inset(inset, inset)

        // 🔥 3. 重新計算圓角半徑
        // 因為矩形內縮了，圓角的弧度也要跟著減去 inset，這樣內外圓弧才會完美平行
        for (i in originalRadii.indices) {
            drawRadii[i] = max(0f, originalRadii[i] - inset)
        }

        // 設定漸層 (注意：漸層的高度使用原本的 bounds 高度，確保顏色分佈正確)
        if (borderColors.size == 1) {
            paint.color = borderColors[0]
        } else if (borderColors.size > 1) {
            paint.shader = LinearGradient(
                0f, 0f, 0f, bounds.height().toFloat(),
                borderColors,
                null,
                Shader.TileMode.CLAMP
            )
        }

        // 建立真正要繪製的路徑
        path.reset()
        path.addRoundRect(rect, drawRadii, Path.Direction.CW)
    }

    override fun draw(canvas: Canvas) {
        if (borderSize > 0 && borderColors.isNotEmpty()) {
            canvas.drawPath(path, paint)
        }
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}

fun ShapeableImageView.setCornersRadius(radius: Any) {
    setCornersRadius(this, radius)
}

fun ShapeableImageView.setBorder(borderWidthPx: Float, @ColorInt borderColor: Int) {
    setBorder(this, borderWidthPx, intArrayOf(borderColor))
}

fun ShapeableImageView.setBorder(borderWidthPx: Float, @ColorInt borderColor: IntArray) {
    setBorder(this, borderWidthPx, borderColor)
}

fun ShapeableImageView.fitWidth(url: String?) {
    fitWidth(this, url)
}


fun ShapeableImageView.loadImage(url: String?) {
    loadImage(this, url)
}