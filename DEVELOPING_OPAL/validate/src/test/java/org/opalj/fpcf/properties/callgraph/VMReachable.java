/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.callgraph;

import org.opalj.fpcf.properties.PropertyValidator;
import org.opalj.tac.fpcf.analyses.cg.TypeIterator;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.CLASS;

/**
 * Describes a method that should be marked as vm-reachable
 *
 * @author Julius Naeumann
 */
@Retention(CLASS)
@Target({METHOD, CONSTRUCTOR})
@Documented
@PropertyValidator(key = "VMReachable", validator = VMReachableMethodMatcher.class)
public @interface VMReachable {

    /**
     * The call graph analyses which we expect to resolve the annotated calls.
     * If the list is empty, we assume the annotation applies to any call graph
     * algorithm.
     */
    Class<? extends TypeIterator>[] analyses() default {};
}