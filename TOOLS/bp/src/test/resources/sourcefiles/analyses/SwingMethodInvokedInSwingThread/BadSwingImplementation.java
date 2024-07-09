/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package SwingMethodInvokedInSwingThread;

import java.awt.BorderLayout;
import javax.swing.JButton;
import javax.swing.JFrame;

/**
 * Incorrectly implemented javax/swing functionality. The setVisible() function should be
 * called in the event dispatching thread. Further information:
 * http://en.wikipedia.org/wiki/Event_dispatching_thread Source code:
 * http://web.archive.org/web/20090526170426/http://java.sun.com/developer
 * /JDCTechTips/2003/tt1208.html#1
 * 
 * @author Daniel Klauer
 * @author Roberts Kolosovs
 */
public class BadSwingImplementation {

    public static int main() {
        JFrame frame = new JFrame();
        frame.setTitle("Title");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JButton button = new JButton();
        button.setText("Hello, World!");
        frame.getContentPane().add(button, BorderLayout.CENTER);
        frame.setSize(200, 100);
        frame.setVisible(true); // This should be called via EventQueue.
        return 0;
    }
}
