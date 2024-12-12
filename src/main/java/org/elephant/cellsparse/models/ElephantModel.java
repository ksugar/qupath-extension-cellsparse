package org.elephant.cellsparse.models;

import java.util.Arrays;
import java.util.List;

import org.elephant.cellsparse.models.AbstractCellsparseModel.CellsparseInferBody;
import org.elephant.cellsparse.models.AbstractCellsparseModel.CellsparseResetBody;
import org.elephant.cellsparse.models.AbstractCellsparseModel.CellsparseTrainBody;

import qupath.lib.io.GsonTools;
import qupath.lib.plugins.parameters.ParameterList;

public class ElephantModel extends
        AbstractCellsparseModel<ElephantModel.ElephantTrainBody.Builder, ElephantModel.ElephantInferBody.Builder, ElephantModel.ElephantResetBody.Builder> {

    public ElephantModel() {
    }

    public ElephantModel(final ElephantModelBuilder builder) {
        super(builder);
    }

    @Override
    public String getName() {
        return "ELEPHANT";
    }

    @Override
    public String getEndpoint() {
        return "elephant";
    }

    @Override
    ParameterList createParameterList() {
        ParameterList params = super.createParameterList();

        return params;
    }

    @Override
    ParameterList createParameterListReset() {
        ParameterList params = super.createParameterListReset();
        return params;
    }

    @Override
    List<String> getPretraineOptions() {
        return Arrays.asList("Versatile");
    }

    @Override
    ElephantTrainBody.Builder getTrainBodyBuilder() {
        return ElephantTrainBody.builder();
    }

    @Override
    public ElephantInferBody.Builder getInferBodyBuilder() {
        return ElephantInferBody.builder();
    }

    @Override
    ElephantResetBody.Builder getResetBodyBuilder() {
        return ElephantResetBody.builder();
    }

    @Override
    public String getRequestBodyStringTrain(List<String> b64imgs, List<String> b64lbls) {
        ElephantTrainBody body = ((ElephantTrainBody.Builder) getDefaultTrainBodyBuilder(b64imgs, b64lbls)).build();
        return GsonTools.getInstance().toJson(body);
    }

    @Override
    public String getRequestBodyStringInfer(String b64img) {
        ElephantInferBody body = ((ElephantInferBody.Builder) getDefaultInferBodyBuilder().b64img(b64img)).build();
        return GsonTools.getInstance().toJson(body);
    }

    @Override
    public String getRequestBodyStringReset() {
        ElephantResetBody body = ((ElephantResetBody.Builder) getDefaultResetBodyBuilder()).build();
        return GsonTools.getInstance().toJson(body);
    }

    public static class ElephantModelBuilder
            extends AbstractCellsparseModelBuilder<ElephantModel, ElephantModelBuilder> {

        public ElephantModelBuilder() {
        }

        @Override
        protected ElephantModelBuilder self() {
            return this;
        }

        @Override
        public ElephantModel build() {
            return new ElephantModel(this);
        }
    }

    public static ElephantModelBuilder builder() {
        return new ElephantModelBuilder();
    }

    public static class ElephantTrainBody extends CellsparseTrainBody {

        public ElephantTrainBody(Builder builder) {
            super(builder);
        }

        public static class Builder extends CellsparseTrainBody.Builder {
            public Builder() {
            }

            public ElephantTrainBody build() {
                return new ElephantTrainBody(this);
            }
        }

        public static Builder builder() {
            return new Builder();
        }

    }

    public static class ElephantInferBody extends CellsparseInferBody {

        public ElephantInferBody(Builder builder) {
            super(builder);
        }

        public static class Builder extends CellsparseInferBody.Builder {
            public Builder() {
            }

            public ElephantInferBody build() {
                return new ElephantInferBody(this);
            }
        }

        public static Builder builder() {
            return new Builder();
        }

    }

    public static class ElephantResetBody extends CellsparseResetBody {

        public ElephantResetBody(Builder builder) {
            super(builder);
        }

        public static class Builder extends CellsparseResetBody.Builder {
            public Builder() {
            }

            public ElephantResetBody build() {
                return new ElephantResetBody(this);
            }
        }

        public static Builder builder() {
            return new Builder();
        }

    }

}