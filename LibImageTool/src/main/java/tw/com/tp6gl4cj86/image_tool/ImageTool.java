package tw.com.tp6gl4cj86.image_tool;

import android.app.DownloadManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Handler;
import android.widget.ImageView;

import com.facebook.common.executors.CallerThreadExecutor;
import com.facebook.common.references.CloseableReference;
import com.facebook.datasource.DataSource;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.controller.BaseControllerListener;
import com.facebook.drawee.controller.ControllerListener;
import com.facebook.drawee.generic.GenericDraweeHierarchyBuilder;
import com.facebook.drawee.generic.RoundingParams;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.core.ImagePipeline;
import com.facebook.imagepipeline.core.ImagePipelineConfig;
import com.facebook.imagepipeline.datasource.BaseBitmapDataSubscriber;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.image.ImageInfo;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;

import java.io.File;
import java.io.IOException;

/**
 * Created by tp6gl4cj86 on 2016/1/14.
 */
public class ImageTool
{


    /**
     * <uses-permission android:name="android.permission.INTERNET" />
     * <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
     * <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
     */
    public static void init(Context context)
    {
        //        final DiskCacheConfig diskCacheConfig = DiskCacheConfig.newBuilder(activity)
        //                                                               .setMaxCacheSize(40 * ByteConstants.MB)
        //                                                               .build();
        //
        final ImagePipelineConfig config = ImagePipelineConfig.newBuilder(context)
                                                              .setDownsampleEnabled(true)
                                                              //.setMainDiskCacheConfig(diskCacheConfig)
                                                              .build();
        Fresco.initialize(context);
    }

    public static void removeCache(String url)
    {
        Fresco.getImagePipeline()
              .evictFromCache(Uri.parse(url));
    }

    //    public static boolean isImageDownloaded(Activity activity, Uri loadUri)
    //    {
    //        if (loadUri == null)
    //        {
    //            return false;
    //        }
    //        CacheKey cacheKey = DefaultCacheKeyFactory.getInstance()
    //                                                  .getEncodedCacheKey(ImageRequest.fromUri(loadUri), activity);
    //        return ImagePipelineFactory.getInstance()
    //                                   .getMainFileCache()
    //                                   .hasKey(cacheKey) || ImagePipelineFactory.getInstance()
    //                                                                            .getSmallImageFileCache()
    //                                                                            .hasKey(cacheKey);
    //    }

    /// return file or null
    //    public static File getCachedImageOnDisk(Activity activity, Uri loadUri)
    //    {
    //        File localFile = null;
    //        if (loadUri != null)
    //        {
    //            CacheKey cacheKey = DefaultCacheKeyFactory.getInstance()
    //                                                      .getEncodedCacheKey(ImageRequest.fromUri(loadUri), activity);
    //            if (ImagePipelineFactory.getInstance()
    //                                    .getMainFileCache()
    //                                    .hasKey(cacheKey))
    //            {
    //                BinaryResource resource = ImagePipelineFactory.getInstance()
    //                                                              .getMainFileCache()
    //                                                              .getResource(cacheKey);
    //                localFile = ((FileBinaryResource) resource).getFile();
    //            }
    //            else if (ImagePipelineFactory.getInstance()
    //                                         .getSmallImageFileCache()
    //                                         .hasKey(cacheKey))
    //            {
    //                BinaryResource resource = ImagePipelineFactory.getInstance()
    //                                                              .getSmallImageFileCache()
    //                                                              .getResource(cacheKey);
    //                localFile = ((FileBinaryResource) resource).getFile();
    //            }
    //        }
    //        return localFile;
    //    }

    public static void loadImage(final SimpleDraweeView image, String url)
    {
        image.setImageURI(url);
    }

    public static void fitWidth(final SimpleDraweeView image, String url)
    {
        final ControllerListener<ImageInfo> controllerListener = new BaseControllerListener<ImageInfo>()
        {
            @Override
            public void onFinalImageSet(String id, ImageInfo imageInfo, Animatable anim)
            {
                if (imageInfo != null && image != null)
                {
                    image.setAspectRatio((float) imageInfo.getWidth() / imageInfo.getHeight());
                }
            }

        };
        final DraweeController controller = Fresco.newDraweeControllerBuilder()
                                                  .setControllerListener(controllerListener)
                                                  .setUri(Uri.parse(url))
                                                  .build();
        image.setController(controller);
    }

    public static void setCornersRadius(Context context, SimpleDraweeView image, float radius)
    {
        final GenericDraweeHierarchyBuilder genericDraweeHierarchyBuilder = new GenericDraweeHierarchyBuilder(context.getResources());
        genericDraweeHierarchyBuilder.setRoundingParams(RoundingParams.fromCornersRadius(radius));
        image.setHierarchy(genericDraweeHierarchyBuilder.build());
    }

    public static void setCornersRadius(Context context, SimpleDraweeView image, float[] radius)
    {
        if (radius.length < 4)
        {
            return;
        }

        final GenericDraweeHierarchyBuilder genericDraweeHierarchyBuilder = new GenericDraweeHierarchyBuilder(context.getResources());
        genericDraweeHierarchyBuilder.setRoundingParams(RoundingParams.fromCornersRadii(radius[0], radius[1], radius[2], radius[3]));
        image.setHierarchy(genericDraweeHierarchyBuilder.build());
    }

    public static void setBorder(SimpleDraweeView image, int color, float width)
    {
        final RoundingParams roundingParams = image.getHierarchy()
                                                   .getRoundingParams();
        if (roundingParams != null)
        {
            roundingParams.setBorder(color, width);
            image.getHierarchy()
                 .setRoundingParams(roundingParams);
        }
    }

    public interface OnDownloadImageListener
    {
        public void onDownloadImageDone(Bitmap bitmap);
    }

    public static void downloadImage(Context context, String url, final OnDownloadImageListener listener)
    {
        final Handler handler = new Handler();
        final ImageRequest imageRequest = ImageRequestBuilder.newBuilderWithSource(Uri.parse(url))
                                                             .build();
        final ImagePipeline imagePipeline = Fresco.getImagePipeline();
        final DataSource<CloseableReference<CloseableImage>> dataSource = imagePipeline.fetchDecodedImage(imageRequest, context);
        dataSource.subscribe(new BaseBitmapDataSubscriber()
        {
            @Override
            public void onNewResultImpl(final Bitmap bitmap)
            {
                if (dataSource.isFinished() && bitmap != null)
                {
                    handler.post(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            if (listener != null)
                            {
                                listener.onDownloadImageDone(Bitmap.createBitmap(bitmap));
                            }
                        }
                    });
                    dataSource.close();
                }
            }

            @Override
            public void onFailureImpl(DataSource dataSource)
            {
                if (dataSource != null)
                {
                    dataSource.close();
                }
            }
        }, CallerThreadExecutor.getInstance());
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
    public static Bitmap ImageView2Bitmap(ImageView image)
    {
        if (image == null)
        {
            return null;
        }

        return Drawable2Bitmap(image.getDrawable());

    }

    public static Bitmap Drawable2Bitmap(Drawable drawable)
    {
        if (drawable == null)
        {
            return null;
        }

        return ((BitmapDrawable) drawable).getBitmap();
    }

    public static Drawable Bitmap2Drawable(Context context, Bitmap bitmap)
    {
        if (bitmap == null)
        {
            return null;
        }

        return new BitmapDrawable(context.getResources(), bitmap);
    }

    public static Bitmap restoreImageOrientation(final Bitmap selectedBitmap, final String imagePath)
    {
        try
        {
            int rotate = 0;
            final File imageFile = new File(imagePath);
            final ExifInterface exif = new ExifInterface(imageFile.getAbsolutePath());
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

            switch (orientation)
            {
                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotate = 270;
                    break;

                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotate = 180;
                    break;

                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotate = 90;
                    break;
            }

            final Matrix matrix = new Matrix();
            matrix.postRotate(rotate);

            return Bitmap.createBitmap(selectedBitmap, 0, 0, selectedBitmap.getWidth(), selectedBitmap.getHeight(), matrix, true);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return selectedBitmap;
    }


    public static Bitmap scaleCenterCrop(Bitmap source)
    {
        final int sourceWidth = source.getWidth();
        final int sourceHeight = source.getHeight();
        final int newWidth = Math.min(sourceWidth, sourceHeight);

        final float xScale = (float) newWidth / sourceWidth;
        final float yScale = (float) newWidth / sourceHeight;
        final float scale = Math.max(xScale, yScale);

        final float scaledWidth = scale * sourceWidth;
        final float scaledHeight = scale * sourceHeight;

        final float left = (newWidth - scaledWidth) / 2;
        final float top = (newWidth - scaledHeight) / 2;

        final RectF targetRect = new RectF(left, top, left + scaledWidth, top + scaledHeight);

        final Bitmap dest = Bitmap.createBitmap(newWidth, newWidth, source.getConfig());
        final Canvas canvas = new Canvas(dest);
        canvas.drawBitmap(source, null, targetRect, null);

        return dest;
    }

    /**
     * @param imageFileUri Uri.fromFile(new File(Environment.getExternalStorageDirectory(), "temp.jpg"));
     */
    public static long saveImageFromUrl(Context context, String url, Uri imageFileUri, String title, String description)
    {
        final DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);

        final DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));

        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE)
               .setTitle(title)
               .setDescription(description)
               .setDestinationUri(imageFileUri);

        return downloadManager.enqueue(request);
    }

}
