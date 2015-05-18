package org.visallo.core.util;

import org.visallo.core.exception.VisalloResourceNotFoundException;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URLConnection;

import static com.google.common.base.Preconditions.checkNotNull;

public class ImageUtils {
    public static byte[] resize(byte[] imageData, int maxWidth, int maxHeight, int jpegQuality) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int type;
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageData));
            checkNotNull(image, "Could not load image");
            type = thumbnailType(image);

            ImageTransform imageTransform = ImageTransformExtractor.getImageTransform(imageData);
            image = reorientImage(image, imageTransform.isYAxisFlipNeeded(), imageTransform.getCWRotationNeeded());

            //Get new image dimensions, which will be used for the icon.
            int[] newImageDims = getScaledDimension(image.getWidth(), image.getHeight(), maxWidth, maxHeight);

            //Resize the image.
            BufferedImage resizedImage = new BufferedImage(newImageDims[0], newImageDims[1], type);
            Graphics2D g = resizedImage.createGraphics();
            if (image.getColorModel().getNumComponents() > 3) {
                g.drawImage(image, 0, 0, resizedImage.getWidth(), resizedImage.getHeight(), null);
                g.dispose();
                ImageIO.write(resizedImage, "png", out);
            } else {
                g.drawImage(image, 0, 0, resizedImage.getWidth(), resizedImage.getHeight(), Color.BLACK, null);
                g.dispose();

                try (ImageOutputStream imageOutputStream = new MemoryCacheImageOutputStream(out)) {
                    JPEGImageWriteParam jpegParams = new JPEGImageWriteParam(null);
                    jpegParams.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                    jpegParams.setCompressionQuality((float) jpegQuality / 100.0f);
                    ImageWriter jpgWriter = ImageIO.getImageWritersByFormatName("jpg").next();
                    jpgWriter.setOutput(imageOutputStream);
                    IIOImage outputImage = new IIOImage(resizedImage, null, null);
                    jpgWriter.write(null, outputImage, jpegParams);
                    jpgWriter.dispose();
                }
            }
        } catch (IOException e) {
            throw new VisalloResourceNotFoundException("Could not resize image", e);
        }
        return out.toByteArray();
    }

    /**
     * Flipping (Mirroring) is performed BEFORE Rotating the image. Example: Flipping the image over the y Axis, and then rotating it 90 degrees CW
     * is not the same as rotating the image 90 degrees CW and then flipping it over the y Axis.
     */
    public static BufferedImage reorientImage(BufferedImage image, boolean yAxisFlipNeeded, int cwRotationNeeded) {
        //If angle greater than 360 is entered, reduce this angle.
        cwRotationNeeded = cwRotationNeeded % 360;

        BufferedImage orientedImage = image;
        if (!yAxisFlipNeeded && cwRotationNeeded == 0) {
            //EXIF Orientation 1.
            return image;
        } else if (yAxisFlipNeeded && cwRotationNeeded == 0) {
            //EXIF Orientation 2.
            orientedImage = flipImageHorizontally(image);
        } else if (!yAxisFlipNeeded && cwRotationNeeded == 180) {
            //EXIF Orientation 3.
            orientedImage = rotateImage(image, 180);
        } else if (yAxisFlipNeeded && cwRotationNeeded == 180) {
            //EXIF Orientation 4.
            orientedImage = flipImageVertically(image);
        } else if (yAxisFlipNeeded && cwRotationNeeded == 270) {
            //EXIF Orientation 5.
            orientedImage = flipImageVertically(image);
            orientedImage = rotateImage(orientedImage, 90);
        } else if (!yAxisFlipNeeded && cwRotationNeeded == 90) {
            //EXIF Orientation 6.
            orientedImage = rotateImage(image, 90);
        } else if (yAxisFlipNeeded && cwRotationNeeded == 90) {
            //EXIF Orientation 7.
            orientedImage = flipImageVertically(image);
            orientedImage = rotateImage(orientedImage, 270);
        } else if (!yAxisFlipNeeded && cwRotationNeeded == 270) {
            //EXIF Orientation 8.
            orientedImage = rotateImage(image, 270);
        } else {
            return image;
        }

        return orientedImage;
    }

    public static BufferedImage flipImageHorizontally(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int type = thumbnailType(image);
        BufferedImage result = new BufferedImage(width, height, type);
        Graphics2D g = result.createGraphics();
        g.drawImage(image, width, 0, 0, height, 0, 0, width, height, null);
        g.dispose();
        return result;
    }

    public static BufferedImage flipImageVertically(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int type = thumbnailType(image);
        BufferedImage result = new BufferedImage(width, height, type);
        Graphics2D g = result.createGraphics();
        g.drawImage(image, 0, height, width, 0, 0, 0, width, height, null);
        g.dispose();
        return result;
    }


    public static BufferedImage rotateImage(BufferedImage image, int cwRotationNeeded) {
        double angle = Math.toRadians(cwRotationNeeded);
        int type = thumbnailType(image);
        double sin = Math.abs(Math.sin(angle));
        double cos = Math.abs(Math.cos(angle));
        int width = image.getWidth();
        int height = image.getHeight();
        int newWidth = (int) Math.floor(width * cos + height * sin);
        int newHeight = (int) Math.floor(height * cos + width * sin);
        BufferedImage result = new BufferedImage(newWidth, newHeight, type);
        Graphics2D g = result.createGraphics();
        g.translate((newWidth - width) / 2, (newHeight - height) / 2);
        g.rotate(angle, width / 2, height / 2);
        g.drawRenderedImage(image, null);
        g.dispose();
        return result;
    }

    public static int[] getScaledDimension(int originalWidth, int originalHeight, int boundWidth, int boundHeight) {
        int newWidth = originalWidth;
        int newHeight = originalHeight;

        if (originalWidth > boundWidth) {
            newWidth = boundWidth;
            newHeight = (newWidth * originalHeight) / originalWidth;
        }

        if (newHeight > boundHeight) {
            newHeight = boundHeight;
            newWidth = (newHeight * originalWidth) / originalHeight;
        }

        return new int[]{newWidth, newHeight};
    }

    public static int thumbnailType(BufferedImage image) {
        if (image.getColorModel().getNumComponents() > 3) {
            return BufferedImage.TYPE_4BYTE_ABGR;
        } else if (image.getColorModel().getNumColorComponents() == 3) {
            return BufferedImage.TYPE_3BYTE_BGR;
        }
        return BufferedImage.TYPE_INT_RGB;
    }

    public static ImageFormat getImageFormat(InputStream in) throws IOException {
        int pushBackLimit = 100;
        PushbackInputStream pushBackIn = new PushbackInputStream(in, pushBackLimit);
        byte[] firstBytes = new byte[pushBackLimit];
        if (pushBackIn.read(firstBytes) <= 0) {
            throw new IOException("Could not read image");
        }
        pushBackIn.unread(firstBytes);

        ByteArrayInputStream bais = new ByteArrayInputStream(firstBytes);
        String mimeType = URLConnection.guessContentTypeFromStream(bais);
        if (!mimeType.startsWith("image/")) {
            mimeType = "image/" + mimeType;
        }
        return new ImageFormat(pushBackIn, mimeType);
    }

    public static class ImageFormat {
        private final PushbackInputStream pushBackIn;
        private final String imageMimeType;

        public ImageFormat(PushbackInputStream pushBackIn, String imageMimeType) {
            this.pushBackIn = pushBackIn;
            this.imageMimeType = imageMimeType;
        }

        public PushbackInputStream getPushBackIn() {
            return pushBackIn;
        }

        public String getImageMimeType() {
            return imageMimeType;
        }
    }
}
