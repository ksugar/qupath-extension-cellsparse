package org.elephant.cellsparse;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.elephant.cellsparse.CellsparseModels.CellsparseModel;
import org.elephant.cellsparse.tasks.CellsparseTask;
import org.elephant.cellsparse.ui.CellsparseResolution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.LongProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import qupath.lib.common.GeneralTools;
import qupath.lib.common.ThreadTools;
import qupath.lib.gui.QuPathGUI;
import qupath.lib.gui.dialogs.Dialogs;
import qupath.lib.gui.tools.PaneTools;
import qupath.lib.images.ImageData;
import qupath.lib.images.servers.ImageServer;
import qupath.lib.images.servers.PixelCalibration;
import qupath.lib.objects.PathObject;
import qupath.lib.objects.hierarchy.PathObjectHierarchy;
import qupath.lib.objects.hierarchy.events.PathObjectHierarchyEvent;
import qupath.lib.objects.hierarchy.events.PathObjectHierarchyListener;
import qupath.process.gui.commands.ml.ClassificationResolution;
import qupath.process.gui.commands.ml.PixelClassifierUI;

public class CellsparsePaneOld {

    private static final Logger logger = LoggerFactory.getLogger(CellsparsePaneOld.class);

    private QuPathGUI qupath;

    private GridPane pane;

    private ObservableList<ClassificationResolution> resolutions = FXCollections.observableArrayList();
    private ComboBox<ClassificationResolution> comboResolutions = new ComboBox<>(resolutions);
    private ReadOnlyObjectProperty<ClassificationResolution> selectedResolution;
    private ReadOnlyObjectProperty<CellsparseModel> selectedModel;

    /**
     * String to contain help text for the user
     */
    private StringProperty infoTextProperty = new SimpleStringProperty();

    public StringProperty getInfoTextProperty() {
        return infoTextProperty;
    }

    /**
     * Timestamp of the last info text representing an error. This can be used for styling,
     * and/or to ensure that errors remain visible for a minimum amount of time.
     */
    private LongProperty infoTextErrorTimestampProperty = new SimpleLongProperty(0);

    public LongProperty getInfoTextErrorTimestampProperty() {
        return infoTextErrorTimestampProperty;
    }

    /**
     * The task currently running
     */
    private final ObservableSet<Task<?>> currentTasks = FXCollections.observableSet();

    /**
     * Currently awaiting a detection from the server
     */
    private final BooleanBinding isTaskRunning = Bindings.isNotEmpty(currentTasks);

    public BooleanBinding getIsTaskRunning() {
        return isTaskRunning;
    }

    private HierarchyListener hierarchyListener = new HierarchyListener();

    private ChangeListener<ImageData<BufferedImage>> imageDataListener = new ChangeListener<ImageData<BufferedImage>>() {

        @Override
        public void changed(ObservableValue<? extends ImageData<BufferedImage>> observable,
                ImageData<BufferedImage> oldValue, ImageData<BufferedImage> newValue) {
            if (oldValue != null)
                oldValue.getHierarchy().removeListener(hierarchyListener);
            if (newValue != null)
                newValue.getHierarchy().addListener(hierarchyListener);
            updateTitle();
            updateAvailableResolutions(newValue);
        }

    };

    private Stage stage;

    /**
     * Task pool
     */
    private ExecutorService pool;

    /**
     * Constructor.
     * 
     * @param qupath
     *            the current {@link QuPathGUI} that will be used for interactive training.
     */
    public CellsparsePaneOld(final QuPathGUI qupath) {
        this.qupath = qupath;
        showStage();
    }

    private void showStage() {
        boolean creatingStage = stage == null;
        if (creatingStage)
            stage = createStage();
        if (stage.isShowing())
            return;
        if (pool == null)
            pool = Executors.newCachedThreadPool(ThreadTools.createThreadFactory("Cellsparse", true));
        stage.show();
        if (creatingStage) {
            // fixStageSizeOnFirstShow(stage);
        }
    }

    /**
     * Update the info text with an error.
     * 
     * @param message
     */
    public void updateInfoTextWithError(String message) {
        infoTextProperty.set(message);
        infoTextErrorTimestampProperty.set(System.currentTimeMillis());
    }

    /**
     * Update the info text.
     * 
     * @param message
     */
    public void updateInfoText(String message) {
        // Don't overwrite an error message until 5 seconds have passed & we have
        // something worthwhile to show
        long lastErrorTimestamp = infoTextErrorTimestampProperty.get();
        boolean hasMessage = message != null && !message.isEmpty();
        if (System.currentTimeMillis() - lastErrorTimestamp < 5000 || (!hasMessage && lastErrorTimestamp > 0))
            return;
        infoTextProperty.set(message);
        infoTextErrorTimestampProperty.set(0);
    }

    private Stage createStage() {

        var imageData = qupath.getImageData();

        int row = 0;

        // Classifier
        pane = new GridPane();

        var labelServerUrl = new Label("Server URL");
        var textFieldServerUrl = new TextField("http://localhost:8000");
        PaneTools.addGridRow(pane, row++, 0, "Cellsparse server URL", labelServerUrl, textFieldServerUrl);

        var labelModel = new Label("Model");
        var comboModel = new ComboBox<CellsparseModel>();
        labelModel.setLabelFor(comboModel);

        selectedModel = comboModel.getSelectionModel().selectedItemProperty();
        selectedModel.addListener((v, o, n) -> updateClassifier());
        var btnEditModel = new Button("Edit");
        btnEditModel.setOnAction(e -> editClassifierParameters());
        btnEditModel.disableProperty().bind(selectedModel.isNull());

        PaneTools.addGridRow(pane, row++, 0, "Choose model type", labelModel, comboModel, comboModel,
                btnEditModel);

        // Image resolution
        var labelResolution = new Label("Resolution");
        labelResolution.setLabelFor(comboResolutions);
        var btnResolution = new Button("Add");
        btnResolution.setOnAction(e -> addResolution());
        selectedResolution = comboResolutions.getSelectionModel().selectedItemProperty();

        PaneTools.addGridRow(pane, row++, 0,
                "Choose the base image resolution based upon required detail in the segmentation (see preview on the right)",
                labelResolution, comboResolutions, comboResolutions, btnResolution);

        // Region
        var labelRegion = new Label("Region");
        var comboRegionFilter = PixelClassifierUI.createRegionFilterCombo(qupath.getOverlayOptions());
        // var nodeLimit =
        // PixelClassifierTools.createLimitToAnnotationsControl(qupath.getOverlayOptions());
        PaneTools.addGridRow(pane, row++, 0,
                "Control where the pixel classification is applied during preview", labelRegion,
                comboRegionFilter, comboRegionFilter, comboRegionFilter);

        // Reset model
        var btnReset = new Button("Reset model");
        btnReset.setTooltip(
                new Tooltip("Reset a previously trained model, or load a new model from a file"));
        btnReset.setOnAction(e -> {
            // TODO: reset model
            logger.debug("reset model");
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
        pane.add(paneIO, 0, row++, pane.getColumnCount(), 1);

        // Train
        var btnTrain = new Button("Train");
        btnTrain.setTooltip(new Tooltip("Train a model"));
        btnTrain.setOnAction(e -> {
            // TODO: Train model
            logger.debug("Train model");
        });
        btnTrain.disableProperty().bind(qupath.projectProperty().isNull());

        // Advanced options
        var btnAdvancedOptions = new Button("Advanced options");
        btnAdvancedOptions.setTooltip(
                new Tooltip("Advanced options to customize preprocessing and classifier behavior"));
        btnAdvancedOptions.setOnAction(e -> {
            if (showAdvancedOptions())
                updateClassifier();
        });

        var paneTrain = PaneTools.createColumnGridControls(btnTrain, btnAdvancedOptions);
        pane.add(paneTrain, 0, row++, pane.getColumnCount(), 1);

        comboModel.getItems().addAll(new CellsparseModels.StarDistModel(), new CellsparseModels.CellposeModel(),
                new CellsparseModels.ElephantModel());

        comboModel.getSelectionModel().clearAndSelect(1);

        PaneTools.setHGrowPriority(Priority.ALWAYS, comboResolutions, comboModel);
        PaneTools.setFillWidth(Boolean.TRUE, comboResolutions, comboModel);

        updateAvailableResolutions(imageData);
        selectedResolution.addListener((v, o, n) -> {
            updateResolution(n);
            updateClassifier();
        });
        if (!comboResolutions.getItems().isEmpty())
            comboResolutions.getSelectionModel().clearAndSelect(resolutions.size() / 2);

        pane.setHgap(5);
        pane.setVgap(6);

        PaneTools.setMaxWidth(Double.MAX_VALUE, pane.getChildren().stream()
                .filter(p -> p instanceof Region).toArray(Region[]::new));

        pane.setMinWidth(400);

        pane.setPadding(new Insets(5));

        stage = new Stage();
        stage.setScene(new Scene(pane));

        stage.setMinHeight(400);
        stage.setMinWidth(600);
        stage.sizeToScene();

        stage.initOwner(QuPathGUI.getInstance().getStage());

        updateTitle();

        PaneTools.setMinWidth(Region.USE_PREF_SIZE,
                PaneTools.getContentsOfType(stage.getScene().getRoot(), Region.class, true)
                        .toArray(Region[]::new));

        stage.setOnCloseRequest(e -> destroy());

        qupath.imageDataProperty().addListener(imageDataListener);
        if (qupath.getImageData() != null)
            qupath.getImageData().getHierarchy().addListener(hierarchyListener);

        return stage;
    }

    private void updateTitle() {
        if (stage == null)
            return;
        stage.setTitle("Cellsparse");
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
        var requestedResolutions = ClassificationResolution.getDefaultResolutions(imageData, selected);
        if (!resolutions.equals(requestedResolutions)) {
            resolutions.setAll(ClassificationResolution.getDefaultResolutions(imageData, selected));
            comboResolutions.getSelectionModel().select(selected);
        }
    }

    private void updateClassifier() {
        // TODO Auto-generated method stub
    }

    private boolean showAdvancedOptions() {
        return true;
    }

    private void destroy() {
        qupath.imageDataProperty().removeListener(imageDataListener);

        for (var viewer : qupath.getViewers()) {
            var hierarchy = viewer.getHierarchy();
            if (hierarchy != null)
                hierarchy.removeListener(hierarchyListener);
        }
        if (stage != null && stage.isShowing())
            stage.close();
    }

    private boolean editClassifierParameters() {
        var model = selectedModel.get();
        if (model == null) {
            Dialogs.showErrorMessage("Edit parameters", "No classifier selected!");
            return false;
        }
        Dialogs.showParameterDialog("Edit parameters", model.getParameterList());
        updateClassifier();
        return true;
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

        ClassificationResolution res;
        if (PixelCalibration.MICROMETER.equals(units)) {
            double scale = pixelSize / cal.getAveragedPixelSizeMicrons();
            // res = new CellsparseResolution("Custom", cal.createScaledInstance(scale, scale, 1));
        } else
            // res = new CellsparseResolution("Custom", cal.createScaledInstance(pixelSize, pixelSize, 1));

            // List<ClassificationResolution> temp = new ArrayList<>(resolutions);
            // temp.add(res);
            // Collections.sort(temp,
            // Comparator.comparingDouble(
            // (ClassificationResolution w) -> w.getPixelCalibration().getAveragedPixelSize().doubleValue()));
            // resolutions.setAll(temp);
            // comboResolutions.getSelectionModel().select(res);
            System.out.println("");
        return true;
    }

    private void updateResolution(ClassificationResolution resolution) {
        ImageServer<BufferedImage> server = qupath.getImageData() == null ? null : qupath.getImageData().getServer();
        if (server == null || resolution == null)
            return;
        // helper.setResolution(resolution.getPixelCalibration());
    }

    class HierarchyListener implements PathObjectHierarchyListener {

        @Override
        public void hierarchyChanged(PathObjectHierarchyEvent event) {
            if (!event.isChanging() && !event.isObjectMeasurementEvent()
                    && (event.isStructureChangeEvent() || event.isObjectClassificationEvent()
                            || !event.getChangedObjects().isEmpty())) {
                if (event.isObjectClassificationEvent() || event.getChangedObjects().stream()
                        .anyMatch(p -> p.getPathClass() != null)) {
                    if (event.getChangedObjects().stream().anyMatch(p -> p.isAnnotation())
                            && !(event.isAddedOrRemovedEvent() && event.getChangedObjects().stream()
                                    .allMatch(p -> p.isLocked())))
                        updateClassifier();
                }
            }
        }

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
                currentTasks.remove(task);
                break;
            case CANCELLED:
                logger.info("Task cancelled");
                currentTasks.remove(task);
                if (task.getException() != null) {
                    logger.warn("Task failed: {}", task, task.getException());
                    updateInfoTextWithError("Task cancelled with exception " + task.getException() +
                            "\nSee log for details.");
                } else {
                    updateInfoText("Task cancelled");
                }
                break;
            case FAILED:
                currentTasks.remove(task);
                if (task.getException() != null) {
                    logger.warn("Task failed: {}", task, task.getException());
                    updateInfoTextWithError("Task failed with exception " + task.getException() +
                            "\nSee log for details.");
                } else {
                    updateInfoTextWithError("Task failed!");
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

    private void submitTask(Task<?> task) {
        task.setOnFailed(event -> {
            Platform.runLater(() -> {
                Dialogs.showErrorMessage("Connection failed",
                        "Please check that the samapi server (v0.4 and above) is running and the URL is correct.");
            });
        });
        pool.submit(task);
        currentTasks.add(task);
        task.stateProperty().addListener((observable, oldValue, newValue) -> taskStateChange(task, newValue));
    }

    /**
     * Submit a task to run training.
     * 
     */
    private void submitTrainingTask() {
        logger.info("Submitting task for training");
        CellsparseTask task = CellsparseTask.builder(qupath.getViewer())
                .endpointURL("http://localhost:8000")
                .train(true)
                .epochs(10)
                .batchsize(1)
                .steps(1)
                .build();
        task.setOnSucceeded(event -> {
            List<PathObject> detected = task.getValue();
            if (detected != null && !task.getValue().isEmpty()) {
                if (!detected.isEmpty()) {
                    Platform.runLater(() -> {
                        PathObjectHierarchy hierarchy = qupath.getViewer().getImageData().getHierarchy();
                        List<PathObject> toRomove = hierarchy.getAnnotationObjects().stream()
                                .filter(pathObject -> pathObject.getPathClass() == null).toList();
                        hierarchy.removeObjects(toRomove, false);
                        hierarchy.addObjects(detected);
                        hierarchy.getSelectionModel().setSelectedObjects(detected, detected.get(0));
                    });
                } else {
                    logger.warn("No objects detected");
                }
            }
        });
        submitTask(task);
    }
}
