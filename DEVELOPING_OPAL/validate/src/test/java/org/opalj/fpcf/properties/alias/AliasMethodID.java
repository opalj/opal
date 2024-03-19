/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.alias;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;

/**
 * Annotation used to set the ID of a method.
 * This is used to specify the enclosing method in the alias line annotations.
 */
@Documented
@Target({METHOD})
@Retention(RetentionPolicy.CLASS)
public @interface AliasMethodID {

    int id();

    Class<?> clazz();

}
