/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package apps.flashcards.model.learning;

import apps.flashcards.model.FlashcardSeries;

/**
 * Enumeration of all learning strategies.
 * 
 * <p>
 * To support a new learning strategy it is sufficient to add a corresponding
 * </p>
 * 
 * @author Michael Eichberg
 */
public enum LearningStrategies {

    // This enumeration basically serves as a registry of learning strategy info objects.

    Systematic(SystematicLearningStrategy.INFO), NewestFirst(
            NewestFirstLearningStrategy.INFO), OldestFirst(
            OldestFirstLearningStrategy.INFO), Random(RandomLearningStrategy.INFO), RandomForever(
            RandomForeverLearningStrategy.INFO), Quiz(QuizLearningStrategy.INFO), OnlyNewFlashcards(
            JustNewLearningStrategy.INFO);

    private final LearningStrategyInfo learningStrategyInfo;

    private LearningStrategies(LearningStrategyInfo learningStrategyInfo) {

        this.learningStrategyInfo = learningStrategyInfo;
    }

    public LearningStrategy create(FlashcardSeries flashcardSeries) {

        return learningStrategyInfo.create(flashcardSeries);
    }

    @Override
    public String toString() {

        return this.learningStrategyInfo.getShortDescription();
    }

}
