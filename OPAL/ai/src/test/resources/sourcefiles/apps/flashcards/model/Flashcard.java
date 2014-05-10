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

import java.awt.Dimension;
import java.io.Serializable;
import java.util.Date;
import java.util.LinkedList;

/**
 * Every flashcard consists of two parts: (i) a question and (ii) an answer to the question.
 * Additionally, meta information is associated with each flashcard to implement different learning
 * strategies.
 * 
 * @author Michael Eichberg
 */
public class Flashcard implements Serializable {

    // TODO Get rid of Java serialization!
    private static final long serialVersionUID = 200L;

    /**
     * The width of a flashcard.
     */
    public static final int WIDTH = 600;

    /**
     * The height of a flashcard.
     */
    public static final int HEIGHT = 400;

    /**
     * The dimension of flashcards.
     * 
     * @see #WIDTH
     * @see #HEIGHT
     */
    public static final Dimension FLASHCARD_DIMENSION = new Dimension(WIDTH, HEIGHT);

    // TODO As soon as we do no longer use Java serialization make this object final!
    private transient LinkedList<FlashcardObserver> observers;

    /**
     * The id of this flashcard; this id is always unique w.r.t. a given flashcard series.
     * <p>
     * The creationID can only be initialized once and must no change afterwards. The id can /
     * should be used to get a stable sorting order, if the sorting criteria does not necessarily
     * give rise to a unique sorting order. For example, if we sort the flashcards by the number of
     * times a card was visited, it is very likely that several cards were visited the same number
     * of times. If we now implement sorting, filtering and undo / redo it is possible that - if a
     * filter and a sorting order is set - the cards appear in a different oder after an operation
     * was undone.
     * </p>
     */
    private int creationID = -1;

    private final Date created = new Date();

    private Date remembered = null;

    private Date notRemembered = null;

    private int shownCount = 0;

    private int notRememberedCount = 0;

    private int rememberedInARowCount = 0;

    private String question;

    private String answer;

    public Flashcard(String question, String answer) {

        this.question = question;
        this.answer = answer;
    }

    /**
     * Creates a new empty flashcard.
     */
    public Flashcard() {

        this("", "");
    }

    void setCreationID(int creationID) {

        // Make the conditions explicit....
        if (this.creationID != -1)
            throw new IllegalStateException("This flashcard already has an ID: "
                    + creationID + ".");

        this.creationID = creationID;
    }

    int getCreationID() {

        return creationID;
    }

    public synchronized void addObserver(FlashcardObserver flashcardObserver) {

        if (observers == null)
            observers = new LinkedList<FlashcardObserver>();

        observers.addFirst(flashcardObserver);
    }

    public synchronized void removeObserver(FlashcardObserver flashcardObserver) {

        observers.remove(flashcardObserver);
    }

    protected void notifyFlashcardObervers() {

        @SuppressWarnings("hiding")
        FlashcardObserver[] observers;
        synchronized (this) {
            if (this.observers == null)
                return;

            // required to enable an observer to remove itself from the list of observers...
            observers = this.observers.toArray(new FlashcardObserver[this.observers
                    .size()]);
        }
        for (FlashcardObserver observer : observers) {
            observer.cardChanged(this);
        }
    }

    public Command createUpdateCommand(final String newQuestion, final String newAnswer) {

        return new Command() {

            private final String oldQuestion = Flashcard.this.question;

            private final String oldAnswer = Flashcard.this.answer;

            public void execute() {

                Flashcard.this.question = newQuestion;
                Flashcard.this.answer = newAnswer;

                Flashcard.this.notifyFlashcardObervers();
            }

            public void unexecute() {

                Flashcard.this.question = oldQuestion;
                Flashcard.this.answer = oldAnswer;

                Flashcard.this.notifyFlashcardObervers();

            }

        };
    }

    public void setNotRemembered(Date notRemembered) {

        this.notRemembered = notRemembered;
        this.rememberedInARowCount = 0;
        this.shownCount++;
        this.notRememberedCount++;

        notifyFlashcardObervers();
    }

    public void setRemembered(Date remembered) {

        this.remembered = remembered;
        this.rememberedInARowCount++;
        this.shownCount++;

        notifyFlashcardObervers();
    }

    public String getAnswer() {

        return answer;
    }

    public String getQuestion() {

        return question;
    }

    public Date getNotRemembered() {

        return notRemembered;
    }

    public int getNotRememberedCount() {

        return notRememberedCount;
    }

    public Date getRemembered() {

        return remembered;
    }

    public Date getCreated() {

        return created;
    }

    public int getShownCount() {

        return shownCount;
    }

    public int getRememberedInARowCount() {

        return rememberedInARowCount;
    }

    public boolean contains(String searchTerm) {

        return question.contains(searchTerm) || answer.contains(searchTerm);
    }

    /**
     * Clones the content related part
     */
    @Override
    public Flashcard clone() {

        return new Flashcard(this.question, this.answer);
    }

}
