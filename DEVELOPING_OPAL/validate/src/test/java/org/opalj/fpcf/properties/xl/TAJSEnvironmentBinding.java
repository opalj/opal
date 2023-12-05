/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.xl;

import org.opalj.fpcf.properties.PropertyValidator;
import org.opalj.fpcf.properties.pts.JavaMethodContextAllocSite;
import org.opalj.fpcf.properties.pts.JavaScriptContextAllocSite;
import org.opalj.fpcf.properties.pts.PointsToSetMatcher;
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

/**
 * to be implemented once TAJS pts have been implemented.
 * Intermediate implementation specifies location of ScriptEngine.eval() call, and allocated type.
 * source file (specified through class) and line number are used
 */
public @interface TAJSEnvironmentBinding {
    String identifier();
    String value();
}