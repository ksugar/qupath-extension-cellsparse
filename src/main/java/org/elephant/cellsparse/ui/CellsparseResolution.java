package org.elephant.cellsparse.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import qupath.lib.common.GeneralTools;
import qupath.lib.images.ImageData;
import qupath.lib.images.servers.PixelCalibration;

public class CellsparseResolution {
    private static List<String> resolutionNames = Arrays.asList("Full", "Very high", "High", "Moderate", "Low",
            "Very low", "Extremely low");

    private final String name;
    final PixelCalibration cal;

    CellsparseResolution(String name, PixelCalibration cal) {
        this.name = name;
        this.cal = cal;
    }

    /**
     * Get the simple name for this resolution.
     * 
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     * Get the {@link PixelCalibration} used to apply this resolution.
     * 
     * @return
     */
    public PixelCalibration getPixelCalibration() {
        return cal;
    }

    @Override
    public String toString() {
        if (cal.hasPixelSizeMicrons())
            return String.format("%s (%.2f %s/px)", name, cal.getAveragedPixelSizeMicrons(),
                    GeneralTools.micrometerSymbol());
        else
            return String.format("%s (downsample = %.2f)", name, cal.getAveragedPixelSize().doubleValue());
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((cal == null) ? 0 : cal.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        CellsparseResolution other = (CellsparseResolution) obj;
        if (cal == null) {
            if (other.cal != null)
                return false;
        } else if (!cal.equals(other.cal))
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        return true;
    }

    /**
     * Get a list of default resolutions to show, derived from {@link PixelCalibration} objects.
     * 
     * @param imageData
     * @param selected
     * @return
     */
    public static List<CellsparseResolution> getDefaultResolutions(ImageData<?> imageData,
            CellsparseResolution selected) {
        var temp = new ArrayList<CellsparseResolution>();
        PixelCalibration cal = imageData.getServer().getPixelCalibration();

        int scale = 1;
        for (String name : resolutionNames) {
            var newResolution = new CellsparseResolution(name, cal.createScaledInstance(scale, scale, 1));
            if (Objects.equals(selected, newResolution))
                temp.add(selected);
            else
                temp.add(newResolution);
            scale *= 2;
        }
        if (selected == null)
            selected = temp.get(0);
        else if (!temp.contains(selected))
            temp.add(selected);

        return temp;
    }
}
