/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package apps.flashcards.model.learning;

import apps.flashcards.model.FlashcardSeries;

/**
 * @author Michael Eichberg
 */
public interface LearningStrategyInfo {

    /**
     * @return A short description of the strategy.
     */
    String getShortDescription();

    /**
     * Creates a new instance of the learning strategy described by this info object. The learning
     * strategy operates on the given flashcard series and can assume that no cards of the flashcard
     * series will be deleted or added while the learning strategy is used.
     */
    LearningStrategy create(FlashcardSeries flashcardSeries);

}
