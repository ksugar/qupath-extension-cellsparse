package org.elephant.cellsparse.ui;

import org.elephant.cellsparse.CellsparseCommand;
import org.elephant.cellsparse.CellsparseModels;
import org.elephant.cellsparse.CellsparseModels.CellsparseModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
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
import javafx.scene.text.TextAlignment;
import qupath.lib.common.GeneralTools;
import qupath.lib.gui.QuPathGUI;
import qupath.lib.gui.dialogs.Dialogs;
import qupath.lib.gui.tools.PaneTools;
import qupath.lib.images.ImageData;
import qupath.lib.images.servers.ImageServer;
import qupath.lib.images.servers.PixelCalibration;
import qupath.process.gui.commands.ml.PixelClassifierUI;

public class CellsparsePane extends GridPane {

    private static final Logger logger = LoggerFactory.getLogger(CellsparsePane.class);

    private final CellsparseCommand command;
    private final QuPathGUI qupath;

    private ObservableList<CellsparseResolution> resolutions = FXCollections.observableArrayList();
    private ComboBox<CellsparseResolution> comboResolutions = new ComboBox<>(resolutions);
    private ReadOnlyObjectProperty<CellsparseResolution> selectedResolution;

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
     *            The Cellsparse command.
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
        ComboBox<CellsparseModel> combo = new ComboBox<>();
        combo.getItems().addAll(
                new CellsparseModels.StarDistModel(),
                new CellsparseModels.CellposeModel(),
                new CellsparseModels.ElephantModel());

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
            var model = combo.getSelectionModel().selectedItemProperty().get();
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
        var comboRegionFilter = PixelClassifierUI.createRegionFilterCombo(qupath.getOverlayOptions());

        add(labelRegion, 0, row);
        add(comboRegionFilter, 1, row, GridPane.REMAINING, 1);
    }

    private void addIOButtons(int row) {
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
        add(paneIO, 0, row, GridPane.REMAINING, 1);
    }

    private void addCommandButtons(int row) {
        // Train
        var btnTrain = new Button("Train");
        btnTrain.setTooltip(new Tooltip("Train a model"));
        btnTrain.setOnAction(e -> {
            // TODO: Train model
            logger.debug("Train model");
        });
        btnTrain.disableProperty().bind(qupath.projectProperty().isNull());

        var paneCommand = PaneTools.createColumnGridControls(btnTrain);
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

}
