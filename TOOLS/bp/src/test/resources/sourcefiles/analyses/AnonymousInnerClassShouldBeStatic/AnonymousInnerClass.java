/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package AnonymousInnerClassShouldBeStatic;

/**
 * Common example of how anonymous inner classes are used. The parent object is never
 * referenced from the anonymous inner class, so it could have been made Static (and
 * explicitly named instead of anonymous) to save memory.
 * 
 * @author Daniel Klauer
 */
public class AnonymousInnerClass {

    void testAwtButton() {
        java.awt.Button button = new java.awt.Button();
        button.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent e) {
                System.out.println("test");
            }
        });
    }
}
