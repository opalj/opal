/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package apps.flashcards.model;

/**
 * General listener interface if the list of commands changes.
 * 
 * @author Michael Eichberg
 */
public interface CommandHistoryChangedListener {

    void historyChanged(CommandHistory history);

}
