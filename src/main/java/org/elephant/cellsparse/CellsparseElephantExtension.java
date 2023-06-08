package org.elephant.cellsparse;

import org.controlsfx.control.action.Action;
import qupath.lib.gui.ActionTools;
import qupath.lib.gui.QuPathGUI;
import qupath.lib.gui.ActionTools.ActionDescription;
import qupath.lib.gui.ActionTools.ActionMenu;
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
		
		private CellsparseElephantCommands(QuPathGUI qupath) {
			actionTraining = qupath.createImageDataAction(imageData -> {
				CellsparseCommand(imageData, "http://localhost:8000/elephant/", true);
			});
			
			actionInference = qupath.createImageDataAction(imageData -> {
				CellsparseCommand(imageData, "http://localhost:8000/elephant/", false);
			});
		}
		
	}

}
