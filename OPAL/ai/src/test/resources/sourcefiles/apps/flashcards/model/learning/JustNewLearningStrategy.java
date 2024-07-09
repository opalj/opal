/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package apps.flashcards.model.learning;

import apps.flashcards.model.Flashcard;
import apps.flashcards.model.FlashcardSeries;

/**
 * This learning strategy only presents flashcards that are newly created and were never shown
 * before.
 * 
 * @author Michael Eichberg
 */
public class JustNewLearningStrategy implements LearningStrategy {

    static final LearningStrategyInfo INFO = new LearningStrategyInfo() {

        public String getShortDescription() {

            return "Just new flashcards";
        }

        @SuppressWarnings("synthetic-access")
        public LearningStrategy create(FlashcardSeries flashcardSeries) {

            return new JustNewLearningStrategy(flashcardSeries);
        }
    };

    private final FlashcardSeries series;

    private int index = -1;

    private JustNewLearningStrategy(FlashcardSeries series) {

        this.series = series;
    }

    public boolean hasNext() {

        for (int i = index + 1; i < series.getSize(); i++) {
            if (series.getElementAt(i).getShownCount() == 0)
                return true;
        }
        return false;
    }

    public void next() throws IndexOutOfBoundsException {

        index = index + 1;
        for (; index < series.getSize(); index++) {
            if (series.getElementAt(index).getShownCount() == 0) {
                return;
            }
        }
        throw new IndexOutOfBoundsException(index + " >= " + series.getSize());
    }

    public Flashcard current() throws IndexOutOfBoundsException {

        return series.getElementAt(index);
    }
}
