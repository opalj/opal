/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.immutability.classes;

import org.opalj.br.fpcf.FPCFAnalysis;
import org.opalj.fpcf.properties.PropertyValidator;
import org.opalj.tac.fpcf.analyses.immutability.L1ClassImmutabilityAnalysis;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotation to state that the annotated class is mutable
 *
 * @author Tobias Roth
 */
@PropertyValidator(key = "ClassImmutability",validator = MutableClassMatcher.class)
@Documented
@Retention(RetentionPolicy.CLASS)
public @interface MutableClass {

    /**
     * A short reasoning of this property.
     */
    String value();

    Class<? extends FPCFAnalysis>[] analyses() default {L1ClassImmutabilityAnalysis.class};
}
