/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package factoryMethodTest;

import org.opalj.fpcf.test.annotations.FactoryMethodKeys;
import org.opalj.fpcf.test.annotations.FactoryMethodProperty;
import org.opalj.fpcf.test.annotations.InstantiabilityProperty;

/**
 * 
 * This class has a protected constructor, hence it can be instantiated. A factory
 * method is not offered by this class, since `fakeInstance()` always returns null.
 * 
 * @author Michael Reif
 *
 */
@InstantiabilityProperty
public class NoFactoryButInstantiable {

	protected NoFactoryButInstantiable(){
		this("Just kidding!");
	}
	
	
	private NoFactoryButInstantiable(String msg){
		System.out.println(msg);
	}
	
	@FactoryMethodProperty(FactoryMethodKeys.NotFactoryMethod)
	public static NoFactoryButInstantiable fakeInstance() { return null; }
}
