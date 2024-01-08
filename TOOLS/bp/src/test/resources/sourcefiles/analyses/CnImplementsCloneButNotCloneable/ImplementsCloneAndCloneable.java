/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package CnImplementsCloneButNotCloneable;

/**
 * A class that implements clone() and Cloneable. Implementing Cloneable isn't enforced by
 * the compiler, but it's required by the Object.clone() contract.
 * 
 * @author Daniel Klauer
 */
public class ImplementsCloneAndCloneable implements Cloneable {

    @Override
    public Object clone() {
        return new ImplementsCloneAndCloneable();
    }
}
