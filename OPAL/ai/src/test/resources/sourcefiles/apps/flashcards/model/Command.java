/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package apps.flashcards.model;

/**
 * Implementation of the command interface of the command pattern.
 * 
 * Calls to the command's execute and unexecute method have to be alternating and always have to
 * start with execute:
 * 
 * <pre>
 * execute -&gt; unexecute -&gt; execute -&gt; unexecute -&gt;....
 * </pre>
 * 
 * Furthermore, it is an error to call a command's execute method after another command's execute
 * method was executed. Calling the unexecute method is only supported directly after execute was
 * called and no other command is effective (if another command was executed and unexecuted in the
 * meantime everything is ok).
 * 
 * @author Michael Eichberg
 */
public interface Command {

    /**
     * Executes the effect of this command.
     */
    void execute();

    /**
     * Undos the effect caused by this command.
     */
    void unexecute();

}
