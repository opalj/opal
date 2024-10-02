/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package AnonymousInnerClassShouldBeStatic;

/**
 * Common examples of how anonymous inner classes are used. The parent object is
 * referenced from the anonymous inner class, so it cannot be made `static`.
 * 
 * @author Daniel Klauer
 */
public class AnonymousInnerClassAccessingOuterClass {

    private String message = "test";

    void testCommonExample() {
        java.awt.Button button = new java.awt.Button();
        button.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent e) {
                System.out.println(message);
            }
        });
    }

    class ReadOuterClassFieldInMethod {

        void test() {
            System.out.println(message);
        }
    }

    class WriteOuterClassFieldInMethod {

        void test() {
            message = "foo";
        }
    }

    class ReadOuterClassFieldInConstructor {

        ReadOuterClassFieldInConstructor() {
            System.out.println(message);
        }
    }

    class WriteOuterClassFieldInConstructor {

        WriteOuterClassFieldInConstructor() {
            message = "foo";
        }
    }
}
