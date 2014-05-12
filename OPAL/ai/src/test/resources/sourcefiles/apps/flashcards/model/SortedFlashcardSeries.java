/** BSD 2-Clause License:
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

/**
 * Implementation of a flashcard list that is sorted.
 * 
 * This flashcard series acts as a decorator for another flashcard series.
 * 
 * @version $Revision: 1.1 $ $Date: 2007/06/14 06:48:16 $
 * @author Michael Eichberg
 */
@SuppressWarnings("boxing")
public class SortedFlashcardSeries extends AbstractFlashcardSeries {

    public final Comparator<Integer> TIMES_REMEMBERED_IN_A_ROW = new Comparator<Integer>() {

        public int compare(Integer f1, Integer f2) {

            int v = flashcardSeries.getElementAt(f1).getRememberedInARowCount()
                    - flashcardSeries.getElementAt(f2).getRememberedInARowCount();
            if (v != 0) {
                return v;
            } else {
                return flashcardSeries.getElementAt(f1).getCreationID()
                        - flashcardSeries.getElementAt(f2).getCreationID();
            }
        }
    };

    /**
     * The order is "most recent - last"; i.e. the last card is the card that was remembered most
     * recently. Cards that were remembered a long time ago (or never) are found at the very
     * beginning.
     */
    public final Comparator<Integer> LAST_TIME_REMEMBERED = new Comparator<Integer>() {

        public int compare(Integer f1, Integer f2) {

            Date f1d = flashcardSeries.getElementAt(f1).getRemembered();
            Date f2d = flashcardSeries.getElementAt(f2).getRemembered();
            if (f1d == null && f2d == null)
                return flashcardSeries.getElementAt(f1).getCreationID()
                        - flashcardSeries.getElementAt(f2).getCreationID();
            if (f1d == null)
                return -1;
            if (f2d == null)
                return 1;

            int v = f1d.compareTo(f2d);
            if (v != 0) {
                return v;
            } else {
                return flashcardSeries.getElementAt(f1).getCreationID()
                        - flashcardSeries.getElementAt(f2).getCreationID();
            }
        }
    };

    public final Comparator<Integer> DATE_CREATED = new Comparator<Integer>() {

        public int compare(Integer f1, Integer f2) {

            Date f1d = flashcardSeries.getElementAt(f1).getCreated();
            Date f2d = flashcardSeries.getElementAt(f2).getCreated();

            int v = f2d.compareTo(f1d);
            if (v != 0) {
                return v;
            } else {
                return flashcardSeries.getElementAt(f1).getCreationID()
                        - flashcardSeries.getElementAt(f2).getCreationID();
            }
        }
    };

    /**
     * The underlying flashcard series.
     */
    private final FlashcardSeries flashcardSeries;

    private final ArrayList<Integer> flashcardReferences = new ArrayList<Integer>();

    private Comparator<Integer> sortingStrategy;

    private final ListDataListener listDataListener = new ListDataListener() {

        public void intervalAdded(ListDataEvent e) {

            // u_ => underlying
            int u_startIndex = e.getIndex0();
            int u_endIndex = e.getIndex1();
            int count = u_endIndex - u_startIndex + 1;

            // remap existing indices
            for (int i = 0; i < flashcardReferences.size(); i++) {
                if (flashcardReferences.get(i) >= u_startIndex)
                    flashcardReferences.set(i, flashcardReferences.get(i) + count);
            }

            // insert new references...
            for (int u_index = u_startIndex; u_index <= u_endIndex; u_index++) {
                int position = Collections.binarySearch(flashcardReferences, u_index,
                        SortedFlashcardSeries.this.sortingStrategy);
                if (position < 0)
                    position = -position - 1;
                flashcardReferences.add(position, u_index);
                fireIntervalAdded(SortedFlashcardSeries.this, position, position);
            }
        }

        public void contentsChanged(ListDataEvent e) {

            // u_ => underlying
            int u_startIndex = e.getIndex0();
            int u_endIndex = e.getIndex1();

            for (int u_index = u_startIndex; u_index <= u_endIndex; u_index++) {

                // the position may have changed...
                for (int index = 0; index < flashcardReferences.size(); index++) {
                    if (flashcardReferences.get(index) >= u_startIndex
                            && flashcardReferences.get(index) <= u_endIndex) {

                        flashcardReferences.remove(index);
                        fireIntervalRemoved(SortedFlashcardSeries.this, index, index);

                        int position = Collections.binarySearch(flashcardReferences,
                                u_index, SortedFlashcardSeries.this.sortingStrategy); // negative if
                                                                                      // not
                                                                                      // included...
                        if (position < 0)
                            position = -position - 1;
                        flashcardReferences.add(position, u_index);
                        fireIntervalAdded(SortedFlashcardSeries.this, position, position);
                    }
                }
            }
        }

        public void intervalRemoved(ListDataEvent e) {

            // u_ => underlying
            int u_startIndex = e.getIndex0();
            int u_endIndex = e.getIndex1();
            int count = u_endIndex - u_startIndex + 1;

            // find indexes of references that we need to remove and remap the other indices
            int[] indices = new int[count];
            int j = 0;
            for (int i = 0; i < flashcardReferences.size(); i++) {
                if (flashcardReferences.get(i) >= u_startIndex) {
                    if (flashcardReferences.get(i) <= u_endIndex)
                        indices[j++] = i;
                    else
                        flashcardReferences.set(i, flashcardReferences.get(i) - count);
                }
            }
            // let's updated this model
            Arrays.sort(indices);
            for (int i = indices.length - 1; i >= 0; i--) {
                flashcardReferences.remove(indices[i]);
            }
            if (indices[indices.length - 1] - indices[0] + 1 == indices.length)
                fireIntervalRemoved(SortedFlashcardSeries.this, indices[0],
                        indices[indices.length - 1]);
            else
                fireContentsUpdated(SortedFlashcardSeries.this, 0,
                        flashcardReferences.size());
        }
    };

    public SortedFlashcardSeries(FlashcardSeries flashcardSeries) {

        this.sortingStrategy = DATE_CREATED;

        this.flashcardSeries = flashcardSeries;

        for (int i = 0; i < flashcardSeries.getSize(); i++)
            flashcardReferences.add(i);
        Collections.sort(flashcardReferences, sortingStrategy);

        flashcardSeries.addListDataListener(listDataListener);

    }

    public int getNextCreationID() {

        return flashcardSeries.getNextCreationID();
    }

    public void setNextCreationID(int creationID) {

        flashcardSeries.setNextCreationID(creationID);
    }

    public void setSortingStrategy(Comparator<Integer> sortingStrategy) {

        this.sortingStrategy = sortingStrategy;
        Collections.sort(flashcardReferences, sortingStrategy);

        fireContentsUpdated(this, 0, flashcardReferences.size() - 1);
    }

    /**
     * @return The source model of the underlying flashcards list.
     */
    public FlashcardSeries getSourceModel() {

        return flashcardSeries.getSourceModel();
    }

    public Command createAddCardCommand(Flashcard flashcard) {

        return flashcardSeries.createAddCardCommand(flashcard);

    }

    public Command createRemoveCardsCommand(int[] indices) {

        // remap indices
        int[] realIndices = new int[indices.length];
        for (int i = indices.length - 1; i >= 0; i--) {
            realIndices[i] = flashcardReferences.get(indices[i]);
        }

        Arrays.sort(realIndices);

        // remove cards on the underlying collection
        return flashcardSeries.createRemoveCardsCommand(realIndices);
    }

    public Flashcard getElementAt(int index) throws IndexOutOfBoundsException {

        return flashcardSeries.getElementAt(flashcardReferences.get(index));
    }

    public int getSize() {

        return flashcardReferences.size();
    }

}
