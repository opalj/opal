/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package libraryLeakage2;

import org.opalj.fpcf.test.annotations.CallabilityKeys;
import org.opalj.fpcf.test.annotations.CallabilityProperty;

/**
 *
 * This class is package private, hence, in a closed library scenario a client
 * can not access the the methods of this class. There are 2 subclasses in that
 * example:
 *  - DontLeakViaAbstractClass 
 *  - DontLeakViaInterface
 * 
 * This Tests, that the methods does not leak via non-abstract classes.
 * 
 * @author Michael Reif
 */
class DontLeakViaNotConcreteClass {

	/**
	 * This is weird. This method should be exposed to the client by the
	 * DontLeakViaAbstractClass but the compiler somehow introduces a method
	 * with the same name that prevent that method from being exposed.
	 * 
	 * THIS DEPENDS ON THE COMPILER; UNCOMMENTED FOR NOW
	 */
//	@CallabilityProperty(cpa = CallabilityKeys.NotCallable)
//	public void iDoNotLeak() {
//	}

	/**
	 * Package visible methods can not leak when the closed packages assumption
	 * is met.
	 */
	@CallabilityProperty(cpa = CallabilityKeys.NotCallable)
	void iCanNotLeakUnderCPA() {

	}

	/**
	 * This method can not leak because it is overridden in one subclass and the
	 * other one is package private.
	 */
	@CallabilityProperty(cpa = CallabilityKeys.NotCallable)
	protected void iDoNotLeakToo() {

	}
}

abstract class YouCantInheritFromMe extends DontLeakViaNotConcreteClass {

	protected void iDoNotLeakToo() {

	}
}
