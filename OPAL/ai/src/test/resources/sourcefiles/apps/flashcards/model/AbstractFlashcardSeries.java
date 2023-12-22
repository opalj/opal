/* BSD 2-Clause License - see OPAL/LICENSE for details. */
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
