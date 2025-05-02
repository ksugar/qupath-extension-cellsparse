import org.elephant.cellsparse.models.StarDistModel

def serverURL = "http://localhost:8000/"
def model = StarDistModel.builder()
                .modelname("test")
                .minarea(5.0)
                .simplify_tol(0.5)
                .build()
def regionRequest = RegionRequest.createInstance(getCurrentServerPath(), 0.8, 100, 100, 100, 100)
model.infer(getCurrentViewer(), serverURL, regionRequest)