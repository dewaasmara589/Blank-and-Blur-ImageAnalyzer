package com.alfastore.checkimagefromcamera;

import android.graphics.Bitmap;
import android.graphics.Color;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

public class ImageAnalyzer {

    // A high-end camera might have a baseline variance of 500, while a budget camera might only hit 150 even when sharp
    private static final double BLUR_THRESHOLD = 100.0;

    // Threshold: 0 is absolute black, 255 is white.
    // 15-20 is usually safe for "near black" sensor noise.
    private static final int BLACK_THRESHOLD = 20;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface AnalysisCallback {
        void onResult(boolean isBlack);
    }

    public void checkIfImageIsBlack(Bitmap bitmap, AnalysisCallback callback) {
        executor.execute(() -> {
            boolean result = isBlackSampling(bitmap);
            mainHandler.post(() -> callback.onResult(result));
        });
    }

    private boolean isBlackSampling(Bitmap bitmap) {
        if (bitmap == null) return true;

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        // Step 1: Sample every 10th pixel to save CPU and Memory
        int sampleStep = 10;
        long totalLuminance = 0;
        int sampledPixelsCount = 0;

        for (int y = 0; y < height; y += sampleStep) {
            for (int x = 0; x < width; x += sampleStep) {
                int pixel = bitmap.getPixel(x, y);

                // Extract RGB values
                int r = Color.red(pixel);
                int g = Color.green(pixel);
                int b = Color.blue(pixel);

                // Use the Weighted Luminance Formula
                // This accounts for the human eye being more sensitive to Green
                double luminance = (0.299 * r) + (0.587 * g) + (0.114 * b);

                totalLuminance += luminance;
                sampledPixelsCount++;
            }
        }

        if (sampledPixelsCount == 0) return true;

        long averageBrightness = totalLuminance / sampledPixelsCount;
        Log.d("TAG", "Average BlackSampling: " + String.valueOf(averageBrightness));

        return averageBrightness < BLACK_THRESHOLD;
    }

    public static boolean isBlurry(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        // Use a 3x3 Laplacian Kernel:
        // [ 0, -1,  0]
        // [-1,  4, -1]
        // [ 0, -1,  0]

        double totalSum = 0;
        double totalSqSum = 0;
        int count = 0;

        // Optimization: Downsample the image or skip pixels if it's high resolution
        int step = 2;

        for (int x = 1; x < width - 1; x += step) {
            for (int y = 1; y < height - 1; y += step) {
                // Get pixel and its immediate neighbors (simplified to grayscale)
                float center = getLuminance(bitmap.getPixel(x, y));
                float left = getLuminance(bitmap.getPixel(x - 1, y));
                float right = getLuminance(bitmap.getPixel(x + 1, y));
                float up = getLuminance(bitmap.getPixel(x, y - 1));
                float down = getLuminance(bitmap.getPixel(x, y + 1));

                // Apply Laplacian formula: (4 * center) - neighbors
                double laplace = (4 * center) - (left + right + up + down);

                totalSum += laplace;
                totalSqSum += (laplace * laplace);
                count++;
            }
        }

        // Calculate Variance: Var(X) = E[X^2] - (E[X])^2
        double mean = totalSum / count;
        double variance = (totalSqSum / count) - (mean * mean);

        Log.d("TAG", "Variance BLUR: " + String.valueOf(variance));

        return variance < BLUR_THRESHOLD;
    }

    private static float getLuminance(int pixel) {
        int r = (pixel >> 16) & 0xFF;
        int g = (pixel >> 8) & 0xFF;
        int b = pixel & 0xFF;
        // ITU-R BT.709 formula for luminance
        return 0.2126f * r + 0.7152f * g + 0.0722f * b;
    }
}
