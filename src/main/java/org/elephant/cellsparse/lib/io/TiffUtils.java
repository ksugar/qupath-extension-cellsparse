package org.elephant.cellsparse.lib.io;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageOutputStream;

import com.twelvemonkeys.imageio.plugins.tiff.TIFFImageWriterSpi;

public class TiffUtils {

    private static final TIFFImageWriterSpi TIFF_IMAGE_WRITER_SPI = new TIFFImageWriterSpi();

    /**
     * Converts a BufferedImage to a Tiff byte array.
     *
     * @param image
     *              The BufferedImage to convert.
     * @return A byte array containing the Tiff data.
     * @throws IOException
     *                     If an error occurs during writing.
     */
    public static byte[] bufferedImageToTiffBytes(BufferedImage image) throws IOException {
        ImageWriter writer = TIFF_IMAGE_WRITER_SPI.createWriterInstance();
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
                writer.setOutput(ios);
                IIOMetadata metadata = writer.getDefaultImageMetadata(ImageTypeSpecifier.createFromRenderedImage(image),
                        null);
                writer.write(null, new IIOImage(image, null, metadata), null);
            }
            return baos.toByteArray();
        }
    }
}
