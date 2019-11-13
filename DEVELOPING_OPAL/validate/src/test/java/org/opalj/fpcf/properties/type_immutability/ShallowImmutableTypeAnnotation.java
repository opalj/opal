/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.type_immutability;

import org.opalj.fpcf.properties.PropertyValidator;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotation to state that the annotated type shallow immutable.
 *
 * @author Tobias Peter Roth
 */
@PropertyValidator(key = "TypeImmutability_new", validator = ShallowImmutableTypeMatcher.class)
@Documented
@Retention(RetentionPolicy.CLASS)
public @interface ShallowImmutableTypeAnnotation {

    /**
     * A short reasoning of this property.
     */
    String value();// default = "N/A";
}
