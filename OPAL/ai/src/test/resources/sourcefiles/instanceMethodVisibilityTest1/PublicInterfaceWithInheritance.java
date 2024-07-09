/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package instanceMethodVisibilityTest1;

import org.opalj.fpcf.test.annotations.ProjectAccessibilityKeys;
import org.opalj.fpcf.test.annotations.ProjectAccessibilityProperty;

public interface PublicInterfaceWithInheritance extends ManInTheMiddle{

	@ProjectAccessibilityProperty
	default void overriddenInPublicSubInterface(){
	}
}

interface ManInTheMiddle extends RootInterface {
	
	@ProjectAccessibilityProperty
	default void overriddenInSubInterface(){
	}
}

interface RootInterface {
	
	@ProjectAccessibilityProperty
	default void nonOverridenInSubInterface(){
	}
	
	@ProjectAccessibilityProperty(
			cpa=ProjectAccessibilityKeys.PackageLocal)
	default void overriddenInSubInterface(){
	}
	
	@ProjectAccessibilityProperty(
			cpa=ProjectAccessibilityKeys.PackageLocal)
	default void overriddenInPublicSubInterface(){
	}
}