/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.test.annotations;

import java.lang.annotation.*;

import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static java.lang.annotation.ElementType.METHOD;

/**
 * 
 * Describes the [[FactoryMethod]] property of the OPAL FixpointAnalysis.
 * 
 * @author Michael Reif
 *
 */
@Retention(RUNTIME)
@Target(METHOD)
public @interface FactoryMethodProperty {
	
	FactoryMethodKeys value() default FactoryMethodKeys.NotFactoryMethod;
}

