package org.elephant.cellsparse.ui;

import org.elephant.cellsparse.CellsparseCommand;
import org.elephant.cellsparse.lib.gui.viewer.SelectedObjectsRegionFilter;
import org.elephant.cellsparse.models.CellposeModel;
import org.elephant.cellsparse.models.CellsparseModel;
import org.elephant.cellsparse.models.ElephantModel;
import org.elephant.cellsparse.models.StarDistModel;
import org.elephant.cellsparse.tasks.CellsparseInferTask;
import org.elephant.cellsparse.tasks.CellsparseResetTask;
import org.elephant.cellsparse.tasks.CellsparseTrainTask;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;
import org.locationtech.jts.index.strtree.STRtree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.stream.IntStream;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashSet;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Separator;
import javafx.scene.input.MouseEvent;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.text.TextAlignment;
import qupath.lib.common.GeneralTools;
import qupath.lib.gui.QuPathGUI;
import qupath.lib.gui.dialogs.Dialogs;
import qupath.lib.gui.tools.PaneTools;
import qupath.lib.gui.viewer.RegionFilter;
import qupath.lib.images.ImageData;
import qupath.lib.images.servers.ColorTransforms.ColorTransform;
import qupath.lib.images.servers.ColorTransforms;
import qupath.lib.images.servers.ImageServer;
import qupath.lib.images.servers.PixelCalibration;
import qupath.lib.images.servers.TileRequest;
import qupath.lib.objects.PathObject;
import qupath.lib.objects.PathObjects;
import qupath.lib.objects.hierarchy.PathObjectHierarchy;
import qupath.lib.regions.RegionRequest;
import qupath.lib.roi.GeometryTools;
import qupath.lib.roi.RoiTools;
import qupath.lib.roi.interfaces.ROI;
import qupath.opencv.ops.ImageDataOp;
import qupath.opencv.ops.ImageDataServer;
import qupath.opencv.ops.ImageOps;

public class CellsparsePane extends GridPane {

    private static final Logger logger = LoggerFactory.getLogger(CellsparsePane.class);

    /**
     * Default tile width and height.
     */
    public static int defaultTileSize = 1024;

    /**
     * Default pad size.
     */
    public static int defaultPad = 32;

    private final CellsparseCommand command;
    private final QuPathGUI qupath;

    private ObservableList<CellsparseResolution> resolutions = FXCollections.observableArrayList();
    private ComboBox<CellsparseResolution> comboResolutions = new ComboBox<>(resolutions);
    private ReadOnlyObjectProperty<CellsparseResolution> selectedResolution;
    private ReadOnlyObjectProperty<RegionFilter> selectedRegionFilter;
    private SimpleBooleanProperty keepExistingProperty = new SimpleBooleanProperty(true);
    private SimpleIntegerProperty tileWidthProperty = new SimpleIntegerProperty(defaultTileSize);
    private SimpleIntegerProperty tileHeightProperty = new SimpleIntegerProperty(defaultTileSize);
    private SimpleIntegerProperty padProperty = new SimpleIntegerProperty(defaultPad);

    private ChangeListener<ImageData<BufferedImage>> imageDataListener = new ChangeListener<ImageData<BufferedImage>>() {

        @Override
        public void changed(ObservableValue<? extends ImageData<BufferedImage>> observable,
                ImageData<BufferedImage> oldValue, ImageData<BufferedImage> newValue) {
            updateAvailableResolutions(newValue);
        }

    };

    /**
     * Create a new main pane for the Cellsparse command.
     * 
     * @param command
     *                The Cellsparse command.
     */
    public CellsparsePane(CellsparseCommand command, final QuPathGUI qupath) {
        super();

        this.command = command;
        this.qupath = qupath;

        int row = 0;

        addInstructionPrompt(row++);

        addSeparator(row++);

        addServerPrompt(row++);
        addModelPrompt(row++);
        addImageResolution(row++);
        addRegion(row++);

        addSeparator(row++);

        addCheckboxes(row++);

        addNumbers(row++);

        addIOButtons(row++);
        addCommandButtons(row++);

        addSeparator(row++);

        addInfoPane(row++);
        addEventFilter(MouseEvent.MOUSE_MOVED, this::handleMouseMoved);

        setHgap(CellsparseUIUtils.H_GAP);
        setVgap(CellsparseUIUtils.V_GAP);
        setPadding(new Insets(10.0));
        setMaxWidth(Double.MAX_VALUE);

        for (int i = 0; i < getColumnCount(); i++) {
            ColumnConstraints constraints = new ColumnConstraints();
            if (i == 1)
                constraints.setHgrow(Priority.ALWAYS);
            getColumnConstraints().add(constraints);
        }

        qupath.imageDataProperty().addListener(imageDataListener);
    }

    /**
     * Update the available resolutions for the specified ImageData.
     * 
     * @param imageData
     */
    private void updateAvailableResolutions(ImageData<BufferedImage> imageData) {
        var selected = selectedResolution.get();
        if (imageData == null) {
            return;
        }
        var requestedResolutions = CellsparseResolution.getDefaultResolutions(imageData, selected);
        if (!resolutions.equals(requestedResolutions)) {
            resolutions.setAll(CellsparseResolution.getDefaultResolutions(imageData, selected));
            comboResolutions.getSelectionModel().select(selected);
        }
    }

    private void handleMouseMoved(MouseEvent event) {
        Node node = event.getPickResult().getIntersectedNode();
        while (node != null) {
            if (node instanceof Control) {
                Tooltip tooltip = ((Control) node).getTooltip();
                if (tooltip != null) {
                    command.updateInfoText(tooltip.getText());
                    return;
                }
            }
            node = node.getParent();
        }
        // Reset the info text, unless it shows an error
        command.updateInfoText("");
    }

    private void addInstructionPrompt(int row) {
        ProgressIndicator progressIndicator = new ProgressIndicator();
        progressIndicator.setPrefSize(32, 32);
        progressIndicator.progressProperty().bind(command.getProgressProperty());
        command.getProgressProperty().set(ProgressIndicator.INDETERMINATE_PROGRESS);
        progressIndicator.visibleProperty().bind(command.getIsTaskRunning());

        Label label = new Label("Cellsparse");
        label.setWrapText(true);
        label.setAlignment(Pos.CENTER);
        label.setTextAlignment(TextAlignment.CENTER);

        BorderPane instructionPane = new BorderPane(label);
        instructionPane.setRight(progressIndicator);
        add(instructionPane, 0, row, GridPane.REMAINING, 1);
    }

    private void addSeparator(int row) {
        Separator separator = new Separator();
        separator.setPadding(new Insets(5.0));
        add(separator, 0, row, GridPane.REMAINING, 1);
    }

    private void addServerPrompt(int row) {
        Label labelUrl = new Label();
        labelUrl.textProperty().bind(command.getServerURLProperty());
        labelUrl.setMaxWidth(Double.MAX_VALUE);
        labelUrl.setTextOverrun(OverrunStyle.LEADING_ELLIPSIS);
        GridPane.setFillWidth(labelUrl, true);
        GridPane.setHgrow(labelUrl, Priority.ALWAYS);
        Tooltip tooltip = new Tooltip("The server running the Cellsparse API.\n" +
                "This can be a local server or a remote server.");
        labelUrl.setTooltip(tooltip);

        Button btnEdit = new Button("Edit");
        btnEdit.setMaxWidth(Double.MAX_VALUE);
        btnEdit.setOnAction(event -> promptToSetUrl());
        btnEdit.setTooltip(new Tooltip("Edit the server URL"));

        Label label = new Label("Server");
        label.setTooltip(tooltip);
        label.setLabelFor(labelUrl);
        add(label, 0, row);
        add(labelUrl, 1, row);
        add(btnEdit, 2, row);
    }

    private void promptToSetUrl() {
        String currentURL = command.getServerURLProperty().get();
        TextInputDialog dialog = new TextInputDialog(currentURL);
        dialog.setHeaderText("Input Cellsparse server URL");
        dialog.getEditor().setPrefColumnCount(32);
        String newURL = dialog.showAndWait().orElse(currentURL);
        if (newURL == null || newURL.isBlank() || newURL.equals(currentURL))
            return;
        command.getServerURLProperty().set(newURL);
    }

    private void addModelPrompt(int row) {
        final ComboBox<CellsparseModel> combo = new ComboBox<>();
        combo.getItems().addAll(
                new StarDistModel(),
                new CellposeModel(),
                new ElephantModel());

        combo.getSelectionModel().select(command.getModelIndexProperty().get());
        command.getModelIndexProperty().bind(combo.getSelectionModel().selectedIndexProperty());
        command.getModelProperty().bind(combo.valueProperty());
        combo.setMaxWidth(Double.MAX_VALUE);
        Tooltip tooltip = new Tooltip("The Cellsparse model to use.");
        combo.setTooltip(tooltip);
        GridPane.setFillWidth(combo, true);

        Label label = new Label("Cellsparse model");
        label.setLabelFor(combo);
        label.setTooltip(tooltip);

        Button btnEditModel = new Button("Edit");
        btnEditModel.setOnAction(e -> {
            CellsparseModel model = combo.getSelectionModel().selectedItemProperty().get();
            if (model == null) {
                Dialogs.showErrorMessage("Edit parameters", "No model selected!");
            }
            Dialogs.showConfirmDialog("Edit parameters", model.getParamterPane());
        });
        btnEditModel.disableProperty().bind(combo.getSelectionModel().selectedItemProperty().isNull());

        add(label, 0, row);
        add(combo, 1, row);
        add(btnEditModel, 2, row, GridPane.REMAINING, 1);
    }

    private void addImageResolution(int row) {
        var labelResolution = new Label("Resolution");
        labelResolution.setLabelFor(comboResolutions);
        var btnResolution = new Button("Add");
        btnResolution.setOnAction(e -> addResolution());
        selectedResolution = comboResolutions.getSelectionModel().selectedItemProperty();

        updateAvailableResolutions(qupath.getImageData());
        if (!comboResolutions.getItems().isEmpty())
            comboResolutions.getSelectionModel().clearAndSelect(resolutions.size() / 2);

        add(labelResolution, 0, row);
        add(comboResolutions, 1, row);
        add(btnResolution, 2, row, GridPane.REMAINING, 1);
    }

    private boolean addResolution() {
        var imageData = qupath.getImageData();
        ImageServer<BufferedImage> server = imageData == null ? null : imageData.getServer();
        if (server == null) {
            Dialogs.showNoImageError("Add resolution");
            return false;
        }
        String units = null;
        Double pixelSize = null;
        PixelCalibration cal = server.getPixelCalibration();
        if (cal.hasPixelSizeMicrons()) {
            pixelSize = Dialogs.showInputDialog("Add resolution",
                    "Enter requested pixel size in " + GeneralTools.micrometerSymbol(), 1.0);
            units = PixelCalibration.MICROMETER;
        } else {
            pixelSize = Dialogs.showInputDialog("Add resolution", "Enter requested downsample factor", 1.0);
        }

        if (pixelSize == null)
            return false;

        CellsparseResolution res;
        if (PixelCalibration.MICROMETER.equals(units)) {
            double scale = pixelSize / cal.getAveragedPixelSizeMicrons();
            res = new CellsparseResolution("Custom", cal.createScaledInstance(scale, scale, 1));
        } else
            res = new CellsparseResolution("Custom", cal.createScaledInstance(pixelSize, pixelSize, 1));

        List<CellsparseResolution> temp = new ArrayList<>(resolutions);
        temp.add(res);
        Collections.sort(temp,
                Comparator.comparingDouble(
                        (CellsparseResolution w) -> w.getPixelCalibration().getAveragedPixelSize().doubleValue()));
        resolutions.setAll(temp);
        comboResolutions.getSelectionModel().select(res);

        return true;
    }

    private void addRegion(int row) {
        var labelRegion = new Label("Region");
        var comboRegionFilter = createRegionFilterCombo();
        selectedRegionFilter = comboRegionFilter.getSelectionModel().selectedItemProperty();

        add(labelRegion, 0, row);
        add(comboRegionFilter, 1, row, GridPane.REMAINING, 1);
    }

    /**
     * Create a {@link ComboBox} that can be used to select the region filter for
     * selected objects.
     * 
     * @param options
     * @return
     */
    private static ComboBox<RegionFilter> createRegionFilterCombo() {
        var comboRegion = new ComboBox<RegionFilter>();
        comboRegion.getItems().addAll(SelectedObjectsRegionFilter.values());
        comboRegion.getSelectionModel().select(SelectedObjectsRegionFilter.EVERYWHERE);
        comboRegion.setMaxWidth(Double.MAX_VALUE);
        comboRegion.setTooltip(new Tooltip("Control where the detection is applied.\n"
                + "Warning! Runnning detection for the entire image at high resolution can be very slow and require a lot of memory."));
        return comboRegion;
    }

    private void addCheckboxes(int row) {
        var keepExisting = new CheckBox("Keep existing annotations");
        keepExisting.setTooltip(new Tooltip("Keep existing annotations when running inference"));
        keepExisting.selectedProperty().bindBidirectional(keepExistingProperty);
        add(keepExisting, 0, row);
    }

    private void addNumbers(int row) {
        var tileWidthLabel = new Label("Tile width");
        StackPane tileWidthLabelContainer = new StackPane(tileWidthLabel);
        tileWidthLabelContainer.setAlignment(Pos.CENTER);
        var tileWidthSpinner = CellsparseUIUtils.createIntegerSpinner(0, Integer.MAX_VALUE, tileWidthProperty, 1,
                "Width of a tile used for detection");
        var tileHeightLabel = new Label("Tile height");
        StackPane tileHeightLabelContainer = new StackPane(tileHeightLabel);
        tileHeightLabelContainer.setAlignment(Pos.CENTER);
        var tileHeightSpinner = CellsparseUIUtils.createIntegerSpinner(0, Integer.MAX_VALUE, tileHeightProperty, 1,
                "Height of a tile used for detection");
        var paneSpinners = PaneTools.createColumnGridControls(
                tileWidthLabelContainer, tileWidthSpinner,
                tileHeightLabelContainer, tileHeightSpinner);
        paneSpinners.setHgap(10);
        add(paneSpinners, 0, row, GridPane.REMAINING, 1);
    }

    private void addIOButtons(int row) {
        // Reset model
        var btnReset = new Button("Reset model");
        btnReset.setTooltip(
                new Tooltip("Reset a previously trained model, or load a new model from a file"));
        btnReset.setOnAction(e -> {
            submitResetTask();
        });
        btnReset.disableProperty().bind(qupath.projectProperty().isNull());

        // Save model
        var btnSave = new Button("Save model");
        btnSave.setTooltip(new Tooltip("Save a previously trained model"));
        btnSave.setOnAction(e -> {
            // TODO: Save model
            logger.debug("Save model");
        });
        btnSave.disableProperty().bind(qupath.projectProperty().isNull());

        var paneIO = PaneTools.createColumnGridControls(btnReset, btnSave);
        add(paneIO, 0, row, GridPane.REMAINING, 1);
    }

    private void addCommandButtons(int row) {
        // Train
        var btnTrain = new Button("Train");
        btnTrain.setTooltip(new Tooltip("Train a model"));
        btnTrain.setOnAction(e -> {
            submitTrainTask();
            logger.debug("Train model");
        });
        btnTrain.disableProperty().bind(qupath.projectProperty().isNull());

        // Inference
        var btnInfer = new Button("Infer");
        btnInfer.setTooltip(new Tooltip("Run inference"));
        btnInfer.setOnAction(e -> {
            submitInferTask();
            logger.debug("Run inference");
        });
        btnInfer.disableProperty().bind(qupath.projectProperty().isNull());

        var paneCommand = PaneTools.createColumnGridControls(btnTrain, btnInfer);
        add(paneCommand, 0, row, GridPane.REMAINING, 1);
    }

    private void addInfoPane(int row) {
        Label labelInfo = new Label();
        labelInfo.setMaxWidth(Double.MAX_VALUE);
        labelInfo.setWrapText(true);
        labelInfo.setAlignment(Pos.CENTER);
        labelInfo.setTextAlignment(TextAlignment.CENTER);
        labelInfo.textProperty().bind(command.getInfoTextProperty());
        labelInfo.setTextOverrun(OverrunStyle.ELLIPSIS);
        labelInfo.setPrefHeight(64);
        labelInfo.styleProperty().bind(Bindings.createStringBinding(() -> {
            if (command.getInfoTextErrorTimestampProperty().get() > 0)
                return "-fx-text-fill: -qp-script-error-color;";
            else
                return null;
        }, command.getInfoTextErrorTimestampProperty()));
        GridPane.setFillWidth(labelInfo, true);
        add(labelInfo, 0, row, GridPane.REMAINING, 1);
    }

    /**
     * Handle a change in task state.
     * 
     * @param task
     * @param newValue
     */
    private void taskStateChange(Task<?> task, Worker.State newValue) {
        switch (newValue) {
            case SUCCEEDED:
                logger.debug("Task completed successfully");
                command.getCurrentTasks().remove(task);
                break;
            case CANCELLED:
                logger.info("Task cancelled");
                command.getCurrentTasks().remove(task);
                if (task.getException() != null) {
                    logger.warn("Task failed: {}", task, task.getException());
                    command.updateInfoTextWithError("Task cancelled with exception " + task.getException() +
                            "\nSee log for details.");
                } else {
                    command.updateInfoText("Task cancelled");
                }
                break;
            case FAILED:
                command.getCurrentTasks().remove(task);
                if (task.getException() != null) {
                    logger.warn("Task failed: {}", task, task.getException());
                    command.updateInfoTextWithError("Task failed with exception " + task.getException() +
                            "\nSee log for details.");
                } else {
                    command.updateInfoTextWithError("Task failed!");
                }
                break;
            case RUNNING:
                logger.trace("Task running");
                break;
            case SCHEDULED:
                logger.trace("Task scheduled");
                break;
            default:
                logger.debug("Task state changed to {}", newValue);
        }
    }

    /**
     * Submit a task.
     * 
     * @param task the task to submit
     * @return a Future representing pending completion of the task
     */
    private Future<?> submitTask(Task<?> task) {
        task.setOnFailed(event -> {
            Platform.runLater(() -> {
                Dialogs.showErrorMessage("Connection failed",
                        "Please check that the samapi server (v0.4 and above) is running and the URL is correct.");
            });
        });
        command.getCurrentTasks().add(task);
        task.stateProperty().addListener((observable, oldValue, newValue) -> taskStateChange(task, newValue));
        return command.getPool().submit(task);
    }

    /**
     * Submit a task to run training.
     * 
     */
    private void submitTrainTask() {
        logger.info("Submitting task for training");
        CellsparseModel model = command.getModelProperty().get();
        if (model == null) {
            Dialogs.showErrorMessage("submitTrainTask", "No model selected!");
        }
        String url = null;
        try {
            url = new URI(command.getServerURLProperty().get())
                    .resolve(model.getEndpoint())
                    .normalize()
                    .toURL()
                    .toString();
        } catch (URISyntaxException | MalformedURLException e) {
            logger.warn("{} is not a valid URL.", command.getServerURLProperty().get());
        }
        if (url == null)
            return;
        if (!url.endsWith("/")) {
            url += "/";
        }
        final double downsample = selectedResolution.get().getPixelCalibration().getAveragedPixelSize().doubleValue();
        final PixelCalibration resolution = qupath.getViewer().getServer().getPixelCalibration()
                .createScaledInstance(downsample, downsample);
        final int tw = tileWidthProperty.get();
        final int th = tileHeightProperty.get();
        final int pad = padProperty.get();
        final ColorTransform[] colorTransforms = IntStream.range(0, qupath.getViewer().getServer().nChannels())
                .mapToObj(c -> ColorTransforms.createChannelExtractor(c))
                .toArray(ColorTransform[]::new).clone();
        final ImageDataOp op = ImageOps.buildImageDataOp(colorTransforms);
        final ImageDataServer<BufferedImage> opServer = ImageOps.buildServer(qupath.getViewer().getImageData(), op,
                resolution, tw - pad * 2, th - pad * 2);
        final PathObjectHierarchy hierarchy = qupath.getViewer().getImageData().getHierarchy();
        final Collection<PathObject> selectedAnnotations = hierarchy.getSelectionModel().getSelectedObjects();
        final RegionFilter regionFilter = selectedRegionFilter.get();
        final ROI union = regionFilter == SelectedObjectsRegionFilter.EVERYWHERE || selectedAnnotations.isEmpty()
                ? null
                : RoiTools.union(selectedAnnotations.stream().map(it -> it.getROI()).collect(Collectors.toList()));

        // Get the RegionRequest with the downsample (and union)
        RegionRequest regionRequest;
        if (union == null) {
            regionRequest = RegionRequest.createInstance(opServer, downsample);
        } else {
            regionRequest = RegionRequest.createInstance(
                    opServer.getPath(),
                    downsample,
                    union);
        }
        Collection<TileRequest> tiles = qupath.getViewer().getServer().getTileRequestManager()
                .getTileRequests(regionRequest);
        tiles = tiles.stream()
                .filter(t -> union == null || union.getGeometry()
                        .intersects(GeometryTools.createRectangle(t.getImageX(), t.getImageY(),
                                t.getImageWidth(), t.getImageHeight())))
                .collect(Collectors.toList());
        List<Future<?>> futures = new ArrayList<>();
        List<RegionRequest> regionRequests = new ArrayList<>();
        for (TileRequest tile : tiles) {
            var request = tile.getRegionRequest();
            var server = qupath.getViewer().getServer();
            int x1 = (int) Math.max(0, Math.round(request.getX() - downsample * pad));
            int y1 = (int) Math.max(0, Math.round(request.getY() - downsample * pad));
            int x2 = (int) Math.min(server.getWidth(), Math.round(request.getMaxX() + downsample * pad));
            int y2 = (int) Math.min(server.getHeight(), Math.round(request.getMaxY() + downsample * pad));
            RegionRequest requestPadded = RegionRequest.createInstance(server.getPath(), downsample, x1, y1, x2 - x1,
                    y2 - y1, request.getZ(), request.getT());
            regionRequests.add(requestPadded);
        }

        CellsparseTrainTask task = CellsparseTrainTask.builder(qupath.getViewer())
                .endpointURL(url.toString())
                .model(model)
                .regionRequests(regionRequests)
                .build();
        task.setOnSucceeded(event -> {
            command.updateInfoText("Training is done");
        });

        futures.add(submitTask(task));

        // Sychronize the results
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (CancellationException e) {
                logger.warn("Task is cancelled", e);
            } catch (ExecutionException e) {
                logger.warn("Error while waiting for task to complete", e);
            } catch (InterruptedException e) {
                logger.warn("Task is interrupted", e);
            }
        }
    }

    /**
     * Submit a task to run inference.
     * 
     */
    private void submitInferTask() {
        logger.info("Submitting task for inference");
        CellsparseModel model = command.getModelProperty().get();
        if (model == null) {
            Dialogs.showErrorMessage("submitInferTask", "No model selected!");
        }
        String url = null;
        try {
            url = new URI(command.getServerURLProperty().get())
                    .resolve(model.getEndpoint())
                    .normalize()
                    .toURL()
                    .toString();
        } catch (URISyntaxException | MalformedURLException e) {
            logger.warn("{} is not a valid URL.", command.getServerURLProperty().get());
        }
        if (url == null)
            return;
        if (!url.endsWith("/")) {
            url += "/";
        }
        final double downsample = selectedResolution.get().getPixelCalibration().getAveragedPixelSize().doubleValue();
        final PixelCalibration resolution = qupath.getViewer().getServer().getPixelCalibration()
                .createScaledInstance(downsample, downsample);
        final int tw = tileWidthProperty.get();
        final int th = tileHeightProperty.get();
        final int pad = padProperty.get();
        final ColorTransform[] colorTransforms = IntStream.range(0, qupath.getViewer().getServer().nChannels())
                .mapToObj(c -> ColorTransforms.createChannelExtractor(c))
                .toArray(ColorTransform[]::new).clone();
        final ImageDataOp op = ImageOps.buildImageDataOp(colorTransforms);
        final ImageDataServer<BufferedImage> opServer = ImageOps.buildServer(qupath.getViewer().getImageData(), op,
                resolution, tw - pad * 2, th - pad * 2);
        final PathObjectHierarchy hierarchy = qupath.getViewer().getImageData().getHierarchy();
        final Collection<PathObject> selectedAnnotations = hierarchy.getSelectionModel().getSelectedObjects();
        final List<PathObject> toRomove = hierarchy.getAnnotationObjects().stream()
                .filter(pathObject -> pathObject.getPathClass() == null).toList();
        final RegionFilter regionFilter = selectedRegionFilter.get();
        final ROI union = regionFilter == SelectedObjectsRegionFilter.EVERYWHERE || selectedAnnotations.isEmpty()
                ? null
                : RoiTools.union(selectedAnnotations.stream().map(it -> it.getROI()).collect(Collectors.toList()));

        // Get the RegionRequest with the downsample (and union)
        RegionRequest regionRequest;
        if (union == null) {
            regionRequest = RegionRequest.createInstance(opServer, downsample);
        } else {
            regionRequest = RegionRequest.createInstance(
                    opServer.getPath(),
                    downsample,
                    union);
        }
        Collection<TileRequest> tiles = qupath.getViewer().getServer().getTileRequestManager()
                .getTileRequests(regionRequest);
        tiles = tiles.stream()
                .filter(t -> union == null || union.getGeometry()
                        .intersects(GeometryTools.createRectangle(t.getImageX(), t.getImageY(),
                                t.getImageWidth(), t.getImageHeight())))
                .collect(Collectors.toList());
        final List<PathObject> detections = Collections.synchronizedList(new ArrayList<>());
        List<Future<?>> futures = new ArrayList<>();
        for (TileRequest tile : tiles) {
            var request = tile.getRegionRequest();
            var server = qupath.getViewer().getServer();
            int x1 = (int) Math.max(0, Math.round(request.getX() - downsample * pad));
            int y1 = (int) Math.max(0, Math.round(request.getY() - downsample * pad));
            int x2 = (int) Math.min(server.getWidth(), Math.round(request.getMaxX() + downsample * pad));
            int y2 = (int) Math.min(server.getHeight(), Math.round(request.getMaxY() + downsample * pad));
            RegionRequest requestPadded = RegionRequest.createInstance(server.getPath(), downsample, x1, y1, x2 - x1,
                    y2 - y1, request.getZ(), request.getT());

            CellsparseInferTask task = CellsparseInferTask.builder(qupath.getViewer())
                    .endpointURL(url.toString())
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

            futures.add(submitTask(task));
        }

        // Sychronize the results
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (CancellationException e) {
                logger.warn("Task is cancelled", e);
            } catch (ExecutionException e) {
                logger.warn("Error while waiting for task to complete", e);
            } catch (InterruptedException e) {
                logger.warn("Task is interrupted", e);
            }
        }

        Platform.runLater(() -> {
            finalize(hierarchy, toRomove, detections, union,
                    (SelectedObjectsRegionFilter) regionFilter,
                    selectedAnnotations);
        });

    }

    /**
     * Finalize the detection results.
     * 
     * @param hierarchy
     * @param toRomove
     * @param detections
     * @param union
     * @param regionFilter
     * @param selectedAnnotations
     */
    private void finalize(PathObjectHierarchy hierarchy, List<PathObject> toRomove, List<PathObject> detections,
            ROI union, SelectedObjectsRegionFilter regionFilter, Collection<PathObject> selectedAnnotations) {
        if (!keepExistingProperty.get()) {
            hierarchy.removeObjects(toRomove, false);
        }

        var nuclei = detections.stream().map(
                obj -> new PotentialNucleus(obj.getROI().getGeometry(), obj.getROI().getGeometry().getArea(), 0))
                .collect(Collectors.toList());
        var filteredNuclei = filterNuclei(nuclei);
        var finalDetections = filteredNuclei.stream()
                .map(nucleus -> GeometryTools.geometryToROI(nucleus.geometry, qupath.getViewer().getImagePlane()))
                .map(roi -> PathObjects.createAnnotationObject(roi))
                .collect(Collectors.toList());

        hierarchy.addObjects(finalDetections);
        if (regionFilter == SelectedObjectsRegionFilter.SELECTED_OBJECTS) {
            hierarchy.removeObjects(selectedAnnotations, false);
            Collection<PathObject> toKeep = hierarchy.getObjectsForROI(null, union);
            hierarchy.removeObjects(finalDetections.stream()
                    .filter(pathObject -> !toKeep.contains(pathObject))
                    .collect(Collectors.toList()),
                    false);
            if (!toKeep.isEmpty()) {
                hierarchy.getSelectionModel().setSelectedObjects(toKeep, toKeep.iterator().next());
            }
        } else {
            if (!finalDetections.isEmpty()) {
                hierarchy.getSelectionModel().setSelectedObjects(finalDetections, finalDetections.get(0));
            }
        }
    }

    /**
     * Submit a task to reset a model.
     * 
     */
    private void submitResetTask() {
        logger.info("Resetting a model");
        CellsparseModel model = command.getModelProperty().get();
        if (model == null) {
            Dialogs.showErrorMessage("submitResetTask", "No model selected!");
        }
        if (!Dialogs.showConfirmDialog("Reset model", model.getParamterPaneReset())) {
            logger.debug("Cancel reset model");
            return;
        }
        String url = null;
        try {
            url = new URI(command.getServerURLProperty().get())
                    .resolve(model.getEndpoint() + "/reset")
                    .normalize()
                    .toURL()
                    .toString();
        } catch (URISyntaxException | MalformedURLException e) {
            logger.warn("{} is not a valid URL.", command.getServerURLProperty().get());
        }
        if (url == null)
            return;
        if (!url.endsWith("/")) {
            url += "/";
        }
        CellsparseResetTask task = CellsparseResetTask.builder()
                .endpointURL(url.toString())
                .model(model)
                .build();
        task.setOnSucceeded(event -> {
            command.updateInfoText("Model is reset.");
        });
        submitTask(task);
    }

    /**
     * Original cocde from qupath-extension-stardist:
     * https://github.com/qupath/qupath-extension-stardist/blob/c9424b488a356a3af26ef7bc58eb9bce2592a108/src/main/java/qupath/ext/stardist/StarDist2D.java#L1573
     */
    private static class PotentialNucleus {

        private Geometry geometry;
        private double fullArea;
        private double probability;
        private int classification;

        PotentialNucleus(Geometry geom, double prob, int classification) {
            this.geometry = geom;
            this.probability = prob;
            this.classification = classification;
            this.fullArea = geom.getArea();
        }

        double getProbability() {
            return probability;
        };

        int getClassification() {
            return classification;
        }

    }

    /**
     * Original cocde from qupath-extension-stardist:
     * https://github.com/qupath/qupath-extension-stardist/blob/c9424b488a356a3af26ef7bc58eb9bce2592a108/src/main/java/qupath/ext/stardist/StarDist2D.java#L1573
     */
    private static List<PotentialNucleus> filterNuclei(List<PotentialNucleus> potentialNuclei) {

        // Sort in descending order of probability
        Collections.sort(potentialNuclei,
                Comparator.comparingDouble((PotentialNucleus n) -> n.getProbability()).reversed());

        // Create array of nuclei to keep & to skip
        var nuclei = new LinkedHashSet<PotentialNucleus>();
        var skippedNucleus = new HashSet<PotentialNucleus>();
        int skipErrorCount = 0;

        // Create a spatial cache to find overlaps more quickly
        // (Because of later tests, we don't need to update envelopes even though
        // geometries may be modified)
        Map<Geometry, Envelope> envelopes = new HashMap<>();
        var tree = new STRtree();
        for (var nuc : potentialNuclei) {
            var env = nuc.geometry.getEnvelopeInternal();
            envelopes.put(nuc.geometry, env);
            tree.insert(env, nuc);
        }

        var preparingFactory = new PreparedGeometryFactory();

        for (var nucleus : potentialNuclei) {
            if (skippedNucleus.contains(nucleus))
                continue;

            nuclei.add(nucleus);
            var envelope = envelopes.computeIfAbsent(nucleus.geometry, g -> g.getEnvelopeInternal());

            @SuppressWarnings("unchecked")
            var overlaps = (List<PotentialNucleus>) tree.query(envelope);

            // Remove the overlaps that we can be sure don't apply using quick tests, to
            // avoid expensive ones
            var iter = overlaps.iterator();
            while (iter.hasNext()) {
                var nucleus2 = iter.next();
                if (nucleus2 == nucleus || skippedNucleus.contains(nucleus2) || nuclei.contains(nucleus2))
                    iter.remove();
                else {
                    // Envelope text needed because nuclei can have been modified
                    var env = envelopes.computeIfAbsent(nucleus2.geometry, g -> g.getEnvelopeInternal());
                    if (!envelope.intersects(env))
                        iter.remove();
                }
            }

            // If we need to compare a lot of intersections, preparing the geometry can
            // speed things up
            PreparedGeometry prepared = null;
            if (overlaps.size() > 5) {
                prepared = preparingFactory.create(nucleus.geometry);
            }
            for (var nucleus2 : overlaps) {
                // If we have an overlap, retain the higher-probability nucleus only (i.e. the
                // one we met first)
                // Try to refine other nuclei
                try {
                    boolean checkDifference = true;
                    if (prepared == null) {
                        // We could check for intersection, but it seems faster to just compute
                        // difference
                        // (this would warrant some more systematic checking though)
                        checkDifference = true;// nucleus.geometry.intersects(nucleus2.geometry);
                    } else
                        checkDifference = prepared.intersects(nucleus2.geometry);
                    if (checkDifference) {
                        // Retain the nucleus only if it is not fragmented, or less than half its
                        // original area
                        var difference = nucleus2.geometry.difference(nucleus.geometry);

                        // Discard linestrings
                        if (difference instanceof GeometryCollection)
                            difference = GeometryTools.ensurePolygonal(difference);

                        if (difference instanceof Polygon && difference.getArea() > nucleus2.fullArea / 2.0)
                            nucleus2.geometry = difference;
                        else {
                            skippedNucleus.add(nucleus2);
                        }
                    }
                } catch (Exception e) {
                    logger.debug("Exception resolving nuclei: " + e.getMessage());
                    logger.trace(e.getMessage(), e);
                    skippedNucleus.add(nucleus2);
                    skipErrorCount++;
                }

            }
        }
        if (skipErrorCount > 0) {
            // Reduce warning to debug - this happens often for 1 or 2 nuclei but isn't
            // necessarily
            // a serious problem that the user should be aware of
            int skipCount = skippedNucleus.size();
            String s = skipErrorCount == 1 ? "1 nucleus" : skipErrorCount + " nuclei";
            logger.debug("Skipped {} due to error in resolving overlaps ({}% of all skipped)",
                    s, GeneralTools.formatNumber(skipErrorCount * 100.0 / skipCount, 1));
        }
        return new ArrayList<>(nuclei);
    }

}
