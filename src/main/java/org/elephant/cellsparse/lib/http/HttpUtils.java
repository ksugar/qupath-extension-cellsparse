package org.elephant.cellsparse.lib.http;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

import org.elephant.cellsparse.lib.io.TiffUtils;

public class HttpUtils {

    /**
     * Creates a multipart/form-data body builder for an image upload request.
     *
     * @param image
     *              The image to upload.
     * @return The multipart/form-data body builder.
     * @throws IOException
     *                     If an error occurs during writing.
     */
    public static MultipartBodyBuilder createImageUploadMultipartBodyBuilder(List<BufferedImage> imageList)
            throws IOException {
        final String boundary = "----------------" + System.currentTimeMillis();
        return createImageUploadMultipartBodyBuilder(boundary, imageList);
    }

    /**
     * Creates a multipart/form-data body builder for an image upload request.
     *
     * @param image
     *              The image to upload.
     * @return The multipart/form-data body builder.
     * @throws IOException
     *                     If an error occurs during writing.
     */
    public static MultipartBodyBuilder createImageUploadMultipartBodyBuilder(List<BufferedImage> imageList,
            List<BufferedImage> labelList)
            throws IOException {
        MultipartBodyBuilder builder = createImageUploadMultipartBodyBuilder(imageList);
        for (int i = 0; i < labelList.size(); i++) {
            final BufferedImage label = labelList.get(i);
            final byte[] labelBytes = TiffUtils.bufferedImageToTiffBytes(label);
            builder.addFilePart("labels", String.format("label_%05d.tif", i), "image/tiff", labelBytes);
        }
        return builder;
    }

    /**
     * Creates a multipart/form-data body builder for an image upload request.
     *
     * @param boundary
     *                 The boundary string to use.
     * @param image
     *                 The image to upload.
     * @return The multipart/form-data body builder.
     * @throws IOException
     *                     If an error occurs during writing.
     */
    public static MultipartBodyBuilder createImageUploadMultipartBodyBuilder(String boundary,
            List<BufferedImage> imageList)
            throws IOException {
        final MultipartBodyBuilder builder = new MultipartBodyBuilder(boundary);
        for (int i = 0; i < imageList.size(); i++) {
            final BufferedImage image = imageList.get(i);
            final byte[] imageBytes = TiffUtils.bufferedImageToTiffBytes(image);
            builder.addFilePart("images", String.format("image_%05d.tif", i), "image/tiff", imageBytes);
        }
        return builder;
    }
}
