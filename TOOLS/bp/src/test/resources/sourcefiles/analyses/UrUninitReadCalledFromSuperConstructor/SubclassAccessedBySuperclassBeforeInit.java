/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package UrUninitReadCalledFromSuperConstructor;

/**
 * A subclass that is accessed by its superclass before it (the subclass) has been
 * initialized. This can happen if the superclass accesses the subclass during its own
 * initialization, because subclasses are initialized after superclasses.
 * 
 * @author Roberts Kolosovs
 * @author Daniel Klauer
 */
public class SubclassAccessedBySuperclassBeforeInit extends
        SuperclassWithAccessToSubclassDuringInit {

    int i = 123;

    /**
     * This method is called from the parent's constructor, but at that point, this child
     * class' i field hasn't been initialized yet.
     */
    @Override
    void f() {
        System.out.println(i);
    }
}
