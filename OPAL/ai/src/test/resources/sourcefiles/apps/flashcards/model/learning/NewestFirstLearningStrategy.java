/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package apps.flashcards.model.learning;

import apps.flashcards.model.Flashcard;
import apps.flashcards.model.FlashcardSeries;

/**
 * A simple learning strategy that just shows all flashcards belonging to a series in the order in
 * which the were created (newest first).
 * 
 * @author Michael Eichberg
 */
public class NewestFirstLearningStrategy implements LearningStrategy {

    static final LearningStrategyInfo INFO = new LearningStrategyInfo() {

        public String getShortDescription() {

            return "Start with the newest card";
        }

        @SuppressWarnings("synthetic-access")
        public LearningStrategy create(FlashcardSeries series) {

            return new NewestFirstLearningStrategy(series);
        }
    };

    private final FlashcardSeries series;

    private int index = -1;

    private NewestFirstLearningStrategy(FlashcardSeries series) {

        this.series = series;
    }

    public boolean hasNext() {

        return (index + 1) < series.getSize();
    }

    public void next() throws IndexOutOfBoundsException {

        index++;

        if (index >= series.getSize())
            throw new IndexOutOfBoundsException();

    }

    public Flashcard current() throws IndexOutOfBoundsException {

        return series.getElementAt(index);
    }
}
