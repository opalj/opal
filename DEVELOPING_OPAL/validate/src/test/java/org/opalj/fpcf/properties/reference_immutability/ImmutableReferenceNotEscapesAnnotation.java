/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.reference_immutability;

import org.opalj.br.fpcf.FPCFAnalysis;
import org.opalj.fpcf.properties.PropertyValidator;
import org.opalj.tac.fpcf.analyses.immutability.reference.L0ReferenceImmutabilityAnalysis;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotation to state that the annotated reference is immutable
 *
 * @author Tobias Peter Roth
 */
@PropertyValidator(key = "ReferenceImmutability",validator = ImmutableReferenceNotEscapesMatcher.class)
@Documented
@Retention(RetentionPolicy.CLASS)
public @interface ImmutableReferenceNotEscapesAnnotation {

    /**
     * A short reasoning of this property.
     */
    String value();// default = "N/A";

    Class<? extends FPCFAnalysis>[] analyses() default {L0ReferenceImmutabilityAnalysis.class};

}
