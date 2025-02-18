package org.elephant.cellsparse;

import java.util.ResourceBundle;

import org.controlsfx.control.action.Action;
import qupath.lib.gui.actions.ActionTools;
import qupath.lib.gui.actions.annotations.ActionConfig;
import qupath.lib.gui.actions.annotations.ActionMenu;
import qupath.lib.common.Version;
import qupath.lib.gui.QuPathGUI;
import qupath.lib.gui.extensions.QuPathExtension;

public class CellsparseExtension implements QuPathExtension {

    private static final ResourceBundle resources = ResourceBundle.getBundle("org.elephant.cellsparse.strings");
    private static final Version EXTENSION_QUPATH_VERSION = Version.parse("v0.5.0");
    private static boolean alreadyInstalled = false;

    @Override
    public void installExtension(QuPathGUI qupath) {
        if (!alreadyInstalled) {
            qupath.installActions(ActionTools.getAnnotatedActions(new CellsparseCommands(qupath)));
            alreadyInstalled = true;
        }
    }

    @Override
    public String getName() {
        return resources.getString("Extension.name");
    }

    @Override
    public String getDescription() {
        return resources.getString("Extension.description");
    }

    @Override
    public Version getQuPathVersion() {
        return EXTENSION_QUPATH_VERSION;
    }

    @ActionMenu("Extensions")
    public class CellsparseCommands {

        @ActionConfig(value = "Action.Cellsparse.mainAction", bundle = "org.elephant.cellsparse.strings")
        public final Action mainAction;

        private CellsparseCommands(QuPathGUI qupath) {
            var commandCellsparse = new CellsparseCommand(qupath);
            mainAction = qupath.createImageDataAction(imageData -> {
                commandCellsparse.run();
            });
        }

    }
}
