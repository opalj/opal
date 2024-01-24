/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package apps.flashcards.model;

import java.util.Vector;

/**
 * Manages a list of commands.
 * 
 * @author Michael Eichberg
 */
public class CommandHistory {

    // We use a Vector's "setSize" method to get rid of redoable commands that are no longer
    // applicable.
    private final Vector<Command> commands = new Vector<Command>();

    private int commandIndex = -1; // index of the last executed command

    private CommandHistoryChangedListener listener = null;

    public void setCommandHistoryChangedListener(CommandHistoryChangedListener listener) {

        assert this.listener == null;

        this.listener = listener;
    }

    protected void notifyListener() {

        if (listener != null) {
            listener.historyChanged(this);
        }
    }

    public void execute(Command command) {

        commandIndex++;
        commands.setSize(commandIndex); // implicitly deletes all potentially existing redoable
        // commands
        commands.add(commandIndex, command);

        // executed new command
        command.execute();

        notifyListener();
    }

    public void undo() {

        assert commandIndex >= 0;

        commands.get(commandIndex).unexecute();
        commandIndex--;

        notifyListener();
    }

    public void redo() {

        assert commandIndex + 1 < commands.size();

        commandIndex++;
        commands.get(commandIndex).execute();

        notifyListener();
    }

    public int undoableCommandsCount() {

        return commandIndex + 1;
    }

    public int redoableCommandsCount() {

        return commands.size() - (commandIndex + 1);
    }
}
