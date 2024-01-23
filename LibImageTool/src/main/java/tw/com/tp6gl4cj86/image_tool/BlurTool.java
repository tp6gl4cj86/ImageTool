package tw.com.tp6gl4cj86.image_tool;

import android.app.Activity;
import android.net.Uri;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.backends.pipeline.PipelineDraweeController;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.facebook.imagepipeline.request.Postprocessor;

import jp.wasabeef.fresco.processors.BlurPostprocessor;

public class BlurTool
{

    public static void blur(Activity activity, SimpleDraweeView mImage, String url)
    {
        blur(activity, 12, mImage, url);
    }

    public static void blur(Activity activity, int radius, SimpleDraweeView mImage, String url)
    {
        if (activity != null && url != null)
        {
            final Postprocessor processor = new BlurPostprocessor(activity, radius);
            final ImageRequest request = ImageRequestBuilder.newBuilderWithSource(Uri.parse(url))
                                                            .setPostprocessor(processor)
                                                            .build();

            final PipelineDraweeController controller = (PipelineDraweeController) Fresco.newDraweeControllerBuilder()
                                                                                         .setImageRequest(request)
                                                                                         .setOldController(mImage.getController())
                                                                                         .build();
            mImage.setController(controller);
        }
    }

}
