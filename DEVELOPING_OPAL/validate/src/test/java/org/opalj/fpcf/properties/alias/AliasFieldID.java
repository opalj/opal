/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.alias;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;

/**
 * Annotation used to set the ID of a field.
 * This is used to specify the referenced field in the alias annotations.
 */
@Documented
@Target({FIELD})
@Retention(RetentionPolicy.CLASS)
public @interface AliasFieldID {

    int id();

    Class<?> clazz();

}

