/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.test.annotations;

/**
 * 
 * Represents all possible property variants of the [[ProjectAccessibility]] property
 * defined in the [[SchadowingAnalysis]].
 * 
 * @Note This enum is used for test-only purposes. It is used as parameter in
 * the [[ProjectAccessibilityProperty]] annotation. Make sure, that the names
 * of the different variants of the Overridden property matches the enumeration
 * names exactly.
 * 
 * @author Michael Reif
 * 
 */
public enum ProjectAccessibilityKeys {
	
	/**
	 * This kind refers to all entities (methods, classes, etc.) that are globally visible to the client.
	 */
	Global,
	
	/**
	 * This kind refers to all entities (methods, classes, etc.) that are in scope of a package.
	 */
	PackageLocal,
	
	/**
	 * This kind refers to all entities (methods, classes, etc.) that are only visible within a class
	 * and doesn't escape of the class.
	 */
	ClassLocal
}