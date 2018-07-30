/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.type_mutability;

import org.opalj.fpcf.properties.PropertyValidator;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotation to state that the annotated type mutable.
 *
 * @author Florian Kuebler
 */
@PropertyValidator(key = "TypeImmutability", validator = MutableTypeMatcher.class)
@Documented
@Retention(RetentionPolicy.CLASS)
public @interface MutableType {

    /**
     * A short reasoning of this property.
     */
    String value();// default = "N/A";
}
