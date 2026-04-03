package tw.com.tp6gl4cj86.image_tool

import android.app.DownloadManager
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.media.ExifInterface
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import androidx.annotation.ColorInt
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.GranularRoundedCorners
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.material.imageview.ShapeableImageView
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

object ImageToolGlide {

    private fun isValidContext(context: Context?): Boolean {
        return !(context == null || (context is android.app.Activity && (context.isDestroyed || context.isFinishing)))
    }

    @JvmStatic
    fun removeCache(context: Context, url: String) {
        // Glide 清除特定 URL 緩存較為複雜，通常清除內存和磁盤緩存
        // 內存緩存必須在主線程，磁盤緩存必須在背景線程
        // Glide.get(context).clearMemory()
        // Thread { Glide.get(context).clearDiskCache() }.start()
    }

    @JvmStatic
    fun loadImage(image: ImageView, url: String?) {
        if (url == null || !isValidContext(image.context)) {
            return
        }
        Glide.with(image)
            .load(url)
            .into(image)
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
    fun loadImage(image: ShapeableImageView, url: String?, radius: Float) {
        loadImage(image, url, radius, null, null)
    }

    @JvmStatic
    fun loadImage(image: ShapeableImageView, url: String?, radius: FloatArray) {
        loadImage(image, url, radius, null, null)
    }

    fun loadImage(
        image: ShapeableImageView,
        url: String?,
        radius: Any,
        borderWidthPx: Float,
        @ColorInt borderColor: Int // (Color.RED 或 ContextCompat.getColor(...))
    ) {
        loadImage(image, url, radius, borderWidthPx, intArrayOf(borderColor))
    }

    @JvmStatic
    fun loadImage(
        image: ShapeableImageView,
        url: String?,
        radius: Any,
        borderWidthPx: Float?,
        @ColorInt borderColor: IntArray? // (Color.RED 或 ContextCompat.getColor(...))
    ) {
        if (url == null || !isValidContext(image.context)) {
            return
        }

        // 動態設定 ShapeableImageView 的圓角
        when (radius) {
            is Float -> {
                // 如果是 Float，設定四角統一
                image.shapeAppearanceModel =
                    image.shapeAppearanceModel.toBuilder().apply {
                        setAllCornerSizes(radius)
                    }.build()
            }

            is FloatArray -> {
                // 如果是 FloatArray，設定四角獨立
                if (radius.size >= 4) {
                    image.shapeAppearanceModel =
                        image.shapeAppearanceModel.toBuilder().apply {
                            setTopLeftCornerSize(radius[0])
                            setTopRightCornerSize(radius[1])
                            setBottomRightCornerSize(radius[2])
                            setBottomLeftCornerSize(radius[3])
                        }.build()
                }
            }
        }

        // Glide 只需要單純載入圖片和裁切
        var request = Glide.with(image).load(url)
        request =
            if (borderWidthPx != null && borderColor != null && borderWidthPx > 0) {
                // 動態獲取 ShapeableImageView 的邊框參數
                val shapeModel = image.shapeAppearanceModel
                val tl = shapeModel.topLeftCornerSize.getCornerSize(RectF())
                val tr = shapeModel.topRightCornerSize.getCornerSize(RectF())
                val br = shapeModel.bottomRightCornerSize.getCornerSize(RectF())
                val bl = shapeModel.bottomLeftCornerSize.getCornerSize(RectF())
                request.transform(
                    CenterCrop(),
                    InnerBorderTransformation(tl, tr, br, bl, borderWidthPx, borderColor)
                )
            } else {
                request.transform(CenterCrop())
            }
        request.into(image)
    }

    interface OnDownloadImageListener {
        fun onDownloadImageDone(bitmap: Bitmap?)
    }

    @JvmStatic
    fun downloadImage(context: Context, url: String?, listener: OnDownloadImageListener?) {
        if (url == null || !isValidContext(context)) {
            listener?.onDownloadImageDone(null)
            return
        }
        val handler = Handler(Looper.getMainLooper())
        Glide.with(context)
            .asBitmap()
            .load(url)
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    if (!isValidContext(context)) {
                        handler.post { listener?.onDownloadImageDone(null) }
                        return
                    }
                    val bitmapCopy = resource.copy(resource.config ?: Bitmap.Config.ARGB_8888, true)
                    handler.post { listener?.onDownloadImageDone(bitmapCopy) }
                }

                override fun onLoadCleared(placeholder: Drawable?) {}

                override fun onLoadFailed(errorDrawable: Drawable?) {
                    handler.post { listener?.onDownloadImageDone(null) }
                }
            })
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
            val bitmap = Bitmap.createBitmap(
                max(1, drawable.intrinsicWidth),
                max(1, drawable.intrinsicHeight),
                Bitmap.Config.ARGB_8888
            )
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
        return BitmapDrawable(context.resources, bitmap)
    }

    @JvmStatic
    fun restoreImageOrientation(selectedBitmap: Bitmap, imagePath: String): Bitmap {
        try {
            var rotate = 0
            val imageFile = File(imagePath)
            val exif = ExifInterface(imageFile.absolutePath)
            val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)

            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_270 -> rotate = 270
                ExifInterface.ORIENTATION_ROTATE_180 -> rotate = 180
                ExifInterface.ORIENTATION_ROTATE_90 -> rotate = 90
            }

            if (rotate == 0) {
                return selectedBitmap
            }

            val matrix = Matrix()
            matrix.postRotate(rotate.toFloat())

            return Bitmap.createBitmap(
                selectedBitmap, 0, 0,
                selectedBitmap.width, selectedBitmap.height, matrix, true
            )
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return selectedBitmap
    }

    @JvmStatic
    fun scaleCenterCrop(source: Bitmap): Bitmap {
        val sourceWidth = source.width
        val sourceHeight = source.height
        val newWidth = min(sourceWidth, sourceHeight)

        val xScale = newWidth.toFloat() / sourceWidth
        val yScale = newWidth.toFloat() / sourceHeight
        val scale = max(xScale, yScale)

        val scaledWidth = scale * sourceWidth
        val scaledHeight = scale * sourceHeight

        val left = (newWidth - scaledWidth) / 2
        val top = (newWidth - scaledHeight) / 2

        val targetRect = RectF(left, top, left + scaledWidth, top + scaledHeight)

        val dest = Bitmap.createBitmap(newWidth, newWidth, source.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(dest)
        canvas.drawBitmap(source, null, targetRect, null)

        return dest
    }

    @JvmStatic
    fun saveImageFromUrl(
        context: Context,
        url: String,
        imageFileUri: Uri,
        title: String,
        description: String
    ): Long {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val request = DownloadManager.Request(Uri.parse(url)).apply {
            setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
            setTitle(title)
            setDescription(description)
            setDestinationUri(imageFileUri)
        }
        return downloadManager.enqueue(request)
    }
}

/// Glide 繪製內 border 工具
class InnerBorderTransformation(
    private val radiusTL: Float,
    private val radiusTR: Float,
    private val radiusBR: Float,
    private val radiusBL: Float,
    private val borderSize: Float,
    private val borderColors: IntArray // 傳 1 個顏色就是單色，傳 2 個以上就是漸變！
) : BitmapTransformation() {

    override fun transform(pool: BitmapPool, toTransform: Bitmap, outWidth: Int, outHeight: Int): Bitmap {
        val width = toTransform.width.toFloat()
        val height = toTransform.height.toFloat()

        // 拿一張乾淨的畫布
        val bitmap = pool.get(width.toInt(), height.toInt(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. 畫圓角圖片
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = BitmapShader(toTransform, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        }
        val imageRect = RectF(0f, 0f, width, height)
        val radii = floatArrayOf(
            radiusTL, radiusTL, radiusTR, radiusTR,
            radiusBR, radiusBR, radiusBL, radiusBL
        )
        val path = Path().apply { addRoundRect(imageRect, radii, Path.Direction.CW) }
        canvas.drawPath(path, paint)

        // 2. 畫「100% 往內縮」的邊框
        if (borderSize > 0 && borderColors.isNotEmpty()) {
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = borderSize
            }

            // 🔥 魔法在這裡：判斷是單色還是漸變色
            if (borderColors.size == 1) {
                // 單色邊框
                borderPaint.color = borderColors[0]
            } else {
                // 漸變邊框 (這裡設定為「上到下」的線性漸變)
                borderPaint.shader = LinearGradient(
                    0f, 0f, 0f, height, // 從上 (0,0) 到下 (width, height)
                    borderColors, // 你的顏色陣列 (可包含半透明)
                    null, // 顏色分佈比例 (傳 null 代表均勻分佈)
                    Shader.TileMode.CLAMP
                )
            }

            // 核心魔法：將邊框矩形往內縮 (Inset) 邊框寬度的一半
            val inset = borderSize / 2f
            val borderRect = RectF(inset, inset, width - inset, height - inset)

            // 圓角半徑也要跟著扣掉 Inset
            val borderRadii = floatArrayOf(
                max(0f, radiusTL - inset), max(0f, radiusTL - inset),
                max(0f, radiusTR - inset), max(0f, radiusTR - inset),
                max(0f, radiusBR - inset), max(0f, radiusBR - inset),
                max(0f, radiusBL - inset), max(0f, radiusBL - inset)
            )
            val borderPath = Path().apply { addRoundRect(borderRect, borderRadii, Path.Direction.CW) }
            canvas.drawPath(borderPath, borderPaint)
        }

        return bitmap
    }

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        // 將顏色陣列轉成字串當作 Cache Key 的一部分，這樣換顏色時 Glide 才會重新繪圖
        val colorsKey = borderColors.joinToString("_")
        val id = "InnerBorder_${radiusTL}_${radiusTR}_${radiusBR}_${radiusBL}_${borderSize}_$colorsKey"
        messageDigest.update(id.toByteArray(CHARSET))
    }
}