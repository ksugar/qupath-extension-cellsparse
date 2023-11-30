package org.elephant.cellsparse;

import org.controlsfx.control.action.Action;
import qupath.lib.gui.ActionTools;
import qupath.lib.gui.ActionTools.ActionDescription;
import qupath.lib.gui.ActionTools.ActionMenu;
import qupath.lib.gui.QuPathGUI;
import qupath.lib.gui.extensions.QuPathExtension;

public class CellsparseExtension implements QuPathExtension {

    @Override
    public void installExtension(QuPathGUI qupath) {
        qupath.installActions(ActionTools.getAnnotatedActions(new CellsparseCommands(qupath)));
    }

    @Override
    public String getName() {
        return "Cellsparse";
    }

    @Override
    public String getDescription() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getDescription'");
    }

    @ActionMenu("Extensions")
    public class CellsparseCommands {

        @ActionMenu("Cellsparse")
        @ActionDescription("Cellsparse training with sparse annotation.")
        public final Action actionTraining;

        private CellsparseCommands(QuPathGUI qupath) {
            var commandCellsparse = new CellsparseCommand(qupath);
            actionTraining = qupath.createImageDataAction(imageData -> {
                commandCellsparse.run();
            });
        }

    }
}
