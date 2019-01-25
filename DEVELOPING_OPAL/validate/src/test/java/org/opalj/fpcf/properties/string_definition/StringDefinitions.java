/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.string_definition;

import org.opalj.fpcf.properties.PropertyValidator;

import java.lang.annotation.*;

/**
 * The StringDefinitions annotation states how a string field or local variable is used during a
 * program execution.
 * <p>
 * Note that the {@link StringDefinitions} annotation is designed in a way to be able to capture
 * only one read operation per test method. If this is a limitation, either (1) duplicate the
 * corresponding test method and remove the first calls which trigger the analysis or (2) put the
 * relevant code of the test function into a dedicated function and then call it from different
 * test methods (to avoid copy&paste).
 *
 * @author Patrick Mell
 */
@PropertyValidator(key = "StringConstancy", validator = LocalStringDefinitionMatcher.class)
@Documented
@Retention(RetentionPolicy.CLASS)
@Target({ ElementType.ANNOTATION_TYPE })
public @interface StringDefinitions {

    /**
     * This value determines the expected level of freedom for a local variable to
     * be changed.
     */
    StringConstancyLevel expectedLevel();

    /**
     * A regexp like string that describes the element(s) that are expected. For the rules, refer to
     * {@link org.opalj.br.fpcf.properties.string_definition.properties.StringTreeElement}.
     * For example, "(* | (hello | world)^5)" describes a string which can 1) either be any string
     * or 2) a five time concatenation of "hello" and/or "world".
     */
    String expectedStrings();

}
