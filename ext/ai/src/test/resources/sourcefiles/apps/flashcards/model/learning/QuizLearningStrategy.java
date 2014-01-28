/** License (BSD Style License):
 *  Copyright (c) 2010
 *  Software Engineering
 *  Department of Computer Science
 *  Technische Universität Darmstadt
 *  All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *  - Neither the name of the Software Engineering Group or Technische 
 *    Universität Darmstadt nor the names of its contributors may be used to 
 *    endorse or promote products derived from this software without specific 
 *    prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 *  AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 *  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 *  ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 *  LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 *  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 *  SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 *  INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 *  CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 *  ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *  POSSIBILITY OF SUCH DAMAGE.
 */
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
