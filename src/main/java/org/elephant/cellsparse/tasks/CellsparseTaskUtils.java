package org.elephant.cellsparse.tasks;

import java.util.concurrent.Future;

import org.elephant.cellsparse.CellsparseCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.concurrent.Task;
import javafx.concurrent.Worker;

public class CellsparseTaskUtils {

    private static final Logger logger = LoggerFactory.getLogger(CellsparseTaskUtils.class);

    /**
     * Handle a change in task state.
     * 
     * @param task
     * @param newValue
     */
    public static void taskStateChange(CellsparseCommand command, Task<?> task, Worker.State newValue) {
        switch (newValue) {
            case SUCCEEDED:
                logger.debug("Task completed successfully");
                command.getCurrentTasks().remove(task);
                break;
            case CANCELLED:
                logger.info("Task cancelled");
                command.getCurrentTasks().remove(task);
                if (task.getException() != null) {
                    logger.warn("Task failed: {}", task, task.getException());
                    command.updateInfoTextWithError("Task cancelled with exception " + task.getException() +
                            "\nSee log for details.");
                } else {
                    command.updateInfoText("Task cancelled");
                }
                break;
            case FAILED:
                command.getCurrentTasks().remove(task);
                if (task.getException() != null) {
                    logger.warn("Task failed: {}", task, task.getException());
                    command.updateInfoTextWithError("Task failed with exception " + task.getException() +
                            "\nSee log for details.");
                } else {
                    command.updateInfoTextWithError("Task failed!");
                }
                break;
            case RUNNING:
                logger.trace("Task running");
                break;
            case SCHEDULED:
                logger.trace("Task scheduled");
                break;
            default:
                logger.debug("Task state changed to {}", newValue);
        }
    }

    /**
     * Submit a task.
     * 
     * @param task the task to submit
     * @return a Future representing pending completion of the task
     */
    public static Future<?> submitTask(CellsparseCommand command, Task<?> task) {
        command.getCurrentTasks().add(task);
        task.stateProperty().addListener((observable, oldValue, newValue) -> taskStateChange(command, task, newValue));
        return command.getPool().submit(task);
    }
}
