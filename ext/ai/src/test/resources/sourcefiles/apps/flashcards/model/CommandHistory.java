/** License (BSD Style License):
 *  Copyright (c) 2010
 *  Software Engineering
 *  Department of Computer Science
 *  Technische Universität Darmstadt
 *  All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *  - Neither the name of the Software Engineering Group or Technische 
 *    Universität Darmstadt nor the names of its contributors may be used to 
 *    endorse or promote products derived from this software without specific 
 *    prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 *  AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 *  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 *  ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 *  LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 *  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 *  SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 *  INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 *  CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 *  ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *  POSSIBILITY OF SUCH DAMAGE.
 */
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
