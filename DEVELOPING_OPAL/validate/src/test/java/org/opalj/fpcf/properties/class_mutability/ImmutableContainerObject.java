/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.class_mutability;

import org.opalj.fpcf.properties.PropertyValidator;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotation to state that the annotated class is an immutable container.
 *
 * @author Florian Kuebler
 */
@PropertyValidator(key = "ClassImmutability",validator = ImmutableContainerObjectMatcher.class)
@Documented
@Retention(RetentionPolicy.CLASS)
public @interface ImmutableContainerObject {

    /**
     * A short reasoning of this property.
     */
    String value();// default = "N/A";
}
