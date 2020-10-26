/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.immutability.references;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.opalj.br.fpcf.FPCFAnalysis;
import org.opalj.fpcf.properties.PropertyValidator;
import org.opalj.tac.fpcf.analyses.immutability.fieldreference.L0FieldReferenceImmutabilityAnalysis;

/**
 * Annotation to state that the annotated field reference is not thread safe but deterministic lazy initialized
 */
@PropertyValidator(key = "FieldReferenceImmutability",validator = LazyInitializedNotThreadSafeButDeterministicFieldReferenceMatcher.class)
@Documented
@Retention(RetentionPolicy.CLASS)
public @interface LazyInitializedNotThreadSafeButDeterministicReference {

    /**
     * A short reasoning of this property.
     */
    String value();

    Class<? extends FPCFAnalysis>[] analyses() default {L0FieldReferenceImmutabilityAnalysis.class};

}
