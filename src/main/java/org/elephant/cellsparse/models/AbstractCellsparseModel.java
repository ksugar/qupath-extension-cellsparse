package org.elephant.cellsparse.models;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.elephant.cellsparse.tasks.CellsparseInferTask;
import org.elephant.cellsparse.tasks.CellsparseResetTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.beans.property.Property;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.Node;
import qupath.lib.gui.dialogs.ParameterPanelFX;
import qupath.lib.gui.prefs.PathPrefs;
import qupath.lib.gui.viewer.QuPathViewer;
import qupath.lib.objects.PathObject;
import qupath.lib.objects.hierarchy.PathObjectHierarchy;
import qupath.lib.plugins.parameters.BooleanParameter;
import qupath.lib.plugins.parameters.ChoiceParameter;
import qupath.lib.plugins.parameters.DoubleParameter;
import qupath.lib.plugins.parameters.EmptyParameter;
import qupath.lib.plugins.parameters.IntParameter;
import qupath.lib.plugins.parameters.Parameter;
import qupath.lib.plugins.parameters.ParameterChangeListener;
import qupath.lib.plugins.parameters.ParameterList;
import qupath.lib.plugins.parameters.StringParameter;
import qupath.lib.regions.RegionRequest;

public abstract class AbstractCellsparseModel<T extends AbstractCellsparseModel.CellsparseTrainBody.Builder, U extends AbstractCellsparseModel.CellsparseInferBody.Builder, V extends AbstractCellsparseModel.CellsparseResetBody.Builder>
        extends CellsparseModel
        implements ParameterChangeListener {

    private static final Logger logger = LoggerFactory.getLogger(AbstractCellsparseModel.class);

    private static final String PARAM_KEY_MODELNAME = "modelname";
    private static final String PARAM_KEY_EPOCHS = "epochs";
    private static final String PARAM_KEY_TRAINPATCH = "trainpatch";
    private static final String PARAM_KEY_BATCHSIZE = "batchsize";
    private static final String PARAM_KEY_STEPS = "steps";
    private static final String PARAM_KEY_LR = "lr";
    private static final String PARAM_KEY_MINAREA = "minarea";
    private static final String PARAM_KEY_SIMPLIFY_TOL = "simplify_tol";
    private static final String PARAM_KEY_PRETRAINED = "pretrained";

    private static final String DEFAULT_MODEL_NAME = "default";
    private static final int DEFAULT_NUM_EPOCHS = 1;
    private static final int DEFAULT_TRAIN_PATCH = 224;
    private static final int DEFAULT_BATCH_SIZE = 8;
    private static final int DEFAULT_NUM_STEPS = 200;
    private static final double DEFAULT_LR = 0.001;
    private static final double DEFAULT_MIN_AREA = 10.0;
    private static final double DEFAULT_SIMPLIFY_TOL = 0;
    private static final String DEFAULT_PRETRAINED = "random";

    private transient ParameterPanelFX parameterPanelFX = null;

    private transient ParameterPanelFX parameterPanelFXReset = null;

    private transient ParameterList params; // Should take defaults from the serialized model

    private transient ParameterList paramsReset; // Should take defaults from the serialized model

    private transient Map<String, Property<?>> propertyMap = new HashMap<>();

    public AbstractCellsparseModel() {
    }

    public AbstractCellsparseModel(final AbstractCellsparseModelBuilder<?, ?> builder) {
        ((StringParameter) getParameter(getParameterList(), PARAM_KEY_MODELNAME)).setValue(builder.modelname);
        ((IntParameter) getParameter(getParameterList(), PARAM_KEY_EPOCHS)).setValue(builder.epochs);
        ((IntParameter) getParameter(getParameterList(), PARAM_KEY_TRAINPATCH)).setValue(builder.trainpatch);
        ((IntParameter) getParameter(getParameterList(), PARAM_KEY_BATCHSIZE)).setValue(builder.batchsize);
        ((IntParameter) getParameter(getParameterList(), PARAM_KEY_STEPS)).setValue(builder.steps);
        ((DoubleParameter) getParameter(getParameterList(), PARAM_KEY_LR)).setValue(builder.lr);
        ((DoubleParameter) getParameter(getParameterList(), PARAM_KEY_MINAREA)).setValue(builder.minarea);
        ((DoubleParameter) getParameter(getParameterList(), PARAM_KEY_SIMPLIFY_TOL)).setValue(builder.simplify_tol);
        ((ChoiceParameter<?>) getParameter(getParameterListReset(), PARAM_KEY_PRETRAINED)).setStringLastValue(null,
                builder.pretrained);
    }

    ParameterList createParameterList() {
        ParameterList params = new ParameterList()
                .addStringParameter(PARAM_KEY_MODELNAME, "Model name", DEFAULT_MODEL_NAME)
                .addIntParameter(PARAM_KEY_TRAINPATCH, "Patch size", DEFAULT_TRAIN_PATCH, null,
                        "Patch size for training")
                .addIntParameter(PARAM_KEY_BATCHSIZE, "Batch size", DEFAULT_BATCH_SIZE, null,
                        "Batch size for training")
                .addIntParameter(PARAM_KEY_EPOCHS, "Number of epochs", DEFAULT_NUM_EPOCHS, null,
                        "Number of epochs for training")
                .addIntParameter(PARAM_KEY_STEPS, "Number of steps", DEFAULT_NUM_STEPS, null,
                        "Number of steps for training")
                .addDoubleParameter(PARAM_KEY_LR, "Learning rate", DEFAULT_LR, null,
                        "Learning rate for training")
                .addDoubleParameter(PARAM_KEY_MINAREA, "Minimum area", DEFAULT_MIN_AREA, "px^2",
                        "Minimum area for inference")
                .addDoubleParameter(PARAM_KEY_SIMPLIFY_TOL, "Simplify tolrence", DEFAULT_SIMPLIFY_TOL, null,
                        "Simplify tolrence for inference (NaN for not applying)");
        return params;
    }

    abstract List<String> getPretraineOptions();

    ParameterList createParameterListReset() {
        ParameterList params = new ParameterList()
                .addChoiceParameter(PARAM_KEY_PRETRAINED, "Pretrained model",
                        DEFAULT_PRETRAINED, getPretraineOptions(), "Pretrained model for reset");
        return params;
    }

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
            for (Entry<String, Parameter<?>> entry : params.getParameters().entrySet()) {
                registerParameter(entry.getKey(), entry.getValue());
            }
        }
        return params;
    }

    @Override
    public ParameterList getParameterListReset() {
        if (paramsReset == null) {
            paramsReset = createParameterListReset();
            for (Entry<String, Parameter<?>> entry : paramsReset.getParameters().entrySet()) {
                registerParameter(entry.getKey(), entry.getValue());
            }
        }
        return paramsReset;
    }

    @Override
    public Parameter<?> getParameter(ParameterList params, String key) {
        return params.getParameters().get(key);
    }

    @Override
    public Node getParamterPane() {
        if (parameterPanelFX == null)
            parameterPanelFX = new ParameterPanelFX(getParameterList());
        parameterPanelFX.removeParameterChangeListener(this);
        parameterPanelFX.addParameterChangeListener(this);
        return parameterPanelFX.getPane();
    }

    @Override
    public Node getParamterPaneReset() {
        if (parameterPanelFXReset == null)
            parameterPanelFXReset = new ParameterPanelFX(getParameterListReset());
        parameterPanelFXReset.removeParameterChangeListener(this);
        parameterPanelFXReset.addParameterChangeListener(this);
        return parameterPanelFXReset.getPane();
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
    public boolean isTrained() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'isTrained'");
    }

    @Override
    public void train() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'train'");
    }

    public void infer(final QuPathViewer viewer, final String serverURL, final RegionRequest regionRequest)
            throws URISyntaxException, MalformedURLException {
        String url = null;
        try {
            url = new URI(serverURL)
                    .resolve(getEndpoint())
                    .normalize()
                    .toURL()
                    .toString();
        } catch (URISyntaxException | MalformedURLException e) {
            logger.warn("{} is not a valid URL.", serverURL);
            throw e;
        }
        if (!url.endsWith("/")) {
            url += "/";
        }
        CellsparseInferTask task = CellsparseInferTask.builder(viewer)
                .endpointURL(url)
                .model(this)
                .regionRequest(regionRequest)
                .build();
        task.setOnSucceeded(event -> {
            List<PathObject> detected = task.getValue();
            if (detected != null && !task.getValue().isEmpty()) {
                if (!detected.isEmpty()) {
                    Platform.runLater(() -> {
                        PathObjectHierarchy hierarchy = viewer.getImageData().getHierarchy();
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
        Platform.runLater(task);
    }

    public void infer(final QuPathViewer viewer, final String serverURL)
            throws URISyntaxException, MalformedURLException {
        infer(viewer, serverURL, null);
    }

    public void reset(final String serverURL) throws URISyntaxException, MalformedURLException {
        String url = null;
        try {
            url = new URI(serverURL)
                    .resolve(getEndpoint() + "/reset")
                    .normalize()
                    .toURL()
                    .toString();
        } catch (URISyntaxException | MalformedURLException e) {
            logger.warn("{} is not a valid URL.", serverURL);
            throw e;
        }
        if (!url.endsWith("/")) {
            url += "/";
        }
        CellsparseResetTask task = CellsparseResetTask.builder()
                .endpointURL(url.toString())
                .model(this)
                .build();
        task.setOnSucceeded(event -> {
            logger.info("Model is reset.");
        });
        Platform.runLater(task);
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

    abstract T getTrainBodyBuilder();

    abstract U getInferBodyBuilder();

    abstract V getResetBodyBuilder();

    CellsparseTrainBody.Builder getDefaultTrainBodyBuilder(List<String> b64imgs, List<String> b64lbls) {
        return getTrainBodyBuilder()
                .modelname(((StringParameter) getParameter(getParameterList(), PARAM_KEY_MODELNAME)).getValue())
                .b64imgs(b64imgs)
                .b64lbls(b64lbls)
                .train(true)
                .eval(true)
                .trainpatch(((IntParameter) getParameter(getParameterList(), PARAM_KEY_TRAINPATCH)).getValue())
                .batchsize(((IntParameter) getParameter(getParameterList(), PARAM_KEY_BATCHSIZE)).getValue())
                .epochs(((IntParameter) getParameter(getParameterList(), PARAM_KEY_EPOCHS)).getValue())
                .steps(((IntParameter) getParameter(getParameterList(), PARAM_KEY_STEPS)).getValue())
                .lr(((DoubleParameter) getParameter(getParameterList(), PARAM_KEY_LR)).getValue())
                .minarea(((DoubleParameter) getParameter(getParameterList(), PARAM_KEY_MINAREA)).getValue())
                .simplify_tol(
                        ((DoubleParameter) getParameter(getParameterList(), PARAM_KEY_SIMPLIFY_TOL)).getValue());
    }

    CellsparseInferBody.Builder getDefaultInferBodyBuilder() {
        return getInferBodyBuilder()
                .modelname(((StringParameter) getParameter(getParameterList(), PARAM_KEY_MODELNAME)).getValue())
                .eval(true)
                .minarea(((DoubleParameter) getParameter(getParameterList(), PARAM_KEY_MINAREA)).getValue())
                .simplify_tol(
                        ((DoubleParameter) getParameter(getParameterList(), PARAM_KEY_SIMPLIFY_TOL)).getValue());
    }

    CellsparseResetBody.Builder getDefaultResetBodyBuilder() {
        return getResetBodyBuilder()
                .modelname(((StringParameter) getParameter(getParameterList(), PARAM_KEY_MODELNAME)).getValue())
                .pretrained(((ChoiceParameter<?>) getParameter(getParameterListReset(), PARAM_KEY_PRETRAINED))
                        .getValue().toString());
    }

    public abstract static class AbstractCellsparseModelBuilder<S extends AbstractCellsparseModel<?, ?, ?>, R extends AbstractCellsparseModelBuilder<S, R>> {
        private String modelname;
        private int trainpatch = 224;
        private int batchsize = 8;
        private int epochs = 10;
        private int steps = 10;
        private double lr = 0.001;
        private double minarea = 10.0;
        private double simplify_tol = 0;
        private String pretrained = "random";

        public AbstractCellsparseModelBuilder() {
        }

        protected abstract R self();

        public R modelname(final String modelname) {
            this.modelname = modelname;
            return self();
        }

        public R trainpatch(final int trainpatch) {
            this.trainpatch = trainpatch;
            return self();
        }

        public R batchsize(final int batchsize) {
            this.batchsize = batchsize;
            return self();
        }

        public R epochs(final int epochs) {
            this.epochs = epochs;
            return self();
        }

        public R steps(final int steps) {
            this.steps = steps;
            return self();
        }

        public R lr(final double lr) {
            this.lr = lr;
            return self();
        }

        public R minarea(final double minarea) {
            this.minarea = minarea;
            return self();
        }

        public R simplify_tol(final double simplify_tol) {
            this.simplify_tol = simplify_tol;
            return self();
        }

        public R pretrained(final String pretrained) {
            this.pretrained = pretrained;
            return self();
        }

        public abstract S build();
    }

    public static class CellsparseTrainBody {

        @SuppressWarnings("unused")
        private String modelname;
        @SuppressWarnings("unused")
        private List<String> b64imgs;
        @SuppressWarnings("unused")
        private List<String> b64lbls;
        @SuppressWarnings("unused")
        private boolean train;
        @SuppressWarnings("unused")
        private boolean eval;
        @SuppressWarnings("unused")
        private int trainpatch;
        @SuppressWarnings("unused")
        private int batchsize;
        @SuppressWarnings("unused")
        private int epochs;
        @SuppressWarnings("unused")
        private int steps;
        @SuppressWarnings("unused")
        private double lr;
        @SuppressWarnings("unused")
        private double minarea;
        @SuppressWarnings("unused")
        private double simplify_tol;

        public CellsparseTrainBody(final Builder builder) {
            this.modelname = builder.modelname;
            this.b64imgs = builder.b64imgs;
            this.b64lbls = builder.b64lbls;
            this.train = builder.train;
            this.eval = builder.eval;
            this.trainpatch = builder.trainpatch;
            this.epochs = builder.epochs;
            this.batchsize = builder.batchsize;
            this.steps = builder.steps;
            this.lr = builder.lr;
            this.minarea = builder.minarea;
            this.simplify_tol = builder.simplify_tol;
        }

        public static class Builder {
            private String modelname;
            private List<String> b64imgs;
            private List<String> b64lbls = null;
            private boolean train = true;
            private boolean eval = true;
            private int trainpatch = 224;
            private int batchsize = 8;
            private int epochs = 10;
            private int steps = 10;
            private double lr = 0.001;
            private double minarea = 10.0;
            private double simplify_tol = 0;

            public Builder() {
            }

            public Builder modelname(final String modelname) {
                this.modelname = modelname;
                return this;
            }

            public Builder b64imgs(final List<String> b64imgs) {
                this.b64imgs = b64imgs;
                return this;
            }

            public Builder b64lbls(final List<String> b64lbls) {
                this.b64lbls = b64lbls;
                return this;
            }

            public Builder train(final boolean train) {
                this.train = train;
                return this;
            }

            public Builder eval(final boolean eval) {
                this.eval = eval;
                return this;
            }

            public Builder epochs(final int epochs) {
                this.epochs = epochs;
                return this;
            }

            public Builder trainpatch(final int trainpatch) {
                this.trainpatch = trainpatch;
                return this;
            }

            public Builder batchsize(final int batchsize) {
                this.batchsize = batchsize;
                return this;
            }

            public Builder steps(final int steps) {
                this.steps = steps;
                return this;
            }

            public Builder lr(final double lr) {
                this.lr = lr;
                return this;
            }

            public Builder minarea(final double minarea) {
                this.minarea = minarea;
                return this;
            }

            public Builder simplify_tol(final double simplify_tol) {
                this.simplify_tol = simplify_tol;
                return this;
            }

            CellsparseTrainBody build() {
                return new CellsparseTrainBody(this);
            }
        }
    }

    public static class CellsparseInferBody {

        @SuppressWarnings("unused")
        private String modelname;
        @SuppressWarnings("unused")
        private String b64img;
        @SuppressWarnings("unused")
        private boolean eval;
        @SuppressWarnings("unused")
        private double minarea;
        @SuppressWarnings("unused")
        private double simplify_tol;

        public CellsparseInferBody(final Builder builder) {
            this.modelname = builder.modelname;
            this.b64img = builder.b64img;
            this.eval = builder.eval;
            this.minarea = builder.minarea;
            this.simplify_tol = builder.simplify_tol;
        }

        public static class Builder {
            private String modelname;
            private String b64img;
            private boolean eval = true;
            private double minarea = 10.0;
            private double simplify_tol = 0;

            public Builder() {
            }

            public Builder modelname(final String modelname) {
                this.modelname = modelname;
                return this;
            }

            public Builder b64img(final String b64img) {
                this.b64img = b64img;
                return this;
            }

            public Builder eval(final boolean eval) {
                this.eval = eval;
                return this;
            }

            public Builder minarea(final double minarea) {
                this.minarea = minarea;
                return this;
            }

            public Builder simplify_tol(final double simplify_tol) {
                this.simplify_tol = simplify_tol;
                return this;
            }

            CellsparseInferBody build() {
                return new CellsparseInferBody(this);
            }
        }
    }

    public static class CellsparseResetBody {

        @SuppressWarnings("unused")
        private String modelname;
        @SuppressWarnings("unused")
        private String pretrained;

        public CellsparseResetBody(final Builder builder) {
            this.modelname = builder.modelname;
            this.pretrained = builder.pretrained;
        }

        public static class Builder {
            private String modelname;
            private String pretrained;

            public Builder() {
            }

            public Builder modelname(final String modelname) {
                this.modelname = modelname;
                return this;
            }

            public Builder pretrained(final String pretrained) {
                this.pretrained = pretrained;
                return this;
            }

            CellsparseResetBody build() {
                return new CellsparseResetBody(this);
            }
        }
    }
}