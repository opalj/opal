/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.test.annotations;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * 
 * @author Michael Reif
 */
@Retention(RUNTIME)
@Target(METHOD)
public @interface EntryPointProperty {
	
	EntryPointKeys opa() default EntryPointKeys.IsEntryPoint;
	
	EntryPointKeys cpa() default EntryPointKeys.IsEntryPoint;
	
	EntryPointKeys application() default EntryPointKeys.NoEntryPoint;
}
