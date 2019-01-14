/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.test.annotations;

import java.lang.annotation.*;

import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static java.lang.annotation.ElementType.TYPE;

/**
 * 
 * Describes the [[Instantiability]] property of the OPAL FixpointAnalysis.
 * 
 * @author Michael Reif
 *
 */
@Retention(RUNTIME)
@Target(TYPE)
public @interface InstantiabilityProperty {
	
	InstantiabilityKeys value() default InstantiabilityKeys.Instantiable;
}