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
