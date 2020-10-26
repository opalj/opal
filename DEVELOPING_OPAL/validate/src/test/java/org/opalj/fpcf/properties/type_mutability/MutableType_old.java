/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.type_mutability;

import org.opalj.fpcf.properties.PropertyValidator;
import org.opalj.fpcf.properties.immutability.types.MutableTypeMatcher;

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
public @interface MutableType_old {

    /**
     * A short reasoning of this property.
     */
    String value();// default = "N/A";
}
