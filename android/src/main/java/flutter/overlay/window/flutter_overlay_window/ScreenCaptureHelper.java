package flutter.overlay.window.flutter_overlay_window;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

public class ScreenCaptureHelper {
    private static final String TAG = "ScreenCaptureHelper";
    private static MediaProjection mediaProjection;
    private static ImageReader imageReader;
    private static int screenWidth;
    private static int screenHeight;
    private static int screenDensity;

    /**
     * Configure the MediaProjection instance.
     */
    public static void configure(MediaProjection projection) {
        mediaProjection = projection;
        Log.d(TAG, "MediaProjection configured");
    }

    /**
     * Check if the MediaProjection is ready.
     */
    public static boolean isReady() {
        return mediaProjection != null;
    }

    /**
     * Initialize the screen capture setup.
     * Call this before attempting capture.
     */
    public static void initialize(Context context) {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        Log.d("ScreenCaptureHElper", "intializeWindow: ");
        if (windowManager != null) {
            windowManager.getDefaultDisplay().getMetrics(metrics);
            screenWidth = metrics.widthPixels;
            screenHeight = metrics.heightPixels;
            screenDensity = metrics.densityDpi;
        }
        imageReader = ImageReader.newInstance(screenWidth, screenHeight, PixelFormat.RGBA_8888, 2);
        Log.d(TAG, "ImageReader initialized: " + screenWidth + "x" + screenHeight + ", density: " + screenDensity);
    }

    /**
     * Capture the screen and return the image as PNG byte array.
     * The passed context must be an Activity context.
     */
    public static byte[] capture(Context context) throws Exception {
        if (mediaProjection == null) {
            Log.e(TAG, "MediaProjection not configured");
            throw new Exception("MediaProjection not configured");
        }
        // Ensure initialization is done.
        if (imageReader == null) {
            initialize(context);
        }
        Log.d("ScreenCaptureHElper", "intializecapture: ");
        VirtualDisplay virtualDisplay = mediaProjection.createVirtualDisplay(
                "ScreenCapture",
                screenWidth, screenHeight, screenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.getSurface(), null, new Handler(Looper.getMainLooper())
        );

        // Wait briefly to allow a frame to be captured.
        Thread.sleep(500);

        Image image = imageReader.acquireLatestImage();
        if (image == null) {
            Log.e(TAG, "Failed to acquire image");
            virtualDisplay.release();
            throw new Exception("No image captured");
        }
        try {
            int width = image.getWidth();
            int height = image.getHeight();
            Image.Plane[] planes = image.getPlanes();
            if (planes.length == 0) {
                virtualDisplay.release();
                throw new Exception("No image planes available");
            }
            ByteBuffer buffer = planes[0].getBuffer();
            int pixelStride = planes[0].getPixelStride();
            int rowStride = planes[0].getRowStride();
            int rowPadding = rowStride - pixelStride * width;

            Bitmap bitmap = Bitmap.createBitmap(
                    width + rowPadding / pixelStride,
                    height,
                    Bitmap.Config.ARGB_8888);
            bitmap.copyPixelsFromBuffer(buffer);

            // Crop the bitmap to the actual screen size.
            Bitmap croppedBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            croppedBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
            Log.d(TAG, "Screen capture successful");
            return outputStream.toByteArray();
        } finally {
            image.close();
            virtualDisplay.release();
        }
    }

    /**
     * Release resources used for screen capture.
     */
    public static void releaseResources() {
        if (mediaProjection != null) {
            mediaProjection.stop();
            mediaProjection = null;
            Log.d(TAG, "MediaProjection resources released");
        }
        if (imageReader != null) {
            imageReader.close();
            imageReader = null;
            Log.d(TAG, "ImageReader resources released");
        }
    }
}
