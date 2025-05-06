package org.elephant.cellsparse.tasks;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import javax.imageio.ImageIO;

import javafx.concurrent.Task;
import qupath.fx.dialogs.Dialogs;
import qupath.lib.images.servers.ImageServer;
import qupath.lib.objects.PathObject;
import qupath.lib.regions.RegionRequest;
import qupath.lib.roi.interfaces.ROI;

public abstract class CellsparseTask<T> extends Task<T> {

    public static String base64Encode(final BufferedImage bufferedImage) {
        String base64Image = null;
        try {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage, "tif", baos);
            final byte[] bytes = baos.toByteArray();
            base64Image = Base64.getEncoder().encodeToString(bytes);
        } catch (IOException e) {
            Dialogs.showErrorMessage(CellsparseTask.class.getName(), e);
        }
        return base64Image;
    }

    BufferedImage readRegionFromServer(final ImageServer<BufferedImage> imageServer,
            final RegionRequest regionRequest) {
        BufferedImage image = null;
        try {
            image = imageServer.readRegion(regionRequest);
        } catch (IOException e) {
            Dialogs.showErrorMessage(getClass().getName(), e);
        }
        return image;
    }

    @Override
    protected abstract T call() throws Exception;

    public static ROI scaleAndTranslatePathObject(final PathObject pathObject, final RegionRequest regionRequest) {
        final double scale = regionRequest.getDownsample();
        final double translateX = regionRequest.getX();
        final double translateY = regionRequest.getY();
        return pathObject.getROI().scale(scale, scale).translate(translateX, translateY);
    }
}
