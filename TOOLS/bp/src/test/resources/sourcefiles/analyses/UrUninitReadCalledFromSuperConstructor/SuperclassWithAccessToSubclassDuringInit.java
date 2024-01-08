/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package UrUninitReadCalledFromSuperConstructor;

/**
 * Abstract superclass for UrUninitReadCallFromSuperConstructor test
 * 
 * @author Roberts Kolosovs
 * @author Daniel Klauer
 */
public abstract class SuperclassWithAccessToSubclassDuringInit {

    abstract void f();

    SuperclassWithAccessToSubclassDuringInit() {
        f();
    }
}
