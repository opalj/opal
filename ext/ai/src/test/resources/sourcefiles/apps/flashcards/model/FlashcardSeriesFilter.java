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

import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import apps.flashcards.util.Arrays;

/**
 * This flashcard series acts as a filter for another flashcard series.
 * <p>
 * The flashcard series is maintained in response to change events issued by the underlying
 * flashcard series; this series just maintains a list of indices pointing to the "real" flashcards
 * in the underlying FlashcardSeries. By making this decorator an observer of the underlying series,
 * operations that are directly called on the underlying flashcard series - and do manipulate the
 * series - are supported. The filtered list always reflects the correct state.
 * </p>
 * 
 * @version $Rev: 122 $ $Date: 2010-02-25 18:01:35 +0100 (Thu, 25 Feb 2010) $
 * @author Michael Eichberg
 */
public class FlashcardSeriesFilter extends AbstractFlashcardSeries {

    /**
     * The underlying flashcard series.
     */
    private final FlashcardSeries flashcardSeries;

    // This array's content is always sorted in ascending order!
    private int[] flashcardIndices = Arrays.EMPTY_INT_ARRAY;

    // FlashcardSeriesFilter does not directly implement the ListDataListener interface
    // to avoid polluting the public interface. Additionally, the code to handle changes of
    // the underlying flashcard series is now well modularized.
    private final ListDataListener listDataListener = new ListDataListener() {

        public void intervalAdded(ListDataEvent e) {

            int u_startIndex = e.getIndex0();
            int u_endIndex = e.getIndex1();

            assert u_startIndex <= u_endIndex;

            // First, we have to find the place where we "potentially" have to insert the elements.
            int index = java.util.Arrays.binarySearch(flashcardIndices, u_startIndex);
            if (index < 0)
                index = -(index + 1);

            // Second, update the existing references; i.e., remap the indices pointing to the old
            // cards.
            int insertCount = u_endIndex - u_startIndex + 1;
            for (int i = index; i < flashcardIndices.length; i++) {
                FlashcardSeriesFilter.this.flashcardIndices[i] = flashcardIndices[i]
                        + insertCount;
            }

            // Third, let's check if we have to insert some of the newly added cards.
            int startIndex = Integer.MAX_VALUE;
            int endIndex = -1;
            for (int u_index = u_startIndex; u_index <= u_endIndex; u_index++) {
                if (accept(flashcardSeries.getElementAt(u_index))) {
                    FlashcardSeriesFilter.this.flashcardIndices = Arrays.add(
                            flashcardIndices, u_index, index);
                    if (index < startIndex)
                        startIndex = index;
                    if (index > endIndex)
                        endIndex = index;
                    index++;
                }
            }
            if (startIndex < Integer.MAX_VALUE /* && endIndex >= 0 */)
                fireIntervalAdded(FlashcardSeriesFilter.this, startIndex, endIndex);
        }

        public void contentsChanged(ListDataEvent e) {

            // We may have to "remove / add" elements, hence we have to start filtering from the
            // end!
            int u_startIndex = e.getIndex0(); // u_ => index w.r.t. the underlying series
            int u_endIndex = e.getIndex1();

            assert u_endIndex >= u_startIndex;

            int index = flashcardIndices.length - 1;

            for (int u_index = u_endIndex; u_index >= u_startIndex; u_index--) {

                // let's search for the card with the current index
                // TODO use Arrays.binarySearch
                while (index >= 0 && flashcardIndices[index] > u_index)
                    index--;

                if (index >= 0 && flashcardIndices[index] == u_index) {
                    // The card is not filtered (we did find the index) ...

                    if (!accept(flashcardSeries.getElementAt(u_index))) {
                        // ... but we have to filter it now.
                        FlashcardSeriesFilter.this.flashcardIndices = Arrays.remove(
                                flashcardIndices, index);
                        fireIntervalRemoved(FlashcardSeriesFilter.this, index, index);
                    } else {
                        fireContentsUpdated(FlashcardSeriesFilter.this, index, index);
                    }
                } else { // also handles the case "index == -1"
                    // The card is currently filtered...
                    if (accept(flashcardSeries.getElementAt(u_index))) {
                        // ... now we have to include it.
                        FlashcardSeriesFilter.this.flashcardIndices = Arrays.add(
                                flashcardIndices, u_index, index + 1);
                        fireIntervalAdded(FlashcardSeriesFilter.this, index + 1,
                                index + 1);
                    }
                }
            }
        }

        public void intervalRemoved(ListDataEvent e) {

            int u_startIndex = e.getIndex0();
            int u_endIndex = e.getIndex1();

            int endIndex = -1;
            int startIndex = flashcardIndices.length;

            assert u_startIndex <= u_endIndex;

            for (int index = flashcardIndices.length - 1; index >= 0; index--) {
                if (flashcardIndices[index] >= u_startIndex
                        && flashcardIndices[index] <= u_endIndex) {

                    if (index > endIndex)
                        endIndex = index;
                    if (index < startIndex)
                        startIndex = index;

                    FlashcardSeriesFilter.this.flashcardIndices = Arrays.remove(
                            flashcardIndices, index);

                    // update the indices...
                    for (int j = index; j < flashcardIndices.length; j++) {
                        flashcardIndices[j] = flashcardIndices[j] - 1;
                    }
                }
            }
            if (startIndex > -1 /* && endIndex >= startIndex */) {
                assert startIndex <= endIndex;
                // we did remove some cards...
                fireIntervalRemoved(FlashcardSeriesFilter.this, startIndex, endIndex);
            }
        }

    };

    /**
     * Stores the current search term.
     */
    private String searchTerm = "";

    /**
     * Constructs a new filter that enables to dynamically filter flashcards. If no filter is set
     * the behavior of this filter is completely transparent.
     */
    public FlashcardSeriesFilter(FlashcardSeries flashcardSeries) {

        for (int i = 0; i < flashcardSeries.getSize(); i++)
            flashcardIndices = Arrays.append(flashcardIndices, i);

        flashcardSeries.addListDataListener(listDataListener);
        this.flashcardSeries = flashcardSeries;
    }

    /**
     * @return The source model of the underlying flashcards list.
     */
    public FlashcardSeries getSourceModel() {

        return flashcardSeries.getSourceModel();
    }

    public int getNextCreationID() {

        return flashcardSeries.getNextCreationID();
    }

    public void setNextCreationID(int creationID) {

        flashcardSeries.setNextCreationID(creationID);
    }

    public Command createAddCardCommand(Flashcard flashcard) {

        return flashcardSeries.createAddCardCommand(flashcard);

    }

    public Command createRemoveCardsCommand(int[] indices) {

        // remap indices
        int[] realIndices = new int[indices.length];
        for (int i = indices.length - 1; i >= 0; i--) {
            realIndices[i] = flashcardIndices[indices[i]];
        }

        return flashcardSeries.createRemoveCardsCommand(realIndices);
    }

    public Flashcard getElementAt(int index) throws IndexOutOfBoundsException {

        return flashcardSeries.getElementAt(flashcardIndices[index]);
    }

    public int getSize() {

        return flashcardIndices.length;
    }

    public void setSearchTerm(String searchTerm) {

        if (this.searchTerm.equals(searchTerm))
            return;

        if (searchTerm.length() == 0) {
            // reset the filter
            this.searchTerm = "";

            for (int u_index = 0; u_index < flashcardSeries.getSize(); u_index++) {
                // let's reintegrate filtered cards
                if (flashcardIndices.length == u_index
                        || flashcardIndices[u_index] != u_index) {
                    flashcardIndices = Arrays.add(flashcardIndices, u_index, u_index);
                    fireIntervalAdded(this, u_index, u_index);
                }
            }

        } else if (this.searchTerm.contains(searchTerm)) {
            // some characters were removed; we may have to (re)integrate some filtered
            // flashcards...

            this.searchTerm = searchTerm;

            int index = 0;
            for (int u_index = 0; u_index < flashcardSeries.getSize(); u_index++) {
                assert index <= u_index; // loop invariant

                if (index < flashcardIndices.length && flashcardIndices[index] == u_index) {
                    index++;
                    continue;
                }

                // either index >= flashcardIndices.size() or the flashcardIndices.get(index) is
                // refereing to a later card; hence we may have to reintegrate filtered cards
                if (accept(flashcardSeries.getElementAt(u_index))) {
                    flashcardIndices = Arrays.add(flashcardIndices, u_index, index);
                    fireIntervalAdded(this, index, index);
                    index++;
                }
            }

        } else if (searchTerm.contains(this.searchTerm)) {
            // The user added more characters; we may have to filter more flashcards...

            this.searchTerm = searchTerm; // has to be done before "accept(...)" is called

            for (int index = flashcardIndices.length - 1; index >= 0; index--) {
                if (!accept(flashcardSeries.getElementAt(flashcardIndices[index]))) {
                    flashcardIndices = Arrays.remove(flashcardIndices, index);
                    fireIntervalRemoved(this, index, index);
                }
            }

        } else {
            // The search term has changed fundamentally; e.g., because the user probably pasted a
            // string over an existing search term...
            setSearchTerm(""); // clear filter
            setSearchTerm(searchTerm); // apply filter
        }
    }

    /**
     * @return <code>true</code>, if the card matches the search condition.
     */
    protected boolean accept(Flashcard flashcard) {

        return flashcard.contains(searchTerm);
    }

}
