/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package apps.flashcards.model.learning;

import apps.flashcards.model.Flashcard;

/**
 * Common interface of all learning strategies. A learning strategy enables the iteration over a set
 * of flashcards.
 * 
 * @author Michael Eichberg
 */
public interface LearningStrategy {

    /**
     * @return True, if there are more flashcards to learn.
     */
    boolean hasNext();

    /**
     * Advances to the next flashcard.
     * 
     * @throws IndexOutOfBoundsException
     *             if {@link #next()} was called after {@link #hasNext()} has returned false.
     */
    void next() throws IndexOutOfBoundsException;

    /**
     * @return The current flashcard.
     * @throws IndexOutOfBoundsException
     *             if {@link #next()} was called after {@link #hasNext()} has returned false.
     */
    Flashcard current() throws IndexOutOfBoundsException;
}
