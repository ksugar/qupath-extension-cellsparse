package org.elephant.cellsparse.models;

import java.util.Arrays;
import java.util.List;

import org.elephant.cellsparse.models.AbstractCellsparseModel.CellsparseInferBody;
import org.elephant.cellsparse.models.AbstractCellsparseModel.CellsparseResetBody;
import org.elephant.cellsparse.models.AbstractCellsparseModel.CellsparseTrainBody;

import qupath.lib.io.GsonTools;
import qupath.lib.plugins.parameters.IntParameter;
import qupath.lib.plugins.parameters.ParameterList;

public class CellposeModel extends
        AbstractCellsparseModel<CellposeModel.CellposeTrainBody.Builder, CellposeModel.CellposeInferBody.Builder, CellposeModel.CellposeResetBody.Builder> {

    private static final String PARAM_KEY_CHAN1 = "chan1";
    private static final String PARAM_KEY_CHAN2 = "chan2";

    private static final int DEFAULT_CHAN1 = 0;
    private static final int DEFAULT_CHAN2 = 0;

    public CellposeModel() {
    }

    @Override
    public String getName() {
        return "Cellpose";
    }

    @Override
    public String getEndpoint() {
        return "cellpose";
    }

    @Override
    ParameterList createParameterList() {
        ParameterList params = super.createParameterList()
                .addIntParameter(PARAM_KEY_CHAN1, "Channel 1", DEFAULT_CHAN1, null,
                        "Channel for cell (0: grayscale, 1: red, 2: green, 3: blue, ...)")
                .addIntParameter(PARAM_KEY_CHAN2, "Channel 2", DEFAULT_CHAN2, null,
                        "Channel for cell (0: None, 1: red, 2: green, 3: blue, ...)");
        return params;
    }

    @Override
    ParameterList createParameterListReset() {
        ParameterList params = super.createParameterListReset();
        return params;
    }

    @Override
    List<String> getPretraineOptions() {
        return Arrays.asList("cyto", "nuclei", "tissuenet", "livecell", "cyto2", "general",
                "CP", "CPx", "TN1", "TN2", "TN3", "LC1", "LC2", "LC3", "LC4");
    }

    @Override
    CellposeTrainBody.Builder getTrainBodyBuilder() {
        return CellposeTrainBody.builder();
    }

    @Override
    CellposeInferBody.Builder getInferBodyBuilder() {
        return CellposeInferBody.builder();
    }

    @Override
    CellposeResetBody.Builder getResetBodyBuilder() {
        return CellposeResetBody.builder();
    }

    @Override
    public String getRequestBodyStringTrain(String b64img, String b64lbl) {
        CellposeTrainBody body = ((CellposeTrainBody.Builder) getDefaultTrainBodyBuilder(b64img, b64lbl))
                .chan1(((IntParameter) getParameter(getParameterList(), PARAM_KEY_CHAN1)).getValue())
                .chan2(((IntParameter) getParameter(getParameterList(), PARAM_KEY_CHAN2)).getValue())
                .build();
        return GsonTools.getInstance().toJson(body);
    }

    @Override
    public String getRequestBodyStringInfer(String b64img) {
        CellposeInferBody body = ((CellposeInferBody.Builder) getDefaultInferBodyBuilder(b64img))
                .chan1(((IntParameter) getParameter(getParameterList(), PARAM_KEY_CHAN1)).getValue())
                .chan2(((IntParameter) getParameter(getParameterList(), PARAM_KEY_CHAN2)).getValue())
                .build();
        return GsonTools.getInstance().toJson(body);
    }

    @Override
    public String getRequestBodyStringReset() {
        CellposeResetBody body = ((CellposeResetBody.Builder) getDefaultResetBodyBuilder()).build();
        return GsonTools.getInstance().toJson(body);
    }

    public static class CellposeTrainBody extends CellsparseTrainBody {

        @SuppressWarnings("unused")
        private int chan1;

        @SuppressWarnings("unused")
        private int chan2;

        public CellposeTrainBody(Builder builder) {
            super(builder);
            this.chan1 = builder.chan1;
            this.chan2 = builder.chan2;
        }

        public static class Builder extends CellsparseTrainBody.Builder {
            private int chan1;
            private int chan2;

            public Builder() {
            }

            public Builder chan1(final int chan1) {
                this.chan1 = chan1;
                return this;
            }

            public Builder chan2(final int chan2) {
                this.chan2 = chan2;
                return this;
            }

            public CellposeTrainBody build() {
                return new CellposeTrainBody(this);
            }
        }

        public static Builder builder() {
            return new Builder();
        }

    }

    public static class CellposeInferBody extends CellsparseInferBody {

        @SuppressWarnings("unused")
        private int chan1;

        @SuppressWarnings("unused")
        private int chan2;

        public CellposeInferBody(Builder builder) {
            super(builder);
            this.chan1 = builder.chan1;
            this.chan2 = builder.chan2;
        }

        public static class Builder extends CellsparseInferBody.Builder {
            private int chan1;
            private int chan2;

            public Builder() {
            }

            public Builder chan1(final int chan1) {
                this.chan1 = chan1;
                return this;
            }

            public Builder chan2(final int chan2) {
                this.chan2 = chan2;
                return this;
            }

            public CellposeInferBody build() {
                return new CellposeInferBody(this);
            }
        }

        public static Builder builder() {
            return new Builder();
        }

    }

    public static class CellposeResetBody extends CellsparseResetBody {

        public CellposeResetBody(Builder builder) {
            super(builder);
        }

        public static class Builder extends CellsparseResetBody.Builder {

            public Builder() {
            }

            public CellposeResetBody build() {
                return new CellposeResetBody(this);
            }
        }

        public static Builder builder() {
            return new Builder();
        }

    }

}