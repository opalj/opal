/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.test.annotations;

import java.lang.annotation.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static java.lang.annotation.ElementType.METHOD;

/**
 * 
 * Describes the MethodLeakage property of the OPAL FixpointAnalysis.
 * 
 * @author Michael Reif
 *
 */
@Retention(RUNTIME)
@Target(METHOD)
public @interface CallabilityProperty {
	
	/**
	 * This refers to the LibraryLeakageProperty when the property is computed for
	 * a library under the open package assumption. 
	 */
	CallabilityKeys opa() default CallabilityKeys.IsClientCallable;
	
	/**
	 * This refers to the LibraryLeakageProperty when the property is computed for
	 * a library under the closed package assumption. 
	 */
	CallabilityKeys cpa() default CallabilityKeys.IsClientCallable;
	
	/**
	 * This refers to the LibraryLeakageProperty when the property is computed for
	 * an application.
	 */
	CallabilityKeys application() default CallabilityKeys.NotClientCallable;
}
