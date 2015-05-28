package org.visallo.core.util;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;

public class ImageTransformExtractorTest {
    @Test
    public void testGetImageTransform() throws Exception {
        assertEquals(new ImageTransform(false, 0), getImageTransform("Landscape_1.jpg"));
        assertEquals(new ImageTransform(true, 0), getImageTransform("Landscape_2.jpg"));
        assertEquals(new ImageTransform(false, 180), getImageTransform("Landscape_3.jpg"));
        assertEquals(new ImageTransform(true, 180), getImageTransform("Landscape_4.jpg"));
        assertEquals(new ImageTransform(true, 270), getImageTransform("Landscape_5.jpg"));
        assertEquals(new ImageTransform(false, 90), getImageTransform("Landscape_6.jpg"));
        assertEquals(new ImageTransform(true, 90), getImageTransform("Landscape_7.jpg"));
        assertEquals(new ImageTransform(false, 270), getImageTransform("Landscape_8.jpg"));
    }

    private ImageTransform getImageTransform(String resourceName) throws IOException {
        String resourcePath = "/org/visallo/core/util/imageTransformExtractorTest/" + resourceName;
        InputStream in = this.getClass().getResourceAsStream(resourcePath);
        if (in == null) {
            throw new RuntimeException("Invalid resource: " + resourceName);
        }
        byte[] data = IOUtils.toByteArray(in);
        return ImageTransformExtractor.getImageTransform(data);
    }
}