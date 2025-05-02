package org.elephant.cellsparse;

import java.io.IOException;
import org.elephant.cellsparse.models.StarDistModel;

public class StarDistModelTest {

    public static void main(String[] args) throws IOException {
        StarDistModel model = StarDistModel.builder()
                .modelname("test")
                .trainpatch(64)
                .batchsize(4)
                .epochs(5)
                .steps(5)
                .lr(0.01)
                .minarea(5.0)
                .simplify_tol(0.5)
                .pretrained("2D_paper_dsb2018")
                .build();
        System.out.println(model.getRequestBodyStringReset());
        System.out.println(model.getRequestBodyStringInfer(null));
        System.out.println(model.getRequestBodyStringTrain(null, null));
    }
}