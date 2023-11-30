package org.elephant.cellsparse;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.Property;
import javafx.beans.property.StringProperty;
import javafx.scene.Node;
import qupath.lib.gui.dialogs.ParameterPanelFX;
import qupath.lib.gui.prefs.PathPrefs;
import qupath.lib.plugins.parameters.BooleanParameter;
import qupath.lib.plugins.parameters.ChoiceParameter;
import qupath.lib.plugins.parameters.DoubleParameter;
import qupath.lib.plugins.parameters.EmptyParameter;
import qupath.lib.plugins.parameters.IntParameter;
import qupath.lib.plugins.parameters.Parameter;
import qupath.lib.plugins.parameters.ParameterChangeListener;
import qupath.lib.plugins.parameters.ParameterList;
import qupath.lib.plugins.parameters.StringParameter;

public class CellsparseModels {

    public abstract static class CellsparseModel {

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
         * Retrieve a list of adjustable parameter that can be used to customize the model.
         * After making changes to the {@link ParameterList}, the model should be retrained
         * before being used.
         * 
         * @return the parameter list for this model
         */
        public abstract ParameterList getParameterList();

        /**
         * Retrieve a JavaFX node that can be used to adjust the parameters of the model.
         * 
         * @return the parameter list for this model
         */
        public abstract Node getParamterPane();

        /**
         * Run training.
         * 
         * @return the parameter list for this model
         */
        public abstract void train();

        @Override
        public String toString() {
            return String.format("Cellsparse %s", getName());
        }
    }

    abstract static class AbstractCellsparseModel extends CellsparseModel implements ParameterChangeListener {

        private transient ParameterPanelFX parameterPanelFX = null;

        private transient ParameterList params; // Should take defaults from the serialized model

        private transient Map<String, Property<?>> propertyMap = new HashMap<>();

        abstract ParameterList createParameterList();

        @Override
        public boolean supportsMissingValues() {
            return false;
        }

        @Override
        public boolean supportsMulticlass() {
            return false;
        }

        @Override
        public boolean supportsAutoUpdate() {
            return false;
        }

        @Override
        public boolean supportsProbabilities() {
            return false;
        }

        @Override
        public ParameterList getParameterList() {
            if (params == null) {
                params = createParameterList();
                registerParameters();
            }
            return params;
        }

        public Node getParamterPane() {
            if (parameterPanelFX == null)
                parameterPanelFX = new ParameterPanelFX(getParameterList());
            parameterPanelFX.removeParameterChangeListener(this);
            parameterPanelFX.addParameterChangeListener(this);
            return parameterPanelFX.getPane();
        }

        @Override
        public void parameterChanged(ParameterList parameterList, String key, boolean isAdjusting) {
            if (isAdjusting)
                return;
            var param = parameterList.getParameters().get(key);
            if (param instanceof EmptyParameter)
                return;
            if (param instanceof DoubleParameter)
                ((DoubleProperty) propertyMap.get(key)).setValue(((DoubleParameter) param).getValue());
            else if (param instanceof IntParameter)
                ((IntegerProperty) propertyMap.get(key)).setValue(((IntParameter) param).getValue());
            else if (param instanceof StringParameter)
                ((StringProperty) propertyMap.get(key)).setValue(((StringParameter) param).getValue());
            else if (param instanceof ChoiceParameter<?>)
                ((StringProperty) propertyMap.get(key)).setValue(((ChoiceParameter<?>) param).getValue().toString());
            else if (param instanceof BooleanParameter)
                ((BooleanProperty) propertyMap.get(key)).setValue(((BooleanParameter) param).getValue());
        }

        @Override
        public void train() {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'train'");
        }

        @Override
        public boolean isTrained() {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'isTrained'");
        }

        private void registerParameters() {
            for (Entry<String, Parameter<?>> entry : params.getParameters().entrySet()) {
                registerParameter(entry.getKey(), entry.getValue());
            }
        }

        private <S> void registerParameter(String key, Parameter<S> param) {
            if (param instanceof EmptyParameter || param.isHidden())
                return;
            String name = String.format("ext.Cellsparse.%s.%s", getName(), key);
            Property<?> property = null;
            if (param instanceof DoubleParameter) {
                property = PathPrefs.createPersistentPreference(name, ((DoubleParameter) param).getDefaultValue());
                ((DoubleParameter) param).setValue(((DoubleProperty) property).getValue());
            } else if (param instanceof IntParameter) {
                property = PathPrefs.createPersistentPreference(name, ((IntParameter) param).getDefaultValue());
                ((IntParameter) param).setValue(((IntegerProperty) property).getValue());
            } else if (param instanceof StringParameter) {
                property = PathPrefs.createPersistentPreference(name, ((StringParameter) param).getDefaultValue());
                ((StringParameter) param).setValue(((StringProperty) property).getValue());
            } else if (param instanceof ChoiceParameter<?>) {
                property = PathPrefs.createPersistentPreference(name,
                        ((ChoiceParameter<?>) param).getDefaultValue().toString());
                for (S choice : ((ChoiceParameter<S>) param).getChoices()) {
                    if (choice.toString().equals(((StringProperty) property).getValue())) {
                        param.setValue(choice);
                        break;
                    }
                }
            } else if (param instanceof BooleanParameter) {
                property = PathPrefs.createPersistentPreference(name, ((BooleanParameter) param).getDefaultValue());
                ((BooleanParameter) param).setValue(((BooleanParameter) property).getValue());
            }
            propertyMap.put(key, property);
        }
    }

    public static class StarDistModel extends AbstractCellsparseModel {

        static final int DEFAULT_BATCH_SIZE = 8;
        static final int DEFAULT_NUM_EPOCHS = 200;

        public StarDistModel() {
        }

        @Override
        public String getName() {
            return "StarDist";
        }

        @Override
        ParameterList createParameterList() {
            ParameterList params = new ParameterList()
                    .addIntParameter("batchSize", "Batch size", DEFAULT_BATCH_SIZE, null, "Batch size for training")
                    .addIntParameter("numEpochs", "Number of epochs", DEFAULT_NUM_EPOCHS, null,
                            "Number of epochs for training");

            return params;
        }

    }

    public static class CellposeModel extends AbstractCellsparseModel {

        static final int DEFAULT_BATCH_SIZE = 8;
        static final int DEFAULT_NUM_EPOCHS = 200;

        public CellposeModel() {
        }

        @Override
        public String getName() {
            return "Cellpose";
        }

        @Override
        ParameterList createParameterList() {
            ParameterList params = new ParameterList()
                    .addIntParameter("batchSize", "Batch size", DEFAULT_BATCH_SIZE, null, "Batch size for training")
                    .addIntParameter("numEpochs", "Number of epochs", DEFAULT_NUM_EPOCHS, null,
                            "Number of epochs for training");

            return params;
        }

    }

    public static class ElephantModel extends AbstractCellsparseModel {

        static final int DEFAULT_BATCH_SIZE = 8;
        static final int DEFAULT_NUM_EPOCHS = 200;

        public ElephantModel() {
        }

        @Override
        public String getName() {
            return "ELEPHANT";
        }

        @Override
        ParameterList createParameterList() {
            ParameterList params = new ParameterList()
                    .addIntParameter("batchSize", "Batch size", DEFAULT_BATCH_SIZE, null, "Batch size for training")
                    .addIntParameter("numEpochs", "Number of epochs", DEFAULT_NUM_EPOCHS, null,
                            "Number of epochs for training");

            return params;
        }

    }

}
