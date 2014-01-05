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
package apps.flashcards.ui;

import static javax.swing.BorderFactory.createBevelBorder;
import static javax.swing.BorderFactory.createCompoundBorder;
import static javax.swing.BorderFactory.createEmptyBorder;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.UIManager;
import javax.swing.border.BevelBorder;

import apps.flashcards.model.Command;
import apps.flashcards.model.Flashcard;

/**
 * Editor for flashcards.
 * 
 * @author Michael Eichberg
 */
public class FlashcardEditor {

    private final FlashcardsEditor owner;

    // GUI components

    private final JDialog dialog;

    private final Box questionBox;

    private final JLabel questionLabel;

    private final JTextField questionField;

    private final Box answerBox;

    private final JLabel answerLabel;

    private final JScrollPane answerTextAreaScrollPane;

    private final JTextArea answerTextArea;

    private final Box okCancelBox;

    private final JButton okButton;

    private final JButton cancelButton;

    /**
     * Creates a new editor that can be used to edit specific flashcards.
     * 
     * @param owner
     *            This editor's parent frame.
     */
    @SuppressWarnings("static-access")
    FlashcardEditor(FlashcardsEditor owner) {

        this.owner = owner;

        questionField = new JTextField();
        questionField.setAlignmentX(0.0f);

        questionLabel = new JLabel("Question");
        questionLabel.setBorder(createEmptyBorder(2, 2, 2, 2));
        questionLabel.setAlignmentX(0.0f);

        questionBox = Box.createVerticalBox();
        questionBox.setBorder(createCompoundBorder(createEmptyBorder(10, 10, 10, 10),
                createBevelBorder(BevelBorder.LOWERED)));
        questionBox.add(questionLabel);
        questionBox.add(Box.createVerticalStrut(5));
        questionBox.add(questionField);

        answerLabel = new JLabel("Answer");
        answerLabel.setAlignmentX(0.0f);
        answerLabel.setBorder(createEmptyBorder(2, 2, 2, 2));

        answerTextArea = new JTextArea();
        answerTextArea.setLineWrap(true);
        answerTextArea.setWrapStyleWord(true);

        answerTextAreaScrollPane = new JScrollPane(answerTextArea);
        answerTextAreaScrollPane.setAlignmentX(0.0f);
        answerTextAreaScrollPane.setBorder(UIManager.getBorder("ScrollPane.border"));
        answerTextAreaScrollPane
                .setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

        answerBox = Box.createVerticalBox();
        answerBox.setBorder(createCompoundBorder(createEmptyBorder(10, 10, 10, 10),
                createBevelBorder(BevelBorder.LOWERED)));
        answerBox.add(answerLabel);
        answerBox.add(Box.createVerticalStrut(5));
        answerBox.add(answerTextAreaScrollPane);

        okButton = new JButton("Ok");
        okButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent event) {

                update = true;
                dialog.setVisible(false);
            }
        });

        cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent event) {

                update = false;
                dialog.setVisible(false);
            }
        });

        okCancelBox = Box.createHorizontalBox();
        okCancelBox.setBorder(createEmptyBorder(10, 10, 10, 10));
        okCancelBox.add(Box.createGlue());
        okCancelBox.add(cancelButton);
        okCancelBox.add(okButton);

        dialog = new JDialog(this.owner.getFrame(), "Edit Flashcard", true);
        dialog.getRootPane().putClientProperty("Window.style", "small");
        dialog.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
        dialog.setMinimumSize(new java.awt.Dimension(320, 240));
        dialog.setSize(640, 480);
        dialog.setLocationRelativeTo(this.owner.getFrame());
        dialog.getContentPane().add(questionBox, BorderLayout.NORTH);
        dialog.getContentPane().add(answerBox);
        dialog.getContentPane().add(okCancelBox, BorderLayout.SOUTH);
    }

    /**
     * True if the card needs to be udpated, false otherwise. This variable is set when the dialog
     * is closed.
     */
    private boolean update = false;

    /**
     * Edits a given flashcard. If the flashcard was edited and the editing process was not
     * canceled, <code>true</code> is returned.
     * 
     * @param card
     *            The flashcard that may be edited. The flashcard is used to initialize this dialog.
     * @return <code>true</code> if the card was edited; <code>false</false> otherwise.
     */
    public Command edit(Flashcard card) {

        // set to false to make sure that the card is not updated, when the dialog
        // is closed using the dialogs "close" button
        update = false;

        // configure the editor
        questionField.setText(card.getQuestion());
        answerTextArea.setText(card.getAnswer());

        // show the dialog to enable the user to edit the flashcard
        dialog.setVisible(true);

        // the dialog is closed
        if (update) {
            return card.createUpdateCommand(questionField.getText().length() == 0 ? " "
                    : questionField.getText(), answerTextArea.getText());

        } else
            return null;
    }

}
