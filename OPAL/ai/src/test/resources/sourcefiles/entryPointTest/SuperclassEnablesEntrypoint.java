/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package entryPointTest;

import org.opalj.fpcf.test.annotations.EntryPointProperty;

/**
 * This class is for test purpose only. In this case enables this superclass the
 * "publicMethod" as EntryPoint. The package private class MethodBecomeEntryPointThroughSuperclass
 * could leak casted to the SuperclassEnablesEntrypoint. Hence, the publicMethod of the supclass
 * could be called.
 * 
 * @author Michael Reif
 */
public class SuperclassEnablesEntrypoint {
	
	public void publicMethod(){
		
	}
}

class MethodBecomeEntryPointThroughSuperclass extends SuperclassEnablesEntrypoint {
	
	/* Since no instance is created is can not escape throug the super class
	 * This test depends stricly on the instantiability analysis.
	 * */
	@EntryPointProperty
	public void publicMethod(){
		
	}
}
