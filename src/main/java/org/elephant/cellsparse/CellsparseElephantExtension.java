package org.elephant.cellsparse;

import org.controlsfx.control.action.Action;
import qupath.lib.gui.ActionTools;
import qupath.lib.gui.QuPathGUI;
import qupath.lib.gui.ActionTools.ActionDescription;
import qupath.lib.gui.ActionTools.ActionMenu;
import qupath.lib.gui.dialogs.Dialogs;
import qupath.lib.gui.extensions.QuPathExtension;

public class CellsparseElephantExtension implements QuPathExtension {

	@Override
	public void installExtension(QuPathGUI qupath) {
		qupath.installActions(ActionTools.getAnnotatedActions(new CellsparseElephantCommands(qupath)));
	}

	@Override
	public String getName() {
		return "Cellsparse ELEPHANT";
	}

	@Override
	public String getDescription() {
		return "ELEPHANT with sparse annotation";
	}
	
	@ActionMenu("Extensions>Cellsparse")
	public class CellsparseElephantCommands extends AbstractCellsparseCommands {
		
		@ActionMenu("ELEPHANT>Training")
		@ActionDescription("ELEPHANT training with sparse annotation.")
		public final Action actionTraining;
		
		@ActionMenu("ELEPHANT>Inference")
		@ActionDescription("ELEPHANT inference.")
		public final Action actionInference;
		
		@ActionMenu("ELEPHANT>Reset")
		@ActionDescription("Reset ELEPHANT model.")
		public final Action actionReset;
		
		@ActionMenu("ELEPHANT>Server URL")
		@ActionDescription("Set API server URL.")
		public final Action actionSetServerURL;
		
		private String serverURL = "http://localhost:8000/elephant/";
		
		private CellsparseElephantCommands(QuPathGUI qupath) {
			actionTraining = qupath.createImageDataAction(imageData -> {
				CellsparseCommand(imageData, serverURL, true, 1, 8, 200);
			});
			
			actionInference = qupath.createImageDataAction(imageData -> {
				CellsparseCommand(imageData, serverURL, false);
			});
			
			actionReset = new Action(event -> CellsparseResetCommand(serverURL + "reset/"));
			
			actionSetServerURL = new Action(event -> {
				String newURL = Dialogs.showInputDialog("Server URL", "Set API server URL", serverURL);
				if (newURL != null) {
					serverURL = newURL;
				}
			});
		}
		
	}

}
