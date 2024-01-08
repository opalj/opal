/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package factoryMethodTest;

import org.opalj.fpcf.test.annotations.FactoryMethodKeys;
import org.opalj.fpcf.test.annotations.FactoryMethodProperty;
import org.opalj.fpcf.test.annotations.InstantiabilityProperty;

/**
 * 
 * @author Michael Reif
 *
 */
@InstantiabilityProperty
public class FactoryAndInstantiable {

	private FactoryAndInstantiable(){
		//do Something
	}
	
	@FactoryMethodProperty(FactoryMethodKeys.IsFactoryMethod)
	static void newInstance(){
		new FactoryAndInstantiable();
	}
	
}
