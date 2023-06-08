package org.elephant.cellsparse;

import org.controlsfx.control.action.Action;
import qupath.lib.gui.ActionTools;
import qupath.lib.gui.QuPathGUI;
import qupath.lib.gui.ActionTools.ActionDescription;
import qupath.lib.gui.ActionTools.ActionMenu;
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
		
		private CellsparseCellposeCommands(QuPathGUI qupath) {
			actionTraining = qupath.createImageDataAction(imageData -> {
				CellsparseCommand(imageData, "http://localhost:8000/cellpose/", true);
			});
			
			actionInference = qupath.createImageDataAction(imageData -> {
				CellsparseCommand(imageData, "http://localhost:8000/cellpose/", false);
			});
		}
		
	}

}
