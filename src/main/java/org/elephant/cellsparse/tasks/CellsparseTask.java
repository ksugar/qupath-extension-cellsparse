package org.elephant.cellsparse.tasks;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.List;
import javax.imageio.ImageIO;

import javafx.concurrent.Task;
import qupath.lib.gui.dialogs.Dialogs;
import qupath.lib.images.servers.ImageServer;
import qupath.lib.objects.PathObject;

public abstract class CellsparseTask extends Task<List<PathObject>> {

    String base64Encode(final BufferedImage bufferedImage) {
        String base64Image = null;
        try {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage, "tif", baos);
            final byte[] bytes = baos.toByteArray();
            base64Image = Base64.getEncoder().encodeToString(bytes);
        } catch (IOException e) {
            Dialogs.showErrorMessage(getClass().getName(), e);
        }
        return base64Image;
    }

    BufferedImage readRegionFromServer(final ImageServer<BufferedImage> imageServer,
            final double downsample, final int x, final int y, final int width, final int height) {
        BufferedImage image = null;
        try {
            image = imageServer.readRegion(downsample, x, y, width, height);
        } catch (IOException e) {
            Dialogs.showErrorMessage(getClass().getName(), e);
        }
        return image;
    }

    static HttpResponse<String> sendRequest(String serverURL, String body)
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
    protected abstract List<PathObject> call() throws Exception;
}
