/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.br.analyses.properties.declared_methods;

import java.lang.annotation.*;

import static java.lang.annotation.ElementType.TYPE;

/**
 * States that the annotated class should have a corresponding DefinedMethod with the given name,
 * descriptor and a method that originates from the given declaring class.
 *
 * @author Dominik Helm
 */
@Target({ TYPE })
@Documented
@Retention(RetentionPolicy.CLASS)
@Repeatable(DeclaredMethods.class)
public @interface DeclaredMethod {
    String name();
    String descriptor();
    Class<?>[] declaringClass();
}
