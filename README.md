# QuPath extension Cellsparse

Train a deep learning model for cell segmentation in a few minutes from scratch.

![](https://github.com/ksugar/qupath-extension-cellsparse/releases/download/assets/cellsparse-demo.gif)

This is a QuPath extension for [Cellsparse API](https://github.com/ksugar/cellsparse-api).

This is a part of the following paper. Please [cite](#citation) it when you use this project.

- Sugawara, K. [*Training deep learning models for cell image segmentation with sparse annotations.*](https://biorxiv.org/cgi/content/short/2023.06.13.544786v1) bioRxiv 2023. doi:10.1101/2023.06.13.544786

## Install & setup

Drag and drop [the extension file](https://github.com/ksugar/qupath-extension-cellsparse/releases/download/v0.2.0/qupath-extension-cellsparse-0.2.0.jar) to [QuPath](https://qupath.github.io) and restart it.

Set up the server following the instructions in the link below.

[https://github.com/ksugar/cellsparse-api](https://github.com/ksugar/cellsparse-api)

If you use [SAM API](https://github.com/ksugar/samapi) together with this API, you need to use different ports for them.

For example, the following command will launch [Cellsparse API](https://github.com/ksugar/cellsparse-api) on the port `8000` (default).

```bash
(cellsparse-api)$ uvicorn cellsparse_api.main:app
INFO:     Started server process [26240]
INFO:     Waiting for application startup.
INFO:     Application startup complete.
INFO:     Uvicorn running on http://127.0.0.1:8000 (Press CTRL+C to quit)
```

Then, you can launch [SAM API](https://github.com/ksugar/samapi) on the port `18000` as follows.

```bash
(samapi) samapi.main:app --port 18000
INFO:     Started server process [12060]
INFO:     Waiting for application startup.
INFO:     Application startup complete.
INFO:     Uvicorn running on http://127.0.0.1:18000 (Press CTRL+C to quit)
```

On QuPath, set the server URL for SAM (`Extensions` > `SAM` > `Server URL`) to `http://localhost:18000/sam/`.

![](https://github.com/ksugar/qupath-extension-sam/releases/download/assets/qupath-sam-extension-server-url.png)

## Usage

Currently, [StarDist](https://stardist.net/index.html), [Cellpose](https://cellpose.readthedocs.io/en/latest/) and [ELEPHANT](https://elephant-track.github.io/) are available.

### `Extensions` > `Cellsparse` > `[Algorithm]` > `Training`

Train a model with the annotations in the current image.

In the training, foreground and background annotations need to be assigned to the annotation classes with the specific names, `Foreground` and `Background`, respectively. These names are case-sensitive.

![](https://github.com/ksugar/qupath-extension-cellsparse/releases/download/assets/qupath-extension-cellsparse-class-names.png)

### `Extensions` > `Cellsparse` > `[Algorithm]` > `Inference`

Run inference with the latest model.

### `Extensions` > `Cellsparse` > `[Algorithm]` > `Reset`

Reset a model (randomly initialized).

### `Extensions` > `Cellsparse` > `[Algorithm]` > `Server URL`

Set the server URL for [Cellsparse API](https://github.com/ksugar/cellsparse-api).

## Citation

Please cite my paper on [bioRxiv](https://biorxiv.org/cgi/content/short/2023.06.13.544786v1).

```.bib
@article {Sugawara2023.06.13.544786,
	author = {Ko Sugawara},
	title = {Training deep learning models for cell image segmentation with sparse annotations},
	elocation-id = {2023.06.13.544786},
	year = {2023},
	doi = {10.1101/2023.06.13.544786},
	publisher = {Cold Spring Harbor Laboratory},
	abstract = {Deep learning is becoming more prominent in cell image analysis. However, collecting the annotated data required to train efficient deep-learning models remains a major obstacle. I demonstrate that functional performance can be achieved even with sparsely annotated data. Furthermore, I show that the selection of sparse cell annotations significantly impacts performance. I modified Cellpose and StarDist to enable training with sparsely annotated data and evaluated them in conjunction with ELEPHANT, a cell tracking algorithm that internally uses U-Net based cell segmentation. These results illustrate that sparse annotation is a generally effective strategy in deep learning-based cell image segmentation. Finally, I demonstrate that with the help of the Segment Anything Model (SAM), it is feasible to build an effective deep learning model of cell image segmentation from scratch just in a few minutes.Competing Interest StatementKS is employed part-time by LPIXEL Inc.},
	URL = {https://www.biorxiv.org/content/early/2023/06/13/2023.06.13.544786},
	eprint = {https://www.biorxiv.org/content/early/2023/06/13/2023.06.13.544786.full.pdf},
	journal = {bioRxiv}
}
```