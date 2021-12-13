/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.callgraph;

import org.opalj.fpcf.properties.PropertyValidator;
import org.opalj.tac.fpcf.analyses.cg.CHATypeProvider;
import org.opalj.tac.fpcf.analyses.cg.TypeProvider;

import java.lang.annotation.*;

/**
 * Container of the {@link DirectCall} annotation.
 *
 * Taken from JCG project. TODO: We should either depend on JCG or include the core base into OPAL.
 *
 * @author Florian Kuebler
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
@Documented
@PropertyValidator(key = "Callees", validator = DirectCallMatcher.class)
public @interface DirectCalls {

    DirectCall[] value();

    /**
     * The call graph analyses which we expect to resolve the annotated calls.
     * If the list is empty, we assume the annotation applies to any call graph
     * algorithm.
     */
    Class<? extends TypeProvider>[] analyses() default { CHATypeProvider.class};
}