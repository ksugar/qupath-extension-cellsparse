package org.elephant.cellsparse.tasks;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import org.elephant.cellsparse.models.CellsparseModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import qupath.lib.gui.viewer.QuPathViewer;
import qupath.lib.images.ImageData;
import qupath.lib.io.GsonTools;
import qupath.lib.objects.PathObject;
import qupath.lib.objects.PathObjects;
import qupath.lib.objects.PathROIObject;
import qupath.lib.regions.RegionRequest;
import qupath.lib.roi.interfaces.ROI;

public class CellsparseInferSubTask extends CellsparseTask<List<PathObject>> {

    private static final Logger logger = LoggerFactory.getLogger(CellsparseInferSubTask.class);

    private final ImageData<BufferedImage> imageData;
    private final String endpointURL;
    private final CellsparseModel model;
    private final RegionRequest regionRequest;
    private final Function<ROI, PathObject> creatorFun;

    public CellsparseInferSubTask(Builder builder) {
        QuPathViewer viewer = builder.viewer;
        Objects.requireNonNull(builder, "Viewer must not be null!");

        this.imageData = viewer.getImageData();
        this.endpointURL = builder.endpointURL;
        this.model = builder.model;
        this.model.getParameterList();
        if (builder.regionRequest == null) {
            this.regionRequest = RegionRequest.createInstance(imageData.getServer());
        } else {
            this.regionRequest = builder.regionRequest;
        }
        this.creatorFun = builder.creatorFun;
    }

    @Override
    protected List<PathObject> call() throws Exception {
        updateProgress(0, 1);
        final BufferedImage image = readRegionFromServer(imageData.getServer(), regionRequest);
        final String strImage = base64Encode(image);

        final Gson gson = GsonTools.getInstance();
        final String bodyJson = model.getRequestBodyStringInfer(strImage);
        final Type type = new com.google.gson.reflect.TypeToken<List<PathObject>>() {
        }.getType();
        try {
            HttpResponse<String> response = CellsparseInferSubTask.sendRequest(endpointURL, bodyJson);
            if (response.statusCode() == HttpURLConnection.HTTP_OK) {
                List<PathObject> pathObjects = gson.fromJson(response.body(), type);
                for (PathObject pathObject : pathObjects) {
                    ((PathROIObject) pathObject).setROI(scaleAndTranslatePathObject(pathObject, regionRequest));
                }
                return pathObjects;
            } else {
                final String message = String.format("HTTP error: %d\n%s", response.statusCode(), response.body());
                logger.warn(message);
                throw new IOException(message);
            }
        } catch (IOException | InterruptedException e) {
            final String message = "Interrupted while sending request to server";
            logger.debug(message, e);
            throw new IOException(message, e);
        } finally {
            updateProgress(1, 1);
        }
    }

    /**
     * New builder for a CellsparseInferTask class.
     * 
     * @param viewer
     *               the viewer containing the image to be processed
     * @return the builder
     */
    public static Builder builder(QuPathViewer viewer) {
        return new Builder(viewer);
    }

    /**
     * Builder for a CellsparseInferTask class.
     */
    public static class Builder {

        private QuPathViewer viewer;

        private String endpointURL;
        private CellsparseModel model;
        private RegionRequest regionRequest;
        private Function<ROI, PathObject> creatorFun;

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
         * @param regionRequest
         * @return this builder
         */
        public Builder regionRequest(final RegionRequest regionRequest) {
            this.regionRequest = regionRequest;
            return this;
        }

        /**
         * Create annotations rather than detections (the default).
         * If cell expansion is not zero, the nucleus will be included as a child
         * object.
         * 
         * @return this builder
         */
        public Builder createAnnotations() {
            this.creatorFun = r -> PathObjects.createAnnotationObject(r);
            return this;
        }

        /**
         * Build the detection task.
         * 
         * @return
         */
        public CellsparseInferSubTask build() {
            return new CellsparseInferSubTask(this);
        }

    }

}
