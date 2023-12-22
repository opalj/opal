/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package apps.flashcards.persistence;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import apps.flashcards.model.DefaultFlashcardSeries;
import apps.flashcards.model.Flashcard;
import apps.flashcards.model.FlashcardSeries;

/**
 * Some helper methods related to persisting a flashcard series.
 * <p>
 * <b><font color="red">Attention: Persistence is currently not handled properly! Do never use Java
 * Serialization for long term storage! </font></b>
 * </p>
 * 
 * @author Michael Eichberg
 */
public class Store {

    public static final String FILE_ENDING = ".flashcards";

    public static FlashcardSeries openSeries(File file) throws IOException {

        ObjectInputStream oin = null;
        try {

            DefaultFlashcardSeries series = new DefaultFlashcardSeries();
            oin = new ObjectInputStream(new FileInputStream(file));
            series.setNextCreationID(oin.readInt());
            int size = oin.readInt();
            for (int i = 0; i < size; i++) {
                Flashcard f = (Flashcard) oin.readObject();
                series.addCard(f);
            }
            return series;

        } catch (ClassNotFoundException e) {
            // The file did contain something unexpected; let's treat this as an IOException.
            throw new IOException(e);
        } finally {
            if (oin != null)
                oin.close();
        }
    }

    public static void saveSeries(FlashcardSeries flashcardSeries, File file)
            throws IOException {

        FlashcardSeries fs = flashcardSeries.getSourceModel();

        ObjectOutputStream oout = null;
        try {
            oout = new ObjectOutputStream(new FileOutputStream(file));
            oout.write(flashcardSeries.getNextCreationID());
            oout.writeInt(fs.getSize());
            for (int i = 0; i < fs.getSize(); i++) {
                oout.writeObject(fs.getElementAt(i));
            }
        } finally {
            if (oout != null)
                oout.close();
        }
    }
}
