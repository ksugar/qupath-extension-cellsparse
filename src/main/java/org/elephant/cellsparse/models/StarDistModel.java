package org.elephant.cellsparse.models;

import java.util.Arrays;
import java.util.List;

import org.elephant.cellsparse.models.AbstractCellsparseModel.CellsparseInferBody;
import org.elephant.cellsparse.models.AbstractCellsparseModel.CellsparseResetBody;
import org.elephant.cellsparse.models.AbstractCellsparseModel.CellsparseTrainBody;

import qupath.lib.io.GsonTools;
import qupath.lib.plugins.parameters.ParameterList;

public class StarDistModel extends
        AbstractCellsparseModel<StarDistModel.StarDistTrainBody.Builder, StarDistModel.StarDistInferBody.Builder, StarDistModel.StarDistResetBody.Builder> {

    public StarDistModel() {
    }

    public StarDistModel(final StarDistModelBuilder builder) {
        super(builder);
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
    public StarDistInferBody.Builder getInferBodyBuilder() {
        return StarDistInferBody.builder();
    }

    @Override
    StarDistResetBody.Builder getResetBodyBuilder() {
        return StarDistResetBody.builder();
    }

    @Override
    public String getRequestBodyStringTrain(List<String> b64imgs, List<String> b64lbls) {
        StarDistTrainBody body = ((StarDistTrainBody.Builder) getDefaultTrainBodyBuilder(b64imgs, b64lbls))
                .build();
        return GsonTools.getInstance().toJson(body);
    }

    @Override
    public String getRequestBodyStringInfer(String b64img) {
        StarDistInferBody body = ((StarDistInferBody.Builder) getDefaultInferBodyBuilder().b64img(b64img))
                .build();
        return GsonTools.getInstance().toJson(body);
    }

    @Override
    public String getRequestBodyStringReset() {
        StarDistResetBody body = ((StarDistResetBody.Builder) getDefaultResetBodyBuilder()).build();
        return GsonTools.getInstance().toJson(body);
    }

    public static class StarDistModelBuilder
            extends AbstractCellsparseModelBuilder<StarDistModel, StarDistModelBuilder> {

        public StarDistModelBuilder() {
        }

        @Override
        protected StarDistModelBuilder self() {
            return this;
        }

        @Override
        public StarDistModel build() {
            return new StarDistModel(this);
        }
    }

    public static StarDistModelBuilder builder() {
        return new StarDistModelBuilder();
    }

    public static class StarDistTrainBody extends CellsparseTrainBody {

        public StarDistTrainBody(Builder builder) {
            super(builder);
        }

        public static class Builder extends CellsparseTrainBody.Builder {

            public Builder() {
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

        public StarDistInferBody(Builder builder) {
            super(builder);
        }

        public static class Builder extends CellsparseInferBody.Builder {

            public Builder() {
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