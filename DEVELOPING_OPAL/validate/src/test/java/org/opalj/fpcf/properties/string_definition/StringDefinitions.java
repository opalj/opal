/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.string_definition;

import org.opalj.fpcf.properties.PropertyValidator;

import java.lang.annotation.*;

/**
 * The StringDefinitions annotation states how a string field or local variable is used during a
 * program execution.
 *
 * @author Patrick Mell
 */
@PropertyValidator(key = "StringDefinitions", validator = LocalStringDefinitionMatcher.class)
@Documented
@Retention(RetentionPolicy.CLASS)
@Target({ ElementType.METHOD })
public @interface StringDefinitions {

    /**
     * A short reasoning of this property.
     */
    String value() default "N/A";

    /**
     * This value determines the expectedLevel of freedom for a string field or local variable to be
     * changed. The default value is {@link StringConstancyLevel#DYNAMIC}.
     */
    StringConstancyLevel expectedLevel() default StringConstancyLevel.DYNAMIC;

    /**
     * A set of string elements that are expected. If exact matching is desired, insert only one
     * element. Otherwise, a super set may be specified, e.g., if some value from an array is
     * expected.
     */
    String[] expectedValues() default "";

    /**
     * `pc` identifies the program counter of the statement for which a `UVar` is to be
     * extracted for a test. Note that if an expression has more than one `UVar`, the test suite
     * is free to choose which one it actually uses for its test(s).
     */
    int pc();

}
