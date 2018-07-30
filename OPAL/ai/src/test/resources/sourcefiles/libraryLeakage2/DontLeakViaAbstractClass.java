/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package libraryLeakage2;

/**
 *
 * This class inherits the public method "iDoNotLeak" from the package private class
 * DontLeakViaNotConcreteClass. Since this class is abstract, "iDoNotLeak" is not
 * already exposed to the client but it can potentially be later on.
 *
 * @author Michael Reif
 */
public abstract class DontLeakViaAbstractClass extends
		DontLeakViaNotConcreteClass {


	protected void iDoNotLeakToo(){

	}
}
