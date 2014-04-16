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

import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import apps.flashcards.util.Arrays;

/**
 * Default implementation of the {@link ListDataListener} related functionality.
 * 
 * @author Michael Eichberg
 */
public abstract class AbstractFlashcardSeries implements FlashcardSeries {

    public final static ListDataListener[] NO_LISTENERS = new ListDataListener[0];

    // This array is treated as an immutable data structure (i.e., its content never changes!)
    // If a listener is added / removed a copy of the array is created and then manipulated. After
    // the new listeners array is configured it is assigned to this field. Hence, an observer is
    // enabled to remove itself from the list of observer in its notify method.
    private ListDataListener[] listeners = NO_LISTENERS;

    /*
     * (non-Javadoc)
     * 
     * @seede.tud.cs.se.flashcards.model.FlashcardSeries#addListDataListener(javax.swing.event.
     * ListDataListener)
     */
    public synchronized void addListDataListener(ListDataListener l) {

        this.listeners = Arrays.append(this.listeners, l);
    }

    /*
     * (non-Javadoc)
     * 
     * @seede.tud.cs.se.flashcards.model.FlashcardSeries#removeListDataListener(javax.swing.event.
     * ListDataListener)
     */
    public synchronized void removeListDataListener(ListDataListener l) {

        this.listeners = Arrays.remove(this.listeners, l, NO_LISTENERS);
    }

    /**
     * @return The list of all list data listeners.
     *         <p>
     *         This array must not be manipulated; <b>only read-only access is permitted</b>!
     *         </p>
     */
    protected ListDataListener[] getListDataListeners() {

        return listeners;
    }

    protected void fireIntervalAdded(Object source, int index0, int index1) {

        ListDataEvent e = new ListDataEvent(source, ListDataEvent.INTERVAL_ADDED, index0,
                index1);

        fireIntervalAdded(e);
    }

    private void fireIntervalAdded(ListDataEvent e) {

        ListDataListener[] currentListeners = listeners;
        for (int i = currentListeners.length - 1; i >= 0; i -= 1) {
            currentListeners[i].intervalAdded(e);
        }
    }

    protected void fireIntervalRemoved(Object source, int index0, int index1) {

        ListDataEvent e = new ListDataEvent(source, ListDataEvent.INTERVAL_REMOVED,
                index0, index1);

        fireIntervalRemoved(e);
    }

    private void fireIntervalRemoved(ListDataEvent e) {

        ListDataListener[] currentListeners = listeners;
        for (int i = currentListeners.length - 1; i >= 0; i -= 1) {
            currentListeners[i].intervalRemoved(e);
        }
    }

    protected void fireContentsUpdated(Object source, int index0, int index1) {

        ListDataEvent e = new ListDataEvent(source, ListDataEvent.CONTENTS_CHANGED,
                index0, index1);

        fireContentsUpdated(e);
    }

    private void fireContentsUpdated(ListDataEvent e) {

        ListDataListener[] currentListeners = listeners;

        for (int i = currentListeners.length - 1; i >= 0; i -= 1) {
            currentListeners[i].contentsChanged(e);
        }
    }

}
