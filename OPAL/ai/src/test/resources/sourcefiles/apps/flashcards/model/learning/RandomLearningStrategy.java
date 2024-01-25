/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package apps.flashcards.model.learning;

import apps.flashcards.model.Flashcard;
import apps.flashcards.model.FlashcardSeries;

/**
 * A strategy that presents all flashcards belonging to a series exactly once in a random order.
 * 
 * @author Michael Eichberg
 */
public class RandomLearningStrategy implements LearningStrategy {

    static final LearningStrategyInfo INFO = new LearningStrategyInfo() {

        public String getShortDescription() {

            return "Random";
        }

        @SuppressWarnings("synthetic-access")
        public LearningStrategy create(FlashcardSeries series) {
            return new RandomLearningStrategy(series);
        }
    };

    private final FlashcardSeries series;

    private final Permutation permutation;

    private int index = 0;

    private RandomLearningStrategy(FlashcardSeries series) {

        this.series = series;
        this.permutation = new Permutation(series.getSize());
    }

    public boolean hasNext() {

        return permutation.hasNext();
    }

    public void next() throws IndexOutOfBoundsException {

        index = -1;
        index = permutation.next(); // If an IndexOutOfBoundsException is thrown the index is -1
    }

    public Flashcard current() throws IndexOutOfBoundsException {

        return series.getElementAt(index);
    }

}
