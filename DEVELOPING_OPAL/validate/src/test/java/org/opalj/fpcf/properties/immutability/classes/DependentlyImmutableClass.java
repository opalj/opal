/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.immutability.classes;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.opalj.br.fpcf.FPCFAnalysis;
import org.opalj.fpcf.properties.PropertyValidator;
import org.opalj.tac.fpcf.analyses.immutability.ClassImmutabilityAnalysis;

/**
 * Annotation to state that the annotated class is dependent immutable
 *
 * @author Tobias Roth
 */
@PropertyValidator(key = "ClassImmutability",validator = DependentlyImmutableClassMatcher.class)
@Documented
@Retention(RetentionPolicy.CLASS)
public @interface DependentlyImmutableClass {

    /**
     * A short reasoning of this property.
     */
    String value();

    String[] parameter() default {""};

    Class<? extends FPCFAnalysis>[] analyses() default {ClassImmutabilityAnalysis.class};
}
