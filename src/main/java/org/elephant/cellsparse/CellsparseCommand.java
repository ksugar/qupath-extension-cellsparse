package org.elephant.cellsparse;

import java.awt.image.BufferedImage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.elephant.cellsparse.CellsparseModels.CellsparseModel;
import org.elephant.cellsparse.ui.CellsparsePane;
import org.elephant.cellsparse.ui.CellsparseUIUtils;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import javafx.concurrent.Task;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import qupath.lib.common.ThreadTools;
import qupath.lib.gui.QuPathGUI;
import qupath.lib.gui.dialogs.Dialogs;
import qupath.lib.gui.prefs.PathPrefs;
import qupath.lib.images.ImageData;

public class CellsparseCommand implements Runnable {

    /**
     * Server for Cellsparseapi - see https://github.com/ksugar/cellsparse-api
     */
    private final StringProperty serverURLProperty = PathPrefs.createPersistentPreference(
            "ext.Cellsparse.serverUrl", "http://localhost:8000/");

    public StringProperty getServerURLProperty() {
        return serverURLProperty;
    }

    /**
     * Model index property.
     */
    private static final int DEFAULT_MODEL_INDEX = 1;
    private final IntegerProperty modelIndexProperty = PathPrefs.createPersistentPreference(
            "ext.Cellsparse.modelIndex", DEFAULT_MODEL_INDEX);

    public IntegerProperty getModelIndexProperty() {
        return modelIndexProperty;
    }

    /**
     * Selected Model property.
     */
    private final ObjectProperty<CellsparseModel> modelProperty = new SimpleObjectProperty<>();

    public ObjectProperty<CellsparseModel> getModelProperty() {
        return modelProperty;
    }

    /**
     * Current image data
     */
    private final ObjectProperty<ImageData<BufferedImage>> imageDataProperty = new SimpleObjectProperty<>();

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

    /**
     * Progress of the current detection.
     */
    private final DoubleProperty progressProperty = new SimpleDoubleProperty(1.0);

    public DoubleProperty getProgressProperty() {
        return progressProperty;
    }

    /**
     * Flag to indicate that running isn't possible right now
     */
    private final BooleanBinding disableRunning = imageDataProperty.isNull()
            .or(serverURLProperty.isEmpty())
            .or(modelProperty.isNull())
            .or(isTaskRunning);

    public BooleanBinding getDisableRunning() {
        return disableRunning;
    }

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

    private final QuPathGUI qupath;

    private Stage stage;

    /**
     * Task pool
     */
    private ExecutorService pool;

    /**
     * Constructor.
     * 
     * @param qupath
     *            main QuPath instance
     */
    public CellsparseCommand(QuPathGUI qupath) {
        this.qupath = qupath;
    }

    @Override
    public void run() {
        var qupath = QuPathGUI.getInstance();
        var imageData = qupath.getImageData();
        if (imageData == null) {
            Dialogs.showNoImageError("Cellsparse");
        } else {
            showStage();
        }

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
            fixStageSizeOnFirstShow(stage);
        }
    }

    private Stage createStage() {
        Stage stage = new Stage();
        Pane pane = new CellsparsePane(this, qupath);
        Scene scene = new Scene(pane);
        stage.setScene(scene);
        Platform.runLater(() -> {
            fixStageSizeOnFirstShow(stage);
        });
        stage.setResizable(false);
        stage.setTitle("Cellsparse");
        stage.initOwner(qupath.getStage());
        stage.setOnCloseRequest(event -> {
            stage.hide();
            event.consume();
        });
        return stage;
    }

    private static void fixStageSizeOnFirstShow(Stage stage) {
        stage.sizeToScene();
        // Make slightly wider, as workaround for column constraints not including h-gap
        stage.setWidth(stage.getWidth() + CellsparseUIUtils.H_GAP * 2);
        // Set other size/position variables so they are retained after stage is hidden
        stage.setHeight(stage.getHeight());
        stage.setX(stage.getX());
        stage.setY(stage.getY());
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

}
