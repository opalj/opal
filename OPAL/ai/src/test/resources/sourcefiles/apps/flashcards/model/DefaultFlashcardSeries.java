/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package apps.flashcards.model;

import java.util.ArrayList;
import java.util.List;

/**
 * A series represents a set of flashcards and basically provides a set of management functions.
 * 
 * @author Michael Eichberg
 */
public class DefaultFlashcardSeries extends AbstractFlashcardSeries {

    // We did deliberately not extend "AbstractListModel" to avoid that Java Serialization of this
    // (then Serializable) class would store references to listeners that do not belong to the model
    // layer.

    /**
     * Convenience method to create an initial flashcard series.
     */
    public static DefaultFlashcardSeries createInitialFlashcardSeries() {

        DefaultFlashcardSeries flashcards = new DefaultFlashcardSeries();
        flashcards.createAddCardCommand(new Flashcard("lose Kopplung", "loose coupling"))
                .execute();
        flashcards.createAddCardCommand(
                new Flashcard("hoher Zusammenhalt", "high cohesion")).execute();
        flashcards.createAddCardCommand(new Flashcard("Stellvertreter", "Proxy"))
                .execute();
        flashcards
                .createAddCardCommand(new Flashcard("Entwurfsmuster", "Design Pattern"))
                .execute();
        flashcards.createAddCardCommand(new Flashcard("Beispiel", "Example")).execute();
        flashcards.createAddCardCommand(new Flashcard("Haus", "House")).execute();
        flashcards.createAddCardCommand(new Flashcard("Hund", "Dog")).execute();
        flashcards.createAddCardCommand(new Flashcard("Erinnerung", "Memento")).execute();

        return flashcards;
    }

    private final FlashcardObserver observer = new FlashcardObserver() {

        // By using an inner class to implement the FlashcardObserver interface instead of
        // letting "FlashcardSeries" directly implement the interface, we do not need to
        // expose this method to clients of the outer class. This avoids pollution of the outer
        // class' (public) interface.
        public void cardChanged(Flashcard flashcard) {

            int index = flashcards.indexOf(flashcard);
            fireContentsUpdated(DefaultFlashcardSeries.this, index, index);
        }
    };

    // The list of all flashcards.
    private final List<Flashcard> flashcards = new ArrayList<Flashcard>();

    // Used to assign a flashcard series unique id with every flashcard.
    private int nextCreationID = 0;

    public int getNextCreationID() {

        return nextCreationID;
    }

    public void setNextCreationID(int value) {

        this.nextCreationID = value;
    }

    // This method is only to be called by the persistence layer to rebuild the model; it must
    // not be called any other class!
    public void addCard(final Flashcard flashcard) {

        flashcards.add(flashcard);
        flashcard.addObserver(observer);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * de.tud.cs.se.flashcards.model.FlashcardSeries#addCard(de.tud.cs.se.flashcards.model.Flashcard
     * )
     */
    public Command createAddCardCommand(final Flashcard flashcard) {

        return new Command() {

            { // associate the card with this list of flashcards
                flashcard.setCreationID(nextCreationID++);
            }

            public void execute() {

                flashcards.add(0, flashcard);
                flashcard.addObserver(observer);

                fireIntervalAdded(DefaultFlashcardSeries.this, 0, 0);
            }

            public void unexecute() {

                flashcard.removeObserver(observer);
                flashcards.remove(0);

                fireIntervalRemoved(DefaultFlashcardSeries.this, 0, 0);
            }

        };
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.tud.cs.se.flashcards.model.FlashcardSeries#removeCards(int[])
     */
    public Command createRemoveCardsCommand(final int[] indices) {

        return new Command() {

            private final Flashcard[] oldFlashcards;

            {
                oldFlashcards = new Flashcard[indices.length];
                for (int i = 0; i < indices.length; i++) {
                    oldFlashcards[i] = flashcards.get(indices[i]);
                }
            }

            public void execute() {

                // we have to start from the end to avoid deleting "arbitrary cards"
                for (int i = indices.length - 1; i >= 0; i--) {
                    int index = indices[i];
                    flashcards.get(index).removeObserver(observer);
                    flashcards.remove(index);
                    fireIntervalRemoved(DefaultFlashcardSeries.this, index, index);
                }

            }

            public void unexecute() {

                // we have to start from the end to avoid deleting "arbitrary cards"
                for (int i = 0; i < indices.length; i++) {
                    int index = indices[i];
                    oldFlashcards[i].addObserver(observer);
                    flashcards.add(index, oldFlashcards[i]);
                    fireIntervalAdded(DefaultFlashcardSeries.this, index, index);
                }

            }

        };
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.tud.cs.se.flashcards.model.FlashcardSeries#getElementAt(int)
     */
    public Flashcard getElementAt(int index) throws IndexOutOfBoundsException {

        return flashcards.get(index);
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.tud.cs.se.flashcards.model.FlashcardSeries#getSize()
     */
    public int getSize() {

        return flashcards.size();
    }

    /**
     * @return <code>this</code>.
     */
    public FlashcardSeries getSourceModel() {

        return this;
    }

}
