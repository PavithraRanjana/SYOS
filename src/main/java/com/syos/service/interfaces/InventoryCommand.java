package com.syos.service.interfaces;

import com.syos.exceptions.InventoryException;

/**
 * Command Pattern interface for inventory operations.
 * Allows encapsulation of inventory requests as objects, enabling:
 * - Queuing of operations
 * - Undo functionality
 * - Logging of operations
 * - Transactional operations
 */
public interface InventoryCommand {

    /**
     * Executes the inventory command.
     *
     * @return CommandResult containing execution details
     * @throws InventoryException if execution fails
     */
    CommandResult execute() throws InventoryException;

    /**
     * Undoes the inventory command if possible.
     *
     * @return CommandResult containing undo details
     * @throws InventoryException if undo fails
     */
    CommandResult undo() throws InventoryException;

    /**
     * Checks if this command can be undone.
     *
     * @return true if command supports undo
     */
    boolean canUndo();

    /**
     * Gets a description of what this command does.
     *
     * @return Human-readable description
     */
    String getDescription();

    /**
     * Gets the command type for logging/categorization.
     *
     * @return Command type
     */
    CommandType getCommandType();

    /**
     * Result of command execution.
     */
    class CommandResult {
        private final boolean success;
        private final String message;
        private final Object result;

        public CommandResult(boolean success, String message, Object result) {
            this.success = success;
            this.message = message;
            this.result = result;
        }

        public static CommandResult success(String message, Object result) {
            return new CommandResult(true, message, result);
        }

        public static CommandResult failure(String message) {
            return new CommandResult(false, message, null);
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public Object getResult() { return result; }

        @Override
        public String toString() {
            return String.format("CommandResult{success=%s, message='%s'}", success, message);
        }
    }

    /**
     * Types of inventory commands.
     */
    enum CommandType {
        ADD_BATCH,
        REMOVE_BATCH,
        ISSUE_STOCK,
        ADD_PRODUCT,
        UPDATE_STOCK
    }
}