/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package ImplementsCloneableButNotClone;

/**
 * A class that implements Cloneable and overrides the clone() method.
 * 
 * @author Daniel Klauer
 */
public class ImplementsCloneAndCloneable implements Cloneable {

    @Override
    public Object clone() {
        return new ImplementsCloneAndCloneable();
    }
}
