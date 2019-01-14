/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.test.annotations;

/**
 * 
 * Represents all possible property variants of the [[Instantiability]] property
 * defined in the [[InstantiabilityAnalysis]].
 * 
 * @Note This enum is used for test-only purposes. It is used as parameter in
 * the [[InstantiabilityProperty]] annotation. Make sure, that the names
 * of the different variants of the [[Instantiability]] property matches the enumeration
 * names exactly.
 * 
 * @author Michael Reif
 *
 */
public enum InstantiabilityKeys {

	Instantiable,
	
	NotInstantiable
}
