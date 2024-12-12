package org.elephant.cellsparse.tasks;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.elephant.cellsparse.models.CellsparseModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import ij.ImagePlus;
import ij.plugin.ImageCalculator;
import qupath.imagej.tools.IJTools;
import qupath.lib.gui.viewer.QuPathViewer;
import qupath.lib.images.ImageData;
import qupath.lib.images.servers.LabeledImageServer;
import qupath.lib.images.servers.LabeledOffsetImageServer;
import qupath.lib.objects.PathObject;
import qupath.lib.objects.classes.PathClass;
import qupath.lib.regions.RegionRequest;

public class CellsparseTrainTask extends CellsparseTask {

    private static final Logger logger = LoggerFactory.getLogger(CellsparseTrainTask.class);

    private final ImageData<BufferedImage> imageData;
    private final String endpointURL;
    private final CellsparseModel model;
    private final List<RegionRequest> regionRequests;

    public CellsparseTrainTask(Builder builder) {
        QuPathViewer viewer = builder.viewer;
        Objects.requireNonNull(builder, "Viewer must not be null!");

        this.imageData = viewer.getImageData();
        this.endpointURL = builder.endpointURL;
        this.model = builder.model;
        if (builder.regionRequests == null) {
            this.regionRequests = new ArrayList<>();
            this.regionRequests.add(RegionRequest.createInstance(imageData.getServer()));
        } else {
            this.regionRequests = builder.regionRequests;
        }
    }

    @Override
    protected List<PathObject> call() throws Exception {
        updateProgress(0, 1);
        List<String> strImages = new ArrayList<>();
        List<String> strLabels = new ArrayList<>();
        for (RegionRequest regionRequest : regionRequests) {
            final BufferedImage image = readRegionFromServer(imageData.getServer(), regionRequest);
            final String strImage = base64Encode(image);
            strImages.add(strImage);
            final LabeledImageServer bgLabelServer = new LabeledImageServer.Builder(imageData)
                    .backgroundLabel(0).addLabel("Background", 1).multichannelOutput(false).build();
            final BufferedImage bgImage = readRegionFromServer(bgLabelServer, regionRequest);
            final LabeledOffsetImageServer fgLabelServer = new LabeledOffsetImageServer.Builder(imageData)
                    .useFilter(pathObject -> pathObject
                            .getPathClass() == PathClass.getInstance("Foreground"))
                    .useInstanceLabels()
                    .offset(1).build();
            final BufferedImage fgImage = readRegionFromServer(fgLabelServer, regionRequest);
            final ImageCalculator imageCalculator = new ImageCalculator();
            final ImagePlus bgImp = IJTools.convertToUncalibratedImagePlus("Background", bgImage);
            final ImagePlus fgImp = IJTools.convertToUncalibratedImagePlus("Foreground", fgImage);
            final BufferedImage lblImage = imageCalculator.run("Max", bgImp, fgImp).getBufferedImage();
            final String strLabel = base64Encode(lblImage);
            strLabels.add(strLabel);
        }
        final String bodyJson = model.getRequestBodyStringTrain(strImages, strLabels);
        try {
            HttpResponse<String> response = CellsparseTrainTask.sendRequest(endpointURL, bodyJson);
            if (response.statusCode() == HttpURLConnection.HTTP_OK) {
                logger.info("Training have been done successfully");
                return Collections.emptyList();
            } else {
                logger.warn(String.format("HTTP error: %d\n%s", response.statusCode(), response.body()));
                return Collections.emptyList();
            }
        } catch (IOException | InterruptedException e) {
            logger.warn("Interrupted while sending request to server", e);
            return Collections.emptyList();
        } finally {
            updateProgress(1, 1);
        }
    }

    /**
     * New builder for a CellsparseTrainTask class.
     * 
     * @param viewer
     *               the viewer containing the image to be processed
     * @return the builder
     */
    public static Builder builder(QuPathViewer viewer) {
        return new Builder(viewer);
    }

    /**
     * Builder for a CellsparseTrainTask class.
     */
    public static class Builder {

        private QuPathViewer viewer;

        private String endpointURL;
        private CellsparseModel model;
        private List<RegionRequest> regionRequests;

        private Builder(QuPathViewer viewer) {
            this.viewer = viewer;
        }

        /**
         * Specify the server URL (required).
         * 
         * @param endpointURL
         * @return this builder
         */
        public Builder endpointURL(final String endpointURL) {
            this.endpointURL = endpointURL;
            return this;
        }

        /**
         * Specify the model (required).
         * 
         * @param model
         * @return this builder
         */
        public Builder model(final CellsparseModel model) {
            this.model = model;
            return this;
        }

        /**
         * Specify the region request (required).
         * 
         * @param List<regionRequest>
         * @return this builder
         */
        public Builder regionRequests(final List<RegionRequest> regionRequests) {
            this.regionRequests = regionRequests;
            return this;
        }

        /**
         * Build the detection task.
         * 
         * @return
         */
        public CellsparseTrainTask build() {
            return new CellsparseTrainTask(this);
        }

    }

}
