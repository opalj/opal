/** License (BSD Style License):
 *  Copyright (c) 2010
 *  Michael Eichberg (Software Engineering)
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
package apps.flashcards;

import static java.lang.System.err;
import static java.lang.System.out;

import java.io.File;
import java.util.Iterator;
import java.util.Map.Entry;

import javax.swing.UIManager;

import apps.flashcards.ui.FlashcardsEditor;

/**
 * Main method to start the flashcard application. For each method passed to the application a new
 * editor is created.
 * 
 * @author Michael Eichberg
 */
@SuppressWarnings("all")
public class Main {

    public final static boolean DEVELOPMENT = true;

    static {

        // Improve the integration with the particular operating system...
        // This has to be done before _anything_ else is done!
        if (System.getProperty("os.name").startsWith("Mac OS X")) {
            // We have to avoid tight coupling to Mac OS X specific class to make the project usable
            // on
            // different platforms.
            try {
                Class.forName("de.tud.cs.se.flashcards.MacOSXIntegration");
            } catch (ClassNotFoundException cnfe) {
                err.println("The Mac OS X integration failed is not available.");
            }
        } else {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                err.println("The native system look and feel cannot be installed ("
                        + e.getLocalizedMessage() + ").");
            }
        }

        if (DEVELOPMENT) {
            boolean assertionsEnabled = false;
            assert assertionsEnabled = true; // use the side effect to check if assertions are
                                             // enabled
            if (assertionsEnabled)
                out.println("Assertions are enabled.");
            else
                out.println("Assertions are disabled; turn on assertions during development using the command line parameter \"-ea\".");

            // show all user interface related properties
            Iterator<Entry<Object, Object>> properties = UIManager.getDefaults()
                    .entrySet().iterator();
            while (properties.hasNext()) {
                Entry<Object, Object> property = properties.next();
                out.println(property.getKey() + " = " + property.getValue());
            }
        }
    }

    /**
     * Starts the application.
     * 
     * @param args
     *            a list of filenames, a new editor is created for each file.
     */
    public static void main(String[] args) {

        boolean documentOpened = false;

        // Let's try to open the documents specified on the command line.
        if (args.length > 0) {
            for (String arg : args) {
                if (FlashcardsEditor.createFlashcardsEditor(new File(arg)))
                    documentOpened = true;
            }
        }

        if (!documentOpened) {
            // Either the user didn't specify a document on start-up or or all
            // specified documents were unreadable.
            FlashcardsEditor.newFlashcardEditor();
        }
    }
}
