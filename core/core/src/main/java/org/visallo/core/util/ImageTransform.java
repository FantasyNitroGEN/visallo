package org.visallo.core.util;

/**
 * NOTE: When displaying the image, Make sure to Flip first, and then Rotate.
 * Transformations must be done in this order.
 */
public class ImageTransform {
    private final boolean yAxisFlipNeeded;
    private final int cwRotationNeeded;

    public ImageTransform(boolean yAxisFlipNeeded, int cwRotationNeeded) {
        this.yAxisFlipNeeded = yAxisFlipNeeded;
        this.cwRotationNeeded = cwRotationNeeded;
    }

    public boolean isYAxisFlipNeeded() {
        return yAxisFlipNeeded;
    }

    public int getCWRotationNeeded() {
        return cwRotationNeeded;
    }

    @Override
    public String toString() {
        return "ImageTransform{" +
                "yAxisFlipNeeded=" + yAxisFlipNeeded +
                ", cwRotationNeeded=" + cwRotationNeeded +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ImageTransform that = (ImageTransform) o;
        if (yAxisFlipNeeded != that.yAxisFlipNeeded) {
            return false;
        }
        return cwRotationNeeded == that.cwRotationNeeded;

    }

    @Override
    public int hashCode() {
        int result = (yAxisFlipNeeded ? 1 : 0);
        result = 31 * result + cwRotationNeeded;
        return result;
    }
}
