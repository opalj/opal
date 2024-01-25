/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package AnonymousInnerClassShouldBeStatic;

/**
 * "Fixed" version of AnonymousInnerClass.
 * 
 * @author Daniel Klauer
 */
public class StaticInnerClass {

    static class MyEventHandler implements java.awt.event.ActionListener {

        public void actionPerformed(java.awt.event.ActionEvent e) {
            System.out.println("test");
        }
    }

    void testAwtButton() {
        java.awt.Button button = new java.awt.Button();
        button.addActionListener(new MyEventHandler());
    }
}
