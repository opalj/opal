/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.test.annotations;

/**
 * 
 * Represents all possible property variants of the [[LibraryLeakage]] property
 * defined in the [[LibraryLeakageAnalysis]].
 * 
 * @Note This enum is used for test-only purposes. It is used as parameter in
 * the [[LibraryLeakageAnalysis]] annotation. Make sure, that the names
 * of the different variants of the Overridden property matches the enumeration
 * names exactly.
 * 
 * @author Michael Reif
 *
 */
public enum CallabilityKeys {
	
	/**
	 * This has to be used if a method can't be inherit by any immediate non-abstract 
	 * subclass and due to this is overridden in every concrete subclass.
	 */
	NotClientCallable,
	
	/**
	 * This has to be used if there is a subclass that inherits that method and the method
	 * is not overridden by every existing subclass.
	 */
	IsClientCallable,
}