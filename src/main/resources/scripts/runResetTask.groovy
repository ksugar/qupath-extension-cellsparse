import org.elephant.cellsparse.models.StarDistModel

def serverURL = "http://localhost:8000/"
def model = StarDistModel.builder()
                .modelname("test")
                .pretrained("2D_paper_dsb2018")
                .build()

model.reset(serverURL)