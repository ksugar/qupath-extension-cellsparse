package org.elephant.cellsparse.models;

import java.util.Arrays;
import java.util.List;

import org.elephant.cellsparse.models.AbstractCellsparseModel.CellsparseInferBody;
import org.elephant.cellsparse.models.AbstractCellsparseModel.CellsparseResetBody;
import org.elephant.cellsparse.models.AbstractCellsparseModel.CellsparseTrainBody;

import qupath.lib.io.GsonTools;
import qupath.lib.plugins.parameters.IntParameter;
import qupath.lib.plugins.parameters.ParameterList;

public class StarDistModel extends
        AbstractCellsparseModel<StarDistModel.StarDistTrainBody.Builder, StarDistModel.StarDistInferBody.Builder, StarDistModel.StarDistResetBody.Builder> {

    private static final String PARAM_KEY_N_CHANNELS_IN = "n_channels_in";

    private static final int DEFAULT_N_CHANNELS_IN = 1;

    public StarDistModel() {
    }

    @Override
    public String getName() {
        return "StarDist";
    }

    @Override
    public String getEndpoint() {
        return "stardist";
    }

    @Override
    ParameterList createParameterList() {
        ParameterList params = super.createParameterList()
                .addIntParameter(PARAM_KEY_N_CHANNELS_IN, "Number of channels", DEFAULT_N_CHANNELS_IN, null,
                        "Number of channels of input image");

        return params;
    }

    @Override
    ParameterList createParameterListReset() {
        ParameterList params = super.createParameterListReset();
        return params;
    }

    @Override
    List<String> getPretraineOptions() {
        return Arrays.asList("2D_versatile_fluo", "2D_versatile_he", "2D_paper_dsb2018", "2D_demo");
    }

    @Override
    StarDistTrainBody.Builder getTrainBodyBuilder() {
        return StarDistTrainBody.builder();
    }

    @Override
    StarDistInferBody.Builder getInferBodyBuilder() {
        return StarDistInferBody.builder();
    }

    @Override
    StarDistResetBody.Builder getResetBodyBuilder() {
        return StarDistResetBody.builder();
    }

    @Override
    public String getRequestBodyStringTrain(String b64img, String b64lbl) {
        StarDistTrainBody body = ((StarDistTrainBody.Builder) getDefaultTrainBodyBuilder(b64img, b64lbl))
                .n_channels_in(((IntParameter) getParameter(getParameterList(), PARAM_KEY_N_CHANNELS_IN)).getValue())
                .build();
        return GsonTools.getInstance().toJson(body);
    }

    @Override
    public String getRequestBodyStringInfer(String b64img) {
        StarDistInferBody body = ((StarDistInferBody.Builder) getDefaultInferBodyBuilder(b64img))
                .n_channels_in(((IntParameter) getParameter(getParameterList(), PARAM_KEY_N_CHANNELS_IN)).getValue())
                .build();
        return GsonTools.getInstance().toJson(body);
    }

    @Override
    public String getRequestBodyStringReset() {
        StarDistResetBody body = ((StarDistResetBody.Builder) getDefaultResetBodyBuilder()).build();
        return GsonTools.getInstance().toJson(body);
    }

    public static class StarDistTrainBody extends CellsparseTrainBody {

        @SuppressWarnings("unused")
        private int n_channels_in;

        public StarDistTrainBody(Builder builder) {
            super(builder);
            this.n_channels_in = builder.n_channels_in;
        }

        public static class Builder extends CellsparseTrainBody.Builder {
            private int n_channels_in;

            public Builder() {
            }

            public Builder n_channels_in(final int n_channels_in) {
                this.n_channels_in = n_channels_in;
                return this;
            }

            public StarDistTrainBody build() {
                return new StarDistTrainBody(this);
            }
        }

        public static Builder builder() {
            return new Builder();
        }

    }

    public static class StarDistInferBody extends CellsparseInferBody {

        @SuppressWarnings("unused")
        private int n_channels_in;

        public StarDistInferBody(Builder builder) {
            super(builder);
            this.n_channels_in = builder.n_channels_in;
        }

        public static class Builder extends CellsparseInferBody.Builder {
            private int n_channels_in;

            public Builder() {
            }

            public Builder n_channels_in(final int n_channels_in) {
                this.n_channels_in = n_channels_in;
                return this;
            }

            public StarDistInferBody build() {
                return new StarDistInferBody(this);
            }
        }

        public static Builder builder() {
            return new Builder();
        }

    }

    public static class StarDistResetBody extends CellsparseResetBody {

        public StarDistResetBody(Builder builder) {
            super(builder);
        }

        public static class Builder extends CellsparseResetBody.Builder {
            public Builder() {
            }

            public StarDistResetBody build() {
                return new StarDistResetBody(this);
            }
        }

        public static Builder builder() {
            return new Builder();
        }

    }

}