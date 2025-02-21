package org.elephant.cellsparse.tasks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.elephant.cellsparse.CellsparseCommand;
import org.elephant.cellsparse.lib.gui.viewer.TileProvider;
import org.elephant.cellsparse.models.CellsparseModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;
import qupath.lib.gui.viewer.QuPathViewer;
import qupath.lib.images.servers.TileRequest;
import qupath.lib.objects.PathObject;
import qupath.lib.regions.RegionRequest;

public class CellsparseInferTask extends CellsparseTask<List<PathObject>> {

    private static final Logger logger = LoggerFactory.getLogger(CellsparseInferTask.class);

    private final CellsparseCommand command;
    private final QuPathViewer viewer;
    private final String endpointURL;
    private final CellsparseModel model;
    private final Collection<TileRequest> tiles;
    private final double downsample;
    private final int pad;
    private final int maxThreads;

    public CellsparseInferTask(Builder builder) {
        this.viewer = builder.viewer;
        Objects.requireNonNull(viewer, "Viewer must not be null!");
        this.command = builder.command;
        this.endpointURL = builder.endpointURL;
        this.model = builder.model;
        this.model.getParameterList();
        this.maxThreads = builder.maxThreads;
        this.tiles = builder.tileProvider.getTiles();
        this.downsample = builder.tileProvider.getDownsample();
        this.pad = builder.tileProvider.getPad();
    }

    @Override
    protected List<PathObject> call() throws Exception {
        final List<PathObject> detections = Collections.synchronizedList(new ArrayList<>());
        List<Future<?>> futures = new ArrayList<>();
        int count = 0;
        AtomicInteger finishedCount = new AtomicInteger(0);
        final int numTiles = tiles.size();
        for (TileRequest tile : tiles) {
            logger.debug("Processing index {} / {}", ++count, numTiles);
            var request = tile.getRegionRequest();
            var server = viewer.getServer();
            int x1 = (int) Math.max(0, Math.round(request.getX() - downsample * pad));
            int y1 = (int) Math.max(0, Math.round(request.getY() - downsample * pad));
            int x2 = (int) Math.min(server.getWidth(), Math.round(request.getMaxX() + downsample * pad));
            int y2 = (int) Math.min(server.getHeight(), Math.round(request.getMaxY() + downsample * pad));
            RegionRequest requestPadded = RegionRequest.createInstance(server.getPath(), downsample, x1, y1,
                    x2 - x1,
                    y2 - y1, request.getZ(), request.getT());

            CellsparseInferSubTask task = CellsparseInferSubTask.builder(viewer)
                    .endpointURL(endpointURL)
                    .model(model)
                    .regionRequest(requestPadded)
                    .build();
            task.setOnSucceeded(event -> {
                final List<PathObject> detected = task.getValue();
                if (detected != null) {
                    if (!detected.isEmpty()) {
                        detections.addAll(detected);
                    } else {
                        logger.info("No objects detected");
                    }
                } else {
                    logger.info("No objects detected");
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
                command.updateInfoText("Task is cancelled");
            });

            futures.add(CellsparseTaskUtils.submitTask(command, task));

            if (futures.size() >= maxThreads) {
                try {
                    futures.get(0).get();
                    futures.remove(0);
                    Platform.runLater(() -> {
                        command.getProgressProperty()
                                .set((double) finishedCount.getAndIncrement() / numTiles);
                        final String progressString = "Processing " + finishedCount.get() + " / " + numTiles;
                        command.updateInfoText(progressString);
                        logger.debug(progressString);
                    });
                } catch (CancellationException e) {
                    logger.debug("Task is cancelled", e);
                    return null;
                } catch (ExecutionException e) {
                    logger.warn("Error while waiting for task to complete", e);
                    return null;
                } catch (InterruptedException e) {
                    logger.debug("Task is interrupted", e);
                    return null;
                }
            }
        }
        for (Future<?> future : futures) {
            try {
                future.get();
                Platform.runLater(() -> {
                    command.getProgressProperty()
                            .set((double) finishedCount.getAndIncrement() / numTiles);
                    final String progressString = "Processing " + finishedCount.get() + " / " + numTiles;
                    command.updateInfoText(progressString);
                    logger.debug(progressString);
                });
            } catch (CancellationException e) {
                logger.warn("Task is cancelled", e);
                return Collections.emptyList();
            } catch (ExecutionException e) {
                logger.warn("Error while waiting for task to complete", e);
                return Collections.emptyList();
            } catch (InterruptedException e) {
                logger.warn("Task is interrupted", e);
                return Collections.emptyList();
            }
        }

        return detections;
    }

    /**
     * New builder for a CellsparseInferTask class.
     * 
     * @param viewer
     *               the viewer containing the image to be processed
     * @return the builder
     */
    public static Builder builder(QuPathViewer viewer, CellsparseCommand command) {
        return new Builder(viewer, command);
    }

    /**
     * Builder for a CellsparseInferTask class.
     */
    public static class Builder {
        private static final int DEFAULT_MAX_THREADS = 4;

        private final QuPathViewer viewer;
        private final CellsparseCommand command;

        private String endpointURL;
        private CellsparseModel model;
        private int maxThreads = DEFAULT_MAX_THREADS;
        private TileProvider tileProvider;

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

        public Builder maxThreads(int maxThreads) {
            this.maxThreads = maxThreads;
            return this;
        }

        public Builder tileProvider(TileProvider tileProvider) {
            this.tileProvider = tileProvider;
            return this;
        }

        /**
         * Build the detection task.
         * 
         * @return
         */
        public CellsparseInferTask build() {
            return new CellsparseInferTask(this);
        }

    }

}
