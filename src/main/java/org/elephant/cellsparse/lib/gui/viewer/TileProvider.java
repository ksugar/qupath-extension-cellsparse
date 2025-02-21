package org.elephant.cellsparse.lib.gui.viewer;

import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import qupath.lib.gui.viewer.QuPathViewer;
import qupath.lib.gui.viewer.RegionFilter;
import qupath.lib.images.servers.ColorTransforms;
import qupath.lib.images.servers.ColorTransforms.ColorTransform;
import qupath.lib.images.servers.PixelCalibration;
import qupath.lib.images.servers.TileRequest;
import qupath.lib.objects.PathObject;
import qupath.lib.regions.RegionRequest;
import qupath.lib.roi.GeometryTools;
import qupath.lib.roi.RoiTools;
import qupath.lib.roi.interfaces.ROI;
import qupath.opencv.ops.ImageDataOp;
import qupath.opencv.ops.ImageDataServer;
import qupath.opencv.ops.ImageOps;

public class TileProvider {
    private final QuPathViewer viewer;
    private final PixelCalibration pixelCalibration;
    private final double downsample;
    private final int tileWidth;
    private final int tileHeight;
    private final int pad;
    private final ImageDataServer<BufferedImage> opServer;
    private final Collection<PathObject> selectedAnnotations;
    private final List<PathObject> pathObjectsToRemove;
    private final RegionFilter regionFilter;
    private final ROI union;
    private final RegionRequest unionRegionRequest;
    private final Collection<TileRequest> tiles;

    public TileProvider(Builder builder) {
        this.viewer = builder.viewer;
        this.pixelCalibration = builder.pixelCalibration;
        this.downsample = pixelCalibration.getAveragedPixelSize().doubleValue()
                / viewer.getServer().getPixelCalibration().getAveragedPixelSize().doubleValue();
        this.tileWidth = builder.tileWidth;
        this.tileHeight = builder.tileHeight;
        this.pad = builder.pad;
        final ColorTransform[] colorTransforms = IntStream
                .range(0, viewer.getServer().nChannels())
                .mapToObj(c -> ColorTransforms.createChannelExtractor(c))
                .toArray(ColorTransform[]::new).clone();
        final ImageDataOp op = ImageOps.buildImageDataOp(colorTransforms);
        this.opServer = ImageOps.buildServer(viewer.getImageData(), op,
                pixelCalibration, tileWidth - pad * 2, tileHeight - pad * 2);
        this.selectedAnnotations = viewer.getHierarchy()
                .getSelectionModel()
                .getSelectedObjects();
        this.pathObjectsToRemove = viewer.getHierarchy()
                .getAnnotationObjects()
                .stream()
                .filter(pathObject -> pathObject.getPathClass() == null)
                .toList();
        this.regionFilter = builder.regionFilter;
        this.union = regionFilter == SelectedObjectsRegionFilter.EVERYWHERE || selectedAnnotations.isEmpty()
                ? null
                : RoiTools.union(selectedAnnotations.stream().map(it -> it.getROI()).collect(Collectors.toList()));
        this.unionRegionRequest = createRegionRequest(opServer, union);
        this.tiles = getTileRequests(unionRegionRequest, union);
    }

    private RegionRequest createRegionRequest(ImageDataServer<BufferedImage> opServer, ROI union) {
        if (union == null) {
            return RegionRequest.createInstance(opServer);
        } else {
            return RegionRequest.createInstance(
                    opServer.getPath(),
                    opServer.getDownsampleForResolution(0),
                    union);
        }
    }

    private Collection<TileRequest> getTileRequests(RegionRequest regionRequest, ROI union) {
        return viewer.getServer().getTileRequestManager()
                .getTileRequests(regionRequest)
                .stream()
                .filter(t -> union == null || union.getGeometry()
                        .intersects(GeometryTools.createRectangle(t.getImageX(), t.getImageY(),
                                t.getImageWidth(), t.getImageHeight())))
                .collect(Collectors.toList());
    }

    private RegionRequest getRegionRequestPadded(RegionRequest regionRequest, double downsample, int pad) {
        var server = viewer.getServer();
        int x1 = (int) Math.max(0, Math.round(regionRequest.getX() - downsample * pad));
        int y1 = (int) Math.max(0, Math.round(regionRequest.getY() - downsample * pad));
        int x2 = (int) Math.min(server.getWidth(), Math.round(regionRequest.getMaxX() + downsample * pad));
        int y2 = (int) Math.min(server.getHeight(), Math.round(regionRequest.getMaxY() + downsample * pad));
        return RegionRequest.createInstance(server.getPath(), downsample, x1, y1, x2 - x1, y2 - y1,
                regionRequest.getZ(), regionRequest.getT());
    }

    public Collection<RegionRequest> getRegionRequestsPadded() {
        return tiles.stream().map(tile -> {
            var request = tile.getRegionRequest();
            var requestPadded = getRegionRequestPadded(request, downsample, pad);
            return requestPadded;
        }).collect(Collectors.toList());
    }

    public ImageDataServer<BufferedImage> getOpServer() {
        return opServer;
    }

    public PixelCalibration getPixelCalibration() {
        return pixelCalibration;
    }

    public double getDownsample() {
        return downsample;
    }

    public int getPad() {
        return pad;
    }

    public int getTileWidth() {
        return tileWidth;
    }

    public int getTileHeight() {
        return tileHeight;
    }

    public Collection<PathObject> getSelectedAnnotations() {
        return selectedAnnotations;
    }

    public List<PathObject> getPathObjectsToRemove() {
        return pathObjectsToRemove;
    }

    public RegionFilter getRegionFilter() {
        return regionFilter;
    }

    public ROI getUnion() {
        return union;
    }

    public RegionRequest getUnionRegionRequest() {
        return unionRegionRequest;
    }

    public Collection<TileRequest> getTiles() {
        return tiles;
    }

    public static Builder builder(QuPathViewer viewer) {
        return new Builder(viewer);
    }

    public static class Builder {
        private static final int DEFAULT_TILE_WIDTH = 512;
        private static final int DEFAULT_TILE_HEIGHT = 512;
        private static final int DEFAULT_PAD = 32;
        private static final RegionFilter DEFAULT_REGION_FILTER = SelectedObjectsRegionFilter.EVERYWHERE;

        private final QuPathViewer viewer;
        private PixelCalibration pixelCalibration;
        private int tileWidth = DEFAULT_TILE_WIDTH;
        private int tileHeight = DEFAULT_TILE_HEIGHT;
        private int pad = DEFAULT_PAD;
        private RegionFilter regionFilter = DEFAULT_REGION_FILTER;

        public Builder(QuPathViewer viewer) {
            this.viewer = viewer;
        }

        public Builder pixelCalibration(PixelCalibration pixelCalibration) {
            this.pixelCalibration = pixelCalibration;
            return this;
        }

        public Builder tileWidth(int tileWidth) {
            this.tileWidth = tileWidth;
            return this;
        }

        public Builder tileHeight(int tileHeight) {
            this.tileHeight = tileHeight;
            return this;
        }

        public Builder pad(int pad) {
            this.pad = pad;
            return this;
        }

        public Builder regionFilter(RegionFilter regionFilter) {
            this.regionFilter = regionFilter;
            return this;
        }

        public TileProvider build() {
            return new TileProvider(this);
        }
    }
}
