package tw.com.tp6gl4cj86.image_tool

import android.app.Activity
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import jp.wasabeef.glide.transformations.BlurTransformation

object BlurToolGlide {

    @JvmStatic
    fun blur(activity: Activity?, mImage: ImageView, url: String?) {
        blur(activity, 12, mImage, url)
    }

    @JvmStatic
    fun blur(activity: Activity?, radius: Int, mImage: ImageView, url: String?) {
        if (activity == null || url == null || activity.isDestroyed || activity.isFinishing) {
            return
        }
        Glide.with(mImage)
            .load(url)
            // Glide的BlurTransformation預設sampling為1
            .apply(RequestOptions.bitmapTransform(BlurTransformation(radius, 1)))
            .into(mImage)
    }
}
