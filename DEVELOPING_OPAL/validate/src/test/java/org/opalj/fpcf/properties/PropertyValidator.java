/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties;

import java.lang.annotation.*;

/**
 * Meta-annotation that specifies which class will check an entity's property. This annotation
 * is always used to annotate the custom annotations that specify the expected property.
 *
 * @author Michael Eichberg
 */
@Documented
@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface PropertyValidator{

    /**
     * The key (name) of the respective property kind.
     *
     * The name is used to filer the property validators that will be used to validate the property.
     */
    String key();

    /**
     * The concrete class which will check if given element's computed property.
     */
    Class<? extends PropertyMatcher> validator();
}
