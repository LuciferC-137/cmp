package com.luciferc137.cmp.ui.utils;

import javafx.geometry.Insets;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Extract the main colors from a picture to generate a background gradient
 */
public class AlbumColorExtractor {

    private static final int THUMBNAIL_SIZE = 16; // Reduction factor for speed

    /**
     * Extract k dominant colors from the source image.
     */
    public static List<Color> extractDominantColors(Image source, int k) {
        // 1. Downsampling
        Image thumbnail = resize(source, THUMBNAIL_SIZE, THUMBNAIL_SIZE);
        PixelReader reader = thumbnail.getPixelReader();

        // 2. Collect and pixel filtering
        List<double[]> pixels = new ArrayList<>();
        for (int y = 0; y < THUMBNAIL_SIZE; y++) {
            for (int x = 0; x < THUMBNAIL_SIZE; x++) {
                Color c = reader.getColor(x, y);
                if (isRelevant(c)) {
                    pixels.add(new double[]{c.getRed(), c.getGreen(), c.getBlue()});
                }
            }
        }

        // Fallback if everything was filtered out (e.g., black album art)
        if (pixels.size() < k) {
            pixels.clear();
            for (int y = 0; y < THUMBNAIL_SIZE; y++) {
                for (int x = 0; x < THUMBNAIL_SIZE; x++) {
                    Color c = reader.getColor(x, y);
                    pixels.add(new double[]{c.getRed(), c.getGreen(), c.getBlue()});
                }
            }
        }

        // 3. Median cut
        List<List<double[]>> buckets = medianCut(pixels, k);

        // 4. Mean of each bucket, sorted by population (dominant color first)
        buckets.sort((a, b) -> b.size() - a.size());

        List<Color> result = new ArrayList<>();
        for (List<double[]> bucket : buckets) {
            result.add(average(bucket));
        }
        return result;
    }

    /** Ignore the pixels that are almost white/black/gray, often artifacts at the edges. */
    private static boolean isRelevant(Color c) {
        double brightness = Math.max(c.getRed(), Math.max(c.getGreen(), c.getBlue()));
        double min = Math.min(c.getRed(), Math.min(c.getGreen(), c.getBlue()));
        double saturation = brightness == 0 ? 0 : (brightness - min) / brightness;
        return saturation > 0.15 && brightness > 0.08 && brightness < 0.95;
    }

    /** Recursive median cut algorithm. */
    private static List<List<double[]>> medianCut(List<double[]> pixels, int k) {
        List<List<double[]>> buckets = new ArrayList<>();
        buckets.add(pixels);

        while (buckets.size() < k) {
            int idx = largestRangeBucketIndex(buckets);
            List<double[]> toSplit = buckets.remove(idx);
            if (toSplit.size() < 2) {
                buckets.add(toSplit);
                break;
            }

            int axis = widestAxis(toSplit);
            toSplit.sort(Comparator.comparingDouble(a -> a[axis]));

            int mid = toSplit.size() / 2;
            buckets.add(new ArrayList<>(toSplit.subList(0, mid)));
            buckets.add(new ArrayList<>(toSplit.subList(mid, toSplit.size())));
        }
        return buckets;
    }

    private static int largestRangeBucketIndex(List<List<double[]>> buckets) {
        int best = 0;
        double bestRange = -1;
        for (int i = 0; i < buckets.size(); i++) {
            double range = rangeOf(buckets.get(i), widestAxis(buckets.get(i)));
            if (range > bestRange) {
                bestRange = range;
                best = i;
            }
        }
        return best;
    }

    private static int widestAxis(List<double[]> bucket) {
        double bestRange = -1;
        int bestAxis = 0;
        for (int axis = 0; axis < 3; axis++) {
            double range = rangeOf(bucket, axis);
            if (range > bestRange) {
                bestRange = range;
                bestAxis = axis;
            }
        }
        return bestAxis;
    }

    private static double rangeOf(List<double[]> bucket, int axis) {
        double min = Double.MAX_VALUE, max = -Double.MAX_VALUE;
        for (double[] p : bucket) {
            min = Math.min(min, p[axis]);
            max = Math.max(max, p[axis]);
        }
        return max - min;
    }

    private static Color average(List<double[]> bucket) {
        double r = 0, g = 0, b = 0;
        for (double[] p : bucket) {
            r += p[0];
            g += p[1];
            b += p[2];
        }
        int n = bucket.size();
        return Color.color(r / n, g / n, b / n);
    }

    /**
     * Resize an image using bilinear interpolation.
     */
    private static Image resize(Image source, int width, int height) {
        javafx.scene.canvas.Canvas canvas = new javafx.scene.canvas.Canvas(width, height);
        javafx.scene.canvas.GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setImageSmoothing(true);
        gc.drawImage(source, 0, 0, width, height);

        WritableImage output = new WritableImage(width, height);
        canvas.snapshot(null, output);
        return output;
    }

    public static LinearGradient buildAngledGradient(List<Color> colors,
                                                     double angleDegrees,
                                                     double width, double height,
                                                     Color baseColor, double blend) {
        double angleRad = Math.toRadians(angleDegrees);
        double dx = Math.cos(angleRad);
        double dy = Math.sin(angleRad);

        double aspect = width / height;
        dx *= aspect;

        double norm = Math.max(Math.abs(dx), Math.abs(dy));
        dx /= norm;
        dy /= norm;

        double startX = 0.5 - dx / 2;
        double startY = 0.5 - dy / 2;
        double endX = 0.5 + dx / 2;
        double endY = 0.5 + dy / 2;

        List<Stop> stops = new ArrayList<>();
        for (int i = 0; i < colors.size(); i++) {
            Color tempered = baseColor.interpolate(colors.get(i), 1.0 - blend);
            double fraction = colors.size() == 1 ? 0 : (double) i / (colors.size() - 1);
            stops.add(new Stop(fraction, tempered));
        }

        return new LinearGradient(startX, startY, endX, endY, true, CycleMethod.NO_CYCLE, stops);
    }

    public static Background buildAngledBackground(List<Color> colors, double angleDegrees,
                                                   double width, double height,
                                                   Color baseColor, double blend) {
        LinearGradient gradient = buildAngledGradient(colors, angleDegrees, width, height, baseColor, blend);
        return new Background(new BackgroundFill(gradient, new CornerRadii(10), Insets.EMPTY));
    }
}
