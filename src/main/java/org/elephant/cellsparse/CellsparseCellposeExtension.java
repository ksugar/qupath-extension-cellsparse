package org.elephant.cellsparse;

import org.controlsfx.control.action.Action;
import qupath.lib.gui.ActionTools;
import qupath.lib.gui.QuPathGUI;
import qupath.lib.gui.ActionTools.ActionDescription;
import qupath.lib.gui.ActionTools.ActionMenu;
import qupath.lib.gui.dialogs.Dialogs;
import qupath.lib.gui.extensions.QuPathExtension;

public class CellsparseCellposeExtension implements QuPathExtension {

	@Override
	public void installExtension(QuPathGUI qupath) {
		qupath.installActions(ActionTools.getAnnotatedActions(new CellsparseCellposeCommands(qupath)));
	}

	@Override
	public String getName() {
		return "Cellsparse Cellpose";
	}

	@Override
	public String getDescription() {
		return "Cellpose with sparse annotation";
	}
	
	@ActionMenu("Extensions>Cellsparse")
	public class CellsparseCellposeCommands extends AbstractCellsparseCommands {
		
		@ActionMenu("Cellpose>Training")
		@ActionDescription("Cellpose training with sparse annotation.")
		public final Action actionTraining;
		
		@ActionMenu("Cellpose>Inference")
		@ActionDescription("Cellpose inference.")
		public final Action actionInference;
		
		@ActionMenu("Cellpose>Reset")
		@ActionDescription("Reset Cellpose model.")
		public final Action actionReset;
		
		@ActionMenu("Cellpose>Server URL")
		@ActionDescription("Set API server URL.")
		public final Action actionSetServerURL;
		
		private String serverURL = "http://localhost:8000/cellpose/";
		
		private CellsparseCellposeCommands(QuPathGUI qupath) {
			actionTraining = qupath.createImageDataAction(imageData -> {
				CellsparseCommand(imageData, serverURL, true, 5, 8, 200);
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
