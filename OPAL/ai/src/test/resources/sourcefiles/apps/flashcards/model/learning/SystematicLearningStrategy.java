/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package apps.flashcards.model.learning;

import java.util.ArrayList;

import apps.flashcards.model.Flashcard;
import apps.flashcards.model.FlashcardSeries;

/**
 * This strategy presents the cards in a way that is meaningful w.r.t. learning the cards.
 * 
 * @author Michael Eichberg
 */
public class SystematicLearningStrategy implements LearningStrategy {

    static final LearningStrategyInfo INFO = new LearningStrategyInfo() {

        public String getShortDescription() {

            return "Systematic";
        }

        @SuppressWarnings("synthetic-access")
        public LearningStrategy create(FlashcardSeries series) {
            return new SystematicLearningStrategy(series);
        }
    };

    private final ArrayList<ArrayList<Flashcard>> flashcardBins;

    private int cardsToLearn = 0;

    private int index = -1;

    private SystematicLearningStrategy(FlashcardSeries series) {

        flashcardBins = new ArrayList<ArrayList<Flashcard>>(5);
        for (int i = 0; i < 5; i++)
            flashcardBins.add(new ArrayList<Flashcard>());

        for (int i = 0; i < series.getSize(); i++) {
            Flashcard flashcard = series.getElementAt(i);
            // If the card was remembered more than 4 times, it is considered to be
            // finally learned.
            if (flashcard.getRememberedInARowCount() < 5) {
                int binID = flashcard.getRememberedInARowCount();
                ArrayList<Flashcard> bin = flashcardBins.get(binID);
                if (binID > 0) { // the card was learned at least once...
                    int pos = 0;
                    for (; pos < bin.size(); pos++) {
                        if (bin.get(pos).getRemembered().after(flashcard.getRemembered())) {
                            break;
                        }
                    }
                    bin.add(pos, flashcard);
                } else
                    bin.add(flashcard);

                cardsToLearn++;
            }
        }
        // We never present all cards (unless there are only new cards...)
        if (cardsToLearn > 15) {
            cardsToLearn = Math.max(15,
                    Math.max(flashcardBins.get(0).size(), (int) (cardsToLearn * 0.75f)));
        }
    }

    public boolean hasNext() {

        return index + 1 < cardsToLearn;
    }

    public void next() throws IndexOutOfBoundsException {

        index++;

        if (index >= cardsToLearn)
            throw new IndexOutOfBoundsException();
    }

    public Flashcard current() throws IndexOutOfBoundsException {

        if (index >= cardsToLearn)
            throw new IndexOutOfBoundsException();

        int cardIndex = index;
        for (int b = 0; b < 5; b++) {
            if (cardIndex < flashcardBins.get(b).size()) {
                return flashcardBins.get(b).get(cardIndex);
            } else {
                cardIndex -= flashcardBins.get(b).size();
            }
        }
        // will never be reached...
        throw new Error();
    }
}
