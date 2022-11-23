/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.immutability.types;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.opalj.br.fpcf.FPCFAnalysis;
import org.opalj.fpcf.properties.PropertyValidator;
import org.opalj.tac.fpcf.analyses.immutability.TypeImmutabilityAnalysis;

/**
 * Annotation to state that the annotated type is dependently immutable.
 *
 * @author Tobias Peter Roth
 */
@PropertyValidator(key = "TypeImmutability", validator = DependentlyImmutableTypeMatcher.class)
@Documented
@Retention(RetentionPolicy.CLASS)
public @interface DependentlyImmutableType {

    /**
     * A short reasoning of this property.
     */
    String value();

    String[] parameter() default {""};

    Class<? extends FPCFAnalysis>[] analyses() default {TypeImmutabilityAnalysis.class};
}
