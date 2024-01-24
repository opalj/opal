/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ai.test.invokedynamic.annotations;

import java.lang.annotation.*;
import static java.lang.annotation.RetentionPolicy.*;
import static java.lang.annotation.ElementType.*;

/**
 * Describes a method call. For types see the {@link TargetResolution} enum.
 * 
 * @author Arne Lottmann
 * @author Michael Reif
 */
@Retention(RUNTIME)
@Target(METHOD)
public @interface InvokedMethod {

	TargetResolution resolution() default TargetResolution.DEFAULT;

	/**
	 * The type name of the receiver using JVM notation (e.g.,
	 * "java/lang/Object").
	 */
	String receiverType();

	String name();

	Class<?> returnType() default Void.class;

	Class<?>[] parameterTypes() default {};

	int line() default -1;

	boolean isStatic() default false;

	boolean isReflective() default false;

	CallGraphAlgorithm[] isContainedIn() default { 
			CallGraphAlgorithm.CHA,
			CallGraphAlgorithm.BasicVTA,
			CallGraphAlgorithm.DefaultVTA,
			CallGraphAlgorithm.ExtVTA,
			CallGraphAlgorithm.CFA   };
}
