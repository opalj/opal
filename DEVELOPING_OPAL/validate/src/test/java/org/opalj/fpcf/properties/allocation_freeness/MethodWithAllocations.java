/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.allocation_freeness;

import org.opalj.fpcf.properties.PropertyValidator;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotation to state that the annotated method has (transitive) allocations.
 *
 * @author Dominik Helm
 */
@PropertyValidator(key = "AllocationFreeness", validator = MethodWithAllocationsMatcher.class)
@Documented
@Retention(RetentionPolicy.CLASS)
public @interface MethodWithAllocations {

    /**
     * A short reasoning of this property.
     */
    String value(); // default = "N/A";
}
