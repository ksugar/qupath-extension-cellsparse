package org.elephant.cellsparse.lib.gui.viewer;

import java.util.Collection;

import qupath.lib.gui.viewer.RegionFilter;
import qupath.lib.images.ImageData;
import qupath.lib.objects.PathObject;
import qupath.lib.regions.ImageRegion;
import qupath.lib.regions.RegionRequest;

/**
 * Region filter for selected annotation objects.
 * 
 * @author Ko Sugawara
 */
public enum SelectedObjectsRegionFilter implements RegionFilter {

    /**
     * Accept all requests
     */
    EVERYWHERE,
    /**
     * Regions overlapping the ROIs of selected objects
     */
    SELECTED_OBJECTS;

    @Override
    public String toString() {
        switch (this) {
            case EVERYWHERE:
                return "Everywhere";
            case SELECTED_OBJECTS:
                return "Selected object ROI";
            default:
                return "Unknown";
        }
    }

    @Override
    public boolean test(ImageData<?> imageData, RegionRequest region) {
        switch (this) {
            case SELECTED_OBJECTS:
                var annotations = imageData.getHierarchy().getSelectionModel().getSelectedObjects();
                return overlapsObjects(annotations, region);
            default:
                return true;
        }
    }

    private static boolean overlapsObjects(Collection<? extends PathObject> pathObjects, ImageRegion region) {
        for (var pathObject : pathObjects) {
            var roi = pathObject.getROI();
            if (roi == null)
                continue;
            if (roi.isPoint()) {
                for (var p : roi.getAllPoints()) {
                    if (region.contains((int) p.getX(), (int) p.getY(), roi.getZ(), roi.getT()))
                        return true;
                }
            } else {
                var shape = roi.getShape();
                if (shape.intersects(region.getX(), region.getY(), region.getWidth(), region.getHeight()))
                    return true;
            }
        }
        return false;
    }

}
