/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package CloneDoesNotCallSuperClone;

/**
 * A class with a correctly implemented `clone()` method, including the call to
 * `super.clone()`. This should not be reported.
 * 
 * @author Daniel Klauer
 */
public class CorrectlyImplementedClone implements Cloneable {

    java.util.ArrayList<String> mutableData = new java.util.ArrayList<String>();

    @Override
    public Object clone() throws CloneNotSupportedException {
        // Call super.clone() and down-cast result
        CorrectlyImplementedClone copy = (CorrectlyImplementedClone) super.clone();

        // Duplicate fields of this class as needed
        copy.mutableData = new java.util.ArrayList<String>();

        return copy;
    }
}
