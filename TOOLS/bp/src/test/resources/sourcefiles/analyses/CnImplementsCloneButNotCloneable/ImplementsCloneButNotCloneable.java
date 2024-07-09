/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package CnImplementsCloneButNotCloneable;

/**
 * A class that has a clone() method but does not implement Cloneable. This violates the
 * Object.clone() contract.
 * 
 * @author Daniel Klauer
 */
public class ImplementsCloneButNotCloneable {

    @Override
    public Object clone() {
        return new ImplementsCloneButNotCloneable();
    }
}
