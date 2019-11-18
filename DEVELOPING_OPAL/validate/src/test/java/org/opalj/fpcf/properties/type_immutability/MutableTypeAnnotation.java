/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.type_immutability;

import org.opalj.br.fpcf.FPCFAnalysis;
import org.opalj.fpcf.properties.PropertyValidator;
import org.opalj.tac.fpcf.analyses.LxTypeImmutabilityAnalysis_new;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotation to state that the annotated type mutable.
 *
 * @author Tobias Peter Roth
 */
@PropertyValidator(key = "TypeImmutability_new", validator = NewMutableTypeMatcher.class)
@Documented
@Retention(RetentionPolicy.CLASS)
public @interface MutableTypeAnnotation {

    /**
     * A short reasoning of this property.
     */
    String value();// default = "N/A";

    Class<? extends FPCFAnalysis>[] analyses() default {LxTypeImmutabilityAnalysis_new.class};
}
