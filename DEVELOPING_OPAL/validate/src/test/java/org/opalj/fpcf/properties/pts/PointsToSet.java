/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.pts;

import org.opalj.fpcf.properties.PropertyValidator;
import org.opalj.tac.fpcf.analyses.cg.TypeIterator;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.CLASS;

/**
 * Describes a method call at a specific call site and states which methods
 * the call must be resolved to.
 * Using this annotation implies that the call edges must be
 * directly available within the call graph from the specified call site. Therefore, this annoation
 * can be used to specify monomorphic or polymorphic method calls but are not suited to specify
 * indirect call targets, e.g., reflective call targets.
 * Furthermore, it is possible to exclude certain target methods.
 *
 * Taken from JCG project. TODO: We should either depend on JCG or include the core base into OPAL.
 *
 * @author Florian Kuebler
 * @author Michael Reif
 */
@Retention(CLASS)
@Target({METHOD, CONSTRUCTOR})
@Documented
@PropertyValidator(key = "PointsToSetIncludes", validator = PointsToSetMatcher.class)
public @interface PointsToSet {

    /**
     * the points-to-set to be validated (line number of def-site)
     */
    int variableDefinition() default -1;
    int parameterIndex() default -1;
    JavaMethodContextAllocSite[] expectedJavaAllocSites() default {};
    JavaScriptContextAllocSite[] expectedJavaScriptAllocSites() default {};

    /**
     * The call graph analyses which we expect to resolve the annotated calls.
     * If the list is empty, we assume the annotation applies to any call graph
     * algorithm.
     */
    Class<? extends TypeIterator>[] analyses() default {};

}