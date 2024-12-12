package org.elephant.cellsparse.models;

import java.util.List;

import javafx.scene.Node;
import qupath.lib.plugins.parameters.Parameter;
import qupath.lib.plugins.parameters.ParameterList;

public abstract class CellsparseModel {

    /**
     * Model can handle missing (NaN) values
     * 
     * @return true if NaNs are supported, false otherwise
     */
    public abstract boolean supportsMissingValues();

    /**
     * User-friendly, readable name for the model
     * 
     * @return the model name
     */
    public abstract String getName();

    /**
     * Endpoint for the model (e.g. /stardist)
     * 
     * @return the endpoint
     */
    public abstract String getEndpoint();

    /**
     * Model has already been trained and is ready to predict.
     * 
     * @return true if the model is trained, false otherwise
     */
    public abstract boolean isTrained();

    /**
     * Model is able to handle more than one outputs for a single sample.
     * 
     * @return true if multiclass classification is supported, false otherwise
     */
    public abstract boolean supportsMulticlass();

    /**
     * Model can be trained interactively (i.e. quickly).
     * 
     * @return true if interactive training is supported, false otherwise
     */
    public abstract boolean supportsAutoUpdate();

    /**
     * Model can output a prediction confidence (expressed between 0 and 1),
     * so may be interpreted as a probability... even if it isn't necessarily one.
     * 
     * @return true if (pseudo-)probabilities can be provided
     */
    public abstract boolean supportsProbabilities();

    /**
     * Retrieve a list of adjustable parameter that can be used to customize the
     * model.
     * After making changes to the {@link ParameterList}, the model should be
     * retrained before being used.
     * 
     * @return the parameter list for this model
     */
    public abstract ParameterList getParameterList();

    /**
     * Retrieve a list of adjustable parameter that can be used to customize the
     * model.
     * After making changes to the {@link ParameterList}, the model should be
     * retrained before being used.
     * 
     * @return the parameter list for this model
     */
    public abstract ParameterList getParameterListReset();

    public abstract Parameter<?> getParameter(ParameterList params, String key);

    /**
     * Retrieve a JavaFX node that can be used to adjust the parameters of the
     * model.
     * 
     * @return the parameter pane for this model
     */
    public abstract Node getParamterPane();

    /**
     * Retrieve a JavaFX node that can be used to adjust the parameters for reset.
     * 
     * @return the parameter pane for reset
     */
    public abstract Node getParamterPaneReset();

    /**
     * Get request body string for training.
     * 
     * @return the body string for training.
     */
    public abstract String getRequestBodyStringTrain(List<String> b64imgs, List<String> b64lbls);

    /**
     * Get request body string for inference.
     * 
     * @param b64img the base64 encoded image
     * 
     * @return the body string for inference.
     */
    public abstract String getRequestBodyStringInfer(String b64img);

    /**
     * Get request body string for reset.
     * 
     * @return the body string for reset.
     */
    public abstract String getRequestBodyStringReset();

    /**
     * Run training.
     * 
     */
    public abstract void train();

    @Override
    public String toString() {
        return String.format("Cellsparse %s", getName());
    }
}