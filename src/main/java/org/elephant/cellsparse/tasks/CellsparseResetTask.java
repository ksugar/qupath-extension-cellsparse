package org.elephant.cellsparse.tasks;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.http.HttpResponse;
import org.elephant.cellsparse.models.CellsparseModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CellsparseResetTask extends CellsparseTask<Boolean> {

    private static final Logger logger = LoggerFactory.getLogger(CellsparseResetTask.class);

    private final String endpointURL;
    private final CellsparseModel model;

    public CellsparseResetTask(Builder builder) {
        this.endpointURL = builder.endpointURL;
        this.model = builder.model;
    }

    @Override
    protected Boolean call() throws Exception {
        final String bodyJson = model.getRequestBodyStringReset();
        try {
            HttpResponse<String> response = CellsparseResetTask.sendRequest(endpointURL, bodyJson);
            if (response.statusCode() == HttpURLConnection.HTTP_OK) {
                return true;
            } else {
                final String message = String.format("HTTP error: %d\n%s", response.statusCode(), response.body());
                logger.warn(message);
                throw new IOException(message);
            }
        } catch (IOException | InterruptedException e) {
            final String message = "Interrupted while sending request to server";
            logger.debug(message, e);
            throw new IOException(message, e);
        }
    }

    /**
     * New builder for a CellsparseTrainTask class.
     * 
     * @param viewer
     *               the viewer containing the image to be processed
     * @return the builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for a CellsparseTrainTask class.
     */
    public static class Builder {

        private String endpointURL;
        private CellsparseModel model;

        private Builder() {
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
         * Build the detection task.
         * 
         * @return
         */
        public CellsparseResetTask build() {
            return new CellsparseResetTask(this);
        }

    }

}
