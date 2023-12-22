/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package apps.flashcards.model.learning;

import java.util.Random;

import apps.flashcards.model.Flashcard;
import apps.flashcards.model.FlashcardSeries;

/**
 * This strategy presents the flashcards in a random order.
 * 
 * @author Michael Eichberg
 */
public class RandomForeverLearningStrategy implements LearningStrategy {

    static final LearningStrategyInfo INFO = new LearningStrategyInfo() {

        public String getShortDescription() {

            return "Random, until canceled";
        }

        @SuppressWarnings("synthetic-access")
        public LearningStrategy create(FlashcardSeries series) {
            return new RandomForeverLearningStrategy(series);
        }
    };

    private static final Random random = new Random(System.currentTimeMillis());

    private final FlashcardSeries series;

    private int index = -1;

    private RandomForeverLearningStrategy(FlashcardSeries series) {

        this.series = series;
    }

    public boolean hasNext() {

        return true;
    }

    public void next() {

        index = random.nextInt(series.getSize());
    }

    public Flashcard current() {

        return series.getElementAt(index);
    }
}
