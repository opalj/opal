/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package apps.flashcards.model.learning;

import apps.flashcards.model.Flashcard;
import apps.flashcards.model.FlashcardSeries;

import java.util.ArrayList;

/**
 * This strategy only presents cards to the user that were already learned successfully (remembered
 * more than two times in a row).
 * 
 * @author Michael Eichberg
 */
public class QuizLearningStrategy implements LearningStrategy {

    static final LearningStrategyInfo INFO = new LearningStrategyInfo() {

        public String getShortDescription() {

            return "Quiz";
        }

        @SuppressWarnings("synthetic-access")
        public LearningStrategy create(FlashcardSeries series) {
            return new QuizLearningStrategy(series);
        }
    };

    private final ArrayList<Flashcard> flashcards;

    private final Permutation permutation;

    private Flashcard currentFlashcard = null;

    private QuizLearningStrategy(FlashcardSeries series) {

        this.flashcards = new ArrayList<Flashcard>(series.getSize());
        for (int i = 0; i < series.getSize(); i++) {
            if (series.getElementAt(i).getRememberedInARowCount() > 1) {
                flashcards.add(series.getElementAt(i));
            }
        }
        this.permutation = new Permutation(flashcards.size());
    }

    public boolean hasNext() {

        return permutation.hasNext();
    }

    public void next() throws IndexOutOfBoundsException {

        currentFlashcard = null;
        currentFlashcard = flashcards.get(permutation.next()); // may throw an
        // IndexOutOfBoundsException
    }

    public Flashcard current() throws IndexOutOfBoundsException {

        if (currentFlashcard == null)
            throw new IndexOutOfBoundsException();

        return currentFlashcard;
    }
}
