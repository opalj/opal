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
