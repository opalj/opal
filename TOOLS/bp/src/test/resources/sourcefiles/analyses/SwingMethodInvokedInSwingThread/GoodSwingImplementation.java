/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package SwingMethodInvokedInSwingThread;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import javax.swing.JButton;
import javax.swing.JFrame;

/**
 * Correctly implemented javax/swing functionality. The setVisible() function is called
 * through invokeLater() function of the EventQueue. Source code:
 * http://web.archive.org/web/20090526170426/http://java.sun.com/developer
 * /JDCTechTips/2003/tt1208.html#1
 * 
 * @author Roberts Kolosovs
 */
public class GoodSwingImplementation {

    private static class FrameShower implements Runnable {

        final JFrame frame;

        public FrameShower(JFrame frame) {
            this.frame = frame;
        }

        public void run() {
            frame.setVisible(true);
        }
    }

    public static void main(String args[]) {
        JFrame frame = new JFrame();
        frame.setTitle("Title");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JButton button = new JButton();
        button.setText("Hello, World!");
        frame.getContentPane().add(button, BorderLayout.CENTER);
        frame.setSize(200, 100);
        Runnable runner = new FrameShower(frame);
        EventQueue.invokeLater(runner);
    }
}
