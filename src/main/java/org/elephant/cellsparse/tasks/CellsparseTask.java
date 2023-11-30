package org.elephant.cellsparse.tasks;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.imageio.ImageIO;

import org.elephant.cellsparse.CellsparseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import ij.ImagePlus;
import ij.plugin.ImageCalculator;
import javafx.concurrent.Task;
import qupath.imagej.tools.IJTools;
import qupath.lib.gui.dialogs.Dialogs;
import qupath.lib.gui.viewer.QuPathViewer;
import qupath.lib.images.ImageData;
import qupath.lib.images.servers.ImageServer;
import qupath.lib.images.servers.LabeledImageServer;
import qupath.lib.images.servers.LabeledOffsetImageServer;
import qupath.lib.io.GsonTools;
import qupath.lib.objects.PathObject;
import qupath.lib.objects.classes.PathClass;

public class CellsparseTask extends Task<List<PathObject>> {

    private static final Logger logger = LoggerFactory.getLogger(CellsparseTask.class);

    private final ImageData<BufferedImage> imageData;
    private final String endpointURL;
    private final boolean train;
    private final int epochs;
    private final int batchsize;
    private final int steps;

    public CellsparseTask(Builder builder) {
        QuPathViewer viewer = builder.viewer;
        Objects.requireNonNull(builder, "Viewer must not be null!");

        this.imageData = viewer.getImageData();
        this.endpointURL = builder.endpointURL;
        this.train = builder.train;
        this.epochs = builder.epochs;
        this.batchsize = builder.batchsize;
        this.steps = builder.steps;
    }

    private String base64Encode(final BufferedImage bufferedImage) {
        String base64Image = null;
        try {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage, "png", baos);
            final byte[] bytes = baos.toByteArray();
            base64Image = Base64.getEncoder().encodeToString(bytes);
        } catch (IOException e) {
            Dialogs.showErrorMessage(getClass().getName(), e);
        }
        return base64Image;
    }

    private BufferedImage readRegionFromServer(final ImageServer<BufferedImage> imageServer,
            final double downsample, final int x, final int y, final int width, final int height) {
        BufferedImage image = null;
        try {
            image = imageServer.readRegion(downsample, x, y, width, height);
        } catch (IOException e) {
            Dialogs.showErrorMessage(getClass().getName(), e);
        }
        return image;
    }

    private static HttpResponse<String> sendRequest(String serverURL, String body)
            throws IOException, InterruptedException {
        final HttpRequest request = HttpRequest.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .uri(URI.create(serverURL))
                .header("accept", "application/json")
                .header("Content-Type", "application/json; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpClient client = HttpClient.newHttpClient();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    @Override
    protected List<PathObject> call() throws Exception {
        final BufferedImage image = readRegionFromServer(imageData.getServer(), 1.0, 0, 0,
                imageData.getServer().getWidth(), imageData.getServer().getHeight());
        final String strImage = base64Encode(image);

        final LabeledImageServer bgLabelServer = new LabeledImageServer.Builder(imageData)
                .backgroundLabel(0).addLabel("Background", 1).multichannelOutput(false).build();
        final BufferedImage bgImage = readRegionFromServer(bgLabelServer, 1.0, 0, 0,
                imageData.getServer().getWidth(), imageData.getServer().getHeight());
        final LabeledOffsetImageServer fgLabelServer = new LabeledOffsetImageServer.Builder(imageData)
                .useFilter(pathObject -> pathObject
                        .getPathClass() == PathClass.getInstance("Foreground"))
                .useInstanceLabels()
                .offset(1).build();
        final BufferedImage fgImage = readRegionFromServer(fgLabelServer, 1.0, 0, 0,
                imageData.getServer().getWidth(), imageData.getServer().getHeight());
        final ImageCalculator imageCalculator = new ImageCalculator();
        final ImagePlus bgImp = IJTools.convertToUncalibratedImagePlus("Background", bgImage);
        final ImagePlus fgImp = IJTools.convertToUncalibratedImagePlus("Foreground", fgImage);
        final BufferedImage lblImage = imageCalculator.run("Max", bgImp, fgImp).getBufferedImage();
        final String strLabel = base64Encode(lblImage);
        final Gson gson = GsonTools.getInstance();
        final CellsparseBody body = CellsparseBody.newBuilder("default").b64img(strImage).b64lbl(strLabel).train(train)
                .eval(true).epochs(epochs).batchsize(batchsize).steps(steps).build();
        final String bodyJson = gson.toJson(body);
        final Type type = new com.google.gson.reflect.TypeToken<List<PathObject>>() {
        }.getType();
        try {
            HttpResponse<String> response = CellsparseTask.sendRequest(endpointURL, bodyJson);
            if (response.statusCode() == HttpURLConnection.HTTP_OK) {
                return gson.fromJson(response.body(), type);
            } else {
                logger.warn(String.format("HTTP error: %d\n%s", response.statusCode(), response.body()));
                return Collections.emptyList();
            }
        } catch (IOException | InterruptedException e) {
            logger.warn("Interrupted while sending request to server", e);
            return Collections.emptyList();
        }
    }

    /**
     * New builder for a CellsparseTask class.
     * 
     * @param viewer
     *            the viewer containing the image to be processed
     * @return the builder
     */
    public static Builder builder(QuPathViewer viewer) {
        return new Builder(viewer);
    }

    /**
     * Builder for a CellsparseTask class.
     */
    public static class Builder {

        private QuPathViewer viewer;

        private String endpointURL;
        private boolean train;
        private int epochs;
        private int batchsize;
        private int steps;

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
         * Specify whether to train the model (required).
         * 
         * @param train
         * @return this builder
         */
        public Builder train(final boolean train) {
            this.train = train;
            return this;
        }

        /**
         * Specify the number of epochs (required).
         * 
         * @param epochs
         * @return this builder
         */
        public Builder epochs(final int epochs) {
            this.epochs = epochs;
            return this;
        }

        /**
         * Specify the batch size (required).
         * 
         * @param batchsize
         * @return this builder
         */
        public Builder batchsize(final int batchsize) {
            this.batchsize = batchsize;
            return this;
        }

        /**
         * Specify the number of steps (required).
         * 
         * @param steps
         * @return this builder
         */
        public Builder steps(final int steps) {
            this.steps = steps;
            return this;
        }

        /**
         * Build the detection task.
         * 
         * @return
         */
        public CellsparseTask build() {
            return new CellsparseTask(this);
        }

    }

}
