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

import java.awt.Color;
import java.awt.Component;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.UIManager;

import apps.flashcards.model.Flashcard;

/**
 * Renders a flashcard.
 * 
 * @author Michael Eichberg
 */
@SuppressWarnings({ "serial", "rawtypes" })
public class FlashcardListCellRenderer extends JLabel implements ListCellRenderer {

    static final Color[] GREEN_COLORS = new Color[] { new Color(0, 0, 0),
            new Color(0, 40, 0), new Color(0, 80, 0), new Color(0, 120, 0),
            new Color(0, 160, 0), new Color(0, 200, 0) };

    static final Color LIGHT_RED = new Color(255, 200, 200);

    {
        setOpaque(true);
        setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
    }

    
    public Component getListCellRendererComponent(JList list, Object value, int index,
            boolean isSelected, boolean cellHasFocus) {

        Flashcard flashcard = ((Flashcard) value);
        setText(flashcard.getQuestion());

        if (isSelected) {
            setBackground(UIManager.getColor("List.selectionBackground"));
            setForeground(UIManager.getColor("List.selectionForeground"));
        } else {
            setForeground(GREEN_COLORS[Math.min(5, flashcard.getRememberedInARowCount())]);

            if (flashcard.getNotRemembered() != null
                    && (flashcard.getRemembered() == null || flashcard.getNotRemembered()
                            .after(flashcard.getRemembered())))
                setBackground(LIGHT_RED);
            else
                setBackground(Color.WHITE);
        }

        return this;
    }
}
