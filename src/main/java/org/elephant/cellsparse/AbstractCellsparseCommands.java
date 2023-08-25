package org.elephant.cellsparse;

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
import java.util.List;

import javax.imageio.ImageIO;

import com.google.gson.Gson;

import ij.ImagePlus;
import ij.plugin.ImageCalculator;
import qupath.imagej.tools.IJTools;
import qupath.lib.gui.dialogs.Dialogs;
import qupath.lib.images.ImageData;
import qupath.lib.images.servers.ImageServer;
import qupath.lib.images.servers.LabeledImageServer;
import qupath.lib.images.servers.LabeledOffsetImageServer;
import qupath.lib.io.GsonTools;
import qupath.lib.objects.PathObject;
import qupath.lib.objects.classes.PathClass;

public abstract class AbstractCellsparseCommands {

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

	void CellsparseCommand(final ImageData<BufferedImage> imageData, final String endpointURL,
			final boolean train) {
		CellsparseCommand(imageData, endpointURL, train, 1, 8, 200);
	}

	void CellsparseCommand(final ImageData<BufferedImage> imageData, final String endpointURL,
			final boolean train, final int epochs, final int batchsize, final int steps) {
		final BufferedImage image = readRegionFromServer(imageData.getServer(), 1.0, 0, 0,
				imageData.getServer().getWidth(), imageData.getServer().getHeight());
		final String strImage = base64Encode(image);

		final LabeledImageServer bgLabelServer = new LabeledImageServer.Builder(imageData)
				.backgroundLabel(0).addLabel("Background", 1).multichannelOutput(false).build();
		final BufferedImage bgImage = readRegionFromServer(bgLabelServer, 1.0, 0, 0,
				imageData.getServer().getWidth(), imageData.getServer().getHeight());
		final LabeledOffsetImageServer fgLabelServer =
				new LabeledOffsetImageServer.Builder(imageData).useFilter(pathObject -> pathObject
						.getPathClass() == PathClass.getInstance("Foreground")).useInstanceLabels()
						.offset(1).build();
		final BufferedImage fgImage = readRegionFromServer(fgLabelServer, 1.0, 0, 0,
				imageData.getServer().getWidth(), imageData.getServer().getHeight());
		final ImageCalculator imageCalculator = new ImageCalculator();
		final ImagePlus bgImp = IJTools.convertToUncalibratedImagePlus("Background", bgImage);
		final ImagePlus fgImp = IJTools.convertToUncalibratedImagePlus("Foreground", fgImage);
		final BufferedImage lblImage = imageCalculator.run("Max", bgImp, fgImp).getBufferedImage();
		final String strLabel = base64Encode(lblImage);
		final Gson gson = GsonTools.getInstance();
		final CellsparseBody body =
				CellsparseBody.newBuilder("default").b64img(strImage).b64lbl(strLabel).train(train)
						.eval(true).epochs(epochs).batchsize(batchsize).steps(steps).build();
		final String bodyJson = gson.toJson(body);

		final HttpRequest request = HttpRequest.newBuilder().version(HttpClient.Version.HTTP_1_1)
				.uri(URI.create(endpointURL)).header("accept", "application/json")
				.header("Content-Type", "application/json; charset=utf-8")
				.POST(HttpRequest.BodyPublishers.ofString(bodyJson)).build();
		HttpClient client = HttpClient.newHttpClient();
		final Type type = new com.google.gson.reflect.TypeToken<List<PathObject>>() {}.getType();
		try {
			HttpResponse<String> response =
					client.send(request, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() == HttpURLConnection.HTTP_OK) {
				List<PathObject> toRomove = imageData.getHierarchy().getAnnotationObjects().stream()
						.filter(pathObject -> pathObject.getPathClass() == null).toList();
				imageData.getHierarchy().removeObjects(toRomove, false);
				List<PathObject> pathObjects = gson.fromJson(response.body(), type);
				imageData.getHierarchy().addObjects(pathObjects);
			} else {
				Dialogs.showErrorMessage("Http error: " + response.statusCode(), response.body());
			}
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
			Dialogs.showErrorMessage(getClass().getName(), e);
		}
	}

	void CellsparseResetCommand(final String endpointURL) {
		final Gson gson = GsonTools.getInstance();
		final CellsparseResetBody body = CellsparseResetBody.newBuilder("default").build();
		final String bodyJson = gson.toJson(body);

		final HttpRequest request = HttpRequest.newBuilder().version(HttpClient.Version.HTTP_1_1)
				.uri(URI.create(endpointURL)).header("accept", "application/json")
				.header("Content-Type", "application/json; charset=utf-8")
				.POST(HttpRequest.BodyPublishers.ofString(bodyJson)).build();
		HttpClient client = HttpClient.newHttpClient();
		try {
			HttpResponse<String> response =
					client.send(request, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() == HttpURLConnection.HTTP_OK) {
				Dialogs.showMessageDialog("Model reset", "Model is reset");
			} else {
				Dialogs.showErrorMessage("Http error: " + response.statusCode(), response.body());
			}
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
			Dialogs.showErrorMessage(getClass().getName(), e);
		}
	}

}
