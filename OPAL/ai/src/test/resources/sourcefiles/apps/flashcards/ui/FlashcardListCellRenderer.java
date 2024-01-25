/* BSD 2-Clause License - see OPAL/LICENSE for details. */
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
