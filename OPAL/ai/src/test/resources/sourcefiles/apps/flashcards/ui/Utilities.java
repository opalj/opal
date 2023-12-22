/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package apps.flashcards.ui;

import java.io.File;
import java.net.URL;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.SwingConstants;

/**
 * General helper methods related to building a user interface with Swing.
 * 
 * @author Michael Eichberg
 */
public class Utilities {

    private Utilities() {

        // do nothing - just defined to avoid accidental instance creations of this class
    }

    /**
     * Sets the frame title based on the name of the file.
     * 
     * @param frame
     *            The frame's title.
     * @param file
     *            The document's file (which is edited in the frame.)
     */
    public static void setFrameTitle(JFrame frame, File file) {

        if (file != null) {
            frame.setTitle(file.getName().substring(0, file.getName().lastIndexOf('.')));
            frame.getRootPane().putClientProperty("Window.documentFile", file);
        } else
            frame.setTitle("untitled");
    }

    /**
     * Creates a JButton object that can be placed in toolbars.
     * 
     * @param title
     *            The (very short) title of the button. The title will be displayed below the
     *            button.
     * @param iconPath
     *            The path to the ImageIcon.
     * @param iconDescription
     *            A short description of the icon / the action.
     * @return A new JButton.
     * @see #createImageIcon(String, String)
     */
    public static JButton createToolBarButton(String title, String iconPath,
            String iconDescription) {

        JButton button = new JButton(title, createImageIcon(iconPath, iconDescription));
        button.setVerticalTextPosition(SwingConstants.BOTTOM);
        button.setHorizontalTextPosition(SwingConstants.CENTER);
        button.setOpaque(false);
        button.setBorder(BorderFactory.createEmptyBorder());
        return button;
    }

    /**
     * Creates an image icon. If the resource is no longer / not available, an exception is thrown.
     * 
     * @param path
     *            A path relative to the location of this classes compiled class file.
     * @param description
     *            A short description of the icon.
     */
    public static ImageIcon createImageIcon(String path, String description) {

        URL imgURL = Utilities.class.getResource(path);
        if (imgURL != null) {
            return new ImageIcon(imgURL, description);
        } else {
            throw new UnknownError("Missing resource: " + path + "(" + description + ")");
        }
    }
}
