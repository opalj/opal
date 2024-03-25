/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.xl;

import org.opalj.fpcf.properties.PropertyValidator;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.CLASS;

/**
 * Describeds expected values to be contained in a TAJS Environment Map
 */
@Retention(CLASS)
@Target({METHOD, CONSTRUCTOR})
@Documented
@PropertyValidator(key = "TAJSEnvironment", validator = TAJSEnvironmentMatcher.class)
public @interface JSEnvironment {

    JSEnvironmentBinding[] bindings();

}