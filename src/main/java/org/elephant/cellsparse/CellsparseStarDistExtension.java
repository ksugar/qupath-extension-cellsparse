package org.elephant.cellsparse;

import org.controlsfx.control.action.Action;
import qupath.lib.gui.ActionTools;
import qupath.lib.gui.QuPathGUI;
import qupath.lib.gui.ActionTools.ActionDescription;
import qupath.lib.gui.ActionTools.ActionMenu;
import qupath.lib.gui.extensions.QuPathExtension;

public class CellsparseStarDistExtension implements QuPathExtension {

	@Override
	public void installExtension(QuPathGUI qupath) {
		qupath.installActions(ActionTools.getAnnotatedActions(new CellsparseStarDistCommands(qupath)));
	}

	@Override
	public String getName() {
		return "Cellsparse StarDist";
	}

	@Override
	public String getDescription() {
		return "StarDist with sparse annotation";
	}
	
	@ActionMenu("Extensions>Cellsparse")
	public class CellsparseStarDistCommands extends AbstractCellsparseCommands {
		
		@ActionMenu("StarDist>Training")
		@ActionDescription("StarDist training with sparse annotation.")
		public final Action actionTraining;
		
		@ActionMenu("StarDist>Inference")
		@ActionDescription("StarDist inference.")
		public final Action actionInference;
		
		private CellsparseStarDistCommands(QuPathGUI qupath) {
			actionTraining = qupath.createImageDataAction(imageData -> {
				CellsparseCommand(imageData, "http://localhost:8000/stardist/", true);
			});
			
			actionInference = qupath.createImageDataAction(imageData -> {
				CellsparseCommand(imageData, "http://localhost:8000/stardist/", false);
			});
		}
		
	}

}
