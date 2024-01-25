/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package apps.flashcards.model;

import javax.swing.ListModel;
import javax.swing.event.ListDataListener;

/**
 * A flashcards list manages a list of flashcards.
 * 
 * @author Michael Eichberg
 */
@SuppressWarnings("rawtypes")
public interface FlashcardSeries extends ListModel {

    /**
     * Adds the given listener.
     * <p>
     * Registering the same listener twice is not supported.
     * </p>
     */
    void addListDataListener(ListDataListener l);

    /**
     * Removes the given, registered listener.
     * <p>
     * It is an error to request removing a listener that is not (no longer) registered.
     * </p>
     */
    void removeListDataListener(ListDataListener l);

    /**
     * Adds the given card to this series; a card is always added as the first card to the core data
     * model. The card must not belong to any other flashcards list.
     */
    Command createAddCardCommand(Flashcard flashcard);

    /**
     * Removes the flashcards with the indices.
     * <p>
     * A flashcard that was removed cannot be added to another series, because it may be
     * reintegrated if the user undos the remove operation. To add a flashcard to another series,
     * clone the card.
     * </p>
     * 
     * @param indices
     *            the indices of the flashcards that will be removed. The array has to be sorted in
     *            ascending order; no index has to appear twice.
     */
    Command createRemoveCardsCommand(int[] indices);

    int getNextCreationID();

    void setNextCreationID(int creationID);

    /**
     * @return The card with the given index.
     */
    Flashcard getElementAt(int index) throws IndexOutOfBoundsException;

    /**
     * @return The number of flashcards.
     */
    int getSize();

    /**
     * @return The flashcard series that maintains the core data model.
     */
    FlashcardSeries getSourceModel();
}
