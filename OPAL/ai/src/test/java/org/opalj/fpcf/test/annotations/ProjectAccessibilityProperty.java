/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.test.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * 
 * Describes the [[ProjectAccessibility]] property of the OPAL FixpointAnalysis.
 * 
 * @author Michael Reif
 *
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface ProjectAccessibilityProperty {
	
	ProjectAccessibilityKeys opa() default ProjectAccessibilityKeys.Global;
	ProjectAccessibilityKeys cpa() default ProjectAccessibilityKeys.Global;
	ProjectAccessibilityKeys application() default ProjectAccessibilityKeys.Global;
}
