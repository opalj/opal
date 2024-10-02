/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package apps.flashcards.model.learning;

import apps.flashcards.model.Flashcard;
import apps.flashcards.model.FlashcardSeries;

/**
 * A simple learning strategy that just shows all flashcards belonging to a series beginning with
 * the oldest card.
 * 
 * @author Michael Eichberg
 */
public class OldestFirstLearningStrategy implements LearningStrategy {

    static final LearningStrategyInfo INFO = new LearningStrategyInfo() {

        public String getShortDescription() {

            return "Start with the oldest card";
        }

        @SuppressWarnings("synthetic-access")
        public LearningStrategy create(FlashcardSeries series) {

            return new OldestFirstLearningStrategy(series);
        }
    };

    private final FlashcardSeries series;

    private int index;

    private OldestFirstLearningStrategy(FlashcardSeries series) {

        this.series = series;
        this.index = series.getSize();
    }

    public boolean hasNext() {

        return index > 0;
    }

    public void next() throws IndexOutOfBoundsException {

        index--;

        if (index < 0)
            throw new IndexOutOfBoundsException();

    }

    public Flashcard current() throws IndexOutOfBoundsException {

        return series.getElementAt(index);
    }
}
