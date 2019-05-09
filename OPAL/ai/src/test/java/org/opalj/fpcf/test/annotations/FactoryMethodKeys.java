/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.test.annotations;

/**
 * Represents all possible property variants of the [[FactoryMethod]] property which
 * is defined in the [[FactoryMethodAnalysis]]. 
 * 
 * @Note This enum is used for test-only purposes. It is used as parameter in
 * the [[FactoryMethodProperty]] annotation. Make sure, that the names
 * of the different variants of the Overridden property matches the enumeration
 * names exactly.
 * 
 * @author Michael Reif
 *
 */
public enum FactoryMethodKeys {
	
	IsFactoryMethod,
	
	NotFactoryMethod
}
