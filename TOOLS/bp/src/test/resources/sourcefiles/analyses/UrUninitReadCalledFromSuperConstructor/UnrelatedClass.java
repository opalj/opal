/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package UrUninitReadCalledFromSuperConstructor;

/**
 * Some class that neither accesses a subclass nor is accessed by its superclass during
 * initialization.
 * 
 * @author Daniel Klauer
 */
public class UnrelatedClass {

    public UnrelatedClass() {
        System.out.println("test");
    }
}
