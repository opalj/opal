/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.immutability.fields;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.opalj.br.fpcf.FPCFAnalysis;
import org.opalj.fpcf.properties.PropertyValidator;
import org.opalj.tac.fpcf.analyses.FieldImmutabilityAnalysis;

/**
 * Annotation to state that the annotated field is transitively immutable.
 */
@PropertyValidator(key="FieldImmutability",validator= TransitiveImmutableFieldMatcher.class)
@Documented
@Retention(RetentionPolicy.CLASS)
public @interface TransitivelyImmutableField {

    /**
     * A short reasoning of this property.
     */
    String value();

    Class<? extends FPCFAnalysis>[] analyses() default { FieldImmutabilityAnalysis.class };
}
