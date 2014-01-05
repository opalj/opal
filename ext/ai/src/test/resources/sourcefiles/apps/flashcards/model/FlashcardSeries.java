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
package apps.flashcards.model;

import javax.swing.ListModel;
import javax.swing.event.ListDataListener;

/**
 * A flashcards list manages a list of flashcards.
 * 
 * @author Michael Eichberg
 */
@SuppressWarnings("rawtypes")
public interface FlashcardSeries extends ListModel {

    /**
     * Adds the given listener.
     * <p>
     * Registering the same listener twice is not supported.
     * </p>
     */
    void addListDataListener(ListDataListener l);

    /**
     * Removes the given, registered listener.
     * <p>
     * It is an error to request removing a listener that is not (no longer) registered.
     * </p>
     */
    void removeListDataListener(ListDataListener l);

    /**
     * Adds the given card to this series; a card is always added as the first card to the core data
     * model. The card must not belong to any other flashcards list.
     */
    Command createAddCardCommand(Flashcard flashcard);

    /**
     * Removes the flashcards with the indices.
     * <p>
     * A flashcard that was removed cannot be added to another series, because it may be
     * reintegrated if the user undos the remove operation. To add a flashcard to another series,
     * clone the card.
     * </p>
     * 
     * @param indices
     *            the indices of the flashcards that will be removed. The array has to be sorted in
     *            ascending order; no index has to appear twice.
     */
    Command createRemoveCardsCommand(int[] indices);

    int getNextCreationID();

    void setNextCreationID(int creationID);

    /**
     * @return The card with the given index.
     */
    Flashcard getElementAt(int index) throws IndexOutOfBoundsException;

    /**
     * @return The number of flashcards.
     */
    int getSize();

    /**
     * @return The flashcard series that maintains the core data model.
     */
    FlashcardSeries getSourceModel();
}
