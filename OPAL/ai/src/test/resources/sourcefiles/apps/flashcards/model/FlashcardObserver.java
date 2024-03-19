/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package apps.flashcards.model;

import java.util.EventListener;

/**
 * Interface implemented by observers of flashcards.
 * 
 * @author Michael Eichberg
 */
public interface FlashcardObserver extends EventListener {

    /**
     * Called whenever the contents of the given card has changed.
     * 
     * @param flashcard
     */
    void cardChanged(Flashcard flashcard);
}
