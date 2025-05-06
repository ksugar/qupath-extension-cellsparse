package org.elephant.cellsparse.tasks;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.elephant.cellsparse.CellsparseCommand;
import org.elephant.cellsparse.lib.http.HttpUtils;
import org.elephant.cellsparse.lib.http.MultipartBodyBuilder;
import org.elephant.cellsparse.models.CellsparseModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ij.ImagePlus;
import ij.plugin.ImageCalculator;
import qupath.imagej.tools.IJTools;
import qupath.lib.gui.viewer.QuPathViewer;
import qupath.lib.images.ImageData;
import qupath.lib.images.servers.LabeledImageServer;
import qupath.lib.images.servers.LabeledOffsetImageServer;
import qupath.lib.objects.classes.PathClass;
import qupath.lib.regions.RegionRequest;

public class CellsparseTrainTask extends CellsparseTask<Boolean> {

    private static final Logger logger = LoggerFactory.getLogger(CellsparseTrainTask.class);

    private final CellsparseCommand command;
    private final ImageData<BufferedImage> imageData;
    private final String endpointURL;
    private final CellsparseModel model;
    private final Collection<RegionRequest> regionRequests;

    public CellsparseTrainTask(Builder builder) {
        QuPathViewer viewer = builder.viewer;
        Objects.requireNonNull(builder, "Viewer must not be null!");

        this.command = builder.command;
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
    protected Boolean call() throws Exception {
        String requestId = generateRequestId(); // 一意のリクエストIDを生成
        CellsparseTrainSubTask task = new CellsparseTrainSubTask(requestId);
        task.setOnSucceeded(event -> {
            if (task.getValue() != null) {
                command.updateInfoText("Training has started successfully");
            } else {
                command.updateInfoText("Training has been cancelled");
            }
        });
        task.setOnFailed(event -> {
            Throwable ex = task.getException();
            command.updateInfoText("Task failed: " + ex.getMessage() + "\n"
                    + "Please check that the samapi server (v0.4 and above) is running and the URL is correct.");
        });
        task.setOnCancelled(event -> {
            if (!isCancelled()) {
                cancel();
            }
            sendCancelRequest(requestId); // サーバーにキャンセル通知を送信
            command.updateInfoText("Task is cancelled");
        });
        Future<?> future = CellsparseTaskUtils.submitTask(command, task);
        @SuppressWarnings("unchecked")
        CompletableFuture<HttpResponse<String>> responseFuture = (CompletableFuture<HttpResponse<String>>) future.get();
        setOnCancelled(event -> {
            if (getOnCancelled() != null) {
                getOnCancelled().handle(event);
            }
            HttpUtils.cancelRequest(responseFuture);
        });

        // Sychronize the results
        try {
            responseFuture.get();
        } catch (CancellationException e) {
            logger.warn("Task is cancelled", e);
        } catch (ExecutionException e) {
            logger.warn("Error while waiting for task to complete", e);
        } catch (InterruptedException e) {
            logger.warn("Task is interrupted", e);
        }
        return true;
    }

    private void sendCancelRequest(String requestId) {
        try {
            HttpUtils.sendCancelRequest(endpointURL + "/cancel", requestId);
            logger.info("Cancel request sent for requestId: {}", requestId);
        } catch (IOException | InterruptedException e) {
            logger.warn("Failed to send cancel request", e);
        }
    }

    private String generateRequestId() {
        return java.util.UUID.randomUUID().toString();
    }

    class CellsparseTrainSubTask extends CellsparseTask<CompletableFuture<HttpResponse<String>>> {

        private final String requestId;

        CellsparseTrainSubTask(String requestId) {
            this.requestId = requestId;
        }

        @Override
        protected CompletableFuture<HttpResponse<String>> call() throws Exception {
            updateProgress(0, regionRequests.size());
            List<BufferedImage> imageList = new ArrayList<>();
            List<BufferedImage> labelList = new ArrayList<>();
            int count = 0;
            for (RegionRequest regionRequest : regionRequests) {
                if (isCancelled()) {
                    updateProgress(0, 0);
                    return null;
                }
                final BufferedImage image = readRegionFromServer(imageData.getServer(), regionRequest);
                imageList.add(image);
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
                labelList.add(lblImage);
                updateProgress(++count, regionRequests.size());
            }
            final MultipartBodyBuilder multipartBodyBuilder = HttpUtils.createImageUploadMultipartBodyBuilder(imageList,
                    labelList);
            final String bodyJson = model.getRequestBodyStringTrain(null, null);
            multipartBodyBuilder.addJsonField("json_data", bodyJson);
            multipartBodyBuilder.addJsonField("request_id", requestId);
            try {
                CompletableFuture<HttpResponse<String>> future = HttpUtils.sendAsyncMultipartRequest(endpointURL,
                        multipartBodyBuilder);
                HttpResponse<String> response = future.join();
                if (response.statusCode() == HttpURLConnection.HTTP_OK) {
                    logger.info("Training have been done successfully");
                } else {
                    final String message = String.format("HTTP error: %d\n%s", response.statusCode(), response.body());
                    logger.warn(message);
                    throw new IOException(message);
                }
                return future;
            } catch (IOException | InterruptedException e) {
                final String message = "Interrupted while sending request to server";
                logger.debug(message, e);
                throw new IOException(message, e);
            } finally {
                updateProgress(regionRequests.size(), regionRequests.size());
            }
        }
    }

    /**
     * New builder for a CellsparseTrainTask class.
     * 
     * @param viewer
     *               the viewer containing the image to be processed
     * @return the builder
     */
    public static Builder builder(QuPathViewer viewer, CellsparseCommand command) {
        return new Builder(viewer, command);
    }

    /**
     * Builder for a CellsparseTrainTask class.
     */
    public static class Builder {

        private final QuPathViewer viewer;
        private final CellsparseCommand command;

        private String endpointURL;
        private CellsparseModel model;
        private Collection<RegionRequest> regionRequests;

        private Builder(QuPathViewer viewer, CellsparseCommand command) {
            this.viewer = viewer;
            this.command = command;
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
         * @param Collection<RegionRequest>
         * @return this builder
         */
        public Builder regionRequests(final Collection<RegionRequest> regionRequests) {
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
