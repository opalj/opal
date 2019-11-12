/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.field_immutability;

import org.opalj.br.fpcf.FPCFAnalysis;
import org.opalj.br.fpcf.analyses.L0FieldImmutabilityAnalysis;
import org.opalj.fpcf.properties.PropertyValidator;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotation to state that the annotated field is shallow immutable
 *
 * @author Tobias Peter Roth
 */
@PropertyValidator(key="FieldImmutability",validator=DependentImmutableFieldMatcher.class)
@Documented
@Retention(RetentionPolicy.CLASS)
public @interface DependentImmutableFieldAnnotation {

    /**
     * A short reasoning of this property.
     */
    String value() ; // default = "N/A";

    Class<? extends FPCFAnalysis>[] analyses() default {
            L0FieldImmutabilityAnalysis.class
    };

}
