/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package FieldShouldBePackageProtected;

/**
 * Some class without fields shouldn't be an issue for the analysis.
 * 
 * @author Daniel Klauer
 */
public class SomeClassWithoutFields {

    void test() {
        System.out.println("test\n");
    }
}
