package org.elephant.cellsparse.lib.http;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.CompletableFuture;

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

    /**
     * Sends a POST request to the specified server URL with the given body.
     *
     * @param serverURL The server URL to send the request to.
     * @param body      The body of the request.
     * @return The response from the server.
     * @throws IOException          If an error occurs during the request.
     * @throws InterruptedException If the request is interrupted.
     */
    public static HttpResponse<String> sendRequest(String serverURL, String body)
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

    /**
     * Sends a multipart/form-data POST request to the specified server URL with the
     * given body.
     *
     * @param serverURL            The server URL to send the request to.
     * @param multipartBodyBuilder The multipart/form-data body builder.
     * @return The response from the server.
     * @throws IOException          If an error occurs during the request.
     * @throws InterruptedException If the request is interrupted.
     */
    public static HttpResponse<String> sendMultipartRequest(String serverURL, MultipartBodyBuilder multipartBodyBuilder)
            throws IOException, InterruptedException {
        final HttpRequest request = HttpRequest.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .uri(URI.create(serverURL))
                .header("Content-Type", "multipart/form-data; boundary=" + multipartBodyBuilder.getBoundary())
                .POST(HttpRequest.BodyPublishers.ofByteArray(multipartBodyBuilder.build()))
                .build();
        HttpClient client = HttpClient.newHttpClient();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    /**
     * Sends an asynchronous POST request to the specified server URL with the given
     * body.
     *
     * @param serverURL The server URL to send the request to.
     * @param body      The body of the request.
     * @return A CompletableFuture containing the response from the server.
     */
    public static CompletableFuture<HttpResponse<String>> sendAsyncRequest(String serverURL, String body) {
        final HttpRequest request = HttpRequest.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .uri(URI.create(serverURL))
                .header("accept", "application/json")
                .header("Content-Type", "application/json; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpClient client = HttpClient.newHttpClient();
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    }

    /**
     * Sends an asynchronous multipart/form-data POST request to the specified
     * server
     * URL with the given body.
     *
     * @param serverURL            The server URL to send the request to.
     * @param multipartBodyBuilder The multipart/form-data body builder.
     * @return A CompletableFuture containing the response from the server.
     * @throws IOException          If an error occurs during the request.
     * @throws InterruptedException If the request is interrupted.
     */
    public static CompletableFuture<HttpResponse<String>> sendAsyncMultipartRequest(String serverURL,
            MultipartBodyBuilder multipartBodyBuilder)
            throws IOException, InterruptedException {
        final HttpRequest request = HttpRequest.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .uri(URI.create(serverURL))
                .header("Content-Type", "multipart/form-data; boundary=" + multipartBodyBuilder.getBoundary())
                .POST(HttpRequest.BodyPublishers.ofByteArray(multipartBodyBuilder.build()))
                .build();
        HttpClient client = HttpClient.newHttpClient();
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    }

    /**
     * Cancels the given CompletableFuture if it is not already completed.
     *
     * @param future The CompletableFuture to cancel.
     */
    public static void cancelRequest(CompletableFuture<HttpResponse<String>> future) {
        if (future != null && !future.isDone()) {
            future.cancel(true);
        }
    }

    /**
     * Sends a cancel request to the specified server URL with the given request ID.
     *
     * @param serverURL The server URL to send the request to.
     * @param requestId The request ID to cancel.
     * @throws IOException          If an error occurs during the request.
     * @throws InterruptedException If the request is interrupted.
     */
    public static void sendCancelRequest(String serverURL, String requestId) throws IOException, InterruptedException {
        final HttpRequest request = HttpRequest.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .uri(URI.create(serverURL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"requestId\": \"" + requestId + "\"}"))
                .build();
        HttpClient client = HttpClient.newHttpClient();
        client.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
