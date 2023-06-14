package org.elephant.cellsparse;

import org.controlsfx.control.action.Action;
import qupath.lib.gui.ActionTools;
import qupath.lib.gui.QuPathGUI;
import qupath.lib.gui.ActionTools.ActionDescription;
import qupath.lib.gui.ActionTools.ActionMenu;
import qupath.lib.gui.dialogs.Dialogs;
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
		
		@ActionMenu("StarDist>Reset")
		@ActionDescription("Reset StarDist model.")
		public final Action actionReset;
		
		@ActionMenu("StarDist>Server URL")
		@ActionDescription("Set API server URL.")
		public final Action actionSetServerURL;
		
		private String serverURL = "http://localhost:8000/stardist/";
		
		private CellsparseStarDistCommands(QuPathGUI qupath) {
			actionTraining = qupath.createImageDataAction(imageData -> {
				CellsparseCommand(imageData, serverURL, true, 1, 8, 200);
			});
			
			actionInference = qupath.createImageDataAction(imageData -> {
				CellsparseCommand(imageData, serverURL, false);
			});
			
			actionReset = new Action(e -> CellsparseResetCommand(serverURL + "reset/"));
			
			actionSetServerURL = new Action(event -> {
				String newURL = Dialogs.showInputDialog("Server URL", "Set API server URL", serverURL);
				if (newURL != null) {
					serverURL = newURL;
				}
			});
		}
		
	}

}
