/* BSD 2-Clause License - see OPAL/LICENSE for details. */
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
