/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.br.analyses.properties.declared_methods;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;

/**
 * Container for mutliple [[DeclaredMethod]] annotations.
 *
 * @author Dominik Helm
 */
@Target({ TYPE })
@Documented
@Retention(RetentionPolicy.CLASS)
public @interface DeclaredMethods {
    DeclaredMethod[] value();
}
